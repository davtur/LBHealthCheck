/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import static au.com.manlyit.fitnesscrm.stats.beans.ActivationBean.generateUniqueToken;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.classes.util.StringEncrypter;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.RequestScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

/**
 *
 * @author david
 */
@Named("loginBean")
@RequestScoped
public class LoginBean implements Serializable {

    private String username;
    private String password;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");

    public String getUsername() {
        return this.username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return this.password;
    }

    public void setPassword(String pass) {
        this.password = pass;
    }

    public String resetPassword() {
        /*     try {
         String redirectUrl = configMapFacade.getConfig("login.password.reset.redirect.url") + this.username;
         FacesContext.getCurrentInstance().getExternalContext().redirect(redirectUrl);
         } catch (IOException ex) {
         Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, "Failed to redirect to password reset URL", ex);
         }*/
        Customers current = ejbCustomerFacade.findCustomerByUsername(username);
        //valid user that wants the password reset
        //generate link and send
        if (current != null) {
            String uniquetoken = generateUniqueToken(10);
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssSSS");
            String timestamp = sdf.format(new Date());
            String nonce = timestamp + uniquetoken;
            Activation act = new Activation(0, nonce, new Date());
            String nonceEncrypted = encrypter.encrypt(configMapFacade.getConfig("login.password.reset.token") + nonce);
            String encodedNonceEncrypted;
            String urlLink;
            try {
                encodedNonceEncrypted = URLEncoder.encode(nonceEncrypted, "UTF-8");
                act.setCustomer(current);
                ejbActivationFacade.create(act);
                urlLink = configMapFacade.getConfig("login.password.reset.redirect.url") + encodedNonceEncrypted;

                //send email
                SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
                String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Pure Fitness Manly</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.purefitnessmanly.com.au | sarah@purefitnessmanly.com.au | +61433818067</td>  </tr></table>";

                //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
                emailAgent.send("david@manlyit.com.au", "", "info@purefitnessmanly.com.au", "Password Reset", htmlText, null, true);
                JsfUtil.addSuccessMessage(configMapFacade.getConfig("PasswordResetSuccessful"));
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("Please enter a valid username before resetting the password"));
        }
        return "/login";

    }

    public String login() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            request.login(this.username, this.password);
        } catch (ServletException e) {

            context.addMessage(null, new FacesMessage("Login failed."));
            return "error";
        }
        return "/index";
    }

    public void logout() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        try {
            request.logout();
        } catch (ServletException e) {

            context.addMessage(null, new FacesMessage("Logout failed."));
        }
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
}
