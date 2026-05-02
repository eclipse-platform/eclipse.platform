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

import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.get;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.getAllAnnotationsForUnifiedDiff;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.getMinPositionAnno;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.getToolbarShellForOneDiff;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.selectAndRevealAnno;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.uncheckToolbarActionItems;

import java.util.List;

import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.swt.widgets.Shell;

public class PreviousRunnable implements Runnable {

	private ITextViewer tv;
	private IAnnotationModel model;
	private ToolBarManager tm;

	public PreviousRunnable(ITextViewer tv, IAnnotationModel model, ToolBarManager tm) {
		this.tv = tv;
		this.model = model;
		this.tm = tm;
	}

	public String getLabel() {
		return "Previous"; //$NON-NLS-1$
	}

	public ImageDescriptor getImageDescriptor() {
		return CompareUIPlugin.getImageDescriptor(ICompareUIConstants.ETOOL_PREV);
	}

	@Override
	public void run() {
		uncheckToolbarActionItems(tm);

		Shell toolbarShellForOneDiff = getToolbarShellForOneDiff(tv.getTextWidget());
		if (toolbarShellForOneDiff == null) {
			return;
		}
		if (!(tv.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		int offset = sel.getOffset();
		List<UnifiedDiff> diffs1 = get(tv);
		// get next UnifiedDiff for given offset
		UnifiedDiff nextDiff = null;
		for (UnifiedDiff diff : diffs1) {
			if (diff.leftStart < offset) {
				nextDiff = diff;
			}
		}
		if (nextDiff == null) {
			nextDiff = diffs1.getLast();
		}
		List<Annotation> all = getAllAnnotationsForUnifiedDiff(model, nextDiff);
		if (all.size() == 0) {
			return;
		}
		Annotation nextAnno = getMinPositionAnno(model, all);
		selectAndRevealAnno(tv, model, nextAnno);
	}
}
