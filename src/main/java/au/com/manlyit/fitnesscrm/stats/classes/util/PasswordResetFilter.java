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
import java.net.URLEncoder;
import java.util.Calendar;
import java.util.GregorianCalendar;
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
@WebFilter("*.xhtml")
public class PasswordResetFilter implements Filter {

    private static final Logger logger = Logger.getLogger(SessionTimeoutFilter.class.getName());
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ConfigMapFacade configMapFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.ActivationFacade ejbActivationFacade;
    @Inject
    private au.com.manlyit.fitnesscrm.stats.beans.CustomersFacade ejbCustomerFacade;

    private final StringEncrypter encrypter = new StringEncrypter("(lqKdh^Gr$2F^KJHG654)");

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        String key = request.getParameter("key");
        if (key != null) {

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
                                    req.login(user, paswd);
                                //Redirect to the user details page
                                    // FacesContext fc = FacesContext.getCurrentInstance();
                                    //CustomersController custController = (CustomersController) fc.getApplication().evaluateExpressionGet(fc, "#{customersController}", CustomersController.class);
                                    // custController.setSelected(cst);

                                    try {

                                        String detailsURL = req.getContextPath() + "/myDetails.xhtml";
                                        res.sendRedirect(detailsURL);

                                    } catch (IOException e) {
                                        JsfUtil.addErrorMessage(e, "Redirect to MyDetails failed");

                                    }

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
