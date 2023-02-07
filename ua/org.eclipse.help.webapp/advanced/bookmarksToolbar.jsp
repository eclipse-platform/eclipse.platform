<%--
 Copyright (c) 2000, 2004 IBM Corporation and others.

 This program and the accompanying materials 
 are made available under the terms of the Eclipse Public License 2.0
 which accompanies this distribution, and is available at
 https://www.eclipse.org/legal/epl-2.0/

 SPDX-License-Identifier: EPL-2.0
 
 Contributors:
     IBM Corporation - initial API and implementation
--%>
<%@ include file="header.jsp"%>


<jsp:include page="toolbar.jsp">
	<jsp:param name="script" value="navActions.js"/>
	<jsp:param name="view" value="bookmarks"/>

	<jsp:param name="name"     value="deleteBookmark"/>
	<jsp:param name="tooltip"  value='deleteBookmark'/>
	<jsp:param name="image"    value="bookmark_rem.svg"/>
	<jsp:param name="action"   value="removeBookmark"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>

	<jsp:param name="name"     value="deleteAllBookmarks"/>
	<jsp:param name="tooltip"  value='deleteAllBookmarks'/>
	<jsp:param name="image"    value="bookmark_remall.svg"/>
	<jsp:param name="action"   value="removeAllBookmarks"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>

</jsp:include>
