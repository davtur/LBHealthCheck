/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.QrtzJobDetails;
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
public class QrtzJobDetailsFacade extends AbstractFacade<QrtzJobDetails> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public QrtzJobDetailsFacade() {
        super(QrtzJobDetails.class);
    }
    public void synchDBwithJPA(){
        try {
            em.flush();
        } catch (Exception e) {
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Couldn't flush persistence layer to the underlying database:", e);
        }
                
    }
    
}
