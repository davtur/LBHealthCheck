package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswerSubitems;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SurveyAnswerSubitemsFacade;


import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;
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


@Named("surveyanswersubitemsController")
@SessionScoped
public class SurveyAnswerSubitemsController implements Serializable {

    private SurveyAnswerSubitems current;
    private SurveyAnswerSubitems selectedForDeletion;
    private DataModel items = null;
    @Inject 
    private au.com.manlyit.fitnesscrm.stats.beans.SurveyAnswerSubitemsFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<SurveyAnswerSubitems> filteredItems;
    private SurveyAnswerSubitems[] multiSelected;

    public SurveyAnswerSubitemsController() {
    }

     public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }
    public SurveyAnswerSubitems getSelected() {
        if (current == null) {
            current = new SurveyAnswerSubitems();
            selectedItemIndex = -1;
        }
        return current;
    }

   public void setSelected(SurveyAnswerSubitems selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }
    private SurveyAnswerSubitemsFacade getFacade() {
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
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem()+getPageSize()}));
                }
            };
        }
        return pagination;
    }
   /**
     * @return the filteredItems
     */
    public List<SurveyAnswerSubitems> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<SurveyAnswerSubitems> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public SurveyAnswerSubitems[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(SurveyAnswerSubitems[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (SurveyAnswerSubitems)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new SurveyAnswerSubitems();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if(current.getId() == null){
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyanswersubitemsCreated"));
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyanswersubitemsCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));            
        }


    }
    public String prepareEdit() {
        //current = (SurveyAnswerSubitems)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyanswersubitemsUpdated"));
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

    public SurveyAnswerSubitems getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(SurveyAnswerSubitems selectedForDeletion) {
         this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyanswersubitemsDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count-1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex+1}).get(0);
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

    public void handleDateSelect(SelectEvent event) {

        Date date = (Date) event.getObject();

        //Add facesmessage
    }
    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl

        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Collection<SurveyAnswerSubitems> getItemsAvailable() {
        return ejbFacade.findAll();
    }
   
   public void onEdit(RowEditEvent event) {
        SurveyAnswerSubitems  cm = (SurveyAnswerSubitems ) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }
    
    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    @FacesConverter(value="surveyAnswerSubitemsControllerConverter", forClass=SurveyAnswerSubitems.class)
    public static class SurveyanswersubitemsControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurveyAnswerSubitemsController controller = (SurveyAnswerSubitemsController)facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "surveyanswersubitemsController");
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
            if (object instanceof SurveyAnswerSubitems) {
                SurveyAnswerSubitems o = (SurveyAnswerSubitems) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: "+SurveyAnswerSubitemsController.class.getName());
            }
        }

    }

}
