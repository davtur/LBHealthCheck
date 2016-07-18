/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Notes;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Date;
import java.util.List;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.config.CacheUsage;
import org.eclipse.persistence.config.QueryHints;

/**
 *
 * @author david
 */
@Stateless
public class NotesFacade extends AbstractFacade<Notes> {

    private static final long serialVersionUID = 1L;

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    private static final Logger LOGGER = Logger.getLogger(NotesFacade.class.getName());

    @Inject
    private ConfigMapFacade configMapFacade;

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
            Expression<Date> express = rt.get("createTimestamp");

            cq.orderBy(cb.desc(express));
            TypedQuery<Notes> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            notes =  q.getResultList();
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Customer not found:" + cust.toString(), e);
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
            LOGGER.log(Level.INFO, "Customer not found:" + cust.toString(), e);
        }
        return -1;
    }

    public List<Notes> findByUserId(Customers user, boolean sortAsc) {
        List<Notes> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Notes> cq = cb.createQuery(Notes.class);
            Root<Notes> rt = cq.from(Notes.class);

            Expression<Customers> cust = rt.get("userId");
            cq.where(cb.equal(cust, user));
            Expression<Date> express = rt.get("createTimestamp");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            TypedQuery<Notes> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

}
