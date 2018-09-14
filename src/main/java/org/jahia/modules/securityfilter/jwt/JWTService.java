package org.jahia.modules.securityfilter.jwt;

import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.Claim;
import com.auth0.jwt.interfaces.DecodedJWT;

import javax.jcr.RepositoryException;
import java.util.Map;

public interface JWTService {
    String createToken(Map<String, Object> claims) throws RepositoryException;
    DecodedJWT verifyToken(String token) throws JWTVerificationException, RepositoryException;
}
