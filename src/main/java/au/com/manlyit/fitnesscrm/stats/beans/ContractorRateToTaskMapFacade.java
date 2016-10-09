/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.ContractorRateToTaskMap;
import au.com.manlyit.fitnesscrm.stats.db.ContractorRates;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
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
public class ContractorRateToTaskMapFacade extends AbstractFacade<ContractorRateToTaskMap> {

    private static final Logger LOGGER = Logger.getLogger(ContractorRateToTaskMapFacade.class.getName());
    private static final boolean DEBUG = true;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ContractorRateToTaskMapFacade() {
        super(ContractorRateToTaskMap.class);
    }

    public List<SessionTypes> findBySessionTypesByContractorRateAndSupplier( Suppliers sup) {

        List<SessionTypes> stl = new ArrayList<>();
        List<ContractorRateToTaskMap> cml;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ContractorRateToTaskMap> cq = cb.createQuery(ContractorRateToTaskMap.class);
            Root<ContractorRateToTaskMap> rt = cq.from(ContractorRateToTaskMap.class);

            Join<ContractorRateToTaskMap, ContractorRates> jn = rt.join("contractorRateId");// join customers.active to customer_state.id
            Expression<Suppliers> supplier = jn.get("supplierId");

            //Expression<ContractorRates> contractorRates = rt.get("contractorRateId");

           // Predicate condition1 = cb.equal(contractorRates, cr);
            Predicate condition2 = cb.equal(supplier, sup);
           // cq.where(cb.equal(condition1, condition2));
            cq.where(condition2);
            //Query q = em.createQuery(cq);
            TypedQuery<ContractorRateToTaskMap> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            cml = q.getResultList();
            for (ContractorRateToTaskMap cm : cml) {
                stl.add(cm.getTaskId());
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "findBySessionTypesByContractorRateAndSupplier ERROR :{0} {1} ", new Object[]{sup.getSupplierName(), e.getMessage()});
        }
        return stl;

    }

    public List<SessionTypes> findBySessionTypesByContractorRate(ContractorRates cr) {

        List<SessionTypes> stl = new ArrayList<>();
        List<ContractorRateToTaskMap> cml;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ContractorRateToTaskMap> cq = cb.createQuery(ContractorRateToTaskMap.class);
            Root<ContractorRateToTaskMap> rt = cq.from(ContractorRateToTaskMap.class);

            Expression<ContractorRates> contractorRates = rt.get("contractorRateId");

            Predicate condition1 = cb.equal(contractorRates, cr);
            cq.where(condition1);

            //Query q = em.createQuery(cq);
            TypedQuery<ContractorRateToTaskMap> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            cml = q.getResultList();
            for (ContractorRateToTaskMap cm : cml) {
                stl.add(cm.getTaskId());
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "findBySessionTypesByContractorRate ERROR :{0} {1} ", new Object[]{cr.getName(), e.getMessage()});
        }
        return stl;

    }

    public ContractorRateToTaskMap findBySessionTypeAndContractorRate(SessionTypes st, ContractorRates cr) {

        ContractorRateToTaskMap cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ContractorRateToTaskMap> cq = cb.createQuery(ContractorRateToTaskMap.class);
            Root<ContractorRateToTaskMap> rt = cq.from(ContractorRateToTaskMap.class);

            Expression<SessionTypes> sessionTypes = rt.get("taskId");
            Expression<ContractorRates> contractorRates = rt.get("contractorRateId");
            Predicate condition1 = cb.equal(sessionTypes, st);
            Predicate condition2 = cb.equal(contractorRates, cr);
            cq.where(cb.and(condition1, condition2));

            //Query q = em.createQuery(cq);
            TypedQuery<ContractorRateToTaskMap> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            int size = q.getResultList().size();
            if (size == 1) {
                cm = q.getSingleResult();
            } else if (size == 0) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByName, Customer not found : Customer name  = {0} {1}", new Object[]{st.getName(), cr.getName(), size});
            } else if (size > 1) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByName, Duplicate Customer id's found for Customer facebookId = {0} {1}. The number of duplicates is {1}", new Object[]{st.getName(), cr.getName(), size});
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found:{0} {1} , {2}", new Object[]{st.getName(), cr.getName(), e.getMessage()});
        }
        return cm;

    }

}
