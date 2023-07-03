<%--
 Copyright (c) 2009 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@
page import="org.eclipse.help.internal.webapp.data.*"  contentType="text/html; charset=UTF-8"
%>
<%@page import="org.eclipse.help.webapp.*" %>
<% 
    request.setCharacterEncoding("UTF-8");
    boolean isRTL = UrlUtil.isRTL(request, response);
    String direction = isRTL?"rtl":"ltr";
	FrameData data = new FrameData(application, request, response);
	WebappPreferences prefs = data.getPrefs();
%>
<html>
<head>
<style type="text/css">
/* need this one for Mozilla */
HTML { 
	width:100%;
	height:100%;
	margin:0px;
	padding:0px;
	border:0px;
 }

BODY {
	background:<%=prefs.getToolbarBackground()%>;
	border:0px;
	height:100%;
}
TABLE {
	font: <%=prefs.getToolbarFont()%>;
	background:<%=prefs.getToolbarBackground()%>;
	margin: 0px;
	padding: 0px;
	height:100%;
}

</style>

</head>
<body dir="<%=direction %>">
<table align ="<%=isRTL?"left":"right"%>">
    <tr>
       <td>
           Hello, Guest!
       </td>
    </tr>
</table>
</body>
</html>