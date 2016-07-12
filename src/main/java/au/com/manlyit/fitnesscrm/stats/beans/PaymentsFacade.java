/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.webservices.ScheduledPayment;
import java.math.BigDecimal;
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
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Session;

/**
 *
 * @author david
 */
@Stateless
public class PaymentsFacade extends AbstractFacade<Payments> {

    private static final long serialVersionUID = 1L;

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

    public void createAndFlush(Payments entity) {
        getEntityManager().persist(entity);
        getEntityManager().flush();
        String message = "Entity Created: " + entity.toString();
        logger.log(Level.INFO, message);

    }

    public List<Payments> findPaymentsByCustomer(Customers customer, boolean bypassCache) {
        List<Payments> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Date> dDate = rt.get("debitDate");
            Expression<Customers> cust = rt.get("customerName");
            cq.where(cb.equal(cust, customer));
            cq.orderBy(cb.desc(dDate));
            TypedQuery<Payments> q = em.createQuery(cq);
            if (bypassCache) {
                q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            }
            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Could not find customers Payments.", e);
        }
        return retList;
    }

    public List<Payments> findPaymentsByCustomerAndStatus(Customers customer, String status) {
        List<Payments> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Date> dDate = rt.get("debitDate");
            Expression<Customers> cust = rt.get("customerName");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            cq.where(cb.and(cb.equal(cust, customer), cb.equal(paymentStatus, status)));
            cq.orderBy(cb.asc(dDate));
            TypedQuery<Payments> q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Could not find customers Payments.", e);
        }
        return retList;
    }

    public Payments findLastSuccessfulScheduledPayment(Customers customer) {
        Payments cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Date> dDate = rt.get("debitDate");
            Expression<Customers> cust = rt.get("customerName");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            cq.where(cb.and(cb.equal(cust, customer), cb.equal(paymentStatus, PaymentStatus.SUCESSFUL.value())));
            cq.orderBy(cb.desc(dDate));

            TypedQuery<Payments> q = em.createQuery(cq);
            List<Payments> pl;
            pl = q.getResultList();
            if (pl == null) {
                pl = new ArrayList<>();
            }

            if (pl.size() > 0) {
                cm = pl.get(0);
            }

        } catch (Exception e) {
            logger.log(Level.INFO, "findLastSuccessfulPayment error customer:{0} " + customer, e);
        }
        return cm;
    }

    public Payments findNextScheduledPayment(Customers customer) {
        Payments cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Date> dDate = rt.get("debitDate");
            Expression<Customers> cust = rt.get("customerName");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            cq.where(cb.and(cb.equal(cust, customer), cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value())));
            cq.orderBy(cb.asc(dDate));

            TypedQuery<Payments> q = em.createQuery(cq);

            if (q.getResultList().size() > 0) {
                cm = q.getResultList().get(0);
            } else {
                logger.log(Level.INFO, "findNextScheduledPayment did not find any scheduled payments for customer:{0}", customer.getUsername());
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "findNextScheduledPayment error customer:{0} " + customer, e);
        }
        return cm;
    }

    public Payments findPaymentById(int id) {
        Payments cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<Integer> payId = rt.get("id");
            cq.where(cb.equal(payId, id));

            TypedQuery<Payments> q = em.createQuery(cq);
            List<Payments> pList = q.getResultList();
            if (pList != null) {
                if (pList.size() > 0) {
                    cm = pList.get(0);
                }
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "payment  not found or duplicate paymentID  found :" + id, e);
        }
        return cm;
    }

    public Payments findPaymentByPaymentId(String paymentId) {
        Payments cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<String> payId = rt.get("paymentID");
            cq.where(cb.equal(payId, paymentId));

            TypedQuery<Payments> q = em.createQuery(cq);
            if (q.getResultList().size() > 0) {
                cm = q.getSingleResult();
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "paymentID not found or duplicate paymentID  found :" + paymentId, e);
        }
        return cm;
    }

    public Payments findScheduledPayment(String paymentReference) {
        Payments cm = null;
        int id;
        try {
            id = Integer.parseInt(paymentReference);
        } catch (NumberFormatException numberFormatException) {
            logger.log(Level.WARNING, "findScheduledPayment by reference. Reference is not a valid number :{0}", paymentReference);
            return null;
        }
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            boolean validReference = false;
            if (paymentReference != null) {
                validReference = !paymentReference.trim().isEmpty();
            }
            Expression<Integer> payRef = rt.get("id");
            Expression<String> paymentStatus = rt.get("paymentStatus");

            predicatesList1.add(cb.equal(payRef, id));

            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()));
            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()));

            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            TypedQuery<Payments> q = em.createQuery(cq);
            if (q.getResultList().size() == 1) {
                cm = q.getSingleResult();
            } else if (q.getResultList().size() > 1) {
                cm = q.getResultList().get(0);
                logger.log(Level.WARNING, "findScheduledPayment Multiple payments found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{paymentReference});

            } else {
                logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{paymentReference});

            }
        } catch (Exception e) {
            logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}, error:{4}", new Object[]{paymentReference, e.getMessage()});
        }
        return cm;
    }

    public Payments findScheduledPayment(BigDecimal paymentAmount, Date debitDate, String paymentReference, Boolean manuallyAddedPayment) {
        Payments cm = null;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        boolean validReference = false;
        if (paymentReference != null) {
            validReference = !paymentReference.trim().isEmpty();
        }
        int id = 0;
        if (validReference) {
            try {
                id = Integer.parseInt(paymentReference);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.WARNING, "findScheduledPayment by reference. Reference is not a valid number :{0}", paymentReference);
                return null;
            }
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<BigDecimal> payAmount = rt.get("paymentAmount");
            Expression<Date> payDate = rt.get("debitDate");
            Expression<Integer> payRef = rt.get("id");
            Expression<Boolean> manualPay = rt.get("manuallyAddedPayment");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            if (validReference) {
                predicatesList1.add(cb.equal(payRef, id));
            } else {

                predicatesList1.add(cb.equal(payAmount, paymentAmount));
                predicatesList1.add(cb.equal(payDate, debitDate));
                predicatesList1.add(cb.equal(manualPay, manuallyAddedPayment));
            }

            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()));
            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()));

            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            TypedQuery<Payments> q = em.createQuery(cq);
            if (q.getResultList().size() == 1) {
                cm = q.getSingleResult();
            } else if (q.getResultList().size() > 1) {
                cm = q.getResultList().get(0);
                logger.log(Level.WARNING, "findScheduledPayment Multiple payments found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{paymentAmount.toString(), debitDate, paymentReference, manuallyAddedPayment});

            } else {
                logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{paymentAmount.toString(), debitDate, paymentReference, manuallyAddedPayment});

            }
        } catch (Exception e) {
            logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}, error:{4}", new Object[]{paymentAmount.toString(), debitDate, paymentReference, manuallyAddedPayment, e.getMessage()});
        }
        return cm;
    }

    public Payments findScheduledPayment(ScheduledPayment pay) {
        Payments cm = null;
        boolean validReference = false;
        if (pay.getPaymentReference().isNil() == false) {
            validReference = !pay.getPaymentReference().getValue().trim().isEmpty();
        }
        int id = 0;
        if (validReference) {
            String ref = pay.getPaymentReference().getValue().trim();
            try {
                id = Integer.parseInt(ref);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.WARNING, "findScheduledPayment by reference. Reference is not a valid number :{0}", ref);
                return null;
            }
        } else {
            logger.log(Level.WARNING, "findScheduledPayment . Reference is not a valid  :{0}", pay.getYourSystemReference());
        }
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<BigDecimal> payAmount = rt.get("paymentAmount");
            Expression<Date> payDate = rt.get("debitDate");
            Expression<Integer> payRef = rt.get("id");
            //Expression<Boolean> manualPay = rt.get("manuallyAddedPayment");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            if (validReference) {
                predicatesList1.add(cb.equal(payRef, id));
            } else {
                predicatesList1.add(cb.equal(payAmount, new BigDecimal(pay.getPaymentAmount().toString())));
                predicatesList1.add(cb.equal(payDate, pay.getPaymentDate().toGregorianCalendar().getTime()));
                //predicatesList1.add(cb.equal(manualPay, pay.isManuallyAddedPayment()));
            }

            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()));
            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()));
            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            TypedQuery<Payments> q = em.createQuery(cq);
            if (q.getResultList().size() == 1) {
                cm = q.getSingleResult();
            } else if (q.getResultList().size() > 1) {
                cm = q.getResultList().get(0);
                logger.log(Level.WARNING, "findScheduledPayment Multiple payments found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString()});

            } else {

                logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString()});

            }
        } catch (Exception e) {
            logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}, error:{4}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString(), e.getMessage()});
        }
        return cm;
    }

    public Payments findScheduledPaymentByCust(ScheduledPayment pay, Customers cust) {
        Payments cm = null;
        boolean validReference = false;
        if (pay.getPaymentReference().isNil() == false) {
            validReference = !pay.getPaymentReference().getValue().trim().isEmpty();
        }
        int id = 0;
        if (validReference) {
            String ref = pay.getPaymentReference().getValue().trim();
            try {
                id = Integer.parseInt(ref);
            } catch (NumberFormatException numberFormatException) {
                logger.log(Level.WARNING, "findScheduledPayment by reference. Reference is not a valid number :{0}", ref);
                return null;
            }
        } else {
            logger.log(Level.WARNING, "findScheduledPayment . Reference is not a valid  :{0}", pay.getYourSystemReference().getValue());
        }
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<BigDecimal> payAmount = rt.get("paymentAmount");
            Expression<Customers> customer = rt.get("customerName");
            Expression<Date> payDate = rt.get("debitDate");
            Expression<Integer> payRef = rt.get("id");
            //Expression<Boolean> manualPay = rt.get("manuallyAddedPayment");
            Expression<String> paymentStatus = rt.get("paymentStatus");
            predicatesList1.add(cb.equal(customer, cust));
            if (validReference) {
                predicatesList1.add(cb.equal(payRef, id));
            } else {
                predicatesList1.add(cb.equal(payAmount, new BigDecimal(pay.getPaymentAmount().toString())));
                predicatesList1.add(cb.equal(payDate, pay.getPaymentDate().toGregorianCalendar().getTime()));
                //predicatesList1.add(cb.equal(manualPay, pay.isManuallyAddedPayment()));
            }

            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()));
            predicatesList1.add(cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()));
            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            TypedQuery<Payments> q = em.createQuery(cq);
            if (q.getResultList().size() == 1) {
                cm = q.getSingleResult();
            } else if (q.getResultList().size() > 1) {
                cm = q.getResultList().get(0);
                logger.log(Level.WARNING, "findScheduledPayment Multiple payments found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString()});

            } else {

                logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString()});

            }
        } catch (Exception e) {
            logger.log(Level.INFO, "findScheduledPayment not found , Amount:{0},Date:{1},Ref:{2},Manual:{3}, error:{4}", new Object[]{pay.getPaymentAmount().toString(), pay.getPaymentDate().toGregorianCalendar().getTime(), pay.getPaymentReference().getValue(), pay.isManuallyAddedPayment().toString(), e.getMessage()});
        }
        return cm;
    }

    public List<Payments> findScheduledPaymentsByCustomer(Customers cust, boolean includeFailed) {
        List<Payments> cm = null;

        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);

            Expression<Customers> customer = rt.get("customerName");
            Expression<String> paymentStatus = rt.get("paymentStatus");

            predicatesList1.add(cb.equal(customer, cust));
            if (includeFailed) {
                predicatesList1.add(cb.or(cb.equal(paymentStatus, PaymentStatus.MISSING_IN_PGW.value()), cb.equal(paymentStatus, PaymentStatus.REJECTED_BY_GATEWAY.value()), cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()), cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()), cb.equal(paymentStatus, PaymentStatus.DELETE_REQUESTED.value())));
            } else {
                predicatesList1.add(cb.or(cb.equal(paymentStatus, PaymentStatus.SCHEDULED.value()), cb.equal(paymentStatus, PaymentStatus.SENT_TO_GATEWAY.value()), cb.equal(paymentStatus, PaymentStatus.DELETE_REQUESTED.value())));

            }
            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            TypedQuery<Payments> q = em.createQuery(cq);
            cm = q.getResultList();

        } catch (Exception e) {
            logger.log(Level.INFO, "findScheduledPaymentsByCustomer not found , Customer:{0}, error:{1}", new Object[]{cust.getUsername(), e.getMessage()});
        }
        return cm;
    }

    public List<Payments> findPaymentsByDateRange(boolean useSettlement, boolean showSuccessful, boolean showFailed, boolean showPending, boolean showScheduled, Date startDate, Date endDate, boolean sortAsc, Customers cust) {
        List<Payments> retList = null;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();
        ArrayList<Predicate> predicatesList2 = new ArrayList<>();
        String activeState = "ACTIVE";
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Payments> cq = cb.createQuery(Payments.class);
            Root<Payments> rt = cq.from(Payments.class);
            Expression<Customers> customer;
            Expression<Date> stime;
            Expression<Date> stime2;
            Expression<Date> stime3;
            Join<Payments, Customers> customersJoin;// join paymenst 
            Join<Customers, CustomerState> customerStateJoin;// join customers.active to customer_state.id
            Expression<String> custState = null;
            //Expression<Customers> cust = null;
            if (showScheduled) {
                customersJoin = rt.join("customerName");
                customerStateJoin = customersJoin.join("active");// join customers.active to customer_state.id
                //cust = customersJoin.get("id");
                custState = customerStateJoin.get("customerState");
            }
            //  if (cust != null) { // filter by customer if provided
            //      customer = rt.get("customerName");

            //  }
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
            //   Predicate condition1 = cb.between(stime, startDate, endDate);
            if (showSuccessful) {
                predicatesList2.add(cb.equal(status, PaymentStatus.SUCESSFUL.value()));
            }
            if (showPending) {
                predicatesList2.add(cb.equal(status, PaymentStatus.PENDING.value()));
                predicatesList2.add(cb.equal(status, PaymentStatus.WAITING.value()));
            }
            if (showFailed) {
                predicatesList2.add(cb.equal(status, PaymentStatus.FATAL_DISHONOUR.value()));
                predicatesList2.add(cb.equal(status, PaymentStatus.DISHONOURED.value()));
            }
            if (showScheduled) {
                predicatesList2.add(cb.and(cb.equal(status, PaymentStatus.SCHEDULED.value()), cb.equal(custState, activeState)));

            }
            if (cust != null) { // filter by customer if provided
                customer = rt.get("customerName");
                //predicatesList1.add(cb.equal(customer, cust));
                cq.where(cb.and(cb.or(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()])), cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()])), cb.equal(customer, cust)));
            } else {
                cq.where(cb.and(cb.or(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()])), cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()]))));
            }

            //cq.where(cb.and(condition1,cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()]))));
            //cq.where(condition1);
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<Payments> q = em.createQuery(cq);
            retList = q.getResultList();

            // for debugging
            Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
            DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
            databaseQuery.prepareCall(session, new DatabaseRecord());
            String sqlString = databaseQuery.getSQLString();
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            logger.log(Level.FINE, "Payment/Settlement Report SQL Query String: {0}  -----------------Records Found:{8}, useSettlement: {1},showSuccessful: {2},showFailed: {3},showPending: {4},showScheduled: {5},startDate: {6},endDate: {7}", new Object[]{sqlString, useSettlement, showSuccessful, showFailed, showPending, showScheduled, startDate, endDate, retList.size()});

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

}
