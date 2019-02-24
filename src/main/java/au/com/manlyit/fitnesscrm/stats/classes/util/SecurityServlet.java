/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import au.com.manlyit.fitnesscrm.stats.beans.ApplicationBean;
import au.com.manlyit.fitnesscrm.stats.beans.LoginBean;
import au.com.manlyit.fitnesscrm.stats.beans.util.CustomerStatus;
import au.com.manlyit.fitnesscrm.stats.chartbeans.MySessionsChart1;
import au.com.manlyit.fitnesscrm.stats.classes.CustomersController;
import au.com.manlyit.fitnesscrm.stats.classes.EziDebitPaymentGateway;
import au.com.manlyit.fitnesscrm.stats.classes.PasswordService;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
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
import net.sf.json.JSONSerializer;

import net.sf.json.JSONObject;

import org.apache.commons.lang.RandomStringUtils;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

@WebServlet("*.sec")
public class SecurityServlet extends HttpServlet {

    private static final long serialVersionUID = 8071426090770097330L;
    private static final Logger logger = Logger.getLogger(SecurityServlet.class.getName());
    //private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
    @Inject
    private CustomersController controller;
    @Inject
    private LoginBean loginBean;

    @Inject
    private ApplicationBean applicationBean;
    @Inject
    private EziDebitPaymentGateway eziDebitPaymentGatewayController;
    @Inject
    private MySessionsChart1 mySessionsChart1Controller;

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
        //logger.log(Level.INFO, "*** Called SecurityServlet");
        HttpSession httpSession = request.getSession();
        String faceCode = request.getParameter("code");
        String stateEncoded = request.getParameter("state");
        String state = URLDecoder.decode(stateEncoded, "UTF-8");
        String redirect = state.substring(0, 1);
        state = state.substring(1);
        if (redirect.compareTo("1") == 0) {
            loginBean.setDontRedirect(true);
        }
        //boolean mobileDevice = false;
        //FacesContext context = FacesContext.getCurrentInstance();
        String accessToken = getFacebookAccessToken(faceCode);
        Customers facebookUser = getUserMailAddressFromJsonResponse(accessToken, httpSession);
        String sessionID = httpSession.getId();
        String facebookUsernameCRM = "NULL";
        String facebookemailCRM = "NULL";
        if (facebookUser != null) {
            facebookUsernameCRM = facebookUser.getUsername();
            facebookemailCRM = facebookUser.getEmailAddress();
        }
        String facebookState = applicationBean.getFacebookLoginState(state);
        logger.log(Level.INFO, "SecurityServlet called: SessionId={0}, State:={1},FacebookCode={2}, Token from code:{3}, facebookLoginBean Stored State {4}, CRM User {5}, CRM email {6}", new Object[]{sessionID, state, faceCode, accessToken, facebookState, facebookUsernameCRM, facebookemailCRM});
        if (facebookState != null) { // the stat exists in our map. The actual value is teh timestamp in miliiseconds of when it was used.
            String pfmEncrptedPassword = null;
            try {
                applicationBean.removeFacebookLoginState(state);
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
                        if (customer.getActive().getCustomerState().contentEquals(CustomerStatus.CANCELLED.value()) == false) {
                            String passwd = RandomStringUtils.random(20);
                            String encPassword = PasswordService.getInstance().encrypt(passwd);
                            pfmEncrptedPassword = customer.getPassword();
                            customer.setPassword(encPassword);
                            customer.setFacebookId(fbid);
                            ejbFacade.editAndFlush(customer);

                            try {
                                request.login(customer.getUsername(), passwd);
                                customer.setPassword(pfmEncrptedPassword);
                                String auditDetails = "Customer Login Successful:" + customer.getUsername() + " Details:  " + customer.getLastname() + " " + customer.getFirstname() + " ";
                                String changedFrom = "UnAuthenticated";
                                String changedTo = "Authenticated User:" + customer.getUsername();
                                if (customer.getUsername().toLowerCase(Locale.getDefault()).equals("synthetic.tester")) {
                                    logger.log(Level.INFO, "Synthetic Tester Logged In.");
                                } else {
                                    ejbAuditLogFacade.audit(customer, customer, "Logged In", auditDetails, changedFrom, changedTo);
                                }
                                customer.setLastLoginTime(new Date());
                                customer.setLoginAttempts(0);
                                ejbFacade.editAndFlush(customer);
                                if (controller != null && eziDebitPaymentGatewayController != null && mySessionsChart1Controller != null) {
                                    controller.updateSelectedCustomer(customer);
                                    eziDebitPaymentGatewayController.setSessionId(sessionID);
                                    logger.log(Level.INFO, "SecurityServlet - SET SESSION ID = {0} for user : {1}", new Object[]{sessionID, customer.getUsername()});
                                    //eziDebitPaymentGatewayController.setSelectedCustomer(customer);

                                } else {
                                    logger.log(Level.WARNING, "Customer Controller injection into security servlet failed!");
                                }
                            } catch (ServletException servletException) {
                                if (servletException.getMessage().contains("UT010030") == true) {//javax.servlet.ServletException: UT010030: User already logged in

                                    logger.log(Level.INFO, "This is request has already been authenticated. User {0} is already logged in.", customer);
                                } else {
                                    logger.log(Level.WARNING, "Login failed. Customer ({0}) was denied access - facebook login button. Message: {1}", new Object[]{customer.getUsername(), servletException.getMessage()});
                                    customer.setPassword(pfmEncrptedPassword);
                                    ejbFacade.editAndFlush(customer);
                                    if (servletException.getMessage().contains("Login failed") == false) {
                                        throw servletException;
                                    } else {
                                        logger.log(Level.WARNING, "Login failed. Customer ({0}) was denied access - facebook login button. Message: {1}", new Object[]{customer.getUsername(), servletException.getMessage()});
                                        //context.addMessage(null, new FacesMessage("Login failed."));
                                        return;
                                    }
                                }
                            }
                        } else {
                            logger.log(Level.WARNING, "A cancelled customer ({0}) was denied access - facebook login button.", customer.getUsername());
                            //context.addMessage(null, new FacesMessage("Access Denied - Cancelled Status"));
                        }
                    } else {
                        logger.log(Level.WARNING, "Login failed. Customer ({0}) was denied access - facebook login button.", customer.getUsername());
                        //context.addMessage(null, new FacesMessage("Login failed."));
                    }
                }
            } catch (ServletException e) {
                logger.log(Level.WARNING, e.getMessage());

                response.sendRedirect(request.getContextPath() + "/facebookError.html");
                return;
            }

            redirectToLandingPage(request, response);

            /*if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.mobilelandingpage"));
            } else {
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.landingpage"));
            }*/
        } else {
            logger.log(Level.WARNING, "CSRF protection validation - The Session ID passed to the original request was does not match the one in the post to this servlet");
            redirectToLandingPage(request, response);
            /*if (mobileDevice(request)) {
                httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.mobilelandingpage"));
            } else {
                response.sendRedirect(request.getContextPath() + getValueFromKey("facebook.redirect.landingpage"));
            }*/

        }
    }

    private void redirectToLandingPage(HttpServletRequest request, HttpServletResponse response) {

        try {

            HttpSession httpSession = request.getSession();
            String landingPage;
            String adminRole = getValueFromKey("facebook.redirect.adminRole");
            if (adminRole == null || adminRole.isEmpty()) {
                adminRole = "ADMIN"; //default
            }
            if (loginBean.isDontRedirect() == true) {
                landingPage = getValueFromKey("timetable.redirect.landingpage");

                logger.log(Level.INFO, "Redirection to timetable.");
                loginBean.setDontRedirect(false);

            } else {
                if (mobileDevice(request)) {
                    httpSession.setAttribute("MOBILE_DEVICE", "TRUE");
                    logger.log(Level.INFO, "Mobile Device user agent detected. Redirecting to the mobile landing page.");
                    if (request.isUserInRole(adminRole) == true) {
                        landingPage = getValueFromKey("facebook.redirect.mobileadminlandingpage");
                    } else {
                        landingPage = getValueFromKey("facebook.redirect.mobilelandingpage");
                    }
                } else if (request.isUserInRole(adminRole) == true) {
                    landingPage = getValueFromKey("facebook.redirect.adminlandingpage");
                } else {
                    landingPage = getValueFromKey("facebook.redirect.landingpage");
                }
            }
            response.sendRedirect(request.getContextPath() + landingPage);
            logger.log(Level.INFO, "Redirecting to Landing Page:", landingPage);
        } catch (Exception e) {
            JsfUtil.addErrorMessage(e, "Redirect to landing page at Login Failed.");
            logger.log(Level.WARNING, "Redirect to landing page at Login Failed", e);
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
            String newUrl = null;
            try {
                newUrl = "https://graph.facebook.com/oauth/access_token?client_id="
                        + appId + "&redirect_uri=" + URLEncoder.encode(redirectUrl, "UTF-8") + "&client_secret="
                        + faceAppSecret + "&code=" + faceCode;
            } catch (UnsupportedEncodingException ex) {
                Logger.getLogger(SecurityServlet.class.getName()).log(Level.SEVERE, "getFacebookAccessToken", ex);
            }
            CloseableHttpClient httpclient = HttpClientBuilder.create().build();
            JSONParser parser = new JSONParser();
            try {
                HttpGet httpget = new HttpGet(newUrl);
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                if (responseBody != null) {
                    try {
                        Object obj = parser.parse(responseBody);
                        //org.json.simple.JSONArray array = (org.json.simple.JSONArray) obj;
                        org.json.simple.JSONObject obj2 = (org.json.simple.JSONObject) obj;
                        token = (String) obj2.get("access_token");
                    } catch (ParseException parseException) {
                        logger.log(Level.WARNING, parseException.getMessage());
                    }
                }
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
                String newUrl = "https://graph.facebook.com/me?fields=id,name,email,birthday&access_token=" + accessToken;
                httpclient = HttpClientBuilder.create().build();
                HttpGet httpget = new HttpGet(newUrl);
                logger.log(Level.WARNING, "Get info from face --> executing request: {0}", httpget.getURI());
                ResponseHandler<String> responseHandler = new BasicResponseHandler();
                String responseBody = httpclient.execute(httpget, responseHandler);
                String name = null;
                String birthday = null;
                String facebookId = null;
                try {
                    JSONObject json = (JSONObject) JSONSerializer.toJSON(responseBody);
                    facebookId = json.getString("id");

                    name = json.getString("name");
                    //lastName = json.getString("last_name");
                    httpSession.setAttribute("FACEBOOK_USER", name + ", facebookId:" + facebookId);

                    email = json.getString("email");
                    //put user data in session
                    birthday = json.getString("birthday");

                    if (facebookId == null || name == null || email == null || birthday == null) {
                        if (facebookId == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: Facebook ID is NULL");
                        }
                        if (name == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: name is NULL");
                        }
                        if (email == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: email is NULL");
                        }
                        if (birthday == null) {
                            logger.log(Level.WARNING, "Error getting JSON objects from facebook: birthday is NULL");
                        }
                    }
                    logger.log(Level.INFO, "facebook JSON params recieve: name={0},email={1},birthday={2},facebookId={3}, httpSession = {4}", new Object[]{name, email, birthday, facebookId, httpSession});
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error getting JSON objects from facebook}", e);
                }
                String[] names = name.split(" ");
                if (names.length > 1) {
                    cust.setFirstname(names[0]);
                    cust.setLastname(names[1]);
                } else {
                    cust.setFirstname(name);
                    cust.setLastname("");
                }
                cust.setEmailAddress(email);
                cust.setFacebookId(facebookId);
                SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy");
                try {
                    cust.setDob(sdf.parse(birthday));
                } catch (java.text.ParseException parseException) {
                    logger.log(Level.WARNING, "Error getting birthday from facebook: birthday is could not be converted to a Date!!", parseException);
                }
                try {
                    cust.setUsername(name.trim().replace(" ", "."));
                } catch (Exception e) {
                    logger.log(Level.WARNING, "Error setting Username from facebook data!!", e);
                }
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
