/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.util.UUID;
import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedProperty;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

@ManagedBean
@SessionScoped
public class PushMessageBean implements Serializable {
    
    private String channel;
    
    @ManagedProperty(value = "#{channelsBean}")
    private ChannelsBean channels;
    
    private String sendMessageUser;
    
    private String user;
    
    private String summary;
    
    private String detail;
    
    @PostConstruct
    public void doPostConstruction() {
        channel = "/" + UUID.randomUUID().toString();
        channels.addChannel(user, channel);
    }

    @PreDestroy
    public void doPreDestruction() {
        channels.destroyChannel(user);
    }

    public void send() {
        
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
        eventBus.publish(channels.getChannel(sendMessageUser), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
    }
    
    public String getSummary() {
        return summary;
    }
    
    public void setSummary(String summary) {
        this.summary = summary;
    }
    
    public String getDetail() {
        return detail;
    }
    
    public void setDetail(String detail) {
        this.detail = detail;
    }
}
