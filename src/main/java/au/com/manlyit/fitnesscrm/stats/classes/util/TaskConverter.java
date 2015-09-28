/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Tasks;
import java.io.Serializable;

import javax.inject.Named;

/**
 *
 * @author david
 */
@Named ("tasksConverter")

public class TaskConverter  extends GenericConverter<Tasks> implements Serializable {
    private static final long serialVersionUID = 1L;


   public TaskConverter(){
   }

}
