/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.internal.ui.views;

import java.io.*;
import java.text.*;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.*;
import org.eclipse.jface.resource.*;
import org.eclipse.update.configurator.*;
import org.eclipse.update.internal.ui.*;

public class InstallationHistoryAction extends Action {

	private BufferedReader buffRead;
	private PrintWriter htmlLog;
	private File tempFile;
	private String rowType;
	private IPath path;
	private static final String lightBlue = "#EEEEFF"; //$NON-NLS-1$
	private static final String white = "#FFFFFF"; //$NON-NLS-1$
	private static final String darkBlue = "#99AADD"; //$NON-NLS-1$
//	private static final String CONFIGURATION = "CONFIGURATION"; //$NON-NLS-1$
	private static final String ACTIVITY = "ACTIVITY"; //$NON-NLS-1$

	public InstallationHistoryAction(String text, ImageDescriptor desc) {
		super(text, desc);
		String location =
			ConfiguratorUtils
				.getCurrentPlatformConfiguration()
				.getConfigurationLocation()
				.getFile();
		path = new Path(location).removeLastSegments(1).append("install.log"); //$NON-NLS-1$
		rowType = "light-row"; //$NON-NLS-1$
	}

	public void run() {
		try {
			openLog();
			parseLog();
			UpdateUI.showURL("file:" + getTempFile().getPath().toString()); //$NON-NLS-1$
		} catch (CoreException e) {
			UpdateUI.logException(e);
		} finally {
			closeLog();
		}
	}

	private void openLog() throws CoreException {
		try {
			buffRead = new BufferedReader(new FileReader(path.toOSString()));
			htmlLog = new PrintWriter(new BufferedWriter(new OutputStreamWriter(new FileOutputStream(getTempFile()), "UTF-8")));
		} catch (FileNotFoundException e) {
			throwCoreException(e);
		} catch (UnsupportedEncodingException e) {
			throwCoreException(e);
		}

	}

	private File getTempFile() throws CoreException {
		if (tempFile == null) {
			try {
				tempFile = File.createTempFile("install-log", ".html"); //$NON-NLS-1$ //$NON-NLS-2$
				tempFile.deleteOnExit();
			} catch (IOException e) {
				throwCoreException(e);
			}
		}
		return tempFile;
	}

	private void parseLog() throws CoreException {
		//		!CONFIGURATION <configuration-date>
		//		!ACTIVITY <date> <target> <action> <status>

		try {
			String type, date, status, target, action;
			StringTokenizer htmlCode;

			htmlLog.println("<html>"); //$NON-NLS-1$
			htmlLog.println("<head>"); //$NON-NLS-1$
			htmlLog.println("<meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\">");
			htmlLog.println("<title>Install-Log</title>"); //$NON-NLS-1$
			addCSS();
			htmlLog.println("</head>"); //$NON-NLS-1$
			htmlLog.println("<body>"); //$NON-NLS-1$
			String title = UpdateUI.getString("InstallationHistoryAction.title"); //$NON-NLS-1$
			String desc = UpdateUI.getString("InstallationHistoryAction.desc"); //$NON-NLS-1$
			htmlLog.println("<h1 class=title>"+title+"</h1>"); //$NON-NLS-1$ //$NON-NLS-2$
			htmlLog.println("<p class=bodyText>"+desc+"</p>"); //$NON-NLS-1$ //$NON-NLS-2$
			
			htmlLog.println("<center>"); //$NON-NLS-1$

			htmlLog.println("<table width =100% border=0 cellspacing=1 cellpadding=2>"); //$NON-NLS-1$

			while (buffRead.ready()) {

				htmlCode = new StringTokenizer(buffRead.readLine());
				while (!(htmlCode.hasMoreElements())) {
					if (!buffRead.ready())
						return;
					htmlCode = new StringTokenizer(buffRead.readLine());
				}

				type = htmlCode.nextToken();
				type = type.substring(type.indexOf("!") + 1, type.length()); //$NON-NLS-1$

				if (type.equals(ACTIVITY)) {
					target = ""; //$NON-NLS-1$
					Date d = new Date(new Long(htmlCode.nextToken()).longValue());
					DateFormat df = DateFormat.getDateTimeInstance();
					date = df.format(d);
					// ignore string date
					htmlCode.nextToken("."); //$NON-NLS-1$
					htmlCode.nextToken(" "); //$NON-NLS-1$
					while (htmlCode.countTokens() > 2)
						target = target + " " + htmlCode.nextToken(); //$NON-NLS-1$

					action = htmlCode.nextToken();
					status = htmlCode.nextToken();

					addActivity(action, date, status, target);

				} else {
					htmlLog.println();
					htmlLog.println(
						"<tr id=separator><td colspan=4></td></tr>"); //$NON-NLS-1$
					htmlLog.println();
					Date d = new Date(new Long(htmlCode.nextToken()).longValue());
					DateFormat df = DateFormat.getDateTimeInstance();
					date = df.format(d);
//					date = ""; //$NON-NLS-1$
//					while (htmlCode.countTokens() > 0)
//						date = date + " " + htmlCode.nextToken(); //$NON-NLS-1$
					addConfigurationHeader(date);
					addActivityHeader();
				}

			}
			htmlLog.println("</table>"); //$NON-NLS-1$
			htmlLog.println("</body>"); //$NON-NLS-1$
			htmlLog.println("</html>"); //$NON-NLS-1$

		} catch (Exception e) {
			throwCoreException(e);
		}

	}

	private void addActivity(
		String type,
		String date,
		String status,
		String target) {
		htmlLog.print("<tr class=" + rowType + ">"); //$NON-NLS-1$ //$NON-NLS-2$
		htmlLog.print("<td class=log-text width=30%>"); //$NON-NLS-1$
		htmlLog.print(date);
		htmlLog.println("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-text width=40%>"); //$NON-NLS-1$
		htmlLog.print(target);
		htmlLog.println("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-text width=20%>"); //$NON-NLS-1$
		htmlLog.print(type);
		htmlLog.println("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-text width=10%>"); //$NON-NLS-1$
		htmlLog.print(status);
		htmlLog.println("</td></tr>"); //$NON-NLS-1$
		toggleRowColor();
	}

	private void addCSS() {
		htmlLog.println("<STYLE type=\"text/css\">"); //$NON-NLS-1$
		htmlLog.println("H1.title { font-family: sans-serif; color: #99AACC }"); //$NON-NLS-1$
		htmlLog.println("P.bodyText { font-family: sans-serif; font-size: 9pt; }"); //$NON-NLS-1$
		htmlLog.println(
			"TD.log-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white}"); //$NON-NLS-1$
		htmlLog.println(
			"TD.log-text { font-family: sans-serif; font-style: normal; font-weight: lighter; font-size: 8pt; color:black}"); //$NON-NLS-1$
		htmlLog.println(
		//	"TD.config-log-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white; ;text-align: right; border-top:10px solid white}");
		"TD.config-log-header { font-family: sans-serif; font-style: normal; font-weight: bold; font-size: 9pt; color: white; border-top:10px solid white}"); //$NON-NLS-1$
		htmlLog.println("TR.light-row {background:" + white + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		htmlLog.println("TR.dark-row {background:" + lightBlue + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		htmlLog.println("TR.header {background:" + darkBlue + "}"); //$NON-NLS-1$ //$NON-NLS-2$
		htmlLog.println("</STYLE>"); //$NON-NLS-1$
	}

	private void addActivityHeader() {
		htmlLog.print("<tr class=header>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-header>"); //$NON-NLS-1$
		htmlLog.print(UpdateUI.getString("InstallationHistoryAction.dateTime")); //$NON-NLS-1$
		htmlLog.print("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-header>"); //$NON-NLS-1$
		htmlLog.print(UpdateUI.getString("InstallationHistoryAction.target")); //$NON-NLS-1$
		htmlLog.print("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-header>"); //$NON-NLS-1$
		htmlLog.print(UpdateUI.getString("InstallationHistoryAction.action")); //$NON-NLS-1$
		htmlLog.print("</td>"); //$NON-NLS-1$
		htmlLog.print("<td class=log-header>"); //$NON-NLS-1$
		htmlLog.print(UpdateUI.getString("InstallationHistoryAction.status")); //$NON-NLS-1$
		htmlLog.println("</td></tr>"); //$NON-NLS-1$
	}

	private void addConfigurationHeader(String date) {
		if (date == null)
			return;

		htmlLog.print("<tr class=header>"); //$NON-NLS-1$
		htmlLog.print("<td class=config-log-header colspan=4>"); //$NON-NLS-1$
		htmlLog.print(date);
		htmlLog.println("</td></tr>"); //$NON-NLS-1$
	}

	private void toggleRowColor() {
		if (rowType.equals("light-row")) //$NON-NLS-1$
			rowType = "dark-row"; //$NON-NLS-1$
		else
			rowType = "light-row"; //$NON-NLS-1$
	}

	private void throwCoreException(Throwable e) throws CoreException {
		throw new CoreException(
			new Status(
				IStatus.ERROR,
				UpdateUI.getPluginId(),
				IStatus.ERROR,
				UpdateUI.getString("InstallationHistoryAction.errors"), //$NON-NLS-1$
				e));
	}

	private void closeLog() {
		try {
			if (buffRead != null)
				buffRead.close();
			if (htmlLog != null)
				htmlLog.close();
		} catch (IOException e) {
		} finally {
			buffRead = null;
			htmlLog = null;
		}
	}

}
