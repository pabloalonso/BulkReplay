package com.bonitasoft.bulk.command;


import com.bonitasoft.bulk.job.DeleteCaseJob;
import org.bonitasoft.engine.builder.BuilderFactory;
import org.bonitasoft.engine.command.SCommandExecutionException;
import org.bonitasoft.engine.command.SCommandParameterizationException;
import org.bonitasoft.engine.command.TenantCommand;
import org.bonitasoft.engine.scheduler.SchedulerService;
import org.bonitasoft.engine.scheduler.builder.SJobDescriptorBuilderFactory;
import org.bonitasoft.engine.scheduler.builder.SJobParameterBuilderFactory;
import org.bonitasoft.engine.scheduler.exception.SSchedulerException;
import org.bonitasoft.engine.scheduler.model.SJobDescriptor;
import org.bonitasoft.engine.scheduler.model.SJobParameter;
import org.bonitasoft.engine.scheduler.trigger.OneShotTrigger;
import org.bonitasoft.engine.scheduler.trigger.Trigger;
import org.bonitasoft.engine.service.TenantServiceAccessor;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by pablo on 10/07/2017.
 */
public class DeleteCommand extends TenantCommand {

    private Long count =1L;
    private final Long startDate = System.currentTimeMillis();
    private static final Logger logger = Logger.getLogger("com.bonitasoft.bulk.command");
    public Serializable execute(Map<String, Serializable> map, TenantServiceAccessor tenantServiceAccessor) throws SCommandParameterizationException, SCommandExecutionException {
        try {
            final SchedulerService schedulerService = tenantServiceAccessor.getSchedulerService();
            Integer tenantId = (Integer)map.get("tenantId");
            List<Long> ids = (List<Long>) map.get("ids");
            Long interval = (Long) map.get("interval");
            Long batchSize = (Long) map.get("batchSize");
            logger.warning("ids size: " + ids.size());
            logger.warning("interval: " + interval);
            logger.warning("batchSize: " + batchSize);
            for(Long id : ids) {
                try {
                    Date date = calculateDate(interval, batchSize);
                    deleteCase(schedulerService, id, date, tenantId);
                }catch(SSchedulerException sse){
                    logger.warning("Case with id "+ id + " could not be planned to delete");
                }finally {
                    continue;
                }
            }
            return ids.size();
        } catch (final Exception e) {
            throw new SCommandExecutionException(e);
        }

    }

    private Date calculateDate(Long interval, Long batchSize) {
        long batchNum = (count / batchSize);
        count++;
        return new Date(startDate + (batchNum * interval * 1000));
    }

    private void deleteCase(SchedulerService schedulerService, Long id, Date date, int tenantId) throws SSchedulerException {
        final Trigger trigger = new OneShotTrigger("OneShot-"+id, date);
        final SJobDescriptor jobDescriptor = BuilderFactory.get(SJobDescriptorBuilderFactory.class)
                .createNewInstance(DeleteCaseJob.class.getName(), "DeleteCaseJob-"+id).done();
        SJobParameter tenantIdParam = BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("tenantId", tenantId).done();
        SJobParameter caseIdParam = BuilderFactory.get(SJobParameterBuilderFactory.class).createNewInstance("caseId", id).done();
        final List<SJobParameter> params = new ArrayList<SJobParameter>(2);
        params.add(tenantIdParam);
        params.add(caseIdParam);

        schedulerService.schedule(jobDescriptor, params, trigger);
    }
}
