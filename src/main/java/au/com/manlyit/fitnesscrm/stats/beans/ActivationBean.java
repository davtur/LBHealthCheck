/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.PasswordService;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.classes.util.StringEncrypter;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Random;
import javax.inject.Named;
import javax.annotation.PostConstruct;
import javax.el.ELException;
import javax.inject.Inject;
import javax.enterprise.context.RequestScoped;
import javax.faces.annotation.ManagedProperty;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author david
 */
@Named("activationBean")
@RequestScoped
public class ActivationBean {

    @ManagedProperty(value = "#{param.key}")
    private String key;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    private boolean valid;
    private Customers current;
      private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");

    @PostConstruct
    public void init() {
        //http://localhost:26153/FitnessStats/faces/activation.xhtml?key=david.turner
        // Get User based on activation key.
     /*   if (getKey() != null) {
            String theKey = getKey();
            String keyDecrypted = encrypter.decrypt(theKey); // decrypt nonce encrypted by login bean
            String token = configMapFacade.getConfig("login.password.reset.token").trim();
            if (keyDecrypted.matches("[0-9A-Z]*")) {//"[0-9A-Z]*"  - nonce 
                try {
                    
                    
                    if (keyDecrypted.indexOf(token) == 0) {
                        // we have a token , now check if it's valid
                        String incomingNonce = keyDecrypted.substring(token.length());
                        
                        Activation act2 ;
                        try {
                            act2 = ejbActivationFacade.findToken(incomingNonce);
                            int minutesValid = Integer.parseInt(configMapFacade.getConfig("login.password.reset.minutes").trim());
                            GregorianCalendar sendTime = new GregorianCalendar();
                            GregorianCalendar now = new GregorianCalendar();
                            sendTime.setTime(act2.getActTimestamp());
                            sendTime.add(Calendar.MINUTE, minutesValid);
                            if (now.compareTo(sendTime) < 0) {
                            // it is valid log them in
                                //Authenticator 
                                HttpServletRequest req = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();
                                Customers cst = act2.getCustomer();
                                String user = cst.getUsername();
                                //Reset Password and log them in
                                String paswd = generateUniqueToken(10);
                                //String paswd = "foooobar";
                                String encryptedPass = PasswordService.getInstance().encrypt(paswd);
                                cst.setPassword(encryptedPass);
                                ejbCustomerFacade.edit(cst);
                                req.login(user, paswd);
                                //Redirect to the user details page
                                FacesContext fc = FacesContext.getCurrentInstance();
                                CustomersController custController = (CustomersController) fc.getApplication().evaluateExpressionGet(fc, "#{customersController}", CustomersController.class);
                                custController.setSelected(cst);

                                ExternalContext ec = fc.getExternalContext();

                                try {
                                    ec.redirect("myDetails.xhtml");
                                    valid = true;
                                } catch (IOException e) {
                                    JsfUtil.addErrorMessage(e, "Redirect to MyDetails failed");

                                }

                            }
                        } catch (ServletException | ELException e2) {
                            JsfUtil.addErrorMessage(e2, "Nonce does not exist. Possible hack attempt: " + getKey());
                        }
                    }

                } catch (NumberFormatException e) {
                    JsfUtil.addErrorMessage(e, "Error processing password reset token. login.password.reset.minutes in configmap may be null or non numeric. ");

                }

            } // moved to LoginBean . this is only if we have to use plain html to reset password
            else {
                theKey = getKey();
                if (theKey.matches("^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]")) {// "^[a-zA-Z][\\w\\.-]*[a-zA-Z0-9]" regex for username
                    current = ejbFacade.findCustomerByUsername(getKey());
                    //valid user that wants the password reset
                    //generate link and send
                    String uniquetoken = generateUniqueToken(10);
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
                    String timestamp = sdf.format(new Date());
                    String nonce = timestamp + uniquetoken;
                    Activation act = new Activation(0, nonce, new Date());
                    act.setCustomer(current);
                    String urlLink = configMapFacade.getConfig("PasswordResetURL");

                    urlLink += token + nonce;
                    ejbActivationFacade.create(act);
                    //send email
                    SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
                    String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Pure Fitness Manly</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.purefitnessmanly.com.au | sarah@purefitnessmanly.com.au | +61433818067</td>  </tr></table>";

                    //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
                    emailAgent.send("david@manlyit.com.au", "", "noreply@purefitnessmanly.com.au", "Password Reset", htmlText, null, true);
                    valid = true;
                } else {
                    JsfUtil.addErrorMessage("Key is non-alphanumeric. Possible hack attempt: " + getKey());
                }
            }

            // Delete activation key from database.
            // Login user.
            //if the key is a username then send an email with the link to reset the password
            //if the key is a valid nonce then log in the user rdirect to the myDetails  
        }*/

        /**
         * Creates a new instance of ActivationBean
         */
    }

    public ActivationBean() {
    }

    public static synchronized String generateUniqueToken(Integer length) {

        byte random[] = new byte[length];
        Random randomGenerator = new Random();
        StringBuilder buffer = new StringBuilder();

        randomGenerator.nextBytes(random);

        for (int j = 0; j < random.length; j++) {
            byte b1 = (byte) ((random[j] & 0xf0) >> 4);
            byte b2 = (byte) (random[j] & 0x0f);
            if (b1 < 10) {
                buffer.append((char) ('0' + b1));
            } else {
                buffer.append((char) ('A' + (b1 - 10)));
            }
            if (b2 < 10) {
                buffer.append((char) ('0' + b2));
            } else {
                buffer.append((char) ('A' + (b2 - 10)));
            }
        }

        return (buffer.toString());
    }

    /**
     * @return the valid
     */
    public boolean isValid() {
        return valid;
    }

    /**
     * @param valid the valid to set
     */
    public void setValid(boolean valid) {
        this.valid = valid;
    }

    /**
     * @return the key
     */
    public String getKey() {
        return key;
    }

    /**
     * @param key the key to set
     */
    public void setKey(String key) {
        this.key = key;
    }
}
