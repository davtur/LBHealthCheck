/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.logging.Level;
import java.util.logging.Logger;
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
    private static final Logger LOGGER = Logger.getLogger(PhoneNumberValidator.class.getName());
    public PhoneNumberValidator() {

    }

    @Override
    public void validate(FacesContext facesContext,
            UIComponent uIComponent, Object object) throws ValidatorException {
        boolean numberIsValid = false;
        String phoneNumber = (String) object;
        //remove white spaces
        phoneNumber = phoneNumber.replaceAll("[\\s.]", "");
        Pattern p;
        Matcher m;
        if (phoneNumber.startsWith("0")) {
            //String regex = "^0(?:[0-9] ?){6,14}[0-9]$";
            String regex = "\\d{10}";
            //p = Pattern.compile("\\d{10}");
            p = Pattern.compile(regex);
            m = p.matcher(phoneNumber);
            boolean matchFound = m.matches();
            if (matchFound && phoneNumber.charAt(1) != '0') {
                numberIsValid = true;
                LOGGER.log(Level.INFO, "Phone number validated successfully: {0}",new Object[]{phoneNumber});
            } 
        } else {
            String regex = "^\\+?(?:[0-9] ?){6,14}[0-9]$";
            /* international numbers with industry-standard notation specified by ITU-T E.123.           
  ^ # Assert position at the beginning of the string.
 \+ # Match a literal "+" character.
 (?: # Group but don't capture:
 [0-9] # Match a digit.
 \\s # Match a space character
 ? # between zero and one time.
 ) # End the noncapturing group.
 {6,14} # Repeat the group between 6 and 14 times.
 [0-9] # Match a digit.
 $ # Assert position at the end of the string.
    
             */

            p = Pattern.compile(regex);
            m = p.matcher(phoneNumber);
            boolean matchFound = m.matches();
            if (matchFound ) {
                numberIsValid = true;
                LOGGER.log(Level.INFO, "Phone number validated successfully: {0}",new Object[]{phoneNumber});
            }
        }
        //Match the given string with the pattern

        //Check whether match is found
        if (numberIsValid == false) {
            LOGGER.log(Level.INFO, "Phone number validation failed: {0}",new Object[]{phoneNumber});
            FacesMessage message = new FacesMessage();
            message.setDetail("Validation Error");
            message.setSummary("The Phone number is not valid");
            message.setSeverity(FacesMessage.SEVERITY_ERROR);
            throw new ValidatorException(message);
        }

    }

}
