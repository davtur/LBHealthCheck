/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import java.util.Properties;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author david
 */
@Stateless
public class EmailQueueFacade extends AbstractFacade<EmailQueue> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;

    protected EntityManager getEntityManager() {
        return em;
    }

    public EmailQueueFacade() {
        super(EmailQueue.class);
    }

    public Properties getEmailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));
        props.put("mail.smtp.headerimage.url", configMapFacade.getConfig("mail.smtp.headerimage.url"));
        props.put("mail.smtp.headerimage.cid", configMapFacade.getConfig("mail.smtp.headerimage.cid"));

        return props;

    }

}
