package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ApplicationBean;
import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.beans.LoginBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentsFacade;
import au.com.manlyit.fitnesscrm.stats.chartbeans.MySessionsChart1;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.AjaxBehaviorEvent;
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

    private Customers current;
    private Customers lastSelected;
    private Customers newCustomer;
    private Customers selectedForDeletion;
    private CustomerState selectedState;
    private Notes selectedNoteForDeletion;
    private static final String paymentGateway = "EZIDEBIT";
    //private CustomerState[] selectedCustomerStates;
    private List<CustomerState> selectedCustomerStates;
    private List<CustomerState> customerStateList;
    private List<Groups> customerGroupsList;
    private List<Groups> newCustomerGroupsList;
    private PfSelectableDataModel<Customers> items = null;
    private PfSelectableDataModel<Customers> customersWithoutScheduledPayments = null;
    private Date testTime;

    private PfSelectableDataModel<Notes> notesItems = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentBean ejbPaymentBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;

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
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade ejbPaymentsFacade;

    private DatatableSelectionHelper pagination;
    private DatatableSelectionHelper notesPagination;
    private int selectedItemIndex;

    private List<Customers> filteredItems;
    private List<Customers> filteredCustomersWithoutScheduledPayments;
    private List<Notes> notesFilteredItems;
    private Customers[] multiSelected;
    private Customers[] multiSelectedCustomersWithoutScheduledPayments;
    // private Groups[] selectedGroups;
    private List<Groups> selectedGroups;
    private String checkPass = "";
    private Boolean[] checkedGroups;
    private Boolean[] newCustomerCheckedGroups;
    private String checkPass2 = "";
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
            controller.setSelectedCustomer(cust);
            //eziDebitPaymentGatewayController.setSelectedCustomer(cust);
            MySessionsChart1 c2 = context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
            c2.setSelectedCustomer(cust);
            //mySessionsChart1Controller.setSelectedCustomer(cust);
            selectedItemIndex = -1;
            checkPass = current.getPassword();
            if (cust.getProfileImage() == null) {
                createDefaultCustomerProfilePicture(cust);
            }
            recreateAllAffectedPageModels();
            setCustomerTabsEnabled(true);
            RequestContext.getCurrentInstance().execute("updatePaymentForms();");

        }
    }

    public void sendCustomerOnboardEmail() {
        FacesContext context = FacesContext.getCurrentInstance();
        LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
        controller.doPasswordReset("system.new.customer.template", current, configMapFacade.getConfig("sendCustomerOnBoardEmailEmailSubject"));
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("sendCustomerOnBoardEmail") + " " + current.getFirstname() + " " + current.getLastname() + ".");
        String auditDetails = "Customer On Board Email Sent For:" + current.getFirstname() + " " + current.getLastname() + ".";
        String changedFrom = "N/A";
        String changedTo = "On Board Email Sent";

        try {
            String url = current.getPaymentParameters().getWebddrUrl();
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
        c.setPassword(configMapFacade.getConfig("default.customer.details.password"));
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

                    return new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(true, "firstname", selectedCustomerStates, showNonUsers, true));

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

    protected void createDefaultPaymentParameters(Customers current) {

        if (current == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer is NULL.");
            return;
        }
        if (current.getId() == null) {
            LOGGER.log(Level.WARNING, "Future Map createDefaultPaymentParameters . Customer.getId is NULL.");
            return;
        }

        PaymentParameters pp = current.getPaymentParameters();

        if (pp == null) {
            pp = ejbPaymentParametersFacade.findPaymentParametersByCustomer(current);
            if (pp == null) {
                try {

                    String phoneNumber = current.getTelephone();
                    if (phoneNumber == null) {
                        phoneNumber = "0000000000";
                        LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", current.getUsername());
                    }
                    Pattern p = Pattern.compile("\\d{10}");
                    Matcher m = p.matcher(phoneNumber);
                    //ezidebit requires an australian mobile phone number that starts with 04
                    if (m.matches() == false || phoneNumber.startsWith("04") == false) {
                        phoneNumber = "0000000000";
                        LOGGER.log(Level.INFO, "Invalid Phone Number for Customer {0}. Setting it to empty string", current.getUsername());
                    }
                    pp = new PaymentParameters();
                    pp.setId(0);
                    pp.setWebddrUrl(null);
                    pp.setLoggedInUser(current);
                    pp.setLastSuccessfulScheduledPayment(ejbPaymentsFacade.findLastSuccessfulScheduledPayment(current));
                    pp.setNextScheduledPayment(ejbPaymentsFacade.findNextScheduledPayment(current));
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
                    ejbPaymentParametersFacade.create(pp);
                    current.setPaymentParameters(pp);
                    ejbFacade.editAndFlush(current);

                } catch (Exception e) {
                    LOGGER.log(Level.SEVERE, "createDefaultPaymentParameters Method in Customers Controller", e);
                }
            } else {
                current.setPaymentParameters(pp);
                ejbFacade.editAndFlush(current);
                LOGGER.log(Level.WARNING, "createDefaultPaymentParameters - The payment parameters existed for this customer but were not linked with this customers foreign key", current.getUsername());
            }
        } else {
            LOGGER.log(Level.WARNING, "createDefaultPaymentParameters - The payment parameters have already been created for this customer", current.getUsername());
        }
    }

    protected PaymentParameters getCustomersPaymentParameters(Customers cust) {
        if (cust != null) {
            PaymentParameters pp = cust.getPaymentParameters();
            if (pp == null) {
                createDefaultPaymentParameters(cust);
            }
            pp = cust.getPaymentParameters();
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
            if (group.contains("LEAD")) {
                // new lead from contact form
                c.setUsername(getUniqueUsername(c.getFirstname() + "." + c.getLastname()));
                getFacade().create(c);

                grp.setUsername(c);
                ejbGroupsFacade.create(grp);
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
                String details = "New LEAD generated: Name: " + c.getFirstname() + ", <br/>Email:  " + c.getEmailAddress() + ", <br/>Phone:   " + c.getTelephone() + ", <br/>Username:   " + c.getUsername() + ", <br/>Group:   " + group + ", IP Address:" + ipAddress;
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
                ejbGroupsFacade.create(grp);
                createDefaultCustomerProfilePicture(c);
                String details = "Name: " + c.getFirstname() + ", Email:" + c.getEmailAddress() + ", Phone:" + c.getTelephone() + ", Username:" + c.getUsername() + ", Group:" + group + ", IP Address:" + ipAddress + ".Customer Onboard email sent";
                FacesContext context = FacesContext.getCurrentInstance();
                LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
                controller.doPasswordReset("system.new.customer.template", c, configMapFacade.getConfig("sendCustomerOnBoardEmailEmailSubject"));
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

        if (c.getId() == null || getFacade().find(c.getId()) == null) {
            // does not exist so create a new customer
            try {
                c.setId(0);
                getFacade().create(c);
                setSelected(c);
                updateCustomersGroupMembership(c);

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
        List<Groups> customersExistingGroups = ejbGroupsFacade.getCustomersGroups(c);
        for (Groups g : customersExistingGroups) {
            boolean exists = false;
            for (Groups sg : selectedGroups) {
                if (sg.equals(g)) {
                    exists = true;
                }
            }
            if (exists == false) {
                ejbGroupsFacade.remove(g);
            }
        }
        for (Groups g : selectedGroups) {
            boolean exists = false;
            for (Groups eg : customersExistingGroups) {
                if (eg.equals(g)) {
                    exists = true;
                }
            }
            if (exists == false) {
                Groups grp = new Groups(0, g.getGroupname());
                grp.setUsername(c);
                ejbGroupsFacade.create(grp);
            }
        }

        addCustomerToUsersGroup(c);

    }

    private void addCustomerToUsersGroup(Customers c) {
        if (ejbGroupsFacade.isCustomerInGroup(c, "USER") == false) {
            Groups grp = new Groups(0, "USER");
            grp.setUsername(c);
            ejbGroupsFacade.create(grp);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersCreatedMustBeInUserGroup"));
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

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void update(ActionEvent event) {
        try {
            Customers selected = getSelected();
            getFacade().edit(selected);
            if (selected.getPaymentParameters() != null) {
                if (selected.getPaymentParameters().getWebddrUrl() != null) {
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
                    if (selectedState.getCustomerState().contains("CANCELLED") == true) {
                        //clear all scheduled payments before cancellation to clean up db
                        controller.setPaymentKeepManualPayments(false);
                        //set our db staus for this customer to cancelled...On second thought get the status from teh payment gateway to be sure.
                        /*PaymentParameters pp = cust.getPaymentParameters();
                         pp.setPaymentPeriod("Z");
                         pp.setStatusCode("C");
                         pp.setPaymentPeriodDayOfMonth("-");
                         pp.setPaymentPeriodDayOfWeek("---");
                         pp.setNextScheduledPayment(null);
                         ejbPaymentParametersFacade.edit(pp);
                         ejbFacade.edit(cust);*/
                        controller.clearEntireSchedule(cust);
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
                recreateModel();
            }
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

            items = new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(false, "id", selectedCustomerStates, showNonUsers, isRefreshFromDB()));
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
            acs.add(new CustomerState(0, "LEAD"));
            List<Customers> custListNoPaymentScheduled = new ArrayList<>();
            List<Customers> custList = ejbFacade.findFilteredCustomers(false, "id", acs, showNonUsers, isRefreshFromDB());

            try {
                for (Customers c : custList) {
                    if (c.getPaymentParameters() != null) {
                        if (c.getPaymentParameters().getNextScheduledPayment() == null) {
                            custListNoPaymentScheduled.add(c);
                        }
                    } else {
                        custListNoPaymentScheduled.add(c);
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

    public void dialogueFirstNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            updateUsername(newVal, null, newCustomer);

        }
    }

    public void dialogueLastNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            updateUsername(null, newVal, newCustomer);

        }
    }

    public void firstNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            updateUsername(newVal, null, current);

        }
    }

    public void lastNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVal = (String) o;
            updateUsername(null, newVal, current);

        }
    }

    private void updateUsername(String firstname, String lastname, Customers customer) {
        String updatedUsername = "";
        if (firstname == null) {
            updatedUsername = customer.getFirstname() + "." + lastname;
        }
        if (lastname == null) {
            updatedUsername = firstname + "." + customer.getLastname();
        }
        String newUsername = updatedUsername.toLowerCase().replace(' ', '_');
        customer.setUsername(newUsername);
        Customers cust = getFacade().findCustomerByUsername(newUsername);
        if (cust != null) {
            setAddUserButtonDisabled(true);
            JsfUtil.addErrorMessage("Error", "That username is already taken!");
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
     * @param checkPass2 the checkPass2 to set
     */
    public void setCheckPass2(String checkPass2) {
        this.checkPass2 = checkPass2;
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
            try {
                name = facesContext.getExternalContext().getRemoteUser();
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

            HttpServletRequest req = (HttpServletRequest) facesContext.getExternalContext().getRequest(); //request;
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

    @FacesConverter(value = "customersControllerConverter", forClass = Customers.class)
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
}
