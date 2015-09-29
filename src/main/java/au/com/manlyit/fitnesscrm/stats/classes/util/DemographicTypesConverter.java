/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.DemographicTypes;
import java.io.Serializable;

import javax.inject.Named;

/**
 *
 * @author david
 */
@Named("demographicTypesConverter")

public class DemographicTypesConverter extends GenericConverter<DemographicTypes> implements Serializable {

    private static final long serialVersionUID = 1L;

    public DemographicTypesConverter() {
    }

}
