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
package org.eclipse.debug.internal.ui.launchConfigurations;


import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * Common function/behavior for launch configuration view actions
 */
public abstract class AbstractLaunchConfigurationAction extends SelectionListenerAction {

	/**
	 * The mode this action was created in (run, debug, ...)
	 */
	private String fMode;

	/**
	 * Allows a requestor to abort this action.
	 */
	public interface IConfirmationRequestor {
		/**
		 * Returns whether this action should proceed. Confirmation is requested
		 * when an action is run.
		 *
		 * @return whether this action should proceed
		 */
		boolean getConfirmation();
	}

	/**
	 * This action's confirmation requestor or <code>null</code> if none
	 */
	private IConfirmationRequestor fConfirmationRequestor;

	/**
	 * The viewer this action is working on
	 */
	private Viewer fViewer;

	/**
	 * Constructor for AbstractLaunchConfigurationAction.
	 * @param text the action label
	 * @param viewer the viewer the action belongs to
	 * @param mode the mode the action applies to
	 */
	public AbstractLaunchConfigurationAction(String text, Viewer viewer, String mode) {
		super(text);
		fViewer = viewer;
		fViewer.addSelectionChangedListener(this);
		fMode = mode;
	}

	/**
	 * Returns the shell this action is contained in.
	 *
	 * @return the shell this action is contained in
	 */
	protected Shell getShell() {
		return getViewer().getControl().getShell();
	}

	/**
	 * Returns the viewer this action is working on
	 *
	 * @return the viewer this action is working on
	 */
	protected Viewer getViewer() {
		return fViewer;
	}

	/**
	 * Performs this action once confirmation has been aquired. Subclasses
	 * should override this method.
	 */
	protected abstract void performAction();

	/**
	 * @see org.eclipse.jface.action.IAction#run()
	 */
	@Override
	public final void run() {
		if (fConfirmationRequestor != null) {
			if (!fConfirmationRequestor.getConfirmation()) {
				return;
			}
		}
		Runnable r = this::performAction;
		BusyIndicator.showWhile(getShell().getDisplay(), r);
	}

	/**
	 * Sets this action's confirmation requestor.
	 *
	 * @param confirmationRequestor the new {@link IConfirmationRequestor}
	 */
	public void setConfirmationRequestor(IConfirmationRequestor confirmationRequestor) {
		fConfirmationRequestor = confirmationRequestor;
	}

	/**
	 * Disposes this action
	 */
	public void dispose() {
		fViewer.removeSelectionChangedListener(this);
	}

	/**
	 * Show an error dialog on the given exception.
	 *
	 * @param exception the exception to show in the dialog
	 */
	protected void errorDialog(CoreException exception) {
		ErrorDialog.openError(getShell(), null, null, exception.getStatus());
	}

	/**
	 * Return this action's mode.
	 *
	 * @return launch mode
	 */
	protected String getMode() {
		return fMode;
	}
}
