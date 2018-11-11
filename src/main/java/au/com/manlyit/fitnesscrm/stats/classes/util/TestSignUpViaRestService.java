/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.webservices.NewLead;
import au.com.manlyit.fitnesscrm.stats.webservices.client.ClientUtil;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 *
 * @author david
 */
public class TestSignUpViaRestService {

    private static final String AUTHTOKEN = "Green21Blue22";
    private static final String REST_URI = "https://test-services.purefitnessmanly.com.au/FitnessStats/api/customers";
   
    public static void main(String args[]) throws Exception {

        RandomString gen = new RandomString(5);
        String name = "fname"+ gen.nextString() + " sname"+ gen.nextString();
        String email = "fname"+ gen.nextString() + ".sname"+ gen.nextString() + "@manlyit.com.au";
        NewLead nl = new NewLead();
        nl.setAuthToken(AUTHTOKEN);
        nl.setMessage("Test Only");
        nl.setName(name);
        nl.setEmailAddress(email);
        nl.setPhoneNumber("0412422700");
       
        System.out.println(nl);
        String targetUrl = "";

        

        Response response = createJsonNewLead(nl);
        System.out.println(response);

        System.out.println("Sucessfully completed TestSignUpViaRestService");
    }
    public static NewLead getJsonNewLEad(int id) {
        Client client = ClientBuilder.newClient();
        return client
          .target(REST_URI)
          .path(String.valueOf(id))
          .request(MediaType.APPLICATION_JSON)
          .get(NewLead.class);
    }
    
    public static Response createJsonNewLead(NewLead emp) {
        Client client = ClientBuilder.newClient();
    return client
      .target(REST_URI)
      .request(MediaType.APPLICATION_JSON)
      .post(Entity.entity(emp, MediaType.APPLICATION_JSON));
}
}
