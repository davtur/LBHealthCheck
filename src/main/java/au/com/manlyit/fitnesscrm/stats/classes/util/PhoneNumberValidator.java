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
public class PhoneNumberValidator implements Validator {

    public PhoneNumberValidator() {

    }

    @Override
    public void validate(FacesContext facesContext,
            UIComponent uIComponent, Object object) throws ValidatorException {
        boolean numberIsValid = false;
        String phoneNumber = (String) object;
        phoneNumber = phoneNumber.replaceAll("[^\\d.]", "");
        Pattern p;
        Matcher m;
        if (phoneNumber.startsWith("0")) {
            //Set the email pattern string
            p = Pattern.compile("\\d{10}");
            m = p.matcher(phoneNumber);
            boolean matchFound = m.matches();
             if (matchFound && phoneNumber.charAt(1) != '0') {
                numberIsValid = true;
            }
        } else {
            p = Pattern.compile("\\d{11}");
            m = p.matcher(phoneNumber);
            boolean matchFound = m.matches();
            if (matchFound && phoneNumber.startsWith("61")) {
                numberIsValid = true;
            }
        }
        //Match the given string with the pattern

        //Check whether match is found
        if (numberIsValid == false) {
            FacesMessage message = new FacesMessage();
            message.setDetail("Validation Error");
            message.setSummary("The Phone number is not valid");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }

    }

}
