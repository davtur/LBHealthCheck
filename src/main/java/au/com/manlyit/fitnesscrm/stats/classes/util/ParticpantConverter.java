/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Participants;
import javax.ejb.EJB;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;

/**
 *
 * @author david
 */
@FacesConverter(value = "particpantConverter")
public class ParticpantConverter implements Converter{

   @EJB
  private  au.com.manlyit.fitnesscrm.stats.beans.ParticipantsFacade ejbFacade;

   public ParticpantConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
     return ejbFacade.find(value);
     //If u look below, I convert the object into a unique string, which is its id.
     //Therefore, I just need to write a method that query the object back from the
     //database if given a id. getProjectById, is a method inside my Session Bean that
     //does what I just described
   }

    @Override
   public String getAsString(FacesContext context, UIComponent component, Object value)
   {
     return ((Participants) value).getId().toString(); //--> convert to a unique string.
   }
}
