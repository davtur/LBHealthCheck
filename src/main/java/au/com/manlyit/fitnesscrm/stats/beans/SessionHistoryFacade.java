/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Participants;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class SessionHistoryFacade extends AbstractFacade<SessionHistory> {

    private static final Logger logger = Logger.getLogger(SessionHistoryFacade.class.getName());
    @Inject
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss");//2010-01-01 00:00:00

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public SessionHistoryFacade() {
        super(SessionHistory.class);
    }

    public int countByCustId(int customer_id) {

        Query q = em.createNativeQuery("SELECT sh.id,sh.sessiondate,sh.session_types_id,sh.comments FROM participants p ,session_history sh WHERE p.session_history_id = sh.id  AND p.customer_id= '" + customer_id + "'");
        return ((Long) q.getSingleResult()).intValue();
    }

    public List<SessionHistory> findAllByCustId(int customer_id, boolean sortAsc) {

        String sort = "ASC";
        if (sortAsc) {
            sort = "ASC";
        } else {
            sort = "DESC";
        }
        Query q = em.createNativeQuery("SELECT sh.id,sh.sessiondate,sh.session_types_id,sh.comments FROM participants p ,session_history sh WHERE p.session_history_id = sh.id  AND p.customer_id= '" + customer_id + "' order by sh.sessiondate " + sort, SessionHistory.class);
        return q.getResultList();
    }

    public int findMySessionsChartData(int customer_id, Date top, Date bottom, String sessionTypeIds) {
        // sessionTypeIds should be a csv string like 1,2,3,7,8
        Query q = em.createNativeQuery("SELECT COUNT(*) FROM session_types st,participants p ,session_history sh WHERE p.session_history_id = sh.id AND sh.session_types_id = st.id AND p.customer_id= '" + customer_id + "' AND sh.sessiondate >  '" + sdf.format(bottom) + "'  AND sh.sessiondate < '" + sdf.format(top) + "' AND st.id IN('" + sessionTypeIds + "')");

        return ((Long) q.getSingleResult()).intValue();
    }


    public List<SessionHistory> findAll(boolean sortAsc) {
        List<SessionHistory> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);
            //
            //Expression<Integer> custId = rt.get("customer_id");
            //cq.where(cb.equal(custId, customerId));
            cq.select(rt);
            Expression<Date> express = rt.get("sessiondate");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            retList = (List<SessionHistory>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<SessionHistory> findSessionsByParticipantAndDateRange(Customers participant, Date startDate, Date endDate, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, Participants> jn = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.between(stime, startDate, endDate);
            Predicate condition2 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition1, condition2));
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            Query q = em.createQuery(cq);
            retList = (List<SessionHistory>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

    public List<SessionHistory> findSessionsByParticipant(Customers participant, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, Participants> jn = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition2 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition2));
            cq.select(rt);

            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            Query q = em.createQuery(cq);
            retList = (List<SessionHistory>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

    public int countSessionsByParticipant(Customers participant) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Long> rt = cq.from(Long.class);

            Join<SessionHistory, Participants> jn = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Predicate condition = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition));
            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.log(Level.INFO, "Participant not found:" + participant.toString(), e);
        }
        return -1;
    }

    public List<SessionHistory> findSessionsByTrainerAndDateRange(Customers trainer, Date startDate, Date endDate, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.between(stime, startDate, endDate);
            Predicate condition2 = cb.equal(sessionTrainer, trainer);
            cq.where(cb.and(condition1, condition2));
            cq.select(rt);

            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            Query q = em.createQuery(cq);
            retList = (List<SessionHistory>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

    public int countSessionsByTrainer(Customers trainer) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Long> rt = cq.from(Long.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Predicate condition = cb.equal(sessionTrainer, trainer);
            cq.where(cb.and(condition));
            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.log(Level.INFO, "Customer not found:" + trainer.toString(), e);
        }
        return -1;
    }

    public List<SessionHistory> findSessionsByTrainer(Customers trainer, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");
            Predicate condition = cb.equal(sessionTrainer, trainer);
            cq.where(cb.and(condition));
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            Query q = em.createQuery(cq);
            retList = (List<SessionHistory>) q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }
}
