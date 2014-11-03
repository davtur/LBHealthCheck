/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.SessionScoped;
import javax.faces.context.ExternalContext;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

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

    public String getFacebookUrlAuth() {
        String returnValue = "";
        FacesContext context = FacesContext.getCurrentInstance();
        ExternalContext ec = context.getExternalContext();
        HttpServletRequest request = (HttpServletRequest) context.getExternalContext().getRequest();

        HttpSession session
                = (HttpSession) ec.getSession(false);
        String userName = (String) session.getAttribute("FACEBOOK_USER");
        if (userName != null) {

            String isMobile = (String) session.getAttribute("MOBILE_DEVICE");
            String landingPage;
            if (isMobile.contains("TRUE")) {
                landingPage = configMapFacade.getConfig("facebook.redirect.mobilelandingpage");
            } else {
                landingPage = configMapFacade.getConfig("facebook.redirect.landingpage");
            }
            String message = "The user " + userName + " is already logged in. Redirecting to the landing Page:" + landingPage;
            logger.log(Level.INFO, message);
            try {
                String sendToThisUrl = request.getContextPath() + landingPage;
                if (request.getPathInfo().contains(sendToThisUrl)) {
                    logger.log(Level.INFO, "getFacebookUrlAuth -  The path is the same as the redirect URL. No need to redirect.");
                } else {
                    ec.redirect(sendToThisUrl);
                }
            } catch (IOException ex) {
                Logger.getLogger(FacebookLoginBean.class.getName()).log(Level.SEVERE, "Redirecting to Landing Page:", ex);
            }
        } else {
            String sessionId = session.getId();
            String appId = configMapFacade.getConfig("facebook.app.id");//"247417342102284";
            String redirectUrl = configMapFacade.getConfig("facebook.redirect.url");//"http://localhost:8080/FitnessStats/index.sec";
            returnValue = configMapFacade.getConfig("facebook.app.oauth.url") + "client_id="
                    + appId + "&redirect_uri=" + redirectUrl
                    + "&scope=email,user_birthday&state=" + sessionId;

        }
        return returnValue;
    }

    public String getUserFromSession() {
        HttpSession session
                = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        String userName = (String) session.getAttribute("FACEBOOK_USER");
        if (userName != null) {
            return "Hello " + userName + ". " + configMapFacade.getConfig("facebook.app.login.disabled.message");
        } else {
            return "";
        }
    }
}
