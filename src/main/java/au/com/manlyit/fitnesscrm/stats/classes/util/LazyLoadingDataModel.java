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
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

/**
 * Dummy implementation of LazyDataModel that uses a list to mimic a real datasource like a database.
 * @param <T>
 */
public class LazyLoadingDataModel<T extends BaseEntity>  extends LazyDataModel<T> {
    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(LazyLoadingDataModel.class.getName());
    
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
    public List<T> load(int first, int pageSize, String sortField,SortOrder sortOrder, Map<String,Object> filters) {
        List<T> list = facade.load(first, pageSize,
            sortField, sortOrder, filters);
        if (list == null) {
            throw new RuntimeException("Problem.");
        }
        // I am using the following line for debugging:
        // throw new RuntimeException(list.toString());
        return list;
    }

    
    @Override
    public T getRowData(String rowKey) {
        
        Integer id = null;
        try {
            id = Integer.parseInt(rowKey);
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "Could not convert rowKey to Integer. The PK should be an integer. rowKey:",rowKey);
        }
        
        return facade.find(id);
    }

    @Override
    public Object getRowKey(T entity) {
        return entity.getId();
    }

    
}
   