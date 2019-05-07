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
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.ConcurrentHashMap;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;

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
    private boolean customerEmailsEnabled = true;
    private String version;

    private ArrayList<Map<String, Date>> ips = new ArrayList<>();
    private final ConcurrentHashMap<String, String> facebookLogingStateMap = new ConcurrentHashMap<>();

    public ApplicationBean() {
    }

    @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "ApplicationBean Created");
        logger.log(Level.INFO, "JSF Package Version = ", FacesContext.class.getPackage().getImplementationVersion());
        Properties manifestProps = loadManifestFile();
        version = manifestProps.getProperty("Implementation-Version");
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
        /*MavenXpp3Reader reader = new MavenXpp3Reader();
        Model model = null;
        String version = "Failed To retrieve version!";
        try {
            model = reader.read(this.getClass().getResourceAsStream("/META-INF/maven/au.com.manlyit/FitnessCRM/pom.xml"));
        } catch (IOException ex) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion IOException ", ex);
        } catch (XmlPullParserException ex) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion XmlPullParserException", ex);
        }
        if (model != null) {
            version = model.getVersion();
        }*/
        //System.out.println(model.getId());
        // System.out.println(model.getGroupId());
        // System.out.println(model.getArtifactId());
        //  System.out.println(model.getVersion());
        //version = getClass().getPackage().getImplementationVersion();
        try {
            if (version == null) {
                version = getVersionNameFromManifest();
            }
        } catch (IOException ex) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion Exception", ex);
        }
        Logger.getLogger(ApplicationBean.class.getName()).log(Level.INFO, "getApplicationVersion ", version);
        return version;
    }

    private Properties loadManifestFile() {
        ServletContext servletContext = (ServletContext) FacesContext
                .getCurrentInstance().getExternalContext().getContext();
        Properties prop = new Properties();
        try {
            InputStream resourceAsStream = servletContext.getResourceAsStream("/META-INF/MANIFEST.MF");
            if (resourceAsStream != null) {
                prop.load(resourceAsStream);
            }
        } catch (IOException e) {
            Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "loadManifestFile Exception", e);
        }
        return prop;
    }

    public String getVersionNameFromManifest() throws IOException {
        String manifestFile = "/META-INF/MANIFEST.MF";
        String attributeName1 = "Implementation-Title";
        String attributeName2 = "Implementation-Version";
        String applicationTitle = "FitnessCRM";

        Enumeration<URL> resources = getClass().getClassLoader()
                .getResources(manifestFile);
        while (resources.hasMoreElements()) {
            try {
                Manifest manifest = new Manifest(resources.nextElement().openStream());
                // check that this is your manifest and do what you need or get the next one
                Attributes attributes = manifest.getMainAttributes();
                String title = attributes.getValue(attributeName1);
                String version = attributes.getValue(attributeName2);
                if (title != null && version != null) {
                    Logger.getLogger(ApplicationBean.class.getName()).log(Level.INFO, "getVersionNameFromManifest Title:{0}, Version:{1}", new Object[]{title, version});
                    if (title.contains(applicationTitle)) {
                        return version;
                    }
                }

            } catch (IOException ex) {
                Logger.getLogger(ApplicationBean.class.getName()).log(Level.SEVERE, "getApplicationVersion Exception", ex);
            }
        }

        return "Not Found";
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

    /**
     * @return the customerEmailsEnabled
     */
    public boolean isCustomerEmailsEnabled() {
        return customerEmailsEnabled;
    }

    /**
     * @param customerEmailsEnabled the customerEmailsEnabled to set
     */
    public void setCustomerEmailsEnabled(boolean customerEmailsEnabled) {
        this.customerEmailsEnabled = customerEmailsEnabled;
    }

    /**
     * @return the version
     */
    public String getVersion() {
        return version;
    }

    /**
     * @param version the version to set
     */
    public void setVersion(String version) {
        this.version = version;
    }
}
