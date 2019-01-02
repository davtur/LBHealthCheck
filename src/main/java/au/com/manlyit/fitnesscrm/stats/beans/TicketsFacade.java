/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.beans.util.PaymentStatus;
import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerState;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.Payments;
import au.com.manlyit.fitnesscrm.stats.db.Tickets;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;
import org.eclipse.persistence.exceptions.QueryException;
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
public class TicketsFacade extends AbstractFacade<Tickets> {

    private static final Logger LOGGER = Logger.getLogger(TicketsFacade.class.getName());
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static boolean debug = false;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public TicketsFacade() {
        super(Tickets.class);
    }

    public List<Tickets> findCustomerTicketsByDateRange(Customers cust, Date startDate, Date endDate, boolean sortAsc) {
        List<Tickets> retList = null;
        

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Tickets> cq = cb.createQuery(Tickets.class);
            Root<Tickets> rt = cq.from(Tickets.class);
            Expression<Customers> customer;
            Expression<Date> orderByDate;
            Expression<Date> validFrom;
            Expression<Date> expires;

            validFrom = rt.get("validFrom");
            expires = rt.get("expires");
            customer = rt.get("customer");

            orderByDate = validFrom;
            Predicate p1 = cb.greaterThanOrEqualTo(validFrom, startDate);
            Predicate p2 = cb.lessThanOrEqualTo(expires, endDate);
            Predicate p3 = cb.equal(customer, cust);

            cq.where(cb.and(p1,p2,p3));

            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(orderByDate));
            } else {
                cq.orderBy(cb.desc(orderByDate));
            }
            TypedQuery<Tickets> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            retList = q.getResultList();

            // for debugging
            if (debug) {
                Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
                DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
                databaseQuery.prepareCall(session, new DatabaseRecord());
                String sqlString = databaseQuery.getSQLString();
                //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
                // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
                LOGGER.log(Level.INFO, "Get Customer Tickets by Date Range- SQL Query String: {0}  -----------------Records Found:{3}, startDate: {1},endDate: {2}", new Object[]{sqlString, startDate, endDate, retList.size()});
            }
        } catch (QueryException e) {
            LOGGER.log(Level.INFO, "TicketsFacade findCustomerTicketsByDateRange", e.getMessage());
        }

        return retList;
    }
    public List<Tickets> findCustomerTicketsValidForSessionDate(Customers cust, Date sessionDate,  boolean sortAsc) {
        List<Tickets> retList = null;
        

        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Tickets> cq = cb.createQuery(Tickets.class);
            Root<Tickets> rt = cq.from(Tickets.class);
            Expression<Customers> customer;
            Expression<Date> orderByDate;
            Expression<Date> validFrom;
            Expression<Date> expires;
            Expression<Date> dateUsed;

            validFrom = rt.get("validFrom");
            expires = rt.get("expires");
            customer = rt.get("customer");
            dateUsed = rt.get("dateUsed");

            orderByDate = validFrom;
            Predicate p1 = cb.lessThanOrEqualTo(validFrom, sessionDate);
            Predicate p2 = cb.greaterThanOrEqualTo(expires, sessionDate);
            Predicate p3 = cb.equal(customer, cust);
            Predicate p4 = cb.isNull(dateUsed);
            cq.where(cb.and(p1,p2,p3,p4));

            cq.select(rt);
            if (sortAsc) {
                cq.orderBy(cb.asc(orderByDate));
            } else {
                cq.orderBy(cb.desc(orderByDate));
            }
            TypedQuery<Tickets> q = em.createQuery(cq);
            //q.setHint(QueryHints.CACHE_USAGE, CacheUsage.CheckCacheThenDatabase);
            retList = q.getResultList();

            // for debugging
            if (debug) {
                Session session = getEntityManager().unwrap(JpaEntityManager.class).getActiveSession();
                DatabaseQuery databaseQuery = ((EJBQueryImpl) q).getDatabaseQuery();
                databaseQuery.prepareCall(session, new DatabaseRecord());
                String sqlString = databaseQuery.getSQLString();
                //This SQL will contain ? for parameters. To get the SQL translated with the arguments you need a DatabaseRecord with the parameter values.
                // String sqlString2 = databaseQuery.getTranslatedSQLString(session, recordWithValues);
                LOGGER.log(Level.INFO, "Get Customer Tickets by Session Date - SQL Query String: {0}  -----------------Records Found:{3}, session Date: {1}", new Object[]{sqlString, sessionDate,retList.size()});
            }
        } catch (QueryException e) {
            LOGGER.log(Level.INFO, "TicketsFacade findCustomerTicketsByDateRange", e.getMessage());
        }

        return retList;
    }


}
