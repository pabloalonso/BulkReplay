package com.bonitasoft.bulk.job;


import org.bonitasoft.engine.api.impl.connector.ConnectorReseter;
import org.bonitasoft.engine.api.impl.connector.ResetAllFailedConnectorStrategy;
import org.bonitasoft.engine.api.impl.flownode.FlowNodeRetrier;
import org.bonitasoft.engine.core.connector.ConnectorInstanceService;
import org.bonitasoft.engine.events.model.SFireEventException;
import org.bonitasoft.engine.scheduler.StatelessJob;
import org.bonitasoft.engine.scheduler.exception.SJobConfigurationException;
import org.bonitasoft.engine.scheduler.exception.SJobExecutionException;
import org.bonitasoft.engine.service.TenantServiceAccessor;
import org.bonitasoft.engine.service.impl.ServiceAccessorFactory;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by pablo on 10/07/2017.
 */
public class ReplayFlowNodeJob implements StatelessJob{
    private Long flowNodeId;
    private long tenantId;
    private static int BATCH_SIZE = 500;
    private static Logger logger = Logger.getLogger("com.bonitasoft.bulk");

    public String getName() {
        return "retryFlowNode-"+flowNodeId;
    }

    public String getDescription() {
        return "Job that retry a failed flow node";
    }

    public void execute() throws SJobExecutionException, SFireEventException {
        ServiceAccessorFactory serviceAccessorFactory = ServiceAccessorFactory.getInstance();
        logger.warning("Retrying Flow node "+ flowNodeId);
        try {
            final TenantServiceAccessor tenantServiceAccessor = serviceAccessorFactory.createTenantServiceAccessor(tenantId);
            final ConnectorInstanceService connectorInstanceService = tenantServiceAccessor.getConnectorInstanceService();
            ResetAllFailedConnectorStrategy strategy = new ResetAllFailedConnectorStrategy(connectorInstanceService,
                    new ConnectorReseter(connectorInstanceService), BATCH_SIZE);
            FlowNodeRetrier flowNodeRetrier = new FlowNodeRetrier(tenantServiceAccessor.getContainerRegistry(),
                    tenantServiceAccessor.getFlowNodeExecutor(),tenantServiceAccessor.getActivityInstanceService(),
                    tenantServiceAccessor.getFlowNodeStateManager(),strategy);
            flowNodeRetrier.retry(flowNodeId);
            logger.warning("Flow node retried"+ flowNodeId);
        } catch (Exception e) {
            logger.severe("Flow node "+ flowNodeId + " could not be retried: " + e.getMessage());
            throw new SJobExecutionException(e);
        }
    }

    public void setAttributes(Map<String, Serializable> map) throws SJobConfigurationException {
        final Long flowNodeId = (Long) map.get("flowNodeId");
        if(flowNodeId != null){
           this.flowNodeId = flowNodeId;
        }
        final Integer tenantId = (Integer) map.get("tenantId");
        if(tenantId != null){
            this.tenantId = tenantId;
        }
        
    }
}
