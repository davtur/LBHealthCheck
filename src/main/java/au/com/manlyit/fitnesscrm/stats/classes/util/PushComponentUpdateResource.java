/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;


import java.io.Serializable;
import javax.faces.application.FacesMessage;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.annotation.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
 
import org.primefaces.push.impl.JSONEncoder;
 
//@PushEndpoint("/{user}")
//@Singleton
public class PushComponentUpdateResource  implements Serializable  {
 
    private final Logger logger = LoggerFactory.getLogger(PushComponentUpdateResource.class);
 
 
    //@PathParam("user")
    private String username;
 
    
    // @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }
 
 
}