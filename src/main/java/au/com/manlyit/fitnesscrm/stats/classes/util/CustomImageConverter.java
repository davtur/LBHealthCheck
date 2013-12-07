/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;

/**
 *
 * @author david
 */
public class CustomImageConverter implements Converter {
/**
     * This method used for getting the input and process and convert.
     */
    @Override
    public Object getAsObject(FacesContext arg0, UIComponent arg1, String value) {
        System.out.println("Input from the user : "+value);
        System.out.println("Use custom convert logic and process the input");
        return "converted Value";
    }

    /**
     * This method used for return the converted value to the user.
     */
    @Override
    public String getAsString(FacesContext arg0, UIComponent arg1, Object value) {
        System.out.println("Value to be displayed to the user"+value);
        String output = (String)value;
        return output;
    }
}
