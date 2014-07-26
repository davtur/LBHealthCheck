/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Schedule;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class ScheduleFacade extends AbstractFacade<Schedule> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ScheduleFacade() {
        super(Schedule.class);
    }
    
    public List<Schedule> findDateRange( Date startDate, Date endDate) {
        CriteriaBuilder builder = em.getCriteriaBuilder();
        CriteriaQuery<Schedule> cq = builder.createQuery(Schedule.class);
        Root<Schedule> ssiScheduleRoot = cq.from(Schedule.class);
        Expression<Date> sde = ssiScheduleRoot.get("shedStartdate");
        cq.where(builder.greaterThan(sde, startDate), builder.lessThan(sde, endDate));
        cq.select(ssiScheduleRoot);
        Query q = em.createQuery(cq);
        return q.getResultList();
}
}
