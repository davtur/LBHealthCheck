/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.IOException;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.primefaces.PrimeFaces;

/**
 *
 * @author david
 */
@Named("facebookLoginBean")
@SessionScoped
public class FacebookLoginBean implements Serializable {

   
    
    private static final Logger logger = Logger.getLogger(FacebookLoginBean.class.getName());
    private static final long serialVersionUID = -1611162265998907599L;
    
    
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.LoginBean loginBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ApplicationBean applicationBean;
    
    public void timetableFacebookRedirect() {
        loginBean.setDontRedirect(true);
        facebookRedirect();
    }
    
    public void facebookRedirect() {
        
        try {
            String destination = "window.location=\"" + getFacebookUrlAuth() + "\"";
            PrimeFaces.current().executeScript(destination);
            Logger.getLogger(FacebookLoginBean.class.getName()).log(Level.INFO, "facebookRedirect to : {0}", destination);
        } catch (Exception e) {
            Logger.getLogger(FacebookLoginBean.class.getName()).log(Level.SEVERE, "facebookRedirect failed", e);
        }
    }
    
    public String getFacebookUrlAuth() {
        String returnValue = "";
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();
        
        HttpSession session
                = (HttpSession) ec.getSession(false);
        if (session != null) {
            String userName = (String) session.getAttribute("FACEBOOK_USER");
            /*  if (userName != null) {

                String isMobile = (String) session.getAttribute("MOBILE_DEVICE");
                String landingPage = configMapFacade.getConfig("facebook.redirect.landingpage");
                if (isMobile != null) {
                    if (isMobile.contains("TRUE")) {
                        landingPage = configMapFacade.getConfig("facebook.redirect.mobilelandingpage");
                    }
                } else {
                    logger.log(Level.WARNING, "getFacebookUrlAuth -  session.getAttribute(\"MOBILE_DEVICE\") is NULL.");
                }
                String message = "The user " + userName + " is already logged in. Redirecting to the landing Page:" + landingPage;
                logger.log(Level.INFO, message);
                try {
                    String sendToThisUrl = request.getContextPath() + landingPage;
                    String pathInfo = request.getPathInfo();
                    // if (pathInfo == null) {
                   //  pathInfo = request.getServletPath();
                    // }
                    if (pathInfo != null) {
                        if (pathInfo.contains(sendToThisUrl)) {
                            logger.log(Level.INFO, "getFacebookUrlAuth -  The path is the same as the redirect URL. No need to redirect.");
                        } else {
                            ec.redirect(sendToThisUrl);
                        }
                    }
                } catch (Exception ex) {
                    Logger.getLogger(FacebookLoginBean.class.getName()).log(Level.SEVERE, "Redirecting to Landing Page:", ex);
                }
            } else {
          
                String sessionId = session.getId();
                String appId = configMapFacade.getConfig("facebook.app.id");//"247417342102284";
                String redirectUrl = configMapFacade.getConfig("facebook.redirect.url");//"http://localhost:8080/FitnessStats/index.sec";
                returnValue = configMapFacade.getConfig("facebook.app.oauth.url") + 
                        "client_id=" + appId + 
                        "&redirect_uri=" + redirectUrl + 
                        "&scope=email,user_birthday&state=" + sessionId;
             */
            // }
            returnValue = getFacebookLoginRedirectUrl(session.getId());
        } else {
            logger.log(Level.WARNING, "getFacebookUrlAuth -  session  is NULL.");
            returnValue = configMapFacade.getConfig("facebook.redirect.landingpage");
        }
        return returnValue;
    }
    
    private String getFacebookLoginRedirectUrl(String state) {
        String returnValue;
        String encodedState = "";
        if (state != null) {
            long ts = new Date().getTime();
            String timesInMill = Long.toString(ts);
            applicationBean.addFacebookLoginState(state,  timesInMill);
            logger.log(Level.INFO, "getFacebookLoginRedirectUrl -  FacebookState",state);
            String appId = configMapFacade.getConfig("facebook.app.id");//"247417342102284";
            String redirectUrl = configMapFacade.getConfig("facebook.redirect.url");//"http://localhost:8080/FitnessStats/index.sec";
            try {
                String redirectState = "1";
                if (loginBean.isDontRedirect() == false) {
                    redirectState = "0";
                }
                encodedState = URLEncoder.encode(redirectState + state, "UTF-8");
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(FacebookLoginBean.class.getName()).log(Level.SEVERE, "getFacebookLoginRedirectUrl", ex);
            }
            returnValue = configMapFacade.getConfig("facebook.app.oauth.url")
                    + "client_id=" + appId
                    + "&redirect_uri=" + redirectUrl
                    + "&scope=email,user_birthday&state=" + encodedState;
        } else {
            logger.log(Level.WARNING, "getFacebookUrlAuth -  session  is NULL.");
            returnValue = configMapFacade.getConfig("facebook.redirect.landingpage");
        }
        return returnValue;
    }
    
    public String getAccessToken() {
        String token = null;
        
        String appId = getValueFromKey("facebook.app.id");//"247417342102284";
        String redirectUrl = getValueFromKey("facebook.redirect.url");//http://localhost:8080/FitnessStats/index.sec";
        String faceAppSecret = getValueFromKey("facebook.app.secret");//"33715d0844267d3ba11a24d44e90be80";
        String newUrl = "https://graph.facebook.com/oauth/access_token?client_id=" + appId + "&client_secret=" + faceAppSecret;
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

    /* public String getFacebookId(String email) {
        String fbid = null;
        if (email != null && !"".equals(email)) {
            String appId = getValueFromKey("facebook.app.id");//"247417342102284";
            String redirectUrl = getValueFromKey("facebook.redirect.url");//http://localhost:8080/FitnessStats/index.sec";
            String faceAppSecret = getValueFromKey("facebook.app.secret");//"33715d0844267d3ba11a24d44e90be80";
            String newUrl = "https://graph.facebook.com/search?access_token=" + appId + "&redirect_uri=" + redirectUrl + "&client_secret="
                    + faceAppSecret + "&code=" + faceCode;
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            try {
                HttpGet httpget = new HttpGet(newUrl);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                fbid = StringUtils.removeEnd(StringUtils.removeStart(responseBody, "access_token="), "&expires=5180795");
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
        }
        return fbid;
    }*/
    private String getFacebookAccessToken(String faceCode) {
        String token = null;
        if (faceCode != null && !"".equals(faceCode)) {
            String appId = getValueFromKey("facebook.app.id");//"247417342102284";
            String redirectUrl = getValueFromKey("facebook.redirect.url");//http://localhost:8080/FitnessStats/index.sec";
            String faceAppSecret = getValueFromKey("facebook.app.secret");//"33715d0844267d3ba11a24d44e90be80";
            String newUrl = "https://graph.facebook.com/oauth/access_token?client_id="
                    + appId + "&redirect_uri=" + redirectUrl + "&client_secret="
                    + faceAppSecret + "&code=" + faceCode;
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
        }
        return token;
    }
    
    private String getValueFromKey(String key) {
        String val;
        val = configMapFacade.getConfig(key);
        return val;
    }
    
    public String getUserFromSession() {
        HttpSession session
                = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        String userName = null;
        if (session != null) {
            userName = (String) session.getAttribute("FACEBOOK_USER");
        }
        if (userName != null) {
            return "Hello " + userName + ". " + configMapFacade.getConfig("facebook.app.login.disabled.message");
        } else {
            return "";
        }
    }
    
}
