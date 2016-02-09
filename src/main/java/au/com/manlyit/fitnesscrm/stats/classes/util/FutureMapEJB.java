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
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.CustomerImagesController;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.faces.application.FacesMessage;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
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
public class FutureMapEJB implements Serializable {

    private static final Logger logger = Logger.getLogger(FutureMapEJB.class.getName());
    private static final int TIMEOUT_SECONDS = 300;
    private static final String FUTUREMAP_INTERNALID = "FMINTID876987";
    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<String, ArrayList<AsyncJob>> futureMap = new ConcurrentHashMap<>();
    private final static String CHANNEL = "/payments/";
    private int counter = 0;
    private final Object lock1 = new Object();
    private final Object lock2 = new Object();
    private final Object lock3 = new Object();
    private final Object lock4 = new Object();
    private final Object lock5 = new Object();
    private final Object lock6 = new Object();
    private final Object lock7 = new Object();
    private final Object lock8 = new Object();
    private final Object lock9 = new Object();
    private final Object futureMapArrayLock = new Object();
    private final AtomicBoolean settlementReportLock = new AtomicBoolean(false);
    private final AtomicBoolean paymentReportLock = new AtomicBoolean(false);
    private final AtomicBoolean asychCheckProcessing = new AtomicBoolean(false);
    private List<Payment> paymentsByCustomersMissingFromCRM;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbCustomerImagesFacade;

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

    public FutureMapEJB() {
    }

    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint rEndPoint, EventBus e) {
        rEndPoint.address();

        logger.log(Level.INFO, "Atmosphere Push Connection Opened. Transport Type = {0}", rEndPoint.address());
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus e) {
        logger.log(Level.INFO, "Atmosphere Push Connection Closed.");

    }

    @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "Application Setup Running");
        try {
            sanityCheckCustomersForDefaultItems();

        } catch (Exception e) {
            logger.log(Level.SEVERE, " @PostConstruct Future Map - applicationSetup(). Exception in sanityCheckCustomersForDefaultItems: ", e);
        }
        logger.log(Level.INFO, "Application Setup Completed");
    }

    /**
     * @return the FUTUREMAP_INTERNALID
     */
    public String getFutureMapInternalSessionId() {
        return FUTUREMAP_INTERNALID;
    }

    /**
     * @param userSessionId
     * @return the futureMap
     */
    public ArrayList<AsyncJob> getFutureMap(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        synchronized (futureMapArrayLock) {
            logger.log(Level.FINE, "Get Future Map.  for sessionID {0}.", userSessionId);

            ArrayList<AsyncJob> fmap = futureMap.get(userSessionId);
            if (fmap == null) {
                logger.log(Level.INFO, "Get Future Map. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

                futureMap.put(userSessionId, new ArrayList<>());
                fmap = futureMap.get(userSessionId);

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
            Date toDate = new Date();
            AsyncJob aj = new AsyncJob("SettlementReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, toDate, true, getDigitalKey()));
            this.put(FUTUREMAP_INTERNALID, aj);
            settlementReportLock.set(true);
        }
        return true;
    }

    public boolean runPaymentReport(Date fromDate) throws InterruptedException {
        // use this if to get the lates payment information
        if (paymentReportLock.get() == true) {
            logger.log(Level.INFO, "The Settlement Report Already Running.");
            return false;
        } else {
            logger.log(Level.INFO, "Future Map, runPaymentReport. from date {0}.", fromDate);
            Date toDate = new Date();
            AsyncJob aj = new AsyncJob("PaymentReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, toDate, false, getDigitalKey()));
            this.put(FUTUREMAP_INTERNALID, aj);
            refreshAllCustomersDetailsFromGateway();
            paymentReportLock.set(true);
        }
        return true;
    }

    private void refreshAllCustomersDetailsFromGateway() {
        List<Customers> acl = customersFacade.findAllActiveCustomers(true);
        AsyncJob aj2;
        for (Customers c : acl) {
            try {
                aj2 = new AsyncJob("GetCustomerDetails", paymentBean.getCustomerDetails(c, getDigitalKey()));
                this.put(FUTUREMAP_INTERNALID, aj2);
                Thread.sleep(500);//sleeping for a long time wont affect performance (the warning is there for a short sleep of say 5ms ) but we don't want to overload the payment gateway or they may get upset.
            } catch (InterruptedException ex) {
                Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "refreshAllCustomersDetailsFromGateway - Thread Sleep InterruptedException", ex.getMessage());
            }
        }
    }

    public void remove(String userSessionId, String key) {
        synchronized (futureMapArrayLock) {
            logger.log(Level.INFO, "Future Map, remove. sessionid {0}, key {1}.", new Object[]{userSessionId, key});
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
        int s;
        synchronized (futureMapArrayLock) {

            s = getFutureMap(userSessionId).size();
        }
        return s;
    }

    public AsyncJob get(String userSessionId, String key) {
        synchronized (futureMapArrayLock) {
            logger.log(Level.FINE, "Future Map, get sessionid {0}, key {1}.", new Object[]{userSessionId, key});
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
        synchronized (futureMapArrayLock) {
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
        synchronized (futureMapArrayLock) {
            try {
                logger.log(Level.INFO, "Future Map, put. sessionid {0},AsyncJob key {1}.", new Object[]{userSessionId, aj.getJobName()});
                getFutureMap(userSessionId).add(aj);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Future Map put(String userSessionId, AsyncJob aj) method. Unable to add Async Job, Session:{1}, job Name:{2}, start Time:{3}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, aj.getJobName(), aj.getStartTime()});
            }
        }
    }

    public void cancelFutures(String userSessionId) {
        synchronized (futureMapArrayLock) {
            if (getFutureMap(userSessionId) != null) {
                List<AsyncJob> fmap = getFutureMap(userSessionId);

                for (AsyncJob aj : fmap) {
                    Future<?> ft = (Future) aj.getFuture();
                    ft.cancel(false);
                }

                getFutureMap(userSessionId).clear();
            }
        }
    }

    @PreDestroy
    private void cancelAllAsyncJobs() {
        synchronized (futureMapArrayLock) {
            for (Map.Entry<String, ArrayList<AsyncJob>> pairs : futureMap.entrySet()) {
                cancelFutures(pairs.getKey());
            }
            futureMap.clear();
        }
    }

    public void sendMessage(String sessionChannel, String summary, String detail) {
        //TODO
        // sessionChannel = "/test";// remove this once the channel is dynamically set by session id
        synchronized (lock2) {
            final String broadcastChannel = CHANNEL + sessionChannel;
            // final String summ = summary;
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

    //@Schedule(dayOfMonth = "*", hour = "*", minute = "*", second = "0")//debug
    @Schedule(dayOfMonth = "*", hour = "6", minute = "0", second = "0")
    public void retrievePaymentsReportFromPaymentGateway(Timer t) {
        try {
            // run every day at 5am seconds
            GregorianCalendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            logger.log(Level.INFO, "Running the daily payment report from date:{0}", cal.getTime());

            boolean result = runPaymentReport(cal.getTime());
            logger.log(Level.INFO, "The daily payment report has completed. Result:{0}", result);
        } catch (InterruptedException ex) {
            Logger.getLogger(FutureMapEJB.class.getName()).log(Level.WARNING, "Run Payment Report was interrupted", ex);
        } catch (Exception ex) {
            Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "retrievePaymentsReportFromPaymentGateway - UNHANDLED EXCEPTION In EJB TIMER", ex);
        }
    }

    @Schedule(hour = "*", minute = "*", second = "*")
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 1 seconds
        counter++;
        if (counter > 180) {
            logger.log(Level.INFO, "EJB Timer Heartbeat (180 seconds) - checkRunningJobsAndNotifyIfComplete.");
            counter = 0;
        }

        if (asychCheckProcessing.get() == false) {
            logger.log(Level.FINE, "Checking Future Map for completed jobs.");
            asychCheckProcessing.set(true);
            try {
                synchronized (futureMapArrayLock) {
                    for (Map.Entry<String, ArrayList<AsyncJob>> pairs : futureMap.entrySet()) {
                        String sessionId = pairs.getKey();
                        ArrayList<AsyncJob> fmap = pairs.getValue();
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

                            StringBuilder details = new StringBuilder();
                            AsyncJob aj;
                            try {

                                for (int x = fmap.size() - 1; x >= 0; x--) {
                                    aj = fmap.get(x);
                                    boolean alreadyRemoved = false;
                                    Future<?> ft = aj.getFuture();
                                    String key = aj.getJobName();
                                    if (ft.isDone()) {
                                        y++;
                                        logger.log(Level.INFO, "SessionId {0} Future Map async job {1} has finished.", new Object[]{key, sessionId});
                                        details.append(key).append(" ");
                                        processCompletedAsyncJobs(key, ft);
                                        if (sessionId.contains(FUTUREMAP_INTERNALID)) {
                                            fmap.remove(x);
                                            alreadyRemoved = true;
                                        }
                                    }
                                    GregorianCalendar jobStartTime = new GregorianCalendar();
                                    GregorianCalendar currentTime = new GregorianCalendar();

                                    jobStartTime.setTime(aj.getStartTime());
                                    jobStartTime.add(Calendar.SECOND, TIMEOUT_SECONDS);
                                    if (jobStartTime.compareTo(currentTime) < 0 && alreadyRemoved == false) {
                                        ft.cancel(true);
                                        fmap.remove(x);
                                        logger.log(Level.INFO, "SessionId {0} async job {1} has timed out ({2} seconds )  and been cancelled.", new Object[]{key, sessionId, TIMEOUT_SECONDS});
                                    }

                                }
                            } catch (Exception e) {
                                logger.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete,  {0} async jobs for sessionId {1} have finished.Exception {2}", new Object[]{Integer.toString(y), sessionId, e});
                            }
                            if (y > 0) {
                                if (sessionId.contains(FUTUREMAP_INTERNALID) == false) {
                                    sendMessage(sessionId, "Asynchronous Tasks Completed", details.toString());
                                    logger.log(Level.INFO, "Notifying that {0} async jobs for sessionId {1} have finished. Deatils:{2}", new Object[]{Integer.toString(y), sessionId, details.toString()});

                                }
                            }

                        }

                    }
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete, Unhandled exception in EJB timer: {0} Cause: {2}", new Object[]{e.getMessage(), e.getCause().getMessage()});

            } finally {
                asychCheckProcessing.set(false);
                logger.log(Level.FINE, "Finished Checking Future Map for completed jobs.");
            }

        } else {
            logger.log(Level.INFO, "Future Map - skipping checkRunningJobsAndNotifyIfComplete as its still running.");
        }
    }
// run a schedules

    private void processConvertSchedule(Future<?> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfScheduledPayment result = null;
        boolean abort = false;
        try {
            result = (ArrayOfScheduledPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getScheduledPayment() != null) {
            List<ScheduledPayment> payList = result.getScheduledPayment();
            if (payList.isEmpty() == false) {
                String customerRef = payList.get(0).getYourSystemReference().getValue();
                int k = customerRef.indexOf('-');
                if (k > 0) {
                    customerRef = customerRef.substring(0, k);
                }
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processConvertSchedule an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        logger.log(Level.INFO, "Future Map processConvertSchedule. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                        for (ScheduledPayment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                logger.log(Level.WARNING, "Future Map processConvertSchedule . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            AsyncJob aj = new AsyncJob("ClearSchedule", paymentBean.clearSchedule(cust, false, cust.getUsername(), getDigitalKey()));
                            this.put(FUTUREMAP_INTERNALID, aj);
                            try {
                                Thread.sleep(250);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "Thread Interrupted", ex);
                            }
                            for (ScheduledPayment pay : payList) {

                                //payment doesn't exist in crm so add it
                                logger.log(Level.WARNING, "Future Map processConvertSchedule - A payment exists in the PGW but not in CRM.(This can be ignored if a customer is onboarded with the online eddr form) EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{pay.getEzidebitCustomerID().getValue(), pay.getYourSystemReference().getValue(), pay.getPaymentAmount().floatValue(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue()});
                                Payments crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                paymentsFacade.createAndFlush(crmPay);
                                crmPay.setPaymentReference(crmPay.getId().toString());
                                crmPay.setManuallyAddedPayment(false);
                                paymentsFacade.edit(crmPay);
                                Long amountLong = null;
                                try {
                                    amountLong = crmPay.getPaymentAmount().movePointRight(2).longValue();
                                } catch (Exception e) {
                                    logger.log(Level.WARNING, "Arithemtic error.");
                                }
                                try {
                                    Thread.sleep(250);
                                } catch (InterruptedException ex) {
                                    Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "Thread Sleep interrupted", ex);
                                }
                                paymentBean.addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay.getPaymentReference(), cust.getUsername(), getDigitalKey());

                            }

                        }
                    }
                } else {
                    logger.log(Level.SEVERE, "Future Map processConvertSchedule couldn't find a customer with our system ref from payment.");
                    /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                     as this means that a customer is in ezidebits system but not ours */

                }
            } else {
                logger.log(Level.WARNING, "Future Map processConvertSchedule our system ref in payment is null.");
            }

        }

        logger.log(Level.INFO,
                "processConvertSchedule completed");
    }

    public void processCompletedAsyncJobs(String key, Future<?> ft) {
        logger.log(Level.INFO, "Future Map is processing Completed Async Jobs .");
        synchronized (lock3) {

            try {

                if (key.contains("GetCustomerDetails")) {
                    processGetCustomerDetails(ft);
                }
                if (key.contains("GetPayments")) {
                    processGetPayments(ft);
                }
                if (key.contains("GetScheduledPayments")) {
                    processGetScheduledPayments(ft);
                }
                if (key.contains("PaymentReport")) {
                    processPaymentReport(ft);
                }
                if (key.contains("SettlementReport")) {
                    processSettlementReport(ft);
                }
                if (key.contains("ConvertSchedule")) {
                    processConvertSchedule(ft);
                }
            } catch (CancellationException ex) {
                logger.log(Level.WARNING, key + " Future Map:", ex);

            }
        }
    }

    // run a schedules
  /*  public void processCompletedAsyncJobs2(String sessionId) {
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
     }*/
    private void processSettlementReport(Future<?> ft) {
        synchronized (lock8) {
            logger.log(Level.INFO, "Future Map is processing the Settlement Report .");
            processReport(ft);
            logger.log(Level.INFO, "Future Map has finished asyc processing the Settlement Report .");
            settlementReportLock.set(false);
        }
    }

    private void processPaymentReport(Future<?> ft) {
        synchronized (lock7) {
            logger.log(Level.INFO, "Future Map is processing the Payment Report .");
            processReport(ft);
            logger.log(Level.INFO, "Future Map has finished async processing the Payment Report .");
            paymentReportLock.set(false);
        }
    }

    @Asynchronous
    private void processReport(Future<?> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment result = null;
        paymentsByCustomersMissingFromCRM = new ArrayList<>();
        try {
            result = (ArrayOfPayment) ft.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getPayment() != null) {
            List<Payment> payList = result.getPayment();
            if (payList.isEmpty() == false) {

                for (Payment pay : payList) {

                    String customerRef = pay.getYourSystemReference().getValue();
                    int k = customerRef.indexOf('-');
                    if (k > 0) {
                        customerRef = customerRef.substring(0, k);
                    }
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processReport an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }
                    Customers cust =null;
                    try {
                        cust = customersFacade.findById(custId);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Future Map processReport customersFacade.findById(custId) Error: custId={0}, Exception={1}",new Object[]{custId, e.getMessage()});
                    }
                    if (cust != null) {
                        String paymentID = pay.getPaymentID().getValue();
                        String paymentReference;
                        Payments crmPay = null;
                        int paymentRefInt = 0;
                        boolean validReference = false;
                        if (pay.getPaymentReference().isNil() == false) {
                            paymentReference = pay.getPaymentReference().getValue().trim();
                            if (paymentReference.contains("-") == false && paymentReference.length() > 0) {
                                try {
                                    paymentRefInt = Integer.parseInt(paymentReference);
                                    crmPay = paymentsFacade.findPaymentById(paymentRefInt);
                                    if (crmPay != null) {
                                        validReference = true;
                                    }
                                } catch (NumberFormatException numberFormatException) {
                                }
                            }
                        }
                        if (validReference) {
                            if (comparePaymentXMLToEntity(crmPay, pay) == false) {
                                crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                logger.log(Level.INFO, "Future Map processReport  - updating payment id:{0}.", paymentRefInt);
                                try {
                                    paymentsFacade.edit(crmPay);

                                } catch (Exception e) {
                                    logger.log(Level.WARNING, "Future Map processReport - edit payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});
                                }
                            }
                        } else {
                            // old payment without a primary key reference
                            if (paymentID.toUpperCase(Locale.getDefault()).contains("SCHEDULED")) {
                                // scheduled payment no paymentID
                                logger.log(Level.INFO, "Future Map processReport scheduled payment .", pay.toString());
                            } else {

                                crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);

                                if (crmPay != null) { //' payment exists
                                    if (comparePaymentXMLToEntity(crmPay, pay)) {
                                        // they are the same so no update
                                        logger.log(Level.FINE, "Future Map processReport paymenst are the same.");
                                    } else {
                                        crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                        try {
                                            paymentsFacade.edit(crmPay);

                                        } catch (Exception e) {
                                            logger.log(Level.WARNING, "Future Map processReport - edit payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});
                                        }
                                    }
                                } else { //payment doesn't exist in crm so add it
                                    logger.log(Level.SEVERE, "Future Map processReport  - payment doesn't exist in crm (this should only happen for webddr form schedule) so adding it:{0}.", paymentRefInt);
                                    //crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                    try {
                                        //paymentsFacade.createAndFlush(crmPay);
                                    } catch (Exception e) {
                                        logger.log(Level.WARNING, "Future Map processReport - create payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});

                                    }
                                }
                            }
                        }
                        /* String paymentID = pay.getPaymentID().getValue();
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
                         }*/

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
    private void processGetPayments(Future<?> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment result = null;
        boolean abort = false;
        try {
            result = (ArrayOfPayment) ft.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getPayment() != null) {
            List<Payment> payList = result.getPayment();
            if (payList.isEmpty() == false) {
                String customerRef = payList.get(0).getYourSystemReference().getValue();
                int k = customerRef.indexOf('-');
                if (k > 0) {
                    customerRef = customerRef.substring(0, k);
                }
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processGetPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        logger.log(Level.INFO, "Future Map processGetPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                        for (Payment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                logger.log(Level.WARNING, "Future Map processGetPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            for (Payment pay : payList) {
                                String paymentID = pay.getPaymentID().getValue();
                                String paymentReference;
                                Payments crmPay = null;
                                int paymentRefInt = 0;
                                boolean validReference = false;
                                if (pay.getPaymentReference().isNil() == false) {
                                    paymentReference = pay.getPaymentReference().getValue().trim();
                                    if (paymentReference.contains("-") == false && paymentReference.length() > 0) {
                                        try {
                                            paymentRefInt = Integer.parseInt(paymentReference);
                                            crmPay = paymentsFacade.findPaymentById(paymentRefInt);
                                            if (crmPay != null) {
                                                validReference = true;
                                            }
                                        } catch (NumberFormatException numberFormatException) {
                                        }
                                    }
                                }
                                if (validReference) {
                                    if (comparePaymentXMLToEntity(crmPay, pay) == false) {
                                        crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                        logger.log(Level.INFO, "Future Map processGetPayments  - updating payment id:{0}.", paymentRefInt);
                                        paymentsFacade.edit(crmPay);
                                    }
                                } else {
                                    // old payment without a primary key reference
                                    if (paymentID.toUpperCase(Locale.getDefault()).contains("SCHEDULED")) {
                                        // scheduled payment no paymentID
                                        logger.log(Level.INFO, "Future Map processGetPayments scheduled payment .", pay.toString());
                                    } else {

                                        crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);

                                        if (crmPay != null) { //' payment exists
                                            if (comparePaymentXMLToEntity(crmPay, pay)) {
                                                // they are the same so no update
                                                logger.log(Level.FINE, "Future Map processGetPayments paymenst are the same.");
                                            } else {
                                                crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                                paymentsFacade.edit(crmPay);
                                            }
                                        } else { //payment doesn't exist in crm so add it
                                            logger.log(Level.WARNING, "Future Map processGetPayments  - payment doesn't exist in crm (this should only happen for webddr form schedule) so adding it:{0}.", paymentRefInt);
                                            crmPay = convertPaymentXMLToEntity(null, pay, cust);
                                            paymentsFacade.createAndFlush(crmPay);
                                        }
                                    }
                                }
                            }
                        }
                    } else {
                        logger.log(Level.SEVERE, "Future Map processGetPayments couldn't find a customer with our system ref ({0}) from payment.", customerRef);
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
    private void processGetScheduledPayments(Future<?> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfScheduledPayment result = null;
        boolean abort = false;
        try {
            result = (ArrayOfScheduledPayment) ft.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        if (result != null && result.getScheduledPayment() != null) {
            List<ScheduledPayment> payList = result.getScheduledPayment();
            if (payList.isEmpty() == false) {
                String customerRef = payList.get(0).getYourSystemReference().getValue();
                int k = customerRef.indexOf('-');
                if (k > 0) {
                    customerRef = customerRef.substring(0, k);
                }
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        logger.log(Level.INFO, "Future Map processGetScheduledPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                        for (ScheduledPayment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                logger.log(Level.WARNING, "Future Map processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            for (ScheduledPayment pay : payList) {
                                Payments crmPay = null;
                                int id = -1;
                                try {
                                    id = Integer.parseInt(pay.getPaymentReference().getValue());
                                    crmPay = paymentsFacade.findPaymentById(id);
                                } catch (NumberFormatException numberFormatException) {
                                    logger.log(Level.INFO, "Future Map processGetScheduledPayments  - found a payment without a valid reference", id);
                                }

                                if (crmPay != null) {
                                    if (compareScheduledPaymentXMLToEntity(crmPay, pay)) {
                                        crmPay = convertScheduledPaymentXMLToEntity(crmPay, pay, cust);
                                        logger.log(Level.INFO, "Future Map processGetScheduledPayments  - updateing scheduled payment id:", id);
                                        paymentsFacade.edit(crmPay);
                                    }
                                } else {
                                    crmPay = paymentsFacade.findScheduledPaymentByCust(pay, cust);
                                    if (crmPay != null) { //' payment exists
                                        if (compareScheduledPaymentXMLToEntity(crmPay, pay)) {
                                            // they are the same so no update
                                            logger.log(Level.FINE, "Future Map processGetScheduledPayments paymenst are the same.");
                                        } else {
                                            crmPay = convertScheduledPaymentXMLToEntity(crmPay, pay, cust);
                                            paymentsFacade.edit(crmPay);
                                        }
                                    } else { //payment doesn't exist in crm so add it
                                        logger.log(Level.WARNING, "Future Map processGetScheduledPayments - A payment exists in the PGW but not in CRM.(This can be ignored if a customer is onboarded with the online eddr form) EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{pay.getEzidebitCustomerID().getValue(), pay.getYourSystemReference().getValue(), pay.getPaymentAmount().floatValue(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue()});
                                        crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                        paymentsFacade.createAndFlush(crmPay);
                                        crmPay.setPaymentReference(crmPay.getId().toString());
                                        crmPay.setManuallyAddedPayment(false);
                                        paymentsFacade.edit(crmPay);
                                        Long amountLong = null;
                                        try {
                                            amountLong = crmPay.getPaymentAmount().movePointRight(2).longValue();
                                        } catch (Exception e) {
                                            logger.log(Level.WARNING, "Arithemtic error.");
                                        }
                                        paymentBean.deletePayment(cust, crmPay.getDebitDate(), amountLong, "", cust.getUsername(), getDigitalKey());
                                        try {
                                            Thread.sleep(250);

                                        } catch (InterruptedException ex) {
                                            Logger.getLogger(FutureMapEJB.class
                                                    .getName()).log(Level.SEVERE, "Thread Sleep interrupted", ex);
                                        }
                                        paymentBean.addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay.getPaymentReference(), cust.getUsername(), getDigitalKey());
                                    }
                                }

                            }
                            // remove any that 
                            GregorianCalendar testCal = new GregorianCalendar();
                            testCal.add(Calendar.MINUTE, -5);
                            Date testDate = testCal.getTime();
                            List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust);
                            for (Payments p : crmPaymentList) {
                                boolean found = false;
                                for (ScheduledPayment pay : payList) {
                                    if (pay.getPaymentReference().isNil() == false) {
                                        String ref = pay.getPaymentReference().getValue().trim();
                                        String id = p.getId().toString().trim();
                                        if (id.equalsIgnoreCase(ref)) {
                                            found = true;
                                        }
                                    }
                                }
                                if (found == false) {
                                    //String ref = p.getId().toString();
                                    if (p.getCreateDatetime().before(testDate)) {// make sure we don't delate payments that have just been added and may still be being processed by the gateway. i.e they've been put into our DB but havn't been put into the payment gateway schedule yet
                                        p.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                                        paymentsFacade.edit(p);
                                    }
                                    //AsyncJob aj = new AsyncJob("DeletePayment", paymentBean.deletePaymentByRef(cust, ref, "system", getDigitalKey()));
                                    //this.put(FUTUREMAP_INTERNALID, aj);

                                }
                            }
                        }
                        updateNextScheduledPayment(cust);
                    } else {
                        logger.log(Level.SEVERE, "Future Map processGetScheduledPayments couldn't find a customer with our system ref from payment.");
                        /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                    }
                } else {
                    logger.log(Level.WARNING, "Future Map processGetScheduledPayments our system ref in payment is null.");
                }

            }
        }

        logger.log(Level.INFO, "Future Map processGetScheduledPayments completed");
    }

    /* @Asynchronous
     private void processGetScheduledPayments2(Future ft) {
     ArrayOfScheduledPayment result = null;
     boolean abort = false;
     int scheduledPayments;
     int existingInCRM;
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
     int k = customerRef.indexOf("-");
     if (k > 0) {
     customerRef = customerRef.substring(0, k);
     }
     if (customerRef.trim().isEmpty() == false) {
     int custId = 0;
     try {
     custId = Integer.parseInt(customerRef.trim());
     } catch (NumberFormatException numberFormatException) {
     logger.log(Level.WARNING, "Future Map processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

     }

     Customers cust = customersFacade.findById(custId);
     if (cust != null) {
     logger.log(Level.INFO, "Future Map processGetScheduledPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
     for (ScheduledPayment pay : payList) {
     if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
     logger.log(Level.WARNING, "Future Map processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
     abort = true;
     }

     }
     if (abort == false) {
     scheduledPayments = payList.size();

     List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(cust, PaymentStatus.SCHEDULED.value());
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

    /*  }
     }
     }
     }

     logger.log(Level.INFO, "processGetScheduledPayments completed");
     }
     }n */
    private void createDefaultPaymentParameters(Customers current) {

        if (current == null) {
            logger.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer is NULL.");
            return;
        }

        PaymentParameters pp;

        try {

            String phoneNumber = current.getTelephone();
            if (phoneNumber == null) {
                phoneNumber = "0000000000";
                logger.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", current.getUsername());
            }
            Pattern p = Pattern.compile("\\d{10}");
            Matcher m = p.matcher(phoneNumber);
            //ezidebit requires an australian mobile phone number that starts with 04
            if (m.matches() == false || phoneNumber.startsWith("04") == false) {
                phoneNumber = "0000000000";
                logger.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", current.getUsername());
            }
            pp = new PaymentParameters();
            pp.setId(0);
            pp.setWebddrUrl(null);
            pp.setLoggedInUser(current);
            pp.setLastSuccessfulScheduledPayment(paymentsFacade.findLastSuccessfulScheduledPayment(current));
            pp.setNextScheduledPayment(paymentsFacade.findNextScheduledPayment(current));
            pp.setAddressLine1("");
            pp.setAddressLine2("");
            pp.setAddressPostCode("");
            pp.setAddressState("");
            pp.setAddressSuburb("");
            pp.setContractStartDate(new Date());
            pp.setCustomerFirstName("");
            pp.setCustomerName("");
            pp.setEmail("");
            pp.setEzidebitCustomerID("");

            pp.setMobilePhoneNumber(phoneNumber);
            pp.setPaymentGatewayName("EZIDEBIT");
            pp.setPaymentMethod("");
            pp.setPaymentPeriod("");
            pp.setPaymentPeriodDayOfMonth("");
            pp.setPaymentPeriodDayOfWeek("");

            pp.setSmsExpiredCard("YES");
            pp.setSmsFailedNotification("YES");
            pp.setSmsPaymentReminder("NO");
            pp.setStatusCode("");
            pp.setStatusDescription("");
            pp.setTotalPaymentsFailed(0);
            pp.setTotalPaymentsFailedAmount(new BigDecimal(0));
            pp.setTotalPaymentsSuccessful(0);
            pp.setTotalPaymentsSuccessfulAmount(new BigDecimal(0));
            pp.setYourGeneralReference("");
            pp.setYourSystemReference("");
            paymentParametersFacade.create(pp);
            current.setPaymentParameters(pp);
            customersFacade.editAndFlush(current);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "createDefaultPaymentParameters Method in Customers Controller", e);
        }
    }

    @Asynchronous
    private void processGetCustomerDetails(Future<?> ft) {
        CustomerDetails custDetails = null;

        try {
            custDetails = (CustomerDetails) ft.get();

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Future Map processGetCustomerDetails", ex);
        }
        try {
            if (custDetails != null) {
                // do something with result
                String customerRef = custDetails.getYourSystemReference().getValue();
                int k = customerRef.indexOf('-');
                if (k > 0) {
                    customerRef = customerRef.substring(0, k);
                }
                if (customerRef.trim().isEmpty() == false) {
                    int custId = 0;
                    try {
                        custId = Integer.parseInt(customerRef.trim());
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number: {0}", custId);

                    }

                    Customers cust = null;
                    try {
                        cust = customersFacade.findById(custId);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "customersFacade.findById(custId) {0}.", new Object[]{custId, e.getMessage()});
                    }
                    if (cust != null) {
                        logger.log(Level.INFO, "Future Map processGetCustomerDetails. Processing details for customer {0}.", new Object[]{cust.getUsername()});

                        PaymentParameters pp = cust.getPaymentParameters();
                        boolean isNew = false;
                        if (pp == null) {
                            pp = new PaymentParameters();
                            pp.setId(0);
                            pp.setWebddrUrl(null);
                            pp.setLoggedInUser(cust);
                            isNew = true;
                        }
                        Payments p1 = null;
                        try {
                            p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Future Map processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                        }
                        Payments p2 = null;
                        try {
                            p2 = paymentsFacade.findNextScheduledPayment(cust);
                        } catch (Exception e) {
                            logger.log(Level.WARNING, "Future Map processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                        }
                        pp.setLastSuccessfulScheduledPayment(p1);
                        pp.setNextScheduledPayment(p2);
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
                        //pp.setPaymentPeriod(custDetails.getPaymentPeriod().getValue());
                        //pp.setPaymentPeriodDayOfMonth(custDetails.getPaymentPeriodDayOfMonth().getValue());
                        //pp.setPaymentPeriodDayOfWeek(custDetails.getPaymentPeriodDayOfWeek().getValue());

                        pp.setSmsExpiredCard(custDetails.getSmsExpiredCard().getValue());
                        pp.setSmsFailedNotification(custDetails.getSmsFailedNotification().getValue());
                        pp.setSmsPaymentReminder(custDetails.getSmsPaymentReminder().getValue());
                        pp.setStatusCode(custDetails.getStatusCode().getValue());
                        pp.setStatusDescription(custDetails.getStatusDescription().getValue());
                        pp.setTotalPaymentsFailed(custDetails.getTotalPaymentsFailed());
                        pp.setTotalPaymentsFailedAmount(new BigDecimal(custDetails.getTotalPaymentsFailed()));
                        pp.setTotalPaymentsSuccessful(custDetails.getTotalPaymentsSuccessful());
                        pp.setTotalPaymentsSuccessfulAmount(new BigDecimal(custDetails.getTotalPaymentsSuccessfulAmount()));
                        pp.setYourGeneralReference(custDetails.getYourGeneralReference().getValue());
                        pp.setYourSystemReference(custDetails.getYourSystemReference().getValue());

                        if (isNew) {
                            paymentParametersFacade.create(pp);
                            cust.setPaymentParameters(pp);
                        } else {
                            paymentParametersFacade.edit(pp);
                            cust.setPaymentParameters(pp);
                        }
                        customersFacade.edit(cust);

                    } else {
                        logger.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number or the customer ID does not exist");
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Future Map processGetCustomerDetails FAILED", e);
        }
        logger.log(Level.INFO, "Future Map processGetCustomerDetails completed");
    }

    private void updateNextScheduledPayment(Customers cust) {
        if (cust != null) {
            logger.log(Level.INFO, "updateNextScheduledPayment. Processing details for customer {0}.", new Object[]{cust.getUsername()});
           PaymentParameters pp = cust.getPaymentParameters();
            if (pp != null) {
               Payments p1 = null;
                try {
                    p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Future Map processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                }
                Payments p2 = null;
                try {
                    p2 = paymentsFacade.findNextScheduledPayment(cust);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Future Map processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                }
                pp.setLastSuccessfulScheduledPayment(p1);
                pp.setNextScheduledPayment(p2);
                 paymentParametersFacade.edit(pp);
                cust.setPaymentParameters(pp);
                customersFacade.edit(cust);
            }

        } else {
            logger.log(Level.WARNING, "Future Map updateNextScheduledPayment  customer paymnt parameters  does not exist");
        }
    }

    private Payments convertPaymentXMLToEntity(Payments payment, Payment pay, Customers cust) {
        synchronized (lock4) {
            if (pay == null || cust == null) {
                String p1 = "NULL";
                String p2 = "NULL";
                String p3 = "NULL";
                if (payment != null) {
                    p1 = payment.getYourSystemReference();
                }
                if (cust != null) {
                    p3 = cust.getUsername();
                }
                if (pay != null) {
                    if (pay.getYourSystemReference().isNil() == false) {
                        p2 = pay.getYourSystemReference().getValue();
                    } else {
                        logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.Your system reference is NULL. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});

                    }
                }

                logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.Cant poceed due to a null value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
                return null;
            }
            if (payment == null) {
                payment = new Payments();
                payment.setCreateDatetime(new Date());
                payment.setManuallyAddedPayment(false);
                payment.setId(0);
                payment.setCustomerName(cust);
            }
            try {

                payment.setLastUpdatedDatetime(new Date());

                if (pay.getBankFailedReason().isNil() == false) {
                    payment.setBankFailedReason(pay.getBankFailedReason().getValue());
                }
                if (pay.getBankReceiptID().isNil() == false) {
                    payment.setBankReceiptID(pay.getBankReceiptID().getValue());
                }
                if (pay.getBankReturnCode().isNil() == false) {
                    payment.setBankReturnCode(pay.getBankReturnCode().getValue());
                }
                //payment.setCustomerName(cust);

                try {
                    payment.setDebitDate(pay.getDebitDate().getValue().toGregorianCalendar().getTime());
                } catch (NullPointerException e) {
                    logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity - DebitDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
                }

                if (pay.getEzidebitCustomerID().isNil() == false) {
                    payment.setEzidebitCustomerID(pay.getEzidebitCustomerID().getValue());
                }
                if (pay.getInvoiceID().isNil() == false) {
                    payment.setInvoiceID(pay.getInvoiceID().getValue());
                }
                if (pay.getPaymentAmount() != null) {
                    payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount()));
                }
                if (pay.getPaymentID().isNil() == false) {
                    payment.setPaymentID(pay.getPaymentID().getValue());
                }
                if (pay.getPaymentMethod().isNil() == false) {
                    payment.setPaymentMethod(pay.getPaymentMethod().getValue());
                }
                if (pay.getPaymentReference().isNil() == false) {
                    if (pay.getPaymentReference().getValue().trim().isEmpty()) {
                        payment.setPaymentReference(null);
                    } else {
                        payment.setPaymentReference(pay.getPaymentReference().getValue());
                    }
                } else {
                    payment.setPaymentReference(null);
                }
                /* if (pay.getPaymentReference().isNil() == false) {
                 if (pay.getPaymentReference().getValue().trim().isEmpty() == false) {
                 payment.setManuallyAddedPayment(true);
                 } else {
                 payment.setManuallyAddedPayment(false);
                 }
                 }*/
                if (pay.getPaymentSource().isNil() == false) {
                    payment.setPaymentSource(pay.getPaymentSource().getValue());
                }
                if (pay.getScheduledAmount() != null) {
                    payment.setScheduledAmount(new BigDecimal(pay.getScheduledAmount()));
                }
                try {
                    payment.setSettlementDate(pay.getSettlementDate().getValue().toGregorianCalendar().getTime());
                } catch (NullPointerException e) {
                    logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity - SettlementDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
                }
                if (pay.getPaymentStatus().isNil() == false) {
                    payment.setPaymentStatus(pay.getPaymentStatus().getValue());
                }
                if (pay.getTransactionFeeClient() != null) {
                    payment.setTransactionFeeClient(new BigDecimal(pay.getTransactionFeeClient()));
                }
                if (pay.getTransactionFeeCustomer() != null) {
                    payment.setTransactionFeeCustomer(new BigDecimal(pay.getTransactionFeeCustomer()));
                }

                try {
                    payment.setTransactionTime(pay.getTransactionTime().getValue().toGregorianCalendar().getTime()); // only valid for real time and credit card payments
                } catch (NullPointerException e) {
                    logger.log(Level.INFO, "Future Map convertPaymentXMLToEntity - TransactionTime is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
                }
                if (pay.getYourGeneralReference().isNil() == false) {
                    payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
                }
                if (pay.getYourSystemReference().isNil() == false) {
                    payment.setYourSystemReference(pay.getYourSystemReference().getValue());
                }
            } catch (RuntimeException e) {
                throw e;
            } catch (Exception e) {
                logger.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed. Customer: {2},Error Message: {0},payment XML: {1}:", new Object[]{e.getMessage(), pay.toString(), cust.getUsername()});
            }

            return payment;
        }
    }

    private Payments convertScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay, Customers cust) {
        synchronized (lock5) {
            if (pay == null || cust == null) {
                String p1 = "NULL";
                String p2 = "NULL";
                String p3 = "NULL";
                if (payment != null) {
                    p1 = payment.getYourSystemReference();
                }
                if (pay != null) {
                    p2 = pay.getYourSystemReference().getValue();
                }
                if (cust != null) {
                    p3 = cust.getUsername();
                }
                logger.log(Level.WARNING, "Future Map convertScheduledPaymentXMLToEntity method failed due to a NULL value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
                return null;
            }
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
                payment.setInvoiceID("");
                payment.setPaymentAmount(new BigDecimal(pay.getPaymentAmount().toString()));
                payment.setPaymentID(null);
                payment.setPaymentMethod("DR");
                if (pay.getPaymentReference().isNil() == false) {
                    if (pay.getPaymentReference().getValue().trim().isEmpty()) {
                        payment.setPaymentReference(null);
                    } else {
                        payment.setPaymentReference(pay.getPaymentReference().getValue());
                    }
                } else {
                    payment.setPaymentReference(null);
                }

                /* if (pay.isManuallyAddedPayment() != null) {

                 payment.setManuallyAddedPayment(pay.isManuallyAddedPayment());

                 }*/
                payment.setPaymentSource("SCHEDULED");
                payment.setScheduledAmount(new BigDecimal(pay.getPaymentAmount()));

                payment.setSettlementDate(null);
                payment.setPaymentStatus(PaymentStatus.SCHEDULED.value());
                payment.setTransactionFeeClient(new BigDecimal(0));
                payment.setTransactionFeeCustomer(new BigDecimal(0));

                payment.setTransactionTime(null); // only valid for real time and credit card payments

                payment.setYourGeneralReference(pay.getYourGeneralReference().getValue());
                payment.setYourSystemReference(pay.getYourSystemReference().getValue());
            } catch (Exception e) {
                logger.log(Level.WARNING, "Future Map convertScheduledPaymentXMLToEntity method failed.:", e);
            }

            return payment;

        }
    }

    private synchronized boolean compareStringToXMLString(String s, JAXBElement<String> xs) {
        boolean result = true;// return true if they are the same
        String s2 = null;
        if (s != null) {
            s = s.trim();
            if (s.isEmpty()) {
                s = null;
            }
        }
        if (xs != null) {
            if (xs.isNil() == false) {
                s2 = xs.getValue().trim();
                if (s2.isEmpty()) {
                    s2 = null;
                }
            } else {
                s2 = null;
            }
        }
        if (s == null && s2 == null) {
            return true;
        }
        if ((s == null && s2 != null) || (s != null && s2 == null)) {
            return false;
        }

        if (s.compareTo(s2) != 0) {
            return false;
        }
        return result;
    }

    private synchronized boolean compareDateToXMLGregCalendar(Date d, XMLGregorianCalendar xgc) {
        boolean result = true;// return true if they are the same
        GregorianCalendar gc = null;
        if (xgc != null) {
            if (xgc.toGregorianCalendar() != null) {
                gc = xgc.toGregorianCalendar();
            }
        }

        if (d == null && gc == null) {
            return true;
        }
        if ((d == null && gc != null) || (d != null && gc == null)) {
            return false;
        }
        Date d2 = gc.getTime();
        if (d.compareTo(d2) != 0) {
            return false;
        }
        return result;
    }

    private synchronized boolean compareDateToXMLGregCal(Date d, JAXBElement<XMLGregorianCalendar> jxgc) {
        boolean result = true;// return true if they are the same
        GregorianCalendar xgc = null;
        if (jxgc != null) {
            if (jxgc.isNil() == false) {
                xgc = jxgc.getValue().toGregorianCalendar();
            }
        }

        if (d == null && xgc == null) {
            return true;
        }
        if ((d == null && xgc != null) || (d != null && xgc == null)) {
            return false;
        }
        Date d2 = xgc.getTime();
        if (d.compareTo(d2) != 0) {
            return false;
        }
        return result;
    }

    private synchronized boolean compareBigDecimalToDouble(BigDecimal d, Double bd) {
        boolean result = true;// return true if they are the same
        BigDecimal d2 = null;
        if (bd != null) {
            d2 = new BigDecimal(bd);
        }

        if (d == null && d2 == null) {
            return true;
        }
        if ((d == null && d2 != null) || (d != null && d2 == null)) {
            return false;
        }

        if (d.compareTo(d2) != 0) {
            return false;
        }
        return result;
    }

    private boolean comparePaymentXMLToEntity(Payments payment, Payment pay) {
        synchronized (lock6) {
            if (payment == null || pay == null) {
                return payment == null && pay == null;
            }
            try {

                if (compareStringToXMLString(payment.getBankFailedReason(), pay.getBankFailedReason()) == false) {
                    return false;
                }

                if (compareStringToXMLString(payment.getBankReceiptID(), pay.getBankReceiptID()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getBankReturnCode(), pay.getBankReturnCode()) == false) {
                    return false;
                }
                if (payment.getCustomerName() == null) {
                    return false;
                } else {
                    if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                        return false;
                    }
                }
                if (compareDateToXMLGregCal(payment.getDebitDate(), pay.getDebitDate()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getInvoiceID(), pay.getInvoiceID()) == false) {
                    return false;
                }
                if (compareBigDecimalToDouble(payment.getPaymentAmount(), pay.getPaymentAmount()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getPaymentID(), pay.getPaymentID()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getPaymentMethod(), pay.getPaymentMethod()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getPaymentReference(), pay.getPaymentReference()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getPaymentSource(), pay.getPaymentSource()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getPaymentStatus(), pay.getPaymentStatus()) == false) {
                    return false;
                }
                if (compareBigDecimalToDouble(payment.getScheduledAmount(), pay.getScheduledAmount()) == false) {
                    return false;
                }
                if (compareDateToXMLGregCal(payment.getSettlementDate(), pay.getSettlementDate()) == false) {
                    return false;
                }
                if (compareBigDecimalToDouble(payment.getTransactionFeeClient(), pay.getTransactionFeeClient()) == false) {
                    return false;
                }
                if (compareBigDecimalToDouble(payment.getTransactionFeeCustomer(), pay.getTransactionFeeCustomer()) == false) {
                    return false;
                }
                if (compareDateToXMLGregCal(payment.getTransactionTime(), pay.getTransactionTime()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getYourGeneralReference(), pay.getYourGeneralReference()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference()) == false) {
                    return false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Future Map comparePaymentXMLToEntity method failed.:", e.getMessage());
                return false;
            }

            return true;
        }
    }

    private boolean compareScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay) {
        synchronized (lock6) {
            if (payment == null || pay == null) {
                return payment == null && pay == null;
            }
            try {

                if (payment.getCustomerName() == null) {
                    return false;
                } else {
                    if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                        return false;
                    }
                }
                if (compareDateToXMLGregCalendar(payment.getDebitDate(), pay.getPaymentDate()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getEzidebitCustomerID(), pay.getEzidebitCustomerID()) == false) {
                    return false;
                }
                if (payment.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value()) == false) {
                    return false;
                }

                if (compareBigDecimalToDouble(payment.getPaymentAmount(), pay.getPaymentAmount()) == false) {
                    return false;
                }
                /* if (!Objects.equals(payment.getManuallyAddedPayment(), pay.isManuallyAddedPayment())) {
                 return false;
                 }*/

                if (compareStringToXMLString(payment.getPaymentReference(), pay.getPaymentReference()) == false) {
                    return false;
                }

                if (compareStringToXMLString(payment.getYourGeneralReference(), pay.getYourGeneralReference()) == false) {
                    return false;
                }
                if (compareStringToXMLString(payment.getYourSystemReference(), pay.getYourSystemReference()) == false) {
                    return false;
                }
            } catch (Exception e) {
                logger.log(Level.WARNING, "Future Map compareScheduledPaymentXMLToEntity method failed.:", e.getMessage());
                return false;
            }

            return true;
        }
    }

    private void sanityCheckCustomersForDefaultItems() {
        logger.log(Level.INFO, "Performing Sanity Checks on Customers");
        if (customersFacade != null) {
            List<Customers> cl = customersFacade.findAll();
            if (cl != null) {
                for (Customers c : cl) {
                    if (c.getProfileImage() == null) {
                        createDefaultProfilePic(c);
                    }
                    if (c.getPaymentParameters() == null) {
                        createDefaultPaymentParameters(c);
                    }
                }
                logger.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
            } else {
                logger.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers. Could not get the list of customers from the DB");
            }
        } else {
            logger.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers.Customer Facade null. HAs it been initialised yet?");
        }
    }

    private void createDefaultProfilePic(Customers cust) {
        String placeholderImage = configMapFacade.getConfig("system.default.profile.image");
        String fileExtension = placeholderImage.substring(placeholderImage.lastIndexOf('.')).toLowerCase(Locale.getDefault());
        int imgType = -1;
        if (fileExtension.contains("jpeg") || fileExtension.contains("jpg")) {
            imgType = 2;
            fileExtension = "jpeg";
        }
        if (fileExtension.contains("png")) {
            imgType = 1;
            fileExtension = "png";
        }
        if (fileExtension.contains("gif")) {
            imgType = 0;
            fileExtension = "gif";
        }
        if (imgType == -1) {
            logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due the picture not being in jpeg, gif or png. resource:{0}", new Object[]{placeholderImage, cust.getUsername()});
            return;
        }
        if (cust != null) {
            if (cust.getProfileImage() == null) {
                CustomerImages ci;
                BufferedImage img;
                //InputStream stream;
                try {
                    ci = new CustomerImages(0);
                    img = null;
                    //FacesContext context =  FacesContext.getCurrentInstance();
                    //String servPath = context.getExternalContext().getRequestServletPath() + placeholderImage;
                    //stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(servPath) ;
                    try {
                        //img = ImageIO.read(stream);
                        img = ImageIO.read(new URL(placeholderImage));

                    } catch (IOException e) {
                        if (e.getCause().getClass() == FileNotFoundException.class) {
                            Logger.getLogger(CustomerImagesController.class
                                    .getName()).log(Level.SEVERE, "createDefaultProfilePic, File not found!!: {0}", placeholderImage);

                        } else {
                            Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, Loading image into buffer error!!", e);
                        }
                    }

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {

                        ImageIO.write(img, fileExtension, os);

                    } catch (IOException ex) {

                        Logger.getLogger(CustomerImagesController.class
                                .getName()).log(Level.SEVERE, "createDefaultProfilePic, write image  error!!", ex);

                    }

                    ci.setImage(os.toByteArray());
                    ci.setImageType(imgType);
                    ci.setCustomers(cust);
                    ci.setCustomerId(cust);
                    ci.setDatetaken(new Date());

                    ejbCustomerImagesFacade.edit(ci);
                    cust.setProfileImage(ci);
                    customersFacade.edit(cust);
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due to an exception:{0}", new Object[]{e, cust.getUsername()});

                }
            }
        } else {
            logger.log(Level.WARNING, "createDefaultProfilePic ERROR, Cannot add default profile pic to a null customer object");
        }
    }

}
