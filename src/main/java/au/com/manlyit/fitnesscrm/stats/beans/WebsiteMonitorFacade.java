/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.WebsiteMonitor;

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
public class WebsiteMonitorFacade extends AbstractFacade<WebsiteMonitor> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//2010-01-01 00:00:00
    private static final Logger logger = Logger.getLogger(WebsiteMonitorFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public WebsiteMonitorFacade() {
        super(WebsiteMonitor.class);
    }

     public List<WebsiteMonitor> findSuccessfulTestResultsBetweenTwoDates(int test_id, Date top, Date bottom) {
    ArrayList<WebsiteMonitor> results = new ArrayList();
    String SQL = "SELECT * FROM website_monitor WHERE start_time > '" + sdf.format(bottom) + "' and start_time < '" + sdf.format(top) + "' and test_type = '" + test_id + "' and result = '0'  order by id DESC";
    logger.log(Level.INFO, SQL);
    Query q = em.createNativeQuery(SQL,WebsiteMonitor.class);
    results.addAll(q.getResultList());
    return results;
    }
    public List<WebsiteMonitor> findFailedTestResultsBetweenTwoDates(int test_id, Date top, Date bottom) {
    ArrayList<WebsiteMonitor> results = new ArrayList();
    String SQL = "SELECT * FROM website_monitor WHERE start_time > '" + sdf.format(bottom) + "' and start_time < '" + sdf.format(top) + "' and test_type = '" + test_id + "' and result > '0'  order by id DESC";
    logger.log(Level.INFO, SQL);
    Query q = em.createNativeQuery(SQL,WebsiteMonitor.class);
    results.addAll(q.getResultList());
    return results;
    }
    public List<WebsiteMonitor> findDateRangeOfWebsiteMonitors(Date startDate, Date endDate, boolean successful, boolean sortAsc,int testType) {
        List<WebsiteMonitor> retList = null;
        Integer successfulResult = 0;
        try {
            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<WebsiteMonitor> cq = cb.createQuery(WebsiteMonitor.class);
            Root<WebsiteMonitor> rt = cq.from(WebsiteMonitor.class);
            Expression<Date> stime = rt.get("startTime");
            Expression<Integer> res = rt.get("result");
            Expression<Integer> ttype = rt.get("testType");
            Predicate condition = null;
            if (successful) {
                condition = cb.equal(res, successfulResult);
            } else {
                condition = cb.equal(res, successfulResult).not();
            }
            Predicate condition2 = cb.between(stime, startDate, endDate);
            Predicate condition3 =  cb.equal(ttype, testType);
            cq.where(cb.and(condition, condition2,condition3));
            cq.select(rt);
            Expression<Date> express = rt.get("startTime");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = getEntityManager().createQuery(cq);
            retList = (List<WebsiteMonitor>) q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Persistence Error - findDateRangeOfWebsiteMonitors", e);
        }
        if (retList != null) {
            if (retList.isEmpty()) {
                retList = null;
            }
        }
        return retList;
    }
}
