/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfArrayOfPaymentTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfArrayOfScheduledPaymentTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfCustomerDetailsTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfNewCustomerXcXH3LiW;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfPaymentDetailTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfstring;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.AsyncResult;
import javax.ejb.Asynchronous;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.inject.Named;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author david
 */
@Named
@Stateless
public class PaymentBean implements Serializable {

    private static final Logger logger = Logger.getLogger(PaymentBean.class.getName());
    private static final String paymentGateway = "EZIDEBIT";
    
    @Inject
    private CustomersFacade customersFacade;
    @Inject ConfigMapFacade configMapFacade;
    
    private INonPCIService getWs(){
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        return new NonPCIService(url).getBasicHttpBindingINonPCIService();
    }


      @Asynchronous
    public Future<CustomerDetails> getCustomerDetails(Customers cust, String digitalKey) {

        CustomerDetails cd = null;
        if (cust == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }

        logger.log(Level.INFO, "Running async task - Getting Customer Details {0}", cust.getUsername());
        
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

        }
        return new AsyncResult<>(cd);

    }

    @Asynchronous
    public Future<CustomerDetails> getCustomerDetailsFromEziDebitId(String eziDebitId, String digitalKey) {

        CustomerDetails cd = null;
        if (eziDebitId == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (eziDebitId.trim().isEmpty() || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }
        logger.log(Level.INFO, "Getting Customer (EziId) Details {0}", eziDebitId);
        
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(digitalKey, eziDebitId, "");
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer (EziId) Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer (EziId) Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
        }
        return new AsyncResult<>(cd);
    }
    
  /*  
    pci compliant version only
    
    @Asynchronous
    public Future<CustomerDetails> getCustomerAccountDetails(Customers cust, String digitalKey) {

        CustomerDetails cd = null;
        if (cust == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (cust.getId() == null || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }

        logger.log(Level.INFO, "Running async task - Getting Customer Account Details {0}", cust.getUsername());
        
        EziResponseOfCustomerAccountDetails customerdetails = getWs().getCustomerAccountDetails(digitalKey, "", cust.getId().toString());
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

        }
        return new AsyncResult<>(cd);

    }

    @Asynchronous
    public Future<CustomerDetails> getCustomerAccountDetailsFromEziDebitId(String eziDebitId, String digitalKey) {

        CustomerDetails cd = null;
        if (eziDebitId == null || digitalKey == null) {
            return new AsyncResult<>(cd);
        }
        if (eziDebitId.trim().isEmpty() || digitalKey.trim().isEmpty()) {
            return new AsyncResult<>(cd);
        }
        logger.log(Level.INFO, "Getting Customer (EziId) Account Details {0}", eziDebitId);
        
        EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerAccountDetails(digitalKey, eziDebitId, "");
        if (customerdetails.getError() == 0) {// any errors will be a non zero value
            logger.log(Level.INFO, "Get Customer (EziId) Account Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

            cd = customerdetails.getData().getValue();

        } else {
            logger.log(Level.WARNING, "Get Customer (EziId) Account Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
        }
        return new AsyncResult<>(cd);
    }*/

    @Asynchronous
    public Future<Boolean> editCustomerDetails(Customers cust, String eziDebitRef, String digitalKey) {
        String ourSystemRef = "";

        if (eziDebitRef == null) {
            eziDebitRef = "";
            ourSystemRef = cust.getId().toString();
        }
        boolean result = false;
        logger.log(Level.INFO, "Editing Customer  {0}", cust.getUsername());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Collection<PaymentParameters> pay = cust.getPaymentParametersCollection();
        PaymentParameters payParams = null;
        String addresssLine2 = ""; // not used
        String humanFriendlyReference = cust.getId() +" "+cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default

        if (pay != null) {
            for (PaymentParameters pp : pay) {
                if (pp.getPaymentGatewayName().compareTo(paymentGateway) == 0) {
                    payParams = pp;
                } else {

                }

            }
        } else {
            logger.log(Level.WARNING, "Payment Parameters are null");
            return new AsyncResult<>(false);
        }
        if (payParams == null) {
            logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
            return new AsyncResult<>(false);
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(digitalKey, eziDebitRef, ourSystemRef, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), cust.getTelephone(), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());
        logger.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
        if (editCustomerDetail.getError() == 0) {// any errors will be a non zero value
            result = true;
        } else {
            logger.log(Level.WARNING, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> clearSchedule(Customers cust, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // This method will remove payments that exist in the payment schedule for the given
        // customer. You can control whether all payments are deleted, or if you wish to preserve
        // any manually added payments, and delete an ongoing cyclic schedule.

        boolean result = false;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.

        String ourSystemCustomerReference = cust.getId().toString();
        
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "clearSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().clearSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                return new AsyncResult<>(true);
            } else {
                logger.log(Level.WARNING, "clearSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> deletePayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser, String digitalKey) {
        	//  This method will delete a single payment from the Customer's payment schedule.
        //  It is important to note the following when deleting a payment:

        //  Only scheduled payments with a PaymentStatus of 'W' can be deleted;
        //  A specified PaymentReference value will override the debit date and payment
        //  amount when identifying the payment to be deleted;
        //  Values for both DebitDate and PaymentAmountInCents must be provided in order
        //  to identify a payment;
        //  If you provide values for DebitDate and PaymentAmountInCents and there is more
        //  than one payment for PaymentAmountInCents scheduled on DebitDate, then only
        //  one of the payments will be deleted.
        boolean result = false;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String debitDateString = sdf.format(debitDate);
        String ourSystemCustomerReference = cust.getId().toString();
        

        if ((paymentReference != null && paymentReference.trim().isEmpty() == false) && paymentAmountInCents.compareTo(new Long(0)) > 0) {
            paymentAmountInCents = new Long(0);
            logger.log(Level.WARNING, "deletePayment paymentReference is not NULL and paymentAmount is also set.It should be 0 if using payment reference. Setting Amount to zero and using paymentReference to identify the payment");
        }
        if (paymentReference == null) {
            paymentReference = "";
        }
        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "deletePayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "deletePayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().deletePayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, paymentReference, debitDateString, paymentAmountInCents, loggedInUser);
        logger.log(Level.INFO, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> changeCustomerStatus(Customers cust, String newStatus, String loggedInUser, String digitalKey) {
        // note: cancelled status cannot be changed with this method. i.e. cancelled is final like deleted.
        boolean result = false;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        String ourSystemCustomerReference = cust.getId().toString();
        
        if (newStatus.compareTo("A") == 0 || newStatus.compareTo("H") == 0 || newStatus.compareTo("C") == 0) {
            if (loggedInUser.length() > 50) {
                loggedInUser = loggedInUser.substring(0, 50);
                logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
            }
            EziResponseOfstring eziResponse = null;
            try {
                eziResponse = getWs().changeCustomerStatus(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, newStatus, loggedInUser);
            } catch (Exception e) {
                logger.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", e);
                return new AsyncResult<>(result);
            }
            logger.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value
                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = true;
                    logger.log(Level.INFO, "changeCustomerStatus  Successful  : ErrorCode - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

                } else {
                    logger.log(Level.WARNING, "changeCustomerStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }

            } else {
                logger.log(Level.WARNING, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeCustomerStatus: newStatus should be A ( Active ), H ( Hold ) or C (Cancelled)  : Error - {0}", newStatus);
        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public synchronized Future<ArrayOfScheduledPayment> getScheduledPayments(Customers cust, Date fromDate, Date toDate, String digitalKey) {

        ArrayOfScheduledPayment result = new ArrayOfScheduledPayment();
        if (cust == null) {
            logger.log(Level.WARNING, "getScheduledPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            return new AsyncResult<>(result);
        }
        logger.log(Level.INFO, "Running asychronous task getScheduledPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        if (fromDate != null) {
            fromDateString = sdf.format(fromDate);
        }
        String toDateString = "";
        if (toDate != null) {
            toDateString = sdf.format(toDate);
        }

        String ourSystemCustomerReference = cust.getId().toString();

        

        EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = getWs().getScheduledPayments(digitalKey, fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
        logger.log(Level.INFO, "getScheduledPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getScheduledPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> addCustomer(Customers cust, String paymentGatewayName, String digitalKey) {
        boolean result = false;
        logger.log(Level.INFO, "Adding Customer  {0}", cust.getUsername());
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        Collection<PaymentParameters> pay = cust.getPaymentParametersCollection();
        if (pay == null) {
            logger.log(Level.WARNING, "Payment Parameters are null");
            return new AsyncResult<>(false);
        }
        PaymentParameters payParams = null;
        String addresssLine2 = ""; // not used
        String humanFriendlyReference = cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default
        if (pay.isEmpty() && paymentGatewayName.toUpperCase().contains(paymentGateway)) {
            payParams = new PaymentParameters(0, new Date(), cust.getTelephone(), "NO", "NO", "NO", paymentGateway);
            //Customers loggedInUser = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
            payParams.setLoggedInUser(cust);

            cust.getPaymentParametersCollection().add(payParams);
            customersFacade.editAndFlush(cust);
        } else {
            for (PaymentParameters pp : pay) {
                if (pp.getPaymentGatewayName().compareTo(paymentGateway) == 0) {
                    payParams = pp;
                }
            }
        }

        if (payParams == null) {
            logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
            return new AsyncResult<>(false);
        }

// note - NB - for Australian Customers the
//mobile phone number must be 10
//digits long and begin with '04'. For
//New Zealand Customers the mobile
//phone number must be 10 digits
//long and begin with '02'
        EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = getWs().addCustomer(digitalKey, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), sdf.format(payParams.getContractStartDate()), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());

        if (addCustomerResponse.getError() == 0) {// any errors will be a non zero value
            result = true;
            logger.log(Level.INFO, "Add Customer Response: Error - {0}, Data - {1}", new Object[]{addCustomerResponse.getErrorMessage().getValue(), addCustomerResponse.getData().getValue()});

        } else {
            logger.log(Level.WARNING, "Add Customer Response: Error - {0},", addCustomerResponse.getErrorMessage().getValue());

        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentDetail> getPaymentDetail(String paymentReference, String digitalKey) {
        //  This method retrieves details about the given payment. It can only be used to retrieve
        //  information about payments where Ezidebit was provided with a PaymentReference.
        //  It is important to note the following when querying payment details:
        //  
        //  This method can be used to retrieve information about payments that have been
        //  scheduled by you. It cannot access information about real-time payments. Other
        //  methods are provided for retrieving real-time payment information.
        PaymentDetail result = null;
        

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentDetail paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentDetail paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailTHgMB7OL eziResponse = getWs().getPaymentDetail(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentDetail Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getPaymentDetail Response: Error - {0}", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> getPaymentStatus(String paymentReference, String digitalKey) {
        //  Description
        //  This method allows you to retrieve the status of a particular payment from the direct
        //  debit system where a PaymentReference has been provided.
        //  It is important to note the following when querying Payment details:
        //  • This method cannot be used to retrieve the status of BPAY or real-time credit card
        //  payments;
        //  • This method will only return the status of one payment at a time;
        //  • To use this method, you must have provided a PaymentReference when adding
        //  the payment to the Customer's schedule.
        //  
        //  Response
        //  The <Data> field in the GetPaymentStatus response will contain either:
        //  • A status of which the possible values are:
        //  - 'W' (waiting) - Payment is scheduled waiting to be sent to the bank;
        //  - 'P' (pending) - Payment request has been sent to the bank for processing
        //  and Ezidebit is waiting on a success or fail response before completing
        //  settlement;
        //  - 'S' (successful) - Payment has been successfully debited from the
        //  Customer and deposited to the client's settlement bank account;
        //  - 'D' (dishonoured) - Payment has been dishonoured by the Customer's
        //  financial institution due to insufficient funds;
        //  - 'F' (fatal dishonour) - Payment has been dishonoured by the Customer's
        //  financial institution;
        boolean result = false;
        

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentStatus paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentStatus paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfstring eziResponse = getWs().getPaymentStatus(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "getPaymentStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<PaymentDetailPlusNextPaymentInfo> getPaymentDetailPlusNextPaymentInfo(String paymentReference, String digitalKey) {
        //  This method retrieves details about the given payment. It can only be used to retrieve
        //  information about payments where Ezidebit was provided with a PaymentReference.
        //  It is important to note the following when querying payment details:
        //  
        //  This method can be used to retrieve information about payments that have been
        //  scheduled by you. It cannot access information about real-time payments. Other
        //  methods are provided for retrieving real-time payment information.
        PaymentDetailPlusNextPaymentInfo result = null;
        

        if (paymentReference == null) {

            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is required but it is NULL");
            return new AsyncResult<>(result);
        }

        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL eziResponse = getWs().getPaymentDetailPlusNextPaymentInfo(digitalKey, paymentReference);
        logger.log(Level.INFO, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public synchronized Future<ArrayOfPayment> getPayments(Customers cust, String paymentType, String paymentMethod, String paymentSource, String paymentReference, Date fromDate, Date toDate, boolean useSettlementDate, String digitalKey) {
        //  Description
        //  	  
        //  This method allows you to retrieve payment information from across Ezidebit's various
        //  payment systems. It provides you with access to scheduled, pending and completed
        //  payments made through all payment channels.
        //  It is important to note the following when querying Payment details:
        //  • This method can be used to retrieve information about payments that have been
        //  made by your Customer through any means;
        //  • This is the recommended method for retrieving a set of payment results in a
        //  single call as most other methods are designed to provide detail about a single
        //  transaction. This method will return a full set of transactions matching the
        //  supplied criteria;
        //  • The flexibility of using a wildcard in the PaymentReference search value means
        //  that if you are adding payments with the AddPayment method as they become
        //  due, you can provide structured PaymentReferences that will allow you to group
        //  or batch payments in a way that you see fit;
        //  • Ezidebit only processes settlement deposits to clients once a day. Since the
        //  "SUCCESS" criteria for a scheduled direct debit payment is set when the payment
        //  is deposited to the client, the combination of
        //  PaymentType=ALL
        //  DateField=SETTLEMENT
        //  DateFrom={LastSuccessfulPaymentDate + 1}
        //  DateTo={currentDate}
        //  will provide you with all payments that have been made to the client since the
        //  last time your system received payment information.

        ArrayOfPayment result = new ArrayOfPayment();
        if (cust == null) {
            logger.log(Level.WARNING, "getPayments: The customer object passed to this method is NULL.This parameter is required.Returning empty array!!");
            return new AsyncResult<>(result);
        }
        logger.log(Level.INFO, "Running asychronous task getPayments Customer {0}, From Date {1}, to Date {2}", new Object[]{cust, fromDate, toDate});
        if (paymentType.compareTo("ALL") != 0 && paymentType.compareTo("PENDING") != 0 && paymentType.compareTo("FAILED") != 0 && paymentType.compareTo("SUCCESSFUL") != 0) {
            logger.log(Level.WARNING, "getPayments: payment Type is required and should be either ALL,PENDING,FAILED,SUCCESSFUL.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentMethod.compareTo("ALL") != 0 && paymentMethod.compareTo("CR") != 0 && paymentMethod.compareTo("DR") != 0) {
            logger.log(Level.WARNING, "getPayments: payment Method is required and should be either ALL,CR,DR.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentSource.compareTo("ALL") != 0 && paymentSource.compareTo("SCHEDULED") != 0 && paymentSource.compareTo("WEB") != 0 && paymentSource.compareTo("PHONE") != 0 && paymentSource.compareTo("BPAY") != 0) {
            logger.log(Level.WARNING, "getPayments: paymentSource is required and should be either ALL,SCHEDULED,WEB,PHONE,BPAY.  Returning null as this parameter is required.");
            return new AsyncResult<>(result);
        }
        if (paymentReference == null) {
            paymentReference = "";
        }
        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "getPayments paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }

        String dateField = "PAYMENT";
        if (useSettlementDate == true) {
            dateField = "SETTLEMENT";
        }
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        if (fromDate != null) {
            fromDateString = sdf.format(fromDate);
        }
        String toDateString = "";
        if (toDate != null) {
            toDateString = sdf.format(toDate);
        }

        String ourSystemCustomerReference = cust.getId().toString();

        

        EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(digitalKey, paymentType, paymentMethod, paymentSource, paymentReference, fromDateString, toDateString, dateField, eziDebitCustomerId, ourSystemCustomerReference);
        logger.log(Level.INFO, "getPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();

        } else {
            logger.log(Level.WARNING, "getPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> addPayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser, String digitalKey) {
        // paymentReference Max 50 chars. It can be search with with a wildcard in other methods. Use invoice number or other payment identifier
        logger.log(Level.INFO, "running asychronous task addPayment Customer {0}, debitDate {1}, paymentAmountInCents {2}", new Object[]{cust, debitDate, paymentAmountInCents});
        boolean result = false;
        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String debitDateString = sdf.format(debitDate);
        String ourSystemCustomerReference = cust.getId().toString();
        
        if (paymentReference.length() > 50) {
            paymentReference = paymentReference.substring(0, 50);
            logger.log(Level.WARNING, "addPayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse;
        try {
            eziResponse = getWs().addPayment(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, debitDateString, paymentAmountInCents, paymentReference, loggedInUser);
        } catch (Exception e) {
            logger.log(Level.WARNING, "addPayment Method Response: Exception - {0}", e);
            return new AsyncResult<>(false);
        }
        logger.log(Level.INFO, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "addPayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> createSchedule(Customers cust, Date scheduleStartDate, char schedulePeriodType, String dayOfWeek, int dayOfMonth, boolean firstWeekOfMonth, boolean secondWeekOfMonth, boolean thirdWeekOfMonth, boolean fourthWeekOfMonth, long paymentAmountInCents, int limitToNumberOfPayments, long limitToTotalAmountInCent, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        //This method allows you to create a new schedule for the given Customer. It will create a
        //schedule for on-going debits (up to one year’s worth of payments will exist at a point in
        //time), or will calculate a schedule to fulfil a required total payment amount, or number of
        //payments.
        //It is important to note the following when creating a payment schedule:
        //• This function will first remove an existing payment schedule and then create a
        //new payment schedule in accordance with your parameters;
        //• You can choose whether to maintain or delete any payments that were manually
        //added to the payment schedule with the AddPayment method by specifying "YES"
        //or "NO" respectively in the KeepManualPayments parameter;
        //• When creating a new schedule for a fixed amount or fixed number of payments,
        //the calculation will not consider any payments already made by the Customer;
        //• When a schedule is created, a series of individual payment records are created in
        //the Ezidebit system, which can be altered independently of each other;
        //• For on-going Customers, when a debit is processed (or removed from the
        //schedule for Customers in a non-processing status), a new debit is added to the
        //end of their existing schedules, at the frequency specified when the schedule was
        //created;
        //• For Customers on a fixed number of payments, or total amount owing, if a
        //payment is unsuccessful, it will cause a new debit to be scheduled at the end of
        //the existing schedule for the correct amount, at the frequency specified when the
        //schedule was created;
        //• Ezidebit will not schedule payments for weekend days, instead updating the
        //scheduled debit record to reflect the fact that it will be drawn from the
        //Customer's payment method on the next business-banking day.
        boolean result = false;
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }
        String firstWeekOfMonthString = "NO";
        if (firstWeekOfMonth) {
            firstWeekOfMonthString = "YES";
        }
        String secondWeekOfMonthString = "NO";
        if (secondWeekOfMonth) {
            secondWeekOfMonthString = "YES";
        }
        String thirdWeekOfMonthString = "NO";
        if (thirdWeekOfMonth) {
            thirdWeekOfMonthString = "YES";
        }
        String fourthWeekOfMonthString = "NO";
        if (fourthWeekOfMonth) {
            fourthWeekOfMonthString = "YES";
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String scheduleStartDateString = sdf.format(scheduleStartDate);
        String ourSystemCustomerReference = cust.getId().toString();
        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'N' || schedulePeriodType == '4') {
            if (dayOfWeek == null || dayOfWeek.trim().isEmpty()) {
                logger.log(Level.WARNING, "createSchedule A value must be provided for dayOfWeek \n" + " when the\n" + "SchedulePeriodType is in\n" + "W,F,4,N");
                return new AsyncResult<>(false);
            }
        }
        if (schedulePeriodType == 'M') {
            if (dayOfMonth < 1 || dayOfMonth > 31) {
                logger.log(Level.WARNING, "createSchedule: A value must be provided for dayOfMonth (1..31 )\n" + " when the\n" + "SchedulePeriodType is in\n" + "M");
                return new AsyncResult<>(false);
            }
        } else {
            dayOfMonth = 0;
        }
        String schedulePeriodTypeString = "";
        schedulePeriodTypeString = schedulePeriodTypeString + schedulePeriodType;
        

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "createSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        if (schedulePeriodType == 'W' || schedulePeriodType == 'F' || schedulePeriodType == 'M' || schedulePeriodType == '4' || schedulePeriodType == 'N' || schedulePeriodType == 'Q' || schedulePeriodType == 'H' || schedulePeriodType == 'Y') {

            EziResponseOfstring eziResponse = getWs().createSchedule(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, scheduleStartDateString, schedulePeriodTypeString, dayOfWeek, dayOfMonth, firstWeekOfMonthString, secondWeekOfMonthString, thirdWeekOfMonthString, fourthWeekOfMonthString, paymentAmountInCents, limitToNumberOfPayments, limitToTotalAmountInCent, keepManualPaymentsString, loggedInUser);
            logger.log(Level.INFO, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            if (eziResponse.getError() == 0) {// any errors will be a non zero value

                if (eziResponse.getData().getValue().compareTo("S") == 0) {
                    result = true;
                } else {
                    logger.log(Level.WARNING, "createSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
                }
            } else {
                logger.log(Level.WARNING, "createSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

            }
        } else {
            logger.log(Level.WARNING, "createSchedule : schedulePeriodType Possible values are:\\n'W' - Weekly\\n'F' - Fortnightly\\n'M' - Monthly\\n'4' - 4 Weekly\\n'N' - Weekday in month (e.g.\\nMonday in the third week of\\nevery month)\\n'Q' - Quarterly\\n'H' - Half Yearly (6 Monthly)\\n'Y' - Yearly\\nThe frequency is applied to the\\npayment scheduled beginning\\nfrom the date defined in\\nScheduledStartDate\\n Incorect value that was submitted = ", schedulePeriodType);

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> changeScheduledAmount(Customers cust, Date changeFromDate, Long newPaymentAmountInCents, int changeFromPaymentNumber, boolean applyToAllFuturePayments, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // Only scheduled payments with a status of 'W' can have their scheduled debit amounts changed;
        // This method will only change the debit amounts of scheduled payments - the debit dates will still remain the same.

        boolean result = false;
        String applyToAllFuturePaymentsString = "NO";// apply to earliest payment
        if (applyToAllFuturePayments) {
            applyToAllFuturePaymentsString = "YES"; //applied to all (YES) payments that occur on or after the position identified by the ChangeFromPaymentNumber or ChangeFromPaymentDate
        }
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String changeFromDateString = sdf.format(changeFromDate);
        String ourSystemCustomerReference = cust.getId().toString();
        

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "changeScheduledAmount loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledAmount(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromPaymentNumber, changeFromDateString, newPaymentAmountInCents, applyToAllFuturePaymentsString, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "changeScheduledAmount Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeScheduledAmount Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> changeScheduledDate(Customers cust, Date changeFromDate, Date changeToDate, String paymentReference, boolean keepManualPayments, String loggedInUser, String digitalKey) {
        // PaymentReference - If you used a specific PaymentReference when adding a payment using the AddPayment Method, then you can use that value here to exactly identify that payment within the Customer's schedule.
        // NB - You must provide a value for either ChangeFromDate or PaymentReference to identify the payment.
        boolean result = false;
        if (paymentReference.trim().isEmpty()) {
            paymentReference = null;
        }
        String keepManualPaymentsString = "NO";// update all specified payments for customer 
        if (keepManualPayments) {
            keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
        }

        String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        String changeFromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
        String changeToDateString = sdf.format(changeToDate); // The new date that you wish for this payment to be deducted from your Customer's bank account or credit card.
        String ourSystemCustomerReference = cust.getId().toString();

        if (changeFromDate != null && paymentReference == null) {
            changeFromDateString = sdf.format(changeFromDate);
            paymentReference = "";
        }

        

        if (loggedInUser.length() > 50) {
            loggedInUser = loggedInUser.substring(0, 50);
            logger.log(Level.WARNING, "changeScheduledDate loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
        }
        EziResponseOfstring eziResponse = getWs().changeScheduledDate(digitalKey, eziDebitCustomerId, ourSystemCustomerReference, changeFromDateString, paymentReference, changeToDateString, keepManualPaymentsString, loggedInUser);
        logger.log(Level.INFO, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value

            if (eziResponse.getData().getValue().compareTo("S") == 0) {
                result = true;
            } else {
                logger.log(Level.WARNING, "changeScheduledDate Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
            }
        } else {
            logger.log(Level.WARNING, "changeScheduledDate Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> isBsbValid(String bsb, String digitalKey) {
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        

        EziResponseOfstring eziResponse = getWs().isBsbValid(digitalKey, bsb);
        logger.log(Level.INFO, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                return new AsyncResult<>(true);
            }

        } else {
            logger.log(Level.WARNING, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> isSystemLocked(String digitalKey) {
        //  Description
        //  check that the BSB is valid
        boolean result = false;

        

        EziResponseOfstring eziResponse = getWs().isSystemLocked(digitalKey);
        logger.log(Level.INFO, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            String valid = eziResponse.getData().getValue();
            if (valid.compareTo("YES") == 0) {
                return new AsyncResult<>(true);
            }

        } else {
            logger.log(Level.WARNING, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

        }

        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<String> getPaymentExchangeVersion() {
        //  Description
        //  
        //  This method returns the version of our web services and API that you are connecting to.
        //  This can be used as a check to ensure that you're connecting to the web services that
        //  you expect to, based on the API document that you have.

        String result = "ERROR Getting version";
        
        EziResponseOfstring eziResponse = getWs().paymentExchangeVersion();
        logger.log(Level.INFO, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        if (eziResponse.getError() == 0) {// any errors will be a non zero value
            result = eziResponse.getData().getValue();
        } else {
            logger.log(Level.WARNING, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
        }
        return new AsyncResult<>(result);
    }

    @Asynchronous
    public Future<Boolean> sendAsynchEmail(String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, Properties serverProperties, boolean debug) {
        SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
        try {
            emailAgent.send(to, ccAddress, from, emailSubject, message, theAttachedfileName, serverProperties, debug);
        } catch (Exception e) {
            String error = "Email Send Failed :" + e.getMessage();
            return new AsyncResult<>(false);
        }
        return new AsyncResult<>(true);
    }

}
