/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.CustomerImagesController;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.context.FacesContext;
import javax.imageio.ImageIO;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named("applicationBean")
@ApplicationScoped
public class ApplicationBean implements Serializable {

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbCustomerImagesFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomersFacade;
    private static final Logger logger = Logger.getLogger(ApplicationBean.class.getName());

    public ApplicationBean() {
    }

    @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "ApplicationBean Created");
        sanityCheckCustomersForDefaultItems();
    }

    private void sanityCheckCustomersForDefaultItems() {
        logger.log(Level.INFO, "Performing Sanity Checks on Customers");
        List<Customers> cl = ejbCustomersFacade.findAll();
        for (Customers c : cl) {
            if (c.getProfileImage() == null) {
                createDefaultProfilePic(c);
            }
        }
        logger.log(Level.INFO, "FINISHED Performing Sanity Checks on Customers");
    }

    public void createDefaultProfilePic(Customers cust) {
        String placeholderImage = configMapFacade.getConfig("system.default.profile.image");
        String fileExtension = placeholderImage.substring(placeholderImage.lastIndexOf(".")).toLowerCase();
        int imgType = -1;
        if (fileExtension.contains("jpeg") || fileExtension.contains("jpg")) {
            imgType = 2;
            fileExtension = "jpeg";
        }
        if (fileExtension.contains("png")) {
            imgType = 1;
            fileExtension = "png";
        }
        if (fileExtension.contains("gif")) {
            imgType = 0;
            fileExtension = "gif";
        }
        if (imgType == -1) {
            logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due the picture not being in jpeg, gif or png. resource:{0}", new Object[]{placeholderImage, cust.getUsername()});
            return;
        }
        if (cust != null) {
            if (cust.getProfileImage() == null) {
                try {
                    CustomerImages ci = new CustomerImages(0);
                    BufferedImage img = null;
                    InputStream stream = FacesContext.getCurrentInstance().getExternalContext().getResourceAsStream(placeholderImage);
                    try {
                        img = ImageIO.read(stream);
                    } catch (IOException e) {
                        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, Loading image into buffer error!!", e);

                        JsfUtil.addErrorMessage(e, "Loading image into buffer error!!");
                    }

                    ByteArrayOutputStream os = new ByteArrayOutputStream();
                    try {

                        ImageIO.write(img, fileExtension, os);

                    } catch (IOException ex) {
                        Logger.getLogger(CustomerImagesController.class.getName()).log(Level.SEVERE, "createDefaultProfilePic, write image  error!!", ex);
                        JsfUtil.addErrorMessage(ex, "createDefaultProfilePic, write image  error!!");
                    }

                    ci.setImage(os.toByteArray());
                    ci.setImageType(imgType);
                    ci.setCustomers(cust);
                    ci.setCustomerId(cust);
                    ci.setDatetaken(new Date());

                    ejbCustomerImagesFacade.edit(ci);
                    cust.setProfileImage(ci);
                    ejbCustomersFacade.edit(cust);
                } catch (Exception e) {
                    logger.log(Level.WARNING, "createDefaultProfilePic , Cannot add default profile pic for customer {1} due to an exception:{0}", new Object[]{e, cust.getUsername()});

                }
            }
        } else {
            logger.log(Level.WARNING, "createDefaultProfilePic ERROR, Cannot add default profile pic to a null customer object");
        }
    }

}
