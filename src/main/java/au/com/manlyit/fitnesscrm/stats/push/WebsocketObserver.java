package au.com.manlyit.fitnesscrm.stats.push;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.event.Observes;
import javax.faces.event.WebsocketEvent;
import javax.faces.event.WebsocketEvent.Closed;
import javax.faces.event.WebsocketEvent.Opened;

@ApplicationScoped
public class WebsocketObserver {

    private static final Logger LOG = Logger.getLogger(WebsocketObserver.class.getName());

    public void onOpen(@Observes @Opened WebsocketEvent event) {
        LOG.log(Level.INFO, "Opened connection {0} from {1}", new Object[]{event.getChannel(), event.getUser()});
    }

    public void onClose(@Observes @Closed WebsocketEvent event) {
        String channel = event.getChannel();
        LOG.log(Level.INFO, "Channel {0} was successfully closed!", channel);
        LOG.log(Level.INFO, "Closed connection {0} from {1} with code {2}", new Object[]{event.getChannel(), event.getUser(), event.getCloseCode()});
    }
}
