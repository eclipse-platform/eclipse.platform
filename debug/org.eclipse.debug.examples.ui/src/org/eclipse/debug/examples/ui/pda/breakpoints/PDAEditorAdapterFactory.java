/*******************************************************************************
 * Copyright (c) 2005, 2015 IBM Corporation and others.
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
 *     Bjorn Freeman-Benson - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.examples.ui.pda.breakpoints;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.examples.ui.pda.editor.PDAEditor;
import org.eclipse.debug.ui.actions.IRunToLineTarget;
import org.eclipse.debug.ui.actions.IToggleBreakpointsTarget;
import org.eclipse.ui.texteditor.ITextEditor;


/**
 * Creates a toggle breakpoint adapter
 */
public class PDAEditorAdapterFactory implements IAdapterFactory {
	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof PDAEditor) {
			ITextEditor editorPart = (ITextEditor) adaptableObject;
			IResource resource = editorPart.getEditorInput().getAdapter(IResource.class);
			if (resource != null) {
				String extension = resource.getFileExtension();
				if (extension != null && extension.equals("pda")) { //$NON-NLS-1$
					if (adapterType.equals(IToggleBreakpointsTarget.class)) {
						return (T) new PDABreakpointAdapter();
					}
					//#ifdef ex7
//#					// TODO: Exercise 7 - create run to line adapter
					//#else
					if (adapterType.equals(IRunToLineTarget.class)) {
						return (T) new PDARunToLineAdapter();
					}
					//#endif
				}
			}
		}
		return null;
	}

	@Override
	public Class<?>[] getAdapterList() {
		return new Class[]{IToggleBreakpointsTarget.class};
	}
}
