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
package org.eclipse.debug.internal.ui;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPageListener;
import org.eclipse.ui.IPartListener2;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * This class tracks instruction pointer contexts for all active debug targets and threads
 * in the current workbench.  There should only ever be one instance of this class, obtained
 * via 'getDefault()'.
 */
public class InstructionPointerManager{

	/**
	 * The singleton instance of this class.
	 */
	private static InstructionPointerManager fgDefault;

	/**
	 * Set containing all instruction pointer contexts this class manages
	 */
	private Set<InstructionPointerContext> fIPCSet = new HashSet<>();

	/**
	 * Maps ITextEditors to the set of instruction pointer contexts that are displayed in the editor
	 */
	private Map<ITextEditor, Set<InstructionPointerContext>> fEditorMap = new HashMap<>();

	/**
	 * Part listener added to editors that contain annotations.  Allows instruction pointer contexts to
	 * be removed when the editor they are displayed in is removed.
	 */
	private IPartListener2 fPartListener;

	/**
	 * Page listener added to the workbench window to remove part listeners when the page is closed.
	 */
	private IPageListener fPageListener;

	/**
	 * Clients must not instantiate this class.
	 */
	private InstructionPointerManager() {
	}

	/**
	 * Return the singleton instance of this class, creating it if necessary.
	 */
	public static InstructionPointerManager getDefault() {
		if (fgDefault == null) {
			fgDefault = new InstructionPointerManager();
		}
		return fgDefault;
	}

	/**
	 * Adds an instruction pointer annotation in the specified editor for the
	 * specified stack frame.
	 */
	public void addAnnotation(ITextEditor textEditor, IStackFrame frame, Annotation annotation) {
		IDocumentProvider docProvider = textEditor.getDocumentProvider();
		if (docProvider == null) {
			return;
		}
		IEditorInput editorInput = textEditor.getEditorInput();
		// If there is no annotation model, there's nothing more to do
		IAnnotationModel annModel = docProvider.getAnnotationModel(editorInput);
		if (annModel == null) {
			return;
		}

		// Create the Position object that specifies a location for the annotation
		Position position = null;
		int charStart = -1;
		int length = -1;
		try {
			charStart = frame.getCharStart();
			length = frame.getCharEnd() - charStart;
		} catch (DebugException de) {
		}
		if (charStart < 0) {
			IDocument doc = docProvider.getDocument(editorInput);
			if (doc == null) {
				return;
			}
			try {
				int lineNumber = frame.getLineNumber() - 1;
				IRegion region = doc.getLineInformation(lineNumber);
				charStart = region.getOffset();
				length = region.getLength();
			} catch (BadLocationException ble) {
				return;
			} catch (DebugException de) {
				return;
			}
		}
		if (charStart < 0) {
			return;
		}
		position = new Position(charStart, length);

		if (frame.isTerminated()) {
			return;
		}

		synchronized (fIPCSet) {

			// Add the annotation at the position to the editor's annotation model.
			annModel.removeAnnotation(annotation);
			annModel.addAnnotation(annotation, position);

			// Create the instruction pointer context
			InstructionPointerContext ipc = new InstructionPointerContext(frame.getDebugTarget(), frame.getThread(), textEditor, annotation);

			// Add the IPC to the set and map
			Set<InstructionPointerContext> editorIPCs = fEditorMap.get(textEditor);
			if (editorIPCs == null){
				editorIPCs = new HashSet<>();
				fEditorMap.put(textEditor, editorIPCs);
			} else {
				editorIPCs.remove(ipc);
			}
			editorIPCs.add(ipc);
			fIPCSet.remove(ipc);
			fIPCSet.add(ipc);

			// Add a listener to the editor so we can remove the IPC when the editor is closed
			textEditor.getSite().getPage().addPartListener(getPartListener());
			textEditor.getSite().getPage().getWorkbenchWindow().addPageListener(getPageListener());
		}
	}

	/**
	 * Remove all annotations associated with the specified debug target that this class
	 * is tracking.
	 */
	public void removeAnnotations(IDebugTarget debugTarget) {
		synchronized (fIPCSet) {
			Iterator<InstructionPointerContext> ipcIter = fIPCSet.iterator();
			while (ipcIter.hasNext()) {
				InstructionPointerContext currentIPC = ipcIter.next();
				if (currentIPC.getDebugTarget().equals(debugTarget)){
					removeAnnotationFromModel(currentIPC);
					ipcIter.remove();
					removeAnnotationFromEditorMapping(currentIPC);
				}
			}
		}
	}

	/**
	 * Remove all annotations associated with the specified thread that this class
	 * is tracking.
	 */
	public void removeAnnotations(IThread thread) {
		synchronized (fIPCSet) {
			Iterator<InstructionPointerContext> ipcIter = fIPCSet.iterator();
			while (ipcIter.hasNext()) {
				InstructionPointerContext currentIPC = ipcIter.next();
				if (currentIPC.getThread().equals(thread)){
					removeAnnotationFromModel(currentIPC);
					ipcIter.remove();
					removeAnnotationFromEditorMapping(currentIPC);
				}
			}
		}
	}

	/**
	 * Remove all annotations associated with the specified editor that this class
	 * is tracking.
	 */
	public void removeAnnotations(ITextEditor editor) {
		synchronized (fIPCSet) {
			Set<InstructionPointerContext> editorIPCs = fEditorMap.get(editor);
			if (editorIPCs != null){
				Iterator<InstructionPointerContext> ipcIter = editorIPCs.iterator();
				while (ipcIter.hasNext()) {
					InstructionPointerContext currentIPC = ipcIter.next();
					removeAnnotationFromModel(currentIPC);
					fIPCSet.remove(currentIPC);
				}
				fEditorMap.remove(editor);
			}
		}
	}

	/**
	 * Remove the given ipc from the mapping of editors.
	 */
	private void removeAnnotationFromEditorMapping(InstructionPointerContext ipc) {
		Set<InstructionPointerContext> editorIPCs = fEditorMap.get(ipc.getEditor());
		if (editorIPCs != null){
			editorIPCs.remove(ipc);
			if (editorIPCs.isEmpty()){
				fEditorMap.remove(ipc.getEditor());
			}
		}

	}

	/**
	 * Remove the annotation from the document model.
	 */
	private void removeAnnotationFromModel(InstructionPointerContext ipc){
		IDocumentProvider docProvider = ipc.getEditor().getDocumentProvider();
		if (docProvider != null) {
			IAnnotationModel annotationModel = docProvider.getAnnotationModel(ipc.getEditor().getEditorInput());
			if (annotationModel != null) {
				annotationModel.removeAnnotation(ipc.getAnnotation());
			}
		}
	}

	/**
	 * Returns the number of instruction pointers.
	 * Used by the test suite.
	 *
	 * @return the number of instruction pointers
	 * @since 3.2
	 */
	public int getInstructionPointerCount() {
		return fIPCSet.size();
	}

	/**
	 * Returns the number of keys in the editor to IPC mapping
	 * Used by the test suite.
	 *
	 * @return the number of keys in the editor mapping
	 * @since 3.3
	 */
	public int getEditorMappingCount() {
		return fEditorMap.size();
	}

	/**
	 * @return the page listener to add to workbench window.
	 */
	private IPageListener getPageListener(){
		if (fPageListener == null){
			fPageListener = new PageListener();
		}
		return fPageListener;
	}

	/**
	 * @return the part listener to add to editors.
	 */
	private IPartListener2 getPartListener(){
		if (fPartListener == null){
			fPartListener = new PartListener();
		}
		return fPartListener;
	}

	/**
	 * Part listener that is added to editors to track when the editor is no longer is displaying
	 * the input containing instruction pointer annotations.
	 */
	class PartListener implements IPartListener2{
		@Override
		public void partActivated(IWorkbenchPartReference partRef) {}
		@Override
		public void partDeactivated(IWorkbenchPartReference partRef) {}
		@Override
		public void partHidden(IWorkbenchPartReference partRef) {}
		@Override
		public void partOpened(IWorkbenchPartReference partRef) {}
		@Override
		public void partVisible(IWorkbenchPartReference partRef) {}
		@Override
		public void partBroughtToTop(IWorkbenchPartReference partRef) {}

		@Override
		public void partClosed(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof ITextEditor){
				removeAnnotations((ITextEditor)part);
			}

		}

		@Override
		public void partInputChanged(IWorkbenchPartReference partRef) {
			IWorkbenchPart part = partRef.getPart(false);
			if (part instanceof ITextEditor){
				removeAnnotations((ITextEditor)part);
			}
		}
	}

	/**
	 * Page listener that is added to the workbench to remove the part listener when the page is closed.
	 */
	class PageListener implements IPageListener{

		@Override
		public void pageActivated(IWorkbenchPage page) {}
		@Override
		public void pageOpened(IWorkbenchPage page) {}

		@Override
		public void pageClosed(IWorkbenchPage page) {
			page.removePartListener(getPartListener());
			page.getWorkbenchWindow().removePageListener(getPageListener());
		}

	}

}
