<%--
 Copyright (c) 2000, 2010 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@ page import="org.eclipse.help.internal.webapp.data.*" contentType="text/html; charset=UTF-8"%>
<!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN">
<html lang="<%=ServletResources.getString("locale", request)%>">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title> Error </title>
</head>
<%
String  direction = "ltr";
try{
	if(UrlUtil.isRTL(request, response)){
		direction = "rtl";
	}
}catch(Exception e){
}
%>

<body dir="<%=direction%>">

	<%@ page isErrorPage="true" %>
	
	There was an error in your action:
	<p>
	<%= UrlUtil.htmlEncode(exception.toString()) %>
	</p>
	
</body>
</html>

