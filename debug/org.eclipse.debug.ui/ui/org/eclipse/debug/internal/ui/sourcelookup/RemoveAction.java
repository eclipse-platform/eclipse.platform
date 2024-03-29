/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.sourcelookup;

import java.util.List;

import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * The action used to remove source containers in the source location dialog/tab.
 */
public class RemoveAction extends SourceContainerAction {
	public RemoveAction() {
		super(SourceLookupUIMessages.sourceTab_removeButton);
	}
	/**
	 * Removes all selected entries.
	 *
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		List<ISourceContainer> targets = getOrderedSelection();
		List<ISourceContainer> list = getEntriesAsList();
		list.removeAll(targets);
		setEntries(list);
	}

	/**
	 * @see SelectionListenerAction#updateSelection(IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		//check that something is selected and it is a root tree node.
		return !selection.isEmpty() && getViewer().getTree().getSelection()[0].getParentItem()==null;
	}

}
