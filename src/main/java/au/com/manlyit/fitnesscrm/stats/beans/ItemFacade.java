/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.Item;
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
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class ItemFacade extends AbstractFacade<Item> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }
    private static final Logger logger = Logger.getLogger(ItemFacade.class.getName());
    public ItemFacade() {
        super(Item.class);
    }
    
     public List<Item> findAllActiveItems() {
        List<Item> retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<Item> cq = cb.createQuery(Item.class);
            Root<Item> rt = cq.from(Item.class);

            Expression<Item> item = rt.get("parent");
            Expression<Boolean> itemActive = rt.get("itemActive");
            cq.where(cb.and(cb.isNull(item), cb.equal(itemActive, 0)));

            TypedQuery<Item> q = em.createQuery(cq);

            retList = q.getResultList();
        } catch (Exception e) {

            logger.log(Level.INFO, "Exception : Could not find all Items.", e);
        }
        return retList;
    }
    
}
