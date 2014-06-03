/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import javax.annotation.ManagedBean;
import javax.enterprise.context.RequestScoped;
import javax.faces.FacesException;
import javax.faces.application.ViewExpiredException;
import javax.faces.context.FacesContext;

@ManagedBean
@RequestScoped
public class AjaxErrorHandlerController {

    public void throwNullPointerException() {
        throw new NullPointerException("Ajax NullPointerException!");
    }
 
    public void throwWrappedIllegalStateException() {
        Throwable t = new IllegalStateException("Ajax wrapped IllegalStateException!");
        throw new FacesException(t);
    }
 
    public void throwViewExpiredException() {
        throw new ViewExpiredException("Ajax ViewExpiredException!",
                FacesContext.getCurrentInstance().getViewRoot().getViewId());
    }
}
