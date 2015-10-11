/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.persistence.EntityManager;
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

    private static final Logger logger = Logger.getLogger(AbstractFacade.class.getName());
    private final Class<T> entityClass;

    public AbstractFacade(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    protected abstract EntityManager getEntityManager();

    public void create(T entity) {
        try {
            getEntityManager().persist(entity);
            String message = "Entity Created: " + entity.toString();
            logger.log(Level.INFO, message);
        } catch (Exception e) {
            logger.log(Level.WARNING, "<-------------------------------------------------- Abstract Facade - could not create Entity ------------------------------------------------------------------------>");
            logger.log(Level.WARNING, e.getMessage(), e);
            logger.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            Throwable t = e.getCause();
            if (t != null) {
                logger.log(Level.WARNING, t.getMessage(), t);
            }
            logger.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");
            t = e.getCause().getCause();
            if (t != null) {
                logger.log(Level.WARNING, t.getMessage(), t);
            }
            logger.log(Level.WARNING, "<---------------------------------------------------------------------------------------------------------------------------------------------------------------------->");

        }
    }

    public void edit(T entity) {
        getEntityManager().merge(entity);
        String message = "Entity Merged: " + entity.toString();
        logger.log(Level.INFO, message);

    }

    public void remove(T entity) {

        getEntityManager().remove(getEntityManager().merge(entity));
        String message = "Entity Removed: " + entity.toString();
        logger.log(Level.INFO, message);
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
        String message = "Lazy Loading: " + entityClass.getSimpleName()+",Rows="+count+", First="+first+", SortField="+sortField+", SortOrder="+sortOrder.name();
        logger.log(Level.INFO, message);
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
        // for debugging
        Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
        DatabaseQuery databaseQuery = ((EJBQueryImpl) query).getDatabaseQuery();
        databaseQuery.prepareCall(session, new DatabaseRecord());
        String sqlString = databaseQuery.getSQLString();
        //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
        // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
        if (filters != null) {
            logger.log(Level.FINE, "Lazy Load SQL Query String: {0}  ----------------- {1}", new Object[]{sqlString, filters.entrySet()});
        } else {
            logger.log(Level.FINE, "Lazy Load SQL Query String: {0}  ----------------- Filters Null", new Object[]{sqlString});
        }

        return resultList;
    }
     public List<T> loadDateRange(int first, int count, String sortField, SortOrder sortOrder, Map<String, Object> filters,Date startDate, Date endDate, String dateRangeFieldName) {
        List<T> resultList;
        String message = "Lazy Loading: " + entityClass.getSimpleName()+",Rows="+count+", First="+first+", SortField="+sortField+", SortOrder="+sortOrder.name()+", dateRangeFieldName="+dateRangeFieldName+", startDate="+startDate.toString()+", endDate="+endDate.toString();
        logger.log(Level.INFO, message);
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
             Expression<Date> stime = root.get(dateRangeFieldName);

            Predicate condition1 = builder.greaterThanOrEqualTo(stime, startDate);
            Predicate condition2 = builder.lessThan(stime, endDate);
            
            
            cq.where(builder.and(predicatesList.<Predicate>toArray(
                    new Predicate[predicatesList.size()])),condition1,condition2);
        }
        javax.persistence.TypedQuery<T> query = getEntityManager().createQuery(cq);

        query.setFirstResult(first);

        query.setMaxResults(count);
        resultList = query.getResultList();
        // for debugging
        Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
        DatabaseQuery databaseQuery = ((EJBQueryImpl) query).getDatabaseQuery();
        databaseQuery.prepareCall(session, new DatabaseRecord());
        String sqlString = databaseQuery.getSQLString();
        //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
        // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
        if (filters != null) {
            logger.log(Level.FINE, "Lazy Load SQL Query String: {0}  ----------------- {1}", new Object[]{sqlString, filters.entrySet()});
        } else {
            logger.log(Level.FINE, "Lazy Load SQL Query String: {0}  ----------------- Filters Null", new Object[]{sqlString});
        }

        return resultList;
    }
      

}
