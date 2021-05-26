package org.jahia.modules.securityfilter.core.grant;

import java.util.Map;

public interface Grant {

    boolean matches(Map<String, Object> query);
}
