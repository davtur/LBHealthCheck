/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.webservices;

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



@WebService(serviceName = "WordpressInterfaceWebService")
public class WordpressInterfaceWebService {

    private static final Logger LOGGER = Logger.getLogger(WordpressInterfaceWebService.class.getName());
@Resource
WebServiceContext context;
@Inject
 CustomersController cc ;


/* use this to test the web service - create an executable file with the below content and run from command line
 <?php
        $url = "https://test-services.purefitnessmanly.com.au/FitnessStats/WordpressInterfaceWebService?wsdl";
        $client = new SoapClient($url);
        $fcs = $client->__getFunctions();
        $res = $client->addNewLead(array('firstname'=> 'Dodgy', 'lastname' => 'Dave', 'email' => 'david@manlyit.com.au', 'mobile' => '0412422700', 'message' => 'Test From PHP'));
        echo "$res->result";
        ?>

*/


    /**
     * Web service operation
     * @param firstname
     * @param lastname
     * @param email
     * @param mobile
     * @param message
     * @return 
     */
    @WebMethod(operationName = "addNewLead")
    public String addNewLead(@WebParam(name = "firstname") String firstname, @WebParam(name = "lastname") String lastname, @WebParam(name = "email") String email, @WebParam(name = "mobile") String mobile, @WebParam(name = "message") String message) {
        //TODO write your implementation code here:
        String result = "OK";
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
        
        if (phoneNumber.startsWith("0")) {
            //Set the email pattern string
            p = Pattern.compile("\\d{10}");
            m = p.matcher(phoneNumber);
             matchFound = m.matches();
             if (matchFound && phoneNumber.charAt(1) != '0') {
                numberIsValid = true;
            }
        } else {
            p = Pattern.compile("\\d{11}");
            m = p.matcher(phoneNumber);
             matchFound = m.matches();
            if (matchFound && phoneNumber.startsWith("61")) {
                numberIsValid = true;
            }
        }
        //Match the given string with the pattern

        //Check whether match is found
        if (numberIsValid == false) {
            result = "ERROR: Phone number is not valid"; 
        }
        
        
        p = Pattern.compile("[ ](?=[ ])|[^-_,A-Za-z0-9 ]+");
        m = p.matcher(firstname);
             matchFound = m.matches();
            if (matchFound == false){
                 result = "ERROR: Firstname contains invalid characters"; 
            }
            
        p = Pattern.compile("[ ](?=[ ])|[^-_,A-Za-z0-9 ]+");
        m = p.matcher(lastname);
             matchFound = m.matches();
            if (matchFound == false){
                 result = "ERROR: Lastname contains invalid characters"; 
            }
            MessageContext msgCtxt = context.getMessageContext();
HttpServletRequest req = (HttpServletRequest)msgCtxt.get(MessageContext.SERVLET_REQUEST);

             
            //----if validation OK
           // ServletContext servletContext = (ServletContext)     context.getMessageContext().get(MessageContext.SERVLET_CONTEXT);
            
           // FacesContext context = FacesContext.getCurrentInstance();
           // CustomersController cc = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
           if(result.contains("OK")==true){
            Customers c = cc.setCustomerDefaults(new Customers());
            c.setFirstname(firstname);
            c.setLastname(lastname);
            c.setEmailAddress(email);
            c.setTelephone(phoneNumber);
          
            cc.createLeadFromWebservice(c,message,req);
            LOGGER.log(Level.INFO ,"Webservice call to add New Lead completed successfully:");
           }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "addNewLead Failed:",e);
            return "ERROR:" + e.getMessage();
        }
        
        return result;
    }
}
