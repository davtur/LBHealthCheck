/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;

/**
 *
 * @author david
 */
@ManagedBean(name = "applicationBean")
@ApplicationScoped
public class ApplicationBean {

    private static final Logger logger = Logger.getLogger(ApplicationBean.class.getName());

    @PostConstruct
    public void init() {
        logger.log(Level.INFO, "ApplicationBean Created");
    }
}
