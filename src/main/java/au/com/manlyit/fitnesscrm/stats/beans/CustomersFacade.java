/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CollectionJoin;
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
public class CustomersFacade extends AbstractFacade<Customers> {

    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(CustomersFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public void editAndFlush(Customers entity) {
        getEntityManager().merge(entity);
        getEntityManager().flush();
        String message = "Entity Edited: " + entity.toString();
        Logger.getLogger(getClass().getName()).log(Level.INFO, message);

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
            logger.log(Level.INFO, "Customer not found:{0}", username);
        }
        return cm;
        // Query q = em.createNativeQuery("SELECT * FROM customers where username = '" + username + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }

    public Customers findCustomerByName(String firstname, String lastname) {

        // compare as uppcase and spaces trimmed
        // CriteriaBuilder cb = em.getCriteriaBuilder();
        //CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        // Root<Customers> rt2 = cq.from(Customers.class);
        // EntityType<Customers> customers_Et = rt2.getModel();
        Customers cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            Expression<String> custFirstname = rt.get("firstname");
            Expression<String> custLastname = rt.get("lastname");
            Predicate condition1 = cb.equal(cb.trim(cb.upper(custFirstname)), firstname.toUpperCase().trim());
            Predicate condition2 = cb.equal(cb.trim(cb.upper(custLastname)), lastname.toUpperCase().trim());
            cq.where(cb.and(condition1, condition2));

            //Query q = em.createQuery(cq);
            Query q = em.createNativeQuery("SELECT * FROM customers where upper(firstname) = upper('" + firstname.trim() + "') and upper(lastname) = upper('" + lastname.trim() + "') ", Customers.class);
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                logger.log(Level.WARNING, "Customers findCustomerByName, Customer not found : Customer name  = {0} {1}", new Object[]{firstname, lastname, size});
            } else if (size > 1) {
                logger.log(Level.WARNING, "Customers findCustomerByName, Duplicate Customer id's found for Customer facebookId = {0} {1}. The number of duplicates is {1}", new Object[]{firstname, lastname, size});
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found:{0} {1} , {2}", new Object[]{firstname, lastname, e.getMessage()});
        }
        return cm;
        // Query q = em.createNativeQuery("SELECT * FROM customers where username = '" + username + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }

    public List<Customers> findCustomersByEmail(String email) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            Expression<String> custEmail = rt.get("emailAddress");
            cq.where(cb.equal(custEmail, email));

            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public Customers findCustomerByFacebookId(String fbId) {
        Customers cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            Expression<String> facebookId = rt.get("facebookId");
            cq.where(cb.equal(facebookId, fbId));

            Query q = em.createQuery(cq);
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                logger.log(Level.WARNING, "Customers findCustomerByFacebookId, Customer not found : Customer facebookId = {0}", fbId);
            } else if (size > 1) {
                logger.log(Level.WARNING, "Customers findCustomerByFacebookId, Duplicate Customer id's found for Customer facebookId = {0}. The number of duplicates is {1}", new Object[]{fbId, size});
            }
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found or duplicate facebookId  found :" + fbId, e);
        }
        return cm;
    }

    public Customers findByIdBypassCache(int id) {
        Customers cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            Expression<Integer> custId = rt.get("id");
            cq.where(cb.equal(custId, id));

            Query q = em.createQuery(cq);
            q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                logger.log(Level.WARNING, "Customers findById, Customer not found : Customer Id = {0}", id);
            } else if (size > 1) {
                logger.log(Level.WARNING, "Customers findById, Duplicate Customer id's found for Customer Id = {0}. The number of duplicates is {1}", new Object[]{id, size});
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Customers findById, An exception occurred for customer id :" + id, e);
        }
        return cm;
    }

    public Customers findById(int id) {
        Customers cm = null;
        Expression<Integer> custId ;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);

            custId = rt.get("id");
            cq.where(cb.equal(custId, id));

            Query q = em.createQuery(cq);
            List retList = q.getResultList();
            if (retList != null) {
                int size = retList.size();
                if (size == 1) {
                    cm = (Customers) retList.get(0);
                } else if (size == 0) {
                    logger.log(Level.WARNING, "Customers findById, Customer not found : Customer Id = {0}", id);
                } else if (size > 1) {
                    logger.log(Level.WARNING, "Customers findById, Duplicate Customer id's found for Customer Id = {0}. The number of duplicates is {1}", new Object[]{id, size});
                }
            } else {
                logger.log(Level.WARNING, "Customers findById, Customer not found, LIST IS NULL : Customer Id = {0}", id);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Customers findById, An exception occurred for customer id :" + id, e);
        }
        return cm;
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
            cq.where(cb.equal(custState, state));
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

    public List<Customers> findFilteredCustomers(boolean sortAsc, CustomerState[] selectedCustomerStates, boolean showTrainers, boolean bypassCache) {
        List<Customers> retList = null;
        if (selectedCustomerStates == null || selectedCustomerStates.length == 0) {
            return new ArrayList<>();
        }
        // String state = "ACTIVE";//Active
        try {
            ArrayList<Predicate> predicatesList = new ArrayList<>();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);

            Root<Customers> rt = cq.from(Customers.class);
            //Root<Groups> rt2 = cq.from(Groups.class);
            // Predicate joinCondition = cb.equal(rt2.get("groupname"), "USER");

            Join<Customers, CustomerState> jn = rt.join("active");// join customers.active to customer_state.id
            // Join<Customers, Groups> jn2 = rt.join("username").on(joinCondition);
            Expression<String> custState = jn.get("customerState");
            // Expression<String> groupName = jn2.get("groupname");

            for (CustomerState cs : selectedCustomerStates) {
                predicatesList.add(cb.equal(custState, cs.getCustomerState()));
            }
            //if (showTrainers == false) {
            //     predicatesList.add(cb.equal(groupName, "USER"));
            // }
            cq.where(cb.or(predicatesList.<Predicate>toArray(
                    new Predicate[predicatesList.size()])));

            cq.select(rt);

            Expression<String> express = rt.get("firstname");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            if (bypassCache) {
                q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            }

            retList = q.getResultList();
            int k = retList.size();
            if (showTrainers == false && k > 0) { // nasty workaround as I cant get the join on groups to work
                //remove trainers
                for (int y = k - 1; y > 0; y--) {
                    Customers c = retList.get(y);
                    Collection<Groups> grps = c.getGroupsCollection();
                    boolean remove = false;
                    for (Groups g : grps) {
                        if (g.getGroupname().contains("USER") == false) {
                            remove = true;
                        }
                    }
                    if (remove) {
                        retList.remove(y);
                    }
                }

            }
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
