/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

/**
 *
 * @author david
 */
@Stateless
public class ActivationFacade extends AbstractFacade<Activation> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public ActivationFacade() {
        super(Activation.class);
    }
       public Activation findToken(String key) {
        Query q = em.createNativeQuery("SELECT * FROM activation t where nonce = '" + key + "'", Activation.class);
        return (Activation) q.getSingleResult();
    }

    
    
}
