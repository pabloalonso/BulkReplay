package com.bonitasoft.bulk.job;


import org.bonitasoft.engine.api.impl.connector.ConnectorReseter;
import org.bonitasoft.engine.api.impl.connector.ResetAllFailedConnectorStrategy;
import org.bonitasoft.engine.api.impl.flownode.FlowNodeRetrier;
import org.bonitasoft.engine.commons.exceptions.SBonitaException;
import org.bonitasoft.engine.commons.exceptions.SBonitaRuntimeException;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.core.process.definition.model.SProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.SSubProcessDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SBoundaryEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SCatchEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SIntermediateCatchEventDefinition;
import org.bonitasoft.engine.core.process.definition.model.event.SStartEventDefinition;
import org.bonitasoft.engine.core.process.instance.api.ActivityInstanceService;
import org.bonitasoft.engine.core.process.instance.api.ProcessInstanceService;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SFlowNodeNotFoundException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.core.process.instance.api.exceptions.SProcessInstanceNotFoundException;
import org.bonitasoft.engine.core.process.instance.model.SFlowElementsContainerType;
import org.bonitasoft.engine.core.process.instance.model.SFlowNodeInstance;
import org.bonitasoft.engine.core.process.instance.model.SProcessInstance;
import org.bonitasoft.engine.core.process.instance.model.event.SCatchEventInstance;
import org.bonitasoft.engine.events.model.SFireEventException;
import org.bonitasoft.engine.exception.BonitaHomeConfigurationException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.DeletionException;
import org.bonitasoft.engine.exception.ProcessInstanceHierarchicalDeletionException;
import org.bonitasoft.engine.execution.job.JobNameBuilder;
import org.bonitasoft.engine.lock.BonitaLock;
import org.bonitasoft.engine.lock.LockService;
import org.bonitasoft.engine.lock.SLockException;
import org.bonitasoft.engine.log.technical.TechnicalLogSeverity;
import org.bonitasoft.engine.log.technical.TechnicalLoggerService;
import org.bonitasoft.engine.persistence.*;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.StatelessJob;
import org.bonitasoft.engine.scheduler.exception.SJobConfigurationException;
import org.bonitasoft.engine.scheduler.exception.SJobExecutionException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;
import org.bonitasoft.engine.transaction.UserTransactionService;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

/**
 * Created by pablo on 10/07/2017.
 */
public class DeleteCaseJob implements StatelessJob{
    private Long caseId;
    private long tenantId;
    private static int BATCH_SIZE = 500;
    private static Logger logger = Logger.getLogger("com.bonitasoft.bulk");

    public String getName() {
        return "deleteCaseId-"+caseId;
    }

    public String getDescription() {
        return "Job that delete a case";
    }
    private TenantServiceAccessor tenantServiceAccessor;


    public void execute() throws SJobExecutionException, SFireEventException {
        ServiceAccessorFactory serviceAccessorFactory = ServiceAccessorFactory.getInstance();
        logger.warning("Deleting case "+ caseId);
        try {
            tenantServiceAccessor = serviceAccessorFactory.createTenantServiceAccessor(tenantId);
            deleteProcessInstance(caseId );
        } catch (Exception e) {
            logger.severe("Case "+ caseId + " could not be deleted: " + e.getMessage());
            throw new SJobExecutionException(e);
        }
    }

    public void setAttributes(Map<String, Serializable> map) throws SJobConfigurationException {
        final Long caseId = (Long) map.get("caseId");
        if(caseId != null){
           this.caseId = caseId;
        }
        final Integer tenantId = (Integer) map.get("tenantId");
        if(tenantId != null){
            this.tenantId = tenantId;
        }
        
    }

    public void deleteProcessInstance(final long processInstanceId) throws DeletionException {

        final LockService lockService = tenantServiceAccessor.getLockService();
        final String objectType = SFlowElementsContainerType.PROCESS.name();
        BonitaLock lock = null;
        try {
            lock = lockService.lock(processInstanceId, objectType, tenantServiceAccessor.getTenantId());
            deleteProcessInstanceInTransaction(tenantServiceAccessor, processInstanceId);
        } catch (final SProcessInstanceHierarchicalDeletionException e) {
            throw new ProcessInstanceHierarchicalDeletionException(e.getMessage(), e.getProcessInstanceId());
        } catch (final SProcessInstanceNotFoundException e) {
            throw new DeletionException(e);
        } catch (final SBonitaException e) {
            throw new DeletionException(e);
        } finally {
            if (lock != null) {
                try {
                    lockService.unlock(lock, tenantServiceAccessor.getTenantId());
                } catch (final SLockException e) {
                    throw new DeletionException("Lock was not released. Object type: " + objectType + ", id: " + processInstanceId, e);
                }
            }
        }
    }
    private void deleteProcessInstanceInTransaction(final TenantServiceAccessor tenantAccessor, final long processInstanceId) throws SBonitaException {
        final UserTransactionService userTransactionService = tenantAccessor.getUserTransactionService();
        final ProcessInstanceService processInstanceService = tenantAccessor.getProcessInstanceService();

        try {
            userTransactionService.executeInTransaction(new Callable<Void>() {

                public Void call() throws Exception {
                    final SProcessInstance sProcessInstance = processInstanceService.getProcessInstance(processInstanceId);
                    deleteJobsOnProcessInstance(sProcessInstance);
                    processInstanceService.deleteParentProcessInstanceAndElements(sProcessInstance);
                    return null;
                }

            });
        } catch (final SBonitaException e) {
            throw e;
        } catch (final Exception e) {
            throw new SBonitaRuntimeException("Error while deleting the parent process instance and elements.", e);
        }
    }
    private void deleteProcessInstance(final ProcessInstanceService processInstanceService, final Long processInstanceId,
                                       final ActivityInstanceService activityInstanceService) throws SBonitaException, SProcessInstanceHierarchicalDeletionException, BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, BonitaHomeNotSetException {
        final SProcessInstance sProcessInstance = processInstanceService.getProcessInstance(processInstanceId);
        final long callerId = sProcessInstance.getCallerId();
        if (callerId > 0) {
            try {
                final SFlowNodeInstance flowNodeInstance = activityInstanceService.getFlowNodeInstance(callerId);
                final long rootProcessInstanceId = flowNodeInstance.getRootProcessInstanceId();
                final SProcessInstanceHierarchicalDeletionException exception = new SProcessInstanceHierarchicalDeletionException(
                        "Unable to delete the process instance, because the parent is still active.", rootProcessInstanceId);
                exception.setProcessInstanceIdOnContext(processInstanceId);
                exception.setRootProcessInstanceIdOnContext(rootProcessInstanceId);
                exception.setFlowNodeDefinitionIdOnContext(flowNodeInstance.getFlowNodeDefinitionId());
                exception.setFlowNodeInstanceIdOnContext(flowNodeInstance.getId());
                exception.setFlowNodeNameOnContext(flowNodeInstance.getName());
                exception.setProcessDefinitionIdOnContext(flowNodeInstance.getProcessDefinitionId());
                throw exception;
            } catch (final SFlowNodeNotFoundException e) {
                // ok the activity that called this process do not exists anymore
            }
        }
        deleteJobsOnProcessInstance(sProcessInstance);
        processInstanceService.deleteArchivedProcessInstanceElements(processInstanceId, sProcessInstance.getProcessDefinitionId());
        processInstanceService.deleteArchivedProcessInstancesOfProcessInstance(processInstanceId);
        processInstanceService.deleteProcessInstance(sProcessInstance);
    }
    void deleteJobsOnProcessInstance(final SProcessInstance sProcessInstance) throws SBonitaException, BonitaHomeConfigurationException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, BonitaHomeNotSetException {
        deleteJobsOnProcessInstance(sProcessInstance.getProcessDefinitionId(), Collections.singletonList(sProcessInstance));
    }
    private void deleteJobsOnProcessInstance(final long processDefinitionId, final List<SProcessInstance> sProcessInstances)
            throws SBonitaException, BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, IllegalAccessException, NoSuchMethodException, BonitaHomeNotSetException {


        final SProcessDefinition processDefinition = tenantServiceAccessor.getProcessDefinitionService().getProcessDefinition(processDefinitionId);

        for (final SProcessInstance sProcessInstance : sProcessInstances) {
            deleteJobsOnProcessInstance(processDefinition, sProcessInstance);
        }
    }
    private void deleteJobsOnProcessInstance(final SProcessDefinition processDefinition, final SProcessInstance sProcessInstance)
            throws SBonitaException, BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, BonitaHomeNotSetException {

        final List<SStartEventDefinition> startEventsOfSubProcess = processDefinition.getProcessContainer().getStartEvents();
        deleteJobsOnProcessInstance(processDefinition, sProcessInstance, startEventsOfSubProcess);

        final List<SIntermediateCatchEventDefinition> intermediateCatchEvents = processDefinition.getProcessContainer().getIntermediateCatchEvents();
        deleteJobsOnProcessInstance(processDefinition, sProcessInstance, intermediateCatchEvents);

        final List<SBoundaryEventDefinition> sEndEventDefinitions = processDefinition.getProcessContainer().getBoundaryEvents();
        deleteJobsOnProcessInstance(processDefinition, sProcessInstance, sEndEventDefinitions);

        deleteJobsOnEventSubProcess(processDefinition, sProcessInstance);
    }

    private void deleteJobsOnProcessInstance(final SProcessDefinition processDefinition, final SProcessInstance sProcessInstance,
                                             final List<? extends SCatchEventDefinition> sCatchEventDefinitions) throws SBonitaException, BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, BonitaHomeNotSetException {
        for (final SCatchEventDefinition sCatchEventDefinition : sCatchEventDefinitions) {
            deleteJobsOnFlowNodeInstances(processDefinition, sCatchEventDefinition, sProcessInstance);
        }
    }

    private void deleteJobsOnFlowNodeInstances(final SProcessDefinition processDefinition, final SCatchEventDefinition sCatchEventDefinition,
                                               final SProcessInstance sProcessInstance) throws SBonitaException, BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, BonitaHomeNotSetException {

        final ActivityInstanceService activityInstanceService = tenantServiceAccessor.getActivityInstanceService();

        final List<OrderByOption> orderByOptions = Collections.singletonList(new OrderByOption(SCatchEventInstance.class, "id", OrderByType.ASC));
        final FilterOption filterOption1 = new FilterOption(SCatchEventInstance.class, "flowNodeDefinitionId", sCatchEventDefinition.getId());
        final FilterOption filterOption2 = new FilterOption(SCatchEventInstance.class, "logicalGroup4", sProcessInstance.getId());
        QueryOptions queryOptions = new QueryOptions(0, 100, orderByOptions, Arrays.asList(filterOption1, filterOption2), null);
        List<SCatchEventInstance> sCatchEventInstances = activityInstanceService.searchFlowNodeInstances(SCatchEventInstance.class, queryOptions);
        while (!sCatchEventInstances.isEmpty()) {
            for (final SCatchEventInstance sCatchEventInstance : sCatchEventInstances) {
                deleteJobsOnFlowNodeInstance(processDefinition, sCatchEventDefinition, sCatchEventInstance);
            }
            queryOptions = QueryOptions.getNextPage(queryOptions);
            sCatchEventInstances = activityInstanceService.searchFlowNodeInstances(SCatchEventInstance.class, queryOptions);
        }
    }

    private void deleteJobsOnFlowNodeInstance(final SProcessDefinition processDefinition, final SCatchEventDefinition sCatchEventDefinition,
                                              final SCatchEventInstance sCatchEventInstance) throws BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, SBonitaException, BonitaHomeNotSetException {

        final SchedulerService schedulerService = tenantServiceAccessor.getSchedulerService();
        final TechnicalLoggerService logger = tenantServiceAccessor.getTechnicalLoggerService();

        try {
            if (!sCatchEventDefinition.getTimerEventTriggerDefinitions().isEmpty()) {
                final String jobName = JobNameBuilder.getTimerEventJobName(processDefinition.getId(), sCatchEventDefinition, sCatchEventInstance);
                final boolean delete = schedulerService.delete(jobName);
                if (!delete && schedulerService.isExistingJob(jobName)) {
                    if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                        logger.log(this.getClass(), TechnicalLogSeverity.WARNING,
                                "No job found with name '" + jobName + "' when interrupting timer catch event named '" + sCatchEventDefinition.getName()
                                        + "' and id '" + sCatchEventInstance.getId() + "'. It was probably already triggered.");
                    }
                }
            }
        } catch (final Exception e) {
            if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                logger.log(this.getClass(), TechnicalLogSeverity.WARNING, e);
            }
        }
    }
    private void deleteJobsOnEventSubProcess(final SProcessDefinition processDefinition, final SProcessInstance sProcessInstance) throws BonitaHomeConfigurationException, SBonitaException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, BonitaHomeNotSetException {
        final Set<SSubProcessDefinition> sSubProcessDefinitions = processDefinition.getProcessContainer().getSubProcessDefinitions();
        for (final SSubProcessDefinition sSubProcessDefinition : sSubProcessDefinitions) {
            final List<SStartEventDefinition> startEventsOfSubProcess = sSubProcessDefinition.getSubProcessContainer().getStartEvents();
            deleteJobsOnEventSubProcess(processDefinition, sProcessInstance, sSubProcessDefinition, startEventsOfSubProcess);

            final List<SIntermediateCatchEventDefinition> intermediateCatchEvents = sSubProcessDefinition.getSubProcessContainer().getIntermediateCatchEvents();
            deleteJobsOnEventSubProcess(processDefinition, sProcessInstance, sSubProcessDefinition, intermediateCatchEvents);

            final List<SBoundaryEventDefinition> sEndEventDefinitions = sSubProcessDefinition.getSubProcessContainer().getBoundaryEvents();
            deleteJobsOnEventSubProcess(processDefinition, sProcessInstance, sSubProcessDefinition, sEndEventDefinitions);
        }
    }

    private void deleteJobsOnEventSubProcess(final SProcessDefinition processDefinition, final SProcessInstance sProcessInstance,
                                             final SSubProcessDefinition sSubProcessDefinition, final List<? extends SCatchEventDefinition> sCatchEventDefinitions) throws BonitaHomeConfigurationException, InvocationTargetException, IOException, InstantiationException, NoSuchMethodException, IllegalAccessException, SBonitaException, BonitaHomeNotSetException {

        final SchedulerService schedulerService = tenantServiceAccessor.getSchedulerService();
        final TechnicalLoggerService logger = tenantServiceAccessor.getTechnicalLoggerService();

        for (final SCatchEventDefinition sCatchEventDefinition : sCatchEventDefinitions) {
            try {
                if (!sCatchEventDefinition.getTimerEventTriggerDefinitions().isEmpty()) {
                    final String jobName = JobNameBuilder.getTimerEventJobName(processDefinition.getId(), sCatchEventDefinition, sProcessInstance.getId(),
                            sSubProcessDefinition.getId());
                    final boolean delete = schedulerService.delete(jobName);
                    if (!delete && schedulerService.isExistingJob(jobName)) {
                        if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                            logger.log(this.getClass(), TechnicalLogSeverity.WARNING, "No job found with name '" + jobName
                                    + "' when interrupting timer catch event named '" + sCatchEventDefinition.getName()
                                    + "' on event sub process with the id '" + sSubProcessDefinition.getId() + "'. It was probably already triggered.");
                        }
                    }
                }
            } catch (final Exception e) {
                if (logger.isLoggable(this.getClass(), TechnicalLogSeverity.WARNING)) {
                    logger.log(this.getClass(), TechnicalLogSeverity.WARNING, e);
                }
            }
        }
    }
}
