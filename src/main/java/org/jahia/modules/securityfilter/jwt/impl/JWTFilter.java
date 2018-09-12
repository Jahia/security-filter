package org.jahia.modules.securityfilter.jwt.impl;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import org.apache.commons.lang.StringUtils;
import org.jahia.services.render.RenderContext;
import org.jahia.services.render.Resource;
import org.jahia.services.render.filter.AbstractFilter;
import org.jahia.services.render.filter.RenderChain;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

public class JWTFilter extends AbstractFilter {

    private static final Logger logger = LoggerFactory.getLogger(JWTFilter.class);

    private static final String BEARER = "Bearer";
    private JWTConfig jwtConfig;
    private static final ThreadLocal<TokenVerificationResult> THREAD_LOCAL = new ThreadLocal<TokenVerificationResult>();

    public static TokenVerificationResult getJWTTokenVerificationStatus() {
        return THREAD_LOCAL.get();
    }

    @Override
    public String prepare(RenderContext renderContext, Resource resource, RenderChain chain) throws Exception {
        HttpServletRequest request = renderContext.getRequest();
        String authorization = request.getHeader("Authorization");
        TokenVerificationResult tvr = new TokenVerificationResult();

        tvr.setToken(null);
        tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.NOT_FOUND);
        tvr.setMessage("Token not found");
        THREAD_LOCAL.set(tvr);

        if (authorization != null && authorization.contains(BEARER)) {
            String token = StringUtils.substringAfter(authorization, BEARER).trim();

            try {
                DecodedJWT decodedToken = jwtConfig.verifyToken(token);

                //Check referers
                String referer = request.getHeader("referer");
                List<String> referers = decodedToken.getClaim("referer").asList(String.class);
                if (referers != null && !referers.contains(referer)) {
                    tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                    tvr.setMessage("Incorrect referer in token");
                    return null;
                }
                //Check IP
                String ip = request.getHeader("X-FORWARDED-FOR") != null ? request.getHeader("X-FORWARDED-FOR") : request.getRemoteAddr();
                List<String> ips = decodedToken.getClaim("ip").asList(String.class);
                if (ips != null && !ips.contains(ip)) {
                    tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                    tvr.setMessage("Your IP did not match any of the permitted IPs");
                    return null;
                }

                tvr.setToken(decodedToken);
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.VERIFIED);
                tvr.setMessage("Token verified");
            }
            catch (Exception e) {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                tvr.setMessage("Failed to verify token");
                logger.error("Failed to verify JWT token: {}", e.getMessage());
            }
        }
        return null;
    }

    public void setJwtConfig(JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }
}
