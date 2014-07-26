package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.JobConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.beans.TasksFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.db.JobConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.Tasks;


import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import org.primefaces.event.RowEditEvent;

@ManagedBean(name = "tasksController")
@SessionScoped
public class TasksController implements Serializable {
    
    private static final Logger logger = Logger.getLogger(TasksController.class.getName());
    private Tasks current;
    private Tasks selectedForDeletion;
    private DataModel items = null;
    private Tasks[] multiSelected;
    @EJB
    private TasksFacade ejbFacade;
    @EJB
    private JobConfigMapFacade JobConfigMapFacade;
    @EJB
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<String> jobClassNames;
    private int rowsReturnedByTaskQuery = 0;
    private DataModel filteredItems = null;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    
    public TasksController() {
    }
    
    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
        
    }
    
    public void scheduleSelected() {
        scheduleSelectedTask(false);
    }
    
    public void runSelectedNow() {
        scheduleSelectedTask(true);
    }

    public void cloneSelected() {
        int c = 0;
        int e = 0;
        for (Tasks tsk : getMultiSelected()) {
             boolean returnVal = cloneTask(tsk);
            if (returnVal == false) {               
                e++;
            } else {
                c++;
            }
        }
        String m = "Cloned " + c + " tasks successfully. ";
        if (e > 0) {
            m += " " + e + " Failed.";
        }
        recreateModel();
        JsfUtil.addSuccessMessage(m);
        
    }    

    private boolean cloneTask(Tasks tsk) {
        boolean success = false;
        Tasks clone = new Tasks(0);
        
        try {
            clone.setCronEntry(tsk.getCronEntry());
            clone.setDescription(tsk.getDescription());
            clone.setName(tsk.getName() + "(Cloned " + sdf.format(new Date()) + ")");
            clone.setTaskClassName(tsk.getTaskClassName());
            getFacade().create(clone);
            Collection<JobConfigMap> configRows = tsk.getJobConfigMapCollection();
            for (JobConfigMap jcm : configRows) {
                jcm.setParentTask(clone);
                jcm.setIdjobConfigMap(0);
                JobConfigMapFacade.create(jcm);
            }
            success = true;
        } catch (Exception e) {
            logger.log(Level.WARNING, "Clone Tasks Error: ", e.getMessage());
        }
        
        return success;
    }

    private void scheduleSelectedTask(boolean runImmediately) {
        int c = 0;
        int e = 0;
        String responseMessage = "";
        for (Tasks tsk : getMultiSelected()) {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            QrtzJobDetailsController controller = (QrtzJobDetailsController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{qrtzJobDetailsController}", QrtzJobDetailsController.class);
            String returnVal = controller.scheduleTask(tsk, runImmediately);
            if (!returnVal.contains("success")) {
                responseMessage = responseMessage + "\r\n" + returnVal;
                e++;
            } else {
                c++;
            }
        }
        String m = "Scheduled " + c + " tasks successfully. ";
        if (e > 0) {
            m += " " + e + " Failed." + responseMessage;
        }
        JsfUtil.addSuccessMessage(m);
    }
    
    public Tasks getSelected() {
        if (current == null) {
            current = new Tasks();
            selectedItemIndex = -1;
        }
        return current;
    }
    
    public void setSelected(Tasks selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }
        
    }
    
    private TasksFacade getFacade() {
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
        //current = (Tasks)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }
    
    public String prepareCreate() {
        current = new Tasks();
        selectedItemIndex = -1;
        recreateModel();
        return "Create";
    }
    
    public String create() {
        try {
            if (current.getIdtasks() == null) {
                current.setIdtasks(0);
            }
            getFacade().create(current);
            FacesContext facesContext = FacesContext.getCurrentInstance();
            JobConfigMapController controller = (JobConfigMapController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{jobConfigMapController}", JobConfigMapController.class);
            controller.createBulk();
            
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TasksCreated"));
            return prepareCreate();
        } catch (EJBException ejbe) {
            Exception e1 = ejbe.getCausedByException();
            Exception e = (Exception) e1.getCause();
            String mess = e.getMessage();
            if (mess.indexOf("Duplicate entry") != -1) {
                JsfUtil.addErrorMessage("That task name already exists. It must be unique.");
            } else {
                logger.log(Level.WARNING, "Create Task Failed\r\n", e);
            }
            return null;
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    public void onEdit(RowEditEvent event) {
        JobConfigMap cm = (JobConfigMap) event.getObject();
        JobConfigMapFacade.edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage(configMapFacade.getConfig("RowEditSuccessful"));
    }
    
    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage(configMapFacade.getConfig("RowEditCancelled"));
    }
    
    public String prepareEdit() {
        //current = (Tasks)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        filteredItems = null;
        return "Edit";
    }
    
    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TasksUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }
    
    public String destroy() {
        current = (Tasks) getItems().getRowData();
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
    
    public Tasks getSelectedForDeletion() {
        return selectedForDeletion;
    }
    
    public void setSelectedForDeletion(Tasks selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;
        
        performDestroy();
        recreateModel();
        
    }
    
    private void performDestroy() {
        try {
            FacesContext facesContext = FacesContext.getCurrentInstance();
            JobConfigMapController controller = (JobConfigMapController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{jobConfigMapController}", JobConfigMapController.class);
            controller.deleteAllForTask(current);
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("TasksDeleted"));
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
    
    public DataModel getItemsFilteredByTaskId() {
        
        if (filteredItems == null) {
            //FacesContext facesContext = FacesContext.getCurrentInstance();
            //TasksController controller = (TasksController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{tasksController}", TasksController.class);
            Tasks task = this.getSelected();
            
            filteredItems = new ListDataModel(JobConfigMapFacade.findConfigForTask(task));
            int rowsCount = filteredItems.getRowCount();
            if (rowsCount == -1) {
                rowsCount = 0;
            }
            setRowsReturnedByTaskQuery(rowsCount);
        }
        return filteredItems;
    }

    /**
     * @return the filteredItems
     */
    public DataModel getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(DataModel filteredItems) {
        this.filteredItems = filteredItems;
    }
    
    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }
    
    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }
    
    private void updateJobClassNames() {
        ArrayList<String> al = new ArrayList<String>();
        al.add("CronJobCallableThreadFactory");
        al.add("");
        
    }

    /**
     * @return the rowsReturnedByTaskQuery
     */
    public int getRowsReturnedByTaskQuery() {
        // FacesContext facesContext = FacesContext.getCurrentInstance();
        // TasksController controller = (TasksController) facesContext.getApplication().evaluateExpressionGet(facesContext, "#{tasksController}", TasksController.class);
        Tasks task = this.getSelected();
        
        return JobConfigMapFacade.countByTask(task);
    }

    /**
     * @param rowsReturnedByTaskQuery the rowsReturnedByTaskQuery to set
     */
    public void setRowsReturnedByTaskQuery(int rowsReturnedByTaskQuery) {
        this.rowsReturnedByTaskQuery = rowsReturnedByTaskQuery;
    }

    /**
     * @return the jobClassNames
     */
    public List<String> getJobClassNames() {
        if (jobClassNames == null) {
            updateJobClassNames();
        }
        return jobClassNames;
    }

    /**
     * @param jobClassNames the jobClassNames to set
     */
    public void setJobClassNames(List<String> jobClassNames) {
        this.jobClassNames = jobClassNames;
    }

    /**
     * @return the multiSelected
     */
    public Tasks[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Tasks[] multiSelected) {
        this.multiSelected = multiSelected;
    }
    
    @FacesConverter(forClass = Tasks.class)
    public static class TasksControllerConverter implements Converter {
        
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            TasksController controller = (TasksController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "tasksController");
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
            if (object instanceof Tasks) {
                Tasks o = (Tasks) object;
                return getStringKey(o.getIdtasks());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + TasksController.class.getName());
            }
        }
    }
}
