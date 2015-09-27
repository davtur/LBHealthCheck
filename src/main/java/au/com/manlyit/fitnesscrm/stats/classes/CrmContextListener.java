/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

/**
 *
 * @author david
 */
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextAttributeEvent;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;

import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

/**
 * Example listener for context-related application events, which were
 * introduced in the 2.3 version of the Servlet API. This listener merely
 * documents the occurrence of such events in the application log associated
 * with our servlet context.
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.3 $ $Date: 2006/10/12 14:31:30 $
 */
public final class CrmContextListener
        implements ServletContextAttributeListener, ServletContextListener {

    private static final Logger logger = Logger.getLogger(CrmContextListener.class.getName());

    // ----------------------------------------------------- Instance Variables
    /**
     * The servlet context with which we are associated.
     */
    private ServletContext context = null;
    //private MainThread main;
    private final ThreadGroup tGroup = new ThreadGroup("Operations");

    private class SimpleThreadFactory implements ThreadFactory {

        @Override
        public Thread newThread(Runnable r) {
            return new Thread(tGroup, r, "Operations Main Thread");
        }
    }
    ThreadFactory tf = new SimpleThreadFactory();
    private final ExecutorService exec = Executors.newSingleThreadExecutor(tf);

    // quartz
    public static final String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
    private boolean performShutdown = true;
    private Scheduler scheduler = null;

    // --------------------------------------------------------- Public Methods
    /**
     * Record the fact that a servlet context attribute was added.
     *
     * @param event The servlet context attribute event
     */
    @Override
    public void attributeAdded(ServletContextAttributeEvent event) {
        if (event != null) {
            log("attributeAdded('" + event.getName() + "', '"
                    + event.getValue() + "')");
        }
    }

    /**
     * Record the fact that a servlet context attribute was removed.
     *
     * @param event The servlet context attribute event
     */
    @Override
    public void attributeRemoved(ServletContextAttributeEvent event) {
       /* if (event != null) {
            String eventName = event.getName();
            String eventValue = "EMPTY";
            Object eventValObject = event.getValue();
            if(eventValObject != null){
                try {
                    eventValue = eventValObject.toString();
                } catch (Exception e) {
                }
                
            }
            
            if(eventName == null){
                eventName = "NULL";
            }
             if(eventValue == null){
                eventValue = "NULL";
            }
            try {
                log("attributeRemoved('" + eventName + "', '"
                        + eventValue + "')");
            } catch (Exception e) {
                logger.log(Level.INFO, "logging attributeRemoved event failed:",e);
            }
        }*/
    }

    /**
     * Record the fact that a servlet context attribute was replaced.
     *
     * @param event The servlet context attribute event
     */
    @Override
    public void attributeReplaced(ServletContextAttributeEvent event) {
        if (event != null) {

            log("attributeReplaced('" + event.getName() + "', '"
                    + event.getValue() + "')");
        }

    }

    /**
     * Record the fact that this web application has been destroyed.
     *
     * @param event The servlet context event
     */
    @Override
    public void contextDestroyed(ServletContextEvent event) {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FitnessStatsPU2");
        if (emf != null) {
            emf.close();
            System.out.println("EntityManagerFactory for unit FitnessStatsPU2 Closed");
        } else {
            System.out.println("EntityManagerFactory  for unit FitnessStatsPU2 was NULL no need to close.");
        }
        /*  EntityManagerFactory emf = (EntityManagerFactory) this.context.getAttribute("emf");
         if (emf != null) {
         emf.close();
         System.out.println("EntityManagerFactory Closed");
         } else {
         System.out.println("EntityManagerFactory was NULL no need to close.");
         }*/

        /*  try {
         main.setCancelled(true);
         exec.shutdown();
         exec.awaitTermination(30, TimeUnit.SECONDS);
         exec.shutdownNow();


         } catch (Exception exception) {
         System.out.println("Error sutting down main Thread in context listener" + exception.getMessage());
         }*/
        if (!performShutdown) {
            return;
        }

        try {
            if (scheduler != null) {
                scheduler.shutdown();
            }
        } catch (SchedulerException e) {
            logger.log(Level.INFO, "Quartz Scheduler failed to shutdown cleanly: {0}", e.toString());

        }

        System.out.println("Quartz Scheduler successful shutdown.");

        this.context.removeAttribute("Operations.Group");
        this.context.removeAttribute("MainThread");
        this.context = null;
        logger.log(Level.INFO, "contextDestroyed()");
        logger.log(Level.INFO, "CONTEXT DESTROYED -  class contextListener1");
    }

    /**
     * Record the fact that this web application has been initialized.
     *
     * @param event The servlet context event
     */
    @Override
    public void contextInitialized(ServletContextEvent event) {
        logger.log(Level.INFO, "INITIALISING CONTEXT with class contextListener1");
        this.context = event.getServletContext();
        logger.log(Level.INFO, "contextInitialized()");
        this.context.setAttribute("Operations.Group", tGroup);

        //ThreadGroup tGroup = Thread.currentThread().getThreadGroup();

        /* try {
         main = new MainThread();
         main.setServCtx(context);
         exec.submit(main);
         this.context.setAttribute("MainThread", main);
         } catch (RejectedExecutionException exception) {
         System.out.println(exception.getMessage());
         }*/
        StdSchedulerFactory factory;
        try {

            String configFile = context.getInitParameter("config-file");
            String shutdownPref = context.getInitParameter("shutdown-on-unload");

            if (shutdownPref != null) {
                performShutdown = Boolean.valueOf(shutdownPref).booleanValue();
            }

            // get Properties
            if (configFile != null) {
                factory = new StdSchedulerFactory(configFile);
            } else {
                factory = new StdSchedulerFactory();
            }

            // Always want to get the scheduler, even if it isn't starting,
            // to make sure it is both initialized and registered.
            scheduler = factory.getScheduler();

            // Should the Scheduler being started now or later
            String startOnLoad = context.getInitParameter("start-scheduler-on-load");

            int startDelay = 0;
            String startDelayS = context.getInitParameter("start-delay-seconds");
            try {
                if (startDelayS != null && startDelayS.trim().length() > 0) {
                    startDelay = Integer.parseInt(startDelayS);
                }
            } catch (NumberFormatException e) {
                logger.log(Level.INFO, "Cannot parse value of ''start-delay-seconds'' to an integer: {0}, defaulting to 5 seconds.", startDelayS);
                startDelay = 5;
            }

            /*
             * If the "start-scheduler-on-load" init-parameter is not specified,
             * the scheduler will be started. This is to maintain backwards
             * compatability.
             */
            if (startOnLoad == null || (Boolean.valueOf(startOnLoad).booleanValue())) {
                if (startDelay <= 0) {
                    // Start now
                    scheduler.start();
                    logger.log(Level.INFO, "Scheduler has been started...");
                } else {
                    // Start delayed
                    scheduler.startDelayed(startDelay);
                    logger.log(Level.INFO, "Scheduler will start in {0} seconds.", startDelay);
                }
            } else {
                System.out.println("Scheduler has not been started. Use scheduler.start()");
            }

            String factoryKey
                    = context.getInitParameter("servlet-context-factory-key");
            if (factoryKey == null) {
                factoryKey = QUARTZ_FACTORY_KEY;
            }

            logger.log(Level.INFO, "Storing the Quartz Scheduler Factory in the servlet context at key: {0}", factoryKey);
            context.setAttribute(factoryKey, factory);

        } catch (SchedulerException e) {
            logger.log(Level.WARNING, "Quartz Scheduler failed to initialize: {0}", e.toString());
        }
        logger.log(Level.INFO, "INITIALISING CONTEXT FINISHED with class contextListener1");
    }

    // -------------------------------------------------------- Private Methods
    /**
     * Log a message to the servlet context application log.
     *
     * @param message Message to be logged
     */
    private void log(String message) {

        if (context != null) {
            context.log("ContextListener: " + message);
        } else {
            logger.log(Level.INFO, "ContextListener: {0}", message);

        }

    }

    /**
     * Log a message and associated exception to the servlet context application
     * log.
     *
     * @param message Message to be logged
     * @param throwable Exception to be logged
     */
    private void log(String message, Throwable throwable) {

        if (context != null) {
            context.log("ContextListener: " + message, throwable);
        } else {
            logger.log(Level.INFO, "ContextListener: {0}", new Object[]{message, throwable});
        }

    }
}
