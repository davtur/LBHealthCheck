/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.PasswordService;
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
import java.util.Locale;
import java.util.Properties;
import java.util.Random;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.primefaces.application.exceptionhandler.PrimeExceptionHandlerELResolver;

/**
 *
 * @author david
 */
@Named("loginBean")
@SessionScoped
 public class LoginBean implements Serializable {

    private static final Logger logger = Logger.getLogger(LoginBean.class.getName());
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private boolean renderFacebook = false;
   
    private String facebookId;
    private boolean mobileDeviceUserAgent = false;
    private String faceBookAccessToken;
    //private Future<Boolean> emailSendResult;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.EmailTemplatesFacade ejbEmailTemplatesFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentBean ejbPaymentBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
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
        props.put("mail.smtp.headerimage.url", configMapFacade.getConfig("mail.smtp.headerimage.url"));
        props.put("mail.smtp.headerimage.cid", configMapFacade.getConfig("mail.smtp.headerimage.cid"));

        return props;

    }

    public void doPasswordReset(String templateName, Customers current, String subject) {

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
                String templateLinkPlaceholder = configMapFacade.getConfig("login.password.reset.templateLinkPlaceholder");
                String templateTemporaryPasswordPlaceholder = configMapFacade.getConfig("login.password.reset.templateTemporaryPasswordPlaceholder");
                String templateUsernamePlaceholder = configMapFacade.getConfig("login.password.reset.templateUsernamePlaceholder");
                //String htmlText = configMapFacade.getConfig(templateName);
                String htmlText = ejbEmailTemplatesFacade.findTemplateByName(templateName).getTemplate();

                htmlText = htmlText.replace(templateLinkPlaceholder, urlLink);
                htmlText = htmlText.replace(templateUsernamePlaceholder, current.getUsername());
                String tempPassword = generateUniqueToken(8) ;
                
                current.setPassword(PasswordService.getInstance().encrypt(tempPassword));
                ejbCustomerFacade.editAndFlush(current);
                htmlText = htmlText.replace(templateTemporaryPasswordPlaceholder, tempPassword);
                //String htmlText = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Pure Fitness Manly</p>      <p>Please click the following link to reset your password:</p><p>To reset your password click <a href=\"" + urlLink + "\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.purefitnessmanly.com.au | sarah@purefitnessmanly.com.au | +61433818067</td>  </tr></table>";

                //String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug
                //emailAgent.send("david@manlyit.com.au", "", "info@purefitnessmanly.com.au", "Password Reset", htmlText, null, true);
                Future<Boolean> emailSendResult = ejbPaymentBean.sendAsynchEmail(current.getEmailAddress(), configMapFacade.getConfig("PasswordResetCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), subject, htmlText, null, emailServerProperties(), false);
                JsfUtil.addSuccessMessage("Password Reset Successful!", configMapFacade.getConfig("PasswordResetSuccessful"));
                FacesContext context = FacesContext.getCurrentInstance();
                ActivationBean controller = context.getApplication().evaluateExpressionGet(context, "#{activationBean}", ActivationBean.class);
                controller.setValid(true);
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
            }

        } else {
            JsfUtil.addErrorMessage("Error", configMapFacade.getConfig("PasswordResetErrorValidUsernameRequired"));
        }

    }
    public void getHandleErrorFromErrorPage(){
                FacesContext context = FacesContext.getCurrentInstance();
                //todo
       // PrimeExceptionHandlerELResolver handler = (PrimeExceptionHandlerELResolver) context.getApplication().getELResolver().getValue(context.getELContext(), null, "pfExceptionHandler");
        
       // String html = handler.
      //                 Future<Boolean> emailSendResult = ejbPaymentBean.sendAsynchEmail(configMapFacade.getConfig("SystemErrorCCEmailAddress"), configMapFacade.getConfig("SystemErrorCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), configMapFacade.getConfig("SystemErrorCCEmailAddress"), htmlText, null, emailServerProperties(), false);
 
    }

    public String resetPassword() {
        /*     try {
         String redirectUrl = configMapFacade.getConfig("login.password.reset.redirect.url") + this.username;
         FacesContext.getCurrentInstance().getExternalContext().redirect(redirectUrl);
         } catch (IOException ex) {
         Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, "Failed to redirect to password reset URL", ex);
         }*/
        Customers current = ejbCustomerFacade.findCustomerByUsername(username);
        doPasswordReset("system.reset.password.template", current, configMapFacade.getConfig("PasswordResetEmailSubject"));
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

        //facesContext = FacesContext.getCurrentInstance();
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

    public void checkAlreadyLoggedin() throws IOException {

        HttpServletRequest request = (HttpServletRequest) FacesContext.getCurrentInstance().getExternalContext().getRequest();

        String authenticatedUser = request.getRemoteUser();
        if (authenticatedUser != null) {
            logger.log(Level.INFO, "Authenticated user accessing login page. Redirecting to the landing page.");
            redirectToLandingPage();
        }

    }

    private void redirectToLandingPage() {
       
        ExternalContext ec = FacesContext.getCurrentInstance().getExternalContext();
        HttpServletRequest request = (HttpServletRequest) ec.getRequest();
        try {

            HttpSession httpSession = request.getSession();
            String landingPage;
            if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                logger.log(Level.INFO, "Mobile Device user agent detected. Redirecting to the mobile landing page.");
                landingPage = getValueFromKey("facebook.redirect.mobilelandingpage");
            } else {
                landingPage = getValueFromKey("facebook.redirect.landingpage");
            }
            ec.redirect(request.getContextPath() + landingPage);
            logger.log(Level.INFO, "Redirecting to Landing Page:", landingPage);
        } catch (IOException e) {
            JsfUtil.addErrorMessage(e, "Login Failed.");
            logger.log(Level.WARNING, "Login Failed", e);
        }
    }

    public void login() {
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) ec.getRequest();

        try {
            try {
                request.login(this.username, this.password);
            } catch (ServletException servletException) {
                String errorMessage = servletException.getMessage();
                if (errorMessage.contains("already been authenticated")) {
                    logger.log(Level.INFO, "User Aleady Authenticated - redirecting to landing page.");
                } else if (errorMessage.contains("Login failed")) {
                    logger.log(Level.INFO, "Login Failed - Bad username or Password");
                    JsfUtil.addErrorMessage("Login Failed", "Username or Password is incorrect!");
                    return;
                } else {
                    // unhandled exception 

                    throw servletException;
                }
            }
            
            Customers cust = ejbCustomerFacade.findCustomerByUsername(username);
            String auditDetails = "Customer Login Successful:" + cust.getUsername() + " Details:  " + cust.getLastname() + " " + cust.getFirstname() + " ";
            String changedFrom = "UnAuthenticated";
            String changedTo = "Authenticated User:" + username;
            if (cust.getUsername().toLowerCase(Locale.getDefault()).equals("synthetic.tester")) {
                logger.log(Level.INFO, "Synthetic Tester Logged In.");
            } else {
                ejbAuditLogFacade.audit(cust, cust, "Logged In", auditDetails, changedFrom, changedTo);
            }
            cust.setLastLoginTime(new Date());
            cust.setLoginAttempts(0);
            ejbCustomerFacade.edit(cust);
            redirectToLandingPage();

        } catch (ServletException e) {
            JsfUtil.addErrorMessage(e, "Login Failed.");
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
            Customers cust = ejbCustomerFacade.findCustomerByUsername(context.getExternalContext().getRemoteUser());
            String auditDetails = "Customer Logout Successful:" + cust.getUsername() + " Details:  " + cust.getLastname() + " " + cust.getFirstname() + " ";
            String changedTo = "UnAuthenticated";
            String changedFrom = "Authenticated User:" + cust.getUsername();
            ejbAuditLogFacade.audit(cust, cust, "Logged Out", auditDetails, changedFrom, changedTo);

        } catch (ServletException e) {
            JsfUtil.addErrorMessage("Logout failed.", e.getMessage());
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

    private boolean updateFacebookId(Customers cust) {

        faceBookAccessToken = getAccessToken();
        facebookId = getCustomerfacebookId(cust.getEmailAddress());
        return facebookId != null;

    }

    public String getCustomerfacebookId(String email) {
        String fbId = null;
        String encodedEmail = "";
        String encodedToken = "";
        try {
            encodedEmail = URLEncoder.encode(email, "UTF-8");
            encodedToken = URLEncoder.encode(faceBookAccessToken, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(LoginBean.class.getName()).log(Level.SEVERE, null, ex);
        }
        String newUrl = "https://graph.facebook.com/search?access_token=" + encodedToken + "&q=" + encodedEmail + "&type=user";
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet(newUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpget, responseHandler);
            String firstName = null;
            String lastName = null;

            try {
                JSONObject json = (JSONObject) JSONSerializer.toJSON(responseBody);
                fbId = json.getString("id");

                firstName = json.getString("first_name");
                lastName = json.getString("last_name");

                String fbEmail = json.getString("email");
                if (fbEmail == null) {
                    logger.log(Level.WARNING, "Error getting JSON objects from facebook: email is NULL");
                } else if (email.contentEquals(fbEmail) == false) {
                    logger.log(Level.WARNING, "Error getting JSON objects from facebook: emails dont match!");
                }
                //put user data in session

                if (fbId == null || firstName == null || lastName == null || email == null) {
                    if (facebookId == null) {
                        logger.log(Level.WARNING, "Error getting JSON objects from facebook: Facebook ID is NULL");
                    }
                    if (firstName == null) {
                        logger.log(Level.WARNING, "Error getting JSON objects from facebook: firstName is NULL");
                    }
                    if (lastName == null) {
                        logger.log(Level.WARNING, "Error getting JSON objects from facebook: lastName is NULL");
                    }

                }

            } catch (Exception e) {
                logger.log(Level.WARNING, "Error getting JSON objects from facebook}", e);
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            try {
                httpclient.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        return fbId;
    }

    public String getAccessToken() {
        String token = null;
        String appId = getValueFromKey("facebook.app.id");//"247417342102284";
        // String redirectUrl = getValueFromKey("facebook.redirect.url");//http://localhost:8080/FitnessStats/index.sec";
        String faceAppSecret = getValueFromKey("facebook.app.secret");//"33715d0844267d3ba11a24d44e90be80";
        String newUrl = "https://graph.facebook.com/oauth/access_token?client_id=" + appId + "&client_secret=" + faceAppSecret + "&grant_type=client_credentials";
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            HttpGet httpget = new HttpGet(newUrl);
            ResponseHandler<String> responseHandler = new BasicResponseHandler();
            String responseBody = httpclient.execute(httpget, responseHandler);
            token = StringUtils.removeEnd(StringUtils.removeStart(responseBody, "access_token="), "&expires=5180795");
        } catch (ClientProtocolException e) {
            logger.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            try {
                httpclient.close();
            } catch (IOException ex) {
                logger.log(Level.SEVERE, null, ex);
            }
        }

        return token;
    }

    /**
     * @return the renderFacebook
     */
    public boolean isRenderFacebook() {
        Customers cust = ejbCustomerFacade.findCustomerByUsername(username);
        if (cust != null) {
            facebookId = cust.getFacebookId();
            if (facebookId == null) {
                //find the facebook id
                return updateFacebookId(cust);

            }
            return true;
        } else {
            facebookId = null;
            return false;
        }

    }

    /**
     * @param renderFacebook the renderFacebook to set
     */
    public void setRenderFacebook(boolean renderFacebook) {
        this.renderFacebook = renderFacebook;
    }

    /**
     * @return the facebookId
     */
    public String getFacebookId() {
        facebookId = null;
        Customers cust = ejbCustomerFacade.findCustomerByUsername(username);
        if (cust != null) {
            facebookId = cust.getFacebookId();
        }
        return facebookId;
    }

    /**
     * @param facebookId the facebookId to set
     */
    public void setFacebookId(String facebookId) {
        this.facebookId = facebookId;
    }

    /**
     * @return the faceBookAccessToken
     */
    public String getFaceBookAccessToken() {
        return faceBookAccessToken;
    }

    /**
     * @param faceBookAccessToken the faceBookAccessToken to set
     */
    public void setFaceBookAccessToken(String faceBookAccessToken) {
        this.faceBookAccessToken = faceBookAccessToken;
    }
}
