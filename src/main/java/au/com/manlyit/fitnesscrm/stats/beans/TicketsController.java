package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Tickets;
import au.com.manlyit.fitnesscrm.stats.beans.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.util.LazyLoadingDataModel;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.classes.util.TicketSummary;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
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

@Named("ticketsController")
@SessionScoped
public class TicketsController implements Serializable {

    private static final long serialVersionUID = 1L;

    private Tickets current;
    private int numberOfTicketsToAdd = 0;
    private int weeksValid = 52;
    private SessionTypes sessionType;
    private Tickets selectedForDeletion;
    private PfSelectableDataModel<Tickets> items = null;
    private int itemCount = 0;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.TicketsFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Tickets> filteredItems;
    private ArrayList<TicketSummary> ticketsOverview;
    private List<Tickets> customerTicketList;
    private Tickets[] multiSelected;
    private LazyLoadingDataModel<Tickets> lazyModel;
    private LazyLoadingDataModel<Tickets> thisWeeksLazyModel;
    private Date startDate;
    private Date endDate;
    private static final Logger LOGGER = Logger.getLogger(TicketsController.class.getName());

    public TicketsController() {
    }

    /**
     * @return the lazyModel
     */
    public LazyLoadingDataModel<Tickets> getLazyModel() {
        if (lazyModel == null) {
            lazyModel = new LazyLoadingDataModel<>(ejbFacade);
            lazyModel.setFromDate(getStartDate());
            lazyModel.setToDate(getEndDate());
            lazyModel.setDateRangeEntityFieldName("datePurchased");
            lazyModel.setUseDateRange(true);
        }
        return lazyModel;
    }

    /**
     * @param lazyModel the lazyModel to set
     */
    public void setLazyModel(LazyLoadingDataModel<Tickets> lazyModel) {
        this.lazyModel = lazyModel;
    }

    /**
     * @return the startDate
     */
    public Date getStartDate() {
        return startDate;
    }

    /**
     * @param startDate the startDate to set
     */
    public void setStartDate(Date startDate) {
        this.startDate = startDate;
    }

    /**
     * @return the endDate
     */
    public Date getEndDate() {
        return endDate;
    }

    /**
     * @param endDate the endDate to set
     */
    public void setEndDate(Date endDate) {
        this.endDate = endDate;
    }

    @PostConstruct
    private void initDates() {

        GregorianCalendar cal1 = new GregorianCalendar();
        cal1.add(Calendar.DAY_OF_YEAR, 1);
        setEndDate(cal1.getTime());
        cal1.add(Calendar.DAY_OF_YEAR, -1);
        cal1.add(Calendar.MONTH, -1);
        setStartDate(cal1.getTime());
        //items = new PfSelectableDataModel<>(ejbFacade.findAuditLogsByDateRange(startDate, endDate, true));
        setLazyModel(new LazyLoadingDataModel<>(ejbFacade));
        getLazyModel().setFromDate(getStartDate());
        getLazyModel().setToDate(getEndDate());
        getLazyModel().setDateRangeEntityFieldName("datePurchased");
        getLazyModel().setUseDateRange(true);
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public Tickets getSelected() {
        if (current == null) {
            current = new Tickets();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(Tickets selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private TicketsFacade getFacade() {
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
    public List<Tickets> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Tickets> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Tickets[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Tickets[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Tickets)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Tickets();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TicketsCreated"));
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TicketsCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (Tickets)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TicketsUpdated"));
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

    public Tickets getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Tickets selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TicketsDeleted"));
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

    private void setStartandEndDatesToCurrentWeek() {
        GregorianCalendar cal = new GregorianCalendar();
        cal.set(Calendar.HOUR, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);

        setStartDate(cal.getTime());
        cal.add(Calendar.WEEK_OF_YEAR, 1);
        cal.add(Calendar.DAY_OF_YEAR, 1);
        cal.add(Calendar.SECOND, -1);
        setEndDate(cal.getTime());
    }

    public ArrayList<TicketSummary> getTicketSummaryList() {

        if (ticketsOverview == null) {

            ticketsOverview = new ArrayList<>();

            List<Tickets> tl = getCustomerTicketList();
            for (Tickets t : tl) {
                String key = t.getSessionType().getName();
                boolean found = false;
                for(TicketSummary ts:ticketsOverview){
                    if(ts.getTicketName().contentEquals(key)){
                        found = true;
                        ts.incrementCount();
                    }
                }
                if(found == false){
                    ticketsOverview.add(new TicketSummary(key, 1));
                }
                
            }
        }
        return ticketsOverview;

    }

    private List<Tickets> getCustomerTicketList() {

        if (customerTicketList == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController controller = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
            customerTicketList = ejbFacade.findCustomerTicketsValidAndUsedForSessionDate(controller.getSelected(), new Date(), true);
        }
        return customerTicketList;
    }

    private void setCustomerTicketList(List<Tickets> ticketsList) {
        customerTicketList = ticketsList;
    }

    // public DataModel getItems() {
    public PfSelectableDataModel<Tickets> getItems() {
        if (items == null) {
            items = new PfSelectableDataModel<>(getCustomerTicketList());
            //items = new PfSelectableDataModel<>(ejbFacade.findCustomerTicketsByDateRange(controller.getSelected(), startDate, endDate, true));
            setItemCount(items.getRowCount());
        }
        LOGGER.log(Level.INFO, "lazy Load Items : rowcount = {0}", new Object[]{items.getRowCount()});
        // if (items == null) {
        //     items = getPagination().createPageDataModel();
        // }
        return items;
    }

    public void recreateModel() {
        items = null;
        filteredItems = null;
        thisWeeksLazyModel = null;
        customerTicketList = null;
        lazyModel = null;
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

    public Collection<Tickets> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        Tickets cm = (Tickets) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");

    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    @FacesConverter(value = "ticketsControllerConverter")
    public static class TicketsControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TicketsController controller = (TicketsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "ticketsController");
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
            if (object instanceof Tickets) {
                Tickets o = (Tickets) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + TicketsController.class.getName());
            }
        }

    }

    /**
     * @return the numberOfTicketsToAdd
     */
    public int getNumberOfTicketsToAdd() {
        return numberOfTicketsToAdd;
    }

    /**
     * @param numberOfTicketsToAdd the numberOfTicketsToAdd to set
     */
    public void setNumberOfTicketsToAdd(int numberOfTicketsToAdd) {
        this.numberOfTicketsToAdd = numberOfTicketsToAdd;
    }

    /**
     * @return the weeksValid
     */
    public int getWeeksValid() {
        return weeksValid;
    }

    /**
     * @param weeksValid the weeksValid to set
     */
    public void setWeeksValid(int weeksValid) {
        this.weeksValid = weeksValid;
    }

    /**
     * @return the sessionType
     */
    public SessionTypes getSessionType() {
        return sessionType;
    }

    /**
     * @param sessionType the sessionType to set
     */
    public void setSessionType(SessionTypes sessionType) {
        this.sessionType = sessionType;
    }

    /**
     * @return the thisWeeksLazyModel
     */
    public LazyLoadingDataModel<Tickets> getThisWeeksLazyModel() {
        if (thisWeeksLazyModel == null) {
            GregorianCalendar cal = new GregorianCalendar();
            cal.set(Calendar.HOUR, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            cal.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
            thisWeeksLazyModel = new LazyLoadingDataModel<>(ejbFacade);
            thisWeeksLazyModel.setFromDate(cal.getTime());
            cal.add(Calendar.WEEK_OF_YEAR, 1);
            cal.add(Calendar.DAY_OF_YEAR, 1);
            cal.add(Calendar.SECOND, -1);
            thisWeeksLazyModel.setToDate(cal.getTime());
            thisWeeksLazyModel.setDateRangeEntityFieldName("datePurchased");
            thisWeeksLazyModel.setUseDateRange(true);
        }

        return thisWeeksLazyModel;
    }

    /**
     * @param thisWeeksLazyModel the thisWeeksLazyModel to set
     */
    public void setThisWeeksLazyModel(LazyLoadingDataModel<Tickets> thisWeeksLazyModel) {
        this.thisWeeksLazyModel = thisWeeksLazyModel;
    }

    /**
     * @param itemCount the itemCount to set
     */
    public void setItemCount(int itemCount) {
        this.itemCount = itemCount;
    }

}
//file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl
