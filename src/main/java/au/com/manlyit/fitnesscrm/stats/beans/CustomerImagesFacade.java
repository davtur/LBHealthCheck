/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.db.CustomerImages;
import java.util.Date;
import java.util.List;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Expression;
import javax.persistence.criteria.Root;

/**
 *
 * @author david
 */
@Stateless
public class CustomerImagesFacade extends AbstractFacade<CustomerImages> {

    @EJB
    private ConfigMapFacade configMapFacade;
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public CustomerImagesFacade() {
        super(CustomerImages.class);
    }

    public List<CustomerImages> findByCustomerId(int customerId, boolean sortAsc) {
        List retList = null;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<CustomerImages> cq = cb.createQuery(CustomerImages.class);
            Root<CustomerImages> rt = cq.from(CustomerImages.class);
            Expression<Integer> custId = rt.get("customer_id");
            cq.where(cb.equal(custId, customerId));
            cq.select(rt);
            Expression<Date> express = rt.get("datetaken");
            if (sortAsc) {
                cq.orderBy(cb.asc(express));
            } else {
                cq.orderBy(cb.desc(express));
            }
            Query q = em.createQuery(cq);
            retList = q.getResultList();
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, configMapFacade.getConfig("PersistenceErrorOccured"));
        }
        return retList;
    }

    public List<CustomerImages> findAllByCustId(int customer_id, boolean sortAsc) {
        if (customer_id == 0) {
            return this.findAll();
        }
        String sort = "ASC";
        if (sortAsc) {
            sort = "ASC";
        } else {
            sort = "DESC";
        }
        Query q = em.createNativeQuery("SELECT * FROM customer_images t where  customer_id  = '" + customer_id + "' order by datetaken " + sort, CustomerImages.class);
        return q.getResultList();
    }
}
