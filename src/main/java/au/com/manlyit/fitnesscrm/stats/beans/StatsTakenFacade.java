/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import java.util.Date;
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

    public List<StatsTaken> findAllByCustomer(Customers customer,boolean sortAsc) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<StatsTaken> cq = cb.createQuery(StatsTaken.class);
            Root<StatsTaken> rt = cq.from(StatsTaken.class);
            Expression<Integer> custId = rt.get("customerId");
            cq.where(cb.equal(custId, customer));
            Expression<Date> express = rt.get("dateRecorded");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "findAllByCustId: ", e);
        }
        return retList;
    }

    public int countByCustId(Customers customer) {
        if (customer == null) {
            return count();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery cq = cb.createQuery();
        Root<StatsTaken> rt = cq.from(StatsTaken.class);
        Expression<Customers> custId = rt.get("customerId");

        cq.where(cb.equal(custId, customer));
        cq.select(cb.count(rt));
        Query q = em.createQuery(cq);
        //Query q = em.createNativeQuery("SELECT count(*) FROM stats_taken t where  customer_id  = '" + customer_id + "'");
        return ((Long) q.getSingleResult()).intValue();
    }

    public void synch() {

        em.flush();
    }
}
