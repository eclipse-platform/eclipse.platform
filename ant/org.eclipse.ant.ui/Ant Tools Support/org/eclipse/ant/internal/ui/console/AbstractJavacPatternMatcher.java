/*******************************************************************************
 * Copyright (c) 2005, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.console;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.IAntUIPreferenceConstants;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.IPatternMatchListenerDelegate;
import org.eclipse.ui.console.PatternMatchEvent;
import org.eclipse.ui.console.TextConsole;

public abstract class AbstractJavacPatternMatcher implements IPatternMatchListenerDelegate {

	protected TextConsole fConsole;
	private static Pattern fgLineNumberPattern = Pattern.compile("\\d+"); //$NON-NLS-1$
	private static List<AbstractJavacPatternMatcher> fgPatternMatchers = new ArrayList<>();
	private Map<String, IFile> fFileNameToIFile = new HashMap<>();

	private JavacMarkerCreator fMarkerCreator;
	protected static final Integer fgWarningType = Integer.valueOf(IMarker.SEVERITY_WARNING);
	protected static final Integer fgErrorType = Integer.valueOf(IMarker.SEVERITY_ERROR);

	@Override
	public void connect(TextConsole console) {
		fConsole = console;
		IPreferenceStore store = AntUIPlugin.getDefault().getPreferenceStore();
		if (store.getBoolean(IAntUIPreferenceConstants.ANT_CREATE_MARKERS)) {
			fMarkerCreator = new JavacMarkerCreator(fConsole, this instanceof EclipseJavacPatternMatcher);
			fgPatternMatchers.add(this);
		}
	}

	@Override
	public void disconnect() {
		fConsole = null;
		fFileNameToIFile.clear();
	}

	protected IFile getIFile(String filePath) {
		if (filePath == null) {
			return null;
		}
		IFile file = fFileNameToIFile.get(filePath);
		if (file == null) {
			IFile[] files = ResourcesPlugin.getWorkspace().getRoot().findFilesForLocationURI(IPath.fromOSString(filePath).toFile().toURI());
			if (files.length > 0) {
				file = files[0];
				fFileNameToIFile.put(filePath, file);
			}
		}
		return file;
	}

	protected String getMatchedText(PatternMatchEvent event) {
		int eventOffset = event.getOffset();
		int eventLength = event.getLength();
		IDocument document = fConsole.getDocument();
		String matchedText = null;
		try {
			matchedText = document.get(eventOffset, eventLength);
		}
		catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
		return matchedText;
	}

	protected int getLineNumber(int eventOffset, boolean sameLine) {
		IDocument document = fConsole.getDocument();
		try {
			int fileLine = document.getLineOfOffset(eventOffset);
			if (!sameLine) {
				fileLine += 1;
			}
			IRegion region = document.getLineInformation(fileLine);
			int regionLength = region.getLength();
			if (region.getOffset() != eventOffset) {
				regionLength = regionLength - (eventOffset - region.getOffset());
			}

			String lineLine = document.get(eventOffset, regionLength);
			Matcher matcher = null;
			synchronized (fgLineNumberPattern) {
				matcher = fgLineNumberPattern.matcher(lineLine);
			}
			if (matcher.find()) {
				String lineString = matcher.group();
				return Integer.parseInt(lineString);
			}

		}
		catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
		catch (NumberFormatException e) {
			AntUIPlugin.log(e);
		}
		return -1;
	}

	protected void addLink(String filePath, int lineNumber, int offset, int length, Integer type) {
		IFile file = getIFile(filePath);
		if (file == null) {
			return;
		}

		if (fMarkerCreator != null) {
			if (type == null) { // match for listfiles
				fMarkerCreator.addFileToBeCleaned(file);
			} else { // match for error or warning
				fMarkerCreator.addMarker(file, lineNumber, offset, type);
			}
		}

		FileLink link = new FileLink(file, null, -1, -1, lineNumber);
		try {
			fConsole.addHyperlink(link, offset, length);
		}
		catch (BadLocationException e) {
			AntUIPlugin.log(e);
		}
	}

	public static void consoleClosed(IProcess process) {
		for (AbstractJavacPatternMatcher matcher : fgPatternMatchers) {
			matcher.finished(process);
		}
	}

	protected void finished(IProcess process) {
		if (fMarkerCreator != null) {
			fMarkerCreator.finished(process);
			fgPatternMatchers.remove(this);
		}
	}
}