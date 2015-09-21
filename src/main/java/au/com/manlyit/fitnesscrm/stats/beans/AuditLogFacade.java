/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.AuditLog;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author david
 */
@Stateless
public class AuditLogFacade extends AbstractFacade<AuditLog> {
    private static final Logger logger = Logger.getLogger(AuditLogFacade.class.getName());
    private static final long serialVersionUID = 1L;
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
        try {
            al.setTimestampOfChange(new Date());
            al.setChangedBy(changedBy);
            al.setCustomer(cust);
            
            if (changedFrom.length() > 255) {
                detail += ", changed From:" + changedFrom;
                changedFrom = "Truncated - see Details";
            }
            if (changedTo.length() > 255) {
                detail += ", changed To:" + changedTo;
                changedTo = "Truncated - see Details";
            }
            if (type.length() > 255) {
                detail += ",Type:" + type;
                type = type.substring(0, 230) + " : Truncated - see Details";
            }
            if (detail.length() > 65534) {
                
                detail = detail.substring(0, 65500) + " : Truncated at 65500 chars";
            }
            al.setTypeOfChange(type);
            al.setDetailsOfChange(detail);
            al.setChangedFrom(changedFrom);
            al.setChangedTo(changedTo);
            create(al);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "AuditLogFacade - audit method threw an exception:",e);
        }

    }

}
