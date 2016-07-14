/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import org.eclipse.persistence.annotations.Cache;
import org.eclipse.persistence.annotations.CacheCoordinationType;
import org.eclipse.persistence.annotations.CacheType;

/**
 *
 * @author david
 * 
 */
@Cache(
  type=CacheType.SOFT, // Cache everything until the JVM decides memory is low.
  size=64000,  // Use 64,000 as the initial cache size.
  expiry=36000000,  // 10 minutes
  coordinationType=CacheCoordinationType.INVALIDATE_CHANGED_OBJECTS  
)
public interface BaseEntity {
    public Integer getId();
    public void setId(Integer id);

}
