package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.session.APISession;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Set;

/**
 * Created by pablo on 06/07/2017.
 */
@Path("processes")
public class Processes {

    private APISession apiSession;

    public void init() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException {
        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        apiSession = loginAPI.login(ServletContextClass.USERNAME, ServletContextClass.PASSWORD);
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProcesses() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException {
        init();
        Set<String> l = ServletContextClass.failedFlowNodesAccesor.getProcessUsedDefinitions(apiSession);
        return Response.status(Response.Status.OK).entity(l).build();
    }
}
