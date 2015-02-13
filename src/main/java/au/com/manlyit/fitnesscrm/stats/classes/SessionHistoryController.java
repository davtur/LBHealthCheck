package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade;
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
import com.lowagie.text.BadElementException;

import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;

import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;

import java.io.Serializable;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Objects;
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
import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import org.apache.poi.hssf.usermodel.HSSFCell;
import org.apache.poi.hssf.usermodel.HSSFCellStyle;
import org.apache.poi.hssf.usermodel.HSSFRow;
import org.apache.poi.hssf.usermodel.HSSFSheet;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.hssf.util.HSSFColor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.DataFormat;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.DualListModel;

@Named("sessionHistoryController")
@SessionScoped
public class SessionHistoryController implements Serializable {

    private static final Logger logger = Logger.getLogger(SessionHistoryController.class.getName());
    private SessionHistory current;
    private SessionHistory selectedForDeletion;
    private Date selectedSessionTime;
    private PfSelectableDataModel<SessionHistory> items = null;
    private PfSelectableDataModel<SessionHistory> customerItems = null;
    private PfSelectableDataModel<SessionHistory> customerOrTrainerItems = null;
    private PfSelectableDataModel<SessionHistory> participantItems = null;
    private List<Customers> selectableActiveCustomers = null;
    private List<SessionHistory> filteredItems;
    private List<SessionHistory> customerOrTrainerfilteredItems;
    private List<SessionHistory> participantFilteredItems;
    private Customers[] participantsArray;
    private Boolean[] checkedCustomers;
    private SessionHistory[] sessionHistoryItems;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.GroupsFacade ejbGroupsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbParticipantsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTrainersFacade ejbSessionTrainersFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private AuditLogFacade auditLogFacade;
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
    private int numberThatAttendedSession = 0;
    private boolean showAllSessionsByTrainer = false;
    private String exportFileName = "export";

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
        GregorianCalendar cl = new GregorianCalendar();
        cl.set(Calendar.MINUTE, 0);
        current.setSessiondate(new Date());

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
        selectableActiveCustomers = null;
        List<Customers> sac = getSelectableActiveCustomers();
        GregorianCalendar cl = new GregorianCalendar();
        cl.setTime(current.getSessiondate());
        setHourSpinner(cl.get(Calendar.HOUR_OF_DAY));
        setMinuteSpinner(cl.get(Calendar.MINUTE));
        Collection<SessionTrainers> st = current.getSessionTrainersCollection();
        Collection<Participants> p = current.getParticipantsCollection();
        for (Participants part : p) {
            attendingCustomers.add(part.getCustomerId());
            for (int x = 0; x < sac.size(); x++) {
                if (Objects.equals(part.getCustomerId().getId(), sac.get(x).getId())) {
                    checkedCustomers[x] = true;
                }
            }
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

                    return new PfSelectableDataModel<>(ejbFacade.findAll(false));
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
                    if (showAllSessionsByTrainer) {
                        return getFacade().countSessionsByTrainer(getSelectedCustomer());
                    } else {
                        FacesContext context = FacesContext.getCurrentInstance();
                        MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                        return getFacade().countSessionsByTrainerAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                    }
                }

                @Override
                public PfSelectableDataModel createPageDataModel() {
                    if (showAllSessionsByTrainer) {
                        return new PfSelectableDataModel<>(ejbFacade.findSessionsByTrainer(getSelectedCustomer(), false));
                    } else {
                        FacesContext context = FacesContext.getCurrentInstance();
                        MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                        List<SessionHistory> shList = ejbFacade.findSessionsByTrainerAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                        if (shList.isEmpty()) {
                            return null;
                        }
                        return new PfSelectableDataModel<>(shList);

                    }
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
                    FacesContext context = FacesContext.getCurrentInstance();
                    MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                    return getFacade().countSessionsByParticipantAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime());
                }

                @Override
                public PfSelectableDataModel createPageDataModel() {
                    FacesContext context = FacesContext.getCurrentInstance();
                    MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                    return new PfSelectableDataModel<>(ejbFacade.findSessionsByParticipantAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), true));
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
     return ejbFacade.countByCustId(getSelectedCustomer().getId());
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
     loggedInUser = getSelectedCustomer();
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
    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();

        MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
        //CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return mySessionsChart1Controller.getSelectedCustomer();

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
        p.setCustomerId(getSelectedCustomer());
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

    private void updateSessionDateTimes() {

        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(current.getSessiondate());
        cal.set(Calendar.HOUR_OF_DAY, hourSpinner);
        cal.set(Calendar.MINUTE, minuteSpinner);
        current.setSessiondate(cal.getTime());
    }

    public String createMobileDatalist() {
        try {
            attendingCustomers = new ArrayList<>();
            for (int x = 0; x < checkedCustomers.length; x++) {
                 Boolean b = checkedCustomers[x];
                if (b != null && b == true) {
                    Customers selectedCust = selectableActiveCustomers.get(x);
                    attendingCustomers.add(selectedCust);
                }
            }
            participants.setTarget(attendingCustomers);
            createDialogue();
            return prepareCreateMobileReturnMainMenu();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String createMobile() {
        try {
            createDialogue();
            return prepareCreateMobileReturnMainMenu();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue() {
        try {
            updateCurrentParticipants(participants.getTarget());
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
            try {
                FacesContext context = FacesContext.getCurrentInstance();
                CustomersController customersController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                Customers cust = customersController.getLoggedInUser();
                String auditDetails = "Type:" + current.getSessionTypesId().getName() + " Date/Time:  " + current.getSessiondate().toString() + "  Participants:  " + current.getParticipantsAsString() + " ";
                String changedFrom = "NULL";
                String changedTo = "New Session " + current.getSessionTypesId().getName();
                auditLogFacade.audit(cust, cust, "Create Session", auditDetails, changedFrom, changedTo);
            } catch (Exception e) {
                logger.log(Level.SEVERE, "Add new Session audit log failed due to an unhandled exception.", e);
            }
            current = null;

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public void onTransfer() {
        logger.log(Level.INFO, "Picklist Transfer");
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
        List<Customers> sourceParticipants = ejbCustomerFacade.findAllActiveCustomers(true);
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

    public PfSelectableDataModel<SessionHistory> getItems() {
        if (items == null) {
            items = new PfSelectableDataModel<>(ejbFacade.findAll(false));
        }
        return items;
    }

    public PfSelectableDataModel<SessionHistory> getCustomerItems() {
        if (customerItems == null) {
            //customerItems = getCustomerPagination().createPageDataModel();
            if (showAllSessionsByTrainer) {
                customerItems = new PfSelectableDataModel<>(ejbFacade.findSessionsByTrainer(getSelectedCustomer(), false));
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
                List<SessionHistory> shList = ejbFacade.findSessionsByTrainerAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                if (shList.isEmpty()) {
                    customerItems = null;
                    return customerItems;
                }
                customerItems = new PfSelectableDataModel<>(shList);

            }

        }
        return customerItems;
    }

    public PfSelectableDataModel<SessionHistory> getCustomerOrTrainerItems() {
        if (customerOrTrainerItems == null) {
            //customerItems = getCustomerPagination().createPageDataModel();
            boolean isTrainer = ejbGroupsFacade.isCustomerInGroup(getSelectedCustomer(), "TRAINER");
            List<SessionHistory> shList = null;
            if (showAllSessionsByTrainer) {
                customerOrTrainerItems = new PfSelectableDataModel<>(ejbFacade.findSessionsByTrainer(getSelectedCustomer(), false));
            } else {
                FacesContext context = FacesContext.getCurrentInstance();
                MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);

                if (isTrainer == false) {
                    shList = ejbFacade.findSessionsByParticipantAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                } else {
                    shList = ejbFacade.findSessionsByTrainerAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                    logger.log(Level.FINE, "Get Sessions for Trainer:{0}, No.Sessions:{1},startdate:{2},End date:{3}", new Object[]{getSelectedCustomer().getUsername(), shList.size(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime()});
                }

                //   List<SessionHistory> shList = ejbFacade.findSessionsByTrainerAndDateRange(getSelectedCustomer(), mySessionsChart1Controller.getChartStartTime(), mySessionsChart1Controller.getChartEndTime(), false);
                if (shList.isEmpty()) {
                    customerOrTrainerItems = null;
                    customerOrTrainerfilteredItems = null;
                    return customerOrTrainerItems;
                }
                customerOrTrainerfilteredItems = null;
                customerOrTrainerItems = new PfSelectableDataModel<>(shList);

            }

        }
        return customerOrTrainerItems;
    }

    public PfSelectableDataModel<SessionHistory> getParticipantItems() {
        if (participantItems == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
            Customers user = getSelectedCustomer();
            Date start = mySessionsChart1Controller.getChartStartTime();
            Date end = mySessionsChart1Controller.getChartEndTime();
            participantItems = new PfSelectableDataModel<>(ejbFacade.findSessionsByParticipantAndDateRange(user, start, end, true));
            //participantItems = getParticipantPagination().createPageDataModel();
        }
        return participantItems;
    }

    public void recreateTrainerSessionsTableModel() {
        customerItems = null;
        filteredItems = null;
       // RequestContext requestContext = RequestContext.getCurrentInstance();

        // requestContext.execute("PF('sessionsDataTable').filter();");
    }

    public void recreateModel() {
        items = null;
        customerItems = null;
        filteredItems = null;
        customerOrTrainerItems = null;
        customerOrTrainerfilteredItems = null;
        participantItems = null;
        participantFilteredItems = null;
        sessionHistoryItems = null;
        activeCustomers = null;
        selectableActiveCustomers = null;

        FacesContext context = FacesContext.getCurrentInstance();

        try {
            MySessionsChart1 controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);
           //            controller.createChart();
            
            controller.recreateModel();
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

    /**
     * @return the showAllSessionsByTrainer
     */
    public boolean isShowAllSessionsByTrainer() {
        return showAllSessionsByTrainer;
    }

    /**
     * @param showAllSessionsByTrainer the showAllSessionsByTrainer to set
     */
    public void setShowAllSessionsByTrainer(boolean showAllSessionsByTrainer) {
        customerItems = null;
        filteredItems = null;

        this.showAllSessionsByTrainer = showAllSessionsByTrainer;
    }

    public void preProcessXLS(Object document) {
        String trainer = "Trainer Session History for " + getSelectedCustomer().getFirstname() + " " + getSelectedCustomer().getLastname();

        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        cell.setCellValue(trainer);
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
        String trainer = "Trainer Session History for " + getSelectedCustomer().getFirstname() + " " + getSelectedCustomer().getLastname();
        HSSFWorkbook wb = (HSSFWorkbook) document;
        HSSFSheet sheet = wb.getSheetAt(0);
        sheet.shiftRows(0, sheet.getLastRowNum(), 7);
        HSSFRow row = sheet.createRow(0);
        HSSFCell cell = row.createCell(0);
        HSSFCellStyle cellStyle = wb.createCellStyle();
        DataFormat df = wb.createDataFormat();
        cellStyle.setDataFormat(df.getFormat("YYYY-MM-DD HH:MM:SS"));
        cell.setCellValue(trainer);
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
        setSessionHistoryExportFileName();
    }

    public void setSessionHistoryExportFileName() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd-MM-yyyy");
        FacesContext context = FacesContext.getCurrentInstance();
        MySessionsChart1 mySessionsChart1Controller = (MySessionsChart1) context.getApplication().evaluateExpressionGet(context, "#{mySessionsChart1}", MySessionsChart1.class);

        String name = "sessions-" + getSelectedCustomer().getFirstname() + "-" + getSelectedCustomer().getLastname() + "--" + sdf.format(mySessionsChart1Controller.getChartStartTime()) + "-to-" + sdf.format(mySessionsChart1Controller.getChartEndTime());
        setExportFileName(name);
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
        pdf.add(Image.getInstance(new URL("https://services.purefitnessmanly.com.au/FitnessStats/resources/images/logo.png")));
        String trainer = "Trainer Session History for " + getSelectedCustomer().getFirstname() + " " + getSelectedCustomer().getLastname();
        pdf.addTitle(trainer);
        pdf.add(new Paragraph(trainer));
        pdf.add(new Paragraph(" "));
        setSessionHistoryExportFileName();
    }

    public void postProcessPDF(Object document) throws IOException,
            BadElementException, DocumentException {

    }

    /**
     * @return the checkedCustomers
     */
    public Boolean[] getCheckedCustomers() {
        return checkedCustomers;
    }

    /**
     * @param checkedCustomers the checkedCustomers to set
     */
    public void setCheckedCustomers(Boolean[] checkedCustomers) {
        this.checkedCustomers = checkedCustomers;
    }

    /**
     * @return the selectableActiveCustomers
     */
    public List<Customers> getSelectableActiveCustomers() {
        if (selectableActiveCustomers == null) {
            selectableActiveCustomers = ejbCustomerFacade.findAllActiveCustomers(true);
            checkedCustomers = new Boolean[selectableActiveCustomers.size()];
        }
        return selectableActiveCustomers;
    }

    /**
     * @param selectableActiveCustomers the selectableActiveCustomers to set
     */
    public void setSelectableActiveCustomers(List<Customers> selectableActiveCustomers) {
        this.selectableActiveCustomers = selectableActiveCustomers;
    }

    /**
     * @return the exportFileName
     */
    public String getExportFileName() {
        return exportFileName;
    }

    /**
     * @param exportFileName the exportFileName to set
     */
    public void setExportFileName(String exportFileName) {
        this.exportFileName = exportFileName;
    }

    /**
     * @return the customerOrTrainerfilteredItems
     */
    public List<SessionHistory> getCustomerOrTrainerfilteredItems() {
        return customerOrTrainerfilteredItems;
    }

    /**
     * @param customerOrTrainerfilteredItems the customerOrTrainerfilteredItems
     * to set
     */
    public void setCustomerOrTrainerfilteredItems(List<SessionHistory> customerOrTrainerfilteredItems) {
        this.customerOrTrainerfilteredItems = customerOrTrainerfilteredItems;
    }

    /**
     * @return the numberThatAttendedSession
     */
    public int getNumberThatAttendedSession() {
        int count = 0;
        if (checkedCustomers != null) {
            for (int x = 0;x <checkedCustomers.length; x++) {
                Boolean b = checkedCustomers[x];
                if (b != null && b == true) {
                    count++;
                }
            }
        }
        numberThatAttendedSession = count;
        return numberThatAttendedSession;
    }

    /**
     * @param numberThatAttendedSession the numberThatAttendedSession to set
     */
    public void setNumberThatAttendedSession(int numberThatAttendedSession) {

        this.numberThatAttendedSession = numberThatAttendedSession;
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
