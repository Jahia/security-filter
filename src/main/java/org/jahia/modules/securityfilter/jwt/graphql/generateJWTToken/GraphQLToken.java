package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("jwt_token")
public class GraphQLToken {

    private String token;
    private String claims;

    public GraphQLToken(String token, String claims) {
        this.token = token;
        this.claims = claims;
    }

    @GraphQLField
    public String getToken() {
        return token;
    }

    @GraphQLField
    public String getClaims() {
        return claims;
    }
}
