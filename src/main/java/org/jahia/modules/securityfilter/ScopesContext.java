package org.jahia.modules.securityfilter;

import org.apache.commons.lang3.StringUtils;
import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.*;

public class ScopesContext implements ManagedService {

    private Map<String, Collection<String>> contexts = new HashMap<>();
    private ThreadLocal<Set<String>> scopesLocal = ThreadLocal.withInitial(HashSet::new);

    public static ScopesContext getInstance() {
        return Holder.INSTANCE;
    }

    public Collection<String> getScopes() {
        return Collections.unmodifiableSet(scopesLocal.get());
    }

    public void addScopes(Collection<String> scopes) {
        scopesLocal.get().addAll(scopes);
    }

    public void addContext(String context) {
        addScopes(contexts.get(context));
    }

    public void resetScopes() {
        scopesLocal.remove();
    }

    @Override
    public void updated(Dictionary<String, ?> properties) throws ConfigurationException {
        Enumeration<String> keys = properties.keys();
        while (keys.hasMoreElements()) {
            String nextElement = keys.nextElement();
            if (nextElement.endsWith(".scopes")) {
                contexts.put(StringUtils.split(nextElement, '.')[0], Arrays.asList(StringUtils.split((String) properties.get(nextElement), ',')));
            }
        }
    }

    private static class Holder {
        static final ScopesContext INSTANCE = new ScopesContext();

        private Holder() {
        }
    }
}
