package org.jahia.modules.securityfilter.core;

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.securityfilter.AuthorizationScopesService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class ContextFilter extends AbstractServletFilter {

    private AuthorizationScopesService authorizationScopesService;

    public void setAuthorizationScopesService(AuthorizationScopesService authorizationScopesService) {
        this.authorizationScopesService = authorizationScopesService;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        String origin = ((HttpServletRequest)request).getHeader("Origin");
        //Todo : Detect context
        if ("http://localhost:8080".equals(origin)) {
            authorizationScopesService.addContext("hosted-context");
        }

        chain.doFilter(request, response);

        authorizationScopesService.resetScopes();
    }

    @Override
    public void destroy() {

    }
}
