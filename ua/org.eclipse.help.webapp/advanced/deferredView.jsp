<%--
 Copyright (c) 2006, 2018 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@ include file="header.jsp"%>

<% 
	RequestData data = new RequestData(application, request, response);
	WebappPreferences prefs = data.getPrefs();
	
	String baseURL = request.getRequestURL().toString();
	baseURL = baseURL.substring(0,baseURL.lastIndexOf("/")+1);
	
	String href = baseURL+request.getParameter("href");
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=ServletResources.getString("Loading", request)%></title>

<script type="text/javascript">
	function onloadHandler() { 
		location.href="<%=UrlUtil.JavaScriptEncode(href)%>";
	}
</script>

<style type="text/css">
	<%@ include file="deferredView.css"%>
</style> 
</head>

<body dir="<%=direction%>" onload="onloadHandler()">
	<%=ServletResources.getString("Loading", request)%>
</body>
</html>
