/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import au.com.manlyit.fitnesscrm.stats.db.Stat;
import au.com.manlyit.fitnesscrm.stats.db.StatsTaken;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;
import javax.persistence.metamodel.EntityType;

/**
 *
 * @author david
 */
@Stateless
public class StatsTakenFacade_old extends AbstractFacade<StatsTaken> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public StatsTakenFacade_old() {
        super(StatsTaken.class);
    }
 public List<StatsTaken> findAllByCustId( int customer_id) {
        if (customer_id == 0) {
            return this.findAll();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<StatsTaken> cq = cb.createQuery(StatsTaken.class);
        Root<StatsTaken> rt2 = cq.from(StatsTaken.class);
        EntityType<StatsTaken> Stats_Et = rt2.getModel();

        Query q = em.createNativeQuery("SELECT * FROM stats_taken t where  customer_id  = '" + customer_id + "' order by date_recorded", StatsTaken.class);
        return q.getResultList();
    }

    public int countByCustId( int customer_id) {
        if (customer_id == 0) {
            return count();
        }
        Query q = em.createNativeQuery("SELECT count(*) FROM stats_taken t where  customer_id  = '" + customer_id + "'");
        return ((Long) q.getSingleResult()).intValue();
    }
    public void synch(){

        em.flush();
    }
  
}
