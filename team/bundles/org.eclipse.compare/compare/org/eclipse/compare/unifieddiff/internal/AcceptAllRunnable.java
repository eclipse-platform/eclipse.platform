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

import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.unifieddiff.UnifiedDiff.ToolbarAction;
import org.eclipse.compare.unifieddiff.UnifiedDiffHunk.Resolution;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiff;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffManager.UnifiedDiffAnnotation;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

public class AcceptAllRunnable implements Runnable {
	private IAnnotationModel model;
	private ITextViewer tv;
	private final ToolbarAction action;

	public AcceptAllRunnable(ITextViewer tv, IAnnotationModel model) {
		this(tv, model, ToolbarAction.ACCEPT_ALL);
	}

	public AcceptAllRunnable(ITextViewer tv, IAnnotationModel model, ToolbarAction action) {
		this.tv = tv;
		this.model = model;
		this.action = action;
	}

	public String getLabel() {
		return CompareMessages.UnifiedDiff_acceptAll;
	}

	public static ImageDescriptor getImageDescriptor() {
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_ADD);
	}

	public static ImageDescriptor getUndoImageDescriptor() {
		return CompareUIPlugin.getImageDescriptor("elcl16/undo.svg"); //$NON-NLS-1$
	}

	@Override
	public void run() {
		StyledText tw = tv.getTextWidget();
		List<UnifiedDiff> diffs1 = get(tv);
		if (diffs1 == null) {
			return;
		}
		if (!UnifiedDiffManager.validateEdit(tv)) {
			return;
		}
		ToolbarAction executedAction = !diffs1.isEmpty() && UnifiedDiffMode.REVERT_MODE.equals(diffs1.get(0).mode)
				? ToolbarAction.REVERT_ALL
				: action;
		List<Position> positions = new ArrayList<>();
		List<String> replaceStrings = new ArrayList<>();
		List<UnifiedDiffManager.ResolvedDiff> pendingEvents = new ArrayList<>();
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
					UnifiedDiffManager.ResolvedDiff pendingEvent = UnifiedDiffManager.prepareEvent(diff,
							Resolution.ACCEPTED, pos);
					pendingEvents.add(pendingEvent);
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
		if (positions.isEmpty()) {
			return;
		}
		List<UnifiedDiffManager.ResolvedDiff> completed = new ArrayList<>();
		runAfterRepaintFinished(tw, () -> {
			for (int i = positions.size() - 1; i >= 0; i--) {
				Position pos = positions.get(i);
				String replaceStr = replaceStrings.get(i);
				try {
					tv.getDocument().replace(pos.offset, pos.length, replaceStr);
					completed.add(0, pendingEvents.get(i));
				} catch (BadLocationException e) {
					UnifiedDiffManager.error(e);
				}
			}
			if (!completed.isEmpty()) {
				UnifiedDiffManager.fireAllEvents(completed, executedAction);
			}
			for (UnifiedDiffManager.ResolvedDiff pendingEvent : pendingEvents) {
				UnifiedDiffManager.cancelEvent(pendingEvent);
			}
		});
	}
}
