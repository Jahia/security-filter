package org.jahia.modules.securityfilter.jwt.impl;

import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class JWTFilter extends AbstractServletFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private static final String BEARER = "Bearer";
    private JWTConfig jwtConfig;
    private static final ThreadLocal<TokenVerificationResult> THREAD_LOCAL = new ThreadLocal<TokenVerificationResult>();

    public static TokenVerificationResult getJWTTokenVerificationStatus() {
        return THREAD_LOCAL.get();
    }

    public void setJwtConfig(JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //Do nothing for now
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        System.out.println("Servlet jwt filter");
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String authorization = httpRequest.getHeader("Authorization");

        TokenVerificationResult tvr = new TokenVerificationResult();

        tvr.setToken(null);
        tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.NOT_FOUND);
        tvr.setMessage("Token not found");
        THREAD_LOCAL.set(tvr);

        if (authorization != null && authorization.contains(BEARER)) {
            String token = StringUtils.substringAfter(authorization, BEARER).trim();

            try {
                DecodedJWT decodedToken = jwtConfig.verifyToken(token);

                String referer = httpRequest.getHeader("referer");
                List<String> referers = decodedToken.getClaim("referers").asList(String.class);

                String ip = httpRequest.getHeader("X-FORWARDED-FOR") != null
                        ? httpRequest.getHeader("X-FORWARDED-FOR") : httpRequest.getRemoteAddr();
                List<String> ips = decodedToken.getClaim("ips").asList(String.class);

                //Check referers
                if (referers != null && !referers.contains(referer)) {
                    tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                    tvr.setMessage("Incorrect referer in token");
                }
                //Check IP
                else if (ips != null && !ips.contains(ip)) {
                    tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                    tvr.setMessage("Your IP did not match any of the permitted IPs");
                }
                else {
                    tvr.setToken(decodedToken);
                    tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.VERIFIED);
                    tvr.setMessage("Token verified");
                }
            }
            catch (Exception e) {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                tvr.setMessage("Failed to verify token");
                logger.error("Failed to verify JWT token: {}", e.getMessage());
            }
        }

        filterChain.doFilter(httpRequest, servletResponse);
    }

    @Override
    public void destroy() {
        //Do nothing for now
    }
}
