package org.jahia.modules.securityfilter.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import java.util.Map;

public interface JWTService {
    String createToken(Map<String, Object> claims);
    DecodedJWT verifyToken(String token) throws JWTVerificationException;
}
