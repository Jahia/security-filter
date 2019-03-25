<%@ page language="java" contentType="text/html;charset=UTF-8" %>
<%@ taglib prefix="template" uri="http://www.jahia.org/tags/templateLib" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>
<%@ taglib prefix="jcr" uri="http://www.jahia.org/tags/jcr" %>
<%@ taglib prefix="fmt" uri="http://java.sun.com/jstl/fmt_rt" %>
<%@ taglib prefix="functions" uri="http://www.jahia.org/tags/functions" %>
<%--@elvariable id="currentNode" type="org.jahia.services.content.JCRNodeWrapper"--%>
<%--@elvariable id="out" type="java.io.PrintWriter"--%>
<%--@elvariable id="script" type="org.jahia.services.render.scripting.Script"--%>
<%--@elvariable id="scriptInfo" type="java.lang.String"--%>
<%--@elvariable id="workspace" type="java.lang.String"--%>
<%--@elvariable id="renderContext" type="org.jahia.services.render.RenderContext"--%>
<%--@elvariable id="currentResource" type="org.jahia.services.render.Resource"--%>
<%--@elvariable id="url" type="org.jahia.services.render.URLGenerator"--%>

<c:set var="moduleVersion" value="${script.view.moduleVersion}"/>
<c:set var="moduleVersion" value="1.0.2-SNAPSHOT"/>
<c:set var="URI" value="${pageContext.request.requestURI}" />

<c:set var="targetId" value="reactComponent${fn:replace(currentNode.identifier,'-','_')}"/>

<html>
<head>
    <script src="${pageContext.request.contextPath}/modules/security-filter-tools/javascript/apps/security-filter-jwt.js" ></script>
</head>
<body>

<div id="${targetId}">loading app ...</div>

<script type="text/javascript">
    var contextualData ={};
    contextualData['moduleVersion'] = '${moduleVersion}';
    contextualData['context'] = '${fn:substringBefore(URI, '/modules')}';

    window.reactRenderJWTApp('${targetId}', "${currentNode.identifier}", contextualData);
</script>

</body>
</html>