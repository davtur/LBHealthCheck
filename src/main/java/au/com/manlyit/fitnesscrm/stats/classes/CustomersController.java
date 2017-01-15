package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ApplicationBean;
import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.LoginBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentPeriod;
import au.com.manlyit.fitnesscrm.stats.chartbeans.MySessionsChart1;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.Plan;
import au.com.manlyit.fitnesscrm.stats.db.QuestionnaireMap;
import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.component.tabview.Tab;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.event.data.FilterEvent;

@Named("customersController")
@SessionScoped
public class CustomersController implements Serializable {

    /**
     * @return the multiSelectedCustomersBadPayments
     */
    public Payments[] getMultiSelectedCustomersBadPayments() {
        return multiSelectedCustomersBadPayments;
    }

    /**
     * @param multiSelectedCustomersBadPayments the
     * multiSelectedCustomersBadPayments to set
     */
    public void setMultiSelectedCustomersBadPayments(Payments[] multiSelectedCustomersBadPayments) {
        this.multiSelectedCustomersBadPayments = multiSelectedCustomersBadPayments;
    }

    /**
     * @return the multiSelectedCustomersOnHold
     */
    public Customers[] getMultiSelectedCustomersOnHold() {
        return multiSelectedCustomersOnHold;
    }

    /**
     * @param multiSelectedCustomersOnHold the multiSelectedCustomersOnHold to
     * set
     */
    public void setMultiSelectedCustomersOnHold(Customers[] multiSelectedCustomersOnHold) {
        this.multiSelectedCustomersOnHold = multiSelectedCustomersOnHold;
    }

    private static final long serialVersionUID = 1L;

    private Customers current;
    private Customers lastSelected;
    private Customers newCustomer;
    private Customers selectedForDeletion;
    private CustomerState selectedState;
    private CustomerState selectedForImpersonation;
    private boolean impersonationOn;
    private Notes selectedNoteForDeletion;
    private static final String paymentGateway = "EZIDEBIT";
    //private CustomerState[] selectedCustomerStates;
    private List<CustomerState> selectedCustomerStates;
    private List<String> selectedCustomerTypes;
    private List<CustomerState> customerStateList;
    private List<String> customerTypesList;
    private List<Groups> customerGroupsList;
    private List<Groups> newCustomerGroupsList;
    private PfSelectableDataModel<Customers> items = null;
    private PfSelectableDataModel<Customers> customersWithoutScheduledPayments = null;
    private PfSelectableDataModel<Customers> leads = null;
    private PfSelectableDataModel<Customers> customersOnHold = null;
    private PfSelectableDataModel<Payments> customersBadPayments = null;
    private Date testTime;

    private PfSelectableDataModel<Notes> notesItems = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SuppliersFacade suppliersFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentBean ejbPaymentBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.QuestionnaireMapFacade questionnaireMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveysFacade surveysFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailFormatFacade ejbEmailFormatFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.DemographicTypesFacade ejbDemoFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.GroupsFacade ejbGroupsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerStateFacade ejbCustomerStateFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerAuthFacade ejbCustomerAuthFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.NotesFacade ejbNotesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PlanFacade ejbPlanFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PreferedContactFacade ejbPreferedContactFacade;
    @Inject
    private FutureMapEJB futureMap;

    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade ejbPaymentsFacade;

    private DatatableSelectionHelper pagination;
    private DatatableSelectionHelper notesPagination;
    private int selectedItemIndex;
    private Plan customersNewPlan;
    private List<Customers> filteredItems;
    private List<Customers> filteredCustomersWithoutScheduledPayments;
    private List<Customers> filteredLeads;
    private List<Customers> filteredCustomersOnHold;
    private List<Payments> filteredCustomersBadPayments;
    private List<Notes> notesFilteredItems;
    private Customers[] multiSelected;
    private Customers[] multiSelectedCustomersWithoutScheduledPayments;
    private Customers[] multiSelectedLeads;
    private Customers[] multiSelectedCustomersOnHold;
    private Payments[] multiSelectedCustomersBadPayments;
    // private Groups[] selectedGroups;
    private List<Groups> selectedGroups;
    private String checkPass;
    private Boolean[] checkedGroups;
    private Boolean[] newCustomerCheckedGroups;
    private String checkPass2;
    private String leadComments = "";
    private Customers impersonate;
    private Customers loggedInUser;
    private boolean passwordsMatch = false;
    private boolean impersonating = false;
    private boolean customerTabsEnabled = false;
    private boolean refreshFromDB = false;
    private boolean addUserButtonDisabled = true;
    private boolean leadFormSubmitted = false;
    private boolean showNonUsers = false;
    private boolean signupFromBookingInProgress = false;
    private boolean signupFormSubmittedOK = false;
    private static final Logger LOGGER = Logger.getLogger(CustomersController.class.getName());

    public CustomersController() {
    }

    //@PostConstruct
    private void getLoggedInCustomer() {

        current = getLoggedInUser();
    }

    /* private void sanityCheckCustomersForDefaultItems() {
     LOGGER.log(Level.INFO, "Performing Sanity Checks on Customers");
     List<Customers> cl = ejbFacade.findAll();
     for (Customers c : cl) {
     if (c.getProfileImage() == null) {
     createDefaultCustomerProfilePicture(c);
     }
     }
     LOGGER.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
     }*/
    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public boolean isCustomerInRole(Customers cust, String roleName) {
        return ejbGroupsFacade.isCustomerInGroup(cust, roleName);
    }

    public void onTableFiltered(FilterEvent event) {
        LOGGER.log(Level.FINE, "tableFiltered");
        DataTable table = (DataTable) event.getSource();

        try {

            int size = table.getFilteredValue().size();
            LOGGER.log(Level.INFO, "onTableFiltered: Filtered Rows={0}", size);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "onTableFiltered, couldn't get the number of filtered records.");
        }

    }

    /*   public Map<String, String> onFilter(AjaxBehaviorEvent event) {
        DataTable table = (DataTable) event.getSource();
        // List<Screenshot> obj =   table.getFilteredData();

        // Do your stuff here
        Map<String, String> filters = table.getFilters();
        return filters;
    }*/
    public void setSelecteDblClick(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Customers.class) {
            Customers c = (Customers) o;
            setSelected(c);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("setSelectedCustomer") + " " + c.getFirstname() + " " + c.getLastname() + ".");

        }
    }

    public void setSelectedCustomer(ActionEvent event) {
        if (multiSelected.length == 1) {
            Customers c = multiSelected[0];
            setSelected(c);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("setSelectedCustomer") + " " + c.getFirstname() + " " + c.getLastname() + ".");
            multiSelected = null;
        } else if (multiSelected.length > 1) {
            JsfUtil.addErrorMessage(configMapFacade.getConfig("setSelectedCustomerTooManySelected"));
        } else if (multiSelected.length == 0) {
            JsfUtil.addErrorMessage(configMapFacade.getConfig("setSelectedCustomerNoneSelected"));
        }
    }

    public void updateSelectedCustomer(Customers cust) {
        current = cust;
    }

    public void setSelected(Customers cust) {
        if (cust != null) {
            lastSelected = current;
            current = cust;

            selectedGroups = ejbGroupsFacade.getCustomersGroups(cust);
            customerGroupsList = null;


            /*customerGroupsList = new ArrayList<>();
             List<String> distinctGroups = ejbGroupsFacade.getGroups();
             if (distinctGroups != null) {
             distinctGroups.remove("DEVELOPER");
             for (String g : distinctGroups) {
             customerGroupsList.add(new Groups(0, g));
             }
             checkedGroups = new Boolean[distinctGroups.size()];
             for(int c = 0;c < distinctGroups.size();c++){
             for(Groups g : selectedGroups){
             if(distinctGroups.get(c).contains(g.getGroupname())){
             checkedGroups[c] = true;
             }
             }
             } 
             }*/
            FacesContext context = FacesContext.getCurrentInstance();
            EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
            //controller.setSelectedCustomer(cust);
            controller.setCurrentCustomerDetails(null);
            controller.clearCustomerProvisionedInPaymentGW();
            controller.setRefreshIFrames(true);
            futureMap.cancelFutures(controller.getSessionId());
            controller.setWaitingForPaymentDetails(false);
            // setAsyncOperationRunning(false);
            // if(cust.getPaymentParametersId().getStatusCode().startsWith("D") || cust.getPaymentParametersId().getStatusCode().isEmpty()){
            controller.getCustDetailsFromEzi();
            // }
            if (controller.isTheCustomerProvisionedInThePaymentGateway()) {
                // controller.getCustDetailsFromEzi();

                controller.getPayments(18, 2);
            } else {
                controller.setCustomerDetailsHaveBeenRetrieved(true);
            }
            controller.setProgress(0);
            //eziDebitPaymentGatewayController.setSelectedCustomer(cust);
            SuppliersController suppliersController = (SuppliersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "suppliersController");
            Suppliers sup = null;
            if (cust.getSuppliersCollection() != null) {

                if (cust.getSuppliersCollection().size() >= 1) {
                    Iterator<Suppliers> i = cust.getSuppliersCollection().iterator();
                    if (i.hasNext()) {
                        sup = i.next();
                    }
                }
                if (cust.getSuppliersCollection().size() > 1) {
                    LOGGER.log(Level.WARNING, "There should only be one Supplier allocated to one customer. Username = {0}", new Object[]{cust.getUsername()});
                }

                if (sup != null) {
                    suppliersController.setSelected(sup);

                }
            }

            if (sup == null) {

                if (isCustomerInRole(cust, "TRAINER")) {
                    LOGGER.log(Level.INFO, "Creating  new supplier details for contractor as it wasn't found in the DB. Contractor username:{0}", new Object[]{cust.getUsername()});
                    sup = new Suppliers(0);
                    sup.setSupplierName(cust.getFirstname() + "" + cust.getLastname());
                    sup.setDescription("Internal Contractor");
                    sup.setInternalContractorId(cust);
                    sup.setSupplierCompanyNumber(" ");
                    sup.setSupplierCompanyNumberType("ABN");
                    suppliersFacade.create(sup);
                    suppliersController.setSelected(sup);
                } else {
                    suppliersController.setSelected(null);
                    suppliersController.setSelectedContractorRate(null);
                    suppliersController.setRateItems(null);
                    suppliersController.setSessionTypesArray(null);
                }
            }
            MySessionsChart1 c2 = context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
            c2.setSelectedCustomer(cust);
            //mySessionsChart1Controller.setSelectedCustomer(cust);
            selectedItemIndex = -1;
            checkPass = current.getPassword();
            if (cust.getQuestionnaireMapCollection() == null) {
                //addQuestionnaireMapItemsToCustomer(cust);
                LOGGER.log(Level.SEVERE, "The customers questionaire collection is NULL. This shouldn't happen as it is created when the customer is created.", cust.getUsername());
            }

            if (cust.getProfileImage() == null) {
                createDefaultCustomerProfilePicture(cust);
            }
            recreateAllAffectedPageModels();
            setCustomerTabsEnabled(true);
            RequestContext.getCurrentInstance().execute("updatePaymentForms();");

        }
    }

    private void addQuestionnaireMapItemsToCustomer(Customers cust) {

        List<Surveys> surveyList = surveysFacade.findAll();
        String defaulSurveyName = configMapFacade.getConfig("DefaultSurveyName");
        List<QuestionnaireMap> qmc = new ArrayList<>(cust.getQuestionnaireMapCollection());
        for (Surveys s : surveyList) {

            boolean foundSurveyMap = false;
            boolean isDefaultSurvey = false;
            if (s.getName().contentEquals(defaulSurveyName)) {
                isDefaultSurvey = true;
            }

            //Collection<QuestionnaireMap> qmc = cust.getQuestionnaireMapCollection();
            if (qmc != null) {
                if (qmc.isEmpty() == false) {
                    for (QuestionnaireMap qm : qmc) {
                        if (qm.getSurveysId().getId().compareTo(s.getId()) == 0) {
                            foundSurveyMap = true;
                        }
                    }
                }

            }
            if (foundSurveyMap == false) {
                //List<QuestionnaireMap> maps = ;

                QuestionnaireMap qmNew = new QuestionnaireMap(0, isDefaultSurvey);
                qmNew.setCustomerId(cust);
                qmNew.setSurveysId(s);
                qmNew.setQuestionnaireCompleted(false);
                qmc.add(qmNew);
                cust.setQuestionnaireMapCollection(qmc);
                //questionnaireMapFacade.create(qmNew);
                //ejbFacade.edit(cust);

                LOGGER.log(Level.INFO, "A new QuestionnaireMap was created for survey  {1} and Customer {0}. Is Default {2}", new Object[]{cust.getUsername(), s.getName(), isDefaultSurvey});
            }

        }
        cust.setQuestionnaireMapCollection(qmc);
        ejbFacade.edit(cust);

    }

    public void sendPaymentFormEmail() {
        FacesContext context = FacesContext.getCurrentInstance();
        LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
        controller.doPasswordReset("system.email.admin.paymentForm.template", current, configMapFacade.getConfig("sendPaymentFormEmailSubject"));
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("sendPaymentFormEmailSuccessMessage") + " " + current.getFirstname() + " " + current.getLastname() + ".");
        String auditDetails = "Customer Payment Form Email Sent For:" + current.getFirstname() + " " + current.getLastname() + ".";
        String changedFrom = "N/A";
        String changedTo = "On Board Email Sent";

        try {
            String url = current.getPaymentParametersId().getWebddrUrl();
            int a = url.indexOf("rAmount");
            int b = url.indexOf("businessOrPerson");
            url = url.substring(a, b);
            auditDetails += "\r\n" + url;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "sendPaymentFormEmail: Couldn't get the payment details from the webDDR Url for the audit log and cutomer notes");
        }
        addCustomerToUsersGroup(current);
        createCombinedAuditLogAndNote(loggedInUser, current, "sendPaymentFormEmail", auditDetails, changedFrom, changedTo);
    }

    public void sendEmailToCustomerFromTemplate() {
        FacesContext context = FacesContext.getCurrentInstance();
        LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
        EmailTemplatesController emailTemplatescontroller = (EmailTemplatesController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "emailTemplatesController");
        controller.doPasswordReset(emailTemplatescontroller.getSelected().getName(), current, emailTemplatescontroller.getSelected().getSubject());
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("sendEmailToCustomerFromTemplate") + " " + current.getFirstname() + " " + current.getLastname() + ".");
        String auditDetails = "Customer Email Sent For:" + current.getFirstname() + " " + current.getLastname() + ".Template Used = " + emailTemplatescontroller.getSelected().getName();
        String changedFrom = "N/A";
        String changedTo = "Email Sent to Customer";

        try {
            String url = current.getPaymentParametersId().getWebddrUrl();
            int a = url.indexOf("rAmount");
            int b = url.indexOf("businessOrPerson");
            url = url.substring(a, b);
            auditDetails += "\r\n" + url;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "sendCustomerTemplateEmail: Couldn't get the payment details from the webDDR Url for the audit log and cutomer notes");
        }
        addCustomerToUsersGroup(current);
        createCombinedAuditLogAndNote(loggedInUser, current, "sendCustomerTemplateEmail", auditDetails, changedFrom, changedTo);
    }

    public void sendCustomerOnboardEmail() {
        FacesContext context = FacesContext.getCurrentInstance();
        LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
        controller.doPasswordReset("system.email.admin.onboardcustomer.template", current, configMapFacade.getConfig("sendCustomerOnBoardEmailEmailSubject"));
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("sendCustomerOnBoardEmail") + " " + current.getFirstname() + " " + current.getLastname() + ".");
        String auditDetails = "Customer On Board Email Sent For:" + current.getFirstname() + " " + current.getLastname() + ".";
        String changedFrom = "N/A";
        String changedTo = "On Board Email Sent";

        try {
            String url = current.getPaymentParametersId().getWebddrUrl();
            int a = url.indexOf("rAmount");
            int b = url.indexOf("businessOrPerson");
            url = url.substring(a, b);
            auditDetails += "\r\n" + url;
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "sendCustomerOnboardEmail: Couldn't get the payment details from the webDDR Url for the audit log and cutomer notes");
        }
        addCustomerToUsersGroup(current);
        createCombinedAuditLogAndNote(loggedInUser, current, "sendCustomerOnboardEmail", auditDetails, changedFrom, changedTo);
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
            ejbFacade.edit(customer);
            notesFilteredItems = null;
            notesItems = null;

            //als.add("growl");
            RequestContext rc = RequestContext.getCurrentInstance();
            if (rc != null) {
                ArrayList<String> als = new ArrayList<>();
                als.add("@(.updateNotesDataTable)");
                rc.update(als);
            }
            //  RequestContext.getCurrentInstance().update("@(.updateNotesDataTable)");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Customers Controller, createCombinedAuditLogAndNote: ", e);
        }

    }

    public void refreshSelectedCustomerFromDB() {
        if (current != null) {
            current = ejbFacade.findById(current.getId());
        }
    }

    public Customers getSelected() {
        if (current == null) {
            String message = "The Customer Object is Null. Setting it to the ID of the logged in user";
            Logger.getLogger(getClass().getName()).log(Level.INFO, message);
            getLoggedInCustomer();
            selectedItemIndex = -1;
        }
        return current;
    }

    public Customers setCustomerDefaults(Customers c) {

        try {
            c.setCountryId(Integer.parseInt(configMapFacade.getConfig("default.customer.details.countryId")));
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Number Format Exception for Country ID in customer deaults.Check config map entry for default.customer.details.countryId and value {0}", configMapFacade.getConfig("default.customer.details.countryId"));
        }
        c.setCity(configMapFacade.getConfig("default.customer.details.city"));
        c.setStreetAddress(configMapFacade.getConfig("default.customer.details.street"));
        c.setAddrState(configMapFacade.getConfig("default.customer.details.state"));
        c.setSuburb(configMapFacade.getConfig("default.customer.details.suburb"));
        c.setPostcode(configMapFacade.getConfig("default.customer.details.postcode"));
        try {
            c.setReferredby(Integer.parseInt(configMapFacade.getConfig("default.customer.details.referredby")));
        } catch (NumberFormatException e) {
            LOGGER.log(Level.SEVERE, "Number Format Exception for Referred by customer ID in customer deaults.Check config map entry for default.customer.details.referredby and value {0}", configMapFacade.getConfig("default.customer.details.referredby"));
        }

        //for debug
        c.setUsername(configMapFacade.getConfig("default.customer.details.username"));
        c.setFirstname(configMapFacade.getConfig("default.customer.details.firstname"));
        c.setLastname(configMapFacade.getConfig("default.customer.details.lastname"));
        c.setPassword(PasswordService.getInstance().encrypt(RandomString.generateRandomString(new Random(), 10)));
        c.setEmailAddress(configMapFacade.getConfig("default.customer.details.email"));
        c.setFax(configMapFacade.getConfig("default.customer.details.fax"));
        c.setTelephone(configMapFacade.getConfig("default.customer.details.mobile"));
        c.setEmergencyContactName("Not Provided");
        c.setEmergencyContactPhone("Not Provided");
        c.setGender('F');
        c.setNewsletter(true);
        c.setLoginAttempts(0);
        c.setLastLoginTime(null);
        c.setMustResetPassword(true);
        c.setGroupPricing(ejbPlanFacade.findPLanByName(configMapFacade.getConfig("default.customer.plan")));
        try {
            c.setEmailFormat(ejbEmailFormatFacade.findAll().get(Integer.parseInt(configMapFacade.getConfig("default.customer.details.emailformat"))));
            c.setAuth(ejbCustomerAuthFacade.findAll().get(Integer.parseInt(configMapFacade.getConfig("default.customer.details.auth"))));
            c.setActive(ejbCustomerStateFacade.findAll().get(Integer.parseInt(configMapFacade.getConfig("default.customer.details.active"))));
            c.setTermsConditionsAccepted(false);
            c.setPreferredContact(ejbPreferedContactFacade.findAll().get(Integer.parseInt(configMapFacade.getConfig("default.customer.details.prefferedcontact"))));
            c.setDemographic(ejbDemoFacade.findAll().get(Integer.parseInt(configMapFacade.getConfig("default.customer.details.demographic"))));

        } catch (NumberFormatException numberFormatException) {
            LOGGER.log(Level.SEVERE, "Number Format Exception for customer defaults.Check config map entry for default.customer.details.xxxx and a numeric value ");

        }

        GregorianCalendar gc = new GregorianCalendar();
        //gc.add(Calendar.YEAR, -18);
        gc.setTimeInMillis(0);
        gc.set(1800, 1, 1);//set to default date  1/1/1800

        c.setDob(gc.getTime());
        return c;
    }

    private CustomersFacade getFacade() {
        return ejbFacade;
    }

    public DatatableSelectionHelper getPagination() {
        if (pagination == null) {
            pagination = new DatatableSelectionHelper() {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public PfSelectableDataModel<Customers> createPageDataModel() {

                    return new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(true, "firstname", selectedCustomerStates, selectedCustomerTypes, true));

                }

            };
        }
        return pagination;
    }

    public DatatableSelectionHelper getNotesPagination() {
        if (notesPagination == null) {
            notesPagination = new DatatableSelectionHelper() {

                @Override
                public int getItemsCount() {
                    return ejbNotesFacade.countNotesByCustomer(getSelected());
                }

                @Override
                public PfSelectableDataModel<Notes> createPageDataModel() {
                    return new PfSelectableDataModel<>(ejbNotesFacade.findNotesByCustomer(getSelected()));
                }

            };
        }
        return notesPagination;
    }

    /**
     * ListDataModel(getFacade().findAll());
     *
     * @return the filteredItems
     */
    public List<Customers> getFilteredItems() {
        /* if(filteredItems==null){
         filteredItems = new ArrayList<>();
         }*/
        if (filteredItems != null) {
            LOGGER.log(Level.FINE, "FILTERED ITEMS SIZE", filteredItems.size());
        }
        LOGGER.log(Level.FINE, "GET FILTERED ITEMS", filteredItems);
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Customers> filteredItems) {
        this.filteredItems = filteredItems;
    }

    protected void createDefaultPaymentParametersFromDetached(int custId) {
        Customers cust = ejbFacade.find(custId);
        ejbFacade.edit(cust);
        createDefaultPaymentParameters(cust);
    }

    protected void createDefaultPaymentParameters(Customers cust) {

        if (cust == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer is NULL.");
            return;
        }
        if (cust.getId() == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer.getId is NULL.");
            return;
        }
        //ejbFacade.edit(cust); 

        if (cust.getId() == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Could not look up Customers using find.");
            return;
        }
        PaymentParameters pp = cust.getPaymentParametersId();

        if (pp == null) {
            pp = ejbPaymentParametersFacade.findPaymentParametersByCustomer(cust);
            if (pp == null) {
                try {

                    String phoneNumber = cust.getTelephone();
                    if (phoneNumber == null) {
                        phoneNumber = "0000000000";
                        LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
                    }
                    Pattern p = Pattern.compile("\\d{10}");
                    Matcher m = p.matcher(phoneNumber);
                    //ezidebit requires an australian mobile phone number that starts with 04
                    if (m.matches() == false || phoneNumber.startsWith("04") == false) {
                        phoneNumber = "0000000000";
                        LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", cust.getUsername());
                    }
                    pp = new PaymentParameters();
                    pp.setId(0);
                    pp.setWebddrUrl(null);
                    pp.setCustomers(cust);
                    pp.setLastSuccessfulScheduledPayment(ejbPaymentsFacade.findLastSuccessfulScheduledPayment(cust));
                    pp.setNextScheduledPayment(ejbPaymentsFacade.findNextScheduledPayment(cust));
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
                    pp.setStatusCode("D");
                    pp.setStatusDescription("DEFAULT NOT PROVISIONED");
                    pp.setTotalPaymentsFailed(0);
                    pp.setTotalPaymentsFailedAmount(new BigDecimal(0));
                    pp.setTotalPaymentsSuccessful(0);
                    pp.setTotalPaymentsSuccessfulAmount(new BigDecimal(0));
                    pp.setYourGeneralReference("");
                    pp.setYourSystemReference("");
                    ejbPaymentParametersFacade.createAndFlushForGeneratedIdEntities(pp);

                    cust.setPaymentParametersId(pp);
                    //ejbFacade.edit(cust);
                    //ejbFacade.addPaymentParameters(cust.getId(), pp);
                    LOGGER.log(Level.INFO, "CC Default Payment Parameters CREATED and Flushed to DB. Customer ID {1}, Username {2},PP ID {3}, PP Status Code {0}.", new Object[]{cust.getPaymentParametersId().getStatusCode(), cust.getId(), cust.getUsername(), cust.getPaymentParametersId().getId()});

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "createDefaultPaymentParameters Method in Customers Controller", e);
                }
            } else {
                //cust.setPaymentParametersId(pp);
                //ejbFacade.editAndFlush(current);
                LOGGER.log(Level.WARNING, "CC Default Payment Parameters already existed but were not referenced by this customer in JPA context. Customer ID {1}, Username {2},PP ID {3}, PP Status Code {0}.", new Object[]{cust.getPaymentParametersId().getStatusCode(), cust.getId(), cust.getUsername(), cust.getPaymentParametersId().getId()});
                ejbFacade.addPaymentParameters(cust.getId(), pp);
            }
        } else {
            LOGGER.log(Level.WARNING, "createDefaultPaymentParameters - The payment parameters have already been created for this customer", cust.getUsername());
        }
    }

    protected PaymentParameters getCustomersPaymentParameters(Customers cust) {
        if (cust != null) {
            PaymentParameters pp = cust.getPaymentParametersId();
            if (pp == null) {
                createDefaultPaymentParameters(cust);
            }
            pp = cust.getPaymentParametersId();
            if (pp == null) {
                LOGGER.log(Level.SEVERE, " Customer {0} has NULL Payment parameters.", new Object[]{cust.getUsername()});
            }
            return pp;
        } else {
            LOGGER.log(Level.SEVERE, " getCustomersPaymentParameters Customer is  NULL Payment .");
            return null;
        }

    }

    protected PaymentParameters getSelectedCustomersPaymentParameters() {
        return getCustomersPaymentParameters(getSelected());

    }

    public boolean getPaymentParametersSmsPaymentReminder() {
        return !getSelectedCustomersPaymentParameters().getSmsPaymentReminder().toUpperCase().contains("NO");
    }

    public boolean getPaymentParametersSmsExpiredCard() {
        return !getSelectedCustomersPaymentParameters().getSmsExpiredCard().toUpperCase().contains("NO");
    }

    public boolean getPaymentParametersSmsFailedNotification() {
        return !getSelectedCustomersPaymentParameters().getSmsFailedNotification().toUpperCase().contains("NO");
    }

    public void setPaymentParametersSmsPaymentReminder(boolean param) {
        String converted = "NO";
        if (param) {
            converted = "YES";
        }
        PaymentParameters pp = getSelectedCustomersPaymentParameters();
        pp.setSmsPaymentReminder(converted);
        ejbPaymentParametersFacade.edit(pp);
    }

    public void setPaymentParametersSmsExpiredCard(boolean param) {
        String converted = "NO";
        if (param) {
            converted = "YES";
        }
        PaymentParameters pp = getSelectedCustomersPaymentParameters();
        pp.setSmsExpiredCard(converted);
        ejbPaymentParametersFacade.edit(pp);
    }

    public void setPaymentParametersSmsFailedNotification(boolean param) {
        String converted = "NO";
        if (param) {
            converted = "YES";
        }
        PaymentParameters pp = getSelectedCustomersPaymentParameters();
        pp.setSmsFailedNotification(converted);
        ejbPaymentParametersFacade.edit(pp);
    }

    /**
     * @return the multiSelected
     */
    public Customers[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Customers[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public void impersonateUser(ActionEvent event) {
        String user = getSelected().getUsername();
        setSelected(impersonate);
        setImpersonating(true);
        recreateAllAffectedPageModels();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ImpersonateCustomer") + getSelected().getUsername());
        String message = "Impersonated User: " + getSelected().getUsername() + " from  " + user;
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);

    }

    public void updateASingleCustomersPaymentInfo(Customers cust) {
        if (items != null) {
            List<Customers> lp = (List<Customers>) items.getWrappedData();
            int index = lp.indexOf(cust);
            if (index == -1) {
                lp.add(cust);
            } else {
                lp.set(index, cust);
            }
        }
        if (filteredItems != null) {
            int index = filteredItems.indexOf(cust);
            if (index == -1) {
                filteredItems.add(cust);
            } else {
                filteredItems.set(index, cust);
            }
        }

    }

    private void recreateAllAffectedPageModels() {
        FacesContext context = FacesContext.getCurrentInstance();
        // recreate any datatables that have stale data after changing users
        String message = "Recreating data models for user : " + getSelected().getUsername() + ". ";
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);
        recreateModel();
        SessionHistoryController c1 = context.getApplication().evaluateExpressionGet(context, "#{sessionHistoryController}", SessionHistoryController.class);
        c1.recreateModel();
        MySessionsChart1 c2 = context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
        c2.recreateModel();
        EziDebitPaymentGateway c3 = context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);
        c3.recreateModels();

        CustomerImagesController c4 = context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
        c4.recreateModel();
        ChartController c5 = context.getApplication().evaluateExpressionGet(context, "#{chartController}", ChartController.class);
        c5.recreateModel();
        SurveyAnswersController sac = context.getApplication().evaluateExpressionGet(context, "#{surveyanswersController}", SurveyAnswersController.class);
        sac.clearSurveyAnswers();
    }

    public void handleUserChange() {
        JsfUtil.addSuccessMessage("User Selected: " + impersonate);
    }

    public void unimpersonateUser(ActionEvent event) {
        String user = getSelected().getUsername();
        getLoggedInCustomer();
        setImpersonate(getSelected());
        setImpersonating(false);
        recreateAllAffectedPageModels();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("UnimpersonateCustomer") + getSelected().getUsername());
        String message = "Unimpersonated User : " + user + " and changed back to " + getSelected().getUsername();
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);

    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        // current = (Customers) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        setSelected(setCustomerDefaults(new Customers()));
        selectedItemIndex = -1;
        return "Create";
    }

    public void signUp(ActionEvent actionEvent) {

        setNewCustomer(setCustomerDefaults(new Customers()));

        LOGGER.log(Level.INFO, "customersController  Sign Up Button Clicked, prepare create called. Returning to signup xhtml");

    }

    public void prepareCreateAjax(ActionEvent actionEvent) {
        //setLastSelected(current);
        setNewCustomer(setCustomerDefaults(new Customers()));
        //selectedItemIndex = -1;
        //RequestContext.getCurrentInstance().update("formCustomersCreate1");
        //RequestContext.getCurrentInstance().openDialog("customersCreateDialogue");
    }

    private void createDefaultCustomerProfilePicture(Customers c) {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomerImagesController custImageCon = context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
        custImageCon.createDefaultProfilePic(c);

    }

    public String create() {
        try {
            FacesContext context = FacesContext.getCurrentInstance();

            Customers c = getSelected();
            c.setId(0);
            c.setPassword(PasswordService.getInstance().encrypt(c.getPassword()));

            if (getFacade().find(c.getId()) == null) {
                getFacade().create(c);
                Groups grp = new Groups(0, "USER");
                grp.setUsername(c);
                ejbGroupsFacade.create(grp);
                createDefaultCustomerProfilePicture(c);
                createDefaultPaymentParameters(c);
                addQuestionnaireMapItemsToCustomer(c);
                recreateModel();
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersCreated"));
                return prepareCreate();
            } else {
                JsfUtil.addErrorMessage("The user you are trying to create (" + c.getUsername() + ") already exists in the database.");
                return null;
            }

        } catch (Exception e) {
            String cause = e.getCause().getCause().getMessage();
            if (cause.toLowerCase().contains("duplicate")) {
                JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("DuplicateCustomerExists"));
                return null;
            } else {
                JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
                return null;
            }
        }
    }

    //lastSelected
    public void cancelCreateDialogue(ActionEvent actionEvent) {
        setSelected(lastSelected);
    }

    private boolean validateNewSignup(String ipAddress, String emailAddress) {
        //TODO - this is still wide open , need a better strategy to stop evil spammers. MAybe recaptcha is a better option.
        Customers c = ejbFacade.findCustomerByEmail(emailAddress);
        int id = -1;
        if (c != null) {
            id = c.getId();
        }
        FacesContext context = FacesContext.getCurrentInstance();
        ApplicationBean appBean = context.getApplication().evaluateExpressionGet(context, "#{applicationBean}", ApplicationBean.class);
        if (appBean.validateIP(ipAddress) == false) {
            // validation failed.multiple attempts fron the same IP in the past hour
        }
        LOGGER.log(Level.INFO, "validateNewSignup: ip:{0}, email:{1}, customerId if found:{2}", new Object[]{ipAddress, emailAddress, id});
// TODO change this to != null when finished testing to stop the same email signing up twice
        return c == null;
    }

    public void createLeadFromSignup(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.isValidationFailed() == false) {
            createFromUnauthenticated("LEAD", getNewCustomer(), leadComments, getHttpServletRequestFromFacesContext(), false);
        } else {
            JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("SignUpValidationFailed"));
        }
    }

    public void createLeadFromWebservice(Customers c, String message, HttpServletRequest request) {
        createFromUnauthenticated("LEAD", c, message, request, true);
    }

    public void createFromSignup(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        if (context.isValidationFailed() == false) {
            createFromUnauthenticated("USER", getNewCustomer(), leadComments, getHttpServletRequestFromFacesContext(), false);
        } else {
            JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("SignUpValidationFailed"));
        }
    }

    private HttpServletRequest getHttpServletRequestFromFacesContext() {
        FacesContext context = FacesContext.getCurrentInstance();
        return (HttpServletRequest) context.getExternalContext().getRequest();

    }

    @TransactionAttribute(REQUIRES_NEW)
    private void createFromUnauthenticated(String group, Customers c, String message, HttpServletRequest request, boolean isWebserviceCall) {

        //TODO validate email address to ensure spammers can't take down site
        //max 3 emails 
        boolean validIP = true;
        String ipAddress = "";
        if (isWebserviceCall == false) {

            ipAddress = request.getHeader("X-FORWARDED-FOR");
            if (ipAddress == null) {
                ipAddress = request.getRemoteAddr();
            }
            if (ipAddress == null) {
                ipAddress = "";
            } else {
                validIP = validateNewSignup(ipAddress, c.getEmailAddress());
            }
        }
        if (validIP == true) {
            c.setId(0);
            Groups grp = new Groups(0, group);
            //Check if they already Exist
            Customers custCheck = ejbFacade.findCustomerByName(c.getFirstname().trim(), c.getLastname().trim());
            if (custCheck != null) {
                sendNotificationEmail(c, grp, "system.email.notification.template", "New LEAD from website, customer already exists in DB. They may be cancelled or on-hold.", message);
            } else if (group.contains("LEAD")) {
                // new lead from contact form

                c.setUsername(getUniqueUsername(c.getFirstname().trim() + "." + c.getLastname().trim()));
                getFacade().createAndFlushForGeneratedIdEntities(c);
                createDefaultPaymentParametersFromDetached(c.getId());
                //createDefaultPaymentParameters(c);
                grp.setUsername(c);
                List<Groups> gl = new ArrayList<>();
                gl.add(grp);
                c.setGroupsCollection(gl);
                getFacade().edit(c);
                //ejbGroupsFacade.create(grp);
                if (isWebserviceCall == false) {
                    createDefaultCustomerProfilePicture(c);
                }
                Notes nt = new Notes(0);
                nt.setNote(message);
                nt.setUserId(c);
                nt.setCreatedBy(c);
                if (isWebserviceCall == false) {
                    addToNotesDataTableLists(nt);
                }
                String details = "New LEAD generated: Name: " + c.getFirstname() + ", <br/>Email:  " + c.getEmailAddress() + ", <br/>Phone:   " + c.getTelephone() + ", <br/>Username:   " + c.getUsername() + ", <br/>Group:   " + group + ", IP Address:" + ipAddress + ", Message:" + message;
                sendNotificationEmail(c, grp, "system.email.notification.template", "New LEAD from website", message);
                createCombinedAuditLogAndNote(c, c, "New Lead", details, "Did Not Exist", "New Lead");
                LOGGER.log(Level.INFO, "createFromLead: {0}", new Object[]{details});
                if (isWebserviceCall == false) {
                    RequestContext.getCurrentInstance().execute("PF('signupDialog').hide();");
                    JsfUtil.addSuccessMessage("Info", configMapFacade.getConfig("LeadSignupSuccessfull"));
                    setLeadFormSubmitted(true);
                }

            } else {
                // new signup
                // this cant be done from teh webservice due to contect lookup below
                getFacade().create(c);
                grp.setUsername(c);
                List<Groups> gl = new ArrayList<>();
                gl.add(grp);
                c.setGroupsCollection(gl);
                getFacade().edit(c);
                //ejbGroupsFacade.create(grp);
                createDefaultCustomerProfilePicture(c);
                String details = "Name: " + c.getFirstname() + ", Email:" + c.getEmailAddress() + ", Phone:" + c.getTelephone() + ", Username:" + c.getUsername() + ", Group:" + group + ", IP Address:" + ipAddress + ".Customer Onboard email sent";
                FacesContext context = FacesContext.getCurrentInstance();
                //SurveysController surveyCon = context.getApplication().evaluateExpressionGet(context, "#{surveysController}", SurveysController.class);
                // SurveysController surveyCon = (SurveysController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "surveysController");
                LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
                controller.doPasswordReset("system.email.admin.onboardcustomer.template", c, configMapFacade.getConfig("sendCustomerOnBoardEmailEmailSubject"));
                createCombinedAuditLogAndNote(c, c, "New Sign Up", details, "Did Not Exist", "New Lead");
                LOGGER.log(Level.INFO, "createFromSignup: {0}", new Object[]{details});
                RequestContext.getCurrentInstance().execute("PF('signupDialog').hide();");
                JsfUtil.addSuccessMessage("Info", configMapFacade.getConfig("SignUpSuccessfulFailed"));
                setSignupFormSubmittedOK(true);
                PaymentParameters pp = getCustomersPaymentParameters(c);
                if (pp == null) {
                    LOGGER.log(Level.WARNING, "createFromSignup: Failed to create payement parameters. Null returned from call to getSelectedCustomersPaymentParameters()");

                }

            }
            setNewCustomer(setCustomerDefaults(new Customers()));
            addQuestionnaireMapItemsToCustomer(c);
        } else {
            JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("SignUpValidationEmailExistsFailed"));
        }

    }

    private String getUniqueUsername(String name) {

        String newUsername = name.trim().toLowerCase().replace(' ', '_');
        boolean usernameValid = false;
        int number = 1;
        while (!usernameValid) {
            Customers cust = getFacade().findCustomerByUsername(newUsername);
            if (cust != null) {
                newUsername += number;
                number++;

            } else {
                usernameValid = true;
            }
        }
        return newUsername;
    }

    public void sendNotificationEmail(Customers c, Groups group, String templateName, String subject, String comments) {

        //send email
        String templateNamePlaceholder = configMapFacade.getConfig("system.email.notification.templateLinkPlaceholder");
        String templateCommentsPlaceholder = configMapFacade.getConfig("system.email.notification.templateCommentsPlaceholder");
        String templatePhonePlaceholder = configMapFacade.getConfig("system.email.notification.templatePhonePlaceholder");
        String templateEmailPlaceholder = configMapFacade.getConfig("system.email.notification.templateEmailPlaceholder");

        //String htmlText = configMapFacade.getConfig(templateName);
        String htmlText = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();
        String name = c.getFirstname() + " " + c.getLastname();
        String phone = c.getTelephone();
        String email = c.getEmailAddress();
        htmlText = htmlText.replace(templateNamePlaceholder, name);
        htmlText = htmlText.replace(templateCommentsPlaceholder, comments);
        htmlText = htmlText.replace(templatePhonePlaceholder, phone);
        htmlText = htmlText.replace(templateEmailPlaceholder, email);

        Future<Boolean> emailSendResult = ejbPaymentBean.sendAsynchEmail(configMapFacade.getConfig("EmailNotificationTOEmailAddress"), configMapFacade.getConfig("EmailNotificationCCEmailAddress"), configMapFacade.getConfig("EmailNotificationFromEmailAddress"), subject, htmlText, null, emailServerProperties(), false);

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

    public void createDialogue(ActionEvent actionEvent) {
        createFromListener();
    }

    private void createFromListener() {
        FacesContext context = FacesContext.getCurrentInstance();
        Customers c = getNewCustomer();

        EziDebitPaymentGateway ezi = context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);
        // SurveysController surveyCon = context.getApplication().evaluateExpressionGet(context, "#{surveysController}", SurveysController.class);

        if (c.getId() == null || getFacade().find(c.getId()) == null) {
            // does not exist so create a new customer
            try {
                c.setId(0);
                getFacade().create(c);
                setSelected(c);
                updateCustomersGroupMembership(c);
                addQuestionnaireMapItemsToCustomer(c);
                createDefaultCustomerProfilePicture(c);
                // createDefaultPaymentParameters(paymentGateway);
                setSelected(c);

                setNewCustomer(setCustomerDefaults(new Customers()));
                PaymentParameters pp = getCustomersPaymentParameters(c);
                if (pp == null) {
                    LOGGER.log(Level.WARNING, "createFromSignup: Failed to create payement parameters. Null returned from call to getSelectedCustomersPaymentParameters()");

                }

                JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersCreated"));
            } catch (Exception e) {
                String cause = e.getCause().getCause().getMessage();
                if (cause.toLowerCase().contains("duplicate")) {
                    JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("DuplicateCustomerExists"));
                } else {
                    JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
                }
            }
        } else {
            // exists so update only
            try {
                getFacade().edit(c);
                updateCustomersGroupMembership(c);

                ezi.editCustomerDetailsInEziDebit(c);
                recreateAllAffectedPageModels();
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("ChangesSaved"));
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e.getMessage(), configMapFacade.getConfig("PersistenceErrorOccured"));
            }

        }

    }

    private void updateCustomersGroupMembership(Customers c) {
        //modify customers groups
        try {
            List<Groups> lg = new ArrayList<>(c.getGroupsCollection());
            ejbFacade.clearAllCustomerGroups(c);

            // check if they are in the hidden developer group
            int count = 0;

            for (Groups g : lg) {
                if (g.getGroupname().contains("DEVELOPER")) {
                    if (count == 0) {
                        c.getGroupsCollection().add(g);
                        count++;
                    }
                }
            }
            // add selected groups
            for (Groups g : selectedGroups) {

                Groups grp = new Groups(0, g.getGroupname());
                grp.setUsername(c);
                ejbFacade.addCustomerToGroup(c, grp);
            }

            //Groups[] grps = new Groups[c.getGroupsCollection().size()];
            //c.getGroupsCollection().toArray(grps);
            // have to use an iterator as we are removing components
            /*       Collection<Groups> customersExistingGroups = c.getGroupsCollection();
            if (customersExistingGroups != null) {
                Iterator<Groups> i = customersExistingGroups.iterator();
                while (i.hasNext()) {
                    // for (int i = grps.length -1; i >= 0; i--) {
                    Groups g = i.next();
                    boolean exists = false;
                    for (Groups sg : selectedGroups) {
                        if (sg.getGroupname().trim().equalsIgnoreCase(g.getGroupname().trim())) {
                            exists = true;
                        }
                    }
                    if (exists == false) {
                        c.getGroupsCollection().remove(g);
                        //ejbGroupsFacade.remove(g);
                    }
                }
            }
            customersExistingGroups = c.getGroupsCollection();
            for (Groups g : selectedGroups) {
                boolean exists = false;
                if (customersExistingGroups != null) {
                    for (Groups eg : customersExistingGroups) {
                        if (eg.getGroupname().trim().equalsIgnoreCase(g.getGroupname().trim())) {
                            exists = true;
                        }
                    }
                }
                if (exists == false) {
                    Groups grp = new Groups(0, g.getGroupname());
                    grp.setUsername(c);
                    // ejbGroupsFacade.create(grp);
                    c.getGroupsCollection().add(grp);

                }
            }*/
            // the customer must be a member of the user base group to login
            // ejbFacade.edit(c);
            //addCustomerToUsersGroup(c);
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "updateCustomersGroupMembership method exception: {0} : {1}", new Object[]{c.getUsername(), e.getMessage()});
        }
    }

    protected void addCustomerToUsersGroup(Customers c) {
        List<Groups> lg = new ArrayList<>(c.getGroupsCollection());
        List<Groups> removalList = new ArrayList<>();
        int count = 0;
        for (Groups g : lg) {
            if (g.getGroupname().contains("USER")) {
                if (count > 0) {
                    removalList.add(g);
                }
                count++;
            }
        }
        for (Groups g : lg) {
            if (g.getGroupname().contains("LEAD")) {
                removalList.add(g);
                count++;
            }
        }

        Iterator<Groups> i = removalList.iterator();
        while (i.hasNext()) {
            Groups eg = i.next();
            c.getGroupsCollection().remove(eg);
        }

        if (count == 0) {
            Groups grp = new Groups(0, "USER");
            grp.setUsername(c);

            ejbFacade.addCustomerToGroup(c, grp);
        }

    }

    public void editDialogue(ActionEvent actionEvent) {
        setNewCustomer(current);
        createFromListener();
    }

    public String prepareEdit() {
        //current = (Customers) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void groupChangedChangeListener() {
        JsfUtil.addSuccessMessage("Group Changed");
        selectedGroups = new ArrayList<>();
        if (checkedGroups != null) {
            for (int y = 0; y < checkedGroups.length; y++) {
                Boolean b = checkedGroups[y];
                if (b != null && b == true) {
                    selectedGroups.add(customerGroupsList.get(y));
                }
            }
        }
    }

    public void newCustomergroupChangedChangeListener() {
        JsfUtil.addSuccessMessage("Group Changed");
        selectedGroups = new ArrayList<>();
        if (newCustomerCheckedGroups != null) {
            for (int y = 0; y < newCustomerCheckedGroups.length; y++) {
                Boolean b = newCustomerCheckedGroups[y];
                if (b != null && b == true) {
                    selectedGroups.add(newCustomerGroupsList.get(y));
                }
            }
        }
    }

    public void selectShowCancelledBooleanChangeListener(ValueChangeEvent vce) {
        recreateModel();
    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void selectOneChangePlanListener(ValueChangeEvent vce) {
        Object newValueObject = vce.getNewValue();
        // Object oldValueObject = vce.getOldValue();
        Plan oldPlan = null;
        Plan newPlan = null;
        if (current.getGroupPricing() != null) {
            oldPlan = current.getGroupPricing();
        } else {
            LOGGER.log(Level.WARNING, "selectOneChangePlanListener: The old plan is null. This shouldn't happen!!");
            // set to default for new customers
            oldPlan = ejbPlanFacade.findPLanByName(configMapFacade.getConfig("default.customer.plan"));
        }
        if (newValueObject.getClass().equals(Plan.class)) {
            newPlan = (Plan) newValueObject;
        }
        if (newPlan == null) {
            LOGGER.log(Level.WARNING, "selectOneChangePlanListener: The new plan is null. The plan cannot be changed!");
            JsfUtil.addErrorMessage("Change Plan Failed. The new Plan is NULL");

        } else {
            LOGGER.log(Level.INFO, "selectOneChangePlanListener: Changing Plans from:{0} to {1}.", new Object[]{oldPlan.getPlanName(), newPlan.getPlanName()});
            setCustomersNewPlan(newPlan);
            //update the dialogue create schedule parameters

            //start date should be next scheduled payment if not default to tomorrow
            FacesContext context = FacesContext.getCurrentInstance();
            EziDebitPaymentGateway ezi = context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);

            PaymentParameters pp = current.getPaymentParametersId();
            GregorianCalendar cal = new GregorianCalendar();

            String newPaymentPeriod = newPlan.getPlanTimePeriod();
            double newPlanPrice = newPlan.getPlanPrice().doubleValue();
            double oldPlanPrice = oldPlan.getPlanPrice().doubleValue();
            if (pp != null && pp.getNextScheduledPayment() != null) {
                ezi.setPaymentDebitDate(pp.getNextScheduledPayment().getDebitDate());
                String oldPaymentPeriod = pp.getPaymentPeriod();
                if (oldPaymentPeriod.compareTo(newPaymentPeriod) != 0) {
                    //plan time periods do not match. Convert the plan to teh existing customers payment period 
                    //i.e if the customer likes to pay weekly and the plan is monthly, convert the monthly plan price to a weekly payment
                    double oldNumberOfDaysBilledPerPayment = convertPaymentPeriodToAverageDays(oldPaymentPeriod);
                    double newNumberOfDaysBilledPerPayment = convertPaymentPeriodToAverageDays(newPaymentPeriod);
                    double ratio = newNumberOfDaysBilledPerPayment / oldNumberOfDaysBilledPerPayment;
                    double newPriceForOldPeriod;
                    if (oldNumberOfDaysBilledPerPayment < newNumberOfDaysBilledPerPayment) {
                        //i.e 4 weekly (28 days) billing to weekly (7 days) plan price .. convert the weekly plan price to be billed monthly
                        newPriceForOldPeriod = newPlanPrice / ratio;

                    } else {
                        //i.e weekly to monthly
                        newPriceForOldPeriod = newPlanPrice * ratio;
                    }
                    // keep customers existing payement period preference . i.e if they pay weekly keep them on weekly
                    ezi.setPaymentSchedulePeriodType(oldPaymentPeriod);
                    ezi.setPaymentAmountInCents(Float.parseFloat(Double.toString(newPriceForOldPeriod)));
                    LOGGER.log(Level.INFO, "selectOneChangePlanListener: Converted price to suit customers old payment period:{0}, plan payment period  {1}, plan price={2}, new converted price={3}.", new Object[]{oldPaymentPeriod, newPaymentPeriod, newPlanPrice, newPriceForOldPeriod});
                } else {
                    // payment period are the same so no need to convert
                    ezi.setPaymentAmountInCents(Float.parseFloat(Double.toString(newPlanPrice)));
                    ezi.setPaymentSchedulePeriodType(newPlan.getPlanTimePeriod());

                    LOGGER.log(Level.INFO, "selectOneChangePlanListener: payment period are the same so no need to convert: Old={0}, new={1}.Old plan price = {2}, newplan price = {3}", new Object[]{oldPaymentPeriod, newPaymentPeriod, oldPlanPrice, newPlanPrice});

                }

            } else {

                cal.add(Calendar.DAY_OF_YEAR, 1);
                ezi.setPaymentDebitDate(cal.getTime());
                // BigDecimal amount = newPlan.getPlanPrice().multiply(BigDecimal.valueOf((long) 100));
                BigDecimal amount = newPlan.getPlanPrice();
                ezi.setPaymentAmountInCents(amount.floatValue());
                ezi.setPaymentSchedulePeriodType(newPlan.getPlanTimePeriod());
                LOGGER.log(Level.INFO, "selectOneChangePlanListener: No scheduled payments: Old Plan Name={0}, new payment period ={1} .Old plan price = {2}, newplan price = {3}", new Object[]{oldPlan.getPlanName(), newPaymentPeriod, oldPlanPrice, newPlanPrice});

            }

            ezi.setPaymentDayOfMonth(cal.get(Calendar.DAY_OF_MONTH));
            if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.TUESDAY
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.WEDNESDAY
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.THURSDAY
                    || cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) {
                ezi.setPaymentDayOfWeek(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));

            } else {
                if (cal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
                    cal.add(Calendar.DAY_OF_YEAR, 2);
                } else {
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
                ezi.setPaymentDayOfWeek(cal.getDisplayName(Calendar.DAY_OF_WEEK, Calendar.SHORT, Locale.US));
            }

            //show dialogue
            RequestContext.getCurrentInstance().execute("PF('changePlanDialogueWidget').show();");
            RequestContext.getCurrentInstance().update("changePlanDialogue");
        }
    }

    private double convertPaymentPeriodToAverageDays(String period) {
        GregorianCalendar cal = new GregorianCalendar();
        boolean isLeapYear = cal.isLeapYear(cal.get(Calendar.YEAR));

        if (period.compareTo(PaymentPeriod.WEEKLY.value()) == 0) {
            return (double) 7;
        }
        if (period.compareTo(PaymentPeriod.FORTNIGHTLY.value()) == 0) {
            return (double) 14;
        }
        if (period.compareTo(PaymentPeriod.FOUR_WEEKLY.value()) == 0) {
            return (double) 28;
        }
        if (period.compareTo(PaymentPeriod.MONTHLY.value()) == 0) {
            if (isLeapYear) {
                return 30.50;
            }
            return 30.42;
        }
        if (period.compareTo(PaymentPeriod.WEEKDAY_IN_MONTH.value()) == 0) {
            if (isLeapYear) {
                return 30.50;
            }
            return 30.42;
        }
        if (period.compareTo(PaymentPeriod.QUARTERLY.value()) == 0) {
            if (isLeapYear) {
                return 91.5;
            }
            return 91.25;
        }
        if (period.compareTo(PaymentPeriod.SIX_MONTHLY.value()) == 0) {
            if (isLeapYear) {
                return (double) 183;
            }
            return 182.5;
        }
        if (period.compareTo(PaymentPeriod.ANNUALLY.value()) == 0) {
            if (isLeapYear) {
                return (double) 366;
            }
            return (double) 365;
        }

        return -1;
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void update(ActionEvent event) {
        try {
            Customers selected = getSelected();
            getFacade().edit(selected);
            if (selected.getPaymentParametersId() != null) {
                if (selected.getPaymentParametersId().getWebddrUrl() != null) {
                    // update the customers details in the direct debit form. If they update their details online before they submit the form we need to update the form
                    // otherwise the address or other fields may  be blank
                    FacesContext context = FacesContext.getCurrentInstance();
                    EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
                    controller.updatePaymentScheduleForm();
                    controller.createEddrLink(event);
                }
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersUpdated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public void dev1(ActionEvent actionEvent) {

    }

    public void dev2(ActionEvent actionEvent) {
        RequestContext.getCurrentInstance().execute("updatePaymentForms();");
    }

    public void dev3(ActionEvent actionEvent) {
        RequestContext.getCurrentInstance().update("devForm:devEffectPanel");
    }

    public void changeCustomersState(ActionEvent actionEvent) {
        int count = 0;
        FacesContext context = FacesContext.getCurrentInstance();
        EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
        if (selectedState.getCustomerState().contains("CANCELLED") == true && controller.isCustomerCancellationConfirmed() == false) {
            RequestContext.getCurrentInstance().update("confirmCancellation");
            RequestContext.getCurrentInstance().execute("PF('confirmCancellationDialogueWidget').show()");
        } else {
            for (Customers cust : multiSelected) {
                if (cust.getActive().getCustomerState().contains("CANCELLED") == false) {
                    // Cancelled customers canot be reinstated in the payment gateway they must be added as new, so only attempt to change in the payment gateway if customer is active or on hold.
                    if (selectedState.getCustomerState().contains("CANCELLED") == true && controller.isTheCustomerProvisionedInThePaymentGateway() == true) {
                        //clear all scheduled payments before cancellation to clean up db
                        controller.setPaymentKeepManualPayments(false);
                        //set our db staus for this customer to cancelled...On second thought get the status from teh payment gateway to be sure.
                        /*PaymentParameters pp = cust.getPaymentParametersId();
                         pp.setPaymentPeriod("Z");
                         pp.setStatusCode("C");
                         pp.setPaymentPeriodDayOfMonth("-");
                         pp.setPaymentPeriodDayOfWeek("---");
                         pp.setNextScheduledPayment(null);
                         ejbPaymentParametersFacade.edit(pp);
                         ejbFacade.edit(cust);*/
                        controller.clearEntireSchedule(cust);
                        //removeFromCustomersTableLists(cust);
                    }
                    controller.changeCustomerStatus(cust, selectedState);

                } else {

                    // add the prevbiously cancelled customer as a new customer in ezidebit
                    controller.createCustomerRecord(cust);

                    String message = configMapFacade.getConfig("CustomersStateChanged") + " " + selectedState.getCustomerState() + "." + configMapFacade.getConfig("CustomersStateCannotChangeCancelled");
                    JsfUtil.addSuccessMessage(message);
                }

                cust.setActive(selectedState);
                getFacade().edit(cust);
                count++;

            }
            recreateModel();
            //RequestContext.getCurrentInstance().update(":tv:customerslistForm1:customersTableList");
            controller.setCustomerCancellationConfirmed(false);
            String message = count + " " + configMapFacade.getConfig("CustomersStateChanged") + " " + selectedState.getCustomerState() + ".";
            JsfUtil.addSuccessMessage(message);
        }

    }

    public String updatePassMobile() {
        try {
            if (checkPass.length() >= 8) {
                updatePassword();
                return "pm:main";
            } else {
                JsfUtil.addErrorMessage("Password Update Error", configMapFacade.getConfig("PasswordLengthError"));
                return null;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Password Update Error", "Password Updated Failed");
            return null;
        }

    }

    public String updatepass() {
        try {
            if (checkPass.length() >= 8) {
                updatePassword();
                return "View";
            } else {
                JsfUtil.addErrorMessage("Password Update Error", configMapFacade.getConfig("PasswordLengthError"));
                return null;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Password Update Error", "Password Updated Failed");
            return null;
        }
    }

    private void updatePassword() {
        Customers c = getSelected();
        c.setPassword(PasswordService.getInstance().encrypt(checkPass));
        getFacade().edit(c);
        JsfUtil.addSuccessMessage("Password Updated", "Your password was changed to " + checkPass);
    }

    public String logoutMobile() {
        FacesContext fc = FacesContext.getCurrentInstance();
        ExternalContext ec = fc.getExternalContext();
        HttpSession session = (HttpSession) ec.getSession(false);
        session.invalidate();

        return "/mobileMenu.xhtml?faces-redirect=true";
    }

    public String logout() {
        FacesContext fc = FacesContext.getCurrentInstance();

        // invalidate session
        ExternalContext ec = fc.getExternalContext();
        HttpSession session = (HttpSession) ec.getSession(false);
        session.invalidate();

        // redirect to the login / home page
        /* try {
         //ec.redirect(ec.getRequestContextPath());

         ec.redirect(configMapFacade.getConfig("WebsiteURL"));
         } catch (IOException e) {
         JsfUtil.addErrorMessage(e, "Log out Failed");
         //LOG.error("Redirect to the login page failed");
         throw new FacesException(e);
         }*/
        return "/index.xhtml?faces-redirect=true";
    }

    /*public String logout() {
     try {
     HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
     request.getSession().invalidate();
     JsfUtil.addSuccessMessage("User Logged Out");
     return "/";
    
    
     } catch (Exception e) {
     JsfUtil.addErrorMessage(e, "Log out Failed");
     return null;
     }
     }*/
    public String destroy() {
        //current = (Customers) getItems().getRowData();
        // selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(getSelected());
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:

        }
        if (selectedItemIndex >= 0) {
            setSelected(getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0));
        }
    }

    /*  public List<Customers> getItems(){
     if (items == null) {
     items = ejbFacade.findAll();
     }
     return items;
     }*/
    public PfSelectableDataModel<Customers> getItems() {
        if (items == null) {

            items = new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(false, "id", selectedCustomerStates, selectedCustomerTypes, isRefreshFromDB()));
            setRefreshFromDB(false);
            // items = getPagination().createPageDataModel();
        }
        if (items == null) {
            items = new PfSelectableDataModel<>(new ArrayList<Customers>());
        }
        return items;
    }

    public PfSelectableDataModel<Customers> getCustomersWithoutScheduledPayments() {
        if (customersWithoutScheduledPayments == null) {
            ArrayList<CustomerState> acs = new ArrayList<>();
            acs.add(new CustomerState(0, "ACTIVE"));
            acs.add(new CustomerState(0, "USER"));
            ArrayList<String> types = new ArrayList<>();
            types.add("USER");

            List<Customers> custListNoPaymentScheduled = new ArrayList<>();
            List<Customers> custList = ejbFacade.findFilteredCustomers(false, "id", acs, types, isRefreshFromDB());

            try {
                for (Customers c : custList) {
                    if (isCustomerInRole(c, "LEAD") == false) {
                        if (c.getPaymentParametersId() != null) {
                            if (c.getPaymentParametersId().getNextScheduledPayment() == null) {
                                custListNoPaymentScheduled.add(c);
                            }
                        } else {
                            custListNoPaymentScheduled.add(c);
                        }
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "getCustomersWithoutScheduledPayments: Exception", e);
            }
            customersWithoutScheduledPayments = new PfSelectableDataModel<>(custListNoPaymentScheduled);
        }
        if (customersWithoutScheduledPayments == null) {
            customersWithoutScheduledPayments = new PfSelectableDataModel<>(new ArrayList<Customers>());
        }
        return customersWithoutScheduledPayments;
    }

    public PfSelectableDataModel<Customers> getLeads() {
        if (leads == null) {
            ArrayList<CustomerState> acs = new ArrayList<>();
            acs.add(new CustomerState(0, "ACTIVE"));
            ArrayList<String> types = new ArrayList<>();

            types.add("LEAD");

            List<Customers> custListOnlyLeads = new ArrayList<>();
            List<Customers> custList = ejbFacade.findFilteredCustomers(false, "id", acs, types, isRefreshFromDB());

            try {
                for (Customers c : custList) {
                    if (isCustomerInRole(c, "LEAD") == true) {
                        custListOnlyLeads.add(c);
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "get Leads: Exception", e);
            }
            leads = new PfSelectableDataModel<>(custListOnlyLeads);
            //leads = new PfSelectableDataModel<>(custList);
        }
        if (leads == null) {
            leads = new PfSelectableDataModel<>(new ArrayList<>());
        }
        return leads;
    }

    public PfSelectableDataModel<Customers> getCustomersOnHold() {
        if (customersOnHold == null) {
            ArrayList<CustomerState> acs = new ArrayList<>();
            acs.add(new CustomerState(0, "ON HOLD"));
            ArrayList<String> types = new ArrayList<>();
            types.add("USER");
            List<Customers> custList = ejbFacade.findFilteredCustomers(false, "id", acs, types, isRefreshFromDB());

            /*  try {
                for (Customers c : custList) {
                    if (isCustomerInRole(c, "LEAD") == true) {
                        custListOnlyLeads.add(c);
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "get Leads: Exception", e);
            }
            leads = new PfSelectableDataModel<>(custListOnlyLeads);*/
            customersOnHold = new PfSelectableDataModel<>(custList);
        }
        if (customersOnHold == null) {
            customersOnHold = new PfSelectableDataModel<>(new ArrayList<>());
        }
        return customersOnHold;
    }

    public PfSelectableDataModel<Payments> getCustomersBadPayments() {
        if (customersBadPayments == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            MySessionsChart1 chart = context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);

            List<Payments> pl = ejbPaymentsFacade.findPaymentsByDateRange(false, false, true, false, false, chart.getDashboardStartDate(), chart.getDashboardEndDate(), false, null);

            customersBadPayments = new PfSelectableDataModel<>(pl);
        }
        if (customersBadPayments == null) {
            customersBadPayments = new PfSelectableDataModel<>(new ArrayList<>());
        }
        return customersBadPayments;
    }

    public PfSelectableDataModel<Notes> getNotesItems() {
        if (notesItems == null) {
            setNotesItems((PfSelectableDataModel<Notes>) getNotesPagination().createPageDataModel());
        }
        if (notesItems == null) {
            setNotesItems(new PfSelectableDataModel<>(new ArrayList<Notes>()));
        }
        return notesItems;
    }

    public void recreateModel() {
        items = null;
        filteredItems = null;
        notesItems = null;
        notesFilteredItems = null;
    }

    public void deleteNote() {
        if (selectedNoteForDeletion != null) {
            try {
                removeFromNotesDataTableLists(selectedNoteForDeletion);
                ejbNotesFacade.remove(selectedNoteForDeletion);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesDeleted"));
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            }
        }
    }

    public void testRequestContext() {
        JsfUtil.addSuccessMessage("Testing Request Context Callback");
        ArrayList<String> als = new ArrayList<>();
        als.add(":tv:customerslistForm1");
        als.add(":growl");
        als.add("@(.updateNotesDataTable)");

        RequestContext.getCurrentInstance().update(als);
        //RequestContext.getCurrentInstance().update("customerslistForm1");
        //RequestContext.getCurrentInstance().update("@(.updateNotesDataTable)");

    }

    private void removeFromNotesDataTableLists(Notes note) {
        if (notesItems != null) {
            List<Notes> lp = (List<Notes>) notesItems.getWrappedData();
            int index = lp.indexOf(note);
            lp.remove(index);
        }
        if (notesFilteredItems != null) {
            int index = notesFilteredItems.indexOf(note);
            notesFilteredItems.remove(index);
        }
    }

    public void addToNotesDataTableLists(Notes note) {
        if (notesItems != null) {
            List<Notes> lp = (List<Notes>) notesItems.getWrappedData();
            lp.add(0, note);//insert at the top of the list
        }
        if (notesFilteredItems != null) {
            notesFilteredItems.add(note);
        }
    }

    private void removeFromCustomersTableLists(Customers cust) {
        if (items != null) {
            List<Customers> lp = (List<Customers>) items.getWrappedData();
            int index = lp.indexOf(cust);
            lp.remove(index);
        }
        if (filteredItems != null) {
            int index = filteredItems.indexOf(cust);
            filteredItems.remove(index);
        }
    }

    public void addToCustomersTableLists(Customers cust) {
        if (items != null) {
            List<Customers> lp = (List<Customers>) items.getWrappedData();
            lp.add(0, cust);//insert at the top of the list
        }
        if (filteredItems != null) {
            filteredItems.add(cust);
        }
    }

    public String next() {
        recreateModel();
        return "List";
    }

    public void handleDateSelect(SelectEvent event) {

        Date date = (Date) event.getObject();

        //Add facesmessage
    }

    public String previous() {

        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(true), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItemsCustomersById(ejbFacade.findAll(true), true);
    }

    public SelectItem[] getItemsAvailableSelectOneObject() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(true), true);
    }

    public Collection<Customers> getCustomersAvailableSelectOneObject() {
        return ejbFacade.findAll(true);
    }

    public Collection<Customers> getActiveCustomersAndStaff() {
        return ejbFacade.findAllActiveCustomersAndStaff(true);
    }

    public SelectItem[] getCustomersByGroupSelectOne(String group, boolean sortAsc) {
        return JsfUtil.getSelectItems(ejbFacade.findAllByGroup(group, sortAsc), true);
    }

    public Collection<Customers> getCustomersByGroupObject(String group, boolean sortAsc) {
        return ejbFacade.findAllByGroup(group, sortAsc);
    }

    public Collection<Customers> getActiveCustomersByGroupObject(boolean sortAsc) {
        return ejbFacade.findAllActiveCustomers(sortAsc);
    }

    public Collection<Customers> getActiveCustomers() {
        return ejbFacade.findAllActiveCustomers(true);
    }

    public void onEdit(RowEditEvent event) {
        Customers cm = (Customers) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    public void checkPassChange() {

        passwordsMatch = checkPass.equals(checkPass2);
        // if(checkPass.length() < 8){
        //      passwordsMatch = false;
        //  }
    }

    /**
     * @return the checkPass
     */
    public String getCheckPass() {
        return checkPass;
    }

    /**
     * @param checkPass the checkPass to set
     */
    public void setCheckPass(String checkPass) {
        this.checkPass = checkPass;
    }

    /**
     * @return the checkPass2
     */
    public String getCheckPass2() {
        return checkPass2;
    }

    /**
     * @param checkPass2 the checkPass2 to set
     */
    public void setCheckPass2(String checkPass2) {
        this.checkPass2 = checkPass2;
    }

    public void dialogueFirstNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            newVal = newVal.trim();
            updateUsername(newVal, null, newCustomer);

        }
    }

    public void dialogueLastNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            newVal = newVal.trim();
            updateUsername(null, newVal, newCustomer);

        }
    }

    public void firstNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            newVal = newVal.trim();
            updateUsername(newVal, null, current);

        }
    }

    public void lastNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            newVal = newVal.trim();
            updateUsername(null, newVal, current);

        }
    }

    private void updateUsername(String firstname, String lastname, Customers customer) {
        String updatedUsername = "";
        if (firstname == null && lastname == null) {
            return;
        }
        if (firstname == null || lastname == null) {
            if (firstname == null) {
                updatedUsername = customer.getFirstname().trim() + "." + lastname.trim();

            }
            if (lastname == null) {
                updatedUsername = firstname.trim() + "." + customer.getLastname().trim();
            }
        } else {
            updatedUsername = firstname.trim() + "." + lastname.trim();
        }
        String newUsername = updatedUsername.toLowerCase().replaceAll(" ", "");
        customer.setUsername(newUsername);
        Customers cust = getFacade().findCustomerByUsername(newUsername);
        if (cust != null) {
            setAddUserButtonDisabled(true);
            JsfUtil.addErrorMessage("Error", "The username:" + newUsername + " is already in use!");
        } else {
            setAddUserButtonDisabled(false);
        }
    }

    public void dobChangeListener(SelectEvent event) {
        Date dob = (Date) event.getObject();
        current.setDob(dob);
        updateDemographic(dob);

    }

    public void onRowSelectEvent(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass().equals(Customers.class)) {
            Customers cust = (Customers) o;
            //setSelected(cust);
        }

    }

    public void genderChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(Character.class)) {
            char gen = (Character) o;
            Date dob = current.getDob();
            current.setGender(gen);
            updateDemographic(dob);
        }
    }

    public void onTabChange(TabChangeEvent event) {
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

    private void updateDemographic(Date dob) {
        char gender = current.getGender();
        int demographic = 0;
        GregorianCalendar dobCal = new GregorianCalendar();
        dobCal.setTime(dob);
        GregorianCalendar cal = new GregorianCalendar();
        boolean demographicFound = false;
        if (gender == 'M') {
            cal.add(Calendar.YEAR, -20);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {//greater than 0 if this dob calender time after cal// dob < than 20 years ago
                demographic = 0;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 30 years ago
                demographic = 1;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 40 years ago
                demographic = 2;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 50 years ago
                demographic = 3;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 60 years ago
                demographic = 4;
                demographicFound = true;
            }
            if (demographicFound == false) {// dob > than 60 years
                demographic = 5;
            }

        } else {
            cal.add(Calendar.YEAR, -20);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 20 years ago
                demographic = 6;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 30 years ago
                demographic = 7;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 40 years ago
                demographic = 8;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 50 years ago
                demographic = 9;
                demographicFound = true;
            }
            cal.add(Calendar.YEAR, -10);
            if (dobCal.compareTo(cal) > 0 && demographicFound == false) {// dob < than 60 years ago
                demographic = 10;
                demographicFound = true;
            }
            if (demographicFound == false) {// dob > than 60 years
                demographic = 11;
            }
        }
        try {
            current.setDemographic(ejbDemoFacade.find(demographic));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Error setting demographic");
        }
    }

    /**
     * @return the impersonate
     */
    public Customers getImpersonate() {
        if (impersonate == null) {
            impersonate = getSelected();
        }
        return impersonate;
    }

    /**
     * @param impersonate the impersonate to set
     */
    public void setImpersonate(Customers impersonate) {
        this.impersonate = impersonate;
    }

    /**
     * @return the impersonating
     */
    public boolean isImpersonating() {
        return impersonating;
    }

    /**
     * @param impersonating the impersonating to set
     */
    public void setImpersonating(boolean impersonating) {
        this.impersonating = impersonating;
    }

    /**
     * @return the selectedForDeletion
     */
    public Customers getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(Customers selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    /**
     * @return the notesFilteredItems
     */
    public List<Notes> getNotesFilteredItems() {
        return notesFilteredItems;
    }

    /**
     * @param notesFilteredItems the notesFilteredItems to set
     */
    public void setNotesFilteredItems(List<Notes> notesFilteredItems) {
        this.notesFilteredItems = notesFilteredItems;
    }

    public void onNotesEdit(RowEditEvent event) {
        Notes cm = (Notes) event.getObject();
        ejbNotesFacade.edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onNotesCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the selectedNoteForDeletion
     */
    public Notes getSelectedNoteForDeletion() {
        return selectedNoteForDeletion;
    }

    /**
     * @param selectedNoteForDeletion the selectedNoteForDeletion to set
     */
    public void setSelectedNoteForDeletion(Notes selectedNoteForDeletion) {
        this.selectedNoteForDeletion = selectedNoteForDeletion;
    }

    /**
     * @return the customerTabsEnabled
     */
    public boolean isCustomerTabsEnabled() {
        return customerTabsEnabled;
    }

    /**
     * @param customerTabsEnabled the customerTabsEnabled to set
     */
    public void setCustomerTabsEnabled(boolean customerTabsEnabled) {
        this.customerTabsEnabled = customerTabsEnabled;
    }

    /**
     * @return the loggedInUser
     */
    public Customers getLoggedInUser() {
        if (loggedInUser == null) {
            String name = "Unknown";
            FacesContext facesContext = FacesContext.getCurrentInstance();
            HttpServletRequest req = (HttpServletRequest) facesContext.getExternalContext().getRequest(); //request;
            try {
                if (facesContext != null) {
                    name = facesContext.getExternalContext().getRemoteUser();
                } else {

                }
                if (name != null) {
                    loggedInUser = ejbFacade.findCustomerByUsername(name);
                    if (loggedInUser == null) {
                        LOGGER.log(Level.INFO, "getLoggedInUser: a call to findCustomerByUsername(name) returned NULL. Name looked up =", name);
                    }
                } else {
                    LOGGER.log(Level.INFO, "getLoggedInUser: a call to facesContext.getExternalContext().getRemoteUser() returned NULL. User not authenticated");
                }
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e, "Couldn't get customer " + name);
            }
            // get user agent and redirect if its a mobile

            String uaString = req.getHeader("User-Agent");
            LOGGER.log(Level.INFO, "The User-Agent of this session is :{0}", uaString);
        }
        return loggedInUser;
    }

    /**
     * @param loggedInUser the loggedInUser to set
     */
    public void setLoggedInUser(Customers loggedInUser) {
        this.loggedInUser = loggedInUser;
    }

    /**
     * @param notesItems the notesItems to set
     */
    public void setNotesItems(PfSelectableDataModel<Notes> notesItems) {
        this.notesItems = notesItems;
    }

    /**
     * @return the lastSelected
     */
    public Customers getLastSelected() {
        return lastSelected;
    }

    /**
     * @param lastSelected the lastSelected to set
     */
    public void setLastSelected(Customers lastSelected) {
        this.lastSelected = lastSelected;
    }

    /**
     * @return the selectedState
     */
    public CustomerState getSelectedState() {
        return selectedState;
    }

    /**
     * @param selectedState the selectedState to set
     */
    public void setSelectedState(CustomerState selectedState) {
        this.selectedState = selectedState;
    }

    /**
     * @return the customerStateList
     */
    public List<CustomerState> getCustomerStateList() {
        if (customerStateList == null) {

            customerStateList = ejbCustomerStateFacade.findAll();
            //selectedCustomerStates = new CustomerState[customerStateList.size()];
            //selectedCustomerStates = customerStateList.toArray(selectedCustomerStates);

            selectedCustomerStates = new ArrayList<>();
            for (CustomerState cs : customerStateList) {
                if (cs.getCustomerState().contains("ACTIVE")) {
                    selectedCustomerStates.add(cs);
                }
                // if (cs.getCustomerState().contains("ON HOLD")) {
                //     selectedCustomerStates[1] = cs;
                //  }

            }

        }
        return customerStateList;
    }

    public List<String> getCustomerTypesList() {
        if (customerTypesList == null) {

            customerTypesList = ejbGroupsFacade.getGroups();
            customerTypesList.remove("DEVELOPER");
            selectedCustomerTypes = new ArrayList<>();
            for (String cs : customerTypesList) {
                if (cs.contains("USER")) {
                    selectedCustomerTypes.add(cs);
                }
                // if (cs.getCustomerState().contains("ON HOLD")) {
                //     selectedCustomerStates[1] = cs;
                //  }

            }
        }
        return Collections.unmodifiableList(customerTypesList);
    }

    /**
     * @return the selectedCustomerStates
     */
    public List<CustomerState> getSelectedCustomerStates() {
        return selectedCustomerStates;
    }

    /**
     * @param selectedCustomerStates the selectedCustomerStates to set
     */
    public void setSelectedCustomerStates(List<String> selectedCustomerStates) {
        List<CustomerState> newCustomerStates = new ArrayList<>();
        for (String s : selectedCustomerStates) {
            for (CustomerState cs : customerStateList) {
                if (cs.getCustomerState().contains(s)) {
                    newCustomerStates.add(cs);
                }
            }
        }

        this.selectedCustomerStates = newCustomerStates;
    }

    /**
     * @return the showNonUsers
     */
    public boolean isShowNonUsers() {
        return showNonUsers;
    }

    /**
     * @param showNonUsers the showNonUsers to set
     */
    public void setShowNonUsers(boolean showNonUsers) {
        this.showNonUsers = showNonUsers;
        recreateModel();
    }

    /**
     * @return the refreshFromDB
     */
    public boolean isRefreshFromDB() {
        return refreshFromDB;
    }

    /**
     * @param refreshFromDB the refreshFromDB to set
     */
    public void setRefreshFromDB(boolean refreshFromDB) {
        this.refreshFromDB = refreshFromDB;
    }

    /**
     * @return the addUserButtonDisabled
     */
    public boolean isAddUserButtonDisabled() {
        return addUserButtonDisabled;
    }

    /**
     * @param addUserButtonDisabled the addUserButtonDisabled to set
     */
    public void setAddUserButtonDisabled(boolean addUserButtonDisabled) {
        this.addUserButtonDisabled = addUserButtonDisabled;
    }

    /**
     * @return the selectedGroups
     *
     * public Groups[] getSelectedGroups() { if (selectedGroups == null) {
     * List<Groups> currentUsersGroupsMembership =
     * ejbGroupsFacade.getCustomersGroups(current); selectedGroups =
     * currentUsersGroupsMembership.toArray(new
     * Groups[currentUsersGroupsMembership.size()]);
     *
     * }
     * return selectedGroups; }
     */
    public List<Groups> getSelectedGroups() {
        if (selectedGroups == null) {
            selectedGroups = ejbGroupsFacade.getCustomersGroups(current);
        }
        return selectedGroups;
    }

    /**
     * @param selectedGroups the selectedGroups to set
     */
    public void setSelectedGroups(List<Groups> selectedGroups) {
        this.selectedGroups = selectedGroups;
    }

    /**
     * @return the customerGroupsList
     */
    public List<Groups> getCustomerGroupsList() {
        if (customerGroupsList == null) {
            customerGroupsList = new ArrayList<>();
            List<String> distinctGroups = ejbGroupsFacade.getGroups();
            if (distinctGroups != null) {
                distinctGroups.remove("DEVELOPER");
                for (String g : distinctGroups) {
                    customerGroupsList.add(new Groups(0, g));
                }
                checkedGroups = new Boolean[distinctGroups.size()];
                for (int c = 0; c < distinctGroups.size(); c++) {
                    for (Groups g : getSelectedGroups()) {
                        if (distinctGroups.get(c).contains(g.getGroupname())) {
                            checkedGroups[c] = true;
                        }
                    }
                }
            }
        }
        return customerGroupsList;
    }

    /**
     * @return the newCustomerGroupsList
     */
    public List<Groups> getNewCustomerGroupsList() {
        if (newCustomerGroupsList == null) {
            newCustomerGroupsList = new ArrayList<>();
            List<String> distinctGroups = ejbGroupsFacade.getGroups();
            if (distinctGroups != null) {
                distinctGroups.remove("DEVELOPER");
                for (String g : distinctGroups) {
                    newCustomerGroupsList.add(new Groups(0, g));
                }
                newCustomerCheckedGroups = new Boolean[distinctGroups.size()];

                for (int c = 0; c < distinctGroups.size(); c++) {

                    if (distinctGroups.get(c).contains("USER")) {
                        newCustomerCheckedGroups[c] = true;
                    }

                }
            }
        }
        return newCustomerGroupsList;
    }

    /**
     * @param customerGroupsList the customerGroupsList to set
     */
    public void setCustomerGroupsList(List<Groups> customerGroupsList) {
        this.customerGroupsList = customerGroupsList;
    }

    /**
     * @return the checkedGroups
     */
    public Boolean[] getCheckedGroups() {
        return checkedGroups;
    }

    /**
     * @param checkedGroups the checkedGroups to set
     */
    public void setCheckedGroups(Boolean[] checkedGroups) {
        this.checkedGroups = checkedGroups;
    }

    /**
     * @return the passwordsMatch
     */
    public boolean isPasswordsMatch() {
        return passwordsMatch;
    }

    /**
     * @param passwordsMatch the passwordsMatch to set
     */
    public void setPasswordsMatch(boolean passwordsMatch) {
        this.passwordsMatch = passwordsMatch;
    }

    /**
     * @return the newCustomer
     */
    public Customers getNewCustomer() {
        if (newCustomer == null) {
            setNewCustomer(setCustomerDefaults(new Customers()));
        }
        return newCustomer;
    }

    /**
     * @param newCustomer the newCustomer to set
     */
    public void setNewCustomer(Customers newCustomer) {
        this.newCustomer = newCustomer;
    }

    /**
     * @param newCustomerGroupsList the newCustomerGroupsList to set
     */
    public void setNewCustomerGroupsList(List<Groups> newCustomerGroupsList) {
        this.newCustomerGroupsList = newCustomerGroupsList;
    }

    /**
     * @return the newCustomerCheckedGroups
     */
    public Boolean[] getNewCustomerCheckedGroups() {
        return newCustomerCheckedGroups;
    }

    /**
     * @param newCustomerCheckedGroups the newCustomerCheckedGroups to set
     */
    public void setNewCustomerCheckedGroups(Boolean[] newCustomerCheckedGroups) {
        this.newCustomerCheckedGroups = newCustomerCheckedGroups;
    }

    /**
     * @return the signupFromBookingInProgress
     */
    public boolean isSignupFromBookingInProgress() {
        return signupFromBookingInProgress;
    }

    /**
     * @param signupFromBookingInProgress the signupFromBookingInProgress to set
     */
    public void setSignupFromBookingInProgress(boolean signupFromBookingInProgress) {
        this.signupFromBookingInProgress = signupFromBookingInProgress;
    }

    /**
     * @return the signupFormSubmittedOK
     */
    public boolean isSignupFormSubmittedOK() {
        return signupFormSubmittedOK;
    }

    /**
     * @param signupFormSubmittedOK the signupFormSubmittedOK to set
     */
    public void setSignupFormSubmittedOK(boolean signupFormSubmittedOK) {
        this.signupFormSubmittedOK = signupFormSubmittedOK;
    }

    /**
     * @return the testTime
     */
    public Date getTestTime() {
        testTime = new Date();
        return testTime;
    }

    /**
     * @param testTime the testTime to set
     */
    public void setTestTime(Date testTime) {
        this.testTime = testTime;
    }

    /**
     * @return the leadComments
     */
    public String getLeadComments() {
        return leadComments;
    }

    /**
     * @param leadComments the leadComments to set
     */
    public void setLeadComments(String leadComments) {
        this.leadComments = leadComments;
    }

    /**
     * @return the leadFormSubmitted
     */
    public boolean isLeadFormSubmitted() {
        return leadFormSubmitted;
    }

    /**
     * @param leadFormSubmitted the leadFormSubmitted to set
     */
    public void setLeadFormSubmitted(boolean leadFormSubmitted) {
        this.leadFormSubmitted = leadFormSubmitted;
    }

    /**
     * @return the filteredCustomersWithoutScheduledPayments
     */
    public List<Customers> getFilteredCustomersWithoutScheduledPayments() {
        return filteredCustomersWithoutScheduledPayments;
    }

    /**
     * @param filteredCustomersWithoutScheduledPayments the
     * filteredCustomersWithoutScheduledPayments to set
     */
    public void setFilteredCustomersWithoutScheduledPayments(List<Customers> filteredCustomersWithoutScheduledPayments) {
        this.filteredCustomersWithoutScheduledPayments = filteredCustomersWithoutScheduledPayments;
    }

    /**
     * @return the multiSelectedCustomersWithoutScheduledPayments
     */
    public Customers[] getMultiSelectedCustomersWithoutScheduledPayments() {
        return multiSelectedCustomersWithoutScheduledPayments;
    }

    /**
     * @param multiSelectedCustomersWithoutScheduledPayments the
     * multiSelectedCustomersWithoutScheduledPayments to set
     */
    public void setMultiSelectedCustomersWithoutScheduledPayments(Customers[] multiSelectedCustomersWithoutScheduledPayments) {
        this.multiSelectedCustomersWithoutScheduledPayments = multiSelectedCustomersWithoutScheduledPayments;
    }

    /**
     * @return the customersNewPlan
     */
    public Plan getCustomersNewPlan() {
        return customersNewPlan;
    }

    /**
     * @param customersNewPlan the customersNewPlan to set
     */
    public void setCustomersNewPlan(Plan customersNewPlan) {
        this.customersNewPlan = customersNewPlan;
    }

    /**
     * @return the selectedForImpersonation
     */
    public CustomerState getSelectedForImpersonation() {
        return selectedForImpersonation;
    }

    /**
     * @param selectedForImpersonation the selectedForImpersonation to set
     */
    public void setSelectedForImpersonation(CustomerState selectedForImpersonation) {
        this.selectedForImpersonation = selectedForImpersonation;
    }

    /**
     * @return the impersonationOn
     */
    public boolean isImpersonationOn() {
        return impersonationOn;
    }

    /**
     * @param impersonationOn the impersonationOn to set
     */
    public void setImpersonationOn(boolean impersonationOn) {
        this.impersonationOn = impersonationOn;
    }

    public void impersonationToggled(ValueChangeEvent vce) {
        Object newObject = vce.getNewValue();

        if (newObject.getClass().equals(Boolean.class)) {
            boolean newValue = (boolean) newObject;
            if (newValue == false) {

                setSelected(getLoggedInUser());
                FacesContext context = FacesContext.getCurrentInstance();
                MySessionsChart1 c2 = context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                c2.recreateModel();
            }

        }

    }

    @FacesConverter(value = "customersControllerConverter")
    public static class CustomersControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            CustomersController controller = (CustomersController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "customersController");
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        }

        @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Customers) {
                Customers o = (Customers) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + CustomersController.class.getName());
            }
        }
    }

    /**
     * @return the multiSelectedLeads
     */
    public Customers[] getMultiSelectedLeads() {
        return multiSelectedLeads;
    }

    /**
     * @param multiSelectedLeads the multiSelectedLeads to set
     */
    public void setMultiSelectedLeads(Customers[] multiSelectedLeads) {
        this.setMultiSelectedLeads(multiSelectedLeads);
    }

    /**
     * @return the filteredLeads
     */
    public List<Customers> getFilteredLeads() {
        return filteredLeads;
    }

    /**
     * @param filteredLeads the filteredLeads to set
     */
    public void setFilteredLeads(List<Customers> filteredLeads) {
        this.filteredLeads = filteredLeads;
    }

    /**
     * @param custmersOnHold the custmersOnHold to set
     */
    public void setCustmersOnHold(PfSelectableDataModel<Customers> custmersOnHold) {
        this.customersOnHold = custmersOnHold;
    }

    /**
     * @return the filteredCustomersOnHold
     */
    public List<Customers> getFilteredCustomersOnHold() {
        return filteredCustomersOnHold;
    }

    /**
     * @param filteredCustomersOnHold the filteredCustomersOnHold to set
     */
    public void setFilteredCustomersOnHold(List<Customers> filteredCustomersOnHold) {
        this.filteredCustomersOnHold = filteredCustomersOnHold;
    }

    /**
     * @param customerTypesList the customerTypesList to set
     */
    public void setCustomerTypesList(List<String> customerTypesList) {
        this.customerTypesList = customerTypesList;
    }

    /**
     * @return the selectedCustomerTypes
     */
    public List<String> getSelectedCustomerTypes() {
        return selectedCustomerTypes;
    }

    /**
     * @param selectedCustomerTypes the selectedCustomerTypes to set
     */
    public void setSelectedCustomerTypes(List<String> selectedCustomerTypes) {
        this.selectedCustomerTypes = selectedCustomerTypes;
    }

    /**
     * @return the filteredCustomersBadPayments
     */
    public List<Payments> getFilteredCustomersBadPayments() {
        return filteredCustomersBadPayments;
    }

    /**
     * @param filteredCustomersBadPayments the filteredCustomersBadPayments to
     * set
     */
    public void setFilteredCustomersBadPayments(List<Payments> filteredCustomersBadPayments) {
        this.filteredCustomersBadPayments = filteredCustomersBadPayments;
    }
}
