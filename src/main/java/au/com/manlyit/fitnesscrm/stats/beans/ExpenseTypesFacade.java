/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.ExpenseTypes;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTimetable;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
import java.sql.Time;
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
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class ExpenseTypesFacade extends AbstractFacade<ExpenseTypes> {

    @Inject
    private ConfigMapFacade configMapFacade;
    private static final Logger logger = Logger.getLogger(ExpenseTypesFacade.class.getName());
    private static final long serialVersionUID = 1L;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ExpenseTypesFacade() {
        super(ExpenseTypes.class);
    }

    public ExpenseTypes findSessionExpenseType(String name) {
        List<ExpenseTypes> retList;
        ExpenseTypes matchingSession = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ExpenseTypes> cq = cb.createQuery(ExpenseTypes.class);
            Root<ExpenseTypes> rt = cq.from(ExpenseTypes.class);
            Expression<String> etn = rt.get("expenseTypeName");
            Predicate condition1 = cb.equal(etn, name);
            cq.where(condition1);
            cq.select(rt);
            TypedQuery<ExpenseTypes> q = em.createQuery(cq);
            retList = q.getResultList();
            if (retList.size() == 1) {
                matchingSession = retList.get(0);
            } else if (retList.size() > 1) {
                matchingSession = retList.get(0);
                logger.log(Level.WARNING, "findSessionExpenseType: more than 1 match found for session timetable: {0}", name);
            } else {
                logger.log(Level.INFO, "findSessionExpenseType: no sessions found for session timetable: {0}", name);
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return matchingSession;
    }
}
