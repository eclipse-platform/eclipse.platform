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
	RequestData data = new RequestData(application,request, response);
	WebappPreferences prefs = data.getPrefs();
	String forwardImage, backImage, homeImage;
	if(isRTL) {
		forwardImage = "back.svg";
		backImage = "forward.svg";
	} else {
		forwardImage = "forward.svg";
		backImage = "back.svg";
	}
	homeImage = "home.svg";
	String homeURL = UrlUtil.getHelpURL(prefs.getHelpHome());
	boolean isBookmarkAction = prefs.isBookmarksView() 
		|| prefs.isBookmarksAction() && data.isIE() && !data.isOpera(); // for infocenter, add to favorites supported on IE
	String bookmarkButtonState = isBookmarkAction?"off":"hidden";
	String bookmarkAction = RequestData.MODE_INFOCENTER==data.getMode()?"bookmarkInfocenterPage":"bookmarkPage";
%>
<jsp:include page="toolbar.jsp">
	<jsp:param name="script" value="contentActions.js"/>
	<jsp:param name="toolbar" value="content"/>
		
	<jsp:param name="name"     value="toggle_highlight"/>
	<jsp:param name="tooltip"  value='highlight_tip'/>
	<jsp:param name="image"    value="highlight.svg"/>
	<jsp:param name="action"   value="toggleHighlight"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='hidden'/>	
	
	<jsp:param name="name"     value="back"/>
	<jsp:param name="tooltip"  value='back_tip'/>
	<jsp:param name="image"    value='<%=backImage%>'/>
	<jsp:param name="action"   value="goBack"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>
	
	<jsp:param name="name"     value="forward"/>
	<jsp:param name="tooltip"  value='forward_tip'/>
	<jsp:param name="image"    value='<%=forwardImage%>'/>
	<jsp:param name="action"   value="goForward"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>

	<jsp:param name="name"     value="home"/>
	<jsp:param name="tooltip"  value='home_tip'/>
	<jsp:param name="image"    value='<%=homeImage%>'/>
	<jsp:param name="action"   value="goHome"/>
	<jsp:param name="param"    value="<%=homeURL%>"/>
	<jsp:param name="state"    value='off'/>
	
	
	<jsp:param name="name"     value="synch"/>
	<jsp:param name="tooltip"  value='Synch'/>
	<jsp:param name="image"    value="synch_toc_nav.svg"/>
	<jsp:param name="action"   value="resynch"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>
	
	<jsp:param name="name"     value="add_bkmrk"/>
	<jsp:param name="tooltip"  value='BookmarkPage'/>
	<jsp:param name="image"    value="add_bkmrk.svg"/>
	<jsp:param name="action"   value="<%=bookmarkAction%>"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='<%=bookmarkButtonState%>'/>

	<jsp:param name="name"     value="print"/>
	<jsp:param name="tooltip"  value='Print'/>
	<jsp:param name="image"    value="print_topic.svg"/>
	<jsp:param name="action"   value="printContent"/>
	<jsp:param name="param"    value=""/>
	<jsp:param name="state"    value='off'/>

</jsp:include>
