package org.jahia.modules.securityfilter.core;

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.securityfilter.ScopesContext;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ScopesFilter extends AbstractServletFilter {
    private ScopesContext scopesContext;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String origin = ((HttpServletRequest)request).getHeader("Origin");
        //Todo : Detect context
        if ("http://localhost:8080".equals(origin)) {
            scopesContext.addContext("hosted-context");
        }

        chain.doFilter(request, response);

        scopesContext.resetScopes();
    }

    @Override
    public void destroy() {

    }

    public void setScopesContext(ScopesContext scopesContext) {
        this.scopesContext = scopesContext;
    }
}
