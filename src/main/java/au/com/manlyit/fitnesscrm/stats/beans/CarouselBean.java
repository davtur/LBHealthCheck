/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomerImagesController;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import java.io.ByteArrayInputStream;
import java.io.Serializable;
import java.util.Map;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import org.primefaces.model.DefaultStreamedContent;

/**
 *
 * @author david
 */
@Named(value = "carouselBean")
@ViewScoped
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
}
