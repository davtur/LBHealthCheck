/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
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
@Named ("custConverter")
@SessionScoped
@FacesConverter(value = "customerConverter")
public class CustomerConverter implements Converter, Serializable{

   @Inject
  private  au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;

   public CustomerConverter(){
   }

    @Override
   public Object getAsObject(FacesContext context, UIComponent component, String value){
        Customers cust;
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
     return ((Customers) value).getId().toString(); //--> convert to a unique string.
   }
}
