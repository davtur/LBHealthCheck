/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ConversationScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;
import org.primefaces.context.RequestContext;

/**
 *
 * @author david
 */
@Named("pushSocketBean")
@javax.faces.view.ViewScoped
public class PushSocketBean implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(PushSocketBean.class.getName());
    private static final long serialVersionUID = 1L;
    private boolean pushConnectionOpen = false;
    private boolean connectInProgress = false;
    private final static String CHANNEL = "/payments/";
    private String sessionId;

    /**
     * Creates a new instance of PushSocketBean
     */
    public PushSocketBean() {
    }

    public boolean getPushChannel() {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        String channel = CHANNEL + getSessionId();

        LOGGER.log(Level.FINER, "PushSocketBean - entered getPushChannel() : {0}", new Object[]{channel});
        if (pushConnectionOpen == false && connectInProgress ==false) {
            connectInProgress = true;
            LOGGER.log(Level.INFO, "PushSocketBean - Push Channel CONNECT, Channel: {0}, SessionId:{1}", new Object[]{channel,getSessionId()});
            pushConnectionOpen = true;
            requestContext.execute("PF('subscriber').connect('/" + getSessionId() + "')");
            connectInProgress =false;
        } else {
            LOGGER.log(Level.FINER, "PushSocketBean - push Connection Already Open -  PUSH CHANNELL: {0}, SessionId:{1}", new Object[]{channel,getSessionId()});
        }

        return false;
    }

    public void getPushChannelReconnect() {
        RequestContext requestContext = RequestContext.getCurrentInstance();
        String channel = CHANNEL + getSessionId();

        LOGGER.log(Level.INFO, "PushSocketBean - Push Channel RECONNECT, Channel: {0}, SessionId:{1}", new Object[]{channel,getSessionId()});
        requestContext.execute("PF('subscriber').connect('/" + getSessionId() + "')");

        // return channel;
    }

    public void pushChannelOpen() {
        pushConnectionOpen = true;
        LOGGER.log(Level.INFO, "PushSocketBean - Push Channel OPEN, sessionId: {0}", new Object[]{getSessionId()});
    }

    public void pushChannelClose() {
        pushConnectionOpen = false;
        LOGGER.log(Level.INFO, "PushSocketBean - Push Channel CLOSE, sessionId: {0}", new Object[]{getSessionId()});
    }

    /**
     * @return the pushConnectionOpen
     */
    public boolean isPushConnectionOpen() {
        return pushConnectionOpen;
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        if (sessionId == null) {
            FacesContext context = FacesContext.getCurrentInstance();
            EziDebitPaymentGateway controller = (EziDebitPaymentGateway) context.getApplication().getELResolver().getValue(context.getELContext(), null, "ezidebit");
            sessionId = controller.getSessionId();
        }
        return sessionId;
    }

    /**
     * @param sessionId the sessionId to set
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

}
