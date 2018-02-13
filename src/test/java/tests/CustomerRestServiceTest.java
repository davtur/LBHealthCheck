package tests;





import au.com.manlyit.fitnesscrm.stats.webservices.NewLead;
import au.com.manlyit.fitnesscrm.stats.webservices.api.NewLeadRestService;
import au.com.manlyit.fitnesscrm.stats.webservices.client.ClientUtil;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

public class CustomerRestServiceTest extends JerseyTest {
private static final String AUTHTOKEN = "Green21Blue22";
    @Override
    protected Application configure() {
        return new ResourceConfig(NewLeadRestService.class);
    }

    @Test
    public void customerRestServiceRawTest() {
        String joe = ClientUtil.createCustomerInJSON("Joe", "dodgy@manlyit.com.au", "555-456-9877","Test only",AUTHTOKEN);
      /*  String response = target("customers").request()
                  .post(Entity.entity(joe, MediaType.APPLICATION_JSON),
                        String.class);
        System.out.println(response);*/
    }
 
    
    @Test
    public void customerRestServiceTest() {
     /*   NewLead newCustomer = new NewLead();
        newCustomer.setName("Jake Mae");
        newCustomer.setEmailAddress("dodgy@manlyit.com.au");
        newCustomer.setPhoneNumber("0412422700");
        newCustomer.setMessage("Test only");
        newCustomer.setAuthToken(AUTHTOKEN);
        String response = target("customers")
                  .request(MediaType.APPLICATION_JSON)
                  .accept(MediaType.TEXT_PLAIN_TYPE)
                  //this time we are calling post to make a HTTP POST call
                  .post(Entity.json(newCustomer), String.class);


        System.out.println(response);*/
    }
}