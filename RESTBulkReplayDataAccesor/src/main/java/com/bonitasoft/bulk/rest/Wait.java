package com.bonitasoft.bulk.rest;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.concurrent.TimeUnit;

/**
 * Created by pablo on 06/07/2017.
 */
@Path("wait")
public class Wait {



    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response waitSomeTime(@QueryParam("secsToWait") Integer time) throws InterruptedException {
        TimeUnit.SECONDS.sleep(time);
        return Response.status(Response.Status.OK).entity("Waited " + time + " seconds").build();
    }
}
