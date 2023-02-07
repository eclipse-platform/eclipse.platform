<%--
 Copyright (c) 2000, 2018 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%><%@
page import="org.eclipse.help.internal.webapp.data.*"  contentType="text/html; charset=UTF-8"
%>
<%@ page import="org.eclipse.help.webapp.*" %>
<% 
request.setCharacterEncoding("UTF-8");
boolean isRTL = UrlUtil.isRTL(request, response);
String  direction = isRTL?"rtl":"ltr";
%> 
 <!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01 Frameset//EN" "http://www.w3.org/TR/html4/frameset.dtd">
<!--
 ! Copyright (c) 2000, 2018 IBM Corporation and others.
 !
 ! This program and the accompanying materials 
 ! are made available under the terms of the Eclipse Public License 2.0
 ! which accompanies this distribution, and is available at
 ! https://www.eclipse.org/legal/epl-2.0/
 !
 ! SPDX-License-Identifier: EPL-2.0
 ! 
 ! Contributors:
 !     IBM Corporation - initial API and implementation
 -->