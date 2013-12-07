/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes;

//import com.sun.org.apache.xml.internal.security.utils.Base64;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import org.primefaces.util.Base64;

/**
 *
 * @author david
 */
public final class PasswordService {

    private static PasswordService instance;

    private PasswordService() {
    }

    public synchronized String encrypt(String plaintext) {
        //user.setPassword(org.myorg.services.PasswordService.getInstance().encrypt(request.getParameter("password"));
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256"); //step 2
        } catch (NoSuchAlgorithmException e) {
            //throw new SystemUnavailableException(e.getMessage());
        }
        try {
            md.update(plaintext.getBytes("UTF-8")); //step 3
        } catch (UnsupportedEncodingException e) {
            // throw new SystemUnavailableException(e.getMessage());
        }
        byte raw[] = md.digest(); //step 4
        //String hash = Base64.encode(raw); //step 5
        //tried to find another implementation of base64 that wasn't sun proprietry. (was using com.sun.org.apache.xml.internal.security.utils.Base64; )
        String hash = Base64.encodeToString(raw, false); //step 5
        return hash; //step 6
    }

    public static synchronized PasswordService getInstance() //step 1
    {
        if (instance == null) {
            return new PasswordService();
        } else {
            return instance;
        }
    }
}
