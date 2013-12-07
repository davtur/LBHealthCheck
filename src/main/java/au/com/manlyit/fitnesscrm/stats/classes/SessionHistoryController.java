package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Participants;
import java.awt.event.ActionEvent;

import java.io.Serializable;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.primefaces.model.DualListModel;

@ManagedBean(name = "sessionHistoryController")
@SessionScoped
public class SessionHistoryController implements Serializable {

    private SessionHistory current;
    private SessionHistory selectedForDeletion;

    private DataModel items = null;
    private DataModel customerItems = null;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @EJB
    private au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbParticipantsFacade;
    @EJB
    private ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;
    private DualListModel<Customers> participants;

    public SessionHistoryController() {
    }

    public void handleDateSelect(SelectEvent event) {
        SimpleDateFormat format = new SimpleDateFormat("dd MMM yyyy");
        Date date = (Date) event.getObject();
        FacesContext facesContext = FacesContext.getCurrentInstance();
        facesContext.addMessage(null, new FacesMessage(FacesMessage.SEVERITY_INFO, "Date Selected", format.format(date)));
    }

    public SessionHistory getSelected() {
        if (current == null) {
            current = new SessionHistory();
            current.setSessiondate(new Date());
            selectedItemIndex = -1;
        }
        return current;
    }

    public void setSelected(SessionHistory selected) {
        this.current = selected;
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    private SessionHistoryFacade getFacade() {
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

    public PaginationHelper getCustomerPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    Customers loggedInUser = null;
                    try {
                        //loggedInUser = ejbCustomerFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
                        FacesContext context = FacesContext.getCurrentInstance();
                        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                        loggedInUser = custController.getSelected();
                        return ejbFacade.countByCustId(loggedInUser.getId());
                    } catch (Exception e) {
                        JsfUtil.addErrorMessage(e, "Couldn't get customer i Session History controller");
                    }

                    return 0;
                }

                @Override
                public DataModel createPageDataModel() {
                    Customers loggedInUser = null;
                    try {
                        //loggedInUser = ejbCustomerFacade.findCustomerByUsername(FacesContext.getCurrentInstance().getExternalContext().getRemoteUser());
                        FacesContext context = FacesContext.getCurrentInstance();
                        CustomersController custController = (CustomersController) context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
                        loggedInUser = custController.getSelected();
                        String msg = "CreatingDataModel for user: " + loggedInUser.getUsername();
                        Logger.getLogger(getClass().getName()).log(Level.INFO, msg);
                        return new ListDataModel(ejbFacade.findAllByCustId(loggedInUser.getId(), true));
                    } catch (Exception e) {
                        JsfUtil.addErrorMessage(e, "Couldn't get customer i Session History controller");
                    }

                    return null;
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
       // current = (SessionHistory) getItems().getRowData();
        // selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new SessionHistory();
        current.setSessiondate(new Date());
        selectedItemIndex = -1;
        addParticipants();
        return "Create";
    }

    public String create() {
        try {
            current.setId(0); // auto generated by DB , but cannot be null
            updateCurrentParticipants();
            getFacade().create(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    private void updateCurrentParticipants() {
        List<Customers> targets = participants.getTarget();
        List<Participants> parts = new ArrayList<Participants>();

        for (Customers cust : targets) {
            Participants p = new Participants(0);
            p.setCustomers(cust);
            p.setSessionHistory(current);
            parts.add(p);
        }
        //current.getParticipantsCollection().clear();
        current.setParticipantsCollection(parts);

    }

    private void populatePickerFromCurrentSessionHistory() {
        List<Customers> allCustomers = ejbCustomerFacade.findAll(); // all customers
        List<Customers> selectedCustomers = new ArrayList<Customers>();
        Collection<Participants> partic = current.getParticipantsCollection(); //ones already selected.
        if (partic != null) {
            for (Participants part : partic) {
                selectedCustomers.add(part.getCustomers());
            }
            //remove any of the selected customers from the all customers list
            for (int i = allCustomers.size() - 1; i >= 0; i--) {
                Customers c1 = allCustomers.get(i);
                for (Customers c2 : selectedCustomers) {
                    if (c1.getId() == c2.getId()) {
                        allCustomers.remove(c1);
                    }
                }
            }
            participants = new DualListModel<Customers>(allCustomers, selectedCustomers);
        } else {
            addParticipants();
        }
    }

    private void addParticipants() {
        List<Customers> sourceParticipants = ejbCustomerFacade.findAll(true);
        List<Customers> targetParticipants = new ArrayList<Customers>();
        participants = new DualListModel<Customers>(sourceParticipants, targetParticipants);
    }

    public String prepareEdit() {
        //current = (SessionHistory) getItems().getRowData();
        //selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        populatePickerFromCurrentSessionHistory();
        return "Edit";
    }

    public String update() {
        try {
            updateCurrentParticipants();
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryUpdated"));
            return "List";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (SessionHistory) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public void destroyListener(ActionEvent event) {
        Object o = event.getSource();
        if (o.getClass() == SessionHistory.class) {
            current = (SessionHistory) o;

            performDestroy();
            recreateModel();
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

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("SessionHistoryDeleted"));
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

    public DataModel getCustomerItems() {
        if (customerItems == null) {
            customerItems = getCustomerPagination().createPageDataModel();
        }
        return customerItems;
    }

    public void recreateModel() {
        items = null;
        customerItems = null;
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
     * @return the participants
     */
    public DualListModel<Customers> getParticipants() {
        if (participants == null) {
            addParticipants();
        }
        return participants;
    }

    /**
     * @param participants the participants to set
     */
    public void setParticipants(DualListModel<Customers> participants) {
        this.participants = participants;
    }

    /**
     * @return the selectedForDeletion
     */
    public SessionHistory getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(SessionHistory selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();

    }

    @FacesConverter(forClass = SessionHistory.class)
    public static class SessionHistoryControllerConverter implements Converter {

        @Override
        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            SessionHistoryController controller = (SessionHistoryController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "sessionHistoryController");
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
            if (object instanceof SessionHistory) {
                SessionHistory o = (SessionHistory) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SessionHistoryController.class.getName());
            }
        }
    }
}
