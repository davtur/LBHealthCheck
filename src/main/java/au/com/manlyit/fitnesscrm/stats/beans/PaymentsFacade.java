/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Participants;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
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
    @Inject
    private ConfigMapFacade configMapFacade;

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
            cq.where(cb.and(cb.equal(cust, customer), cb.isNull(paymentID)));

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

    public List<Payments> findPaymentsByDateRange(boolean useSettlement, boolean showSuccessful, boolean showFailed, boolean showPending, Date startDate, Date endDate, boolean sortAsc) {
        List<Payments> retList = null;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        ArrayList<Predicate> predicatesList2 = new ArrayList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Date> stime;
            Expression<Date> stime2;
            Expression<Date> stime3;
            if (useSettlement) {
                stime = rt.get("settlementDate");
                predicatesList1.add(cb.between(stime, startDate, endDate));
            } else {
                stime = rt.get("paymentDate");
                stime2 = rt.get("debitDate");
                stime3 = rt.get("transactionTime");
                predicatesList1.add(cb.between(stime, startDate, endDate));
                predicatesList1.add(cb.between(stime2, startDate, endDate));
                predicatesList1.add(cb.between(stime3, startDate, endDate));
            }
            Expression<Date> status = rt.get("paymentStatus");
            Predicate condition1 = cb.between(stime, startDate, endDate);
            if (showSuccessful) {
                predicatesList2.add(cb.equal(status, "S"));
            }
            if (showPending) {
                predicatesList2.add(cb.equal(status, "P"));
            }
            if (showFailed) {
                predicatesList2.add(cb.equal(status, "F"));
                predicatesList2.add(cb.equal(status, "D"));
            }
            cq.where(cb.and(cb.or(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()])), cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()]))));
            //cq.where(cb.and(condition1,cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()]))));
            //cq.where(condition1);
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            Query q = em.createQuery(cq);
            retList = (List<Payments>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

}
