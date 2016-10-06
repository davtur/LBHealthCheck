/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Suppliers;
import java.util.List;
import java.util.logging.Level;
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
public class SuppliersFacade extends AbstractFacade<Suppliers> {
    private static final Logger logger = Logger.getLogger(SuppliersFacade.class.getName());
    private static final long serialVersionUID = 1L;
    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SuppliersFacade() {
        super(Suppliers.class);
    }
    public Suppliers findSupplierByName(String name) {
        List<Suppliers> retList;
        Suppliers matchingSession = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Suppliers> cq = cb.createQuery(Suppliers.class);
            Root<Suppliers> rt = cq.from(Suppliers.class);
            Expression<String> etn = rt.get("SupplierName");
            Predicate condition1 = cb.equal(etn, name);
            cq.where(condition1);
            cq.select(rt);
            TypedQuery<Suppliers> q = em.createQuery(cq);
            retList = q.getResultList();
            if (retList.size() == 1) {
                matchingSession = retList.get(0);
            } else if (retList.size() > 1) {
                matchingSession = retList.get(0);
                logger.log(Level.WARNING, "findTrainerSupplierDetails: more than 1 match found : {0}", name);
            } else {
                logger.log(Level.INFO, "findTrainerSupplierDetails: no suppliers found : {0}", name);
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return matchingSession;
    } 
    public Suppliers findSupplierByTrainerCustomerId(Customers cust) {
        List<Suppliers> retList;
        Suppliers matchingSession = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Suppliers> cq = cb.createQuery(Suppliers.class);
            Root<Suppliers> rt = cq.from(Suppliers.class);
            Expression<Customers> customers = rt.get("internalContractorId");
            Predicate condition1 = cb.equal(customers, cust);
            cq.where(condition1);
            cq.select(rt);
            TypedQuery<Suppliers> q = em.createQuery(cq);
            retList = q.getResultList();
            if (retList.size() == 1) {
                matchingSession = retList.get(0);
            } else if (retList.size() > 1) {
                matchingSession = retList.get(0);
                logger.log(Level.WARNING, "findSupplierByTrainerCustomerId: more than 1 match found : {0}", cust.getUsername());
            } else {
                logger.log(Level.INFO, "findSupplierByTrainerCustomerId: no suppliers found : {0}", cust.getUsername());
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return matchingSession;
    } 
}
