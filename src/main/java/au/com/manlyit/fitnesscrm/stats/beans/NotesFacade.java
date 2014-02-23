/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
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
import javax.persistence.criteria.Root;
import static org.atmosphere.annotation.AnnotationUtil.logger;

/**
 *
 * @author david
 */
@Stateless
public class NotesFacade extends AbstractFacade<Notes> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(NotesFacade.class.getName());

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public NotesFacade() {
        super(Notes.class);
    }

    public List<Notes> findNotesByCustomer(Customers cust) {
        List<Notes> notes = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Notes> cq = cb.createQuery(Notes.class);
            Root<Notes> rt = cq.from(Notes.class);

            Expression<Customers> custUsername = rt.get("userId");
            cq.where(cb.equal(custUsername, cust));

            Query q = em.createQuery(cq);
            notes = (List<Notes>) q.getResultList();
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found:" + cust.toString(), e);
        }
        return notes;
    }

    public int countNotesByCustomer(Customers cust) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Long> rt = cq.from(Long.class);

            Expression<Customers> custUsername = rt.get("userId");
            cq.where(cb.equal(custUsername, cust));
            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found:" + cust.toString(), e);
        }
        return -1;
    }

    
}
