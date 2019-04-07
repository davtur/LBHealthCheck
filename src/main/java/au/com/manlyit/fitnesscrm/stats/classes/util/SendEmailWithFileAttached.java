/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.db.EmailAttachments;
import com.sun.mail.smtp.SMTPTransport;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Resource;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

/**
 *
 * @author dturner
 */
public class SendEmailWithFileAttached {

    public SendEmailWithFileAttached() {
    }
    @Resource(name = "optusEmail")
    private Session optusEmail;

    public boolean send(String host, String to, String ccAddress, String from, String emailSubject, String message, String theAttachedfileName, boolean debug) {
        boolean result = true;
        //boolean debug = Boolean.valueOf(args[4]).booleanValue();
        String msgText1 = message + "\n";
        String subject = emailSubject;
        boolean auth = false;
        // create some properties and get the default Session
        String user = "david.turner@optusnet.com.au";                 // user ID
        String password = "";              // password
        // password

        boolean ssl = false;
        Properties props = System.getProperties();

        if (host != null) {
            props.put("mail.smtp.host", host);
        }
        if (auth) {
            props.put("mail.smtp.auth", "true");
        }
        //Main.getConfig("mail.smtp.host")
        Session session = Session.getInstance(props, null);
        session.setDebug(debug);
        MimeMessage msg = new MimeMessage(session);

        // You only need to add the email host default user and default return address
        //MimeMessage msg = new MimeMessage(optusEmail);




        Multipart mp = new MimeMultipart();
        MimeBodyPart mbp1 = new MimeBodyPart();
        MimeBodyPart mimeBodyPart2 = new MimeBodyPart();


        try {
            // create a message
            msg.setFrom(new InternetAddress(from));

            //InternetAddress[] address = {new InternetAddress(to)};
            //InternetAddress[] address2 = {new InternetAddress(ccAddress)};
            //msg.setRecipients(Message.RecipientType.TO, address);
            //msg.setRecipients(Message.RecipientType.CC, address2);


            msg.setRecipients(Message.RecipientType.TO, iterateEmailAddressesIntoArray(to));
            msg.setRecipients(Message.RecipientType.CC, iterateEmailAddressesIntoArray(ccAddress));


            msg.setSubject(subject);

            // create and fill the first message part
            mbp1.setText(msgText1);
            /*
             * Use the following approach instead of the above line if
             * you want to control the MIME type of the attached file.
             * Normally you should never need to do this.
             *
            FileDataSource fds = new FileDataSource(filename) {
            public String getContentType() {
            //return    "text/html;charset=UTF-8";
            return "application/octet-stream";

            }
            };
            mbp2.setDataHandler(new DataHandler(fds));
            mbp2.setFileName(fds.getName());
             */

            // create the Multipart and add its parts to it

            mp.addBodyPart(mbp1);
            if (theAttachedfileName != null) {

                mimeBodyPart2.attachFile(theAttachedfileName);
                //mimeBodyPart2.addHeaderLine(theAttachedfileName);
                // attach the file to the message
                mp.addBodyPart(mimeBodyPart2);

            }
            // add the Multipart to the message
            msg.setContent(mp);

            // set the Date: header
            msg.setSentDate(new Date());

            /*
             * If you want to control the Content-Transfer-Encoding
             * of the attached file, do the following.  Normally you
             * should never need to do this.
             *
            msg.saveChanges();
            mbp2.setHeader("Content-Transfer-Encoding", "base64");
             */

            // send the message
            msg.setHeader("X-Priority", "1");  //1 means high, 5 means low


            msg.setHeader("Priority", "Urgent");

            msg.setHeader("Importance", "high");
            msg.setHeader("X-MSMail-Priority", "High");// high or low

            // Transport.send(msg);

            SMTPTransport t = (SMTPTransport) session.getTransport(ssl ? "smtps" : "smtp");
            try {
                if (auth) {
                    t.connect(host, user, password);
                } else {
                    t.connect();
                }

                t.sendMessage(msg, msg.getAllRecipients());
            } finally {
                t.close();
            }

            /* SMTPTransport t = (SMTPTransport) optusEmail.getTransport(ssl ? "smtps" : "smtp");
            try {
            if (auth) {
            t.connect(host, user, password);
            } else {
            t.connect();
            }

            t.sendMessage(msg, msg.getAllRecipients());
            } finally {
            t.close();
            }*/

        } catch (MessagingException mex) {
            Logger.getLogger(SendEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Send Email Messaging Exception: \r\n" + msg.toString(), mex);
            JsfUtil.addErrorMessage(mex, mex.getMessage());
            Exception ex = null;
            result = false;
            if ((ex = mex.getNextException()) != null) {
                Logger.getLogger(SendEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Send Email 2nd Messaging Exception: \r\n" + msg.toString(), ex);
                JsfUtil.addErrorMessage(ex, ex.getMessage());
            }
        } catch (Exception ioex) {
            result = false;
            Logger.getLogger(SendEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Send Email IO Exception: \r\n" + msg.toString(), ioex);
            JsfUtil.addErrorMessage(ioex, ioex.getMessage());
        }
        return result;
    }

    private InternetAddress[] iterateEmailAddressesIntoArray(String csvEmailAddresses) {

        String[] addresses = null;
        try {
            addresses = csvEmailAddresses.split(",");
        } catch (Exception e) {
            Logger.getLogger(SendEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Could not split the email addresses: ", e);

        }

        InternetAddress[] InetAddresses = new InternetAddress[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            try {
                InetAddresses[i] = new InternetAddress(addresses[i]);
            } catch (AddressException ex) {
                Logger.getLogger(SendEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Invalid email address: " + addresses[i], ex);
            }
        }
        return InetAddresses;
    }
}
