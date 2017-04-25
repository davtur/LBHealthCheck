/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Named;
import javax.enterprise.context.RequestScoped;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseId;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 *
 * @author david
 */
@Named(value = "graphicImageBean")
@RequestScoped
public class GraphicImageBean {

    private static final Logger LOGGER = Logger.getLogger(GraphicImageBean.class.getName());

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;

    /**
     * Creates a new instance of GraphicImageBean
     */
    public GraphicImageBean() {
    }

    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();
        String imageId;
        if (context != null) {
            //if (context.getRenderResponse()) {
            if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
                // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
                LOGGER.log(Level.FINE, "getImage: we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.");
                return new DefaultStreamedContent();
            } else // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            {
                imageId = context.getExternalContext().getRequestParameterMap().get("imageId");
                if (imageId != null) {
                    CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                    if (custImage.getCustomerId().getId().compareTo(getSelectedCustomer().getId()) == 0) {
                        LOGGER.log(Level.FINE, "getImage - returning image:{0},size:{1}, name:{2}, Encoding:{3}, Content Type: {4} ", new Object[]{imageId, custImage.getImage().length, custImage.getImageFileName(), custImage.getImageStream().getContentEncoding(), custImage.getImageStream().getContentType()});
                        return new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));
                    } else {
                        LOGGER.log(Level.WARNING, "A customer is attempting to access anothers images by directly manipulating the URL posted parameters. It might be a hacker. Returning NULL instead of the image. Logged In Customer:{0},Imageid:{1}", new Object[]{getSelectedCustomer().getUsername(), imageId});
                        return null;
                    }
                } else {
                    LOGGER.log(Level.WARNING, "getImage: imageId is NULL");
                    return null;
                }
            }
        }
        return null;
    }

    public StreamedContent getImageBytes(int imageId) throws IOException {

        FacesContext context = FacesContext.getCurrentInstance();

        if (context != null) {
            //if (context.getRenderResponse()) {
            if (context.getCurrentPhaseId() == PhaseId.RENDER_RESPONSE) {
                // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
                LOGGER.log(Level.FINE, "getImageBytes: we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.");
                return new DefaultStreamedContent();
            } else // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            {
                CustomerImages custImage = ejbFacade.find(imageId);
                if (custImage.getCustomerId().getId().compareTo(getSelectedCustomer().getId()) == 0) {
                    LOGGER.log(Level.FINE, "getImageBytes - returning image:{0},size:{1}, name:{2}, Encoding:{3}, Content Type: {4} ", new Object[]{imageId, custImage.getImage().length, custImage.getImageFileName(), custImage.getImageStream().getContentEncoding(), custImage.getImageStream().getContentType()});
                    return new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));
                } else {
                    LOGGER.log(Level.WARNING, " getImageBytes - A customer is attempting to access anothers images by directly manipulating the URL posted parameters. It might be a hacker. Returning NULL instead of the image. Logged In Customer:{0},Imageid:{1}", new Object[]{getSelectedCustomer().getUsername(), imageId});
                    return null;
                }
            }
        }
        LOGGER.log(Level.WARNING, "getImageBytes: returned NULL");
        return null;

    }

    private Customers getSelectedCustomer() {
        FacesContext context = FacesContext.getCurrentInstance();
        CustomersController custController = context.getApplication().evaluateExpressionGet(context, "#{customersController}", CustomersController.class);
        return custController.getSelected();
    }
}
