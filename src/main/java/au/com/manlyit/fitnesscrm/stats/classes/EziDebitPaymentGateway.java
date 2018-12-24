/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomerStateFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.NotesFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentSource;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.util.AsyncJob;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaymentGatewayResponse;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Invoice;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfCustomerDetailsTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import com.itextpdf.text.BadElementException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.math.BigDecimal;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.NumberFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.EJBException;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.xml.ws.WebServiceException;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.primefaces.PrimeFaces;

import org.primefaces.component.tabview.Tab;

import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author david
 */
@Named("ezidebit")
@SessionScoped

public class EziDebitPaymentGateway implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(EziDebitPaymentGateway.class.getName());
    //private static final String digitalKey = "78F14D92-76F1-45B0-815B-C3F0F239F624";// test
    private static final String PAYMENT_GATEWAY = "EZIDEBIT";

    private static final long serialVersionUID = 1L;
    private final static int MONTHS_IN_ADVANCE_FOR_PAYMENT_SCHEDULE = 9;
    private int testAjaxCounter = 0;
    @Inject
    private FutureMapEJB futureMap;
    // private final Map<String, Future> futureMap = new HashMap<>();
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;
    @Inject
    private PaymentBean paymentBean;
    @Inject
    private NotificationsLogController notificationsLogController;
    @Inject
    private CustomersController controller;
    @Inject
    private EziDebitPaymentGateway ezidebitcontroller;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private CustomerStateFacade customerStateFacade;
    @Inject
    private NotesFacade ejbNotesFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;

    private boolean asyncOperationRunning = false;
    private boolean refreshFromDB = false;
    private final ThreadGroup tGroup1 = new ThreadGroup("EziDebitOps");
    private List<Payment> paymentsList;

    private PfSelectableDataModel<Payments> paymentDBList = null;
    private PfSelectableDataModel<Payments> reportPaymentsList = null;
    private PfSelectableDataModel<Invoice> reportEndOfMonthList = null;
    private List<Payment> paymentsListFilteredItems;
    private List<Payments> paymentsDBListFilteredItems;
    private List<Payments> reportPaymentsListFilteredItems;
    private List<Invoice> reportEndOfMonthListFilteredItems;

    private List<ScheduledPayment> scheduledPaymentsList;
    private Payments selectedReportItem;
    private Payments selectedEomReportItem;
    private String reportName = "defaultreport";
    private String paymentGatewayVersion;
    private Payments selectedScheduledPayment;
    private List<ScheduledPayment> scheduledPaymentsListFilteredItems;
    private CustomerDetails currentCustomerDetails;
    private Payment payment;
    private Date paymentDebitDate = new Date();
    private DatatableSelectionHelper pagination;
    private Date changeFromDate = new Date();
    private Date testAjaxUpdateTime = new Date();
    private float paymentAmountInCents = (float) 0;
    private float oneOffPaymentAmount = (float) 0;
    private Double proRataChangePlanAmount = new Double(0);
   
    private Date oneOffPaymentDate = new Date();
    private Long paymentLimitAmountInCents = new Long("0");

    private String paymentSchedulePeriodType = "W";
    private String paymentDayOfWeek = "MON";// required when Period Type is W, F, 4, N
    private int paymentDayOfMonth = 0; // required when Period Type is M
    private int paymentLimitToNumberOfPayments = 0;
    private Integer[] daysInMonth;
    private boolean pushChannelIsConnected = false;
    private boolean paymentFirstWeekOfMonth = false;
    private boolean applyToAllFuturePayments = true;
    private boolean refreshIFrames = false;
    private boolean paymentSecondWeekOfMonth = false;
    private boolean paymentThirdWeekOfMonth = false;
    private boolean paymentFourthWeekOfMonth = false;
    private boolean paymentKeepManualPayments = false;
    private boolean waitingForPaymentDetails = false;
    private boolean customerCancelledInPaymentGateway = false;
    private boolean paymentGatewayEnabled = true;
    private boolean editPaymentMethodEnabled = false;
    private boolean reportUseSettlementDate = false;
    private boolean reportShowSuccessful = true;
    private boolean reportShowFailed = true;
    private boolean reportShowPending = true;
    private boolean reportShowScheduled = false;
    private boolean manualRefreshFromPaymentGateway = false;
    private boolean customerCancellationConfirmed = false;
    private AtomicBoolean customerProvisionedInPaymentGW;
    private String cashPaymentReceiptReference = "";
    private Integer progress;
    private AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private Date reportStartDate;
    private Date reportEndDate;

    private String bulkvalue = "";
    private String duplicateValues = "";
    private String listOfIdsToImport;
    //private boolean theCustomerProvisionedInThePaymentGateway = false;
    private boolean editPaymentDetails = false;
    private boolean autoStartPoller = true;
    private boolean stopPoller = false;
    private int reportType = 0;

    private boolean customerDetailsHaveBeenRetrieved = false;
    private String eziDebitWidgetUrl = "";
    private String eziDebitEDDRFormUrl = "";
    //private Customers selectedCustomer;

    private float reportTotalSuccessful = 0;
    private float reportTotalDishonoured = 0;
    private float reportTotalScheduled = 0;

    ThreadFactory tf1 = new eziDebitThreadFactory();
    private String sessionId;

    private INonPCIService ws;

    public INonPCIService getWs() {
        if (ws == null) {
            URL url = null;
            WebServiceException e = null;
            try {
                url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
            } catch (MalformedURLException ex) {

                LOGGER.log(Level.SEVERE, "MalformedURLException - payment.ezidebit.gateway.url", ex);

            }
            ws = new NonPCIService(url).getBasicHttpBindingINonPCIService();
        }
        return ws;
    }

    @PostConstruct
    private void setSessionId() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
            this.sessionId = ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();
        }
        GregorianCalendar cal = new GregorianCalendar();
        reportEndDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        reportStartDate = cal.getTime();
        LOGGER.log(Level.INFO, "Payment Gateway BEAN CREATED - SESSION ID = {0}", new Object[]{sessionId});
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;

    }

    public void clearCustomerProvisionedInPaymentGW() {
        customerProvisionedInPaymentGW = null;
    }

    public String getSessionId() {
        /*String sessId = null;
        FacesContext facesContext = FacesContext.getCurrentInstance();
        if (facesContext != null) {
           sessId =    ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();
           LOGGER.log(Level.INFO, "Payment Gateway BEAN getSessionId - SESSION ID = {0}",new Object[]{sessId});
        }
       
        return sessId;*/
        return this.sessionId;
    }

    /**
     * @return the autoStartPoller
     */
    public boolean isAutoStartPoller() {
        return autoStartPoller;
    }

    /**
     * @param autoStartPoller the autoStartPoller to set
     */
    public void setAutoStartPoller(boolean autoStartPoller) {
        this.autoStartPoller = autoStartPoller;
    }

    /**
     * @return the paymentGatewayEnabled
     */
    public boolean isPaymentGatewayEnabled() {
        String val = configMapFacade.getConfig("payment.ezidebit.enabled");
        if (val == null) {
            setPaymentGatewayEnabled(false);
        } else if (val.trim().compareToIgnoreCase("true") == 0) {
            setPaymentGatewayEnabled(true);
        } else {
            setPaymentGatewayEnabled(false);
        }
        return paymentGatewayEnabled;
    }

    /**
     * @param paymentGatewayEnabled the paymentGatewayEnabled to set
     */
    public void setPaymentGatewayEnabled(boolean paymentGatewayEnabled) {
        this.paymentGatewayEnabled = paymentGatewayEnabled;
    }

    /**
     * @return the paymentsList
     */
    public List<Payment> getPaymentsList() {

        if (paymentsList == null) {
            paymentsList = new ArrayList<>();
        }

        return paymentsList;
    }

    public void createCombinedAuditLogAndNote(Customers adminUser, Customers customer, String title, String message, String changedFrom, String ChangedTo) {
        try {
            if (adminUser == null) {
                adminUser = customer;
                LOGGER.log(Level.WARNING, "Payment Gateway Controller, createCombinedAuditLogAndNote: The logged in user is NULL");
            }
            ejbAuditLogFacade.audit(adminUser, customer, title, message, changedFrom, ChangedTo);
            Notes note = new Notes(0);
            note.setCreateTimestamp(new Date());
            note.setCreatedBy(adminUser);
            note.setUserId(customer);
            note.setNote(message);
            ejbNotesFacade.create(note);
            customersFacade.editAndFlush(customer);

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Payment Gateway, createCombinedAuditLogAndNote: ", e);
        }

    }

    private Customers getCustomers() {
        String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Customers cust = customersFacade.findCustomerByUsername(user);

        if (cust == null) {
            LOGGER.log(Level.SEVERE, "getLoggedInUser - the remote user couldn't be found in the database. This shouldn't happen. We may have been hacked!!");
        }
        return cust;
    }

    protected void getCustDetailsFromEzi() {
        startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(getSelectedCustomer(), getDigitalKey(), sessionId));
    }

    protected void getPayments(int monthsAhead, int monthsbehind) {
        GregorianCalendar cal = new GregorianCalendar();

        cal.add(Calendar.MONTH, monthsAhead);
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -(monthsAhead));
        cal.add(Calendar.MONTH, -(monthsbehind));
        startAsynchJob("GetAllCustPaymentsAndDetails", paymentBean.getAllCustPaymentsAndDetails(getSelectedCustomer(), "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey(), sessionId));
        // startAsynchJob("GetPayments", paymentBean.getPayments(getSelectedCustomer(), "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey(), sessionId));
        //  startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(getSelectedCustomer(), cal.getTime(), endDate, getDigitalKey(), sessionId));
    }

    public void editCustomerDetailsInEziDebit(Customers cust) {
        try {
            if (isTheCustomerProvisionedInThePaymentGateway()) {
                startAsynchJob("EditCustomerDetails", paymentBean.editCustomerDetails(cust, null, getCustomers(), getDigitalKey()));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "editCustomerDetailsInEziDebit - an exception occurred whilst attempting to edit the customer details in teh payment gateway.", e);
        }

    }

    /* public void reportDateChange() {
     reportPaymentsList = null;
     reportPaymentsListFilteredItems = null;

     }*/
    /**
     * @param paymentsList the paymentsList to set
     */
    public void setPaymentsList(List<Payment> paymentsList) {
        this.paymentsList = paymentsList;
    }

    /**
     * @return the paymentsListFilteredItems
     */
    public List<Payment> getPaymentsListFilteredItems() {
        return paymentsListFilteredItems;
    }

    /**
     * @param paymentsListFilteredItems the paymentsListFilteredItems to set
     */
    public void setPaymentsListFilteredItems(List<Payment> paymentsListFilteredItems) {
        this.paymentsListFilteredItems = paymentsListFilteredItems;
    }

    /**
     * @return the payment
     */
    public Payment getPayment() {
        return payment;
    }

    /**
     * @param payment the payment to set
     */
    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    /**
     * @return the paymentDebitDate
     */
    public Date getPaymentDebitDate() {
        return paymentDebitDate;
    }

    /**
     * @param paymentDebitDate the paymentDebitDate to set
     */
    public void setPaymentDebitDate(Date paymentDebitDate) {
        this.paymentDebitDate = paymentDebitDate;
    }

    /**
     * @return the paymentAmountInCents
     */
    public float getPaymentAmountInCents() {
        return paymentAmountInCents;
    }

    /**
     * @param paymentAmountInCents the paymentAmountInCents to set
     */
    public void setPaymentAmountInCents(float paymentAmountInCents) {
        this.paymentAmountInCents = paymentAmountInCents;
    }

    /**
     * @return the scheduledPaymentsList
     */
    public List<ScheduledPayment> getScheduledPaymentsList() {
        if (scheduledPaymentsList == null) {
            scheduledPaymentsList = new ArrayList<>();
        }
        return scheduledPaymentsList;
    }

    /**
     * @param scheduledPaymentsList the scheduledPaymentsList to set
     */
    public void setScheduledPaymentsList(List<ScheduledPayment> scheduledPaymentsList) {
        this.scheduledPaymentsList = scheduledPaymentsList;
    }

    /**
     * @return the scheduledPaymentsListFilteredItems
     */
    public List<ScheduledPayment> getScheduledPaymentsListFilteredItems() {
        return scheduledPaymentsListFilteredItems;
    }

    /**
     * @param scheduledPaymentsListFilteredItems the
     * scheduledPaymentsListFilteredItems to set
     */
    public void setScheduledPaymentsListFilteredItems(List<ScheduledPayment> scheduledPaymentsListFilteredItems) {
        this.scheduledPaymentsListFilteredItems = scheduledPaymentsListFilteredItems;
    }

    /**
     * @return the paymentLimitAmountInCents
     */
    public Long getPaymentLimitAmountInCents() {
        return paymentLimitAmountInCents;
    }

    /**
     * @param paymentLimitAmountInCents the paymentLimitAmountInCents to set
     */
    public void setPaymentLimitAmountInCents(Long paymentLimitAmountInCents) {
        this.paymentLimitAmountInCents = paymentLimitAmountInCents;
    }

    /**
     * @return the paymentSchedulePeriodType
     */
    public String getPaymentSchedulePeriodType() {
        return paymentSchedulePeriodType;
    }

    /**
     * @param paymentSchedulePeriodType the paymentSchedulePeriodType to set
     */
    public void setPaymentSchedulePeriodType(String paymentSchedulePeriodType) {
        this.paymentSchedulePeriodType = paymentSchedulePeriodType;
    }

    /**
     * @return the paymentDayOfWeek
     */
    public String getPaymentDayOfWeek() {
        return paymentDayOfWeek;
    }

    /**
     * @param paymentDayOfWeek the paymentDayOfWeek to set
     */
    public void setPaymentDayOfWeek(String paymentDayOfWeek) {
        this.paymentDayOfWeek = paymentDayOfWeek;
    }

    /**
     * @return the paymentDayOfMonth
     */
    public int getPaymentDayOfMonth() {
        return paymentDayOfMonth;
    }

    /**
     * @param paymentDayOfMonth the paymentDayOfMonth to set
     */
    public void setPaymentDayOfMonth(int paymentDayOfMonth) {
        this.paymentDayOfMonth = paymentDayOfMonth;
    }

    /**
     * @return the paymentLimitToNumberOfPayments
     */
    public int getPaymentLimitToNumberOfPayments() {
        return paymentLimitToNumberOfPayments;
    }

    /**
     * @param paymentLimitToNumberOfPayments the paymentLimitToNumberOfPayments
     * to set
     */
    public void setPaymentLimitToNumberOfPayments(int paymentLimitToNumberOfPayments) {
        this.paymentLimitToNumberOfPayments = paymentLimitToNumberOfPayments;
    }

    /**
     * @return the paymentFirstWeekOfMonth
     */
    public boolean isPaymentFirstWeekOfMonth() {
        return paymentFirstWeekOfMonth;
    }

    /**
     * @param paymentFirstWeekOfMonth the paymentFirstWeekOfMonth to set
     */
    public void setPaymentFirstWeekOfMonth(boolean paymentFirstWeekOfMonth) {
        this.paymentFirstWeekOfMonth = paymentFirstWeekOfMonth;
    }

    /**
     * @return the paymentSecondWeekOfMonth
     */
    public boolean isPaymentSecondWeekOfMonth() {
        return paymentSecondWeekOfMonth;
    }

    /**
     * @param paymentSecondWeekOfMonth the paymentSecondWeekOfMonth to set
     */
    public void setPaymentSecondWeekOfMonth(boolean paymentSecondWeekOfMonth) {
        this.paymentSecondWeekOfMonth = paymentSecondWeekOfMonth;
    }

    /**
     * @return the paymentThirdWeekOfMonth
     */
    public boolean isPaymentThirdWeekOfMonth() {
        return paymentThirdWeekOfMonth;
    }

    /**
     * @param paymentThirdWeekOfMonth the paymentThirdWeekOfMonth to set
     */
    public void setPaymentThirdWeekOfMonth(boolean paymentThirdWeekOfMonth) {
        this.paymentThirdWeekOfMonth = paymentThirdWeekOfMonth;
    }

    /**
     * @return the paymentFourthWeekOfMonth
     */
    public boolean isPaymentFourthWeekOfMonth() {
        return paymentFourthWeekOfMonth;
    }

    /**
     * @param paymentFourthWeekOfMonth the paymentFourthWeekOfMonth to set
     */
    public void setPaymentFourthWeekOfMonth(boolean paymentFourthWeekOfMonth) {
        this.paymentFourthWeekOfMonth = paymentFourthWeekOfMonth;
    }

    /**
     * @return the paymentKeepManualPayments
     */
    public boolean isPaymentKeepManualPayments() {
        return paymentKeepManualPayments;
    }

    /**
     * @param paymentKeepManualPayments the paymentKeepManualPayments to set
     */
    public void setPaymentKeepManualPayments(boolean paymentKeepManualPayments) {
        this.paymentKeepManualPayments = paymentKeepManualPayments;
    }

    /**
     * @return the daysInMonth
     */
    public Integer[] getDaysInMonth() {
        if (daysInMonth == null) {
            daysInMonth = new Integer[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, 26, 27, 28, 29, 30, 31};
        }
        return daysInMonth;
    }

    /**
     * @return the changeFromDate
     */
    public Date getChangeFromDate() {
        return changeFromDate;
    }

    /**
     * @param changeFromDate the changeFromDate to set
     */
    public void setChangeFromDate(Date changeFromDate) {
        this.changeFromDate = changeFromDate;
    }

    /**
     * @return the applyToAllFuturePayments
     */
    public boolean isApplyToAllFuturePayments() {
        return applyToAllFuturePayments;
    }

    /**
     * @param applyToAllFuturePayments the applyToAllFuturePayments to set
     */
    public void setApplyToAllFuturePayments(boolean applyToAllFuturePayments) {
        this.applyToAllFuturePayments = applyToAllFuturePayments;
    }

    /**
     * @return the selectedScheduledPayment
     */
    public Payments getSelectedScheduledPayment() {
        return selectedScheduledPayment;
    }

    /**
     * @param selectedScheduledPayment the selectedScheduledPayment to set
     */
    public void setSelectedScheduledPayment(Payments selectedScheduledPayment) {
        this.selectedScheduledPayment = selectedScheduledPayment;
    }

    /**
     * @return the customerDetailsHaveBeenRetrieved
     */
    public boolean isCustomerDetailsHaveBeenRetrieved() {

        LOGGER.log(Level.FINE, "isCustomerDetailsHaveBeenRetrieved:{0}", new Object[]{customerDetailsHaveBeenRetrieved});
        return customerDetailsHaveBeenRetrieved;
    }

    /**
     * @param customerDetailsHaveBeenRetrieved the
     * customerDetailsHaveBeenRetrieved to set
     */
    public void setCustomerDetailsHaveBeenRetrieved(boolean customerDetailsHaveBeenRetrieved) {
        this.customerDetailsHaveBeenRetrieved = customerDetailsHaveBeenRetrieved;
        LOGGER.log(Level.FINE, "setCustomerDetailsHaveBeenRetrieved:{0}", new Object[]{customerDetailsHaveBeenRetrieved});
    }

    /**
     * @return the asyncOperationRunning
     */
    public boolean isAsyncOperationRunning() {
        if (pageLoaded.get() == false) {// if its null we havn't retrieved the details for the customer
            pageLoaded.set(true);
            //loginToPushServer();
            //FacesContext context = FacesContext.getCurrentInstance();
            //CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
            //this.setSelectedCustomer(controller.getSelected());
            if (isTheCustomerProvisionedInThePaymentGateway()) {
                getPayments(18, 2);
                updatePaymentTableComponents();
            }
        }
        boolean isRunning = futureMap.isAnAsyncOperationRunning(sessionId);
        LOGGER.log(Level.FINE, "isAsyncOperationRunning:{0}", new Object[]{isRunning});
        return isRunning;

        //return asyncOperationRunning;
    }

    /**
     * @param asyncOperationRunning the asyncOperationRunning to set
     */
    /*public void setAsyncOperationRunning(boolean asyncOperationRunning) {
        this.asyncOperationRunning = asyncOperationRunning;
    }*/
    /**
     * @return the testAjaxCounter
     */
    public int getTestAjaxCounter() {
        return testAjaxCounter;
    }

    /**
     * @param testAjaxCounter the testAjaxCounter to set
     */
    public void setTestAjaxCounter(int testAjaxCounter) {
        this.testAjaxCounter = testAjaxCounter;
    }

    /**
     * @return the refreshIFrames
     */
    public boolean isRefreshIFrames() {
        return refreshIFrames;
    }

    /**
     * @param refreshIFrames the refreshIFrames to set
     */
    public void setRefreshIFrames(boolean refreshIFrames) {
        this.refreshIFrames = refreshIFrames;
    }

    /**
     * @return the bulkvalue
     */
    public String getBulkvalue() {
        return bulkvalue;
    }

    /**
     * @param bulkvalue the bulkvalue to set
     */
    public void setBulkvalue(String bulkvalue) {
        this.bulkvalue = bulkvalue;
    }

    /**
     * @return the duplicateValues
     */
    public String getDuplicateValues() {
        return duplicateValues;
    }

    /**
     * @param duplicateValues the duplicateValues to set
     */
    public void setDuplicateValues(String duplicateValues) {
        this.duplicateValues = duplicateValues;
    }

    /**
     * @return the currentCustomerDetails
     */
    public CustomerDetails getCurrentCustomerDetails() {
        return currentCustomerDetails;
    }

    /**
     * @param currentCustomerDetails the currentCustomerDetails to set
     */
    public void setCurrentCustomerDetails(CustomerDetails currentCustomerDetails) {
        this.currentCustomerDetails = currentCustomerDetails;
    }

    /**
     * @return the waitingForPaymentDetails
     */
    public boolean isWaitingForPaymentDetails() {
        return waitingForPaymentDetails;
    }

    /**
     * @param waitingForPaymentDetails the waitingForPaymentDetails to set
     */
    public void setWaitingForPaymentDetails(boolean waitingForPaymentDetails) {
        this.waitingForPaymentDetails = waitingForPaymentDetails;
    }

    /**
     * @return the editPaymentMethodEnabled
     */
    public boolean isEditPaymentMethodEnabled() {
        return editPaymentMethodEnabled;
    }

    /**
     * @param editPaymentMethodEnabled the editPaymentMethodEnabled to set
     */
    public void setEditPaymentMethodEnabled(boolean editPaymentMethodEnabled) {
        this.editPaymentMethodEnabled = editPaymentMethodEnabled;
    }

    /**
     * @return the customerCancelledInPaymentGateway
     */
    public boolean isCustomerCancelledInPaymentGateway() {
        return !isTheCustomerProvisionedInThePaymentGateway();
    }

    /**
     * @param customerCancelledInPaymentGateway the
     * customerCancelledInPaymentGateway to set
     */
    public void setCustomerCancelledInPaymentGateway(boolean customerCancelledInPaymentGateway) {
        this.customerCancelledInPaymentGateway = customerCancelledInPaymentGateway;
    }

    public void preProcessXLS(Object document) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        String type = "Settlement";
        if (reportType == 0) {
            type = "Payment";
        }
        String reportTitle = type + " report for the period " + sdf.format(reportStartDate) + " to " + sdf.format(reportEndDate);
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue(reportTitle);
        HSSFCellStyle cellStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cellStyle.setFillForegroundColor(IndexedColors.BLUE.getIndex());
        cellStyle.setFillPattern(FillPatternType.BIG_SPOTS);
        for (int i = 1; i < row.getPhysicalNumberOfCells(); i++) {
            row.getCell(i).setCellStyle(cellStyle);
        }
        cellStyle.setDataFormat(df.getFormat("YYYY-MM-DD HH:MM:SS"));

        cell = row.createCell(1);
        cell.setCellValue(new Date());
        cell.setCellStyle(cellStyle);
        sheet.createRow(1);
        sheet.createRow(2);

    }

    public void postProcessXLS(Object document) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        String type = "Settlement";
        if (reportType == 0) {
            type = "Payment";
        }
        String reportTitle = type + " report for the period " + sdf.format(reportStartDate) + " to " + sdf.format(reportEndDate);
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        sheet.shiftRows(0, sheet.getLastRowNum(), 7);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        HSSFCellStyle cellStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat("YYYY-MM-DD HH:MM:SS"));
        cell.setCellValue(reportTitle);
        //first row (0-based),last row  (0-based),first column (0-based),last column  (0-based)
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, 2));
        CellStyle style = wb.createCellStyle();
        CellStyle style2 = wb.createCellStyle();
        style.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        //style.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style.setFillBackgroundColor(IndexedColors.AQUA.getIndex());
        style.setFillPattern(FillPatternType.BIG_SPOTS);
        style2.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        //style2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style2.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style2.setFillPattern(FillPatternType.BIG_SPOTS);

        HSSFCell cell3 = row.createCell(3);
        cell3.setCellValue(new Date());
        cell3.setCellStyle(cellStyle);
        //sheet.createRow(1);
        //sheet.createRow(2);
        boolean rowColor = false;
        for (Row row2 : sheet) {
            if (row2.getRowNum() > 2) {
                rowColor = !(rowColor);
                for (Cell cell2 : row2) {
                    //cell2.setCellValue(cell.getStringCellValue().toUpperCase());
                    if (rowColor) {
                        cell2.setCellStyle(style);
                    } else {
                        cell2.setCellStyle(style2);
                    }

                }
            }
        }
        for (int c = 0; c < row.getLastCellNum() + 1; c++) {
            sheet.autoSizeColumn(c);
        }
    }

    public void preProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {
        Document pdf = (Document) document;
        pdf.setPageSize(PageSize.A4.rotate());

        //FacesContext context = FacesContext.getCurrentInstance();
        //ExternalContext ec = context.getExternalContext();
        //HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        ServletContext servletContext = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        String logo = servletContext.getContextPath() + File.separator + "resources" + File.separator + "images"
                + File.separator + "logo.png";
        URL imageResource = servletContext.getResource(File.separator + "resources" + File.separator + "images"
                + File.separator + "logo.png");
        pdf.open();
        String urlForLogo = configMapFacade.getConfig("system.email.logo");
        try {
            pdf.add(Image.getInstance(new URL(urlForLogo)));
        } catch (IOException | DocumentException iOException) {
            LOGGER.log(Level.WARNING, "Logo URL error", iOException);
        }
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM yyyy");
        String type = "Settlement";
        if (reportType == 0) {
            type = "Payment";
        }
        String reportTitle = type + " report for the period " + sdf.format(reportStartDate) + " to " + sdf.format(reportEndDate);
        pdf.addTitle(reportTitle);
        pdf.add(new Paragraph(reportTitle));
        pdf.add(new Paragraph(" "));
        Font font = new Font();
        font.setSize(8);
    }

    public void postProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {

    }

    public void redirectToPaymentGateway() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
            controller.update(null);
            //controller.setSelected(customersFacade.find(getSelectedCustomer().getId()));

            PaymentParameters pp = controller.getSelectedCustomersPaymentParameters();
            if (pp.getWebddrUrl() != null) {

                ExternalContext ec = context.getExternalContext();
                ec.redirect(pp.getWebddrUrl());
            }
        } catch (IOException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void refreshCustomersDeatilsFromPaymentGateway(List<Customers> acl) {

        for (Customers c : acl) {
            try {
                PaymentParameters pp = c.getPaymentParametersId();
                if (pp != null) {
                    String sr = pp.getYourSystemReference();
                    if (sr != null && sr.trim().isEmpty() == false) {
                        startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(c, getDigitalKey(), sessionId));

                        TimeUnit.MILLISECONDS.sleep(200);//sleeping for a long time wont affect performance (the warning is there for a short sleep of say 5ms ) but we don't want to overload the payment gateway or they may get upset.
                    }
                }
            } catch (InterruptedException ex) {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public void handleDateSelect(SelectEvent event) {
        runReport();
    }

    public void runReport() {
        reportPaymentsList = null;
        reportEndOfMonthList = null;
        reportPaymentsListFilteredItems = null;
        reportEndOfMonthListFilteredItems = null;
        String key = "PaymentReport";
        reportUseSettlementDate = false;
        if (reportType == 1) {
            key = "SettlementReport";
            reportUseSettlementDate = true;
        }

        if (manualRefreshFromPaymentGateway) {
            manualRefreshFromPaymentGateway = false;
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Starting async Manual Refresh from Payment Gateway with starting date:", reportStartDate);
            startAsynchJob(key, paymentBean.getAllPaymentsBySystemSinceDate(reportStartDate, reportEndDate, reportUseSettlementDate, getDigitalKey(), sessionId));
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Starting async Manual Refresh of All active customers:");
            Customers cust = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
            String auditDetails = "User:" + cust.getUsername() + " is running the :  " + key + " report.(" + cust.getFirstname() + " " + cust.getLastname() + "). Start Date:" + reportStartDate.toString() + ", to :" + reportEndDate.toString();
            String changedTo = "Not Running";
            String changedFrom = "Report Running:" + key;
            ejbAuditLogFacade.audit(cust, getSelectedCustomer(), key + " Report", auditDetails, changedFrom, changedTo);

            refreshCustomersDeatilsFromPaymentGateway(customersFacade.findAllActiveCustomers(true));
        }
        PrimeFaces instance = PrimeFaces.current();
        instance.ajax().update("reportsForm");
    }

    /**
     * @param eziDebitEDDRFormUrl the eziDebitEDDRFormUrl to set
     */
    public void setEziDebitEDDRFormUrl(String eziDebitEDDRFormUrl) {
        this.eziDebitEDDRFormUrl = eziDebitEDDRFormUrl;
    }

    /**
     * @param paymentDBList the paymentDBList to set
     */
    public void setPaymentDBList(PfSelectableDataModel<Payments> paymentDBList) {
        this.paymentDBList = paymentDBList;
    }

    /**
     * @return the paymentsDBListFilteredItems
     */
    public List<Payments> getPaymentsDBListFilteredItems() {
        return paymentsDBListFilteredItems;
    }

    /**
     * @param paymentsListFilteredItems2 the paymentsDBListFilteredItems to set
     */
    public void setPaymentsDBListFilteredItems(List<Payments> paymentsListFilteredItems2) {
        this.paymentsDBListFilteredItems = paymentsListFilteredItems2;
    }

    public void runReconcileCustomersReport() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Running Reconcile Customer Status With Payment Gateway");
        refreshCustomersDeatilsFromPaymentGateway(customersFacade.findAll(true));
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Completed Running Reconcile Customer Status With Payment Gateway");

    }

    private void generateEndOfMonthReport() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Running End Of Month Report");

        //get the list of active customers
        List<Customers> customerList = customersFacade.findAllActiveCustomers(true);
        List<Invoice> invoices = new ArrayList<>();
        FacesContext context = FacesContext.getCurrentInstance();
        InvoiceController controller = (InvoiceController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "invoiceController");

        for (Customers cust : customerList) {
            if (cust.getGroupPricing() != null) {
                invoices.add(controller.generateInvoiceForCustomer(cust, reportUseSettlementDate, reportShowSuccessful, reportShowFailed, reportShowPending, isReportShowScheduled(), reportStartDate, reportEndDate));
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "generateEndOfMonthReport - Customers PLAN is null, name = {0}", new Object[]{cust.getUsername()});
            }
        }
        if (invoices.isEmpty() == false) {
            reportEndOfMonthList = new PfSelectableDataModel<>(invoices);
        } else {
            reportEndOfMonthList = new PfSelectableDataModel<>(new ArrayList<Invoice>());
        }

        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Completed End Of Month Report");
    }

    private void generatePaymentsReport() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Running Report");
        try {
            List<Payments> pl = paymentsFacade.findPaymentsByDateRange(reportUseSettlementDate, reportShowSuccessful, reportShowFailed, reportShowPending, isReportShowScheduled(), reportStartDate, reportEndDate, false, null);
            reportTotalSuccessful = 0;
            reportTotalDishonoured = 0;
            reportTotalScheduled = 0;
            if (pl != null) {
                for (Payments p : pl) {
                    if (p.getPaymentStatus().contains(PaymentStatus.SUCESSFUL.value()) || p.getPaymentStatus().contains(PaymentStatus.PENDING.value())) {
                        reportTotalSuccessful = reportTotalSuccessful + p.getScheduledAmount().floatValue();
                    } else if (p.getPaymentStatus().contains(PaymentStatus.DISHONOURED.value()) || p.getPaymentStatus().contains(PaymentStatus.FATAL_DISHONOUR.value())) {
                        reportTotalDishonoured = reportTotalDishonoured + p.getScheduledAmount().floatValue();
                    } else if (p.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value()) || p.getPaymentStatus().contains(PaymentStatus.WAITING.value())) {
                        reportTotalScheduled = reportTotalScheduled + p.getScheduledAmount().floatValue();
                    }
                }

                reportPaymentsList = new PfSelectableDataModel<>(pl);
            } else {
                reportPaymentsList = new PfSelectableDataModel<>(new ArrayList<Payments>());
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.WARNING, "Report Failed - paymentsFacade.findPaymentsByDateRange returned NULL");
            }
        } catch (Exception e) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.WARNING, "Report Failed", e);
            reportPaymentsList = new PfSelectableDataModel<>(new ArrayList<Payments>());
        }
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Report Completed");
        PrimeFaces instance = PrimeFaces.current();
        instance.ajax().update("reportsForm");

    }

    /**
     * @return the reportEndOfMonthList
     */
    public PfSelectableDataModel<Invoice> getReportEndOfMonthList() {
        if (reportEndOfMonthList == null) {
            if (reportType == 3) {
                generateEndOfMonthReport();

            } else {
                reportEndOfMonthList = new PfSelectableDataModel<>(new ArrayList<Invoice>());
            }
        }
        return reportEndOfMonthList;
    }

    /**
     * @return the reportPaymentsList
     */
    public PfSelectableDataModel<Payments> getReportPaymentsList() {
        if (reportPaymentsList == null) {

            if (reportType != 3) {
                generatePaymentsReport();

            } else {
                reportPaymentsList = new PfSelectableDataModel<>(new ArrayList<Payments>());
            }
        }
        return reportPaymentsList;
    }

    /**
     * @param reportPaymentsList the reportPaymentsList to set
     */
    public void setReportPaymentsList(PfSelectableDataModel<Payments> reportPaymentsList) {
        this.reportPaymentsList = reportPaymentsList;
    }

    /**
     * @return the reportPaymentsListFilteredItems
     */
    public List<Payments> getReportPaymentsListFilteredItems() {
        return reportPaymentsListFilteredItems;
    }

    /**
     * @param reportPaymentsListFilteredItems the
     * reportPaymentsListFilteredItems to set
     */
    public void setReportPaymentsListFilteredItems(List<Payments> reportPaymentsListFilteredItems) {
        this.reportPaymentsListFilteredItems = reportPaymentsListFilteredItems;
    }

    /**
     * @return the reportUseSettlementDate
     */
    public boolean isReportUseSettlementDate() {
        return reportUseSettlementDate;
    }

    /**
     * @param reportUseSettlementDate the reportUseSettlementDate to set
     */
    public void setReportUseSettlementDate(boolean reportUseSettlementDate) {
        this.reportUseSettlementDate = reportUseSettlementDate;
    }

    /**
     * @return the reportShowSuccessful
     */
    public boolean isReportShowSuccessful() {
        return reportShowSuccessful;
    }

    /**
     * @param reportShowSuccessful the reportShowSuccessful to set
     */
    public void setReportShowSuccessful(boolean reportShowSuccessful) {
        this.reportShowSuccessful = reportShowSuccessful;
    }

    /**
     * @return the reportShowFailed
     */
    public boolean isReportShowFailed() {
        return reportShowFailed;
    }

    /**
     * @param reportShowFailed the reportShowFailed to set
     */
    public void setReportShowFailed(boolean reportShowFailed) {
        this.reportShowFailed = reportShowFailed;
    }

    /**
     * @return the reportShowPending
     */
    public boolean isReportShowPending() {
        return reportShowPending;
    }

    /**
     * @param reportShowPending the reportShowPending to set
     */
    public void setReportShowPending(boolean reportShowPending) {
        this.reportShowPending = reportShowPending;
    }

    /**
     * @return the reportStartDate
     */
    public Date getReportStartDate() {
        return reportStartDate;
    }

    /**
     * @param reportStartDate the reportStartDate to set
     */
    public void setReportStartDate(Date reportStartDate) {
        this.reportStartDate = reportStartDate;
    }

    /**
     * @return the reportEndDate
     */
    public Date getReportEndDate() {
        return reportEndDate;
    }

    /**
     * @param reportEndDate the reportEndDate to set
     */
    public void setReportEndDate(Date reportEndDate) {
        this.reportEndDate = reportEndDate;
    }

    /**
     * @return the selectedReportItem
     */
    public Payments getSelectedReportItem() {
        return selectedReportItem;
    }

    /**
     * @param selectedReportItem the selectedReportItem to set
     */
    public void setSelectedReportItem(Payments selectedReportItem) {
        this.selectedReportItem = selectedReportItem;
    }

    /**
     * @return the reportName
     */
    public String getReportName() {
        return reportName;
    }

    /**
     * @param reportName the reportName to set
     */
    public void setReportName(String reportName) {
        this.reportName = reportName;
    }

    /**
     * @return the reportType
     */
    public int getReportType() {

        return reportType;
    }

    /**
     * @param reportType the reportType to set
     */
    public void setReportType(int reportType) {
        this.reportType = reportType;
    }

    /**
     * @return the manualRefreshFromPaymentGateway
     */
    public boolean isManualRefreshFromPaymentGateway() {
        return manualRefreshFromPaymentGateway;
    }

    /**
     * @param manualRefreshFromPaymentGateway the
     * manualRefreshFromPaymentGateway to set
     */
    public void setManualRefreshFromPaymentGateway(boolean manualRefreshFromPaymentGateway) {
        this.manualRefreshFromPaymentGateway = manualRefreshFromPaymentGateway;
    }

    /**
     * @return the reportTotalSuccessful
     */
    public float getReportTotalSuccessful() {
        return reportTotalSuccessful;
    }

    /**
     * @param reportTotalSuccessful the reportTotalSuccessful to set
     */
    public void setReportTotalSuccessful(float reportTotalSuccessful) {
        this.reportTotalSuccessful = reportTotalSuccessful;
    }

    /**
     * @return the reportTotalDishonoured
     */
    public float getReportTotalDishonoured() {
        return reportTotalDishonoured;
    }

    /**
     * @param reportTotalDishonoured the reportTotalDishonoured to set
     */
    public void setReportTotalDishonoured(float reportTotalDishonoured) {
        this.reportTotalDishonoured = reportTotalDishonoured;
    }

    /**
     * @return the customerCancellationConfirmed
     */
    public boolean isCustomerCancellationConfirmed() {
        return customerCancellationConfirmed;
    }

    /**
     * @param customerCancellationConfirmed the customerCancellationConfirmed to
     * set
     */
    public void setCustomerCancellationConfirmed(boolean customerCancellationConfirmed) {
        this.customerCancellationConfirmed = customerCancellationConfirmed;
    }

    /**
     * @return the reportShowScheduled
     */
    public boolean isReportShowScheduled() {
        return reportShowScheduled;
    }

    /**
     * @param reportShowScheduled the reportShowScheduled to set
     */
    public void setReportShowScheduled(boolean reportShowScheduled) {
        this.reportShowScheduled = reportShowScheduled;
    }

    /**
     * @return the reportTotalScheduled
     */
    public float getReportTotalScheduled() {
        return reportTotalScheduled;
    }

    /**
     * @param reportTotalScheduled the reportTotalScheduled to set
     */
    public void setReportTotalScheduled(float reportTotalScheduled) {
        this.reportTotalScheduled = reportTotalScheduled;
    }

    /**
     * @return the oneOffPaymentAmount
     */
    public float getOneOffPaymentAmountInCents() {
        return oneOffPaymentAmount;
    }

    /**
     * @param oneOffPaymentAmountInCents the oneOffPaymentAmount to set
     */
    public void setOneOffPaymentAmountInCents(float oneOffPaymentAmountInCents) {
        this.oneOffPaymentAmount = oneOffPaymentAmountInCents;
    }

    /**
     * @return the oneOffPaymentDate
     */
    public Date getOneOffPaymentDate() {
        return oneOffPaymentDate;
    }

    /**
     * @param oneOffPaymentDate the oneOffPaymentDate to set
     */
    public void setOneOffPaymentDate(Date oneOffPaymentDate) {
        this.oneOffPaymentDate = oneOffPaymentDate;
    }

    /**
     * @param reportEndOfMonthList the reportEndOfMonthList to set
     */
    public void setReportEndOfMonthList(PfSelectableDataModel<Invoice> reportEndOfMonthList) {
        this.reportEndOfMonthList = reportEndOfMonthList;
    }

    /**
     * @return the reportEndOfMonthListFilteredItems
     */
    public List<Invoice> getReportEndOfMonthListFilteredItems() {
        return reportEndOfMonthListFilteredItems;
    }

    /**
     * @param reportEndOfMonthListFilteredItems the
     * reportEndOfMonthListFilteredItems to set
     */
    public void setReportEndOfMonthListFilteredItems(List<Invoice> reportEndOfMonthListFilteredItems) {
        this.reportEndOfMonthListFilteredItems = reportEndOfMonthListFilteredItems;
    }

    /**
     * @return the selectedEomReportItem
     */
    public Payments getSelectedEomReportItem() {
        return selectedEomReportItem;
    }

    /**
     * @param selectedEomReportItem the selectedEomReportItem to set
     */
    public void setSelectedEomReportItem(Payments selectedEomReportItem) {
        this.selectedEomReportItem = selectedEomReportItem;
    }

    /**
     * @return the cashPaymentReceiptReference
     */
    public String getCashPaymentReceiptReference() {
        return cashPaymentReceiptReference;
    }

    /**
     * @param cashPaymentReceiptReference the cashPaymentReceiptReference to set
     */
    public void setCashPaymentReceiptReference(String cashPaymentReceiptReference) {
        this.cashPaymentReceiptReference = cashPaymentReceiptReference;
    }

    /**
     * @return the testAjaxUpdateTime
     */
    public Date getTestAjaxUpdateTime() {
        return new Date();
    }

    /**
     * @param testAjaxUpdateTime the testAjaxUpdateTime to set
     */
    public void setTestAjaxUpdateTime(Date testAjaxUpdateTime) {
        this.testAjaxUpdateTime = testAjaxUpdateTime;
    }

    private class eziDebitThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(tGroup1, r, "EziDebit Operation");
        }
    }

    /*  @PostConstruct
     private void init() {
     executor1 = Executors.newFixedThreadPool(THREAD_POOL_SIZE, tf1);
     getCustDetailsFuture = paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey());
     asyncOperationRunning.set(true);

     }*/
    /**
     * Creates a new instance of EziDebitPaymentGateway
     */
    public EziDebitPaymentGateway() {

    }

    public void doBulkUpload() {
        // addBulkCustomersToPaymentGateway();
    }

    /*  public void syncEziDebitIds() {
     String[] eziDebitIds = getListOfIdsToImport().split(",");
     for (int i = 0; eziDebitIds.length >= i; i++) {
     String refNumber = eziDebitIds[i];
     CustomerDetails cd = getCustomerDetails(refNumber);
     if (cd != null) {
     String username = cd.getCustomerFirstName().getValue().toLowerCase() + "." + cd.getCustomerName().getValue().toLowerCase();

     Customers cust = customersFacade.findCustomerByUsername(username);
     if (cust != null) {
     editCustomerDetails(cust, refNumber);

     }
     }
     }

     }*/
    public boolean isTheCustomerProvisionedInThePaymentGateway() {
        boolean stat = false;
        if (customerProvisionedInPaymentGW == null) {
            if (getSelectedCustomer() != null) {
                //Customers cust = customersFacade.findById(getSelectedCustomer().getId());
                Customers cust = getSelectedCustomer();
                //customersFacade.edit(cust);
                PaymentParameters pp = cust.getPaymentParametersId();
                if (pp == null) {// if its a new lead or customer create the default payment parameters.
                    FacesContext context = FacesContext.getCurrentInstance();
                    CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
                    controller.createDefaultPaymentParameters(cust);
                    pp = cust.getPaymentParametersId();
                }

                if (pp != null) {

                    String statusCode = pp.getStatusCode();
                    if (statusCode == null) {
                        stat = false;
                        customerProvisionedInPaymentGW = new AtomicBoolean(false);
                    } else if (pp.getStatusCode().trim().isEmpty() || pp.getStatusCode().trim().startsWith("C") || pp.getStatusCode().trim().startsWith("D")) {

                        //customer has never been added or is cancelled. If they are cancelled they must be added again like a new customer
                        stat = false;
                        customerProvisionedInPaymentGW = new AtomicBoolean(false);
                    } else if (pp.getStatusCode().trim().startsWith("A") || pp.getStatusCode().trim().startsWith("H") || pp.getStatusCode().trim().startsWith("N") || pp.getStatusCode().trim().startsWith("W")) {
                        // They are on hold or active or waiting bank details
                        stat = true;
                        customerProvisionedInPaymentGW = new AtomicBoolean(true);
                    } else {
                        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.WARNING, "Unkown ezidebit status code: {0}", new Object[]{statusCode});
                        customerProvisionedInPaymentGW = new AtomicBoolean(false);
                    }
                } else {
                    LOGGER.log(Level.INFO, "isTheCustomerProvisionedInThePaymentGateway - PaymentParameters are null returning false for {0}. ", new Object[]{getSelectedCustomer().getUsername()});
                    customerProvisionedInPaymentGW = new AtomicBoolean(false);
                }

            }
        }
        return customerProvisionedInPaymentGW.get();
    }

    public boolean isCustomerWebDDRFormEnabled() {

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        Customers stale = controller.getSelected();
        if (stale != null) {
            Customers cust = customersFacade.findById(stale.getId());
            if (cust != null) {
                if (cust.getPaymentParametersId() != null) {
                    String webDdrUrl = cust.getPaymentParametersId().getWebddrUrl();// contains payment information e.g 
                    if (webDdrUrl != null && (cust.getPaymentParametersId().getStatusCode().trim().isEmpty() || cust.getPaymentParametersId().getStatusCode().trim().startsWith("C") || cust.getPaymentParametersId().getStatusCode().trim().startsWith("D"))) {
                        LOGGER.log(Level.INFO, "isCustomerWebDDRFormEnabled - YES customer direct debit button is enabled : ", webDdrUrl);
                        return true;
                    }
                }
            }
        }
        return false;

    }

    public boolean isShowAddToPaymentGatewayButton() {
        if (customerDetailsHaveBeenRetrieved) {
            if (customerCancelledInPaymentGateway || isTheCustomerProvisionedInThePaymentGateway() == false) {
                if (getSelectedCustomer().getActive().getCustomerState().contains("ACTIVE")) {

                    return true;
                }
            }
        }

        return false;
    }

    public PfSelectableDataModel<Payments> getPaymentDBList() {
        if (paymentDBList == null) {
            paymentDBList = new PfSelectableDataModel<>(paymentsFacade.findPaymentsByCustomer(getSelectedCustomer(), false));
        }
        if (paymentDBList == null) {
            paymentDBList = new PfSelectableDataModel<>(new ArrayList<Payments>());
        }
        return paymentDBList;
    }

    public Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        //controller.refreshSelectedCustomerFromDB();
        /*if (refreshFromDB) {
         refreshFromDB = false;
         controller.updateSelectedCustomer(customersFacade.findByIdBypassCache(controller.getSelected().getId()));
         }*/
        return controller.getSelected();

    }

    /**
     * @param theCustomerProvisionedInThePaymentGateway the
     * theCustomerProvisionedInThePaymentGateway to set
     */
    /*  public void checktheCustomerProvisionedInThePaymentGateway(ActionEvent actionEvent) {
     CustomerDetails cd = getCustomerDetails(getSelectedCustomer());
     theCustomerProvisionedInThePaymentGateway = cd != null;
     }*/
    /**
     *
     * @return the eziDebitWidgetUrl
     */
    public String getEziDebitWidgetUrl() {
        Customers cust = getSelectedCustomer();
        if (cust == null || cust.getId() == null) {
            return "";
        }
        String amp = "&";
        String viewOrEdit = "view";
        if (isEditPaymentMethodEnabled()) {
            viewOrEdit = "edit";
        }

        String widgetUrl = configMapFacade.getConfig("payment.ezidebit.widget.baseurl") + viewOrEdit;
        widgetUrl += "?" + "dk=" + getDigitalKey();
        widgetUrl += amp + "cr=" + cust.getId().toString();
        widgetUrl += amp + "e=" + configMapFacade.getConfig("payment.ezidebit.widget.e"); // 0 = dont allow customer to edit
        String template = configMapFacade.getConfig("payment.ezidebit.widget.template");
        /* if (template.trim().isEmpty() == false) {
         widgetUrl += amp + "template=" + template;//template name win7 
         } else {

         widgetUrl += amp + "f=" + configMapFacade.getConfig("payment.ezidebit.widget.f");//font Arial
         widgetUrl += amp + "h1c=" + configMapFacade.getConfig("payment.ezidebit.widget.h1c");//header colour FF5595
         widgetUrl += amp + "h1s=" + configMapFacade.getConfig("payment.ezidebit.widget.h1s");//header size in pixels 24
         widgetUrl += amp + "lblc=" + configMapFacade.getConfig("payment.ezidebit.widget.lblc"); // label colour EB7636
         widgetUrl += amp + "lbls=" + configMapFacade.getConfig("payment.ezidebit.widget.lbls");//label size in pixels 18
         widgetUrl += amp + "bgc=" + configMapFacade.getConfig("payment.ezidebit.widget.bgc");//background FFFFFF
         widgetUrl += amp + "hgl=" + configMapFacade.getConfig("payment.ezidebit.widget.hgl");// highlight 1892CD
         widgetUrl += amp + "txtc=" + configMapFacade.getConfig("payment.ezidebit.widget.txtc");//text 333333
         widgetUrl += amp + "txtbgc=" + configMapFacade.getConfig("payment.ezidebit.widget.txtbgc");//text background FFFFFF
         widgetUrl += amp + "txtbc=" + configMapFacade.getConfig("payment.ezidebit.widget.txtbc");//Textbox Focus Border Colour EB7636
         }*/
        eziDebitWidgetUrl = widgetUrl;
        /*try {
         eziDebitWidgetUrl = URLEncoder.encode(widgetUrl,"UTF-8");
         } catch (UnsupportedEncodingException ex) {
         Logger.getLogger(CustomersController.class.getName()).log(Level.SEVERE, "UTF-8 unsupported. This shouldn't happen!", ex);
         }*/
        return eziDebitWidgetUrl;
    }

    /**
     * pp.setSmsExpiredCard(converted); ejbPaymentParametersFacade.edit(pp);
     *
     * @return the eziDebitWidgetUrl
     */
    public String getEziDebitEDDRFormUrl() {

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        PaymentParameters pp = controller.getSelectedCustomersPaymentParameters();
        String webDdrUrl = null;
        if (pp != null) {
            webDdrUrl = pp.getWebddrUrl();// contains payment information e.g 
        }

        String amp = "&";

        // eziDebitEDDRFormUrl = widgetUrl;
        // try {
        if (webDdrUrl != null) {
            //eziDebitEDDRFormUrl = URLEncoder.encode(webDdrUrl, "UTF-8");
            eziDebitEDDRFormUrl = webDdrUrl;
        } else {
            webDdrUrl = "";
        }
        // } catch (UnsupportedEncodingException ex) {
        //     Logger.getLogger(CustomersController.class.getName()).log(Level.SEVERE, "UTF-8 unsupported. This shouldn't happen!", ex);
        // }
        return webDdrUrl;
    }

    /**
     * @return the editPaymentDetails
     */
    public boolean isEditPaymentDetails() {
        return editPaymentDetails;
    }

    public void onTabShow(TabChangeEvent event) {
        Tab tb = event.getTab();

        String tabName = "unknown";
        if (tb != null) {
            tabName = tb.getTitle();
            if (tb.getId().compareTo("tab3") == 0) {

            }
        }

        // FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + tabName);
        // FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    public void onTabChange(TabChangeEvent event) {
        Tab tb = event.getTab();
        // String compId =   event.getTab().getClientId();
        //logger.log(Level.INFO, "Request Context update of {0} actioned",compId);
        //PrimeFaces.current().ajax().update(compId);

        String tabName = "unknown";
        if (tb != null) {
            tabName = tb.getTitle();
            if (tb.getId().compareTo("tab3") == 0) {

            }
        }

        // FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + tabName);
        // FacesContext.getCurrentInstance().addMessage(null, msg);
    }

    /**
     * @param editPaymentDeatils the editPaymentDetails to set
     */
    public void setEditPaymentDetails(boolean editPaymentDeatils) {
        this.editPaymentDetails = editPaymentDeatils;
    }

    private void addDefaultPaymentParametersIfEmpty(Customers cust, Date contractStartDate) {
        if (cust == null) {
            LOGGER.log(Level.WARNING, "Customer is null: addDefaultPaymentParametersIfEmpty(Customers cust)");
            return;
        }
        PaymentParameters pay = cust.getPaymentParametersId();

        if (cust.getTelephone() == null) {
            cust.setTelephone("0400000000");
        }

        if (pay == null) {
            pay = new PaymentParameters(0, contractStartDate, cust.getTelephone(), "NO", "NO", "NO", PAYMENT_GATEWAY);
            pay.setCustomers(cust);
            cust.setPaymentParametersId(pay);
            LOGGER.log(Level.INFO, "Adding default payment gateway parameters for this customer as they don't have any.");
            customersFacade.editAndFlush(cust);
        }

    }

    public void createCustomerRecord() {
        createCustomerRecord(getSelectedCustomer());
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        controller.addCustomerToUsersGroup(getSelectedCustomer());
    }

    public void createCustomerRecord(Customers cust) {
        String authenticatedUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (cust.getPaymentParametersId() == null) {
            getCustomersController().createDefaultPaymentParameters(cust);
        }
        startAsynchJob("AddCustomer", paymentBean.addCustomer(cust, PAYMENT_GATEWAY, getDigitalKey(), authenticatedUser, sessionId));
        JsfUtil.addSuccessMessage("Processing Add Customer to Payment Gateway Request.");

    }

    public void testAjax(ActionEvent event) {
        testAjaxCounter++;
        PrimeFaces context = PrimeFaces.current();
        context.ajax().update("tabView");

        JsfUtil.addSuccessMessage("Testing Ajax");

    }

    private void processAddCustomer(PaymentGatewayResponse pgr) {

        // updatePaymentTableComponents();
        LOGGER.log(Level.INFO, "Session BEAN processAddCustomer completed.No updates necessary as GetCustDetails was called after successful add.");
        /*boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            LOGGER.log(Level.WARNING, "processAddCustomer", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Customer Added to Payment Gateway Successfully.", "Customer Added to Payment Gateway Successfully.");

            startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
            getPayments(18, 2);

        } else {
            JsfUtil.addErrorMessage("Couldn't add Customer To Payment Gateway. Check logs");
        }*/
    }

    /*   private boolean addBulkCustomersToPaymentGateway() {
     boolean custDetails = false;
     LOGGER.log(Level.INFO, "Starting tests!");
     List<Customers> custList = customersFacade.findAllActiveCustomers(custDetails);
     String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
     for (Customers c : custList) {
     PaymentParameters pp = new PaymentParameters(0, new Date(), c.getTelephone(), "NO", "NO", "NO", user);
     c.getPaymentParametersCollection().add(pp);
     customersFacade.edit(c);
     boolean success = addCustomer(c, PAYMENT_GATEWAY);
     if (!success) {
     LOGGER.log(Level.WARNING, "Add customer failed! {0}", c.getUsername());
     }
     }
     return custDetails;
     }*/
    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    /**
     * @return the listOfIdsToImport
     */
    public String getListOfIdsToImport() {
        return listOfIdsToImport;
    }

    /**
     * @param listOfIdsToImport the listOfIdsToImport to set
     */
    public void setListOfIdsToImport(String listOfIdsToImport) {
        this.listOfIdsToImport = listOfIdsToImport;
    }

    /**
     * @return the progress
     */
    public Integer getProgress() {
        if (isAsyncOperationRunning()) {
            this.progress = 101;
        }
        progress += 1;
        return progress;
    }

    /**
     * @param progress the progress to set
     */
    public void setProgress(Integer progress) {
        this.progress = progress;
    }

    public void onComplete() {
        //FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Payment Details", "Successfully retireved from payment gateway."));
        JsfUtil.addSuccessMessage("Payment Details", "Successfully retireved from payment gateway.");
        this.progress = 0;
    }

    /*public void checkPaymentRequestStatusListener() {
     isAsyncOperationRunning();
     }

     public boolean isPaymentRequestStatusIdle() {

     int k = futureMap.size();
     if (k <= 0) {
     if (stopPoller) {
     stopPoller();
     return true;
                
     } else {
     stopPoller = true;
     return false;
     }
     } else {
     stopPoller = false;
     return false;
     }

     }*/
    public void checkPushChannelIsOpen() {
        LOGGER.log(Level.INFO, "Checking if push channel is open. sessionID {0}", sessionId);
        loginToPushServer();
    }

    public void loginToPushServerAction(ActionEvent event) {
        loginToPushServer();
    }

    public void loginToPushServer() {

        // Connect to the push channel based on sessionId
        PrimeFaces instance = PrimeFaces.current();

        // instance.execute("PF('subscriber').connect('/" + sessionId + "')");
        //  LOGGER.log(Level.INFO, "Adding connect request to PrimeFaces instance. PF(\'subscriber\').connect(\'/{0}\')", sessionId);
    }

    @PreDestroy
    private void cleanUp() {
        futureMap.cancelFutures(sessionId);
    }

    /* private void cancelAllAsyncJobs() {

     Iterator it = futureMap.entrySet().iterator();
     while (it.hasNext()) {
     Map.Entry pairs = (Map.Entry) it.next();
     Future ft = (Future) pairs.getValue();
     ft.cancel(false);
     it.remove(); // avoids a ConcurrentModificationException
     }
     futureMap.clear();
     }*/
    private boolean futureMapContainsKey(String key) {
        return futureMap.containsKey(sessionId, key);
    }

    private Future futureMapGetKey(String key) {
        AsyncJob aj = futureMap.get(sessionId, key);
        return aj.getFuture();
    }

    private void futureMapRemoveKey(String key) {
        futureMap.remove(sessionId, key);
    }

    public void remoteCommandListener() {
        LOGGER.log(Level.INFO, "MESSAGE RECIEVED FROM FUTURE MAP BEAN. Checking for updates to process.");

        // int k = futureMap.getComponentsToUpdate(sessionId).size();
        boolean updatedFlag = futureMap.isAnAsyncOperationRunning(sessionId);
        //setAsyncOperationRunning(updatedFlag);
        int k = futureMap.getFutureMap(sessionId).size();
        if (k > 0) {
            LOGGER.log(Level.INFO, "{0} jobs are running. Checking to see if asych jobs have finished so their results can be processed.", k);
            if (isAsyncOperationRunning() == false) {
                LOGGER.log(Level.WARNING, "{0} jobs are running but asychOperationRunning flag is false!!", k);
                //setAsyncOperationRunning(true);
            }
        } else {
            // setAsyncOperationRunning(false);
            //logger.log(Level.INFO, "Asking request context to update components.");

            //ArrayList<String> componentsToUpdate = new ArrayList<>();
            //componentsToUpdate.add(":tv:paymentsForm:progressBarPanel");
            //componentsToUpdate.add("paymentsForm:mainPanel");
            //componentsToUpdate.add("tv:paymentsForm:iFrameHeaderPanel");
            //componentsToUpdate.add("paymentsTablePanel");
            //componentsToUpdate.add("scheduledPaymentsTablePanel");
            //componentsToUpdate.add("tv:paymentsForm");
            // instance.execute("PF('paymentPoller').stop();");
            //pushComponentUpdateBean.sendMessage("Notification", "Payment Gateway Request Completed");
            LOGGER.log(Level.INFO, "All asych jobs have been processed. Will continue to process any component updates.");
        }

        int y = 0;
        //String details = "";
        PaymentGatewayResponse pgr;
        ArrayList<PaymentGatewayResponse> fmap = futureMap.getComponentsToUpdate(sessionId);
        if (fmap.isEmpty() == false) {
            try {
                Iterator<PaymentGatewayResponse> i = fmap.iterator();
                while (i.hasNext()) {
                    pgr = i.next();
                    String key = pgr.getOperationName();
                    y++;
                    LOGGER.log(Level.INFO, "PAYMENT EJB - Update Components key={0}, Session ID = {1} .", new Object[]{key, sessionId});
                    checkIfAsyncJobsHaveFinishedAndUpdate(key, pgr);
                }
                futureMap.clearComponentUpdates(sessionId);

            } catch (Exception e) {
                LOGGER.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete,  {0} async jobs for sessionId {1} have finished.Exception {2}", new Object[]{Integer.toString(y), sessionId, e});
            }
        } else {
            LOGGER.log(Level.INFO, "PAYMENT EJB - No Components retrieved from FutureMap components to Update List");
        }

        // checkIfAsyncJobsHaveFinishedAndUpdate();
        //k = futureMap.getFutureMap(sessionId).size();
        if (isRefreshIFrames() == true) {
            //PrimeFaces.current().ajax().update("@(.updatePaymentInfo)");
            //PrimeFaces.current().ajax().update(":iFrameForm");
            updatePaymentTableComponents();
            //PrimeFaces.current().ajax().update("createCustomerForm");
            LOGGER.log(Level.INFO, "Asking Request Context to update IFrame forms.");
            setRefreshIFrames(false);
        }
        /*  ArrayList<PaymentGatewayResponse> ctu = futureMap.getComponentsToUpdate(sessionId);
        if (ctu.isEmpty() == false) {
            PrimeFaces.current().ajax().update(ctu);
        }
        PrimeFaces.current().executeScript("updatePaymentForms();");*/
//PrimeFaces.current().ajax().update("devForm");
        //  refreshFromDB = true;
        boolean currentFlag = updatedFlag;
        updatedFlag = futureMap.isAnAsyncOperationRunning(sessionId);
        // setAsyncOperationRunning(updatedFlag);
        // PrimeFaces.current().ajax().update("payments");

        if (currentFlag != updatedFlag) {
            if (updatedFlag == false) {
                LOGGER.log(Level.INFO, "Async Jobs have all completed");
            } else {
                LOGGER.log(Level.INFO, "Async Jobs have started. {0} jobs are running", k);
            }
            //PrimeFaces.current().ajax().update(":tv:paymentsForm");
            updatePaymentTableComponents();
        }

    }

    public void checkIfAsyncJobsHaveFinishedAndUpdate(String key, PaymentGatewayResponse pgr) {

        try {
            if (key.contentEquals("GetCustomerDetails")) {//GetCustomerDetailsAndPayments
                processGetCustomerDetails(pgr);
            } else if (key.contentEquals("GetCustomerDetailsAndPayments")) {
                processGetCustomerDetailsAndPayments(pgr);
            } else if (key.contentEquals("AddPayment")) {
                processAddPaymentResult(pgr);
            } else if (key.contentEquals("DeletePaymentBatch")) {
                processDeletePaymentBatch(pgr);
            } else if (key.contentEquals("AddPaymentBatch")) {
                processAddPaymentBatch(pgr);
            } else if (key.contentEquals("GetPayments")) {
                processGetPayments(pgr);
            } else if (key.contentEquals("GetScheduledPayments")) {
                processGetScheduledPayments(pgr);
            } else if (key.contentEquals("CreateSchedule")) {
                processCreateSchedule(pgr);
            } else if (key.contentEquals("AddCustomer")) {
                processAddCustomer(pgr);
            } else if (key.contentEquals("EditCustomerDetails")) {
                processEditCustomerDetails(pgr);
            } /* if (key.contains("ClearSchedule")) {
                processClearSchedule(ft);
            }*/ else if (key.contentEquals("DeletePayment")) {
                processDeletePayment(pgr);
            } /* else if (key.contains("ChangeCustomerStatus")) {
                processChangeCustomerStatus(ft);
            }*/ else if (key.contentEquals("GetPaymentStatus")) {
                processGetPaymentStatus(pgr);
            } else if (key.contentEquals("ChangeScheduledAmount")) {
                processChangeScheduledAmount(pgr);
            } else if (key.contentEquals("ChangeScheduledDate")) {
                processChangeScheduledDate(pgr);
            } else if (key.contentEquals("IsBsbValid")) {
                processIsBsbValid(pgr);
            } else if (key.contentEquals("IsSystemLocked")) {
                processIsSystemLocked(pgr);
            } else if (key.contentEquals("GetPaymentExchangeVersion")) {
                processGetPaymentExchangeVersion(pgr);
            } else if (key.contentEquals("GetCustomerDetailsFromEziDebitId")) {
                processGetCustomerDetailsFromEziDebitId(pgr);
            } else if (key.contentEquals("GetPaymentDetail")) {
                processGetPaymentDetail(pgr);
            } else if (key.contentEquals("GetPaymentDetailPlusNextPaymentInfo")) {
                processGetPaymentDetailPlusNextPaymentInfo(pgr);
            } else if (key.contentEquals("PaymentReport")) {
                processPaymentReport(pgr);
            } else if (key.contentEquals("SettlementReport")) {
                processSettlementReport(pgr);
            } else if (key.contentEquals("EmailAlert")) {
                processEmailAlert(pgr);
            } else {
                LOGGER.log(Level.WARNING, "checkIfAsyncJobsHaveFinishedAndUpdate Key not matched:{0}", key);
            }

        } catch (CancellationException ex) {
            LOGGER.log(Level.WARNING, key + ":", ex);

        } catch (Exception ex) {
            LOGGER.log(Level.SEVERE, key + ":", ex);
        }
    }

    private void processPaymentReport(PaymentGatewayResponse pgr) {

        recreatePaymentTableData();
        //refreshFromDB = true;
        //getCustomersController().setRefreshFromDB(true);
        getCustomersController().recreateModel();

        LOGGER.log(Level.INFO, "Session BEAN processPaymentReport completed");
    }

    private void processSettlementReport(PaymentGatewayResponse pgr) {

        recreatePaymentTableData();

        // refreshFromDB = true;
        // getCustomersController().setRefreshFromDB(true);
        getCustomersController().recreateModel();

        LOGGER.log(Level.INFO, "Session BEAN processSettlementReport completed");
    }

    private void processEmailAlert(PaymentGatewayResponse pgr) {

        LOGGER.log(Level.INFO, "Session BEAN -- Email Alert processing completed");
    }

    private void processGetPayments(PaymentGatewayResponse pgr) {

        String cust = getSelectedCustomer().getUsername();
        LOGGER.log(Level.INFO, "Session BEAN   processing GetPayments Response Recieved from ezidebit for Customer  - {0}", cust);

        recreatePaymentTableData();
        updatePaymentTableComponents();
        notificationsLogController.recreateModel();
        //@(.parentOfUploadPhoto)
        // PrimeFaces.current().ajax().update("customerslistForm1");
        // PrimeFaces.current().ajax().update("@(.updatePaymentInfo)");

        LOGGER.log(Level.INFO, "Session BEAN processGetPayments completed");
    }

    private void recreatePaymentTableData() {
        setScheduledPaymentsList(null);
        setScheduledPaymentsListFilteredItems(null);
        setPaymentDBList(null);
        setPaymentsDBListFilteredItems(null);
    }

    private void updatePaymentTableComponents() {
        //ArrayList<String> als = new ArrayList<>();
        //als.add("\\:tv\\:paymentsForm");
        //als.add("\\:tv\\:paymentsForm\\:paymentsTable2");
        //als.add("\\:tv\\:paymentsForm\\:customerDetailsPanel");
        //als.add("\\:tv\\:paymentsForm\\:paymentsTable");
        //als.add("\\:tv\\:paymentsForm\\:scheduledPaymentsTable");
        //als.add("\\:tv\\:paymentsForm\\:customersTableList");
        //als.add("\\:createCustomerForm\\:myDetailsPaymentInfo");
        // als.add("\\:tv\\:paymentsForm\\:testPaymentUpdate");
        // als.add("\\:tv\\:paymentsForm\\:testPaymentUpdate2");
        //PrimeFaces.current().ajax().update(als);
        //PrimeFaces.current().ajax().update("@(.updatePaymentInfo)");
        //PrimeFaces.current().ajax().update("\\:tv\\:paymentsForm");
        futureMap.sendMessage(getSessionId(), "UpdatePaymentForms", "UpdatePaymentForms");
        //PrimeFaces.current().executeScript("updatePaymentForms();");

        //LOGGER.log(Level.INFO, "Session BEAN PrimeFaces --------------------------------------------------");
        LOGGER.log(Level.INFO, "Session BEAN PrimeFaces ------>> Updated Payment Table Components ( execute(\"updatePaymentForms();\") ) <<-------");
        //LOGGER.log(Level.INFO, "Session BEAN PrimeFaces --------------------------------------------------");
    }

    private void processGetScheduledPayments(PaymentGatewayResponse pgr) {

        String cust = getSelectedCustomer().getUsername();
        LOGGER.log(Level.INFO, "Session BEAN   processing GetScheduledPayments Response to update components for the Customer  - {0}", cust);
        try {
            // if successful it should return a ArrayOfPayment Object from the getData method;

            recreatePaymentTableData();
            // PrimeFaces.current().ajax().update("customerslistForm1");
            updatePaymentTableComponents();
            //refreshFromDB = true;
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processGetScheduledPayments FAILED", ex);
        }
        LOGGER.log(Level.INFO, "Session BEAN processGetScheduledPayments completed");
    }

    private void updatePaymentLists(Payments pay) {
        LOGGER.log(Level.INFO, "updatePaymentLists started for {0}", pay.getId());
        if (paymentDBList != null) {
            LOGGER.log(Level.INFO, "updatePaymentList  paymentDBList : {0}", pay.getId());
            List<Payments> lp = (List<Payments>) paymentDBList.getWrappedData();
            int index = lp.indexOf(pay);
            if (index == -1) {
                lp.add(pay);
            } else {
                lp.set(index, pay);
            }
        }
        if (paymentsDBListFilteredItems != null) {
            LOGGER.log(Level.INFO, "updatePaymentList  paymentsDBListFilteredItems : {0}", pay.getId());
            int index = paymentsDBListFilteredItems.indexOf(pay);
            if (index == -1) {
                paymentsDBListFilteredItems.add(pay);
            } else {
                paymentsDBListFilteredItems.set(index, pay);
            }
        }
        LOGGER.log(Level.INFO, "updatePaymentLists completed for {0}", pay.getId());
    }

    private void removeFromPaymentLists(Payments pay) {
        if (paymentDBList != null) {
            List<Payments> lp = (List<Payments>) paymentDBList.getWrappedData();
            int index = lp.indexOf(pay);
            lp.remove(index);
        }
        if (paymentsDBListFilteredItems != null) {
            int index = paymentsDBListFilteredItems.indexOf(pay);
            if (index >= 0) {
                paymentsDBListFilteredItems.remove(index);
            }
        }
    }

    private void processAddPaymentBatch(PaymentGatewayResponse pgr) {

        LOGGER.log(Level.INFO, "Session BEAN processAddPaymentBatch started");
        recreatePaymentTableData();
        updatePaymentTableComponents();
        //PrimeFaces.current().ajax().update("\\:tv\\:paymentsForm");
        LOGGER.log(Level.INFO, "Session BEAN processAddPaymentBatch completed");
    }

    private void processDeletePaymentBatch(PaymentGatewayResponse pgr) {

        LOGGER.log(Level.INFO, "Session BEAN processDeletePaymentBatch started");
        recreatePaymentTableData();
        updatePaymentTableComponents();
        //PrimeFaces.current().ajax().update("\\:tv\\:paymentsForm");
        LOGGER.log(Level.INFO, "Session BEAN processDeletePaymentBatch completed");
    }

    private void processAddPaymentResult(PaymentGatewayResponse pgr) {
        int id = 0;
        LOGGER.log(Level.INFO, "Session BEAN processAddPaymentResult started");
        /*try {
            id = Integer.parseInt(pgr.getTextData());
        } catch (NumberFormatException numberFormatException) {
            LOGGER.log(Level.WARNING, "processAddPaymentResult - Payment reference could not convert to a number!");
        }*/

        Payments pay = (Payments) pgr.getData();
        id = pay.getId();
        pay = paymentsFacade.find(pay.getId());
        if (pay != null) {
            updatePaymentLists(pay);
            LOGGER.log(Level.INFO, "Session BEAN process Add Payment Result - updatePaymentLists ---> reference {0}", id);
        } else {
            LOGGER.log(Level.WARNING, "Session BEAN processAddPaymentResult - Payment could not be found in the cache or DB with reference {0}", id);
        }
        //updatePaymentTableComponents();
        //recreatePaymentTableData();"
        PrimeFaces.current().ajax().update("paymentsForm:paymentsTablePanel2");
        LOGGER.log(Level.INFO, "Session BEAN processAddPaymentResult completed, updating paymentsForm:paymentsTable2 ");
    }

    private void processCreateSchedule(PaymentGatewayResponse pgr) {
        /*boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment", "Successfully Created Schedule.");
            getPayments(18, 2);

        } else {
            JsfUtil.addErrorMessage("Payment", "The Create Schedule operation failed!.");
        }*/
        //recreatePaymentTableData();
        updatePaymentTableComponents();
        LOGGER.log(Level.INFO, "processCreateSchedule completed");
    }

    private void processEditCustomerDetails(PaymentGatewayResponse pgr) {
        /* boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Edited Customer Details  .");
            getCustDetailsFromEzi();
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }*/
        LOGGER.log(Level.INFO, "processEditCustomerDetails completed");
    }

    /*  private void processClearSchedule(Future ft) {
        String custDetails = "0,FAILED";
        try {
            custDetails = (String) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails.contains("OK")) {
            String[] res = custDetails.split(",");
            String ref = res[0];
            int reference = -1;
            try {
                reference = Integer.parseInt(ref);
            } catch (NumberFormatException numberFormatException) {
            }
            Customers cust = customersFacade.findById(reference);
            if (cust != null) {
                List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(cust, PaymentStatus.DELETE_REQUESTED.value());
                for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                    Payments p = crmPaymentList.get(x);
                    paymentsFacade.remove(p);
                }
            }
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Cleared Schedule .");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processClearSchedule completed");
    }*/
    private void processDeletePayment(PaymentGatewayResponse pgr) {
        /* String custDetails = "0,FAILED";
        String message = "The delete payment operation failed!.";
        try {
            custDetails = (String) ft.get();
        } catch (InterruptedException | ExecutionException | EJBException ex) {
            String causedBy = ex.getCause().getCause().getMessage();
            if (causedBy.contains("Payment selected for deletion could not be found")) {
                LOGGER.log(Level.WARNING, "deletePayment - Payment selected for deletion could not be found..");
                message = "The payment selected for deletion could not be found!";
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (custDetails.startsWith("ERROR:") == false) {

            int reference = -1;
            try {
                reference = Integer.parseInt(custDetails);

            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.WARNING, "Process deletePayment - Thepayment reference could not be converted to a number: {0}", new Object[]{custDetails});
            }
            Payments pay = paymentsFacade.findPaymentById(reference);
            if (pay != null) {
                removeFromPaymentLists(pay);
                paymentsFacade.remove(pay);

            } else {
                LOGGER.log(Level.WARNING, "Process deletePayment - Payment that was deleted could not be found in the our DB key={0}", new Object[]{reference});
            }
            setSelectedScheduledPayment(null);
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Deleted Payment  .");
            //getPayments(18, 2);

        } else {

            LOGGER.log(Level.WARNING, "Process deletePayment - DELETE PAYMENT FAILED: {0}", new Object[]{custDetails});
            String errorMessage = custDetails.substring(6);
            int k = errorMessage.indexOf(':');
            String paymentRef = errorMessage.substring(0, k);
            errorMessage = errorMessage.substring(k + 1);
            int id = 0;
            try {
                id = Integer.parseInt(paymentRef);
            } catch (NumberFormatException numberFormatException) {
                LOGGER.log(Level.WARNING, "Process deletePayment  FAILED - PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", custDetails);
            }
            Payments pay = paymentsFacade.findPaymentById(id);
            if (pay != null) {
                if (errorMessage.contains("Payment selected for deletion could not be found")) {
                    JsfUtil.addErrorMessage("Payment Gateway", "A payment with this reference could not be found in the payment gateway!");

                    if (pay.getBankFailedReason().contentEquals("MISSING")) {
                        paymentsFacade.remove(pay);
                        removeFromPaymentLists(pay);
                    } else {
                        pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                        paymentsFacade.edit(pay);
                        updatePaymentLists(pay);
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
                        paymentsFacade.edit(pay);
                        if (retries < 10) {
                            retryDeletePayment(pay);
                        } else {
                            pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                            paymentsFacade.edit(pay);
                            updatePaymentLists(pay);
                        }
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Process deletePayment  FAILED - Unhandled error ", custDetails);
                    JsfUtil.addSuccessMessage("Payment Gateway", "Deleted Payment Error - see logs for more details .");
                }

            }
        }*/
        int reference = -1;
        try {
            reference = Integer.parseInt(pgr.getTextData());

        } catch (NumberFormatException numberFormatException) {
            LOGGER.log(Level.WARNING, "Process deletePayment - Thepayment reference could not be converted to a number: {0}", new Object[]{pgr.getTextData()});
        }
        //Payments pay = paymentsFacade.findPaymentById(reference,false);
        Payments pay = (Payments) pgr.getData();

        //pay = paymentsFacade.find(pay.getId());
        if (pay != null) {
            removeFromPaymentLists(pay);

        } else {
            LOGGER.log(Level.WARNING, "Process deletePayment - Payment that was deleted could not be found in the our DB key={0}", new Object[]{reference});
        }
        //updatePaymentTableComponents();
        PrimeFaces.current().ajax().update("paymentsForm");

        LOGGER.log(Level.INFO, "processDeletePayment completed");
    }

    //moved to FutureMap
    private void processChangeCustomerStatus(PaymentGatewayResponse pgr) {
        CustomersController cc = getCustomersController();

        cc.setSelected(cc.getSelected());
        LOGGER.log(Level.INFO, "processChangeCustomerStatus completed");
    }

    private void processGetPaymentStatus(PaymentGatewayResponse pgr) {
        /*boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Retrieved Payment Status  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The getPayment status operation failed!.");
        }*/
        recreatePaymentTableData();
        LOGGER.log(Level.INFO, "processGetPaymentStatus completed");
    }

    private void processChangeScheduledAmount(PaymentGatewayResponse pgr) {
        /* boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully  Changed Scheduled Amount .");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }*/
        recreatePaymentTableData();
        LOGGER.log(Level.INFO, "processChangeScheduledAmount completed");
    }

    private void processChangeScheduledDate(PaymentGatewayResponse pgr) {
        /* boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Changed Scheduled Date  .");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }*/
        recreatePaymentTableData();
        LOGGER.log(Level.INFO, "processChangeScheduledDate completed");
    }

    private void processIsBsbValid(PaymentGatewayResponse pgr) {
        /*boolean custDetails = false;
        try {
            custDetails = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (custDetails == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Checked BSB  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }*/

        //this is only called on success
        LOGGER.log(Level.INFO, "processIsBsbValid completed: BSB Is Valid");
    }

    private void processIsSystemLocked(PaymentGatewayResponse pgr) {
        boolean result = false;
        try {
            result = pgr.isOperationSuccessful();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Payment Gateway Session Bean - processIsSystemLocked", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully checked if System is Locked  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processIsSystemLocked completed");
    }

    private void processGetPaymentExchangeVersion(PaymentGatewayResponse pgr) {
        String result = "";

        try {
            result = pgr.getTextData();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Payment Gateway Session Bean - processGetPaymentExchangeVersion", ex);
        }
        if (result != null && result.trim().isEmpty() == false) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Payment Exchange Version: " + result);

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        LOGGER.log(Level.INFO, "processGetPaymentExchangeVersion completed");
    }

    private void processGetCustomerDetailsAndPayments(PaymentGatewayResponse pgr) {
        CustomerDetails custDetails = null;
        Customers selectedCust = getSelectedCustomer();
        String cust = selectedCust.getUsername();
        setCustomerDetailsHaveBeenRetrieved(true);
        ArrayList<Object> returnedObjects = null;
        LOGGER.log(Level.INFO, "Ezidebit Controller -  processing processGetCustomerDetailsAndPayments Response Recieved from ezidebit for Customer  - {0}", cust);
        try {
            Object o = pgr.getData();
            if (o.getClass() == ArrayList.class) {
                returnedObjects = (ArrayList<Object>) o;
            }

            custDetails = (CustomerDetails) returnedObjects.get(2);
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processGetCustomerDetailsAndPayments - ArrayList of returned Objects Issue", ex);
            return;
        }
        if (custDetails != null) {
            // do something with custDetails
            setAutoStartPoller(false);

            String userId = custDetails.getYourSystemReference().getValue();
            if (userId.trim().matches(selectedCust.getId().toString())) {
                // the singleton FutureMap operates under a separate cache so we need to refresh our stale cache.
                // selectedCust = customersFacade.find(selectedCust.getId());
                //customersFacade.refreshfromDB(selectedCust);
                customerProvisionedInPaymentGW = null;
                setCurrentCustomerDetails(custDetails);
                //updatePaymentParameters(selectedCust,custDetails);
                String eziStatusCode = "Unknown";
                if (custDetails.getStatusDescription() != null) {
                    eziStatusCode = custDetails.getStatusDescription().getValue().toUpperCase().trim();
                }
                String ourStatus = selectedCust.getActive().getCustomerState().toUpperCase().trim();
                String message = "EZI - processGetCustomerDetailsAndPayments - Processing customer status codes. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + ". Pay Params StatusCode=" + selectedCust.getPaymentParametersId().getStatusCode();
                LOGGER.log(Level.INFO, message);
                if (ourStatus.contains(eziStatusCode) == false && (ourStatus.contains("ACTIVE") == true && eziStatusCode.contains("NEW") == true) == false) {
                    // status codes don't match

                    message = "Customer Status codes dont match. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                    if (eziStatusCode.contains("WAITING BANK DETAILS")) {
                        message = "The Customer does not have any banking details. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                        LOGGER.log(Level.INFO, message);
                        setWaitingForPaymentDetails(true);
                        JsfUtil.addSuccessMessage("Important!", "You must enter customer banking details before payments can be set up.");
                    } else {
                        LOGGER.log(Level.WARNING, message);
                        setWaitingForPaymentDetails(false);
                        JsfUtil.addErrorMessage(message, "");
                    }
                    if (eziStatusCode.trim().toUpperCase().contains("CANCELLED")) {
                        message = "The Customer is in a Cancelled status in the payment gateway";
                        LOGGER.log(Level.INFO, message);
                        JsfUtil.addSuccessMessage("Important!", message);
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
                String message = "The Customer data from teh payment gateway does not match the selected customer:" + selectedCust.getId().toString() + ", payment gateway reference No:" + custDetails.getYourSystemReference().toString();
                LOGGER.log(Level.SEVERE, message);
            }

        }
        List<Payment> payListFromGateway = (List<Payment>) returnedObjects.get(0);
        ArrayOfScheduledPayment resultArrayOfScheduledPayments = (ArrayOfScheduledPayment) returnedObjects.get(1);
        List<ScheduledPayment> schedPayListFromGateway = resultArrayOfScheduledPayments.getScheduledPayment();

        paymentsList = payListFromGateway;
        scheduledPaymentsList = schedPayListFromGateway;

        //PrimeFaces.current().ajax().update("customerslistForm1");
        //refreshFromDB = true;
        //getCustomersController().setRefreshFromDB(true);
        //getCustomersController().recreateModel();
        // updateASingleCustomersPaymentInfo(selectedCust);
        updatePaymentTableComponents();
        LOGGER.log(Level.INFO, "processGetCustomerDetailsAndPayments completed");
    }

    private void processGetCustomerDetails(PaymentGatewayResponse pgr) {
        CustomerDetails result = null;
        Customers selectedCust = getSelectedCustomer();
        String cust = selectedCust.getUsername();
        setCustomerDetailsHaveBeenRetrieved(true);
        LOGGER.log(Level.INFO, "Ezidebit Controller -  processing GetCustomerDetails Response Recieved from ezidebit for Customer  - {0}", cust);

        try {
            result = (CustomerDetails) pgr.getData();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetails", ex);
        }
        if (result != null) {
            // do something with custDetails
            setAutoStartPoller(false);

            String userId = result.getYourSystemReference().getValue();
            if (userId.trim().matches(selectedCust.getId().toString())) {
                // the singleton FutureMap operates under a separate cache so we need to refresh our stale cache.
                // selectedCust = customersFacade.find(selectedCust.getId());
                //customersFacade.refreshfromDB(selectedCust);
                customerProvisionedInPaymentGW = null;
                setCurrentCustomerDetails(result);
                //updatePaymentParameters(selectedCust,custDetails);
                String eziStatusCode = "Unknown";
                if (result.getStatusDescription() != null) {
                    eziStatusCode = result.getStatusDescription().getValue().toUpperCase().trim();
                }
                String ourStatus = selectedCust.getActive().getCustomerState().toUpperCase().trim();
                String message = "EZI - processGetCustomerDetails - Processing customer status codes. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + ". Pay Params StatusCode=" + selectedCust.getPaymentParametersId().getStatusCode();
                LOGGER.log(Level.INFO, message);
                if (ourStatus.contains(eziStatusCode) == false && (ourStatus.contains("ACTIVE") == true && eziStatusCode.contains("NEW") == true) == false) {
                    // status codes don't match

                    message = "Customer Status codes dont match. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                    if (eziStatusCode.contains("WAITING BANK DETAILS")) {
                        message = "The Customer does not have any banking details. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                        LOGGER.log(Level.INFO, message);
                        setWaitingForPaymentDetails(true);
                        JsfUtil.addSuccessMessage("Important!", "You must enter customer banking details before payments can be set up.");
                    } else {
                        LOGGER.log(Level.WARNING, message);
                        setWaitingForPaymentDetails(false);
                        JsfUtil.addErrorMessage(message, "");
                    }
                    if (eziStatusCode.trim().toUpperCase().contains("CANCELLED")) {
                        message = "The Customer is in a Cancelled status in the payment gateway";
                        LOGGER.log(Level.INFO, message);
                        JsfUtil.addSuccessMessage("Important!", message);
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
                String message = "The Customer data from teh payment gateway does not match the selected customer:" + selectedCust.getId().toString() + ", payment gateway reference No:" + result.getYourSystemReference().toString();
                LOGGER.log(Level.SEVERE, message);
            }

        }
        //PrimeFaces.current().ajax().update("customerslistForm1");

        //refreshFromDB = true;
        //getCustomersController().setRefreshFromDB(true);
        //getCustomersController().recreateModel();
        // updateASingleCustomersPaymentInfo(selectedCust);
        updatePaymentTableComponents();
        LOGGER.log(Level.INFO, "processGetCustomerDetails completed");
    }

    /* private void updateASingleCustomersPaymentInfo(Customers cust) {
        CustomersController cc = getCustomersController();

    }*/
    private CustomersController getCustomersController() {
        FacesContext context = FacesContext.getCurrentInstance();
        return (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");

    }

    public void refreshPaymentsPage(ActionEvent actionEvent) {
        loginToPushServer();
        //setSelectedCustomer(getSelectedCustomer());
        paymentDBList = null;
        paymentsList = null;

    }

    private void processGetCustomerDetailsFromEziDebitId(PaymentGatewayResponse pgr) {
        CustomerDetails result = null;
        try {
            result = (CustomerDetails) pgr.getData();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with custDetails
        }
        LOGGER.log(Level.INFO, "processGetCustomerDetailsFromEziDebitId completed");
    }

    private void processGetPaymentDetail(PaymentGatewayResponse pgr) {
        PaymentDetail result = null;
        try {
            result = (PaymentDetail) pgr.getData();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with custDetails
        }
        LOGGER.log(Level.INFO, "processGetPaymentDetail completed");
    }

    private void processGetPaymentDetailPlusNextPaymentInfo(PaymentGatewayResponse pgr) {
        PaymentDetailPlusNextPaymentInfo result = null;
        try {
            result = (PaymentDetailPlusNextPaymentInfo) pgr.getData();
        } catch (Exception ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with custDetails
        }
        LOGGER.log(Level.INFO, "processGetPaymentDetailPlusNextPaymentInfo completed");
    }

    private void stopPoller() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Stopping poller on customers page");
        PrimeFaces.current().ajax().addCallbackParam("stopPolling", true);
    }

    public void recreateModels() {
        // clear all arrays and reload from DB
        setPaymentsList(null);
        setPaymentsListFilteredItems(null);

        setPaymentDBList(null);
        setPaymentsDBListFilteredItems(null);

        setScheduledPaymentsList(null);
        setScheduledPaymentsListFilteredItems(null);

        //setCustomerDetailsHaveBeenRetrieved(false);
        setCurrentCustomerDetails(null);

    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    /* public void setSelectedCustomer(Customers selectedCustomer) {
        this.selectedCustomer = selectedCustomer;
        this.currentCustomerDetails = null;
        refreshIFrames = true;
        futureMap.cancelFutures(sessionId);
        // setAsyncOperationRunning(false);
        if (isTheCustomerProvisionedInThePaymentGateway()) {
            getCustDetailsFromEzi();

            getPayments(18, 2);
        } else {
            setCustomerDetailsHaveBeenRetrieved(true);
        }
        this.progress = 0;

        /*CustomerDetails cd = getCustomerDetails(selectedCustomer);
         if (cd == null) {
         theCustomerProvisionedInThePaymentGateway = false;
         } else {
         theCustomerProvisionedInThePaymentGateway = true;
         }*/
 /*int pp = selectedCustomer.getPaymentParametersCollection().size();
         if( pp > 0) {
         theCustomerProvisionedInThePaymentGateway = false;
         } else {
         theCustomerProvisionedInThePaymentGateway = true;
         }*/
    //}
    public String closeEditPaymentMobile(ActionEvent actionEvent) {
        setEditPaymentMethodEnabled(false);
        getCustDetailsFromEzi();
        getPayments(18, 2);
        return "pm:main";
    }

    public void closeEditPaymentMethodDialogue(ActionEvent actionEvent) {
        setEditPaymentMethodEnabled(false);
        getCustDetailsFromEzi();
        getPayments(18, 2);
    }

    public void editPaymentMethodDialogue(ActionEvent actionEvent) {
        setEditPaymentMethodEnabled(true);
    }

    private void startAsynchJob(String key, Future future) {
        // setAsyncOperationRunning(true);
        AsyncJob aj = new AsyncJob(key, future);
        futureMap.put(sessionId, aj);

    }

    public void createEddrLink(ActionEvent actionEvent) {
        Customers cust = getSelectedCustomer();
        if (cust == null || cust.getId() == null) {
            LOGGER.log(Level.WARNING, "Create EDDR Link cannot be completed as the selected customer is null.");
            return;
        }

        PaymentParameters pp = null;
        Long amount = (long) (paymentAmountInCents * (float) 100);
        Long amountLimit = paymentLimitAmountInCents * (long) 100;
        char spt = paymentSchedulePeriodType.charAt(0);
        GregorianCalendar debitDate = new GregorianCalendar();
        debitDate.setTime(paymentDebitDate);
        SimpleDateFormat sdf2 = new SimpleDateFormat("E");
        GregorianCalendar endCal = new GregorianCalendar();
        //endCal.setTime(paymentDebitDate);// ezi debit only supports 1 year from current date
        endCal.add(Calendar.YEAR, 1);
        String dom = Integer.toString(debitDate.get(Calendar.DAY_OF_MONTH));

        pp = cust.getPaymentParametersId();
        if (pp == null) {
            getCustomersController().createDefaultPaymentParameters(cust);
            pp = cust.getPaymentParametersId();
        }
        pp.setContractStartDate(paymentDebitDate);
        pp.setPaymentPeriod(paymentSchedulePeriodType);
        pp.setPaymentPeriodDayOfMonth(dom);
        pp.setPaymentPeriodDayOfWeek(sdf2.format(debitDate.getTime()).toUpperCase());
        pp.setNextScheduledPayment(null);
        pp.setPaymentRegularAmount(new BigDecimal(amount));
        pp.setPaymentRegularTotalPaymentsAmount(new BigDecimal(amountLimit));
        pp.setPaymentsRegularTotalNumberOfPayments(paymentLimitToNumberOfPayments);
        String dur = "1";
        if (paymentLimitToNumberOfPayments == 0 && amountLimit == 0) {
            pp.setPaymentRegularDuration(1);//ongoing payments
        } else {
            if (paymentLimitToNumberOfPayments > 0) {
                pp.setPaymentRegularDuration(4);// Total Amount
                dur = "4";
            }
            if (amountLimit > 0) {
                pp.setPaymentRegularDuration(2);// Total payments 
                dur = "2";
            }
            if (amountLimit > 0 && paymentLimitToNumberOfPayments > 0) {
                pp.setPaymentRegularDuration(6);// Total payments - takes precedence if both are set
            }
        }
        ejbPaymentParametersFacade.edit(pp);
        customersFacade.edit(cust);
        String urlPaymentPeriodFormat = convertPaymentPeriod(paymentSchedulePeriodType);
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        String amp = "&";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        String widgetUrl = configMapFacade.getConfig("payment.ezidebit.webddr.baseurl");
        widgetUrl += "?" + "a=" + configMapFacade.getConfig("payment.ezidebit.webddr.hash");
        widgetUrl += amp + "uRefLabel=" + configMapFacade.getConfig("payment.ezidebit.webddr.uRefLabel");
        widgetUrl += amp + "fName=" + cust.getFirstname().trim();
        widgetUrl += amp + "lName=" + cust.getLastname().trim();
        widgetUrl += amp + "uRef=" + cust.getId();
        widgetUrl += amp + "email=" + cust.getEmailAddress().trim();
        widgetUrl += amp + "mobile=" + cust.getTelephone().trim();
        if (pp.getSmsPaymentReminder().contains("YES")) {
            widgetUrl += amp + "sms=1";
        } else {
            widgetUrl += amp + "sms=0";
        }
        widgetUrl += amp + "addr=" + cust.getStreetAddress();
        widgetUrl += amp + "suburb=" + cust.getSuburb();
        widgetUrl += amp + "state=" + cust.getAddrState();
        widgetUrl += amp + "pCode=" + cust.getPostcode();
        String db = configMapFacade.getConfig("payment.ezidebit.webddr.debits");
        if (db.contains("0") == false) {
            widgetUrl += amp + "debits=" + db;// if its 0 leave unset to show once off and regular debits
        }
        if (paymentDebitDate.compareTo(oneOffPaymentDate) == 0) {
            if (paymentAmountInCents == oneOffPaymentAmount) {
                // as the ezidebit form will add the payments without a reference number we need to be able to differentiate between them if they are for teh same amount on the same date.
                oneOffPaymentAmount += (float) 0.01;
            }
        }
        if (paymentAmountInCents > 0) {
            widgetUrl += amp + "rAmount=" + nf.format((pp.getPaymentRegularAmount().divide(new BigDecimal(100))));
            widgetUrl += amp + "rDate=" + sdf.format(paymentDebitDate);
        } else {
            widgetUrl += amp + "debits=1";
        }
        if (oneOffPaymentAmount > 0) {
            widgetUrl += amp + "oAmount=" + nf.format(oneOffPaymentAmount);
            widgetUrl += amp + "oDate=" + sdf.format(oneOffPaymentDate);
        } else {
            widgetUrl += amp + "debits=2";
        }

        widgetUrl += amp + "aFreq=" + urlPaymentPeriodFormat;
        widgetUrl += amp + "freq=" + urlPaymentPeriodFormat;
        widgetUrl += amp + "aDur=" + pp.getPaymentRegularDuration().toString();
        widgetUrl += amp + "dur=" + dur;
        widgetUrl += amp + "businessOrPerson=1";
        if (paymentLimitToNumberOfPayments > 0) {
            widgetUrl += amp + "tPay=" + Integer.toString(paymentLimitToNumberOfPayments);
        }
        if (amountLimit > 0) {
            widgetUrl += amp + "tAmount=" + nf.format((new Float(amountLimit) / (float) 100));
        }
        widgetUrl += amp + "callback=" + configMapFacade.getConfig("payment.ezidebit.webddr.callback");
        pp.setWebddrUrl(widgetUrl);
        String oldUrl = "Empty";
        if (cust.getPaymentParametersId().getWebddrUrl() != null) {
            oldUrl = cust.getPaymentParametersId().getWebddrUrl();
        }
        ejbPaymentParametersFacade.edit(pp);
        customersFacade.edit(cust);
        LOGGER.log(Level.INFO, "eddr request url:{0}.", widgetUrl);
        createCombinedAuditLogAndNote(getCustomers(), cust, "createEddrLink", "The Direct Debit Request form was modified.", oldUrl.replace(amp, ", "), widgetUrl.replace(amp, ", "));
    }

    private String convertPaymentPeriod(String period) {
        String rt = "";
        switch (period) {
            case "W":
                rt = "1";
                break;
            case "F":
                rt = "2";
                break;
            case "M":
                rt = "4";
                break;
            case "4":
                rt = "8";
                break;
            case "N":
                rt = "";
                break;
            case "Q":
                rt = "16";
                break;
            case "H":
                rt = "32";
                break;
            case "Y":
                rt = "64";
                break;
        }

        return rt;
    }

    public void updatePaymentScheduleForm() {
        PaymentParameters pp = getSelectedCustomer().getPaymentParametersId();
        if (pp != null
                && pp.getWebddrUrl() != null
                && pp.getWebddrUrl().isEmpty() == false
                && pp.getContractStartDate() != null
                && pp.getPaymentRegularAmount() != null
                && pp.getPaymentRegularTotalPaymentsAmount() != null
                && pp.getPaymentsRegularTotalNumberOfPayments() != null
                && pp.getPaymentPeriod() != null) {

            paymentDebitDate = pp.getContractStartDate();
            paymentAmountInCents = pp.getPaymentRegularAmount().floatValue() / (float) 100;
            paymentLimitAmountInCents = pp.getPaymentRegularTotalPaymentsAmount().longValue() / (long) 100;
            paymentLimitToNumberOfPayments = pp.getPaymentsRegularTotalNumberOfPayments();
            paymentSchedulePeriodType = pp.getPaymentPeriod();

            NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
            nf.setMaximumFractionDigits(2);
            nf.setMinimumFractionDigits(2);
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
            String amp = "&";
            String url = pp.getWebddrUrl();
            String[] params = url.split(amp);
            for (String p : params) {
                String[] kv = p.split("=");
                if (kv.length == 2) {
                    String k = kv[0];
                    String v = kv[1];
                    try {
                        if (k.contentEquals("oAmount")) {
                            oneOffPaymentAmount = nf.parse(v).floatValue();

                        }
                        if (k.contentEquals("oDate")) {

                            oneOffPaymentDate = sdf.parse(v);
                        }
                    } catch (ParseException parseException) {
                        LOGGER.log(Level.FINE, "updatePaymentScheduleForm - could not parse one off payment info from getWebddrUrl for user", getSelectedCustomer().getUsername());
                    }

                }

            }
        } else {
            //set to default values as this customer has not been set up yet
            paymentDebitDate = new Date();
            paymentAmountInCents = 0;
            paymentLimitAmountInCents = (long) 0;
            paymentLimitToNumberOfPayments = 0;
            oneOffPaymentAmount = 0;
            oneOffPaymentDate = new Date();

        }

    }

    private Properties emailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));

        return props;

    }
    
  
    public void createPaymentSchedule(ActionEvent actionEvent) {

        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmountInCents * (float) 100);
        Long amountLimit = paymentLimitAmountInCents * (long) 100;
        char spt = paymentSchedulePeriodType.charAt(0);
        GregorianCalendar endCal = new GregorianCalendar();
        //endCal.setTime(paymentDebitDate);// ezi debit only supports 1 year from current date so only add 9 months in advance. A cronjob will keep add new payments each week if necessary to keep it a 9 months
        endCal.add(Calendar.MONTH, MONTHS_IN_ADVANCE_FOR_PAYMENT_SCHEDULE);
        int dow = Calendar.MONDAY;
        if (paymentDayOfWeek.contains("TUE")) {
            dow = Calendar.TUESDAY;
        }
        if (paymentDayOfWeek.contains("WED")) {
            dow = Calendar.WEDNESDAY;
        }
        if (paymentDayOfWeek.contains("THU")) {
            dow = Calendar.THURSDAY;
        }
        if (paymentDayOfWeek.contains("FRI")) {
            dow = Calendar.FRIDAY;
        }
        if (loggedInUser != null) {

            String dom = Integer.toString(paymentDayOfMonth);
            Customers c = getSelectedCustomer();
            PaymentParameters pp = c.getPaymentParametersId();
            pp.setPaymentPeriod(paymentSchedulePeriodType);
            pp.setPaymentPeriodDayOfMonth(dom);
            pp.setPaymentPeriodDayOfWeek(paymentDayOfWeek);
            pp.setNextScheduledPayment(null);
            pp.setPaymentRegularAmount(new BigDecimal(amount));
            pp.setPaymentRegularTotalPaymentsAmount(new BigDecimal(amountLimit));
            pp.setPaymentsRegularTotalNumberOfPayments(paymentLimitToNumberOfPayments);
            if (paymentLimitToNumberOfPayments == 0 && amountLimit == 0) {
                pp.setPaymentRegularDuration(1);//ongoing payments
            } else {
                if (paymentLimitToNumberOfPayments > 0) {
                    pp.setPaymentRegularDuration(4);// Total Amount
                }
                if (amountLimit > 0) {
                    pp.setPaymentRegularDuration(2);// Total payments - takes precedence if both are set
                }
            }
            ejbPaymentParametersFacade.edit(pp);
            customersFacade.edit(c);
            startAsynchJob("CreateSchedule", paymentBean.createCRMPaymentSchedule(c, paymentDebitDate, endCal.getTime(), spt, dow, paymentDayOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean, false));
            /* List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(c, PaymentStatus.SCHEDULED.value());
             for (Payments p : crmPaymentList) {
             if (!(paymentKeepManualPayments && p.getManuallyAddedPayment())) {
             paymentsFacade.remove(p);
             }
             }
             startAsynchJob("CreateSchedule", paymentBean.createSchedule(c, paymentDebitDate, spt, paymentDayOfWeek, paymentDayOfMonth, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
             */
            JsfUtil.addSuccessMessage("Sending CreateSchedule Request to Payment Gateway.");
            //startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(c, paymentDebitDate, endCal.getTime(), getDigitalKey()));
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
        //getPayments(18, 2);
        //paymentDBList = null;
        //paymentsDBListFilteredItems = null;
    }

    /* private void retryAddPayment(Payments pay) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        LOGGER.log(Level.INFO, "Retry Payment Created for customer {0} with paymentID: {1}", new Object[]{pay.getCustomerName().getUsername(), pay.getId()});
        setAsyncOperationRunning(paymentBean.retryAddNewPayment(pay, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean));

        paymentDBList = null;
        paymentsDBListFilteredItems = null;

    }

    private void retryDeletePayment(Payments pay) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        LOGGER.log(Level.INFO, "Retry Payment Created for customer {0} with paymentID: {1}", new Object[]{pay.getCustomerName().getUsername(), pay.getId()});
        setAsyncOperationRunning(paymentBean.retryDeletePayment(pay, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean));

        paymentDBList = null;
        paymentsDBListFilteredItems = null;

    }*/
    public void addSinglePayment(Customers cust, float paymentAmount, Date debitDate) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmount * (float) 100);
        // setAsyncOperationRunning(true);
        paymentBean.addNewPayment(cust, debitDate, amount, true, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean, 0);
        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    public void addSinglePayment(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmountInCents * (float) 100);
        // setAsyncOperationRunning(true);
        paymentBean.addNewPayment(getSelectedCustomer(), paymentDebitDate, amount, true, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean, 0);
        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    public void logSingleCashPayment(ActionEvent actionEvent) {
        try {
            Payments newPayment = new Payments(0);
            newPayment.setPaymentSource(PaymentSource.CASH.value());
            newPayment.setDebitDate(paymentDebitDate);
            newPayment.setSettlementDate(paymentDebitDate);
            newPayment.setCreateDatetime(new Date());
            newPayment.setLastUpdatedDatetime(new Date());
            newPayment.setYourSystemReference(getSelectedCustomer().getId().toString());
            newPayment.setPaymentAmount(new BigDecimal(paymentAmountInCents));
            newPayment.setScheduledAmount(new BigDecimal(paymentAmountInCents));
            newPayment.setCustomerName(getSelectedCustomer());
            newPayment.setPaymentStatus(PaymentStatus.SUCESSFUL.value());
            newPayment.setManuallyAddedPayment(true);
            newPayment.setBankReturnCode("");
            newPayment.setBankFailedReason("");
            newPayment.setBankReceiptID(cashPaymentReceiptReference);
            newPayment.setTransactionFeeClient(BigDecimal.ZERO);
            newPayment.setTransactionFeeCustomer(BigDecimal.ZERO);
            paymentsFacade.createAndFlushForGeneratedIdEntities(newPayment);

            String newPaymentID = newPayment.getId().toString();
            newPayment.setPaymentReference(newPaymentID);
            newPayment.setPaymentID("Cash_Payment" + "_" + newPaymentID);
            paymentsFacade.edit(newPayment);
            LOGGER.log(Level.INFO, "Cash/Direct Deposit Payment Created for customer {0} with paymentID: {1}", new Object[]{getSelectedCustomer().getUsername(), newPaymentID});

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Cash/Direct Deposit Payment Payment failed due to exception:", e);
        }

        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    private void clearSchedPayments(Customers cust, boolean keepManual) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (loggedInUser != null) {
            List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(cust, true);
            if (crmPaymentList != null) {
                LOGGER.log(Level.INFO, "createSchedule - Found {0} existing scheduled payments for {1}", new Object[]{crmPaymentList.size(), cust.getUsername()});
                for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                    Payments p = crmPaymentList.get(x);
                    String ref = p.getId().toString();
                    boolean isManual = true;// all payments now manual as far as ezidebit is concerned. we just add and delete payments and dont use their scheduling
                    if (p.getManuallyAddedPayment() != null) {
                        isManual = p.getManuallyAddedPayment();
                    }

                    if (keepManual == true && isManual) {
                        LOGGER.log(Level.INFO, "createSchedule - keeping manual payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                    } else {
                        LOGGER.log(Level.INFO, "createSchedule - Deleting payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{cust.getUsername(), ref, isManual});
                        // if (paymentKeepManualPayments == false) {
                        //     paymentsFacade.remove(p);
                        //  } else {
                        p.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                        paymentsFacade.edit(p);
                        updatePaymentLists(p);
                        if (keepManual == true) {
                            startAsynchJob("DeletePayment", paymentBean.deletePayment(cust, null, null, p, loggedInUser, getDigitalKey(), sessionId));
                        }
                    }
                }
            }
            if (keepManual == false) {
                startAsynchJob("ClearSchedule", paymentBean.clearSchedule(cust, false, loggedInUser, getDigitalKey(), sessionId));
            }

            PaymentParameters pp = cust.getPaymentParametersId();
            if (pp != null) {

                pp.setPaymentPeriod("Z");
                pp.setPaymentPeriodDayOfMonth("-");
                pp.setPaymentPeriodDayOfWeek("---");
                pp.setNextScheduledPayment(null);
                ejbPaymentParametersFacade.edit(pp);
                customersFacade.edit(cust);
            }
            // paymentDBList = null;
            //  paymentsDBListFilteredItems = null;
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. clearSchedule aborted.");
        }

    }

    public void clearEntireSchedule(Customers cust) {
        clearSchedPayments(cust, false);
    }

    public void clearSchedule(ActionEvent actionEvent) {
        clearSchedPayments(getSelectedCustomer(), paymentKeepManualPayments);

    }

    public void updateSelectedScheduledPayment(SelectEvent event) {
        Object o = event.getObject();
        if (o != null) {
            if (o.getClass() == Payments.class) {
                Payments pay = (Payments) o;
                selectedScheduledPayment = pay;
                LOGGER.log(Level.INFO, "Paymenmt Table Payment Selected. Payment Id :{0}", selectedScheduledPayment.getId().toString());
            } else {
                LOGGER.log(Level.WARNING, "Payment Table Payment Select Failed.");
            }

        } else {
            LOGGER.log(Level.WARNING, "Select Event Object is NULL - updateSelectedScheduledPayment.");
        }
    }

    public void deleteScheduledPayment(ActionEvent actionEvent) {
        if (selectedScheduledPayment != null) {
            String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
            Double amount = selectedScheduledPayment.getScheduledAmount().floatValue() * (double) 100;
            if (loggedInUser != null) {
                Payments pay = paymentsFacade.findPaymentById(selectedScheduledPayment.getId(), false);

                if (pay != null) {
                    if (pay.getPaymentStatus().contentEquals(PaymentStatus.SCHEDULED.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.WAITING.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.DELETE_REQUESTED.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.REJECTED_CUST_ON_HOLD.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.MISSING_IN_PGW.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.REJECTED_BY_GATEWAY.value())) {

                        if (pay.getPaymentStatus().contentEquals(PaymentStatus.MISSING_IN_PGW.value())) {
                            pay.setBankFailedReason("DELETED");

                            paymentDBList = null;
                            paymentsDBListFilteredItems = null;
                            LOGGER.log(Level.INFO, "Deleted payment that was missing in payment gateway.The payment gateway can delete scheduled payemnts when a user is on hold. Payment ID:{0}", new Object[]{pay.getId()});
                            paymentsFacade.remove(pay);
                            createCombinedAuditLogAndNote(getCustomers(), pay.getCustomerName(), "PAYMENT_DELETED", "A scheduled payment missing in the gateway was deleted.", "Payment Amount:" + pay.getScheduledAmount().toPlainString() + ", Date:" + pay.getDebitDate().toString(), "DELETED");
                        } else {
                            pay.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                            paymentsFacade.edit(pay);
                            updatePaymentLists(pay);
                            startAsynchJob("DeletePayment", paymentBean.deletePayment(getSelectedCustomer(), selectedScheduledPayment.getDebitDate(), amount.longValue(), selectedScheduledPayment, loggedInUser, getDigitalKey(), sessionId));
                        }

                    }
                    if (pay.getPaymentStatus().contentEquals(PaymentStatus.SUCESSFUL.value())) {
                        if (pay.getPaymentSource() != null) {
                            if (pay.getPaymentSource().contentEquals(PaymentSource.CASH.value())) {
                                pay.setBankFailedReason("DELETED");

                                paymentDBList = null;
                                paymentsDBListFilteredItems = null;
                                LOGGER.log(Level.INFO, "Deleted CASH payment. Payment ID:{0}", new Object[]{pay.getId()});
                                paymentsFacade.remove(pay);

                            }
                            createCombinedAuditLogAndNote(getCustomers(), pay.getCustomerName(), "PAYMENT_DELETED", "A scheduled payment missing in the gateway was deleted.", "Payment Amount:" + pay.getScheduledAmount().toPlainString() + ", Date:" + pay.getDebitDate().toString(), "DELETED");

                        }
                    }

                } else {
                    LOGGER.log(Level.WARNING, "deleteScheduledPayment , cant't find the local scheduled payment in our DB.");
                }
            } else {
                LOGGER.log(Level.WARNING, "Logged in user is null. Delete Payment aborted.");
            }
        }
    }

    public void changeCustomerStatus(Customers cust, CustomerState cs) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        String newStatus = cs.getCustomerState();
        String eziStatus;
        if (cs.getCustomerState().contains("ACTIVE")) {
            eziStatus = "A";
        } else if (cs.getCustomerState().contains("ON HOLD")) {
            eziStatus = "H";
        } else if (cs.getCustomerState().contains("CANCELLED")) {
            eziStatus = "C";
            if (isTheCustomerProvisionedInThePaymentGateway() == true) {
                startAsynchJob("ClearSchedule", paymentBean.clearSchedule(cust, false, loggedInUser, getDigitalKey(), sessionId));
            }
        } else {
            LOGGER.log(Level.WARNING, "Customer status is not one of ACTIVE,ON HOLD or CANCELLED. changeCustomerStatus aborted.");
            return;
        }

        if (loggedInUser != null) {
            if (isTheCustomerProvisionedInThePaymentGateway() == true) {
                startAsynchJob("ChangeCustomerStatus", paymentBean.changeCustomerStatus(cust, eziStatus, loggedInUser, getDigitalKey(), sessionId));
                LOGGER.log(Level.INFO, "Starting Async Job ChangeCustomerStatus.");
            }
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. changeCustomerStatus aborted.");
        }
    }

    public void changeScheduledAmount(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmountInCents * (float) 100);
        if (loggedInUser != null) {
            List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(getSelectedCustomer(), PaymentStatus.WAITING.value());
            for (Payments p : crmPaymentList) {
                if (!(paymentKeepManualPayments && p.getManuallyAddedPayment())) {
                    paymentsFacade.remove(p);
                }
            }
            startAsynchJob("ChangeScheduledAmount", paymentBean.changeScheduledAmount(getSelectedCustomer(), paymentDebitDate, amount, paymentLimitToNumberOfPayments, applyToAllFuturePayments, paymentKeepManualPayments, loggedInUser, getDigitalKey(), sessionId));
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    public void changeScheduledDate(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String paymentReference = getSelectedCustomer().getId().toString() + "-" + sdf.format(new Date());
        if (loggedInUser != null) {
            startAsynchJob("ChangeScheduledDate", paymentBean.changeScheduledDate(getSelectedCustomer(), changeFromDate, paymentDebitDate, paymentReference, paymentKeepManualPayments, loggedInUser, getDigitalKey(), sessionId));
        } else {
            LOGGER.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    /* private INonPCIService getWs() {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        return new NonPCIService(url).getBasicHttpBindingINonPCIService();
    }*/
    public String createBulk() {
        // this script doea a bulk inport from the payment gateway
        // delimiter is semi colon as ezidebit puts commas in the report
        // example line from csv file -Note: delimter is semicolon and line feeds replaced with tabs in column1
        //
        //"LASTNAME, TEST66\t  Our Ref: 664898\t  Your Ref: LASTNAME TEST66";Incorrect BSB or A/C;$795.00;15/07/14;22/07/14;0;1;
        int count = 0;
        try {
            String[] st = bulkvalue.split("\r\n");
            ArrayList<Integer> duplicaterows = new ArrayList<>();
            //sort the array and remove duplicates
            for (int x = 0; x < st.length; x++) {
                String line = st[x];
                // remove any non apha chars that will break the name search
                line = line.replaceAll("[^;:,a-zA-Z0-9\\s\t/]", "");
                int d = line.indexOf(";");
                if (d > 1) {// make sure we have a valid row
                    String[] cells = line.split(";");
                    String references[] = cells[0].split("\t");
                    if (references.length == 3) {
                        String status = cells[1];
                        String startDateText = cells[3];
                        String name = references[0];
                        if (status.contains("Cancelled") == false) {// we only want to check non cancelled customers for duplicates
                            for (int y = 0; y < st.length; y++) {
                                if (y != x) {
                                    String line2 = st[y];
                                    line2 = line2.replaceAll("[^;:,a-zA-Z0-9\\s\t/]", "");
                                    int d2 = line.indexOf(";");
                                    if (d2 > 1) {// make sure we have a valid row
                                        String[] cells2 = line2.split(";");
                                        String references2[] = cells2[0].split("\t");
                                        if (references2.length == 3) {
                                            if (name.contains(references2[0])) {
                                                // duplicate found
                                                if (cells2[1].contains("Cancelled") == true) {
                                                    duplicaterows.add(y);
                                                    LOGGER.log(Level.INFO, "Duplicate Found: {0}", line2);
                                                } else {
                                                    LOGGER.log(Level.WARNING, "Non Cancelled Duplicate Found: {0}", line2);
                                                }
                                            }
                                        }
                                        // String status2 = cells2[1];
                                        // String startDateText2 = cells2[3];
                                        // String name2 = references[0];
                                    }

                                }
                            }
                        }
                    }
                }
            }
            String duplicateLines = "";
            List<String> sta = new ArrayList<>(Arrays.asList(st));
            for (int z = sta.size() - 1; z >= 0; z--) {
                if (duplicaterows.contains(z)) {
                    String message = "REMOVING DUPLICATE: " + sta.get(z);
                    LOGGER.log(Level.INFO, message);
                    duplicateLines += message + "\r\n";
                    sta.remove(z);
                }
            }

            st = new String[sta.size()];
            st = sta.toArray(st);

            CustomerDetails cd = null;
            for (int x = 0; x < st.length; x++) {
                String line = st[x];
                // remove any non apha chars that will break the name search
                line = line.replaceAll("[^;:,a-zA-Z0-9\\s\t/]", "");
                int d = line.indexOf(";");
                if (d > 1) {
                    String[] cells = line.split(";");
                    if (cells.length > 3) {

                        count++;

                        String references[] = cells[0].split("\t");
                        if (references.length == 3) {
                            String status = cells[1];
                            String startDateText = cells[3];
                            String lastname = references[0];
                            String firstname = "";
                            String name[] = references[0].split(",");
                            if (name.length > 1) {
                                lastname = name[0].replaceAll("[^a-zA-Z0-9\\s]", "").trim();
                                firstname = name[1].replaceAll("[^a-zA-Z0-9\\s]", "").trim();

                            } else {
                                LOGGER.log(Level.INFO, "No firstname for {0}", references[0]);
                            }
                            String ezidebitRef = references[1].substring(references[1].indexOf(":") + 1);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
                            Date contractStartDate = sdf.parse(startDateText);

                            // check for existing customer in our database
                            Customers localCustomer = customersFacade.findCustomerByName(firstname, lastname);
                            LOGGER.log(Level.INFO, "Looking for {0} {1} in the local CRM database", new Object[]{firstname, lastname});

                            if (localCustomer != null) {
                                LOGGER.log(Level.INFO, "Found {0} {1} in the local CRM database with CRM id:{2}", new Object[]{firstname, lastname, localCustomer.getId()});
                                addDefaultPaymentParametersIfEmpty(localCustomer, contractStartDate);
                                LOGGER.log(Level.INFO, "Looking for {0} {1} in the payment Gateway database using Ezidebit reference:{2} database", new Object[]{firstname, lastname, ezidebitRef});

                                EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(getDigitalKey(), ezidebitRef, "");
                                if (customerdetails.getError() == 0) {// any errors will be a non zero value
                                    LOGGER.log(Level.INFO, "Get Customer (EziId) Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());
                                    LOGGER.log(Level.INFO, "Found {0} {1} in the Payment Gateway database with Ezidebit id:{2}", new Object[]{firstname, lastname, ezidebitRef});
                                    cd = customerdetails.getData().getValue();

                                } else {
                                    LOGGER.log(Level.WARNING, "Get Customer (EziId) Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
                                }
                                if (cd != null) {
                                    boolean updateSystemRefInEzidebit = false;
                                    // found the customer in ezidebit. check the system reference and update it to our customers primary key
                                    int key = localCustomer.getId();
                                    String eziYourSysRef = cd.getYourSystemReference().getValue();
                                    String eziSysRef = cd.getEzidebitCustomerID().getValue();
                                    LOGGER.log(Level.INFO, "Customer {0}, Our primary Key: {1},EziYourSysRef {3}, Ezidebit Ref: {2}", new Object[]{references[0], key, eziSysRef, eziYourSysRef});
                                    try {
                                        int eziRef = Integer.parseInt(eziYourSysRef);
                                        if (eziRef == key) {
                                            LOGGER.log(Level.INFO, "Keys already match. Nothing to do.", new Object[]{references[0], key, eziYourSysRef});
                                        } else {
                                            LOGGER.log(Level.SEVERE, "The system references don't match!  Possible Duplicate!");
                                            duplicateLines += "The system references don't match! Possible Duplicate! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";
                                            updateSystemRefInEzidebit = true;
                                        }

                                    } catch (NumberFormatException numberFormatException) {
                                        updateSystemRefInEzidebit = true;
                                    }
                                    if (updateSystemRefInEzidebit == true) {
                                        try {
                                            Future<Boolean> res = paymentBean.editCustomerDetails(localCustomer, eziSysRef, getCustomers(), getDigitalKey());

                                            if (res.get(180, TimeUnit.SECONDS) == true) {
                                                LOGGER.log(Level.INFO, "System reference updated");
                                            } else {
                                                LOGGER.log(Level.WARNING, "System reference update Failed");
                                                duplicateLines += "The System reference update Failed ! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";

                                            }
                                        } catch (InterruptedException | ExecutionException | TimeoutException interruptedException) {
                                            LOGGER.log(Level.WARNING, "System reference update Failed due to timeout, execution or interuption exception!");
                                            duplicateLines += "The System reference update Failed ! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";

                                        } catch (EJBException ex) {
                                            Exception causedByEx = ex.getCausedByException();
                                            LOGGER.log(Level.WARNING, "System reference update Failed due to EJB Excpetion:", causedByEx.getMessage());
                                            duplicateLines += "The System reference update Failed ! System Error " + causedByEx.getMessage() + "\r\n";

                                        }

                                    }

                                    status = status.toUpperCase().trim();
                                    if (status.contains("HOLD")) {
                                        status = "ON HOLD";
                                    }
                                    if (status.contains("NEW") || status.contains("WAITING BANK DETAILS") || status.contains("INCORRECT BSB")) {
                                        status = "ACTIVE";
                                    }
                                    List<CustomerState> csa = customerStateFacade.findAll();
                                    for (CustomerState cs : csa) {
                                        if (status.toUpperCase().contains(cs.getCustomerState())) {
                                            if (localCustomer.getActive().getCustomerState().contains(cs.getCustomerState()) == false) {
                                                localCustomer.setActive(cs);
                                                customersFacade.editAndFlush(localCustomer);
                                                LOGGER.log(Level.INFO, "Updating CRM Status to: {0}. EziDebit Status: {1}", new Object[]{cs.getCustomerState(), status});
                                            }
                                        }
                                    }

                                } else {

                                    LOGGER.log(Level.WARNING, "Could not find the customer in ezidebit {0}", line);

                                }
                            } else {
                                LOGGER.log(Level.WARNING, "Customer does not exist in local system! Customer {0}\r\n", references[0]);
                                duplicateLines += "Customer does not exist in local system! Customer " + references[0] + "\r\n";

                            }

                        }

                    }
                }
            }
            setDuplicateValues(duplicateLines);

            LOGGER.log(Level.INFO, "Customer sync with Ezidebit completed. Updated or created count={0}", count);
            JsfUtil.addSuccessMessage(count + ", " + configMapFacade.getConfig("EzidebitImportCreated"));
            return "/admin/customers/List";
        } catch (ParseException e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }
    }

    /**
     * @return the paymentGatewayVersion
     */
    public String getPaymentGatewayVersion() {
        if (paymentGatewayVersion == null) {
            Future<PaymentGatewayResponse> ft = paymentBean.getPaymentExchangeVersion(sessionId);
            try {
                PaymentGatewayResponse pgr = ft.get();
                paymentGatewayVersion = pgr.getTextData();
            } catch (InterruptedException | ExecutionException ex) {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, " getPaymentGatewayVersion FAILED", ex);
            }
        }
        return paymentGatewayVersion;
    }

    /**
     * @param paymentGatewayVersion the paymentGatewayVersion to set
     */
    public void setPaymentGatewayVersion(String paymentGatewayVersion) {
        this.paymentGatewayVersion = paymentGatewayVersion;
    }

    /**
     * @return the proRataChangePlanAmount
     */
    public Double getProRataChangePlanAmount() {
        return proRataChangePlanAmount;
    }

    /**
     * @param proRataChangePlanAmount the proRataChangePlanAmount to set
     */
    public void setProRataChangePlanAmount(Double proRataChangePlanAmount) {
        this.proRataChangePlanAmount = proRataChangePlanAmount;
    }

   

}
