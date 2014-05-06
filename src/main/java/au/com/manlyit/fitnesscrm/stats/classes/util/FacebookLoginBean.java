/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import javax.enterprise.context.SessionScoped;

import javax.faces.context.FacesContext;
import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpSession;

/**
 *
 * @author david
 */
@Named("facebookLoginBean")
@SessionScoped
public class FacebookLoginBean implements Serializable {

    private static final long serialVersionUID = -1611162265998907599L;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;

    public String getFacebookUrlAuth() {
        HttpSession session
                = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        String sessionId = session.getId();
        String appId = configMapFacade.getConfig("facebook.app.id");//"247417342102284";
        String redirectUrl = configMapFacade.getConfig("facebook.redirect.url");//"http://localhost:8080/FitnessStats/index.sec";
        String returnValue = "https://www.facebook.com/dialog/oauth?client_id="
                + appId + "&redirect_uri=" + redirectUrl
                + "&scope=email,user_birthday&state=" + sessionId;
        return returnValue;
    }

    public String getUserFromSession() {
        HttpSession session
                = (HttpSession) FacesContext.getCurrentInstance().getExternalContext().getSession(false);
        String userName = (String) session.getAttribute("FACEBOOK_USER");
        if (userName != null) {
            return "Hello " + userName + ". We couldn't find you in the Pure Fitness Manly system. Please contact us to enable your login. http://www.purefitnessmanly.com.au/contact-the-pure-fitness-team/manly-boot-camp-and-personal-training-contact-form";
        } else {
            return "";
        }
    }
}
