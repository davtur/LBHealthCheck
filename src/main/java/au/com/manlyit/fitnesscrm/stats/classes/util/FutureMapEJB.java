/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
import org.primefaces.push.EventBus.Reply;
import org.primefaces.push.EventBusFactory;
import org.primefaces.push.RemoteEndpoint;
import org.primefaces.push.annotation.OnClose;
import org.primefaces.push.annotation.OnMessage;
import org.primefaces.push.annotation.OnOpen;
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
    private static final int TIMEOUT_SECONDS = 30;
    private final ConcurrentHashMap<String, List<AsyncJob>> futureMap = new ConcurrentHashMap<>();
    private final static String CHANNEL = "/payments/";

    @PathParam("user")
    private String username;

    @OnMessage(encoders = {JSONEncoder.class})
    public FacesMessage onMessage(FacesMessage message) {
        return message;
    }

    @OnOpen
    public void onOpen(RemoteEndpoint r, EventBus e) {
        r.transport().toString();
        logger.log(Level.INFO, "Atmosphere Push Connection Opened. Transport Type = {0}", r.transport().toString());
    }

    @OnClose
    public void onClose(RemoteEndpoint r, EventBus e) {
        logger.log(Level.INFO, "Atmosphere Push Connection Closed.");

    }

    /**
     * @param userSessionId
     * @return the futureMap
     */
    public List<AsyncJob> getFutureMap(String userSessionId) {
        //return a map of future tasks that belong to a sessionid
        List<AsyncJob> fmap = futureMap.get(userSessionId);
        if (fmap == null) {
            futureMap.put(userSessionId, new ArrayList<AsyncJob>());
        }
        return fmap;
    }

    public void remove(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        for (int x = fmap.size(); x > 0; x--) {
            AsyncJob aj = fmap.get(x - 1);
            if (aj.getJobName().contentEquals(key)) {
                fmap.remove(x - 1);
            }
        }
    }

    public int size(String userSessionId) {
        return getFutureMap(userSessionId).size();
    }

    public AsyncJob get(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        for (int x = fmap.size(); x > 0; x--) {
            AsyncJob aj = fmap.get(x - 1);
            if (aj.getJobName().contentEquals(key)) {
                return aj;
            }
        }
        return null;
    }

    public boolean containsKey(String userSessionId, String key) {
        List<AsyncJob> fmap = getFutureMap(userSessionId);
        try {
            for (int x = fmap.size(); x > 0; x--) {
                AsyncJob aj = fmap.get(x - 1);
                if (aj.getJobName().contentEquals(key)) {
                    return true;
                }
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "futureMap.containsKey", e);
        }
        return false;

    }

    /**
     * @param userSessionId
     *
     *
     * @param aj
     *
     */
    public void put(String userSessionId, AsyncJob aj) {
        getFutureMap(userSessionId).add(aj);
    }

    public void cancelFutures(String userSessionId) {
        if (getFutureMap(userSessionId) != null) {
            List<AsyncJob> fmap = getFutureMap(userSessionId);
            for (AsyncJob aj : fmap) {
                Future ft = (Future) aj.getFuture();
                ft.cancel(false);
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
        final String broadcastChannel = CHANNEL + sessionChannel;
        final String summ = summary;
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
       /* Reply rep = new EventBus.Reply() {
            @Override
            public void completed(String message) {

                logger.log(Level.INFO, "Message Delivered:Channel={0}, Summary={1}.", new Object[]{broadcastChannel, summ});
            }
        };*/
        // eventBus.publish(channels.getChannel(getUser()), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
        eventBus.publish(broadcastChannel, new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }

    @Schedule(hour = "*", minute = "*", second = "*")
    public void checkRunningJobsAndNotifyIfComplete(Timer t) {  // run every 2 seconds

        logger.log(Level.FINE, "Checking Future Map for completed jobs.");

        for (Map.Entry pairs : futureMap.entrySet()) {
            String sessionId = (String) pairs.getKey();
            ArrayList<AsyncJob> fmap = (ArrayList<AsyncJob>) pairs.getValue();
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
                for (AsyncJob aj : fmap) {
                    Future ft = aj.getFuture();
                    String key = aj.getJobName();
                    if (ft.isDone()) {
                        y++;
                        logger.log(Level.INFO, "SessionId {0} async job {1} has finished.", new Object[]{key, sessionId});
                        details += key + " ";
                    }
                    GregorianCalendar jobStartTime = new GregorianCalendar();
                    GregorianCalendar currentTime = new GregorianCalendar();

                    jobStartTime.setTime(aj.getStartTime());
                    jobStartTime.add(Calendar.SECOND, TIMEOUT_SECONDS);
                    if (jobStartTime.compareTo(currentTime) < 0) {
                        ft.cancel(true);
                        logger.log(Level.INFO, "SessionId {0} async job {1} has timed out ({2} seconds )  and been cancelled.", new Object[]{key, sessionId, TIMEOUT_SECONDS});
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
