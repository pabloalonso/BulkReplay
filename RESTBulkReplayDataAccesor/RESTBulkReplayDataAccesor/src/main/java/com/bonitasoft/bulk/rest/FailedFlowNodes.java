package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.FailedFlowNodesAccesor;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.api.ApiAccessType;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.bpm.flownode.FlowNodeInstance;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
import org.bonitasoft.engine.exception.ServerAPIException;
import org.bonitasoft.engine.exception.UnknownAPITypeException;
import org.bonitasoft.engine.platform.LoginException;
import org.bonitasoft.engine.session.APISession;
import org.bonitasoft.engine.util.APITypeManager;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by pablo on 06/07/2017.
 */
@Path("failedFlowNodes")
public class FailedFlowNodes {

    private APISession apiSession;

    public void init() throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException {

        LoginAPI loginAPI = TenantAPIAccessor.getLoginAPI();
        apiSession = loginAPI.login(ServletContextClass.USERNAME, ServletContextClass.PASSWORD);
    }


    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFailedFlowNodes(@QueryParam("startDate") Long startDateMs, @QueryParam("endDate") Long endDateMs) throws UnknownAPITypeException, ServerAPIException, ConnectorInstanceNotFoundException, BonitaHomeNotSetException, SearchException, LoginException {
        init();
        Map<String, Map<String, Map<String, Serializable>>> l = ServletContextClass.failedFlowNodesAccesor.searchFailedFlowNodes(apiSession, startDateMs, endDateMs);
        return Response.status(Response.Status.OK).entity(l).build();
    }
}
