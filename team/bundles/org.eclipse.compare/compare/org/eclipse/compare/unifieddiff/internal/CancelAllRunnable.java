/*******************************************************************************
 * Copyright (c) 2026 SAP
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * SAP - initial implementation
 *******************************************************************************/
package org.eclipse.compare.unifieddiff.internal;

import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.disposeUnifiedDiff;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.get;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.getAllAnnotationsForUnifiedDiff;

import java.util.List;

import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.texteditor.ITextEditor;

public class CancelAllRunnable implements Runnable {

	private IAnnotationModel model;
	private ITextViewer tv;

	public CancelAllRunnable(ITextViewer tv, IAnnotationModel model) {
		this.tv = tv;
		this.model = model;
	}

	public CancelAllRunnable(ITextEditor textEditor) {
		this.tv = textEditor.getAdapter(ITextViewer.class);
		this.model = textEditor.getDocumentProvider().getAnnotationModel(textEditor.getEditorInput());
	}

	public String getLabel() {
		return "Cancel All"; //$NON-NLS-1$
	}

	public static ImageDescriptor getCancelImageDescriptor() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_ELCL_STOP);
	}

	public ImageDescriptor getImageDescriptor() {
		return getCancelImageDescriptor();
	}

	@Override
	public void run() {
		List<UnifiedDiff> diffs1 = get(tv);
		for (UnifiedDiff diff : diffs1) {
			List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
			for (var lanno : annos) {
				model.removeAnnotation(lanno);
			}
		}
		diffs1.clear();
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
		disposeUnifiedDiff(tv, model, tv.getTextWidget());
	}
}
