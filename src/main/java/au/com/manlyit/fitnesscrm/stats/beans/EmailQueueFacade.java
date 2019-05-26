/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

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
    private static final Logger LOGGER = Logger.getLogger(EmailQueue.class.getName());
    private static final boolean DEBUG = false;

    protected EntityManager getEntityManager() {
        return em;
    }

    public EmailQueueFacade() {
        super(EmailQueue.class);
    }

    public List<EmailQueue> findEmailsByStatus(int status) {
        List<EmailQueue> eql = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<EmailQueue> cq = cb.createQuery(EmailQueue.class);
            Root<EmailQueue> rt = cq.from(EmailQueue.class);

            Expression<Integer> st = rt.get("status");
            cq.where(cb.equal(st, status));

            TypedQuery<EmailQueue> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            eql = q.getResultList();
       } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error getting emails from email Queue :{0}", e.getMessage());
        }
        return eql;
     }

    public Properties getEmailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.ssl.enable", configMapFacade.getConfig("mail.smtp.ssl.enable"));
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