/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.EmailQueue;
import javax.ejb.Stateless;
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

    protected EntityManager getEntityManager() {
        return em;
    }

    public EmailQueueFacade() {
        super(EmailQueue.class);
    }
    
}
