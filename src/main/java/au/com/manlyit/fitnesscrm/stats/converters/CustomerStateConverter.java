/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.converters;

import au.com.manlyit.fitnesscrm.stats.beans.CustomerStateFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;
import javax.inject.Named;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 *
 * @author david
 */
@Named(value = "customerStateConverter")
//@SessionScoped 
//@FacesConverter(value = "customerStateConverter")
public class CustomerStateConverter extends GenericConverter<CustomerState> implements   Serializable {
    private static final long serialVersionUID = 1L;
    
public CustomerStateConverter() {
    }
   /* private static final long serialVersionUID = 1L;
    private static final Logger logger = Logger.getLogger(CustomerStateConverter.class.getName());
    @Inject
    private CustomerStateFacade ejbFacade;

    public CustomerStateConverter() {
    }

    @Override
    public Object getAsObject(FacesContext context, UIComponent component, String value) {
        CustomerState sess = null;
        try {
            Integer val = Integer.parseInt(value);
            if (ejbFacade != null) {
                sess = ejbFacade.find(val);
            } else {
                logger.log(Level.WARNING, "Injection of ejbFacade FAILED");
            }

        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "Number Format Exception", value);
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
        if (object instanceof CustomerState) {
            CustomerState o = (CustomerState) object;
            return getStringKey(o.getId());
        } else {
            throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + CustomerState.class.getName());
        }
    }*/
}
