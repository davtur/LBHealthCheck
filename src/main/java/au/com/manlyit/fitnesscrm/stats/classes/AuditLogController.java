package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.AuditLog;
import au.com.manlyit.fitnesscrm.stats.beans.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.beans.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.LazyLoadingDataModel;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
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
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named("auditLogController")
@SessionScoped
public class AuditLogController implements Serializable {

    private static final Logger logger = Logger.getLogger(AuditLogController.class.getName());
    private AuditLog current;
    private AuditLog selectedForDeletion;
    private PfSelectableDataModel<AuditLog> items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<AuditLog> filteredItems;
    private AuditLog[] multiSelected;
    private LazyLoadingDataModel<AuditLog> lazyModel;
    private Date startDate;
    private Date endDate;

    public AuditLogController() {
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
        lazyModel = new LazyLoadingDataModel<>(ejbFacade);
        lazyModel.setFromDate(getStartDate());
        lazyModel.setToDate(getEndDate());
        lazyModel.setDateRangeEntityFieldName("timestampOfChange");
        lazyModel.setUseDateRange(true);
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public AuditLog getSelected() {
        if (current == null) {
            current = new AuditLog();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(AuditLog selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    public LazyLoadingDataModel<AuditLog> getLazyModel() {
        if (lazyModel == null) {
            lazyModel = new LazyLoadingDataModel<>(ejbFacade);
            lazyModel.setFromDate(getStartDate());
            lazyModel.setToDate(getEndDate());
            lazyModel.setDateRangeEntityFieldName("timestampOfChange");
            lazyModel.setUseDateRange(true);
        }
        return lazyModel;
    }

    private AuditLogFacade getFacade() {
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
    public List<AuditLog> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<AuditLog> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public AuditLog[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(AuditLog[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (AuditLog)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new AuditLog();
        selectedItemIndex = -1;
        return "Create";
    }

    public void datesChangedOnAuditLogTable() {
        //items = null;
        //filteredItems = null;
        lazyModel.setFromDate(startDate);
        lazyModel.setToDate(endDate);
        recreateModel();
       // RequestContext requestContext = RequestContext.getCurrentInstance();

    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("AuditLogCreated"));
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("AuditLogCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (AuditLog)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("AuditLogUpdated"));
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

    public AuditLog getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(AuditLog selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("AuditLogDeleted"));
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

    public PfSelectableDataModel<AuditLog> getItems() {
        if (items == null) {
            //items = getPagination().createPageDataModel();
            items = new PfSelectableDataModel<>(ejbFacade.findAuditLogsByDateRange(startDate, endDate, true));
        }
        logger.log(Level.INFO, "lazy Load Items : rowcount = {0}", new Object[]{items.getRowCount()});
        return items;
    }

    private void recreateModel() {
        RequestContext.getCurrentInstance().execute("PF('sessionsDataTable').filter();");
        // items = null;
        //filteredItems = null;
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
        file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl

        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Collection<AuditLog> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        AuditLog cm = (AuditLog) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
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

    @FacesConverter(value = "auditLogControllerConverter", forClass = AuditLog.class)
    public static class AuditLogControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            AuditLogController controller = (AuditLogController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "auditLogController");
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
            if (object instanceof AuditLog) {
                AuditLog o = (AuditLog) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + AuditLogController.class.getName());
            }
        }

    }

}
