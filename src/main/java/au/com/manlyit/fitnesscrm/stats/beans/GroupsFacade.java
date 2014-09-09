/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
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
public class GroupsFacade extends AbstractFacade<Groups> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(GroupsFacade.class.getName());

    protected EntityManager getEntityManager() {
        return em;
    }

    public GroupsFacade() {
        super(Groups.class);
    }

    public boolean isCustomerInGroup(Customers cust, String group) {
        Groups cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Groups> cq = cb.createQuery(Groups.class);
            Root<Groups> rt = cq.from(Groups.class);
            Expression<String> groupname = rt.get("groupname");
            Expression<Customers> customer = rt.get("username");
            Predicate condition1 = cb.equal(cb.trim(cb.upper(groupname)), group.toUpperCase().trim());
            Predicate condition2 = cb.equal(customer, cust);
            cq.where(cb.and(condition1, condition2));
            Query q = em.createQuery(cq);
            List retList = q.getResultList();
            if(retList.size() > 0){
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "isCustomerInGroup error:{0} {1} ", new Object[]{cust.getUsername(), group, e.getMessage()});
        }
        return false;
     }
}
