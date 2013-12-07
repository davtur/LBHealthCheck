/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Monitoringcharts;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
public class MonitoringchartsFacade extends AbstractFacade<Monitoringcharts> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(MonitoringchartsFacade.class.getName());
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");//2010-01-01 00:00:00

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public MonitoringchartsFacade() {
        super(Monitoringcharts.class);
    }
    
    public List<Monitoringcharts> findChartsBetweenTwoDates(Date day) {
        ArrayList<Monitoringcharts> results = new ArrayList();
        String start = sdf.format(day) + " 00:00:00";
        String end = sdf.format(day) + " 23:59:59";
        String SQL = "SELECT * FROM website_monitor WHERE start_time > '" + start + "' and start_time < '" + end + "' ";
        logger.log(Level.INFO, SQL);
        Query q = em.createNativeQuery(SQL, Monitoringcharts.class);
        results.addAll(q.getResultList());
        return results;
    }
/*
     * 
     * 
     * JPA 2.0 provides a set of standard query hints to allow refreshing or bypassing the cache. The query hints are defined on the two enum classes CacheRetrieveMode and CacheStoreMode.

Query hints:

    javax.persistence.cache.retrieveMode : CacheRetrieveMode
        BYPASS : Ignore the cache, and build the object directly from the database result.
        USE : Allow the query to use the cache. If the object/data is already in the cache, the cached object/data will be used.
    javax.persistence.cache.storeMode : CacheStoreMode
        BYPASS : Do not cache the database results.
        REFRESH : If the object/data is already in the cache, then refresh/replace it with the database results.
        USE : Cache the objects/data returned from the query.

[edit] 
     * 
     */
    public List<Monitoringcharts> findChartForDate(Date startDate,Date endDate, boolean sortAsc) {
        List<Monitoringcharts> retList = null;
 
        try {
            
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<Monitoringcharts> cq = cb.createQuery(Monitoringcharts.class);
            Root<Monitoringcharts> rt = cq.from(Monitoringcharts.class);
            Expression<Date> stime = rt.get("date");

            Predicate condition = cb.between(stime, startDate,endDate);
            cq.where(condition);
            cq.select(rt);
            Expression<Date> express = rt.get("date");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = getEntityManager().createQuery(cq);
            q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            retList = (List<Monitoringcharts>) q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Persistence Error - findChartForDate Monitoringcharts", e);
        }
        if (retList != null) {
            if (retList.isEmpty()) {
                retList = null;
                String nsg =  "Didn't find any charts for date "+sdf.format(startDate)+" in the database.!!" ;
                logger.log(Level.INFO,nsg);
            }
        }
        return retList;
    }
}
