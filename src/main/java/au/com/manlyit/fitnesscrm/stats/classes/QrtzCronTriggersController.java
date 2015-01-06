package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.QrtzCronTriggersFacade;
import au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetails;
import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;
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

@Named("qrtzCronTriggersController")
@SessionScoped
public class QrtzCronTriggersController implements Serializable {

    private QrtzCronTriggers current;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.QrtzCronTriggersFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    private QrtzCronTriggers[] multiSelected;
    private List<QrtzCronTriggers> filteredItems;
    private QrtzCronTriggers selectedForDeletion;

    public QrtzCronTriggersController() {
    }

    public QrtzCronTriggers getSelected() {
        if (getCurrent() == null) {
            setCurrent(new QrtzCronTriggers());
            setSelectedItemIndex(-1);
        }
        return getCurrent();
    }

    public void setSelected(QrtzCronTriggers t) {
        current = t;
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    private QrtzCronTriggersFacade getFacade() {
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
        setCurrent((QrtzCronTriggers) getItems().getRowData());
        setSelectedItemIndex(pagination.getPageFirstItem() + getItems().getRowIndex());
        return "View";
    }

    public String prepareCreate() {
        setCurrent(new QrtzCronTriggers());
        setSelectedItemIndex(-1);
        return "Create";
    }

    public String create() {
        try {
            getFacade().create(getCurrent());

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzCronTriggersCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        setCurrent((QrtzCronTriggers) getItems().getRowData());
        setSelectedItemIndex(pagination.getPageFirstItem() + getItems().getRowIndex());
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(getCurrent());
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzCronTriggersUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        setCurrent((QrtzCronTriggers) getItems().getRowData());
        setSelectedItemIndex(pagination.getPageFirstItem() + getItems().getRowIndex());
        performDestroy();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        updateCurrentItem();
        if (getSelectedItemIndex() >= 0) {
            return "View";
        } else {
            return "List";
        }
    }

    public void deleteListener(ActionEvent event) {
        performDestroy();
    }

    private void performDestroy() {
        try {
            getFacade().remove(getCurrent());
            recreateModel();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzCronTriggersDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (getSelectedItemIndex() >= count) {
            // selected index cannot be bigger than number of items:
            setSelectedItemIndex(count - 1);
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (getSelectedItemIndex() >= 0) {
            setCurrent(getFacade().findRange(new int[]{getSelectedItemIndex(), getSelectedItemIndex() + 1}).get(0));
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = new ListDataModel(getFacade().findAll());
        }
        return items;
    }

    protected void recreateModel() {
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
     * @return the current
     */
    public QrtzCronTriggers getCurrent() {
        return current;
    }

    /**
     * @param current the current to set
     */
    public void setCurrent(QrtzCronTriggers current) {
        this.current = current;
    }

    /**
     * @return the selectedItemIndex
     */
    public int getSelectedItemIndex() {
        return selectedItemIndex;
    }

    /**
     * @param selectedItemIndex the selectedItemIndex to set
     */
    public void setSelectedItemIndex(int selectedItemIndex) {
        this.selectedItemIndex = selectedItemIndex;
    }

    /**
     * @return the multiSelected
     */
    public QrtzCronTriggers[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(QrtzCronTriggers[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    /**
     * @return the filteredItems
     */
    public List<QrtzCronTriggers> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<QrtzCronTriggers> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the selectedForDeletion
     */
    public QrtzCronTriggers getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(QrtzCronTriggers selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
    }

    @FacesConverter(forClass = QrtzCronTriggers.class)
    public static class QrtzCronTriggersControllerConverter implements Converter {

        private static final String SEPARATOR = "#";
        private static final String SEPARATOR_ESCAPED = "\\#";

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            QrtzCronTriggersController controller = (QrtzCronTriggersController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "qrtzCronTriggersController");
            return controller.ejbFacade.find(getKey(value));
        }

        au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggersPK getKey(String value) {
            au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggersPK key;
            String values[] = value.split(SEPARATOR_ESCAPED);
            key = new au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggersPK();
            key.setSchedName(values[0]);
            key.setTriggerName(values[1]);
            key.setTriggerGroup(values[2]);
            return key;
        }

        String getStringKey(au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggersPK value) {
            StringBuffer sb = new StringBuffer();
            sb.append(value.getSchedName());
            sb.append(SEPARATOR);
            sb.append(value.getTriggerName());
            sb.append(SEPARATOR);
            sb.append(value.getTriggerGroup());
            return sb.toString();
        }

        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof QrtzCronTriggers) {
                QrtzCronTriggers o = (QrtzCronTriggers) object;
                return getStringKey(o.getQrtzCronTriggersPK());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + QrtzCronTriggersController.class.getName());
            }
        }
    }
}
