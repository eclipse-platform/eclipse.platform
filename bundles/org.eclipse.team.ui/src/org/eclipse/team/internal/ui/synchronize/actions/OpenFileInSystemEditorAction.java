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

	@Override
	protected List<? extends IResource> getSelectedResources() {
		IStructuredSelection selection = getStructuredSelection();
		IResource[] resources = Utils.getResources(selection.toArray());
		return Arrays.asList(resources);
	}

	@Override
	protected List<?> getSelectedNonResources() {
		return Collections.EMPTY_LIST;
	}
}
