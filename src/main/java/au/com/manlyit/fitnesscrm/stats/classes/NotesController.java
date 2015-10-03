package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Notes;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.NotesFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.CrmScheduleEvent;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import java.io.Serializable;
import java.util.Calendar;
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

@Named("notesController")
@SessionScoped
public class NotesController implements Serializable {

    private Notes current;
    private Notes selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.NotesFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomersFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Notes> filteredItems;
    private List<Notes> customerNoteItems;
    private Notes[] multiSelected;
    private Customers selectedUser;
    private boolean reminder = false;
    private Date reminderDate;

    public NotesController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public Notes getSelected() {
        if (current == null) {
            current = new Notes();
            if (selectedUser != null) {
                current.setUserId(selectedUser);
            }
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(Notes selected) {
        if (selected != null) {
            current = selected;
            if (selectedUser != null) {
                current.setUserId(selectedUser);
            }

            selectedItemIndex = -1;
        }

    }

    public void handleReminderDateSelect(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass() == Date.class) {
            Date date = (Date) event.getObject();
            setReminderDate(date);
            //Add facesmessage
        }
    }

    private NotesFacade getFacade() {
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
    public List<Notes> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Notes> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Notes[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Notes[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Notes)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreateFromMobile() {
        current = new Notes();
        if (selectedUser != null) {
            current.setUserId(selectedUser);
        }
        selectedItemIndex = -1;
        return "pm:createCustomerNotes";
    }

    public String prepareCreate() {
        current = new Notes();
        selectedItemIndex = -1;
        return "Create";
    }

    private void createNote() {
        if (current.getId() == null) {
            current.setId(0);
        }
        current.setCreateTimestamp(new Date());
        current.setDeleted(new Short("0"));
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");
        current.setUserId(controller.getSelected());
        current.setCreatedBy(controller.getLoggedInUser());
        getFacade().create(current);
    }

    public String createFromMobile() {
        try {
            createNote();
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesCreated"));
            return prepareCreateFromMobile();
        } catch (NumberFormatException e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String create() {
        try {
            createNote();
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue(ActionEvent actionEvent) {
        try {
            createNote();
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController controller = (CustomersController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "customersController");

            controller.addToNotesDataTableLists(current);
            if (isReminder()) {

                addReminderToCalendar();
                reminder = false;
                reminderDate = new Date();
            }
            current = null;

            //controller.setNotesItems(null);
            Logger.getLogger(NotesController.class.getName()).log(Level.INFO, "Note added for customer {0} by {1}.", new Object[]{controller.getSelected().toString(), controller.getLoggedInUser().toString()});
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    private void addReminderToCalendar() {

        CrmScheduleEvent event = new CrmScheduleEvent();
        event.setStartDate(reminderDate);
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(reminderDate);
        cal.add(Calendar.HOUR_OF_DAY, 1);
        event.setEndDate(cal.getTime());

        event.setReminderDate(reminderDate);
        event.setAllDay(false);
        event.setAddReminder(true);
        String title = "Reminder: " + current.getUserId().getFirstname() + " " + current.getUserId().getLastname();
        event.setTitle(title);
        event.setStringData(current.getNote());
        FacesContext context = FacesContext.getCurrentInstance();
        ScheduleController scheduleController = (ScheduleController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "scheduleController");
        scheduleController.persistEvent(event);
        scheduleController.addReminder(event);

    }

    public String prepareEdit() {
        //current = (Notes)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void updateNotesValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        if (o.getClass() == String.class) {
            String text = (String) o;
            current.setNote(text);
        }
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Notes) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
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

    public Notes getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Notes selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("NotesDeleted"));
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
        customerNoteItems = null;
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
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public void onEdit(RowEditEvent event) {
        Notes cm = (Notes) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the customerNoteItems
     */
    public List<Notes> getCustomerNoteItems() {
        if (customerNoteItems == null) {
            customerNoteItems = ejbFacade.findByUserId(getSelectedUser(), false);
        }
        return customerNoteItems;
    }

    /**
     * @param customerNoteItems the customerNoteItems to set
     */
    public void setCustomerNoteItems(List<Notes> customerNoteItems) {
        this.customerNoteItems = customerNoteItems;
    }

    /**
     * @return the selectedUser
     */
    public Customers getSelectedUser() {
        if (selectedUser == null) {
            selectedUser = current.getUserId();
        }
        return selectedUser;
    }

    /**
     * @param selectedUser the selectedUser to set
     */
    public void setSelectedUser(Customers selectedUser) {
        this.customerNoteItems = null;
        if (current != null) {
            current.setUserId(selectedUser);
        }
        this.selectedUser = selectedUser;
    }

    /**
     * @return the reminder
     */
    public boolean isReminder() {
        return reminder;
    }

    /**
     * @param reminder the reminder to set
     */
    public void setReminder(boolean reminder) {
        this.reminder = reminder;
    }

    /**
     * @return the reminderDate
     */
    public Date getReminderDate() {
        return reminderDate;
    }

    /**
     * @param reminderDate the reminderDate to set
     */
    public void setReminderDate(Date reminderDate) {
        this.reminderDate = reminderDate;
    }

    @FacesConverter(value="notesControllerConverter", forClass = Notes.class)
    public static class NotesControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            NotesController controller = (NotesController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "notesController");
            return controller.ejbFacade.find(getKey(value));
        }

        java.lang.Integer getKey(String value) {
            java.lang.Integer key;
            key = Integer.valueOf(value);
            return key;
        }

        String getStringKey(java.lang.Integer value) {
            StringBuffer sb = new StringBuffer();
            sb.append(value);
            return sb.toString();
        }

        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof Notes) {
                Notes o = (Notes) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + NotesController.class.getName());
            }
        }

    }

}
