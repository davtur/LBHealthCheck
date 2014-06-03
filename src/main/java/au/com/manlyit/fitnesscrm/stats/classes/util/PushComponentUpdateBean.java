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
import javax.faces.context.FacesContext;
import org.apache.commons.lang.StringEscapeUtils;
import org.primefaces.push.EventBus;
import org.primefaces.push.EventBusFactory;

@ManagedBean
@SessionScoped
public class PushComponentUpdateBean implements Serializable {
    
    private String chan;
    private final static String CHANNEL = "/{room}/";
    //@ManagedProperty(value = "#{channelsBean}")
    //private ChannelsBean channels;
    private String sessionChannel;
    
    private String user;
    
    private String summary;
    
    private String detail;
    
    @PostConstruct
    public void doPostConstruction() {
       // sessionChannel = "/" + UUID.randomUUID().toString();
        //channels.addChannel(getUser(), chan);
        sessionChannel = "/test";
    }

   // @PreDestroy
   // public void doPreDestruction() {
   //     channels.destroyChannel(getUser());
   // }
    public void sendMessage(String summ, String det){
        if(summ == null ){
            summ = "";
        }
        if(det == null){
            det = "";
        }
        this.setSummary(summ);
        this.setDetail(det);
        this.send();
    }
    public void send() {
        
        EventBus eventBus = EventBusFactory.getDefault().eventBus();
       // eventBus.publish(channels.getChannel(getUser()), new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
        eventBus.publish(sessionChannel, new FacesMessage(StringEscapeUtils.escapeHtml(summary), StringEscapeUtils.escapeHtml(detail)));
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

    
    /**
     * @return the user
     */
    public String getUser() {
        if(user == null){
            user = FacesContext.getCurrentInstance().getExternalContext().getRemoteUser();
        }
        return user;
    }

    
}
