/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.webservices.*;
import static java.lang.Thread.sleep;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.xml.bind.JAXBElement;

/**
 *
 * @author david
 */
public class TestEziDebitModule {

    private static final Logger logger = Logger.getLogger(TestEziDebitModule.class.getName());
    private static final String digitalKey = "78F14D92-76F1-45B0-815B-C3F0F239F624";// test

    private boolean addCustomer(Customers cust, String loggedInUser) {
        boolean result = false;
        logger.log(Level.INFO, "Starting tests!");
        INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        PaymentParameters payParams = cust.getPaymentParametersId();

        String addresssLine2 = "";

        
        if (payParams == null) {
            logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found ( null )");
            return false;
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = ws.addCustomer(digitalKey, cust.getId().toString(), cust.getUsername(), cust.getLastname(), cust.getLastname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), sdf.format(payParams.getContractStartDate()), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), loggedInUser);
        logger.log(Level.INFO, "Add Customer Response: Error - {0}, Data - {1}", new Object[]{addCustomerResponse.getErrorMessage().getValue(), addCustomerResponse.getData().getValue()});

        return result;
    }

    public static void main(String args[]) throws Exception {
        logger.log(Level.INFO, "Starting tests!");
        INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");

        String paymentType = "ALL";//PENDING,FAILED,SUCCESSFUL
        String paymentMethod = "ALL";//CR,DR
        String paymentSource = "ALL";// SCHEDULED,WEB,PHONE,BPAY
        String paymentDate = sdf.format(new Date());
        String paymentReference = "2013%"; //optional - blank for all
        String dateFrom = "2013-06-01";//optional - blank for all
        String dateTo = "2014-02-02";//optional  - blank for all
        String dateField = "PAYMENT";// SETTLEMENT
        String eziDebitCustomerId = "1";
        String primaryKey = "1";
        String yourSystemReference = "";// 50 chars max

        String customerName = "Sarah Turner";

        String result = ws.testFunction("JUST A TEST --------------------> ").getData().getValue();
        logger.log(Level.INFO, "Result: {0}", result);

        EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = ws.addCustomer(digitalKey, primaryKey, yourSystemReference, "Turner", "Sarah", "Unit 21", "5-17 High St", "Manly", "NSW", "2095", "sarah@purefitnessmanly.com.au", "0433818067", paymentDate, "NO", "YES", "YES", "LocalUser");
        logger.log(Level.INFO, "Add Customer Response: Error - {0}, Data - {1}", new Object[]{addCustomerResponse.getErrorMessage().getValue(), addCustomerResponse.getData().getValue()});
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = ws.getCustomerDetails(digitalKey, "", "1");
        logger.log(Level.INFO, "Get Customer Response: Error - {0}, Data - {1}", new Object[]{customerdetails.getErrorMessage().getValue(), customerdetails.getData().getValue().getCustomerName().getValue()});

        EziResponseOfstring paymentResponse = ws.addPayment(digitalKey, eziDebitCustomerId, yourSystemReference, paymentDate, new Long(2000), paymentReference, "JoeBloggs");
        logger.log(Level.INFO, "Payment Response: Error - {0}, Data - {1}", new Object[]{paymentResponse.getErrorMessage().getValue(), paymentResponse.getData().getValue()});

        JAXBElement<ArrayOfPayment> aop = ws.getPayments(digitalKey, paymentType, paymentMethod, paymentSource, "%", "", "", dateField, "", "").getData();

        if (aop.isNil() == false) {
            List<Payment> lop = aop.getValue().getPayment();

            for (Payment p : lop) {
                logger.log(Level.INFO, "Payment: {0}", p.getScheduledAmount().toString());
            }
        } else {
            logger.log(Level.WARNING, "Array of Payment is null");
        }
        TimeUnit.MILLISECONDS.sleep(1000);;
    }
    
}
