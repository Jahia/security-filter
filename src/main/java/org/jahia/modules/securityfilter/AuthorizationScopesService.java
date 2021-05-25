package org.jahia.modules.securityfilter;

import java.util.Collection;
import java.util.Collections;

public interface AuthorizationScopesService {

    Collection<String> getScopes();

    void addScopes(Collection<String> scopes);

    void addContext(String context);

    void resetScopes();

    void addScopeInContext(String context, String scope);

    void removeScopeFromContext(String context, String scope);

}
