/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
/*package au.com.manlyit.fitnesscrm.stats.webservices;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jws.WebService;
import javax.jws.WebMethod;
import javax.jws.WebParam;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

/**
 *
 * @author david
 */
/*@WebService(serviceName = "WordpressInterfaceWebService")
public class WordpressInterfaceWebService {

    private static final Logger LOGGER = Logger.getLogger(WordpressInterfaceWebService.class.getName());
    @Resource
    WebServiceContext context;
    @Inject
    CustomersController cc;*/


    /* use this to test the web service - create an executable file with the below content and run from command line
     <?php
     $url = "https://test-services.manlybeachfemalefitness.com.au/FitnessStats/WordpressInterfaceWebService?wsdl";
     $client = new SoapClient($url);
     $fcs = $client->__getFunctions();
     $res = $client->addNewLead(array('firstname'=> 'Dodgy', 'lastname' => 'Dave', 'email' => 'david@manlyit.com.au', 'mobile' => '0412422700', 'message' => 'Test From PHP'));
     echo "$res->result";
     ?>

     */
    /**
     * Web service operation
     *
     * @param firstname
     * @param lastname
     * @param email
     * @param mobile
     * @param message
     * @return
     */
 /*   @WebMethod(operationName = "addNewLead")
    public String addNewLead(@WebParam(name = "firstname") String firstname, @WebParam(name = "lastname") String lastname, @WebParam(name = "email") String email, @WebParam(name = "mobile") String mobile, @WebParam(name = "message") String message) {
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

            MessageContext msgCtxt = context.getMessageContext();
            HttpServletRequest req = (HttpServletRequest) msgCtxt.get(MessageContext.SERVLET_REQUEST);

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
    }
}*/
