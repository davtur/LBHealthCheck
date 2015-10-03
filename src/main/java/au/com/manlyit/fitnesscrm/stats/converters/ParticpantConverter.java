/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.converters;

import au.com.manlyit.fitnesscrm.stats.db.Participants;
import java.io.Serializable;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named("particpantConverter")

public class ParticpantConverter extends GenericConverter<Participants> implements Serializable {

    private static final long serialVersionUID = 1L;

    public ParticpantConverter() {
    }
}

