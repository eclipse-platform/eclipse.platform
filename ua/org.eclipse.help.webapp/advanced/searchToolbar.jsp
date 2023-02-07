<%--
 Copyright (c) 2000, 2009 IBM Corporation and others.

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
    // See  Bug 290064 for an explanation of why these constants are used
    final String ON = "on";
    final String OFF = "off";
%>


<jsp:include page="toolbar.jsp">
	<jsp:param name="script" value="navActions.js"/>
	<jsp:param name="view" value="search"/>

	<jsp:param name="name"     value="show_all"/>
	<jsp:param name="tooltip"  value='show_all'/>
	<jsp:param name="image"    value="show_all.svg"/>
	<jsp:param name="action"   value="toggleShowAll"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value="<%=(new ActivitiesData(application, request, response)).getButtonState()%>"/>

	<jsp:param name="name"     value="show_categories"/>
	<jsp:param name="tooltip"  value='show_categories'/>
	<jsp:param name="image"    value="show_categories.svg"/>
	<jsp:param name="action"   value="toggleShowCategories"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value="<%=((new SearchData(application, request, response)).isShowCategories() ? ON : OFF)%>"/>

	<jsp:param name="name"     value="show_locations"/>
	<jsp:param name="tooltip"  value='show_locations'/>
	<jsp:param name="image"    value="show_locations.svg"/>
	<jsp:param name="action"   value="toggleShowLocations"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value="<%=((new SearchData(application, request, response)).isShowLocations() ? ON : OFF)%>"/>

	<jsp:param name="name"     value="show_descriptions"/>
	<jsp:param name="tooltip"  value='show_descriptions'/>
	<jsp:param name="image"    value="show_descriptions.svg"/>
	<jsp:param name="action"   value="toggleShowDescriptions"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value="<%=((new SearchData(application, request, response)).isShowDescriptions() ? ON : OFF)%>"/>

</jsp:include>