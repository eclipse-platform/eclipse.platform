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
	new ActivitiesData(application, request, response); // here it can turn filtering on or off
	LayoutData data = new LayoutData(application,request, response);
	AbstractView view = data.getCurrentView();
	if (view == null) return;
%>

<html lang="<%=ServletResources.getString("locale", request)%>">

<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title><%=ServletResources.getString(view.getName(), request)%></title>

<script type="text/javascript">

function onloadHandler(e)
{
    resize();
}

function resize()
{
<% if (data.isIE() || data.isMozilla() && "1.2.1".compareTo(data.getMozillaVersion()) <=0){
%>	var titleText=window.<%=view.getName()%>ToolbarFrame.document.getElementById("titleText");
	if (!titleText) return;
	var h=titleText.offsetHeight; <%-- default 13 --%>
	if(h<=19){
		return; <%-- no need to resize up to 19px --%>
	}
	document.getElementById("viewFrameset").setAttribute("rows", (11+h)+",*"); <%-- default 24 --%>
	window.<%=view.getName()%>ToolbarFrame.document.getElementById("titleTextTableDiv").style.height=(9+h)+"px"; <%-- default 22 --%>
<%}%>
}

var resized = false;

// Called when the view is made visible. This function is needed because 
// with IE the resize only works after the view has been displayed for the first time.

function onShow() 
{
    if (!resized) {
        resize();
        resized = true;
    }
    try{
		window.<%=view.getName()%>ViewFrame.onShow();
	} catch(ex) {}
    
}

</script>

</head>

<frameset id="viewFrameset" onload="onloadHandler()" rows="24,*" frameborder="0" framespacing="0" border=0  >
	<frame id="toolbar" name="<%=view.getName()%>ToolbarFrame" title="<%=ServletResources.getString(view.getName()+"ViewToolbar", request)%>" 
	    src='<%=data.getAdvancedURL(view,"Toolbar.jsp")%>'  marginwidth="0" marginheight="0" scrolling="no" frameborder="0" noresize=0>
	<%
	if (view.isDeferred()) {
	%>
		<frame name='<%=view.getName()%>ViewFrame' title="<%=ServletResources.getString(view.getName()+"View", request)%>" 
		    src='<%="deferredView.jsp?href="+data.getAdvancedURL(view,"View.jsp")+"?"+UrlUtil.htmlEncode(request.getQueryString())%>'  marginwidth="10" marginheight="0" frameborder="0" >
	<%
	}
	else {
	%>
		<frame name='<%=view.getName()%>ViewFrame' title="<%=ServletResources.getString(view.getName()+"View", request)%>" 
		    src='<%=data.getAdvancedURL(view,"View.jsp") + "?" + UrlUtil.htmlEncode(request.getQueryString())%>'  marginwidth="10" marginheight="0" frameborder="0" >
	<%
	}
	%>
</frameset>

</html>

