package org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.query;

import com.fasterxml.jackson.core.JsonProcessingException;
import graphql.annotations.annotationTypes.GraphQLDescription;
import graphql.annotations.annotationTypes.GraphQLField;
import graphql.annotations.annotationTypes.GraphQLName;
import graphql.annotations.annotationTypes.GraphQLTypeExtension;
import org.jahia.modules.graphql.provider.dxm.DXGraphQLProvider;
import org.jahia.modules.securityfilter.jwt.JWTService;
import org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.GraphQLToken;
import org.jahia.osgi.BundleUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;

@GraphQLTypeExtension(DXGraphQLProvider.Query.class)
@GraphQLDescription("Retrieve JWT token")
public class QueryExtension {
    private static final Logger logger = LoggerFactory.getLogger(QueryExtension.class);

    @GraphQLField
    @GraphQLDescription("Retrieves retrieve jwt token")
    public static GraphQLToken existingJWTToken(@GraphQLName("path") String path) {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        try {
            return jwtService.getExistingToken(path);
        } catch(RepositoryException ex ) {
            logger.error("Failed to retrieve token", ex);
        } catch(JsonProcessingException ex) {
            logger.error("Failed to convert object to json", ex);
        }
        return null;
    }

    @GraphQLField
    @GraphQLDescription("Verify if user has permissions")
    public static boolean isAuthorized() {
        JWTService jwtService = BundleUtils.getOsgiService(JWTService.class, null);
        return jwtService.isAuthorized();
    }
}
