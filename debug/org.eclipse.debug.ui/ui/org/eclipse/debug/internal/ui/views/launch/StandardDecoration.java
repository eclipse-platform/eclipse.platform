/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.launch;

import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.ui.IDebugEditorPresentation;
import org.eclipse.ui.IEditorPart;

/**
 * A reminder to remove any annotations created by an editor presentation
 */
public class StandardDecoration extends Decoration {

	private IThread fThread;
	private IEditorPart fEditor;
	private IDebugEditorPresentation fPresentation;

	public StandardDecoration(IDebugEditorPresentation presentation, IEditorPart editorPart, IThread thread) {
		fThread = thread;
		fEditor = editorPart;
		fPresentation = presentation;
	}

	@Override
	public void remove() {
		fPresentation.removeAnnotations(fEditor, fThread);
	}

	@Override
	public IThread getThread() {
		return fThread;
	}

}
