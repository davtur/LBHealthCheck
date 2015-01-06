/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import java.io.IOException;
import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Properties;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.context.FacesContext;
import javax.inject.Inject;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.primefaces.context.RequestContext;

@WebServlet("/callback.html")
public class WebDDRSignUpServlet extends HttpServlet {

    private static final String paymentGateway = "EZIDEBIT";
    private static final long serialVersionUID = 8071426090770097330L;
    private static final Logger logger = Logger.getLogger(WebDDRSignUpServlet.class.getName());
    //private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private FutureMapEJB futureMap;
    @Inject
    private PaymentBean paymentBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;

    public WebDDRSignUpServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        try {
            logger.log(Level.INFO, "*** Call Back from Web EDDR Form ***");
            HttpSession httpSession = request.getSession();
            String uref = request.getParameter("uref");
            String cref = request.getParameter("cref");
            String fname = request.getParameter("fname");
            String lname = request.getParameter("lname");
            String email = request.getParameter("email");
            String mobile = request.getParameter("mobile");
            String addr = request.getParameter("addr");
            String suburb = request.getParameter("suburb");
            String state = request.getParameter("state");
            String pcode = request.getParameter("pcode");
            String rdate = request.getParameter("rdate");
            String ramount = request.getParameter("ramount");
            String freq = request.getParameter("freq");
            String odate = request.getParameter("odate");
            String oamount = request.getParameter("oamount");
            String numpayments = request.getParameter("numpayments");
            String totalamount = request.getParameter("totalamount");
            String method = request.getParameter("method");
            //boolean mobileDevice = false;
            FacesContext context = FacesContext.getCurrentInstance();
            String sessionID = httpSession.getId();
            logger.log(Level.INFO, "Session:{0}, Params:{1},{2},{3},{4},{5},{6},{7},{8},{9},{10},{11},{12},{13},{14},{15},{16},{17},{18}", new Object[]{sessionID, uref, cref, fname, lname, email, mobile, addr, suburb, state, pcode, rdate, ramount, freq, odate, oamount, numpayments, totalamount, method});

            if (uref != null) {
                uref = uref.trim();
                if (uref.length() > 0) {
                    int customerId = 0;
                    Customers current = null;
                    try {
                        customerId = Integer.parseInt(uref);
                        current = ejbFacade.findById(customerId);
                    } catch (NumberFormatException numberFormatException) {
                    }

                    if (current != null) {

                        String templatePlaceholder = "<!--LINK-URL-->";
                        String htmlText = configMapFacade.getConfig("system.admin.ezidebit.webddrcallback.template");
                        String name = current.getFirstname() + " " + current.getLastname();
                        htmlText = htmlText.replace(templatePlaceholder, name);
                        response.sendRedirect(request.getContextPath() + getValueFromKey("payment.ezidebit.callback.redirect"));
                        Future<Boolean> emailSendResult = paymentBean.sendAsynchEmail(configMapFacade.getConfig("AdminEmailAddress"), configMapFacade.getConfig("PasswordResetCCEmailAddress"), configMapFacade.getConfig("PasswordResetFromEmailAddress"), configMapFacade.getConfig("system.ezidebit.webEddrCallback.EmailSubject"), htmlText, null, emailServerProperties(), false);
                        if (emailSendResult.get() == false) {
                            logger.log(Level.WARNING, "Email for Call Back from Web EDDR Form FAILED. Future result false from async job");
                        }

                        PaymentParameters pp = current.getPaymentParameters();
                        if (pp != null) {
                            if (pp.getWebddrUrl() != null) {
                                // the url is not null so this is the first time the customer has clicked the link -this should only happen once so it cant be abused.
                                pp.setWebddrUrl(null);
                                ejbPaymentParametersFacade.edit(pp);
                                ejbFacade.editAndFlush(current);
                                logger.log(Level.INFO, " Customer {0} has set up payment info. Setting Web DDR URL to NULL as it should only be used once.", new Object[]{current.getUsername()});
                                //startAsynchJob("ConvertSchedule", paymentBean.clearSchedule(current, false, current.getUsername(), getDigitalKey()), futureMap.getFutureMapInternalSessionId());

                                GregorianCalendar cal = new GregorianCalendar();
                                cal.add(Calendar.MONTH, 18);
                                Date endDate = cal.getTime();
                                cal.add(Calendar.MONTH, -24);

                                //startAsynchJob("GetCustomerDetails", paymentBean.getCustomerDetails(current, getDigitalKey()), futureMap.getFutureMapInternalSessionId());
                                //startAsynchJob("GetPayments", paymentBean.getPayments(current, "ALL", "ALL", "ALL", "", cal.getTime(), endDate, false, getDigitalKey()), futureMap.getFutureMapInternalSessionId());
                                startAsynchJob("ConvertSchedule", paymentBean.getScheduledPayments(current, cal.getTime(), endDate, getDigitalKey()), futureMap.getFutureMapInternalSessionId());

                            }
                        }
                    }
                }
            }
        } catch (InterruptedException | ExecutionException ex) {
            Logger.getLogger(WebDDRSignUpServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private Properties emailServerProperties() {
        Properties props = new Properties();

        props.put("mail.smtp.host", configMapFacade.getConfig("mail.smtp.host"));
        props.put("mail.smtp.auth", configMapFacade.getConfig("mail.smtp.auth"));
        props.put("mail.debug", configMapFacade.getConfig("mail.debug"));
        props.put("mail.smtp.port", configMapFacade.getConfig("mail.smtp.port"));
        props.put("mail.smtp.socketFactory.port", configMapFacade.getConfig("mail.smtp.socketFactory.port"));
        props.put("mail.smtp.socketFactory.class", configMapFacade.getConfig("mail.smtp.socketFactory.class"));
        props.put("mail.smtp.socketFactory.fallback", configMapFacade.getConfig("mail.smtp.socketFactory.fallback"));
        props.put("mail.smtp.ssluser", configMapFacade.getConfig("mail.smtp.ssluser"));
        props.put("mail.smtp.sslpass", configMapFacade.getConfig("mail.smtp.sslpass"));
        props.put("mail.smtp.headerimage.url", configMapFacade.getConfig("mail.smtp.headerimage.url"));
        props.put("mail.smtp.headerimage.cid", configMapFacade.getConfig("mail.smtp.headerimage.cid"));
        return props;

    }

    private String getDigitalKey() {
        return configMapFacade.getConfig("payment.ezidebit.widget.digitalkey");
    }

    private void startAsynchJob(String key, Future future, String sessionId) {

        AsyncJob aj = new AsyncJob(key, future);
        futureMap.put(sessionId, aj);
    }

    private boolean mobileDevice(HttpServletRequest req) {
        //FacesContext context = FacesContext.getCurrentInstance();
        //HttpServletRequest req = (HttpServletRequest) context.getExternalContext().getRequest();
        String userAgent = req.getHeader("user-agent");
        String accept = req.getHeader("Accept");

        if (userAgent != null && accept != null) {
            UAgentInfo agent = new UAgentInfo(userAgent, accept);
            if (agent.detectMobileQuick()) {
                return true;
            }
        }

        return false;
    }

    private String getValueFromKey(String key) {
        String val;
        val = configMapFacade.getConfig(key);
        return val;
    }

    private Customers getUserMailAddressFromJsonResponse(String accessToken, HttpSession httpSession) {
        String email = null;
        Customers cust = new Customers();
        //deprecated use HttpClientBuilder
        CloseableHttpClient httpclient = HttpClientBuilder.create().build();
        try {
            if (accessToken != null && !"".equals(accessToken)) {
                String newUrl = "https://graph.facebook.com/me?access_token=" + accessToken;
                httpclient = HttpClientBuilder.create().build();
                HttpGet httpget = new HttpGet(newUrl);
                logger.log(Level.WARNING, "Get info from face --> executing request: {0}", httpget.getURI());
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                String firstName = null;
                String lastName = null;
                String facebookId = null;
                try {
                    JSONObject json = (JSONObject) JSONSerializer.toJSON(responseBody);
                    facebookId = json.getString("id");

                    firstName = json.getString("first_name");
                    lastName = json.getString("last_name");
                    httpSession.setAttribute("FACEBOOK_USER", firstName + " "
                            + lastName + ", facebookId:" + facebookId);

                    email = json.getString("email");
                    //put user data in session

                    if (facebookId == null || firstName == null || lastName == null || email == null) {
                        if (facebookId == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: Facebook ID is NULL");
                        }
                        if (firstName == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: firstName is NULL");
                        }
                        if (lastName == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: lastName is NULL");
                        }
                        if (email == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: email is NULL");
                        }
                    }

                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error getting JSON objects from facebook}", e);
                }
                cust.setFirstname(firstName);
                cust.setLastname(lastName);
                cust.setEmailAddress(email);
                cust.setFacebookId(facebookId);

            } else {
                logger.log(Level.WARNING, "The facebook token is null");
            }
        } catch (ClientProtocolException e) {
            logger.log(Level.WARNING, e.getMessage());
        } catch (IOException e) {
            logger.log(Level.WARNING, e.getMessage());
        } finally {
            try {
                httpclient.close();
            } catch (IOException ex) {
                logger.log(Level.WARNING, "Error closing httpclient in facebook security servlet}", ex);
            }
        }
        return cust;
    }
}
