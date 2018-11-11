/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import static au.com.manlyit.fitnesscrm.stats.beans.ActivationBean.generateUniqueToken;
import au.com.manlyit.fitnesscrm.stats.classes.PasswordService;
import au.com.manlyit.fitnesscrm.stats.db.Activation;
import au.com.manlyit.fitnesscrm.stats.db.Customers;
import java.io.IOException;
import java.security.Principal;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.el.ELException;
import javax.inject.Inject;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author david
 */
@WebFilter("/reset.html")
public class PasswordResetFilter implements Filter {

    private static final Logger logger = Logger.getLogger(SessionTimeoutFilter.class.getName());
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.AuditLogFacade ejbAuditLogFacade;
    private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(true);
        String sessionId = session.getId();
        String key = request.getParameter("key");
        if (key != null) {
            logger.log(Level.INFO, "Checking password reset key for session id:{0}.", sessionId);
            String keyDecrypted = encrypter.decrypt(key);
// decrypt nonce encrypted by login bean
            if (keyDecrypted != null) {
                String token = configMapFacade.getConfig("login.password.reset.token").trim();
                if (keyDecrypted.matches("[0-9A-Z]*")) {//"[0-9A-Z]*"  - nonce 
                    try {

                        if (keyDecrypted.indexOf(token) == 0) {
                            // we have a token , now check if it's valid
                            String incomingNonce = keyDecrypted.substring(token.length());

                            Activation act2;
                            try {
                                act2 = ejbActivationFacade.findToken(incomingNonce);
                                int minutesValid = Integer.parseInt(configMapFacade.getConfig("login.password.reset.minutes").trim());
                                GregorianCalendar sendTime = new GregorianCalendar();
                                GregorianCalendar now = new GregorianCalendar();
                                sendTime.setTime(act2.getActTimestamp());
                                sendTime.add(Calendar.MINUTE, minutesValid);
                                if (now.compareTo(sendTime) < 0) {
                                    // it is valid log them in
                                    //Authenticator 
                                    Customers cst = act2.getCustomer();
                                    String user = cst.getUsername();
                                    //Reset Password and log them in
                                    String paswd = generateUniqueToken(10);
                                    //String paswd = "foooobar";
                                    String encryptedPass = PasswordService.getInstance().encrypt(paswd);
                                    // String encodedEncryptedPass = URLEncoder.encode(encryptedPass,"UTF-8");
                                    cst.setPassword(encryptedPass);
                                    ejbCustomerFacade.edit(cst);
                                    try {
                                        

                                            req.login(user, paswd);
                                            logger.log(Level.INFO, "The reset password link executed successfully. User {0} has been logged in.", user);
                                            String auditDetails = "Customer Login Successful:" + cst.getUsername() + " Details:  " + cst.getLastname() + " " + cst.getFirstname() + " ";
                                            String changedFrom = "UnAuthenticated";
                                            String changedTo = "Authenticated User:" + cst.getUsername();
                                            if (cst.getUsername().toLowerCase(Locale.getDefault()).equals("synthetic.tester")) {
                                                logger.log(Level.INFO, "Synthetic Tester Logged In.");
                                            } else {
                                                ejbAuditLogFacade.audit(cst, cst, "Logged In", auditDetails, changedFrom, changedTo);
                                            }
                                        
                                    } catch (ServletException servletException) {
                                        if (servletException.getMessage().contains("UT010030") == false) {//javax.servlet.ServletException: UT010030: User already logged in
                                            throw servletException;
                                        } else {
                                            logger.log(Level.INFO, "This is request has already been authenticated. User {0} is already logged in.", user);
                                        }
                                    }
                                    //Redirect to the user details page
                                    // FacesContext fc = FacesContext.getCurrentInstance();
                                    //CustomersController custController = (CustomersController) fc.getApplication().evaluateExpressionGet(fc, "#{customersController}", CustomersController.class);
                                    // custController.setSelected(cst);

                                    try {

                                        String detailsURL = req.getContextPath() + "/myDetails.xhtml";
                                        String surveysURL = req.getContextPath() + "/customerSurveys.xhtml";
                                        //String timetableURL =  "http://www.manlybeachfemalefitness.com.au/schedule/schedule.xhtml";
                                        String timetableURL =  configMapFacade.getConfig("login.signup.timetable.url").trim();
                                        if (cst.getTermsConditionsAccepted() == true) {
                                            res.sendRedirect(detailsURL);
                                        } else {
                                            //res.sendRedirect(surveysURL);
                                            res.sendRedirect(timetableURL);
                                        }

                                    } catch (IOException e) {
                                        logger.log(Level.SEVERE, "Redirect to MyDetails failed", e);
                                        //JsfUtil.addErrorMessage(e, "Redirect to MyDetails failed");
 
                                    }

                                } else {
                                    String detailsURL = req.getContextPath() + "/login.xhtml";
                                    res.sendRedirect(detailsURL);
                                    logger.log(Level.INFO, "The reset password link has expired");
                                }
                            } catch (ServletException | ELException e2) {
                                JsfUtil.addErrorMessage(e2, "Nonce does not exist. Possible hack attempt: " + key);
                            }
                        }

                    } catch (NumberFormatException e) {
                        JsfUtil.addErrorMessage(e, "Error processing password reset token. login.password.reset.minutes in configmap may be null or non numeric. ");

                    }

                }

            } else {
                chain.doFilter(req, res);
                logger.log(Level.INFO, "nonce could not be decrypted for password reset", key);
            }
        } else {
            chain.doFilter(req, res);
        }
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
    }

    @Override
    public void destroy() {
    }

}
