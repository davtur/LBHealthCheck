package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Stat;
import au.com.manlyit.fitnesscrm.stats.db.StatTypes;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;

import org.primefaces.event.SelectEvent;

@ManagedBean(name = "statsTakenController")
@SessionScoped
public class StatsTakenController implements Serializable {

    private StatsTaken current;
    private StatsTaken selectedForDeletion;
    private Stat currentStat;
    private DataModel items = null;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbCustomerImages;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade ejbFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.StatTypesFacade ejbStatTypesFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @EJB
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private int rows = 1000000;

    public StatsTakenController() {
    }

    public StatsTaken getSelected() {
        if (current == null) {
            current = new StatsTaken();
            current.setDateRecorded(new Date());
            current.setCustomerId(getCustomer());
            current.setTrainerComments(" ");
            current.setImageId(1); //default image
            current.setId(0); // auto generated by DB , but cannot be null

            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(StatsTaken selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public Customers getCustomer() {
        Customers cust = null;
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        String name = custController.getSelected().getUsername();
        cust = ejbCustomerFacade.findCustomerByUsername(name);
        return cust;
    }

    public Stat getSelectedStat() {
        if (currentStat == null) {
            currentStat = new Stat();
            //selectedItemIndex = -1;
        }
        return currentStat;
    }

    private StatsTakenFacade getFacade() {
        return ejbFacade;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(rows) {

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

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (StatsTaken) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        recreateStatModel();
        updateCustomerImage(current.getImageId());
        return "View";
    }

    public String prepareCreate() {
        current = new StatsTaken();
        current.setDateRecorded(new Date());
        current.setCustomerId(getCustomer());
        current.setTrainerComments(" ");
        current.setImageId(1); //default image
        current.setId(0); // auto generated by DB , but cannot be null
        int count = 0;
        ArrayList<Stat> stats = new ArrayList<Stat>();
        List<StatTypes> statTypesList = ejbStatTypesFacade.findAll();
        for (StatTypes st : statTypesList) {
            count++;
            Stat stat = new Stat();
            stat.setId(count);
            stat.setStatsTakenId(current);
            stat.setStatType(st);
            stats.add(stat);
        }
        current.setStatCollection(stats);
        try {
            getFacade().create(current);
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Persistence Error");
        }
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenCreated"));
        recreateStatModel();
        current.setImageId(prepareCreateCustomerImage().getId());

        selectedItemIndex = -1;
        // set the parent of the list of stats 

        return "Create";
    }

    public String create() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenUpdated"));
            prepareCreate();
            return "Create";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return "List";
        }
    }

    public String createAndReturnToList() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenUpdated"));

            return "List";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return "List";
        }
    }

    public void recreateStatModel() {
        FacesContext context = FacesContext.getCurrentInstance();
        StatController statController = (StatController) context.getApplication().evaluateExpressionGet(context, "#{statController}", StatController.class);
        statController.setParent(current);
        statController.recreateModel();

    }

    /*  public void prepareCreateStat() {
     // add each stat type to the table;
     ArrayList<Stat> stats = new  ArrayList<Stat>();
     List<StatTypes> statTypesList = ejbStatTypesFacade.findAll();
     for (StatTypes st : statTypesList) {
     Stat stat = new Stat();
     stat.setId(-1);
     stat.setStatsTakenId(current);
     stat.setStatType(st);
     current.getStatCollection().add(stat);
     }
     }*/
    public CustomerImages prepareCreateCustomerImage() {
        CustomerImages ci = new CustomerImages();
        ci.setId(-1);
        ci.setDatetaken(new Date());
        ci.setCustomerId(current.getCustomerId());
        //getFacade().create(current);
        ejbCustomerImages.create(ci);
        FacesContext context = FacesContext.getCurrentInstance();
        CustomerImagesController ciController = (CustomerImagesController) context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
        ciController.prepareCreateFromStatTakenController(ci);
        return ci;
    }

    public void updateCustomerImage(int id) {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomerImagesController ciController = (CustomerImagesController) context.getApplication().evaluateExpressionGet(context, "#{customerImagesController}", CustomerImagesController.class);
        CustomerImages ci = ejbCustomerImages.find(id);
        if (ci == null) {
            String message = "The Image wasnt found :-( " + id;
            JsfUtil.addErrorMessage(message);
        }
        ciController.setCurrent(ci);

    }

    public void destroyImage() {
        ejbCustomerImages.remove(ejbCustomerImages.find(current.getImageId()));
    }

    public void destroyStats() {
        int id = current.getId();
        List<Stat> stats = ejbStatFacade.findAll(id);
        for (Stat s : stats) {
            ejbStatFacade.remove(s);
        }
        //FacesContext context = FacesContext.getCurrentInstance();
        //StatController statController = (StatController) context.getApplication().evaluateExpressionGet(context, "#{statController}", StatController.class);
        // statController.recreateModel();
    }

    public String prepareEdit() {
        //current = (StatsTaken) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        recreateStatModel();
        updateCustomerImage(current.getImageId());
        return "Edit";
    }

    public void handleDateSelect(SelectEvent event) {
        Date date = (Date) event.getObject();
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");

        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(date)));
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenUpdated"));
            return "List";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void destroy() {
        current = (StatsTaken) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("FitnessAssessmentDetailsDeleted"));
    }

    public String discard() {
        try {
            destroyStats();
            destroyImage();
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenDiscarded"));
            recreateModel();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
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

    private void performDestroy() {
        try {
            destroyStats();
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenDeleted"));
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
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
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

    /**
     * @return the ejbStatFacade
     */
    public au.com.manlyit.fitnesscrm.stats.beans.StatsFacade getEjbStatFacade() {
        return ejbStatFacade;
    }

    /**
     * @param ejbStatFacade the ejbStatFacade to set
     */
    public void setEjbStatFacade(au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatFacade) {
        this.ejbStatFacade = ejbStatFacade;
    }

    public void handleUserChange() {
        JsfUtil.addSuccessMessage("User Selected");
    }

    /**
     * @return the rows
     */
    public int getRows() {
        return rows;
    }

    /**
     * @param rows the rows to set
     */
    public void setRows(int rows) {
        this.rows = rows;
    }

    /**
     * @return the selectedForDeletion
     */
    public StatsTaken getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(StatsTaken selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    @FacesConverter(forClass = StatsTaken.class)
    public static class StatsTakenControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StatsTakenController controller = (StatsTakenController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "statsTakenController");
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
            if (object instanceof StatsTaken) {
                StatsTaken o = (StatsTaken) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + StatsTakenController.class.getName());
            }
        }
    }
}
/* old version without editable table


 import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
 import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
 import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
 import au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade;
 import au.com.manlyit.fitnesscrm.stats.db.Customers;
 import au.com.manlyit.fitnesscrm.stats.db.Stat;

 import java.io.Serializable;
 import java.text.SimpleDateFormat;
 import java.util.Date;
 import java.util.ResourceBundle;
 import javax.ejb.EJB;
 import javax.faces.application.FacesMessage;
 import javax.faces.bean.ManagedBean;
 import javax.faces.bean.SessionScoped;
 import javax.faces.component.UIComponent;
 import javax.faces.context.FacesContext;
 import javax.faces.convert.Converter;
 import javax.faces.convert.FacesConverter;
 import javax.faces.model.DataModel;
 import javax.faces.model.ListDataModel;
 import javax.faces.model.SelectItem;
 import org.primefaces.event.DateSelectEvent;

 @ManagedBean(name = "statsTakenController")
 @SessionScoped
 public class StatsTakenController_old implements Serializable {

 private StatsTaken current;
 private Stat currentStat;
 private DataModel items = null;
 @EJB
 private au.com.manlyit.fitnesscrm.stats.beans.StatsTakenFacade ejbFacade;
 @EJB
 private au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatFacade;
 @EJB
 private au.com.manlyit.fitnesscrm.stats.beans.StatTypesFacade ejbStatTypesFacade;
 @EJB
 private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
 private PaginationHelper pagination;
 private int selectedItemIndex;

 public StatsTakenController_old() {
 }

 public StatsTaken getSelected() {
 if (current == null) {
 current = new StatsTaken();
 selectedItemIndex = -1;
 }
 return current;
 }

 public Customers getCustomer() {
 Customers cust = null;
 FacesContext facesContext = FacesContext.getCurrentInstance();
 String name = facesContext.getExternalContext().getRemoteUser();
 cust = ejbCustomerFacade.findCustomerByUsername(name);

 return cust;
 }

 public Stat getSelectedStat() {
 if (currentStat == null) {
 currentStat = new Stat();
 //selectedItemIndex = -1;
 }
 return currentStat;
 }

 private StatsTakenFacade getFacade() {
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

 public String prepareList() {
 recreateModel();
 return "List";
 }

 public String prepareView() {
 current = (StatsTaken) getItems().getRowData();
 selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
 return "View";
 }

 public String prepareCreate() {
 current = new StatsTaken();
 current.setId(0);
 current.setDateRecorded(new Date());
 current.setCustomerId(getCustomer());
 current.setTrainerComments(" ");
 current.setId(0); // auto generated by DB , but cannot be null    \n         getFacade().create(current); 
 prepareCreateStat();

 selectedItemIndex = -1;
 // set the parent of the list of stats 

 return "Create";
 }

 public String create() {
 try {
 getFacade().edit(current);
 JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenCreated"));
 return prepareCreate();
 } catch (Exception e) {
 JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
 return null;
 }
 }

 public void prepareCreateStat() {
 currentStat = new Stat();
 currentStat.setId(0);
 currentStat.setStatsTakenId(current);
 FacesContext context = FacesContext.getCurrentInstance();
 StatController statController = (StatController) context.getApplication().evaluateExpressionGet(context, "#{statController}", StatController.class);
 statController.setParent(current);
 statController.recreateModel();


 }

 public void createStat() {
 try {
 getEjbStatFacade().create(currentStat);
 JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatCreated"));
 prepareCreateStat();
 } catch (Exception e) {
 JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

 }
 }

 public void destroyStat() {
 FacesContext context = FacesContext.getCurrentInstance();
 StatController statController = (StatController) context.getApplication().evaluateExpressionGet(context, "#{statController}", StatController.class);
 statController.destroy();
 statController.recreateModel();
 }

 public String prepareEdit() {
 current = (StatsTaken) getItems().getRowData();
 selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
 return "Edit";
 }

 public void handleDateSelect(DateSelectEvent event) {
 SimpleDateFormat format = new SimpleDateFormat("MM/dd/yyyy");

 FacesContext facesContext = FacesContext.getCurrentInstance();
 facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(event.getDate())));
 }

 public String update() {
 try {
 getFacade().edit(current);
 JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenUpdated"));
 return "View";
 } catch (Exception e) {
 JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
 return null;
 }
 }

 public String destroy() {
 current = (StatsTaken) getItems().getRowData();
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

 private void performDestroy() {
 try {
 getFacade().remove(current);
 JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatsTakenDeleted"));
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
 }

 public String next() {
 getPagination().nextPage();
 recreateModel();
 return "List";
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

 **
 * @return the ejbStatFacade
 *
 public au.com.manlyit.fitnesscrm.stats.beans.StatsFacade getEjbStatFacade() {
 return ejbStatFacade;
 }

 **
 * @param ejbStatFacade the ejbStatFacade to set
 *
 public void setEjbStatFacade(au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbStatFacade) {
 this.ejbStatFacade = ejbStatFacade;
 }

 @FacesConverter(forClass = StatsTaken.class)
 public static class StatsTakenControllerConverter implements Converter {

 public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
 if (value == null || value.length() == 0) {
 return null;
 }
 StatsTakenController controller = (StatsTakenController) facesContext.getApplication().getELResolver().
 getValue(facesContext.getELContext(), null, "statsTakenController");
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
 if (object instanceof StatsTaken) {
 StatsTaken o = (StatsTaken) object;
 return getStringKey(o.getId());
 } else {
 throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + StatsTakenController.class.getName());
 }
 }
 }
 }


 */
