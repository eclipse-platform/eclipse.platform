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
	AbstractView[] views = data.getViews();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title><%=ServletResources.getString("Tabs", request)%></title>
    
<style type="text/css">


BODY {
	margin:0px;
	padding:0px;
	height:100%;
<% 
if (data.isMozilla()){
%>
	height:21px;
<%
}
%>
}

a {
    cursor : default
}

/* tabs at the bottom */
.tab {
	font-size:5px;<%-- needed to verticaly center icon image --%>
	margin:0px;
	padding:0px;
	border-top:1px solid ThreeDShadow;
	border-bottom:1px solid <%=data.isMozilla()?prefs.getToolbarBackground():"ThreeDShadow"%>;
	cursor:default;
	background:<%=prefs.getToolbarBackground()%>;
}

.pressed {
	font-size:5px;<%-- needed to verticaly center icon image (on IE--%>
	margin:0px;
	padding:0px;
	cursor:default;
	<%=prefs.getViewBackgroundStyle()%>
	border-top:0px solid <%=prefs.getToolbarBackground()%>;
	border-bottom:1px solid ThreeDShadow;
}

.separator {
	height:100%;
	background-color:ThreeDShadow;
	border-bottom:1px solid <%=prefs.getToolbarBackground()%>;
}

.separator_pressed {
	height:100%;
	background-color:ThreeDShadow;
	border-top:0px solid <%=prefs.getToolbarBackground()%>;
	border-bottom:1px solid <%=prefs.getToolbarBackground()%>;
}

A {
	text-decoration:none;
	vertical-align:middle;
	height:16px;
	width:16px;
<% 
if (data.isIE()){
%>
	writing-mode:tb-rl; <%-- needed to verticaly center icon image on IE--%>
<%
} else {
%>
	display:block;<%-- needed to verticaly center icon image (remove extra pixels below) on Mozilla--%>
<%
}
%>
}

IMG {
	border:0px;
	margin:0px;
	padding:0px;
	height:16px;
	width:16px;
}

<%@ include file="darkTheme.css"%>
</style>
 
<script type="text/javascript">

var isMozilla = navigator.userAgent.indexOf('Mozilla') != -1 && parseInt(navigator.appVersion.substring(0,1)) >= 5;
var isIE = navigator.userAgent.indexOf('MSIE') != -1;
var linksArray = new Array ("linktoc", "linkindex", "linksearch", "linkbookmarks");

if (isIE){
  document.onkeydown = keyDownHandler;
} else {
  document.addEventListener('keydown', keyDownHandler, true);
}

/**
 * Returns the target node of an event
 */
function getTarget(e) {
	var target;
  	if (isIE)
   		target = window.event.srcElement;
  	else
  		target = e.target;

	return target;
}

<%
for (int i=0; i<views.length; i++) {
    if (views[i].isVisible()) {
%>
	var <%=views[i].getName()%> = new Image();
	<%=views[i].getName()%>.src = "<%=data.getImageURL(views[i])%>";
<%
    }
}
%>

var lastTab = "";
/* 
 * Switch tabs.
 */ 
function showTab(tab)
{ 	
	if (tab == lastTab) 
		return;
	
	lastTab = tab;
	
 	// show the appropriate pressed tab
  	var buttons = document.body.getElementsByTagName("TD");
  	for (var i=0; i<buttons.length; i++)
  	{
  		if (buttons[i].id == tab) { 
			buttons[i].className = "pressed";
			if (i > 0) 
				buttons[i-1].className = "separator_pressed";
			if (i<buttons.length-1) 
				buttons[i+1].className = "separator_pressed";
		} else if (buttons[i].className == "pressed") {
			buttons[i].className = "tab";
			if (i > 0) 
				if (i > 1 && buttons[i-2].id == tab) 
					buttons[i-1].className = "separator_pressed";
				else
					buttons[i-1].className = "separator";
			if (i<buttons.length-1) 
				if (i<buttons.length-2 && buttons[i+2].id == tab) 
					buttons[i+1].className = "separator_pressed";
				else
					buttons[i+1].className = "separator";
		}
 	 }
}

/**
 * Handler for key down (arrows)
 */
function keyDownHandler(e)
{
	var key;

	if (isIE) {
		key = window.event.keyCode;
	} else {
		key = e.keyCode;
	}
		
	if (key <37 || key > 39) 
		return true;
	
  	var clickedNode = getTarget(e);
  	if (!clickedNode) return true;

	var linkId="";
	if (clickedNode.tagName == 'A')
		linkId=clickedNode.id;
	else if(clickedNode.tagName == 'TD')
		linkId="link"+clickedNode.id;

  	if (isIE)
  		window.event.cancelBubble = true;
  	else
  		e.cancelBubble = true;
  	if (key == 38 ) { // up arrow
		if(linkId.length>4){
			parent.showView(linkId.substring(4, linkId.length));
			clickedNode.blur();
			parent.frames.ViewsFrame.focus();
		}
  	} else if (key == 39) { // Right arrow, expand
  		var nextLink=getNextLink(linkId);
		if(nextLink!=null){
			document.getElementById(nextLink).focus();
		}
  	} else if (key == 37) { // Left arrow,collapse
   		var previousLink=getPreviousLink(linkId);
		if(previousLink!=null){
			document.getElementById(previousLink).focus();
		}
 	}
  	 			
  	return false;
}

function getNextLink(currentLink){
	for(i=0; i<linksArray.length; i++){
		if(currentLink==linksArray[i]){
			if((i+1)<linksArray.length)
				return linksArray[i+1];
		}
	}
	return linksArray[0];
}

function getPreviousLink(currentLink){
	for(i=0; i<linksArray.length; i++){
		if(currentLink==linksArray[i]){
			if(i>0)
				 return linksArray[i-1];
		}
	}
	return linksArray[linksArray.length-1];
}

</script>

</head>
   
<body dir="<%=direction%>" onload="showTab('<%=data.getVisibleView()%>')">

  <table cellspacing="0" cellpadding="0" border="0" width="100%" height="100%" valign="middle">
   <tr>

<%
	for (int i=0; i<views.length; i++) 
	{
	    if (views[i].isVisible()) {
		    String title = data.getTitle(views[i]);
		    if (i != 0) {
%>
	<td width="1px" class="separator"><div style="width:1px;height:1px;display:block;"></div></td>
	<%-- div inside separator cell fixes top separator pixel that was not white on IE, or first separator not displayed when frame width happens to be even number of pixels --%>
<%
		    }
%>
	<td  title="<%=UrlUtil.htmlEncode(title)%>" 
	     align="center"  
	     valign="middle"
	     class="tab" 
	     id="<%=views[i].getName()%>" 
	     onclick="parent.showView('<%=views[i].getName()%>')" 
	     onmouseover="window.status='<%=UrlUtil.JavaScriptEncode(title)%>';return true;" 
	     onmouseout="window.status='';">
	     <a  href='javascript:parent.showView("<%=views[i].getName()%>");' 
	         onclick='this.blur();return false;' 
	         onmouseover="window.status='<%=UrlUtil.JavaScriptEncode(title)%>';return true;" 
	         onmouseout="window.status='';"
	         id="link<%=views[i].getName()%>"
	         <%=views[i].getKey()==View.NO_SHORTCUT?"":"ACCESSKEY=\""+views[i].getKey()+"\""%>>
	         <img alt="<%=UrlUtil.htmlEncode(title)%>" 
	              title="<%=UrlUtil.htmlEncode(title)%>" 
	              src="<%=data.getImageURL(views[i])%>"
	              id="img<%=views[i].getName()%>"
	              height="16"
	         >
	     </a>
	</td>
<%
        }
	}
%>
 
   </tr>
   </table>

</body>
</html>

