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
import static org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.runAfterRepaintFinished;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiffAnnotation;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.swt.custom.StyledText;

public class AcceptAllRunnable implements Runnable {
	private IAnnotationModel model;
	private ITextViewer tv;

	public AcceptAllRunnable(ITextViewer tv, IAnnotationModel model) {
		this.tv = tv;
		this.model = model;
	}

	public String getLabel() {
		return "Accept All";
	}

	@Override
	public void run() {
		StyledText tw = tv.getTextWidget();
		List<UnifiedDiff> diffs1 = get(tv);
		List<Position> positions = new ArrayList<>();
		List<String> replaceStrings = new ArrayList<>();
		for (UnifiedDiff diff : diffs1) {
			List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
			int unifiedDiffAdditionCount = 0;
			for (var lanno : annos) {
				if (lanno instanceof UnifiedDiffAnnotation) {
					unifiedDiffAdditionCount++;
					if (unifiedDiffAdditionCount > 1) {
						throw new IllegalStateException("Multiple UnifiedDiffAnnotation for one UnifiedDiff found"); //$NON-NLS-1$
					}
					Position pos = model.getPosition(lanno);
					positions.add(pos);
					replaceStrings.add(diff.rightStr);
				}
				model.removeAnnotation(lanno);
			}
		}
		diffs1.clear();
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
		disposeUnifiedDiff(tv, model, tv.getTextWidget());
		// we have to insert with delay because otherwise the line header code minings
		// cannot be deleted
		runAfterRepaintFinished(tw, () -> {
			for (int i = positions.size() - 1; i >= 0; i--) {
				Position pos = positions.get(i);
				String replaceStr = replaceStrings.get(i);
				try {
					tv.getDocument().replace(pos.offset, pos.length, replaceStr);
				} catch (BadLocationException e) {
					UnifiedDiffManager.error(e);
				}
			}
		});
	}
}
