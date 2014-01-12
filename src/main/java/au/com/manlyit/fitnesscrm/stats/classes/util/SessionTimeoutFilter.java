/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
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
 * @author tgiunipero
 */
//@WebFilter(servletNames = {"Controller"})
@WebFilter("*.xhtml")
public class SessionTimeoutFilter implements Filter {

    private static final Logger logger = Logger.getLogger(SessionTimeoutFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;
        HttpSession session = req.getSession(false);
        //User user = (session != null) ? (User) session.getAttribute("user") : null;
        String loginURL = req.getContextPath() + "/login.xhtml";

        if (session == null && !req.getRequestURI().equals(loginURL)) {
            res.sendRedirect(loginURL);
            logger.log(Level.INFO, "Redirecting to login page as the session has timed out");
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
