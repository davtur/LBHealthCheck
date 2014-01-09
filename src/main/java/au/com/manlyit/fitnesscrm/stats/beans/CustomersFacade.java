/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class CustomersFacade extends AbstractFacade<Customers> {

    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(CustomersFacade.class.getName());

    protected EntityManager getEntityManager() {
        return em;
    }

    public CustomersFacade() {
        super(Customers.class);
    }

    public Customers findCustomerByUsername(String username) {

       // CriteriaBuilder cb = em.getCriteriaBuilder();
        //CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        // Root<Customers> rt2 = cq.from(Customers.class);
        // EntityType<Customers> customers_Et = rt2.getModel();
        Customers cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            Expression<String> custUsername = rt.get("username");
            cq.where(cb.equal(custUsername, username));

            Query q = em.createQuery(cq);
            cm = (Customers) q.getSingleResult();
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found:" + username, e);
        }
        return cm;
       // Query q = em.createNativeQuery("SELECT * FROM customers where username = '" + username + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }
 public List<Customers> findAllActiveCustomers(boolean sortAsc) {
        List retList = null;
        String state = "ACTIVE";//Active
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            
            Join<Customers, CustomerState> jn = rt.join("active");// join customers.active to customer_state.id
            Expression<String> custState = jn.get("customerState");
            cq.where(cb.equal(custState,state ));
            cq.select(rt);
            
            
            
            Expression<String> express = rt.get("firstname");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
    public List<Customers> findAll(boolean sortAsc) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            //
            //Expression<Integer> custId = rt.get("customer_id");
            //cq.where(cb.equal(custId, customerId));
            cq.select(rt);
            Expression<String> express = rt.get("firstname");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<Customers> findAllByGroup(String group, boolean sortAsc) {
        List retList = null;
        try {

            /*    CriteriaBuilder cb = em.getCriteriaBuilder();
             Metamodel m = em.getMetamodel();
             EntityType<Customers> Customers_ = m.entity(Customers.class);
             CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
             Root<Customers> rt = cq.from(Customers.class);
             Join<Customers, Groups> jn = rt.join("username");
             Expression<String> grp = jn.get("groupname");
             cq.where(cb.equal(grp, group));
             cq.select(jn.getParent());
             Expression<Date> express = rt.get("firstname");
             if (sortAsc) {
             cq.orderBy(cb.asc(express));
             } else {
             cq.orderBy(cb.desc(express));
             }
             Query q = em.createQuery(cq);
             */
            String sort = "DESC";
            if (sortAsc) {
                sort = "ASC";
            }
            Query q = em.createNativeQuery("SELECT c.id, c.gender, c.firstname, c.lastname, c.dob, c.email_address, c.preferred_contact, c.username, c.street_address, c.suburb, c.postcode, c.city, c.addr_state, c.country_id, c.telephone, c.fax, c.password, c.newsletter, c.group_pricing, c.email_format, c.auth, c.active, c.referredby, c.demographic FROM fitnessStats.customers c , fitnessStats.groups g  WHERE c.username = g.username and groupname = '" + group + "'  order By c.firstname " + sort + " ", Customers.class);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
}
