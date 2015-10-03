/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.converters;

import au.com.manlyit.fitnesscrm.stats.converters.GenericConverter;
import au.com.manlyit.fitnesscrm.stats.db.Plan;
import java.io.Serializable;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named("planConverter")
public class PlanConverter extends GenericConverter<Plan> implements Serializable {

    private static final long serialVersionUID = 1L;

    public PlanConverter() {
    }
}
