/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * @author david
 */
@Named("applicationBean")
@ApplicationScoped
public class ApplicationBean implements Serializable {

    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomerImagesFacade ejbCustomerImagesFacade;
    @Inject
    private ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomersFacade;
    private static final Logger logger = Logger.getLogger(ApplicationBean.class.getName());

    public ApplicationBean() {
    }

    @PostConstruct
    private void applicationSetup() {
        logger.log(Level.INFO, "ApplicationBean Created");
        //sanityCheckCustomersForDefaultItems();
    }

    
}
