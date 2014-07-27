/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Tasks;
import java.io.Serializable;

import javax.inject.Inject;
import javax.enterprise.context.SessionScoped;
import javax.inject.Named;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author david
 */
@Named ("tasksConverter")
@SessionScoped
@FacesConverter(value = "tasksConverter")
public class TaskConverter implements Converter, Serializable{

   @Inject
  private  au.com.manlyit.fitnesscrm.stats.beans.TasksFacade ejbFacade;

   public TaskConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
        Tasks cust;
        int val = Integer.parseInt(value);
        cust = ejbFacade.find(val);
        return cust;



     //If u look below, I convert the object into a unique string, which is its id.
     //Therefore, I just need to write a method that query the object back from the
     //database if given a id. getProjectById, is a method inside my Session Bean that
     //does what I just described
   }

    @Override
   public String getAsString(FacesContext context, UIComponent component, Object value)
   {
     return ((Tasks) value).getIdtasks().toString(); //--> convert to a unique string.
   }
}
