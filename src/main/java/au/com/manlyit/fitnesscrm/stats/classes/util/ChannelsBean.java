/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;

/**
 *
 * @author david
 */
@ManagedBean
@ApplicationScoped
public class ChannelsBean implements Serializable {
    
    private Map<String, String> channels;
    
    @PostConstruct
    public void init(){
    channels = new HashMap<>();
    }
    public void addChannel(String user, String channel) {
        channels.put(user, channel);
    }

    public String getChannel(String user) {
        return channels.get(user);
    }
    
    public void destroyChannel(String user){
        channels.remove(user);
    }

}