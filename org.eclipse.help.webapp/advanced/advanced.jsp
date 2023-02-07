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
	SearchData data = new SearchData(application, request, response);
	WebappPreferences prefs = data.getPrefs();
%>


<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<title><%=ServletResources.getString("Advanced", request)%></title>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<style type="text/css">

/* need this one for Mozilla */
HTML { 
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
	border:1px solid ThreeDShadow;
	padding:0px;
	margin:0px;
}

TABLE {
	font:<%=prefs.getViewFont()%>;
	font-size: .875rem;
	background:<%=prefs.getToolbarBackground()%>;
}

TD, TR {
	margin:0px;
	padding:0px;
	border:0px;
}

FORM {
	margin:0px;
	padding:0px;
	border:0px;
	height:100%;
}


#searchTable {
	background:transparent; 
	margin:10px 0px 20px 0px;
}

#searchWord {
	border:1px solid ThreeDShadow;
	width:100%;
	font:icon;
	font-size:.875rem;
}

#booksContainer {
	border:1px solid ThreeDShadow;
	margin:0px 10px;
	overflow:auto;
}

.book {
	margin:0xp;
	border:0px;
	padding:0px;
}

.button {
	font:<%=prefs.getViewFont()%>;
	font-size: .875rem;
}

<%
if (data.isMozilla()) {
%>
input[type="checkbox"] {
	border:2px solid ThreeDShadow; 
	margin:0xp; 
	padding:0px;	
	height:12px;
	width:12px;
}
<%
}
%>
</style>

<script type="text/javascript">

function doAdvancedSearch()
{
	try
	{
		var form = document.forms["searchForm"];
		var searchWord = form.searchWord.value;
		var maxHits = form.maxHits.value;
		if (!searchWord || searchWord == "")
			return;
	
		var scope = "";
		var buttons = document.getElementsByTagName("INPUT");
		for (var i=0; i<buttons.length; i++)
		{
			if (buttons[i].type != "checkbox") continue;
			if (buttons[i].checked == false) continue;
			scope += "&scope="+encodeURIComponent(buttons[i].name);
		}
		
		// persist selection
		window.opener.saveSelectedBooks(getSelectedBooks());
		
		window.opener.document.forms["searchForm"].searchWord.value = searchWord;
		var query = "searchWord="+encodeURIComponent(searchWord)+"&scopedSearch=true&maxHits="+maxHits + scope;
		window.opener.doSearch(query);
		window.opener.focus();
		window.close();
	} catch(ex) {}
}

function restoreSelectedBooks()
{
	var selectedBooks = window.opener.selectedBooks;
	var inputs = document.body.getElementsByTagName("INPUT");
	for (var i=0; i<inputs.length; i++) {
		if (inputs[i].type == "checkbox" && isSelected(inputs[i].name, selectedBooks))
			inputs[i].checked = true;
	}
}


function getSelectedBooks()
{
	var selectedBooks = new Array();
	var inputs = document.body.getElementsByTagName("INPUT");
	for (var i=0; i<inputs.length; i++) {
		if (inputs[i].type == "checkbox"  && inputs[i].checked)
			selectedBooks[selectedBooks.length] = inputs[i].name;
	}
	return selectedBooks;
}

function isSelected(book, selectedBooks)
{
	// the first time select all
	if (!selectedBooks)
		return true;
		
	for (var i=0; i<selectedBooks.length; i++)
		if (book == selectedBooks[i])
			return true;
	return false;
}

function onloadHandler()
{
	// select the books from previous run, or all otherwise
	restoreSelectedBooks();
}

</script>

</head>

<body dir="<%=direction%>" onload="onloadHandler()">

<form name="searchForm" onsubmit="doAdvancedSearch()">
<div style="overflow:auto;height:250px;" role="search">
	<table id="searchTable" width="100%" cellspacing=0 cellpading=0 border=0 align=center role="presentation">
		<tr><td style="padding:0px 10px;"><label for="searchWord"><%=ServletResources.getString("SearchExpression", request)%></label>
		</td></tr>
		<tr><td style="padding:0px 10px;"><input type="text" id="searchWord" name="searchWord" value="<%=UrlUtil.htmlEncode(data.getSearchWord())%>" maxlength=256 alt="<%=ServletResources.getString("SearchExpression", request)%>" title="<%=ServletResources.getString("SearchExpression", request)%>">
          	  	<input type="hidden" name="maxHits" value="500" >
        </td></tr>
        <tr><td style="padding:0px 10px;"><%=ServletResources.getString("expression_label", request)%>
        </td></tr>
    </table>
  
  	<table id="filterTable" width="100%" cellspacing=0 cellpading=0 border=0 align=center  style="background:transparent;">
		<tr><td><div id="selectBook" style="margin-<%=isRTL?"right":"left"%>:10px;"><%=ServletResources.getString("Select", request)%></div>
		</td></tr>
		<tr><td>
			<div id="booksContainer">
<% 
TocData tocData = new TocData(application, request, response);
for (int i=0; i<tocData.getTocCount(); i++)
{
	String label = tocData.getTocLabel(i);
%>
				<div class="book"><input class="checkbox" type="checkbox" name='<%=tocData.getTocHref(i)%>' id="checkbox<%=i%>" alt="<%=label%>"><label for="checkbox<%=i%>"><%=label%></label></div>
<%
}		
%>
			</div>
		</td></tr>
	</table>
</div>
<div style="height:50px;">
	<table valign="bottom" align="<%=isRTL?"left":"right"%>">
		<tr id="buttonsTable" valign="bottom"><td valign="bottom" align="<%=isRTL?"left":"right"%>">
  			<table cellspacing=10 cellpading=0 border=0 align=<%=isRTL?"left":"right"%>  style="background:transparent;">
				<tr>
					<td>
						<input id="searchButton" class='button' type="submit" role="button" onclick="doAdvancedSearch()" value="<%=ServletResources.getString("SearchLabel", request)%>"  id="go" alt="<%=ServletResources.getString("SearchLabel", request)%>" title="<%=ServletResources.getString("SearchLabel", request)%>">
					</td>
					<td>
					  	<input class='button' type="button" onclick="window.close()"  type="button"  value="<%=ServletResources.getString("Cancel", request)%>"  id="cancel" alt="<%=ServletResources.getString("Cancel", request)%>" title="<%=ServletResources.getString("Cancel", request)%>">
					</td>
				</tr>
  			</table>
		</td></tr>
	</table>
</div>
 </form>

</body>
</html>
