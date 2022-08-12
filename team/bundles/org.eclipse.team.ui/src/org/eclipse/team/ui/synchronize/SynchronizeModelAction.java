/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.team.core.synchronize.FastSyncInfoFilter;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoSet;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.synchronize.SyncInfoModelElement;
import org.eclipse.ui.actions.BaseSelectionListenerAction;
import org.eclipse.ui.ide.IDE;

/**
 * This action provides utilities for performing operations on selections that
 * contain {@link org.eclipse.team.ui.synchronize.ISynchronizeModelElement}
 * instances. Subclasses can use this support to filter the selection in order
 * to determine action enablement and generate the input for a
 * {@link SynchronizeModelOperation}.
 *
 * @see SyncInfo
 * @see SyncInfoSet
 * @see SynchronizeModelOperation
 * @since 3.0
 */
public abstract class SynchronizeModelAction extends BaseSelectionListenerAction {

	private ISynchronizePageConfiguration configuration;

	/**
	 * Create an action with the given text and configuration. By default,
	 * the action registers for selection change with the selection provider
	 * from the configuration's site.
	 *
	 * @param text the action's text
	 * @param configuration the actions synchronize page configuration
	 */
	protected SynchronizeModelAction(String text, ISynchronizePageConfiguration configuration) {
		this(text, configuration, configuration.getSite().getSelectionProvider());
	}

	/**
	 * Create an action with the given text and configuration. By default,
	 * the action registers for selection change with the given selection provider.
	 *
	 * @param text the action's text
	 * @param configuration the actions synchronize page configuration
	 * @param selectionProvider a selection provider
	 */
	protected SynchronizeModelAction(String text, ISynchronizePageConfiguration configuration, ISelectionProvider selectionProvider) {
		super(text);
		this.configuration = configuration;
		initialize(configuration, selectionProvider);
	}

	/**
	 * Method invoked from the constructor.
	 * The default implementation registers the action as a selection change
	 * listener. Subclasses may override.
	 *
	 * @param configuration the synchronize page configuration
	 * @param selectionProvider a selection provider
	 */
	protected void initialize(final ISynchronizePageConfiguration configuration, final ISelectionProvider selectionProvider) {
		selectionProvider.addSelectionChangedListener(this);
		configuration.getPage().getViewer().getControl().addDisposeListener(e -> selectionProvider.removeSelectionChangedListener(SynchronizeModelAction.this));
	}

	@Override
	public void run() {
		if(needsToSaveDirtyEditors()) {
			if(!saveAllEditors(confirmSaveOfDirtyEditor())) {
				return;
			}
		}
		try {
			runOperation();
		} catch (InvocationTargetException e) {
			handle(e);
		} catch (InterruptedException e) {
			handle(e);
		}
	}

	/**
	 * Create and run the operation for this action. By default, the operation is created
	 * by calling <code>getSubscriberOperation</code> and then run. Subclasses may
	 * override.
	 *
	 * @throws InvocationTargetException if an error occurs
	 * @throws InterruptedException if operation is interrupted
	 * @since 3.1
	 */
	protected void runOperation() throws InvocationTargetException, InterruptedException {
		getSubscriberOperation(configuration, getFilteredDiffElements()).run();
	}

	/**
	 * Return whether dirty editor should be saved before this action is run.
	 * Default is <code>true</code>.
	 *
	 * @return whether dirty editor should be saved before this action is run
	 */
	protected boolean needsToSaveDirtyEditors() {
		return true;
	}

	/**
	 * Returns whether the user should be prompted to save dirty editors. The
	 * default is <code>true</code>.
	 *
	 * @return whether the user should be prompted to save dirty editors
	 */
	protected boolean confirmSaveOfDirtyEditor() {
		return true;
	}

	/**
	 * Return the subscriber operation associated with this action. This
	 * operation will be run when the action is run. Subclass may implement this
	 * method and provide an operation subclass or may override the
	 * <code>run(IAction)</code> method directly if they choose not to
	 * implement a <code>SynchronizeModelOperation</code>.
	 *
	 * @param configuration the synchronize page configuration for the page to
	 * which this action is associated
	 * @param elements the selected diff element for which this action is
	 * enabled.
	 * @return the subscriber operation to be run by this action.
	 */
	protected abstract SynchronizeModelOperation getSubscriberOperation(ISynchronizePageConfiguration configuration, IDiffElement[] elements);

	/**
	 * Generic error handling code that uses an error dialog to show the error
	 * to the user. Subclasses can use this method and/or override it.
	 *
	 * @param e the exception that occurred.
	 */
	protected void handle(Exception e) {
		Utils.handle(e);
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		super.updateSelection(selection);
		return isEnabledForSelection(selection);
	}

	private boolean isEnabledForSelection(IStructuredSelection selection) {
		return Utils.hasMatchingDescendant(selection, getSyncInfoFilter());
	}

	/**
	 * This method returns all instances of IDiffElement that are in the current
	 * selection.
	 *
	 * @return the selected elements
	 */
	protected final IDiffElement[] getSelectedDiffElements() {
		return Utils.getDiffNodes(getStructuredSelection().toArray());
	}

	/**
	 * Filter uses to filter the user selection to contain only those elements
	 * for which this action is enabled. Default filter includes all out-of-sync
	 * elements in the current selection. Subclasses may override.
	 *
	 * @return a sync info filter which selects all out-of-sync resources.
	 */
	protected FastSyncInfoFilter getSyncInfoFilter() {
		return new FastSyncInfoFilter();
	}

	/**
	 * Return the selected diff element for which this action is enabled.
	 * @return the list of selected diff elements for which this action is
	 *               enabled.
	 */
	protected final IDiffElement[] getFilteredDiffElements() {
		IDiffElement[] elements = getSelectedDiffElements();
		List<IDiffElement> filtered = new ArrayList<>();
		for (IDiffElement e : elements) {
			if (e instanceof SyncInfoModelElement) {
				SyncInfo info = ((SyncInfoModelElement) e).getSyncInfo();
				if (info != null && getSyncInfoFilter().select(info)) {
					filtered.add(e);
				}
			}
		}
		return filtered.toArray(new IDiffElement[filtered.size()]);
	}

	/**
	 * Set the selection of this action to the given selection
	 *
	 * @param selection the selection
	 */
	public void selectionChanged(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			super.selectionChanged((IStructuredSelection)selection);
		} else {
			super.selectionChanged(StructuredSelection.EMPTY);
		}

	}

	/**
	 * Returns the configuration showing this action.
	 *
	 * @return the configuration showing this action.
	 */
	public ISynchronizePageConfiguration getConfiguration() {
		return configuration;
	}

	/**
	 * Save all dirty editors in the workbench that are open on files that may
	 * be affected by this operation. Opens a dialog to prompt the user if
	 * <code>confirm</code> is true. Return true if successful. Return false
	 * if the user has canceled the command. Must be called from the UI thread.
	 *
	 * @param confirm prompt the user if true
	 * @return boolean false if the operation was canceled.
	 */
	public final boolean saveAllEditors(boolean confirm) {
		return IDE.saveAllEditors(Utils.getResources(getFilteredDiffElements()), confirm);
	}
}
