/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

/**
 *
 * @author david
 */
import java.io.InputStream;
import java.security.Security;
import java.util.Date;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.URLDataSource;
import javax.mail.BodyPart;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

public class GoogleTest {

    private static final String SMTP_HOST_NAME = "smtp.gmail.com";
    private static final String SMTP_PORT = "465";
    private static final String emailMsgTxt = "<table width=\"600\" border=\"0\" cellspacing=\"0\" cellpadding=\"0\">  <tr>    <td><img src=\"cid:logoimg_cid\"/></td>  </tr>  <tr>    <td height=\"220\"> <p>Thanks for Joining Site.com</p>      <p>Lorem ipsum dolor sit amet, consectetur adipisicing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum. </p>    <p>Username:<br />      Password: </p>    <p>To confirm your email click <a href=\"#\">here</a>.</p></td>  </tr>  <tr>    <td height=\"50\" align=\"center\" valign=\"middle\" bgcolor=\"#CCCCCC\">www.site.com | contact@site.com | +38200 123 456</td>  </tr></table>";
    private static final String emailSubjectTxt = "A test from gmail";
    private static final String emailFromAddress = "noreply@purefitnessmanly.com.au";
    private static final String SSL_FACTORY = "javax.net.ssl.SSLSocketFactory";
    private static final String[] sendTo = {"david@manlyit.com.au"};

    public static void main(String args[]) throws Exception {

        Security.addProvider(new com.sun.net.ssl.internal.ssl.Provider());

        new GoogleTest().sendSSLMessage(sendTo, emailSubjectTxt,
                emailMsgTxt, emailFromAddress);
        System.out.println("Sucessfully Sent mail to All Users");
    }

    public void sendSSLMessage(String recipients[], String subject,
            String message, String from) throws MessagingException {
        boolean debug = true;

        Properties props = new Properties();
        props.put("mail.smtp.host", SMTP_HOST_NAME);
        props.put("mail.smtp.auth", "true");
        props.put("mail.debug", "true");
        props.put("mail.smtp.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.port", SMTP_PORT);
        props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
        props.put("mail.smtp.socketFactory.fallback", "false");

        Session session = Session.getDefaultInstance(props,
                new javax.mail.Authenticator() {

                    protected PasswordAuthentication getPasswordAuthentication() {
                        return new PasswordAuthentication("noreply@purefitnessmanly.com.au", "W*X6xh})F6)sKDwy");
                    }
                });

        session.setDebug(debug);

        Message msg = new MimeMessage(session);
        InternetAddress addressFrom = new InternetAddress(from);
        msg.setFrom(addressFrom);

        InternetAddress[] addressTo = new InternetAddress[recipients.length];
        for (int i = 0; i < recipients.length; i++) {
            addressTo[i] = new InternetAddress(recipients[i]);
        }
        msg.setRecipients(Message.RecipientType.TO, addressTo);

// Setting the Subject and Content Type
        msg.setSubject(subject);
        // create and fill the first message part
        msg.setContent(message, "text/html");

        Date timeStamp = new Date();
        msg.setSentDate(timeStamp);

        // Prepare a multipart HTML
        Multipart multipart = new MimeMultipart();
        // Prepare the HTML
        BodyPart htmlPart = new MimeBodyPart();
        htmlPart.setContent(message, "text/html");
        htmlPart.setDisposition(BodyPart.INLINE);

        // PREPARE THE IMAGE
        BodyPart imgPart = new MimeBodyPart();

        String fileName = "resources/images/headerimg.jpg";
        // String fileName = "http://www.purefitnessmanly.com.au/FitnessStats/resources/images/pure_fitness_manly_group_and_personal_training.jpg";

        InputStream stream = null;
               // getServletContext().getResourceAsStream(fileName); //or null if you can't obtain a ServletContext

        if (stream == null) {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            if (classLoader == null) {
                classLoader = this.getClass().getClassLoader();
            }

            stream = classLoader.getResourceAsStream(fileName);
        }

       

        try {
             DataSource ds = new ByteArrayDataSource(stream, "image/*");


            imgPart.setDataHandler(new DataHandler(ds));
            imgPart.setHeader("Content-ID", "<logoimg_cid>");
            imgPart.setDisposition(MimeBodyPart.INLINE);
            imgPart.setFileName(fileName);
            multipart.addBodyPart(imgPart);
        } catch (Exception exception) {
            System.out.println("Image ERROR:" + exception.getMessage());
        }
        // Set the message content!

        multipart.addBodyPart(htmlPart);




        msg.setContent(multipart);

        Transport.send(msg);
    }
}
