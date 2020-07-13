package org.jahia.modules.securityfilter.accesskey.api.graphql;

import org.jahia.modules.graphql.provider.dxm.DXGraphQLExtensionsProvider;
import org.jahia.modules.securityfilter.accesskey.api.graphql.mutations.AccessKeyMutationExtension;
import org.jahia.modules.securityfilter.accesskey.api.graphql.queries.AccessKeyQueryExtension;
import org.osgi.service.component.annotations.Component;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component(service = DXGraphQLExtensionsProvider.class, immediate = true)
public class AccessKeyExtensionProvider implements DXGraphQLExtensionsProvider {

    /**
     * This method will build the list of extension points Commerce IO wants to add to the DX GraphQL provider
     * @return list of classes to be used as extension points for the DX GraphQL provider
     */
    @Override
    public Collection<Class<?>> getExtensions() {
        List<Class<?>> extensions = new ArrayList<>();
        extensions.add(AccessKeyMutationExtension.class);
        extensions.add(AccessKeyQueryExtension.class);
        return extensions;
    }
}
