<%--
 Copyright (c) 2009, 2018 IBM Corporation and others.

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
    SearchData data=new SearchData(application,request,response);
    WebappPreferences prefs = data.getPrefs();
    String quickSearchType = data.isSelectedTopicQuickSearchRequest()? "QuickSearchTopic":"QuickSearchToc";
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<title><%= ServletResources.getString(quickSearchType, request)%></title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<style type="text/css">
HTML, BODY {
	width:100%;
	height:100%;
	margin:0px;
	padding:0px;
	border:0px;
}

BODY {
	font:<%=prefs.getViewFont()%>;
	font-size:.875rem;
	background-color: <%=prefs.getToolbarBackground()%>;
	color:WindowText; 	
}

TABLE {
	width:auto;
	margin:0px;
	padding:0px;
}

TD, TR {
	margin:0px;
	padding:0px;
	border:0px;
}
#typeinContainer {
	overflow:auto; 	
	margin: 5px 5px 5px 5px;
	padding: 5px 5px 5px 5px;	
}

#searchWord {
    margin-top : 5px;
    padding-top : 5px;
	width:95%;
	font-size:1.0em;
}

BUTTON {
	font:<%=prefs.getViewFont()%>;
	font-size:.875rem;
	margin:5px;
}

FORM {
    margin: 0px;
    border: 0px;
}

#buttonArea {
    height:4em; 
    position:absolute;
    bottom : 0px;
<%
if (data.isMozilla()) {
%>
    padding-bottom:5px;
<%
}
%>
}

</style>

<script type="text/javascript" src="resize.js"></script>
<script type="text/javascript" src="utils.js"></script>
<script type="text/javascript" src="list.js"></script>
<script type="text/javascript">

var quickSearchType = "<%=quickSearchType%>";

function onloadHandler() {
<%
if(!data.isMozilla() || "1.3".compareTo(data.getMozillaVersion()) <=0){
// buttons are not resized immediately on mozilla before 1.3
%>
	sizeButtons();
<%}%>
	document.getElementById("searchWord").value = '<%=UrlUtil.JavaScriptEncode(data.getSearchWord())%>';
	document.getElementById("searchWord").focus();
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

function doQuickSearch(){
	var searchWord = document.getElementById("searchWord").value;
	window.opener.searchFor(searchWord, quickSearchType);		
	window.close();
}

</script>

</head>

<body dir="<%=direction%>" onload="onloadHandler()" >
<form onsubmit="doQuickSearch();return false;">
    <div id="typeinContainer" role="search">
        <label id="searchLabel"
		        for="searchWord"
		        accesskey="<%=ServletResources.getAccessKey("SearchExpressionColon", request)%>">
		      <%=ServletResources.getLabel("SearchExpressionColon", request)%> </label>
    	 <br/>
		<input type="text" id="searchWord" name="searchWord"
			value='' size='<%=data.isIE()?"32":"24"%>' maxlength="256"
			alt="<%=ServletResources.getString("SearchExpression", request)%>"
			title="<%=ServletResources.getString("SearchExpression", request)%>"/>
   </div>

	<div id="buttonArea">
		<table align="<%=isRTL?"left":"right"%>" style="background:<%=prefs.getToolbarBackground()%>">
			<tr id="buttonsTable"><td align="<%=isRTL?"left":"right"%>">
	  			<table cellspacing=0 cellpadding=0 border=0 style="background:transparent;">
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
