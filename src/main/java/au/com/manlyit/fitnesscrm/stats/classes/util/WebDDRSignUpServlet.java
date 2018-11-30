/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.LoginBean;
import au.com.manlyit.fitnesscrm.stats.beans.PaymentBean;
import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import au.com.manlyit.fitnesscrm.stats.db.PaymentParameters;
import java.io.IOException;
import java.util.Calendar;
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
    private LoginBean loginBean;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.PaymentParametersFacade ejbPaymentParametersFacade;

    @Inject
    private CustomersController controller;
    @Inject
    private EziDebitPaymentGateway ezidebitcontroller;

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

            futureMap.processCallbackFromOnlinePaymentForm(request.getRemoteUser(),request.getSession().getId(),uref, cref, fname, lname, email, mobile, addr, suburb, state, pcode, rdate, ramount, freq, odate, oamount, numpayments, totalamount, method);
            if (loginBean.isMobileDeviceUserAgent() == false) {
                response.sendRedirect(request.getContextPath() + getValueFromKey("payment.ezidebit.callback.redirect"));
            } else {
                response.sendRedirect(request.getContextPath() + getValueFromKey("payment.ezidebit.callback.mobileRedirect"));
            }

        } catch (Exception ex) {
            Logger.getLogger(WebDDRSignUpServlet.class.getName()).log(Level.SEVERE, null, ex);
        }
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
