package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.mutation;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import graphql.annotations.annotationTypes.*;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.securityfilter.JWTService;
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
public class MutationExtension {
    private static final Logger logger = LoggerFactory.getLogger(MutationExtension.class);
    @GraphQLField
    @GraphQLDescription("Generate a new JWT token")
    public static GraphQLToken jwtToken (@GraphQLNonNull @GraphQLName("scopes") @GraphQLDescription("") List<String> scopes,
                                         @GraphQLName("referer") @GraphQLDescription("") List<String> referer,
                                         @GraphQLName("ips") @GraphQLDescription("") List<String> ips) throws RepositoryException {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        Map<String, Object> claims = new HashMap<>();
        claims.put("scopes", scopes);
        if (referer != null) {
            claims.put("referer", referer);
        }
        if (ips != null) {
            claims.put("ips", ips);
        }
        try {
            String token = jwtService.createToken(claims);
            DecodedJWT decoded = jwtService.verifyToken(token);
            return new GraphQLToken(decoded, new ObjectMapper().writeValueAsString(claims));
        } catch(JsonProcessingException ex) {
            logger.error("Cannot convert object to json", ex);
        }
        return null;
    }
}
