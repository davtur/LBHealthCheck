package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SurveysFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.QuestionnaireMap;
import au.com.manlyit.fitnesscrm.stats.db.ToDoList;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
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

@Named("surveysController")
@SessionScoped
public class SurveysController implements Serializable {
    private static final Logger LOGGER = Logger.getLogger(SurveysController.class.getName());
    private Surveys current;
    private Surveys selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveysFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private CustomersFacade customersFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.QuestionnaireMapFacade questionnaireMapFacade;
    
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Surveys> filteredItems;
    private Surveys[] multiSelected;
    
    public SurveysController() {
    }
    
    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }
    
    public Surveys getSelected() {
        if (current == null) {
            current = new Surveys();
            selectedItemIndex = -1;
        }
        return current;
    }
    
    public void setSelected(Surveys selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }
        
    }
    
    private SurveysFacade getFacade() {
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
                public DataModel<Surveys> createPageDataModel() {
                    return new ListDataModel<>(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }
    public void updateSurveyMap(ActionEvent ae){
        updateSurveyToCustomerMap();
    }
    
    private void updateSurveyToCustomerMap() {
        // get a list of all surveys

        // get a list of all customers
        // iterate over them and add a new map if necessary
        List<Customers> customersList = customersFacade.findAll(true);
        
        List<Surveys> surveyList = ejbFacade.findAll();
        
        for (Surveys s : surveyList) {
            for (Customers c : customersList) {
                boolean foundSurveyMap = false;
                
                Collection<QuestionnaireMap> qmc = c.getQuestionnaireMapCollection();
                if (qmc != null) {
                    if (qmc.isEmpty() == false) {
                        for (QuestionnaireMap qm : qmc) {
                            if (qm.getSurveysId().getId().compareTo(s.getId()) == 0) {
                                foundSurveyMap = true;
                            }
                        }
                    }
                    
                }
                if (foundSurveyMap == false) {
                    QuestionnaireMap qmNew = new QuestionnaireMap(0, false);
                    qmNew.setCustomerId(c);
                    qmNew.setSurveysId(s);
                    qmNew.setQuestionnaireCompleted(false);
                    questionnaireMapFacade.create(qmNew);
                  
                    LOGGER.log(Level.INFO, "A new QuestionnaireMap was created for survey {1} and Customer {0}.",new Object[]{c.getUsername(),s.getName()});
                }
                
            }
        }
        
    }
    
    
    public void onEditQMap(RowEditEvent event) {
        QuestionnaireMap cm = (QuestionnaireMap) event.getObject();
        questionnaireMapFacade.edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancelQMap(RowEditEvent event) {
        JsfUtil.addSuccessMessage("Row Edit Cancelled");
    }

    /**
     * @return the filteredItems
     */
    public List<Surveys> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Surveys> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Surveys[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Surveys[] multiSelected) {
        this.multiSelected = multiSelected;
    }
    
    public String prepareList() {
        recreateModel();
        return "List";
    }
    
    public String prepareView() {
        //current = (Surveys)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }
    
    public String prepareCreate() {
        current = new Surveys();
        selectedItemIndex = -1;
        return "Create";
    }
    
    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            updateSurveyToCustomerMap();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveysCreated"));
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
            updateSurveyToCustomerMap();
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveysCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        
    }
    
    public String prepareEdit() {
        //current = (Surveys)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveysUpdated"));
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
    
    public Surveys getSelectedForDeletion() {
        return selectedForDeletion;
    }
    
    public void setSelectedForDeletion(Surveys selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;
        
        performDestroy();
        recreateModel();
        
    }
    
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveysDeleted"));
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
    
    public Collection<Surveys> getItemsAvailable() {
        return ejbFacade.findAll();
    }
    
    public void onEdit(RowEditEvent event) {
        Surveys cm = (Surveys) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }
    
    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }
    
    @FacesConverter(value = "surveysControllerConverter")
    public static class SurveysControllerConverter implements Converter {
        
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurveysController controller = (SurveysController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "surveysController");
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
            if (object instanceof Surveys) {
                Surveys o = (Surveys) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SurveysController.class.getName());
            }
        }
        
    }
    
}
