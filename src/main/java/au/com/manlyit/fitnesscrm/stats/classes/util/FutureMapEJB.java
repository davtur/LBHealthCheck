/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

/**
 *
 * @author david
 */
@PushEndpoint("/payments/{user}")

@Singleton
@LocalBean
@Startup
public class FutureMapEJB {

    private static final Logger logger = Logger.getLogger(FutureMapEJB.class.getName());
    private static final int TIMEOUT_SECONDS = 30;
    private final ConcurrentHashMap<String, List<AsyncJob>> futureMap = new ConcurrentHashMap<>();
    private final static String CHANNEL = "/payments/";
    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade paymentsFacade;

    @PathParam("user")
    private String username;

    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus e) {
        r.transport().toString();
        logger.log(Level.INFO, "Atmosphere Push Connection Opened. Transport Type = {0}", r.transport().toString());
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus e) {
        logger.log(Level.INFO, "Atmosphere Push Connection Closed.");

    }

    /**
     * @param userSessionId
     * @return the futureMap
     */
    public List<AsyncJob> getFutureMap(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        List<AsyncJob> fmap = futureMap.get(userSessionId);
        if (fmap == null) {
            futureMap.put(userSessionId, new ArrayList<AsyncJob>());
        }
        return fmap;
    }

    public void remove(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        for (int x = fmap.size(); x > 0; x--) {
            AsyncJob aj = fmap.get(x - 1);
            if (aj.getJobName().contentEquals(key)) {
                fmap.remove(x - 1);
            }
        }
    }

    public int size(String userSessionId) {
        return getFutureMap(userSessionId).size();
    }

    public AsyncJob get(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        for (int x = fmap.size(); x > 0; x--) {
            AsyncJob aj = fmap.get(x - 1);
            if (aj.getJobName().contentEquals(key)) {
                return aj;
            }
        }
        return null;
    }

    public boolean containsKey(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        try {
            for (int x = fmap.size(); x > 0; x--) {
                AsyncJob aj = fmap.get(x - 1);
                if (aj.getJobName().contentEquals(key)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "futureMap.containsKey", e);
        }
        return false;

    }

    /**
     * @param userSessionId
     *
     *
     * @param aj
     *
     */
    public void put(String userSessionId, AsyncJob aj) {
        getFutureMap(userSessionId).add(aj);
    }

    public void cancelFutures(String userSessionId) {
        if (getFutureMap(userSessionId) != null) {
            List<AsyncJob> fmap = getFutureMap(userSessionId);
            for (AsyncJob aj : fmap) {
                Future ft = (Future) aj.getFuture();
                ft.cancel(false);
            }
            getFutureMap(userSessionId).clear();
        }
    }

    @PreDestroy
    private void cancelAllAsyncJobs() {

        for (Map.Entry pairs : futureMap.entrySet()) {
            cancelFutures((String) pairs.getKey());
        }
        futureMap.clear();
    }

    public void sendMessage(String sessionChannel, String summary, String detail) {
        //TODO
        // sessionChannel = "/test";// remove this once the channel is dynamically set by session id
        final String broadcastChannel = CHANNEL + sessionChannel;
        final String summ = summary;
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        /* Reply rep = new EventBus.Reply() {
         @Override
         public void completed(String message) {

         logger.log(Level.INFO, "Message Delivered:Channel={0}, Summary={1}.", new Object[]{broadcastChannel, summ});
         }
         };*/
        // eventBus.publish(channels.getChannel(getUser()), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
        eventBus.publish(broadcastChannel, new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }

    @Schedule(hour = "*", minute = "*", second = "*")
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 2 seconds

        logger.log(Level.FINE, "Checking Future Map for completed jobs.");

        for (Map.Entry pairs : futureMap.entrySet()) {
            String sessionId = (String) pairs.getKey();
            ArrayList<AsyncJob> fmap = (ArrayList<AsyncJob>) pairs.getValue();
            int k = fmap.size();
            if (k > 0) {

                logger.log(Level.INFO, "{0} jobs are running. Checking to see if asych jobs have finished so their results can be processed.", k);
                /*for (Map.Entry pairsFut : fmap.entrySet()) {
                 Future ft = (Future) pairsFut.getValue();
                 String key = (String) pairsFut.getKey();
                 if (ft.isDone()) {
                 sendMessage(sessionId, "Asynchronous Task Completed", key);
                 logger.log(Level.INFO, "Notifying sessionId {0} that async job {1} has finished.", new Object[]{key, sessionId});
                 }
                 }*/
                int y = 0;
                String details = "";
                AsyncJob aj;
                for (int x = fmap.size(); x > 0; x--) {
                    aj = fmap.get(x - 1);
                    Future ft = aj.getFuture();
                    String key = aj.getJobName();
                    if (ft.isDone()) {
                        y++;
                        logger.log(Level.INFO, "SessionId {0} async job {1} has finished.", new Object[]{key, sessionId});
                        details += key + " ";
                        processCompletedAsyncJobs(sessionId);
                    }
                    GregorianCalendar jobStartTime = new GregorianCalendar();
                    GregorianCalendar currentTime = new GregorianCalendar();

                    jobStartTime.setTime(aj.getStartTime());
                    jobStartTime.add(Calendar.SECOND, TIMEOUT_SECONDS);
                    if (jobStartTime.compareTo(currentTime) < 0) {
                        ft.cancel(true);
                        fmap.remove(x - 1);
                        logger.log(Level.INFO, "SessionId {0} async job {1} has timed out ({2} seconds )  and been cancelled.", new Object[]{key, sessionId, TIMEOUT_SECONDS});
                    }

                }
                if (y > 0) {
                    sendMessage(sessionId, "Asynchronous Tasks Completed", details);
                    logger.log(Level.INFO, "Notifying that {0} async jobs for sessionId {1} have finished.", new Object[]{Integer.toString(y), sessionId});
                }

            }

        }
    }

    // run a schedules
    public void processCompletedAsyncJobs(String sessionId) {

        String key = "";

        try {

            key = "GetCustomerDetails";
            if (containsKey(sessionId, key)) {
                Future ft = (Future) get(sessionId, key).getFuture();
                if (ft.isDone()) {
                    // remove(sessionId, key);
                    processGetCustomerDetails(ft);

                }
            }

            key = "GetPayments";
            if (containsKey(sessionId, key)) {
                Future ft = (Future) get(sessionId, key).getFuture();
                if (ft.isDone()) {
                    //   remove(sessionId, key);
                    processGetPayments(ft);
                }
            }

            key = "GetScheduledPayments";
            if (containsKey(sessionId, key)) {
                Future ft = (Future) get(sessionId, key).getFuture();
                if (ft.isDone()) {
                    //    remove(sessionId, key);
                    processGetScheduledPayments(ft);
                }
            }

        } catch (CancellationException ex) {
            logger.log(Level.WARNING, key + ":", ex);

        }
    }

    private void processGetPayments(Future ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment result = null;
        boolean abort = false;
        try {
            result = (ArrayOfPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getPayment().isEmpty() == false) {
            List<Payment> payList = result.getPayment();
            if (payList != null) {
                String customerRef = payList.get(0).getYourSystemReference().getValue();
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "processGetPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        for (Payment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                logger.log(Level.WARNING, "processGetPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            for (Payment pay : payList) {
                                String paymentID = pay.getPaymentID().getValue();
                                if (paymentID.toUpperCase().contains("SCHEDULED")) {
                                    // scheduled payment no paymentID
                                    logger.log(Level.INFO, "processGetPayments scheduled payment .", pay.toString());
                                } else {
                                    Payments crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);
                                    if (crmPay != null) { //' payment exists
                                        if (comparePaymentXMLToEntity(crmPay, pay)) {
                                            // they are the same so no update
                                            logger.log(Level.FINE, "processGetPayments paymenst are the same.");
                                        } else {
                                            crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                            paymentsFacade.edit(crmPay);
                                        }
                                    } else { //payment doesn't exist in crm so add it
                                        crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                        paymentsFacade.create(crmPay);
                                    }
                                }

                            }
                        }
                    } else {
                        logger.log(Level.SEVERE, "processGetPayments couldn't find a customer with our system ref from payment.");
                        /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                    }
                } else {
                    logger.log(Level.WARNING, "processGetPayments our system ref in payment is null.");
                }

            }
        }

        logger.log(Level.INFO, "processGetPayments completed");
    }

    private void processGetScheduledPayments(Future ft) {
        ArrayOfScheduledPayment result = null;
        boolean abort = false;
        int scheduledPayments = 0;
        int existingInCRM = 0;
        int createScheduledPayments = 0;
        try {
            result = (ArrayOfScheduledPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null) {
            List<ScheduledPayment> payList = result.getScheduledPayment();
            if (payList != null) {
                if (payList.size() > 1) {
                    String customerRef = payList.get(0).getYourSystemReference().getValue();
                    if (customerRef.trim().isEmpty() == false) {
                        int custId = 0;
                        try {
                            custId = Integer.parseInt(customerRef.trim());
                        } catch (NumberFormatException numberFormatException) {
                            logger.log(Level.WARNING, "processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                        }

                        Customers cust = customersFacade.findById(custId);
                        if (cust != null) {
                            for (ScheduledPayment pay : payList) {
                                if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                    logger.log(Level.WARNING, "processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                    abort = true;
                                }

                            }
                            if (abort == false) {
                                scheduledPayments = payList.size();

                                List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust);
                                existingInCRM = crmPaymentList.size();
                                int numberDeleted = 0;
                                for (Payments p : crmPaymentList) {
                                    paymentsFacade.remove(p);
                                    numberDeleted++;
                                }
                                if (numberDeleted != existingInCRM) {
                                    logger.log(Level.WARNING, "processGetScheduledPayments - Failed to delete some scheduled payments. ExistedInCRM={0}, Deleted={1},Existeding in Payment Gateway={2}", new Object[]{existingInCRM, numberDeleted, scheduledPayments});

                                }
                                for (ScheduledPayment pay : payList) {
                                    Payments crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                    paymentsFacade.create(crmPay);
                                    createScheduledPayments++;
                                }
                                if (createScheduledPayments != scheduledPayments) {

                                    logger.log(Level.WARNING, "processGetScheduledPayments - The number of payments created does not equal the number retrieved from the payment gateway. Retireved={1}, Created={2}, Existed In CRM and were deleted={0}", new Object[]{existingInCRM, createScheduledPayments, scheduledPayments});

                                }

                            }
                        } else {
                            logger.log(Level.SEVERE, "processGetScheduledPayments couldn't find a customer with our system ref from payment.");
                            /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                             as this means that a customer is in ezidebits system but not ours */

                        }
                    }
                }
            }

            logger.log(Level.INFO, "processGetScheduledPayments completed");
        }
    }

    private Payments convertPaymentXMLToEntity(Payments payment, Payment pay, Customers cust) {

        if (payment == null) {
            payment = new Payments();
            payment.setCreateDatetime(new Date());
            payment.setLastUpdatedDatetime(new Date());
            payment.setId(0);
            payment.setCustomerName(cust);
        }
        try {
            payment.setBankFailedReason(pay.getBankFailedReason().getValue());
            payment.setBankReceiptID(pay.getBankReceiptID().getValue());
            payment.setBankReturnCode(pay.getBankReturnCode().getValue());
            //payment.setCustomerName(cust);
            payment.setDebitDate(pay.getDebitDate().getValue().toGregorianCalendar().getTime());
            payment.setEzidebitCustomerID(pay.getEzidebitCustomerID().getValue());
            payment.setInvoiceID(pay.getInvoiceID().getValue());
            payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount().floatValue()));
            payment.setPaymentID(pay.getPaymentID().getValue());
            payment.setPaymentMethod(pay.getPaymentMethod().getValue());
            payment.setPaymentReference(pay.getPaymentReference().getValue());
            payment.setPaymentSource(pay.getPaymentSource().getValue());
            payment.setScheduledAmount(new BigDecimal(pay.getScheduledAmount().floatValue()));

            payment.setSettlementDate(pay.getSettlementDate().getValue().toGregorianCalendar().getTime());
            payment.setPaymentStatus(pay.getPaymentStatus().getValue());
            payment.setTransactionFeeClient(new BigDecimal(pay.getTransactionFeeClient().floatValue()));
            payment.setTransactionFeeCustomer(new BigDecimal(pay.getTransactionFeeCustomer().floatValue()));

            if (pay.getTransactionTime().getValue() != null) {
                payment.setTransactionTime(pay.getTransactionTime().getValue().toGregorianCalendar().getTime()); // only valid for real time and credit card payments
            }

            payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
            payment.setYourSystemReference(pay.getYourSystemReference().getValue());
        } catch (Exception e) {
            logger.log(Level.WARNING, "convertPaymentXMLToEntity method failed.:", e);
        }

        return payment;

    }

    private Payments convertScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay, Customers cust) {

        if (payment == null) {
            payment = new Payments();
            payment.setCreateDatetime(new Date());
            payment.setLastUpdatedDatetime(new Date());
            payment.setId(0);
            payment.setCustomerName(cust);
        }
        try {
            payment.setLastUpdatedDatetime(new Date());
            payment.setBankFailedReason("");
            payment.setBankReceiptID("");
            payment.setBankReturnCode("");
            payment.setDebitDate(pay.getPaymentDate().toGregorianCalendar().getTime());
            payment.setEzidebitCustomerID(pay.getEzidebitCustomerID().getValue());
            payment.setInvoiceID(pay.getEzidebitCustomerID().getValue());
            payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount().floatValue()));
            payment.setPaymentID(null);
            payment.setPaymentMethod("DR");
            payment.setPaymentReference(pay.getPaymentReference().getValue());
            payment.setPaymentSource("SCHEDULED");
            payment.setScheduledAmount(new BigDecimal(pay.getPaymentAmount().floatValue()));

            payment.setSettlementDate(null);
            payment.setPaymentStatus(null);
            payment.setTransactionFeeClient(new BigDecimal(0));
            payment.setTransactionFeeCustomer(new BigDecimal(0));

            payment.setTransactionTime(null); // only valid for real time and credit card payments

            payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
            payment.setYourSystemReference(pay.getYourSystemReference().getValue());
        } catch (Exception e) {
            logger.log(Level.WARNING, "convertPaymentXMLToEntity method failed.:", e);
        }

        return payment;

    }

    private boolean comparePaymentXMLToEntity(Payments payment, Payment pay) {

        boolean theSame = true;
        try {
            if (payment.getBankFailedReason().contains(pay.getBankFailedReason().getValue()) == false) {
                theSame = false;
            }
            if (payment.getBankReceiptID().contains(pay.getBankReceiptID().getValue()) == false) {
                theSame = false;
            }
            if (payment.getBankReturnCode().contains(pay.getBankReturnCode().getValue()) == false) {
                theSame = false;
            }
            if (payment.getCustomerName() == null) {
                theSame = false;
            } else {
                if (payment.getCustomerName().getId().toString().contains(pay.getYourSystemReference().getValue()) == false) {
                    theSame = false;
                }
            }
            if (payment.getDebitDate().compareTo(pay.getDebitDate().getValue().toGregorianCalendar().getTime()) != 0) {
                theSame = false;
            }
            if (payment.getEzidebitCustomerID().contains(pay.getBankFailedReason().getValue()) == false) {
                theSame = false;
            }
            if (payment.getInvoiceID().contains(pay.getEzidebitCustomerID().getValue()) == false) {
                theSame = false;
            }
            if (payment.getPaymentAmount().compareTo(new BigDecimal(pay.getPaymentAmount().floatValue())) != 0) {
                theSame = false;
            }
            if (payment.getPaymentID().contains(pay.getPaymentID().getValue()) == false) {
                theSame = false;
            }
            if (payment.getPaymentMethod().contains(pay.getPaymentMethod().getValue()) == false) {
                theSame = false;
            }
            if (payment.getPaymentReference().contains(pay.getPaymentReference().getValue()) == false) {
                theSame = false;
            }
            if (payment.getPaymentSource().contains(pay.getPaymentSource().getValue()) == false) {
                theSame = false;
            }
            if (payment.getPaymentStatus().contains(pay.getPaymentStatus().getValue()) == false) {
                theSame = false;
            }
            if (payment.getScheduledAmount().compareTo(new BigDecimal(pay.getScheduledAmount().floatValue())) != 0) {
                theSame = false;
            }
            if (payment.getSettlementDate().compareTo(pay.getSettlementDate().getValue().toGregorianCalendar().getTime()) != 0) {
                theSame = false;
            }
            if (payment.getTransactionFeeClient().compareTo(new BigDecimal(pay.getTransactionFeeClient().floatValue())) != 0) {
                theSame = false;
            }
            if (payment.getTransactionFeeCustomer().compareTo(new BigDecimal(pay.getTransactionFeeCustomer().floatValue())) != 0) {
                theSame = false;
            }
            if (pay.getTransactionTime().getValue() != null) {
                if (payment.getTransactionTime().compareTo(pay.getTransactionTime().getValue().toGregorianCalendar().getTime()) != 0) {
                    theSame = false;
                }
            }
            if (payment.getYourGeneralReference().contains(pay.getYourGeneralReference().getValue()) == false) {
                theSame = false;
            }
            if (payment.getYourSystemReference().contains(pay.getYourSystemReference().getValue()) == false) {
                theSame = false;
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "convertPaymentXMLToEntity method failed.:", e);
        }

        return theSame;

    }

    private void processGetCustomerDetails(Future ft) {
        CustomerDetails result = null;
        /*  setCustomerDetailsHaveBeenRetrieved(true);
         try {
         result = (CustomerDetails) ft.get();
         } catch (InterruptedException | ExecutionException ex) {
         Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetails", ex);
         }
         if (result != null) {
         // do something with result
         setAutoStartPoller(false);
         customerExistsInPaymentGateway = true;

         setCurrentCustomerDetails(result);
         String eziStatusCode = result.getStatusDescription().getValue().toUpperCase().trim();
         String ourStatus = getSelectedCustomer().getActive().getCustomerState().toUpperCase().trim();
         String message = "";
         if (ourStatus.contains(eziStatusCode) == false) {
         // status codes don't match
         String cust = getSelectedCustomer().getUsername();
         message = "Customer Status codes dont match. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
         if (eziStatusCode.contains("WAITING BANK DETAILS")) {
         message = "The Customer does not have any banking details. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
         logger.log(Level.INFO, message);
         setWaitingForPaymentDetails(true);
         JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Important!", "You must enter customer banking details before payments can be set up.");
         } else {
         logger.log(Level.WARNING, message);
         setWaitingForPaymentDetails(false);
         JsfUtil.pushErrorMessage(CHANNEL + sessionId, message, "");
         }
         if (eziStatusCode.toUpperCase().contains("CANCELLED")) {
         message = "The Customer is in a Cancelled status in the payment gateway";
         logger.log(Level.INFO, message);
         JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Important!", message);
         setCustomerCancelledInPaymentGateway(true);
         } else {
         setCustomerCancelledInPaymentGateway(false);
         }
         } else {
         //status codes match
         setWaitingForPaymentDetails(false);
         if (eziStatusCode.toUpperCase().contains("CANCELLED")) {
         setCustomerCancelledInPaymentGateway(true);
         } else {
         setCustomerCancelledInPaymentGateway(false);
         }
         }

         } else {
         customerExistsInPaymentGateway = false;
         }
         logger.log(Level.INFO, "processGetCustomerDetails completed");
         */
    }

}
