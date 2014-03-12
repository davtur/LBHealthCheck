/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.Comparator;
import org.primefaces.model.SortOrder;

/**
 *
 * @author dturner
 * @param <T>
 */

public class LazySorter<T> implements Comparator<T> {

    private String sortField;
    
    private SortOrder sortOrder;
    private Class typeClass;
    
    public LazySorter(String sortField, SortOrder sortOrder) {
        this.sortField = sortField;
        this.sortOrder = sortOrder;
        this.typeClass = this.getClass();
    }

    @Override
    public int compare(T item1, T item2) {
        try {
            
            Object value1 = this.typeClass.getField(this.sortField).get(item1);
            Object value2 = this.typeClass.getField(this.sortField).get(item2);

            int value = ((Comparable)value1).compareTo(value2);
            
            return SortOrder.ASCENDING.equals(sortOrder) ? value : -1 * value;
        }
        catch(IllegalAccessException | IllegalArgumentException | NoSuchFieldException | SecurityException e) {
            throw new RuntimeException();
        }
    }
}