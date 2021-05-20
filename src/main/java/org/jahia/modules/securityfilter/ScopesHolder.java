package org.jahia.modules.securityfilter;

import java.util.*;

public class ScopesHolder {

    private Map<String, Collection<String>> contexts = new HashMap<>();
    private ThreadLocal<Set<String>> scopesLocal = ThreadLocal.withInitial(HashSet::new);

    public static ScopesHolder getInstance() {
        return Holder.INSTANCE;
    }

    public Collection<String> getScopes() {
        return Collections.unmodifiableSet(scopesLocal.get());
    }

    public void addScopes(Collection<String> scopes) {
        scopesLocal.get().addAll(scopes);
    }

    public void addContext(String context) {
        if (contexts.get(context) != null) {
            addScopes(contexts.get(context));
        }
    }

    public void resetScopes() {
        scopesLocal.remove();
    }

    public void addScopeInContext(String context, String scope) {
        contexts.putIfAbsent(context, new HashSet<>());
        contexts.get(context).add(scope);
    }

    public void removeScopeFromContext(String context, String scope) {
        if (contexts.containsKey(context)) {
            contexts.get(context).remove(scope);
        }
    }

    private static class Holder {
        static final ScopesHolder INSTANCE = new ScopesHolder();

        private Holder() {
        }
    }
}
