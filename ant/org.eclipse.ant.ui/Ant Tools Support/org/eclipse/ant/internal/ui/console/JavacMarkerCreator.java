/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceRuleFactory;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.internal.ui.views.console.ProcessConsole;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.ui.console.TextConsole;

public class JavacMarkerCreator {

	private final TextConsole fConsole;
	private IProcess fProcess;
	private static List<IFile> fgFilesToBeCleaned = new ArrayList<>();
	private final Map<IFile, List<MarkerInfo>> fFileToMarkerInfo = new HashMap<>();
	private final boolean fUseCustomMessage;

	private static class MarkerInfo {

		public int fLineNumber;
		public int fOffset;
		public Integer fType;

		public void setLineNumber(int lineNumber) {
			fLineNumber = lineNumber;
		}

		public void setOffset(int offset) {
			fOffset = offset;
		}

		public void setType(Integer type) {
			fType = type;

		}
	}

	public JavacMarkerCreator(TextConsole console, boolean useCustomMessage) {
		fConsole = console;
		fUseCustomMessage = useCustomMessage;
		if (fConsole instanceof ProcessConsole) {
			fProcess = ((ProcessConsole) fConsole).getProcess();
		}
	}

	protected ISchedulingRule getMarkerRule(IResource resource) {
		ISchedulingRule rule = null;
		if (resource != null) {
			IResourceRuleFactory ruleFactory = ResourcesPlugin.getWorkspace().getRuleFactory();
			rule = ruleFactory.markerRule(resource);
		}
		return rule;
	}

	protected void run(ISchedulingRule rule, IWorkspaceRunnable wr) {
		try {
			ResourcesPlugin.getWorkspace().run(wr, rule, 0, null);
		}
		catch (CoreException e) {
			AntUIPlugin.log(e.getStatus());
		}
	}

	protected void addFileToBeCleaned(IFile file) {
		fgFilesToBeCleaned.add(file);
	}

	protected void addMarker(IFile file, int lineNumber, int offset, Integer type) {
		MarkerInfo info = new MarkerInfo();
		info.setLineNumber(lineNumber);
		info.setOffset(offset);
		info.setType(type);
		List<MarkerInfo> infos = fFileToMarkerInfo.get(file);
		if (infos == null) {
			infos = new ArrayList<>();
			fFileToMarkerInfo.put(file, infos);
		}
		infos.add(info);
	}

	private void createMarkers(final IFile file, final List<MarkerInfo> infos) {
		IWorkspaceRunnable wr = monitor -> {

			try {
				for (MarkerInfo info : infos) {
					IMarker marker = file.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
					Map<String, Object> attributes = new HashMap<>(3);
					attributes.put(IMarker.LINE_NUMBER, Integer.valueOf(info.fLineNumber));
					String message = getMessage(info);
					attributes.put(IMarker.MESSAGE, message);
					attributes.put(IMarker.SEVERITY, info.fType);
					marker.setAttributes(attributes);
				}
			}
			catch (CoreException e) {
				AntUIPlugin.log(e.getStatus());
			}
		};
		run(getMarkerRule(file), wr);
	}

	protected String getMessage(MarkerInfo info) {
		IDocument doc = fConsole.getDocument();
		String message = ConsoleMessages.JavacMarkerCreator_0;
		if (fUseCustomMessage) {
			FindReplaceDocumentAdapter adapter = new FindReplaceDocumentAdapter(doc);
			try {
				IRegion match = adapter.find(info.fOffset, "[javac] ----------", true, false, false, false); //$NON-NLS-1$
				if (match != null) {
					match = adapter.find(match.getOffset(), "[javac]", false, false, false, false); //$NON-NLS-1$
					if (match != null) {
						int start = match.getOffset() + match.getLength() + 1;
						IRegion lineInfo = doc.getLineInformationOfOffset(start);
						message = doc.get(start, lineInfo.getOffset() - start + lineInfo.getLength());
					}
				}
			}
			catch (BadLocationException e) {
				AntUIPlugin.log(e);
			}
		}

		return message;
	}

	protected void finished(IProcess process) {
		if (process.equals(fProcess)) {
			for (IFile file : fgFilesToBeCleaned) {
				try {
					file.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
				}
				catch (CoreException e) {
					AntUIPlugin.log(e.getStatus());
				}
			}
			for (IFile file : fFileToMarkerInfo.keySet()) {
				createMarkers(file, fFileToMarkerInfo.get(file));
			}
			fFileToMarkerInfo.clear();
			fgFilesToBeCleaned.clear();
		}
	}
}