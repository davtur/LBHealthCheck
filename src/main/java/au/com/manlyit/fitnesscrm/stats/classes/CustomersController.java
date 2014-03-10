package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.chartbeans.MySessionsChart1;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import java.io.IOException;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.faces.FacesException;
import javax.inject.Named;
import java.util.List;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
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
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TabChangeEvent;
import org.primefaces.model.SelectableDataModel;

@Named("customersController")
@SessionScoped
public class CustomersController implements Serializable {

    private Customers current;
    private Customers selectedForDeletion;
    private Notes selectedNoteForDeletion;
    private PfSelectableDataModel<Customers> items = null;
    private PfSelectableDataModel<Notes> notesItems = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.DemographicTypesFacade ejbDemoFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.GroupsFacade ejbGroupsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.NotesFacade ejbNotesFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
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

//resp.setStatus(HttpServletResponse.SC_MOVED_PERMANENTLY);
//resp.setHeader("Location", "/AppName/site/ie/home.jsp");
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public void setSelected(Customers cust) {
        if (cust != null) {
            current = cust;
            selectedItemIndex = -1;
            checkPass = current.getPassword();
            setNotesItems(null);
            notesFilteredItems = null;
            recreateAllAffectedPageModels();
            setCustomerTabsEnabled(true);
            FacesContext context = FacesContext.getCurrentInstance();
            EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
            controller.setSelectedCustomer(cust);

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

    private Customers setCusomerDefaults(Customers current) {
        current.setCountryId(61);
        current.setCity("Sydney");
        current.setAddrState("NSW");
        current.setSuburb("Manly");
        current.setPostcode("2095");
        current.setReferredby(2);

        //for debug
        current.setUsername("firstname.lastname");
        current.setFirstname("Firstname");
        current.setLastname("Lastname");
        current.setPassword("MyStr0ngP@ssw0rd");
        current.setEmailAddress("myname@myemail.com.au");
        current.setFax("0212345678");
        current.setTelephone("0212345678");

        GregorianCalendar gc = new GregorianCalendar();
        gc.add(Calendar.YEAR, -30);
        current.setDob(gc.getTime());
        return current;
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
                    return new PfSelectableDataModel<>(ejbFacade.findAll());
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
                    return ejbNotesFacade.countNotesByCustomer(current);
                }

                @Override
                public PfSelectableDataModel createPageDataModel() {
                    return new PfSelectableDataModel<>(ejbNotesFacade.findNotesByCustomer(current));
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
        String user = current.getUsername();
        current = impersonate;
        setImpersonating(true);
        recreateAllAffectedPageModels();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ImpersonateCustomer") + current.getUsername());
        String message = "Impersonated User: " + current.getUsername() + " from  " + user;
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);

    }

    private void recreateAllAffectedPageModels() {
        FacesContext context = FacesContext.getCurrentInstance();
        // recreate any datatables that have stale data after changing users
        String message = "Recreating data models for user : " + current.getUsername() + ". ";
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);

        SessionHistoryController controller = (SessionHistoryController) context.getApplication().evaluateExpressionGet(context, "#{sessionHistoryController}", SessionHistoryController.class);
        controller.recreateModel();
        MySessionsChart1 c2 = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
        c2.recreateModel();

    }

    public void handleUserChange() {
        JsfUtil.addSuccessMessage("User Selected: " + impersonate);
    }

    public void unimpersonateUser(ActionEvent event) {
        String user = current.getUsername();
        getLoggedInCustomer();
        setImpersonate(current);
        setImpersonating(false);
        recreateAllAffectedPageModels();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("UnimpersonateCustomer") + current.getUsername());
        String message = "Unimpersonated User : " + user + " and changed back to " + current.getUsername();
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
        current = setCusomerDefaults(new Customers());
        selectedItemIndex = -1;
        return "Create";
    }

    public void prepareCreateAjax() {
        current = setCusomerDefaults(new Customers());
        selectedItemIndex = -1;

    }

    public String create() {
        try {
            current.setId(0);
            current.setPassword(PasswordService.getInstance().encrypt(current.getPassword()));

            if (getFacade().find(current.getId()) == null) {
                getFacade().create(current);
                Groups grp = new Groups(0, "USER");
                grp.setUsername(current);
                ejbGroupsFacade.create(grp);
                recreateModel();
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersCreated"));
                return prepareCreate();
            } else {
                JsfUtil.addErrorMessage("The user you are trying to create (" + current.getUsername() + ") already exists in the database.");
                return null;
            }

        } catch (Exception e) {
            String cause = e.getCause().getCause().getMessage();
            if (cause.toLowerCase().indexOf("duplicate") != -1) {
                JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("DuplicateCustomerExists"));
                return null;
            } else {
                JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
                return null;
            }
        }
    }

    public void createDialogue(ActionEvent actionEvent) {

        if (current.getId() == null || getFacade().find(current.getId()) == null) {
            // does not exist so create a new customer
            try {
                current.setId(0);
                getFacade().create(current);
                Groups grp = new Groups(0, "USER");
                grp.setUsername(current);
                ejbGroupsFacade.create(grp);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersCreated"));
            } catch (Exception e) {
                String cause = e.getCause().getCause().getMessage();
                if (cause.toLowerCase().indexOf("duplicate") != -1) {
                    JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("DuplicateCustomerExists"));
                } else {
                    JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
                }
            }
        } else {
            // exists so update only
            try {
                getFacade().edit(current);
                recreateModel();
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

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("CustomersUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String updatepass() {
        try {
            if (checkPass.length() >= 8) {
                current.setPassword(PasswordService.getInstance().encrypt(checkPass));
                getFacade().edit(current);
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

    public String logout() {
        FacesContext fc = FacesContext.getCurrentInstance();

        // invalidate session
        ExternalContext ec = fc.getExternalContext();
        HttpSession session = (HttpSession) ec.getSession(false);
        session.invalidate();

        // redirect to the login / home page
        try {
            //ec.redirect(ec.getRequestContextPath());

            ec.redirect(configMapFacade.getConfig("WebsiteURL"));
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Log out Failed");
            //LOG.error("Redirect to the login page failed");
            throw new FacesException(e);
        }

        return null;
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
            getFacade().remove(current);
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
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    /*  public List<Customers> getItems(){
     if (items == null) {
     items = ejbFacade.findAll();
     }
     return items;
     }*/
    public SelectableDataModel<Customers> getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    public SelectableDataModel<Notes> getNotesItems() {
        if (notesItems == null) {
            setNotesItems((PfSelectableDataModel<Notes>) getNotesPagination().createPageDataModel());
        }
        return notesItems;
    }

    public void recreateModel() {
        items = null;
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

    public void rowSelectEvent(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass().equals(Customers.class)) {
            Customers cust = (Customers) o;
            setSelected(current);
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
        }

        FacesMessage msg = new FacesMessage("Tab Changed", "Active Tab: " + tabName);

        FacesContext.getCurrentInstance().addMessage(null, msg);
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
            impersonate = current;
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
