/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.JobConfigMap;
import au.com.manlyit.fitnesscrm.stats.db.Tasks;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.*;

/**
 *
 * @author david
 */
@Stateless
public class JobConfigMapFacade extends AbstractFacade<JobConfigMap> {

    @PersistenceContext(unitName = "McasPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(JobConfigMapFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public JobConfigMapFacade() {
        super(JobConfigMap.class);
    }

    public int countByTask(Tasks currentTask) {
        try {
            List<JobConfigMap> retList = findConfigForTask(currentTask);
            if (retList == null) {
                return -1;
            } else {
                return retList.size();
            }



        } catch (Exception e) {
            logger.log(Level.WARNING, "Persistence Error - findChartForDate Monitoringcharts", e);
            return -1;
        }




    }
 public List<JobConfigMap> findConfigForTask(Tasks currentTask) {
        List<JobConfigMap> retList = null;

        try {

            CriteriaBuilder cb = getEntityManager().getCriteriaBuilder();
            CriteriaQuery<JobConfigMap> cq = cb.createQuery(JobConfigMap.class);
            Root<JobConfigMap> rt = cq.from(JobConfigMap.class);
            Expression<Tasks> cTask = rt.get("parentTask");


            Predicate condition = cb.equal(cTask, currentTask);

            cq.where(condition);
            cq.select(rt);
            Query q = getEntityManager().createQuery(cq);
            q.setHint("javax.persistence.cache.retrieveMode", "BYPASS");
            retList = (List<JobConfigMap>) q.getResultList();
        } catch (Exception e) {
            logger.log(Level.WARNING, "Persistence Error - findChartForDate Monitoringcharts", e);
        }
        if (retList != null) {
            if (retList.isEmpty()) {
                //retList = null;
                String nsg = "Didn't find any paremeters for task " + currentTask.getName() + " in the database.!!";
                logger.log(Level.INFO, nsg);
            }
        }
        return retList;
    }
  
}
