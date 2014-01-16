/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.DemographicTypes;
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
@Named ("demographicTypesConverter")
@SessionScoped
@FacesConverter(value = "demographicTypesConverter")
public class DemographicTypesConverter implements Converter, Serializable{

   @Inject
  private  au.com.manlyit.fitnesscrm.stats.beans.DemographicTypesFacade ejbFacade;

   public DemographicTypesConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
        DemographicTypes sess;
        int val = Integer.parseInt(value);
        sess = ejbFacade.find(val);
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
            if (object instanceof DemographicTypes) {
                DemographicTypes o = (DemographicTypes) object;
                return getStringKey(o.getId());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + DemographicTypes.class.getName());
            }
        }
}
