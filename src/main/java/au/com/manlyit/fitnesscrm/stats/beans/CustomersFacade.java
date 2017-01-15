/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Groups;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.PersistenceException;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import javax.persistence.criteria.Subquery;
import javax.validation.ConstraintViolationException;

/**
 *
 * @author david
 */
@Stateless
public class CustomersFacade extends AbstractFacade<Customers> {
    
    private static final long serialVersionUID = 1L;
    
    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger LOGGER = Logger.getLogger(CustomersFacade.class.getName());
    private static final boolean DEBUG = false;
    
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
    
    public void addPaymentParameters(int custID, PaymentParameters newPayParams) {
        Customers cust = em.find(Customers.class, custID);
        cust.setPaymentParametersId(newPayParams);
        newPayParams.setCustomers(cust);
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
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            cm = (Customers) q.getSingleResult();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found:{0}", username);
        }
        return cm;
        // Query q = em.createNativeQuery("SELECT * FROM customers where username = '" + username + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }
    
    public Customers findCustomerByEmail(String email) {

        // CriteriaBuilder cb = em.getCriteriaBuilder();
        //CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
        // Root<Customers> rt2 = cq.from(Customers.class);
        // EntityType<Customers> customers_Et = rt2.getModel();
        Customers cm = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            
            Expression<String> emailAddress = rt.get("emailAddress");
            cq.where(cb.equal(emailAddress, email));
            
            Query q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            cm = (Customers) q.getSingleResult();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found:{0}", email);
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
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByName, Customer not found : Customer name  = {0} {1}", new Object[]{firstname, lastname, size});
            } else if (size > 1) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByName, Duplicate Customer id's found for Customer facebookId = {0} {1}. The number of duplicates is {1}", new Object[]{firstname, lastname, size});
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found:{0} {1} , {2}", new Object[]{firstname, lastname, e.getMessage()});
        }
        return cm;
        // Query q = em.createNativeQuery("SELECT * FROM customers where username = '" + username + "'", Customers.class);
        // return (Customers) q.getSingleResult();
    }
    
    public List<Customers> findCustomersByEmail(String email) {
        List<Customers> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            
            Expression<String> custEmail = rt.get("emailAddress");
            cq.where(cb.equal(custEmail, email));
            
            TypedQuery<Customers> q = em.createQuery(cq);
            if (DEBUG) {
                debug(q);
            }
            // q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            retList = q.getResultList();
        } catch (ConstraintViolationException cve) {
            
            JsfUtil.addErrorMessage(cve, configMapFacade.getConfig("PersistenceErrorOccured"));
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
     @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public boolean addCustomerToGroup(Customers cust, Groups group){
        // add a customer to a new group
        Customers c = find(cust.getId());
        group.setUsername(c);
       
        c.getGroupsCollection().add(group);
        getEntityManager().flush();
        
        return true;
    }
      @TransactionAttribute(TransactionAttributeType.REQUIRES_NEW)
    public void clearAllCustomerGroups(Customers cust){

        Customers c = find(cust.getId());            
        c.getGroupsCollection().clear();
        getEntityManager().flush();
        
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
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByFacebookId, Customer not found : Customer facebookId = {0}", fbId);
            } else if (size > 1) {
                LOGGER.log(Level.WARNING, "Customers findCustomerByFacebookId, Duplicate Customer id's found for Customer facebookId = {0}. The number of duplicates is {1}", new Object[]{fbId, size});
            }
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found or duplicate facebookId  found :" + fbId, e);
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
            if (DEBUG) {
                debug(q);
            }
            int size = q.getResultList().size();
            if (size == 1) {
                cm = (Customers) q.getSingleResult();
            } else if (size == 0) {
                LOGGER.log(Level.WARNING, "Customers findById, Customer not found : Customer Id = {0}", id);
            } else if (size > 1) {
                LOGGER.log(Level.WARNING, "Customers findById, Duplicate Customer id's found for Customer Id = {0}. The number of duplicates is {1}", new Object[]{id, size});
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Customers findById, An exception occurred for customer id :" + id, e);
        }
        return cm;
    }
    
    public Customers findById(int id) {
        Customers c = null;
        /*  try {
         c = em.find(Customers.class, id);
         } catch (Exception e) {
         LOGGER.log(Level.WARNING, "CustomersFacade findById, Exception {1}, : Customer Id = {0}", new Object[]{id, e.getMessage()});
         }
         if (c == null) {
         LOGGER.log(Level.WARNING, "CustomersFacade findById, Customer not found : Customer Id = {0}", id);
         }
         return c;
         Customers cm = null;
         Expression<Integer> custId ;*/
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            Expression<Integer> custId;
            custId = rt.get("id");
            cq.where(cb.equal(custId, id));
            
            TypedQuery<Customers> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            List<Customers> retList = q.getResultList();
            if (retList != null) {
                int size = retList.size();
                if (size == 1) {
                    c = retList.get(0);
                } else if (size == 0) {
                    LOGGER.log(Level.WARNING, "Customers findById, Customer not found : Customer Id = {0}", id);
                } else if (size > 1) {
                    LOGGER.log(Level.WARNING, "Customers findById, Duplicate Customer id's found for Customer Id = {0}. The number of duplicates is {1}", new Object[]{id, size});
                }
            } else {
                LOGGER.log(Level.WARNING, "Customers findById, Customer not found, LIST IS NULL : Customer Id = {0}", id);
            }
            
        } catch (PersistenceException pe) {
            Throwable e;
            e = pe.getCause();
            if (e.getClass() == ConstraintViolationException.class) {
                
                ConstraintViolationException cve = (ConstraintViolationException) e;
                LOGGER.log(Level.WARNING, "Customers findById, An ConstraintViolationException occurred for customer id :{0}, Message: {1}", new Object[]{id, cve.getConstraintViolations().toString()});
            }
            
            JsfUtil.addErrorMessage(pe, configMapFacade.getConfig("PersistenceErrorOccured"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Customers findById, An exception occurred for customer id :" + id, e);
        }
        return c;
    }
    
    public List<Customers> findAllActiveCustomers2(boolean sortAsc) {
        List<Customers> retList = null;
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
            TypedQuery<Customers> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
    
    public List<Customers> findAllActiveCustomersAndStaff(boolean sortAsc) {
        ArrayList<CustomerState> acs = new ArrayList<>();
        acs.add(new CustomerState(0, "ACTIVE"));
        ArrayList<String> types = new ArrayList<>();
        types.add("USER");
        types.add("LEAD");
        types.add("TRAINER");
        types.add("ADMIN");
        types.add("DEVELOPER");
        String sortField = "firstname";
        return findFilteredCustomers(sortAsc, sortField, acs, types, false);
    }
    
    public List<Customers> findAllActiveCustomers(boolean sortAsc) {
        ArrayList<CustomerState> acs = new ArrayList<>();
        acs.add(new CustomerState(0, "ACTIVE"));
        ArrayList<String> types = new ArrayList<>();
        types.add("USER");
        String sortField = "firstname";
        return findFilteredCustomers(sortAsc, sortField, acs, types, false);
    }

    //public List<Customers> findFilteredCustomers(boolean sortAsc, CustomerState[] selectedCustomerStates, boolean showStaff, boolean bypassCache) {
    public List<Customers> findFilteredCustomers(boolean sortAsc, String sortField, List<CustomerState> selectedCustomerStates, List<String> selectedCustomerTypes, boolean bypassCache) {
        List<Customers> retList = null;
        if (selectedCustomerStates == null || selectedCustomerStates.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<Predicate> predicatesList = new ArrayList<>();
            ArrayList<Predicate> predicatesList2 = new ArrayList<>();
            CriteriaBuilder cb = em.getCriteriaBuilder();
            
            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            cq.select(rt);
            
            Subquery<String> subquery = cq.subquery(String.class);
            Root<Groups> fromGroups = subquery.from(Groups.class);
            Expression<String> subUser = fromGroups.get("username").get("username");
            subquery.select(subUser);
            
            Join<Customers, CustomerState> jn = rt.join("active");// join customers.active to customer_state.id

            Expression<String> custState = jn.get("customerState");
            Expression<String> groupName = fromGroups.get("groupname");
            Expression<String> user = rt.get("username");

            //   predicatesList2.add(cb.equal(groupName, "TRAINER"));
            //   predicatesList2.add(cb.equal(groupName, "ADMIN"));
            //  predicatesList2.add(cb.equal(groupName, "DEVELOPER"));
            for (CustomerState cs : selectedCustomerStates) {
                predicatesList.add(cb.equal(custState, cs.getCustomerState()));
            }
            for (String ct : selectedCustomerTypes) {
                predicatesList2.add(cb.equal(groupName, ct));
            }

            //  if (showStaff == false) {
            //      subquery.where(cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()])));
            //       Predicate pred = cb.not(user.in(subquery));
            //       cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])), pred));
            //   } 
            //else {
            //   cq.where(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])));
            // }
            // else {
            subquery.where(cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()])));
            Predicate pred = user.in(subquery);
            cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])), pred));
            //  }
            
            Expression<String> express = rt.get(sortField);
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            TypedQuery<Customers> q = em.createQuery(cq);
            if (bypassCache) {
                q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            } else // q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            
            retList = q.getResultList();
            // for debugging
            // Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
            // DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
            // databaseQuery.prepareCall(session, new DatabaseRecord());
            // String sqlString = databaseQuery.getSQLString();
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            // LOGGER.log(Level.INFO, "CustomersFacade.findFilteredCustomers SQL Query String: {0}  -----------------Records Found:{1},", new Object[]{sqlString, retList.size()});

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    /*  
    // old filtered customers method before splitting groups and state
       public List<Customers> findFilteredCustomers(boolean sortAsc, String sortField, List<CustomerState> selectedCustomerStates, boolean showStaff, boolean bypassCache) {
        List<Customers> retList = null;
        if (selectedCustomerStates == null || selectedCustomerStates.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            ArrayList<Predicate> predicatesList = new ArrayList<>();
            ArrayList<Predicate> predicatesList2 = new ArrayList<>();
            CriteriaBuilder cb = em.getCriteriaBuilder();

            CriteriaQuery<Customers> cq = cb.createQuery(Customers.class);
            Root<Customers> rt = cq.from(Customers.class);
            cq.select(rt);

            Subquery<String> subquery = cq.subquery(String.class);
            Root<Groups> fromGroups = subquery.from(Groups.class);
            Expression<String> subUser = fromGroups.get("username").get("username");
            subquery.select(subUser);

            Join<Customers, CustomerState> jn = rt.join("active");// join customers.active to customer_state.id

            Expression<String> custState = jn.get("customerState");
            Expression<String> groupName = fromGroups.get("groupname");
            Expression<String> user = rt.get("username");

            predicatesList2.add(cb.equal(groupName, "TRAINER"));
            predicatesList2.add(cb.equal(groupName, "ADMIN"));
            predicatesList2.add(cb.equal(groupName, "DEVELOPER"));

            for (CustomerState cs : selectedCustomerStates) {
                predicatesList.add(cb.equal(custState, cs.getCustomerState()));
            }
            
            if (showStaff == false) {

                subquery.where(cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()])));
                Predicate pred = cb.not(user.in(subquery));
                cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])), pred));
            } 
            //else {
             //   cq.where(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])));
           // }
            else {
                subquery.where(cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList2.size()])));
                Predicate pred = user.in(subquery);
                cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])),pred));
            }
            
            Expression<String> express = rt.get(sortField);
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            TypedQuery<Customers> q = em.createQuery(cq);
            if (bypassCache) {
                q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            } else // q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }

            retList = q.getResultList();
            // for debugging
            // Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
            // DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
            // databaseQuery.prepareCall(session, new DatabaseRecord());
            // String sqlString = databaseQuery.getSQLString();
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            // LOGGER.log(Level.INFO, "CustomersFacade.findFilteredCustomers SQL Query String: {0}  -----------------Records Found:{1},", new Object[]{sqlString, retList.size()});

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
     }*/
    public List<Customers> findAll(boolean sortAsc) {
        List<Customers> retList = null;
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
            TypedQuery<Customers> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            if (DEBUG) {
                debug(q);
            }
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
    
    public List<Customers> findAllByGroup(String group, boolean sortAsc) {
        List<Customers> retList = null;
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
            if (DEBUG) {
                debug(q);
            }
            retList = (List<Customers>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }
}
