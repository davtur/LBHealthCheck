/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

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
@Named ("sessTypesConverter")
@SessionScoped
@FacesConverter(value = "sessionTypesConverter")
public class SessionTypesConverter implements Converter, Serializable{
    private static final Logger logger = Logger.getLogger(SessionTypesConverter.class.getName());

   @Inject
  private  au.com.manlyit.fitnesscrm.stats.beans.SessionTypesFacade ejbFacade;

   public SessionTypesConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
        SessionTypes sess = null;
       
        try {
            int val = Integer.parseInt(value);
            sess = ejbFacade.find(val);
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.INFO, "The passed value ({0}) could not be converted to a sessionType primary key",value);
        }
        return sess;



     //If u look below, I convert the object into a unique string, which is its id.
     //Therefore, I just need to write a method that query the object back from the
     //database if given a id. getProjectById, is a method inside my Session Bean that
     //does what I just described
   }

        String getStringKey(java.lang.Integer value) {
            StringBuilder sb = new StringBuilder();
            sb.append(value);
            return sb.toString();
        } 

   @Override
        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof SessionTypes) {
                SessionTypes o = (SessionTypes) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + SessionTypes.class.getName());
            }
        }
}
