/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.validator.Validator;
import javax.faces.validator.ValidatorException;

/**
 *
 * @author dturner
 */
public class NoSpecialCharStringValidator implements Validator{

    public NoSpecialCharStringValidator(){

    }

    @Override
     public void validate(FacesContext facesContext,
            UIComponent uIComponent, Object object) throws ValidatorException {

        String enteredText = (String)object;
        //Set the email pattern string
        Pattern p = Pattern.compile("[a-zA-Z0-9-@_+=,:;!/()%$#*.\n\r\t ]+");
        //Pattern p = Pattern.compile("[a-zA-Z0-9-@_+=,:;!/()%$#* ]+.\n\r\t");
        //Match the given string with the pattern
        Matcher m = p.matcher(enteredText);

        //Check whether match is found
        boolean matchFound = m.matches();

        if (!matchFound) {
            FacesMessage message = new FacesMessage();
            message.setDetail("Sorry, the entered text contains characters that are not allowed. You can use letters, numbers and -@_+=,:;!/()%$#* only.");
            message.setSummary("Sorry, the entered text contains characters that are not allowed. You can use letters, numbers and -@_+=,:;!/()%$#* only.");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }
    }


}
