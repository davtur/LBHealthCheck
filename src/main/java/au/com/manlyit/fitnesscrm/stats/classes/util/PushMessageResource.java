/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.logging.Level;
import javax.faces.application.FacesMessage;
import org.primefaces.push.EventBus;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.annotation.Singleton;

import javax.inject.Inject;
import javax.servlet.ServletContext;
import org.primefaces.push.impl.JSONEncoder;

@PushEndpoint("/payments/{session}")
@Singleton
public class PushMessageResource {

   

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(PushMessageResource.class.getName());

  /*  @PathParam("session")
    private String sessionId;
   

    @Inject
    private ServletContext ctx;

    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
               LOGGER.log(Level.INFO, "Atmosphere Push Connection MESSAGE RECIEVED., SessionID={2}, Summary={0}, Details={1} ", new Object[]{message.getSummary(), message.getDetail(),sessionId});
        return message;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint rEndPoint, EventBus e) {
        rEndPoint.address(); 
     
 //       LOGGER.log(Level.INFO, "Atmosphere Push Connection OPENED. Transport Type = {0}, Address = {1}, Path = {2}, URI = {3}, Status = {4}, sessionID={5}", new Object[]{rEndPoint.transport().name(), rEndPoint.address(), rEndPoint.path(), rEndPoint.uri(), rEndPoint.status(),sessionId});
    }

    @OnClose
    public void onClose(RemoteEndpoint rEndPoint, EventBus e) {
      
//        LOGGER.log(Level.INFO, "Atmosphere Push Connection CLOSED. Transport Type = {0}, Address = {1}, Path = {2}, URI = {3}, Status = {4}", new Object[]{rEndPoint.transport().name(), rEndPoint.address(), rEndPoint.path(), rEndPoint.uri(), rEndPoint.status(),sessionId});

    }

    
    @OnMessage(decoders = {MessageDecoder.class}, encoders = {MessageEncoder.class})
    public Message onMessage(Message message) {
        return message;
    }*/

   

}
