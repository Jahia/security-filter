package org.jahia.modules.securityfilter.jwt.impl;

import com.auth0.jwt.interfaces.DecodedJWT;

public class TokenVerificationResult {

    public enum VerificationStatus {
        VERIFIED,
        REJECTED,
        NOT_FOUND
    }

    private VerificationStatus verificationStatusCode;
    private DecodedJWT token;

    public VerificationStatus getVerificationStatusCode() {
        return verificationStatusCode;
    }

    public void setVerificationStatusCode(VerificationStatus verificationStatusCode) {
        this.verificationStatusCode = verificationStatusCode;
    }

    public DecodedJWT getToken() {
        return token;
    }

    public void setToken(DecodedJWT token) {
        this.token = token;
    }
}
