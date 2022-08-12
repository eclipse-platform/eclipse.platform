/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Rolf Theunissen - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import org.eclipse.core.resources.IFile;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.ide.ResourceUtil;
import org.eclipse.ui.navigator.ILinkHelper;

/**
 * Links CompareEditorInput to IFiles in the CommonNavigator.
 *
 * @since 3.8
 */
public class CompareLinkHelper implements ILinkHelper {

	@Override
	public IStructuredSelection findSelection(IEditorInput anInput) {
		IEditorInput eInput = anInput.getAdapter(IEditorInput.class);

		IFile file = ResourceUtil.getFile(eInput);
		if (file != null) {
			return new StructuredSelection(file);
		}

		return StructuredSelection.EMPTY;
	}

	@Override
	public void activateEditor(IWorkbenchPage aPage, IStructuredSelection aSelection) {
		// do nothing
	}

}
