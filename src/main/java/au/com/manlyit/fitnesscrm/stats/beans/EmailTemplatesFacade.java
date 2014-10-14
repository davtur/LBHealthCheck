/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.EmailTemplates;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class EmailTemplatesFacade extends AbstractFacade<EmailTemplates> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(EmailTemplatesFacade.class.getName());
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public EmailTemplatesFacade() {
        super(EmailTemplates.class);
    }
   
    public EmailTemplates findTemplateByName(String templateName) {

        EmailTemplates cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<EmailTemplates> cq = cb.createQuery(EmailTemplates.class);
            Root<EmailTemplates> rt = cq.from(EmailTemplates.class);

            Expression<String> name = rt.get("name");
            cq.where(cb.equal(name, templateName));

            Query q = em.createQuery(cq);
            cm = (EmailTemplates) q.getSingleResult();
        } catch (Exception e) {
            logger.log(Level.INFO, "Template not found:{0}", templateName);
        }
        return cm;
        
    } 
}
