/*******************************************************************************
 * Copyright (c) 2017, 2018 IBM Corporation and others.
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
 *     Wind River Systems - added support for IToggleBreakpointsTargetFactory
 *******************************************************************************/
package org.eclipse.debug.ui.actions;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.Platform;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.ISuspendResume;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextManager;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to run to line in a vertical ruler of a workbench part containing a
 * document. The part must provide an <code>IRunToLineTarget</code> adapter and
 * <code>ISuspendResume</code> adapter.
 * <p>
 * Clients may instantiate this class.
 * </p>
 *
 * @since 3.12
 * @see org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate
 * @noextend This class is not intended to be subclassed by clients.
 */
public class RunToLineAction extends Action implements IUpdate {

	private IWorkbenchPart fActivePart = null;
	private IRunToLineTarget fPartTarget = null;
	private DebugContextListener fContextListener = new DebugContextListener();
	private ISuspendResume fTargetElement = null;
	private IDocument fDocument;
	private IVerticalRulerInfo fRulerInfo;

	class DebugContextListener implements IDebugContextListener {

		protected void contextActivated(ISelection selection) {
			fTargetElement = null;
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (ss.size() == 1) {
					fTargetElement = (ISuspendResume) DebugPlugin.getAdapter(ss.getFirstElement(), ISuspendResume.class);
				}
			}
			update();
		}

		@Override
		public void debugContextChanged(DebugContextEvent event) {
			contextActivated(event.getContext());
		}

	}

	/**
	 * Constructs a new action to toggle a breakpoint in the given
	 * part containing the given document and ruler.
	 *
	 * @param part the part in which to toggle the breakpoint - provides
	 *  an <code>IToggleBreakpointsTarget</code> adapter
	 * @param document the document breakpoints are being set in or
	 * <code>null</code> when the document should be derived from the
	 * given part
	 * @param rulerInfo specifies location the user has double-clicked
	 */
	public RunToLineAction(IWorkbenchPart part, IDocument document, IVerticalRulerInfo rulerInfo) {
		super(ActionMessages.RunToLineAction_2 + '\t' + DebugUIPlugin.formatKeyBindingString(SWT.MOD1 + SWT.MOD3, ActionMessages.RunToLineAction_3));
		fActivePart = part;
		fDocument = document;
		fRulerInfo = rulerInfo;
		initializeListeners();
	}

	@Override
	public void run() {
		doIt();
	}

	@Override
	public void runWithEvent(Event event) {
		if (event.widget instanceof MenuItem) {
			doIt();
		} else {
			// Even comes here on RulerClick, perform Run to Line if Ctrl Alt
			// Click was used
			if ((event.stateMask & SWT.MOD1) > 0 && (event.stateMask & SWT.MOD3) > 0) {
				doIt();
			}
		}
	}

	void doIt() {
		if (fPartTarget != null && fTargetElement != null) {
			try {
				IDocument document= getDocument();
				if (document != null) {
					int line = fRulerInfo.getLineOfLastMouseButtonActivity();
					if (line > -1) {
						ITextSelection selection = getTextSelection(document, line);
						fPartTarget.runToLine(fActivePart, selection, fTargetElement);
					}
				}
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(fActivePart.getSite().getWorkbenchWindow().getShell(), ActionMessages.RunToLineAction_0, ActionMessages.RunToLineAction_1, e.getStatus()); //
			} catch (BadLocationException e) {
				// ignore
			}
		}
	}


	/**
	 * Disposes this action. Clients must call this method when
	 * this action is no longer needed.
	 */
	public void dispose() {
		IDebugContextManager manager = DebugUITools.getDebugContextManager();
		if (fActivePart != null) {
			IWorkbenchWindow ww = fActivePart.getSite().getWorkbenchWindow();
			if (ww != null) {
				manager.getContextService(ww).removeDebugContextListener(fContextListener);
			} else {
				manager.removeDebugContextListener(fContextListener);
			}
		}
		fActivePart = null;
		fTargetElement = null;
		fPartTarget = null;

	}

	@Override
	public void update() {
		Runnable r = () -> setEnabled(isTargetEnabled());
		DebugUIPlugin.getStandardDisplay().asyncExec(r);
	}

	private boolean isTargetEnabled() {
		boolean enabled = false;
		if (fPartTarget != null && fTargetElement != null) {
			IWorkbenchPartSite site = fActivePart.getSite();
			if (site != null) {
				ISelectionProvider selectionProvider = site.getSelectionProvider();
				if (selectionProvider != null) {
					ISelection selection = selectionProvider.getSelection();
					enabled = fTargetElement.isSuspended() && fPartTarget.canRunToLine(fActivePart, selection, fTargetElement);
				}
			}
		}
		return enabled;
	}

	private void initializeListeners() {
		IDebugContextManager manager = DebugUITools.getDebugContextManager();
		IWorkbenchWindow workbenchWindow = fActivePart.getSite().getWorkbenchWindow();
		IDebugContextService service = manager.getContextService(workbenchWindow);
		service.addDebugContextListener(fContextListener);
		fPartTarget = fActivePart.getAdapter(IRunToLineTarget.class);
		if (fPartTarget == null) {
			IAdapterManager adapterManager = Platform.getAdapterManager();
			if (adapterManager.hasAdapter(fActivePart, IRunToLineTarget.class.getName())) {
				fPartTarget = (IRunToLineTarget) adapterManager.loadAdapter(fActivePart, IRunToLineTarget.class.getName());
			}
		}
		ISelection activeContext = service.getActiveContext();
		fContextListener.contextActivated(activeContext);

	}

	/**
	 * Returns the document on which this action operates.
	 *
	 * @return the document or <code>null</code> if none
	 */

	private IDocument getDocument() {
		if (fDocument != null) {
			return fDocument;
		}
		if (fActivePart instanceof ITextEditor) {
			ITextEditor editor = (ITextEditor) fActivePart;
			IDocumentProvider provider = editor.getDocumentProvider();
			if (provider != null) {
				return provider.getDocument(editor.getEditorInput());
			}
		}
		IDocument doc = fActivePart.getAdapter(IDocument.class);
		if (doc != null) {
			return doc;
		}
		return null;
	}

	/**
	 * Determines the text selection for the ruler action. If clicking on the
	 * ruler inside the highlighted text, return the text selection for the
	 * highlighted text. Otherwise, return a text selection representing the
	 * start of the line.
	 *
	 * @param document The IDocument backing the Editor.
	 * @param line The line clicked on in the ruler.
	 * @return An ITextSelection as described.
	 * @throws BadLocationException If underlying operations throw.
	 */
	private ITextSelection getTextSelection(IDocument document, int line) throws BadLocationException {
		IRegion region = document.getLineInformation(line);
		ITextSelection textSelection = new TextSelection(document, region.getOffset(), 0);
		ISelectionProvider provider = fActivePart.getSite().getSelectionProvider();
		if (provider != null) {
			ISelection selection = provider.getSelection();
			if (selection instanceof ITextSelection && ((ITextSelection) selection).getStartLine() <= line && ((ITextSelection) selection).getEndLine() >= line) {
				textSelection = (ITextSelection) selection;
			}
		}
		return textSelection;
	}

}
