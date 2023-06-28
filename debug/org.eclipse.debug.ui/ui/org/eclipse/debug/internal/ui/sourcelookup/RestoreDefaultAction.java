/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.DefaultSourceContainer;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.actions.SelectionListenerAction;

/**
 * The action for adding the default container to the list.
 *
 * @since 3.0
 */
public class RestoreDefaultAction extends SourceContainerAction {

	private ISourceLookupDirector fDirector;

	public RestoreDefaultAction() {
		super(SourceLookupUIMessages.sourceTab_defaultButton);
	}
	/**
	 * @see IAction#run()
	 */
	@Override
	public void run() {
		ISourceContainer[] containers = new ISourceContainer[1];
		containers[0] = new DefaultSourceContainer();
		containers[0].init(fDirector);
		getViewer().setEntries(containers);
		setEnabled(false);
	}

	public void setSourceLookupDirector(ISourceLookupDirector director) {
		fDirector = director;
	}

	/**
	 * @see SelectionListenerAction#updateSelection(IStructuredSelection)
	 */
	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		//disable if selection is empty, default already present, or non-root node selected
		ISourceContainer[] containers = getViewer().getEntries();
		if(containers != null && containers.length == 1) {
			if(containers[0] instanceof DefaultSourceContainer) {
				return false;
			}
		}
		return true;
	}
}
