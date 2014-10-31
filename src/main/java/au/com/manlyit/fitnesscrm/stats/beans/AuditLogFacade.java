/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.AuditLog;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.util.Date;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author david
 */
@Stateless
public class AuditLogFacade extends AbstractFacade<AuditLog> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public AuditLogFacade() {
        super(AuditLog.class);
    }

    public synchronized void audit(Customers changedBy, Customers cust, String type, String detail, String changedFrom, String changedTo) {
        AuditLog al = new AuditLog(0);
        al.setTimestampOfChange(new Date());
        al.setChangedBy(changedBy);
        al.setCustomer(cust);
        al.setTypeOfChange(type);
        al.setDetailsOfChange(detail);
        al.setChangedFrom(changedFrom);
        al.setChangedTo(changedTo);
        create(al);

    }

}
