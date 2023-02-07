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
	BookmarksData data = new BookmarksData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">

<title><%=ServletResources.getString("Bookmarks", request)%></title>

<style type="text/css">
<%@ include file="list.css"%>
</style>

<base target="ContentViewFrame">

<script type="text/javascript" src="utils.js"></script>
<script type="text/javascript" src="list.js"></script>

<script type="text/javascript">		

/**
 * Removes bookmark 
 */
function removeBookmark() 
{
	if (!active) 
		return false;
		
	var bookmark = active;
	active = null;
	
	// Note: bookmark is an anchor "a"
	var url = bookmark.href;
	var i = url.indexOf("/topic/");
	if (i >=0 )
		url = url.substring(i+6);
	// remove any query string
	i = url.indexOf("?");
	if (i >= 0)
		url = url.substring(0, i);
		
	var title = bookmark.title;
	if (title == null || title == "")
		title = url;
			
	window.location.replace("bookmarksView.jsp?operation=remove&bookmark="+encodeURIComponent(url)+"&title="+encodeURIComponent(title));
	return true;
}

/**
 * Removes all bookmarks
 */
function removeAllBookmarks() 
{
	hidePopupMenu();
	if(!confirm("<%=UrlUtil.JavaScriptEncode(ServletResources.getString("confirmDeleteAllBookmarks",request))%>"))
		return true;
	window.location.replace("bookmarksView.jsp?operation=removeAll");
	return true;
}

/**
 * If the Del key was pressed, remove the bookmark
 */
function bookmarkKeyDownHandler(e) {
	var key;
	
	if (isIE) {
		key = window.event.keyCode;
	} else {
		key = e.keyCode;
	}

	// Check if this is the Delete key (code 46)
	if (key != 46)
		return true;
		
	if (isIE) {
		window.event.cancelBubble = true;
	} else {
	    e.cancelBubble = true;
	}
  	
  	return removeBookmark();
}

function onShow() { 
}

</script>

</head>


<body dir="<%=direction%>">
 
<%
if(data.getBookmarks().length == 0) {
	out.write(ServletResources.getString("addBookmark", request));
} else {
%>
<table id='list'  cellspacing='0' >

<%
	Topic[] bookmarks = data.getBookmarks();
	for (int i=0; i<bookmarks.length; i++) 
	{
%>

<tr class='list' id='r<%=i%>'>
	<td align='<%=isRTL?"right":"left"%>' class='label' nowrap>
		<a id='a<%=i%>' 
		   href='<%=UrlUtil.htmlEncode(bookmarks[i].getHref())%>' 
		   onmouseover="showStatus(event);return true;"
		   onmouseout="clearStatus();return true;"
		   oncontextmenu="contextMenuHandler(event);return false;"
		   onkeydown="bookmarkKeyDownHandler(event);"
		   title="<%=UrlUtil.htmlEncode(bookmarks[i].getLabel())%>">
		   <img src="<%=prefs.getImagesDirectory()%>/topic.svg" alt=""><%=UrlUtil.htmlEncode(bookmarks[i].getLabel())%></a>
	</td>
</tr>

<%
	}
%>

</table>
<div id="menu">
	<div class="unselectedMenuItem" onmouseover="this.className='selectedMenuItem'" onmouseout="this.className='unselectedMenuItem'" onclick="removeBookmark()" ><nobr><%=ServletResources.getString("RemoveBookmark",request)%></nobr></div>
	<div class="unselectedMenuItem" onmouseover="this.className='selectedMenuItem'" onmouseout="this.className='unselectedMenuItem'" onclick="removeAllBookmarks()" ><nobr><%=ServletResources.getString("RemoveAllBookmarks",request)%></nobr></div>
</div>

<%
}
%>

</body>
</html>
