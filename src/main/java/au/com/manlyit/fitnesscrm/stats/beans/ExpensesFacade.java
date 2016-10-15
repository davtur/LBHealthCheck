/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Expenses;
import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class ExpensesFacade extends AbstractFacade<Expenses> {

    private static final Logger LOGGER = Logger.getLogger(ExpensesFacade.class.getName());
    private static final long serialVersionUID = 1L;
    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ExpensesFacade() {
        super(Expenses.class);
    }

    public List<Expenses> findExpensesByDateRange(Date startDate, Date endDate, boolean sortAsc) {
        List<Expenses> retList = null;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Expenses> cq = cb.createQuery(Expenses.class);
            Root<Expenses> rt = cq.from(Expenses.class);

            Expression<Date> stime;

            stime = rt.get("expenseIncurredTimestamp");

            predicatesList1.add(cb.between(stime, startDate, endDate));

            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<Expenses> q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<Expenses> findExpensesByDateRangeAndSupplier(Date startDate, Date endDate, Suppliers sup, boolean sortAsc) {
        List<Expenses> retList = null;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Expenses> cq = cb.createQuery(Expenses.class);
            Root<Expenses> rt = cq.from(Expenses.class);

            Expression<Suppliers> supplier = rt.get("supplierId");
            Expression<Date> stime = rt.get("expenseIncurredTimestamp");

            predicatesList1.add(cb.equal(supplier, sup));
            predicatesList1.add(cb.between(stime, startDate, endDate));

            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<Expenses> q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public Double findSumOfExpensesByDateRangeAndSupplier(Date startDate, Date endDate, Suppliers sup) {
        Double sumOfExpenses = (double) 0;
        ArrayList<Predicate> predicatesList1 = new ArrayList<>();

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Number> cq = cb.createQuery(Number.class);
            Root<Expenses> rt = cq.from(Expenses.class);

            Expression<Suppliers> supplier = rt.get("supplierId");
            Expression<Date> stime = rt.get("expenseIncurredTimestamp");
            Expression<Double> expenseAmount = rt.get("expenseAmount");
            Expression<Double> totalAmount = cb.sum(expenseAmount);

            predicatesList1.add(cb.equal(supplier, sup));
            predicatesList1.add(cb.between(stime, startDate, endDate));

            cq.where(predicatesList1.<Predicate>toArray(new Predicate[predicatesList1.size()]));

            cq.select(totalAmount.alias("Total Amount"));

            TypedQuery<Number> q = em.createQuery(cq);
            //debug(q);
            if (q.getSingleResult() != null) {
                sumOfExpenses = q.getSingleResult().doubleValue();
            } else {
                sumOfExpenses = (double) 0;
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return sumOfExpenses;
    }

}
