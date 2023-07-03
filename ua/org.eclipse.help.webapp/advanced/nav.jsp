<%--
 Copyright (c) 2000, 2018 IBM Corporation and others.

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
	LayoutData data = new LayoutData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=ServletResources.getString("Help", request)%></title>

<style type="text/css">
<% 
if (!data.isMozilla()) {
%>
FRAMESET {
	border-top:1px solid ThreeDShadow;
	border-left:1px solid ThreeDShadow;
	border-right:1px solid ThreeDShadow;
}
<%
}
%>
</style>

<script type="text/javascript">
var isMozilla = navigator.userAgent.indexOf('Mozilla') != -1 && parseInt(navigator.appVersion.substring(0,1)) >= 5;
var isMozilla10 = isMozilla && navigator.userAgent.indexOf('rv:1') != -1;
var isIE = navigator.userAgent.indexOf('MSIE') != -1;

/**
 * Shows specified view. Called from actions that switch the view
 */
function showView(view)
{
	// Note: assumes the same id shared by tabs and iframes
	ViewsFrame.showView(view);
	TabsFrame.showTab(view);
}

/**
 * Shows the TOC frame, loads appropriate TOC, and selects the topic
 * isAutosynch means that we are synching as a result of following a hyperlink
 * and should not display errors or force the TOC view to show
 */
function displayTocFor(topic, isAutosynch)
{
    if (!ViewsFrame || !ViewsFrame.toc) {
        return;
    }
	var tocView = ViewsFrame.toc.tocViewFrame;
	if (!tocView) return;
	
	if (isAutosynch) {
	    if (!tocView.isAutosynchEnabled()) {
	        return;
	    }
    } else {
        showView("toc");
    }
	
	if (tocView.selectTopic) {
	    tocView.selectTopic(topic, isAutosynch);
	}
}

/*
 * Shows the TOC frame and collapses all TOCs.
 */
function collapseToc()
{
	/******** HARD CODED VIEW NAME *********/
	showView("toc");
	var tocView = ViewsFrame.toc.tocViewFrame;
	if (tocView.location.href.indexOf("?") > 0) {
		tocView.location = "tocView.jsp";
	}
}

</script>
</head>

<frameset id="navFrameset" rows="*,21"  framespacing="0" border="0"  frameborder="0" scrolling="no">
   <frame name="ViewsFrame" title="<%=ServletResources.getString("ignore", "ViewsFrame", request)%>" src='<%="views.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" resize=yes>
   <frame name="TabsFrame" title="<%=ServletResources.getString("TabsFrame", request)%>" src='<%="tabs.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" noresize>
</frameset>

</html>