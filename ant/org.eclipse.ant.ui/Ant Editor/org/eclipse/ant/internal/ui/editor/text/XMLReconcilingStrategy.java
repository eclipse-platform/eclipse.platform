/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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

package org.eclipse.ant.internal.ui.editor.text;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.editor.AntEditor;
import org.eclipse.ant.internal.ui.model.AntModel;
import org.eclipse.ant.internal.ui.model.IAntModel;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.reconciler.DirtyRegion;
import org.eclipse.jface.text.reconciler.IReconcilingStrategy;
import org.eclipse.jface.text.reconciler.IReconcilingStrategyExtension;

public class XMLReconcilingStrategy implements IReconcilingStrategy, IReconcilingStrategyExtension {

	/**
	 * How long the reconciler will wait for further text changes before reconciling
	 */
	public static final int DELAY = 500;

	private final AntEditor fEditor;

	public XMLReconcilingStrategy(AntEditor editor) {
		fEditor = editor;
	}

	private void internalReconcile() {
		try {
			IAntModel model = fEditor.getAntModel();
			if (model instanceof AntModel) {
				((AntModel) model).reconcile();
			}
		}
		catch (Exception e) {
			AntUIPlugin.log(e);
		}
	}

	@Override
	public void reconcile(IRegion partition) {
		internalReconcile();
	}

	@Override
	public void reconcile(DirtyRegion dirtyRegion, IRegion subRegion) {
		internalReconcile();
	}

	@Override
	public void setDocument(IDocument document) {
		// do nothing
	}

	@Override
	public void setProgressMonitor(IProgressMonitor monitor) {
		// do nothing
	}

	@Override
	public void initialReconcile() {
		internalReconcile();
	}
}
