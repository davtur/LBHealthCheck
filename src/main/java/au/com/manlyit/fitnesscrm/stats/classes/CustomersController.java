package au.com.manlyit.fitnesscrm.stats.classes;

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
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
import org.primefaces.component.tabview.Tab;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;

@Named("customersController")
@SessionScoped
public class CustomersController implements Serializable {

    private Customers current;
    private Customers lastSelected;
    private Customers selectedForDeletion;
    private CustomerState selectedState;
    private Notes selectedNoteForDeletion;
    private static final String paymentGateway = "EZIDEBIT";
    private CustomerState[] selectedCustomerStates;
    private List<CustomerState> customerStateList;
    private PfSelectableDataModel<Customers> items = null;
    private PfSelectableDataModel<Notes> notesItems = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.DemographicTypesFacade ejbDemoFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.GroupsFacade ejbGroupsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerStateFacade ejbCustomerStateFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.NotesFacade ejbNotesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private PaymentsFacade ejbPaymentsFacade;
    private DatatableSelectionHelper pagination;
    private DatatableSelectionHelper notesPagination;
    private int selectedItemIndex;

    private List<Customers> filteredItems;
    private List<Notes> notesFilteredItems;
    private Customers[] multiSelected;
    private String checkPass = "";

    private String checkPass2 = "";
    private Customers impersonate;
    private Customers loggedInUser;
    private boolean impersonating = false;
    private boolean customerTabsEnabled = false;
    private boolean refreshFromDB = false;
    private boolean showNonUsers = false;
    private static final Logger logger = Logger.getLogger(CustomersController.class.getName());

    public CustomersController() {
    }

    @PostConstruct
    private void getLoggedInCustomer() {

        String name = "Unknown";
        FacesContext facesContext = FacesContext.getCurrentInstance();
        try {
            name = facesContext.getExternalContext().getRemoteUser();
            if (name != null) {
                current = ejbFacade.findCustomerByUsername(name);
                loggedInUser = current;
            } else {
                JsfUtil.addErrorMessage("Couldn't get customer not logged in yet!");
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Couldn't get customer " + name);
        }
        // get user agent and redirect if its a mobile

        HttpServletRequest req = (HttpServletRequest) facesContext.getExternalContext().getRequest(); //request;
        String uaString = req.getHeader("User-Agent");
        logger.log(Level.INFO, "User-Agent of this seesion is :{0}", uaString);
        // sanityCheckCustomersForDefaultItems();
//resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
//resp.setHeader("Location", "/AppName/site/ie/home.jsp");
    }

    /* private void sanityCheckCustomersForDefaultItems() {
     logger.log(Level.INFO, "Performing Sanity Checks on Customers");
     List<Customers> cl = ejbFacade.findAll();
     for (Customers c : cl) {
     if (c.getProfileImage() == null) {
     createDefaultCustomerProfilePicture(c);
     }
     }
     logger.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
     }*/
    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public boolean isCustomerInRole(Customers cust, String roleName) {
        return ejbGroupsFacade.isCustomerInGroup(cust, roleName);
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
    public void updateSelectedCustomer(Customers cust){
        current = cust;
    }

    public void setSelected(Customers cust) {
        if (cust != null) {
            lastSelected = current;
            current = cust;
            FacesContext context = FacesContext.getCurrentInstance();
            EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
            controller.setSelectedCustomer(cust);
            selectedItemIndex = -1;
            checkPass = current.getPassword();
            if (cust.getProfileImage() == null) {
                createDefaultCustomerProfilePicture(cust);
            }
            recreateAllAffectedPageModels();
            setCustomerTabsEnabled(true);
            RequestContext.getCurrentInstance().update("iFrameForm");

        }
    } 

    public void sendCustomerOnboardEmail() {
        FacesContext context = FacesContext.getCurrentInstance();
        LoginBean controller = (LoginBean) context.getApplication().getELResolver().getValue(context.getELContext(), null, "loginBean");
        controller.doPasswordReset("system.new.customer.template", current, configMapFacade.getConfig("sendCustomerOnBoardEmailEmailSubject"));
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("sendCustomerOnBoardEmail") + " " + current.getFirstname() + " " + current.getLastname() + ".");
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

    private Customers setCusomerDefaults(Customers c) {

        try {
            c.setCountryId(Integer.parseInt(configMapFacade.getConfig("default.customer.details.countryId")));
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Number Format Exception for Country ID in customer deaults.Check config map entry for default.customer.details.countryId and value {0}", configMapFacade.getConfig("default.customer.details.countryId"));
        }
        c.setCity(configMapFacade.getConfig("default.customer.details.city"));
        c.setStreetAddress(configMapFacade.getConfig("default.customer.details.street"));
        c.setAddrState(configMapFacade.getConfig("default.customer.details.state"));
        c.setSuburb(configMapFacade.getConfig("default.customer.details.suburb"));
        c.setPostcode(configMapFacade.getConfig("default.customer.details.postcode"));
        try {
            c.setReferredby(Integer.parseInt(configMapFacade.getConfig("default.customer.details.referredby")));
        } catch (NumberFormatException e) {
            logger.log(Level.SEVERE, "Number Format Exception for Referred by customer ID in customer deaults.Check config map entry for default.customer.details.referredby and value {0}", configMapFacade.getConfig("default.customer.details.referredby"));
        }

        //for debug
        c.setUsername(configMapFacade.getConfig("default.customer.details.username"));
        c.setFirstname(configMapFacade.getConfig("default.customer.details.firstname"));
        c.setLastname(configMapFacade.getConfig("default.customer.details.lastname"));
        c.setPassword(configMapFacade.getConfig("default.customer.details.password"));
        c.setEmailAddress(configMapFacade.getConfig("default.customer.details.email"));
        c.setFax(configMapFacade.getConfig("default.customer.details.fax"));
        c.setTelephone(configMapFacade.getConfig("default.customer.details.mobile"));

        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.YEAR, -30);
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
                public PfSelectableDataModel createPageDataModel() {

                    return new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(true, selectedCustomerStates,showNonUsers,true));

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
                public PfSelectableDataModel createPageDataModel() {
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
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Customers> filteredItems) {
        this.filteredItems = filteredItems;
    }

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
            logger.log(Level.SEVERE, "createDefaultPaymentParameters Method in Customers Controller", e);
        }
    }

    protected PaymentParameters getSelectedCustomersPaymentParameters() {
        PaymentParameters pp = getSelected().getPaymentParameters();
        if (pp == null) {
            createDefaultPaymentParameters(getSelected());
        }
        pp = getSelected().getPaymentParameters();
        if (pp == null) {
            logger.log(Level.SEVERE, " Customer {0} has NULL Payment parameters.", new Object[]{current.getUsername()});
        }
        return pp;
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

    private void recreateAllAffectedPageModels() {
        FacesContext context = FacesContext.getCurrentInstance();
        // recreate any datatables that have stale data after changing users
        String message = "Recreating data models for user : " + getSelected().getUsername() + ". ";
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);
        recreateModel();
        SessionHistoryController c1 = (SessionHistoryController) context.getApplication().evaluateExpressionGet(context, "#{sessionHistoryController}", SessionHistoryController.class);
        c1.recreateModel();
        MySessionsChart1 c2 = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
        c2.recreateModel();
        EziDebitPaymentGateway c3 = (EziDebitPaymentGateway) context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);
        c3.recreateModels();

        CustomerImagesController c4 = (CustomerImagesController) context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
        c4.recreateModel();
        ChartController c5 = (ChartController) context.getApplication().evaluateExpressionGet(context, "#{chartController}", ChartController.class);
        c5.recreateModel();
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
        setSelected(setCusomerDefaults(new Customers()));
        selectedItemIndex = -1;
        return "Create";
    }

    public void prepareCreateAjax(ActionEvent actionEvent) {
        setLastSelected(current);
        current = setCusomerDefaults(new Customers());
        selectedItemIndex = -1;
        //RequestContext.getCurrentInstance().update("customersCreateDialogue");
        //RequestContext.getCurrentInstance().openDialog("customersCreateDialogue");
    }

    private void createDefaultCustomerProfilePicture(Customers c) {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomerImagesController custImageCon = (CustomerImagesController) context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
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

    public void createDialogue(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        Customers c = getSelected();
        EziDebitPaymentGateway ezi = (EziDebitPaymentGateway) context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);

        if (c.getId() == null || getFacade().find(c.getId()) == null) {
            // does not exist so create a new customer
            try {
                c.setId(0);
                getFacade().create(c);
                Groups grp = new Groups(0, "USER");
                grp.setUsername(c);
                ejbGroupsFacade.create(grp);
                createDefaultCustomerProfilePicture(c);
                // createDefaultPaymentParameters(paymentGateway);
                recreateAllAffectedPageModels();
                setSelected(c);

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
                recreateAllAffectedPageModels();
                ezi.editCustomerDetailsInEziDebit(c);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("ChangesSaved"));
            } catch (Exception e) {
                JsfUtil.addErrorMessage(e.getMessage(), configMapFacade.getConfig("PersistenceErrorOccured"));
            }

        }

    }

    public void editDialogue(ActionEvent actionEvent) {

    }

    public String prepareEdit() {
        //current = (Customers) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
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
            getFacade().edit(getSelected());
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersUpdated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
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
                    controller.changeCustomerStatus(cust, selectedState);
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

    public String updatepass() {
        try {
            if (checkPass.length() >= 8) {
                Customers c = getSelected();
                c.setPassword(PasswordService.getInstance().encrypt(checkPass));
                getFacade().edit(c);
                JsfUtil.addSuccessMessage("Password Updated", "Your password was changed to " + checkPass);
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
            
            items = new PfSelectableDataModel<>(ejbFacade.findFilteredCustomers(true, selectedCustomerStates,showNonUsers, isRefreshFromDB()));
            setRefreshFromDB(false);
            // items = getPagination().createPageDataModel();
        }
        return items;
    }

    public PfSelectableDataModel<Notes> getNotesItems() {
        if (notesItems == null) {
            setNotesItems((PfSelectableDataModel<Notes>) getNotesPagination().createPageDataModel());
        }
        return notesItems;
    }

    public void recreateModel() {
        items = null;
        filteredItems = null;
        notesItems = null;
        notesFilteredItems = null;
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

        String y = checkPass;
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

    public void firstNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVAl = (String) o;
            String updatedUsername = newVAl + "." + current.getLastname();
            current.setUsername(updatedUsername.toLowerCase().replace(' ', '_'));
        }
    }

    public void lastNameListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass().equals(String.class)) {
            String newVAl = (String) o;
            String updatedUsername = current.getFirstname() + "." + newVAl;

            current.setUsername(updatedUsername.toLowerCase().replace(' ', '_'));
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

            selectedCustomerStates = new CustomerState[2];
            for (CustomerState cs : customerStateList) {
                if (cs.getCustomerState().contains("ACTIVE")) {
                    selectedCustomerStates[0] = cs;
                }
                if (cs.getCustomerState().contains("ON HOLD")) {
                    selectedCustomerStates[1] = cs;
                }

            }

        }
        return customerStateList;
    }

    /**
     * @return the selectedCustomerStates
     */
    public CustomerState[] getSelectedCustomerStates() {
        return selectedCustomerStates;
    }

    /**
     * @param selectedCustomerStates the selectedCustomerStates to set
     */
    public void setSelectedCustomerStates(CustomerState[] selectedCustomerStates) {
        this.selectedCustomerStates = selectedCustomerStates;
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

    @FacesConverter(forClass = Customers.class)
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
