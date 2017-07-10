package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.beans.FlowNodesToReplay;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.session.APISession;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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
    public Response getProcesses(FlowNodesToReplay fntr) throws LoginException, ServerAPIException, BonitaHomeNotSetException, UnknownAPITypeException {
        init();
        logger.warning(fntr.toString());

        return Response.status(Response.Status.OK).entity(fntr.toString()).build();

    }
}
