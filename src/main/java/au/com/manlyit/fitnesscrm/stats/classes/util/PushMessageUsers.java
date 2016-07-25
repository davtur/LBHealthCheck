/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.faces.bean.ApplicationScoped;
import javax.faces.bean.ManagedBean;
 
@ManagedBean
@ApplicationScoped
public class PushMessageUsers implements Serializable {

    private static final long serialVersionUID = 1L;
     
    private List<String> users;
     
    @PostConstruct
    public void init() {
        users = new ArrayList<>();
    }
 
    public List<String> getUsers() {
        if(users == null){
             users = new ArrayList<>();
        }
        return users;
    }
     
    public void remove(String user) {
        this.users.remove(user);
    }
     
    public void add(String user) {
        this.users.add(user);
    }
         
    public boolean contains(String user) {
        return this.getUsers().contains(user);
    }
}