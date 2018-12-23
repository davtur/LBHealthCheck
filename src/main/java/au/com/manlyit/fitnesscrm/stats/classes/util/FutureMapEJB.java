/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade;
import au.com.manlyit.fitnesscrm.stats.beans.NotificationsLogFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentSource;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import au.com.manlyit.fitnesscrm.stats.db.NotificationsLog;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.Plan;
import au.com.manlyit.fitnesscrm.stats.db.Tickets;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.Asynchronous;
import javax.ejb.ConcurrencyManagement;
import static javax.ejb.ConcurrencyManagementType.BEAN;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.ejb.TransactionManagement;
import javax.ejb.TransactionManagementType;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.xml.bind.JAXBElement;
import javax.xml.datatype.XMLGregorianCalendar;
import javax.xml.ws.WebServiceException;

/**
 *
 * @author david
 */
@ConcurrencyManagement(BEAN)
@TransactionManagement(TransactionManagementType.BEAN)
@Singleton
//@LocalBean
@Startup
@ApplicationScoped
public class FutureMapEJB implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(FutureMapEJB.class.getName());
    private static final int TIMEOUT_SECONDS = 300;
    private static final int PAYMENT_SEARCH_MONTHS_AHEAD = 18;
    private static final int PAYMENT_SEARCH_MONTHS_BEHIND = 3;
    private static final int WEEKS_AHEAD_TO_POPULATE_TICKETS = 12; // 3 months
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
    private final Object sendMessageObject = new Object();
    private final Object updateCustomerSchedulesLock = new Object();
    private final Object settlementReportLockObject = new Object();
    private final Object paymentReportLockObject = new Object();
    private final Object updateScheduleLock = new Object();
    private final Object issueTicketsLockObject = new Object();
    private final Object issueOneWeeksTicketsLockObject = new Object();
    private final Object issueBlockOfTicketsLockObject = new Object();
    private final AtomicBoolean settlementReportLock = new AtomicBoolean(false);
    private final AtomicBoolean paymentReportLock = new AtomicBoolean(false);
    //private final AtomicBoolean updateScheduleLock = new AtomicBoolean(false);
    private final AtomicBoolean asychCheckProcessing = new AtomicBoolean(false);
    private List<Payment> paymentsByCustomersMissingFromCRM;

    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.NotesFacade ejbNotesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTypesFacade sessionTypesFacade;
    @Inject
    private NotificationsLogFacade notificationsLogFacade;
    @Inject
    @Push
    private PushContext payments;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.TicketsFacade ejbTicketsFacade;
    @Inject
    private PaymentBean paymentBean;
    @Inject
    private EmailTemplatesFacade ejbEmailTemplatesFacade;
    private INonPCIService ws;

    /* @PathParam("user")
    private String username;*/
    public FutureMapEJB() {
    }

    /* @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint rEndPoint, EventBus e) {
        rEndPoint.address();

        LOGGER.log(Level.INFO, "Atmosphere Push Connection OPENED. Transport Type = {0}, Address = {1}, Path = {2}, URI = {3}, Status = {4}", new Object[]{rEndPoint.transport().name(), rEndPoint.address(), rEndPoint.path(), rEndPoint.uri(), rEndPoint.status()});
    }

    @OnClose
    public void onClose(RemoteEndpoint rEndPoint, EventBus e) {

        LOGGER.log(Level.INFO, "Atmosphere Push Connection CLOSED. Transport Type = {0}, Address = {1}, Path = {2}, URI = {3}, Status = {4}", new Object[]{rEndPoint.transport().name(), rEndPoint.address(), rEndPoint.path(), rEndPoint.uri(), rEndPoint.status()});

    }*/

 /*   @PostConstruct
    private void applicationSetup() {
        LOGGER.log(Level.INFO, "Application Setup Running");
        try {
            sanityCheckCustomersForDefaultItems();

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, " @PostConstruct Future Map - applicationSetup(). Exception in sanityCheckCustomersForDefaultItems: ", e);
        }
        LOGGER.log(Level.INFO, "Application Setup Completed");
    }*/
    public void sendMessage(String message) {
        try {
            payments.send(message);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Push error- payments.send(message) ", e);
        }
    }

    /*  public void sendMessage(String message, String  sessionId) {
      
        try {
            payments.send(message, sessionId);            
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Push error - payments.send(message, sessionId) ", e);
        }
 }*/
    public void sendMessage(String growlMessage, String sessionId) {

        try {
            payments.send(growlMessage, sessionId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Push error - payments.send(message, recipientUserId)", e);
        }
    }

    public void sendMessage(Object facesmessage, Customers recipientUser) {
        int recipientUserId = recipientUser.getId();
        try {
            payments.send(facesmessage, recipientUserId);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Push error - payments.send(message, recipientUserId)", e);
        }
    }

    public void sendMessage(String message, Collection<Integer> recipientUserIds) {
        try {
            payments.send(message, recipientUserIds);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Push error- payments.send(message, recipientUserIds)", e);
        }
    }

    // @TransactionAttribute(TransactionAttributeType.NEVER)
    public void sendMessage(String sessionChannel, String summary, String detail) {
        synchronized (sendMessageObject) {
            if ("Batch-Dont-Update".equals(sessionChannel)) {
                return;
            }
            LOGGER.log(Level.INFO, "Entering - SEND MESSAGE ");
            if (sessionChannel.contains(FUTUREMAP_INTERNALID) == false) {// we don't want to send a message unless there is a session to send it to

                LOGGER.log(Level.INFO, "Entering - SEND MESSAGE :Channel={0}, Summary={1}, detail={2}", new Object[]{sessionChannel, summary, detail});

                try {
                    sendMessage(detail, sessionChannel);
                    LOGGER.log(Level.INFO, "Sending Async Message, summary:{0}, details:{1}", new Object[]{summary, detail});
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "NOT Sending Async Message as there was an exception, summary:{0}, details:{1}, error:{2}", new Object[]{summary, detail, e.getMessage()});
                }
            } else {
                LOGGER.log(Level.INFO, "NOT Sending Async Message as the session is internal, summary:{0}, details:{1}", new Object[]{summary, detail});
            }
            //}
            LOGGER.log(Level.INFO, "Exiting - SEND MESSAGE  :Channel={0}, Summary={1}, detail={2}", new Object[]{sessionChannel, summary, detail});
        }
    }

    /**
     * @return the FUTUREMAP_INTERNALID
     */
    public synchronized String getFutureMapInternalSessionId() {
        return FUTUREMAP_INTERNALID;
    }

    public INonPCIService getWs() {
        synchronized (lock9) {
            if (ws == null) {
                URL url = null;
                WebServiceException e = null;
                try {
                    url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
                } catch (MalformedURLException ex) {

                    LOGGER.log(Level.SEVERE, "MalformedURLException - payment.ezidebit.gateway.url", ex);

                }
                try {
                    ws = new NonPCIService(url).getBasicHttpBindingINonPCIService();
                } catch (Exception e2) {
                    LOGGER.log(Level.SEVERE, "Failed to initialise the Payment Gateway Web service", e2);
                }
            }
            return ws;
        }

    }

    /**
     * @param userSessionId
     * @return the futureMap
     */
    public ArrayList<AsyncJob> getFutureMap(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        synchronized (futureMapArrayLock) {
            LOGGER.log(Level.FINE, "Get Future Map.  for sessionID {0}.", userSessionId);

            ArrayList<AsyncJob> fmap = futureMap.get(userSessionId);
            if (fmap == null) {
                LOGGER.log(Level.INFO, "Get Future Map. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

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
            LOGGER.log(Level.FINE, "componentsToUpdate Map.  for sessionID {0}.", userSessionId);

            ArrayList<PaymentGatewayResponse> fmap = componentsToUpdate.get(userSessionId);
            if (fmap == null) {
                LOGGER.log(Level.INFO, "Get componentsToUpdate. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

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
            LOGGER.log(Level.FINE, "batchJobs Map.  for sessionID {0}.", userSessionId);

            ArrayList<BatchOfPaymentJobs> fmap = batchJobs.get(userSessionId);
            if (fmap == null) {
                LOGGER.log(Level.INFO, "Get batchJobs. Map is null for sessionID {0} . Creating an empty list.", userSessionId);

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
     * @param pgr
     *
     */
    public void addComponentToUpdatesList(String userSessionId, PaymentGatewayResponse pgr) {
        synchronized (lock1) {
            try {
                LOGGER.log(Level.INFO, "addComponentToUpdatesList, put. sessionid {0},Component To Update {1}.", new Object[]{userSessionId, pgr});
                getComponentsToUpdate(userSessionId).add(pgr);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "addComponentToUpdatesList put(String userSessionId, String aj) method. Unable to add component to update list, Session:{1}, component Name:{2}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, pgr});
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
                LOGGER.log(Level.INFO, "addComponentToUpdatesList, put. sessionid {0},Component To Update {1}.", new Object[]{userSessionId, bj});
                getBatchJobs(userSessionId).add(bj);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "addComponentToUpdatesList put(String userSessionId, String aj) method. Unable to add component to update list, Session:{1}, component Name:{2}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, bj});
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
        if ("Batch-Dont-Update".equals(sessionId)) {
            return;
        }
        if (operationName != null && sessionId != null && pgr != null && operationName.trim().isEmpty() == false) {
            // no point storing it if the operation name is empty as it will have no effect in the session bean
            pgr.setOperationName(operationName);
            addComponentToUpdatesList(sessionId, pgr);
        }
    }

    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    public boolean runSettlementReport(Date fromDate, String sessionId) {
// use this if you need to match up against what is in your bank account
        synchronized (settlementReportLockObject) {
            if (settlementReportLock.get() == true) {
                LOGGER.log(Level.INFO, "The Settlement Report Already Running.");
                return false;
            } else {
                LOGGER.log(Level.INFO, "Future Map, runSettlementReport. from date {0}.", fromDate);
                Date toDate = new Date();
                AsyncJob aj = new AsyncJob("SettlementReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, toDate, true, getDigitalKey(), sessionId));
                this.put(FUTUREMAP_INTERNALID, aj);
                settlementReportLock.set(true);
            }
            return true;
        }
    }

    public boolean runPaymentReport(Date fromDate, String sessionId) throws InterruptedException {
        // use this if to get the lates payment information
        synchronized (paymentReportLockObject) {
            if (paymentReportLock.get() == true) {
                LOGGER.log(Level.INFO, "The Payment Report Already Running.");
                return false;
            } else {
                LOGGER.log(Level.INFO, "Future Map, runPaymentReport. from date {0}.", fromDate);
                Date toDate = new Date();
                AsyncJob aj = new AsyncJob("PaymentReport", paymentBean.getAllPaymentsBySystemSinceDate(fromDate, toDate, false, getDigitalKey(), sessionId));
                this.put(FUTUREMAP_INTERNALID, aj);
                refreshAllCustomersDetailsFromGateway();
                updateCustomerPaymentSchedules();
                paymentReportLock.set(true);
            }
            return true;
        }

    }

    public boolean runUpdateSchedules() throws InterruptedException {
        // use this if to get the lates payment information
        synchronized (updateScheduleLock) {
            updateCustomerPaymentSchedules();
            return true;
        }

    }

    public void issueOneWeeksTicketsForCust(Customers c, Date ticketStartDate, Date ticketStopDate) {

        synchronized (issueOneWeeksTicketsLockObject) {
            try {
                List<Tickets> at = ejbTicketsFacade.findCustomerTicketsByDateRange(c, ticketStartDate, ticketStopDate, true);
                int ticketsAdded = 0;
                if (at.isEmpty()) {// if the weeks tickets are empty add the tickets based on their plan session allocated.

                    // get the list of sub items on the plan. Sub items per week i.e. 2 group training session for two a week , 10 for unlimited etc.
                    ArrayList<Plan> ap = new ArrayList<>(c.getGroupPricing().getPlanCollection());
                    for (Plan p : ap) {
                        Tickets t = new Tickets();
                        t.setDatePurchased(new Date());
                        t.setCustomer(c);
                        t.setSessionType(p.getSessionType());
                        t.setValidFrom(ticketStartDate);
                        t.setExpires(ticketStopDate);
                        ejbTicketsFacade.create(t);
                        ticketsAdded++;
                    }
                }
                LOGGER.log(Level.INFO, "Adding Tickets for Customer id {0}, existing tickets {1}, tickets added {2},startDate {3}, stopDate {4} ", new Object[]{c.getId(), at.size(), ticketsAdded, ticketStartDate, ticketStopDate});
            } catch (Exception ex) {
                Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "issueOneWeeksTicketsForCust", ex.getMessage());
            }
        }
    }

    public void issueWeeklyCustomerTickets(Customers c, int weeksAheadToPolulate) {

        synchronized (issueTicketsLockObject) {
            try {

                GregorianCalendar ticketStartDate = new GregorianCalendar();
                CalendarUtil.SetToLastDayOfWeek(Calendar.SUNDAY, ticketStartDate);
                CalendarUtil.SetTimeToMidnight(ticketStartDate);

                GregorianCalendar ticketStopDate = new GregorianCalendar();
                CalendarUtil.SetToNextDayOfWeek(Calendar.SUNDAY, ticketStopDate);
                CalendarUtil.SetTimeToMidnight(ticketStopDate);
                ticketStopDate.add(Calendar.SECOND, -1);

                for (int week = 0; week < weeksAheadToPolulate; week++) {
                    issueOneWeeksTicketsForCust(c, ticketStartDate.getTime(), ticketStopDate.getTime());
                    ticketStartDate.add(Calendar.WEEK_OF_YEAR, 1);
                    ticketStopDate.add(Calendar.WEEK_OF_YEAR, 1);
                }

            } catch (Exception ex) {
                Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "issueWeeklyCustomerTicketsForPlansSessionBookings", ex.getMessage());
            }
        }
    }

    public void issueWeeklyCustomerTicketsForPlansSessionBookings() {

        List<Customers> acl = customersFacade.findAllActiveCustomers(true);
        LOGGER.log(Level.INFO, "###### Updating {0} Customer issueWeeklyCustomerTicketsForPlansSessionBookings #####", acl.size());

        for (Customers c : acl) {
            issueWeeklyCustomerTickets(c, WEEKS_AHEAD_TO_POPULATE_TICKETS);
        }

    }

    public void updateCustomerPaymentSchedules() {

        synchronized (updateCustomerSchedulesLock) {
            List<Customers> acl = customersFacade.findAllActiveCustomers(true);
            LOGGER.log(Level.INFO, "###### Updating {0} Customer Payment Schedules #####", acl.size());

            for (Customers c : acl) {
                try {
                    if (c.getPaymentParametersId().getStatusCode() != null) {
                        if (c.getPaymentParametersId().getStatusCode().trim().compareTo("A") == 0) {
                            this.put(FUTUREMAP_INTERNALID, new AsyncJob("GetCustomerDetails", paymentBean.updateCustomerPaymentSchedule(c, getDigitalKey(), FUTUREMAP_INTERNALID)));
                            TimeUnit.MILLISECONDS.sleep(50);//sleeping for a long time wont affect performance (the warning is there for a short sleep of say 5ms ) but we don't want to overload the payment gateway or they may get upset.
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "updateCustomerPaymentSchedules", ex.getMessage());
                }
            }
        }
    }

    private synchronized void refreshAllCustomersDetailsFromGateway() {
        List<Customers> acl = customersFacade.findAllActiveCustomers(true);

        for (Customers c : acl) {
            try {
                this.put(FUTUREMAP_INTERNALID, new AsyncJob("GetCustomerDetails", paymentBean.getCustomerDetails(c, getDigitalKey(), FUTUREMAP_INTERNALID)));
                TimeUnit.MILLISECONDS.sleep(50);//sleeping for a long time wont affect performance (the warning is there for a short sleep of say 5ms ) but we don't want to overload the payment gateway or they may get upset.

            } catch (InterruptedException ex) {
                Logger.getLogger(FutureMapEJB.class
                        .getName()).log(Level.SEVERE, "refreshAllCustomersDetailsFromGateway - Thread Sleep InterruptedException", ex.getMessage());
            }
        }
    }

    public void remove(String userSessionId, String key) {
        synchronized (futureMapArrayLock) {
            LOGGER.log(Level.INFO, "Future Map, remove. sessionid {0}, key {1}.", new Object[]{userSessionId, key});
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
            LOGGER.log(Level.FINE, "Future Map, get sessionid {0}, key {1}.", new Object[]{userSessionId, key});
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
                LOGGER.log(Level.SEVERE, "futureMap.containsKey", e);
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
                LOGGER.log(Level.INFO, "Future Map, put. sessionid {0},AsyncJob key {1}.", new Object[]{userSessionId, aj.getJobName()});
                getFutureMap(userSessionId).add(aj);
            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "Future Map put(String userSessionId, AsyncJob aj) method. Unable to add Async Job, Session:{1}, job Name:{2}, start Time:{3}, Error Message:{0}", new Object[]{e.getMessage(), userSessionId, aj.getJobName(), aj.getStartTime()});
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
            clearComponentUpdates(userSessionId);
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

    // @Schedule(dayOfMonth = "*", hour = "*", minute = "*/5", second = "0")//debug
    //@Schedule(dayOfMonth = "*", hour = "6", minute = "0", second = "0")
    public void updateScheduledPayments(Timer t) {
        try {
            // run every day at 5am seconds
            GregorianCalendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            LOGGER.log(Level.INFO, "Running  update Scheduled Payments from date:{0}", cal.getTime());
            boolean result = runUpdateSchedules();
            LOGGER.log(Level.INFO, "The update Scheduled Payments has completed. Result:{0}", result);

        } catch (InterruptedException ex) {
            Logger.getLogger(FutureMapEJB.class
                    .getName()).log(Level.WARNING, "update Scheduled Payments was interrupted", ex);

        } catch (Exception ex) {
            Logger.getLogger(FutureMapEJB.class
                    .getName()).log(Level.SEVERE, "update Scheduled Payments - UNHANDLED EXCEPTION In EJB TIMER", ex);
        }
    }

    //@Schedule(dayOfMonth = "*", hour = "*", minute = "*", second = "0")//debug
    @Schedule(dayOfMonth = "*", hour = "6", minute = "0", second = "0")
    public void retrievePaymentsReportFromPaymentGateway(Timer t) {
        try {
            // run every day at 5am seconds
            GregorianCalendar cal = new GregorianCalendar();
            cal.add(Calendar.DAY_OF_YEAR, -7);
            LOGGER.log(Level.INFO, "Running the daily payment report from date:{0}", cal.getTime());

            boolean result = runPaymentReport(cal.getTime(), getFutureMapInternalSessionId());
            LOGGER.log(Level.INFO, "The daily payment report has completed. Result:{0}", result);

        } catch (InterruptedException ex) {
            Logger.getLogger(FutureMapEJB.class
                    .getName()).log(Level.WARNING, "Run Payment Report was interrupted", ex);

        } catch (Exception ex) {
            Logger.getLogger(FutureMapEJB.class
                    .getName()).log(Level.SEVERE, "retrievePaymentsReportFromPaymentGateway - UNHANDLED EXCEPTION In EJB TIMER", ex);
        }
    }

    public synchronized boolean isAnAsyncOperationRunning(String sessionId) {

        //return !getFutureMap(sessionId).isEmpty();
        return !getComponentsToUpdate(sessionId).isEmpty();

    }

    @Schedule(hour = "*", minute = "*", second = "*", persistent = false)
    // @TransactionAttribute(TransactionAttributeType.NEVER)// we don't want a transaction for this method as the calls within this method will invoke their own transactions
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 1 seconds
        long start = new Date().getTime();

        // if (asychCheckProcessing.get() == false) {
        //  if (   futureMap.isEmpty() == false){
        LOGGER.log(Level.FINE, "Checking Future Map for completed jobs.");
        //asychCheckProcessing.set(true);
        try {
            //  synchronized (futureMapArrayLock) {
            String temp = "";
            for (Map.Entry<String, ArrayList<AsyncJob>> pairs : futureMap.entrySet()) {
                String sessionId = pairs.getKey();
                ArrayList<AsyncJob> fmap = pairs.getValue();
                int k = fmap.size();
                if (k > 0) {

                    LOGGER.log(Level.INFO, "{0} jobs are running. Checking Future Map to see if asych jobs have finished so their results can be processed.", k);
                    /*for (Map.Entry pairsFut : fmap.entrySet()) {
                             Future ft = (Future) pairsFut.getValue();
                             String key = (String) pairsFut.getKey();
                             if (ft.isDone()) {
                             sendMessage(sessionId, "Asynchronous Task Completed", key);
                             LOGGER.log(Level.INFO, "Notifying sessionId {0} that async job {1} has finished.", new Object[]{key, sessionId});
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
                                LOGGER.log(Level.INFO, "SessionId {0} Future Map async job {1} has finished.", new Object[]{key, sessionId});
                                details.append(key).append(" ");

                                // processCompletedAsyncJobs(sessionId, key, ft);
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
                                                //String batchJobSessionId = FUTUREMAP_INTERNALID;
                                                String jobName = bj.getJobName();

                                                LOGGER.log(Level.INFO, "checkRunningJobsAndNotifyIfComplete, Processing Batch Job Child {1} - sessionId {0}, batch ID = {2}", new Object[]{sessionId, jobName, aj.getBatchId()});

                                                //  processCompletedAsyncJobs(sessionId, jobName, ft);
                                                //processCompletedAsyncJobs(sessionId,jobName, ft);
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
                                LOGGER.log(Level.INFO, "SessionId {0} async job {1} has timed out ({2} seconds )  and been cancelled.", new Object[]{key, sessionId, TIMEOUT_SECONDS});
                            }
                        }
                    } catch (Exception e) {
                        LOGGER.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete,  {0} async jobs for sessionId {1} have finished.Exception {2}", new Object[]{Integer.toString(y), sessionId, e});
                    }
                    /*   if (y > 0) {
                                if (sessionId.contains(FUTUREMAP_INTERNALID) == false) {
                                    // hack until the migration is complete
                                    if (temp.contains("GetCustomerDetails") || temp.contains("GetPayments") || temp.contains("GetScheduledPayments") || temp.contains("PaymentReport") || temp.contains("SettlementReport") || temp.contains("AddCustomer") || temp.contains("AddPayment")) {
                                        // ignore these as they send their own messages, TODO finish moving the rest of teh async jobs to this class. 
                                    } else {
                                        // legacy jobs that need to be moved to this class
                                        sendMessage(sessionId, "Asynchronous Tasks Completed", details.toString());
                                        LOGGER.log(Level.INFO, "Notifying that {0} async jobs for sessionId {1} have finished. Deatils:{2}", new Object[]{Integer.toString(y), sessionId, details.toString()});
                                    }
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
            LOGGER.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete, Unhandled exception in EJB timer: {0} Cause: {2}", new Object[]{e.getMessage(), e.getCause().getMessage()});

        } finally {
            asychCheckProcessing.set(false);
            LOGGER.log(Level.FINE, "Finished Checking Future Map for completed jobs.");
        }

        /*  } else {
            LOGGER.log(Level.INFO, "Future Map - skipping checkRunningJobsAndNotifyIfComplete as its still running.");
        }*/
        counter++;
        if (counter > 300) {
            long finish = new Date().getTime();
            long durationInMilli = finish - start;
            LOGGER.log(Level.INFO, "EJB Timer Heartbeat (5 minute interval) - checkRunningJobsAndNotifyIfCompleted, duration in milliseconds={0}", new Object[]{durationInMilli});
            counter = 0;
        }
    }
// run a schedules

    /**
     * @param remoteUser
     * @param sessionID
     * @param uref
     * @param cref
     * @param fname
     * @param lname
     * @param suburb
     * @param email
     * @param mobile
     * @param addr
     * @param state
     * @param pcode
     * @param totalamount
     * @param rdate
     * @param method
     * @param oamount
     * @param ramount
     * @param odate
     * @param freq
     * @param numpayments
     *
     */
    @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public synchronized void processCallbackFromOnlinePaymentForm(String remoteUser, String sessionID, String uref, String cref, String fname, String lname, String email, String mobile, String addr, String suburb, String state, String pcode, String rdate, String ramount, String freq, String odate, String oamount, String numpayments, String totalamount, String method) {

        //FacesContext facesContext = FacesContext.getCurrentInstance();
        //String sessionID = httpSession.getId();
        LOGGER.log(Level.INFO, "Session:{0}, Params:{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18}", new Object[]{sessionID, uref, cref, fname, lname, email, mobile, addr, suburb, state, pcode, rdate, ramount, freq, odate, oamount, numpayments, totalamount, method});

        if (uref != null) {
            uref = uref.trim();
            if (uref.length() > 0) {
                int customerId = 0;
                Customers customer = null;
                try {
                    customerId = Integer.parseInt(uref);
                    customer = customersFacade.findById(customerId);
                } catch (NumberFormatException numberFormatException) {
                }

                if (customer != null) {

                    String templatePlaceholder = "<!--LINK-URL-->";
                    String htmlText = configMapFacade.getConfig("system.admin.ezidebit.webddrcallback.template");
                    String name = customer.getFirstname() + " " + customer.getLastname();
                    htmlText = htmlText.replace(templatePlaceholder, name);

                    Future<Boolean> emailSendResult = paymentBean.sendAsynchEmail(configMapFacade.getConfig("AdminEmailAddress"), configMapFacade.getConfig("PasswordResetCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), configMapFacade.getConfig("system.ezidebit.webEddrCallback.EmailSubject"), htmlText, null, emailServerProperties(), false);
                    try {
                        if (emailSendResult.get() == false) {
                            LOGGER.log(Level.WARNING, "Email for Call Back from Web EDDR Form FAILED. Future result false from async job");
                        }
                    } catch (InterruptedException ex) {
                        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "emailSendResult.get()", ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "emailSendResult.get()", ex);
                    }

                    PaymentParameters pp = customer.getPaymentParametersId();
                    if (pp != null) {
                        if (pp.getWebddrUrl() != null) {
                            // the url is not null so this is the first time the customer has clicked the link -this should only happen once so it cant be abused.
                            pp.setWebddrUrl(null);
                            pp.setStatusCode("A");
                            pp.setStatusDescription("Active");
                            pp.setEzidebitCustomerID(cref);
                            //ejbPaymentParametersFacade.edit(customer.getPaymentParametersId());
                            customer.setPaymentParametersId(pp);
                            customersFacade.editAndFlush(customer);
                            LOGGER.log(Level.INFO, " Customer {0} has set up payment info. Setting Web DDR URL to NULL as it should only be used once.", new Object[]{customer.getUsername()});
                            //startAsynchJob("ConvertSchedule", paymentBean.clearSchedule(customer, false, customer.getUsername(), getDigitalKey()), futureMap.getFutureMapInternalSessionId());

                            GregorianCalendar cal = new GregorianCalendar();
                            cal.add(Calendar.MONTH, 18);
                            Date endDate = cal.getTime();
                            cal.add(Calendar.MONTH, -24);

                            //
                            //startAsynchJob("GetPayments", paymentBean.getPayments(customer, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()), futureMap.getFutureMapInternalSessionId());
                            startAsynchJob(sessionID, "GetCustomerDetails", paymentBean.getCustomerDetails(customer, getDigitalKey(), sessionID));
                            startAsynchJob(sessionID, "ConvertSchedule", paymentBean.convertEzidebitScheduleToCrmSchedule(customer, cal.getTime(), endDate, getDigitalKey(), getFutureMapInternalSessionId()));

                        }

                        try {

                            Customers user = null;
                            if (remoteUser != null) {
                                user = customersFacade.findCustomerByUsername(remoteUser);
                            }

                            createCombinedAuditLogAndNote(user, customer, "Direct Debit Form", "The direct debit form has been completed and payments created.", "Not Registered in Payemnt Gateway", "Registered in payment gateway with scheduled payments");
                            /* try {
                                    controller.getSelected().getPaymentParametersId().setWebddrUrl(null);
                                } catch (Exception e) {
                                    logger.log(Level.WARNING, " Customer {0} . Setting Web DDR URL to NULL FAILED .: {1}", new Object[]{customer.getUsername(), e.getMessage()});
                                }*/
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "createCombinedAuditLogAndNote FAILED.", e);
                        }
                    }

                }
            }
        }

    }

    private Properties emailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.ssl.enable", configMapFacade.getConfig("mail.smtp.ssl.enable"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));
        props.put("mail.smtp.headerimage.url", configMapFacade.getConfig("mail.smtp.headerimage.url"));
        props.put("mail.smtp.headerimage.cid", configMapFacade.getConfig("mail.smtp.headerimage.cid"));
        return props;

    }

    public void createCombinedAuditLogAndNote(Customers adminUser, Customers customer, String title, String message, String changedFrom, String ChangedTo) {
        try {
            if (adminUser == null) {
                adminUser = customer;
                LOGGER.log(Level.WARNING, "Customers Controller, createCombinedAuditLogAndNote: The logged in user is NULL");
            }
            ejbAuditLogFacade.audit(adminUser, customer, title, message, changedFrom, ChangedTo);
            Notes note = new Notes(0);
            note.setCreateTimestamp(new Date());
            note.setCreatedBy(adminUser);
            note.setUserId(customer);
            note.setNote(message);
            ejbNotesFacade.create(note);
            customersFacade.edit(customer);
            //notesFilteredItems = null;
            //notesItems = null;

            //als.add("growl");
            // FacesContext fc = FacesContext.getCurrentInstance();// check this isnt originating from a web service call with no faces context.
            // if (fc != null) {
            //     PrimeFaces rc = PrimeFaces.customer();
            //      if (rc != null) {
            //          ArrayList<String> als = new ArrayList<>();
            //          als.add("@(.updateNotesDataTable)");
            //          rc.ajax().update(als);
            //      }
            //   }
            //  PrimeFaces.customer().ajax().update("@(.updateNotesDataTable)");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Customers Controller, createCombinedAuditLogAndNote: ", e);
        }

    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processConvertSchedule(String sessionId, PaymentGatewayResponse pgr) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        // synchronized (lock9) {

        ArrayOfScheduledPayment resultArraySchedPayments = null;
        boolean abort = false;
        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            // Object resultObject = ft.get();
            //if (resultObject != null) {
            //   if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //      PaymentGatewayResponse pgr = (PaymentGatewayResponse) resultObject;
            if (pgr.isOperationSuccessful()) {
                resultArraySchedPayments = (ArrayOfScheduledPayment) pgr.getData();

            }
            // }
            // }

        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Future Map processConvertSchedule:", ex);
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
                        LOGGER.log(Level.WARNING, "Future Map processConvertSchedule an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                    }

                    Customers cust = customersFacade.findById(custId);
                    if (cust != null) {
                        LOGGER.log(Level.INFO, "Future Map processConvertSchedule. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                        for (ScheduledPayment pay : payList) {
                            if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                LOGGER.log(Level.WARNING, "Future Map processConvertSchedule . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
                                abort = true;
                            }

                        }
                        if (abort == false) {
                            /* AsyncJob aj = new AsyncJob("ClearSchedule", paymentBean.clearSchedule(cust, false, cust.getUsername(), getDigitalKey(), sessionId));
                            this.put(FUTUREMAP_INTERNALID, aj);
                            try {
                                TimeUnit.MILLISECONDS.sleep(100);
                            } catch (InterruptedException ex) {
                                Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "Thread Interrupted", ex);
                            }*/

                            Future<PaymentGatewayResponse> clearScheduleResponse = paymentBean.clearSchedule(cust, false, cust.getUsername(), getDigitalKey(), sessionId);
                            try {
                                PaymentGatewayResponse clearSchedule = clearScheduleResponse.get();

                                if (clearSchedule.isOperationSuccessful()) {
                                    Logger.getLogger(PaymentBean.class
                                            .getName()).log(Level.INFO, "Payment Schedule Cleared Successfully - Cust ID={0}, Username {1}", new Object[]{cust.getId(), cust.getUsername()});

                                } else {
                                    Logger.getLogger(PaymentBean.class
                                            .getName()).log(Level.SEVERE, "Payment Schedule Clear FAILED - Cust ID={0}, Username {1}", new Object[]{cust.getId(), cust.getUsername()});

                                }
                            } catch (InterruptedException | ExecutionException ex) {
                                Logger.getLogger(FutureMapEJB.class
                                        .getName()).log(Level.SEVERE, "processConvertSchedule", ex);
                            }

                            ArrayList<Payments> newPayments = new ArrayList<>();
                            for (ScheduledPayment pay : payList) {

                                //payment doesn't exist in crm so add it
                                LOGGER.log(Level.WARNING, "Future Map processConvertSchedule - Adding Payment to CRM DB - EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{pay.getEzidebitCustomerID().getValue(), pay.getYourSystemReference().getValue(), pay.getPaymentAmount().floatValue(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue()});
                                Payments crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                paymentsFacade.createAndFlushForGeneratedIdEntities(crmPay);
                                try {
                                    TimeUnit.MILLISECONDS.sleep(200);

                                } catch (InterruptedException ex) {
                                    Logger.getLogger(FutureMapEJB.class
                                            .getName()).log(Level.SEVERE, "Thread Sleep interrupted", ex);
                                }
                                crmPay.setPaymentReference(crmPay.getId().toString());
                                crmPay.setManuallyAddedPayment(false);
                                // paymentsFacade.edit(crmPay);

                                newPayments.add(crmPay);

                            }
                            for (Payments newPay : newPayments) {
                                /* Future<PaymentGatewayResponse> addPayResponse = paymentBean.addPayment(cust, crmPay.getDebitDate(), amountLong, crmPay, cust.getUsername(), getDigitalKey(), sessionId);
                                try {
                                    PaymentGatewayResponse appPay = addPayResponse.get();
                                    if (appPay.isOperationSuccessful()) {
                                        Logger.getLogger(PaymentBean.class.getName()).log(Level.INFO, "Payment Added Successfully - ID=", new Object[]{crmPay.getId()});
                                    } else {
                                        Logger.getLogger(PaymentBean.class.getName()).log(Level.SEVERE, "Payment Add FAILED - ID=", new Object[]{crmPay.getId()});
                                    }
                                } catch (InterruptedException | ExecutionException ex) {
                                    Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "processConvertSchedule", ex);
                                }*/
                                try {
                                    LOGGER.log(Level.WARNING, "Future Map processConvertSchedule - Adding Payment to Payment Gateway - EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{cust, newPay.getId(), newPay.getScheduledAmount().floatValue(), newPay.getDebitDate().getTime(), newPay.getPaymentReference()});
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Future Map processConvertSchedule ");
                                }
                                Long amountLong = null;
                                try {
                                    amountLong = newPay.getScheduledAmount().movePointRight(2).longValue();
                                } catch (Exception e) {
                                    LOGGER.log(Level.WARNING, "Arithemtic error.");
                                }
                                startAsynchJob(sessionId, "AddPayment", paymentBean.addPayment(cust, newPay.getDebitDate(), amountLong, newPay, cust.getUsername(), getDigitalKey(), sessionId));

                            }
                            startAsynchJob(sessionId, "GetCustomerDetails", paymentBean.getCustomerDetails(cust, getDigitalKey(), sessionId));
                            getPayments(sessionId, cust, 12, 2);
                        }
                    }
                } else {
                    LOGGER.log(Level.SEVERE, "Future Map processConvertSchedule couldn't find a customer with our system ref from payment.");
                    /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                     as this means that a customer is in ezidebits system but not ours */

                }
            } else {
                LOGGER.log(Level.WARNING, "Future Map processConvertSchedule our system ref in payment is null.");
            }

        }

        LOGGER.log(Level.INFO,
                "processConvertSchedule completed");
        // }
    }

    // @TransactionAttribute(TransactionAttributeType.NEVER)
    public void processCompletedAsyncJobs(String sessionId, String key, Future<PaymentGatewayResponse> ft) {
        LOGGER.log(Level.INFO, "Future Map is processing Completed Async Jobs .");
        synchronized (lock3) {
// TODO convert all methods to use PaymentGatewayResponse class
            try {

                if (key.contains("ChangeScheduledAmount")) {
                    // processChangeScheduledAmount(sessionId, ft);
                }
                if (key.contains("ChangeScheduledDate")) {
                    // processChangeScheduledDate(sessionId, ft);
                }
                if (key.contains("GetPaymentExchangeVersion")) {
                    //processGetPaymentExchangeVersion(sessionId, ft);
                }

                if (key.contains("GetPaymentStatus")) {
                    //  processGetPaymentStatus(sessionId, ft);
                    LOGGER.log(Level.INFO, "GetPaymentStatus has been migrated to a direct call from payment bean.");
                }
                if (key.contains("IsBsbValid")) {
                    //processIsBsbValid(sessionId, ft);
                }
                if (key.contains("IsSystemLocked")) {
                    // processIsSystemLocked(sessionId, ft);
                }
                if (key.contains("EmailAlert")) {
                    // processEmailAlert(sessionId, ft);
                }
                // NOTE: Most of these have been moved to direct cals from the payment bean for improved performance 
                if (key.contains("GetCustomerDetails")) {
                    //   processGetCustomerDetails(sessionId, ft);
                    LOGGER.log(Level.INFO, "GetCustomerDetails to direct call from payment bean.");
                }
                if (key.contains("GetPayments")) {
                    // processGetPayments(sessionId, ft);
                    LOGGER.log(Level.INFO, "GetPayments to direct call from payment bean.");
                }
                if (key.contains("GetScheduledPayments")) {
                    // processGetScheduledPayments(sessionId, ft);
                    LOGGER.log(Level.INFO, "GetScheduledPayments to direct call from payment bean.");
                }
                if (key.contains("PaymentReport")) {
                    // processPaymentReport(sessionId, ft);
                    LOGGER.log(Level.INFO, "PaymentReport has been migrated to a direct call from payment bean.");
                }
                if (key.contains("SettlementReport")) {
                    // processSettlementReport(sessionId, ft);
                    LOGGER.log(Level.INFO, "SettlementReport has been migrated to a direct call from payment bean.");
                }
                // this is only run by the system so sessionId is not needed.
                if (key.contains("ConvertSchedule")) {
                    // processConvertSchedule(ft, sessionId);
                    LOGGER.log(Level.INFO, "ConvertSchedule has been migrated to a direct call from payment bean.");
                }
                if (key.contains("AddCustomer")) {
                    //processAddCustomer(sessionId, ft);
                    LOGGER.log(Level.INFO, "AddCustomer  has been migrated to a direct call from payment bean.");
                }
                if (key.contains("AddPayment")) {
                    //  processAddPaymentResult(sessionId, ft);
                    LOGGER.log(Level.INFO, "AddPayment to direct call from payment bean.");
                }
                if (key.contains("ClearSchedule")) {
                    //  processClearSchedule(sessionId, ft);
                    LOGGER.log(Level.INFO, "ClearSchedule to direct call from payment bean.");
                }
                if (key.contains("CreateSchedule")) {
                    //processCreateSchedule(sessionId, ft);
                    LOGGER.log(Level.INFO, "CreateSchedule to direct call from payment bean.");
                }
                if (key.contains("ChangeCustomerStatus")) {
                    // processChangeCustomerStatus(sessionId, ft);
                    LOGGER.log(Level.INFO, "ChangeCustomerStatus to direct call from payment bean.");
                }

                if (key.contains("DeletePayment")) {
                    //processDeletePayment(sessionId, ft);
                    LOGGER.log(Level.INFO, "DeletePayment to direct call from payment bean.");
                }
                if (key.contains("DeletePaymentBatch")) {
                    // processDeletePaymentBatch(sessionId, ft);
                }
                if (key.contains("AddPaymentBatch")) {
                    // processAddPaymentBatch(sessionId, ft);
                }

            } catch (Exception ex) {
                LOGGER.log(Level.WARNING, "Key:" + key + ", Future Map: processCompletedAsyncJobs", ex);

            }
        }
    }

    // run a schedules
    /*  public void processCompletedAsyncJobs2(String sessionId) {
     LOGGER.log(Level.INFO, "Future Map is processing Completed Async Jobs .");
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
     LOGGER.log(Level.WARNING, key + " Future Map:", ex);

     }
     }
     }*/
    public void processSettlementReport(String sessionId, PaymentGatewayResponse pgr) {
        synchronized (lock7) {
            LOGGER.log(Level.INFO, "Future Map is processing the Settlement Report .");
            processReport(sessionId, pgr);
            LOGGER.log(Level.INFO, "Future Map has finished asyc processing the Settlement Report .");
            settlementReportLock.set(false);
        }
    }

    public void processPaymentReport(String sessionId, PaymentGatewayResponse pgr) {
        synchronized (lock7) {
            LOGGER.log(Level.INFO, "Future Map is processing the Payment Report .");
            processReport(sessionId, pgr);
            LOGGER.log(Level.INFO, "Future Map has finished async processing the Payment Report .");
            paymentReportLock.set(false);
        }
    }

    public synchronized void processGetPaymentStatus(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        if (pgr != null) {
            try {
                //Object resultObject = ft.get();
                //if (resultObject != null) {

                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                // }
                // }

            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            sendMessage(sessionId, "Payment Gateway", "Successfully Retrieved Payment Status  .");

        } else {
            sendMessage(sessionId, "Payment Gateway", "The getPayment status operation failed!.");
        }
        //recreatePaymentTableData();
        LOGGER.log(Level.INFO, "processGetPaymentStatus completed");
    }

    public synchronized void processChangeScheduledAmount(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        Customers cust = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
                // }

            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("ChangeScheduledAmount", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully  Changed Scheduled Amount .");

            getPayments(sessionId, cust, 18, 2);
        } else {
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        //recreatePaymentTableData();
        // updateScheduleLock.set(false);
        LOGGER.log(Level.INFO, "processChangeScheduledAmount completed");
    }

    public synchronized void processChangeScheduledDate(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        // PaymentGatewayResponse pgr = null;
        Customers cust = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
                // }

            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("ChangeScheduledDate", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully Changed Scheduled Date  .");
            getPayments(sessionId, cust, 18, 2);
        } else {
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processChangeScheduledDate completed");
    }

    public synchronized void processIsBsbValid(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();

                // }
            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("IsBsbValid", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully Checked BSB  .");

        } else {
            storeResponseForSessionBeenToRetrieve("IsBsbValid", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processIsBsbValid completed");
    }

    public synchronized void processIsSystemLocked(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();

                // }
            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("IsSystemLocked", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Successfully checked if System is Locked  .");

        } else {
            storeResponseForSessionBeenToRetrieve("IsSystemLocked", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processIsSystemLocked completed");
    }

    @Asynchronous
    public void processEmailAlert(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();

                // }
            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result == true) {
            storeResponseForSessionBeenToRetrieve("EmailAlert", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway Alert", "Successfully sent Alert Email to Admin users.");

        } else {
            storeResponseForSessionBeenToRetrieve("EmailAlert", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Failed to Send Alert Email!.");
        }
        LOGGER.log(Level.INFO, "processEmailAlert completed");
    }

    public synchronized void processGetPaymentExchangeVersion(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        String versionInfo = "";
        //PaymentGatewayResponse pgr = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //     pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                versionInfo = pgr.getTextData();
                //}

            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (versionInfo != null && versionInfo.trim().isEmpty() == false) {
            storeResponseForSessionBeenToRetrieve("GetPaymentExchangeVersion", sessionId, pgr);
            sendMessage(sessionId, "Payment Gateway", "Payment Exchange Version: " + versionInfo);

        } else {

            sendMessage(sessionId, "Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processGetPaymentExchangeVersion completed");
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    private void processReport(String sessionId, PaymentGatewayResponse pgr) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfPayment resultArrayPayments = null;
        boolean result = false;
        paymentsByCustomersMissingFromCRM = new ArrayList<>();
        //PaymentGatewayResponse pgr = null;
        if (pgr != null) {
            try {
                // if successful it should return a Customers Object from the getData method;
                // Object resultObject = ft.get();
                // if (resultObject.getClass() == PaymentGatewayResponse.class) {
                //   pgr = (PaymentGatewayResponse) resultObject;
                result = pgr.isOperationSuccessful();
                resultArrayPayments = (ArrayOfPayment) pgr.getData();
                //}

            } catch (Exception ex) {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "processReport", ex);
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
                            LOGGER.log(Level.WARNING, "Future Map processReport an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                        }
                        Customers cust = null;
                        try {
                            cust = customersFacade.findById(custId);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Future Map processReport customersFacade.findById(custId) Error: custId={0}, Exception={1}", new Object[]{custId, e.getMessage()});
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
                                        } else {
                                            LOGGER.log(Level.SEVERE, "Future Map processReport  - paymentReference NOT FOUND in our system:{0}.", new Object[]{paymentRefInt, pay.getCustomerName().getValue(), pay.getPaymentAmount().toString(), pay.getDebitDate().getValue().toGregorianCalendar().getTime().toString()});
                                        }
                                    } catch (NumberFormatException numberFormatException) {
                                        LOGGER.log(Level.SEVERE, "Future Map processReport  - paymentReference NOT NUMERICAL:{0} , exception Message: {1}", new Object[]{paymentRefInt, numberFormatException.getMessage()});

                                    }
                                }
                            }
                            if (validReference) {
                                if (comparePaymentXMLToEntity(crmPay, pay) == false) {
                                    crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                    LOGGER.log(Level.INFO, "Future Map processReport  - updating payment id:{0}.", paymentRefInt);
                                    try {
                                        paymentsFacade.edit(crmPay);

                                    } catch (Exception e) {
                                        LOGGER.log(Level.WARNING, "Future Map processReport - edit payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});
                                    }
                                }
                            } else // old payment without a primary key reference
                            {
                                if (paymentID.toUpperCase(Locale.getDefault()).contains("SCHEDULED")) {
                                    // scheduled payment no paymentID
                                    LOGGER.log(Level.INFO, "Future Map processReport scheduled payment .", pay.toString());
                                } else {

                                    crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);

                                    if (crmPay != null) { //' payment exists
                                        if (comparePaymentXMLToEntity(crmPay, pay)) {
                                            // they are the same so no update
                                            LOGGER.log(Level.FINE, "Future Map processReport paymenst are the same.");
                                        } else {
                                            crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                            try {
                                                paymentsFacade.edit(crmPay);

                                            } catch (Exception e) {
                                                LOGGER.log(Level.WARNING, "Future Map processReport - edit payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});
                                            }
                                        }
                                    } else { //payment doesn't exist in crm so add it
                                        LOGGER.log(Level.SEVERE, "Future Map processReport  - payment doesn't exist in crm (this should only happen for webddr form schedule) so adding it:{0}.", paymentRefInt);
                                        //crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                        try {
                                            //paymentsFacade.createAndFlush(crmPay);
                                        } catch (Exception e) {
                                            LOGGER.log(Level.WARNING, "Future Map processReport - create payment {0} , Exception {1}.", new Object[]{crmPay.getId().toString(), e.getMessage()});

                                        }
                                    }
                                }
                            }
                            /* String paymentID = pay.getPaymentID().getValue();
                         if (paymentID.toUpperCase().contains("SCHEDULED")) {
                         // scheduled payment no paymentID
                         LOGGER.log(Level.INFO, "Future Map processReport scheduled payment .", pay.toString());
                         } else {
                         Payments crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);
                         if (crmPay != null) { //' payment exists
                         if (comparePaymentXMLToEntity(crmPay, pay)) {
                         // they are the same so no update
                         LOGGER.log(Level.FINE, "Future Map processReport paymenst are the same.");
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
                            LOGGER.log(Level.SEVERE, "Future Map processReport couldn't find a customer with our system ref from payment.EziDebit Ref No: {0}", eziRef);
                            /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

                        }
                    }
                    result = true;
                } else {
                    LOGGER.log(Level.WARNING, "Future Map processReport couldn't find any Payments.");

                }
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
        LOGGER.log(Level.INFO, "Future Map processReport completed");
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processGetPayments(String sessionId, PaymentGatewayResponse pgr) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        boolean result = true;
        //PaymentGatewayResponse pgr = null;

        ArrayOfPayment resultPaymentArray;
        boolean abort = false;

        try {

            // if successful it should return a ArrayOfPayment Object from the getData method;
            // Object resultObject = ft.get();
            // if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //   pgr = (PaymentGatewayResponse) resultObject;
            //}
            if (pgr != null) {
                /* resultPaymentArray = (ArrayOfPayment) pgr.getData();
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
                                LOGGER.log(Level.WARNING, "Future Map processGetPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                            }

                            Customers cust = customersFacade.findById(custId);
                            if (cust != null) {
                                LOGGER.log(Level.INFO, "Future Map processGetPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                                for (Payment pay : payList) {
                                    if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
                                        LOGGER.log(Level.WARNING, "Future Map processGetPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
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
                                                LOGGER.log(Level.INFO, "Future Map processGetPayments  - updating payment id:{0}.", paymentRefInt);
                                                paymentsFacade.edit(crmPay);
                                            }
                                        } else // old payment without a primary key reference
                                        {
                                            if (paymentID.toUpperCase(Locale.getDefault()).contains("SCHEDULED")) {
                                                // scheduled payment no paymentID
                                                LOGGER.log(Level.INFO, "Future Map processGetPayments scheduled payment .", pay.toString());
                                            } else {

                                                crmPay = paymentsFacade.findPaymentByPaymentId(paymentID);

                                                if (crmPay != null) { //' payment exists
                                                    if (comparePaymentXMLToEntity(crmPay, pay)) {
                                                        // they are the same so no update
                                                        LOGGER.log(Level.FINE, "Future Map processGetPayments paymenst are the same.");
                                                    } else {
                                                        crmPay = convertPaymentXMLToEntity(crmPay, pay, cust);
                                                        paymentsFacade.edit(crmPay);
                                                    }
                                                } else { //payment doesn't exist in crm so add it
                                                    LOGGER.log(Level.WARNING, "Future Map processGetPayments  - payment doesn't exist in crm (this should only happen for webddr form schedule) so adding it:{0}.", paymentRefInt);
                                                    crmPay = convertPaymentXMLToEntity(null, pay, cust);
                                                    paymentsFacade.createAndFlushForGeneratedIdEntities(crmPay);
                                                }
                                            }
                                        }
                                    }
                                }
                            } else {
                                LOGGER.log(Level.SEVERE, "Future Map processGetPayments couldn't find a customer with our system ref ({0}) from payment.", customerRef);
                                /*TODO email a report at the end of the process if there are any payments swithout a customer reference
                         as this means that a customer is in ezidebits system but not ours */

 /*      }
                        } else {
                            LOGGER.log(Level.WARNING, "Future Map processGetPayments our system ref in payment is null.");
                        }

                    }
                }*/
                result = pgr.isOperationSuccessful();
                LOGGER.log(Level.INFO, "Future Map processGetPayments completed");

                if (result) {
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
                Logger
                        .getLogger(EziDebitPaymentGateway.class
                                .getName()).log(Level.SEVERE, message);

                sendMessage(sessionId, "Get Payments Error", message);

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Future Map processGetPayments FAILED", ex);
        }
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processGetScheduledPayments(String sessionId, PaymentGatewayResponse pgr) {
        // Update the payments table with any new information retrived by the getPayments exzidebit web service.
        // Only for one customer.
        ArrayOfScheduledPayment resultArrayOfScheduledPayments = null;
        boolean abort = false;
        boolean result;
        // PaymentGatewayResponse pgr = null;

        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            // Object resultObject = ft.get();
            //  if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //      pgr = (PaymentGatewayResponse) resultObject;

            //  }
            if (pgr != null) {
                resultArrayOfScheduledPayments = (ArrayOfScheduledPayment) pgr.getData();
                result = pgr.isOperationSuccessful();

                // MOved to Payments Bean

                /*if (resultArrayOfScheduledPayments != null && resultArrayOfScheduledPayments.getScheduledPayment() != null) {
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
                                LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

                            }

                            Customers cust = customersFacade.findById(custId);
                            if (cust != null) {
                                LOGGER.log(Level.INFO, "Future Map processGetScheduledPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
                                for (ScheduledPayment pay : payList) {
                                    int paymentCustRef;
                                    try {
                                        paymentCustRef = Integer.parseInt(pay.getYourSystemReference().getValue());
                                    } catch (NumberFormatException numberFormatException) {
                                        paymentCustRef = -1;
                                    }
                                    if (custId != paymentCustRef) {
                                        LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
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
                                            LOGGER.log(Level.INFO, "Future Map processGetScheduledPayments  - found a payment without a valid reference", id);
                                        }

                                        if (crmPay != null) {
                                            if (compareScheduledPaymentXMLToEntity(crmPay, pay)) {
                                                crmPay = convertScheduledPaymentXMLToEntity(crmPay, pay, cust);
                                                LOGGER.log(Level.INFO, "Future Map processGetScheduledPayments  - updateing scheduled payment id:", id);
                                                paymentsFacade.edit(crmPay);
                                            }
                                        } else {
                                            crmPay = paymentsFacade.findScheduledPaymentByCust(pay, cust);
                                            if (crmPay != null) { //' payment exists
                                                if (compareScheduledPaymentXMLToEntity(crmPay, pay)) {
                                                    // they are the same so no update
                                                    LOGGER.log(Level.FINE, "Future Map processGetScheduledPayments paymenst are the same.");
                                                } else {
                                                    crmPay = convertScheduledPaymentXMLToEntity(crmPay, pay, cust);
                                                    paymentsFacade.edit(crmPay);
                                                }
                                            } else { //payment doesn't exist in crm so add it
                                                LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments - A payment exists in the PGW but not in CRM.(This can be ignored if a customer is onboarded with the online eddr form) EzidebitID={0}, CRM Ref:{1}, Amount={2}, Date={3}, Ref={4}", new Object[]{pay.getEzidebitCustomerID().getValue(), pay.getYourSystemReference().getValue(), pay.getPaymentAmount().floatValue(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue()});
                                                crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
                                                paymentsFacade.createAndFlush(crmPay);
                                                crmPay.setPaymentReference(crmPay.getId().toString());
                                                crmPay.setManuallyAddedPayment(false);
                                                paymentsFacade.edit(crmPay);
                                                Long amountLong = null;
                                                try {
                                                    amountLong = crmPay.getPaymentAmount().movePointRight(2).longValue();
                                                } catch (Exception e) {
                                                    LOGGER.log(Level.WARNING, "Arithemtic error.");
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
                                                if (cust.getPaymentParametersId().getStatusDescription().toUpperCase().contains("HOLD")) {
                                                    p.setPaymentStatus(PaymentStatus.REJECTED_CUST_ON_HOLD.value());
                                                } else {
                                                    String message = "This payment exists in our database but not in the payment gateway so it won't be processed.Customer " + cust.getUsername() + ", Payment ID:" + p.getId().toString() + " for Amount:$" + p.getPaymentAmount().toPlainString() + " on Date:" + p.getDebitDate().toString() + " was rejected by the payment gateway and requires your action or revenue loss may occur!!.";

                                                    sendAlertEmailToAdmin(message);
                                                }
                                                paymentsFacade.edit(p);
                                            }
                                            //AsyncJob aj = new AsyncJob("DeletePayment", paymentBean.deletePaymentByRef(cust, ref, "system", getDigitalKey()));
                                            //this.put(FUTUREMAP_INTERNALID, aj);

                                        }
                                    }
                                }
                                updateNextScheduledPayment(cust);
                            } else {
                                LOGGER.log(Level.SEVERE, "Future Map processGetScheduledPayments couldn't find a customer with our system ref from payment.");
                                //TODO email a report at the end of the process if there are any payments swithout a customer reference
                         //as this means that a customer is in ezidebits system but not ours 

                            }
                        } else {
                            LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments our system ref in payment is null.");
                        }

                    }
                }*/
                LOGGER.log(Level.INFO, "Future Map processGetScheduledPayments completed");
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
                Logger
                        .getLogger(EziDebitPaymentGateway.class
                                .getName()).log(Level.SEVERE, message);

                sendMessage(sessionId, "Get Scheduled Payments Error", message);

            }
        } catch (Exception ex) {
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
     LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments an ezidebit YourSystemReference string cannot be converted to a number.", numberFormatException);

     }

     Customers cust = customersFacade.findById(custId);
     if (cust != null) {
     LOGGER.log(Level.INFO, "Future Map processGetScheduledPayments. Processing {0} payments for customer {1}.", new Object[]{payList.size(), cust.getUsername()});
     for (ScheduledPayment pay : payList) {
     if (customerRef.compareTo(pay.getYourSystemReference().getValue().trim()) != 0) {
     LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments . The list being processed contains multiple customers.It should only contain one for this method. Aborting.");
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
     LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments - Failed to delete some scheduled payments. ExistedInCRM={0}, Deleted={1},Existeding in Payment Gateway={2}", new Object[]{existingInCRM, numberDeleted, scheduledPayments});

     }
     for (ScheduledPayment pay : payList) {
     Payments crmPay = convertScheduledPaymentXMLToEntity(null, pay, cust);
     paymentsFacade.create(crmPay);
     createScheduledPayments++;
     }
     if (createScheduledPayments != scheduledPayments) {

     LOGGER.log(Level.WARNING, "Future Map processGetScheduledPayments - The number of payments created does not equal the number retrieved from the payment gateway. Retireved={1}, Created={2}, Existed In CRM and were deleted={0}", new Object[]{existingInCRM, createScheduledPayments, scheduledPayments});

     }

     }
     } else {
     LOGGER.log(Level.SEVERE, "Future Map processGetScheduledPayments couldn't find a customer with our system ref from payment.");
     /*TODO email a report at the end of the process if there are any payments swithout a customer reference
     as this means that a customer is in ezidebits system but not ours */

 /*  }
     }
     }
     }

     LOGGER.log(Level.INFO, "processGetScheduledPayments completed");
     }
     }n */
 /*  private synchronized void createDefaultPaymentParameters(Customers customer) {

        if (customer == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer is NULL.");
            return;
        }

        PaymentParameters pp;

        try {

            String phoneNumber = customer.getTelephone();
            if (phoneNumber == null) {
                phoneNumber = "0000000000";
                LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", customer.getUsername());
            }
            Pattern p = Pattern.compile("\\d{10}");
            Matcher m = p.matcher(phoneNumber);
            //ezidebit requires an australian mobile phone number that starts with 04
            if (m.matches() == false || phoneNumber.startsWith("04") == false) {
                phoneNumber = "0000000000";
                LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", customer.getUsername());
            }
            pp = new PaymentParameters();
            pp.setId(0);
            pp.setWebddrUrl(null);
            pp.setCustomers(customer);
            pp.setLastSuccessfulScheduledPayment(paymentsFacade.findLastSuccessfulScheduledPayment(customer));
            pp.setNextScheduledPayment(paymentsFacade.findNextScheduledPayment(customer));
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
            customer.setPaymentParametersId(pp);
            customersFacade.editAndFlush(customer);

        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "createDefaultPaymentParameters Method in Customers Controller", e);
        }
    }*/

 /* public synchronized void processAddCustomer(String sessionId, Future<PaymentGatewayResponse> ft) {
        try {
            PaymentGatewayResponse pgr = null;
            Object resultObject = ft.get();
            if (resultObject != null) {
                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    pgr = (PaymentGatewayResponse) resultObject;
                    processAddCustomerAsync(sessionId, pgr);
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(FutureMapEJB.class.getName()).log(Level.SEVERE, "processDeletePaymentFuture Method", ex);
        }
    }*/
    @Asynchronous
    // @TransactionAttribute(REQUIRES_NEW)
    public void processAddCustomer(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        Customers cust;
        try {
            // if successful it should return a Customers Object from the getData method;
            // Object resultObject = ft.get();
            //  if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //     pgr = (PaymentGatewayResponse) resultObject;
            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                // }

                if (result == true) {

                    cust = (Customers) pgr.getData();
                    startAsynchJob(sessionId, "GetCustomerDetails", paymentBean.getCustomerDetails(cust, getDigitalKey(), sessionId));
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
                LOGGER.log(Level.SEVERE, "processAddCustomer : The PaymentGatewayResponse Object is NULL");
                String message = "The PaymentGatewayResponse Object is NULL";
                sendMessage(sessionId, "Add Customer Error!", message);
            }
        } catch (Exception ex) {
            LOGGER.log(Level.WARNING, "processAddCustomer", ex);
        }
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processChangeCustomerStatus(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        //PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            //Object resultObject = ft.get();
            // if (resultObject != null) {
            //      if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //pgr = (PaymentGatewayResponse) resultObject;
            result = pgr.isOperationSuccessful();
            Object o = pgr.getData();

            if (o != null) {
                if (o.getClass() == Customers.class) {
                    cust = (Customers) o;

                }

            }
            // }
            // }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            String message = pgr.getTextData();
            LOGGER.log(Level.INFO, "Successfully Changed Customer Status.Sending request to refresh details and payments from payemnt gateway.");
            startAsynchJob(sessionId, "GetCustomerDetails", paymentBean.getCustomerDetails(cust, getDigitalKey(), sessionId));
            storeResponseForSessionBeenToRetrieve("ChangeCustomerStatus", sessionId, pgr);
            sendMessage(sessionId, "Change Customer Status", message);
        } else {
            String message = "The change status operation failed!.";
            LOGGER.log(Level.WARNING, message);

            sendMessage(sessionId, "Change Customer Status Error!", message);
        }
        LOGGER.log(Level.INFO, "FutureMap - processChangeCustomerStatus completed");
    }

    public synchronized void processDeletePayment(String sessionId, Future<PaymentGatewayResponse> ft) {
        try {
            PaymentGatewayResponse pgr = null;
            Object resultObject = ft.get();

            if (resultObject != null) {
                if (resultObject.getClass() == PaymentGatewayResponse.class) {
                    pgr = (PaymentGatewayResponse) resultObject;
                    processDeletePaymentAsync(sessionId, pgr);

                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(FutureMapEJB.class
                    .getName()).log(Level.SEVERE, "processDeletePaymentFuture Method", ex);
        }
    }

    @Asynchronous
    // @TransactionAttribute(REQUIRES_NEW)
    public void processDeletePaymentAsync(String sessionId, PaymentGatewayResponse pgr) {

        String paymentRef = null;
        boolean result = false;
        Payments pay = null;
        Payments paymentToBeProcessed = null;
        String message = "The delete payment operation failed!.";
        LOGGER.log(Level.INFO, "FutureMap - processAddPaymentResult started");
        // PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            // Object resultObject = ft.get();
            // if (resultObject != null) {
            //     if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //         pgr = (PaymentGatewayResponse) resultObject;
            result = pgr.isOperationSuccessful();
            //we have a response 
            paymentRef = pgr.getTextData();
            paymentToBeProcessed = (Payments) pgr.getData();
            //  }
            // }

            if (paymentRef == null || paymentRef.isEmpty() || paymentRef.trim().isEmpty()) {
                //this should always be set and will be the primary key of teh row in the payments table
                LOGGER.log(Level.WARNING, "FutureMap - processDeletePayment FAILED - paymentRef IS NULL or Empty");
                return;
            }

            if (result == true) {

                int reference = -1;
                try {
                    reference = Integer.parseInt(paymentRef);

                } catch (NumberFormatException numberFormatException) {
                    LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment - Thepayment reference could not be converted to a number: {0}", new Object[]{paymentRef});
                }
                pay = paymentsFacade.findPaymentById(reference, false);
                if (pay != null) {
                    //removeFromPaymentLists(pay);
                    paymentsFacade.remove(pay);

                } else {
                    LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment - Payment that was deleted could not be found in the our DB key={0}, RETRYING - BYPASSING CACHE", new Object[]{reference});
                    pay = paymentsFacade.findPaymentById(reference, true);
                    if (pay != null) {
                        //removeFromPaymentLists(pay);
                        paymentsFacade.remove(pay);

                    } else {
                        LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment - Payment that was deleted could not be found in the our DB key={0}", new Object[]{reference});
                    }

                }
                // setSelectedScheduledPayment(null);
                //JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Deleted Payment  .");
                message = "The payment was successfully deleted.";
                storeResponseForSessionBeenToRetrieve("DeletePayment", sessionId, pgr);
                sendMessage(sessionId, "Delete Payment", message);
                //getPayments(18, 2);

            } else if (pgr != null) {
                LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment - DELETE PAYMENT FAILED: {0}", new Object[]{result});
                String errorMessage = pgr.getErrorMessage();
                sendMessage(sessionId, "Delete Payment FAILED", errorMessage);
                int id = 0;
                try {
                    id = Integer.parseInt(paymentRef);
                } catch (NumberFormatException numberFormatException) {
                    LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment  FAILED - PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", result);
                }
                pay = paymentsFacade.findPaymentById(id, false);
                if (pay != null) {

                    if (errorMessage.contains("Payment selected for deletion could not be found")) {
                        //JsfUtil.addErrorMessage("Payment Gateway", "A payment with this reference could not be found in the payment gateway!");

                        if (pay.getBankFailedReason().contentEquals("MISSING")) {
                            paymentsFacade.remove(pay);
                            //  removeFromPaymentLists(pay);
                        } else {
                            if (pay.getPaymentStatus().contentEquals(PaymentStatus.DELETE_REQUESTED.value()) == false) {
                                pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                                paymentsFacade.editAndFlush(pay);
                                //  updatePaymentLists(pay);
                            }
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
                                startAsynchJob(sessionId, "AddPayment", paymentBean.deletePayment(pay.getCustomerName(), pay.getDebitDate(), pay.getScheduledAmount().movePointRight(2).longValue(), pay, "Auto Retry", getDigitalKey(), sessionId));

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
                        LOGGER.log(Level.WARNING, "FutureMap - Process deletePayment  FAILED - Unhandled error ", result);
                        message = "Could not delete the payment after 10 attempts";
                        sendMessage(sessionId, "Delete Payment", message);
                        //JsfUtil.addSuccessMessage("Payment Gateway", "Deleted Payment Error - see logs for more details .");
                    }

                }
            }
        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, "FutureMap - processDeletePayment FAILED", ex);
        }
        LOGGER.log(Level.INFO, "FutureMap - processDeletePayment completed");
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processAddPaymentResult(String sessionId, PaymentGatewayResponse pgr) {
        String paymentRef;
        Payments pay = null;
        boolean result = false;
        LOGGER.log(Level.INFO, "FutureMap - processAddPaymentResult started");
        //PaymentGatewayResponse pgr = null;
        try {
            // if successful it should return a Payment Object from the getData method;
            // Object resultObject = ft.get();
            /// if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //   pgr = (PaymentGatewayResponse) resultObject;

            // }
            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                pay = (Payments) pgr.getData();
                //we have a response 
                paymentRef = pgr.getTextData();

                if (paymentRef == null) {
                    //this should always be set and will be the primary key of the row in the payments table
                    LOGGER.log(Level.SEVERE, "processAddPaymentResult FAILED - RESULT IS NULL or Empty. This should never happen");
                    return;
                }
                if (result == false) {

                    String errorMessage = "Error Code: " + pgr.getErrorCode() + ", " + pgr.getErrorMessage();
                    int id = 0;
                    try {
                        id = Integer.parseInt(paymentRef);
                    } catch (NumberFormatException numberFormatException) {
                        LOGGER.log(Level.SEVERE, "processAddPaymentResult FAILED - PaymentReference could not be converted to a number. It should be the primary key of the payments table row. This is the string:", paymentRef);
                    }
                    //Payments pay = paymentsFacade.findPaymentById(id, false);
                    if (pay != null) {
                        LOGGER.log(Level.INFO, "FutureMap - processAddPaymentResult FAILED TO ADD PAYMENT - processing error for:{0}", new Object[]{paymentRef});
                        if (errorMessage.contains("cannot process your add payment request at this time.")) {
                            if (pay.getBankReturnCode() == null) {
                                pay.setBankReturnCode("");
                            }
                            if (pay.getBankReturnCode().trim().isEmpty()) {
                                // first attempt at retry so set counter to 0
                                pay.setBankReturnCode("0");
                            } else {
                                int retries = Integer.parseInt(pay.getBankReturnCode().trim());
                                retries++;
                                pay.setBankReturnCode(Integer.toString(retries));
                                // paymentsFacade.editAndFlush(pay);
                                if (retries < 10) {
                                    int sleeptime = 1000 * retries * retries;
                                    try {
                                        LOGGER.log(Level.WARNING, "processAddPaymentResult for {0} - PAYMENT GATEWAY BUSY - ATTEMPTING RETRY IN {1} Milliseconds ", new Object[]{paymentRef, sleeptime});

                                        TimeUnit.MILLISECONDS.sleep(sleeptime);
                                    } catch (InterruptedException e) {
                                    }
                                    startAsynchJob(sessionId, "AddPayment", paymentBean.addPayment(pay.getCustomerName(), pay.getDebitDate(), pay.getScheduledAmount().movePointRight(2).longValue(), pay, "Auto Retry", getDigitalKey(), sessionId));
                                } else {
                                    LOGGER.log(Level.SEVERE, "processAddPaymentResult PAYMENT GATEWAY BUSY - FAILED - SEVERAL RETRIES WERE PERFORMED - ", paymentRef);
                                    String message = "The payment gateway rejected the payment as it was busy. Several retries were made but the gateway continued to respond with a message stating it was busy. Payment ID:" + pay.getId().toString() + " for Amount:$" + pay.getPaymentAmount().toPlainString() + " on Date:" + pay.getDebitDate().toString() + " could not be added as the payment gateway was unavailable. Several attempts to resubmit have been made and also failed!!. Customer username = " + pay.getCustomerName().getUsername();

                                    sendMessage(sessionId, "Add Payment Error!", message);
                                    sendNotificationToAdmin(message, null);

                                }
                            }
                            pay.setPaymentStatus(PaymentStatus.REJECTED_BY_GATEWAY.value());
                            //paymentsFacade.editAndFlush(pay);

                        } else if (errorMessage.contains("This customer already has two payments on this date.")) {
                            paymentsFacade.remove(pay);
                            String message = "Payment ID:" + pay.getId().toString() + " for Amount:$" + pay.getPaymentAmount().toPlainString() + " on Date:" + pay.getDebitDate().toString() + " could not be added as teh customer already has two existing payments on this date!!.";
                            sendMessage(sessionId, "Add Payment Error!", message);

                        } else {
                            pay.setPaymentStatus(PaymentStatus.REJECTED_BY_GATEWAY.value());
                            pay.setPaymentReference(Integer.toString(id));
                            // paymentsFacade.editAndFlush(pay);
                        }

                    } else {
                        LOGGER.log(Level.SEVERE, "processAddPaymentResult FAILED - ERROR processing could not find payment id {0}, time: {1}", new Object[]{paymentRef, new Date().toString()});
                    }
                } else if (paymentRef.isEmpty() == false) {

                    int id = 0;
                    try {
                        id = Integer.parseInt(paymentRef);
                    } catch (NumberFormatException numberFormatException) {
                        LOGGER.log(Level.SEVERE, "processAddPaymentResult FAILED - Successful result but PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", paymentRef);

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
                        LOGGER.log(Level.INFO, "processAddPaymentResult FAILED - paymentsFacade.findPaymentById(id) failed to find the record:{0}, RETRYING {2}   ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date()), c});
                        sleep(100);
                    }*/
                    //Payments pay = paymentsFacade.findScheduledPayment(paymentRef);
                    if (pay != null) {
                        boolean ret = paymentsFacade.updatePaymentToScheduledStatus(pay);
                        //pay.setPaymentStatus(PaymentStatus.SCHEDULED.value());
                        //pay.setPaymentReference(Integer.toString(id));
                        //paymentsFacade.editAndFlush(pay);
                        if (ret) {
                            LOGGER.log(Level.INFO, "processAddPaymentResult - Updated payment status to SCHEDULED in CRM DB -  record:{0}, ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});
                        } else {
                            LOGGER.log(Level.SEVERE, "processAddPaymentResult - Updated payment status to SCHEDULED in CRM DB FAILED-  record:{0}, ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});

                        }

                    } else {
                        LOGGER.log(Level.SEVERE, "processAddPaymentResult FAILED - paymentsFacade.findPaymentById(id) failed to find the record:{0}, ---------->>> time: {1}", new Object[]{paymentRef, new SimpleDateFormat("dd/MM/yy HH:mm:ss.SSS").format(new Date())});
                    }
                    LOGGER.log(Level.INFO, "FutureMap - processAddPaymentResult Scheduled Successfully in payment gateway - processed:{0}", new Object[]{paymentRef});
                    String message = "The Payment (ref:" + paymentRef + ") was submitted successfully.";
                    storeResponseForSessionBeenToRetrieve("AddPayment", sessionId, pgr);
                    sendMessage(sessionId, "Add Payment", message);

                } else {
                    Logger.getLogger(EziDebitPaymentGateway.class
                            .getName()).log(Level.SEVERE, "Add Payment Failed! The payment Reference is empty! This should not happen as its taken from the primary key of the row in teh payments table. ");
                }
            } else {
                String message = "The information returned by the payment gateway was empty! (pgr = NULL)";
                sendMessage(sessionId, "Add Payment Error!", message);

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "processAddPaymentResult FAILED - Async Task Exception ", ex);
        }
        LOGGER.log(Level.INFO, "FutureMap - processAddPaymentResult completed");
    }

    public synchronized void sendAlertEmailToAdmin(String message, String sessionId) {

        if (message == null) {
            LOGGER.log(Level.WARNING, "Future Map sendAlertEmailToAdmin . Message is NULL.Alert Email not sent!");
            return;
        }
        String templatePlaceholder = "!--LINK--URL--!";
        //String htmlText = configMapFacade.getConfig("system.email.admin.alert.template");
        String htmlText = ejbEmailTemplatesFacade.findTemplateByName("system.email.admin.alert.template").getTemplate();
        htmlText = htmlText.replace(templatePlaceholder, message);
        AsyncJob aj = new AsyncJob("EmailAlert", paymentBean.sendAsynchEmailWithPGR(configMapFacade.getConfig("AdminEmailAddress"), configMapFacade.getConfig("PasswordResetCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), configMapFacade.getConfig("system.ezidebit.webEddrCallback.EmailSubject"), htmlText, null, paymentBean.emailServerProperties(), false, sessionId));
        this.put(FUTUREMAP_INTERNALID, aj);

    }

    public synchronized void sendNotificationToAdmin(String message, Customers customer) {

        if (message == null) {
            LOGGER.log(Level.WARNING, "Future Map sendNotificationToAdmin . Message is NULL.Alert Email not sent!");
            return;
        }
        LOGGER.log(Level.WARNING, "Future Map sendNotificationToAdmin . Message:{0}", message);
        String templatePlaceholder = "!--LINK--URL--!";
        //String htmlText = configMapFacade.getConfig("system.email.admin.alert.template");
        String htmlText = ejbEmailTemplatesFacade.findTemplateByName("system.email.admin.alert.template").getTemplate();
        htmlText = htmlText.replace(templatePlaceholder, message);

        NotificationsLog entity = new NotificationsLog();
        entity.setCustomer(customer);
        entity.setMessage(message);
        entity.setTypeOfNotification("ALERT");
        entity.setTimestampOfNotification(new Date());

        notificationsLogFacade.create(entity);

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
        startAsynchJob(sessionId, "GetPayments", paymentBean.getPayments(cust, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey(), sessionId));
        startAsynchJob(sessionId, "GetScheduledPayments", paymentBean.getScheduledPayments(cust, cal.getTime(), endDate, getDigitalKey(), sessionId, true));

    }
    //CreateSchedule

    private synchronized int getNumberofProcessingPayments(Customers cust) {
        int size = 0;
        List<Payments> payList = paymentsFacade.findPaymentsByCustomerAndStatus(cust, PaymentStatus.SENT_TO_GATEWAY.value());
        if (payList != null) {
            size = payList.size();
        }
        LOGGER.log(Level.INFO, "Future Map; The Number of Processing Payments {0} = {1}", new Object[]{cust.getUsername(), size});
        return size;
    }

    @Asynchronous
    // @TransactionAttribute(TransactionAttributeType.NEVER)
    public void processCreateSchedule(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to create the customers schedule. Refer to logs for more info";
        //PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a Customers Object from the getData method;
            // Object resultObject = ft.get();
            //if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //     pgr = (PaymentGatewayResponse) resultObject;
            //  }

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
                            LOGGER.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        getPayments(sessionId, cust, 18, 2);
                        LOGGER.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after creating the schedule for {0} ", new Object[]{cust.getUsername()});

                    }

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "processCreateSchedule - the PaymentGatewayResponse is null");
            }
            LOGGER.log(Level.INFO, "Future Map processCreateSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been created.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "processCreateSchedule - retrieving PaymentGatewayResponse", ex);
        }
        sendMessage(sessionId, "Create Payment Schedule", returnedMessage);
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processDeletePaymentBatch(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to processDeletePaymentBatch. Refer to logs for more info";
        // PaymentGatewayResponse pgr = null;

        Customers cust;
        try {
// if successful it should return a Customers Object from the getData method;
            // if successful it should return a Customers Object from the getData method;
            // Object resultObject = ft.get();
            //  if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //     pgr = (PaymentGatewayResponse) resultObject;
            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
                //  }

                if (cust != null) {
                    result = pgr.isOperationSuccessful();
                    if (result == true) {


                        /* Integer i = 0;
                        try {
                            //wait up to 30 seconds for all payments to be added before sending refresh for components 
                            while (getNumberofProcessingPayments(cust) > 0 && i++ < 100) {
                                Thread.sleep(300);
                            }
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        storeResponseForSessionBeenToRetrieve("DeletePaymentBatch", sessionId, pgr);
                        sendMessage(sessionId, "Delete Payment Batch", "OK");
                        getPayments(sessionId, cust, 18, 2);
                        LOGGER.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after processDeletePaymentBatch for {0} ", new Object[]{cust.getUsername()});

                    }
                } else {
                    Logger.getLogger(EziDebitPaymentGateway.class
                            .getName()).log(Level.SEVERE, "processDeletePaymentBatch - the customer Object in the response is null");

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "processDeletePaymentBatch - the PaymentGatewayResponse is null");
            }
            LOGGER.log(Level.INFO, "Future Map processDeletePaymentBatch completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been deleted.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "processDeletePaymentBatch - calling getPayments", ex);
        }

        LOGGER.log(Level.INFO, "Future Map --- BATCH ---> processDeletePaymentBatch completed");

    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processAddPaymentBatch(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to create the customers schedule. Refer to logs for more info";
        //PaymentGatewayResponse pgr = null;

        Customers cust;
        try {
// if successful it should return a Customers Object from the getData method;
            // if successful it should return a Customers Object from the getData method;
            // Object resultObject = ft.get();
            //  if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //     pgr = (PaymentGatewayResponse) resultObject;
            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                cust = (Customers) pgr.getData();
                //  }

                if (cust != null) {
                    result = pgr.isOperationSuccessful();
                    if (result == true) {

                        /* Integer i = 0;
                        try {
                            //wait up to 30 seconds for all payments to be added before sending refresh for components 
                            while (getNumberofProcessingPayments(cust) > 0 && i++ < 100) {
                                Thread.sleep(300);
                            }
                        } catch (InterruptedException e) {
                            LOGGER.log(Level.WARNING, "Future Map processCreateSchedule error:", e);
                        }*/
                        // update the payment schedule and double check that the clear schedule actually worked 
                        storeResponseForSessionBeenToRetrieve("AddPaymentBatch", sessionId, pgr);
                        sendMessage(sessionId, "Add Payment Batch", "OK");
                        getPayments(sessionId, cust, 18, 2);
                        LOGGER.log(Level.INFO, "Future Map; Requesting a check of payments from the payment gateway after creating the schedule for {0} ", new Object[]{cust.getUsername()});

                    }

                } else {
                    Logger.getLogger(EziDebitPaymentGateway.class
                            .getName()).log(Level.SEVERE, "processDeletePaymentBatch - the customer Object in the response is null");

                }
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "processCreateSchedule - the PaymentGatewayResponse is null");
            }
            LOGGER.log(Level.INFO, "Future Map processCreateSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been created.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "processAddPaymentBatch - calling getPayments", ex);
        }
        LOGGER.log(Level.INFO, "Future Map --- BATCH ---> processAddPaymentBatch completed");

    }

    @Asynchronous
    // @TransactionAttribute(REQUIRES_NEW)
    public void processClearSchedule(String sessionId, PaymentGatewayResponse pgr) {
        boolean result = false;
        String returnedMessage = "An error occurred trying to clear the customers schedule. Refer to logs for more info";
        //PaymentGatewayResponse pgr = null;
        Customers cust = null;
        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;
            // Object resultObject = ft.get();
            //  if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //      pgr = (PaymentGatewayResponse) resultObject;
            //   }

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
                Logger.getLogger(EziDebitPaymentGateway.class
                        .getName()).log(Level.SEVERE, "processClearSchedule - the PaymentGatewayResponse is null");
            }
            LOGGER.log(Level.INFO, "Future Map processClearSchedule completed");

            if (result == true) {
                returnedMessage = "The customers payment schedule has been cleared.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";

            }
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class
                    .getName()).log(Level.SEVERE, "processClearSchedule - retrieving PaymentGatewayResponse", ex);
        }
        sendMessage(sessionId, "Clear Payment Schedule", returnedMessage);
    }

    @Asynchronous
    // @TransactionAttribute(REQUIRES_NEW)
    public void processGetCustomerDetails(String sessionId, PaymentGatewayResponse pgr) {
        CustomerDetails custDetails = null;

        boolean result = false;
        String returnedMessage = "An error occurred trying to clear the customers schedule. Refer to logs for more info";
        //PaymentGatewayResponse pgr = null;

        try {
            // if successful it should return a CustomerDetails Object from the getData method;
            //Object resultObject = ft.get();
            // if (resultObject.getClass() == PaymentGatewayResponse.class) {
            //     pgr = (PaymentGatewayResponse) resultObject;
            //  }

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
                        LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number: {0}", custId);

                    }

                    Customers cust = null;
                    try {
                        cust = customersFacade.find(custId);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "customersFacade.findById(custId) {0}.", new Object[]{custId, e.getMessage()});
                    }
                    if (cust != null) {
                        LOGGER.log(Level.INFO, "Future Map processGetCustomerDetails. Processing details for customer {0}.", new Object[]{cust.getUsername()});

                        // PaymentParameters pp = paymentParametersFacade.find(cust.getPaymentParametersId().getId());
                        if (cust.getPaymentParametersId() == null) {

                            LOGGER.log(Level.SEVERE, "Future Map processGetCustomerDetails. Payment Parameters Object is NULL for customer {0}. CustomerDetails from the payment gateway cant be stored.", new Object[]{cust.getUsername()});
                            return;
                        }
                        // moved this into teh payment bean
                        /*  Payments p1 = null;
                        try {
                            p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                        }
                        Payments p2 = null;
                        try {
                            p2 = paymentsFacade.findNextScheduledPayment(cust);
                        } catch (Exception e) {
                            LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                        }
                      cust.getPaymentParametersId().setLastSuccessfulScheduledPayment(p1);
                        cust.getPaymentParametersId().setNextScheduledPayment(p2);
                        cust.getPaymentParametersId().setAddressLine1(custDetails.getAddressLine1().getValue());
                        cust.getPaymentParametersId().setAddressLine2(custDetails.getAddressLine2().getValue());
                        cust.getPaymentParametersId().setAddressPostCode(custDetails.getAddressPostCode().getValue());
                        cust.getPaymentParametersId().setAddressState(custDetails.getAddressState().getValue());
                        cust.getPaymentParametersId().setAddressSuburb(custDetails.getAddressSuburb().getValue());
                        cust.getPaymentParametersId().setContractStartDate(custDetails.getContractStartDate().getValue().toGregorianCalendar().getTime());
                        cust.getPaymentParametersId().setCustomerFirstName(custDetails.getCustomerFirstName().getValue());
                        cust.getPaymentParametersId().setCustomerName(custDetails.getCustomerName().getValue());
                        cust.getPaymentParametersId().setEmail(custDetails.getEmail().getValue());
                        cust.getPaymentParametersId().setEzidebitCustomerID(custDetails.getEzidebitCustomerID().getValue());

                        cust.getPaymentParametersId().setMobilePhoneNumber(custDetails.getMobilePhone().getValue());
                        cust.getPaymentParametersId().setPaymentGatewayName("EZIDEBIT");
                        cust.getPaymentParametersId().setPaymentMethod(custDetails.getPaymentMethod().getValue());
                        //cust.getPaymentParametersId().setPaymentPeriod(custDetails.getPaymentPeriod().getValue());
                        //cust.getPaymentParametersId().setPaymentPeriodDayOfMonth(custDetails.getPaymentPeriodDayOfMonth().getValue());
                        //cust.getPaymentParametersId().setPaymentPeriodDayOfWeek(custDetails.getPaymentPeriodDayOfWeek().getValue());

                        cust.getPaymentParametersId().setSmsExpiredCard(custDetails.getSmsExpiredCard().getValue());
                        cust.getPaymentParametersId().setSmsFailedNotification(custDetails.getSmsFailedNotification().getValue());
                        cust.getPaymentParametersId().setSmsPaymentReminder(custDetails.getSmsPaymentReminder().getValue());
                        cust.getPaymentParametersId().setStatusCode(custDetails.getStatusCode().getValue());
                        cust.getPaymentParametersId().setStatusDescription(custDetails.getStatusDescription().getValue());
                        cust.getPaymentParametersId().setTotalPaymentsFailed(custDetails.getTotalPaymentsFailed());
                        cust.getPaymentParametersId().setTotalPaymentsFailedAmount(new BigDecimal(custDetails.getTotalPaymentsFailed()));
                        cust.getPaymentParametersId().setTotalPaymentsSuccessful(custDetails.getTotalPaymentsSuccessful());
                        cust.getPaymentParametersId().setTotalPaymentsSuccessfulAmount(new BigDecimal(custDetails.getTotalPaymentsSuccessfulAmount()));
                        cust.getPaymentParametersId().setYourGeneralReference(custDetails.getYourGeneralReference().getValue());
                        cust.getPaymentParametersId().setYourSystemReference(custDetails.getYourSystemReference().getValue());
                        paymentParametersFacade.pushChangesToDBImmediatleyInsteadOfAtTxCommit();
                        /* paymentParametersFacade.edit(pp);
                        paymentParametersFacade.pushChangesToDBImmediatleyInsteadOfAtTxCommit();

                        cust.setPaymentParametersId(pp);
                        customersFacade.editAndFlush(cust);
                        customersFacade.pushChangesToDBImmediatleyInsteadOfAtTxCommit();*/
                        result = true;
                        LOGGER.log(Level.INFO, "Future Map processGetCustomerDetails updated payment parameters and edited customer record.ID {1}, Username {2},PP ID {4}, PP Status Code {0}, XML Status Code {3}.", new Object[]{cust.getPaymentParametersId().getStatusCode(), cust.getId(), cust.getUsername(), custDetails.getStatusCode().getValue(), cust.getPaymentParametersId().getId()});
                    } else {
                        LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails an ezidebit YourSystemReference string cannot be converted to a number or the customer ID does not exist");
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
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails FAILED", e);
        }

        LOGGER.log(Level.INFO, "Future Map processGetCustomerDetails completed");
        storeResponseForSessionBeenToRetrieve("GetCustomerDetails", sessionId, pgr);
        sendMessage(sessionId, "Get Customer Details", returnedMessage);
        LOGGER.log(Level.INFO, "Future Map processGetCustomerDetails. Completed - Committing transaction to update.");
    }

    @Asynchronous
    //@TransactionAttribute(REQUIRES_NEW)
    public void processGetAllCustPaymentsAndDetails(String sessionId, PaymentGatewayResponse pgr) {

        boolean result = false;
        String returnedMessage = "An error occurred trying to get customer details and payments. Refer to logs for more info";

        try {

            if (pgr != null) {
                result = pgr.isOperationSuccessful();
                if (result == true) {
                    Object o = pgr.getData();
                    if (o.getClass() == ArrayList.class) {
                        ArrayList<Object> returnedObjects = (ArrayList<Object>) o;

                        Object custObject = returnedObjects.get(2);// cust details

                        if (custObject != null && custObject.getClass() == String.class) {
                            returnedMessage = (String) custObject;
                        }
                        if (custObject != null && custObject.getClass() == CustomerDetails.class) {
                            CustomerDetails cd = (CustomerDetails) custObject;
                            returnedMessage = "Customer Details synced with Payment gateway OK";
                        }
                    }
                }
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Future Map processGetAllCustPaymentsAndDetails FAILED", e);
        }

        LOGGER.log(Level.INFO, "Future Map processGetAllCustPaymentsAndDetails completed");
        storeResponseForSessionBeenToRetrieve("GetCustomerDetailsAndPayments", sessionId, pgr);
        sendMessage(sessionId, "Get Customer Details & Payments", returnedMessage);
        LOGGER.log(Level.INFO, "Future Map processGetAllCustPaymentsAndDetails. Completed - Committing transaction to update.");
    }

    @Asynchronous
    //  @TransactionAttribute(REQUIRES_NEW)
    public void processUpdateCustomerSchedule(String sessionId, PaymentGatewayResponse pgr) {
        CustomerDetails custDetails = null;

        boolean result = false;
        String returnedMessage = "An error occurred trying to processUpdateCustomerSchedule. Refer to logs for more info";
        try {

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
                        LOGGER.log(Level.WARNING, "Future Map processUpdateCustomerSchedule an ezidebit YourSystemReference string cannot be converted to a number: {0}", custId);

                    }

                    Customers cust = null;
                    try {
                        cust = customersFacade.find(custId);

                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "customersFacade.findById(custId) {0}.", new Object[]{custId, e.getMessage()});
                    }
                    if (cust != null) {
                        LOGGER.log(Level.INFO, "Future Map processUpdateCustomerSchedule. Processing details for customer {0}.", new Object[]{cust.getUsername()});

                        // PaymentParameters pp = paymentParametersFacade.find(cust.getPaymentParametersId().getId());
                        if (cust.getPaymentParametersId() == null) {

                            LOGGER.log(Level.SEVERE, "Future Map processUpdateCustomerSchedule. Payment Parameters Object is NULL for customer {0}. CustomerDetails from the payment gateway cant be stored.", new Object[]{cust.getUsername()});
                            return;
                        }
                        result = true;
                        LOGGER.log(Level.INFO, "Future Map processUpdateCustomerSchedule updated payment parameters, added any new scheduled payments and edited customer record.ID {1}, Username {2},PP ID {4}, PP Status Code {0}, XML Status Code {3}.", new Object[]{cust.getPaymentParametersId().getStatusCode(), cust.getId(), cust.getUsername(), custDetails.getStatusCode().getValue(), cust.getPaymentParametersId().getId()});
                    } else {
                        LOGGER.log(Level.WARNING, "Future Map processUpdateCustomerSchedule an ezidebit YourSystemReference string cannot be converted to a number or the customer ID does not exist");
                    }
                }
            }
            if (result == true) {
                returnedMessage = "The customer scheduled payments have been updated.";
            } else if (pgr != null) {
                returnedMessage = "Error Code:" + pgr.getErrorCode() + " : " + pgr.getErrorMessage();
            } else {
                returnedMessage = "Error - the response from the payment gateway was empty";
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Future Map processUpdateCustomerSchedule FAILED", e);
        }

        LOGGER.log(Level.INFO, "Future Map processUpdateCustomerSchedule completed");

    }

    /* private synchronized void updateNextScheduledPayment(Customers cust) {
        if (cust != null) {
            LOGGER.log(Level.INFO, "updateNextScheduledPayment. Processing details for customer {0}.", new Object[]{cust.getUsername()});
            PaymentParameters pp = cust.getPaymentParametersId();
            if (pp != null) {
                Payments p1 = null;
                try {
                    p1 = paymentsFacade.findLastSuccessfulScheduledPayment(cust);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails. findLastSuccessfulScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                }
                Payments p2 = null;
                try {
                    p2 = paymentsFacade.findNextScheduledPayment(cust);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Future Map processGetCustomerDetails. findNextScheduledPayment for customer {0}. {1}", new Object[]{cust.getUsername(), e});
                }
                pp.setLastSuccessfulScheduledPayment(p1);
                pp.setNextScheduledPayment(p2);
                //paymentParametersFacade.edit(pp);
                cust.setPaymentParametersId(pp);
                customersFacade.editAndFlush(cust);
            }

        } else {
            LOGGER.log(Level.WARNING, "Future Map updateNextScheduledPayment  customer paymnt parameters  does not exist");
        }
    }*/
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
                        LOGGER.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.Your system reference is NULL. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});

                    }
                }

                LOGGER.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed.Cant poceed due to a null value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
                return null;
            }
            if (payment == null) {
                payment = new Payments();
                payment.setPaymentSource(PaymentSource.DIRECT_DEBIT.value());
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
                    LOGGER.log(Level.WARNING, "Future Map convertPaymentXMLToEntity - DebitDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
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
                    LOGGER.log(Level.WARNING, "Future Map convertPaymentXMLToEntity - SettlementDate is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
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
                    LOGGER.log(Level.INFO, "Future Map convertPaymentXMLToEntity - TransactionTime is NULL. Customer: {2},payment XML: {1}:", new Object[]{pay.toString(), cust.getUsername()});
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
                LOGGER.log(Level.WARNING, "Future Map convertPaymentXMLToEntity method failed. Customer: {2},Error Message: {0},payment XML: {1}:", new Object[]{e.getMessage(), pay.toString(), cust.getUsername()});
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
                LOGGER.log(Level.WARNING, "Future Map convertScheduledPaymentXMLToEntity method failed due to a NULL value. Customer {2},payment pojo {0},payment XML {1}:", new Object[]{p1, p2, p3});
                return null;
            }
            if (payment == null) {
                payment = new Payments();
                payment.setCreateDatetime(new Date());
                payment.setPaymentSource(PaymentSource.DIRECT_DEBIT.value());
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
                LOGGER.log(Level.WARNING, "Future Map convertScheduledPaymentXMLToEntity method failed.:", e);
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
                LOGGER.log(Level.WARNING, "Future Map comparePaymentXMLToEntity method failed.:", e.getMessage());
                return false;
            }

            return true;
        }
    }

    /*  private boolean compareScheduledPaymentXMLToEntity(Payments payment, ScheduledPayment pay) {
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
                 }

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
                LOGGER.log(Level.WARNING, "Future Map compareScheduledPaymentXMLToEntity method failed.:", e.getMessage());
                return false;
            }

            return true;
        }
    }*/

 /*  private void sanityCheckCustomersForDefaultItems() {
        LOGGER.log(Level.INFO, "Performing Sanity Checks on Customers");
        if (customersFacade != null) {
            List<Customers> cl = customersFacade.findAll();
            if (cl != null) {
                for (Customers c : cl) {
                    if (c.getProfileImage() == null) {
                        createDefaultProfilePic(c);
                    }
                    if (c.getPaymentParametersId() == null) {
                        createDefaultPaymentParameters(c);
                        LOGGER.log(Level.INFO, "Creating default payment parameters");
                    }
                }
                LOGGER.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
            } else {
                LOGGER.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers. Could not get the list of customers from the DB");
            }
        } else {
            LOGGER.log(Level.WARNING, "FAILED Performing Sanity Checks on Customers.Customer Facade null. HAs it been initialised yet?");
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
            LOGGER.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due the picture not being in jpeg, gif or png. resource:{0}", new Object[]{placeholderImage, cust.getUsername()});
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
                    LOGGER.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due to an exception:{0}", new Object[]{e, cust.getUsername()});

                }
            }
        } else {
            LOGGER.log(Level.WARNING, "createDefaultProfilePic ERROR, Cannot add default profile pic to a null customer object");
        }
    }*/
}
