/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.SurveyAnswers;
import au.com.manlyit.fitnesscrm.stats.db.SurveyQuestions;
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
public class SurveyAnswersFacade extends AbstractFacade<SurveyAnswers> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    @Inject
    private ConfigMapFacade configMapFacade;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SurveyAnswersFacade() {
        super(SurveyAnswers.class);
    }

    public SurveyAnswers findSurveyAnswersByCustomerAndQuestion(Customers customer, SurveyQuestions quest) {
        List<SurveyAnswers> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SurveyAnswers> cq = cb.createQuery(SurveyAnswers.class);
            Root<SurveyAnswers> rt = cq.from(SurveyAnswers.class);
            Expression<Customers> cust = rt.get("userId");
            Expression<SurveyQuestions> question = rt.get("questionId");
            Predicate condition1 = cb.equal(cust, customer);
            Predicate condition2 = cb.equal(question, quest);
            cq.where(cb.and(condition1, condition2));
            cq.select(rt);

            Query q = em.createQuery(cq);
            retList = (List<SurveyAnswers>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        if(retList == null){
            return null;
        }
        if(retList.isEmpty()){
            return null;
        }
        if(retList.size() > 1){
             JsfUtil.addErrorMessage("Duplicate Survey Answers exist for customer "+ customer.toString() + ". The number of answers returned for the question \""+ quest.getQuestion() + "\" is " + retList.size() + "."  );
        }

        return retList.get(0);
    }
    /*  
     public List<SurveyAnswers> findSurveyAnswersByCustomerAndSurvey(Customers customer, Surveys survey) {
     List<SurveyAnswers> retList = null;

     try {
     CriteriaBuilder cb = em.getCriteriaBuilder();
     CriteriaQuery<SurveyAnswers> cq = cb.createQuery(SurveyAnswers.class);
     Root<SurveyAnswers> rt = cq.from(SurveyAnswers.class);
     Expression<Customers> cust = rt.get("UserId");
     Expression<Surveys> surv = rt.get("SurveyId");
     Predicate condition1 = cb.equal(cust, customer);
     Predicate condition2 = cb.equal(surv, survey);
     cq.where(cb.and(condition1, condition2));
     cq.select(rt);

     Query q = em.createQuery(cq);
     retList = (List<SurveyAnswers>) q.getResultList();
     } catch (Exception e) {
     JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
     }

     return retList;
     }
     public List<SurveyAnswers> findSurveyAnswersBySurvey(Surveys survey) {
     List<SurveyAnswers> retList = null;

     try {
     CriteriaBuilder cb = em.getCriteriaBuilder();
     CriteriaQuery<SurveyAnswers> cq = cb.createQuery(SurveyAnswers.class);
     Root<SurveyAnswers> rt = cq.from(SurveyAnswers.class);
            
     Expression<Surveys> surv = rt.get("SurveyId");
          
     Predicate condition2 = cb.equal(surv, survey);
     cq.where(cb.and( condition2));
     cq.select(rt);

     Query q = em.createQuery(cq);
     retList = (List<SurveyAnswers>) q.getResultList();
     } catch (Exception e) {
     JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
     }

     return retList;
     }*/

}
