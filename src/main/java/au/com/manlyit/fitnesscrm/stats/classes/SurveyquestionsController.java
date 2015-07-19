package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestions;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SurveyquestionsFacade;
import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestionSubItems;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Consumer;
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

@Named("surveyquestionsController")
@SessionScoped
public class SurveyquestionsController implements Serializable {
    
    private SurveyQuestions current;
    private SurveyQuestions selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveyquestionsFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<SurveyQuestions> filteredItems;
    private SurveyQuestions[] multiSelected;
    private ArrayList<SurveyQuestionSubItems> subItems;
    private SurveyQuestionSubItems subItem;
    private SurveyQuestionSubItems deleteSubItem;
    
    public SurveyquestionsController() {
    }
    
    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }
    
    public SurveyQuestions getSelected() {
        if (current == null) {
            current = new SurveyQuestions();
            selectedItemIndex = -1;
        }
        return current;
    }
    
    public void setSelected(SurveyQuestions selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }
        
    }
    
    private SurveyquestionsFacade getFacade() {
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
     * @return the subItems
     */
    public ArrayList<SurveyQuestionSubItems> getSubItems() {
        if (subItems == null) {
            subItems = new ArrayList<>();
        }
        return subItems;
    }

    /**
     * @param subItems the subItems to set
     */
    public void setSubItems(ArrayList<SurveyQuestionSubItems> subItems) {
        this.subItems = subItems;
    }

    /**
     * @return the subItem
     */
    public SurveyQuestionSubItems getSubItem() {
        if (subItem == null) {
            subItem = new SurveyQuestionSubItems(0, "");
        }
        return subItem;
    }

    /**
     * @param subItem the subItem to set
     */
    public void setSubItem(SurveyQuestionSubItems subItem) {
        this.subItem = subItem;
    }

    /**
     * @return the filteredItems
     */
    public List<SurveyQuestions> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<SurveyQuestions> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public SurveyQuestions[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(SurveyQuestions[] multiSelected) {
        this.multiSelected = multiSelected;
    }
    
    public String prepareList() {
        recreateModel();
        return "List";
    }
    
    public String prepareView() {
        //current = (SurveyQuestions)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }
    
    public String prepareCreate() {
        current = new SurveyQuestions();
        selectedItemIndex = -1;
        return "Create";
    }
    
    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    Collection<SurveyQuestionSubItems> getSubItemsForCreate() {
        
        subItems.stream().forEach((SurveyQuestionSubItems si) -> {
            si.setQuestionId(current);
        });
        
        return subItems;
    }
    
    public void addSubItem() {
        
        getSubItems().add(subItem);
        subItem = new SurveyQuestionSubItems(0, "");
    }
    
    public void createDialogue(ActionEvent actionEvent) {
        try {
            current.setId(0);
            
            getFacade().create(current);
            current.setSurveyQuestionsubitemsCollection(getSubItemsForCreate());
            getFacade().edit(current);
            subItems = null;
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        
    }
    
    public void prepareEditDialogue() {
        if (current.getSurveyQuestionsubitemsCollection() != null) {
            subItems = new ArrayList<>(current.getSurveyQuestionsubitemsCollection());
        }
        subItem = new SurveyQuestionSubItems(0, "");
        RequestContext.getCurrentInstance().update(":SurveyquestionsEditForm");
    }
    
    public String prepareEdit() {
        //current = (SurveyQuestions)getItems().getRowData();
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
            current.setSurveyQuestionsubitemsCollection(getSubItemsForCreate());
            getFacade().edit(current);
            subItems = null;
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsUpdated"));
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
    
    public SurveyQuestions getSelectedForDeletion() {
        return selectedForDeletion;
    }
    
    public void setSelectedForDeletion(SurveyQuestions selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;
        
        performDestroy();
        recreateModel();
        
    }
    
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsDeleted"));
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
    
    public Collection<SurveyQuestions> getItemsAvailable() {
        return ejbFacade.findAll();
    }
    
    public void onEdit(RowEditEvent event) {
        SurveyQuestions cm = (SurveyQuestions) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }
    
    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the deleteSubItem
     */
    public SurveyQuestionSubItems getDeleteSubItem() {
        return deleteSubItem;
    }

    /**
     * @param deleteSubItem the deleteSubItem to set
     */
    public void setDeleteSubItem(SurveyQuestionSubItems deleteSubItem) {
        this.deleteSubItem = deleteSubItem;
        subItems.remove(deleteSubItem);
    }
    
    @FacesConverter(forClass = SurveyQuestions.class)
    public static class SurveyquestionsControllerConverter implements Converter {
        
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurveyquestionsController controller = (SurveyquestionsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "surveyquestionsController");
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
            if (object instanceof SurveyQuestions) {
                SurveyQuestions o = (SurveyQuestions) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SurveyquestionsController.class.getName());
            }
        }
        
    }
    
}
