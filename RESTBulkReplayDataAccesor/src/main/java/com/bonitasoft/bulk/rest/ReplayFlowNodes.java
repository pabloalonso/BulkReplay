package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.beans.FlowNodes;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.command.CommandExecutionException;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.CommandParameterizationException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.platform.LogoutException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.session.SessionNotFoundException;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by pablo on 10/07/2017.
 */
@Path("replay")
public class ReplayFlowNodes {


    private Logger logger = Logger.getLogger("com.bonitasoft.bulk.rest");


    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response replayFlowNodes(FlowNodes fntr) throws LoginException, ServerAPIException, BonitaHomeNotSetException, UnknownAPITypeException, CommandNotFoundException, CommandParameterizationException, CommandExecutionException, LogoutException, SessionNotFoundException {
        APISession apiSession = ServletContextClass.login();
        final Map<String, Serializable> parameters = new HashMap<String, Serializable>();

        parameters.put("tenantId", 1);
        Long interval = fntr.getInterval();
        if(interval < ServletContextClass.getBulkMinSeconds()){
            logger.warning("Original interval of "+ interval + " has been overrided by the configured BULK_MIN_SECONDS: "+ ServletContextClass.getBulkMinSeconds());
            interval = ServletContextClass.getBulkMinSeconds();
        }

        Long batchSize = fntr.getBatchSize();
        if(batchSize > ServletContextClass.getBulkMaxBatchSize()){
            logger.warning("Original batchSize of "+ batchSize + " has been overrided by the configured BULK_MAX_BATCH_SIZE: "+ ServletContextClass.getBulkMaxBatchSize());
            batchSize = ServletContextClass.getBulkMaxBatchSize();
        }

        parameters.put("interval", interval);
        parameters.put("batchSize", batchSize);
        parameters.put("ids", (Serializable) fntr.getIds());
        TenantAPIAccessor.getCommandAPI(apiSession).execute(ServletContextClass.RETRY_COMMAND_NAME, parameters);
        ServletContextClass.logout(apiSession);
        return Response.status(Response.Status.OK).entity(fntr).build();

    }
}
