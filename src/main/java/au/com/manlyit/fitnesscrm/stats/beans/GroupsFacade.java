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

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public GroupsFacade() {
        super(Groups.class);
    }

    public List<String>  getGroups() {

        List<String>  ga = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery();
            Root<Groups> rt = cq.from(Groups.class);
            Expression<String> gname = rt.get("groupname");

            cq.select(rt.get("groupname")).distinct(true);
            Query q = em.createQuery(cq);

            ga = (List<String> ) q.getResultList();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "getGroups error: ", e.getMessage());
        }
        return ga;

    }

    public List<Groups> getCustomersGroups(Customers cust) {

        List<Groups> ga = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Groups> cq = cb.createQuery(Groups.class);
            Root<Groups> rt = cq.from(Groups.class);
            Expression<Customers> customer = rt.get("username");
            Predicate condition1 = cb.equal(customer, cust);
            
            cq.where(condition1);
            cq.distinct(true);
            Query q = em.createQuery(cq);

            ga = (List<Groups>) q.getResultList();

        } catch (Exception e) {
            logger.log(Level.SEVERE, "getCustomersGroups error: ", e.getMessage());
        }
        return ga;

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
            if (retList.size() > 0) {
                return true;
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "isCustomerInGroup error:{0} {1} ", new Object[]{cust.getUsername(), group, e.getMessage()});
        }
        return false;
    }

    public Groups getCustomerGroup(Customers cust, String group) {
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
            int k = retList.size();
            if (k > 0) {
                cm = (Groups) retList.get(0);
            }
            if (k > 1) {
                logger.log(Level.SEVERE, "getCustomerGroup. {2} duplicates returned. User:{0}, Group:{1} ", new Object[]{cust.getUsername(), group, k});
            }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "isCustomerInGroup error:{0} {1} ", new Object[]{cust.getUsername(), group, e.getMessage()});
        }
        return cm;
    }
}
