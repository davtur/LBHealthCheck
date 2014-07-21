/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.ejb.LocalBean;
import javax.ejb.Schedule;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Timer;
import javax.faces.application.FacesMessage;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.PathParam;
import org.primefaces.push.annotation.PushEndpoint;
import org.primefaces.push.impl.JSONEncoder;

/**
 *
 * @author david
 */
@PushEndpoint("/payments/{user}")

@Singleton
@LocalBean
@Startup
public class FutureMapEJB {

    private static final Logger logger = Logger.getLogger(FutureMapEJB.class.getName());
    private final HashMap<String, HashMap<String, Future>> futureMap = new HashMap<>();
    private final static String CHANNEL = "/payments/";

    @PathParam("user")
    private String username;

    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }

    /**
     * @param userSessionId
     * @return the futureMap
     */
    public HashMap<String, Future> getFutureMap(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        HashMap<String, Future> fmap = futureMap.get(userSessionId);
        if (fmap == null) {
            futureMap.put(userSessionId, new HashMap<String, Future>());
        }
        return fmap;
    }

    public void remove(String userSessionId, String key) {
        getFutureMap(userSessionId).remove(key);
    }

    public int size(String userSessionId) {
        return getFutureMap(userSessionId).size();
    }

    public Future get(String userSessionId, String key) {
        return (Future) getFutureMap(userSessionId).get(key);
    }

    public boolean containsKey(String userSessionId, String key) {
        return getFutureMap(userSessionId).containsKey(key);
    }

    /**
     * @param userSessionId
     * @param key
     * @param future
     *
     */
    public void put(String userSessionId, String key, Future future) {
        getFutureMap(userSessionId).put(key, future);
    }

    public void cancelFutures(String userSessionId) {
        if (getFutureMap(userSessionId) != null) {
            Iterator it = getFutureMap(userSessionId).entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry pairs = (Map.Entry) it.next();
                Future ft = (Future) pairs.getValue();
                ft.cancel(false);
                it.remove(); // avoids a ConcurrentModificationException
            }
            getFutureMap(userSessionId).clear();
        }
    }

    @PreDestroy
    private void cancelAllAsyncJobs() {

        for (Map.Entry pairs : futureMap.entrySet()) {
            cancelFutures((String) pairs.getKey());
        }
        futureMap.clear();
    }

    public void sendMessage(String sessionChannel, String summary, String detail) {
        //TODO
        // sessionChannel = "/test";// remove this once the channel is dynamically set by session id

        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        // eventBus.publish(channels.getChannel(getUser()), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
        eventBus.publish(CHANNEL + sessionChannel, new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }

    @Schedule(hour = "*", minute = "*", second = "0/1")
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 2 seconds

        logger.log(Level.FINE, "Checking Future Map for completed jobs.");

        for (Map.Entry pairs : futureMap.entrySet()) {
            String sessionId = (String) pairs.getKey();
            HashMap<String, Future> fmap = (HashMap<String, Future>) pairs.getValue();
            int k = fmap.size();
            if (k > 0) {

                logger.log(Level.INFO, "{0} jobs are running. Checking to see if asych jobs have finished so their results can be processed.", k);
                /*for (Map.Entry pairsFut : fmap.entrySet()) {
                 Future ft = (Future) pairsFut.getValue();
                 String key = (String) pairsFut.getKey();
                 if (ft.isDone()) {
                 sendMessage(sessionId, "Asynchronous Task Completed", key);
                 logger.log(Level.INFO, "Notifying sessionId {0} that async job {1} has finished.", new Object[]{key, sessionId});
                 }
                 }*/
                int y = 0;
                String details = "";
                for (Map.Entry pairsFut : fmap.entrySet()) {
                    Future ft = (Future) pairsFut.getValue();
                    String key = (String) pairsFut.getKey();
                    if (ft.isDone()) {
                        y++;
                        logger.log(Level.INFO, "SessionId {0} async job {1} has finished.", new Object[]{key, sessionId});
                        details += key + " ";
                    }
                }
                if (y > 0) {
                    sendMessage(sessionId, "Asynchronous Tasks Completed", details);
                    logger.log(Level.INFO, "Notifying that {0} async jobs for sessionId {1} have finished.", new Object[]{Integer.toString(y), sessionId});
                }

            }

        }
    }

    // run a schedules
}
