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
@Named(value = "carouselBean")
@RequestScoped
public class CarouselBean implements Serializable {

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbFacade;

    /**
     * Creates a new instance of CarouselBean
     */
    public CarouselBean() {
    }

    public void removePicture() {
        FacesContext context = FacesContext.getCurrentInstance();
        Map<String, String[]> paramValues = context.getExternalContext().getRequestParameterValuesMap();
        String[] selectedImages = paramValues.get("selectedImage");

        for (String item : selectedImages) {
            if (item != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(item));
                if (custImage != null) {
                    CustomerImagesController controller = (CustomerImagesController) context.getApplication().getELResolver().
                    getValue(context.getELContext(), null, "customerImagesController");
                    controller.removeImageFromList(custImage);
                    ejbFacade.remove(custImage);
                }
            }
        }
    }
    
    public StreamedContent getImage() throws IOException {
        FacesContext context = FacesContext.getCurrentInstance();

        if (context.getRenderResponse()) {
            // So, we're rendering the HTML. Return a stub StreamedContent so that it will generate right URL.
            return new DefaultStreamedContent();
        } else {
            // So, browser is requesting the image. Return a real StreamedContent with the image bytes.
            String imageId = context.getExternalContext().getRequestParameterMap().get("imageId");
            if (imageId != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                return new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));
            } else {
                return null;
            }
        }
    }
}
