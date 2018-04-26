package au.com.manlyit.fitnesscrm.stats.webservices;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.NotAuthorizedException;


public class NewLeadDataService {

    private List<NewLead> customerList = new ArrayList<>();
    private static final Logger LOGGER = Logger.getLogger(NewLeadDataService.class.getName());
    private static final String AUTHTOKEN = "Green21Blue22";
    //@Resource
    // WebServiceContext context;
    // @Context
    // private HttpServletRequest req;
    // @Context MessageContext jaxrsContext;
   
    private static NewLeadDataService ourInstance = new NewLeadDataService();

    public static NewLeadDataService getInstance() {
        return ourInstance;
    }

    public String addCustomer(NewLead customer, HttpServletRequest req, CustomersController cc) {
        //FacesContext context = (FacesContext) req;

        //CustomersController cc = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");

        //String newId = Integer.toString(customerList.size() + 1);
        // customer.setId(newId);
        // customerList.add(customer);
        if (checkAuthorisation(customer.getAuthToken()) == false) {
            throw new NotAuthorizedException("Token");
        }
        String authToken = customer.getAuthToken();
        String firstname = customer.getName();
        String[] name = customer.getName().split(" ", 2);
        if (name.length > 0) {
            firstname = name[0];
        }
        String lastname = "";
        if (name.length > 1) {
            lastname = name[1];
        }
        String email = customer.getEmailAddress();
        String mobile = customer.getPhoneNumber();
        String message = customer.getMessage();

        //TODO write your implementation code here:
        String result = "OK";
        if (firstname == null) {
            firstname = " ";

        } else if (firstname.trim().isEmpty()) {
            firstname = " ";
        }
        if (lastname == null) {
            lastname = " ";

        } else if (lastname.trim().isEmpty()) {
            lastname = " ";
        }
        if (email == null) {
            email = " ";

        }
        if (mobile == null) {
            mobile = "";

        }
        if (message == null) {
            message = "No Message";

        } else if (message.trim().isEmpty()) {
            message = "No Message";
        }
        LOGGER.log(Level.INFO, "ADD New LEAD Webservice Called: Name: {0} {1}, email: {2}, Phone: {3}, Message: {4}", new Object[]{firstname, lastname, email, mobile, message});
        if (mobile.trim().isEmpty() && email.trim().isEmpty()) {
            result = "ERROR: No email or phone details sent - no way to contact the lead - Fail.";
            LOGGER.log(Level.WARNING, "addNewLead Failed:{0}", result);
            return result;
        }
        if (mobile.trim().isEmpty()) {
            mobile = "0400000000";
        }
        if (email.trim().isEmpty()) {
            email = "no.email.given@noreply.com";
        }
        try {
            // validate input

            //Set the email pattern string
            Pattern p = Pattern.compile(".+@.+\\.[a-z]+");
            //Match the given string with the pattern
            Matcher m = p.matcher(email);
            //Check whether match is found
            boolean matchFound = m.matches();
            if (!matchFound) {
                result = "ERROR: Email address is not valid";
            }

            // validate phone number
            boolean numberIsValid = false;
            String phoneNumber = mobile;
            phoneNumber = phoneNumber.replaceAll("[^\\d.]", "");

            // replace any non word characters -  A word character: [a-zA-Z_0-9]
            firstname = firstname.replaceAll("\\W", " ");
            lastname = lastname.replaceAll("\\W", " ");
            if (firstname.trim().isEmpty()) {
                firstname = " ";
            }
            if (lastname.trim().isEmpty()) {
                lastname = " ";
            }

            //MessageContext msgCtxt = context.getMessageContext();
            //HttpServletRequest req = (HttpServletRequest) msgCtxt.get(MessageContext.SERVLET_REQUEST);
            if (result.contains("OK") == true) {
                Customers c = cc.setCustomerDefaults(new Customers());
                c.setFirstname(firstname.trim());
                c.setLastname(lastname.trim());
                c.setEmailAddress(email.trim());
                c.setTelephone(phoneNumber.trim());
   
                cc.createLeadFromWebservice(c, message, req);
                cc.updateASingleCustomersPaymentInfo(c);
                LOGGER.log(Level.INFO, "Webservice call to add New Lead completed successfully:");
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "addNewLead Failed:", e);
            return "ERROR:" + e.getMessage();
        }

        return result;

        //return newId;
    }

    public List<NewLead> getCustomerList() {
        return customerList;
    }

    private boolean checkAuthorisation(String token) {
        boolean result = false;

        if (token.contentEquals(AUTHTOKEN)) {
            return true;
        }

        return result;
    }

    public NewLead getCustomerById(String id) {
        for (NewLead customer : customerList) {
            if (customer.getId().equals(id)) {
                return customer;
            }
        }

        return null;
    }

}
