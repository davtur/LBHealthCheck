/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.primefaces.model.DefaultStreamedContent;

/**
 *
 * @author david
 */
@WebServlet("/StreamImage")
public class StreamGraphicImage extends HttpServlet {

    private static final long serialVersionUID = 4593558495041379082L;
    private static final Logger LOGGER = Logger.getLogger(StreamGraphicImage.class.getName());
    @Inject
    private CustomersController controller;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbFacade;
    @Inject
    private ConfigMapFacade configMapFacade;

    @Override
    public void doGet(HttpServletRequest request,
            HttpServletResponse response)
            throws ServletException, IOException {

        LOGGER.log(Level.INFO, "StreamGraphicImage Called");

        try {

            String imageId = request.getParameter("imageId");
            if (imageId != null) {
                CustomerImages custImage = ejbFacade.find(Integer.valueOf(imageId));
                if (custImage.getCustomerId().getId().compareTo(getSelectedCustomer().getId()) == 0) {
                    LOGGER.log(Level.INFO, "getImage - returning image:{0},size:{1}, name:{2}, Encoding:{3}, Content Type: {4} ", new Object[]{imageId, custImage.getImage().length, custImage.getImageFileName(), custImage.getImageStream().getContentEncoding(), custImage.getImageStream().getContentType()});

                    // ByteArrayInputStream byteArray = 
                    DefaultStreamedContent dsc = new DefaultStreamedContent(new ByteArrayInputStream(custImage.getImage()));

                    response.reset();
                    response.setContentType(dsc.getContentType());

                    BufferedInputStream input = null;
                    BufferedOutputStream output = null;

                    try {
                        input = new BufferedInputStream(dsc.getStream());

                        output = new BufferedOutputStream(response.getOutputStream());
                        byte[] buffer = new byte[8192];
                        int bytesCount;
                        while ((bytesCount = input.read(buffer)) != -1) {
                            output.write(buffer, 0, bytesCount);
                        }

                    } finally {
                        if (output != null) {
                            try {
                                output.close();
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "StreamGraphicImage: Trying to close output", e);
                            }
                        }
                        if (input != null) {
                            try {
                                input.close();
                            } catch (IOException e) {
                                LOGGER.log(Level.WARNING, "StreamGraphicImage: Trying to close input", e);
                            }
                        }
                    }

                } else {
                    LOGGER.log(Level.WARNING, "A customer is attempting to access anothers images by directly manipulating the URL posted parameters. It might be a hacker. Returning NULL instead of the image. Logged In Customer:{0},Imageid:{1}", new Object[]{getSelectedCustomer().getUsername(), imageId});

                }
            } else {
                LOGGER.log(Level.WARNING, "getImage: imageId is NULL");

            }

        } catch (NumberFormatException | IOException e) {
            LOGGER.log(Level.WARNING, "StreamGraphicImage: ", e);
        }
    }

    private Customers getSelectedCustomer() {

        return controller.getSelected();
    }
}
