/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.event.ActionEvent;
import javax.inject.Inject;
import javax.inject.Named;
import au.com.manlyit.fitnesscrm.stats.classes.util.FutureMapEJB;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import javax.faces.context.FacesContext;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;

/**
 *
 * @author david
 */
@Named("applicationBean")
@ApplicationScoped
public class ApplicationBean implements Serializable {

    private static final long serialVersionUID = 1L;

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbCustomerImagesFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomersFacade;
    @Inject
    private FutureMapEJB futureMap;
    private static final Logger logger = Logger.getLogger(ApplicationBean.class.getName());

    private ArrayList<Map<String, Date>> ips = new ArrayList<>();
    private final ConcurrentHashMap<String, String> facebookLogingStateMap = new ConcurrentHashMap<>();

    public ApplicationBean() {
    }

    @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "ApplicationBean Created");
        logger.log(Level.INFO, "JSF Package Version = ", FacesContext.class.getPackage().getImplementationVersion());
        //sanityCheckCustomersForDefaultItems();
    }

    public boolean validateIP(String ipAddress) {
        //TODO search ip list and check if they have already tried to register in the past hour

        return true;
    }

    public void testFunction(ActionEvent event) {
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ EXECUTING TEST FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        futureMap.updateCustomerPaymentSchedules();
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ COMPLETED TEST FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }

    public void testFunction2(ActionEvent event) {
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ EXECUTING TEST TICKETS FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        futureMap.issueWeeklyCustomerTicketsForPlansSessionBookings();
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ COMPLETED TEST TICKETS FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }
    
     public void testReport(ActionEvent event) {
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ EXECUTING TEST REPORT FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
        futureMap.sendDailyReportEmail();
        logger.log(Level.INFO, "@@@@@@@@@@@@@@@@@@@@@@@@ COMPLETED TEST REPORT FUNCTION @@@@@@@@@@@@@@@@@@@@@@@@@@@@@@");
    }

    public void addFacebookLoginState(String key, String value) {
        facebookLogingStateMap.put(key, value);
        cleanUpOldLoginState();
    }

    public String getFacebookLoginState(String key) {
        return facebookLogingStateMap.get(key);
    }

    public String removeFacebookLoginState(String key) {
        return facebookLogingStateMap.remove(key);
    }
    
    public String getApplicationVersion() {
         MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        String version = "Failed To retrieve version!";
        try {
            model = reader.read(this.getClass().getResourceAsStream("/META-INF/maven/au.com.manlyit/FitnessCRM/pom.xml"));
        } catch (IOException ex) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion IOException ", ex);
        } catch (XmlPullParserException ex) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion XmlPullParserException", ex);
        }
        if(model != null){
            version = model.getVersion();
        }
        //System.out.println(model.getId());
       // System.out.println(model.getGroupId());
       // System.out.println(model.getArtifactId());
      //  System.out.println(model.getVersion());
        
        
        return version;
    }

    private void cleanUpOldLoginState() {
        ArrayList<String> keysToRemove = new ArrayList<>();
        long timeStamp1hourOld = (new Date()).getTime() - 3600000;
        try {
            for (Iterator<Entry<String, String>> iter = facebookLogingStateMap.entrySet().iterator(); iter.hasNext();) {
                Entry<String, String> entry = iter.next();
                long ts = Long.parseLong(entry.getValue());
                if (ts < timeStamp1hourOld) {
                    keysToRemove.add(entry.getKey());
                }
            }
            for (String key : keysToRemove) {
                facebookLogingStateMap.remove(key);
            }
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "cleanUpOldLoginState numberFormatException", numberFormatException.getMessage());
        }

    }
}
