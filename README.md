# API Security service and filter

This module provides a service and a render filter which can be fed by different API providers to secure access. A centralized
configuration file provides rules for APIs associating an operation to a required permission.

## Permission configuration

API can be restricted based on permissions by adding `org.jahia.modules.api.permissions-*.cfg` files into `digital-factory-data/karaf/etc`.
These files contain a list of rules which will be checked for each node potentially returned by the API. Rules coming from all files are merged and sorted. Rules can be based on path, node type and/or workspace. 

A permission check is always done on a JCR node, and is associated with an API name. Different API names are provided for 
json views, the JCRest API module, or the GraphQL API.

The first rule that matches defines the permission that will be checked on the node to ensure the user is correctly allowed to use the API on it. 

A permission rule is defined by a list of entries of the following format :

```
permission.<rulename>.<propertyname>=<value>
```

A rule can define a `requiredPermission` property. This is the name of the permission that will be checked if the rule matches.
If no `requiredPermission` is set, no other rules will be tested, and no permission needs to be checked.

A rule can specify any number of matching criteria. Each of these criteria can have a single value or a comma separated list of values.
 - `api` : The names of the API, if the rule should only apply to some entry points
 - `pathPattern` : Regular expressions that will be tested on the node path.
 - `workspace` : `live` or `default`, only request on the specified workspace will match.
 - `nodeType` : Only request on nodes of these type will match.

An optional priority can be also specified - the rules with the lowest value will be executed first. Default priority is 0.

```
permission.checkFirst.priority=-10
```

A rule with no condition will always match (and thus, should have a very high value for priority so that it is checked last):
```
permission.global.requiredPermission=jcr:write_default
permission.global.priority=99999
```

Rules can specify a simple path pattern : 
```
permission.users.pathPattern=/sites/[^/]+/home/.*
```

Or a combination of all criterias :
```    
permission.digitallPostsInLive.pathPattern=/sites/digitall/.*
permission.digitallPostsInLive.nodeType=jnt:post,jnt:message
permission.digitallPostsInLive.workspace=live
permission.digitallPostsInLive.requiredPermission=jcr:write
```

## Render filter

A render filter catches all ajax calls to `*.json` and `*.html.ajax`. The filter calls the service to check if the request is allowed or not.
The API name contains the template type and the name of the view itself : `view.<template type>.<view name>`

So for example, the following rule will apply to all requests on the tree.json view :

```
permission.tree.api=view.json.tree
```

The following rule will match all json views on pages :

```
permission.tree.api=view.json
permission.tree.nodeType=jnt:page
```

## Module

A default configuration file is provided by this module. On DX 7.2.2+ , the configuration file will be copied automatically to `karaf/etc` 
once the module is started. An existing file won't be overwritten, and the configuration is never automatically removed.
On older versions of DX, the configuration file need to be deployed and maintained manually.
