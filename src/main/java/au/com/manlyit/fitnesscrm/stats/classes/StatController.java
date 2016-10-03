package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.Stat;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.StatsFacade;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;

import java.io.Serializable;
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
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;

@Named("statController")
@SessionScoped
public class StatController implements Serializable {

    private Stat current;
    private StatsTaken parent;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.StatsFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private int rows = 1000000;
    private Stat selectedStat = new Stat();
    //private Cell selectedCell;

    public StatController() {
    }

    public Stat getSelected() {
        if (current == null) {
            current = new Stat();
            selectedItemIndex = -1;
        }
        return current;
    }

    private StatsFacade getFacade() {
        return ejbFacade;
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole ;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(rows) {

                @Override
                public int getItemsCount() {
                    if (parent == null) {
                        return getFacade().count();
                    } else {
                        return getFacade().count(parent.getId());
                    }
                }

                @Override
                public DataModel createPageDataModel() {
                    if (parent == null) {
                        return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                    } else {
                        return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}, parent.getId()));

                    }
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
        current = (Stat) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Stat();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            current.setId(0);
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public void createInPlace() {
        try {
            current.setId(0);
            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatCreated"));

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));

        }
    }

    public String prepareEdit() {
        current = (Stat) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (Stat) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public void destroyInPlace() {
        current = (Stat) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();

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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("StatDeleted"));
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

    public void recreateModel() {
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

    public void onEditRow2(RowEditEvent event) {

        Stat obj ;
        try {
            obj = (Stat) event.getObject();
            ejbFacade.edit(obj);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public void onEditRow() {

        Stat obj = null;
        try {
            //obj = (Stat) event.getNewValue();
            // ejbFacade.edit(selectedStat);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    public void onStatSelect(SelectEvent event) {
        Stat obj ;
        try {
            obj = (Stat) event.getObject();
            ejbFacade.edit(obj);
        } catch (Exception ex) {
            JsfUtil.addErrorMessage(ex.getMessage());
            FacesContext.getCurrentInstance().validationFailed();
        }
    }

    /**
     * @return the parent
     */
    public StatsTaken getParent() {
        return parent;
    }

    /**
     * @param parent the parent to set
     */
    public void setParent(StatsTaken parent) {
        this.parent = parent;
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
     * @return the SelectedStat
     */
    public Stat getSelectedStat() {
        return selectedStat;
    }

    /**
     * @param newStat
     * 
     */
    public void setSelectedStat(Stat newStat) {
        if (selectedStat.getId() != null) {
            try {
                //obj = (Stat) event.getNewValue();
                ejbFacade.edit(selectedStat);
            } catch (Exception ex) {
                JsfUtil.addErrorMessage(ex.getMessage());
                FacesContext.getCurrentInstance().validationFailed();
            }
        }
        this.selectedStat = newStat;
    }

    /**
     * @return the selectedCell
     */
    // public Cell getSelectedCell() {
    //       return selectedCell;
    //   }
    /**
     */
    //   public void setSelected(Stat selected) {
//        this.current = selected;
    //   }
    @FacesConverter(value="statControllerConverter")
    public static class StatControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            StatController controller = (StatController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "statController");
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
            if (object instanceof Stat) {
                Stat o = (Stat) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + StatController.class.getName());
            }
        }
    }
}
