package org.jahia.modules.securityfilter.jwt.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class JWTTokenGraphQLExtensionProvider implements DXGraphQLExtensionsProvider {

    /**
     * This method will build the list of extension points Commerce IO wants to add to the DX GraphQL provider
     * @return list of classes to be used as extension points for the DX GraphQL provider
     */
    @Override
    public Collection<Class<?>> getExtensions() {
        List<Class<?>> extensions = new ArrayList<>();
        extensions.add(org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.mutation.QueryExtension.class);
        extensions.add(org.jahia.modules.securityfilter.jwt.graphql.generateJWTToken.query.QueryExtension.class);
        return extensions;
    }
}
