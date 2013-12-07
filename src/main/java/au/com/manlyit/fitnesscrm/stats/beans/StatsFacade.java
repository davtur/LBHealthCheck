/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Stat;
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
public class StatsFacade extends AbstractFacade<Stat> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public StatsFacade() {
        super(Stat.class);
    }

    public List<Stat> findAll(int stat_type, int stats_taken_id) {
        if (stat_type == 0) {
            return this.findAll();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Stat> cq = cb.createQuery(Stat.class);
        Root<Stat> rt2 = cq.from(Stat.class);
        EntityType<Stat> Stats_Et = rt2.getModel();

        Query q = em.createNativeQuery("SELECT * FROM stat t where stat_type = '" + stat_type + "' and stats_taken_id = '" + stats_taken_id + "'", Stat.class);
        return q.getResultList();
    }

    public int count(int stat_type, int stats_taken_id) {
        if (stat_type == 0) {
            return count();
        }
        Query q = em.createNativeQuery("SELECT count(*) FROM stat t where stat_type = '" + stat_type + "' and stats_taken_id = '" + stats_taken_id + "'");
        return ((Long) q.getSingleResult()).intValue();
    }

    public List<Stat> findAll(int stats_taken_id) {
        if (stats_taken_id == 0) {
            return this.findAll();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Stat> cq = cb.createQuery(Stat.class);
        Root<Stat> rt2 = cq.from(Stat.class);
        EntityType<Stat> Stats_Et = rt2.getModel();

        Query q = em.createNativeQuery("SELECT * FROM stat t where stats_taken_id = '" + stats_taken_id + "'", Stat.class);
        return q.getResultList();
    }

    public int count(int stats_taken_id) {
        if (stats_taken_id == 0) {
            return count();
        }
        Query q = em.createNativeQuery("SELECT count(*) FROM stat t where stats_taken_id = '" + stats_taken_id + "'");
        return ((Long) q.getSingleResult()).intValue();
    }

    public List<Stat> findRange(int[] range, int stats_taken_id) {
        if (stats_taken_id == 0) {
            return this.findAll();
        }
        CriteriaBuilder cb = em.getCriteriaBuilder();

        CriteriaQuery<Stat> cq = cb.createQuery(Stat.class);
        Root<Stat> rt2 = cq.from(Stat.class);
        EntityType<Stat> Stats_Et = rt2.getModel();

        Query q = em.createNativeQuery("SELECT * FROM stat t where stats_taken_id = '" + stats_taken_id + "'", Stat.class);
        q.setMaxResults(range[1] - range[0]);
        q.setFirstResult(range[0]);
        return q.getResultList();
    }
    /*
    public void removeByBookingId(int bookingId) {
    Query q = em.createNativeQuery("SELECT * FROM te_projSysRequiredBookingDetails t where booking_Id = '" + bookingId + "'", TeprojSysRequiredBookingDetails.class);
    List<TeprojSysRequiredBookingDetails> itemsToRemove = q.getResultList();
    for (TeprojSysRequiredBookingDetails teprojSysRequiredBookingDetails : itemsToRemove) {
    em.remove(em.merge(teprojSysRequiredBookingDetails));
    }
    }
    
    public List<TeprojSysRequiredBookingDetails> findRangeByBookingId(int[] range, int bookingId) {
    if(bookingId == 0){
    return this.findRange(range);
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    
    CriteriaQuery<TeprojSysRequiredBookingDetails> cq = cb.createQuery(TeprojSysRequiredBookingDetails.class);
    Root<TeprojSysRequiredBookingDetails> rt = cq.from(TeprojSysRequiredBookingDetails.class);
    EntityType<TeprojSysRequiredBookingDetails> TeprojSysRequiredBookingDetails_Et = rt.getModel();
    
    Query q = em.createNativeQuery("SELECT * FROM te_projSysRequiredBookingDetails t where booking_ID = '" + bookingId + "'", TeprojSysRequiredBookingDetails.class);
    q.setMaxResults(range[1] - range[0]);
    q.setFirstResult(range[0]);
    return q.getResultList();
    }
    
    public List<TeprojSysRequiredBookingDetails> findAllByBookingId(int bookingId) {
    if(bookingId == 0){
    return this.findAll();
    }
    CriteriaBuilder cb = em.getCriteriaBuilder();
    
    CriteriaQuery<TeprojSysRequiredBookingDetails> cq = cb.createQuery(TeprojSysRequiredBookingDetails.class);
    Root<TeprojSysRequiredBookingDetails> rt2 = cq.from(TeprojSysRequiredBookingDetails.class);
    EntityType<TeprojSysRequiredBookingDetails> TeprojSysRequiredBookingDetails_Et = rt2.getModel();
    
    Query q = em.createNativeQuery("SELECT * FROM te_projSysRequiredBookingDetails t where booking_ID = '" + bookingId + "'", TeprojSysRequiredBookingDetails.class);
    return q.getResultList();
    }
    
    public int countByBookingId(int bookingId) {
    if(bookingId == 0){
    return count();
    }
    Query q = em.createNativeQuery("SELECT count(*) FROM te_projSysRequiredBookingDetails t where booking_ID = '" + bookingId + "'");
    return ((Long) q.getSingleResult()).intValue();
    }
     */
}
