/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.db.ContractorRates;
import au.com.manlyit.fitnesscrm.stats.db.SessionTypes;
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
public class ContractorRatesFacade extends AbstractFacade<ContractorRates> {

    private static final Logger logger = Logger.getLogger(ContractorRatesFacade.class.getName());
    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ContractorRatesFacade() {
        super(ContractorRates.class);
    }

    public ContractorRates findAContractorRateByName(String name) {

        TypedQuery<ContractorRates> q;
        try {
            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ContractorRates> cq = cb.createQuery(ContractorRates.class);
            Root<ContractorRates> rt = cq.from(ContractorRates.class);

            Expression<String> express = rt.get("name");
            cq.where(cb.like(express, name));
            
            q = em.createQuery(cq);
            return q.getSingleResult();
        } catch (Exception e) {
            logger.log(Level.SEVERE, "ContractorRates Facade : findAContractorRateByName", e);
        }
        return null;

    }
}
