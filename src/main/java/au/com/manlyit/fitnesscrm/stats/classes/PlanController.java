package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Plan;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.PlanFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.PfSelectableDataModel;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
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
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named("planController")
@SessionScoped
public class PlanController implements Serializable {

    private static final Logger logger = Logger.getLogger(PlanController.class.getName());
    private Plan current;
    private Plan newPlan;
    private Plan selectedSubItem;
    private List<Plan> filteredSubItems;
    private Plan selectedForDeletion;
    private PfSelectableDataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PlanFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Plan> filteredItems;
    private Plan[] multiSelected;
    private boolean subItem = false;

    public PlanController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    

    public Plan getSelected() {
        /*if (current == null) {
         current = new Plan();
         selectedItemIndex = -1;
         }*/
        return current;
    }

    public void setSelected(Plan selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private PlanFacade getFacade() {
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
    public List<Plan> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Plan> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Plan[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Plan[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Plan)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Plan();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createDialogue(ActionEvent actionEvent) {
        try {
            if (isSubItem()) {
                Plan newSubItem = new Plan(0, selectedSubItem.getPlanName(), selectedSubItem.getPlanPrice(), selectedSubItem.getPlanActive());
                newSubItem.setParent(current);
                newSubItem.setPlanDescription(selectedSubItem.getPlanDescription());
                newSubItem.setPlanDiscount(selectedSubItem.getPlanDiscount());
                //getFacade().create(newSubItem);
                current.getPlanCollection().add(newSubItem);
                getFacade().edit(current);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanSubItemCreated"));
            } else {
                newPlan.setId(0);
                getFacade().create(newPlan);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanCreated"));
            }
            recreateModel();
            //  JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (Plan)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void destroySubItemFromDialogue(ActionEvent event) {
        //Object o = event.getSource();
        // if (o.getClass().equals(Plan.class)) {

        Plan p = getSelectedSubItem();
        if (p != null) {
            Plan parent = p.getParent();
            getFacade().remove(p);
            getFacade().edit(parent);
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanSubItemDeleted"));
        } else {
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanSubItemNullDeleted"));
        }
    }

    public void subItemRowSelect(SelectEvent event) {
        Object o = event.getObject();
        if (o.getClass().equals(Plan.class)) {
            Plan p = (Plan) o;
            //setSelectedSubItem(p);
            logger.log(Level.INFO, "subItemRowSelect: {0}", p);
        }
    }

    public void destroyFromDialogue(ActionEvent event) {
        //performDestroy();
        List<Plan> plans = ejbFacade.findPLansByName(current.getPlanName());
        for (Plan p : plans) {

            getFacade().remove(p);
        }
        current = null;
        recreateModel();
    }

    public void addSubItemToPlan(ActionEvent event) {
        setSubItem(true);
        RequestContext.getCurrentInstance().update("formPlanCreate1");
        RequestContext.getCurrentInstance().execute("PF('planCreateDialogue').show()");
    }

    public void addPlan(ActionEvent event) {
        setSubItem(false);
        RequestContext.getCurrentInstance().update("formPlanCreate1");
        RequestContext.getCurrentInstance().execute("PF('planCreateDialogue').show()");

    }

    public String destroy() {
        current = (Plan) getItems().getRowData();
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

    public Plan getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Plan selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanDeleted"));
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

    public PfSelectableDataModel getItems() {
        if (items == null) {
            // items = getPagination().createPageDataModel();
            items = new PfSelectableDataModel(getFacade().findAllPlans());
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
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public List<Plan> getItemsAvailableAsObjects() {
        return ejbFacade.findAllPlans();
    }

    public void onEdit(RowEditEvent event) {
        Plan cm = (Plan) event.getObject();
        getFacade().edit(cm);
        List<Plan> plans = ejbFacade.findPLansByName(cm.getPlanName());
        for (Plan p : plans) {
            p.setPlanActive(cm.getPlanActive());
            p.setPlanDescription(cm.getPlanDescription());
            p.setPlanDiscount(cm.getPlanDiscount());
            p.setPlanPrice(cm.getPlanPrice());
            getFacade().edit(p);
        }
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanRowEditSuccessFul"));
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage(configMapFacade.getConfig("PlanRowEditCancelled"));
    }

    /**
     * @return the subItem
     */
    public boolean isSubItem() {
        return subItem;
    }

    /**
     * @param subItem the subItem to set
     */
    public void setSubItem(boolean subItem) {
        this.subItem = subItem;
    }

    /**
     * @return the selectedSubItem
     */
    public Plan getSelectedSubItem() {
        return selectedSubItem;
    }

    /**
     * @param selectedSubItem the selectedSubItem to set
     */
    public void setSelectedSubItem(Plan selectedSubItem) {
        logger.log(Level.INFO, "setSelectedSubItem: {0}", selectedSubItem);
        this.selectedSubItem = selectedSubItem;
    }

    public void setSelectedSubItemAndDelete(Plan selectedSubItem) {
        logger.log(Level.INFO, "setSelectedSubItemAndDelete: {0}", selectedSubItem);
        //this.selectedSubItem = selectedSubItem;

        if (selectedSubItem != null) {
            Plan parent = selectedSubItem.getParent();
            parent.getPlanCollection().remove(selectedSubItem);
            getFacade().remove(selectedSubItem);
            getFacade().edit(parent);
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanSubItemDeleted"));
        } else {
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("PlanSubItemNullDeleted"));
        }
    }

    /**
     * @return the filteredSubItems
     */
    public List<Plan> getFilteredSubItems() {
        return filteredSubItems;
    }

    /**
     * @param filteredSubItems the filteredSubItems to set
     */
    public void setFilteredSubItems(List<Plan> filteredSubItems) {
        this.filteredSubItems = filteredSubItems;
    }

    /**
     * @return the newPlan
     */
    public Plan getNewPlan() {
        if (newPlan == null) {
            newPlan = new Plan();
            selectedItemIndex = -1;
        }
        return newPlan;
    }

    /**
     * @param newPlan the newPlan to set
     */
    public void setNewPlan(Plan newPlan) {
        this.newPlan = newPlan;
    }

    @FacesConverter(forClass = Plan.class)
    public static class PlanControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            PlanController controller = (PlanController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "planController");
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
            if (object instanceof Plan) {
                Plan o = (Plan) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + PlanController.class.getName());
            }
        }

    }

}
