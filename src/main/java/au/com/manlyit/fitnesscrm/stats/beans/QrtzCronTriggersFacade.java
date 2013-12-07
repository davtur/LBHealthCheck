/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.QrtzCronTriggers;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author david
 */
@Stateless
public class QrtzCronTriggersFacade extends AbstractFacade<QrtzCronTriggers> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    protected EntityManager getEntityManager() {
        return em;
    }

    public QrtzCronTriggersFacade() {
        super(QrtzCronTriggers.class);
    }
    
}
