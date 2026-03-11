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
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.selectAndRevealAnno;
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.uncheckToolbarActionItems;

import java.util.List;

import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;

public class NextRunnable implements Runnable {

	private ToolBarManager tm;
	private IAnnotationModel model;
	private ITextViewer tv;

	public NextRunnable(ITextViewer tv, IAnnotationModel model, ToolBarManager tm) {
		this.tv = tv;
		this.model = model;
		this.tm = tm;
	}

	public String getLabel() {
		return CompareMessages.UnifiedDiff_next;
	}

	public ImageDescriptor getImageDescriptor() {
		return CompareUIPlugin.getImageDescriptor(ICompareUIConstants.ETOOL_NEXT);
	}

	@Override
	public void run() {
		uncheckToolbarActionItems(tm);

		if (!(tv.getSelectionProvider().getSelection() instanceof ITextSelection sel)) {
			return;
		}
		int offset = sel.getOffset();
		List<UnifiedDiff> diffs1 = get(tv);
		// get next UnifiedDiff for given offset
		UnifiedDiff nextDiff = null;
		for (UnifiedDiff diff : diffs1) {
			int diffStart = getStartOffset(diff, model);
			if (diffStart > offset) {
				nextDiff = diff;
				break;
			}
			if (nextDiff != null) {
				break;
			}
		}
		if (nextDiff == null) {
			nextDiff = diffs1.get(0);
		}
		List<Annotation> all = getAllAnnotationsForUnifiedDiff(model, nextDiff);
		if (all.size() == 0) {
			return;
		}
		Annotation nextAnno = getMinPositionAnno(model, all);
		selectAndRevealAnno(tv, model, nextAnno);
	}

	static int getStartOffset(UnifiedDiff diff, IAnnotationModel model) {
		int diffStart = diff.leftStart;
		List<Annotation> all = getAllAnnotationsForUnifiedDiff(model, diff);
		if (all.size() > 0) {
			Annotation min = getMinPositionAnno(model, all);
			if (min != null) {
				Position pos = model.getPosition(min);
				if (pos != null) {
					diffStart = pos.getOffset();
				}
			}
		}
		return diffStart;
	}
}
