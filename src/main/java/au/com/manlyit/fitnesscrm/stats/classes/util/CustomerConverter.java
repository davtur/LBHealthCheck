/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named(value="custConverter")
public class CustomerConverter extends GenericConverter<Customers> implements Serializable {
    private static final long serialVersionUID = 1L;

    public CustomerConverter() {
    }

}
