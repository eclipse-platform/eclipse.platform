/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.actions.OpenFileAction;

public class OpenFileInSystemEditorAction extends OpenFileAction {

	public OpenFileInSystemEditorAction(IWorkbenchPage page) {
		super(page);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionListenerAction#getSelectedResources()
	 */
	@Override
	protected List getSelectedResources() {
		IStructuredSelection selection = getStructuredSelection();
		IResource[] resources = Utils.getResources(selection.toArray());
		return Arrays.asList(resources);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.actions.SelectionListenerAction#getSelectedNonResources()
	 */
	@Override
	protected List getSelectedNonResources() {		
		return Collections.EMPTY_LIST;
	}
}
