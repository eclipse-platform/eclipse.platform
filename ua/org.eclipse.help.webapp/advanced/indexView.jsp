<%--
 Copyright (c) 2005, 2018 Intel Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     Intel Corporation - initial API and implementation
     IBM Corporation - 122967 [Help] Remote help system (improve responsiveness)
     IBM Corporation - 166695 [Webapp] Index View truncates button if large fonts are used
     IBM Corporation 2006, refactored index view into a single frame
     IBM Corporation 2009, css changes
     IBM Corporation 2010, added lang to html tag
--%>
<%@ include file="header.jsp"%>

<%
    RequestData requestData = new ActivitiesData(application,request, response);
    SearchData searchData = new SearchData(application,request, response);
	WebappPreferences prefs = requestData.getPrefs();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<title><%=ServletResources.getString("IndexViewTitle", request)%></title>

<style type="text/css">
<%@ include file="indexView.css"%>
</style>
<% 
    if (requestData.isMacMozilla()) {
%>
<style type="text/css">
#button {
    background:GrayText;
}
</style>
<%
    }
%>

<base target="ContentViewFrame">

<script type="text/javascript">

var loadingMessage = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("Loading", request))%>";
</script>

<script type="text/javascript" src="indexView.js"></script>
<script type="text/javascript" src="resize.js"></script>
<script type="text/javascript" src="helptree.js"></script>
<script type="text/javascript" src="helptreechildren.js"></script>
<script type="text/javascript" src="xmlajax.js"></script>
<script type="text/javascript" src="utils.js"></script>
<script type="text/javascript" src="view.js"></script>
</head>

<body dir="<%=direction%>" onload="onloadHandler()" onresize = "sizeList()">

<table id="typeinTable">
<%if (prefs.isIndexInstruction()) {%>
<%
    if (searchData.isScopeActive()) {
%>

<p>
<tr>
		<td colspan="2">
<%= UrlUtil.htmlEncode(searchData.getScopeActiveMessage()) %>
<a class="showall" onclick="showAll();" ><%=ServletResources.getString("showAllLink", request)%></a>
</p></td></tr>
<%
    }
%>
	<tr>
		<td colspan="2"><p id="instruction"><%=ServletResources.getString("IndexTypeinInstructions", request)%></p></td>
	</tr>
<%}%>
	<tr>
		<td width="100%"><input type="text" id="typein" aria-label="Find word"></td>
	<%if (prefs.isIndexButton()) {%>
		<td><input type="button" id="button" value="<%=ServletResources.getString("IndexTypeinButton", request)%>" onclick="this.blur();showIndex()"></td>
	<%}%>
	</tr>
</table>
<div id = "indexList">

<DIV class = "group" id = "wai_application" aria-label = "Index search results">
    <DIV class = "root" aria-label = "Search results" id = "tree_root">
    </DIV>
</DIV>
</div>
<div id="navigation">
    <table id="innerNavigation" cellspacing=0 cellpadding=0 border=0 style="background:transparent;">
		<tr>
			<td id = "td_previous">				
                <a role = "link" id = "previous" class = "enabled" onclick="this.blur();loadPreviousPage()"><%=ServletResources.getString("IndexPrevious", request)%></a> 
			</td>
			<td id = "td_next">
				<a role = "link" id = "next" class = "enabled" onclick="this.blur();loadNextPage()"><%=ServletResources.getString("IndexNext", request)%></a> 
			</td>
  	 </table>
</div>
</body>

</html>
