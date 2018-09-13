package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken;

import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.securityfilter.jwt.JWTService;
import org.jahia.osgi.BundleUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
public class QueryExtension {

    @GraphQLField
    @GraphQLDescription("")
    public static GraphQLToken jwtToken (@GraphQLNonNull @GraphQLName("scopes") @GraphQLDescription("") List<String> scopes,
                                         @GraphQLName("referers") @GraphQLDescription("") List<String> referers,
                                         @GraphQLName("ips") @GraphQLDescription("") List<String> ips) {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        Map<String, Object> claims = new HashMap<>();
        claims.put("scopes", scopes);
        if (referers != null) {
            claims.put("referers", scopes);
        }
        if (ips != null) {
            claims.put("ips", scopes);
        }

        return new GraphQLToken(jwtService.createToken(claims));
    }
}
