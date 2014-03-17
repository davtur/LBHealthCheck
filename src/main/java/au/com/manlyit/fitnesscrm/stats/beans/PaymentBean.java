/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfCustomerDetailsTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import java.io.Serializable;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named
@Stateless
public class PaymentBean implements Serializable {

    private static final Logger logger = Logger.getLogger(PaymentBean.class.getName());

    @Asynchronous
    public Future<CustomerDetails> getCustomerDetails(Customers cust, String digitalKey) {

        CustomerDetails cd = null;
        if (cust == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }

        logger.log(Level.INFO, "Getting Customer Details {0}", cust.getUsername());
        INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = ws.getCustomerDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError().intValue() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

        }
        return new AsyncResult<>(cd);

    }
  @Asynchronous
    public Future<String> sendAsynchEmail(String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, Properties serverProperties, boolean debug) {
        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        try {
            emailAgent.send(to, ccAddress, from, emailSubject, message, theAttachedfileName, serverProperties, debug);
        } catch (Exception e) {
            String error = "Email Send Failed :" + e.getMessage();
            return new AsyncResult<>(error);
        }
        return new AsyncResult<>("Email Sent");
    }

}
