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
package org.eclipse.ant.internal.ui.launchConfigurations;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.model.AntUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.debug.ui.console.IConsole;
import org.eclipse.debug.ui.console.IConsoleHyperlink;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IRegion;

/**
 * Manages task links per process. As messages are logged to the console from
 * build events, hyperlinks are created to link task names to the associated ant
 * buildfile. The build logger registers a task hyperlink with this manager for
 * each build event associated with a task. When the associated line is later
 * appended to the console, the corresponding text region in the console
 * document is determined (as the length of a console document can not be
 * determined beforehand), and the hyperlink is added to the document.
 * The new line is added to the console, information from that line 
 * may be stored to process future incoming tasks hyperlinks.
 */
public class TaskLinkManager {
	
	/**
	 * A map of processes to lists of queued task hyperlink entries
	 */
	private static Map fgProcessToLinks;
	
	/**
	 * A map of processes to lists of queued new line regions
	 */
	private static Map fgProcessToNewLines;
	
	private static List fgAntBuilds;
	
	private static class HyperlinkEntry {
		private IConsoleHyperlink fLink;
		private IRegion fRegion;
		private String fMessage;
		
		public HyperlinkEntry(IConsoleHyperlink link, IRegion region, String message) {
			fLink = link;
			fRegion = region;	
			fMessage = message;
		}
		
		public IRegion getRegion() {
			return fRegion;
		}
		
		public IConsoleHyperlink getLink() {
			return fLink;
		}
		
		public String getMessage() {
			return fMessage;
		}
	}
	
	private static class LineEntry {
		private IConsole fConsole;
		private IRegion fRegion;
	
		public LineEntry(IConsole console, IRegion region) {
			fConsole = console;
			fRegion = region;	
		}
	
		public IRegion getRegion() {
			return fRegion;
		}
	
		public IConsole getConsole() {
			return fConsole;
		}
	}

	/**
	 * Not to be called.
	 */
	private TaskLinkManager() {
		super();
	}
	
	/**
	 * Registers a hyperlink for the given process and task name. The given
	 * region is relative to the beginning of the line (not the document).
	 * 
	 * @param process the process associated with the link
	 * @param link the link for the process
	 * @param region The region within the line
	 * @param message The message related to the link
	 */
	public static synchronized void addTaskHyperlink(IProcess process, IConsoleHyperlink link, IRegion region, String message) {
		if (fgProcessToNewLines != null) {
			List newLines = (List)fgProcessToNewLines.get(process);
			if (newLines != null) {
				for (int index= 0; index < newLines.size(); index++) {
					LineEntry newLine = (LineEntry) newLines.get(index);
					if (addLink(newLine.getConsole(), link, newLine.getRegion(), region, message)) {
						newLines.subList(0, index + 1).clear();
						return;
					}
				}
			}
		}
				
		if (fgProcessToLinks == null) {
			fgProcessToLinks = new HashMap();
			
		}
		List links = (List)fgProcessToLinks.get(process);
		if (links == null) {
			links = new ArrayList(10);
			fgProcessToLinks.put(process, links);
		}
		
		links.add(new HyperlinkEntry(link, region, message));
	}
	
	private static boolean addLink(IConsole console, IConsoleHyperlink link, IRegion lineRegion, IRegion region, String message) {
		
		int length = region.getLength();
		
		String text;
		try {
			text = console.getDocument().get(lineRegion.getOffset(), lineRegion.getLength());
		} catch (BadLocationException e) {
			return false;
		}
		if (text.trim().equals(message)) {
			int offset = lineRegion.getOffset() + region.getOffset();
			console.addLink(link, offset, length);
			return true;
		}
		return false;
	}

	/**
	 * A new line has been added to the given console. Adds any task hyperlink
	 * associated with the line, to the console.
	 * The new line may be stored to process future incoming tasks hyperlinks.
	 * 
	 * @param console
	 * @param newLine
	 */
	public static synchronized void processNewLine(IConsole console, IRegion newLine) {
		IProcess process = console.getProcess();
		if (fgAntBuilds != null && fgAntBuilds.contains(process)) {
			if (linkBuildFileMessage(console, newLine)) {
				fgAntBuilds.remove(process);
				return;
			}
		}
		if (fgProcessToLinks == null) {
			addNewLine(console, newLine, process);
			return;
		}
		
		List links = (List)fgProcessToLinks.get(process);
		if (links == null) {
			addNewLine(console, newLine, process);
			return;
		}
		
		for (int index= 0; index < links.size(); index++) {
			HyperlinkEntry link = (HyperlinkEntry) links.get(index);
			if (addLink(console, link.getLink(), newLine, link.getRegion(), link.getMessage())) {
				links.subList(0, index + 1).clear();
				return;
			}
		}
	}
	
	private static void addNewLine(IConsole console, IRegion newLine, IProcess process) {
		if (fgProcessToNewLines == null) {
			fgProcessToNewLines = new HashMap();
		}
		List newLines = (List)fgProcessToNewLines.get(process);
		if (newLines == null) {
			newLines= new ArrayList();
		}
	
		newLines.add(new LineEntry(console, newLine));
		fgProcessToNewLines.put(process, newLines);
	}

	/**
	 * Disposes any information stored for the given process.
	 * 
	 * @param process
	 */
	public static void dispose(IProcess process) {
		if (fgProcessToLinks != null) {
			fgProcessToLinks.remove(process);
		}
		if (fgProcessToNewLines != null) {
			fgProcessToNewLines.remove(process);
		}
		if (fgAntBuilds != null) {
			fgAntBuilds.remove(process);
		}
	}
	
	/**
	 * Registers the specified process as an Ant build process.
	 * Allows for the generation of the "Buildfile: somefile" link in the 
	 * Ant output.
	 * 
	 * @param process
	 */
	public static synchronized void registerAntBuild(IProcess process) {
		if (fgProcessToNewLines != null) {
			List newLines = (List)fgProcessToNewLines.get(process);
			if (newLines != null) {
				for (Iterator iter = newLines.iterator(); iter.hasNext();) {
					LineEntry newLine = (LineEntry) iter.next();
					if (linkBuildFileMessage(newLine.getConsole(), newLine.getRegion())){
						iter.remove();
						return;
					}
				}
			}
		}
		if (fgAntBuilds == null) {
			fgAntBuilds= new ArrayList();
		}
		fgAntBuilds.add(process);
	}
	
	private static boolean linkBuildFileMessage(IConsole console, IRegion region) {
		
		String message= ""; //$NON-NLS-1$
		int offset= region.getOffset();
		try {
			message = console.getDocument().get(offset, region.getLength());
		} catch (BadLocationException e) {
		}
		if (message.startsWith("Buildfile:")) { //$NON-NLS-1$
			String fileName = message.substring(10).trim();
			IFile file = AntUtil.getFileForLocation(fileName, null);
			if (file != null) {
				FileLink link = new FileLink(file, null,  -1, -1, -1);
				console.addLink(link, offset + 11, fileName.length()); //$NON-NLS-1$
				//fBuildFileParent= file.getLocation().toFile().getParentFile();
				return true;
			}
		}
		return false;
	}
}
