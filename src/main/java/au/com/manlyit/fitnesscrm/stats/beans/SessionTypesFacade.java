/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.util.List;
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
public class SessionTypesFacade extends AbstractFacade<SessionTypes> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(SessionTypesFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SessionTypesFacade() {
        super(SessionTypes.class);
    }
    
    public List<SessionTypes> findAllSessionTypesOrderByName(boolean sortAsc) {

        Query q;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionTypes> cq = cb.createQuery(SessionTypes.class);
            Root<SessionTypes> rt = cq.from(SessionTypes.class);

            Expression<String> express = rt.get("name");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
}
            q = em.createQuery(cq);
            return q.getResultList();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "SessionTypes Facade : findAllSessionTypesOrderByName",e);
        }
        return null;
        // Query q = em.createNativeQuery("SELECT * FROM customers where sessionName = '" + sessionName + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }

}
