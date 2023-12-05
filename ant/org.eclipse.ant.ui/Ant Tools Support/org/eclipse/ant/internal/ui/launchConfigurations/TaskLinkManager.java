/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.util.FileUtils;
import org.eclipse.ant.internal.core.IAntCoreConstants;
import org.eclipse.ant.internal.launching.AntLaunch;
import org.eclipse.ant.internal.launching.AntLaunchingUtil;
import org.eclipse.ant.internal.launching.LinkDescriptor;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.ExternalHyperlink;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IHyperlink;

/**
 * Manages task links per process. As messages are logged to the console from build events, hyperlinks are created to link task names to the
 * associated ant buildfile. The build logger registers a task hyperlink with this manager for each build event associated with a task. When the
 * associated line is later appended to the console, the corresponding text region in the console document is determined (as the length of a console
 * document can not be determined beforehand), and the hyperlink is added to the document. The new line is added to the console, information from that
 * line may be stored to process future incoming tasks hyperlinks.
 */
public class TaskLinkManager {

	private static Map<String, IFile> fFileNameToIFile = new HashMap<>();

	/**
	 * Not to be called.
	 */
	private TaskLinkManager() {
		super();
	}

	private static IHyperlink createHyperlink(LinkDescriptor linkDescriptor) {
		String fileName = linkDescriptor.getFileName();
		int lineNumber = linkDescriptor.getLineNumber();

		IHyperlink taskLink = null;
		if (lineNumber == -1) {
			// fileName will actually be the String representation of Location
			taskLink = AntUtil.getLocationLink(fileName, null);
		} else {
			IFile file = fFileNameToIFile.get(fileName);
			if (file == null) {
				file = AntLaunchingUtil.getFileForLocation(fileName, null);
				if (file != null) {
					fFileNameToIFile.put(fileName, file);
					taskLink = new FileLink(file, null, -1, -1, lineNumber);
				} else if (fileName != null) {
					File javaIOFile = FileUtils.getFileUtils().resolveFile(null, fileName);
					if (javaIOFile.exists()) {
						taskLink = new ExternalHyperlink(javaIOFile, lineNumber);
					}
				}
			} else {
				taskLink = new FileLink(file, null, -1, -1, lineNumber);
			}
		}
		return taskLink;
	}

	private static boolean addLink(IConsole console, IRegion lineRegion, LinkDescriptor descriptor) {
		try {
			String text = console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
			if (text.trim().equals(descriptor.getLine())) {
				int offset = lineRegion.getOffset() + descriptor.getOffset();
				IHyperlink link = createHyperlink(descriptor);
				if (link != null) {
					console.addLink(link, offset, descriptor.getLength());
				}
				return true;
			}
		}
		catch (BadLocationException e) {
			// do nothing
		}
		return false;
	}

	/**
	 * A new line has been added to the given console. Adds any task hyperlink associated with the line, to the console. The new line may be stored to
	 * process future incoming tasks hyperlinks.
	 */
	public static synchronized void processNewLine(IConsole console, IRegion newLine) {
		AntLaunch launch = (AntLaunch) console.getProcess().getLaunch();
		List<LinkDescriptor> links = launch.getLinkDescriptors();

		if (linkBuildFileMessage(console, newLine)) {
			return;
		}
		for (LinkDescriptor descriptor : links) {
			if (addLink(console, newLine, descriptor)) {
				launch.removeLinkDescriptor(descriptor);
				return;
			}
		}
	}

	/**
	 * Disposes any information stored for the given process.
	 */
	public static void dispose(IProcess process) {
		AntLaunch launch = (AntLaunch) process.getLaunch();
		launch.clearLinkDescriptors();
	}

	@SuppressWarnings("deprecation")
	private static boolean linkBuildFileMessage(IConsole console, IRegion region) {

		String message = IAntCoreConstants.EMPTY_STRING;
		int offset = region.getOffset();
		try {
			message = console.getDocument().get(offset, region.getLength());
		}
		catch (BadLocationException e) {
			// do nothing
		}
		if (message.startsWith("Buildfile:")) { //$NON-NLS-1$
			String fileName = message.substring(10).trim();
			IFile file = AntUtil.getFileForLocation(fileName, null);
			if (file != null) {
				FileLink link = new FileLink(file, null, -1, -1, -1);
				console.addLink(link, offset + 11, fileName.length());
				return true;
			}
		}
		return false;
	}
}
