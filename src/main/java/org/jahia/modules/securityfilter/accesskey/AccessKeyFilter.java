/*
 * ==========================================================================================
 * =                   JAHIA'S DUAL LICENSING - IMPORTANT INFORMATION                       =
 * ==========================================================================================
 *
 *                                 http://www.jahia.com
 *
 *     Copyright (C) 2002-2020 Jahia Solutions Group SA. All rights reserved.
 *
 *     THIS FILE IS AVAILABLE UNDER TWO DIFFERENT LICENSES:
 *     1/GPL OR 2/JSEL
 *
 *     1/ GPL
 *     ==================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE GPL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 *
 *     2/ JSEL - Commercial and Supported Versions of the program
 *     ===================================================================================
 *
 *     IF YOU DECIDE TO CHOOSE THE JSEL LICENSE, YOU MUST COMPLY WITH THE FOLLOWING TERMS:
 *
 *     Alternatively, commercial and supported versions of the program - also known as
 *     Enterprise Distributions - must be used in accordance with the terms and conditions
 *     contained in a separate written agreement between you and Jahia Solutions Group SA.
 *
 *     If you are unsure which license is appropriate for your use,
 *     please contact the sales department at sales@jahia.com.
 */
package org.jahia.modules.securityfilter.accesskey;

import org.apache.commons.lang.StringUtils;
import org.jahia.bin.filters.AbstractServletFilter;
import org.jahia.modules.securityfilter.accesskey.valves.AccessKeyAuthValve;
import org.jahia.modules.securityfilter.jwt.impl.TokenVerificationResult;
import org.jahia.services.content.JCRTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.jcr.RepositoryException;
import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public class AccessKeyFilter extends AbstractServletFilter {

    private static final Logger logger = LoggerFactory.getLogger(AccessKeyFilter.class);

    private AccessKeyConfig accessKeyConfig;
    private JCRTemplate jcrTemplate;

    public void setAccessKeyConfig(AccessKeyConfig accessKeyConfig) {
        this.accessKeyConfig = accessKeyConfig;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        //Do nothing for now
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        TokenVerificationResult tvr = AccessKeyAuthValve.getAccessKeyVerificationStatus();

        if (tvr == null) {
            filterChain.doFilter(httpRequest, servletResponse);
            return;
        }

        AccessKey accessKey = tvr.getAccessKey();

        // Make sure token is set so that PermissionsConfig recognizes request as access key request
        tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);

        System.out.println("Verified accessKey: " + accessKey.isVerified());

        if (accessKey.isVerified()) {
            String referer = httpRequest.getHeader("referer");
            String ip = httpRequest.getHeader("X-FORWARDED-FOR") != null ? httpRequest.getHeader("X-FORWARDED-FOR") : httpRequest.getRemoteAddr();

            List<String> referrers = accessKey.getReferrers();
            List<String> ips = accessKey.getIps();
            if (referrers != null && !referrers.isEmpty() && !checkReferer(referrers, referer)) {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                tvr.setMessage("Incorrect referer in token");
            } else if (ips != null && !ips.isEmpty() && !ips.contains(ip)) {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.REJECTED);
                tvr.setMessage("Your IP did not match any of the permitted IPs");
            } else {
                tvr.setVerificationStatusCode(TokenVerificationResult.VerificationStatus.VERIFIED);
                tvr.setMessage("Token verified");
            }
        }

        filterChain.doFilter(httpRequest, servletResponse);
        AccessKeyAuthValve.removeThreadLocal();
    }

    private boolean checkReferer(List<String> claimReferers, String referer) {
        if (referer == null) {
            return false;
        }

        for (String claimReferer : claimReferers) {
            if (referer.startsWith(claimReferer)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void destroy() {
        //Do nothing for now
    }

    public JCRTemplate getJcrTemplate() {
        return jcrTemplate;
    }

    public void setJcrTemplate(JCRTemplate jcrTemplate) {
        this.jcrTemplate = jcrTemplate;
    }
}
