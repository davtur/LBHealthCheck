/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;


import com.itextpdf.text.log.LoggerFactory;
import java.io.Serializable;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;

 
//@PushEndpoint("/{user}")
//@Singleton
public class PushComponentUpdateResource  implements Serializable  {
 
    
    private static final Logger logger = Logger.getLogger(PushComponentUpdateResource.class.getName());
 
    //@PathParam("user")
    private String username;
 
    
    // @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }
 
 
}