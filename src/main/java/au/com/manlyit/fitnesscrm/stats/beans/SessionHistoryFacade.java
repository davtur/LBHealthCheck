/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Expenses;
import au.com.manlyit.fitnesscrm.stats.db.Participants;
import au.com.manlyit.fitnesscrm.stats.db.SessionBookings;
import au.com.manlyit.fitnesscrm.stats.db.SessionHistory;
import au.com.manlyit.fitnesscrm.stats.db.SessionTimetable;
import au.com.manlyit.fitnesscrm.stats.db.SessionTrainers;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.internal.jpa.EJBQueryImpl;
import org.eclipse.persistence.jpa.JpaEntityManager;

import org.eclipse.persistence.queries.DatabaseQuery;
import org.eclipse.persistence.sessions.DatabaseRecord;
import org.eclipse.persistence.sessions.Session;

/**
 *
 * @author david
 */
@Stateless
public class SessionHistoryFacade extends AbstractFacade<SessionHistory> {

    private static final Logger logger = Logger.getLogger(SessionHistoryFacade.class.getName());
    private static final boolean DEBUG = false;
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

            Predicate condition1 = cb.lessThan(stime, endDate);
            Predicate condition2 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition3 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition1, condition2, condition3));
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<SessionHistory> q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

    public List<SessionHistory> findSessionBookingsByParticipantAndDateRange(Customers participant, Date startDate, Date endDate, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, SessionBookings> jn = rt.joinCollection("sessionBookingsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.lessThan(stime, endDate);
            Predicate condition2 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition3 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition1, condition2, condition3));
            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<SessionHistory> q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return retList;
    }

    public int countSessionBookingsByParticipantAndDateRange(Customers participant, Date startDate, Date endDate) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Long> rt = cq.from(Long.class);

            Join<SessionHistory, Participants> jn = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.between(stime, startDate, endDate);
            Predicate condition2 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition1, condition2));
            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.log(Level.INFO, "Participant not found:" + participant.toString(), e);
        }
        return -1;
    }

    public int countSessionsByParticipantAndDateRange(Customers participant, Date startDate, Date endDate) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class);
            Root<Long> rt = cq.from(Long.class);

            Join<SessionHistory, Participants> jn = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.between(stime, startDate, endDate);
            Predicate condition2 = cb.equal(sessionParticipant, participant);
            cq.where(cb.and(condition1, condition2));
            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();
        } catch (Exception e) {
            logger.log(Level.INFO, "Participant not found:" + participant.toString(), e);
        }
        return -1;
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

    public List<SessionHistory> findSessionsWithoutExpenseLogged(boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Expression<Expenses> expense = rt.get("expenseId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition = cb.isNull(expense);
            cq.where(condition);
            cq.select(rt);

            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<SessionHistory> q = em.createQuery(cq);
            retList = q.getResultList();
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

            Predicate condition1 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition2 = cb.lessThan(stime, endDate);
            Predicate condition3 = cb.equal(sessionTrainer, trainer);
            cq.where(cb.and(condition1, condition2, condition3));
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

    public SessionHistory findSessionBySessionTimetable(Date sessionTimestamp, SessionTimetable st) {

        //Customers trainer, Date startDate, Date endDate, boolean sortAsc
        List<SessionHistory> retList = null;
        SessionHistory matchingSession = null;
        // GregorianCalendar gc = new GregorianCalendar();
        // gc.setTime(template.getSessiondate());
        //SELECT * FROM fitnessStats.session_history h ,fitnessStats.session_timetable t  where t.id =2 AND  CAST(h.sessionDate as Time) = CAST(t.sessionDate as Time);

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            //Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            // Expression<Customers> sessionTrainer = jn.get("customerId");
            Expression<SessionTimetable> sessionTemplate = rt.get("sessionTemplate");
            Expression<Date> stime = rt.get("sessiondate");
            //Expression<SessionTypes> type = rt.get("sessionTypesId");

            Predicate condition1 = cb.equal(stime, sessionTimestamp);
            // Predicate condition2 = cb.equal(type, template.getSessionTypesId());
            // Predicate condition3 = cb.equal(sessionTrainer, st.getTrainerId());
            Predicate condition4 = cb.equal(sessionTemplate, st);
            cq.where(cb.and(condition1, condition4));
            cq.select(rt);

            TypedQuery<SessionHistory> q = em.createQuery(cq);
            retList = q.getResultList();
            if (DEBUG) {
                debug(q);
            }
            if (retList.size() == 1) {
                matchingSession = retList.get(0);
            } else {
                if (retList.size() > 1) {
                    matchingSession = retList.get(0);
                    logger.log(Level.WARNING, "findSessionBySessionTimetable: {3} matches found for date: {0} and session timetable: id: {1},Title:{2}",new Object[]{ sessionTimestamp.toString(),st.getId(),st.getSessionTitle(),retList.size()});
                    for(SessionHistory s:retList){
                        logger.log(Level.WARNING, "duplicate Session History Object: id:{0}, Session Date:{1}.",new Object[]{s.getId(),s.getSessiondate()});
                    
                    }
                } else {
                    logger.log(Level.INFO, "findSessionBySessionTimetable: no sessions found for session timetable: {0}", sessionTimestamp.toString());
                }
            }
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return matchingSession;
    }

    public List<SessionHistory> findFilteredSessions(Customers[] participants, Customers[] trainers, Date startDate, Date endDate, boolean sortAsc) {

        // Dates must be valid or return empty list
        // if participants or trainers is empty or null use a wildcard.
        List<SessionHistory> retList = null;
        boolean showAllTrainers = false;
        boolean showAllParticipants = false;
        ArrayList<Predicate> predicatesList = new ArrayList<>();
        ArrayList<Predicate> predicatesList2 = new ArrayList<>();
        if (trainers == null || trainers.length == 0) {
            showAllTrainers = true;
        }
        if (participants == null || participants.length == 0) {
            showAllParticipants = true;
        }
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class);
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Join<SessionHistory, Participants> jn2 = rt.joinCollection("participantsCollection");
            Expression<Customers> sessionParticipant = jn2.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");
            Predicate condition1 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition2 = cb.lessThan(stime, endDate);

            if (showAllTrainers == false) {
                for (Customers trainer : trainers) {
                    predicatesList.add(cb.equal(sessionTrainer, trainer));
                }
            }
            if (showAllParticipants == false) {
                for (Customers participant : participants) {
                    predicatesList2.add(cb.equal(sessionParticipant, participant));
                }
            }

            cq.where(cb.and(condition1, condition2));
            if (showAllTrainers == false && showAllParticipants == false) {
                cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])), cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList.size()])), condition1, condition2));
            }
            if (showAllTrainers == true && showAllParticipants == false) {
                cq.where(cb.and(cb.or(predicatesList2.<Predicate>toArray(new Predicate[predicatesList.size()])), condition1, condition2));
            }
            if (showAllTrainers == false && showAllParticipants == true) {
                cq.where(cb.and(cb.or(predicatesList.<Predicate>toArray(new Predicate[predicatesList.size()])), condition1, condition2));
            }
            if (showAllTrainers == true && showAllParticipants == true) {
                cq.where(cb.and(condition1, condition2));
            }

            if (sortAsc) {
                cq.orderBy(cb.asc(stime));
            } else {
                cq.orderBy(cb.desc(stime));
            }
            TypedQuery<SessionHistory> q = em.createQuery(cq);

            retList = q.getResultList();
            // for debugging
            Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
            DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
            databaseQuery.prepareCall(session, new DatabaseRecord());
            String sqlString = databaseQuery.getSQLString();
            //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
            // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
            logger.log(Level.INFO, "findFilteredSessions SQL Query String: {0}  -----------------Records Found:{1},", new Object[]{sqlString, retList.size()});

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<SessionHistory> findSessionsByDateRange(Date startDate, Date endDate, boolean sortAsc) {
        List<SessionHistory> retList = null;

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class
            );
            Root<SessionHistory> rt = cq.from(SessionHistory.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition2 = cb.lessThan(stime, endDate);

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

    public int countSessionsByTrainerAndDateRange(Customers trainer, Date startDate, Date endDate, boolean sortAsc) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class
            );
            Root<Long> rt = cq.from(Long.class);

            Join<SessionHistory, SessionTrainers> jn = rt.joinCollection("sessionTrainersCollection");
            Expression<Customers> sessionTrainer = jn.get("customerId");
            Expression<Date> stime = rt.get("sessiondate");

            Predicate condition1 = cb.greaterThanOrEqualTo(stime, startDate);
            Predicate condition2 = cb.lessThan(stime, endDate);
            Predicate condition3 = cb.equal(sessionTrainer, trainer);

            cq.where(cb.and(condition1, condition2, condition3));

            cq.select(cb.count(rt));

            Query q = em.createQuery(cq);
            return ((Long) q.getSingleResult()).intValue();

        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }

        return -1;
    }

    public int countSessionsByTrainer(Customers trainer) {

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Long> cq = cb.createQuery(Long.class
            );
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
            CriteriaQuery<SessionHistory> cq = cb.createQuery(SessionHistory.class
            );
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
