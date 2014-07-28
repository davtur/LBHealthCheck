package au.com.manlyit.fitnesscrm.stats.classes;

import au.com.manlyit.fitnesscrm.stats.callable.HttpSimpleWebpageCheckCallable;
import au.com.manlyit.fitnesscrm.stats.callable.renderMonitoringChartsCallable1;
import au.com.manlyit.fitnesscrm.stats.callable.HttpsLoginAndCheckWebpageCallable;
import au.com.manlyit.fitnesscrm.stats.jobs.CallableExecutorWithTimeoutJob;
import au.com.manlyit.fitnesscrm.stats.jobs.sshJob1;
import au.com.manlyit.fitnesscrm.stats.jobs.EmailerJob;
import org.quartz.Trigger;
import java.util.Date;

import au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetails;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.PaginationHelper;
import au.com.manlyit.fitnesscrm.stats.beans.QrtzJobDetailsFacade;
import au.com.manlyit.fitnesscrm.stats.db.ConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.JobConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.Tasks;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Named;
import javax.enterprise.context.SessionScoped;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.convert.Converter;
import javax.faces.convert.FacesConverter;
import javax.faces.model.DataModel;
import javax.faces.model.ListDataModel;
import javax.faces.model.SelectItem;
import javax.servlet.ServletContext;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;

import static org.quartz.TriggerBuilder.*;
import static org.quartz.JobBuilder.*;
import static org.quartz.CronScheduleBuilder.*;
import org.quartz.JobKey;

/*
 * 
 *
 * import static org.quartz.TriggerBuilder.*;
 import static org.quartz.JobBuilder.*;
 import static org.quartz.CronScheduleBuilder.*;
 import static org.quartz.TriggerKey.*;
 import static org.quartz.JobKey.*;
 import static org.quartz.SimpleScheduleBuilder.*;
 import static org.quartz.DateBuilder.*;
 import static org.quartz.impl.matchers.GroupMatcher.*;

 * 
 */
@Named("qrtzJobDetailsController")
@SessionScoped
public class QrtzJobDetailsController implements Serializable {

    private static final String ETAG = "{!-ENCRYPT-!}";
    private QrtzJobDetails current;
    private QrtzJobDetails[] multiSelected;
    private List<QrtzJobDetails> filteredItems;
    private QrtzJobDetails selectedForDeletion;
    private DataModel items = null;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.QrtzJobDetailsFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    private PaginationHelper pagination;
    private int selectedItemIndex;

    public QrtzJobDetailsController() {
    }

    public QrtzJobDetails getSelected() {
        if (current == null) {
            current = new QrtzJobDetails();
            selectedItemIndex = -1;
        }
        return current;
    }

    private QrtzJobDetailsFacade getFacade() {
        return ejbFacade;
    }

    public PaginationHelper getPagination() {
        if (pagination == null) {
            pagination = new PaginationHelper(1000000) {

                @Override
                public int getItemsCount() {
                    return getFacade().count();
                }

                @Override
                public DataModel createPageDataModel() {
                    return new ListDataModel(getFacade().findRange(new int[]{getPageFirstItem(), getPageFirstItem() + getPageSize()}));
                }
            };
        }
        return pagination;
    }

    public JobDataMap addEmailParametersToJdm(JobDataMap jdm) {
        jdm.put("dbUsername", configMapFacade.getConfig("db.fitness.username"));
        jdm.put("dbPassword", configMapFacade.getConfig("db.fitness.password"));
        jdm.put("dbConnectURL", configMapFacade.getConfig("db.fitness.url"));
        jdm.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        jdm.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        jdm.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        jdm.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        jdm.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        jdm.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        jdm.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        jdm.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        jdm.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));

        return jdm;
    }

    public void rescheduleAllJobs() {
        removeAllJobs();

        scheduleAllJobs();
    }

    public String scheduleTask(Tasks tsk, boolean runImmediately) {
        String mess = tsk.getName() + " task scheduled successfully";
        JobDataMap jdm = new JobDataMap();
        Thread t = Thread.currentThread();
        ClassLoader cl = t.getContextClassLoader();
//Class toRun = cl.loadClass(args[0]);

        Class jobClass;
        Class callableJobClassToRun;
        try {
            jobClass = cl.loadClass(tsk.getTaskClassName());
        } catch (ClassNotFoundException ex) {

            String m = tsk.getName() + ". Error : Class not found. ClassName:" + tsk.getTaskClassName() + "Classpath = " + System.getProperty("java.class.path");
            Logger.getLogger(QrtzJobDetailsController.class.getName()).log(Level.SEVERE, m, ex);
            return m;
        }
        Collection<JobConfigMap> params = tsk.getJobConfigMapCollection();
        for (JobConfigMap jcm : params) {
            String k;
            String v;
            ConfigMap cm = jcm.getConfigMapKey();
            if (cm == null) {
                k = jcm.getBasicKey();
                v = jcm.getBasicValue();
            } else {
                k = jcm.getBasicKey();
                if (k.matches("configKey")) {
                    k = cm.getConfigkey();
                }
                v = cm.getConfigvalue();
                // check for encrypted value
                if (v.indexOf(ETAG) == 0) {
                    v = configMapFacade.getConfig(k);
                }
            }

            if (k.contains("jobClassToRun")) {

                try {
                    callableJobClassToRun = cl.loadClass(v);
                } catch (ClassNotFoundException ex) {
                    String m = tsk.getName() + ". Error : jobClassToRun Callable Class not found. ClassName:" + v + ", Classpath = " + System.getProperty("java.class.path");
                    Logger.getLogger(QrtzJobDetailsController.class.getName()).log(Level.SEVERE, null, ex);
                    return m;
                }
                jdm.put(k, callableJobClassToRun);
            } else {
                jdm.put(k, v);
            }
        }
        boolean result;
        if (runImmediately == true) {
            result = scheduleJob(null, tsk.getName(), jdm, jobClass);
        } else {
            result = scheduleJob(tsk.getCronEntry(), tsk.getName(), jdm, jobClass);
        }
        if (result == false)// something broke
        {
            mess = tsk.getName() + " task !! FAILED !! to be scheduled! Check the log for details.";
        }
        return mess;
    }

    public void scheduleAllJobs() {
        //emailer job - checks the emailQueue for emails that have been scheduled to be sent
        JobDataMap jdm = new JobDataMap();

        jdm = addEmailParametersToJdm(jdm);

        scheduleJob("0 0/5 * * * ?", "Emailer", jdm, EmailerJob.class); // fire every 5 minutes

        JobDataMap jdm2 = new JobDataMap();
        int tout = 5000;
        jdm2.put("username", "david");
        jdm2.put("passphrase", "tar5ha");
        jdm2.put("host", "192.168.0.103");
        jdm2.put("timeout", tout);
        jdm2.put("key", "/home/david/.ssh/id_dsa");
        String cmd = "echo " + '`' + "date" + '`' + " > testfile123.txt\n";
        jdm2.put("command", cmd);

        scheduleJob("0 0/2 * * * ?", "SSH", jdm2, sshJob1.class); // fire every 5 minutes
        JobDataMap jdm3 = new JobDataMap();

        jdm3.put("jobClassToRun", HttpsLoginAndCheckWebpageCallable.class);
        jdm3.put("jobTimeoutInMilli", "60000");
        jdm3.put("jobType", "1");
        jdm3.put("loginURL1", "https://memberservices.optuszoo.com.au/login/");
        jdm3.put("loginURL2", "https://idp.optusnet.com.au/idp/optus/Authn/Service/ISP");
        jdm3.put("loginReferer", "https://idp.optusnet.com.au/idp/optus/Authn/Service/ISP?spEntityID=https%3A%2F%2Fmemberservices.optuszoo.com.au%2Fshibboleth");
        jdm3.put("loginPostQuery", "spEntityID=https%3A%2F%2Fmemberservices.optuszoo.com.au%2Fshibboleth&j_username=david.turner10&j_password=Surf2day%21%21&rememberMe=on&j_principal_type=ISP&j_security_check=true");
        jdm3.put("testURL1", "https://memberservices.optuszoo.com.au/myusage/");
        jdm3.put("testOKCriteria1", "id=\"planData\"");
        jdm3.put("testOKCriteria2", "<!--- PLAN NAME SECTION --->");
        jdm3.put("logoutURL1", "https://memberservices.optuszoo.com.au/?logout=1");
        jdm3.put("userAgent", "Mozilla/5.0 (X11; Linux x86_64; rv:7.0.1) Gecko/20100101 Firefox/7.0.1");

        jdm3 = addEmailParametersToJdm(jdm3);
        scheduleJob("0 0/2 * * * ?", "Login-and-webcheck", jdm3, CallableExecutorWithTimeoutJob.class); // fire every 2 minutes

        JobDataMap jdm4 = new JobDataMap();

        jdm4.put("jobClassToRun", renderMonitoringChartsCallable1.class);
        jdm4.put("jobTimeoutInMilli", "60000");
        jdm4.put("jobType", "2");

        jdm4 = addEmailParametersToJdm(jdm4);
        scheduleJob("0 0/2 * * * ?", "render-Charts", jdm4, CallableExecutorWithTimeoutJob.class); // fire every 2 minute

        JobDataMap jdm5 = new JobDataMap();
        jdm5.put("jobClassToRun", HttpSimpleWebpageCheckCallable.class);
        jdm5.put("jobTimeoutInMilli", "60000");
        jdm5.put("jobType", "3");
        jdm5.put("successIfFound", "<html>");
        jdm5.put("url1", "http://www.google.com.au");
        jdm5 = addEmailParametersToJdm(jdm5);
        scheduleJob("0 0/2 * * * ?", "google-check", jdm5, CallableExecutorWithTimeoutJob.class); // fire every 2 minute

    }

    public void removeAllJobs() {
        JsfUtil.addSuccessMessage("Removing all jobs");
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "Scheduling Generic SQL Executor Quartz Job!");

        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            //FacesContext.getCurrentInstance().getExternalContext();
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);

        }
        Scheduler sched = null;
        try {
            sched = factory.getScheduler();
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
        }

        try {
            sched.clear();
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't clear all jobs, triggers and calendars using the Scheduler.clear() command", ex);
            JsfUtil.addErrorMessage(ex, "AN error occurred trying to remove all Jobs, Triggers and Calenders");
        }
        //ejbFacade.synchDBwithJPA();
        recreateModel();
    }

    public void pauseSelected() {
        int c = 0;
        int e = 0;
        String responseMessage = "An Error occurred pausing the job. Check logs for details!";

        for (QrtzJobDetails tsk : getMultiSelected()) {

            boolean ret = pauseJob(tsk.getQrtzJobDetailsPK().getJobName(), tsk.getQrtzJobDetailsPK().getJobGroup());
            if (ret == false) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Task failed to pause!");

                e++;
            } else {
                c++;
            }

        }
        String m = "Paused " + c + " tasks successfully. ";
        if (e > 0) {
            m += " " + e + " Failed." + responseMessage;
        }
        JsfUtil.addSuccessMessage(m);
    }

    public void cancelSelected() {
        int c = 0;
        int e = 0;
        String responseMessage = "An Error occurred cancelling the job. Check logs for details!";

        for (QrtzJobDetails tsk : getMultiSelected()) {

            boolean ret = cancelJob(tsk.getQrtzJobDetailsPK().getJobName(), tsk.getQrtzJobDetailsPK().getJobGroup());
            if (ret == false) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Task failed to cancel!");

                e++;
            } else {
                c++;
            }

        }
        String m = "Cancelled " + c + " tasks successfully. ";
        if (e > 0) {
            m += " " + e + " Failed." + responseMessage;
        }
        JsfUtil.addSuccessMessage(m);
    }

    public void resumeSelected() {
        int c = 0;
        int e = 0;
        String responseMessage = "An Error occurred resuming the job. Check logs for details!";

        for (QrtzJobDetails tsk : getMultiSelected()) {

            boolean ret = resumeJob(tsk.getQrtzJobDetailsPK().getJobName(), tsk.getQrtzJobDetailsPK().getJobGroup());
            if (ret == false) {
                Logger.getLogger(getClass().getName()).log(Level.INFO, "Task failed to resume!");

                e++;
            } else {
                c++;
            }

        }
        String m = "Resumed " + c + " tasks successfully. ";
        if (e > 0) {
            m += " " + e + " Failed." + responseMessage;
        }
        JsfUtil.addSuccessMessage(m);
    }

    private boolean cancelJob(String jobName, String jobGroup) {
        boolean retValue = false;

        JsfUtil.addSuccessMessage("Removing job named: " + jobName);
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "Cancelling Generic SQL Executor Quartz Job!");

        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            //FacesContext.getCurrentInstance().getExternalContext();
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);

        }
        Scheduler sched = null;
        try {
            sched = factory.getScheduler();
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
        }

        try {
            JobKey job = new JobKey(jobName, jobGroup);
            List<Trigger> jobTriggers = (List<Trigger>) sched.getTriggersOfJob(job);
            for (Trigger tr : jobTriggers) {
                sched.unscheduleJob(tr.getKey());
            }
            retValue = true;
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't cancel job all jobs", ex);
            JsfUtil.addErrorMessage(ex, "An error occurred trying to cancel the job!");
        }
        //ejbFacade.synchDBwithJPA();
        recreateModel();

        return retValue;

    }

    private boolean resumeJob(String jobName, String jobGroup) {
        boolean retValue = false;

        JsfUtil.addSuccessMessage("Resuming job named: " + jobName);
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "Resume Generic SQL Executor Quartz Job!");

        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            //FacesContext.getCurrentInstance().getExternalContext();
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);

        }
        Scheduler sched = null;
        try {
            sched = factory.getScheduler();
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
        }

        try {
            JobKey job = new JobKey(jobName, jobGroup);
            List<Trigger> jobTriggers = (List<Trigger>) sched.getTriggersOfJob(job);
            for (Trigger tr : jobTriggers) {
                sched.resumeTrigger(tr.getKey());
            }
            retValue = true;

        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't resume job all jobs", ex);
            JsfUtil.addErrorMessage(ex, "An error occurred trying to resume the job!");
        }
        //ejbFacade.synchDBwithJPA();
        recreateModel();

        return retValue;

    }

    private boolean pauseJob(String jobName, String jobGroup) {
        boolean retValue = false;

        JsfUtil.addSuccessMessage("Paused job named: " + jobName + " in group " + jobGroup + " .");
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();

        Logger.getLogger(getClass().getName()).log(Level.INFO, "Paused Generic SQL Executor Quartz Job!");

        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            //FacesContext.getCurrentInstance().getExternalContext();
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);

        }
        Scheduler sched = null;
        try {
            sched = factory.getScheduler();
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
        }

        try {
            JobKey job = new JobKey(jobName, jobGroup);
            List<Trigger> jobTriggers = (List<Trigger>) sched.getTriggersOfJob(job);
            for (Trigger tr : jobTriggers) {
                sched.pauseTrigger(tr.getKey());
            }
            retValue = true;
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't Pause job ", ex);
            JsfUtil.addErrorMessage(ex, "An error occurred trying to Pause the job!");
        }
        //ejbFacade.synchDBwithJPA();
        recreateModel();

        return retValue;

    }

    private boolean scheduleJob(String cronString, String jobName, JobDataMap jdm, Class jobClass) {
        boolean result = true;
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Scheduling Quartz Jobs!");
        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);
        }
        if (factory == null) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Quartz factory is NULL. Quartz may not have initialised due to DB issues. Check the server log for more info!");
            return false;
        }
        try {
            Scheduler sched;
            try {
                sched = factory.getScheduler();
            } catch (SchedulerException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
                return false;
            }
            String jn = "Job_" + jobName;
            JobDetail jobDetail = null;
            try {
                jobDetail = newJob(jobClass).withIdentity(jn, sched.DEFAULT_GROUP).usingJobData(jdm).build();
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Job Detail creation failed !", e);
                result = false;
            }
            CronTrigger cronTrigger = null;
            Trigger trigger = null;
            if (cronString != null && cronString.trim().isEmpty() == false) {
                cronTrigger = newTrigger().withIdentity("Trigger_" + jobName, sched.DEFAULT_GROUP).withSchedule(cronSchedule(cronString)).usingJobData(jdm).build();
                sched.scheduleJob(jobDetail, cronTrigger);
            } else {
                long startTime = System.currentTimeMillis() + 2000L;
                trigger = newTrigger().withIdentity("trigger1", "group1").startAt(new Date(startTime)).build();
                sched.scheduleJob(jobDetail, trigger);

            }

            //addSuccessMessage("Added job: " + jobDetail.getName());
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Scheduler Exception Occurred when scheduling quartz jobs:", ex);
            result = false;
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "An Exception Occurred when when scheduling quartz jobs:", ex);
            result = false;
        }
        Logger.getLogger(getClass().getName()).log(Level.INFO, "FINISHED Scheduling Quartz Jobs!");
        //ejbFacade.synchDBwithJPA();
        recreateModel();
        return result;
    }

    public void scheduledFireOnce(String jobName, Date fireDate, JobDataMap jdm, Class jobClass) {
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Scheduling Quartz Jobs!");
        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);
        }
        if (factory != null) {
            try {
                Scheduler sched = null;
                try {
                    sched = factory.getScheduler();
                } catch (SchedulerException ex) {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
                }
                if (sched != null) {
                    String jn = "Job_" + jobName;
                    JobDetail jobDetail = null;
                    try {
                        jobDetail = newJob(jobClass).withIdentity(jn, "DEFAULT").usingJobData(jdm).build();
                    } catch (Exception e) {
                        Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Job Detail creation failed !", e);
                    }
                    Trigger trigger = newTrigger().withIdentity("trigger1", "group1").startAt(fireDate).build();
                    sched.scheduleJob(jobDetail, trigger);
                    //addSuccessMessage("Added job: " + jobDetail.getName());
                } else {
                    Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Scheduler is NULL:");
                }
            } catch (SchedulerException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Scheduler Exception Occurred when scheduling quartz jobs:", ex);
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "An Exception Occurred when when scheduling quartz jobs:", ex);
            }
        } else {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Factory is NULL:");
        }
        Logger.getLogger(getClass().getName()).log(Level.INFO, "FINISHED Scheduling Quartz Jobs!");
    }

    private void immediateJob(String jobName, JobDataMap jdm, Class jobClass) {
        ServletContext sc = (ServletContext) FacesContext.getCurrentInstance().getExternalContext().getContext();
        Logger.getLogger(getClass().getName()).log(Level.INFO, "Scheduling Quartz Jobs!");
        StdSchedulerFactory factory = null;
        String QUARTZ_FACTORY_KEY = "org.quartz.impl.StdSchedulerFactory.KEY";
        try {
            factory = (StdSchedulerFactory) sc.getAttribute(QUARTZ_FACTORY_KEY);
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not get the QUARTZ_FACTORY_KEY from the servlet context OR the context is null ", e);
        }
        try {
            Scheduler sched = null;
            try {
                sched = factory.getScheduler();
            } catch (SchedulerException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Could not obtain the quartz scheduler from the SchedulerFactory or the factory is null!", ex);
            }
            String jn = "Job_" + jobName;
            JobDetail jobDetail = null;
            try {
                jobDetail = newJob(jobClass).withIdentity(jn, "DEFAULT").usingJobData(jdm).build();
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Job Detail creation failed !", e);
            }
            long startTime = System.currentTimeMillis() + 2000L;
            Trigger trigger = newTrigger().withIdentity("trigger1", "group1").startAt(new Date(startTime)).build();

            sched.scheduleJob(jobDetail, trigger);
            //addSuccessMessage("Added job: " + jobDetail.getName());
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Scheduler Exception Occurred when scheduling quartz jobs:", ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "An Exception Occurred when when scheduling quartz jobs:", ex);
        }
        Logger.getLogger(getClass().getName()).log(Level.INFO, "FINISHED Scheduling Quartz Jobs!");
    }

    public String prepareList() {
        recreateModel();
        return "List";
    }

    public static boolean isUserInRole(String roleName) {
        boolean inRole = false;
        inRole = FacesContext.getCurrentInstance().getExternalContext().isUserInRole(roleName);
        return inRole;

    }

    public String prepareView() {
        current = (QrtzJobDetails) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "View";
    }

    public String prepareCreate() {
        current = new QrtzJobDetails();
        selectedItemIndex = -1;
        return "Create";
    }

    public String create() {
        try {
            getFacade().create(current);

            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzJobDetailsCreated"));
            return prepareCreate();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String prepareEdit() {
        current = (QrtzJobDetails) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        return "Edit";
    }

    public String update() {
        try {
            getFacade().edit(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzJobDetailsUpdated"));
            return "View";
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
            return null;
        }
    }

    public String destroy() {
        current = (QrtzJobDetails) getItems().getRowData();
        selectedItemIndex = pagination.getPageFirstItem() + getItems().getRowIndex();
        performDestroy();
        recreateModel();
        return "List";
    }

    public String destroyAndView() {
        performDestroy();
        recreateModel();
        updateCurrentItem();
        if (selectedItemIndex >= 0) {
            return "View";
        } else {
            // all items were removed - go back to list
            recreateModel();
            return "List";
        }
    }

    private void performDestroy() {
        try {
            getFacade().remove(current);
            JsfUtil.addSuccessMessage(configMapFacade.getConfig("QrtzJobDetailsDeleted"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
    }

    private void updateCurrentItem() {
        int count = getFacade().count();
        if (selectedItemIndex >= count) {
            // selected index cannot be bigger than number of items:
            selectedItemIndex = count - 1;
            // go to previous page if last page disappeared:
            if (pagination.getPageFirstItem() >= count) {
                pagination.previousPage();
            }
        }
        if (selectedItemIndex >= 0) {
            current = getFacade().findRange(new int[]{selectedItemIndex, selectedItemIndex + 1}).get(0);
        }
    }

    public DataModel getItems() {
        if (items == null) {
            items = getPagination().createPageDataModel();
        }
        return items;
    }

    private void recreateModel() {
        items = null;
        filteredItems = null;
    }

    public String next() {
        getPagination().nextPage();
        recreateModel();
        return "List";
    }

    public String previous() {
        getPagination().previousPage();
        recreateModel();
        return "List";
    }

    public SelectItem[] getItemsAvailableSelectMany() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), false);
    }

    public SelectItem[] getItemsAvailableSelectOne() {
        return JsfUtil.getSelectItems(ejbFacade.findAll(), true);
    }

    /**
     * @return the multiSelected
     */
    public QrtzJobDetails[] getMultiSelected() {
        return multiSelected;
    }

    /**
     * @param multiSelected the multiSelected to set
     */
    public void setMultiSelected(QrtzJobDetails[] multiSelected) {
        this.multiSelected = multiSelected;
    }

    /**
     * @return the filteredItems
     */
    public List<QrtzJobDetails> getFilteredItems() {
        return filteredItems;
    }

    /**
     * @param filteredItems the filteredItems to set
     */
    public void setFilteredItems(List<QrtzJobDetails> filteredItems) {
        this.filteredItems = filteredItems;
    }

    /**
     * @return the selectedForDeletion
     */
    public QrtzJobDetails getSelectedForDeletion() {
        return selectedForDeletion;
    }

    /**
     * @param selectedForDeletion the selectedForDeletion to set
     */
    public void setSelectedForDeletion(QrtzJobDetails selectedForDeletion) {
        this.selectedForDeletion = selectedForDeletion;
        current = selectedForDeletion;

        performDestroy();
        recreateModel();
    }

    @FacesConverter(forClass = QrtzJobDetails.class)
    public static class QrtzJobDetailsControllerConverter implements Converter {

        private static final String SEPARATOR = "#";
        private static final String SEPARATOR_ESCAPED = "\\#";

        public Object getAsObject(FacesContext facesContext, UIComponent component, String value) {
            if (value == null || value.length() == 0) {
                return null;
            }
            QrtzJobDetailsController controller = (QrtzJobDetailsController) facesContext.getApplication().getELResolver().
                    getValue(facesContext.getELContext(), null, "qrtzJobDetailsController");
            return controller.ejbFacade.find(getKey(value));
        }

        au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetailsPK getKey(String value) {
            au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetailsPK key;
            String values[] = value.split(SEPARATOR_ESCAPED);
            key = new au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetailsPK();
            key.setSchedName(values[0]);
            key.setJobName(values[1]);
            key.setJobGroup(values[2]);
            return key;
        }

        String getStringKey(au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetailsPK value) {
            StringBuffer sb = new StringBuffer();
            sb.append(value.getSchedName());
            sb.append(SEPARATOR);
            sb.append(value.getJobName());
            sb.append(SEPARATOR);
            sb.append(value.getJobGroup());
            return sb.toString();
        }

        public String getAsString(FacesContext facesContext, UIComponent component, Object object) {
            if (object == null) {
                return null;
            }
            if (object instanceof QrtzJobDetails) {
                QrtzJobDetails o = (QrtzJobDetails) object;
                return getStringKey(o.getQrtzJobDetailsPK());
            } else {
                throw new IllegalArgumentException("object " + object + " is of type " + object.getClass().getName() + "; expected type: " + QrtzJobDetailsController.class.getName());
            }
        }
    }
}
