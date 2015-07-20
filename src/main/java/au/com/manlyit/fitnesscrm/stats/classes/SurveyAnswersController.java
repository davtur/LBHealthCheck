package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SurveyAnswersFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.SurveyMap;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswerSubitems;
import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestions;
import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestionSubitems;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;

import java.io.Serializable;
import java.util.ArrayList;
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

@Named("surveyanswersController")
@SessionScoped
public class SurveyAnswersController implements Serializable {

    private static final Logger logger = Logger.getLogger(SurveyAnswersController.class.getName());
    private SurveyAnswers current;
    private Surveys selectedSurvey;
    private SurveyAnswers selectedForDeletion;
    private SurveyMap selectSurvey;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveyAnswersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<SurveyAnswers> filteredItems;
    private List<SurveyAnswers> surveyAnswers;
    private SurveyAnswers[] multiSelected;
    private ArrayList<SurveyMap> usersSurveys;

    public SurveyAnswersController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public SurveyAnswers getSelected() {
        if (current == null) {
            current = new SurveyAnswers();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(SurveyAnswers selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private SurveyAnswersFacade getFacade() {
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
    public List<SurveyAnswers> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<SurveyAnswers> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public SurveyAnswers[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(SurveyAnswers[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (SurveyAnswers)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new SurveyAnswers();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyAnswersCreated"));
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyAnswersCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String saveSurvey() {
        try {
            for (SurveyAnswers sa : surveyAnswers) {
                 getFacade().edit(sa);
            }
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyAnswersCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return "myDetails";
    }
    public void surveyBooleanChangeListener() {
        logger.log(Level.FINE, "Boolean Answer modified");
    }

    public String prepareEdit() {
        //current = (SurveyAnswers)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void textAnswerValueChange(ValueChangeEvent vce) {
        Object o = vce.getNewValue();

    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyAnswersUpdated"));
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

    public SurveyAnswers getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(SurveyAnswers selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void prepareSurveysForSelectedCustomer() {

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController customersController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        SurveysController surveysController = (SurveysController) context.getApplication().evaluateExpressionGet(context, "#{surveysController}", SurveysController.class);
        Customers selectedCustomer = customersController.getSelected();

        Collection<Surveys> surveys = surveysController.getItemsAvailable();
        setUsersSurveys(new ArrayList<>());
        try {
            for (Surveys s : surveys) {
                List<SurveyQuestions> lsq = new ArrayList<>(s.getSurveyQuestionsCollection());

                // if (lsa == null || lsa.isEmpty()) {// the survey hasn't been taken so add blank answers
                for (SurveyQuestions quest : lsq) {
                    // ArrayList<SurveyAnswers> lsa = new ArrayList<>(quest.getSurveyAnswersCollection());
                    SurveyAnswers answ = ejbFacade.findSurveyAnswersByCustomerAndQuestion(selectedCustomer, quest);

                    if (answ == null) {
                        answ = new SurveyAnswers(0, "");
                        answ.setQuestionId(quest);
                        answ.setId(0);
                        //answ.setSurveyId(s);
                        answ.setAnswerTypeid(quest.getQuestionType());
                        answ.setUserId(selectedCustomer);
                        ejbFacade.create(answ);
                        Collection<SurveyQuestionSubitems> qSubItems = quest.getSurveyQuestionSubitemsCollection();
                        ArrayList<SurveyAnswerSubitems> aSubItems = new ArrayList<>();
                        for (SurveyQuestionSubitems qsi : qSubItems) {

                            SurveyAnswerSubitems asi = new SurveyAnswerSubitems(0, qsi.getSubitemText());
                            Boolean subBoll = qsi.getSubitemBool();
                            if (subBoll == null) {
                                subBoll = false;
                            }
                            Integer subInt = qsi.getSubitemInt();
                            if (subInt == null) {
                                subInt = -1;
                            }
                            asi.setSubitemBool(subBoll);
                            asi.setSubitemInt(subInt);
                            asi.setAnswerId(answ);

                            aSubItems.add(asi);

                        }
                        answ.setSurveyAnswerSubitemsCollection(aSubItems);
                        ejbFacade.edit(answ);
                        //lsa.add(answ);
                    }
                }
                //}
                //  getUsersSurveys().add(new SurveyMap(s, lsa));
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "prepareSurveysForSelectedCustomer()", e);
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyAnswersDeleted"));
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
            items = new ListDataModel(ejbFacade.findAll());
            //items = getPagination().createPageDataModel();
        }
        return items;
    }

    public List<SurveyAnswers> getSurveyList() {
        if (surveyAnswers == null) {
            surveyAnswers = new ArrayList<>();
            prepareSurveysForSelectedCustomer();
            FacesContext context = FacesContext.getCurrentInstance();
            CustomersController customersController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
            Collection<SurveyQuestions> lsq = selectedSurvey.getSurveyQuestionsCollection();
            for (SurveyQuestions quest : lsq) {
                SurveyAnswers sa = ejbFacade.findSurveyAnswersByCustomerAndQuestion(customersController.getSelected(), quest);
                if (sa != null) {
                    surveyAnswers.add(sa);
                } else {
                    logger.log(Level.WARNING, "getSurveyList , answer for question \"{0}\" is null", quest.getQuestion());
                }
            }
        }

        return surveyAnswers;

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

    public Collection<SurveyAnswers> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        SurveyAnswers cm = (SurveyAnswers) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the selectSurvey
     */
    public SurveyMap getSelectSurvey() {
        return selectSurvey;
    }

    /**
     * @param selectSurvey the selectSurvey to set
     */
    public void setSelectSurvey(SurveyMap selectSurvey) {
        this.selectSurvey = selectSurvey;
    }

    /**
     * @return the usersSurveys
     */
    public ArrayList<SurveyMap> getUsersSurveys() {
        if (usersSurveys == null) {
            prepareSurveysForSelectedCustomer();
        }
        return usersSurveys;
    }

    /**
     * @param usersSurveys the usersSurveys to set
     */
    public void setUsersSurveys(ArrayList<SurveyMap> usersSurveys) {
        this.usersSurveys = usersSurveys;
    }

    /**
     * @return the selectedSurvey
     */
    public Surveys getSelectedSurvey() {
        return selectedSurvey;
    }

    /**
     * @param selectedSurvey the selectedSurvey to set
     */
    public void setSelectedSurvey(Surveys selectedSurvey) {
        this.selectedSurvey = selectedSurvey;
    }

    @FacesConverter(forClass = SurveyAnswers.class)
    public static class SurveyAnswersControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurveyAnswersController controller = (SurveyAnswersController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "surveyanswersController");
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
            if (object instanceof SurveyAnswers) {
                SurveyAnswers o = (SurveyAnswers) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SurveyAnswersController.class.getName());
            }
        }

    }

}
