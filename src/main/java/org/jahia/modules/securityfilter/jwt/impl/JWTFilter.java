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

import javax.servlet.http.HttpServletRequest;

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

        if (authorization != null && authorization.contains(BEARER)) {
            String token = StringUtils.substringAfter(authorization, BEARER).trim();

            try {
                DecodedJWT decodedToken = jwtConfig.verifyToken(token);
                tvr.setToken(decodedToken);
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.VERIFIED);
            }
            catch (JWTVerificationException e) {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                logger.error("Failed to verify token", e);
            }
        }

        THREAD_LOCAL.set(tvr);
        return null;
    }

    public void setJwtConfig(JWTConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }
}
