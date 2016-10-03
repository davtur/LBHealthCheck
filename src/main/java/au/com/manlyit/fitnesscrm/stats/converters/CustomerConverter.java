/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.converters;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author david
 */
//@Named(value="custConverter")
@FacesConverter(value = "customersConverter")
public class CustomerConverter extends GenericConverter<Customers> implements Serializable {
    private static final long serialVersionUID = 1L;

    public CustomerConverter() {
    }

}
