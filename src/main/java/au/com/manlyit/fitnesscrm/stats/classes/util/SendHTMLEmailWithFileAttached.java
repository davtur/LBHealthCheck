/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import javax.mail.util.ByteArrayDataSource;

/**
 *
 * @author dturner
 */
public class SendHTMLEmailWithFileAttached implements Serializable {

    private static final Logger LOGGER = Logger.getLogger(SendHTMLEmailWithFileAttached.class.getName());
    private static final long serialVersionUID = 1L;

    public SendHTMLEmailWithFileAttached() {
    }
    //@Resource(name = "mail/pureFitnessMail")
    //private Session pureFitnessEmail;

    public synchronized boolean send(String to, String ccAddress,String bccAddress, String from, String emailSubject, String message, String theAttachedfileName, Properties serverProperties, boolean debug) {
        boolean result = true;
        //boolean debug = Boolean.valueOf(args[4]).booleanValue();
        String msgText1 = message + "\n";
        String subject = emailSubject;
        boolean auth = false;
        // create some properties and get the default Session
        String user = "noreply";                 // user ID
        String password = "";              // password
        // password

        boolean ssl = true;
        /*Properties props = System.getProperties();
        
         if (host != null) {
         props.put("mail.smtp.host", host);
         }
         if (auth) {
         props.put("mail.smtp.auth", "true");
         }*/

        //Session session = Session.getInstance(props, null);
       /* Properties props = new Properties();
         props.put("mail.smtp.host", SMTP_HOST_NAME);
         props.put("mail.smtp.auth", "true");
         props.put("mail.debug", "true");
         props.put("mail.smtp.port", SMTP_PORT);
         props.put("mail.smtp.socketFactory.port", SMTP_PORT);
         props.put("mail.smtp.socketFactory.class", SSL_FACTORY);
         props.put("mail.smtp.socketFactory.fallback", "false");*/
        Session session = null;
        final String sslUser = serverProperties.getProperty("mail.smtp.ssluser");
        final String sslPass = serverProperties.getProperty("mail.smtp.sslpass");
        try {
            session = Session.getInstance(serverProperties,
                    new javax.mail.Authenticator() {

                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(sslUser, sslPass);
                        }
                    });
        } catch (Exception e) {
            result = false;
            Logger.getLogger(SendHTMLEmailWithFileAttached.class.getName()).log(Level.SEVERE, "Couldn't get a session \r\n", e);
            JsfUtil.addErrorMessage(e, "Couldn't get a session \r\n");
        }

        if (session == null) {
            return false;
        }

        session.setDebug(debug);
        MimeMessage msg = new MimeMessage(session);

        // You only need to add the email host default user and default return address
        //Session session = pureFitnessEmail;
        //MimeMessage msg = new MimeMessage(session);
        MimeBodyPart mimeBodyPart2 = new MimeBodyPart();

        try {
            // create a message
            msg.setFrom(new InternetAddress(from));

            //InternetAddress[] address = {new InternetAddress(to)};
            //InternetAddress[] address2 = {new InternetAddress(ccAddress)};
            //msg.setRecipients(Message.RecipientType.TO, address);
            //msg.setRecipients(Message.RecipientType.CC, address2);
            msg.setRecipients(Message.RecipientType.TO, iterateEmailAddressesIntoArray(to));
            if (ccAddress != null) {
                ccAddress = ccAddress.trim();

                if (ccAddress.trim().length() > 0) {
                    msg.setRecipients(Message.RecipientType.CC, iterateEmailAddressesIntoArray(ccAddress));
                }
            }
            if (bccAddress != null) {
                bccAddress = bccAddress.trim();

                if (bccAddress.trim().length() > 0) {
                    msg.setRecipients(Message.RecipientType.BCC, iterateEmailAddressesIntoArray(bccAddress));
                }
            }

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
            final String fileName = serverProperties.getProperty("mail.smtp.headerimage.url");
            final String fileNameCid = serverProperties.getProperty("mail.smtp.headerimage.cid");
            // String fileName = "/resources/images/headerimg.jpg";
            //String fileName = "http://www.manlybeachfemalefitness.com.au/FitnessStats/resources/images/pure_fitness_manly_group_and_personal_training.jpg";

            InputStream stream ;
            // getServletContext().getResourceAsStream(fileName); //or null if you can't obtain a ServletContext

            if (fileName != null && fileNameCid != null) {
                ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
                if (classLoader == null) {
                    classLoader = this.getClass().getClassLoader();
                }

                stream = classLoader.getResourceAsStream(fileName);

                if (stream == null) {
                    //try and get the URL if the loacla file name doesn't exist
                    try {
                        stream = new URL(fileName).openStream();
                    } catch (IOException iOException) {
                    }
                }
                if (stream != null) {
                    try {
                        DataSource ds = new ByteArrayDataSource(stream, "image/*");

                        imgPart.setDataHandler(new DataHandler(ds));
                        imgPart.setHeader("Content-ID", "<" + fileNameCid + ">");
                        imgPart.setDisposition(MimeBodyPart.INLINE);
                        imgPart.setFileName(fileName);
                        multipart.addBodyPart(imgPart);
                    } catch (IOException | MessagingException exception) {
                        System.out.println("Image ERROR:" + exception.getMessage());
                    }
                } else {
                    LOGGER.log(Level.WARNING, "Email Header filename and/or header cid name not found: {0}", new Object[]{fileName});
                }
            // Set the message content!
            }
            multipart.addBodyPart(htmlPart);

            msg.setContent(multipart);

            if (theAttachedfileName != null) {

                mimeBodyPart2.attachFile(theAttachedfileName);
                //mimeBodyPart2.addHeaderLine(theAttachedfileName);
                // attach the file to the message
                multipart.addBodyPart(mimeBodyPart2);

            }

            msg.setContent(multipart);

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
            //mp.addBodyPart(mbp1);
            // add the Multipart to the message
            //msg.setContent(mp);
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

            Transport.send(msg);

            /* SMTPTransport t = (SMTPTransport) session.getTransport(ssl ? "smtps" : "smtp");
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
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Send Email Messaging Exception: \r\n" + msg.toString(), mex);
            JsfUtil.addErrorMessage(mex, mex.getMessage());
            Exception ex;
            result = false;
            if ((ex = mex.getNextException()) != null) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Send Email 2nd Messaging Exception: \r\n" + msg.toString(), ex);
                JsfUtil.addErrorMessage(ex, ex.getMessage());
            }
        } catch (IOException ioex) {
            result = false;
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Send Email IO Exception: \r\n" + msg.toString(), ioex);
            JsfUtil.addErrorMessage(ioex, ioex.getMessage());
        }
        return result;
    }

    private InternetAddress[] iterateEmailAddressesIntoArray(String csvEmailAddresses) {

        if (!csvEmailAddresses.contains(",")) {
            InternetAddress[] inetAddresses = new InternetAddress[1];
            try {
                inetAddresses[0] = new InternetAddress(csvEmailAddresses);
            } catch (AddressException ex) {
                String message = "Single Address -not a valid email address: " + csvEmailAddresses;
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, message, ex);
            }
            return inetAddresses;

        }

        String[] addresses = null;
        try {

            addresses = csvEmailAddresses.split(",");
        } catch (Exception e) {
            String message = "Could not split the email addresses: " + csvEmailAddresses;
            Logger.getLogger(getClass().getName()).log(Level.SEVERE, message, e);

        }
        if (addresses == null) {
            return null;
        }
        InternetAddress[] InetAddresses = new InternetAddress[addresses.length];
        for (int i = 0; i < addresses.length; i++) {
            try {
                InetAddresses[i] = new InternetAddress(addresses[i]);
            } catch (AddressException ex) {
                Logger.getLogger(getClass().getName()).log(Level.SEVERE, "Invalid email address: " + addresses[i], ex);
            }
        }
        return InetAddresses;
    }
}
