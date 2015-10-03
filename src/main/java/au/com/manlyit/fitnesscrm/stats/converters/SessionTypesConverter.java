/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.converters;

import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.io.Serializable;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named ("sessionTypesConverter")

public class SessionTypesConverter extends GenericConverter<SessionTypes> implements Serializable {
    
    private static final long serialVersionUID = 1L;

   

   public SessionTypesConverter(){
   }

   
}
