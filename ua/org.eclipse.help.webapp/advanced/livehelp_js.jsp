<%--
 Copyright (c) 2000, 2021 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@ page import="org.eclipse.help.internal.webapp.data.*" errorPage="/advanced/err.jsp" contentType="text/html; charset=UTF-8"%>
<%
	request.setCharacterEncoding("UTF-8");
%>
<script type="text/javascript">
function liveActionInternal(topHelpWindow, pluginId, className, argument)
{
<%
	RequestData data = new RequestData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
	if(data.getMode() == RequestData.MODE_INFOCENTER){
%>
	alert("<%=UrlUtil.JavaScriptEncode(ServletResources.getString("noLiveHelpInInfocenter", request))%>");
	return;
<%
	}else if(!prefs.isActiveHelp()){
%>
	alert("<%=UrlUtil.JavaScriptEncode(ServletResources.getString("noLiveHelp", request))%>");
	return;
<%
	}else {
%>
	// construct the proper url for communicating with the server	
	var url= window.location.href;
	
	var i = url.indexOf("?");
	if(i>0)
		url=url.substring(0, i);
	
	i = url.indexOf("/topic/");
	if(i < 0)
		i = url.lastIndexOf("/");

	url=url.substring(0, i+1);
	var encodedArg=encodeURIComponent(argument);
	url=url+"livehelp/?pluginID="+pluginId+"&class="+className+"&arg="+encodedArg+"&nocaching="+Math.random();
	<%
	Object token = request.getSession().getAttribute("LSESSION"); //$NON-NLS-1$
	// Validate token to protect against XSS
	if (token instanceof String && ((String)token).matches("[a-z0-9-]{36}")) {//$NON-NLS-1$) {
	%>
	url=url+"&token=<%=token%>";
	<%
	}
	%>
	// we need to find the toolbar frame.
	// to do: cleanup this, including the location of the hidden livehelp frame.	
	var toolbarFrame = topHelpWindow.HelpFrame.ContentFrame.ContentToolbarFrame;
	if (!toolbarFrame){
		window.location=url;
		return;
	}

	if(toolbarFrame.liveHelpFrame){
		toolbarFrame.liveHelpFrame.location=url;
	}
<%
	}
%>
}
function showTopicInContentsInternal(topHelpWindow, topic) {
	try{
		topHelpWindow.HelpFrame.NavFrame.displayTocFor(topic, false);
	}catch(e){
	}
}

</script>
