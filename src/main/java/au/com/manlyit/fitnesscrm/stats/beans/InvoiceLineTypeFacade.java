/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.beans;


import au.com.manlyit.fitnesscrm.stats.db.InvoiceLineType;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 *
 * @author david
 */
@Stateless
public class InvoiceLineTypeFacade extends AbstractFacade<InvoiceLineType> {
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public InvoiceLineTypeFacade() {
        super(InvoiceLineType.class);
    }
    
}
