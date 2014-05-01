/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
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
@Named ("sessHistoryConverter")
@SessionScoped
@FacesConverter(value = "sessionHistoryConverter")
public class SessionHistoryConverter implements Converter, Serializable{

   @Inject
  private  au.com.manlyit.fitnesscrm.stats.beans.SessionHistoryFacade ejbFacade;

   public SessionHistoryConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
        SessionHistory cust;
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
     return ((SessionHistory) value).getId().toString(); //--> convert to a unique string.
   }
}
