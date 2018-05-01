package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.JobConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.db.ConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.JobConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.Tasks;


import java.io.Serializable;
import java.util.List;

import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
//import javax.enterprise.context.SessionScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.event.ActionEvent;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;

import javax.faces.model.SelectItem;
import javax.inject.Named;

//@Named("jobConfigMapController")
@Named("jobConfigMapController")
@SessionScoped
public class JobConfigMapController implements Serializable {
    
    private JobConfigMap current;
    private JobConfigMap selectedForDeletion;
    private DataModel items = null;
    @EJB
    private JobConfigMapFacade ejbFacade;
    @EJB
    private ConfigMapFacade configMapFacade;
    private static final Logger logger = Logger.getLogger(JobConfigMapController.class.getName());
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private String bulkvalue = "";
    private String duplicateValues = "";
    
    public JobConfigMapController() {
    }
    
    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
        
    }
    
    public JobConfigMap getSelected() {
        if (current == null) {
            current = new JobConfigMap();
            selectedItemIndex = -1;
        }
        return current;
    }
    
    public void setSelected(JobConfigMap selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }
        
    }
    
    public String createBulk() {
        int count = 0;
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            TasksController controller = (TasksController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{tasksController}", TasksController.class);
            Tasks task = controller.getSelected();
            
            StringTokenizer st = new StringTokenizer(getBulkvalue(), "\r\n");
            String duplicateLines = "";
            
            while (st.hasMoreTokens()) {
                String line = st.nextToken();
                int d = line.indexOf("=");
                if (d == -1) {
                }
                if (d > 1) {
                    if (line.indexOf("==") == d) {
                        //check for a config key instaed
                        count++;
                        String val = line.substring(d + 2);
                        String configKey = val.trim();
                        List<ConfigMap> cml = configMapFacade.findAllByConfigKey(configKey, true);
                        if (cml.isEmpty()) {
                            duplicateLines += line + " *** CONFIG KEY NOT FOUND ***\r\n";
                        }
                        for (ConfigMap cm : cml) {
                            JobConfigMap cm1 = new JobConfigMap(0, cm, task);
                            cm1.setBasicKey(configKey);
                            cm1.setBasicValue("*** INHERITED FROM CONFIGMAP ***");
                            try {
                                getFacade().create(cm1);
                            } catch (EJBException ejbe) {
                                Exception e1 = ejbe.getCausedByException();
                                Exception e = (Exception) e1.getCause();
                                String mess = e.getMessage();
                                if (mess.contains("Duplicate entry")) {
                                    count--;
                                    duplicateLines += line + "\r\n";
                                } else {
                                    logger.log(Level.SEVERE, "Bulk Config Insert Failed:" + line + "\r\n", e);
                                }
                            }
                            
                        }
                    } else {
                        // only one equals sign in the string so we can safley add it as a key value pair
                        count++;
                        String k = line.substring(0, d);
                        String v = line.substring(d + 1);
                        JobConfigMap cm1 = new JobConfigMap(0, k, v, task);
                        try {
                            getFacade().create(cm1);
                        } catch (EJBException ejbe) {
                            Exception e1 = ejbe.getCausedByException();
                            Exception e = (Exception) e1.getCause();
                            String mess = e.getMessage();
                            if (mess.contains("Duplicate entry")) {
                                count--;
                                duplicateLines += line + "\r\n";
                            } else {
                                logger.log(Level.SEVERE, "Bulk Config Insert Failed:" + line + "\r\n", e);
                            }
                        }
                    }
                    
                } else {
                    JsfUtil.addSuccessMessage("This key does not have a value:" + line);
                }
            }
            setDuplicateValues(duplicateLines);
            
            
            
            JsfUtil.addSuccessMessage(count + ", " + configMapFacade.getConfig("ConfigMapCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    private JobConfigMapFacade getFacade() {
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
        //current = (JobConfigMap)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }
    
    public String prepareCreate() {
        current = new JobConfigMap();
        current.setIdjobConfigMap(0);
        selectedItemIndex = -1;
        return "Create";
    }
     
    public String create() {
        try {
            if(current.getBasicKey().trim().isEmpty() && current.getConfigMapKey() == null){
                return null;
            }
            if(current.getBasicKey().trim().isEmpty() && current.getConfigMapKey() != null){
                 current.setBasicKey(current.getConfigMapKey().getConfigkey());
                            current.setBasicValue("*** INHERITED FROM CONFIGMAP ***");
            }
            
            getFacade().create(current);
            
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    public void createFromTasksEditor(ActionEvent event) {
        try {
            
            if (current.getBasicKey().trim().isEmpty()) {
                current.setBasicKey(null);
            }
            if (current.getIdjobConfigMap() == null) {
                current.setIdjobConfigMap(0);
            }
            if (current.getBasicKey() == null && current.getConfigMapKey() == null) {
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapErrorBothNull"));
                return ;
            }
            if (current.getBasicKey() == null) {
                current.setBasicKey(current.getConfigMapKey().getConfigkey());
                current.setBasicValue("*** INHERITED FROM CONFIGMAP ***");
            }
            if (current.getConfigMapKey() != null && current.getBasicKey() != null) {
                current.setBasicValue("*** INHERITED FROM CONFIGMAP ***");
            } else if (current.getConfigMapKey() != null && current.getBasicKey() == null) {
                current.setBasicKey(current.getConfigMapKey().getConfigkey());
                current.setBasicValue("*** INHERITED FROM CONFIGMAP ***");
            }
            
            if (current.getIdjobConfigMap() == 0) {
                FacesContext facesContext = FacesContext.getCurrentInstance();
                TasksController controller = (TasksController) facesContext.getApplication().getELResolver().
                        getValue(facesContext.getELContext(), null, "tasksController");
                current.setIdjobConfigMap(0);
                current.setParentTask(controller.getSelected());
                getFacade().create(current);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapCreated"));
            } else {
                getFacade().edit(current);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapUpdated"));
                
            }
            
            current = new JobConfigMap();
            current.setIdjobConfigMap(0);
            selectedItemIndex = -1;
            recreateModel();
            FacesContext facesContext = FacesContext.getCurrentInstance();
            TasksController controller = (TasksController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{tasksController}", TasksController.class);
            controller.setFilteredItems(null);
            
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            
        }
    }
    
    public String prepareEdit() {
        //current = (JobConfigMap)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }
    
    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    public String destroy() {
        current = (JobConfigMap) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }
    
    public void destroyFromTasks() {
        current = selectedForDeletion;
        selectedItemIndex = -1;
        performDestroy();
        recreateModel();
        
    }
    
    public int deleteAllForTask(Tasks tsk) {
        List<JobConfigMap> jcml = getFacade().findConfigForTask(tsk);
        if (jcml == null) {
            return 0;
        }
        if (jcml.isEmpty()) {
            return 0;
        }
        int numberDeleted = 0;
        for (JobConfigMap jcm : jcml) {
            getFacade().remove(jcm);
        }
        numberDeleted = jcml.size();
        return numberDeleted;
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
    
    public JobConfigMap getSelectedForDeletion() {
        return selectedForDeletion;
    }
    
    public void setSelectedForDeletion(JobConfigMap selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;
        
        performDestroy();
        recreateModel();
        
    }
    
    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("JobConfigMapDeleted"));
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
        FacesContext facesContext = FacesContext.getCurrentInstance();
        TasksController controller = (TasksController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{tasksController}", TasksController.class);
        controller.setFilteredItems(null);
        
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
    
    @FacesConverter(value="jobConfigMapControllerConverter")
    public static class JobConfigMapControllerConverter implements Converter {
        
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            JobConfigMapController controller = (JobConfigMapController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "jobConfigMapController");
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
            if (object instanceof JobConfigMap) {
                JobConfigMap o = (JobConfigMap) object;
                return getStringKey(o.getIdjobConfigMap());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + JobConfigMapController.class.getName());
            }
        }
    }
}
