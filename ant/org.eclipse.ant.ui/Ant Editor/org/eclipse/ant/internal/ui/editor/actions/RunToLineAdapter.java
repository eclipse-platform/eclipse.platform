/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.editor.actions;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.ant.internal.launching.debug.IAntDebugConstants;
import org.eclipse.ant.internal.launching.debug.model.AntDebugElement;
import org.eclipse.ant.internal.launching.debug.model.AntLineBreakpoint;
import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.debug.ui.actions.RunToLineHandler;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Run to line target for the Ant debugger
 */
public class RunToLineAdapter implements IRunToLineTarget {
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.actions.IRunToLineTarget#runToLine(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection, org.eclipse.debug.core.model.ISuspendResume)
	 */
	public void runToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) throws CoreException {
		IEditorPart editorPart = (IEditorPart)part;
		IEditorInput input = editorPart.getEditorInput();
		String errorMessage = null;
		if (input == null) {
			errorMessage = AntEditorActionMessages.getString("RunToLineAdapter.0"); //$NON-NLS-1$
		} else {
			ITextEditor textEditor = (ITextEditor)editorPart;
			IDocumentProvider provider= textEditor.getDocumentProvider();
			IDocument document= provider.getDocument(input);
			
			if (document == null) {
				errorMessage = AntEditorActionMessages.getString("RunToLineAdapter.1"); //$NON-NLS-1$
			} else {
				
				ITextSelection textSelection = (ITextSelection) selection;
				int lineNumber = textSelection.getStartLine() + 1;
				
				IBreakpoint breakpoint= null;
				Map attributes = getRunToLineAttributes();
				IFile file = (IFile)input.getAdapter(IFile.class);
				if (file == null) {
				    errorMessage= AntEditorActionMessages.getString("RunToLineAdapter.2"); //$NON-NLS-1$
				} else {
				    breakpoint= new AntLineBreakpoint(file, lineNumber, attributes, false);
                    breakpoint.setPersisted(false);
				    errorMessage = AntEditorActionMessages.getString("RunToLineAdapter.3"); //$NON-NLS-1$
				    if (target instanceof IAdaptable) {
				        IDebugTarget debugTarget = (IDebugTarget) ((IAdaptable)target).getAdapter(IDebugTarget.class);
				        if (debugTarget != null) {
				            RunToLineHandler handler = new RunToLineHandler(debugTarget, target, breakpoint);
                            handler.run(new NullProgressMonitor());
				            return;
				        }
				    }
				}
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, AntUIPlugin.getUniqueIdentifier(), AntUIPlugin.INTERNAL_ERROR,
				errorMessage, null));
	}

    private Map getRunToLineAttributes() {
        Map attributes = new HashMap();
        attributes.put(IMarker.TRANSIENT, Boolean.TRUE);
        attributes.put(IAntDebugConstants.ANT_RUN_TO_LINE, Boolean.TRUE);
        return attributes;
    }

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.actions.IRunToLineTarget#canRunToLine(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection, org.eclipse.debug.core.model.ISuspendResume)
	 */
	public boolean canRunToLine(IWorkbenchPart part, ISelection selection, ISuspendResume target) {
	    return target instanceof AntDebugElement;
	}
}
