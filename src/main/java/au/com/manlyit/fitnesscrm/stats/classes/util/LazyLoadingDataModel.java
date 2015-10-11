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
public class LazyLoadingDataModel<T extends BaseEntity> extends LazyDataModel<T> {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(LazyLoadingDataModel.class.getName());
    private Date fromDate;
    private Date toDate;
    private boolean useDateRange = false;
    private String dateRangeEntityFieldName = "";

    private volatile AbstractFacade<T> facade;

    public LazyLoadingDataModel(AbstractFacade<T> facade) {
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

    @Override
    public List<T> load(int first, int pageSize, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<T> list = null;
        if (useDateRange == false) {
            list = facade.load(first, pageSize, sortField, sortOrder, filters);
        } else {
            list = facade.loadDateRange(first, pageSize, sortField, sortOrder, filters, getFromDate(), getToDate(), getDateRangeEntityFieldName());
        }
        if (list == null) {
            throw new RuntimeException("Problem.");
        }
        // I am using the following line for debugging:
        // throw new RuntimeException(list.toString());
        logger.log(Level.INFO, "Lazy Load return a list of size:{0}", list.size());
        return list;
    }

    @Override
    public T getRowData(String rowKey) {

        Integer id = null;
        try {
            id = Integer.parseInt(rowKey);
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "Could not convert rowKey to Integer. The PK should be an integer. rowKey:", rowKey);
        }

        return facade.find(id);
    }

    @Override
    public Object getRowKey(T entity) {
        return entity.getId();
    }

    /**
     * @return the fromDate
     */
    public Date getFromDate() {
        return fromDate;
    }

    /**
     * @param fromDate the fromDate to set
     */
    public void setFromDate(Date fromDate) {
        this.fromDate = fromDate;
    }

    /**
     * @return the toDate
     */
    public Date getToDate() {
        return toDate;
    }

    /**
     * @param toDate the toDate to set
     */
    public void setToDate(Date toDate) {
        this.toDate = toDate;
    }

    /**
     * @return the useDateRange
     */
    public boolean isUseDateRange() {
        return useDateRange;
    }

    /**
     * @param useDateRange the useDateRange to set
     */
    public void setUseDateRange(boolean useDateRange) {
        this.useDateRange = useDateRange;
    }

    /**
     * @return the dateRangeEntityFieldName
     */
    public String getDateRangeEntityFieldName() {
        return dateRangeEntityFieldName;
    }

    /**
     * @param dateRangeEntityFieldName the dateRangeEntityFieldName to set
     */
    public void setDateRangeEntityFieldName(String dateRangeEntityFieldName) {
        this.dateRangeEntityFieldName = dateRangeEntityFieldName;
    }

}
