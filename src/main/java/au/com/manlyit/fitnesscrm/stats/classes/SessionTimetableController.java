package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SessionTimetable;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SessionTimetableFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.TimetableRows;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.SortOrder;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;

@Named("sessionTimetableController")
@SessionScoped
public class SessionTimetableController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(SessionTimetableController.class.getName());
    private SessionTimetable current;
    private SessionTimetable selectedForDeletion;
    private DataModel<SessionTimetable> items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTimetableFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade sessionHistoryFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private int sessionForTheWeekMaxColumns = 1;
    private List<SessionTimetable> filteredItems;
    private SessionTimetable[] multiSelected;
    private Date timetableStartDate;
    private MapModel simpleModel;
    private SessionHistory selectedSessionHistory;

    public SessionTimetableController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public SessionTimetable getSelected() {
        if (current == null) {
            current = new SessionTimetable();
            selectedItemIndex = -1;
        }
        return current;
    }

    public MapModel getSimpleModel() {
        if (simpleModel == null) {
            simpleModel = new DefaultMapModel();

            //Shared coordinates - Double
            LatLng coord1 = new LatLng(36.879466, 30.667648);
            //LatLng coord2 = new LatLng(36.883707, 30.689216);
            // LatLng coord3 = new LatLng(36.879703, 30.706707);
            // LatLng coord4 = new LatLng(36.885233, 30.702323);

            //Basic marker
            simpleModel.addOverlay(new Marker(coord1, "Konyaalti"));
            //simpleModel.addOverlay(new Marker(coord2, "Ataturk Parki"));
            //simpleModel.addOverlay(new Marker(coord3, "Karaalioglu Parki"));
            //simpleModel.addOverlay(new Marker(coord4, "Kaleici"));  

        }

        return simpleModel;
    }

    public void setSelected(SessionTimetable selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private SessionTimetableFacade getFacade() {
        return ejbFacade;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel<SessionTimetable> createPageDataModel() {
                    return new ListDataModel<>(getFacade().findAll());
                }
            };
        }
        return pagination;
    }

    public void populateSessions() {

        List<SessionTimetable> sessions = ejbFacade.findAll();

        for (SessionTimetable st : sessions) {

            cloneSessionsFromTimetable(st, 90);

        }

    }

    private void cloneSessionsFromTimetable(SessionTimetable st, int daysIntoFuture) {
        GregorianCalendar startCal = new GregorianCalendar();
        GregorianCalendar endCal = new GregorianCalendar();
        try {
            endCal.add(Calendar.DAY_OF_YEAR, daysIntoFuture);
            GregorianCalendar templateTime = new GregorianCalendar();
            templateTime.setTime(st.getSessiondate());
            startCal.set(Calendar.HOUR_OF_DAY, templateTime.get(Calendar.HOUR_OF_DAY));
            startCal.set(Calendar.MINUTE, templateTime.get(Calendar.MINUTE));
            startCal.set(Calendar.SECOND, templateTime.get(Calendar.SECOND));
            startCal.set(Calendar.MILLISECOND, templateTime.get(Calendar.MILLISECOND));

            while (startCal.compareTo(endCal) < 0) {

                while (startCal.get(Calendar.DAY_OF_WEEK) != templateTime.get(Calendar.DAY_OF_WEEK)) {
                    startCal.add(Calendar.DAY_OF_YEAR, -1);
                }
                SessionHistory sh = new SessionHistory(0, startCal.getTime());
                ArrayList<SessionTrainers> ac = new ArrayList<>();
                SessionTrainers trainers = new SessionTrainers(0);
                trainers.setCustomerId(st.getTrainerId());
                trainers.setSessionHistoryId(sh);
                ac.add(trainers);
                sh.setSessionTrainersCollection(ac);
                sh.setSessionTemplate(st);
                sh.setParticipantsCollection(new ArrayList<>());
                sh.setSessionTypesId(st.getSessionTypesId());
                sh.setComments(st.getComments());
                SessionHistory existing = sessionHistoryFacade.findSessionBySessionTimetable(sh, st);
                if (existing == null) {
                    sessionHistoryFacade.create(sh);
                }
                startCal.add(Calendar.DAY_OF_YEAR, 7);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "cloneSessionsFromTimetable", e);
        }

    }

    public List<TimetableRows> getSessionForTheWeekItems() {

        ArrayList<TimetableRows> daysOfWeek = new ArrayList<>();
        sessionForTheWeekMaxColumns = 1;
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getTimetableStartDate());
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        GregorianCalendar endCal = new GregorianCalendar();
        endCal.setTime(startCal.getTime());
        endCal.add(Calendar.DAY_OF_YEAR, 1);

        for (int index = 0; index < 7; index++) {
            List<SessionHistory> sessions;

            sessions = sessionHistoryFacade.loadDateRange(0, 100, "sessiondate", SortOrder.ASCENDING, null, startCal.getTime(), endCal.getTime(), "sessiondate");
            daysOfWeek.add(new TimetableRows(startCal.getTime(), sessions));
            endCal.add(Calendar.DAY_OF_YEAR, 1);
            startCal.add(Calendar.DAY_OF_YEAR, 1);
            if (sessions.size() > sessionForTheWeekMaxColumns) {
                sessionForTheWeekMaxColumns = sessions.size();
            }
        }

        return daysOfWeek;
    }
    
    public void incrementWeek(){
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getTimetableStartDate());
        startCal.add(Calendar.DAY_OF_YEAR, 7);
        setTimetableStartDate(startCal.getTime());
    }
    public void decrementWeek(){
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getTimetableStartDate());
        startCal.add(Calendar.DAY_OF_YEAR, -7);
        setTimetableStartDate(startCal.getTime());
    }

    public void setToCurrentWeek(){
       
        setTimetableStartDate(new Date());
    }

    
    
    public String signUpFromTimetable(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        String sessionHistoryId = context.getExternalContext().getRequestParameterMap().get("sessionHistorySignupId");
        if (sessionHistoryId != null) {
            SessionHistory sh = sessionHistoryFacade.find(Integer.valueOf(sessionHistoryId));
            logger.log(Level.INFO, "signUpFromTimetable: {0}", new Object[]{sh.getSessiondate().toString()});
        }
        return "index.xhtml";
    }

    public String bookFromTimetable(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        String sessionHistoryId = context.getExternalContext().getRequestParameterMap().get("sessionHistoryBookingId");
        if (sessionHistoryId != null) {
            SessionHistory sh = sessionHistoryFacade.find(Integer.valueOf(sessionHistoryId));
            logger.log(Level.INFO, "BookFromTimetable: {0}", new Object[]{sh.getSessiondate().toString()});
        }
        return "index.xhtml";
    }

    /**
     * @return the filteredItems
     */
    public List<SessionTimetable> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<SessionTimetable> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public SessionTimetable[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(SessionTimetable[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (SessionTimetable)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new SessionTimetable();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionTimetableCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue(ActionEvent actionEvent) {
        try {
            current.setId(0);
            getFacade().create(current);
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionTimetableCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (SessionTimetable)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionTimetableUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void destroy() {
        performDestroy();
        recreateModel();
        current = null;
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

    public SessionTimetable getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(SessionTimetable selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionTimetableDeleted"));
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
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel<SessionTimetable> getItems() {
        if (items == null) {
            items = new ListDataModel<>(getFacade().findAll());
        }
        return items;
    }

    private void recreateModel() {
        items = null;
        filteredItems = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public void handleDateSelect(SelectEvent event) {

        Date date = (Date) event.getObject();

        //Add facesmessage
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        // file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl

        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Collection<SessionTimetable> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        SessionTimetable cm = (SessionTimetable) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the timetableStartDate
     */
    public Date getTimetableStartDate() {
        if (timetableStartDate == null) {
            GregorianCalendar date1 = new GregorianCalendar();

            while (date1.get(Calendar.DAY_OF_WEEK) != Calendar.MONDAY) {
                date1.add(Calendar.DATE, -1);
            }
            timetableStartDate = date1.getTime();
        }
        return timetableStartDate;
    }

    /**
     * @param timetableStartDate the timetableStartDate to set
     */
    public void setTimetableStartDate(Date timetableStartDate) {
        this.timetableStartDate = timetableStartDate;
    }

    /**
     * @return the sessionForTheWeekMaxColumns
     */
    public int getSessionForTheWeekMaxColumns() {
        return sessionForTheWeekMaxColumns;
    }

    /**
     * @param sessionForTheWeekMaxColumns the sessionForTheWeekMaxColumns to set
     */
    public void setSessionForTheWeekMaxColumns(int sessionForTheWeekMaxColumns) {
        this.sessionForTheWeekMaxColumns = sessionForTheWeekMaxColumns;
    }

    /**
     * @return the selectedSessionHistory
     */
    public SessionHistory getSelectedSessionHistory() {
        return selectedSessionHistory;
    }

    /**
     * @param selectedSessionHistory the selectedSessionHistory to set
     */
    public void setSelectedSessionHistory(SessionHistory selectedSessionHistory) {
        this.selectedSessionHistory = selectedSessionHistory;
    }

    @FacesConverter(value = "sessionTimetableControllerConverter", forClass = SessionTimetable.class)
    public static class SessionTimetableControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionTimetableController controller = (SessionTimetableController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionTimetableController");
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
            if (object instanceof SessionTimetable) {
                SessionTimetable o = (SessionTimetable) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SessionTimetableController.class.getName());
            }
        }

    }

}
