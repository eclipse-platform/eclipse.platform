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
<%@ include file="header.jsp"%>
<%
	String msg = (String)request.getAttribute("msg");
%>

<html lang="<%=ServletResources.getString("locale", request)%>">
<head>
<meta http-equiv="Content-Type" content="text/html; charset=utf-8">
<meta http-equiv="Pragma" content="no-cache">
<meta http-equiv="Expires" content="-1">
<link rel="stylesheet" href="printAlert.css" charset="utf-8" type="text/css">

<script type="text/javascript">

function onloadHandler() {
	sizeButtons();
}

function sizeButtons() {
	var minWidth=60;

	if(document.getElementById("ok").offsetWidth < minWidth){
		document.getElementById("ok").style.width = minWidth+"px";
	}
}

</script>

</head>

<body dir='<%=direction%>' onload="onloadHandler()">
<p>&nbsp;</p>
<div align="center">
<div class="printAlertDiv">
	<table align="center" width="400" cellpadding="10" cellspacing="0">
		<tr>
			<td class="caption"><strong><%=ServletResources.getString("error", request)%></strong></td>
		</tr>
		<tr>
			<td height="50" class="message">
			<p><%=ServletResources.getString(msg, request)%>
			</p>
			</td>
		</tr>
		<tr>
			<td class="button">
			<div align="center">
				<button id="ok" onClick="top.close()"><%=ServletResources.getString("OK", request)%></button>
            </div>
			</td>
		</tr>
	</table>
</Ddiv>
</div>
</body>
</html>