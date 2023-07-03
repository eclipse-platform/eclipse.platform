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
<%@ page import="org.eclipse.help.internal.search.SearchHit" %>
<%@ page import="org.eclipse.help.IUAElement" %>
<%@ page import="org.eclipse.help.IHelpResource" %>
<%@ include file="header.jsp"%>

<% 
	SearchData data = new SearchData(application, request, response);
	WebappPreferences prefs = data.getPrefs();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<title><%=ServletResources.getString("SearchResults", request)%></title>

<style type="text/css">
<%@ include file="searchList.css"%>
</style>


<base target="ContentViewFrame">
<script type="text/javascript" src="utils.js"></script>
<script type="text/javascript" src="list.js"></script>
<script type="text/javascript" src="view.js"></script>
<script type="text/javascript">	

var cookiesRequired = "<%=UrlUtil.JavaScriptEncode(ServletResources.getString("cookiesRequired", request))%>";	
var showCategories = <%=data.isShowCategories()%>;
var scope ="<%=UrlUtil.JavaScriptEncode(data.getScope())%>";

function refresh() { 
	window.location.replace("searchView.jsp?<%=UrlUtil.JavaScriptEncode(request.getQueryString())%>");
}

function isShowLocations() {
	var value = getCookie("showLocations");
	return value ? value == "true" : <%=data.isShowLocations()%>;
}

function isShowDescriptions() {
	var value = getCookie("showDescriptions");
	return value ? value == "true" : <%=data.isShowDescriptions()%>;
}

function setShowCategories(value) { 	    
	parent.searchToolbarFrame.setButtonState("show_categories", value);
	var searchWord = "<%=UrlUtil.JavaScriptEncode(data.getSearchWord())%>";
	    window.location="searchView.jsp?searchWord=" + encodeURIComponent(searchWord) 
	       + "&showSearchCategories=" + value +
	       "&scope=" + encodeURIComponent(scope);    
}

function setShowLocations(value) {
	setCookie("showLocations", value);
	var newValue = isShowLocations();
	parent.searchToolbarFrame.setButtonState("show_locations", newValue);
	if (value != newValue) {
		alert(cookiesRequired);
	} else {
		setCSSRule(".location", "display", value ? "block" : "none");
	}
}

function setShowDescriptions(value) {
	setCookie("showDescriptions", value);
	var newValue = isShowDescriptions();   	
	parent.searchToolbarFrame.setButtonState("show_descriptions", newValue);
	if (value != newValue) {
	    alert(cookiesRequired);
	} else { 
	    setCSSRule(".description", "display", value ? "block" : "none");
	}
}

function toggleShowCategories() {
	setShowCategories(!showCategories);
}

function toggleShowLocations() {
	setShowLocations(!isShowLocations());
}

function toggleShowDescriptions() {
	setShowDescriptions(!isShowDescriptions());
}

function onShow() { 
}
</script>


</head>

<body dir="<%=direction%>">

<%
	String preResults = data.getPreProcessorResults();

	if (!preResults.equals(""))
	{
		out.write(preResults);
		out.write("<HR/>");
	}


if (!data.isSearchRequest()) {
	out.write(ServletResources.getString("doSearch", request));
} else if (data.getQueryExceptionMessage()!=null) {
	out.write(data.getQueryExceptionMessage());
} else if (data.isProgressRequest()) {
%>

<CENTER>
<TABLE BORDER='0'>
	<TR><TD><%=ServletResources.getString("Indexing", request)%></TD></TR>
	<TR><TD ALIGN='<%=isRTL?"RIGHT":"LEFT"%>'>
		<DIV STYLE='width:100px;height:16px;border:1px solid ThreeDShadow;'>
			<DIV ID='divProgress' STYLE='width:<%=data.getIndexedPercentage()%>px;height:100%;background-color:Highlight'></DIV>
		</DIV>
	</TD></TR>
	<TR><TD><%=data.getIndexedPercentage()%>% <%=ServletResources.getString("complete", request)%></TD></TR>
	<TR><TD><br><%=ServletResources.getString("IndexingPleaseWait", request)%></TD></TR>
</TABLE>
</CENTER>
<script language='JavaScript'>
setTimeout('refresh()', 2000);
</script>
</body>
</html>

<%
	return;
} else if (data.getResultsCount() == 0){
	out.write(UrlUtil.htmlEncode(data.getNotFoundMessage()));   
    if (data.isScopeActive()) {
%>
<a class="showall" onclick="showAll();" ><%=ServletResources.getString("showAllLink", request)%></a>
<%
    }
} else { // data.getResultsCount() != 0
%>
<p><b><%=UrlUtil.htmlEncode(data.getMatchesInScopeMessage())%></b>
<%
    if (data.isScopeActive()) {
%>
&nbsp;<a class="showall" onclick="showAll();" ><%=ServletResources.getString("showAllLink", request)%></a>
<% 
    } else {
%>
    &nbsp;<a class="showall" onclick="rescope();" ><%=ServletResources.getString("changeScopeLink", request)%></a>
<%
    }
%>
</p>


<table class="results" cellspacing='0'>

<%
	String oldCat = null;
	for (int topic = 0; topic < data.getResultsCount(); topic++)
	{
		if(data.isActivityFiltering() && !data.isEnabled(topic)){
			continue;
		}

		String cat = data.getCategoryLabel(topic);
		if (data.isShowCategories() && cat != null
				&& (oldCat == null || !oldCat.equals(cat))) {
%>

</table>
<table class="category" cellspacing='0'>
	<tr class='list' id='r<%=topic%>c'>
		<td>
			<a href="<%=UrlUtil.htmlEncode(data.getCategoryHref(topic))%>"
					id="a<%=topic%>c"'
					class="link"
					onmouseover="showStatus(event);return true;"
					onmouseout="clearStatus();return true;">
				<%=UrlUtil.htmlEncode(cat)%>
			</a>
		</td>
	</tr>
</table>
<table class="results" cellspacing='0'>

<%
			oldCat = cat;
		}
%>

<tr class='list' id='r<%=topic%>'>
	<td class='icon'>

<%
		boolean isPotentialHit = data.isPotentialHit(topic);
		if (isPotentialHit) {
%>


	<img src="<%=prefs.getImagesDirectory()%>/d_topic.svg" alt=""/>

<%
		}
		else {
%>

	<img src="<%=prefs.getImagesDirectory()%>/topic.svg" alt=""/>

<%
		}
%>

	</td>
	<td align='<%=isRTL?"right":"left"%>'>
		<a class='link' id='a<%=topic%>' 
		   href="<%=UrlUtil.htmlEncode(data.getTopicHref(topic))%>" 
		   onmouseover="showStatus(event);return true;"
		   onmouseout="clearStatus();return true;"
		   title="<%=data.getTopicTocLabel(topic)%>">

<%
		String label = null;
		if (isPotentialHit) {
            label = ServletResources.getString("PotentialHit", data.getTopicLabel(topic), request);
        }
        else {
            label = data.getTopicLabel(topic);
        }
%>

        <%=label%></a>
	</td>
</tr>
<%
			// location breadcrumb
			%><tr><td class='icon'></td><td><div class="location"><%
			SearchHit hit = data.getResults()[topic];

			// book
			if (!data.isShowCategories()) {
				%><a href="<%=UrlUtil.htmlEncode(data.getCategoryHref(topic))%>"><%=UrlUtil.htmlEncode(hit.getToc().getLabel())%></a> &#8250; <%
			}

			// chapters
			String href = hit.getHref();
			int queryPartStart = href.indexOf('?');
			if (queryPartStart >= 0) {
				href = href.substring(0, queryPartStart);
			}
			int[] path = UrlUtil.getTopicPath("/topic" + href,
			                                  ServletResources.getString("locale", request));
			IUAElement node = hit.getToc();
			for (int i = 1; path != null && i < path.length - 1; i++) {
				node = node.getChildren()[path[i]];
				if (node instanceof IHelpResource) {
					IHelpResource breadcrumbItem = (IHelpResource) node;
					if (i > 1) {
						%> &#8250; <%
					}
					String itemHref = breadcrumbItem.getHref();
					if (itemHref == null) {
						itemHref = "../nav/";
						for (int j = 0; j <= i; j++) {
							if (j > 0) {
								itemHref += "_";
							}
							itemHref += path[j];
						}
					} else {
						itemHref = UrlUtil.getHelpURL(itemHref);
					}
					%><a href="<%=UrlUtil.htmlEncode(itemHref)%>"><%=UrlUtil.htmlEncode(breadcrumbItem.getLabel())%></a><%
				}
			}
			%></div></td></tr><%

		String desc = data.getTopicDescription(topic);
		if (desc!=null) {
%>
<tr id='d<%=topic%>'>
	<td class='icon'>
	</td>
	<td align='<%=isRTL?"right":"left"%>'>
		<div class="description">
			<%=desc%>
		</div>
	</td>
</tr>
<%
		}
	}
%>
</table>

<%
}
%>

<script type="text/javascript">
	selectTopicById('<%=UrlUtil.JavaScriptEncode(data.getSelectedTopicId())%>');
</script>

</body>
</html>
