package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SuppliersFacade;
import au.com.manlyit.fitnesscrm.stats.classes.util.ContractorRateToDualListModelMap;
import au.com.manlyit.fitnesscrm.stats.db.ContractorRateToTaskMap;
import au.com.manlyit.fitnesscrm.stats.db.ContractorRates;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
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
import org.primefaces.event.DragDropEvent;
import org.primefaces.event.RowEditEvent;
import org.primefaces.event.SelectEvent;
import org.primefaces.event.TransferEvent;
import org.primefaces.model.DualListModel;

@Named("suppliersController")
@SessionScoped
public class SuppliersController implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(SuppliersController.class.getName());
    private Suppliers current;
    private Suppliers selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.SuppliersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ContractorRateToTaskMapFacade contractorRateToTaskMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ContractorRatesFacade contractorRatesFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private List<Suppliers> filteredItems;
    private Suppliers[] multiSelected;
    private SessionTypes selectedSessionTypeDrop;
    private ContractorRates selectedContractorRate;
    private List<SessionTypes> sessionTypesArray;
    private DualListModel<SessionTypes> rateItems;
    private List<ContractorRateToDualListModelMap> rateItemsList;

    public SuppliersController() {
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;
    }

    public Suppliers getSelected() {
        if (current == null) {
            current = new Suppliers();
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(Suppliers selected) {
        if (selected != null) {
            current = selected;
            selectedItemIndex = -1;
        }

    }

    private SuppliersFacade getFacade() {
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
    public List<Suppliers> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<Suppliers> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the multiSelected
     */
    public Suppliers[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(Suppliers[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public String prepareView() {
        //current = (Suppliers)getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new Suppliers();
        selectedItemIndex = -1;
        return "Create";
    }

    public void onTransfer(TransferEvent event) {
        boolean add = event.isAdd();
        if (add) {
            for (Object item : event.getItems()) {
                SessionTypes st = (SessionTypes) item;
                ContractorRateToTaskMap crtm = new ContractorRateToTaskMap(0, st, getSelectedContractorRate(),getSelected());
                contractorRateToTaskMapFacade.create(crtm);
                //st.getContractorRateToTaskMapCollection().add(crtm);

                getSessionTypesArray().remove(st);
                //rateItems.getTarget().add(st);

            }
        } else {
            for (Object item : event.getItems()) {
                SessionTypes st = (SessionTypes) item;
                Collection<ContractorRateToTaskMap> stl = st.getContractorRateToTaskMapCollection();
                ContractorRateToTaskMap mapToRemove = contractorRateToTaskMapFacade.findBySessionTypeAndContractorRate(st, getSelectedContractorRate(),getSelected());
                if (mapToRemove != null) {
                    stl.remove(mapToRemove);
                    st.setContractorRateToTaskMapCollection(stl);
                }

                getSessionTypesArray().add(st);
                //rateItems.getTarget().remove(st);

            }
        }

    }

    public void addSessionType(ActionEvent actionEvent) {

    }

    public void contractorRatesChangeListener(ValueChangeEvent vce) {
        Object o = vce.getNewValue();
        rateItems = null;
    }

    /**
     * @return the rateItems
     */
    public DualListModel<SessionTypes> getRateItems() {
        if (rateItems == null) {
            if (getSelectedContractorRate() == null) {
                List<SessionTypes> source = getSessionTypesArray();
                List<SessionTypes> destination = new ArrayList<>();
                rateItems = new DualListModel<>(source, destination);
                LOGGER.log(Level.WARNING, "getRateItems - the call to getSelectedContractorRate() returned a NULL value");
            } else {

                List<SessionTypes> source = getSessionTypesArray();
                List<SessionTypes> removeFromSource = contractorRateToTaskMapFacade.findBySessionTypesByContractorRateAndSupplier(getSelected());
                for (SessionTypes stRem : removeFromSource) {
                    source.remove(stRem);
                }
                List<SessionTypes> destination = contractorRateToTaskMapFacade.findBySessionTypesByContractorRate(getSelectedContractorRate(),getSelected());
                if (destination == null) {
                    destination = new ArrayList<>();
                    LOGGER.log(Level.WARNING, "getRateItems - the call to contractorRateToTaskMapFacade.findBySessionTypesByContractorRate(getSelectedContractorRate()) returned a NULL value");
                }
                rateItems = new DualListModel<>(source, destination);
            }
        }
        return rateItems;
    }

    /**
     * @param rateItems the rateItems to set
     */
    public void setRateItems(DualListModel<SessionTypes> rateItems) {
        this.rateItems = rateItems;
    }

  /*  public void onSessionTypeDrop(DragDropEvent ddEvent) {
        SessionTypes st = ((SessionTypes) ddEvent.getData());

        FacesContext context = FacesContext.getCurrentInstance();
        try {
            int contractorRateId = Integer.parseInt(context.getExternalContext().getRequestParameterMap().get("contractorRate"));
            ContractorRates cr = contractorRatesFacade.find(contractorRateId);
            //ContractorRates cr = getSelectedContractorRate();
            Suppliers sup = getSelected();
            getSessionTypesArray().remove(st);
            ContractorRateToTaskMap crtm = new ContractorRateToTaskMap(0);
            crtm.setContractorRateId(cr);
            crtm.setTaskId(st);
            contractorRateToTaskMapFacade.create(crtm);

        } catch (NumberFormatException numberFormatException) {
            LOGGER.log(Level.SEVERE, "onSessionTypeDrop Method, contractorRateId is NULL or not a number!");
        }

    }*/

    public String create() {
        try {
            if (current.getId() == null) {
                current.setId(0);
            }

            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SuppliersCreated"));
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
            current = new Suppliers();
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SuppliersCreated"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

    }

    public String prepareEdit() {
        //current = (Suppliers)getItems().getRowData();
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
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SuppliersUpdated"));
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

    public Suppliers getSelectedForDeletion() {
        return selectedForDeletion;
    }

    public void setSelectedForDeletion(Suppliers selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SuppliersDeleted"));
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

        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    public Collection<Suppliers> getItemsAvailable() {
        return ejbFacade.findAll();
    }

    public void onEdit(RowEditEvent event) {
        Suppliers cm = (Suppliers) event.getObject();
        getFacade().edit(cm);
        recreateModel();
        JsfUtil.addSuccessMessage("Row Edit Successful");
    }

    public void onCancel(RowEditEvent event) {
        JsfUtil.addErrorMessage("Row Edit Cancelled");
    }

    /**
     * @return the selectedSessionTypeDrop
     */
    public SessionTypes getSelectedSessionTypeDrop() {
        return selectedSessionTypeDrop;
    }

    /**
     * @param selectedSessionTypeDrop the selectedSessionTypeDrop to set
     */
    public void setSelectedSessionTypeDrop(SessionTypes selectedSessionTypeDrop) {
        this.selectedSessionTypeDrop = selectedSessionTypeDrop;
    }

    /**
     * @return the selectedContractorRate
     */
    public ContractorRates getSelectedContractorRate() {
        return selectedContractorRate;
    }

    /**
     * @param selectedContractorRate the selectedContractorRate to set
     */
    public void setSelectedContractorRate(ContractorRates selectedContractorRate) {
        this.selectedContractorRate = selectedContractorRate;
    }

    /**
     * @return the sessionTypesArray
     */
    public List<SessionTypes> getSessionTypesArray() {
        if (sessionTypesArray == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            SessionTypesController controller = (SessionTypesController) context.getApplication().getELResolver().getValue(context.getELContext(), null, "sessionTypesController");
            //sessionTypesArray = new SessionTypes[controller.getItemsAvailable().size()];
            //controller.getItemsAvailable().toArray(sessionTypesArray);
            sessionTypesArray = controller.getAllSessionTypes();
        }
        return sessionTypesArray;
    }

    /**
     * @param sessionTypesArray the sessionTypesArray to set
     */
    public void setSessionTypesArray(List<SessionTypes> sessionTypesArray) {
        this.sessionTypesArray = sessionTypesArray;
    }

    /**
     * @return the rateItemsList
     */
    public List<ContractorRateToDualListModelMap> getRateItemsList() {
        return rateItemsList;
    }

    /**
     * @param rateItemsList the rateItemsList to set
     */
    public void setRateItemsList(List<ContractorRateToDualListModelMap> rateItemsList) {
        this.rateItemsList = rateItemsList;
    }

    @FacesConverter(value = "suppliersControllerConverter")
    public static class SuppliersControllerConverter implements Converter {

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SuppliersController controller = (SuppliersController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "suppliersController");
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
            if (object instanceof Suppliers) {
                Suppliers o = (Suppliers) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SuppliersController.class.getName());
            }
        }

    }

}
//file:///home/david/.netbeans/8.0/config/Templates/JSF/JSF_From_Entity_Wizard/StandardJSF/create.ftl
