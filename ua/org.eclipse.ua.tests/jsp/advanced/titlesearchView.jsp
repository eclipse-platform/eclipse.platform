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
<%@ include file="/advanced/header.jsp"%>
<%@ page import="org.eclipse.ua.tests.help.webextension.*" %>
<% 
	TitleSearchData data = new TitleSearchData(application, request, response);
	String searchWord = request.getParameter("searchWord");
	if (searchWord == null) {
	    searchWord = "";
	}
	
%>

<html>
<head>
<base target="ContentViewFrame">
<script type="text/javascript">

function doSearch(query)
{
	var typein = document.getElementById("typein");
	var query ="searchWord="+encodeURIComponent(typein.value);
	location.replace("titlesearchView.jsp?"+query);
}

</script>
</head>

<body>

<table id="typeinTable">

	<tr>
		<td colspan="2"><p id="instruction">Search topic title</p></td>
	</tr>

	<tr>
		<td width="100%"><input type="text" id="typein" value = "<%=searchWord%>"></td>

		<td><input type="button" id="button" value="Go" onclick="this.blur();doSearch()"></td>

	</tr>
</table>
<% 
    if (searchWord != "") {
        TitleSearchData.SearchResult[] results = data.getSearchResults();
        if (results.length > 0) {
            for (int r = 0; r < results.length; r++) {
%>
<br><a href = "<%=results[r].href%>"><%=results[r].title%></a>
<%
            }
        } else {
%>
    <p>No match found for <%=searchWord%></p>
<%
        }
    } else {
%>
    <p>Enter a search word</p>
<%
    }
%>

</html>