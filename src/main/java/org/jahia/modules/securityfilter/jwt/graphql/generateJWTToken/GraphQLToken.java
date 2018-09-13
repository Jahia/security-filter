package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken;

import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;

@GraphQLName("jwt_token")
public class GraphQLToken {

    private String token;

    public GraphQLToken(String token) {
        this.token = token;
    }

    @GraphQLField
    public String getToken() {
        return token;
    }

}
