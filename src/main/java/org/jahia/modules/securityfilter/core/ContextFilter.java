package org.jahia.modules.securityfilter.core;

import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.securityfilter.AuthorizationScopesService;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;

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
        authorizationScopesService.addContext("default");

        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String origin = httpServletRequest.getHeader("Origin");
        if (origin == null) {
            origin = httpServletRequest.getHeader("Referer");
        }
        if (origin != null) {
            String host = new URL(origin).getHost();
            if (host.equals(request.getServerName())) {
                authorizationScopesService.addContext("hosted-context");
            }
        }

        chain.doFilter(request, response);

        authorizationScopesService.resetScopes();
    }

    @Override
    public void destroy() {

    }
}
