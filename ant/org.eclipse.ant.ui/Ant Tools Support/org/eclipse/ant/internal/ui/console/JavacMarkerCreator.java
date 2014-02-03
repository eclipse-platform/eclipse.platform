/*******************************************************************************
 * Copyright (c) 2006, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.console;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
import org.eclipse.core.runtime.IProgressMonitor;
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
	private static List fgFilesToBeCleaned= new ArrayList();
	private Map fFileToMarkerInfo= new HashMap();
	private final boolean fUseCustomMessage;

	private class MarkerInfo {
		
		public int fLineNumber;
		public int fOffset;
		public Integer fType;

		public void setLineNumber(int lineNumber) {
			fLineNumber= lineNumber;
		}
		public void setOffset(int offset) {
			fOffset = offset;
		}
		public void setType(Integer type) {
			fType= type;
			
		}
	}
	public JavacMarkerCreator(TextConsole console, boolean useCustomMessage) {
		fConsole = console;
		fUseCustomMessage = useCustomMessage;
		if (fConsole instanceof ProcessConsole) {
			fProcess= ((ProcessConsole) fConsole).getProcess();
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
		} catch (CoreException e) {
			AntUIPlugin.log(e.getStatus());
		}			
	}

	protected void addFileToBeCleaned(IFile file) {
		fgFilesToBeCleaned.add(file);
	}
	
	protected void addMarker(IFile file, int lineNumber, int offset, Integer type) {
		MarkerInfo info= new MarkerInfo();
		info.setLineNumber(lineNumber);
		info.setOffset(offset);
		info.setType(type);
		List infos= (List) fFileToMarkerInfo.get(file);
		if (infos == null) {
			infos= new ArrayList();
			fFileToMarkerInfo.put(file, infos);
		}
		infos.add(info);
	}
	
	private void createMarkers(final IFile file, final List infos) {
		IWorkspaceRunnable wr= new IWorkspaceRunnable() {
			public void run(IProgressMonitor monitor) throws CoreException {
				
				try {
					for (Iterator iter = infos.iterator(); iter.hasNext();) {
						MarkerInfo info = (MarkerInfo) iter.next();
						IMarker marker= file.createMarker(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER);
						Map attributes= new HashMap(3);
						attributes.put(IMarker.LINE_NUMBER, new Integer(info.fLineNumber));
						String message= getMessage(info);
						attributes.put(IMarker.MESSAGE, message);
						attributes.put(IMarker.SEVERITY, info.fType);
						marker.setAttributes(attributes);
					}
				} catch (CoreException e) {
					AntUIPlugin.log(e.getStatus());
				}	
			}
		};
		run(getMarkerRule(file), wr);
	}

	protected String getMessage(MarkerInfo info) {
		IDocument doc= fConsole.getDocument();
		String message= ConsoleMessages.JavacMarkerCreator_0;
		if (fUseCustomMessage) {
			FindReplaceDocumentAdapter adapter= new FindReplaceDocumentAdapter(doc);
			try {
				IRegion match=adapter.find(info.fOffset, "[javac] ----------", true, false, false, false); //$NON-NLS-1$
				if (match != null) {
					match= adapter.find(match.getOffset(), "[javac]", false, false, false, false); //$NON-NLS-1$
					if (match != null) {
						int start= match.getOffset() + match.getLength() + 1;
						IRegion lineInfo= doc.getLineInformationOfOffset(start);
						message= doc.get(start, lineInfo.getOffset() - start + lineInfo.getLength());
					}
				}
			} catch (BadLocationException e) {
				AntUIPlugin.log(e);
			}
		}
		
		return message;
	}

	protected void finished(IProcess process) {
		if (process.equals(fProcess)) {
			if (!fgFilesToBeCleaned.isEmpty()) {
				Iterator itr= fgFilesToBeCleaned.iterator();
				while (itr.hasNext()) {
					IFile file = (IFile) itr.next();
					try {
						file.deleteMarkers(IJavaModelMarker.JAVA_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_INFINITE);
					} catch (CoreException e) {
						AntUIPlugin.log(e.getStatus());
					}					
				}
			}
			if (!fFileToMarkerInfo.isEmpty()) {
				//generate markers for all the warnings and errors
				Iterator files= fFileToMarkerInfo.keySet().iterator();
				while (files.hasNext()) {
					IFile file = (IFile) files.next();
					List infos= (List) fFileToMarkerInfo.get(file);
					createMarkers(file, infos);	
				}	
			}
			
			fFileToMarkerInfo.clear();
			fgFilesToBeCleaned.clear();
		}
	}
}