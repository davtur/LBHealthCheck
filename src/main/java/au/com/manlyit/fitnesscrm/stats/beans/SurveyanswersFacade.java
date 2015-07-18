/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Surveyanswers;
import au.com.manlyit.fitnesscrm.stats.db.Surveys;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
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
public class SurveyanswersFacade extends AbstractFacade<Surveyanswers> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    @Inject
    private ConfigMapFacade configMapFacade;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SurveyanswersFacade() {
        super(Surveyanswers.class);
    }

    public List<Surveyanswers> findSurveyAnswersByCustomerAndSurvey(Customers customer, Surveys survey) {
        List<Surveyanswers> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Surveyanswers> cq = cb.createQuery(Surveyanswers.class);
            Root<Surveyanswers> rt = cq.from(Surveyanswers.class);
            Expression<Customers> cust = rt.get("UserId");
            Expression<Surveys> surv = rt.get("SurveyId");
            Predicate condition1 = cb.equal(cust, customer);
            Predicate condition2 = cb.equal(surv, survey);
            cq.where(cb.and(condition1, condition2));
            cq.select(rt);

            Query q = em.createQuery(cq);
            retList = (List<Surveyanswers>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

}
