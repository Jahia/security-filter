package org.jahia.modules.securityfilter.core.constraint;

import javax.servlet.http.HttpServletRequest;

public interface Constraint {
    boolean isValid(HttpServletRequest request);
}
