/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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

import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IActionDelegate2;
import org.eclipse.ui.IViewActionDelegate;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.progress.IWorkbenchSiteProgressService;

/**
 * This class provides a base implementation of a selection action delegate, more specifically a delegate
 * that uses a selection context to update its underlying <code>IAction</code>.
 *
 * This class is intended to be extended by clients
 *
 * @see IViewActionDelegate
 * @see IActionDelegate2
 *
 */
public abstract class AbstractSelectionActionDelegate implements IViewActionDelegate, IActionDelegate2 {

	/**
	 * The underlying action for this delegate
	 */
	private IAction fAction;

	/**
	 * This action's view part, or <code>null</code> if not installed in a
	 * view.
	 */
	private IViewPart fViewPart;

	/**
	 * Cache of the most recent selection
	 */
	private IStructuredSelection fSelection = StructuredSelection.EMPTY;

	/**
	 * Used to schedule jobs, or <code>null</code> if none
	 */
	private IWorkbenchSiteProgressService fProgressService = null;

	/**
	 * It's crucial that delegate actions have a zero-arg constructor so that
	 * they can be reflected into existence when referenced in an action set in
	 * the plugin's plugin.xml file.
	 */
	public AbstractSelectionActionDelegate() {}

	@Override
	public void dispose() {
		fSelection = null;
	}

	@Override
	public void selectionChanged(IAction action, ISelection s) {
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			action.setEnabled(getEnableStateForSelection(ss));
			setSelection(ss);
		} else {
			action.setEnabled(false);
			setSelection(StructuredSelection.EMPTY);
		}
	}

	/**
	 * Returns the String to use as an error dialog message for a failed action.
	 * This message appears as the "Message:" in the error dialog for this
	 * action. Default is to return null.
	 *
	 * @return the message for the error dialog
	 */
	protected String getErrorDialogMessage() {
		return null;
	}

	/**
	 * Returns the String to use as a status message for a failed action. This
	 * message appears as the "Reason:" in the error dialog for this action.
	 * Default is to return the empty String.
	 *
	 * @return the status message
	 */
	protected String getStatusMessage() {
		return IInternalDebugCoreConstants.EMPTY_STRING;
	}

	@Override
	public void init(IViewPart view) {
		setView(view);
		fProgressService = view.getAdapter(IWorkbenchSiteProgressService.class);
	}

	/**
	 * Returns this action's view part, or <code>null</code> if not installed
	 * in a view.
	 *
	 * @return the underlying <code>IViewPart</code> or <code>null</code>
	 */
	protected IViewPart getView() {
		return fViewPart;
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
	 * @param context structured selection
	 */
	private void setSelection(IStructuredSelection context) {
		fSelection = context;
	}

	/**
	 * Allows the underlying <code>IAction</code> to be set to the specified one
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
	 * Allows the underlying <code>IViewPart</code> to be set
	 * @param viewPart the <code>IViewPart</code> to set
	 */
	protected void setView(IViewPart viewPart) {
		fViewPart = viewPart;
	}

	/**
	 * Return whether the action should be enabled or not based on the given
	 * selection.
	 * @param selection the current selection
	 *
	 * @return true if the action should be enabled for the specified selection context
	 * false otherwise
	 */
	protected boolean getEnableStateForSelection(IStructuredSelection selection) {
		if (selection.size() == 0) {
			return false;
		}
		Iterator<?> itr = selection.iterator();
		while (itr.hasNext()) {
			Object element = itr.next();
			if (!isEnabledFor(element)) {
				return false;
			}
		}
		return true;
	}

	/**
	 * Returns if the action should be enabled for the specified object context
	 * @param element the object context
	 * @return true if the action should be enabled for the specified object context
	 * false otherwise
	 */
	protected boolean isEnabledFor(Object element) {
		return true;
	}

	@Override
	public void runWithEvent(IAction action, Event event) {
		run(action);
	}

	@Override
	public void init(IAction action) {
		setAction(action);
	}

	/**
	 * Schedules the given job with this action's progress service
	 *
	 * @param job the {@link Job} to schedule
	 */
	protected void schedule(Job job) {
		if (fProgressService == null) {
			job.schedule();
		} else {
			fProgressService.schedule(job);
		}
	}
}
