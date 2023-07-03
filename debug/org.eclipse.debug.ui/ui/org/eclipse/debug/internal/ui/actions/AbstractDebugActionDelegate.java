/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.actions;


import java.util.Iterator;

import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchWindow;

/**
 * This class is an abstract implementation of common features for a debug <code>IViewActionDelegate</code>
 *
 * This class is intended to be extended by clients
 * @see IViewActionDelegate
 * @see IActionDelegate2
 */
public abstract class AbstractDebugActionDelegate implements IViewActionDelegate, IActionDelegate2 {

	/**
	 * The underlying action for this delegate
	 */
	private IAction fAction;
	/**
	 * This action's view part, or <code>null</code>
	 * if not installed in a view.
	 */
	private IViewPart fViewPart;

	/**
	 * Cache of the most recent selection
	 */
	private IStructuredSelection fSelection = StructuredSelection.EMPTY;

	/**
	 * Whether this delegate has been initialized
	 */
	private boolean fInitialized = false;

	/**
	 * Whether this delegate was started with Shift pressed
	 */
	private boolean fIsShift = false;

	/**
	 * It's crucial that delegate actions have a zero-argument constructor so
	 * that they can be reflected into existence when referenced in an action
	 * set in the plugin's plugin.xml file.
	 */
	public AbstractDebugActionDelegate() {}

	@Override
	public void dispose(){
		fSelection= null;
	}

	@Override
	public void run(IAction action){
		if (action.isEnabled()) {
			IStructuredSelection selection = getSelection();
			// disable the action so it cannot be run again until an event or selection change
			// updates the enablement
			action.setEnabled(false);
			runInForeground(selection, false);
		}
	}

	/**
	 * Runs this action in the UI thread.
	 * @param selection the current selection
	 */
	private void runInForeground(final IStructuredSelection selection, boolean isShift) {
		fIsShift = isShift;
		final MultiStatus status=
			new MultiStatus(DebugUIPlugin.getUniqueIdentifier(), DebugException.REQUEST_FAILED, getStatusMessage(), null);
		BusyIndicator.showWhile(Display.getCurrent(), () -> {
			Iterator<?> selectionIter = selection.iterator();
			while (selectionIter.hasNext()) {
				Object element = selectionIter.next();
				try {
					// Action's enablement could have been changed since
					// it was last enabled. Check that the action is still
					// enabled before running the action.
					if (isEnabledFor(element)) {
						doAction(element);
					}
				} catch (DebugException e) {
					status.merge(e.getStatus());
				}
			}
		});
		reportErrors(status);
	}

	/**
	 * Reports the specified <code>MultiStatus</code> in an error dialog
	 * @param ms the specified <code>MultiStatus</code>
	 */
	private void reportErrors(final MultiStatus ms) {
		if (!ms.isOK()) {
			IWorkbenchWindow window= DebugUIPlugin.getActiveWorkbenchWindow();
			if (window != null) {
				DebugUIPlugin.errorDialog(window.getShell(), ActionMessages.AbstractDebugActionDelegate_0, getErrorDialogMessage(), ms);
			} else {
				DebugUIPlugin.log(ms);
			}
		}
	}

	@Override
	public void selectionChanged(IAction action, ISelection s) {
		boolean wasInitialized= initialize(action, s);
		if (!wasInitialized) {
			if (getView() != null) {
				update(action, s);
			}
		}
	}

	/**
	 * Updates the specified selection based on the selection, as well as setting the selection
	 * for this action
	 * @param action the action to update
	 * @param s the selection
	 */
	protected void update(IAction action, ISelection s) {
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = getTargetSelection((IStructuredSelection)s);
			action.setEnabled(getEnableStateForSelection(ss));
			setSelection(ss);
		} else {
			action.setEnabled(false);
			setSelection(StructuredSelection.EMPTY);
		}
	}

	/**
	 * Returns a selection this operation should act on based on the given selection.
	 * Provides an opportunity for actions to translate the selection/targets of the
	 * operation.
	 * <p>
	 * By default, the original selection is returned. Subclasses may override.
	 * </p>
	 * @param s selection
	 * @return selection to operate on
	 * @since 3.6
	 */
	protected IStructuredSelection getTargetSelection(IStructuredSelection s) {
		return s;
	}

	/**
	 * Performs the specific action on this element.
	 * @param element the element context to perform the action on
	 * @throws DebugException if an exception occurs
	 */
	protected abstract void doAction(Object element) throws DebugException;

	/**
	 * Returns the String to use as an error dialog message for
	 * a failed action. This message appears as the "Message:" in
	 * the error dialog for this action.
	 * Default is to return null.
	 * @return the message to be displayed in the an error dialog
	 */
	protected String getErrorDialogMessage(){
		return null;
	}
	/**
	 * Returns the String to use as a status message for
	 * a failed action. This message appears as the "Reason:"
	 * in the error dialog for this action.
	 * Default is to return the empty String.
	 * @return the message to be displayed as a status
	 */
	protected String getStatusMessage(){
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	public void init(IViewPart view) {
		fViewPart = view;
	}

	/**
	 * Returns this action's view part, or <code>null</code>
	 * if not installed in a view.
	 *
	 * @return view part or <code>null</code>
	 */
	protected IViewPart getView() {
		return fViewPart;
	}

	/**
	 * Initialize this delegate, updating this delegate's
	 * presentation.
	 * As well, all of the flavors of AbstractDebugActionDelegates need to
	 * have the initial enabled state set with a call to update(IAction, ISelection).
	 *
	 * @param action the presentation for this action
	 * @param selection the current selection - workbench or text
	 * @return whether the action was initialized
	 */
	protected boolean initialize(IAction action, ISelection selection) {
		if (!isInitialized()) {
			setAction(action);
			update(action, selection);
			setInitialized(true);
			return true;
		}
		return false;
	}

	/**
	 * Returns the most recent selection
	 *
	 * @return structured selection
	 */
	protected IStructuredSelection getSelection() {
		return fSelection;
	}

	/**
	 * Sets the most recent selection
	 *
	 * @param selection structured selection
	 */
	private void setSelection(IStructuredSelection selection) {
		fSelection = selection;
	}

	/**
	 * Allows the underlying <code>IAction</code> to be set to the specified <code>IAction</code>
	 * @param action the action to set
	 */
	protected void setAction(IAction action) {
		fAction = action;
	}

	/**
	 * Allows access to the underlying <code>IAction</code>
	 * @return the underlying <code>IAction</code>
	 */
	protected IAction getAction() {
		return fAction;
	}

	/**
	 * Returns if this action has been initialized or not
	 * @return if this action has been initialized or not
	 */
	protected boolean isInitialized() {
		return fInitialized;
	}

	/**
	 * Sets the initialized state of this action to the specified boolean value
	 * @param initialized the value to set the initialized state to
	 */
	protected void setInitialized(boolean initialized) {
		fInitialized = initialized;
	}

	/**
	 * Return whether the action should be enabled or not based on the given selection.
	 * @param selection the selection
	 * @return if the action should be enabled or not
	 */
	protected boolean getEnableStateForSelection(IStructuredSelection selection) {
		if (selection.size() == 0) {
			return false;
		}
		Iterator<?> itr = selection.iterator();
		while (itr.hasNext()) {
			Object element= itr.next();
			if (!isEnabledFor(element)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if this action is enabled for the specified object context
	 * @param element the context
	 * @return true if it is, false otherwise
	 */
	protected boolean isEnabledFor(Object element) {
		return true;
	}

	/**
	 * Returns if this action is started with Shift pressed
	 * @return true if it is, false otherwise
	 */
	protected boolean isShift() {
		return fIsShift;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		if (action.isEnabled()) {
			IStructuredSelection selection = getSelection();
			// disable the action so it cannot be run again until an event or
			// selection change
			// updates the enablement
			action.setEnabled(false);
			runInForeground(selection, ((event.stateMask & SWT.SHIFT) > 0) ? true : false);
		}
	}

	@Override
	public void init(IAction action) {
		fAction = action;
	}

}
