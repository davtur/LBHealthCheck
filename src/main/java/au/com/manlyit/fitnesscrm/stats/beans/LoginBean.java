/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.StringEncrypter;
import au.com.manlyit.fitnesscrm.stats.classes.util.UAgentInfo;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author david
 */
@Named("loginBean")
@SessionScoped
public class LoginBean implements Serializable {

    private static final Logger logger = Logger.getLogger(LoginBean.class.getName());
    private String username;
    private String password;
    private boolean mobileDeviceUserAgent = false;
    private Future<Boolean> emailSendResult;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentBean ejbPaymentBean;
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

    private Properties emailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));

        return props;

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
                String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Pure Fitness Manly</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.purefitnessmanly.com.au | sarah@purefitnessmanly.com.au | +61433818067</td>  </tr></table>";

                //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
                //emailAgent.send("david@manlyit.com.au", "", "info@purefitnessmanly.com.au", "Password Reset", htmlText, null, true);
                emailSendResult = ejbPaymentBean.sendAsynchEmail(current.getEmailAddress(), configMapFacade.getConfig("PasswordResetCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), configMapFacade.getConfig("PasswordResetEmailSubject"), htmlText, null, emailServerProperties(), false);
                JsfUtil.addSuccessMessage("Password Reset Successful!", configMapFacade.getConfig("PasswordResetSuccessful"));
                FacesContext context = FacesContext.getCurrentInstance();
                ActivationBean controller = (ActivationBean) context.getApplication().evaluateExpressionGet(context, "#{activationBean}", ActivationBean.class);
                controller.setValid(true);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("PasswordResetErrorValidUsernameRequired"));
        }
        return "activation";
        /* if (this.isMobileDeviceUserAgent() == true) {
         return "/mobileMenu";
         } else {
         return "/index";
         }*/

    }

    @PostConstruct
    public void myPostConstruct() {
        //String renderKitId = FacesContext.getCurrentInstance().getViewRoot().getRenderKitId();
        // if (renderKitId.equalsIgnoreCase("PRIMEFACES_MOBILE")) {
        if (mobileDevice() == true) {
            //REDIRECT TO  MOBILE PAGE
            ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();

            try {
                this.setMobileDeviceUserAgent(true);
                logger.log(Level.INFO, "Mobile Device user agent detected. Redirecting to the mobile login page.");
                ec.redirect("mobileLogin.xhtml");
            } catch (IOException e) {
                JsfUtil.addErrorMessage(e, "Redirect to Mobile Login failed");

            }

        }
    }

    public boolean mobileDevice() {
        FacesContext context = FacesContext.getCurrentInstance();
        HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String userAgent = req.getHeader("user-agent");
        String accept = req.getHeader("Accept");

        if (userAgent != null && accept != null) {
            UAgentInfo agent = new UAgentInfo(userAgent, accept);
            if (agent.detectMobileQuick()) {
                return true;
            }
        } else {
            logger.log(Level.WARNING, "Can't detect mobile device as the user agent or accept header is null!");
        }

        return false;
    }

    public void login() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        try {
            try {
                request.login(this.username, this.password);
            } catch (ServletException servletException) {
                 if (servletException.getMessage().contains("Login failed") == false) {
                    throw servletException;
                } else {
                    logger.log(Level.INFO, "Login Failed - Bad username or Password");
                    context.addMessage(null, new FacesMessage("Login Failed. Username or Password is incorrect!"));
                    return;
                }
            }
            HttpSession httpSession = request.getSession();
            String landingPage;
            if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                logger.log(Level.INFO, "Mobile Device user agent detected. Redirecting to the mobile landing page.");
                landingPage =  getValueFromKey("facebook.redirect.mobilelandingpage");
            } else {
                landingPage = getValueFromKey("facebook.redirect.landingpage");
            }
            ec.redirect(request.getContextPath() + landingPage);
            logger.log(Level.INFO, "Redirecting to Landing Page:", landingPage);
        } catch (ServletException | IOException e) {
            context.addMessage(null, new FacesMessage("Login failed."));
            logger.log(Level.WARNING, "Login Failed", e);
        }
    }

    private String getValueFromKey(String key) {
        String val;
        val = configMapFacade.getConfig(key);
        return val;
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

    /**
     * @return the mobileDeviceUserAgent
     */
    public boolean isMobileDeviceUserAgent() {
        return mobileDeviceUserAgent;
    }

    /**
     * @param mobileDeviceUserAgent the mobileDeviceUserAgent to set
     */
    public void setMobileDeviceUserAgent(boolean mobileDeviceUserAgent) {
        this.mobileDeviceUserAgent = mobileDeviceUserAgent;
    }

    private boolean mobileDevice(HttpServletRequest req) {
        //FacesContext context = FacesContext.getCurrentInstance();
        //HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String userAgent = req.getHeader("user-agent");
        String accept = req.getHeader("Accept");

        if (userAgent != null && accept != null) {
            UAgentInfo agent = new UAgentInfo(userAgent, accept);
            if (agent.detectMobileQuick()) {
                return true;
            }
        }

        return false;
    }
}
