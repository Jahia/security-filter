package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.mutation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.securityfilter.jwt.JWTService;
import org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.GraphQLToken;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GraphQLTypeExtension(DXGraphQLProvider.Mutation.class)
@GraphQLMutation
@GraphQLDescription("Create JWT token")
public class QueryExtension {
    private static final Logger logger = LoggerFactory.getLogger(QueryExtension.class);
    @GraphQLField
    @GraphQLDescription("")
    public static GraphQLToken jwtToken (@GraphQLNonNull @GraphQLName("scopes") @GraphQLDescription("") List<String> scopes,
                                         @GraphQLName("referers") @GraphQLDescription("") List<String> referers,
                                         @GraphQLName("ips") @GraphQLDescription("") List<String> ips) throws RepositoryException {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        Map<String, Object> claims = new HashMap<>();
        claims.put("scopes", scopes);
        if (referers != null) {
            claims.put("referers", referers);
        }
        if (ips != null) {
            claims.put("ips", ips);
        }
        try {
            return new GraphQLToken(jwtService.createToken(claims), new ObjectMapper().writeValueAsString(claims));
        } catch(JsonProcessingException ex) {
            logger.error("Cannot convert object to json", ex);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Deletes the token specified by the given path")
    public static boolean deleteJWTToken (@GraphQLNonNull @GraphQLName("path") @GraphQLDescription("Path of the node to be deleted") String path) {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        return jwtService.deleteJWTToken(path);
    }
}
