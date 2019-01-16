/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2019 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
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
