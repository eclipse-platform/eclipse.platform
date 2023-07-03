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
<%@ include file="header.jsp"%>

<% 
	LayoutData data = new LayoutData(application,request, response);
	FrameData frameData = new FrameData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=ServletResources.getString("Help", request)%></title>

<style type="text/css">
<% 
if (data.isMozilla()) {
%>
HTML {
	border-<%=isRTL?"left":"right"%>:1px solid ThreeDShadow;
}
<% 
} else {
%>
FRAMESET {
	border-top:1px solid ThreeDShadow;
	border-left:1px solid ThreeDShadow;
	border-right:1px solid ThreeDShadow;
	border-bottom:1px solid ThreeDShadow;
}
<%
}
%>
</style>

</head>
    <frameset id="contentFrameset" rows="<%=frameData.getContentAreaFrameSizes()%>" frameborder=0" framespacing="0" border="0" spacing="0">
	<frame name="ContentToolbarFrame" title="<%=ServletResources.getString("topicViewToolbar", request)%>" src='<%="contentToolbar.jsp"+UrlUtil.htmlEncode(data.getQuery())%>'  marginwidth="0" marginheight="0" scrolling="no" frameborder="0" >
	<frame ACCESSKEY="K" name="ContentViewFrame" title="<%=ServletResources.getString("topicView", request)%>" src='<%=UrlUtil.htmlEncode(data.getContentURL())%>'  marginwidth="10"<%=(data.isIE() && "6.0".compareTo(data.getIEVersion()) <=0)?"scrolling=\"yes\"":""%> marginheight="0" frameborder="0" >
	<%
	    AbstractFrame[] frames = frameData.getFrames(AbstractFrame.BELOW_CONTENT);
	    for (int f = 0; f < frames.length; f++) {
	        AbstractFrame frame = frames[f];
	        String url = frameData.getUrl(frame);
	%>
	<frame name="<%=frame.getName()%>" src="<%=url %>" <%=frame.getFrameAttributes()%> >
	<% 
	 } 
	%>
</frameset>

</html>

