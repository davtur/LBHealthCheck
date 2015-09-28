/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.AbstractFacade;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.inject.Inject;

/**
 *
 * @author david
 * @param <T>
 */
public abstract class GenericConverter<T> implements Converter, Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(GenericConverter.class.getName());
    @Inject
    private AbstractFacade<T> ejbFacade;

    public GenericConverter() {
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        Object o = null;
        try {
            Integer val = Integer.parseInt(value);
            if (ejbFacade != null) {
                o = ejbFacade.find(val);
            } else {
                logger.log(Level.WARNING, "Injection of ejbFacade FAILED");
            }

        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "Number Format Exception", value);
        }
        return o;

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
      
   

        if (object instanceof BaseEntity) {
            BaseEntity o = (BaseEntity) object;
            return getStringKey(o.getId());

        } else {
              
            throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() );
        }
    }
}
