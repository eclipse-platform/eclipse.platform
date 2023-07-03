<%--
 Copyright (c) 2000, 2010 IBM Corporation and others.

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
	String printTopicLabel = UrlUtil.JavaScriptEncode(ServletResources.getString("PrintTopic", request));
	String printTocLabel = UrlUtil.JavaScriptEncode(ServletResources.getString("PrintToc", request));
	String printError = UrlUtil.JavaScriptEncode(ServletResources.getString("PrintError", request));
	String menuData = printTopicLabel + "=printTopic(\\'" + printError + "\\')," + printTocLabel + "=printToc(\\'" + printError + "\\')";
   
	String quickSearchTopicLabel = UrlUtil.JavaScriptEncode(ServletResources.getString("QuickSearchTopic", request));
	String quickSearchTocLabel = UrlUtil.JavaScriptEncode(ServletResources.getString("QuickSearchToc", request));
	String quickSearchError = UrlUtil.JavaScriptEncode(ServletResources.getString("QuickSearchError", request));
	String quickSearchMenuData = quickSearchTopicLabel + "=quickSearchTopic(\\'" + quickSearchError + "\\')," + quickSearchTocLabel+ "=quickSearchToc(\\'" + quickSearchError + "\\')";
    
	// See  Bug 290064 for an explanation of why the constants below are used
    final String ON = "on";
    final String OFF = "off";
%>

<jsp:include page="toolbar.jsp">
	<jsp:param name="script" value="navActions.js"/>
	<jsp:param name="view" value="toc"/>

    <jsp:param name="name"     value="show_all"/>
	<jsp:param name="tooltip"  value='show_all'/>
	<jsp:param name="image"    value="show_all.svg"/>
	<jsp:param name="action"   value="toggleShowAll"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value="<%=(new ActivitiesData(application, request, response)).getButtonState()%>"/>
    
	<jsp:param name="name"     value="print_toc"/>
	<jsp:param name="tooltip"  value='PrintMulti'/>
	<jsp:param name="image"    value="print_toc.svg"/>
	<jsp:param name="action"   value="menu"/>
	<jsp:param name="param"    value="<%=menuData%>"/>
	<jsp:param name="state"    value='off'/>
	
	<jsp:param name="name"     value="quick_search"/>
	<jsp:param name="tooltip"  value='QuickSearchMulti'/>
	<jsp:param name="image"    value="quick_search_multi.svg"/>
	<jsp:param name="action"   value="menu"/>
	<jsp:param name="param"    value="<%=quickSearchMenuData%>"/>
	<jsp:param name="state"    value='off'/> 

	<jsp:param name="name"     value="collapseall"/>
	<jsp:param name="tooltip"  value='CollapseAll'/>
	<jsp:param name="image"    value="collapseall.svg"/>
	<jsp:param name="action"   value="collapseAll"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>

	<jsp:param name="name"     value="synchnav"/>
	<jsp:param name="tooltip"  value='SynchNav'/>
	<jsp:param name="image"    value="auto_synch_toc.svg"/>
	<jsp:param name="action"   value="toggleAutosynch"/>
	<jsp:param name="param"    value=""/>	
	<jsp:param name="state"    value="<%=((new CookiesData(application, request, response)).isSynchToc() ? ON : OFF)%>"/>
	
</jsp:include>