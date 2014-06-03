/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.classes.PasswordService;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.FacesMessage;
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
import org.apache.commons.lang.RandomStringUtils;

import org.apache.commons.lang.StringUtils;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

@WebServlet("*.sec")
public class SecurityServlet extends HttpServlet {

    private static final long serialVersionUID = 8071426090770097330L;
    private static final Logger logger = Logger.getLogger(SecurityServlet.class.getName());
    //private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;

    public SecurityServlet() {
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        logger.log(Level.INFO, "*** Called SecurityServlet");
        HttpSession httpSession = request.getSession();
        String faceCode = request.getParameter("code");
        String state = request.getParameter("state");
        //boolean mobileDevice = false;
        FacesContext context = FacesContext.getCurrentInstance();
        String accessToken = getFacebookAccessToken(faceCode);
        Customers facebookUser = getUserMailAddressFromJsonResponse(accessToken, httpSession);
        String sessionID = httpSession.getId();
        if (state.equals(sessionID)) {
            String pfmEncrptedPassword = null;
            try {
                //do some specific user data operation like saving to DB or login user
                //request.login(email, "somedefaultpassword");

                // log in user with existing facebookID
                String fbid = facebookUser.getFacebookId();
                if (fbid == null) {
                    logger.log(Level.WARNING, "FaceBook User ID is NULL");
                } else {
                    Customers customer = ejbFacade.findCustomerByFacebookId(facebookUser.getFacebookId());
                    if (customer == null) {
                        //customer has not logged in with facebook before
                        //see if we can match on email and name
                        if (facebookUser.getEmailAddress() != null) {
                            List<Customers> clist = ejbFacade.findCustomersByEmail(facebookUser.getEmailAddress());
                            for (Customers c : clist) {

                                if (c != null) {

                                    if (c.getFirstname().toLowerCase().compareTo(facebookUser.getFirstname().toLowerCase()) == 0 && c.getLastname().toLowerCase().compareTo(facebookUser.getLastname().toLowerCase()) == 0) { // matched facebook username to local user
                                        // we have not matched names to email address... Should probably do dob as well                                
                                        customer = c;
                                    }
                                }
                            }
                        } else {
                            logger.log(Level.WARNING, "Facebook Email address is null for customer. ");
                        }
                    }
                    if (customer != null) {
                        // login facebook user with random password that is changed each login
                        String passwd = RandomStringUtils.random(20);
                        String encPassword = PasswordService.getInstance().encrypt(passwd);
                        pfmEncrptedPassword = customer.getPassword();
                        customer.setPassword(encPassword);
                        customer.setFacebookId(fbid);
                        ejbFacade.editAndFlush(customer);

                        try {
                            request.login(customer.getUsername(), passwd);
                            customer.setPassword(pfmEncrptedPassword);
                            ejbFacade.editAndFlush(customer);
                        } catch (ServletException servletException) {
                            logger.log(Level.INFO, "Login failed!");
                            customer.setPassword(pfmEncrptedPassword);
                            ejbFacade.editAndFlush(customer);
                            if (servletException.getMessage().contains("Login failed") == false) {
                                throw servletException;
                            }else{
                               context.addMessage(null, new FacesMessage("Login failed."));
                               return;
                            }
                        }
                    } else {
                        context.addMessage(null, new FacesMessage("Login failed."));
                    }
                }
            } catch (ServletException e) {
                logger.log(Level.WARNING, e.getMessage());

                response.sendRedirect(request.getContextPath() + "/facebookError.html");
                return;
            }
            if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.mobilelandingpage"));
            } else {
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.landingpage"));
            }

        } else {
            logger.log(Level.WARNING, "CSRF protection validation");
             if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.mobilelandingpage"));
            } else {
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.landingpage"));
            }

        }
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

    private String getFacebookAccessToken(String faceCode) {
        String token = null;
        if (faceCode != null && !"".equals(faceCode)) {
            String appId = getValueFromKey("facebook.app.id");//"247417342102284";
            String redirectUrl = getValueFromKey("facebook.redirect.url");//http://localhost:8080/FitnessStats/index.sec";
            String faceAppSecret = getValueFromKey("facebook.app.secret");//"33715d0844267d3ba11a24d44e90be80";
            String newUrl = "https://graph.facebook.com/oauth/access_token?client_id="
                    + appId + "&redirect_uri=" + redirectUrl + "&client_secret="
                    + faceAppSecret + "&code=" + faceCode;
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            try {
                HttpGet httpget = new HttpGet(newUrl);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                token = StringUtils.removeEnd(StringUtils.removeStart(responseBody, "access_token="), "&expires=5180795");
            } catch (ClientProtocolException e) {
                logger.log(Level.WARNING, e.getMessage());
            } catch (IOException e) {
                logger.log(Level.WARNING, e.getMessage());
            } finally {
                try {
                    httpclient.close();
                } catch (IOException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
        }
        return token;
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
