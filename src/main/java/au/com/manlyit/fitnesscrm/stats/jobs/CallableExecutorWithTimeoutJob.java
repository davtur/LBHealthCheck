/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.jobs;

import au.com.manlyit.fitnesscrm.stats.callable.CallableTaskResults;
import au.com.manlyit.fitnesscrm.stats.callable.CronJobCallableThreadFactory;
import au.com.manlyit.fitnesscrm.stats.classes.util.SendHTMLEmailWithFileAttached;
import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import org.quartz.JobDataMap;

/**
 *
 * @author david
 */
public class CallableExecutorWithTimeoutJob implements Job {

    public CallableExecutorWithTimeoutJob() {
    }
    private static int TEST_TYPE = 1;
    private String dbUsername = null;
    private String dbPassword = null;
    private String dbConnectURL = null;
    private Object jobClassToRun = null;
    private String jobTimeoutInMilli = "60000";
    private int jobType = 0;
    private static final Logger logger = Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName());
    private static EntityManager entityManager;

    @Override
    public void execute(JobExecutionContext context)
            throws JobExecutionException {
        EntityManagerFactory emf = Persistence.createEntityManagerFactory("FitnessStatsPU2");
        
        entityManager = emf.createEntityManager();

        if (entityManager == null) {

            logger.log(Level.SEVERE, "Entity Manager for PU  is NULL in {0}", this.getClass().getName());
        }

        String mess = "Executing cronjob callable with classname " + jobClassToRun + " with timeout set to " + jobTimeoutInMilli + " milliseconds";
        GregorianCalendar cal1 = new GregorianCalendar();
        long startTime = cal1.getTimeInMillis();
        java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, mess);
        ThreadFactory tf = new CronJobCallableThreadFactory();
        ExecutorService exec = Executors.newSingleThreadExecutor(tf);
        JobDataMap dataMap = context.getJobDetail().getJobDataMap();
        CallableTaskResults taskResults = new CallableTaskResults();

        Properties props = new Properties();

        props.put("mail.smtp.host", dataMap.get("mail.smtp.host"));
        props.put("mail.smtp.auth", dataMap.get("mail.smtp.auth"));
        props.put("mail.debug", dataMap.get("mail.debug"));
        props.put("mail.smtp.port", dataMap.get("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", dataMap.get("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", dataMap.get("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", dataMap.get("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", dataMap.get("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", dataMap.get("mail.smtp.sslpass"));

        Class cl = (Class) jobClassToRun;
        //try {
        //    cl = Class.forName(getJobClassToRun());
        // } catch (ClassNotFoundException ex) {
        //     Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "The class wasn't found", ex);
        // }
        Constructor construct = null;
        Callable callableClass = null;
        try {
            construct = cl.getConstructor(JobDataMap.class);
        } catch (NoSuchMethodException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "The class didn't have the contructor that takes a JobDataMap", ex);
        } catch (SecurityException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Couldn't access the constructor due to a security problem. Is it accessible??", ex);
        }
        try {
            callableClass = (Callable) construct.newInstance(dataMap);
        } catch (InstantiationException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Instantiation Exception", ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Illegal Access Exception", ex);
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Illegal Argument Exception", ex);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Invocation Target Exception", ex);
        }

        //HttpsLoginAndCheckWebpageCallable jobToExec = new HttpsLoginAndCheckWebpageCallable(dataMap);      // execute the job
        Future<CallableTaskResults> future1 = exec.submit(callableClass);
        int errorCode = 0;
        String errorMessage = "OK";
        int timeout = Integer.parseInt(jobTimeoutInMilli);
        try {
            taskResults = future1.get(timeout, TimeUnit.MILLISECONDS);
            //log the results in the db and send an email
        } catch (InterruptedException ex) {
            errorCode = 1;
            errorMessage = "Interrupted!!";
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Interrupted!!", ex);
        } catch (ExecutionException ex) {
            errorCode = 2;
            errorMessage = "Died whilst executing!!";
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.SEVERE, "Died whilst executing!!", ex);
        } catch (TimeoutException ex) {
            errorCode = 3;
            errorMessage = "Timed Out: " + jobTimeoutInMilli + " secs!!";
            Logger.getLogger(CallableExecutorWithTimeoutJob.class.getName()).log(Level.WARNING, "Timed Out: 120 secs", ex);
        }
        GregorianCalendar cal2 = new GregorianCalendar();
        long endTime = cal2.getTimeInMillis();
        long duration = (cal2.getTimeInMillis() - cal1.getTimeInMillis());
        String to = "david@manlyit.com.au";
        String cc = "david.turner@optus.com.au";
        String bcc = "david.turner@optus.com.au";
        String from = "david@manlyit.com.au";
        String subject = "Proactive Monitoring Alert";
        String msg = "";
        boolean success = false;
        if (errorCode == 0) {
            msg = taskResults.getResultData();
            success = taskResults.isIsSuccessful();
            if (success == false) {
                errorCode = 4;
                errorMessage = "Failed to find the string(s) that were expected!";
            }

        } else {
            msg = "The task Failed errorcode = " + errorCode + " , Error Message: " + errorMessage;
        }
        if (success) {
            msg = " SUCCESSFULL : duration = " + duration + "ms\r\n\r\n";
            Logger.getLogger(getClass().getName()).log(Level.INFO, msg);
            /*SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
             if (cc.trim().length() == 0) {
             cc = null;
             }
             try {
             emailAgent.send(to, cc, from, subject, msg, null, false);
             Logger.getLogger(getClass().getName()).log(Level.INFO, "Message Sent");
             } catch (Exception e) {
             String message = "There was a problem sending the email , to: " + to + ", cc: " + cc + ", from: " + from + ", subject: " + subject + ".The exception is: \r\n" + e.getMessage();
             Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
             }*/

        } else {

            msg = "The task Failed errorcode = " + errorCode + " , Error Message: " + errorMessage;
            msg += " FAILED : duration = " + duration + "ms\r\n\r\n" + msg;
            SendHTMLEmailWithFileAttached emailAgent = new SendHTMLEmailWithFileAttached();
            if (cc.trim().length() == 0) {
                cc = null;
            }
            try {
                emailAgent.send(to, cc,bcc,from, subject, msg, null,props, false);
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Message Sent");
            } catch (Exception e) {
                String message = "There was a problem sending the email , to: " + to + ", cc: " + cc + ", from: " + from + ", subject: " + subject + ".The exception is: \r\n" + e.getMessage();
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
            }

        }

        Connection con = null;
        PreparedStatement prepStmt = null;
        String insertQuery = "INSERT INTO website_monitor (id, test_type, result, duration, start_time, notify, job_to_run_on_fail, description) "
                + "VALUES ( ?,?,?,?, ?,?,?,?);";

        try {
            getEntityManager().getTransaction().begin();
            WebsiteMonitor wsm = new WebsiteMonitor();
            wsm.setId(0);
            wsm.setDuration(Integer.parseInt(Long.toString(duration)));
            wsm.setDuration2(Integer.parseInt(Long.toString(taskResults.getLongResult1())));
            wsm.setDuration3(Integer.parseInt(Long.toString(taskResults.getLongResult2())));
            wsm.setDuration4(Integer.parseInt(Long.toString(taskResults.getLongResult3())));
            wsm.setResult(errorCode);
            wsm.setDescription(errorMessage);
            wsm.setTestType(jobType);
            wsm.setStartTime(new Timestamp(startTime));
            wsm.setNotify(0);
            wsm.setJobToRunOnFail(0);
            getEntityManager().persist(wsm);
            getEntityManager().getTransaction().commit();

            /* con = getAMySqlDBConnection(dbConnectURL, dbUsername, dbPassword);
             prepStmt = con.prepareStatement(insertQuery);
             prepStmt.setInt(1, wsm.getId());
             prepStmt.setInt(2, wsm.getTestType());
             prepStmt.setInt(3, wsm.getResult());
             prepStmt.setInt(4, wsm.getDuration());
             prepStmt.setTimestamp(5, new Timestamp(wsm.getStartTime().getTime()));
             prepStmt.setInt(6, wsm.getNotify());
             prepStmt.setInt(7, wsm.getJobToRunOnFail());
             prepStmt.setString(8, wsm.getDescription());
             prepStmt.executeUpdate();
            
             } catch (SQLException e) {
             java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Test results", e);
             */
        } catch (Exception ex) {
            String message = "Attempting to persist WebsiteMonitor , exception: \r\n" + ex.getMessage();
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);
            //String message = "The query threw an exception : " + insertQuery + "\r\n" + ex.getMessage();
            try {
                getEntityManager().getTransaction().rollback();
            } catch (Exception e) {
                message = "Attempting to persist WebsiteMonitor but couldn't rollback transaction , exception: \r\n" + e.getMessage();
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message);

            }

        } finally {
            try {
                if (prepStmt != null) {
                    prepStmt.close();
                }
                prepStmt = null;

            } catch (Exception e) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Error:Couldn't close th eprepared statement!!", e);
            } finally {
                if (con != null) {
                    try {
                        con.close();
                    } catch (SQLException ex) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Closing DB connection", ex);
                    }
                }

                String msg2 = "Finished  cronjob callable with classname " + jobClassToRun + " with timeout set to " + jobTimeoutInMilli + " milliseconds ";
                if (success) {
                    msg2 = msg2 + " -- SUCCESSFULL : duration = " + duration + "ms";
                } else {
                    msg2 = msg2 + " -- FAILED : duration = " + duration + "ms\r\n" + msg;
                }
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.INFO, msg);

            }

        }
    }

    public EntityManager getEntityManager() {
        return entityManager;
    }

    private void persist(WebsiteMonitor wsm) {
        try {

            getEntityManager().getTransaction().begin();
            WebsiteMonitor wm = wsm;
            getEntityManager().persist(wsm);
            getEntityManager().getTransaction().commit();

        } catch (Exception e) {
            java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.SEVERE, "Could not persist Chart results", e);
        }
    }

    private synchronized Connection getAMySqlDBConnection(String connectUrl, String user, String pass) {
        Connection con = null;
        try {
            //String dbUrl = "jdbc:mysql://" + failFromServer + ":3306/" + db;
             java.util.Properties properties = new java.util.Properties();
            properties.setProperty("user", user);
            properties.setProperty("password", pass);
            properties.setProperty("useSSL", "false");
            properties.setProperty("autoReconnect", "true");
            try {
                Class.forName("com.mysql.jdbc.Driver").newInstance();
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt load the com.mysql.jdbc.Driver class\r\n {0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            try {
                con = DriverManager.getConnection(connectUrl, properties);
            } catch (SQLException ex) {

                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :{0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            } catch (Exception ex) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a connection :{0}\r\n{1}", new Object[]{connectUrl, ex.getMessage()});
            }
            if (con == null) {
                java.util.logging.Logger.getLogger(getClass().getName()).log(java.util.logging.Level.WARNING, "Couldnt get a Database Connection to {0}\r\n{1},{2}", new Object[]{connectUrl, user, pass});
            }
        } finally {
            return con;
        }
    }

    /**
     * @param dbUsername the dbUsername to set
     */
    public void setDbUsername(String dbUsername) {
        this.dbUsername = dbUsername;
    }

    /**
     * @param dbPassword the dbPassword to set
     */
    public void setDbPassword(String dbPassword) {
        this.dbPassword = dbPassword;
    }

    /**
     * @param dbConnectURL the dbConnectURL to set
     */
    public void setDbConnectURL(String dbConnectURL) {
        this.dbConnectURL = dbConnectURL;
    }

    /**
     * @return the jobClassToRun
     */
    public Object getJobClassToRun() {
        return jobClassToRun;
    }

    /**
     * @param jobClassToRun the jobClassToRun to set
     */
    public void setJobClassToRun(Object jobClassToRun) {
        this.jobClassToRun = jobClassToRun;
    }

    /**
     * @return the jobTimeoutInMilli
     */
    public String getJobTimeoutInMilli() {
        return jobTimeoutInMilli;
    }

    /**
     * @param jobTimeoutInMilli the jobTimeoutInMilli to set
     */
    public void setJobTimeoutInMilli(String jobTimeoutInMilli) {
        this.jobTimeoutInMilli = jobTimeoutInMilli;
    }

    /**
     * @return the jobType
     */
    public int getJobType() {
        return jobType;
    }

    /**
     * @param jobType the jobType to set
     */
    public void setJobType(int jobType) {
        this.jobType = jobType;
    }
}
