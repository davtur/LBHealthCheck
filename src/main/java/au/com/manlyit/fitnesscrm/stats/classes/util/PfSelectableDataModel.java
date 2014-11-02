/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.util.List;
import java.util.Objects;
import javax.faces.model.ListDataModel;
import org.primefaces.model.SelectableDataModel;

/**
 *
 * @author david
 * @param <T>
 */
public class PfSelectableDataModel<T extends BaseEntity> extends ListDataModel<T> implements SelectableDataModel<T>, Serializable {

    public PfSelectableDataModel() {
    }

    public PfSelectableDataModel(List<T> data) {
        super(data);
    }

    @Override
    public T getRowData(String rowKey) {
        List<T> list = (List<T>) getWrappedData();

        for (T ejb : list) {
            if (Objects.equals(ejb.getId(), new Integer(rowKey))) {
                return ejb;
            }
        }
        return null;
    }

    @Override
    public Object getRowKey(T item) {
        return item.getId();
    }
    @Override
    public int getRowCount(){
        return super.getRowCount();
        
    }
}

