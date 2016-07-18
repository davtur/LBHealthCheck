/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.config.CacheUsage;
import org.eclipse.persistence.config.QueryHints;

/**
 *
 * @author david
 */
@Stateless
public class PaymentParametersFacade extends AbstractFacade<PaymentParameters> {

    private static final long serialVersionUID = 1L;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger LOGGER = Logger.getLogger(PaymentParametersFacade.class.getName());
    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public PaymentParametersFacade() {
        super(PaymentParameters.class);
    }
    
     public PaymentParameters findPaymentParametersByCustomer(Customers  cust) {

        // The logged in user field is unique so there can only be one
        PaymentParameters paymentParams = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<PaymentParameters> cq = cb.createQuery(PaymentParameters.class);
            Root<PaymentParameters> rt = cq.from(PaymentParameters.class);

            Expression<Customers> loggedInUser = rt.get("loggedInUser");
            cq.where(cb.equal(loggedInUser, cust));

            Query q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            paymentParams = (PaymentParameters) q.getSingleResult();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Payment Parameters for customer not found:{0}", cust);
        }
        return paymentParams;
       
    }
    
}
