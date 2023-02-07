<%--
 Copyright (c) 2000, 2018 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation    
     Pierre Candela  - fix for Bug 194911
--%>
<%@ include file="header.jsp"%>

<% 
	SearchData data = new SearchData(application, request, response);
	WebappPreferences prefs = data.getPrefs();
%>


<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">

<title><%=ServletResources.getString("SearchLabel", request)%></title>
     
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
	background:<%=prefs.getToolbarBackground()%>;
	border:0px;
	height:100%;
}

TABLE {
	background:<%=prefs.getToolbarBackground()%>;
	font: <%=prefs.getToolbarFont()%>;
	font-size: .875rem;
	margin: 0px;
	padding: 0px;
	height:100%;
}

FORM {
	background:<%=prefs.getToolbarBackground()%>;
	height:100%;
	margin:0px;
}

INPUT {
	font: <%=prefs.getToolbarFont()%>;
	font-size: .875rem;
	margin:0px;
	padding:0px;
}

A {
	color:WindowText;
	text-decoration:none;
}

#searchTD {
	padding-<%=isRTL?"right":"left"%>:7px;
	padding-<%=isRTL?"left":"right"%>:4px;
}

#searchWord {
	margin-left:5px;
	margin-right:5px;
	border:1px solid ThreeDShadow;
}

#searchLabel {
	color:WindowText;
}

#go {
<% 
    if (data.isMacMozilla()) {
%>
    background:GrayText;
<% 
    } else {
%>
    background:ThreeDShadow;
<%
    }
%>
	color:Window;
	font-weight:bold;
	border:1px solid ThreeDShadow;
	margin-left:1px;
	font-size: 1.0em;
}

#scopeLabel {
	text-decoration:underline; 
	color:#005FFF; 
	cursor:pointer;
	padding-left:15px;   /* This should be the same for both RTL and LTR. */
}

#scope { 
	text-align:<%=isRTL?"left":"right"%>;
	margin-<%=isRTL?"right":"left"%>:5px;
	border:0px;
	color:WindowText;
	text-decoration:none;
}

<%
	if (data.isIE()) {
%>
#go {
	padding-<%=isRTL?"right":"left"%>:1px;
}
<%
	}
%>
<%@ include file="darkTheme.css"%>
</style>

<script type="text/javascript">
var isIE = navigator.userAgent.indexOf('MSIE') != -1;
var isMozilla = navigator.userAgent.toLowerCase().indexOf('mozilla') != -1 && parseInt(navigator.appVersion.substring(0,1)) >= 5;

var advancedDialog;

function openAdvanced() 
{ 
    var scope = document.getElementById("scope").firstChild;
    var workingSet = ""; 
    if (scope != null) 
              workingSet = document.getElementById("scope").firstChild.nodeValue;                
    var minSize = 300; 
    var maxHeight= 500;  
    var maxWidth = 600;       
    var w = minSize; 
    var h = minSize; 
      
    // If we have large fonts make the dialog larger, up to 500 pixels high, 600 wide
    try {         
        var letterHeight = document.getElementById("searchWord").offsetHeight; 
        var requiredSize = 16 * letterHeight; 
        if (requiredSize > minSize) { 
            if (requiredSize < maxWidth) { 
                w = requiredSize; 
            } else { 
                w =  maxWidth; 
            }
            if (requiredSize < maxHeight) { 
                h = requiredSize; 
            } else {               
                h = maxHeight;
            }
        } 
             
    } catch (e) {} 
    
<%
if (data.isIE()){
%>
	var l = parent.screenLeft + (parent.document.body.clientWidth - w) / 2;
	var t = parent.screenTop + (parent.document.body.clientHeight - h) / 2;
<%
} else {
%>
	var l = parent.screenX + (parent.innerWidth - w) / 2;
	var t = parent.screenY + (parent.innerHeight - h) / 2;
<%
}
%>
	// move the dialog just a bit higher than the middle
	if (t-50 > 0) t = t-50;
	
	window.location="javascript://needModal";
	advancedDialog = window.open("workingSetManager.jsp?workingSet="+encodeURIComponent(workingSet), "advancedDialog", "resizable=yes,height="+h+",width="+w+",left="+l+",top="+t );
	advancedDialog.focus(); 
}

function closeAdvanced()
{
	try {
		if (advancedDialog)
			advancedDialog.close();
	}
	catch(e) {}
}

/**
 * This function can be called from this page or from
 * the advanced search page. When called from the advanced
 * search page, a query is passed.
 * noRefocus is a boolean which if true suppresses
 * switch of focus to the search view
 */
function doSearch(query, noRefocus)
{
	var workingSet = document.getElementById("scope").firstChild.nodeValue;

	var form = document.forms["searchForm"];
	var searchWord = form.searchWord.value;
	var maxHits = form.maxHits.value;
	if (!searchWord || searchWord == "")
		return;
	query ="searchWord="+encodeURIComponent(searchWord)+"&maxHits="+maxHits;
	if (workingSet != '<%=UrlUtil.JavaScriptEncode(ServletResources.getString("All", request))%>')
		query = query +"&scope="+encodeURIComponent(workingSet);
		
	/******** HARD CODED VIEW NAME *********/
	// do some tests to ensure the results are available
	if (parent.parent.HelpFrame && 
		parent.parent.HelpFrame.NavFrame && 
		parent.parent.HelpFrame.NavFrame.showView &&
		parent.parent.HelpFrame.NavFrame.ViewsFrame && 
		parent.parent.HelpFrame.NavFrame.ViewsFrame.search && 
		parent.parent.HelpFrame.NavFrame.ViewsFrame.search.searchViewFrame) 
	{
	    if (!noRefocus) {
		    parent.parent.HelpFrame.NavFrame.showView("search");
		}
		var searchView = parent.parent.HelpFrame.NavFrame.ViewsFrame.search.searchViewFrame;
		searchView.location.replace("searchView.jsp?"+query);
	}
}

function getSearchWord() {
    var form = document.forms["searchForm"];
    var searchWord = form.searchWord.value;
    if (searchWord ) {
        return searchWord;
    }
    return "";
}

function rescope() {
    if (parent.parent.HelpFrame && 
		parent.parent.HelpFrame.NavFrame && 
		parent.parent.HelpFrame.NavFrame.ViewsFrame) {
		var viewsFrame = parent.parent.HelpFrame.NavFrame.ViewsFrame;
		if (viewsFrame.toc && viewsFrame.toc.tocViewFrame) {
		    var tocView = viewsFrame.toc.tocViewFrame;
		    tocView.repaint();
		}
		if (viewsFrame.index && viewsFrame.index.indexViewFrame) {
		    var indexView = viewsFrame.index.indexViewFrame;
		    indexView.repaint();
		}
		doSearch(null, true);
	}
}

function fixHeights()
{
	if (!isIE) return;
	
	var h = document.getElementById("searchWord").offsetHeight;
	document.getElementById("go").style.height = h;
}

function onloadHandler(e)
{
	var form = document.forms["searchForm"];
	form.searchWord.value = '<%=UrlUtil.JavaScriptEncode(data.getSearchWord())%>';
	fixHeights();
<%
    if (data.isScopeRequest() && RequestScope.filterBySearchScope(request)) {
%>
    rescope();
<%
    }
%>
}

</script>

</head>

<body dir="<%=direction%>" onload="onloadHandler()"  onunload="closeAdvanced()">

	<form  name="searchForm"   onsubmit="doSearch()">
	<div role="search">
		<table id="searchTable" align="<%=isRTL?"right":"left"%>" valign="middle" cellspacing="0" cellpadding="0" border="0" role="presentation">
			<tr nowrap  valign="middle">
				<td <%=isRTL?"nowrap":""%> id="searchTD">
					<label id="searchLabel" for="searchWord" accesskey="<%=ServletResources.getAccessKey("SearchLabel", request)%>">
					&nbsp;<%=ServletResources.getLabel("SearchLabel", request)%>
					</label>
				</td>
				<td>
					<input type="text" id="searchWord" name="searchWord" value='' size="24" maxlength="256" 
					       alt="<%=UrlUtil.htmlEncode(ServletResources.getString("expression_label", request))%>" 
					       title="<%=UrlUtil.htmlEncode(ServletResources.getString("expression_label", request))%>">
				</td>
				<td >
					<input type="submit" role="button" onclick="this.blur();doSearch()" value="<%=ServletResources.getString("GO", request)%>" id="go" alt="<%=ServletResources.getString("GO", request)%>" title="<%=ServletResources.getString("GO", request)%>">
					<input type="hidden" name="maxHits" value="500" >
				</td>
				<td nowrap>
					<a id="scopeLabel" href="javascript:openAdvanced();" title="<%=ServletResources.getString("ScopeTooltip", request)%>" alt="<%=ServletResources.getString("ScopeTooltip", request)%>" onmouseover="window.status='<%=UrlUtil.JavaScriptEncode(ServletResources.getString("ScopeTooltip", request))%>'; return true;" onmouseout="window.status='';"><%=ServletResources.getLabel("Scope", request)%></a>
				</td>
				<td nowrap>
					<input type="hidden" name="workingSet" value='<%=UrlUtil.htmlEncode(data.getScope())%>'>
					<div id="scope" ><%=UrlUtil.htmlEncode(data.getScope())%></div>
				</td>
			</tr>

		</table>
	</div>
	</form>		

</body>
</html>

