package org.jahia.modules.securityfilter.accesskey;

import org.jahia.modules.securityfilter.impl.Permission;

import javax.jcr.RepositoryException;
import java.io.IOException;

public interface AccessKeyService {
    Permission getPermission(AccessKey accessKey);
    AccessKey createAccessKey(AccessKey accessKey) throws IOException, RepositoryException;
    AccessKey createRootAccessKey(AccessKey accessKey) throws IOException, RepositoryException;
    boolean verifyAccessKey(AccessKey accessKey) throws RepositoryException;
    boolean deactivateAccessKey(AccessKey accessKey) throws RepositoryException;
    boolean activateAccessKey(AccessKey accessKey) throws RepositoryException;
    boolean destroyAccessKey(AccessKey accessKey) throws RepositoryException;
}
