package org.jahia.modules.securityfilter.accesskey.valves;

import org.apache.commons.lang.StringUtils;
import org.jahia.modules.securityfilter.accesskey.AccessKey;
import org.jahia.modules.securityfilter.accesskey.AccessKeyConfig;
import org.jahia.modules.securityfilter.jwt.impl.TokenVerificationResult;
import org.jahia.params.ProcessingContext;
import org.jahia.params.valves.AuthValveContext;
import org.jahia.params.valves.AutoRegisteredBaseAuthValve;
import org.jahia.pipelines.PipelineException;
import org.jahia.pipelines.valves.ValveContext;
import org.jahia.registries.ServicesRegistry;
import org.jahia.services.content.decorator.JCRUserNode;
import org.slf4j.Logger;

import javax.jcr.RepositoryException;
import javax.servlet.http.HttpServletRequest;

public class AccessKeyAuthValve extends AutoRegisteredBaseAuthValve {

    private static transient Logger logger = org.slf4j.LoggerFactory.getLogger(AccessKeyAuthValve.class);
    private static final ThreadLocal<TokenVerificationResult> THREAD_LOCAL = new ThreadLocal<TokenVerificationResult>();
    private static final String BEARER = "Bearer";
    private static final String SEPARATOR = ":";
    private AccessKeyConfig accessKeyConfig;


    public static TokenVerificationResult getAccessKeyVerificationStatus() {
        return THREAD_LOCAL.get();
    }
    public static void removeThreadLocal() { THREAD_LOCAL.remove(); }

    @Override
    public void invoke(Object context, ValveContext valveContext) throws PipelineException {

        if (!isEnabled()) {
            valveContext.invokeNext(context);
            return;
        }

        final AuthValveContext authContext = (AuthValveContext) context;
        final HttpServletRequest httpServletRequest = authContext.getRequest();

        String authorization = httpServletRequest.getHeader("Authorization");

        if (authorization == null || "".equals(authorization) || !authorization.contains(BEARER) || !authorization.contains(SEPARATOR)) {
            valveContext.invokeNext(context);
            return;
        }


        String token = StringUtils.substringAfter(authorization, BEARER).trim();
        String[] parts = token.split(SEPARATOR);
        AccessKey accessKey = new AccessKey();
        accessKey.setAccessId(parts[0]);
        accessKey.setKey(parts[1]);

        TokenVerificationResult tvr = new TokenVerificationResult();
        tvr.setAccessKey(accessKey);
        tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.NOT_FOUND);
        tvr.setMessage("Token not found");
        THREAD_LOCAL.set(tvr);

        try {
            if (accessKeyConfig.verifyAccessKey(accessKey)) {
                JCRUserNode user = ServicesRegistry.getInstance().getJahiaUserManagerService().lookup(accessKey.getUserKey());
                authContext.getSessionFactory().setCurrentUser(user.getJahiaUser());
                httpServletRequest.getSession().setAttribute(ProcessingContext.SESSION_USER, user);
                logger.info("Authenticated access key call for user {}", user.getDisplayableName());
                return;
            }
        } catch (RepositoryException e) {
            logger.error("Auth failed", e);
        }

        valveContext.invokeNext(context);
    }

    public void setAccessKeyConfig(AccessKeyConfig accessKeyConfig) {
        this.accessKeyConfig = accessKeyConfig;
    }
}
