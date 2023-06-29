/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.actions.ActionMessages;
import org.eclipse.debug.internal.ui.actions.ToggleBreakpointsTargetManager;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.IUpdate;

/**
 * Action to toggle a breakpoint in a vertical ruler of a workbench part
 * containing a document. The part must provide an <code>IToggleBreakpointsTarget</code>
 * adapter which may optionally be an instance of an
 * <code>IToggleBreakpointsTargetExtension</code>.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.1
 * @see org.eclipse.debug.ui.actions.RulerToggleBreakpointActionDelegate
 * @noextend This class is not intended to be subclassed by clients.
 */
public class ToggleBreakpointAction extends Action implements IUpdate {

	private IWorkbenchPart fPart;
	private IDocument fDocument;
	private IVerticalRulerInfo fRulerInfo;
	private IToggleBreakpointsTargetManagerListener fListener = this::update;

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
	public ToggleBreakpointAction(IWorkbenchPart part, IDocument document, IVerticalRulerInfo rulerInfo) {
		super(ActionMessages.ToggleBreakpointAction_0 + '\t' + ActionMessages.ToggleBreakpointAction_3);
		fPart = part;
		fDocument = document;
		fRulerInfo = rulerInfo;
		DebugUITools.getToggleBreakpointsTargetManager().addChangedListener(fListener);
	}

	@Override
	public void run() {
		doIt(null);
	}

	@Override
	public void runWithEvent(Event event) {
		doIt(event);
	}

	/**
	 * Delegate method to perform the toggling
	 * @param event the event, possibly <code>null</code>
	 *
	 * @since 3.8
	 */
	void doIt(Event event) {
		IDocument document= getDocument();
		if (document != null) {
			int line = fRulerInfo.getLineOfLastMouseButtonActivity();
			if(line > -1) {
				try {
					ITextSelection selection = getTextSelection(document, line);
					IToggleBreakpointsTarget target = DebugUITools.getToggleBreakpointsTargetManager().getToggleBreakpointsTarget(fPart, selection);
					if (target != null) {
						IToggleBreakpointsTargetExtension2 ext = (IToggleBreakpointsTargetExtension2)
								DebugPlugin.getAdapter(target, IToggleBreakpointsTargetExtension2.class);
						if (ext != null) {
							if(ext.canToggleBreakpointsWithEvent(fPart, selection, event)) {
								ext.toggleBreakpointsWithEvent(fPart, selection, event);
								return;
							}
						}
						IToggleBreakpointsTargetExtension ext2 = (IToggleBreakpointsTargetExtension)
								DebugPlugin.getAdapter(target, IToggleBreakpointsTargetExtension.class);
						if(ext2 != null) {
							if (ext2.canToggleBreakpoints(fPart, selection)) {
								ext2.toggleBreakpoints(fPart, selection);
								return;
							}
						}
						if (target.canToggleLineBreakpoints(fPart, selection)) {
							target.toggleLineBreakpoints(fPart, selection);
						} else if (target.canToggleWatchpoints(fPart, selection)) {
							target.toggleWatchpoints(fPart, selection);
						} else if (target.canToggleMethodBreakpoints(fPart, selection)) {
							target.toggleMethodBreakpoints(fPart, selection);
						}
					}
				}
				catch(BadLocationException ble) {
					reportException(ble);
				}
				catch(CoreException ce) {
					reportException(ce);
				}
			}
		}
	}

	/**
	 * Report an error to the user.
	 *
	 * @param e underlying exception
	 */
	private void reportException(Exception e) {
		DebugUIPlugin.log(e);
		DebugUIPlugin.errorDialog(fPart.getSite().getShell(), ActionMessages.ToggleBreakpointAction_1, ActionMessages.ToggleBreakpointAction_2, e);
	}

	/**
	 * Disposes this action. Clients must call this method when
	 * this action is no longer needed.
	 */
	public void dispose() {
		fDocument = null;
		fPart = null;
		fRulerInfo = null;
		DebugUITools.getToggleBreakpointsTargetManager().removeChangedListener(fListener);
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

		if (fPart == null) {
			return null;
		}

		if (fPart instanceof ITextEditor) {
			ITextEditor editor= (ITextEditor)fPart;
			IDocumentProvider provider = editor.getDocumentProvider();
			if (provider != null) {
				return provider.getDocument(editor.getEditorInput());
			}
		}

		IDocument doc = fPart.getAdapter(IDocument.class);
		if (doc != null) {
			return doc;
		}

		return null;
	}

	@Override
	public void update() {
		IDocument document= getDocument();
		if (document != null) {
			int line = fRulerInfo.getLineOfLastMouseButtonActivity();
			if (line > -1) {
				try {
					ITextSelection selection = getTextSelection(document, line);

					IToggleBreakpointsTarget adapter =
						ToggleBreakpointsTargetManager.getDefault().getToggleBreakpointsTarget(fPart, selection);
					if (adapter == null) {
						setEnabled(false);
						return;
					}
					if (adapter instanceof IToggleBreakpointsTargetExtension) {
						IToggleBreakpointsTargetExtension extension = (IToggleBreakpointsTargetExtension) adapter;
						if (extension.canToggleBreakpoints(fPart, selection)) {
							setEnabled(true);
							return;
						}
					}
					if (adapter.canToggleLineBreakpoints(fPart, selection) ||
						adapter.canToggleWatchpoints(fPart, selection) ||
						adapter.canToggleMethodBreakpoints(fPart, selection))
					{
						setEnabled(true);
						return;
					}
				} catch (BadLocationException e) {
					reportException(e);
				}
			}
		}
		setEnabled(false);
	}

	/**
	 * Determines the text selection for the breakpoint action.  If clicking on the ruler inside
	 * the highlighted text, return the text selection for the highlighted text.  Otherwise,
	 * return a text selection representing the start of the line.
	 *
	 * @param document	The IDocument backing the Editor.
	 * @param line	The line clicked on in the ruler.
	 * @return	An ITextSelection as described.
	 * @throws BadLocationException	If underlying operations throw.
	 */
	private ITextSelection getTextSelection(IDocument document, int line) throws BadLocationException {
		IRegion region = document.getLineInformation(line);
		ITextSelection textSelection = new TextSelection(document, region.getOffset(), 0);
		ISelectionProvider provider = fPart.getSite().getSelectionProvider();
		if (provider != null){
			ISelection selection = provider.getSelection();
			if (selection instanceof ITextSelection
					&& ((ITextSelection) selection).getStartLine() <= line
					&& ((ITextSelection) selection).getEndLine() >= line) {
				textSelection = (ITextSelection) selection;
			}
		}
		return textSelection;
	}

}
