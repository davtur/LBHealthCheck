package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SessionTimetable;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SessionTimetableFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.CrmScheduleEvent;
import au.com.manlyit.fitnesscrm.stats.classes.util.TimetableRows;
import au.com.manlyit.fitnesscrm.stats.classes.util.TimetableScheduleEvent;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Schedule;
import au.com.manlyit.fitnesscrm.stats.db.SessionBookings;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;
import java.io.IOException;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.ExternalContext;
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
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.servlet.http.HttpServletRequest;
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.ScheduleEntryMoveEvent;
import org.primefaces.event.ScheduleEntryResizeEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.LazyScheduleModel;
import org.primefaces.model.ScheduleEvent;
import org.primefaces.model.ScheduleModel;
import org.primefaces.model.SortOrder;
import org.primefaces.model.map.DefaultMapModel;
import org.primefaces.model.map.LatLng;
import org.primefaces.model.map.MapModel;
import org.primefaces.model.map.Marker;
import org.primefaces.push.Status;

@Named("sessionTimetableController")
@SessionScoped
public class SessionTimetableController implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(SessionTimetableController.class.getName());
    private static final int DAYS_AHEAD_TO_POPULATE_TIMETABLE = 90;
    private SessionTimetable current;
    private SessionTimetable selectedForDeletion;
    private SessionHistory selectedTimetableSession;
    private DataModel<SessionTimetable> items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionTimetableFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionBookingsFacade ejbSessionBookingsFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade sessionHistoryFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ScheduleFacade ejbScheduleFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String eventStyleClass = "";
    private int sessionForTheWeekMaxColumns = 1;
    private List<SessionTimetable> filteredItems;
    private SessionTimetable[] multiSelected;
    private Date timetableStartDate;
    private MapModel simpleModel;
    private SessionHistory selectedSessionHistory;
    private SessionHistory bookingButtonSessionHistory;
    private SessionHistory signupButtonSessionHistory;
    private List<Schedule> filteredScheduleItems;
    private Schedule[] multiSelectedScheduleItems;
    private ScheduleModel eventModel;
    private TimetableScheduleEvent event;
    private Schedule selectedSchedule;

    public SessionTimetableController() {
    }

    public enum TimetableSessionStatus {
        PUBLISHED(0, "Published"), HIDDEN(1, "Hidden");
        private final int value;
        private final String label;

        private TimetableSessionStatus(int value, String label) {
            this.value = value;
            this.label = label;
        }

        public int getValue() {
            return value;
        }

        public String getLabel() {
            return label;
        }
    }

    public TimetableSessionStatus[] getTimetableSessionStatusValues() {
        return TimetableSessionStatus.values();
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

    public void persistEvent(CrmScheduleEvent event) {
        CrmScheduleEvent ssievent = event;
        Schedule ss = new Schedule(0, ssievent.getTitle(), ssievent.getStartDate(), ssievent.getEndDate());
        ss.setShedAllday(ssievent.isAllDay());
        ss.setDataObject(ssievent.getData());
        ss.setSchedReminder(ssievent.isAddReminder());
        ss.setSchedRemindDate(ssievent.getReminderDate());
        selectedSchedule = ss;
        try {
            ejbScheduleFacade.create(selectedSchedule);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleCreated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
        ssievent.setDatabasePK(selectedSchedule.getId());
        selectedSchedule = null;
    }

    private void deleteEvent(CrmScheduleEvent event) {
        CrmScheduleEvent ssievent = event;
        selectedSchedule = ejbScheduleFacade.find(ssievent.getDatabasePK());
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    private void editEvent(ScheduleEvent event) {
        CrmScheduleEvent ssievent = null;

        if (event != null) {
            if (event.getClass() == CrmScheduleEvent.class) {
                ssievent = (CrmScheduleEvent) event;
                Schedule ss = ejbScheduleFacade.find(ssievent.getDatabasePK());
                ss.setShedEnddate(ssievent.getEndDate());
                ss.setShedStartdate(ssievent.getStartDate());
                ss.setShedTitle(ssievent.getTitle());
                ss.setSchedRemindDate(ssievent.getReminderDate());
                ss.setSchedReminder(ssievent.isAddReminder());
                ss.setShedAllday(ssievent.isAllDay());

                ss.setDataObject(ssievent.getData());

                selectedSchedule = ss;
                try {
                    ejbScheduleFacade.edit(selectedSchedule);
                    JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleUpdated"));

                } catch (Exception e) {
                    JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

                }

            }
        }
        selectedSchedule = null;
    }

    public void timetableRowSelected(SelectEvent event) {

    }

    /**
     * @param eventModel the eventModel to set
     */
    public void setEventModel(ScheduleModel eventModel) {
        this.eventModel = eventModel;
    }

    /**
     * @return the event
     */
    public TimetableScheduleEvent getEvent() {
        if (event == null) {
            event = new TimetableScheduleEvent();
        }
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(TimetableScheduleEvent event) {
        this.event = event;
    }

    public void onEventSelect(SelectEvent selectEvent) {
        Object o = selectEvent;
        if (o != null) {
            o = selectEvent.getObject();
            if (o.getClass() == TimetableScheduleEvent.class) {
                setEvent((TimetableScheduleEvent) selectEvent.getObject());
                RequestContext.getCurrentInstance().execute("PF('eventDialog').show()");
            }
        }
    }

    public void onDateSelect(SelectEvent selectEvent) {
        Object o = selectEvent.getObject();
        if (o != null) {
            if (o.getClass() == Date.class) {
                Date date = (Date) selectEvent.getObject();
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(date);

                //   if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0) {
                cal.add(Calendar.HOUR_OF_DAY, 8); // set default hour to 9am
                // }
                Date start = cal.getTime();
                cal.add(Calendar.HOUR_OF_DAY, 1);
                Date end = cal.getTime();
                cal.add(Calendar.HOUR_OF_DAY, -25);
                Date remind = cal.getTime();
                TimetableScheduleEvent sEvent = new TimetableScheduleEvent("New Event", start, end);
                sEvent.setReminderDate(remind);
                setEvent(sEvent);

                //Add facesmessage
            } else {
                LOGGER.log(Level.WARNING, "onDateSelect - the Object returned by the evenet is not a date");
            }
        }

    }

    public void onTimetableEventSelect(SelectEvent selectEvent) {
        Object o = selectEvent.getObject();
        if (o != null) {
            if (o.getClass() == Date.class) {
                Date date = (Date) selectEvent.getObject();
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(date);

                //   if (cal.get(Calendar.HOUR_OF_DAY) == 0 && cal.get(Calendar.MINUTE) == 0 && cal.get(Calendar.SECOND) == 0) {
                cal.add(Calendar.HOUR_OF_DAY, 8); // set default hour to 9am
                // }
                Date start = cal.getTime();
                cal.add(Calendar.HOUR_OF_DAY, 1);
                Date end = cal.getTime();
                cal.add(Calendar.HOUR_OF_DAY, -25);
                Date remind = cal.getTime();
                TimetableScheduleEvent sEvent = new TimetableScheduleEvent("New Event", start, end);
                sEvent.setReminderDate(remind);
                setEvent(sEvent);
                //RequestContext.getCurrentInstance().execute("PF('eventDialog').show()");

                //Add facesmessage
            } else {
                LOGGER.log(Level.WARNING, "onDateSelect - the Object returned by the evenet is not a date");
            }
        }

    }

    private void addMessage(FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public void onEventMove(ScheduleEntryMoveEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event moved", "Day delta:" + event.getDayDelta() + ", Minute delta:" + event.getMinuteDelta());
        editEvent(event.getScheduleEvent());
        addMessage(message);
    }

    public void onEventResize(ScheduleEntryResizeEvent event) {
        FacesMessage message = new FacesMessage(FacesMessage.SEVERITY_INFO, "Event resized", "Day delta:" + event.getDayDelta() + ", Minute delta:" + event.getMinuteDelta());
        editEvent(event.getScheduleEvent());
        addMessage(message);
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
                } else {

                }
                startCal.add(Calendar.DAY_OF_YEAR, 7);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "cloneSessionsFromTimetable", e);
        }

    }

    /**
     * @return the eventModel
     */
    public ScheduleModel getEventModel() {
        if (eventModel == null) {
            eventModel = new LazyScheduleModel() {
                private static final long serialVersionUID = 1L;

                @Override
                public void loadEvents(Date start, Date end) {
                    clear();

                    List<SessionHistory> events = sessionHistoryFacade.findSessionsByDateRange(start, end, true);
                    for (SessionHistory ss : events) {
                        GregorianCalendar sessionDate = new GregorianCalendar();
                        sessionDate.setTime(ss.getSessiondate());
                        SessionTimetable template = ss.getSessionTemplate();
                        if (template != null) {
                            if (template.getSessionTimetableStatus() == TimetableSessionStatus.PUBLISHED.getValue()) {
                                sessionDate.add(Calendar.MINUTE, template.getDurationMinutes());
                                TimetableScheduleEvent ev = new TimetableScheduleEvent(template.getSessionTitle(), ss.getSessiondate(), sessionDate.getTime(), false);
                                ev.setDatabasePK(ss.getId());
                                ev.setStyleClass(template.getSessionStyleClasses());
                                ev.setData(ss);
                                ev.setDescription(template.getComments());

                                addEvent(ev);
                            }
                        }
                    }

                }
            };
        }
        return eventModel;
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

    public void incrementWeek() {
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getTimetableStartDate());
        startCal.add(Calendar.DAY_OF_YEAR, 7);
        setTimetableStartDate(startCal.getTime());
    }

    public void decrementWeek() {
        GregorianCalendar startCal = new GregorianCalendar();
        startCal.setTime(getTimetableStartDate());
        startCal.add(Calendar.DAY_OF_YEAR, -7);
        setTimetableStartDate(startCal.getTime());
    }

    public void setToCurrentWeek() {

        setTimetableStartDate(new Date());
    }

    public String signUpFromTimetable(ActionEvent actionEvent) {
        FacesContext context = FacesContext.getCurrentInstance();
        String sessionHistoryId = context.getExternalContext().getRequestParameterMap().get("sessionHistorySignupId");
        if (sessionHistoryId != null) {
            SessionHistory sh = sessionHistoryFacade.find(Integer.valueOf(sessionHistoryId));
            LOGGER.log(Level.INFO, "signUpFromTimetable: {0}", new Object[]{sh.getSessiondate().toString()});
        }
        return "index.xhtml";
    }

    public void bookFromTimetable(ActionEvent actionEvent) {

        // String sessionHistoryId = context.getExternalContext().getRequestParameterMap().get("sessionHistoryBookingId");
        // if (getBookingButtonSessionHistory()!= null) {
        //     SessionHistory sh = getBookingButtonSessionHistory();
        //     LOGGER.log(Level.INFO, "BookFromTimetable: {0}", new Object[]{sh.getSessiondate().toString()});
        // }
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
            cloneSessionsFromTimetable(current, DAYS_AHEAD_TO_POPULATE_TIMETABLE);
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

    public void unPublish() {
        current.setSessionTimetableStatus(TimetableSessionStatus.HIDDEN.getValue());
        getFacade().edit(current);
        //delete any future sessions so they don't show up in the timetable
        deleteFutureChildSessions();
        recreateModel();
    }

    public void publish() {
        current.setSessionTimetableStatus(TimetableSessionStatus.PUBLISHED.getValue());
        getFacade().edit(current);
        // add three months worth of sessions
        cloneSessionsFromTimetable(current, DAYS_AHEAD_TO_POPULATE_TIMETABLE);
        recreateModel();
    }

    public void destroy() {
        if (current.getSessionHistoryCollection().isEmpty()) {
            // we cant delete it if session still reference it
            performDestroy();
            recreateModel();
            current = null;
        } else {
            LOGGER.log(Level.WARNING, "You are trying to delete a sessiontemplate when there are sessions that still reference it. Use hide instead to remove it from the timetable ahead.");
            JsfUtil.addErrorMessage("You are trying to delete a sessiontemplate when there are sessions that still reference it. Use the hide button instead to unpublish it from the timetable ahead.");
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
            deleteFutureChildSessions();
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionTimetableDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void deleteFutureChildSessions() {
        Collection<SessionHistory> children = current.getSessionHistoryCollection();
        for (Iterator<SessionHistory> iterator = children.iterator(); iterator.hasNext();) {
            SessionHistory next = iterator.next();
            if (next.getSessiondate().compareTo(new Date()) > 0) {
                sessionHistoryFacade.remove(next);
            }

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

    /**
     * @return the bookingButtonSessionHistory
     */
    public SessionHistory getBookingButtonSessionHistory() {
        return bookingButtonSessionHistory;
    }

    /**
     * @param bookingButtonSessionHistory the bookingButtonSessionHistory to set
     */
    public void setBookingButtonSessionHistory(SessionHistory bookingButtonSessionHistory) {
        this.bookingButtonSessionHistory = bookingButtonSessionHistory;
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);

        if (context.getExternalContext().getRemoteUser() != null) {
            // autheticated user
            RequestContext.getCurrentInstance().execute("PF('bookingDialog').show();");

            controller.setSignupFromBookingInProgress(false);
        } else {
            //unauthenticated 
            RequestContext.getCurrentInstance().execute("PF('signupDialog').show();");
            //RequestContext.getCurrentInstance().update("signupDialog");
            //CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");

            controller.setSignupFromBookingInProgress(true);
        }
    }

    public void purchaseSession() {
        LOGGER.log(Level.INFO, "Purchase Session button clicked.");
        FacesContext context = FacesContext.getCurrentInstance();

        //SessionTimetableController sessionTimetableController = context.getApplication().evaluateExpressionGet(context, "#{sessionTimetableController}", SessionTimetableController.class);
        EziDebitPaymentGateway eziDebitPaymentGateway = context.getApplication().evaluateExpressionGet(context, "#{ezidebit}", EziDebitPaymentGateway.class);
        CustomersController controller = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        Customers c = controller.getSelected();
        eziDebitPaymentGateway.setSelectedCustomer(c);
        SessionBookings sb = new SessionBookings(0);
        Date sessionPurchaseTimestamp = new Date();
        sb.setBookingTime(sessionPurchaseTimestamp);
        sb.setSessionHistoryId(bookingButtonSessionHistory);
        sb.setCustomerId(c);

        // is the customer already set up in the payment gateway ?
        String paymentGatewayStatusDescription = c.getPaymentParameters().getStatusDescription();
        if (paymentGatewayStatusDescription == null || paymentGatewayStatusDescription.contains("Cancelled")) {
            try {
                // no they are not setup so redirect to ezidebit signup form
                sb.setStatus("PURCHASE-SIGNUP");
                sb.setStatusDescription("New authenticated customer signup is being redirected to the payment gateway signup form");
                ejbSessionBookingsFacade.create(sb);
                // a booking is in progress for a new signup
                // setup eddr url and payment
                eziDebitPaymentGateway.setOneOffPaymentAmountInCents(bookingButtonSessionHistory.getSessionTemplate().getSessionCasualRate().floatValue());
                eziDebitPaymentGateway.setOneOffPaymentDate(sessionPurchaseTimestamp);
                eziDebitPaymentGateway.createEddrLink(null);
                //call ezibeit controller

                eziDebitPaymentGateway.redirectToPaymentGateway();
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "purchaseSession, customer  {0} New authenticated customer signup is being redirected to the payment gateway signup form", c.getUsername());
            }
            // TODO need to check the call back from ezidebit to add the payment id to the booking
            // TODO need to check the call back from ezidebit to add the payment id to the booking
        } else {
            try {
                if (paymentGatewayStatusDescription.contains("Active") || paymentGatewayStatusDescription.contains("New")) {
                    // customer is set up just add a payment
                    LOGGER.log(Level.INFO, "purchaseSession, customer  {0}  active in payment gateway - adding payment", c.getUsername());

                    eziDebitPaymentGateway.addSinglePayment(c, bookingButtonSessionHistory.getSessionTemplate().getSessionCasualRate().floatValue(), sessionPurchaseTimestamp);
                    JsfUtil.addSuccessMessage(configMapFacade.getConfig("purchaseSessionDirectDebitPaymentProcessing"));
                    sb.setStatus("PURCHASE-ACTIVE");
                    sb.setStatusDescription("A new payment is being added for an existing customer");

                } else if (paymentGatewayStatusDescription.contains("Hold")) {
                    // customer is on hold notify them to contact staff and redirect customer to instant payment page
                    // if we just take them off hold without comfirmation their regular payemts may restart

                    LOGGER.log(Level.WARNING, "purchaseSession, Customer {0} is on Hold", c.getUsername());
                    JsfUtil.addSuccessMessage(configMapFacade.getConfig("purchaseSessionDirectDebitPaymentProcessingFailedOnHold"));
                    sb.setStatus("PURCHASE-HOLD");
                    sb.setStatusDescription("The purchase via Direct debit failed as the customer is On-Hold");

                } else if (paymentGatewayStatusDescription.contains("Waiting Bank Details")) {
                    // the customer has not set up their payment method
                    // notify them to contact staff or redirect to instant payment page
                    LOGGER.log(Level.WARNING, "purchaseSession, Customer {0} is Waiting Bank Details", c.getUsername());
                    JsfUtil.addSuccessMessage(configMapFacade.getConfig("purchaseSessionDirectDebitPaymentProcessingFailedBankDetails"));
                    sb.setStatus("PURCHASE-WBD");
                    sb.setStatusDescription("The purchase via Direct debit failed as the customer has not completed their payment method - Waiting Bank Details");
                } else {
                    // an unkown status is present log an error and redirect customer to instant payment page
                    LOGGER.log(Level.SEVERE, "purchaseSession, Customer {0} is in an unknown status:{1}", new Object[]{c.getUsername(), paymentGatewayStatusDescription});
                    JsfUtil.addSuccessMessage(configMapFacade.getConfig("purchaseSessionDirectDebitPaymentProcessingFailedUnknown" + paymentGatewayStatusDescription));
                    sb.setStatus("PURCHASE-FAIL");
                    sb.setStatusDescription("The purchase via Direct debit failed as the customer is in an unknown status: " + paymentGatewayStatusDescription);
                }
                //persist sb
                ejbSessionBookingsFacade.create(sb);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "purchaseSession, customer  {0} existing customer setup in gateway", c.getUsername());
            }
        }
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) ec.getRequest();
        try {
            ec.redirect(request.getContextPath() + "/myDetails.xhtml");
        } catch (IOException iOException) {
            LOGGER.log(Level.WARNING, "purchaseSession, could not redirect to /myDetails page", iOException);
        }

    }

    /**
     * @return the signupButtonSessionHistory
     */
    public SessionHistory getSignupButtonSessionHistory() {
        return signupButtonSessionHistory;
    }

    /**
     * @param signupButtonSessionHistory the signupButtonSessionHistory to set
     */
    public void setSignupButtonSessionHistory(SessionHistory signupButtonSessionHistory) {
        this.signupButtonSessionHistory = signupButtonSessionHistory;
    }

    /**
     * @return the eventStyleClass
     */
    public String getEventStyleClass() {
        return eventStyleClass;
    }

    /**
     * @param eventStyleClass the eventStyleClass to set
     */
    public void setEventStyleClass(String eventStyleClass) {
        this.eventStyleClass = eventStyleClass;
    }

    /**
     * @return the selectedTimetableSession
     */
    public SessionHistory getSelectedTimetableSession() {
        return selectedTimetableSession;
    }

    /**
     * @param selectedTimetableSession the selectedTimetableSession to set
     */
    public void setSelectedTimetableSession(SessionHistory selectedTimetableSession) {
        this.selectedTimetableSession = selectedTimetableSession;
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
