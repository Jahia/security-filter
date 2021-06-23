package org.jahia.modules.securityfilter.core.apply;

import javax.servlet.http.HttpServletRequest;

public interface AutoApply {
    boolean shouldApply(HttpServletRequest request);
}
