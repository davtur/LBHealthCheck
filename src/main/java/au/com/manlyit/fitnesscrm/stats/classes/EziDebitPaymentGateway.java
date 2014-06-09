/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PushComponentUpdateBean;
import au.com.manlyit.fitnesscrm.stats.classes.util.ScheduledPaymentPojo;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.ArrayOfScheduledPayment;
import au.com.manlyit.fitnesscrm.stats.webservices.CustomerDetails;
import au.com.manlyit.fitnesscrm.stats.webservices.Payment;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetail;
import au.com.manlyit.fitnesscrm.stats.webservices.PaymentDetailPlusNextPaymentInfo;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;
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
    private int testAjaxCounter = 0;
    @Inject
    private FutureMapEJB futureMap;
    // private final Map<String, Future> futureMap = new HashMap<>();
    private boolean asyncOperationRunning = false;
    private final ThreadGroup tGroup1 = new ThreadGroup("EziDebitOps");
    private List<Payment> paymentsList;
    private List<Payment> paymentsListFilteredItems;
    private List<ScheduledPaymentPojo> scheduledPaymentsList;
    private ScheduledPaymentPojo selectedScheduledPayment;
    private List<ScheduledPaymentPojo> scheduledPaymentsListFilteredItems;
    private Payment payment;
    private Date paymentDebitDate = new Date();
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

    private boolean paymentGatewayEnabled = true;
    private Integer progress;
    private AtomicBoolean pageLoaded = new AtomicBoolean(false);
    @Inject
    private PaymentBean paymentBean;
    @Inject
    private PushComponentUpdateBean pushComponentUpdateBean;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private CustomersFacade customersFacade;
    private String listOfIdsToImport;
    private boolean customerExistsInPaymentGateway = false;
    private boolean editPaymentDetails = false;
    private boolean autoStartPoller = true;
    private boolean stopPoller = false;
    private boolean customerDetailsHaveBeenRetrieved = false;
    private String eziDebitWidgetUrl = "";
    private Customers selectedCustomer;

    ThreadFactory tf1 = new eziDebitThreadFactory();
    private String sessionId;

    @PostConstruct
    private void setSessionId() {
        FacesContext facesContext = FacesContext.getCurrentInstance();
        this.sessionId = ((HttpSession) facesContext.getExternalContext().getSession(false)).getId();
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

    private void getPayments() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.add(Calendar.MONTH, 12);
        Date endDate = cal.getTime();
        cal.add(Calendar.MONTH, -24);
        startAsynchJob("GetPayments", paymentBean.getPayments(selectedCustomer, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()));
        startAsynchJob("GetScheduledPayments", paymentBean.getScheduledPayments(selectedCustomer, cal.getTime(), endDate, getDigitalKey()));
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
    public ScheduledPaymentPojo getSelectedScheduledPayment() {
        return selectedScheduledPayment;
    }

    /**
     * @param selectedScheduledPayment the selectedScheduledPayment to set
     */
    public void setSelectedScheduledPayment(ScheduledPaymentPojo selectedScheduledPayment) {
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
            loginToPushServer();
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
     * @param isEditable
     * @return the eziDebitWidgetUrl
     */
    public String getEziDebitWidgetUrl(boolean isEditable) {
        Customers cust = getSelectedCustomer();
        if (cust == null || cust.getId() == null) {
            return "";
        }
        String amp = "&";
        String viewOrEdit = "view";
        if (isEditable == true) {
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

    public void createCustomerRecord() {

        startAsynchJob("AddCustomer", paymentBean.addCustomer(getSelectedCustomer(), paymentGateway, getDigitalKey()));
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
        return futureMap.get(sessionId, key);
    }

    private void futureMapRemoveKey(String key) {
        futureMap.remove(sessionId, key);
    }

    public void checkIfAsyncJobsHaveFinishedAndUpdate() {
        CustomerDetails cd;
        String key = "";

        try {

            key = "GetCustomerDetails";
            if (futureMapContainsKey(key)) {
                Future ft = (Future) futureMapGetKey(key);
                if (ft.isDone()) {
                    futureMapRemoveKey(key);
                    setAutoStartPoller(false);
                    cd = (CustomerDetails) ft.get();
                    customerExistsInPaymentGateway = cd != null;
                    setCustomerDetailsHaveBeenRetrieved(true);
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

        } catch (ExecutionException | CancellationException | InterruptedException ex) {
            logger.log(Level.WARNING, key + ":", ex);

        }
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
            JsfUtil.addSuccessMessage("Payment", "Successfully added payment.");
            getPayments();
        } else {
            JsfUtil.addErrorMessage("Payment", "The operation failed!.");
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
            getPayments();

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
            getPayments();
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
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
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Cleared Schedule .");
            getPayments();
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
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
            JsfUtil.addSuccessMessage("Payment Gateway", "Successfully Deleted Payment  .");
            getPayments();
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
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
            getPayments();
        } else {
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
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
            JsfUtil.addErrorMessage("Payment Gateway", "The operation failed!.");
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
            getPayments();
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
            getPayments();
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
        setScheduledPaymentsList(null);
        setScheduledPaymentsListFilteredItems(null);
        setCustomerDetailsHaveBeenRetrieved(false);

    }

    /**
     * @param selectedCustomer the selectedCustomer to set
     */
    public void setSelectedCustomer(Customers selectedCustomer) {
        this.selectedCustomer = selectedCustomer;
        refreshIFrames = true;
        futureMap.cancelFutures(sessionId);
        startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(selectedCustomer, getDigitalKey()));
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
        futureMap.put(sessionId, key, future);
    }

    public void createPaymentSchedule(ActionEvent actionEvent) {
        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Long amount = paymentAmountInCents * (long) 100;
        Long amountLimit = paymentLimitAmountInCents * (long) 100;
        char spt = paymentSchedulePeriodType.charAt(0);
        if (loggedInUser != null) {
            startAsynchJob("CreateSchedule", paymentBean.createSchedule(selectedCustomer, paymentDebitDate, spt, paymentDayOfWeek, paymentDayOfMonth, paymentFirstWeekOfMonth, paymentSecondWeekOfMonth, paymentThirdWeekOfMonth, paymentFourthWeekOfMonth, amount, paymentLimitToNumberOfPayments, amountLimit, paymentKeepManualPayments, loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Add Single Payment aborted.");
        }
    }

    public void deleteScheduledPayment(ActionEvent actionEvent) {

        String loggedInUser = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        Double amount = selectedScheduledPayment.getPaymentAmount() * (double) 100;
        if (loggedInUser != null) {
            startAsynchJob("DeletePayment", paymentBean.deletePayment(selectedCustomer, selectedScheduledPayment.getPaymentDate(), amount.longValue(), selectedScheduledPayment.getPaymentReference(), loggedInUser, getDigitalKey()));
        } else {
            logger.log(Level.WARNING, "Logged in user is null. Delete Payment aborted.");
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 EziResponseOfCustomerDetailsTHgMB7OL customerdetails = ws.getCustomerDetails(getDigitalKey(), "", cust.getId().toString());
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 EziResponseOfCustomerDetailsTHgMB7OL customerdetails = ws.getCustomerDetails(getDigitalKey(), eziDebitId, "");
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
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
 EziResponseOfNewCustomerXcXH3LiW addCustomerResponse = ws.addCustomer(getDigitalKey(), cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getAddrState(), cust.getPostcode(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), sdf.format(payParams.getContractStartDate()), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());

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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 if (paymentReference == null) {

 logger.log(Level.WARNING, "getPaymentDetail paymentReference is required but it is NULL");
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentDetail paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfPaymentDetailTHgMB7OL eziResponse = ws.getPaymentDetail(getDigitalKey(), paymentReference);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 if (paymentReference == null) {

 logger.log(Level.WARNING, "getPaymentStatus paymentReference is required but it is NULL");
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentStatus paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfstring eziResponse = ws.getPaymentStatus(getDigitalKey(), paymentReference);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 if (paymentReference == null) {

 logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is required but it is NULL");
 return result;
 }

 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "getPaymentDetailPlusNextPaymentInfo paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }

 EziResponseOfPaymentDetailPlusNextPaymentInfoTHgMB7OL eziResponse = ws.getPaymentDetailPlusNextPaymentInfo(getDigitalKey(), paymentReference);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

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
 EziResponseOfstring eziResponse = ws.deletePayment(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, paymentReference, debitDateString, paymentAmountInCents, loggedInUser);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 if (paymentReference.length() > 50) {
 paymentReference = paymentReference.substring(0, 50);
 logger.log(Level.WARNING, "addPayment paymentReference is greater than the allowed 50 characters. Truncating! to 50 chars");
 }
 if (loggedInUser.length() > 50) {
 loggedInUser = loggedInUser.substring(0, 50);
 logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
 }
 EziResponseOfstring eziResponse = ws.addPayment(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, debitDateString, paymentAmountInCents, paymentReference, loggedInUser);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 String keepManualPaymentsString = "NO";// update all specified payments for customer 
 if (keepManualPayments) {
 keepManualPaymentsString = "YES";// maintain any one off or ad-hoc payment amounts
 }
 if (loggedInUser.length() > 50) {
 loggedInUser = loggedInUser.substring(0, 50);
 logger.log(Level.WARNING, "clearSchedule loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
 }
 EziResponseOfstring eziResponse = ws.clearSchedule(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, keepManualPaymentsString, loggedInUser);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 if (newStatus.compareTo("A") == 0 || newStatus.compareTo("H") == 0 || newStatus.compareTo("C") == 0) {
 if (loggedInUser.length() > 50) {
 loggedInUser = loggedInUser.substring(0, 50);
 logger.log(Level.WARNING, "addPayment loggedInUser is greater than the allowed 50 characters. Truncating! to 50 chars");
 }
 EziResponseOfstring eziResponse = ws.changeCustomerStatus(getDigitalKey(), eziDebitCustomerId, ourSystemCustomerReference, newStatus, loggedInUser);
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
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
 EziResponseOfstring editCustomerDetail = ws.editCustomerDetails(getDigitalKey(), eziDebitRef, ourSystemRef, cust.getId().toString(), humanFriendlyReference, cust.getLastname(), cust.getFirstname(), cust.getStreetAddress(), addresssLine2, cust.getSuburb(), cust.getPostcode(), cust.getAddrState(), cust.getEmailAddress(), payParams.getMobilePhoneNumber(), payParams.getSmsPaymentReminder(), payParams.getSmsFailedNotification(), payParams.getSmsExpiredCard(), payParams.getLoggedInUser().getUsername());
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

 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 EziResponseOfArrayOfPaymentTHgMB7OL eziResponse = ws.getPayments(getDigitalKey(), paymentType, paymentMethod, paymentSource, paymentReference, fromDateString, toDateString, dateField, eziDebitCustomerId, ourSystemCustomerReference);
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

 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 EziResponseOfArrayOfScheduledPaymentTHgMB7OL eziResponse = ws.getScheduledPayments(getDigitalKey(), fromDateString, toDateString, eziDebitCustomerId, ourSystemCustomerReference);
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

 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 EziResponseOfstring eziResponse = ws.isBsbValid(getDigitalKey(), bsb);
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

 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();

 EziResponseOfstring eziResponse = ws.isSystemLocked(getDigitalKey());
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
 INonPCIService ws = new NonPCIService().getBasicHttpBindingINonPCIService();
 EziResponseOfstring eziResponse = ws.paymentExchangeVersion();
 logger.log(Level.INFO, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 if (eziResponse.getError().intValue() == 0) {// any errors will be a non zero value
 result = eziResponse.getData().getValue();
 } else {
 logger.log(Level.WARNING, "getPaymentExchangeVersion Response: Error - {0}, Data - {1}", new Object[]{eziResponse.getErrorMessage().getValue(), eziResponse.getData().getValue()});
 }
 return result;
 }*/
