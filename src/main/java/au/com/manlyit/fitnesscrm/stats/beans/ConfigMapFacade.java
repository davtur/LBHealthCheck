/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.beans;

import au.com.manlyit.fitnesscrm.stats.classes.util.JsfUtil;
import au.com.manlyit.fitnesscrm.stats.classes.util.StringEncrypter;
import au.com.manlyit.fitnesscrm.stats.db.ConfigMap;
import java.util.List;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
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
public class ConfigMapFacade extends AbstractFacade<ConfigMap> {

    @PersistenceContext(unitName = "FitnessStatsPU")
    private EntityManager em;
    private static final Logger logger = Logger.getLogger(ConfigMapFacade.class.getName());
    private final StringEncrypter encrypter = new StringEncrypter("(H6%efRdswWw2@8j&6yvFdsP)");
    private static final String ETAG = "{!-ENCRYPT-!}";

    @Override
    protected EntityManager getEntityManager() {
        return em;
    }

    public ConfigMapFacade() {
        super(ConfigMap.class);
    }

    public void createEncrypted(ConfigMap cm) {
        cm.setConfigvalue(ETAG + encrypter.encrypt(cm.getConfigvalue()));
        create(cm);
    }

    public String getConfig(String key) {
        String val = getValueFromKey(key);
        if (val.indexOf(ETAG) == 0) {
            // its encrypted so decrypt
            val = val.substring(ETAG.length());
            val = encrypter.decrypt(val);
        }
        return val;
    }

    public String getValueFromKey(String configkey) {
        String value = null;
        String ky = null;
        if (configkey == null || configkey.trim().isEmpty()) {
            logger.log(Level.WARNING, "Refusing to get a config value from the ConfigMap database table as the key supplied is NULL or empty :(");
        } else {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
                Root<ConfigMap> rt = cq.from(ConfigMap.class);
                Expression<String> expresskey = rt.get("configkey");
                cq.where(cb.equal(expresskey, configkey));
                Query q = em.createQuery(cq);
                //Query q = em.createNativeQuery("SELECT * FROM configMap where key = '" + configkey + "'", ConfigMap.class);
                ConfigMap cm = (ConfigMap) q.getSingleResult();
                if (cm != null) {
                    value = cm.getConfigvalue();
                    ky = cm.getConfigkey();
                }
                logger.log(Level.FINER, "Key={0}, Value={1}", new Object[]{ky, value});
            } catch (NoResultException e) {
                logger.log(Level.WARNING, "NoResultException: Failed to find this key ({0}) in the ConfigMap database table :(", configkey);
                value = "??? ConfigMap key not found (" + configkey + ")";
                getEntityManager().persist(new ConfigMap(0, configkey, value));
            } catch (NonUniqueResultException e) {

                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
                Root<ConfigMap> rt = cq.from(ConfigMap.class);
                Expression<String> expresskey = rt.get("configkey");
                cq.where(cb.equal(expresskey, configkey));

                Query q = em.createQuery(cq);
                List<ConfigMap> cmList = (List<ConfigMap>) q.getResultList();
                ConfigMap cm = cmList.get(0);
                value = cm.getConfigvalue();
                int numberOfDups = cmList.size();
                logger.log(Level.SEVERE, "Found {0} duplicate keys for key {1} in the ConfigMap database table. Using the first one found :(", new Object[]{numberOfDups, configkey});
            } catch (Exception e) {
                logger.log(Level.INFO, "Key={0}, Value={1}", new Object[]{ky, value});
                logger.log(Level.WARNING, "Failed to get this key " + configkey + " from the ConfigMap database table due to an exception:(", e);
            }
        }
        return value;
    }

    public ConfigMap getConfigMapFromKey(String configkey) {
        ConfigMap cm = null;
        String value = null;
        String ky = null;
        if (configkey == null || configkey.trim().isEmpty()) {
            logger.log(Level.WARNING, "Refusing to get a config value from the ConfigMap database table as the key supplied is NULL or empty :(");
        } else {
            try {
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
                Root<ConfigMap> rt = cq.from(ConfigMap.class);
                Expression<String> expresskey = rt.get("configkey");
                cq.where(cb.equal(expresskey, configkey));
                Query q = em.createQuery(cq);
                cm = (ConfigMap) q.getSingleResult();
                if (cm != null) {
                    value = cm.getConfigvalue();
                    ky = cm.getConfigkey();
                }

                logger.log(Level.FINER, "Key={0}, Value={1}", new Object[]{ky, value});

            } catch (NoResultException e) {
                logger.log(Level.WARNING, "NoResultException: Failed to find this key ({0}) in the ConfigMap database table :(", configkey);
                value = "??? ConfigMap key not found (" + configkey + ")";
                getEntityManager().persist(new ConfigMap(0, configkey, value));
                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
                Root<ConfigMap> rt = cq.from(ConfigMap.class);
                Expression<String> expresskey = rt.get("configkey");
                cq.where(cb.equal(expresskey, configkey));
                Query q = em.createQuery(cq);

                cm = (ConfigMap) q.getSingleResult();
            } catch (NonUniqueResultException e) {

                CriteriaBuilder cb = em.getCriteriaBuilder();
                CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
                Root<ConfigMap> rt = cq.from(ConfigMap.class);
                Expression<String> expresskey = rt.get("configkey");
                cq.where(cb.equal(expresskey, configkey));

                Query q = em.createQuery(cq);
                List<ConfigMap> cmList = (List<ConfigMap>) q.getResultList();
                cm = cmList.get(0);
                value = cm.getConfigvalue();
                int numberOfDups = cmList.size();
                logger.log(Level.SEVERE, "Found {0} duplicate keys for key {1} in the ConfigMap database table. Using the first one found :(", new Object[]{numberOfDups, configkey});

            } catch (Exception e) {
                logger.log(Level.INFO, "Key={0}, Value={1}", new Object[]{ky, value});
                logger.log(Level.WARNING, "Failed to get this key " + configkey + " from the ConfigMap database table due to an exception:(", e);
            }
        }
        return cm;
    }

    public List<ConfigMap> findAllByConfigKey(String configKey, boolean sortAsc) {
        List retList = null;
        try {

            CriteriaBuilder cb = em.getCriteriaBuilder();
            CriteriaQuery<ConfigMap> cq = cb.createQuery(ConfigMap.class);
            Root<ConfigMap> rt = cq.from(ConfigMap.class);
            Expression<String> expresskey = rt.get("configkey");
            cq.where(cb.like(expresskey, configKey));
            Query q = em.createQuery(cq);
            //Query q = em.createNativeQuery("SELECT * FROM configMap where key = '" + configkey + "'", ConfigMap.class);
            retList = q.getResultList();
            /*
             * String sort = "DESC"; if (sortAsc) { sort = "ASC"; } Query q =
             * em.createNativeQuery("SELECT * FROM support.configMap WHERE
             * configkey LIKE '" + configKey + "%' order By configKey " + sort +
             * " ", ConfigMap.class); retList = q.getResultList();
             */
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "A Persistence Error Occured while trying to find all config by wildcard key");
        }
        return retList;
    }
}
