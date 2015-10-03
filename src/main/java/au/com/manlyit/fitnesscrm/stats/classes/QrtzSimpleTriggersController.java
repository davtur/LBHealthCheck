package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.QrtzSimpleTriggers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.QrtzSimpleTriggersFacade;

import javax.inject.Inject;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import java.io.Serializable;

@Named("qrtzSimpleTriggersController")
@SessionScoped
public class QrtzSimpleTriggersController implements Serializable {

    private QrtzSimpleTriggers current;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.QrtzSimpleTriggersFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    public QrtzSimpleTriggersController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public QrtzSimpleTriggers getSelected() {
        if (current == null) {
            current = new QrtzSimpleTriggers();
            selectedItemIndex = -1;
        }
        return current;
    }

    private QrtzSimpleTriggersFacade getFacade() {
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
        current = (QrtzSimpleTriggers) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new QrtzSimpleTriggers();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzSimpleTriggersCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (QrtzSimpleTriggers) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzSimpleTriggersUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (QrtzSimpleTriggers) getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzSimpleTriggersDeleted"));
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

    @FacesConverter(value="qrtzSimpleTriggersControllerConverter", forClass = QrtzSimpleTriggers.class)
    public static class QrtzSimpleTriggersControllerConverter implements Converter {

        private static final String SEPARATOR = "#";
        private static final String SEPARATOR_ESCAPED = "\\#";

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            QrtzSimpleTriggersController controller = (QrtzSimpleTriggersController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "qrtzSimpleTriggersController");
            return controller.ejbFacade.find(getKey(value));
        }

        au.com.manlyit.fitnesscrm.stats.db.QrtzSimpleTriggersPK getKey(String value) {
            au.com.manlyit.fitnesscrm.stats.db.QrtzSimpleTriggersPK key;
            String values[] = value.split(SEPARATOR_ESCAPED);
            key = new au.com.manlyit.fitnesscrm.stats.db.QrtzSimpleTriggersPK();
            key.setSchedName(values[0]);
            key.setTriggerName(values[1]);
            key.setTriggerGroup(values[2]);
            return key;
        }

        String getStringKey(au.com.manlyit.fitnesscrm.stats.db.QrtzSimpleTriggersPK value) {
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
            if (object instanceof QrtzSimpleTriggers) {
                QrtzSimpleTriggers o = (QrtzSimpleTriggers) object;
                return getStringKey(o.getQrtzSimpleTriggersPK());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + QrtzSimpleTriggersController.class.getName());
            }
        }
    }
}
