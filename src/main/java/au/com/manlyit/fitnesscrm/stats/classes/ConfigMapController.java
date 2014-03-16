package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.ConfigMap;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.LazyLoadingDataModel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ejb.EJBException;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.persistence.EntityExistsException;
import javax.persistence.PersistenceException;
import org.primefaces.event.RowEditEvent;
import org.primefaces.model.LazyDataModel;

@Named("configMapController")
@SessionScoped
public class ConfigMapController implements Serializable {

    private ConfigMap current;
    private ConfigMap selectedForDeletion;
    private DataModel items = null;
    private String password1 = "";
    private String password2 = "";
    private boolean password = false;
    private LazyDataModel lazyModel;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String bulkvalue = "";
    private String duplicateValues = "";
    private List<ConfigMap> filteredItems;
    private ConfigMap[] multiSelected;
    private static final Logger logger = Logger.getLogger(ConfigMapController.class.getName());

    public ConfigMapController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public ConfigMap getSelected() {
        if (current == null) {
            current = new ConfigMap();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(ConfigMap selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }
   
    public LazyDataModel<ConfigMap> getLazyModel() {
        if(lazyModel == null){
            lazyModel = new LazyLoadingDataModel(ejbFacade);
        }
        return lazyModel;
    }
    public String getKey(String key) {
        return ejbFacade.getConfig(key);
    }


    private ConfigMapFacade getFacade() {
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
        //current = (ConfigMap)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new ConfigMap();
        selectedItemIndex = -1;
        return "Create";
    }

    public void createDialogue(ActionEvent actionEvent) {

        current.setId(0);
        getFacade().create(current);
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ConfigMapCreated"));



    }

    public void createPasswordDialogue(ActionEvent actionEvent) {

        
        current.setId(0);
       if(getPassword1().compareTo(getPassword2()) == 0){
        current.setConfigvalue(password1);
        getFacade().createEncrypted(current);
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("ConfigMapCreated"));
       }else{
          JsfUtil.addSuccessMessage(configMapFacade.getConfig("The Passwords Dont Match!")); 
       }


    }

    public String create() {
        try {
            current.setId(0);
            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ConfigMapCreated"));
            return prepareCreate();
        } catch (EntityExistsException e) {
            JsfUtil.addErrorMessage(e, "Sorry, I can't put the entity in the database as it already exists. Edit or delete the object instead.");
            return null;
        } catch (PersistenceException e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String createBulk() {
        int count = 0;
        try {
            StringTokenizer st = new StringTokenizer(bulkvalue, "\r\n");
            String duplicateLines = "";
 
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                int d = line.indexOf("=");
                if (d > 1) {

                    // only one equals sign in the string so we can safley add it as a key value pair
                    count++;
                    String k = line.substring(0, d);
                    String v = line.substring(d + 1);
                    //ConfigMap cm1 = new ConfigMap(0, k, v);
                     ConfigMap cm1 = getFacade().getConfigMapFromKey(k);
                    try {
                        String existingValue = cm1.getConfigvalue();
                        if(existingValue.indexOf("??? ConfigMap key not found") == 0) {
                            cm1.setConfigvalue(v);
                            getFacade().edit(cm1);
                        }else{
                            count--;
                            duplicateLines += line + "\r\n";
                        }
                        
                    } catch (EJBException ejbe) {
                        Exception e1 = ejbe.getCausedByException();
                        Exception e = (Exception) e1.getCause();
                        String mess = e.getMessage();
                        if (mess.indexOf("Duplicate entry") != -1) {
                            count--;
                            duplicateLines += line + "\r\n";
                        } else {
                            logger.log(Level.SEVERE, "Bulk Config Insert Failed:" + line + "\r\n", e);
                        }
                    }catch (Exception e) {
                        logger.log(Level.SEVERE, "Bulk Config Insert Failed:" + line + "\r\n", e);
                    }

                } else {
                    JsfUtil.addSuccessMessage("This key does not have a value:" + line);
                }
            }
            setDuplicateValues(duplicateLines);


            logger.log(Level.INFO, "Bulk Insert to config map completed. Updated or created count={0}", count);
            JsfUtil.addSuccessMessage(count + ", " + configMapFacade.getConfig("ConfigMapCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void onEdit(RowEditEvent event) {
        ConfigMap cm = (ConfigMap) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("RowEditSuccessful"));
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage(configMapFacade.getConfig("RowEditCancelled"));
    }

    public String prepareEdit() {
        //current = (ConfigMap)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ConfigMapUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (ConfigMap) getItems().getRowData();
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

    public ConfigMap getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(ConfigMap selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("ConfigMapDeleted"));
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

    public SelectItem[] getConfigJobClassesAvailableSelectOne() {

        ArrayList<String> al = new ArrayList<String>();
        List<ConfigMap> cms = ejbFacade.findAllByConfigKey("jobs.classnames.callable.%", true);
        for (ConfigMap cm : cms) {
            al.add(cm.getConfigvalue());
        }
        return JsfUtil.getSelectItems(al, true);

    }

    /**
     * @return the bulkvalue
     */
    public String getBulkvalue() {
        return bulkvalue;
    }

    /**
     * @param bulkvalue the bulkvalue to set
     */
    public void setBulkvalue(String bulkvalue) {
        this.bulkvalue = bulkvalue;
    }

    /**
     * @return the duplicateValues
     */
    public String getDuplicateValues() {
        return duplicateValues;
    }

    /**
     * @param duplicateValues the duplicateValues to set
     */
    public void setDuplicateValues(String duplicateValues) {
        this.duplicateValues = duplicateValues;
    }

    /**
     * @return the filteredItems
     */
    public List<ConfigMap> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<ConfigMap> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public ConfigMap[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(ConfigMap[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    /**
     * @return the password1
     */
    public String getPassword1() {
        return password1;
    }

    /**
     * @param password1 the password1 to set
     */
    public void setPassword1(String password1) {
        this.password1 = password1;
    }

    /**
     * @return the password2
     */
    public String getPassword2() {
        return password2;
    }

    /**
     * @param password2 the password2 to set
     */
    public void setPassword2(String password2) {
        this.password2 = password2;
    }

    /**
     * @return the password
     */
    public boolean isPassword() {
        return password;
    }

    /**
     * @param password the password to set
     */
    public void setPassword(boolean password) {
        this.password = password;
    }

    @FacesConverter(forClass = ConfigMap.class)
    public static class ConfigMapControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            ConfigMapController controller = (ConfigMapController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "configMapController");
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
            if (object instanceof ConfigMap) {
                ConfigMap o = (ConfigMap) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + ConfigMapController.class.getName());
            }
        }
    }
}
