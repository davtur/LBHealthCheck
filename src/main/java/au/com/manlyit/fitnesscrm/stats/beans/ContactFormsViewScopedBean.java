/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomerImagesController;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.Map;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@Named(value = "contactFormsViewScopedBean")
@RequestScoped
public class ContactFormsViewScopedBean implements Serializable {

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbFacade;
    private boolean contactFormSubmitted = false;
    /**
     * Creates a new instance of CarouselBean
     */
    public ContactFormsViewScopedBean() {
    }

    /**
     * @return the contactFormSubmitted
     */
    public boolean isContactFormSubmitted() {
        return contactFormSubmitted;
    }

    /**
     * @param contactFormSubmitted the contactFormSubmitted to set
     */
    public void setContactFormSubmitted(boolean contactFormSubmitted) {
        this.contactFormSubmitted = contactFormSubmitted;
    }

    
}
