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

import java.io.Serializable;
import java.util.ResourceBundle;
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

    private QrtzJobDetails current;
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

    public void scheduleAllJobs() {
        //emailer job - checks the emailQueue for emails that have been scheduled to be sent
        JobDataMap jdm = new JobDataMap();
        jdm.put("dbUsername", configMapFacade.getConfig("db.fitness.username"));
        jdm.put("dbPassword", configMapFacade.getConfig("db.fitness.password"));
        jdm.put("dbConnectURL", configMapFacade.getConfig("db.fitness.url"));
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

        jdm3.put("dbUsername", configMapFacade.getConfig("db.fitness.username"));
        jdm3.put("dbPassword", configMapFacade.getConfig("db.fitness.password"));
        jdm3.put("dbConnectURL", configMapFacade.getConfig("db.fitness.url"));
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


        scheduleJob("0 0/2 * * * ?", "Login-and-webcheck", jdm3, CallableExecutorWithTimeoutJob.class); // fire every 2 minutes


        JobDataMap jdm4 = new JobDataMap();
        jdm4.put("dbUsername", configMapFacade.getConfig("db.fitness.username"));
        jdm4.put("dbPassword", configMapFacade.getConfig("db.fitness.password"));
        jdm4.put("dbConnectURL", configMapFacade.getConfig("db.fitness.url"));
        jdm4.put("jobClassToRun", renderMonitoringChartsCallable1.class);
        jdm4.put("jobTimeoutInMilli", "60000");
        jdm4.put("jobType", "2");

        scheduleJob("0 0/2 * * * ?", "render-Charts", jdm4, CallableExecutorWithTimeoutJob.class); // fire every 2 minute
        JobDataMap jdm5 = new JobDataMap();
        jdm5.put("dbUsername", configMapFacade.getConfig("db.fitness.username"));
        jdm5.put("dbPassword", configMapFacade.getConfig("db.fitness.password"));
        jdm5.put("dbConnectURL", configMapFacade.getConfig("db.fitness.url"));
        jdm5.put("jobClassToRun", HttpSimpleWebpageCheckCallable.class);
        jdm5.put("jobTimeoutInMilli", "60000");
        jdm5.put("jobType", "3");
        jdm5.put("successIfFound", "<html>");
        jdm5.put("url1", "http://www.google.com.au");

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

    private void scheduleJob(String cronString, String jobName, JobDataMap jdm, Class jobClass) {
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
                jobDetail = newJob(jobClass).withIdentity(jn, sched.DEFAULT_GROUP).usingJobData(jdm).build();
            } catch (Exception e) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Job Detail creation failed !", e);
            }
            CronTrigger trigger = newTrigger().withIdentity("Trigger_" + jobName, sched.DEFAULT_GROUP).withSchedule(cronSchedule(cronString)).usingJobData(jdm).build();
            sched.scheduleJob(jobDetail, trigger);
            //addSuccessMessage("Added job: " + jobDetail.getName());
        } catch (SchedulerException ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Scheduler Exception Occurred when scheduling quartz jobs:", ex);
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "An Exception Occurred when when scheduling quartz jobs:", ex);
        }
        Logger.getLogger(getClass().getName()).log(Level.INFO, "FINISHED Scheduling Quartz Jobs!");
        //ejbFacade.synchDBwithJPA();
        recreateModel();
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
