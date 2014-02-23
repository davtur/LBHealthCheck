/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.security.spec.AlgorithmParameterSpec;
import java.security.spec.KeySpec;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.PBEParameterSpec;
//import sun.misc.BASE64Encoder;
import javax.xml.bind.DatatypeConverter;



/**
 *
 * @author dturner
 */
    public class StringEncrypter  implements Serializable {
        private static final Logger logger = Logger.getLogger(StringEncrypter.class.getName());
        Cipher ecipher;
        Cipher dcipher;
    
        // 8-byte Salt
        byte[] salt = {
            (byte)0xA9, (byte)0x9B, (byte)0xC8, (byte)0x32,
            (byte)0x56, (byte)0x35, (byte)0xE3, (byte)0x03
        };
    
        // Iteration count
        int iterationCount = 19;
    
    public    StringEncrypter(String passPhrase) {
            try {
                // Create the key
                KeySpec keySpec = new PBEKeySpec(passPhrase.toCharArray(), salt, iterationCount);
                SecretKey key = SecretKeyFactory.getInstance(
                    "PBEWithMD5AndDES").generateSecret(keySpec);
                ecipher = Cipher.getInstance(key.getAlgorithm());
                dcipher = Cipher.getInstance(key.getAlgorithm());
    
                // Prepare the parameter to the ciphers
                AlgorithmParameterSpec paramSpec = new PBEParameterSpec(salt, iterationCount);
    
                // Create the ciphers
                ecipher.init(Cipher.ENCRYPT_MODE, key, paramSpec);
                dcipher.init(Cipher.DECRYPT_MODE, key, paramSpec);
            } catch (    java.security.InvalidAlgorithmParameterException | java.security.spec.InvalidKeySpecException | javax.crypto.NoSuchPaddingException | java.security.NoSuchAlgorithmException | java.security.InvalidKeyException e) {
            }
        }
    
        public String encrypt(String str) {
            try {
                // Encode the string into bytes using utf-8
                byte[] utf8 = str.getBytes("UTF-8");
    
                // Encrypt
                byte[] enc = ecipher.doFinal(utf8);
    
                // Encode bytes to base64 to get a string
                //return new BASE64Encoder().encode(enc);
                String encoded = DatatypeConverter.printBase64Binary(enc);
                return encoded;
            } catch (    javax.crypto.BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {
                logger.log(Level.WARNING, "Encryption Exception",e);
            } 
            return null;
        }
    
        public String decrypt(String str) {
            try {
                // Decode base64 to get bytes
                //byte[] dec = new sun.misc.BASE64Decoder().decodeBuffer(str);
                byte[] dec = DatatypeConverter.parseBase64Binary(str);
                // Decrypt
                byte[] utf8 = dcipher.doFinal(dec);
    
                // Decode using utf-8
                String decoded = new String(utf8, "UTF-8");
                return decoded;
            } catch (    javax.crypto.BadPaddingException | IllegalBlockSizeException | UnsupportedEncodingException e) {  
                  logger.log(Level.WARNING, "Encryption Exception",e);
            }
            return null;
        }
    }

/*    // Here is an example that uses the class
    try {
        // Create encrypter/decrypter class
        DesEncrypter encrypter = new DesEncrypter("My Pass Phrase!");
    
        // Encrypt
        String encrypted = encrypter.encrypt("Don't tell anybody!");
    
        // Decrypt
        String decrypted = encrypter.decrypt(encrypted);
    } catch (Exception e) {
    }
*/