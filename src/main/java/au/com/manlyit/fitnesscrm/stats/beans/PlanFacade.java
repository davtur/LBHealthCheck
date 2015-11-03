/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Plan;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class PlanFacade extends AbstractFacade<Plan> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(PlanFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PlanFacade() {
        super(Plan.class);
    }
    
    public List<Plan> findAllPlans() {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Plan> cq = cb.createQuery(Plan.class);
            Root<Plan> rt = cq.from(Plan.class);

            Expression<Plan> plan = rt.get("parent");
            cq.where(cb.isNull(plan));

            Query q = em.createQuery(cq);

            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Exception : Could not find all Plans.", e);
}
        return retList;
    }

    public List<Plan> findAllPlansForSelectItems() {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Plan> cq = cb.createQuery(Plan.class);
            Root<Plan> rt = cq.from(Plan.class);

            Expression<Plan> plan = rt.get("parent");
            Expression<Boolean> planActive = rt.get("planActive");
            cq.where(cb.and(cb.isNull(plan), cb.equal(planActive, 0)));

            Query q = em.createQuery(cq);

            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Exception : Could not find all Plans.", e);
        }
        return retList;
    }

    public List<Plan> findPLansByName(String name) {
        List<Plan> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Plan> cq = cb.createQuery(Plan.class);
            Root<Plan> rt = cq.from(Plan.class);

            Expression<String> plan = rt.get("planName");
            cq.where(cb.equal(plan, name));

            TypedQuery<Plan> q = em.createQuery(cq);

            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Exception : Could not find plans by name.", e);
        }
        return retList;
    }
    public Plan findPLanByName(String name) {
        List<Plan> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Plan> cq = cb.createQuery(Plan.class);
            Root<Plan> rt = cq.from(Plan.class);

            Expression<String> plan = rt.get("planName");
            cq.where(cb.equal(plan, name));

            TypedQuery<Plan> q = em.createQuery(cq);

            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Exception : Could not find plans by name.", e);
        }
        if(retList != null && retList.size() == 1){
            return retList.get(0);
        }
        if(retList != null && retList.size() > 1){
            logger.log(Level.SEVERE, " Found more than 1 plan with the same name .", name);
            return retList.get(0);
        }
        if(retList == null){
            logger.log(Level.WARNING, " Could not find plan by name .", name);
            return null;
        }
        return null;
    }

}
