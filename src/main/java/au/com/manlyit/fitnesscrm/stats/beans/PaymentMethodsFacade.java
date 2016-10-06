/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.PaymentMethods;
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
public class PaymentMethodsFacade extends AbstractFacade<PaymentMethods> {
    private static final Logger logger = Logger.getLogger(PaymentMethodsFacade.class.getName());
    private static final long serialVersionUID = 1L;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
@Inject
    private ConfigMapFacade configMapFacade;
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentMethodsFacade() {
        super(PaymentMethods.class);
    }
    public PaymentMethods findPaymentMethodByName(String name) {
        List<PaymentMethods> retList;
        PaymentMethods matchingSession = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<PaymentMethods> cq = cb.createQuery(PaymentMethods.class);
            Root<PaymentMethods> rt = cq.from(PaymentMethods.class);
            Expression<String> etn = rt.get("paymentMethodName");
            Predicate condition1 = cb.equal(etn, name);
            cq.where(condition1);
            cq.select(rt);
            TypedQuery<PaymentMethods> q = em.createQuery(cq);
            retList = q.getResultList();
            if (retList.size() == 1) {
                matchingSession = retList.get(0);
            } else if (retList.size() > 1) {
                matchingSession = retList.get(0);
                logger.log(Level.WARNING, "findPaymentMethodByName: more than 1 match found : {0}", name);
            } else {
                logger.log(Level.INFO, "findPaymentMethodByName: no paymentMethod found for name: {0}", name);
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return matchingSession;
    } 
    
}
