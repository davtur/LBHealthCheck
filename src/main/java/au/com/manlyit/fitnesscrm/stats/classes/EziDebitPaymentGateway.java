/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomerStateFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade;
import au.com.manlyit.fitnesscrm.stats.beans.SessionTypesFacade;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.util.AsyncJob;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.classes.util.PushComponentUpdateBean;
import au.com.manlyit.fitnesscrm.stats.classes.util.ScheduledPaymentPojo;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Invoice;
import au.com.manlyit.fitnesscrm.stats.db.InvoiceLine;
import au.com.manlyit.fitnesscrm.stats.db.InvoiceLineType;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.Plan;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.EziResponseOfCustomerDetailsTHgMB7OL;
import au.com.manlyit.fitnesscrm.stats.webservices.INonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.NonPCIService;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import com.lowagie.text.BadElementException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
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
import net.sf.ezmorph.MorphUtils;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.primefaces.component.tabview.Tab;
import org.primefaces.context.RequestContext;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

/**
 *
 * @author david
 */
@Named("ezidebit")
@SessionScoped

public class EziDebitPaymentGateway implements Serializable {

    private static final Logger logger = Logger.getLogger(EziDebitPaymentGateway.class.getName());
    //private static final String digitalKey = "78F14D92-76F1-45B0-815B-C3F0F239F624";// test
    private static final String paymentGateway = "EZIDEBIT";
    private final static String CHANNEL = "/payments/";
    private int testAjaxCounter = 0;
    @Inject
    private FutureMapEJB futureMap;
    // private final Map<String, Future> futureMap = new HashMap<>();
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;
    @Inject
    private PaymentBean paymentBean;
    @Inject
    private PushComponentUpdateBean pushComponentUpdateBean;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private CustomerStateFacade customerStateFacade;
    @Inject
    private PaymentsFacade paymentsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
    @Inject
    private SessionHistoryFacade sessionHistoryFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceLineTypeFacade invoiceLineTypeFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceLineFacade invoiceLineFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.InvoiceFacade invoiceFacade;
    @Inject
    private SessionTypesFacade sessionTypesFacade;
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
    private List<ScheduledPaymentPojo> scheduledPaymentsList;
    private Payments selectedReportItem;
    private Payments selectedEomReportItem;
    private String reportName = "defaultreport";
    private Payments selectedScheduledPayment;
    private List<ScheduledPaymentPojo> scheduledPaymentsListFilteredItems;
    private CustomerDetails currentCustomerDetails;
    private Payment payment;
    private Date paymentDebitDate = new Date();
    private DatatableSelectionHelper pagination;
    private Date changeFromDate = new Date();
    private float paymentAmountInCents = (float) 0;
    private float oneOffPaymentAmount = (float) 0;
    private Date oneOffPaymentDate = new Date();
    private Long paymentLimitAmountInCents = new Long("0");

    private String paymentSchedulePeriodType = "W";
    private String paymentDayOfWeek = "MON";// required when Period Type is W, F, 4, N
    private int paymentDayOfMonth = 0; // required when Period Type is M
    private int paymentLimitToNumberOfPayments = 0;
    private Integer[] daysInMonth;
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
    private String cashPaymentReceiptReference = "";
    private Integer progress;
    private AtomicBoolean pageLoaded = new AtomicBoolean(false);
    private Date reportStartDate;
    private Date reportEndDate;

    private String bulkvalue = "";
    private String duplicateValues = "";
    private String listOfIdsToImport;
    private boolean customerExistsInPaymentGateway = false;
    private boolean editPaymentDetails = false;
    private boolean autoStartPoller = true;
    private boolean stopPoller = false;
    private int reportType = 0;
    private boolean customerDetailsHaveBeenRetrieved = false;
    private String eziDebitWidgetUrl = "";
    private String eziDebitEDDRFormUrl = "";
    private Customers selectedCustomer;
    private float reportTotalSuccessful = 0;
    private float reportTotalDishonoured = 0;
    private float reportTotalScheduled = 0;

    ThreadFactory tf1 = new eziDebitThreadFactory();
    private String sessionId;

    @PostConstruct
    private void setSessionId() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        this.sessionId = ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();
        GregorianCalendar cal = new GregorianCalendar();
        reportEndDate = cal.getTime();
        cal.add(Calendar.DAY_OF_YEAR, -7);
        reportStartDate = cal.getTime();
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
        } else {
            if (val.trim().compareToIgnoreCase("true") == 0) {
                setPaymentGatewayEnabled(true);
            } else {
                setPaymentGatewayEnabled(false);
            }
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

    private Customers getLoggedInUser() {
        String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Customers cust = customersFacade.findCustomerByUsername(user);

        if (cust == null) {
            logger.log(Level.SEVERE, "getLoggedInUser - the remote user couldn't be found in the database. This shouldn't happen. We may have been hacked!!");
        }
        return cust;
    }

    private void getCustDetailsFromEzi() {
        startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
    }

    protected void getPayments(int monthsAhead, int monthsbehind) {
        GregorianCalendar cal = new GregorianCalendar();

        cal.add(Calendar.MONTH, monthsAhead);
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -(monthsAhead));
        cal.add(Calendar.MONTH, -(monthsbehind));
        startAsynchJob("GetPayments", paymentBean.getPayments(selectedCustomer, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()));
        startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(selectedCustomer, cal.getTime(), endDate, getDigitalKey()));
    }

    public void editCustomerDetailsInEziDebit(Customers cust) {
        if (customerExistsInPaymentGateway) {
            startAsynchJob("EditCustomerDetails", paymentBean.editCustomerDetails(cust, null, getLoggedInUser(), getDigitalKey()));
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
    public List<ScheduledPaymentPojo> getScheduledPaymentsList() {
        if (scheduledPaymentsList == null) {
            scheduledPaymentsList = new ArrayList<>();
        }
        return scheduledPaymentsList;
    }

    /**
     * @param scheduledPaymentsList the scheduledPaymentsList to set
     */
    public void setScheduledPaymentsList(List<ScheduledPaymentPojo> scheduledPaymentsList) {
        this.scheduledPaymentsList = scheduledPaymentsList;
    }

    /**
     * @return the scheduledPaymentsListFilteredItems
     */
    public List<ScheduledPaymentPojo> getScheduledPaymentsListFilteredItems() {
        return scheduledPaymentsListFilteredItems;
    }

    /**
     * @param scheduledPaymentsListFilteredItems the
     * scheduledPaymentsListFilteredItems to set
     */
    public void setScheduledPaymentsListFilteredItems(List<ScheduledPaymentPojo> scheduledPaymentsListFilteredItems) {
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
        return customerDetailsHaveBeenRetrieved;
    }

    /**
     * @param customerDetailsHaveBeenRetrieved the
     * customerDetailsHaveBeenRetrieved to set
     */
    public void setCustomerDetailsHaveBeenRetrieved(boolean customerDetailsHaveBeenRetrieved) {
        this.customerDetailsHaveBeenRetrieved = customerDetailsHaveBeenRetrieved;
    }

    /**
     * @return the asyncOperationRunning
     */
    public boolean isAsyncOperationRunning() {
        if (pageLoaded.get() == false) {// if its null we havn't retrieved the details for the customer
            pageLoaded.set(true);
            //loginToPushServer();
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
            this.setSelectedCustomer(controller.getSelected());
            getPayments(18, 2);
            RequestContext.getCurrentInstance().update("iFrameForm");
        }
        return asyncOperationRunning;
    }

    /**
     * @param asyncOperationRunning the asyncOperationRunning to set
     */
    public void setAsyncOperationRunning(boolean asyncOperationRunning) {
        this.asyncOperationRunning = asyncOperationRunning;
    }

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
        return customerCancelledInPaymentGateway;
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
        cellStyle.setFillForegroundColor(HSSFColor.BLUE.index);
        cellStyle.setFillPattern(HSSFCellStyle.SOLID_FOREGROUND);
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
        style.setFillPattern(CellStyle.BIG_SPOTS);
        style2.setFillForegroundColor(IndexedColors.BLACK.getIndex());
        //style2.setFillPattern(CellStyle.SOLID_FOREGROUND);
        style2.setFillBackgroundColor(IndexedColors.LIGHT_TURQUOISE.getIndex());
        style2.setFillPattern(CellStyle.BIG_SPOTS);

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
            logger.log(Level.WARNING, "Logo URL error", iOException);
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
        Font font = new Font(Font.NORMAL);
        font.setSize(8);
    }

    public void postProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {

    }

    public void redirectToPaymentGateway() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
            controller.setSelected(customersFacade.find(getSelectedCustomer().getId()));
            PaymentParameters pp = controller.getSelectedCustomersPaymentParameters();
            if (pp.getWebddrUrl() != null) {

                ExternalContext ec = context.getExternalContext();
                ec.redirect(eziDebitEDDRFormUrl);
            }
        } catch (IOException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }

    }

    public void refreshAllActiveCustomers() {
        List<Customers> acl = customersFacade.findAllActiveCustomers(true);
        AsyncJob aj2;
        for (Customers c : acl) {
            try {
                startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(c, getDigitalKey()));

                Thread.sleep(300);//sleeping for a long time wont affect performance (the warning is there for a short sleep of say 5ms ) but we don't want to overload the payment gateway or they may get upset.
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
            startAsynchJob(key, paymentBean.getAllPaymentsBySystemSinceDate(reportStartDate, reportEndDate, reportUseSettlementDate, getDigitalKey()));
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Starting async Manual Refresh of All active customers:");
            Customers cust = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
            String auditDetails = "User:" + cust.getUsername() + " is running the :  " + key + " report.(" + cust.getFirstname() + " " + cust.getLastname() + "). Start Date:" + reportStartDate.toString() + ", to :" + reportEndDate.toString();
            String changedTo = "Not Running";
            String changedFrom = "Report Running:" + key;
            ejbAuditLogFacade.audit(cust, getSelectedCustomer(), key + " Report", auditDetails, changedFrom, changedTo);

            refreshAllActiveCustomers();
        }
        RequestContext.getCurrentInstance().update("reportsForm");
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

    private void generateEndOfMonthReport() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Running End Of Month Report");

        //get the list of active customers
        List<Customers> customerList = customersFacade.findAllActiveCustomers(true);
        List<Invoice> invoices = new ArrayList<>();

        for (Customers cust : customerList) {
            Invoice inv = new Invoice();
            inv.setInvoiceLineCollection(new ArrayList<InvoiceLine>());
            inv.setUserId(cust);
//add plan line item to invoice with number of billable sessions
            InvoiceLineType ilt = invoiceLineTypeFacade.findAll().get(1);
            InvoiceLine il = new InvoiceLine(0);
            BigDecimal paymentsTotal;
            BigDecimal bankFeesTotal;
            PaymentParameters pp = cust.getPaymentParameters();
            String ppPlanPaymentDetails = "Unknown";
            if (pp != null) {
                try {
                    String period = getPaymentPeriodString(pp.getPaymentPeriod());
                    String dom = ", DOM: " + pp.getPaymentPeriodDayOfMonth() + " ";
                    String dow = ", DOW: " + pp.getPaymentPeriodDayOfWeek();
                    if (pp.getPaymentPeriodDayOfMonth().contentEquals("0")) {
                        dom = "";
                    }

                    ppPlanPaymentDetails = "Period: " + period + dom + dow;
                } catch (Exception e) {
                    ppPlanPaymentDetails = "Unknown";
                }
            }
            BigDecimal productsAndServicesTotal = new BigDecimal(0);
            il.setTypeId(ilt);

            il.setQuantity(new BigDecimal(1));
            il.setDescription("Plan: " + cust.getGroupPricing().getPlanName());
            il.setPrice(cust.getGroupPricing().getPlanPrice());
            il.setAmount(cust.getGroupPricing().getPlanPrice());
            productsAndServicesTotal = productsAndServicesTotal.add(il.getAmount());
            inv.getInvoiceLineCollection().add(il);
            //get payments for customer
            List<Payments> pl = paymentsFacade.findPaymentsByDateRange(reportUseSettlementDate, reportShowSuccessful, reportShowFailed, reportShowPending, isReportShowScheduled(), reportStartDate, reportEndDate, false, cust);
            //get sessions for customer
            List<SessionTypes> sessionTypesList = sessionTypesFacade.findAll();
            for (SessionTypes sessType : sessionTypesList) {
                List<Date> weeks = getWeeksInMonth(reportStartDate, reportEndDate);
                Date weekStart = reportStartDate;
                int billableSessionsTotal = 0;
                for (Date weekEnd : weeks) {
                    List<SessionHistory> sessions = sessionHistoryFacade.findSessionsByParticipantAndDateRange(cust, weekStart, weekEnd, true);

                    //for each session type count the sessions and bill as a line item if they are not included in the plan
                    int count = 0;

                    for (SessionHistory sess : sessions) {
                        String type = sess.getSessionTypesId().getName();
                        if (type.contains(sessType.getName())) {
                            count++;
                            //total = total + sess.getSessionTypesId().
                        }
                    }
                    if (count > 0) {
                        billableSessionsTotal += checkSessionsAgainstPlanWeek(count, cust, sessType);
                    }
                    weekStart = weekEnd;
                }
                //add line item to invoice with number of billable sessions
                ilt = invoiceLineTypeFacade.findAll().get(2);
                il = new InvoiceLine(0);
                il.setTypeId(ilt);
                BigDecimal bdBillableSessionsTotal = new BigDecimal(billableSessionsTotal);
                Plan sessionPlan = sessType.getPlan();
                BigDecimal cdPrice = new BigDecimal(0);
                if (sessionPlan != null) {
                    cdPrice = sessionPlan.getPlanPrice();
                }
                il.setQuantity(bdBillableSessionsTotal);
                il.setDescription(sessType.getName());
                il.setPrice(cdPrice);
                il.setAmount(bdBillableSessionsTotal.multiply(cdPrice));
                if (cdPrice.compareTo(new BigDecimal(0)) > 0 && bdBillableSessionsTotal.compareTo(new BigDecimal(0)) > 0) {
                    inv.getInvoiceLineCollection().add(il);
                    productsAndServicesTotal = productsAndServicesTotal.add(il.getAmount());
                }

            }
            //add payments line item to invoice with number of billable sessions
            ilt = invoiceLineTypeFacade.findAll().get(0);
            il = new InvoiceLine(0);
            il.setTypeId(ilt);
            paymentsTotal = new BigDecimal(0);
            bankFeesTotal = new BigDecimal(0);
            int numberOfPayments = 0;
            for (Payments p : pl) {
                BigDecimal fee = p.getTransactionFeeCustomer();
                if (fee == null) {
                    fee = new BigDecimal(0);
                }
                paymentsTotal = paymentsTotal.add(p.getPaymentAmount());
                bankFeesTotal = bankFeesTotal.add(fee);
                numberOfPayments++;
            }
            // add scheduled payment amounts 
            il.setQuantity(new BigDecimal(numberOfPayments));
            il.setDescription("Payment(s)" + " --- " + ppPlanPaymentDetails);
            productsAndServicesTotal = productsAndServicesTotal.add(bankFeesTotal);
            productsAndServicesTotal = productsAndServicesTotal.subtract(paymentsTotal);
            il.setPrice(paymentsTotal);
            paymentsTotal = paymentsTotal.negate();
            il.setAmount(paymentsTotal);

            // add line item for bank fees
            InvoiceLine il2 = new InvoiceLine(0);
            il2.setTypeId(ilt);
            il2.setQuantity(new BigDecimal(numberOfPayments));
            il2.setDescription("Bank Transaction Fees");
            il2.setPrice(bankFeesTotal);
            il2.setAmount(bankFeesTotal);
            inv.getInvoiceLineCollection().add(il2);
            inv.getInvoiceLineCollection().add(il);
            inv.setTotal(productsAndServicesTotal);
            invoices.add(inv);
        }
        if (invoices.isEmpty() == false) {
            reportEndOfMonthList = new PfSelectableDataModel<>(invoices);
        } else {
            reportEndOfMonthList = new PfSelectableDataModel<>(new ArrayList<Invoice>());
        }

        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Completed End Of Month Report");
    }

    private String getPaymentPeriodString(String key) {
        String period = "";
        switch (key) {
            case "4":
                period = "4 Weekly";
                break;
            case "W":
                period = "Weekly";
                break;
            case "M":
                period = "Monthly";
                break;
            case "F":
                period = "Fortnightly";
                break;
            case "Z":
                period = "No Schedule";
                break;
            case "Q":
                period = "Quarterly";
                break;
            case "H":
                period = "Half Yearly";
                break;
            case "Y":
                period = "Annually";
                break;
            case "N":
                period = "Weekday In Month";
                break;
        }
        return period;
    }

    private List<Date> getWeeksInMonth(Date start, Date end) {
        List<Date> dl = new ArrayList<>();
        GregorianCalendar gc = new GregorianCalendar();
        gc.setTime(start);
        while (gc.getTime().compareTo(end) <= 0) {
            gc.add(Calendar.DAY_OF_WEEK, 7);
            if (gc.getTime().compareTo(end) >= 0) {
                dl.add(end);
            } else {
                dl.add(gc.getTime());
            }

        }

        return dl;
    }

    private int checkSessionsAgainstPlanWeek(int count, Customers cust, SessionTypes sessType) {
        int billableSessions = 0;
        Plan plan = cust.getGroupPricing();
        PaymentParameters pp = cust.getPaymentParameters();
        String paymentPeriod = pp.getPaymentPeriod();
        List<Plan> plans = new ArrayList<>(plan.getPlanCollection());
        int includedInPlanCount = 0;
        for (Plan p : plans) {
            SessionTypes st = p.getSessionType();
            if (st != null) {
                if (sessType.getName().contentEquals(st.getName())) {
                    includedInPlanCount++;
                }
            }
        }
        billableSessions = count - includedInPlanCount;
        if (billableSessions < 0) {
            billableSessions = 0;
        }

        /* if (paymentPeriod.contentEquals(PaymentPeriod.MONTHLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.FOUR_WEEKLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.WEEKLY.value())) {

         }
         if (paymentPeriod.contentEquals(PaymentPeriod.FORTNIGHTLY.value())) {

         }*/
        logger.log(Level.INFO, "checkSessionsAgainstPlanWeek: Billed Sessions:{0}, Session Count:{1}, Customer: {2}, Plan: {5}, Session Type: {3}, Session Plan: {4}", new Object[]{billableSessions, count, cust.getUsername(), sessType.getName(), sessType.getPlan(), cust.getGroupPricing().getPlanName()});
        return billableSessions;
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
                        reportTotalSuccessful = reportTotalSuccessful + p.getPaymentAmount().floatValue();
                    } else if (p.getPaymentStatus().contains(PaymentStatus.DISHONOURED.value()) || p.getPaymentStatus().contains(PaymentStatus.FATAL_DISHONOUR.value())) {
                        reportTotalDishonoured = reportTotalDishonoured + p.getPaymentAmount().floatValue();
                    } else if (p.getPaymentStatus().contains(PaymentStatus.SCHEDULED.value())) {
                        reportTotalScheduled = reportTotalScheduled + p.getPaymentAmount().floatValue();
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
        RequestContext.getCurrentInstance().update("reportsForm");

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
    /**
     * @return the customerExistsInPaymentGateway
     */
    public boolean isCustomerExistsInPaymentGateway() {
        return customerExistsInPaymentGateway;
    }

    public boolean isCustomerWebDDRFormEnabled() {

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        PaymentParameters pp = controller.getSelectedCustomersPaymentParameters();
        String webDdrUrl = pp.getWebddrUrl();// contains payment information e.g 
        return webDdrUrl != null;

    }

    public boolean isShowAddToPaymentGatewayButton() {
        if (customerDetailsHaveBeenRetrieved) {
            if (customerCancelledInPaymentGateway || customerExistsInPaymentGateway == false) {
                if (selectedCustomer.getActive().getCustomerState().contains("ACTIVE")) {

                    return true;
                }
            }
        }

        return false;
    }

    public PfSelectableDataModel<Payments> getPaymentDBList() {
        if (paymentDBList == null) {
            paymentDBList = new PfSelectableDataModel<>(paymentsFacade.findPaymentsByCustomer(selectedCustomer, false));
        }
        if (paymentDBList == null) {
            paymentDBList = new PfSelectableDataModel<>(new ArrayList<Payments>());
        }
        return paymentDBList;
    }

    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        /*if (refreshFromDB) {
         refreshFromDB = false;
         controller.updateSelectedCustomer(customersFacade.findByIdBypassCache(controller.getSelected().getId()));
         }*/
        return controller.getSelected();

    }

    /**
     * @param customerExistsInPaymentGateway the customerExistsInPaymentGateway
     * to set
     */
    public void setCustomerExistsInPaymentGateway(boolean customerExistsInPaymentGateway) {
        this.customerExistsInPaymentGateway = customerExistsInPaymentGateway;
    }

    /*  public void checkCustomerExistsInPaymentGateway(ActionEvent actionEvent) {
     CustomerDetails cd = getCustomerDetails(getSelectedCustomer());
     customerExistsInPaymentGateway = cd != null;
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
        String webDdrUrl = pp.getWebddrUrl();// contains payment information e.g 

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
        //RequestContext.getCurrentInstance().update(compId);

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
            logger.log(Level.WARNING, "Customer is null: addDefaultPaymentParametersIfEmpty(Customers cust)");
            return;
        }
        PaymentParameters pay = cust.getPaymentParameters();

        if (cust.getTelephone() == null) {
            cust.setTelephone("0400000000");
        }

        if (pay == null) {
            pay = new PaymentParameters(0, contractStartDate, cust.getTelephone(), "NO", "NO", "NO", paymentGateway);
            pay.setLoggedInUser(cust);
            cust.setPaymentParameters(pay);
            logger.log(Level.INFO, "Adding default payment gateway parameters for this customer as they don't have any.");
            customersFacade.editAndFlush(cust);
        }

    }

    public void createCustomerRecord() {
        String authenticatedUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        startAsynchJob("AddCustomer", paymentBean.addCustomer(getSelectedCustomer(), paymentGateway, getDigitalKey(), authenticatedUser));
        JsfUtil.addSuccessMessage("Processing Add Customer to Payment Gateway Request.", "");

    }

    public void testAjax(ActionEvent event) {
        testAjaxCounter++;
        RequestContext context = RequestContext.getCurrentInstance();
        context.update("tabView");

        JsfUtil.addSuccessMessage("Testing Ajax");

    }

    private void processAddCustomer(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            logger.log(Level.WARNING, "processAddCustomer", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Customer Added to Payment Gateway Successfully.", "Customer Added to Payment Gateway Successfully.");
            customerExistsInPaymentGateway = true;
            startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
            getPayments(18, 2);

        } else {
            JsfUtil.addErrorMessage("Couldn't add Customer To Payment Gateway. Check logs");
        }
    }

    /*   private boolean addBulkCustomersToPaymentGateway() {
     boolean result = false;
     logger.log(Level.INFO, "Starting tests!");
     List<Customers> custList = customersFacade.findAllActiveCustomers(result);
     String user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
     for (Customers c : custList) {
     PaymentParameters pp = new PaymentParameters(0, new Date(), c.getTelephone(), "NO", "NO", "NO", user);
     c.getPaymentParametersCollection().add(pp);
     customersFacade.edit(c);
     boolean success = addCustomer(c, paymentGateway);
     if (!success) {
     logger.log(Level.WARNING, "Add customer failed! {0}", c.getUsername());
     }
     }
     return result;
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
        progress = progress + 1;
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
        logger.log(Level.INFO, "Checking if push channel is open. sessionID {0}", sessionId);
        loginToPushServer();
    }

    public void loginToPushServerAction(ActionEvent event) {
        loginToPushServer();
    }

    public void loginToPushServer() {

        // Connect to the push channel based on sessionId
        RequestContext requestContext = RequestContext.getCurrentInstance();

        requestContext.execute("PF('subscriber').connect('/" + sessionId + "')");
        logger.log(Level.INFO, "Adding connect request to primefaces requestcontext. PF(\'subscriber\').connect(\'/{0}\')", sessionId);
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

    public void pollerListener() {
        logger.log(Level.INFO, "Poller called backing bean listener method.");

        int k = futureMap.size(sessionId);
        if (k > 0) {
            logger.log(Level.INFO, "{0} jobs are running. Checking to see if asych jobs have finished so their results can be processed.", k);
            if (isAsyncOperationRunning() == false) {
                logger.log(Level.WARNING, "{0} jobs are running but asychOperationRunning flag is false!!", k);
                setAsyncOperationRunning(true);
            }

            int y = 0;
            String details = "";
            AsyncJob aj;
            ArrayList<AsyncJob> fmap = futureMap.getFutureMap(sessionId);
            try {

                for (int x = fmap.size() - 1; x >= 0; x--) {
                    aj = fmap.get(x);

                    Future ft = aj.getFuture();
                    String key = aj.getJobName();
                    if (ft.isDone()) {
                        y++;
                        logger.log(Level.INFO, "SessionId {0} Future Map async job {1} has finished.", new Object[]{key, sessionId});
                        details += key + " ";
                        checkIfAsyncJobsHaveFinishedAndUpdate(key, ft);

                        fmap.remove(x);

                    }

                }
            } catch (Exception e) {
                logger.log(Level.SEVERE, "checkRunningJobsAndNotifyIfComplete,  {0} async jobs for sessionId {1} have finished.Exception {2}", new Object[]{Integer.toString(y), sessionId, e});
            }

            // checkIfAsyncJobsHaveFinishedAndUpdate();
        }
        k = futureMap.size(sessionId);
        if (k == 0) {
            setAsyncOperationRunning(false);
            logger.log(Level.INFO, "Asking request context to update components.");

            //ArrayList<String> componentsToUpdate = new ArrayList<>();
            //componentsToUpdate.add(":tv:paymentsForm:progressBarPanel");
            //componentsToUpdate.add("paymentsForm:mainPanel");
            //componentsToUpdate.add("tv:paymentsForm:iFrameHeaderPanel");
            //componentsToUpdate.add("paymentsTablePanel");
            //componentsToUpdate.add("scheduledPaymentsTablePanel");
            //componentsToUpdate.add("tv:paymentsForm");
            // requestContext.execute("PF('paymentPoller').stop();");
            //pushComponentUpdateBean.sendMessage("Notification", "Payment Gateway Request Completed");
            logger.log(Level.INFO, "All asych jobs have finished.");
        }

        if (isRefreshIFrames() == true) {
            RequestContext.getCurrentInstance().update("iFrameForm");
            logger.log(Level.INFO, "Asking Request Context to update IFrame forms.");
            setRefreshIFrames(false);
        }
        RequestContext.getCurrentInstance().update("paymentsForm");
        //  refreshFromDB = true;
    }

    public void checkIfAsyncJobsHaveFinishedAndUpdate(String key, Future ft) {

        try {
            if (key.contains("GetCustomerDetails")) {
                processGetCustomerDetails(ft);
                RequestContext.getCurrentInstance().update("customerslistForm1");
            }
            if (key.contains("AddPayment")) {
                processAddPaymentResult(ft);
            }
            if (key.contains("GetPayments")) {
                processGetPayments(ft);
            }
            if (key.contains("GetScheduledPayments")) {
                processGetScheduledPayments(ft);
            }
            if (key.contains("CreateSchedule")) {
                processCreateSchedule(ft);
            }
            if (key.contains("AddCustomer")) {
                processAddCustomer(ft);
            }
            if (key.contains("EditCustomerDetails")) {
                processEditCustomerDetails(ft);
            }
            if (key.contains("ClearSchedule")) {
                processClearSchedule(ft);
            }
            if (key.contains("DeletePayment")) {
                processDeletePayment(ft);
            }
            if (key.contains("ChangeCustomerStatus")) {
                processChangeCustomerStatus(ft);
            }
            if (key.contains("GetPaymentStatus")) {
                processGetPaymentStatus(ft);
            }
            if (key.contains("ChangeScheduledAmount")) {
                processChangeScheduledAmount(ft);
            }
            if (key.contains("ChangeScheduledDate")) {
                processChangeScheduledDate(ft);
            }
            if (key.contains("IsBsbValid")) {
                processIsBsbValid(ft);
            }
            if (key.contains("IsSystemLocked")) {
                processIsSystemLocked(ft);
            }
            if (key.contains("GetPaymentExchangeVersion")) {
                processGetPaymentExchangeVersion(ft);
            }
            if (key.contains("GetCustomerDetailsFromEziDebitId")) {
                processGetCustomerDetailsFromEziDebitId(ft);
            }
            if (key.contains("GetPaymentDetail")) {
                processGetPaymentDetail(ft);
            }
            if (key.contains("GetPaymentDetailPlusNextPaymentInfo")) {
                processGetPaymentDetailPlusNextPaymentInfo(ft);
            }
            if (key.contains("PaymentReport")) {
                processPaymentReport(ft);
            }
            if (key.contains("SettlementReport")) {
                processSettlementReport(ft);
            }

        } catch (CancellationException ex) {
            logger.log(Level.WARNING, key + ":", ex);

        }
    }

    private void processPaymentReport(Future ft) {
        ArrayOfPayment result = null;
        try {
            result = (ArrayOfPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null) {
            List<Payment> payList = result.getPayment();
            if (payList != null) {
                setPaymentsList(payList);
                setPaymentsListFilteredItems(null);
                setPaymentDBList(null);
                setPaymentsDBListFilteredItems(null);
            }
        }

        //refreshFromDB = true;
        //getCustomersController().setRefreshFromDB(true);
        getCustomersController().recreateModel();

        logger.log(Level.INFO, "processPaymentReport completed");
    }

    private void processSettlementReport(Future ft) {
        ArrayOfPayment result = null;
        try {
            result = (ArrayOfPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null) {
            List<Payment> payList = result.getPayment();
            if (payList != null) {
                setPaymentsList(payList);
                setPaymentsListFilteredItems(null);
                setPaymentDBList(null);
                setPaymentsDBListFilteredItems(null);
            }
        }

        // refreshFromDB = true;
        // getCustomersController().setRefreshFromDB(true);
        getCustomersController().recreateModel();

        logger.log(Level.INFO, "processSettlementReport completed");
    }

    private void processGetPayments(Future ft) {
        ArrayOfPayment result = null;
        String cust = getSelectedCustomer().getUsername();
        logger.log(Level.INFO, "Ezidebit Controller -  processing GetPayments Response Recieved from ezidebit for Customer  - {0}", cust);

        try {
            result = (ArrayOfPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null) {
            List<Payment> payList = result.getPayment();
            if (payList != null) {
                setPaymentsList(payList);
                setPaymentsListFilteredItems(null);
                setPaymentDBList(null);
                setPaymentsDBListFilteredItems(null);
            }
        }
        RequestContext.getCurrentInstance().update("customerslistForm1");
        // refreshFromDB = true;
        logger.log(Level.INFO, "processGetPayments completed");
    }

    private void processGetScheduledPayments(Future ft) {
        ArrayOfScheduledPayment result = null;
        String cust = getSelectedCustomer().getUsername();
        logger.log(Level.INFO, "Ezidebit Controller -  processing GetScheduledPayments Response Recieved from ezidebit for Customer  - {0}", cust);
        try {
            result = (ArrayOfScheduledPayment) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result != null) {
            List<ScheduledPayment> payList = result.getScheduledPayment();
            if (payList != null) {
                // 
                List<ScheduledPaymentPojo> pojoList = new ArrayList<>();
                int id = 1;
                for (ScheduledPayment sp : payList) {
                    pojoList.add(new ScheduledPaymentPojo(id, sp.getEzidebitCustomerID().getValue(), sp.isManuallyAddedPayment(), sp.getPaymentAmount(), sp.getPaymentDate().toGregorianCalendar().getTime(), sp.getPaymentReference().getValue(), sp.getYourGeneralReference().getValue(), sp.getYourSystemReference().getValue()));
                    id++;
                }
                setScheduledPaymentsList(pojoList);
                setScheduledPaymentsListFilteredItems(null);
                setPaymentDBList(null);
                setPaymentsDBListFilteredItems(null);
            }
        }
        RequestContext.getCurrentInstance().update("customerslistForm1");
        //refreshFromDB = true;
        logger.log(Level.INFO, "processGetScheduledPayments completed");
    }

    private void updatePaymentLists(Payments pay) {
        if (paymentDBList != null) {
            List<Payments> lp = (List<Payments>) paymentDBList.getWrappedData();
            int index = lp.indexOf(pay);
            lp.set(index, pay);
        }
        if (paymentsDBListFilteredItems != null) {
            int index = paymentsDBListFilteredItems.indexOf(pay);
            paymentsDBListFilteredItems.set(index, pay);
        }
    }

    private void removeFromPaymentLists(Payments pay) {
        if (paymentDBList != null) {
            List<Payments> lp = (List<Payments>) paymentDBList.getWrappedData();
            int index = lp.indexOf(pay);
            lp.remove(index);
        }
        if (paymentsDBListFilteredItems != null) {
            int index = paymentsDBListFilteredItems.indexOf(pay);
            paymentsDBListFilteredItems.remove(index);
        }
    }

    private void processAddPaymentResult(Future ft) {
        String result = "";
        try {
            result = (String) ft.get();
        } catch (InterruptedException | ExecutionException | EJBException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "processAddPaymentResult FAILED - Async Task Exception ", ex);
        }
        if (result == null) {
            logger.log(Level.WARNING, "processAddPaymentResult FAILED - RESULT IS NULL ");
            return;
        }
        if (result.startsWith("ERROR:")) {

            String errorMessage = result.substring(6);
            int k = errorMessage.indexOf(':');
            String paymentRef = errorMessage.substring(0, k);
            errorMessage = errorMessage.substring(k + 1);
            int id = 0;
            try {
                id = Integer.parseInt(paymentRef);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.INFO, "processAddPaymentResult FAILED - PaymentReference could not be converted to a number. It should be the primary key of teh payments table row ", result);
            }
            Payments pay = paymentsFacade.findPaymentById(id);
            if (pay != null) {
                if (errorMessage.contains("Your update could not be processed at this time")) {
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
                            retryAddPayment(pay);
                            logger.log(Level.INFO, "processAddPaymentResult PAYMENT GATEWAY BUSY - ATTEMPTING RETRY - ", result);
                        } else {
                            pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                            paymentsFacade.edit(pay);
                            updatePaymentLists(pay);
                        }
                    }
                } else if (errorMessage.contains("This customer already has two payments on this date.")) {
                    JsfUtil.addErrorMessage("Add Payment", "Payment ID:" + pay.getId().toString() + " for Amount:$" + pay.getPaymentAmount().toPlainString() + " on Date:" + pay.getDebitDate().toString() + " could not be added as teh customer already has two existing payments on this date!!.");
                    paymentsFacade.remove(pay);
                    updatePaymentLists(pay);
                } else {
                    pay.setPaymentStatus(PaymentStatus.MISSING_IN_PGW.value());
                    pay.setPaymentReference(Integer.toString(id));
                    paymentsFacade.edit(pay);
                    updatePaymentLists(pay);
                }
            } else {
                logger.log(Level.INFO, "processAddPaymentResult FAILED - ERROR processing could not find payment id ", result);
            }
        } else if (result.isEmpty() == false) {
            JsfUtil.addSuccessMessage("Add Payment", "Payment (" + result + ") submitted successfully.");
            int id = 0;
            try {
                id = Integer.parseInt(result);
            } catch (NumberFormatException numberFormatException) {
            }
            Payments pay = paymentsFacade.findPaymentById(id);
            if (pay != null) {
                pay.setPaymentStatus(PaymentStatus.SCHEDULED.value());
                pay.setPaymentReference(Integer.toString(id));
                paymentsFacade.edit(pay);
                updatePaymentLists(pay);
                // if (pay.getManuallyAddedPayment()) {
                //     getPayments(18, 2);
                // }
            } else {
                logger.log(Level.INFO, "processAddPaymentResult FAILED - could not find payment id ", result);
            }
        } else {
            JsfUtil.addErrorMessage("Add Payment", "Payment Failed! Is the customer active with valid account/card details?");
        }

        logger.log(Level.INFO, "processAddPaymentResult completed");
    }

    private void processCreateSchedule(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment", "Successfully Created Schedule.");
            getPayments(18, 2);

        } else {
            JsfUtil.addErrorMessage("Payment", "The Create Schedule operation failed!.");
        }
        logger.log(Level.INFO, "processCreateSchedule completed");
    }

    private void processEditCustomerDetails(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Edited Customer Details  .");
            getCustDetailsFromEzi();
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processEditCustomerDetails completed");
    }

    private void processClearSchedule(Future ft) {
        String result = "0,FAILED";
        try {
            result = (String) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result.contains("OK")) {
            String[] res = result.split(",");
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
        logger.log(Level.INFO, "processClearSchedule completed");
    }

    private void processDeletePayment(Future ft) {
        String result = "0,FAILED";
        String message = "The delete payment operation failed!.";
        try {
            result = (String) ft.get();
        } catch (InterruptedException | ExecutionException | EJBException ex) {
            String causedBy = ex.getCause().getCause().getMessage();
            if (causedBy.contains("Payment selected for deletion could not be found")) {
                logger.log(Level.WARNING, "deletePayment - Payment selected for deletion could not be found..");
                message = "The payment selected for deletion could not be found!";
            } else {
                Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
            }
        }
        if (result.startsWith("ERROR:") == false) {

            int reference = -1;
            try {
                reference = Integer.parseInt(result);

            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.WARNING, "Process deletePayment - Thepayment reference could not be converted to a number: {0}", new Object[]{result});
            }
            Payments pay = paymentsFacade.findPaymentById(reference);
            if (pay != null) {
                removeFromPaymentLists(pay);
                paymentsFacade.remove(pay);

            } else {
                logger.log(Level.WARNING, "Process deletePayment - Payment that was deleted could not be found in the our DB key={0}", new Object[]{reference});
            }
            setSelectedScheduledPayment(null);
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Deleted Payment  .");
            //getPayments(18, 2);

        } else {

            logger.log(Level.WARNING, "Process deletePayment - DELETE PAYMENT FAILED: {0}", new Object[]{result});
            String errorMessage = result.substring(6);
            int k = errorMessage.indexOf(':');
            String paymentRef = errorMessage.substring(0, k);
            errorMessage = errorMessage.substring(k + 1);
            int id = 0;
            try {
                id = Integer.parseInt(paymentRef);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.WARNING, "Process deletePayment  FAILED - PaymentReference could not be converted to a number. It should be the primary key of the payments table row ", result);
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
                    logger.log(Level.WARNING, "Process deletePayment  FAILED - Unhandled error ", result);
                    JsfUtil.addSuccessMessage("Payment Gateway", "Deleted Payment Error - see logs for more details .");
                }

            }
        }
        logger.log(Level.INFO, "processDeletePayment completed");
    }

    private void processChangeCustomerStatus(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Changed Customer Status  .");
            startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
            RequestContext.getCurrentInstance().update("customerslistForm1");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The change status operation failed!.");
        }
        logger.log(Level.INFO, "processChangeCustomerStatus completed");
    }

    private void processGetPaymentStatus(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Retrieved Payment Status  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The getPayment status operation failed!.");
        }
        logger.log(Level.INFO, "processGetPaymentStatus completed");
    }

    private void processChangeScheduledAmount(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully  Changed Scheduled Amount .");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processChangeScheduledAmount completed");
    }

    private void processChangeScheduledDate(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Changed Scheduled Date  .");
            getPayments(18, 2);
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processChangeScheduledDate completed");
    }

    private void processIsBsbValid(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Checked BSB  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processIsBsbValid completed");
    }

    private void processIsSystemLocked(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully checked if System is Locked  .");

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processIsSystemLocked completed");
    }

    private void processGetPaymentExchangeVersion(Future ft) {
        String result = "";
        try {
            result = (String) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result != null && result.trim().isEmpty() == false) {
            JsfUtil.addSuccessMessage("Payment Gateway", "Payment Exchange Version: " + result);

        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processGetPaymentExchangeVersion completed");
    }

    private void processGetCustomerDetails(Future ft) {
        CustomerDetails result = null;
        String cust = getSelectedCustomer().getUsername();
        setCustomerDetailsHaveBeenRetrieved(true);
        logger.log(Level.INFO, "Ezidebit Controller -  processing GetCustomerDetails Response Recieved from ezidebit for Customer  - {0}", cust);

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
            String eziStatusCode = "Unknown";
            if (result.getStatusDescription() != null) {
                eziStatusCode = result.getStatusDescription().getValue().toUpperCase().trim();
            }
            String ourStatus = getSelectedCustomer().getActive().getCustomerState().toUpperCase().trim();
            String message = "";
            if (ourStatus.contains(eziStatusCode) == false) {
                // status codes don't match

                message = "Customer Status codes dont match. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                if (eziStatusCode.contains("WAITING BANK DETAILS")) {
                    message = "The Customer does not have any banking details. Customer: " + cust + ", ezidebit status:" + eziStatusCode + ", Crm Status:" + ourStatus + "";
                    logger.log(Level.INFO, message);
                    setWaitingForPaymentDetails(true);
                    JsfUtil.addSuccessMessage("Important!", "You must enter customer banking details before payments can be set up.");
                } else {
                    logger.log(Level.WARNING, message);
                    setWaitingForPaymentDetails(false);
                    JsfUtil.addErrorMessage(message, "");
                }
                if (eziStatusCode.trim().toUpperCase().contains("CANCELLED")) {
                    message = "The Customer is in a Cancelled status in the payment gateway";
                    logger.log(Level.INFO, message);
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
            customerExistsInPaymentGateway = false;
        }
        RequestContext.getCurrentInstance().update("customerslistForm1");
        //refreshFromDB = true;
        getCustomersController().setRefreshFromDB(true);
        getCustomersController().recreateModel();
        logger.log(Level.INFO, "processGetCustomerDetails completed");
    }

    private CustomersController getCustomersController() {
        FacesContext context = FacesContext.getCurrentInstance();
        return (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");

    }

    public void refreshPaymentsPage(ActionEvent actionEvent) {
        loginToPushServer();
        setSelectedCustomer(selectedCustomer);

    }

    private void processGetCustomerDetailsFromEziDebitId(Future ft) {
        CustomerDetails result = null;
        try {
            result = (CustomerDetails) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with result
        }
        logger.log(Level.INFO, "processGetCustomerDetailsFromEziDebitId completed");
    }

    private void processGetPaymentDetail(Future ft) {
        PaymentDetail result = null;
        try {
            result = (PaymentDetail) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with result
        }
        logger.log(Level.INFO, "processGetPaymentDetail completed");
    }

    private void processGetPaymentDetailPlusNextPaymentInfo(Future ft) {
        PaymentDetailPlusNextPaymentInfo result = null;
        try {
            result = (PaymentDetailPlusNextPaymentInfo) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "GetCustomerDetailsFromEziDebitId", ex);
        }
        if (result != null) {
            // do something with result
        }
        logger.log(Level.INFO, "processGetPaymentDetailPlusNextPaymentInfo completed");
    }

    private void stopPoller() {
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Stopping poller on customers page");
        RequestContext.getCurrentInstance().addCallbackParam("stopPolling", true);
    }

    public void recreateModels() {
        // clear all arrays and reload from DB
        setPaymentsList(null);
        setPaymentsListFilteredItems(null);

        setPaymentDBList(null);
        setPaymentsDBListFilteredItems(null);

        setScheduledPaymentsList(null);
        setScheduledPaymentsListFilteredItems(null);

        setCustomerDetailsHaveBeenRetrieved(false);
        setCurrentCustomerDetails(null);

    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    public void setSelectedCustomer(Customers selectedCustomer) {
        this.selectedCustomer = selectedCustomer;
        this.currentCustomerDetails = null;
        refreshIFrames = true;
        futureMap.cancelFutures(sessionId);
        getCustDetailsFromEzi();

        getPayments(18, 2);
        this.progress = 0;

        /*CustomerDetails cd = getCustomerDetails(selectedCustomer);
         if (cd == null) {
         customerExistsInPaymentGateway = false;
         } else {
         customerExistsInPaymentGateway = true;
         }*/
        /*int pp = selectedCustomer.getPaymentParametersCollection().size();
         if( pp > 0) {
         customerExistsInPaymentGateway = false;
         } else {
         customerExistsInPaymentGateway = true;
         }*/
    }

    public void closeEditPaymentMethodDialogue(ActionEvent actionEvent) {
        setEditPaymentMethodEnabled(false);
        setSelectedCustomer(selectedCustomer);
    }

    public void editPaymentMethodDialogue(ActionEvent actionEvent) {
        setEditPaymentMethodEnabled(true);
    }

    private void startAsynchJob(String key, Future future) {
        setAsyncOperationRunning(true);
        AsyncJob aj = new AsyncJob(key, future);
        futureMap.put(sessionId, aj);

    }

    public void createEddrLink(ActionEvent actionEvent) {
        Customers cust = getSelectedCustomer();
        if (cust == null || cust.getId() == null) {
            logger.log(Level.WARNING, "Create EDDR Link cannot be completed as the selected customer is null.");
            return;
        }

        PaymentParameters pp = null;
        Long amount = (long) (paymentAmountInCents * (float) 100);
        Long amountLimit = paymentLimitAmountInCents * (long) 100;
        char spt = paymentSchedulePeriodType.charAt(0);
        GregorianCalendar endCal = new GregorianCalendar();
        //endCal.setTime(paymentDebitDate);// ezi debit only supports 1 year from current date
        endCal.add(Calendar.YEAR, 1);
        String dom = Integer.toString(paymentDayOfMonth);

        pp = cust.getPaymentParameters();
        pp.setContractStartDate(paymentDebitDate);
        pp.setPaymentPeriod(paymentSchedulePeriodType);
        pp.setPaymentPeriodDayOfMonth(dom);
        pp.setPaymentPeriodDayOfWeek(paymentDayOfWeek);
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

        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMaximumFractionDigits(2);
        nf.setMinimumFractionDigits(2);
        String amp = "&";
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");

        String widgetUrl = configMapFacade.getConfig("payment.ezidebit.webddr.baseurl");
        widgetUrl += "?" + "a=" + configMapFacade.getConfig("payment.ezidebit.webddr.hash");
        widgetUrl += amp + "uRefLabel=" + configMapFacade.getConfig("payment.ezidebit.webddr.uRefLabel");
        widgetUrl += amp + "fName=" + cust.getFirstname();
        widgetUrl += amp + "lName=" + cust.getLastname();
        widgetUrl += amp + "uRef=" + cust.getId();
        widgetUrl += amp + "email=" + cust.getEmailAddress();
        widgetUrl += amp + "mobile=" + cust.getTelephone();
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

        widgetUrl += amp + "aFreq=" + paymentSchedulePeriodType;
        widgetUrl += amp + "freq=" + paymentSchedulePeriodType;
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
        ejbPaymentParametersFacade.edit(pp);
        customersFacade.edit(cust);
        logger.log(Level.INFO, "eddr request url:{0}.", widgetUrl);
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
        //endCal.setTime(paymentDebitDate);// ezi debit only supports 1 year from current date
        endCal.add(Calendar.YEAR, 1);
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
            Customers c = selectedCustomer;
            PaymentParameters pp = c.getPaymentParameters();
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
            paymentBean.createCRMPaymentSchedule(selectedCustomer, paymentDebitDate, endCal.getTime(), spt, dow, paymentDayOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean);
            /* List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(selectedCustomer, PaymentStatus.SCHEDULED.value());
             for (Payments p : crmPaymentList) {
             if (!(paymentKeepManualPayments && p.getManuallyAddedPayment())) {
             paymentsFacade.remove(p);
             }
             }
             startAsynchJob("CreateSchedule", paymentBean.createSchedule(selectedCustomer, paymentDebitDate, spt, paymentDayOfWeek, paymentDayOfMonth, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
             */
            JsfUtil.addSuccessMessage("Sending CreateSchedule Request to Payment Gateway.");
            startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(selectedCustomer, paymentDebitDate, endCal.getTime(), getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
        getPayments(18, 2);
        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    private void retryAddPayment(Payments pay) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        logger.log(Level.INFO, "Retry Payment Created for customer {0} with paymentID: {1}", new Object[]{pay.getCustomerName().getUsername(), pay.getId()});
        setAsyncOperationRunning(paymentBean.retryAddNewPayment(pay, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean));

        paymentDBList = null;
        paymentsDBListFilteredItems = null;

    }

    private void retryDeletePayment(Payments pay) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        logger.log(Level.INFO, "Retry Payment Created for customer {0} with paymentID: {1}", new Object[]{pay.getCustomerName().getUsername(), pay.getId()});
        setAsyncOperationRunning(paymentBean.retryDeletePayment(pay, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean));

        paymentDBList = null;
        paymentsDBListFilteredItems = null;

    }

    public void addSinglePayment(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmountInCents * (float) 100);
        setAsyncOperationRunning(true);
        paymentBean.addNewPayment(selectedCustomer, paymentDebitDate, amount, true, loggedInUser, sessionId, getDigitalKey(), futureMap, paymentBean);
        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    public void logSingleCashPayment(ActionEvent actionEvent) {
        try {
            Payments newPayment = new Payments(0);
            newPayment.setDebitDate(paymentDebitDate);
            newPayment.setSettlementDate(paymentDebitDate);
            newPayment.setCreateDatetime(new Date());
            newPayment.setLastUpdatedDatetime(new Date());
            newPayment.setYourSystemReference(selectedCustomer.getId().toString());
            newPayment.setPaymentAmount(new BigDecimal(paymentAmountInCents));
            newPayment.setCustomerName(selectedCustomer);
            newPayment.setPaymentStatus(PaymentStatus.SUCESSFUL.value());
            newPayment.setManuallyAddedPayment(true);
            newPayment.setBankReturnCode("");
            newPayment.setBankFailedReason("");
            newPayment.setBankReceiptID(cashPaymentReceiptReference);
            newPayment.setTransactionFeeClient(BigDecimal.ZERO);
            newPayment.setTransactionFeeCustomer(BigDecimal.ZERO);
            paymentsFacade.createAndFlush(newPayment);

            String newPaymentID = newPayment.getId().toString();
            newPayment.setPaymentReference(newPaymentID);
            newPayment.setPaymentID("Cash_Payment" + "_" + newPaymentID);
            paymentsFacade.edit(newPayment);
            logger.log(Level.INFO, "Cash/Direct Deposit Payment Created for customer {0} with paymentID: {1}", new Object[]{selectedCustomer.getUsername(), newPaymentID});

        } catch (Exception e) {
            logger.log(Level.WARNING, "Cash/Direct Deposit Payment Payment failed due to exception:", e);
        }

        paymentDBList = null;
        paymentsDBListFilteredItems = null;
    }

    public void clearSchedule(ActionEvent actionEvent) {

        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        if (loggedInUser != null) {
            List<Payments> crmPaymentList = paymentsFacade.findScheduledPaymentsByCustomer(selectedCustomer);
            if (paymentKeepManualPayments == false) {
                startAsynchJob("ClearSchedule", paymentBean.clearSchedule(selectedCustomer, false, loggedInUser, getDigitalKey()));
            }
            if (crmPaymentList != null) {
                logger.log(Level.INFO, "createSchedule - Found {0} existing scheduled payments for {1}", new Object[]{crmPaymentList.size(), selectedCustomer.getUsername()});
                for (int x = crmPaymentList.size() - 1; x > -1; x--) {
                    Payments p = crmPaymentList.get(x);
                    String ref = p.getId().toString();
                    boolean isManual = true;
                    if (p.getManuallyAddedPayment() != null) {
                        isManual = p.getManuallyAddedPayment();
                    }

                    if (paymentKeepManualPayments == true && isManual) {
                        logger.log(Level.INFO, "createSchedule - keeping manual payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{selectedCustomer.getUsername(), ref, isManual});
                    } else {
                        logger.log(Level.INFO, "createSchedule - Deleting payment: Cust={0}, Ref={1}, Manaul Payment = {2}", new Object[]{selectedCustomer.getUsername(), ref, isManual});
                        // if (paymentKeepManualPayments == false) {
                        //     paymentsFacade.remove(p);
                        //  } else {
                        p.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                        paymentsFacade.edit(p);
                        updatePaymentLists(p);
                        if (paymentKeepManualPayments == true) {
                            startAsynchJob("DeletePayment", paymentBean.deletePayment(selectedCustomer, null, null, ref, loggedInUser, getDigitalKey()));
                        }
                    }
                }
            }
            Customers c = selectedCustomer;
            PaymentParameters pp = c.getPaymentParameters();
            pp.setPaymentPeriod("Z");
            pp.setPaymentPeriodDayOfMonth("-");
            pp.setPaymentPeriodDayOfWeek("---");
            pp.setNextScheduledPayment(null);
            ejbPaymentParametersFacade.edit(pp);
            customersFacade.edit(c);

            // paymentDBList = null;
            //  paymentsDBListFilteredItems = null;
        } else {
            logger.log(Level.WARNING, "Logged in user is null. clearSchedule aborted.");
        }

    }

    public void updateSelectedScheduledPayment(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Payments.class) {
            Payments pay = (Payments) o;
            selectedScheduledPayment = pay;
            logger.log(Level.INFO, "Paymenmt Table Payment Selected. Payment Id :{0}", selectedScheduledPayment.getId().toString());
        } else {
            logger.log(Level.WARNING, "Payment Table Payment Select Failed.");
        }

    }

    public void deleteScheduledPayment(ActionEvent actionEvent) {
        if (selectedScheduledPayment != null) {
            String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
            Double amount = selectedScheduledPayment.getPaymentAmount().floatValue() * (double) 100;
            if (loggedInUser != null) {
                Payments pay = paymentsFacade.findPaymentById(selectedScheduledPayment.getId());

                if (pay != null) {
                    if (pay.getPaymentStatus().contentEquals(PaymentStatus.SCHEDULED.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.DELETE_REQUESTED.value()) || pay.getPaymentStatus().contentEquals(PaymentStatus.MISSING_IN_PGW.value())) {

                        if (pay.getPaymentStatus().contentEquals(PaymentStatus.MISSING_IN_PGW.value())) {
                            pay.setBankFailedReason("MISSING");
                        }
                        pay.setPaymentStatus(PaymentStatus.DELETE_REQUESTED.value());
                        paymentsFacade.edit(pay);
                        updatePaymentLists(pay);
                        startAsynchJob("DeletePayment", paymentBean.deletePayment(selectedCustomer, selectedScheduledPayment.getDebitDate(), amount.longValue(), selectedScheduledPayment.getId().toString(), loggedInUser, getDigitalKey()));

                        //paymentsFacade.remove(pay);
                        //paymentDBList = null;
                        //paymentsDBListFilteredItems = null;
                    }

                } else {
                    logger.log(Level.WARNING, "deleteScheduledPayment , cant't find the local scheduled payment in our DB.");
                }
            } else {
                logger.log(Level.WARNING, "Logged in user is null. Delete Payment aborted.");
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
            startAsynchJob("ClearSchedule", paymentBean.clearSchedule(cust, false, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Customer status is not one of ACTIVE,ON HOLD or CANCELLED. changeCustomerStatus aborted.");
            return;
        }

        if (loggedInUser != null) {
            startAsynchJob("ChangeCustomerStatus", paymentBean.changeCustomerStatus(cust, eziStatus, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. changeCustomerStatus aborted.");
        }
    }

    public void changeScheduledAmount(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = (long) (paymentAmountInCents * (float) 100);
        if (loggedInUser != null) {
            List<Payments> crmPaymentList = paymentsFacade.findPaymentsByCustomerAndStatus(selectedCustomer, PaymentStatus.SCHEDULED.value());
            for (Payments p : crmPaymentList) {
                if (!(paymentKeepManualPayments && p.getManuallyAddedPayment())) {
                    paymentsFacade.remove(p);
                }
            }
            startAsynchJob("ChangeScheduledAmount", paymentBean.changeScheduledAmount(selectedCustomer, paymentDebitDate, amount, paymentLimitToNumberOfPayments, applyToAllFuturePayments, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    public void changeScheduledDate(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String paymentReference = selectedCustomer.getId().toString() + "-" + sdf.format(new Date());
        if (loggedInUser != null) {
            startAsynchJob("ChangeScheduledDate", paymentBean.changeScheduledDate(selectedCustomer, changeFromDate, paymentDebitDate, paymentReference, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    private INonPCIService getWs() {
        URL url = null;
        WebServiceException e = null;
        try {
            url = new URL(configMapFacade.getConfig("payment.ezidebit.gateway.url"));
        } catch (MalformedURLException ex) {
            e = new WebServiceException(ex);
        }
        return new NonPCIService(url).getBasicHttpBindingINonPCIService();
    }

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
                                                    logger.log(Level.INFO, "Duplicate Found: {0}", line2);
                                                } else {
                                                    logger.log(Level.WARNING, "Non Cancelled Duplicate Found: {0}", line2);
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
                    logger.log(Level.INFO, message);
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
                                logger.log(Level.INFO, "No firstname for {0}", references[0]);
                            }
                            String ezidebitRef = references[1].substring(references[1].indexOf(":") + 1);
                            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yy");
                            Date contractStartDate = sdf.parse(startDateText);

                            // check for existing customer in our database
                            Customers localCustomer = customersFacade.findCustomerByName(firstname, lastname);
                            logger.log(Level.INFO, "Looking for {0} {1} in the local CRM database", new Object[]{firstname, lastname});

                            if (localCustomer != null) {
                                logger.log(Level.INFO, "Found {0} {1} in the local CRM database with CRM id:{2}", new Object[]{firstname, lastname, localCustomer.getId()});
                                addDefaultPaymentParametersIfEmpty(localCustomer, contractStartDate);
                                logger.log(Level.INFO, "Looking for {0} {1} in the payment Gateway database using Ezidebit reference:{2} database", new Object[]{firstname, lastname, ezidebitRef});

                                EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(getDigitalKey(), ezidebitRef, "");
                                if (customerdetails.getError() == 0) {// any errors will be a non zero value
                                    logger.log(Level.INFO, "Get Customer (EziId) Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());
                                    logger.log(Level.INFO, "Found {0} {1} in the Payment Gateway database with Ezidebit id:{2}", new Object[]{firstname, lastname, ezidebitRef});
                                    cd = customerdetails.getData().getValue();

                                } else {
                                    logger.log(Level.WARNING, "Get Customer (EziId) Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
                                }
                                if (cd != null) {
                                    boolean updateSystemRefInEzidebit = false;
                                    // found the customer in ezidebit. check the system reference and update it to our customers primary key
                                    int key = localCustomer.getId();
                                    String eziYourSysRef = cd.getYourSystemReference().getValue();
                                    String eziSysRef = cd.getEzidebitCustomerID().getValue();
                                    logger.log(Level.INFO, "Customer {0}, Our primary Key: {1},EziYourSysRef {3}, Ezidebit Ref: {2}", new Object[]{references[0], key, eziSysRef, eziYourSysRef});
                                    try {
                                        int eziRef = Integer.parseInt(eziYourSysRef);
                                        if (eziRef == key) {
                                            logger.log(Level.INFO, "Keys already match. Nothing to do.", new Object[]{references[0], key, eziYourSysRef});
                                        } else {
                                            logger.log(Level.SEVERE, "The system references don't match!  Possible Duplicate!");
                                            duplicateLines += "The system references don't match! Possible Duplicate! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";
                                            updateSystemRefInEzidebit = true;
                                        }

                                    } catch (NumberFormatException numberFormatException) {
                                        updateSystemRefInEzidebit = true;
                                    }
                                    if (updateSystemRefInEzidebit == true) {
                                        try {
                                            Future<Boolean> res = paymentBean.editCustomerDetails(localCustomer, eziSysRef, getLoggedInUser(), getDigitalKey());

                                            if (res.get(180, TimeUnit.SECONDS) == true) {
                                                logger.log(Level.INFO, "System reference updated");
                                            } else {
                                                logger.log(Level.WARNING, "System reference update Failed");
                                                duplicateLines += "The System reference update Failed ! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";

                                            }
                                        } catch (InterruptedException | ExecutionException | TimeoutException interruptedException) {
                                            logger.log(Level.WARNING, "System reference update Failed due to timeout, execution or interuption exception!");
                                            duplicateLines += "The System reference update Failed ! Customer " + references[0] + ", Our primary Key:" + key + ", Ezidebit Ref: " + eziSysRef + "\r\n";

                                        } catch (EJBException ex) {
                                            Exception causedByEx = ex.getCausedByException();
                                            logger.log(Level.WARNING, "System reference update Failed due to EJB Excpetion:", causedByEx.getMessage());
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
                                                logger.log(Level.INFO, "Updating CRM Status to: {0}. EziDebit Status: {1}", new Object[]{cs.getCustomerState(), status});
                                            }
                                        }
                                    }

                                } else {

                                    logger.log(Level.WARNING, "Could not find the customer in ezidebit {0}", line);

                                }
                            } else {
                                logger.log(Level.WARNING, "Customer does not exist in local system! Customer {0}\r\n", references[0]);
                                duplicateLines += "Customer does not exist in local system! Customer " + references[0] + "\r\n";

                            }

                        }

                    }
                }
            }
            setDuplicateValues(duplicateLines);

            logger.log(Level.INFO, "Customer sync with Ezidebit completed. Updated or created count={0}", count);
            JsfUtil.addSuccessMessage(count + ", " + configMapFacade.getConfig("EzidebitImportCreated"));
            return "/admin/customers/List";
        } catch (ParseException e) {
            JsfUtil.addErrorMessage(e, e.getMessage());
            return null;
        }
    }

}
