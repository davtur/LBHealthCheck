/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package au.com.manlyit.fitnesscrm.stats.classes.util;

import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;
import javax.faces.application.ResourceHandler;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebFilter(servletNames = {"Faces Servlet"}) // Must match <servlet-name> of your FacesServlet.
public class NoCacheFilter implements Filter {
   // this is used to stop the annoying viewExpiredException that can occur due to cacheing
    // we also catch the exception in Web.xml with a ViewExpiredException listener
    private static final Logger logger = Logger.getLogger(NoCacheFilter.class.getName());

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse res = (HttpServletResponse) response;

        if (!req.getRequestURI().startsWith(req.getContextPath() + ResourceHandler.RESOURCE_IDENTIFIER)) { // Skip JSF resources (CSS/JS/Images/etc)
            res.setHeader("Cache-Control", "no-cache, no-store, must-revalidate"); // HTTP 1.1.
            res.setHeader("Pragma", "no-cache"); // HTTP 1.0.
            res.setDateHeader("Expires", 0); // Proxies.
        }

        chain.doFilter(request, response);
    }

    @Override
    public void init(FilterConfig fc) {
        logger.log(Level.INFO, "NoCacheFilter Initialised");

    }

    @Override
    public void destroy() {
        logger.log(Level.INFO, "NoCacheFilter Destroyed");
    }
}
