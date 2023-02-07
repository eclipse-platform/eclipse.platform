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

<script type="text/javascript">
<%-- map of maximize listener functions indexed by name --%>
var maximizeListeners=new Object();
function registerMaximizeListener(name, listener){
	maximizeListeners[name]=listener;
}
function notifyMaximizeListeners(maximizedNotRestored){
	for(i in maximizeListeners){
		try{
			maximizeListeners[i](maximizedNotRestored);
		}catch(exc){}
	}
}
<%-- vars to keep track of frame sizes before max/restore --%>
var leftCols = "<%=isRTL?"70.5%":"29.5%"%>";
var rightCols = "<%=isRTL?"29.5%":"70.5%"%>";
<%--
param title "" for content frame
--%>
function toggleFrame(title)
{
	var frameset = document.getElementById("helpFrameset"); 
	var navFrameSize = frameset.getAttribute("cols");
	var comma = navFrameSize.indexOf(',');
	var left = navFrameSize.substring(0,comma);
	var right = navFrameSize.substring(comma+1);

	if (left == "*" || right == "*") {
		// restore frames
		frameset.frameSpacing="3";
		frameset.setAttribute("border", "6");
		frameset.setAttribute("cols", leftCols+","+rightCols);
		notifyMaximizeListeners(false);
	} else {
		// the "cols" attribute is not always accurate, especially after resizing.
		// offsetWidth is also not accurate, so we do a combination of both and 
		// should get a reasonable behavior
<%
if(isRTL) {
%>
		var leftSize = ContentFrame.document.body.offsetWidth;
		var rightSize = NavFrame.document.body.offsetWidth;
<%
} else {
%>
		var leftSize = NavFrame.document.body.offsetWidth;
		var rightSize = ContentFrame.document.body.offsetWidth;
<%
}
%>
		
		leftCols = leftSize * 100 / (leftSize + rightSize);
		rightCols = 100 - leftCols;

		// maximize the frame.
		//leftCols = left;
		//rightCols = right;
		// Assumption: the content toolbar does not have a default title.
<%
if(isRTL) {
%>
		if (title != "") // this is the right side for right-to-left rendering
			frameset.setAttribute("cols", "*,100%");
		else // this is the content toolbar
			frameset.setAttribute("cols", "100%,*");
<%
} else {
%>
		if (title != "") // this is the left side for left-to-right rendering
			frameset.setAttribute("cols", "100%,*");
		else // this is the content toolbar
			frameset.setAttribute("cols", "*,100%");
<%
}
%>	
		frameset.frameSpacing="0";
		frameset.setAttribute("border", "1");
		notifyMaximizeListeners(true);
	}
}
</script>
</head>

<frameset
<% 
if (data.isIE()) {
%> 
	style="border-top: 0px solid <%=prefs.getToolbarBackground()%>;"
	style="border-right: 4px solid <%=prefs.getToolbarBackground()%>;"
	style="border-bottom: 4px solid <%=prefs.getToolbarBackground()%>;"
	style="border-left: 4px solid <%=prefs.getToolbarBackground()%>;"
<%
}
%> 
    id="helpFrameset" cols="<%=isRTL?"70.5%,29.5%":"29.5%,70.5%"%>" framespacing="1" border="1"  frameborder="1"   scrolling="no">
<%
if (isRTL) {
%>
   	<frame name="ContentFrame" title="<%=ServletResources.getString("ignore", "ContentFrame", request)%>" class="content" src='<%="content.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" resize=yes>
   	<frame class="nav" name="NavFrame" title="<%=ServletResources.getString("ignore", "NavFrame", request)%>" src='<%="nav.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="1" resize=yes>
<%
} else {
%>
   	<frame class="nav" name="NavFrame" title="<%=ServletResources.getString("ignore", "NavFrame", request)%>" src='<%="nav.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="1" resize=yes>
   	<frame name="ContentFrame" title="<%=ServletResources.getString("ignore", "ContentFrame", request)%>" class="content" src='<%="content.jsp"+UrlUtil.htmlEncode(data.getQuery())%>' marginwidth="0" marginheight="0" scrolling="no" frameborder="0" resize=yes>
<%
}
%>
</frameset>

</html>

