/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.annotation.WebListener;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 *
 * @author david
 */
@WebListener
public class SessionListener implements HttpSessionListener {

    private static final Logger logger = Logger.getLogger(SessionListener.class.getName());

    @Override
    public void sessionCreated(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        
 
        
        if(session.isNew()){
            logger.log(Level.INFO, "New Session Created.The server has created a session, but the client has not yet joined. The client may have disabled cookies. Id: {0}", session.getId());
        }else{
            logger.log(Level.INFO, "Session Created id: {0}", session.getId());
        }
       
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent event) {
        HttpSession session = event.getSession();
        logger.log(Level.INFO, "Session Destroyedid: {0}", session.getId());
       

    }
}
