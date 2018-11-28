package tests;






import au.com.manlyit.fitnesscrm.stats.classes.util.RandomString;
import au.com.manlyit.fitnesscrm.stats.webservices.api.NewLeadRestService;
import au.com.manlyit.fitnesscrm.stats.webservices.client.ClientUtil;
import javax.ws.rs.client.Entity;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

public class CustomerRestServiceTest extends JerseyTest {
  private static final String AUTHTOKEN = "Green21Blue22";
    private static final String REST_URI = "https://test-services.manlybeachfemalefitness.com.au/FitnessStats/api/customers";
    @Override
    protected Application configure() {
        return new ResourceConfig(NewLeadRestService.class);
    }

    @Test
    public void customerRestServiceRawTest() {
        RandomString gen = new RandomString(5);
        String name = "fname"+ gen.nextString() + " sname"+ gen.nextString();
        String email = "fname"+ gen.nextString() + ".sname"+ gen.nextString() + "@manlyit.com.au";
        String newLead = ClientUtil.createCustomerInJSON(name, email, "0412422700","Test only",AUTHTOKEN);
        System.out.println(newLead);
      /*String response = target(REST_URI).request()
                  .post(Entity.entity(newLead, MediaType.APPLICATION_JSON),
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