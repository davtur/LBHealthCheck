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
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.io.Serializable;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import static javax.ejb.ConcurrencyManagementType.BEAN;
import javax.ejb.EJBException;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.faces.application.FacesMessage;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBus.Reply;
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
@ConcurrencyManagement(BEAN)
@Singleton
@LocalBean
@Startup
public class FutureMapEJB implements Serializable {

    private static final Logger logger = Logger.getLogger(FutureMapEJB.class.getName());
    private static final int TIMEOUT_SECONDS = 300;
    private static final int PAYMENT_SEARCH_MONTHS_AHEAD = 18;
    private static final int PAYMENT_SEARCH_MONTHS_BEHIND = 3;
    private static final String FUTUREMAP_INTERNALID = "FMINTID876987";
    private static final long serialVersionUID = 1L;

    private final ConcurrentHashMap<String, ArrayList<AsyncJob>> futureMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ArrayList<PaymentGatewayResponse>> componentsToUpdate = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ArrayList<BatchOfPaymentJobs>> batchJobs = new ConcurrentHashMap<>();
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

    /*   @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "Application Setup Running");
        try {
            sanityCheckCustomersForDefaultItems();

        } catch (Exception e) {
            logger.log(Level.SEVERE, " @PostConstruct Future Map - applicationSetup(). Exception in sanityCheckCustomersForDefaultItems: ", e);
        }
        logger.log(Level.INFO, "Application Setup Completed");
    }*/
    /**
     * @return the FUTUREMAP_INTERNALID
     */
    public synchronized String getFutureMapInternalSessionId() {
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

    /**
     * @param userSessionId
     * @return the componentsToUpdate
     */
    public ArrayList<PaymentGatewayResponse> getComponentsToUpdate(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        synchronized (lock1) {
            logger.log(Level.FINE, "componentsToUpdate Map.  for sessionID {0}.", userSessionId);

            ArrayList<PaymentGatewayResponse> fmap = componentsToUpdate.get(userSessionId);
            if (fmap == null) {
                logger.log(Level.INFO, "Get componentsToUpdate. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

                componentsToUpdate.put(userSessionId, new ArrayList<>());
                fmap = componentsToUpdate.get(userSessionId);

            }
            return fmap;

        }
    }

    /**
     * @param userSessionId
     * @return the componentsToUpdate
     */
    public ArrayList<BatchOfPaymentJobs> getBatchJobs(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        synchronized (lock2) {
            logger.log(Level.FINE, "batchJobs Map.  for sessionID {0}.", userSessionId);

            ArrayList<BatchOfPaymentJobs> fmap = batchJobs.get(userSessionId);
            if (fmap == null) {
                logger.log(Level.INFO, "Get batchJobs. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

                batchJobs.put(userSessionId, new ArrayList<>());
                fmap = batchJobs.get(userSessionId);

            }
            return fmap;

        }
    }

    /**
     * @param userSessionId
     *
     *
     * @param aj
     *
     */
    public void addComponentToUpdatesList(String userSessionId, PaymentGatewayResponse aj) {
        synchronized (lock1) {
            try {
                logger.log(Level.INFO, "addComponentToUpdatesList, put. sessionid {0},Component To Update {1}.", new Object[]{userSessionId, aj});
                getComponentsToUpdate(userSessionId).add(aj);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "addComponentToUpdatesList put(String userSessionId, String aj) method. Unable to add component to update list, Session:{1}, component Name:{2}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, aj});
            }
        }
    }

    /**
     * @param userSessionId
     *
     *
     * @param bj
     *
     */
    public void addBatchJobToList(String userSessionId, BatchOfPaymentJobs bj) {
        synchronized (lock2) {
            try {
                logger.log(Level.INFO, "addComponentToUpdatesList, put. sessionid {0},Component To Update {1}.", new Object[]{userSessionId, bj});
                getBatchJobs(userSessionId).add(bj);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "addComponentToUpdatesList put(String userSessionId, String aj) method. Unable to add component to update list, Session:{1}, component Name:{2}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, bj});
            }
        }
    }

    public void clearComponentUpdates(String userSessionId) {
        synchronized (lock1) {
            if (getComponentsToUpdate(userSessionId) != null) {
                getComponentsToUpdate(userSessionId).clear();
            }
        }
    }

    public void clearBatchJobs(String userSessionId) {
        synchronized (lock2) {
            if (getBatchJobs(userSessionId) != null) {
                getBatchJobs(userSessionId).clear();
            }
        }
    }

    private void storeResponseForSessionBeenToRetrieve(String operationName, String sessionId, PaymentGatewayResponse pgr) {
        if (operationName != null && operationName.trim().isEmpty() == false) {
            // no point storing it if the operation name is empty as it will have no effect in the session bean
            pgr.setOperationName(operationName);
            addComponentToUpdatesList(sessionId, pgr);
        }
    }

    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    public synchronized boolean runSettlementReport(Date fromDate) {
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

    public synchronized boolean runPaymentReport(Date fromDate) throws InterruptedException {
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

    private synchronized void refreshAllCustomersDetailsFromGateway() {
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

    @TransactionAttribute(TransactionAttributeType.NEVER)
    @Asynchronous
    public void sendMessage(String sessionChannel, String summary, String detail) {
        //TODO
        // sessionChannel = "/test";// remove this once the channel is dynamically set by session id
        // synchronized (lock2) {
        if (sessionChannel.contains(FUTUREMAP_INTERNALID) == false) {// we don't want to send a message unless there is a session to send it to
            final String broadcastChannel = CHANNEL + sessionChannel;
            // final String summ = summary;
            EventBus eventBus = EventBusFactory.getDefault().eventBus();
            //Reply rep = (String message) -> {
           //     logger.log(Level.INFO, "Message Delivered:Channel={0}, Summary={1}.", new Object[]{broadcastChannel, summary});
           // };
            // eventBus.publish(channels.getChannel(getUser()), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
            eventBus.publish(broadcastChannel, new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
            logger.log(Level.INFO, "Sending Async Message, summary:{0}, details:{1}", new Object[]{summary, detail});
        }else{
            logger.log(Level.INFO, "NOT Sending Async Message as the session is internal, summary:{0}, details:{1}", new Object[]{summary, detail});
        }
        //   }
        
    }

    //@Schedule(dayOfMonth = "*", hour = "*", minute = "*", second = "0")//debug
    @Schedule(dayOfMonth = "*", hour = "6", minute = "0", second = "0")
    public  void retrievePaymentsReportFromPaymentGateway(Timer t) {
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

    public synchronized boolean isAnAsyncOperationRunning(String sessionId) {

        return !getFutureMap(sessionId).isEmpty();

    }

    @Schedule(hour = "*", minute = "*", second = "*")
    @TransactionAttribute(TransactionAttributeType.NEVER)// we don't want a transaction for this method as the calls within this method will invoke their own transactions
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 1 seconds
        long start = new Date().getTime();

        // if (asychCheckProcessing.get() == false) {
        //  if (   futureMap.isEmpty() == false){
        logger.log(Level.FINE, "Checking Future Map for completed jobs.");
        //asychCheckProcessing.set(true);
        try {
            //  synchronized (futureMapArrayLock) {
            String temp = "";
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
                            Future<PaymentGatewayResponse> ft = aj.getFuture();
                            String key = aj.getJobName();
                            temp += key;
                            if (ft.isDone()) {
                                y++;
                                logger.log(Level.INFO, "SessionId {0} Future Map async job {1} has finished.", new Object[]{key, sessionId});
                                details.append(key).append(" ");

                                processCompletedAsyncJobs(sessionId, key, ft);
                                if (aj.getBatchId() != 0) {
                                    //this is part of a batch job
                                    ArrayList<BatchOfPaymentJobs> batchJobsForThisSession = getBatchJobs(sessionId);
                                    for (int w = batchJobsForThisSession.size() - 1; w >= 0; w--) {

                                        BatchOfPaymentJobs bj = batchJobsForThisSession.get(w);

                                        if (bj.getBatchId() == aj.getBatchId()) {

                                            ArrayList<Integer> jobIds = bj.getJobs();
                                            int z = jobIds.size() - 1;
                                            jobIds.remove(z);
                                            /* for (int z = jobIds.size() - 1; z >= 0; z--) {
                                                int id = jobIds.get(z);
                                                if (id == aj.getBatchId()) {
                                                    // remove this id from the batch
                                                    jobIds.remove(z);
                                                }
                                            }*/
                                            if (jobIds.isEmpty()) {
                                                batchJobsForThisSession.remove(bj);
                                                processCompletedAsyncJobs(sessionId, bj.getJobName(), ft);

                                            }

                                        }
                                    }

                                }

                                //if (sessionId.contains(FUTUREMAP_INTERNALID)) {
                                //NOTE: we dont want to remove any futures that still require to update the components on the web page for the user.
                                // once we send an ajax message the session bean will process components and remove the Future from the map.
                                fmap.remove(x);
                                alreadyRemoved = true;
                                //}
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
                    /*   if (y > 0) {
                                if (sessionId.contains(FUTUREMAP_INTERNALID) == false) {
                                    // hack until the migration is complete
                                    if (temp.contains("GetCustomerDetails") || temp.contains("GetPayments") || temp.contains("GetScheduledPayments") || temp.contains("PaymentReport") || temp.contains("SettlementReport") || temp.contains("AddCustomer") || temp.contains("AddPayment")) {
                                        // ignore these as they send their own messages, TODO finish moving the rest of teh async jobs to this class. 
                                    } else {
                                        // legacy jobs that need to be moved to this class
                                        sendMessage(sessionId, "Asynchronous Tasks Completed", details.toString());
                                        logger.log(Level.INFO, "Notifying that {0} async jobs for sessionId {1} have finished. Deatils:{2}", new Object[]{Integer.toString(y), sessionId, details.toString()});
                                    }
                                }
                            }*/

                }

            }
            // }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            logger.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete, Unhandled exception in EJB timer: {0} Cause: {2}", new Object[]{e.getMessage(), e.getCause().getMessage()});

        } finally {
            asychCheckProcessing.set(false);
            logger.log(Level.FINE, "Finished Checking Future Map for completed jobs.");
        }

        /*  } else {
            logger.log(Level.INFO, "Future Map - skipping checkRunningJobsAndNotifyIfComplete as its still running.");
        }*/
        counter++;
        if (counter > 300) {
            long finish = new Date().getTime();
            long durationInMilli = finish - start;
            logger.log(Level.INFO, "EJB Timer Heartbeat (5 minute interval) - checkRunningJobsAndNotifyIfCompleted, duration in milliseconds={0}", new Object[]{durationInMilli});
            counter = 0;
        }
    }
// run a schedules

    @Asynchronous
    private void processConvertSchedule(Future<PaymentGatewayResponse> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        // synchronized (lock9) {

        ArrayOfScheduledPayment resultArraySchedPayments = null;
        boolean abort = false;
        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject != null) {
                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    PaymentGatewayResponse pgr = (PaymentGatewayResponse) resultObject;
                    if (pgr.isOperationSuccessful()) {
                        resultArraySchedPayments = (ArrayOfScheduledPayment) pgr.getData();
                    }
                }
            }

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Future Map processConvertSchedule:", ex);
        }

        if (resultArraySchedPayments != null && resultArraySchedPayments.getScheduledPayment() != null) {
            List<ScheduledPayment> payList = resultArraySchedPayments.getScheduledPayment();
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
                                paymentBean.addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay, cust.getUsername(), getDigitalKey());

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
        // }
    }

    @TransactionAttribute(TransactionAttributeType.NEVER)
    public void processCompletedAsyncJobs(String sessionId, String key, Future<PaymentGatewayResponse> ft) {
        logger.log(Level.INFO, "Future Map is processing Completed Async Jobs .");
        synchronized (lock3) {
// TODO convert all methods to use PaymentGatewayResponse class
            try {

                if (key.contains("GetCustomerDetails")) {
                    processGetCustomerDetails(sessionId, ft);
                }
                if (key.contains("GetPayments")) {
                    processGetPayments(sessionId, ft);
                }
                if (key.contains("GetScheduledPayments")) {
                    processGetScheduledPayments(sessionId, ft);
                }
                if (key.contains("PaymentReport")) {
                    processPaymentReport(sessionId, ft);
                }
                if (key.contains("SettlementReport")) {
                    processSettlementReport(sessionId, ft);
                }
                // this is only run by the system so sessionId is not needed.
                if (key.contains("ConvertSchedule")) {
                    processConvertSchedule(ft);
                }
                if (key.contains("AddCustomer")) {
                    processAddCustomer(sessionId, ft);
                }
                if (key.contains("AddPayment")) {
                    processAddPaymentResult(sessionId, ft);
                }
                if (key.contains("ClearSchedule")) {
                    processClearSchedule(sessionId, ft);
                }
                if (key.contains("CreateSchedule")) {
                    processCreateSchedule(sessionId, ft);
                }
                if (key.contains("ChangeCustomerStatus")) {
                    processChangeCustomerStatus(sessionId, ft);
                }

                if (key.contains("ChangeScheduledAmount")) {
                    processChangeScheduledAmount(sessionId, ft);
                }
                if (key.contains("ChangeScheduledDate")) {
                    processChangeScheduledDate(sessionId, ft);
                }
                if (key.contains("DeletePayment")) {
                    processDeletePayment(sessionId, ft);
                }
                if (key.contains("DeletePaymentBatch")) {
                    processDeletePaymentBatch(sessionId, ft);
                }
                if (key.contains("AddPaymentBatch")) {
                    processAddPaymentBatch(sessionId, ft);
                }
                if (key.contains("GetPaymentExchangeVersion")) {
                    processGetPaymentExchangeVersion(sessionId, ft);
                }

                if (key.contains("GetPaymentStatus")) {
                    processGetPaymentStatus(sessionId, ft);
                }
                if (key.contains("IsBsbValid")) {
                    processIsBsbValid(sessionId, ft);
                }
                if (key.contains("IsSystemLocked")) {
                    processIsSystemLocked(sessionId, ft);
                }

            } catch (Exception ex) {
                logger.log(Level.WARNING, "Key:" + key + ", Future Map: processCompletedAsyncJobs", ex);

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
    private void processSettlementReport(String sessionId, Future<?> ft) {
        synchronized (lock8) {
            logger.log(Level.INFO, "Future Map is processing the Settlement Report .");
            processReport(sessionId, ft);
            logger.log(Level.INFO, "Future Map has finished asyc processing the Settlement Report .");
            settlementReportLock.set(false);
        }
    }

    private void processPaymentReport(String sessionId, Future<?> ft) {
        synchronized (lock7) {
            logger.log(Level.INFO, "Future Map is processing the Payment Report .");
            processReport(sessionId, ft);
            logger.log(Level.INFO, "Future Map has finished async processing the Payment Report .");
            paymentReportLock.set(false);
        }
    }

    private synchronized void processGetPaymentStatus(String sessionId, Future<?> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        try {
            Object resultObject = ft.get();
            if (resultObject != null) {

                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    pgr = (PaymentGatewayResponse) resultObject;
                    result = pgr.isOperationSuccessful();
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            sendMessage(sessionId, "Payment Gateway", "Successfully Retrieved Payment Status  .");

        } else {
            sendMessage(sessionId, "Payment Gateway", "The getPayment status operation failed!.");
        }
        //recreatePaymentTableData();
        logger.log(Level.INFO, "processGetPaymentStatus completed");
    }

    private synchronized void processChangeScheduledAmount(String sessionId, Future<?> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("ChangeScheduledAmount", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully  Changed Scheduled Amount .");

            getPayments(sessionId, cust, 18, 2);
        } else {
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        //recreatePaymentTableData();
        logger.log(Level.INFO, "processChangeScheduledAmount completed");
    }

    private synchronized void processChangeScheduledDate(String sessionId, Future<?> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("ChangeScheduledDate", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully Changed Scheduled Date  .");
            getPayments(sessionId, cust, 18, 2);
        } else {
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processChangeScheduledDate completed");
    }

    private synchronized void processIsBsbValid(String sessionId, Future<?> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("IsBsbValid", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully Checked BSB  .");

        } else {
            storeResponseForSessionBeenToRetrieve("IsBsbValid", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processIsBsbValid completed");
    }

    private synchronized void processIsSystemLocked(String sessionId, Future<?> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("IsSystemLocked", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully checked if System is Locked  .");

        } else {
            storeResponseForSessionBeenToRetrieve("IsSystemLocked", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processIsSystemLocked completed");
    }

    private synchronized void processGetPaymentExchangeVersion(String sessionId, Future<?> ft) {
        boolean result = false;
        String versionInfo = "";
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                versionInfo = pgr.getTextData();
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (versionInfo != null && versionInfo.trim().isEmpty() == false) {
            storeResponseForSessionBeenToRetrieve("GetPaymentExchangeVersion", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Payment Exchange Version: " + versionInfo);

        } else {

            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processGetPaymentExchangeVersion completed");
    }

    @Asynchronous
    private void processReport(String sessionId, Future<?> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment resultArrayPayments = null;
        boolean result = false;
        paymentsByCustomersMissingFromCRM = new ArrayList<>();
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                resultArrayPayments = (ArrayOfPayment) pgr.getData();
            }

        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, null, ex);
        }

        if (resultArrayPayments != null && resultArrayPayments.getPayment() != null) {
            List<Payment> payList = resultArrayPayments.getPayment();
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
                    Customers cust = null;
                    try {
                        cust = customersFacade.findById(custId);
                    } catch (Exception e) {
                        logger.log(Level.WARNING, "Future Map processReport customersFacade.findById(custId) Error: custId={0}, Exception={1}", new Object[]{custId, e.getMessage()});
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
                                    crmPay = paymentsFacade.findPaymentById(paymentRefInt, false);
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
                        } else // old payment without a primary key reference
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
                result = true;
            } else {
                logger.log(Level.WARNING, "Future Map processReport couldn't find any Payments.");

            }
        }
        if (result == true) {
            String message = "The payment report has completed.";
            storeResponseForSessionBeenToRetrieve("PaymentReport", sessionId, pgr);
            sendMessage(sessionId, "Payment Report", message);

        } else {
            String message = "THE payment report failed to update!";
            //String message = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            sendMessage(sessionId, "Payment Report Error", message);
        }
        logger.log(Level.INFO, "Future Map processReport completed");
    }

    @Asynchronous
    private void processGetPayments(String sessionId, Future<PaymentGatewayResponse> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        boolean result;
        PaymentGatewayResponse pgr = null;

        ArrayOfPayment resultPaymentArray;
        boolean abort = false;

        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;

            }

            if (pgr != null) {
                resultPaymentArray = (ArrayOfPayment) pgr.getData();
                result = pgr.isOperationSuccessful();

                if (resultPaymentArray != null && resultPaymentArray.getPayment() != null) {
                    List<Payment> payList = resultPaymentArray.getPayment();
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
                                                    crmPay = paymentsFacade.findPaymentById(paymentRefInt, false);
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
                                        } else // old payment without a primary key reference
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
                if (result == true) {
                    String message = "Payment Information has been updated.";
                    // send the gateway response object back to the hashmap that can be accessed by the session bean
                    storeResponseForSessionBeenToRetrieve("GetPayments", sessionId, pgr);

                    sendMessage(sessionId, "Get Payments", message);
                } else {
                    String message = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();

                    sendMessage(sessionId, "Get Payments Error", message);

                }
            } else {
                String message = "The Payment Gateway Response was empty!";
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, message);

                sendMessage(sessionId, "Get Payments Error", message);
            }
        } catch (InterruptedException | ExecutionException | NullPointerException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Future Map processGetPayments FAILED", ex);
        }
    }

    @Asynchronous
    private void processGetScheduledPayments(String sessionId, Future<PaymentGatewayResponse> ft) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfScheduledPayment resultArrayOfScheduledPayments = null;
        boolean abort = false;
        boolean result;
        PaymentGatewayResponse pgr = null;

        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;

            }

            if (pgr != null) {
                resultArrayOfScheduledPayments = (ArrayOfScheduledPayment) pgr.getData();
                result = pgr.isOperationSuccessful();

                if (resultArrayOfScheduledPayments != null && resultArrayOfScheduledPayments.getScheduledPayment() != null) {
                    List<ScheduledPayment> payList = resultArrayOfScheduledPayments.getScheduledPayment();
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
                                    int paymentCustRef;
                                    try {
                                        paymentCustRef = Integer.parseInt(pay.getYourSystemReference().getValue());
                                    } catch (NumberFormatException numberFormatException) {
                                        paymentCustRef = -1;
                                    }
                                    if (custId != paymentCustRef) {
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
                                            crmPay = paymentsFacade.findPaymentById(id, false);
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
                                                paymentBean.deletePayment(cust, crmPay.getDebitDate(), amountLong, null, cust.getUsername(), getDigitalKey());
                                                try {
                                                    Thread.sleep(250);

                                                } catch (InterruptedException ex) {
                                                    Logger.getLogger(FutureMapEJB.class
                                                            .getName()).log(Level.SEVERE, "Thread Sleep interrupted", ex);
                                                }
                                                paymentBean.addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay, cust.getUsername(), getDigitalKey());
                                            }
                                        }

                                    }
                                    // remove any that 
                                    GregorianCalendar testCal = new GregorianCalendar();
                                    testCal.add(Calendar.MINUTE, -5);
                                    Date testDate = testCal.getTime();
                                    List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust, false);
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
                if (result == true) {
                    String message = "Scheduled Payment Information has been updated.";
                    // send the gateway response object back to the hashmap that can be accessed by the session bean
                    storeResponseForSessionBeenToRetrieve("GetScheduledPayments", sessionId, pgr);
                    sendMessage(sessionId, "Get Scheduled Payments", message);

                } else {
                    String message = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
                    sendMessage(sessionId, "Get Scheduled Payments Error", message);
                }
            } else {
                String message = "The Payment Gateway Response was empty!";
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, message);

                sendMessage(sessionId, "Get Scheduled Payments Error", message);
            }
        } catch (InterruptedException | ExecutionException | NullPointerException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Future Map processGetScheduledPayments FAILED", ex);
        }
    }

    /* @Asynchronous
     private void processGetScheduledPayments2(Future ft) {
     ArrayOfScheduledPayment resultPaymentArray = null;
     boolean abort = false;
     int scheduledPayments;
     int existingInCRM;
     int createScheduledPayments = 0;
     try {
     resultPaymentArray = (ArrayOfScheduledPayment) ft.get();
     } catch (InterruptedException | ExecutionException ex) {
     Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
     }
     if (resultPaymentArray != null) {
     List<ScheduledPayment> payList = resultPaymentArray.getScheduledPayment();
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
    private synchronized void createDefaultPaymentParameters(Customers current) {

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
    private void processAddCustomer(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        Customers cust;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
            }

            if (pgr != null) {
                if (result == true) {

                    cust = (Customers) pgr.getData();
                    startAsynchJob(sessionId, "GetCustomerDetails", paymentBean.getCustomerDetails(cust, getDigitalKey()));
                    if (pgr.getTextData().contains("EXISTING")) {
                        // if the customer was cancelled and re-added to the payment gateway, check for any existing or scheduled payments just to be sure.

                        getPayments(sessionId, cust, PAYMENT_SEARCH_MONTHS_AHEAD, PAYMENT_SEARCH_MONTHS_BEHIND);
                    }
                    String message = "The customer was added to Payment Gateway Successfully.";

                    storeResponseForSessionBeenToRetrieve("AddCustomer", sessionId, pgr);
                    sendMessage(sessionId, "Add Customer", message);

                } else {

                    String message = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
                    sendMessage(sessionId, "Add Customer Error!", message);
                }
            } else {
                logger.log(Level.SEVERE, "processAddCustomer : The PaymentGatewayResponse Object is NULL");
                String message = "The PaymentGatewayResponse Object is NULL";
                sendMessage(sessionId, "Add Customer Error!", message);
            }
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.WARNING, "processAddCustomer", ex);
        }
    }

    @Asynchronous
    private void processChangeCustomerStatus(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject != null) {
                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    pgr = (PaymentGatewayResponse) resultObject;
                    result = pgr.isOperationSuccessful();
                    Object o = pgr.getData();
                    if (o != null) {
                        if (o.getClass() == Customers.class) {
                            cust = (Customers) o;
                        }

                    }
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            String message = pgr.getTextData();
            logger.log(Level.INFO, "Successfully Changed Customer Status.Sending request to refresh details and payments from payemnt gateway.");
            startAsynchJob(sessionId, "GetCustomerDetails", paymentBean.getCustomerDetails(cust, getDigitalKey()));
            storeResponseForSessionBeenToRetrieve("ChangeCustomerStatus", sessionId, pgr);
            sendMessage(sessionId, "Change Customer Status", message);
        } else {
            String message = "The change status operation failed!.";
            logger.log(Level.WARNING, message);

            sendMessage(sessionId, "Change Customer Status Error!", message);
        }
        logger.log(Level.INFO, "processChangeCustomerStatus completed");
    }

    @Asynchronous
    private void processDeletePayment(String sessionId, Future<PaymentGatewayResponse> ft) {

        String paymentRef = null;
        boolean result = false;
        Payments pay = null;
        String message = "The delete payment operation failed!.";
        logger.log(Level.INFO, "FutureMap - processAddPaymentResult started");
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject != null) {
                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    pgr = (PaymentGatewayResponse) resultObject;
                    result = pgr.isOperationSuccessful();
                    //we have a response 
                    paymentRef = pgr.getTextData();
                    pay = (Payments) pgr.getData();
                }
            }

            if (paymentRef == null) {
                //this should always be set and will be the primary key of teh row in the payments table
                logger.log(Level.WARNING, "processDeletePayment FAILED - paymentRef IS NULL or Empty");
                return;
            }

            if (result == true) {

                int reference = -1;
                try {
                    reference = Integer.parseInt(paymentRef);

                } catch (NumberFormatException numberFormatException) {
                    logger.log(Level.WARNING, "Process deletePayment - Thepayment reference could not be converted to a number: {0}", new Object[]{paymentRef});
                }
                pay = paymentsFacade.findPaymentById(reference, false);
                if (pay != null) {
                    //removeFromPaymentLists(pay);
                    paymentsFacade.remove(pay);

                } else {
                    logger.log(Level.WARNING, "Process deletePayment - Payment that was deleted could not be found in the our DB key={0}", new Object[]{reference});
                }
                // setSelectedScheduledPayment(null);
                //JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Deleted Payment  .");
                message = "The payment was successfully deleted.";
                storeResponseForSessionBeenToRetrieve("DeletePayment", sessionId, pgr);
                sendMessage(sessionId, "Delete Payment", message);
                //getPayments(18, 2);

            } else if (pgr != null) {
                logger.log(Level.WARNING, "Process deletePayment - DELETE PAYMENT FAILED: {0}", new Object[]{result});
                String errorMessage = pgr.getErrorMessage();
                sendMessage(sessionId, "Delete Payment FAILED", errorMessage);
                int id = 0;
                try {
                    id = Integer.parseInt(paymentRef);
                } catch (NumberFormatException numberFormatException) {
                    logger.log(Level.WARNING, "Process deletePayment  FAILED - PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", result);
                }
                //Payments pay = paymentsFacade.findPaymentById(id, false);
                if (pay != null) {

                    if (errorMessage.contains("Payment selected for deletion could not be found")) {
                        //JsfUtil.addErrorMessage("Payment Gateway", "A payment with this reference could not be found in the payment gateway!");

                        if (pay.getBankFailedReason().contentEquals("MISSING")) {
                            paymentsFacade.remove(pay);
                            //  removeFromPaymentLists(pay);
                        } else {
                            pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                            paymentsFacade.editAndFlush(pay);
                            //  updatePaymentLists(pay);
                        }
                    } else if (errorMessage.contains("Your update could not be processed at this time")) {
                        if (pay.getBankReturnCode() == null) {
                            pay.setBankReturnCode("");
                        }
                        if (pay.getBankReturnCode().trim().isEmpty()) {
                            // firts attempt at retry so set counter to 0
                            pay.setBankReturnCode("0");
                        } else {
                            int retries = Integer.parseInt(pay.getBankReturnCode().trim());
                            retries++;
                            pay.setBankReturnCode(Integer.toString(retries));
                            paymentsFacade.editAndFlush(pay);
                            if (retries < 10) {
                                startAsynchJob(sessionId, "AddPayment", paymentBean.deletePayment(pay.getCustomerName(), pay.getDebitDate(), pay.getPaymentAmount().movePointRight(2).longValue(), pay, "Auto Retry", getDigitalKey()));

                                //retryDeletePayment(pay);
                            } else {
                                pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                                paymentsFacade.editAndFlush(pay);
                                message = "Could not delete the payment after 10 attempts";
                                sendMessage(sessionId, "Delete Payment", message);
                                //updatePaymentLists(pay);
                            }
                        }
                    } else {
                        logger.log(Level.WARNING, "Process deletePayment  FAILED - Unhandled error ", result);
                        message = "Could not delete the payment after 10 attempts";
                        sendMessage(sessionId, "Delete Payment", message);
                        //JsfUtil.addSuccessMessage("Payment Gateway", "Deleted Payment Error - see logs for more details .");
                    }

                }
            }
        } catch (InterruptedException | ExecutionException | EJBException ex) {

        }
        logger.log(Level.INFO, "processDeletePayment completed");
    }

    @Asynchronous
    private void processAddPaymentResult(String sessionId, Future<PaymentGatewayResponse> ft) {
        String paymentRef;
        Payments pay = null;
        boolean result = false;
        logger.log(Level.INFO, "FutureMap - processAddPaymentResult started");
        PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                pay = (Payments) pgr.getData();
            }

            if (pgr != null) {
                //we have a response 
                paymentRef = pgr.getTextData();

                if (paymentRef == null) {
                    //this should always be set and will be the primary key of teh row in the payments table
                    logger.log(Level.WARNING, "processAddPaymentResult FAILED - RESULT IS NULL or Empty");
                    return;
                }
                if (result == false) {

                    String errorMessage = "Error Code: " + pgr.getErrorCode() + ", " + pgr.getErrorMessage();
                    int id = 0;
                    try {
                        id = Integer.parseInt(paymentRef);
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.INFO, "processAddPaymentResult FAILED - PaymentReference could not be converted to a number. It should be the primary key of teh payments table row ", paymentRef);
                    }
                    //Payments pay = paymentsFacade.findPaymentById(id, false);
                    if (pay != null) {
                        logger.log(Level.INFO, "FutureMap - processAddPaymentResult FAILED TO ADD PAYMENT - processing error for:{0}", new Object[]{paymentRef});
                        if (errorMessage.contains("cannot process your add payment request at this time.")) {
                            if (pay.getBankReturnCode() == null) {
                                pay.setBankReturnCode("");
                            }
                            if (pay.getBankReturnCode().trim().isEmpty()) {
                                // firts attempt at retry so set counter to 0
                                pay.setBankReturnCode("0");
                            } else {
                                int retries = Integer.parseInt(pay.getBankReturnCode().trim());
                                retries++;
                                pay.setBankReturnCode(Integer.toString(retries));
                                paymentsFacade.editAndFlush(pay);
                                if (retries < 10) {

                                    startAsynchJob(sessionId, "AddPayment", paymentBean.addPayment(pay.getCustomerName(), pay.getDebitDate(), pay.getPaymentAmount().movePointRight(2).longValue(), pay, "Auto Retry", getDigitalKey()));
                                    logger.log(Level.INFO, "processAddPaymentResult PAYMENT GATEWAY BUSY - ATTEMPTING RETRY - ", paymentRef);
                                } else {
                                    pay.setPaymentStatus(PaymentStatus.REJECTED_BY_GATEWAY.value());
                                    paymentsFacade.editAndFlush(pay);

                                }
                            }
                        } else if (errorMessage.contains("This customer already has two payments on this date.")) {
                            paymentsFacade.remove(pay);
                            String message = "Payment ID:" + pay.getId().toString() + " for Amount:$" + pay.getPaymentAmount().toPlainString() + " on Date:" + pay.getDebitDate().toString() + " could not be added as teh customer already has two existing payments on this date!!.";
                            sendMessage(sessionId, "Add Payment Error!", message);

                        } else {
                            pay.setPaymentStatus(PaymentStatus.REJECTED_BY_GATEWAY.value());
                            pay.setPaymentReference(Integer.toString(id));
                            paymentsFacade.editAndFlush(pay);
                        }

                    } else {
                        logger.log(Level.WARNING, "processAddPaymentResult FAILED - ERROR processing could not find payment id {0}, time: {1}", new Object[]{paymentRef, new Date().toString()});
                    }
                } else if (paymentRef.isEmpty() == false) {

                    int id = 0;
                    try {
                        id = Integer.parseInt(paymentRef);
                    } catch (NumberFormatException numberFormatException) {
                        logger.log(Level.SEVERE, "processAddPaymentResult FAILED - Successful result but PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", paymentRef);

                    }

                    //if the response is very fast we may need to wait for the object to be persisted 
                    /*  boolean waitForObjectToArriveInDB = true;
                    int c = 0;
                    Payments pay = null;
                    while (waitForObjectToArriveInDB) {
                        c++;
                        pay = paymentsFacade.findPaymentById(id, false);
                        if (pay != null) {
                            waitForObjectToArriveInDB = false;
                        }
                        if (c > 30) { // wait up to 3 seconds in case DB is running slow with the from a separate thread.
                            waitForObjectToArriveInDB = false;
                        }
                        logger.log(Level.INFO, "processAddPaymentResult FAILED - paymentsFacade.findPaymentById(id) failed to find the record:{0}, RETRYING {2}   ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date()), c});
                        sleep(100);
                    }*/
                    //Payments pay = paymentsFacade.findScheduledPayment(paymentRef);
                    if (pay != null) {
                        pay.setPaymentStatus(PaymentStatus.SCHEDULED.value());
                        pay.setPaymentReference(Integer.toString(id));
                        paymentsFacade.editAndFlush(pay);
                        logger.log(Level.INFO, "processAddPaymentResult - Updated payment status to SCHEDULED in CRM DB -  record:{0}, ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});
                    } else {
                        logger.log(Level.SEVERE, "processAddPaymentResult FAILED - paymentsFacade.findPaymentById(id) failed to find the record:{0}, ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});
                    }
                    logger.log(Level.INFO, "FutureMap - processAddPaymentResult Scheduled Successfully in payment gateway - processed:{0}", new Object[]{paymentRef});
                    String message = "The Payment (ref:" + paymentRef + ") was submitted successfully.";
                    storeResponseForSessionBeenToRetrieve("AddPayment", sessionId, pgr);
                    sendMessage(sessionId, "Add Payment", message);
                } else {
                    Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Add Payment Failed! The payment Reference is empty! This should not happen as its taken from the primary key of the row in teh payments table. ");
                }
            } else {
                String message = "The information returned by the payment gateway was empty! (pgr = NULL)";
                sendMessage(sessionId, "Add Payment Error!", message);
            }
        } catch (InterruptedException | ExecutionException | EJBException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processAddPaymentResult FAILED - Async Task Exception ", ex);
        }
        logger.log(Level.INFO, "FutureMap - processAddPaymentResult completed");
    }

    private synchronized void startAsynchJob(String sessionId, String key, Future<PaymentGatewayResponse> future) {

        AsyncJob aj = new AsyncJob(key, future);
        put(sessionId, aj);

    }

    protected synchronized void getPayments(String sessionId, Customers cust, int monthsAhead, int monthsbehind) {
        GregorianCalendar cal = new GregorianCalendar();

        cal.add(Calendar.MONTH, monthsAhead);
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -(monthsAhead));
        cal.add(Calendar.MONTH, -(monthsbehind));
        startAsynchJob(sessionId, "GetPayments", paymentBean.getPayments(cust, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()));
        startAsynchJob(sessionId, "GetScheduledPayments", paymentBean.getScheduledPayments(cust, cal.getTime(), endDate, getDigitalKey()));

    }
    //CreateSchedule

    private synchronized int getNumberofProcessingPayments(Customers cust) {
        int size = 0;
        List<Payments> payList = paymentsFacade.findPaymentsByCustomerAndStatus(cust, PaymentStatus.SENT_TO_GATEWAY.value());
        if (payList != null) {
            size = payList.size();
        }
        logger.log(Level.INFO, "Future Map; The Number of Processing Payments {0} = {1}", new Object[]{cust.getUsername(), size});
        return size;
    }

    @Asynchronous
    @TransactionAttribute(TransactionAttributeType.NEVER)
    private void processCreateSchedule(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to create the customers schedule. Refer to logs for more info";
        PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
            }

            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    Object custObject = pgr.getData();
                    if (custObject != null && custObject.getClass() == Customers.class) {
                        cust = (Customers) custObject;
                    }

                    if (cust != null) {

                        /* Integer i = 0;
                        try {
                            //wait up to 30 seconds for all payments to be added before sending refresh for components 
                            while (getNumberofProcessingPayments(cust) > 0 && i++ < 100) {
                                Thread.sleep(300);
                            }
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        getPayments(sessionId, cust, 18, 2);
                        logger.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after creating the schedule for {0} ", new Object[]{cust.getUsername()});
                    }

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processCreateSchedule - the PaymentGatewayResponse is null");
            }
            logger.log(Level.INFO, "Future Map processCreateSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been created.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processCreateSchedule - retrieving PaymentGatewayResponse", ex);
        }
        sendMessage(sessionId, "Create Payment Schedule", returnedMessage);
    }

    @Asynchronous
    private void processDeletePaymentBatch(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to processDeletePaymentBatch. Refer to logs for more info";
        PaymentGatewayResponse pgr = null;

        Payments pay = null;
        Customers cust = null;
        try {
// if successful it should return a Customers Object from the getData method;
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                pay = (Payments) pgr.getData();
            }

            if (pgr != null && pay != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    cust = pay.getCustomerName();

                    if (cust != null) {

                        /* Integer i = 0;
                        try {
                            //wait up to 30 seconds for all payments to be added before sending refresh for components 
                            while (getNumberofProcessingPayments(cust) > 0 && i++ < 100) {
                                Thread.sleep(300);
                            }
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        pgr.setOperationName("AddPaymentBatch");
                        storeResponseForSessionBeenToRetrieve("AddPayment", sessionId, pgr);
                          sendMessage(sessionId, "Delete Payment Batch", "OK");
                        getPayments(sessionId, cust, 18, 2);
                        logger.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after processDeletePaymentBatch for {0} ", new Object[]{cust.getUsername()});
                    }

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processDeletePaymentBatch - the PaymentGatewayResponse is null");
            }
            logger.log(Level.INFO, "Future Map processDeletePaymentBatch completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been deleted.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processDeletePaymentBatch - calling getPayments", ex);
        }

        logger.log(Level.INFO, "Future Map --- BATCH ---> processDeletePaymentBatch completed");
      
    }

    @Asynchronous
    private void processAddPaymentBatch(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to create the customers schedule. Refer to logs for more info";
        PaymentGatewayResponse pgr = null;

        Payments pay = null;
        Customers cust = null;
        try {
// if successful it should return a Customers Object from the getData method;
            // if successful it should return a Customers Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                pay = (Payments) pgr.getData();
            }

            if (pgr != null && pay != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    cust = pay.getCustomerName();

                    if (cust != null) {

                        /* Integer i = 0;
                        try {
                            //wait up to 30 seconds for all payments to be added before sending refresh for components 
                            while (getNumberofProcessingPayments(cust) > 0 && i++ < 100) {
                                Thread.sleep(300);
                            }
                        } catch (InterruptedException e) {
                            logger.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        getPayments(sessionId, cust, 18, 2);
                        pgr.setOperationName("AddPaymentBatch");
                        storeResponseForSessionBeenToRetrieve("AddPayment", sessionId, pgr);
                        sendMessage(sessionId, "Add Payment Batch", "OK");
                        logger.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after creating the schedule for {0} ", new Object[]{cust.getUsername()});
                    }

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processCreateSchedule - the PaymentGatewayResponse is null");
            }
            logger.log(Level.INFO, "Future Map processCreateSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been created.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processAddPaymentBatch - calling getPayments", ex);
        }
        logger.log(Level.INFO, "Future Map --- BATCH ---> processAddPaymentBatch completed");

    }

    @Asynchronous
    private void processClearSchedule(String sessionId, Future<PaymentGatewayResponse> ft) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to clear the customers schedule. Refer to logs for more info";
        PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
            }

            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    Object custObject = pgr.getData();
                    if (custObject != null && custObject.getClass() == Customers.class) {
                        cust = (Customers) custObject;
                    }

                    if (cust != null) {
                        List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(cust, PaymentStatus.DELETE_REQUESTED.value());
                        for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                            Payments p = crmPaymentList.get(x);
                            paymentsFacade.remove(p);
                        }
                    }
                    // update the payment schedule and double check that the clear schedule actually worked 
                    getPayments(sessionId, cust, 18, 2);
                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processClearSchedule - the PaymentGatewayResponse is null");
            }
            logger.log(Level.INFO, "Future Map processClearSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been cleared.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processClearSchedule - retrieving PaymentGatewayResponse", ex);
        }
        sendMessage(sessionId, "Clear Payment Schedule", returnedMessage);
    }

    @Asynchronous
    private void processGetCustomerDetails(String sessionId, Future<PaymentGatewayResponse> ft) {
        CustomerDetails custDetails = null;

        boolean result = false;
        String returnedMessage = "An error occurred trying to clear the customers schedule. Refer to logs for more info";
        PaymentGatewayResponse pgr = null;

        try {
            // if successful it should return a CustomerDetails Object from the getData method;
            Object resultObject = ft.get();
            if (resultObject.getClass() == PaymentGatewayResponse.class) {
                pgr = (PaymentGatewayResponse) resultObject;
            }

            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    Object custObject = pgr.getData();
                    if (custObject != null && custObject.getClass() == CustomerDetails.class) {
                        custDetails = (CustomerDetails) custObject;
                    }
                }
            }

            if (custDetails != null) {
                // do something with resultPaymentArray
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
                        result = true;

                    } else {
                        logger.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number or the customer ID does not exist");
                    }
                }
            }
            if (result == true) {
                returnedMessage = "The customer details have recieved and updated.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (InterruptedException | ExecutionException | NullPointerException e) {
            logger.log(Level.WARNING, "Future Map processGetCustomerDetails FAILED", e);
        }

        logger.log(Level.INFO, "Future Map processGetCustomerDetails completed");
        storeResponseForSessionBeenToRetrieve("GetCustomerDetails", sessionId, pgr);
        sendMessage(sessionId, "Get Customer Details", returnedMessage);
    }

    private synchronized void updateNextScheduledPayment(Customers cust) {
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
                } else if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                    return false;
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
                } else if (compareStringToXMLString(payment.getCustomerName().getId().toString(), (pay.getYourSystemReference())) == false) {
                    return false;
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

    /*  private void sanityCheckCustomersForDefaultItems() {
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
                        logger.log(Level.INFO, "Creating default payment parameters");
                    }
                }
                logger.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
            } else {
                logger.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers. Could not get the list of customers from the DB");
            }
        } else {
            logger.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers.Customer Facade null. HAs it been initialised yet?");
        }
    }*/

 /*    private void createDefaultProfilePic(Customers cust) {
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
    }*/
}
