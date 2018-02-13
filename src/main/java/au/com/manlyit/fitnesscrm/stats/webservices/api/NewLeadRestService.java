package au.com.manlyit.fitnesscrm.stats.webservices.api;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.webservices.NewLead;
import au.com.manlyit.fitnesscrm.stats.webservices.NewLeadDataService;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Context;

@Path("customers")
@Stateless
public class NewLeadRestService { 

    @Inject
    private CustomersController cc;
    private NewLeadDataService dataService = NewLeadDataService.getInstance();
    @Context
    private HttpServletRequest req;

    @GET
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public List<NewLead> getCustomers() {
        return dataService.getCustomerList();
    }

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public String createCustomer(NewLead newCustomer) {
        return dataService.addCustomer(newCustomer, req,cc);
    }

    @GET
    @Path("{id}")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public NewLead getCustomer(@PathParam("id") String id) {
        return dataService.getCustomerById(id);
    }
}
