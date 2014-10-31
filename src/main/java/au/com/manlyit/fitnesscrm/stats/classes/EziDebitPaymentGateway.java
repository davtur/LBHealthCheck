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
import au.com.manlyit.fitnesscrm.stats.classes.util.AsyncJob;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.classes.util.PushComponentUpdateBean;
import au.com.manlyit.fitnesscrm.stats.classes.util.ScheduledPaymentPojo;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
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
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.util.CellRangeAddress;
import org.primefaces.component.tabview.Tab;
import org.primefaces.context.RequestContext;
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
    private boolean asyncOperationRunning = false;
    private final ThreadGroup tGroup1 = new ThreadGroup("EziDebitOps");
    private List<Payment> paymentsList;
    private PfSelectableDataModel<Payments> paymentDBList = null;
    private PfSelectableDataModel<Payments> reportPaymentsList = null;
    private List<Payment> paymentsListFilteredItems;
    private List<Payment> paymentsDBListFilteredItems;
    private List<Payment> reportPaymentsListFilteredItems;
    private List<ScheduledPaymentPojo> scheduledPaymentsList;
    private Payments selectedReportItem;
    private String reportName = "defaultreport";
    private Payments selectedScheduledPayment;
    private List<ScheduledPaymentPojo> scheduledPaymentsListFilteredItems;
    private CustomerDetails currentCustomerDetails;
    private Payment payment;
    private Date paymentDebitDate = new Date();
    private DatatableSelectionHelper pagination;
    private Date changeFromDate = new Date();
    private Long paymentAmountInCents = new Long("0");
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
    private boolean manualRefreshFromPaymentGateway = false;

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
    private float reportTotalSuccessful=0;
    private float reportTotalDishonoured=0;

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
        // if (paymentsList == null) {
        //     getPayments();
        //  }
        return paymentsList;
    }

    private void getCustDetailsFromEzi() {
        startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
    }

    protected void getPayments() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, 12);
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -24);
        startAsynchJob("GetPayments", paymentBean.getPayments(selectedCustomer, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()));
        startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(selectedCustomer, cal.getTime(), endDate, getDigitalKey()));
    }

    public void editCustomerDetailsInEziDebit(Customers cust) {
        if (customerExistsInPaymentGateway) {
            startAsynchJob("EditCustomerDetails", paymentBean.editCustomerDetails(cust, null, getDigitalKey()));
        }

    }

    public void reportDateChange() {
        reportPaymentsList = null;
        reportPaymentsListFilteredItems = null;
        generatePaymentsReport();
    }

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
    public Long getPaymentAmountInCents() {
        return paymentAmountInCents;
    }

    /**
     * @param paymentAmountInCents the paymentAmountInCents to set
     */
    public void setPaymentAmountInCents(Long paymentAmountInCents) {
        this.paymentAmountInCents = paymentAmountInCents;
    }

    /**
     * @return the scheduledPaymentsList
     */
    public List<ScheduledPaymentPojo> getScheduledPaymentsList() {
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
            getPayments();
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
        String urlForLogo =  configMapFacade.getConfig("system.email.logo");
        try {
            pdf.add(Image.getInstance(new URL(urlForLogo)));
        } catch (IOException | DocumentException iOException) {
            logger.log(Level.WARNING, "Logo URL error",iOException);
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

    public void runReport() {
        reportPaymentsList = null;
        reportPaymentsListFilteredItems = null;
        String key = "PaymentReport";
        reportUseSettlementDate = false;
        if (reportType == 1) {
            key = "SettlementReport";
            reportUseSettlementDate = true;
        }

        if (manualRefreshFromPaymentGateway) {
            manualRefreshFromPaymentGateway = false;
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Starting async Manual Refresh from Payment Gateway with starting date:", reportStartDate);
            startAsynchJob(key, paymentBean.getAllPaymentsBySystemSinceDate(reportStartDate,reportEndDate, reportUseSettlementDate, getDigitalKey()));

        }
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
    public List<Payment> getPaymentsDBListFilteredItems() {
        return paymentsDBListFilteredItems;
    }

    /**
     * @param paymentsListFilteredItems2 the paymentsDBListFilteredItems to set
     */
    public void setPaymentsDBListFilteredItems(List<Payment> paymentsListFilteredItems2) {
        this.paymentsDBListFilteredItems = paymentsListFilteredItems2;
    }

    
    private void generatePaymentsReport(){
        Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Running Report");
            List<Payments> pl = paymentsFacade.findPaymentsByDateRange(reportUseSettlementDate, reportShowSuccessful, reportShowFailed, reportShowPending, reportStartDate, reportEndDate, false);
            reportTotalSuccessful = 0;
            reportTotalDishonoured = 0;
            for(Payments p : pl){
                if(p.getPaymentStatus().contains("S") || p.getPaymentStatus().contains("P") ){
                    reportTotalSuccessful = reportTotalSuccessful + p.getPaymentAmount().floatValue();
                }else{
                    reportTotalDishonoured = reportTotalDishonoured +  p.getPaymentAmount().floatValue();
                }
            }
            
            reportPaymentsList = new PfSelectableDataModel<>(pl);
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.INFO, "Report Completed");
    }
    /**
     * @return the reportPaymentsList
     */
    public PfSelectableDataModel<Payments> getReportPaymentsList() {
        if (reportPaymentsList == null) {
            generatePaymentsReport();
            
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
    public List<Payment> getReportPaymentsListFilteredItems() {
        return reportPaymentsListFilteredItems;
    }

    /**
     * @param reportPaymentsListFilteredItems the
     * reportPaymentsListFilteredItems to set
     */
    public void setReportPaymentsListFilteredItems(List<Payment> reportPaymentsListFilteredItems) {
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
            paymentDBList = new PfSelectableDataModel<>(paymentsFacade.findPaymentsByCustomer(selectedCustomer));
        }
        return paymentDBList;
    }

    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
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
        widgetUrl += amp + "template=" + configMapFacade.getConfig("payment.ezidebit.widget.template");//template name win7
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
            JsfUtil.addSuccessMessage("Customer Added to Payment Gateway Successfully.", "");
            customerExistsInPaymentGateway = true;
            startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
            getPayments();

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

    public void pollerListener() {
        logger.log(Level.INFO, "Poller called backing bean listener method.");

        int k = futureMap.size(sessionId);
        if (k > 0) {
            logger.log(Level.INFO, "{0} jobs are running. Checking to see if asych jobs have finished so their results can be processed.", k);
            if (isAsyncOperationRunning() == false) {
                logger.log(Level.WARNING, "{0} jobs are running but asychOperationRunning flag is false!!", k);
                setAsyncOperationRunning(true);
            }
            checkIfAsyncJobsHaveFinishedAndUpdate();
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

    public void checkIfAsyncJobsHaveFinishedAndUpdate() {

        String key = "";

        try {

            key = "GetCustomerDetails";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetCustomerDetails(ft);

                }
            }
            /// process next op

            key = "AddPayment";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processAddPaymentResult(ft);
                }
            }
            // process next op

            key = "GetPayments";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetPayments(ft);
                }
            }

            key = "GetScheduledPayments";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetScheduledPayments(ft);
                }
            }
            key = "CreateSchedule";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processCreateSchedule(ft);
                }
            }
            key = "AddCustomer";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processAddCustomer(ft);
                }
            }

            key = "EditCustomerDetails";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processEditCustomerDetails(ft);
                }
            }
            key = "ClearSchedule";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processClearSchedule(ft);
                }
            }
            key = "DeletePayment";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processDeletePayment(ft);
                }
            }
            key = "ChangeCustomerStatus";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processChangeCustomerStatus(ft);
                }
            }
            key = "GetPaymentStatus";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetPaymentStatus(ft);
                }
            }
            key = "ChangeScheduledAmount";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processChangeScheduledAmount(ft);
                }
            }
            key = "ChangeScheduledDate";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processChangeScheduledDate(ft);
                }
            }
            key = "IsBsbValid";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processIsBsbValid(ft);
                }
            }
            key = "IsSystemLocked";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processIsSystemLocked(ft);
                }
            }
            key = "GetPaymentExchangeVersion";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetPaymentExchangeVersion(ft);
                }
            }
            key = "GetCustomerDetailsFromEziDebitId";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetCustomerDetailsFromEziDebitId(ft);
                }
            }
            key = "GetPaymentDetail";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetPaymentDetail(ft);
                }
            }
            key = "GetPaymentDetailPlusNextPaymentInfo";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processGetPaymentDetailPlusNextPaymentInfo(ft);
                }
            }
            key = "PaymentReport";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processPaymentReport(ft);
                }
            }
            key = "SettlementReport";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    processSettlementReport(ft);
                }
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

        logger.log(Level.INFO, "processSettlementReport completed");
    }

    private void processGetPayments(Future ft) {
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

        logger.log(Level.INFO, "processGetPayments completed");
    }

    private void processGetScheduledPayments(Future ft) {
        ArrayOfScheduledPayment result = null;
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

        logger.log(Level.INFO, "processGetScheduledPayments completed");
    }

    private void processAddPaymentResult(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, null, ex);
        }
        if (result == true) {
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Add Payment", "Payment submitted successfully.");
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Add Payment", "Payment Failed! Is the customer active with valid account/card details?");
        }
        getPayments();
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment", "Successfully Created Schedule.");
            getPayments();

        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment", "The Create Schedule operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Edited Customer Details  .");
            getCustDetailsFromEzi();
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processEditCustomerDetails completed");
    }

    private void processClearSchedule(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Cleared Schedule .");
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processClearSchedule completed");
    }

    private void processDeletePayment(Future ft) {
        boolean result = false;
        try {
            result = (boolean) ft.get();
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(EziDebitPaymentGateway.class.getName()).log(Level.SEVERE, "Processing Async Results", ex);
        }
        if (result == true) {
            setSelectedScheduledPayment(null);
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Deleted Payment  .");
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The delete payment operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Changed Customer Status  .");
            startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The change status operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Retrieved Payment Status  .");

        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The getPayment status operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully  Changed Scheduled Amount .");
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Changed Scheduled Date  .");
            getPayments();
        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully Checked BSB  .");

        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Successfully checked if System is Locked  .");

        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
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
            JsfUtil.pushSuccessMessage(CHANNEL + sessionId, "Payment Gateway", "Payment Exchange Version: " + result);

        } else {
            JsfUtil.pushErrorMessage(CHANNEL + sessionId, "Payment Gateway", "The operation failed!.");
        }
        logger.log(Level.INFO, "processGetPaymentExchangeVersion completed");
    }

    private void processGetCustomerDetails(Future ft) {
        CustomerDetails result = null;
        setCustomerDetailsHaveBeenRetrieved(true);
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

        getPayments();
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

    public void addSinglePayment(ActionEvent actionEvent) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddhhmmss");
        String paymentReference = selectedCustomer.getId().toString() + "-" + sdf.format(new Date());
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = paymentAmountInCents * (long) 100;
        if (loggedInUser != null) {
            startAsynchJob("AddPayment", paymentBean.addPayment(selectedCustomer, paymentDebitDate, amount, paymentReference, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }

    }

    private void startAsynchJob(String key, Future future) {
        setAsyncOperationRunning(true);
        AsyncJob aj = new AsyncJob(key, future);
        futureMap.put(sessionId, aj);
    }

    public void createEddrLink(ActionEvent actionEvent) {
        Customers cust = getSelectedCustomer();
        if (cust == null || cust.getId() == null) {
            logger.log(Level.WARNING, "Create EDDR Link cannot be completed as teh selected customer is null.");
            return;
        }
        NumberFormat nf = NumberFormat.getNumberInstance();
        nf.setMaximumFractionDigits(2);
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
        widgetUrl += amp + "sms=" + configMapFacade.getConfig("payment.ezidebit.webddr.sms");
        widgetUrl += amp + "addr=" + cust.getStreetAddress();
        widgetUrl += amp + "suburb=" + cust.getSuburb();
        widgetUrl += amp + "state=" + cust.getAddrState();
        widgetUrl += amp + "pCode=" + cust.getPostcode();
        widgetUrl += amp + "debits=" + configMapFacade.getConfig("payment.ezidebit.webddr.debits");
        widgetUrl += amp + "rAmount=" + nf.format(paymentAmountInCents);
        widgetUrl += amp + "rDate=" + sdf.format(paymentDebitDate);
        widgetUrl += amp + "aFreq=" + configMapFacade.getConfig("payment.ezidebit.webddr.aFreq");
        widgetUrl += amp + "freq=" + paymentSchedulePeriodType;
        widgetUrl += amp + "aDur=" + configMapFacade.getConfig("payment.ezidebit.webddr.aDur");
        widgetUrl += amp + "dur=" + configMapFacade.getConfig("payment.ezidebit.webddr.dur");
        widgetUrl += amp + "callback=" + configMapFacade.getConfig("payment.ezidebit.webddr.callback");

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        PaymentParameters pp = controller.getSelectedCustomersPaymentParameters();
        pp.setWebddrUrl(widgetUrl);
        ejbPaymentParametersFacade.edit(pp);
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
        Long amount = paymentAmountInCents * (long) 100;
        Long amountLimit = paymentLimitAmountInCents * (long) 100;
        char spt = paymentSchedulePeriodType.charAt(0);
        if (loggedInUser != null) {
            startAsynchJob("CreateSchedule", paymentBean.createSchedule(selectedCustomer, paymentDebitDate, spt, paymentDayOfWeek, paymentDayOfMonth, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
        JsfUtil.addSuccessMessage("Sending Delete Request to Payment Gateway.");
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    public void deleteScheduledPayment(ActionEvent actionEvent) {

        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Double amount = selectedScheduledPayment.getPaymentAmount().floatValue() * (double) 100;
        if (loggedInUser != null) {
            startAsynchJob("DeletePayment", paymentBean.deletePayment(selectedCustomer, selectedScheduledPayment.getDebitDate(), amount.longValue(), selectedScheduledPayment.getPaymentReference(), loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Delete Payment aborted.");
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
        Long amount = paymentAmountInCents * (long) 100;
        if (loggedInUser != null) {
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
                                            Future<Boolean> res = paymentBean.editCustomerDetails(localCustomer, eziSysRef, getDigitalKey());

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

// old stuff that was moved to asych methods in paymentBean

/* private Future executeGetCustomerDetails(Customers cust) {
 Callable<CustomerDetails> callable1 = new CustomerDetailsCallable(cust, getDigitalKey());
 Future<CustomerDetails> fTask = executor1.submit(callable1);
 return fTask;
 }
 private CustomerDetails getCustomerDetails(Customers cust) {
 if (cust.getId() == null) {
 return null;
 }

 CustomerDetails cd = null;
 logger.log(Level.INFO, "Getting Customer Details {0}", cust.getUsername());
 
 EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(getDigitalKey(), "", cust.getId().toString());
 if (customerdetails.getError().intValue() == 0) {// any errors will be a non zero value
 logger.log(Level.INFO, "Get Customer Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

 cd = customerdetails.getData().getValue();

 } else {
 logger.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());

 }
 return cd;
 }*/
/* private CustomerDetails getCustomerDetails(String eziDebitId) {

 CustomerDetails cd = null;
 logger.log(Level.INFO, "Getting Customer Details {0}", eziDebitId);
 
 EziResponseOfCustomerDetailsTHgMB7OL customerdetails = getWs().getCustomerDetails(getDigitalKey(), eziDebitId, "");
 if (customerdetails.getError().intValue() == 0) {// any errors will be a non zero value
 logger.log(Level.INFO, "Get Customer Details Response: Name - {0}", customerdetails.getData().getValue().getCustomerName().getValue());

 cd = customerdetails.getData().getValue();

 } else {
 logger.log(Level.WARNING, "Get Customer Details Response: Error - {0}", customerdetails.getErrorMessage().getValue());
 }
 return cd;
 }*/

/* private boolean addCustomer(Customers cust, String paymentGatewayName) {
 boolean result = false;
 logger.log(Level.INFO, "Adding Customer  {0}", cust.getUsername());
 
 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
 Collection<PaymentParameters> pay = cust.getPaymentParametersCollection();
 if (pay == null) {
 logger.log(Level.WARNING, "Payment Parameters are null");
 return false;
 }
 PaymentParameters payParams = null;
 String addresssLine2 = ""; // not used
 String humanFriendlyReference = cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default
 if (pay.isEmpty() && paymentGatewayName.toUpperCase().indexOf(paymentGateway) != -1) {
 payParams = new PaymentParameters(0, new Date(), cust.getTelephone(), "NO", "NO", "NO", paymentGateway);
 Customers loggedInUser = customersFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
 payParams.setLoggedInUser(loggedInUser);

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
 return false;
 }

 // note - NB - for Australian Customers the
 //mobile phone number must be 10
 //digits long and begin with '04'. For
 //New Zealand Customers the mobile
 //phone number must be 10 digits
 //long and begin with '02'
 EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = getWs().addCustomer(getDigitalKey(), cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), sdf.format(payParams.getContractStartDate()), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());

 if (addCustomerResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = true;
 logger.log(Level.INFO, "Add Customer Response: Error - {0}, Data - {1}", new Object[]{addCustomerResponse.getErrorMessage().getValue(), addCustomerResponse.getData().getValue()});

 } else {
 logger.log(Level.WARNING, "Add Customer Response: Error - {0},", addCustomerResponse.getErrorMessage().getValue());

 }
 return result;
 }

 private PaymentDetail getPaymentDetail(String paymentReference) {
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
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentDetail paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfPaymentDetailTHgMB7OL eziResponse = getWs().getPaymentDetail(getDigitalKey(), paymentReference);
 logger.log(Level.INFO, "getPaymentDetail Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();

 } else {
 logger.log(Level.WARNING, "getPaymentDetail Response: Error - {0}", eziResponse.getErrorMessage().getValue());

 }

 return result;
 }

 private boolean getPaymentStatus(String paymentReference) {
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
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentStatus paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfstring eziResponse = getWs().getPaymentStatus(getDigitalKey(), paymentReference);
 logger.log(Level.INFO, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value

 if (eziResponse.getData().getValue().compareTo("S") == 0) {
 result = true;
 } else {
 logger.log(Level.WARNING, "getPaymentStatus Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 } else {
 logger.log(Level.WARNING, "getPaymentStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }

 private PaymentDetailPlusNextPaymentInfo getPaymentDetailPlusNextPaymentInfo(String paymentReference) {
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
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL eziResponse = getWs().getPaymentDetailPlusNextPaymentInfo(getDigitalKey(), paymentReference);
 logger.log(Level.INFO, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();

 } else {
 logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

 }

 return result;
 }*/

/*  private boolean deletePayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser) {
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
 

 if (paymentReference != null && paymentAmountInCents != new Long(0)) {
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
 EziResponseOfstring eziResponse = getWs().deletePayment(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, paymentReference, debitDateString, paymentAmountInCents, loggedInUser);
 logger.log(Level.INFO, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value

 if (eziResponse.getData().getValue().compareTo("S") == 0) {
 result = true;
 } else {
 logger.log(Level.WARNING, "deletePayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 } else {
 logger.log(Level.WARNING, "deletePayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }*/

/*private boolean addPayment(Customers cust, Date debitDate, Long paymentAmountInCents, String paymentReference, String loggedInUser) {
 // paymentReference Max 50 chars. It can be search with with a wildcard in other methods. Use invoice number or other payment identifier
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
 EziResponseOfstring eziResponse = getWs().addPayment(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, debitDateString, paymentAmountInCents, paymentReference, loggedInUser);
 logger.log(Level.INFO, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value

 if (eziResponse.getData().getValue().compareTo("S") == 0) {
 result = true;
 } else {
 logger.log(Level.WARNING, "addPayment Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 } else {
 logger.log(Level.WARNING, "addPayment Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }*/
/*   private boolean clearSchedule(Customers cust, boolean keepManualPayments, String loggedInUser) {
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
 EziResponseOfstring eziResponse = getWs().clearSchedule(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, keepManualPaymentsString, loggedInUser);
 logger.log(Level.INFO, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value

 if (eziResponse.getData().getValue().compareTo("S") == 0) {
 result = true;
 } else {
 logger.log(Level.WARNING, "clearSchedule Response Data value should be S ( Successful ) : Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 } else {
 logger.log(Level.WARNING, "clearSchedule Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }*/

/* private boolean changeCustomerStatus(Customers cust, String newStatus, String loggedInUser) {
 // note: cancelled status cannot be changed with this method. i.e. cancelled is final like deleted.
 boolean result = false;
 String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
 String ourSystemCustomerReference = cust.getId().toString();
 
 if (newStatus.compareTo("A") == 0 || newStatus.compareTo("H") == 0 || newStatus.compareTo("C") == 0) {
 if (loggedInUser.length() > 50) {
 loggedInUser = loggedInUser.substring(0, 50);
 logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
 }
 EziResponseOfstring eziResponse = getWs().changeCustomerStatus(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, newStatus, loggedInUser);
 logger.log(Level.INFO, "changeCustomerStatus Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
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
 return result;
 }*/

/*  private boolean editCustomerDetails(Customers cust, String eziDebitRef) {
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
 String humanFriendlyReference = cust.getLastname().toUpperCase() + " " + cust.getFirstname().toUpperCase(); // existing customers use this type of reference by default

 if (pay != null) {
 for (PaymentParameters pp : pay) {
 if (pp.getPaymentGatewayName().compareTo(paymentGateway) == 0) {
 payParams = pp;
 } else {

 }

 }
 } else {
 logger.log(Level.WARNING, "Payment Parameters are null");
 return false;
 }
 if (payParams == null) {
 logger.log(Level.WARNING, "Payment gateway EZIDEBIT parameters not found");
 return false;
 }

 // note - NB - for Australian Customers the
 //mobile phone number must be 10
 //digits long and begin with '04'. For
 //New Zealand Customers the mobile
 //phone number must be 10 digits
 //long and begin with '02'
 EziResponseOfstring editCustomerDetail = getWs().editCustomerDetails(getDigitalKey(), eziDebitRef, ourSystemRef, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());
 logger.log(Level.INFO, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});
 if (editCustomerDetail.getError().intValue() == 0) {// any errors will be a non zero value
 result = true;
 } else {
 logger.log(Level.WARNING, "editCustomerDetail Response: Error - {0}, Data - {1}", new Object[]{editCustomerDetail.getErrorMessage().getValue(), editCustomerDetail.getData().getValue()});

 }
 return result;
 }*/

/* private ArrayOfPayment getPayments(Customers cust, String paymentType, String paymentMethod, String paymentSource, String paymentReference, Date fromDate, Date toDate, boolean useSettlementDate) {
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
 if (paymentType.compareTo("ALL") != 0 && paymentType.compareTo("PENDING") != 0 && paymentType.compareTo("FAILED") != 0 && paymentType.compareTo("SUCCESSFUL") != 0) {
 logger.log(Level.WARNING, "getPayments: payment Type is required and should be either ALL,PENDING,FAILED,SUCCESSFUL.  Returning null as this parameter is required.");
 return null;
 }
 if (paymentMethod.compareTo("ALL") != 0 && paymentMethod.compareTo("CR") != 0 && paymentMethod.compareTo("DR") != 0) {
 logger.log(Level.WARNING, "getPayments: payment Method is required and should be either ALL,CR,DR.  Returning null as this parameter is required.");
 return null;
 }
 if (paymentSource.compareTo("ALL") != 0 && paymentSource.compareTo("SCHEDULED") != 0 && paymentSource.compareTo("WEB") != 0 && paymentSource.compareTo("PHONE") != 0 && paymentSource.compareTo("BPAY") != 0) {
 logger.log(Level.WARNING, "getPayments: paymentSource is required and should be either ALL,SCHEDULED,WEB,PHONE,BPAY.  Returning null as this parameter is required.");
 return null;
 }
 if (paymentReference == null) {
 paymentReference = "";
 }
 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPayments paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 ArrayOfPayment result = null;
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

 

 EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = getWs().getPayments(getDigitalKey(), paymentType, paymentMethod, paymentSource, paymentReference, fromDateString, toDateString, dateField, eziDebitCustomerId, ourSystemCustomerReference);
 logger.log(Level.INFO, "getPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();

 } else {
 logger.log(Level.WARNING, "getPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

 }

 return result;
 }

 private ArrayOfScheduledPayment getScheduledPayments(Customers cust, Date fromDate, Date toDate) {
 //  Description
 //  This method allows you to retrieve information about payments that are scheduled for a
 //  given Customer in the Ezidebit direct debit system.
 //  It is important to note the following when querying Payment details:
 //  • This method can be used to retrieve information about payments that are
 //  scheduled to be debited, but have not yet been sent to the bank for processing;
 //  • This method provides access only to payments that have been added to a payer's
 //  schedule through the integrated web services, Ezidebit Online website or by
 //  Ezidebit client support.
 //  Payment information about real-time credit card or BPAY payments cannot be accessed
 //  through this method.
 ArrayOfScheduledPayment result = null;

 String eziDebitCustomerId = ""; // use our reference instead. THis must be an empty string.
 SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
 String fromDateString = ""; // The exact date on which the payment that you wish to move is scheduled to be deducted from your Customer's bank account or credit card.
 String toDateString = sdf.format(toDate); // The new date that you wish for this payment to be deducted from your Customer's bank account or credit card.
 String ourSystemCustomerReference = cust.getId().toString();

 

 EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = getWs().getScheduledPayments(getDigitalKey(), fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
 logger.log(Level.INFO, "getScheduledPayments Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();

 } else {
 logger.log(Level.WARNING, "getScheduledPayments Response: Error - {0}, ", eziResponse.getErrorMessage().getValue());

 }

 return result;
 }*/
/* private boolean isBsbValid(String bsb) {
 //  Description
 //  check that the BSB is valid
 boolean result = false;

 

 EziResponseOfstring eziResponse = getWs().isBsbValid(getDigitalKey(), bsb);
 logger.log(Level.INFO, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 String valid = eziResponse.getData().getValue();
 if (valid.compareTo("YES") == 0) {
 return true;
 }

 } else {
 logger.log(Level.WARNING, "isBsbValid Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }

 private boolean isSystemLocked() {
 //  Description
 //  check that the BSB is valid
 boolean result = false;

 

 EziResponseOfstring eziResponse = getWs().isSystemLocked(getDigitalKey());
 logger.log(Level.INFO, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 String valid = eziResponse.getData().getValue();
 if (valid.compareTo("YES") == 0) {
 return true;
 }

 } else {
 logger.log(Level.WARNING, "isSystemLocked Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});

 }

 return result;
 }

 private String getPaymentExchangeVersion() {
 //  Description
 //  
 //  This method returns the version of our web services and API that you are connecting to.
 //  This can be used as a check to ensure that you're connecting to the web services that
 //  you expect to, based on the API document that you have.

 String result = "ERROR Getting version";
 
 EziResponseOfstring eziResponse = getWs().paymentExchangeVersion();
 logger.log(Level.INFO, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();
 } else {
 logger.log(Level.WARNING, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 return result;
 }*/
