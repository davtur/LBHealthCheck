/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Participants;
import java.util.List;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;

/**
 *
 * @author david
 */
@Stateless
public class ParticipantsFacade extends AbstractFacade<Participants> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ParticipantsFacade() {
        super(Participants.class);
    }

    public int countByCustId(int custId) {
        if (custId == 0) {
            return 0;
        }
        Query q = em.createNativeQuery("SELECT count(*) FROM participants where customer_id = '" + custId + "'");
        return ((Long) q.getSingleResult()).intValue();
    }

    public List<Participants> findAllByCustId(int customer_id, boolean sortAsc) {

        String sort = "ASC";
        if (sortAsc) {
            sort = "ASC";
        } else {
            sort = "DESC";
        }
        Query q = em.createNativeQuery("SELECT * FROM participants t where  customer_id  = '" + customer_id + "' order by customer_id " + sort, Participants.class);
        return q.getResultList();
    }
}
