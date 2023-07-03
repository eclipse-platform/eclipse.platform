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
	ToolbarData data = new ToolbarData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
%>


<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">

<title><%=ServletResources.getString("Toolbar", request)%></title>
 
<style type="text/css">

/* need this one for Mozilla */
HTML { 
	margin:0px;
	padding:0px;
}
 
BODY {
	background:<%=prefs.getToolbarBackground()%>;
}

#titleText {
	font-weight:bold;
	color:WindowText;
}

a {
    cursor : default
}
 
.buttonOn a { 
	display:block;
	margin-left:2px;
	margin-right:2px;
	width:<%=data.isMozilla()?18:20%>px;
	height:<%=data.isMozilla()?18:20%>px;
	border:1px solid Highlight;
<% 
if (data.isIE()){
%>
	writing-mode:tb-rl; <%-- needed to verticaly center icon image on IE--%>
<%
}
%>	
    vertical-align:middle;
<% 
    String bg = prefs.getViewBackgroundStyle();
    if (bg.length() == 0) {
%>
	background: Window;
<%
    } else {
%>
   <%=bg%>
<%
}
%>
}

.button a, .buttonMenu a { 
	display:block;
	margin-left:2px;
	margin-right:2px;
	height:<%=data.isMozilla()?18:20%>px;
	border:1px solid <%=prefs.getToolbarBackground()%>;
<% 
if (data.isIE()){
%>
	writing-mode:tb-rl; <%-- needed to verticaly center icon image on IE--%>
<%
}
%>	
	vertical-align:middle;
}

.button a {
	width:<%=data.isMozilla()?18:20%>px;
}

.buttonMenu a {
	width:<%=data.isMozilla()?30:32%>px;
}

.buttonHidden a { 
	display:none;
}

.button a:hover, .buttonMenu a:hover { 
	border-top:1px solid ButtonHighlight; 
	border-<%=isRTL?"right":"left"%>:1px solid ButtonHighlight; 
	border-<%=isRTL?"left":"right"%>:1px solid ButtonShadow; 
	border-bottom:1px solid ButtonShadow;
}

<% if (data.isIE() || data.isMozilla() && "1.2.1".compareTo(data.getMozillaVersion()) <=0){
// maximize (last) button should not jump
%>

<%}%>

.separator {
	background-color: ThreeDShadow;
	height:100%;
	width: 1px;
	border-top:2px solid <%=prefs.getToolbarBackground()%>;
	border-bottom:2px solid <%=prefs.getToolbarBackground()%>;
	border-left:3px solid <%=prefs.getToolbarBackground()%>;
	border-right:3px solid <%=prefs.getToolbarBackground()%>;
	
}

#container {
	border-bottom:1px solid ThreeDShadow;
<%
if (data.isIE()) {
%> 
<%
}else if (data.isMozilla()){
%>
	border-top:1px solid ThreeDShadow;
	height:24px;
<%
}
%>
}

<%
// workaround for adding right border on mozilla (ugly..)
if (data.isMozilla() && "content".equals(request.getParameter("toolbar"))) { 
%>

/* need this one for Mozilla */
HTML { 
	margin:0px;
	padding:0px;
}
<%
}
%>

<%@ include file="darkTheme.css"%>
</style>

<%
    String[] scripts = data.getScriptFiles();
    for (int s = 0; s < scripts.length; s++) {
        String path = scripts[s];
%>
    <script type="text/javascript" src = "<%=path%>" ></script>
<%
    }
%>

<script type="text/javascript">

var bRestore = false;
// Preload images
<%
ToolbarButton[] buttons = data.getButtons();
for (int i=0; i<buttons.length; i++) {
	if (!buttons[i].isSeparator()) {
%>
	var <%=buttons[i].getName()%> = new Image();
	<%=UrlUtil.JavaScriptEncode(buttons[i].getName())%>.src = "<%=UrlUtil.JavaScriptEncode(buttons[i].getImage())%>";
<%
	}
}
%>

function setTitle(label)
{
	if( label == null) label = "";
	var title = document.getElementById("titleText");
	if (title == null) return;
	var text = title.lastChild;
	if (text == null) return;
	text.nodeValue = label;
}

<% if (data.isIE()
	|| data.isMozilla() && "1.2.1".compareTo(data.getMozillaVersion()) <=0
	|| (data.isSafari() && "120".compareTo(data.getSafariVersion()) <= 0) ){
%>
function registerMaximizedChangedListener(){
	// get to the frameset
	var p = parent;
	while (p && !p.registerMaximizeListener) {	   
	    if (p === p.parent)  {
	        return;
        }
		p = p.parent;
	}
	
	if (p!= null){
		p.registerMaximizeListener('<%=UrlUtil.JavaScriptEncode(data.getName())%>Toolbar', maximizedChanged);
	}
}
registerMaximizedChangedListener();

/**
 * Handler for double click: maximize/restore this view
 * Note: Mozilla browsers prior to 1.2.1 do not support programmatic frame resizing well.
 */
function mouseDblClickHandler(e) {
	// ignore double click on buttons
	var target=<%=data.isIE()?"window.event.srcElement":"e.target"%>;
	if (target.tagName && (target.tagName == "A" || target.tagName == "IMG"))
		return;
	toggleFrame();
	return false;
}		
function restore_maximize(button)
{
	toggleFrame();
	if (button && document.getElementById(button)){
		document.getElementById(button).blur();
	}
}
function toggleFrame(){
	// get to the frameset
	var p = parent;
	while (p && !p.toggleFrame)  {	   
	    if (p === p.parent)  {
	        return;
        }
		p = p.parent;
	}
	
	if (p!= null){
		p.toggleFrame('<%=UrlUtil.JavaScriptEncode(data.getTitle())%>');
	}
	document.selection.clear;	
}

function maximizedChanged(maximizedNotRestored){
	if(maximizedNotRestored){
		document.getElementById("maximize_restore").src="<%=data.getRestoreImage()%>";
		document.getElementById("maximize_restore").setAttribute("title", 
		    "<%=UrlUtil.JavaScriptEncode(data.getRestoreTooltip())%>");
		document.getElementById("maximize_restore").setAttribute("alt", 
		    "<%=UrlUtil.JavaScriptEncode(data.getRestoreTooltip())%>");
		bRestore = true;
	}else{
		document.getElementById("maximize_restore").src="<%=data.getMaximizeImage()%>";
		document.getElementById("maximize_restore").setAttribute("title", 
		    "<%=UrlUtil.JavaScriptEncode(data.getMaximizeTooltip())%>");
		document.getElementById("maximize_restore").setAttribute("alt", 
		    "<%=UrlUtil.JavaScriptEncode(data.getMaximizeTooltip())%>");
		bRestore = false;
	}
}

<%=( data.isIE() || data.isSafari() )?
	"document.ondblclick = mouseDblClickHandler;"
:
	"document.addEventListener('dblclick', mouseDblClickHandler, true);"%>
<%}%>

function setButtonState(buttonName, pressed) {
	if(!document.getElementById("tdb_"+buttonName))
		return;
	if (pressed == "hidden")
		document.getElementById("tdb_"+buttonName).className="buttonHidden";
	else if(pressed == true)
		document.getElementById("tdb_"+buttonName).className="buttonOn";
	else
		document.getElementById("tdb_"+buttonName).className="button";
}

function setWindowStatus(buttonName){
	<%
	for (int i=0; i<buttons.length; i++) {
		String name = buttons[i].getName();%>
		if (buttonName == "<%=UrlUtil.JavaScriptEncode(name)%>"){
			if (buttonName == "maximize_restore"){
				if (bRestore){
					window.status = "<%=UrlUtil.JavaScriptEncode(data.getRestoreTooltip())%>";
				}else{
					window.status = "<%=UrlUtil.JavaScriptEncode(data.getMaximizeTooltip())%>";
				}
			}else{
				window.status = "<%=UrlUtil.JavaScriptEncode(buttons[i].getTooltip())%>";
			}
		}
	<%	
	}
	%>
}

<%
if (data.hasMenu()) {
%>

function menu(button, param) {
	var doc = parent.frames[1].document;
	if (!doc.getElementById("menu")) {
		var menu = doc.createElement("div");
		menu.id = "menu";
		menu.srcButton = button;
		menu.onmouseout = menuExit;
		menu.onkeydown = menuKey;
		
		menu.style.padding = "2px 2px 2px 2px";
		menu.style.position = "absolute";
		menu.style.<%=isRTL ? "left" : "right"%> = "0px";
		menu.style.top = "0px";
		menu.style.background = "<%=prefs.getToolbarBackground()%>";
		menu.style.font = "<%=prefs.getToolbarFont()%>";
		menu.style.fontSize = ".875rem";
		menu.style.border<%=isRTL ? "Right" : "Left"%> = "1px solid ThreeDShadow";
		menu.style.borderBottom = "1px solid ThreeDShadow";

		var entries = param.split(",");
		for (var i=0;i<entries.length;++i) {
			var properties = entries[i].split("=");
			var anchor = doc.createElement("a");
			var text = doc.createTextNode(properties[0]);
			anchor.title = properties[0];
			anchor.appendChild(text);
			anchor.href = "javascript:parent.frames[0].closeMenu(),parent.frames[0]." + properties[1];
			anchor.target = "_self";
			anchor.onmouseover = itemEnter;
			anchor.onmouseout = itemExit;
			anchor.onfocus = itemEnter;
			anchor.onblur = itemExit;
			anchor.style.display = "block";
			anchor.style.cursor = "default";
			anchor.style.textDecoration = "none";
			anchor.style.padding = "4px 4px 4px 4px";
			anchor.style.background = "transparent";
			anchor.style.color = "WindowText";
			menu.appendChild(anchor);
		}

		doc.body.appendChild(menu);
		menu.focus();
	}

	if (button && document.getElementById(button)) {
		var buttonElem = document.getElementById(button);
		buttonElem.blur();
		buttonElem.firstChild.title = "";
	}
}

function menuKey(e) {
	var key;
	if (!e) var e = parent.frames[parent.frames.length - 1].window.event;
	if (e.keyCode) key = e.keyCode;
	else if (e.which) key = e.which;
    var src = e.srcElement ? e.srcElement : e.target;

  	if (key == 38) { // Up arrow
  		if (src.id != "menu" && src.previousSibling) {
  			src.previousSibling.focus();
  		}
  	}
  	else if (key == 40) { // Down arrow
  		if (src.id == "menu") {
  			src.firstChild.focus();
  		}
  		else if (src.nextSibling) {
  			src.nextSibling.focus();
  		}
  	}
  	else if (key == 27) { // Esc
  		closeMenu();
  	}
  	else {
  		return true;
  	}
  	return false;
}

function closeMenu() {
    parent.frames[parent.frames.length - 1].window.status = "";
	var menu = parent.frames[1].document.getElementById("menu");
	menu.parentNode.removeChild(menu);

	var img = document.getElementById(menu.srcButton).firstChild;
	img.title = img.alt;
}

function itemEnter(e) {
    this.style.background = "Highlight";
    this.style.color = "HighlightText";
    parent.frames[parent.frames.length - 1].window.status = this.firstChild.nodeValue;
    return true;
}

function itemExit(e) {
    this.style.background = "transparent";
    this.style.color = "WindowText";
    parent.frames[parent.frames.length - 1].window.status = "";
    return true;
}

function menuExit(e) {
	if (!e) var e = parent.frames[parent.frames.length - 1].window.event;
    var target = e.relatedTarget ? e.relatedTarget : e.toElement;
    while (target && target != this)
         target = target.parentNode;
    if (target == this) return;
    closeMenu();
}

<%
}
%>

</script>

<%
if (data.getScript() != null) {
%>
<script type="text/javascript" src="<%=UrlUtil.htmlEncode(data.getScript())%>"></script>
<%
}
%>

</head>
 
<%
if(buttons.length > 0){
%>
	<body dir="<%=direction%>">
<%
}else{
%>
	<body dir="<%=direction%>" tabIndex="-1">
<%
}
%>

<table id="container" width="100%" border="0" cellspacing="0" cellpadding="0" height="100%" style='padding-<%=isRTL?"right":"left"%>:<%=data.isIE()?"5px":"8px"%>;'>

	<tr>
		<td nowrap style="font: <%=prefs.getToolbarFont()%>; font-size: .875rem" valign="middle">
			<div id="titleTextTableDiv" style="overflow:hidden; height:22px;"><table><tr><td nowrap style="font:<%=prefs.getToolbarFont()%>;font-size:1em"><div id="titleText" >&nbsp;<%=UrlUtil.htmlEncode(data.getTitle())%></div></td></tr></table>
			</div>
		
		
		<div style="position:absolute; top:1px; <%=isRTL?"left":"right"%>:0px;">
		<table width="100%" border="0" cellspacing="1" cellpadding="0" height="100%">
			<tr>
				<td align="<%=isRTL?"left":"right"%>">
					<table align="<%=isRTL?"left":"right"%>" border="0" cellspacing="0" cellpadding="0" height="100%">
					<tr>
<%
	for (int i=0; i<buttons.length; i++) {
		if (buttons[i].isSeparator()) {
%>
						<td align="middle" class="separator" valign="middle">
						</td>
<%
		} else {
%>
						<td align="middle" id="tdb_<%=UrlUtil.htmlEncode(buttons[i].getName())%>" class="<%=UrlUtil.htmlEncode(buttons[i].getStyleClass())%>" height=18>
							<a href="javascript:<%=UrlUtil.htmlEncode(buttons[i].getAction())%>('b<%=i%>', '<%=UrlUtil.htmlEncode(buttons[i].getParam())%>');" 
							   onmouseover="javascript:setWindowStatus('<%=UrlUtil.htmlEncode(buttons[i].getName())%>');return true;" 
							   onmouseout="window.status='';"
							   id="b<%=i%>">
							   <img src="<%=UrlUtil.htmlEncode(buttons[i].getImage())%>" 
									alt='<%=UrlUtil.htmlEncode(buttons[i].getTooltip())%>' 
									title='<%=UrlUtil.htmlEncode(buttons[i].getTooltip())%>'
									border="0"
									id="<%=UrlUtil.htmlEncode(buttons[i].getName())%>">
							</a>
						</td>
<%
		}
	}
%>				
					</tr>
					</table>
				</td>
			</tr>
		</table> 
		</div>
		</td>
	</tr>
</table>

<%// special case for content toolbar - internally used live help frame
if ("content".equals(request.getParameter("toolbar"))) {
%>
    <iframe name="liveHelpFrame" title="<%=ServletResources.getString("ignore", "liveHelpFrame", request)%>" src="blank.html" style="visibility:hidden" tabindex="-1" frameborder="no" width="0" height="0" scrolling="no">
    </iframe>
<%
}
%>

</body>     
</html>

