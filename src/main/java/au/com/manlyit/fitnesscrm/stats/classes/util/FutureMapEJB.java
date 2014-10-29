/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
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
    private static final int TIMEOUT_SECONDS = 120;
    private static final String FUTUREMAP_INTERNALID = "FMINTID876987";
    private final ConcurrentHashMap<String, List<AsyncJob>> futureMap = new ConcurrentHashMap<>();
    private final static String CHANNEL = "/payments/";
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private final Object lock3 = new Object();
    private final Object lock4 = new Object();
    private final Object lock5 = new Object();
    private final Object lock6 = new Object();
    private final Object lock7 = new Object();
    private final Object lock8 = new Object();
    private final Object lock9 = new Object();
    private final AtomicBoolean settlementReportLock = new AtomicBoolean(false);
    private final AtomicBoolean paymentReportLock = new AtomicBoolean(false);
    private List<Payment> paymentsByCustomersMissingFromCRM;
    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private PaymentParametersFacade paymentParametersFacade;
    @Inject
    private PaymentBean paymentBean;
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
        synchronized (lock9) {
            logger.log(Level.INFO, "Get Future Map.  for sessionID {0}.", userSessionId);

            List<AsyncJob> fmap = futureMap.get(userSessionId);
            if (fmap == null) {
                logger.log(Level.INFO, "Get Future Map. Map is null for sessionID {0} . Creating an empty list.", userSessionId);
                futureMap.put(userSessionId, new ArrayList<AsyncJob>());
            }
            return fmap;
        }
    }

    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    public boolean runSettlementReport(Date fromDate) {
// use this if you need to match up against what is in your bank account
        if (settlementReportLock.get() == true) {
            logger.log(Level.INFO, "The Settlement Report Already Running.");
            return false;
        } else {
            logger.log(Level.INFO, "Future Map, runSettlementReport. from date {0}.", fromDate);
            AsyncJob aj = new AsyncJob("SettlementReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, true, getDigitalKey()));
            this.put(FUTUREMAP_INTERNALID, aj);
            settlementReportLock.set(true);
        }
        return true;
    }

    public boolean runPaymentReport(Date fromDate) {
        // use this if to get the lates payment information
        if (paymentReportLock.get() == true) {
            logger.log(Level.INFO, "The Settlement Report Already Running.");
            return false;
        } else {
            logger.log(Level.INFO, "Future Map, runPaymentReport. from date {0}.", fromDate);
            AsyncJob aj = new AsyncJob("PaymentReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, false, getDigitalKey()));
            this.put(FUTUREMAP_INTERNALID, aj);
            paymentReportLock.set(true);
        }
        return true;
    }

    public void remove(String userSessionId, String key) {
        synchronized (lock1) {
            logger.log(Level.INFO, "Future Map, remove. sessionid {0}, key {1}.",new Object[]{userSessionId,key} );
            List<AsyncJob> fmap = getFutureMap(userSessionId);
            for (int x = fmap.size(); x > 0; x--) {
                AsyncJob aj = fmap.get(x - 1);
                if (aj.getJobName().contentEquals(key)) {
                    fmap.remove(x - 1);
                }
            }
        }
    }

    public int size(String userSessionId) {
        return getFutureMap(userSessionId).size();
    }

    public AsyncJob get(String userSessionId, String key) {
        synchronized (lock1) {
            logger.log(Level.INFO, "Future Map, get sessionid {0}, key {1}.",new Object[]{userSessionId,key} );
            List<AsyncJob> fmap = getFutureMap(userSessionId);
            for (int x = fmap.size(); x > 0; x--) {
                AsyncJob aj = fmap.get(x - 1);
                if (aj.getJobName().contentEquals(key)) {
                    return aj;
                }
            }
            return null;
        }
    }

    public boolean containsKey(String userSessionId, String key) {
        synchronized (lock1) {
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
    }

    /**
     * @param userSessionId
     *
     *
     * @param aj
     *
     */
    public void put(String userSessionId, AsyncJob aj) {
        synchronized (lock1) {
            try {
                logger.log(Level.INFO, "Future Map, put. sessionid {0},AsyncJob key {1}.",new Object[]{userSessionId,aj.getJobName()} );
                getFutureMap(userSessionId).add(aj);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Future Map put(String userSessionId, AsyncJob aj) method. Unable to add Async Job, Session:{1}, job Name:{2}, start Time:{3}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, aj.getJobName(), aj.getStartTime()});
            }
        }
    }

    public void cancelFutures(String userSessionId) {
        synchronized (lock1) {
            if (getFutureMap(userSessionId) != null) {
                List<AsyncJob> fmap = getFutureMap(userSessionId);
                for (AsyncJob aj : fmap) {
                    Future ft = (Future) aj.getFuture();
                    ft.cancel(false);
                }
                getFutureMap(userSessionId).clear();
            }
        }
    }

    @PreDestroy
    private void cancelAllAsyncJobs() {
        synchronized (lock1) {
            for (Map.Entry pairs : futureMap.entrySet()) {
                cancelFutures((String) pairs.getKey());
            }
            futureMap.clear();
        }
    }

    public void sendMessage(String sessionChannel, String summary, String detail) {
        //TODO
        // sessionChannel = "/test";// remove this once the channel is dynamically set by session id
        synchronized (lock2) {
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
    }

    @Schedule(dayOfMonth = "*", hour = "6", minute = "0", second = "0")
    //@Schedule(dayOfMonth = "*", hour = "*", minute = "*", second = "0")//debug
    public void retrievePaymentsReportFromPaymentGateway(Timer t) {  // run every day at 5am seconds
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        logger.log(Level.INFO, "Running the daily payment report from date:{0}", cal.getTime());

        boolean result = runPaymentReport(cal.getTime());
        logger.log(Level.INFO, "The daily payment report has completed. Result:{0}", result);
    }

    @Schedule(hour = "*", minute = "*", second = "*")
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 1 seconds

        logger.log(Level.FINE, "Checking Future Map for completed jobs.");

        for (Map.Entry pairs : futureMap.entrySet()) {
            String sessionId = (String) pairs.getKey();
            ArrayList<AsyncJob> fmap = (ArrayList<AsyncJob>) pairs.getValue();
            int k = fmap.size();
            if (k > 0) {

                logger.log(Level.INFO, "{0} jobs are running. Checking Future Map to see if asych jobs have finished so their results can be processed.", k);
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
                        logger.log(Level.INFO, "SessionId {0} Future Map async job {1} has finished.", new Object[]{key, sessionId});
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
        logger.log(Level.FINE, "Finished Checking Future Map for completed jobs.");
    }

    // run a schedules
    public void processCompletedAsyncJobs(String sessionId) {
        logger.log(Level.INFO, "Future Map is processing Completed Async Jobs .");
        synchronized (lock3) {
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
                key = "PaymentReport";
                if (containsKey(sessionId, key)) {
                    Future ft = (Future) get(sessionId, key).getFuture();
                    if (ft.isDone()) {
                        //    remove(sessionId, key);
                        processPaymentReport(ft);
                        if (sessionId.contains(FUTUREMAP_INTERNALID)) {
                            remove(sessionId, key);
                        }
                    }
                }
                key = "SettlementReport";
                if (containsKey(sessionId, key)) {
                    Future ft = (Future) get(sessionId, key).getFuture();
                    if (ft.isDone()) {
                        //    remove(sessionId, key);
                        processSettlementReport(ft);
                        if (sessionId.contains(FUTUREMAP_INTERNALID)) {
                            remove(sessionId, key);
                        }
                    }
                }

            } catch (CancellationException ex) {
                logger.log(Level.WARNING, key + " Future Map:", ex);

            }
        }
    }

    private void processSettlementReport(Future ft) {
        synchronized (lock8) {
            logger.log(Level.INFO, "Future Map is processing the Settlement Report .");
            processReport(ft);
            logger.log(Level.INFO, "Future Map has finished asyc processing the Settlement Report .");
            settlementReportLock.set(false);
        }
    }

    private void processPaymentReport(Future ft) {
        synchronized (lock7) {
            logger.log(Level.INFO, "Future Map is processing the Payment Report .");
            processReport(ft);
            logger.log(Level.INFO, "Future Map has finished async processing the Payment Report .");
            paymentReportLock.set(false);
        }
    }

    @Asynchronous
    private void processReport(Future ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment result = null;
        paymentsByCustomersMissingFromCRM = new ArrayList<>();
        try {
            result = (ArrayOfPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getPayment() != null) {
            List<Payment> payList = result.getPayment();
            if (payList.isEmpty() == false) {

                for (Payment pay : payList) {

                    String customerRef = pay.getYourSystemReference().getValue();
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processReport an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }
                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        String paymentID = pay.getPaymentID().getValue();
                        if (paymentID.toUpperCase().contains("SCHEDULED")) {
                            // scheduled payment no paymentID
                            logger.log(Level.INFO, "Future Map processReport scheduled payment .", pay.toString());
                        } else {
                            Payments crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);
                            if (crmPay != null) { //' payment exists
                                if (comparePaymentXMLToEntity(crmPay, pay)) {
                                    // they are the same so no update
                                    logger.log(Level.FINE, "Future Map processReport paymenst are the same.");
                                } else {
                                    crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                    paymentsFacade.edit(crmPay);
                                }
                            } else { //payment doesn't exist in crm so add it
                                crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                paymentsFacade.create(crmPay);
                            }
                        }

                    } else {
                        paymentsByCustomersMissingFromCRM.add(pay);
                        String eziRef = pay.getEzidebitCustomerID().getValue();
                        logger.log(Level.SEVERE, "Future Map processReport couldn't find a customer with our system ref from payment.EziDebit Ref No: {0}", eziRef);
                        /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                    }
                }
            } else {
                logger.log(Level.WARNING, "Future Map processReport couldn't find any Payments.");

            }
        }
        logger.log(Level.INFO, "Future Map processReport completed");
    }

    @Asynchronous
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

        if (result != null && result.getPayment() != null) {
            List<Payment> payList = result.getPayment();
            if (payList.isEmpty() == false) {
                String customerRef = payList.get(0).getYourSystemReference().getValue();
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processGetPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        for (Payment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                logger.log(Level.WARNING, "Future Map processGetPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            for (Payment pay : payList) {
                                String paymentID = pay.getPaymentID().getValue();
                                if (paymentID.toUpperCase().contains("SCHEDULED")) {
                                    // scheduled payment no paymentID
                                    logger.log(Level.INFO, "Future Map processGetPayments scheduled payment .", pay.toString());
                                } else {
                                    Payments crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);
                                    if (crmPay != null) { //' payment exists
                                        if (comparePaymentXMLToEntity(crmPay, pay)) {
                                            // they are the same so no update
                                            logger.log(Level.FINE, "Future Map processGetPayments paymenst are the same.");
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
                        logger.log(Level.SEVERE, "Future Map processGetPayments couldn't find a customer with our system ref from payment.");
                        /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                    }
                } else {
                    logger.log(Level.WARNING, "Future Map processGetPayments our system ref in payment is null.");
                }

            }
        }

        logger.log(Level.INFO, "Future Map processGetPayments completed");
    }

    @Asynchronous
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
                            logger.log(Level.WARNING, "Future Map processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                        }

                        Customers cust = customersFacade.findById(custId);
                        if (cust != null) {
                            for (ScheduledPayment pay : payList) {
                                if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                    logger.log(Level.WARNING, "Future Map processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
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
                                    logger.log(Level.WARNING, "Future Map processGetScheduledPayments - Failed to delete some scheduled payments. ExistedInCRM={0}, Deleted={1},Existeding in Payment Gateway={2}", new Object[]{existingInCRM, numberDeleted, scheduledPayments});

                                }
                                for (ScheduledPayment pay : payList) {
                                    Payments crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                    paymentsFacade.create(crmPay);
                                    createScheduledPayments++;
                                }
                                if (createScheduledPayments != scheduledPayments) {

                                    logger.log(Level.WARNING, "Future Map processGetScheduledPayments - The number of payments created does not equal the number retrieved from the payment gateway. Retireved={1}, Created={2}, Existed In CRM and were deleted={0}", new Object[]{existingInCRM, createScheduledPayments, scheduledPayments});

                                }

                            }
                        } else {
                            logger.log(Level.SEVERE, "Future Map processGetScheduledPayments couldn't find a customer with our system ref from payment.");
                            /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                             as this means that a customer is in ezidebits system but not ours */

                        }
                    }
                }
            }

            logger.log(Level.INFO, "processGetScheduledPayments completed");
        }
    }

    @Asynchronous
    private void processGetCustomerDetails(Future ft) {
        CustomerDetails custDetails = null;

        try {
            custDetails = (CustomerDetails) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Future Map processGetCustomerDetails", ex);
        }
        if (custDetails != null) {
            // do something with result
            String customerRef = custDetails.getYourSystemReference().getValue();
            if (customerRef.trim().isEmpty() == false) {
                int custId = 0;
                try {
                    custId = Integer.parseInt(customerRef.trim());
                } catch (NumberFormatException numberFormatException) {
                    logger.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                }

                Customers cust = customersFacade.findById(custId);
                if (cust != null) {
                    PaymentParameters pp = cust.getPaymentParameters();
                    boolean isNew = false;
                    if (pp == null) {
                        pp = new PaymentParameters();
                        pp.setId(0);
                        pp.setWebddrUrl(null);
                        pp.setLoggedInUser(cust);
                        isNew = true;
                    }

                    pp.setAddressLine1(custDetails.getAddressLine1().getValue());
                    pp.setAddressLine2(custDetails.getAddressLine2().getValue());
                    pp.setAddressPostCode(custDetails.getAddressPostCode().getValue());
                    pp.setAddressState(custDetails.getAddressState().getValue());
                    pp.setAddressSuburb(custDetails.getAddressSuburb().getValue());
                    pp.setContractStartDate(custDetails.getContractStartDate().getValue().toGregorianCalendar().getTime());
                    pp.setCustomerFirstName(custDetails.getCustomerFirstName().getValue());
                    pp.setCustomerName(custDetails.getCustomerName().getValue());
                    pp.setEmail(custDetails.getEmail().getValue());
                    pp.setEzidebitCustomerID(custDetails.getEzidebitCustomerID().getValue());

                    pp.setMobilePhoneNumber(custDetails.getMobilePhone().getValue());
                    pp.setPaymentGatewayName("EZIDEBIT");
                    pp.setPaymentMethod(custDetails.getPaymentMethod().getValue());
                    pp.setPaymentPeriod(custDetails.getPaymentPeriod().getValue());
                    pp.setPaymentPeriodDayOfMonth(custDetails.getPaymentPeriodDayOfMonth().getValue());
                    pp.setPaymentPeriodDayOfWeek(custDetails.getPaymentPeriodDayOfWeek().getValue());

                    pp.setSmsExpiredCard(custDetails.getSmsExpiredCard().getValue());
                    pp.setSmsFailedNotification(custDetails.getSmsFailedNotification().getValue());
                    pp.setSmsPaymentReminder(custDetails.getSmsPaymentReminder().getValue());
                    pp.setStatusCode(custDetails.getStatusCode().getValue());
                    pp.setStatusDescription(custDetails.getStatusDescription().getValue());
                    pp.setTotalPaymentsFailed(custDetails.getTotalPaymentsFailed());
                    pp.setTotalPaymentsFailedAmount(new BigDecimal(custDetails.getTotalPaymentsFailed().floatValue()));
                    pp.setTotalPaymentsSuccessful(custDetails.getTotalPaymentsSuccessful());
                    pp.setTotalPaymentsSuccessfulAmount(new BigDecimal(custDetails.getTotalPaymentsSuccessfulAmount().floatValue()));
                    pp.setYourGeneralReference(custDetails.getYourGeneralReference().getValue());
                    pp.setYourSystemReference(custDetails.getYourSystemReference().getValue());
                    if (isNew) {
                        paymentParametersFacade.create(pp);
                    } else {
                        paymentParametersFacade.edit(pp);
                    }
                } else {
                    logger.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number.");
                }
            }
        }
        logger.log(Level.INFO, "Future Map processGetCustomerDetails completed");
    }

    private Payments convertPaymentXMLToEntity(Payments payment, Payment pay, Customers cust) {
        synchronized (lock4) {
            if (payment == null) {
                payment = new Payments();
                payment.setCreateDatetime(new Date());
                payment.setManuallyAddedPayment(false);
                payment.setId(0);
                payment.setCustomerName(cust);
            }
            try {
                payment.setLastUpdatedDatetime(new Date());
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
                if (pay.getPaymentReference() != null) {
                    if (pay.getPaymentReference().getValue().trim().isEmpty() == false) {
                        payment.setManuallyAddedPayment(true);
                    } else {
                        payment.setManuallyAddedPayment(false);
                    }
                }
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
                logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.:", e);
            }

            return payment;
        }
    }

    private Payments convertScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay, Customers cust) {
        synchronized (lock5) {
            if (payment == null) {
                payment = new Payments();
                payment.setCreateDatetime(new Date());
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
                if (pay.isManuallyAddedPayment() != null) {

                    payment.setManuallyAddedPayment(pay.isManuallyAddedPayment());

                }
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
                logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.:", e);
            }

            return payment;

        }
    }

    private boolean comparePaymentXMLToEntity(Payments payment, Payment pay) {
        synchronized (lock6) {
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
                logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.:", e);
            }

            return theSame;
        }
    }

}
