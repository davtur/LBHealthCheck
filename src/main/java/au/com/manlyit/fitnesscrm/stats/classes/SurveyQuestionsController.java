package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestions;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SurveyquestionsFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswerSubitems;
import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswers;
import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestionSubitems;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
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
import org.primefaces.context.RequestContext;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named("surveyquestionsController")
@SessionScoped
public class SurveyQuestionsController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(SurveyQuestionsController.class.getName());
    private SurveyQuestions current;
    private SurveyQuestions selectedForDeletion;
    private DataModel<SurveyQuestions> items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveyquestionsFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SurveysFacade surveysFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<SurveyQuestions> filteredItems;
    private SurveyQuestions[] multiSelected;
    private ArrayList<SurveyQuestionSubitems> subItems;
    private SurveyQuestionSubitems subItem;
    private SurveyQuestionSubitems deleteSubItem;
    private Surveys selectedSurvey;
    private List<SurveyAnswers> surveyAnswers;

    public SurveyQuestionsController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public SurveyQuestions getSelected() {
        if (current == null) {
            current = new SurveyQuestions(0, "");
            current.setSurveyQuestionSubitemsCollection(new ArrayList<>());
            current.setSurveyId(selectedSurvey);
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

    /**
     * @return the selectedSurvey
     */
    public Surveys getSelectedSurvey() {
        if (selectedSurvey == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            SurveysController surveysController = context.getApplication().evaluateExpressionGet(context, "#{surveysController}", SurveysController.class);
            ArrayList<Surveys> sia = new ArrayList<>(surveysController.getItemsAvailable());
            if (sia.isEmpty() == false) {
                selectedSurvey = sia.get(0);
            }
        }
        return selectedSurvey;
    }

    /**
     * @param selectedSurvey the selectedSurvey to set
     */
    public void setSelectedSurvey(Surveys selectedSurvey) {
        this.selectedSurvey = selectedSurvey;
    }

    private SurveyquestionsFacade getFacade() {
        return ejbFacade;
    }

    public DataModel<SurveyQuestions> getItems() {
        if (items == null) {
            items = new ListDataModel<>(new ArrayList<>(sortQuestionsByOrderField((List<SurveyQuestions>) getSelectedSurvey().getSurveyQuestionsCollection())));
        }
        return items;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getSelectedSurvey().getSurveyQuestionsCollection().size();
                }

                @Override
                public DataModel<SurveyQuestions> createPageDataModel() {
                    return new ListDataModel<>(new ArrayList<>(getSelectedSurvey().getSurveyQuestionsCollection()));
                }
            };
        }
        return pagination;
    }

    /**
     * @return the subItems
     */
    public ArrayList<SurveyQuestionSubitems> getSubItems() {
        if (subItems == null) {
            subItems = new ArrayList<>();
        }
        return subItems;
    }

    /**
     * @param subItems the subItems to set
     */
    public void setSubItems(ArrayList<SurveyQuestionSubitems> subItems) {
        this.subItems = subItems;
    }

    /**
     * @return the subItem
     */
    public SurveyQuestionSubitems getSubItem() {
        if (subItem == null) {
            subItem = new SurveyQuestionSubitems(0, "");
        }
        return subItem;
    }

    /**
     * @param subItem the subItem to set
     */
    public void setSubItem(SurveyQuestionSubitems subItem) {
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
        current = null;
        getSelected();

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

    Collection<SurveyQuestionSubitems> getSubItemsForCreate() {

        subItems.stream().forEach((SurveyQuestionSubitems si) -> {
            si.setQuestionId(getSelected());
        });

        return subItems;
    }

    public void addSubItem() {
        subItem.setQuestionId(getSelected());
        getSelected().getSurveyQuestionSubitemsCollection().add(subItem);
        // getSubItems().add(subItem);
        subItem = new SurveyQuestionSubitems(0, "");
    }

    /**
     * @return the deleteSubItem
     */
    public SurveyQuestionSubitems getDeleteSubItem() {
        return deleteSubItem;
    }

    /**
     * @param deleteSubItem the deleteSubItem to set
     */
    public void setDeleteSubItem(SurveyQuestionSubitems deleteSubItem) {
        this.deleteSubItem = deleteSubItem;
        if (getSelected().getSurveyQuestionSubitemsCollection().remove(deleteSubItem) == false) {
            LOGGER.log(Level.WARNING, "Could not remove subitem in method setDeleteSubItem(SurveyQuestionSubitems deleteSubItem) ");
        }
        //subItems.remove(deleteSubItem);
    }

    /* public void createDialogue(ActionEvent actionEvent) {
        try {
            current.setId(0);
            
            getFacade().create(current);
            current.setSurveyQuestionSubitemsCollection(getSubItemsForCreate());
            getFacade().edit(current);
            subItems = null;
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        
    }*/
    public void createDialogue(ActionEvent actionEvent) {
        try {
            current.setId(0);

            //getFacade().create(current);
            //current.setSurveyQuestionSubitemsCollection(getSubItemsForCreate());
            Collection<SurveyQuestions> sqc = getSelectedSurvey().getSurveyQuestionsCollection();
            sqc.add(current);
            getSelectedSurvey().setSurveyQuestionsCollection(sqc);
            surveysFacade.edit(getSelectedSurvey());
            subItems = null;
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public void editSurveyQuestion(ActionEvent actionEvent) {
        try {
            List<SurveyQuestions> sqc = (List<SurveyQuestions>) getSelectedSurvey().getSurveyQuestionsCollection();
            int oIndex = sqc.indexOf(current);

            ((List<SurveyQuestions>) getSelectedSurvey().getSurveyQuestionsCollection()).get(oIndex).setSurveyQuestionSubitemsCollection(current.getSurveyQuestionSubitemsCollection());
            ((List<SurveyQuestions>) getSelectedSurvey().getSurveyQuestionsCollection()).get(oIndex).setQuestion(current.getQuestion());
            ((List<SurveyQuestions>) getSelectedSurvey().getSurveyQuestionsCollection()).get(oIndex).setQuestionType(current.getQuestionType());

            surveysFacade.edit(getSelectedSurvey());
            subItems = null;
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsUpdated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public void prepareEditDialogue() {
        if (current.getSurveyQuestionSubitemsCollection() != null) {
            subItems = new ArrayList<>(current.getSurveyQuestionSubitemsCollection());
        }
        subItem = new SurveyQuestionSubitems(0, "");
        RequestContext.getCurrentInstance().update(":SurveyquestionsEditForm");
    }

    public String prepareEdit() {
        //current = (SurveyQuestions)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    private List<SurveyQuestions> sortQuestionsByOrderField(List<SurveyQuestions> lsq) {
        Comparator<SurveyQuestions> idComparator = new Comparator<SurveyQuestions>() {
            @Override
            public int compare(SurveyQuestions o1, SurveyQuestions o2) {
                return Integer.valueOf(o1.getQuestionOrder()).compareTo(o2.getQuestionOrder());
            }
        };
        Collections.sort(lsq, idComparator);
        return lsq;
    }

    private List<SurveyAnswers> prepareSurveysForDemo() {

        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController customersController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        Customers selectedCustomer = customersController.getSelected();
        ArrayList<SurveyAnswers> lsa = new ArrayList<>();

        try {

            List<SurveyQuestions> lsq = new ArrayList<>(getSelectedSurvey().getSurveyQuestionsCollection());
            sortQuestionsByOrderField(lsq);
            // if (lsa == null || lsa.isEmpty()) {// the survey hasn't been taken so add blank answers
            for (SurveyQuestions quest : lsq) {
                // ArrayList<SurveyAnswers> lsa = new ArrayList<>(quest.getSurveyAnswersCollection());

                SurveyAnswers answ = new SurveyAnswers(0, "");
                answ.setQuestionId(quest);
                answ.setId(0);
                //answ.setSurveyId(s);
                answ.setAnswerTypeid(quest.getQuestionType());
                answ.setUserId(selectedCustomer);
                // ejbFacade.create(answ);
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
                // ejbFacade.edit(answ);
                lsa.add(answ);

            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "prepareSurveysForSelectedCustomer()", e);
        }
        return lsa;
    }

    public List<SurveyAnswers> getSurveyDemoList() {
        if (surveyAnswers == null) {
            surveyAnswers = prepareSurveysForDemo();

            /*  FacesContext context = FacesContext.getCurrentInstance();
            CustomersController customersController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
            Collection<SurveyQuestions> lsq = selectedSurvey.getSurveyQuestionsCollection();
            for (SurveyQuestions quest : lsq) {
                SurveyAnswers sa = prepareSurveysForDemo();
                if (sa != null) {
                    surveyAnswers.add(sa);
                } else {
                    LOGGER.log(Level.WARNING, "getSurveyList , answer for question \"{0}\" is null", quest.getQuestion());
                }
            }*/
        }

        return surveyAnswers;

    }

    public void selectOneMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public void selectOneMenuSurveyValueChangeListener(ValueChangeEvent vce) {
        items = null;
        surveyAnswers = null;

    }

    public void selectManyMenuValueChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
    }

    public String update() {
        try {
            current.setSurveyQuestionSubitemsCollection(getSubItemsForCreate());
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
        getSelectedSurvey().getSurveyQuestionsCollection().remove(getSelected());
        try {
            surveysFacade.edit(getSelectedSurvey());
            current = null;
            subItems = null;
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SurveyquestionsDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
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

    private void recreateModel() {
        items = null;
        surveyAnswers = null;
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

    @FacesConverter(value = "surveyQuestionsControllerConverter")
    public static class SurveyquestionsControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SurveyQuestionsController controller = (SurveyQuestionsController) facesContext.getApplication().getELResolver().
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
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SurveyQuestionsController.class.getName());
            }
        }

    }

}
