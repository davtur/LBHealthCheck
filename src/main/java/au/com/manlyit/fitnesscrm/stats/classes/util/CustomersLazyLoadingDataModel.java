/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

/**
 *
 * @author dturner
 */
import au.com.manlyit.fitnesscrm.stats.beans.AbstractFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Dummy implementation of LazyDataModel that uses a list to mimic a real
 * datasource like a database.
 *
 * @param <T>
 */
public class CustomersLazyLoadingDataModel<T extends BaseEntity> extends LazyDataModel<T> {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = Logger.getLogger(CustomersLazyLoadingDataModel.class.getName());
    private List<CustomerState> selectedCustomerStates;
    private List<String> selectedCustomerTypes;

    private volatile AbstractFacade<T> facade;

    public CustomersLazyLoadingDataModel(AbstractFacade<T> facade) {
        super();
        this.facade = facade;
        this.setRowCount(facade.count());
    }

    public final AbstractFacade<T> getFacade() {
        return facade;
    }

    public final void setFacade(AbstractFacade<T> facade) {
        this.facade = facade;
    }

    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<T> list = null;

        list = facade.loadCustomers(getSelectedCustomerStates(), getSelectedCustomerTypes(), first, pageSize, sortField, sortOrder, filters);
        Long totalRows = facade.loadedCustomersTotalRowCount(getSelectedCustomerStates(), getSelectedCustomerTypes(), first, pageSize, sortField, sortOrder, filters);
        this.setRowCount(totalRows.intValue());

        if (list == null) {
            throw new RuntimeException("Failed to load customers. Returned List is null.");
        }

        // I am using the following line for debugging:
        // throw new RuntimeException(list.toString());
        LOGGER.log(Level.INFO, "Lazy Load returned a list of size:{0}", list.size());
        return list;
    }

    @Override
    public T getRowData(String rowKey) {

        Integer id = null;
        try {
            id = Integer.parseInt(rowKey);
        } catch (NumberFormatException numberFormatException) {
            LOGGER.log(Level.WARNING, "Could not convert rowKey to Integer. The PK should be an integer. rowKey:", rowKey);
        }

        return facade.find(id);
    }

    @Override
    public Object getRowKey(T entity) {
        return entity.getId();
    }

    /**
     * @return the selectedCustomerStates
     */
    public List<CustomerState> getSelectedCustomerStates() {
        return selectedCustomerStates;
    }

    /**
     * @param selectedCustomerStates the selectedCustomerStates to set
     */
    public void setSelectedCustomerStates(List<CustomerState> selectedCustomerStates) {
        this.selectedCustomerStates = selectedCustomerStates;
    }

    /**
     * @return the selectedCustomerTypes
     */
    public List<String> getSelectedCustomerTypes() {
        return selectedCustomerTypes;
    }

    /**
     * @param selectedCustomerTypes the selectedCustomerTypes to set
     */
    public void setSelectedCustomerTypes(List<String> selectedCustomerTypes) {
        this.selectedCustomerTypes = selectedCustomerTypes;
    }

}
