package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Schedule;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.CrmScheduleEvent;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.ScheduleFacade;
import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.event.ValueChangeEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import org.primefaces.event.ScheduleEntryMoveEvent;
import org.primefaces.event.ScheduleEntryResizeEvent;
import javax.inject.Inject;
import javax.inject.Named;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.model.*;

@Named("scheduleController")
@SessionScoped
public class ScheduleController implements Serializable {

    private static final Logger logger = Logger.getLogger(ScheduleController.class.getName());
    private Schedule current;
    private Schedule selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ScheduleFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailQueueFacade emailQueueFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Schedule> filteredItems;
    private Schedule[] multiSelected;
    private ScheduleModel eventModel;
    private CrmScheduleEvent event;
    private String theme;

    public ScheduleController() {
        eventModel = new LazyScheduleModel() {

            @Override
            public void loadEvents(Date start, Date end) {
                clear();
                List<Schedule> events = ejbFacade.findDateRange(start, end);
                for (Schedule ss : events) {
                    CrmScheduleEvent ev = new CrmScheduleEvent(ss.getShedTitle(), ss.getShedStartdate(), ss.getShedEnddate(), ss.getShedAllday());
                    ev.setDatabasePK(ss.getId());
                    ev.setStyleClass(ss.getShedstyleClass());
                    ev.setData(ss.getDataObject());

                    addEvent(ev);
                }

            }
        };

    }

    public void removeEvent(ActionEvent actionEvent) {
        if (getEvent().getId() != null) {

            deleteEvent(getEvent());
            getEventModel().deleteEvent(getEvent());
            setEvent(new CrmScheduleEvent());

        } else {
            JsfUtil.addErrorMessage("Couldn't remove the event as the id is null.");

        }
    }

    public void addReminder(CrmScheduleEvent crmEvent) {
        //FacesContext context = FacesContext.getCurrentInstance();
        //CustomersController customersController = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        EmailQueue email = new EmailQueue();
        email.setId(0);
        email.setStatus(0);
        email.setSendDate(crmEvent.getReminderDate());
        email.setCreateDate(new Date());
        email.setSubject(configMapFacade.getConfig("ScheduleReminderEmailMessageSubject") + crmEvent.getTitle());
        email.setToaddresses(configMapFacade.getConfig("email.calendar.reminder.address"));
        email.setMessage(crmEvent.getStringData());
        email.setFromaddress(configMapFacade.getConfig("email.from.address"));

        emailQueueFacade.create(email);
        JsfUtil.addSuccessMessage("Event Reminder Added.");
    }

    public void addEvent(ActionEvent actionEvent) {
        if (getEvent().getId() == null) {
            getEventModel().addEvent(getEvent());
            persistEvent(getEvent());
        } else {
            getEventModel().updateEvent(getEvent());
            editEvent(getEvent());
        }
        if (getEvent().isAddReminder()) {
            addReminder(getEvent());
        }
        setEvent(new CrmScheduleEvent());
        
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException, ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public void persistEvent(CrmScheduleEvent event) {
        CrmScheduleEvent ssievent = (CrmScheduleEvent) event;
        Schedule ss = new Schedule(0, ssievent.getTitle(), ssievent.getStartDate(), ssievent.getEndDate());
        ss.setShedAllday(ssievent.isAllDay());
        ss.setDataObject(ssievent.getData());
        ss.setSchedReminder(ssievent.isAddReminder());
        ss.setSchedRemindDate(ssievent.getReminderDate());
        current = ss;
        try {
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleCreated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
        ssievent.setDatabasePK(current.getId());
        current = null;
    }

    private void deleteEvent(CrmScheduleEvent event) {
        CrmScheduleEvent ssievent = (CrmScheduleEvent) event;
        current = ejbFacade.find(ssievent.getDatabasePK());
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    private void editEvent(ScheduleEvent event) {
        CrmScheduleEvent ssievent = (CrmScheduleEvent) event;

        Schedule ss = ejbFacade.find(ssievent.getDatabasePK());
        ss.setShedEnddate(ssievent.getEndDate());
        ss.setShedStartdate(ssievent.getStartDate());
        ss.setShedTitle(ssievent.getTitle());
        ss.setSchedRemindDate(ssievent.getReminderDate());
        ss.setSchedReminder(ssievent.isAddReminder());
        ss.setShedAllday(ssievent.isAllDay());

        ss.setDataObject(ssievent.getData());

        current = ss;
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleUpdated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
        current = null;
    }

    public void onEventSelect(SelectEvent selectEvent) {
        setEvent((CrmScheduleEvent) selectEvent.getObject());
    }

    public void onDateSelect(SelectEvent selectEvent) {
        Object o = selectEvent.getObject();
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
            CrmScheduleEvent sEvent = new CrmScheduleEvent("New Event", start, end);
            sEvent.setReminderDate(remind);
            setEvent(sEvent);

            //Add facesmessage
        } else {
            logger.log(Level.WARNING, "onDateSelect - the Object returned by the evenet is not a date");
        }

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

    private void addMessage(FacesMessage message) {
        FacesContext.getCurrentInstance().addMessage(null, message);
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public Schedule getSelected() {
        if (current == null) {
            current = new Schedule();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(Schedule selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private ScheduleFacade getFacade() {
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
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    /**
     * @return the filteredItems
     */
    public List<Schedule> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Schedule> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Schedule[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Schedule[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Schedule)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Schedule();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleCreated"));
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (Schedule)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleUpdated"));
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

    public Schedule getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Schedule selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ScheduleDeleted"));
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

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
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

    public void handleReminderDateSelect(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Date.class) {
            Date date = (Date) event.getObject();

            if (date.after(new Date())) {
                getEvent().setReminderDate(date);
            }

            //Add facesmessage
        }
    }

    public void handleEndDateSelect(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Date.class) {
            Date date = (Date) event.getObject();

            if (date.after(getEvent().getEndDate())) {
                getEvent().setEndDate(date);
            }else{
                JsfUtil.addErrorMessage("The reminder date cannot be in the past!");
            }

            //Add facesmessage
        }
    }

    public void handleStartDateSelect(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Date.class) {
            Date date = (Date) event.getObject();
            if (date.before(getEvent().getStartDate())) {
                getEvent().setStartDate(date);
            }
            if (date.after(getEvent().getEndDate())) {
                GregorianCalendar cal = new GregorianCalendar();
                cal.setTime(date);
                cal.add(Calendar.HOUR_OF_DAY, 1);
                getEvent().setEndDate(cal.getTime());
            }
            //Add facesmessage
        }
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Collection<Schedule> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        Schedule cm = (Schedule) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the eventModel
     */
    public ScheduleModel getEventModel() {
        return eventModel;
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
    public CrmScheduleEvent getEvent() {
        if (event == null) {
            event = new CrmScheduleEvent();
        }
        return event;
    }

    /**
     * @param event the event to set
     */
    public void setEvent(CrmScheduleEvent event) {
        this.event = event;
    }

    @FacesConverter(forClass = Schedule.class)
    public static class ScheduleControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ScheduleController controller = (ScheduleController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "scheduleController");
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
            if (object instanceof Schedule) {
                Schedule o = (Schedule) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + ScheduleController.class.getName());
            }
        }

    }

}
