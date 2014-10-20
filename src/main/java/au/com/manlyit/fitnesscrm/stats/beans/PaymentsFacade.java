/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
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
public class PaymentsFacade extends AbstractFacade<Payments> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(PaymentsFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentsFacade() {
        super(Payments.class);
    }

    public List<Payments> findPaymentsByCustomer(Customers customer) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<Customers> cust = rt.get("customerName");
            cq.where(cb.equal(cust, customer));

            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Could not find customers Payments.", e);
        }
        return retList;
    }
     public List<Payments> findScheduledPaymentsByCustomer(Customers customer) {
        List retList = null;
        
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<Customers> cust = rt.get("customerName");
            Expression<String> paymentID = rt.get("paymentID");
            cq.where(cb.and(cb.equal(cust, customer),cb.equal(paymentID, "SCHEDULED")));

            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Could not find customers Payments.", e);
        }
        return retList;
    }

    public Payments findPaymentByPaymentId(String paymentId) {
        Payments cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<String> payId = rt.get("paymentID");
            cq.where(cb.equal(payId, paymentId));

            Query q = em.createQuery(cq);
            if (q.getResultList().size() > 0) {
                cm = (Payments) q.getSingleResult();
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "paymentID not found or duplicate paymentID  found :" + paymentId, e);
        }
        return cm;
    }

}
