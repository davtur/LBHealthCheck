/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.TransactionAttribute;
import static javax.ejb.TransactionAttributeType.REQUIRES_NEW;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.jpa.JpaEntityManager;
import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Session;
import org.primefaces.model.SortOrder;

/**
 *
 * @author david
 * @param <T>
 */
public abstract class AbstractFacade<T> implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(AbstractFacade.class.getName());
    private final Class<T> entityClass;
    private static final boolean DEBUG = true;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void evictFromL2cache(Object thePrimaryKey) {
        //return getEntityManager().find(entityClass, id);
        getEntityManager().getEntityManagerFactory().getCache().evict(entityClass, thePrimaryKey);
    }

    public void create(T entity) {
        // try {

        getEntityManager().persist(entity);
        if (DEBUG) {

            String message = "Entity Created: Class=" + entityClass.getName() + ", Entity : =" + entity.toString() + ", EM Delegate=" + getEntityManager().getDelegate().toString();
            LOGGER.log(Level.INFO, message);
        }
        /*catch (Exception e) {
            LOGGER.log(Level.WARNING, "<-------------------------------------------------- Abstract Facade - could not create Entity ------------------------------------------------------------------------>");
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            Throwable t = e.getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            t = e.getCause().getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");

        }*/

    }
@TransactionAttribute(REQUIRES_NEW)
    public void createAndFlushForGeneratedIdEntities(T entity) {

        /*
        
The EntityManager.flush() operation can be used to write all changes to the database before the transaction is committed. By default JPA does not normally write changes to the database until the transaction is committed. This is normally desirable as it avoids database access, resources and locks until required. It also allows database writes to be ordered, and batched for optimal database access, and to maintain integrity constraints and avoid deadlocks. This means that when you call persist, merge, or remove the database DML INSERT, UPDATE, DELETE is not executed, until commit, or until a flush is triggered.

The flush() does not execute the actual commit: the commit still happens when an explicit commit() is requested in case of resource local transactions, or when a container managed (JTA) transaction completes.

Flush has several usages:

    Flush changes before a query execution to enable the query to return new objects and changes made in the persistence unit.
    Insert persisted objects to ensure their Ids are assigned and accessible to the application if using IDENTITY sequencing.
    Write all changes to the database to allow error handling of any database errors (useful when using JTA or SessionBeans).
    To flush and clear a batch for batch processing in a single transaction.
    Avoid constraint errors, or reincarnate an object.

        
         */
        try {

            getEntityManager().persist(entity);
            getEntityManager().flush();
            if (DEBUG) {

                String message = "Entity Created and Flushed: Class=" + entityClass.getName() + ", Entity: =" + entity.toString() + ", EM Delegate=" + getEntityManager().getDelegate().toString();
                LOGGER.log(Level.INFO, message);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "<-------------------------------------------------- Abstract Facade - could not create and flush Entity ------------------------------------------------------------------------>");
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            Throwable t = e.getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            t = e.getCause().getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");

        }

    }

    public void pushChangesToDBImmediatleyInsteadOfAtTxCommit() {

        /*
        
The EntityManager.flush() operation can be used to write all changes to the database before the transaction is committed. By default JPA does not normally write changes to the database until the transaction is committed. This is normally desirable as it avoids database access, resources and locks until required. It also allows database writes to be ordered, and batched for optimal database access, and to maintain integrity constraints and avoid deadlocks. This means that when you call persist, merge, or remove the database DML INSERT, UPDATE, DELETE is not executed, until commit, or until a flush is triggered.

The flush() does not execute the actual commit: the commit still happens when an explicit commit() is requested in case of resource local transactions, or when a container managed (JTA) transaction completes.

Flush has several usages:

    Flush changes before a query execution to enable the query to return new objects and changes made in the persistence unit.
    Insert persisted objects to ensure their Ids are assigned and accessible to the application if using IDENTITY sequencing.
    Write all changes to the database to allow error handling of any database errors (useful when using JTA or SessionBeans).
    To flush and clear a batch for batch processing in a single transaction.
    Avoid constraint errors, or reincarnate an object.

        
         */
        try {

            getEntityManager().flush();
            if (DEBUG) {

                String message = "Entitys Flushed: ";
                LOGGER.log(Level.INFO, message);
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "<-------------------------------------------------- Abstract Facade - could not  flush Entity's ------------------------------------------------------------------------>");
            LOGGER.log(Level.WARNING, e.getMessage(), e);
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            Throwable t = e.getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            t = e.getCause().getCause();
            if (t != null) {
                LOGGER.log(Level.WARNING, t.getMessage(), t);
            }
            LOGGER.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");

        }

    }

    public void edit(T entity) {

        getEntityManager().merge(entity);
        if (DEBUG) {

            String message = "Entity  Merged: Class=" + entityClass.getName() + ", Entity: =" + entity.toString() + ", EM Delegate=" + getEntityManager().getDelegate().toString();
            LOGGER.log(Level.INFO, message);
        }
    }

    public void refreshfromDB(T entity) {

        getEntityManager().refresh(getEntityManager().merge(entity));
        if (DEBUG) {

            String message = "Entity Refreshed: Class=" + entityClass.getName() + ", Primary Key=" + entity.toString() + ", EM Delegate=" + getEntityManager().getDelegate().toString();
            LOGGER.log(Level.INFO, message);
        }
    }

    public void debug(Query query) {
        // for debugging
        Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
        DatabaseQuery databaseQuery = ((EJBQueryImpl) query).getDatabaseQuery();
        databaseQuery.prepareCall(session, new DatabaseRecord());
        String sqlString = databaseQuery.getSQLString();
        //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
        String sqlString2 = databaseQuery.getTranslatedSQLString(session, databaseQuery.getTranslationRow());
        String sqlString3 = databaseQuery.getEJBQLString();
        String sqlString4 = databaseQuery.getJPQLString();
        //logger.log(Level.INFO, "DEBUG ( Turn this off if not needed ) SQL Query String: {0}  ----------------- {1}", new Object[]{sqlString, sqlString2});
        LOGGER.log(Level.INFO, "DEBUG SQL Query String: -------------> {0} ,  ( Turn this off by setting DEBUG = false in facade class if not needed )", new Object[]{sqlString2});
    }

    public void remove(T entity) {

        getEntityManager().remove(getEntityManager().merge(entity));
        //  String message = "Entity Removed: " + entity.toString();
        //  LOGGER.log(Level.INFO, message);
    }

    public T find(Object id) {
        return getEntityManager().find(entityClass, id);
    }

    public List<T> findAll() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        //q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
        return q.getResultList();
    }

    public List<T> findRange(int[] range) {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        cq.select(cq.from(entityClass));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        //q.setHint("CACHE_USAGE", "DoNotCheckCache");
        q.setMaxResults(range[1] - range[0]);
        q.setFirstResult(range[0]);
        //q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
        return q.getResultList();
    }

    public int count() {
        javax.persistence.criteria.CriteriaQuery cq = getEntityManager().getCriteriaBuilder().createQuery();
        javax.persistence.criteria.Root<T> rt = cq.from(entityClass);
        cq.select(getEntityManager().getCriteriaBuilder().count(rt));
        javax.persistence.Query q = getEntityManager().createQuery(cq);
        //q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
        return ((Long) q.getSingleResult()).intValue();
    }
// This method provides a bridge
    // between the session beans and my LazyDataModel.
// Note: all of our databse entities that are involved in the lazy loading table must implement BaseEntity and have the id column name as the primary key.( see Customers class as an example )

    public List<T> load(int first, int count, String sortField, SortOrder sortOrder, Map<String, Object> filters) {
        List<T> resultList;
        String message = "Abstract Facade: load method - Lazy Loading: " + entityClass.getSimpleName() + ",Rows=" + count + ", First=" + first + ", SortField=" + sortField + ", SortOrder=" + sortOrder.name();
        LOGGER.log(Level.INFO, message);
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = builder.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq.select(root);
        if (sortField != null) {
            if (sortOrder == SortOrder.ASCENDING) {
                cq.orderBy(builder.asc(root.get(sortField)));
            } else if (sortOrder == SortOrder.DESCENDING) {
                cq.orderBy(builder.desc(root.get(sortField)));
            }
        }
        if (filters != null) {
            Set<Map.Entry<String, Object>> entries = filters.entrySet();
            ArrayList<Predicate> predicatesList = new ArrayList<>(entries.size());
            for (Map.Entry<String, Object> filter : entries) {
                String key = filter.getKey();
                Expression expresskey = root.get(key);
                Object val = filter.getValue();
                Class type = expresskey.getJavaType();
                try {
                    Method getIdMethod = type.getMethod("getId");// this is implemented by the BaseEntity class so if it is a join to another table it should have this method
                    String sVal = (String) val;
                    int iVal = Integer.parseInt(sVal);
                    Expression<Integer> custId = root.join(key).get("id");
                    predicatesList.add(builder.equal(custId, iVal));
                } catch (NoSuchMethodException ex) {
                    predicatesList.add(builder.like(expresskey, filter.getValue() + "%"));
                } catch (SecurityException ex) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            cq.where(predicatesList.<Predicate>toArray(
                    new Predicate[predicatesList.size()]));
        }
        javax.persistence.TypedQuery<T> query = getEntityManager().createQuery(cq);

        query.setFirstResult(first);

        query.setMaxResults(count);
        resultList = query.getResultList();
        if (DEBUG) {
            debug(query);
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            if (filters != null) {
                LOGGER.log(Level.FINE, "filters: {0}  ", new Object[]{filters.entrySet()});
            } else {
                LOGGER.log(Level.FINE, "Lazy Load SQL Query    Filters Null");
            }
        }

        return resultList;
    }

    public int countDateRange(Date startDate, Date endDate, String dateRangeFieldName) {
        int count = -1;
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery cq = cb.createQuery();
            Root<T> rt = cq.from(entityClass);

            Expression<Date> stime = rt.get(dateRangeFieldName);
            Predicate c1 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate c2 = cb.lessThan(stime, endDate);
            cq.where(cb.and(c1, c2));
            cq.select(cb.count(rt));

            Query q = getEntityManager().createQuery(cq);
            count = ((Long) q.getSingleResult()).intValue();
            LOGGER.log(Level.INFO, "countDateRange:{0}", count);
            return count;
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "countDateRange:", e);
        }
        return -1;

    }

    public List<T> loadDateRange(int first, int count, String sortField, SortOrder sortOrder, Map<String, Object> filters, Date startDate, Date endDate, String dateRangeFieldName) {
        List<T> resultList;
        String message = "Abstract Facade: loadDateRange  method -Lazy Loading: " + entityClass.getSimpleName() + ",Rows=" + count + ", First=" + first + ", SortField=" + sortField + ", SortOrder=" + sortOrder.name() + ", dateRangeFieldName=" + dateRangeFieldName + ", startDate=" + startDate.toString() + ", endDate=" + endDate.toString();
        LOGGER.log(Level.INFO, message);
        CriteriaBuilder builder = getEntityManager().getCriteriaBuilder();
        CriteriaQuery<T> cq = builder.createQuery(entityClass);
        Root<T> root = cq.from(entityClass);
        cq.select(root);
        ArrayList<Predicate> predicatesList = new ArrayList<>(2);
        Expression<Date> stime = root.get(dateRangeFieldName);

        Predicate condition1 = builder.greaterThanOrEqualTo(stime, startDate);
        Predicate condition2 = builder.lessThan(stime, endDate);
        predicatesList.add(condition1);
        predicatesList.add(condition2);
        if (sortField != null) {
            if (sortOrder == SortOrder.ASCENDING) {
                cq.orderBy(builder.asc(root.get(sortField)));
            } else if (sortOrder == SortOrder.DESCENDING) {
                cq.orderBy(builder.desc(root.get(sortField)));
            }
        }
        if (filters != null) {
            Set<Map.Entry<String, Object>> entries = filters.entrySet();
            predicatesList = new ArrayList<>(entries.size() + 2);
            for (Map.Entry<String, Object> filter : entries) {
                String key = filter.getKey();
                Expression expresskey = root.get(key);
                Object val = filter.getValue();
                Class type = expresskey.getJavaType();
                try {
                    Method getIdMethod = type.getMethod("getId");// this is implemented by the BaseEntity class so if it is a join to another table it should have this method
                    String sVal = (String) val;
                    int iVal = Integer.parseInt(sVal);
                    Expression<Integer> custId = root.join(key).get("id");
                    predicatesList.add(builder.equal(custId, iVal));
                } catch (NoSuchMethodException ex) {
                    predicatesList.add(builder.like(expresskey, filter.getValue() + "%"));
                } catch (SecurityException ex) {
                    Logger.getLogger(AbstractFacade.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            predicatesList.add(condition1);
            predicatesList.add(condition2);

        }
        cq.where(builder.and(predicatesList.<Predicate>toArray(
                new Predicate[predicatesList.size()])));
        javax.persistence.TypedQuery<T> query = getEntityManager().createQuery(cq);

        query.setFirstResult(first);

        query.setMaxResults(count);
        resultList = query.getResultList();
        // for debugging
        if (DEBUG) {
            debug(query);
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            if (filters != null) {
                LOGGER.log(Level.FINE, "filters: {0}  ", new Object[]{filters.entrySet()});
            } else {
                LOGGER.log(Level.FINE, "Lazy Load SQL Query    Filters Null");
            }
        }

        return resultList;
    }

}
