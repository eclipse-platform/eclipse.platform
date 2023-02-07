<%--
 Copyright (c) 2000, 2018 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
     Sybase, Inc. - Workaround for enableOK slowdown, Bug 289161
     
--%>
<%@ include file="header.jsp"%>

<% 
	WorkingSetData data = new WorkingSetData(application, request, response);
	TocData tocData = new TocData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
	boolean isCriteriaEnable = data.isCriteriaScopeEnabled();
%>


<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<title><%=ServletResources.getString(data.isEditMode()?"EditWorkingSet":"NewWorkingSet", request)%></title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<style type="text/css">

/* need this one for Mozilla */
HTML, BODY {
	width:100%;
	height:100%;
	margin:0px;
	padding:0px;
	border:0px;
}
 
BODY {
	font: <%=prefs.getViewFont()%>;
	font-size: .875rem;
	background:<%=prefs.getToolbarBackground()%>;
	color: WindowText;
}

TABLE {
	font:<%=prefs.getViewFont()%>;
	font-size:.875rem;
	background:<%=prefs.getToolbarBackground()%>;
}

TD, TR {
	margin:0px;
	padding:0px;
	border:0px;
}

INPUT {
    font:<%=prefs.getViewFont()%>;
    font-size:.875rem;
}

#workingSet {
	width:100%;
	font-size:1.0em;
}

#booksContainer {
    background:Window;
	color:WindowText;
	border:	2px inset ThreeDHighlight;
	margin:10px;
	margin-top:2px;
	margin-bottom: 0px;
	padding-<%=isRTL?"right":"left"%>:5px;
	overflow:auto;
	height:350px;
<%
if (data.isIE()) {
%>
    width:100%; 
<%
}
%>
}

#criteriaContainer {
    background:Window;
	color:WindowText;
	border:	2px inset ThreeDHighlight;
	margin-<%=isRTL?"left":"right"%>:10px;
	margin-top:2px;
	margin-bottom: 0px;
	padding-<%=isRTL?"right":"left"%>:5px;
	overflow:auto;
	height:350px;
<%
if (data.isIE()) {
%>
    width:100%; 
<%
}
%>
}

#buttonBar {
    height:3em; 
<%
if (data.isMozilla()) {
%>
    padding-bottom:5px;
<%
}
%>
}

.book, .criterion {
	margin:0px;
	border:0px;
	padding:0px;
	white-space: nowrap;
}

.topic, .criterionValue {
	margin-<%=isRTL?"right":"left"%>:30px;
	border:0px;
	padding:0px;
	white-space: nowrap;
}

BUTTON {
	font:<%=prefs.getViewFont()%>;
	font-size:.875rem;
}

.expanded {
	display:block;
}

.collapsed {
	display:none;
}

.grayed {
	background-color: <%=prefs.getToolbarBackground()%>;
}

<%
if (data.isMozilla()) {
%>
input[type="checkbox"] {
	border:2px solid WindowText; 
	margin:0px; 
	padding:0px;	
}

.grayed {
	background: <%=prefs.getToolbarBackground()%>;
}
<%
}
%>
</style>
<script type="text/javascript" src="resize.js"></script>
<script type="text/javascript">

// Preload images
var minus = new Image();
minus.src = "<%=prefs.getImagesDirectory()%>"+"/minus.svg";
var plus = new Image();
plus.src = "<%=prefs.getImagesDirectory()%>"+"/plus.svg";

var oldName = '<%=data.isEditMode()?UrlUtil.JavaScriptEncode(data.getWorkingSetName()):""%>';
var altBookClosed = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("bookClosed", request))%>";
var altBookOpen = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("bookOpen", request))%>";
var altCriterionClosed = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("criterionClosed", request))%>";
var altCriterionOpen = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("criterionOpen", request))%>";
var noTopicsSelected = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("NoTopicsSelected", request))%>";
var noNameEntered = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("NoNameEntered", request))%>";
        
var alwaysEnableOK = true;  // See Bug 289161

function onloadHandler() {
<%if(!data.isMozilla() || "1.3".compareTo(data.getMozillaVersion()) <=0){
// buttons are not resized immediately on mozilla before 1.3
%>
	sizeButtons();
<%}%>
    sizeContainer();
	document.getElementById("workingSet").focus();
	enableOK();
<%-- event handlers that call enableOK() are not invoked properly on Japanese --%>
	//setInterval("enableOK()", 250);

}

function sizeButtons() {
	var minWidth=60;

	if(document.getElementById("ok").offsetWidth < minWidth){
		document.getElementById("ok").style.width = minWidth+"px";
	}
	if(document.getElementById("cancel").offsetWidth < minWidth){
		document.getElementById("cancel").style.width = minWidth+"px";
	}
}

function sizeContainer() {
    resizeVertical("booksContainer", "wsTable", "buttonBar", 100, 30);
    <%if(isCriteriaEnable){%>
    resizeVertical("criteriaContainer", "wsTable", "buttonBar", 100, 30);
    <%}%>
}

function doSubmit()
{
	try
	{
		var workingSet = document.getElementById("workingSet").value;
		if (!workingSet || workingSet.length == 0 || workingSet.charAt(0) == " ")  {
         		alert(noNameEntered);
 			return false;
        }
        
        if (!hasContentSelections()) {
            alert(noTopicsSelected);
            return false;
        }
	
		var hrefs = getSelectedContentResources();
		if (!hrefs || hrefs == "")
			return false;

		var criteria = getSelectedCriteriaResources();
		var query = "operation="+'<%=UrlUtil.JavaScriptEncode(data.getOperation())%>'+"&workingSet="+encodeURIComponent(workingSet)+ hrefs+criteria+"&oldName="+encodeURIComponent(oldName);
		window.opener.location.replace("../workingSetState.jsp?"+query);
	    window.opener.focus();
		window.close();
	} catch(ex) {alert("Error..." + ex.message)}
}

function getSelectedContentResources() {
	var hrefs = "";
	var inputs = document.getElementsByTagName("INPUT");
	for (var i=0; i<inputs.length; i++)
	{
		if (inputs[i].type != "checkbox") continue;
		if (inputs[i].parentNode.id.indexOf("_criterion") > -1) continue;
		if (inputs[i].checked == false) continue;
		if (getGrayed(inputs[i])) continue;
		if (isToc(inputs[i].name)) {
			hrefs += "&hrefs="+encodeURIComponent(inputs[i].name);
		} else if (!isParentTocSelected(inputs[i].name)) {
			hrefs += "&hrefs="+encodeURIComponent(inputs[i].name);
		}
	}
	return hrefs;
}

//Assumption: last character of a toc reference cannot be underscore _
function isToc(name) {
	return name.charAt(name.length-1) != "_";
}

function isParentTocSelected(name) {
	return isParentCheckboxSelected(name);
}

function getSelectedCriteriaResources() {
	var criteria = "";
	var inputs = document.getElementsByTagName("INPUT");
	for (var i=0; i<inputs.length; i++)
	{
		if (inputs[i].type != "checkbox") continue;
		if (inputs[i].parentNode.id.indexOf("_criterion") < 0) continue;
		if (inputs[i].checked == false) continue;
		if (getGrayed(inputs[i])) continue;
		if (isCriterionCategory(inputs[i].name)) {
			criteria += "&criteria=" + encodeURIComponent(inputs[i].name);
		} else if(!isParentTocSelected(inputs[i].name)){
            criteria += "&criteria=" + encodeURIComponent(inputs[i].name);
		}
	}
	return criteria;
}

function isCriterionCategory(name) {
	return name.charAt(name.length-1) != "_";
}

function isParentCategorySelected(name){
	return isParentCheckboxSelected(name);
}

function isParentCheckboxSelected(name) {
	var parentCheckbox = getParentCheckbox(name);
	return (parentCheckbox.checked && !getGrayed(parentCheckbox));
}

function getParentCheckbox(name) {
	var parentId = name.substring(0, name.lastIndexOf("_", name.length-2));
	return document.getElementById(parentId);
}

function isCriterionNode(nodeId){
	if(nodeId && nodeId.indexOf("_criterion") > -1){
		return true; 
	}
	return false;
}

function collapseOrExpand(nodeId) {
	var node = document.getElementById("div"+nodeId);
	var img = document.getElementById("img"+nodeId);
	if (!node || !img) return;
	if (node.className == "expanded") {
		node.className = "collapsed";
		img.src = plus.src;
		if(isCriterionNode(nodeId)){
			img.alt = altCritrionClosed;
			img.title = altCriterionClosed;
		}else{
			img.alt = altBookClosed;
			img.title = altBookClosed;
		}
	} else {
		node.className = "expanded";
		img.src = minus.src;
		if(isCriterionNode(nodeId)){
			img.alt = altCriterionOpen;
			img.title = altCriterionOpen;
		}else{
			img.alt = altBookOpen;
			img.title = altBookOpen;
		}
	}
}

function collapse(nodeId) {
	var node = document.getElementById("div"+nodeId);
	var img = document.getElementById("img"+nodeId);
	if (!node || !img) return;
	node.className = "collapsed";
	img.src = plus.src;
	if(isCriterionNode(nodeId)){
		img.alt = altCritrionClosed;
		img.title = altCriterionClosed;
	}else{
		img.alt = altBookClosed;
		img.title = altBookClosed;
	}
}

function expand(nodeId) {
	var node = document.getElementById("div"+nodeId);
	var img = document.getElementById("img"+nodeId);
	if (!node || !img) return;
	node.className = "expanded";
	img.src = minus.src;
	if(isCriterionNode(nodeId)){
		img.alt = altCriterionOpen;
		img.title = altCriterionOpen;
	}else{
		img.alt = altBookOpen;
		img.title = altBookOpen;
	}
}

function getParent(child) {
	var id = child.name;
	var parentId = id.substring(0, id.lastIndexOf("_", id.length-2));
	return document.getElementById(parentId);
}

function updateParentState(checkbox,parentDiv) {
	enableOK();
	if (checkbox == null)
		return;

	var baseChildState = checkbox.checked;
	var parent = getParent(checkbox);
	if (parent == null)
		return;

	var allSameState = true;
	var children = document.getElementById(parentDiv).getElementsByTagName("INPUT");
	for (var i = children.length - 1; i >= 0; i--) {
		if (children[i].checked != baseChildState ) {
			allSameState = false;
			break;
		}
	}

	setGrayed(parent, !allSameState);
	parent.checked = !allSameState || baseChildState;
}

function setSubtreeChecked(checkbox, parentDiv) {
	enableOK();
	var state = checkbox.checked;
	var children = document.getElementById(parentDiv).getElementsByTagName("INPUT");
	for (var i = children.length - 1; i >= 0; i--) {
		var element = children[i];
		if (state) {
			element.checked = true;
		} else {
			element.checked = false;
		}
	}
	setGrayed(checkbox, false);
}

function setGrayed(node, enableGray) {
	if (enableGray)
		node.className = "grayed";
	else
		node.className = "checkbox";
}

function getGrayed(node) {
	return node.className == "grayed";
}

function isExpanded(nodeId) {
	var node = document.getElementById("div"+nodeId);
	if (node == null) return false;
	return node.className == "expanded";
}

function isCollapsed(nodeId) {
	var node = document.getElementById("div"+nodeId);
	if (node == null) return false;
	return node.className == "collapsed";
}

/**
 * Handler for key down (arrows)
 */
function keyDownHandler(folderId, key, target)
{
	if (key != 37 && key != 39) 
		return true;

 	if (key == 39) { // Right arrow, expand
		if (isCollapsed(folderId))
			expand(folderId);
		target.focus();
  	} else if (key == 37) { // Left arrow,collapse
		if (isExpanded(folderId))
			collapse(folderId);
		var parentCheckbox = getParentCheckbox(target.name);
		if (parentCheckbox != null)
			parentCheckbox.focus();	
		else
			target.focus();
  	} 
  			
  	return false;
}

function hasContentSelections() {
		var hrefs = getSelectedContentResources();
		if (!hrefs || hrefs == "")
			return false;
		else
			return true;
}

function enableOK() {
    if (alwaysEnableOK) {
        document.getElementById("ok").disabled = false;
        return;
    }
	var value = document.getElementById("workingSet").value;
	if (!value || value.length == 0 || value.charAt(0) == " " || !hasContentSelections())
		document.getElementById("ok").disabled = true;
	else
		document.getElementById("ok").disabled = false;
}

</script>

</head>

<body dir="<%=direction%>" onload="onloadHandler()"  onresize = "sizeContainer()">
<form onsubmit="doSubmit();return false;">
	<table id="wsTable" width="100%" cellspacing=0 cellpading=0 border=0 align=center >
		<tr><td style="padding:5px 10px 0px 10px;"><label for="workingSet" accesskey="<%=ServletResources.getAccessKey("WorkingSetName", request)%>"><%=ServletResources.getLabel("WorkingSetName", request)%></label>
		</td></tr>
		<tr><td style="padding:0px 10px;"><input type="text" id="workingSet" name="workingSet" 
		    value='<%=data.isEditMode()?UrlUtil.htmlEncode(data.getWorkingSetName()):data.getDefaultName()%>' maxlength=256 alt="<%=ServletResources.getString("WorkingSetName", request)%>" title="<%=ServletResources.getString("WorkingSetName", request)%>" onkeyup="enableOK();return true;">
        </td></tr>
    </table>
    
    <table width="100%" cellspacing=0 cellpading=0 border=0 align=center style="table-layout:fixed;">
        <tr>
            <td>
               <div id="selectBook" style="padding-top:5px; margin-<%=isRTL?"right":"left"%>:10px;"><%=ServletResources.getString("WorkingSetContent", request)%>:</div>
		    </td>
		    <%if(isCriteriaEnable){ %>
		    <td width="50%">
               <div id="selectCriteria" style="padding-top:5px;"><%=ServletResources.getString("Criteria", request)%>:</div>
		    </td>
		    <% }%>
		</tr>
		<tr>
		    <td>
		       <div id="booksContainer">
		       <% for (int i=0; i<data.getTocCount(); i++){
	                 if(!data.isTocEnabled(i)){
		             // do not show
		                 continue;
	                  }
	                 String label = data.getTocLabel(i);
	                 short state = data.getTocState(i);
	                 String checked = state == WorkingSetData.STATE_CHECKED || state == WorkingSetData.STATE_GRAYED ? "checked" : "";
	                 String className = state == WorkingSetData.STATE_GRAYED ? "grayed" : "checkbox";
               %>
				    <div class="book" id='<%="id"+i%>' >
					   <img id='<%="img"+i%>' alt="<%=ServletResources.getString("bookClosed", request)%>" title="<%=ServletResources.getString("bookClosed", request)%>" src="<%=prefs.getImagesDirectory()%>/plus.svg" onclick="collapseOrExpand('<%=i%>')">
					   <input 	class='<%=className%>' 
							    type="checkbox" 
							    id='<%=UrlUtil.htmlEncode(data.getTocHref(i))%>' 
							    name='<%=UrlUtil.htmlEncode(data.getTocHref(i))%>' 
							    alt="<%=UrlUtil.htmlEncode(label)%>" <%=checked%> 
						  	    onkeydown="keyDownHandler(<%=i%>, event.keyCode, this)"
							    onclick="setSubtreeChecked(this, '<%="div"+i%>')">
							    <label for="<%=UrlUtil.htmlEncode(data.getTocHref(i))%>"><%=UrlUtil.htmlEncode(label)%></label>
					   <div id='<%="div"+i%>' class="collapsed">
                         <%
	                         for (int topic=0; topic<data.getTopicCount(i); topic++)
	                         {
	                            if(!data.isTopicEnabled(i, topic)){
		                           // do not show
		                           continue;
	                            }
		                        String topicLabel = data.getTopicLabel(i, topic);
		                        String topicChecked = (state == WorkingSetData.STATE_CHECKED) || 
							                          (state == WorkingSetData.STATE_GRAYED && data.getTopicState(i,topic) == WorkingSetData.STATE_CHECKED) 
							                          ? "checked" : "";
                         %>
						    <div class="topic" id='<%="id"+i+"_"+topic%>'>
							    <input 	class="checkbox" 
									    type="checkbox" 
									    id='<%=UrlUtil.htmlEncode(data.getTocHref(i))+"_"+topic+"_"%>' 
									    name='<%=UrlUtil.htmlEncode(data.getTocHref(i))+"_"+topic+"_"%>' 
									    alt="<%=UrlUtil.htmlEncode(topicLabel)%>" <%=topicChecked%> 
									    onkeydown="keyDownHandler(<%=i%>, event.keyCode, this)"
									    onclick="updateParentState(this, '<%="div"+i%>')">
									    <label for="<%=UrlUtil.htmlEncode(data.getTocHref(i))+"_"+topic+"_"%>"><%=UrlUtil.htmlEncode(topicLabel)%></label>
						    </div>
                         <%
	                         }
                         %>
					   </div>
				    </div>
               <%
                   }		
               %>
                </div>
		    </td>
		    <%if(isCriteriaEnable){ %>
		    <td width="50%">
               <div id="criteriaContainer">
                   <% 
                     String[] category = data.getCriterionIds();
                     for (int i=0; i < category.length; i++){
                    	 String criterionId = category[i];
	                     if(null == criterionId || 0 == criterionId.trim().length()){
		                  // do not show
		                     continue;
	                     }
	                     
	                     short categoryState = data.getCriterionCategoryState(i);	            
	                     String categoryChecked = categoryState == WorkingSetData.STATE_CHECKED || categoryState == WorkingSetData.STATE_GRAYED ? "checked" : "";
		                 String inputClassName = categoryState == WorkingSetData.STATE_GRAYED ? "grayed" : "checkbox";
		                 String criterionDisplayName = data.getCriterionDisplayName(criterionId);
                   %>
				    <div class="criterion" id='<%="id_criterion"+i%>' >
					   <img id='<%="img_criterion"+i%>' alt="<%=ServletResources.getString("criterionClosed", request)%>" title="<%=ServletResources.getString("criterionClosed", request)%>" src="<%=prefs.getImagesDirectory()%>/plus.svg" onclick="collapseOrExpand('_criterion'+'<%=i%>')">
					   <input 	class='<%=inputClassName%>' 
							    type="checkbox" 
							    id='<%=UrlUtil.htmlEncode(criterionId)%>' 
							    name='<%=UrlUtil.htmlEncode(criterionId)%>' 
							    alt="<%=UrlUtil.htmlEncode(criterionDisplayName)%>" <%=categoryChecked%> 
						  	    onkeydown="keyDownHandler('_criterion'+<%=i%>, event.keyCode, this)"
							    onclick="setSubtreeChecked(this, '<%="div_criterion"+i%>')">
							    <label for="<%=UrlUtil.htmlEncode(criterionId)%>"><%=UrlUtil.htmlEncode(criterionDisplayName)%></label>
					   <div id='<%="div_criterion"+i%>' class="collapsed">
                         <%
                             String[] criterionValueIds = data.getCriterionValueIds(criterionId);
	                         for (int j=0; j<criterionValueIds.length; j++)
	                         {
		                        String criterionValue = criterionValueIds[j];
		                        String valueChecked = (categoryState == WorkingSetData.STATE_CHECKED) || 
		                          (categoryState == WorkingSetData.STATE_GRAYED && data.getCriterionValueState(i,j) == WorkingSetData.STATE_CHECKED) 
		                          ? "checked" : "";
		                        String criterionValueDisplayName ="";
		                        if(criterionValue.equalsIgnoreCase("Uncategorized")){
		                        	criterionValueDisplayName = ServletResources.getString("Uncategorized", request);
		                        } else {
		                        	criterionValueDisplayName = data.getCriterionValueDisplayName(criterionId, criterionValue);
		                        }
                         %>
						    <div class="criterionValue" id='<%="id_criterion"+i+"_"+j%>'>
							    <input 	class="checkbox" 
									    type="checkbox" 
									    id='<%=UrlUtil.htmlEncode(criterionId)+"_"+j+"_"%>' 
									    name='<%=UrlUtil.htmlEncode(criterionId)+"_"+j+"_"%>' 
									    alt="<%=UrlUtil.htmlEncode(criterionValueDisplayName)%>" <%=valueChecked%> 
									    onkeydown="keyDownHandler('_criterion'+<%=i%>, event.keyCode, this)"
									    onclick="updateParentState(this, '<%="div_criterion"+i%>')">
									    <label for="<%=UrlUtil.htmlEncode(criterionId)+"_"+j+"_"%>"><%=UrlUtil.htmlEncode(criterionValueDisplayName)%></label>
						    </div>
                         <%
	                         }
                         %>
					   </div>
				    </div>
               <%
                   }		
               %>
               </div>
		    </td>
		    <% }%>
		</tr>

    </table>

<div id="buttonBar" >
	<table valign="bottom" align="<%=isRTL?"left":"right"%>">
		<tr id="buttonsTable" valign="bottom"><td valign="bottom" align="<%=isRTL?"left":"right"%>">
  			<table cellspacing=10 cellpading=0 border=0 style="background:transparent;">
				<tr>
					<td>
						<button type="submit" id="ok"><%=ServletResources.getString("OK", request)%></button>
					</td>
					<td>
					  	<button type="reset" onclick="window.close()" id="cancel"><%=ServletResources.getString("Cancel", request)%></button>
					</td>
				</tr>
  			</table>
		</td></tr>
	</table>
</div>
</form>
</body>
</html>

