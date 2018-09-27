package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken;

import com.auth0.jwt.interfaces.DecodedJWT;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("JWTToken")
public class GraphQLToken {

    private DecodedJWT token;
    private String claims;

    public GraphQLToken(DecodedJWT token, String claims) {
        this.token = token;
        this.claims = claims;
    }

    @GraphQLField
    public String getId() {
        return token.getId();
    }

    @GraphQLField
    public String getToken() {
        return token.getToken();
    }

    @GraphQLField
    public String getClaims() {
        return claims;
    }
}
