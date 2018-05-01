package au.com.manlyit.fitnesscrm.stats.push;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import static javax.faces.annotation.FacesConfig.Version.JSF_2_3;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Startup;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.annotation.FacesConfig;
import javax.faces.push.Push;
import javax.faces.push.PushContext;
import javax.inject.Inject;
import javax.inject.Named;



//@FacesConfig(
        // Activates CDI build-in beans
 //       version = JSF_2_3
//)
//@ApplicationScoped
//@Named
public class PushBean implements Serializable {

    /**
     *
     */
/*    private static final long serialVersionUID = -4531588431756121167L;

    private static final Logger LOG = Logger.getLogger(PushBean.class.getName());

    @Inject
    @Push
    private PushContext payments;

    public void sendMessage(String message) {
        try {
            payments.send(message);

        } catch (Exception e) {
            LOG.log(Level.WARNING, "Push error", e);
        }
    }

    public void sendMessage(Object message, Customers recipientUser) {
        int recipientUserId = recipientUser.getId();
        payments.send(message, recipientUserId);
    }

    public void sendMessage(Object message, Collection<Integer> recipientUserIds) {
        payments.send(message, recipientUserIds);
    }*/

}
