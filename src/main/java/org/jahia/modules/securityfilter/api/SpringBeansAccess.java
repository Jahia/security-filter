package org.jahia.modules.securityfilter.api;

import org.jahia.services.content.JCRTemplate;

public final class SpringBeansAccess {
    private final static SpringBeansAccess INSTANCE = new SpringBeansAccess();
    private JCRTemplate jcrTemplate;

    private SpringBeansAccess() {
    }

    public static SpringBeansAccess getInstance() {
        return INSTANCE;
    }

    public JCRTemplate getJcrTemplate() {
        return jcrTemplate;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }

}
