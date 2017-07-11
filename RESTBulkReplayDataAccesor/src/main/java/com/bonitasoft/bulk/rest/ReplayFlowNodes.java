package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.beans.FlowNodesToReplay;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import junit.framework.Assert;
import org.bonitasoft.engine.command.CommandExecutionException;
import org.bonitasoft.engine.command.CommandNotFoundException;
import org.bonitasoft.engine.command.CommandParameterizationException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.session.APISession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by pablo on 10/07/2017.
 */
@Path("replay")
public class ReplayFlowNodes {

    private APISession apiSession;
    private Logger logger = Logger.getLogger("com.bonitasoft.bulk.rest");

    public void init() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        apiSession = loginAPI.login(ServletContextClass.USERNAME, ServletContextClass.PASSWORD);
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response replayFlowNodes(FlowNodesToReplay fntr) throws LoginException, ServerAPIException, BonitaHomeNotSetException, UnknownAPITypeException, CommandNotFoundException, CommandParameterizationException, CommandExecutionException {
        init();
        final Map<String, Serializable> parameters = new HashMap<String, Serializable>();

        parameters.put("tenantId", 1);
        parameters.put("interval", fntr.getInterval());
        parameters.put("batchSize", fntr.getBatchSize());
        parameters.put("ids", (Serializable) fntr.getIds());
        TenantAPIAccessor.getCommandAPI(apiSession).execute(ServletContextClass.COMMAND_NAME, parameters);

        return Response.status(Response.Status.OK).entity(fntr.toString()).build();

    }
}
