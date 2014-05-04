/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
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
public class StatsTakenFacade extends AbstractFacade<StatsTaken> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(StatsTakenFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public StatsTakenFacade() {
        super(StatsTaken.class);
    }

    public List<StatsTaken> findAllByCustId(int customer_id) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<StatsTaken> cq = cb.createQuery(StatsTaken.class);
            Root<StatsTaken> rt = cq.from(StatsTaken.class);
            Expression<String> custId = rt.get("customerId");
            cq.where(cb.equal(custId, customer_id));
            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "findAllByCustId: ", e);
        }
        return retList;
    }

}
