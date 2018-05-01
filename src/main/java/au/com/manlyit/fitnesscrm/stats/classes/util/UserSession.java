package au.com.manlyit.fitnesscrm.stats.classes.util;


import java.io.Serializable;

public class UserSession implements Serializable{

    private static final long serialVersionUID = 1L;
    
    private final String username;
    private final String sessionId;
    
    public UserSession(String username, String sessionId){
        this.username = username;
        this.sessionId = sessionId;
        
    }

    /**
     * @return the username
     */
    public String getUsername() {
        return username;
    }

    /**
     * @return the sessionId
     */
    public String getSessionId() {
        return sessionId;
    }
    
    
}
