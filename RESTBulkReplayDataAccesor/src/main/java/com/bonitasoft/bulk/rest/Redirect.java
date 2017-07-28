package com.bonitasoft.bulk.rest;

import com.bonitasoft.bulk.setup.ServletContextClass;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.util.EntityUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by pablo on 06/07/2017.
 */
@Path("getDSNCrud")
public class Redirect {

    private final Logger log = Logger.getLogger("com.bonitasoft.bulk.rest");

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response callCrud(@QueryParam("idDsn") String idDsn) throws InterruptedException, IOException, JSONException {
        HttpClient client = new DefaultHttpClient();
        HttpGet request = new HttpGet(ServletContextClass.getCrudEndpoint()+idDsn+ServletContextClass.getDsnInformation());
        HttpResponse response = client.execute(request);
        //Map<String,String> json = response.getEntity(new GenericType<Map<String,String>>(){});

/*
        String jsonString = EntityUtils.toString(response.getEntity());
        log.warning(jsonString);
        JSONObject json = new JSONObject(jsonString);
        log.warning(json.toString());

        BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
        String line = "";
        StringBuilder json = new StringBuilder();
        while ((line = rd.readLine()) != null) {
            json.append(line);
        }

*/
        HttpEntity entity = response.getEntity();

        if (entity != null) {

            // A Simple JSON Response Read
            InputStream instream = entity.getContent();
            String result = convertStreamToString(instream);
            log.warning(result);
            ObjectMapper mapper = new ObjectMapper();
            Map<String,Object> json = mapper.readValue(result, new TypeReference<Map<String,Object>>() {
            });

            //JSONObject json = new JSONObject(result);
            log.warning(json.toString());
            instream.close();
            return Response.status(Response.Status.OK).entity(json).build();
        }else{
            return Response.status(Response.Status.BAD_REQUEST).entity("No response from CRUD").build();
        }
    }
    private String convertStreamToString(InputStream is) {

        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }
}
