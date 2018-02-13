
package au.com.manlyit.fitnesscrm.stats.webservices;
 
import javax.xml.bind.annotation.XmlRootElement; 
 
@XmlRootElement 
public class NewLead { 
    private String id; 
    private String name; 
    private String emailAddress; 
    private String phoneNumber; 
    private String message; 
    private String authToken;
 
    public NewLead() { 
    } 
 
    public NewLead(String id) { 
        this.id = id; 
    } 
 
    public void setId(String id) { 
        this.id = id; 
    } 
 
    public void setName(String name) { 
        this.name = name; 
    } 
 
    public void setEmailAddress(String emailAddress) { 
        this.emailAddress = emailAddress; 
    } 
 
    public void setPhoneNumber(String phoneNumber) { 
        this.phoneNumber = phoneNumber; 
    } 
 
    public String getId() { 
        return id; 
    } 
 
    public String getName() { 
        return name; 
    } 
 
    public String getPhoneNumber() { 
        return phoneNumber; 
    } 
 
    public String getEmailAddress() { 
        return emailAddress; 
    } 

    /**
     * @return the message
     */
    public String getMessage() {
        return message;
    }

    /**
     * @param message the message to set
     */
    public void setMessage(String message) {
        this.message = message;
    }

    /**
     * @return the authToken
     */
    public String getAuthToken() {
        return authToken;
    }

    /**
     * @param authToken the authToken to set
     */
    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }
}