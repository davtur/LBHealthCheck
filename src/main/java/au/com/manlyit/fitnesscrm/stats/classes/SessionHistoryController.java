package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade;
import au.com.manlyit.fitnesscrm.stats.chartbeans.MySessionsChart1;
import au.com.manlyit.fitnesscrm.stats.classes.util.DatatableSelectionHelper;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Participants;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;
import java.awt.event.ActionEvent;
import java.io.IOException;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.inject.Inject;
import javax.faces.application.FacesMessage;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.SelectItem;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DualListModel;
import org.primefaces.model.SelectableDataModel;

@Named("sessionHistoryController")
@SessionScoped
public class SessionHistoryController implements Serializable {

    private static final Logger logger = Logger.getLogger(SessionHistoryController.class.getName());
    private SessionHistory current;
    private SessionHistory selectedForDeletion;
    private Date selectedSessionTime;
    private PfSelectableDataModel<SessionHistory> items = null;
    private PfSelectableDataModel<SessionHistory> customerItems = null;
    private PfSelectableDataModel<SessionHistory> participantItems = null;
    private List<SessionHistory> filteredItems;
    private List<SessionHistory> participantFilteredItems;
    private Customers[] participantsArray;
    private SessionHistory[] sessionHistoryItems;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbParticipantsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTrainersFacade ejbSessionTrainersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private DatatableSelectionHelper pagination;
    private DatatableSelectionHelper customerPagination;
    private DatatableSelectionHelper participantPagination;
    private int selectedItemIndex;
    private DualListModel<Customers> participants;
    private List<Customers> activeCustomers; // all customers
    private List<Customers> attendingCustomers;
    private List<Customers> trainers;
    private int hourSpinner = 0;
    private int minuteSpinner = 0;

    public SessionHistoryController() {
    }

    public void handleDateSelect(SelectEvent event) {

        Object o = event.getObject();
        if (o.getClass() == Date.class) {
            SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy HH:mm");
            Date date = (Date) event.getObject();
            FacesContext facesContext = FacesContext.getCurrentInstance();
            facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(date)));
        }

    }

    public SessionHistory getSelected() {
        if (current == null) {
            createNewSessionHistory();
        }
        return current;
    }

    private void createNewSessionHistory() {
        current = new SessionHistory();
        current.setSessiondate(new Date());
        GregorianCalendar cl = new GregorianCalendar();
        setHourSpinner(cl.get(Calendar.HOUR_OF_DAY));
        setMinuteSpinner(cl.get(Calendar.MINUTE));
        selectedItemIndex = -1;
        addParticipants();
        addTrainer();

        recreateModel();

    }

    public void setSelected(SessionHistory selected) {
        this.current = selected;
        this.trainers = new ArrayList<>();
        this.attendingCustomers = new ArrayList<>();
        GregorianCalendar cl = new GregorianCalendar();
        cl.setTime(current.getSessiondate());
        setHourSpinner(cl.get(Calendar.HOUR_OF_DAY));
        setMinuteSpinner(cl.get(Calendar.MINUTE));
        Collection<SessionTrainers> st = current.getSessionTrainersCollection();
        Collection<Participants> p = current.getParticipantsCollection();
        for (Participants part : p) {
            attendingCustomers.add(part.getCustomerId());
        }
        for (SessionTrainers train : st) {
            trainers.add(train.getCustomerId());
        }
    }

    public static boolean isUserInRole(String roleName) {

        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    private SessionHistoryFacade getFacade() {
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
    /* public PaginationHelper getPagination() {
     if (pagination == null) {
     pagination = new PaginationHelper(1000000) {

     @Override
     public int getItemsCount() {
     return getFacade().count();
     }

     @Override
     public DataModel createPageDataModel() {
     return new ListDataModel(getFacade().findAll(false));
     }
     };
     }
     return pagination;
     }*/

    public DatatableSelectionHelper getCustomerPagination() {
        if (customerPagination == null) {
            customerPagination = new DatatableSelectionHelper() {

                @Override
                public int getItemsCount() {
                    return getFacade().countSessionsByTrainer(getLoggedInUser());
                }

                @Override
                public PfSelectableDataModel createPageDataModel() {
                    return new PfSelectableDataModel<>(ejbFacade.findSessionsByTrainer(getLoggedInUser(), true));
                }

            };
        }
        return customerPagination;
    }

    public DatatableSelectionHelper getParticipantPagination() {
        if (participantPagination == null) {
            participantPagination = new DatatableSelectionHelper() {

                @Override
                public int getItemsCount() {
                    return getFacade().countSessionsByParticipant(getLoggedInUser());
                }

                @Override
                public PfSelectableDataModel createPageDataModel() {
                    return new PfSelectableDataModel<>(ejbFacade.findSessionsByParticipant(getLoggedInUser(), true));
                }

            };
        }
        return participantPagination;
    }

    /*  public PaginationHelper getCustomerPagination() {
     if (pagination == null) {
     pagination = new PaginationHelper(1000000) {

     @Override
     public int getItemsCount() {
     Customers loggedInUser = null;
     try {
     return ejbFacade.countByCustId(getLoggedInUser().getId());
     } catch (ELException e) {
     JsfUtil.addErrorMessage(e, "Couldn't get customer i Session History controller");
     }

     return 0;
     }

     @Override
     public DataModel createPageDataModel() {
     Customers loggedInUser = null;
     try {
     //loggedInUser = ejbCustomerFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
     loggedInUser = getLoggedInUser();
     String msg = "CreatingDataModel for user: " + loggedInUser.getUsername();
     Logger.getLogger(getClass().getName()).log(Level.INFO, msg);
     return new ListDataModel(ejbFacade.findAllByCustId(loggedInUser.getId(), true));
     } catch (ELException e) {
     JsfUtil.addErrorMessage(e, "Couldn't get customer i Session History controller");
     }

     return null;
     }
     };
     }
     return pagination;
     }*/
    private Customers getLoggedInUser() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected();

    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        // current = (SessionHistory) getItems().getRowData();
        // selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        createNewSessionHistory();
        return "Create";
    }

    public String prepareCreateTrainers() {
        createNewSessionHistory();
        return "CreateSessionInfo";
    }

    private void addTrainer() {
        List<SessionTrainers> parts = new ArrayList<>();

        SessionTrainers p = new SessionTrainers(0);
        p.setCustomerId(getLoggedInUser());
        p.setSessionHistoryId(current);
        parts.add(p);
        current.setSessionTrainersCollection(parts);
    }

    public String prepareCreateMobileReturnMainMenu() {
        createNewSessionHistory();

        return "/mobileMenu";
    }

    public String prepareCreateMobileReturnCreate() {
        createNewSessionHistory();
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            ec.redirect(request.getContextPath() + "/trainer/sessionHistory/CreateSessionMobile.xhtml");
        } catch (IOException ex) {
            Logger.getLogger(SessionHistoryController.class.getName()).log(Level.SEVERE, "prepareCreateMobileReturnCreate", ex);
        }
        return "pm:sessions";
    }
    
    private void updateSessionDateTimes(){
     
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getSessiondate());
            cal.set(Calendar.HOUR_OF_DAY, hourSpinner);
            cal.set(Calendar.MINUTE, minuteSpinner);
            current.setSessiondate(cal.getTime());
    }

    public String createMobile() {
        try {
            updateCurrentParticipants(getAttendingCustomers());
            updateCurrentTrainers(getTrainers());
            setSessionHistoryItems(null);

            if (current.getId() == null) {
                current.setId(0); // auto generated by DB , but cannot be null
                updateSessionDateTimes();
                getFacade().create(current);
            } else {
                updateSessionDateTimes();
                getFacade().edit(current);
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryCreated"));
            return prepareCreateMobileReturnMainMenu();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String create() {
        try {
            current.setId(0); // auto generated by DB , but cannot be null
            updateCurrentParticipants(participants.getTarget());
            updateCurrentTrainers(getTrainers());
            setSessionHistoryItems(null);
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    private void updateCurrentTrainers(List<Customers> trainerList) {

        List<SessionTrainers> sessionTrainersList = new ArrayList<>();
        Collection<SessionTrainers> oldList = current.getSessionTrainersCollection();
        if (oldList != null) {
            for (SessionTrainers st : oldList) {
                ejbSessionTrainersFacade.remove(st);
            }
        }
        current.getSessionTrainersCollection().clear();
        for (Customers cust : trainerList) {
            SessionTrainers p = new SessionTrainers(0);
            p.setCustomerId(cust);
            p.setSessionHistoryId(current);
            sessionTrainersList.add(p);
        }
        //current.getParticipantsCollection().clear();
        current.setSessionTrainersCollection(sessionTrainersList);

    }

    private void updateCurrentParticipants(List<Customers> targets) {
        // List<Customers> targets = participants.getTarget(); //for the pick lIst
        // List<Customers> targets = Arrays.asList(participantsArray);// for the datatable
        // List<Customers> targets = activeCustomers; // for the selectMany  Menu
        List<Participants> parts = new ArrayList<>();
        Collection<Participants> pc = current.getParticipantsCollection();
        if (pc != null) {
            for (Participants p : pc) {
                ejbParticipantsFacade.remove(p);
            }
        }
        for (Customers cust : targets) {
            Participants newPart = new Participants(0);
            newPart.setCustomerId(cust);
            newPart.setSessionHistoryId(current);
            parts.add(newPart);
        }
        //current.getParticipantsCollection().clear();
        current.setParticipantsCollection(parts);

    }

    private void populatePickerFromCurrentSessionHistory() {
        List<Customers> allCustomers = ejbCustomerFacade.findAll(); // all customers
        List<Customers> selectedCustomers = new ArrayList<>();
        Collection<Participants> partic = current.getParticipantsCollection(); //ones already selected.
        if (partic != null) {
            for (Participants part : partic) {
                selectedCustomers.add(part.getCustomerId());
            }
            //remove any of the selected customers from the all customers list
            for (int i = allCustomers.size() - 1; i >= 0; i--) {
                Customers c1 = allCustomers.get(i);
                for (Customers c2 : selectedCustomers) {
                    if (c1.getId() == c2.getId()) {
                        allCustomers.remove(c1);
                    }
                }
            }
            participants = new DualListModel<>(allCustomers, selectedCustomers);
        } else {
            addParticipants();
        }
    }

    private void addParticipants() {
        List<Customers> sourceParticipants = ejbCustomerFacade.findAll(true);
        List<Customers> targetParticipants = new ArrayList<>();
        participants = new DualListModel<>(sourceParticipants, targetParticipants);
    }

    public String prepareEdit() {
        //current = (SessionHistory) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        populatePickerFromCurrentSessionHistory();
        return "Edit";
    }

    public String update() {
        try {
            updateCurrentParticipants(participants.getTarget());
            updateCurrentTrainers(getTrainers());
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryUpdated"));
            return "List";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        //current = (SessionHistory) getItems().getRowData();
        // selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        selectedItemIndex = -1;
        performDestroy();
        recreateModel();
        return "List";
    }

    public void destroyListener(ActionEvent event) {
        Object o = event.getSource();
        if (o.getClass() == SessionHistory.class) {
            current = (SessionHistory) o;

            performDestroy();
            recreateModel();
        }

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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryDeleted"));
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
            //  if (pagination.getPageFirstItem() >= count) {
            //      pagination.previousPage();
            // }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public SelectableDataModel<SessionHistory> getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    public SelectableDataModel<SessionHistory> getCustomerItems() {
        if (customerItems == null) {
            customerItems = getCustomerPagination().createPageDataModel();
        }
        return customerItems;
    }

    public SelectableDataModel<SessionHistory> getParticipantItems() {
        if (participantItems == null) {
            participantItems = getParticipantPagination().createPageDataModel();
        }
        return participantItems;
    }

    public void recreateModel() {
        items = null;
        customerItems = null;
        filteredItems = null;
        participantItems = null;
        participantFilteredItems = null;
        sessionHistoryItems = null;

        FacesContext context = FacesContext.getCurrentInstance();

        try {
            MySessionsChart1 controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
            controller.createChart();
        } catch (ELException e) {
            JsfUtil.addErrorMessage(e, "My Sessions Chart create method exception..");
        }

    }

    /*  public String next() {
     getPagination().nextPage();
     recreateModel();
     return "List";
     }

     public String previous() {
     getPagination().previousPage();
     recreateModel();
     return "List";
     }*/
    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    /**
     * @return the participants
     */
    public DualListModel<Customers> getParticipants() {
        if (participants == null) {
            addParticipants();
        }
        return participants;
    }

    /**
     * @param participants the participants to set
     */
    public void setParticipants(DualListModel<Customers> participants) {
        this.participants = participants;
    }

    /**
     * @return the selectedForDeletion
     */
    public SessionHistory getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(SessionHistory selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    /**
     * @return the activeCustomers
     */
    public Collection<Customers> getActiveTrainers() {
        return ejbCustomerFacade.findAllByGroup("TRAINER", true);
    }

    public Collection<Customers> getActiveCustomers() {

        if (activeCustomers == null) {
            activeCustomers = ejbCustomerFacade.findAllActiveCustomers(true);
        }

        return activeCustomers;

    }

    /**
     * @param activeCustomers the activeCustomers to set
     */
    /*public void setActiveCustomers(List<Customers> activeCustomers) {
     this.activeCustomers = activeCustomers;
     }*/
    /**
     * @return the attendingCustomers
     */
    public List<Customers> getAttendingCustomers() {
        if (attendingCustomers == null) {
            attendingCustomers = new ArrayList<>();
        }
        return attendingCustomers;
    }

    /**
     * @param attendingCustomers the attendingCustomers to set
     */
    public void setAttendingCustomers(List<Customers> attendingCustomers) {
        this.attendingCustomers = attendingCustomers;
    }

    /**
     * @return the participantsArray
     */
    public Customers[] getParticipantsArray() {
        return participantsArray;
    }

    /**
     * @param participantsArray the participantsArray to set
     */
    public void setParticipantsArray(Customers[] participantsArray) {
        this.participantsArray = participantsArray;
    }

    /**
     * @return the trainers
     */
    public List<Customers> getTrainers() {
        return trainers;
    }

    /**
     * @param trainers the trainers to set
     */
    public void setTrainers(List<Customers> trainers) {
        this.trainers = trainers;
    }

    /**
     * @return the sessionHistoryItems
     */
    public SessionHistory[] getSessionHistoryItems() {
        if (sessionHistoryItems == null) {
            String user = FacesContext.getCurrentInstance().getExternalContext().getUserPrincipal().getName();
            Customers cust = ejbCustomerFacade.findCustomerByUsername(user);
            GregorianCalendar startDate = new GregorianCalendar();

            GregorianCalendar endDate = new GregorianCalendar();
            // pull up sessions for the last week on the mobile menu, they can log in to teh desktop version for older dates.
            startDate.set(Calendar.HOUR_OF_DAY, 0);
            startDate.set(Calendar.MINUTE, 0);
            startDate.set(Calendar.SECOND, 0);
            startDate.set(Calendar.MILLISECOND, 0);
            endDate.setTime(startDate.getTime());
            endDate.add(Calendar.HOUR_OF_DAY, 24);
            endDate.add(Calendar.MILLISECOND, -1);
            startDate.add(Calendar.DAY_OF_MONTH, -6);
            List<SessionHistory> sessionItems = getFacade().findSessionsByTrainerAndDateRange(cust, startDate.getTime(), endDate.getTime(), true);
            SessionHistory[] sha = new SessionHistory[sessionItems.size()];
            sessionHistoryItems = sessionItems.toArray(sha);
            if (sessionHistoryItems == null) {
                logger.log(Level.WARNING, "getSessionHistoryItems() sessionHistoryItems are null ");
            }
        }
        return sessionHistoryItems;
    }

    /**
     * @param sessionHistoryItems the sessionHistoryItems to set
     */
    public void setSessionHistoryItems(SessionHistory[] sessionHistoryItems) {
        this.sessionHistoryItems = sessionHistoryItems;
    }

    /**
     * @return the filteredItems
     */
    public List<SessionHistory> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<SessionHistory> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the participantFilteredItems
     */
    public List<SessionHistory> getParticipantFilteredItems() {
        return participantFilteredItems;
    }

    /**
     * @param participantFilteredItems the participantFilteredItems to set
     */
    public void setParticipantFilteredItems(List<SessionHistory> participantFilteredItems) {
        this.participantFilteredItems = participantFilteredItems;
    }

    /**
     * @return the selectedSessionTime
     */
    public Date getSelectedSessionTime() {
        if (selectedSessionTime == null) {
            selectedSessionTime = new Date();
        }
        return selectedSessionTime;
    }

    /**
     * @param selectedSessionTime the selectedSessionTime to set
     */
    public void setSelectedSessionTime(Date selectedSessionTime) {
        this.selectedSessionTime = selectedSessionTime;
    }

    /**
     * @return the hourSpinner
     */
    public int getHourSpinner() {
        return hourSpinner;
    }

    public void hourSpinnerChange(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass() == Integer.class) {
            int newHour = (int) o;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getSessiondate());
            cal.set(Calendar.HOUR_OF_DAY, newHour);
            current.setSessiondate(cal.getTime());

        }
    }

    public void minuteSpinnerChange(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass() == Integer.class) {
            int newMinute = (int) o;
            GregorianCalendar cal = new GregorianCalendar();
            cal.setTime(current.getSessiondate());
            cal.set(Calendar.MINUTE, newMinute);
            current.setSessiondate(cal.getTime());

        }
    }

    /**
     * @param hourSpinner the hourSpinner to set
     */
    public void setHourSpinner(int hourSpinner) {
        this.hourSpinner = hourSpinner;
    }

    /**
     * @return the minuteSpinner
     */
    public int getMinuteSpinner() {
        return minuteSpinner;
    }

    /**
     * @param minuteSpinner the minuteSpinner to set
     */
    public void setMinuteSpinner(int minuteSpinner) {
        this.minuteSpinner = minuteSpinner;
    }

    @FacesConverter(forClass = SessionHistory.class)
    public static class SessionHistoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionHistoryController controller = (SessionHistoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionHistoryController");
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
            if (object instanceof SessionHistory) {
                SessionHistory o = (SessionHistory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SessionHistoryController.class.getName());
            }
        }
    }
}
