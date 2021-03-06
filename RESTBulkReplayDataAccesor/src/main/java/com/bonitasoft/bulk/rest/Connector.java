package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.beans.FlowNodes;
import com.bonitasoft.bulk.setup.ServletContextClass;
import com.bonitasoft.engine.api.LoginAPI;
import com.bonitasoft.engine.api.TenantAPIAccessor;
import org.bonitasoft.engine.bpm.connector.ConnectorInstanceNotFoundException;
import org.bonitasoft.engine.exception.BonitaHomeNotSetException;
import org.bonitasoft.engine.exception.SearchException;
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
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by pablo on 06/07/2017.
 */
@Path("connector")
public class Connector {

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    @Consumes(MediaType.APPLICATION_JSON)
    public Response getConnectorInformation(FlowNodes fns) throws BonitaHomeNotSetException, ServerAPIException, UnknownAPITypeException, LoginException, SearchException, ConnectorInstanceNotFoundException, LogoutException, SessionNotFoundException {
        APISession apiSession = ServletContextClass.login();
        List<Map<String, Serializable>> l = ServletContextClass.failedFlowNodesAccesor().getDetailedConnectorInformation(apiSession, fns.getIds());
        ServletContextClass.logout(apiSession);
        return Response.status(Response.Status.OK).entity(l).build();
    }
}
