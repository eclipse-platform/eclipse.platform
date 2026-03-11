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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import org.eclipse.compare.contentmergeviewer.IIgnoreWhitespaceContributor;
import org.eclipse.compare.contentmergeviewer.ITokenComparator;
import org.eclipse.compare.contentmergeviewer.TokenComparator;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.DocLineComparator;
import org.eclipse.compare.rangedifferencer.IRangeComparator;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.rangedifferencer.RangeDifferencer;
import org.eclipse.compare.unifieddiff.UnifiedDiff.IgnoreWhitespaceContributorFactory;
import org.eclipse.compare.unifieddiff.UnifiedDiff.TokenComparatorFactory;
import org.eclipse.compare.unifieddiff.UnifiedDiffMode;
import org.eclipse.compare.unifieddiff.internal.UnifiedDiffCodeMiningProvider.UnifiedDiffLineHeaderCodeMining;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.codemining.ICodeMining;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.AnnotationModelEvent;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IAnnotationModelExtension;
import org.eclipse.jface.text.source.IAnnotationModelListener;
import org.eclipse.jface.text.source.IAnnotationModelListenerExtension;
import org.eclipse.jface.text.source.ISourceViewerExtension5;
import org.eclipse.jface.text.source.inlined.AbstractInlinedAnnotation;
import org.eclipse.jface.text.source.projection.ProjectionViewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.ControlListener;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.text.undo.DocumentUndoManagerRegistry;
import org.eclipse.text.undo.IDocumentUndoListener;
import org.eclipse.text.undo.IDocumentUndoManager;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.texteditor.ITextEditor;

public class UnifiedDiffManager {

	private static final String CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY = "CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY"; //$NON-NLS-1$
	private static final String UNDO_LISTENER_KEY = "UNIFIED_DIFF_UNDO_LISTENER_KEY"; //$NON-NLS-1$
	private static final String UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY = "UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY"; //$NON-NLS-1$
	private static final String ADDITION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.addition"; //$NON-NLS-1$
	private static final String DELETION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.deletion"; //$NON-NLS-1$
	private static final String DETAILED_ADDITION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedAddition"; //$NON-NLS-1$
	private static final String DETAILED_DELETION_ANNO_TYPE = "org.eclipse.compare.unifieddiff.internal.detailedDeletion"; //$NON-NLS-1$
	private static final String TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY = "TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY"; //$NON-NLS-1$
	private static final String TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY = "TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY"; //$NON-NLS-1$
	private static final Map<ITextViewer, List<UnifiedDiff>> diffsByViewer = new HashMap<>();

	public static void put(ITextViewer viewer, List<UnifiedDiff> diffs) {
		diffsByViewer.put(viewer, diffs);
	}

	public static List<UnifiedDiff> get(ITextViewer viewer) {
		return diffsByViewer.get(viewer);
	}

	public static IStatus open(ITextEditor editor, String source, UnifiedDiffMode mode, List<Action> additionalActions,
			TokenComparatorFactory tokenComparatorFactory,
			IgnoreWhitespaceContributorFactory ignoreWhitespaceContributorFactory, boolean ignoreWhiteSpace) {
		ITextViewer viewer = editor.getAdapter(ITextViewer.class);
		if (viewer instanceof ProjectionViewer pv) {
			pv.doOperation(ProjectionViewer.EXPAND_ALL);
		}
		IAnnotationModel model = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
		if (model == null) {
			return Status.CANCEL_STATUS;
		}
		clearAll(viewer, model);

		IDocument leftDocument = editor.getDocumentProvider().getDocument(editor.getEditorInput());
		IDocument rightDocument = new Document(source);

		DocLineComparator left = null, right = null;
		Optional<IIgnoreWhitespaceContributor> lDocIgnonerWhitespaceContributor = Optional.empty();
		Optional<IIgnoreWhitespaceContributor> rDocIgnonreWhitespaceContributor = Optional.empty();
		if (ignoreWhitespaceContributorFactory != null) {
			lDocIgnonerWhitespaceContributor = ignoreWhitespaceContributorFactory.apply(leftDocument);
			left = new DocLineComparator(leftDocument, null, ignoreWhiteSpace, null, '?',
					lDocIgnonerWhitespaceContributor);
			rDocIgnonreWhitespaceContributor = ignoreWhitespaceContributorFactory.apply(rightDocument);
			right = new DocLineComparator(rightDocument, null, ignoreWhiteSpace, null, '?',
					rDocIgnonreWhitespaceContributor);
		} else {
			left = new DocLineComparator(leftDocument, null, ignoreWhiteSpace);
			right = new DocLineComparator(rightDocument, null, ignoreWhiteSpace);
		}
		List<UnifiedDiff> unifiedDiffs = new ArrayList<>();
		RangeDifference[] rangeDiffs = RangeDifferencer.findDifferences(left, right);
		for (RangeDifference rangeDiff : rangeDiffs) {
			try {
				int leftStart = left.getTokenStart(rangeDiff.leftStart());
				int leftEnd = getTokenEnd(left, rangeDiff.leftStart(), rangeDiff.leftLength());
				String leftDiffSource = leftDocument.get(leftStart, leftEnd - leftStart);

				int rightStart = right.getTokenStart(rangeDiff.rightStart());
				int rightEnd = getTokenEnd(right, rangeDiff.rightStart(), rangeDiff.rightLength());
				String rightDiffSource = rightDocument.get(rightStart, rightEnd - rightStart);

				if (leftDiffSource.length() == 0 && rightDiffSource.length() == 0) {
					continue;
				}
				boolean isWhitespace = false;
				// Indicate whether all contributors are whitespace
				if (ignoreWhiteSpace && leftDiffSource.trim().length() == 0 && rightDiffSource.trim().length() == 0) {
					isWhitespace = true;

					// Check if whitespace can be ignored by the contributor
					if (leftDiffSource.length() > 0 && !lDocIgnonerWhitespaceContributor.isEmpty()) {
						boolean isIgnored = lDocIgnonerWhitespaceContributor.get()
								.isIgnoredWhitespace(rangeDiff.leftStart(), rangeDiff.leftLength());
						isWhitespace = isIgnored;
					}
					if (isWhitespace && rightDiffSource.length() > 0 && !rDocIgnonreWhitespaceContributor.isEmpty()) {
						boolean isIgnored = rDocIgnonreWhitespaceContributor.get()
								.isIgnoredWhitespace(rangeDiff.rightStart(), rangeDiff.rightLength());
						isWhitespace = isIgnored;
					}
				}
				if (isWhitespace) {
					continue;
				}
				int kind = rangeDiff.kind();
				switch (kind) {
				case RangeDifference.NOCHANGE:
					break;
				case RangeDifference.CHANGE:
					var diff = new UnifiedDiff(leftDocument, leftStart, leftEnd, leftDiffSource, rightDocument,
							rightStart, rightEnd, rightDiffSource, unifiedDiffs, mode);
					unifiedDiffs.add(diff);

					// line based fine granular diff via DocumentMerger#simpleTokenDiff
					ITokenComparator l = createTokenComparator(leftDiffSource, tokenComparatorFactory);
					ITokenComparator r = createTokenComparator(rightDiffSource, tokenComparatorFactory);
					RangeDifference[] detailedDiffs = RangeDifferencer.findRanges((IRangeComparator) null, l, r);
					for (RangeDifference detailedDiff : detailedDiffs) {
						if (detailedDiff.kind() == RangeDifference.NOCHANGE) {
							continue;
						}
						int detailedLeftStart = l.getTokenStart(detailedDiff.leftStart());
						int detailedLeftEnd = getTokenEnd(l, detailedDiff.leftStart(), detailedDiff.leftLength());
						String detailedLeftDiffSource = leftDiffSource.substring(detailedLeftStart, detailedLeftEnd);

						int detailedRightStart = r.getTokenStart(detailedDiff.rightStart());
						int detailedRightEnd = getTokenEnd(r, detailedDiff.rightStart(), detailedDiff.rightLength());
						String detailedDiffRightDiffSource = rightDiffSource.substring(detailedRightStart,
								detailedRightEnd);
						if (detailedLeftDiffSource.trim().length() == 0
								&& detailedDiffRightDiffSource.trim().length() == 0) {
							continue;
						}
						diff.detailedDiffs.add(new UnifiedDiff(leftDocument, detailedLeftStart, detailedLeftEnd,
								detailedLeftDiffSource, rightDocument, detailedRightStart, detailedRightEnd,
								detailedDiffRightDiffSource, unifiedDiffs, mode));
					}
					break;
				case RangeDifference.CONFLICT:
					break;
				case RangeDifference.LEFT:
					break;
				case RangeDifference.ERROR:
					break;
				case RangeDifference.ANCESTOR:
					break;
				default:
					break;
				}
			} catch (BadLocationException e) {
				error(e);
			}
		}
		// call validateEdit before modifying the leftDocument; in read-only overlay
		// mode the document is not modified, so skip the check to avoid prompting
		// the user to make the file writable for a purely read-only diff
		if (!UnifiedDiffMode.OVERLAY_READ_ONLY_MODE.equals(mode)) {
			IFile file = editor.getEditorInput().getAdapter(IFile.class);
			if (file != null && !validateEdit(file)) {
				return Status.CANCEL_STATUS;
			}
		}
		Map<Annotation, UnifiedDiff> diffByAnno = new HashMap<>();
		// When the model supports batch updates, stage annotations into a map and flush
		// them with a single replaceAnnotations() call so listeners receive one event
		// instead of one per annotation. Otherwise fall back to per-annotation addAnnotation().
		Map<Annotation, Position> annotationsToAdd = (model instanceof IAnnotationModelExtension) ? new HashMap<>()
				: null;
		BiConsumer<Annotation, Position> addOrStage = (anno, pos) -> {
			if (annotationsToAdd != null) {
				annotationsToAdd.put(anno, pos);
			} else {
				model.addAnnotation(anno, pos);
			}
		};
		if (UnifiedDiffMode.REPLACE_MODE.equals(mode)) {
			// modify document
			int delta = 0;
			for (UnifiedDiff unifiedDiff : unifiedDiffs) {
				try {
					unifiedDiff.leftStart += delta;
					leftDocument.replace(unifiedDiff.leftStart, unifiedDiff.leftLength, unifiedDiff.rightStr);
					delta += (unifiedDiff.rightLength - unifiedDiff.leftLength);
					Annotation myAnnotation = new UnifiedDiffAnnotation(mode, unifiedDiff);
					Position position = new Position(unifiedDiff.leftStart, unifiedDiff.rightLength);
					addOrStage.accept(myAnnotation, position);
					for (var detailedDiff : unifiedDiff.detailedDiffs) {
						if (detailedDiff.rightStr.trim().length() == 0) {
							continue;
						}
						Annotation detailedAnno = new DetailedDiffAnnotation(mode, unifiedDiff);
						Position detailedPos = new Position(unifiedDiff.leftStart + detailedDiff.rightStart,
								detailedDiff.rightLength);
						addOrStage.accept(detailedAnno, detailedPos);
					}
					diffByAnno.put(myAnnotation, unifiedDiff);
				} catch (BadLocationException e) {
					error(e);
				}
			}
		} else {
			for (UnifiedDiff unifiedDiff : unifiedDiffs) {
				Annotation myAnnotation = new UnifiedDiffAnnotation(mode, unifiedDiff);
				Position position = new Position(unifiedDiff.leftStart, unifiedDiff.leftLength);
				addOrStage.accept(myAnnotation, position);
				for (var detailedDiff : unifiedDiff.detailedDiffs) {
					if (detailedDiff.leftStr.trim().length() == 0) {
						continue;
					}
					Annotation detailedAnno = new DetailedDiffAnnotation(mode, unifiedDiff);
					Position detailedPos = new Position(unifiedDiff.leftStart + detailedDiff.leftStart,
							detailedDiff.leftLength);
					addOrStage.accept(detailedAnno, detailedPos);
				}
				diffByAnno.put(myAnnotation, unifiedDiff);
			}
		}
		if (annotationsToAdd != null) {
			((IAnnotationModelExtension) model).replaceAnnotations(new Annotation[] {}, annotationsToAdd);
		}

		UnifiedDiffManager.put(viewer, unifiedDiffs);
		addPaintListener(viewer, model, mode);
		addMouseMoveListener(viewer, model);
		if (viewer instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
		drawToolBarForAllDiffs(viewer, model, additionalActions, mode);
		addUndoListener(viewer, leftDocument, model);
		addAnnoModelChangeListener(viewer, model);

		if (unifiedDiffs.size() > 0) {
			runAfterRepaintFinished(viewer.getTextWidget(), () -> {
				Annotation firstAnno = getFirstAnnotationForUnifiedDiff(model, unifiedDiffs.get(0));
				selectAndRevealAnno(viewer, model, firstAnno);
			});
		}
		return Status.OK_STATUS;
	}

	static boolean isViewerInPart(IWorkbenchPart part, ITextViewer viewer) {
		if (part == null) {
			return false;
		}
		Control partControl = part.getAdapter(Control.class);
		if (partControl == null) {
			return false;
		}
		StyledText textWidget = viewer.getTextWidget();
		if (textWidget == null || textWidget.isDisposed()) {
			return false;
		}
		// Check if the viewer's text widget is a descendant of the part's control
		Control current = textWidget;
		while (current != null) {
			if (current == partControl) {
				return true;
			}
			current = current.getParent();
		}
		return false;
	}

	static Composite getToolbarCompositeForOneDiff(StyledText tw) {
		if (tw == null) {
			return null;
		}
		var result = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY);
		return result;
	}

	private static Composite getToolbarCompositeForAllDiffs(StyledText tw) {
		if (tw == null) {
			return null;
		}
		var result = (Composite) tw.getData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY);
		return result;
	}

	private static void addAnnoModelChangeListener(ITextViewer tv, IAnnotationModel model) {
		// ensure not registered multiple times
		var listener = (UnifiedDiffAnnotationmodelListener) tv.getTextWidget()
				.getData(UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY);
		if (listener != null) {
			return;
		}
		// we need to remove the UnifiedDiffs in the range when the user deletes the
		// ranges manually via keyboard - listen to annotation model changes
		listener = new UnifiedDiffAnnotationmodelListener(tv);
		tv.getTextWidget().setData(UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY, listener);
		model.addAnnotationModelListener(listener);
	}

	private static final class UnifiedDiffAnnotationmodelListener
			implements IAnnotationModelListener, IAnnotationModelListenerExtension {

		private final ITextViewer tv;

		public UnifiedDiffAnnotationmodelListener(ITextViewer tv) {
			this.tv = tv;
		}

		@Override
		public void modelChanged(AnnotationModelEvent event) {
			Annotation[] annos = event.getRemovedAnnotations();
			if (annos == null) {
				return;
			}
			List<UnifiedDiff> unifiedDiffsToDelete = new ArrayList<>();
			for (var anno : annos) {
				UnifiedDiff unifiedDiff = getUnifiedDiffForAnno(anno);
				if (unifiedDiff != null) {
					unifiedDiffsToDelete.add(unifiedDiff);
				}
			}
			if (unifiedDiffsToDelete.size() > 0) {
				Display.getDefault().asyncExec(() -> {
					UnifiedDiff currentToolbarUnifiedDiff = null;
					Composite toolbarCompositeForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
					if (toolbarCompositeForOneDiff != null) {
						var currentToolbarUnifiedDiffAnno = (Annotation) toolbarCompositeForOneDiff
								.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
						currentToolbarUnifiedDiff = getUnifiedDiffForAnno(currentToolbarUnifiedDiffAnno);
					}
					List<UnifiedDiff> container = null;
					IAnnotationModel model = event.getAnnotationModel();
					for (var unifiedDiff : unifiedDiffsToDelete) {
						List<Annotation> annos1 = getAllAnnotationsForUnifiedDiff(model, unifiedDiff);
						for (var lanno : annos1) {
							model.removeAnnotation(lanno);
						}
						if (currentToolbarUnifiedDiff != null && currentToolbarUnifiedDiff.equals(unifiedDiff)) {
							disposeToolbarForOneDiff(toolbarCompositeForOneDiff,
									UnifiedDiffAnnotationmodelListener.this.tv);
						}
						unifiedDiff.container.remove(unifiedDiff);
						container = unifiedDiff.container;
					}
					if (UnifiedDiffAnnotationmodelListener.this.tv instanceof ISourceViewerExtension5 ext) {
						ext.updateCodeMinings();
					}
					if (container != null && container.size() == 0) {
						disposeUnifiedDiff(UnifiedDiffAnnotationmodelListener.this.tv, model, tv.getTextWidget());
					}
				});
			}
		}

		@Override
		public void modelChanged(IAnnotationModel model) {
		}
	}

	public static void error(Exception e) {
		Platform.getLog(UnifiedDiffManager.class).error(e.getMessage(), e);
	}

	static final class UnifiedDiffAnnotation extends Annotation {
		private final UnifiedDiff unifiedDiff;

		public UnifiedDiffAnnotation(UnifiedDiffMode mode, UnifiedDiff unifiedDiff) {
			super(UnifiedDiffMode.REPLACE_MODE.equals(mode) || UnifiedDiffMode.REVERT_MODE.equals(mode)
					? ADDITION_ANNO_TYPE
					: DELETION_ANNO_TYPE, false, null);
			this.unifiedDiff = unifiedDiff;
		}

		public UnifiedDiff getUnifiedDiff() {
			return this.unifiedDiff;
		}
	}

	private static final class DetailedDiffAnnotation extends Annotation {
		private final UnifiedDiff unifiedDiff;

		public DetailedDiffAnnotation(UnifiedDiffMode mode, UnifiedDiff unifiedDiff) {
			super(UnifiedDiffMode.REPLACE_MODE.equals(mode) || UnifiedDiffMode.REVERT_MODE.equals(mode)
					? DETAILED_ADDITION_ANNO_TYPE
					: DETAILED_DELETION_ANNO_TYPE, false, null);
			this.unifiedDiff = unifiedDiff;
		}

		public UnifiedDiff getUnifiedDiff() {
			return this.unifiedDiff;
		}
	}

	private static void drawToolBarForAllDiffs(ITextViewer tv, IAnnotationModel model, List<Action> additionalActions,
			UnifiedDiffMode mode) {
		StyledText tw = tv.getTextWidget();
		var tm = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
		List<UnifiedDiff> diffs = get(tv);
		if (UnifiedDiffMode.OVERLAY_MODE.equals(mode) || UnifiedDiffMode.OVERLAY_READ_ONLY_MODE.equals(mode)) {
			if (!isReadOnly(diffs)) {
				var acceptAll = new AcceptAllRunnable(tv, model);
				addToolbarAction(tm, acceptAll.getLabel(), AcceptAllRunnable.getImageDescriptor(), acceptAll);
			}
			var hideAll = new HideAllDiffsRunnable(tv, model);
			addToolbarAction(tm, hideAll.getLabel(), hideAll.getImageDescriptor(), hideAll);
		} else if (UnifiedDiffMode.REVERT_MODE.equals(mode)) {
			var revertAll = new AcceptAllRunnable(tv, model);
			addToolbarAction(tm, CompareMessages.UnifiedDiff_revert, AcceptAllRunnable.getUndoImageDescriptor(), revertAll);
			var hideAll = new HideAllDiffsRunnable(tv, model);
			addToolbarAction(tm, hideAll.getLabel(), hideAll.getImageDescriptor(), hideAll);
		} else {
			var keepAll = new KeepAllRunnable(tv, model);
			addToolbarAction(tm, keepAll.getLabel(), null, keepAll);
			var undoAll = new UndoAllRunnable(tv, model);
			addToolbarAction(tm, undoAll.getLabel(), null, undoAll);
		}

		var previous = new PreviousRunnable(tv, model, tm);
		addToolbarAction(tm, previous.getLabel(), previous.getImageDescriptor(), previous);
		var next = new NextRunnable(tv, model, tm);
		addToolbarAction(tm, next.getLabel(), next.getImageDescriptor(), next);
		if (additionalActions != null) {
			for (var additionalAction : additionalActions) {
				addToolbarAction(tm, additionalAction);
			}
		}
		if (tm.isEmpty()) {
			return;
		}
		var toolbarCompositeForAllDiffs = getToolbarCompositeForAllDiffs(tv.getTextWidget());
		if (toolbarCompositeForAllDiffs != null && !toolbarCompositeForAllDiffs.isDisposed()) {
			disposeToolbarForAllDiffs(toolbarCompositeForAllDiffs, tv);
		}

		Composite composite = new Composite(tw.getParent(), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(2, 2).spacing(2, 0).applyTo(composite);
		ToolBar fToolBar = tm.createControl(composite);
		composite.pack();

		var setLocationRunnable = (Runnable) () -> {
			Rectangle clientArea = tw.getClientArea();
			Point toolbarSize = fToolBar.getSize();
			// Center horizontally at the bottom of the text widget
			int x = clientArea.x + (clientArea.width - toolbarSize.x) / 2;
			int y = clientArea.y + clientArea.height - toolbarSize.y;
			composite.setLocation(x, y);
		};
		composite.moveAbove(null);
		setLocationRunnable.run();
		composite.setVisible(true);
		tw.setData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY, composite);

		tw.addControlListener(new ControlListener() {

			@Override
			public void controlResized(ControlEvent e) {
				if (fToolBar == null || fToolBar.isDisposed()) {
					tw.removeControlListener(this);
					return;
				}
				setLocationRunnable.run();
			}

			@Override
			public void controlMoved(ControlEvent e) {
			}
		});

		Stream<ToolbarDisposeListenerForOneAndAllDiffs> stream = tw.getTypedListeners(SWT.Dispose,
				ToolbarDisposeListenerForOneAndAllDiffs.class);
		if (stream == null || stream.count() == 0) {
			tw.addDisposeListener(new ToolbarDisposeListenerForOneAndAllDiffs(tv, model));
		}
	}

	private static void clearAll(ITextViewer tv, IAnnotationModel model) {
		List<UnifiedDiff> diffs1 = get(tv);
		if (diffs1 == null) {
			return;
		}
		StyledText tw = tv.getTextWidget();
		removeAnnotationModelListener(model, tw);
		for (UnifiedDiff diff : diffs1) {
			List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
			for (var lanno : annos) {
				model.removeAnnotation(lanno);
			}
		}
		diffs1.clear();
		// open unified diff a second time for the same viewer; the old code minings
		// need to be removed before adding new ones
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
	}

	static void uncheckToolbarActionItems(ToolBarManager tm) {
		IContributionItem[] items = tm.getItems();
		if (items == null) {
			return;
		}
		for (IContributionItem item : items) {
			if (item instanceof ActionContributionItem actionItem) {
				IAction action = actionItem.getAction();
				if (action == null) {
					continue;
				}
				action.setChecked(false);
			}
		}
	}

	public static boolean isOverlay(List<UnifiedDiff> diffs) {
		if (diffs != null && diffs.size() > 0) {
			return diffs.get(0).mode.equals(UnifiedDiffMode.OVERLAY_MODE)
					|| diffs.get(0).mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
		}
		return false;
	}

	public static boolean isRevert(List<UnifiedDiff> diffs) {
		if (diffs != null && diffs.size() > 0) {
			return diffs.get(0).mode.equals(UnifiedDiffMode.REVERT_MODE);
		}
		return false;
	}

	public static boolean isReadOnly(List<UnifiedDiff> diffs) {
		if (diffs != null && diffs.size() > 0) {
			return diffs.get(0).mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE);
		}
		return false;
	}

	private static void applyDiffRightStr(ITextViewer tv, IAnnotationModel model) {
		StyledText tw = tv.getTextWidget();
		Composite toolbarCompositeForOneDiff = getToolbarCompositeForOneDiff(tw);
		if (toolbarCompositeForOneDiff == null) {
			return;
		}
		var anno = (Annotation) toolbarCompositeForOneDiff.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
		if (anno == null) {
			return;
		}
		UnifiedDiff diff = getUnifiedDiffForAnno(anno);
		if (diff == null) {
			return;
		}
		List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
		List<Position> positions = new ArrayList<>();
		List<String> replaceStrings = new ArrayList<>();
		for (var lanno : annos) {
			if (lanno instanceof UnifiedDiffAnnotation) {
				Position pos = model.getPosition(lanno);
				positions.add(pos);
				replaceStrings.add(diff.rightStr);
			}
			model.removeAnnotation(lanno);
		}
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
		// we have to insert with delay because otherwise the line header code minings
		// cannot be deleted
		runAfterRepaintFinished(tw, () -> {
			for (int i = positions.size() - 1; i >= 0; i--) {
				Position pos = positions.get(i);
				String replaceStr = replaceStrings.get(i);
				try {
					tv.getDocument().replace(pos.offset, pos.length, replaceStr);
				} catch (BadLocationException e) {
					error(e);
				}
			}
		});
	}

	/**
	 * Dismisses the current diff (navigates to the next one or disposes everything
	 * when it is the last one). Used by the "Hide Diff" (overlay/revert) and "Keep"
	 * (replace) toolbar actions.
	 */
	private static void dismissCurrentDiff(ITextViewer tv, IAnnotationModel model) {
		StyledText tw = tv.getTextWidget();
		Composite toolbarCompositeForOneDiff = getToolbarCompositeForOneDiff(tw);
		if (toolbarCompositeForOneDiff == null) {
			return;
		}
		var anno = (Annotation) toolbarCompositeForOneDiff.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
		UnifiedDiff diff = getUnifiedDiffForAnno(anno);
		int idx = diff.container.indexOf(diff);
		if (idx < 0) {
			throw new IllegalStateException("UnifiedDiff not found in container"); //$NON-NLS-1$
		}
		idx++;
		if (idx >= diff.container.size()) {
			idx = 0;
		}
		UnifiedDiff next = diff.container.get(idx);
		if (next == diff) {
			disposeUnifiedDiff(tv, model, tw);
		} else {
			List<Annotation> nextAnnos = getAllAnnotationsForUnifiedDiff(model, next);
			if (nextAnnos.size() > 0) {
				disposeToolbarForOneDiff(toolbarCompositeForOneDiff, tv);
				Position pos = getMinPosition(model, nextAnnos);
				tv.revealRange(pos.offset, pos.length);
				tv.setSelectedRange(pos.offset, 0);
				// we can update the toolbar location after the line header code minings are
				// removed
				runAfterRepaintFinished(tw, () -> setToolbarLocationForOneDiff(tv, model, nextAnnos.get(0)));
			}
		}
		diff.container.remove(diff);
		List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
		for (var lanno : annos) {
			model.removeAnnotation(lanno);
		}
		if (tv instanceof ISourceViewerExtension5 ext) {
			ext.updateCodeMinings();
		}
	}

	private static void drawToolbarForOneDiff(ITextViewer tv, IAnnotationModel model) {
		List<UnifiedDiff> diffs = get(tv);
		StyledText tw = tv.getTextWidget();
		var tm = new ToolBarManager(SWT.FLAT | SWT.HORIZONTAL | SWT.RIGHT);
		if (isOverlay(diffs)) {
			if (!isReadOnly(diffs)) {
				addToolbarAction(tm, CompareMessages.UnifiedDiff_accept, AcceptAllRunnable.getImageDescriptor(),
						() -> applyDiffRightStr(tv, model));
			}
			addToolbarAction(tm, CompareMessages.UnifiedDiff_hideDiff, HideAllDiffsRunnable.getHideDiffImageDescriptor(),
					() -> dismissCurrentDiff(tv, model));
		} else if (isRevert(diffs)) {
			addToolbarAction(tm, CompareMessages.UnifiedDiff_revert, AcceptAllRunnable.getUndoImageDescriptor(),
					() -> applyDiffRightStr(tv, model));
			addToolbarAction(tm, CompareMessages.UnifiedDiff_hideDiff, HideAllDiffsRunnable.getHideDiffImageDescriptor(),
					() -> dismissCurrentDiff(tv, model));
		} else {
			addToolbarAction(tm, CompareMessages.UnifiedDiff_keep, null, () -> dismissCurrentDiff(tv, model));
			addToolbarAction(tm, CompareMessages.UnifiedDiff_undo, null, () -> {
				var fToolbarShellForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
				if (fToolbarShellForOneDiff == null) {
					return;
				}
				var anno = (Annotation) fToolbarShellForOneDiff.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
				UnifiedDiff diff = getUnifiedDiffForAnno(anno);
				int idx = diff.container.indexOf(diff);
				if (idx < 0) {
					return;
				}
				idx++;
				if (idx >= diff.container.size()) {
					idx = 0;
				}
				UnifiedDiff next = diff.container.get(idx);
				final Annotation[] nextAnno = new Annotation[] { null };
				if (next == diff) {
					disposeUnifiedDiff(tv, model, tv.getTextWidget());
				} else {
					nextAnno[0] = getFirstAnnotationForUnifiedDiff(model, next);
				}
				diff.container.remove(diff);
				List<Annotation> annos = getAllAnnotationsForUnifiedDiff(model, diff);
				int unifiedDiffAdditionCount = 0;
				for (var lanno : annos) {
					if (lanno instanceof UnifiedDiffAnnotation) {
						unifiedDiffAdditionCount++;
						if (unifiedDiffAdditionCount > 1) {
							throw new IllegalStateException(
									"Multiple UnifiedDiffAdditionAnnotation for one UnifiedDiff found"); //$NON-NLS-1$
						}
						Position pos = model.getPosition(lanno);
						// we have to insert with delay because otherwise the line header code minings
						// cannot be deleted
						runAfterRepaintFinished(tw, () -> {
							try {
								tv.getDocument().replace(pos.offset, pos.length, diff.leftStr);
								if (nextAnno[0] != null) {
									disposeToolbarForOneDiff(fToolbarShellForOneDiff, tv);
									Position pos1 = model.getPosition(nextAnno[0]);
									tv.revealRange(pos1.offset, pos1.length);
									tv.setSelectedRange(pos1.offset, 0);
									setToolbarLocationForOneDiff(tv, model, nextAnno[0]);
								}
							} catch (BadLocationException e) {
								error(e);
							}
						});
					}
					model.removeAnnotation(lanno);
				}
				if (unifiedDiffAdditionCount == 0) {
					throw new IllegalStateException("No UnifiedDiffAdditionAnnotation for UnifiedDiff found"); //$NON-NLS-1$
				}
				if (tv instanceof ISourceViewerExtension5 ext) {
					ext.updateCodeMinings();
				}
			});
		}
		if (tm.isEmpty()) {
			return;
		}
		var fToolbarShellForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
		if (fToolbarShellForOneDiff != null && !fToolbarShellForOneDiff.isDisposed()) {
			disposeToolbarForOneDiff(fToolbarShellForOneDiff, tv);
		}

		Composite composite = new Composite(tw.getParent(), SWT.NONE);
		GridDataFactory.fillDefaults().grab(true, true).align(GridData.FILL, GridData.FILL).applyTo(composite);
		GridLayoutFactory.fillDefaults().numColumns(2).equalWidth(false).margins(2, 2).spacing(2, 0).applyTo(composite);
		tm.createControl(composite);
		composite.pack();
		composite.moveAbove(null);
		tw.setData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY, composite);

		ScrollBar verticalBar = tw.getVerticalBar();
		if (verticalBar != null) {
			Stream<VerticalBarSelectionAdapter> stream = verticalBar.getTypedListeners(SWT.Selection,
					VerticalBarSelectionAdapter.class);
			if (stream == null || stream.count() == 0) {
				verticalBar.addSelectionListener(new VerticalBarSelectionAdapter(tv, model));
			}
		}
		Stream<ToolbarDisposeListenerForOneAndAllDiffs> stream = tw.getTypedListeners(SWT.Dispose,
				ToolbarDisposeListenerForOneAndAllDiffs.class);
		if (stream == null || stream.count() == 0) {
			tw.addDisposeListener(new ToolbarDisposeListenerForOneAndAllDiffs(tv, model));
		}
	}

	private static final class VerticalBarSelectionAdapter extends SelectionAdapter {
		private final IAnnotationModel model;
		private final ITextViewer tv;

		public VerticalBarSelectionAdapter(ITextViewer tv, IAnnotationModel model) {
			this.tv = tv;
			this.model = model;
		}

		@Override
		public void widgetSelected(SelectionEvent e) {
			var fToolbarShellForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
			if (fToolbarShellForOneDiff == null || fToolbarShellForOneDiff.isDisposed()) {
				return;
			}
			var anno = (Annotation) fToolbarShellForOneDiff.getData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY);
			if (anno != null) {
				setToolbarLocationForOneDiff(this.tv, this.model, anno);
			}
		}
	}

	private static final class ToolbarDisposeListenerForOneAndAllDiffs implements DisposeListener {

		private final ITextViewer tv;
		private final IAnnotationModel model;

		public ToolbarDisposeListenerForOneAndAllDiffs(ITextViewer tv, IAnnotationModel model) {
			this.tv = tv;
			this.model = model;
		}

		@Override
		public void widgetDisposed(DisposeEvent e) {
			disposeUnifiedDiff(this.tv, this.model, (StyledText) e.getSource());
		}
	}

	private static UnifiedDiff getUnifiedDiffForAnno(Annotation anno) {
		if (anno instanceof DetailedDiffAnnotation da) {
			return da.getUnifiedDiff();
		} else if (anno instanceof UnifiedDiffAnnotation a) {
			return a.getUnifiedDiff();
		} else if (anno instanceof AbstractInlinedAnnotation inlineAnno) {
			List<ICodeMining> minings = inlineAnno.getMinings();
			if (minings.size() == 1 && minings.get(0) instanceof UnifiedDiffLineHeaderCodeMining idlhcm) {
				return idlhcm.getUnifiedDiff();
			}
		}
		return null;
	}

	private static void disposeToolbarForOneDiff(Composite fToolbarCompositeForOneDiff, ITextViewer tv) {
		if (fToolbarCompositeForOneDiff != null) {
			fToolbarCompositeForOneDiff.dispose();
		}
		if (tv == null) {
			return;
		}
		var tw = tv.getTextWidget();
		if (tw == null) {
			return;
		}
		tw.setData(TOOLBAR_COMPOSITE_FOR_ONE_DIFF_KEY, null);
	}

	private static void disposeToolbarForAllDiffs(Composite toolbarShellForAllDiffs, ITextViewer tv) {
		if (toolbarShellForAllDiffs != null) {
			toolbarShellForAllDiffs.dispose();
		}
		if (tv == null) {
			return;
		}
		var tw = tv.getTextWidget();
		if (tw == null) {
			return;
		}
		tw.setData(TOOLBAR_COMPOSITE_FOR_ALL_DIFFS_KEY, null);
	}

	private static void setToolbarLocationForOneDiff(ITextViewer tv, IAnnotationModel model, Annotation anno) {
		var fToolbarShellForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
		if (fToolbarShellForOneDiff == null || fToolbarShellForOneDiff.isDisposed()) {
			drawToolbarForOneDiff(tv, model);
			fToolbarShellForOneDiff = getToolbarCompositeForOneDiff(tv.getTextWidget());
			if (fToolbarShellForOneDiff == null) {
				return;
			}
		}
		UnifiedDiff unifiedDiff = getUnifiedDiffForAnno(anno);
		if (unifiedDiff == null) {
			return;
		}
		List<Annotation> all = getAllAnnotationsForUnifiedDiff(model, unifiedDiff);
		if (all.size() == 0) {
			return;
		}
		Position pos = getMinPosition(model, all);
		fToolbarShellForOneDiff.setData(CURRENT_SELECTED_UNIFIED_DIFF_ANNO_KEY, anno);
		StyledText tw = tv.getTextWidget();
		Rectangle clientArea = tw.getClientArea();

		int startOffset = pos.offset;
		if (tv instanceof ProjectionViewer pv) {
			startOffset = pv.modelOffset2WidgetOffset(pos.offset);
			if (startOffset < 0) {
				return; // not visible
			}
		}
		if (startOffset >= tw.getCharCount()) {
			// ensure no out of bounds
			startOffset = tw.getCharCount() - 1;
		}
		Rectangle startBounds = tw.getTextBounds(startOffset, startOffset);
		if (!startBounds.intersects(clientArea)) {
			fToolbarShellForOneDiff.setVisible(false);
			return;
		}
		Rectangle twBounds = tw.getBounds();
		Point verticalBarSize = tw.getVerticalBar().getSize();
		fToolbarShellForOneDiff.setLocation(
				twBounds.x + twBounds.width - fToolbarShellForOneDiff.getBounds().width - verticalBarSize.x,
				startBounds.y);
		fToolbarShellForOneDiff.setVisible(true);
	}

	private static Position getMinPosition(IAnnotationModel model, List<Annotation> all) {
		Position min = null;
		for (Annotation ann : all) {
			Position position = model.getPosition(ann);
			if (min == null) {
				min = position;
			} else if (position.offset < min.offset) {
				min = position;
			}
		}
		return min;
	}

	static Annotation getMinPositionAnno(IAnnotationModel model, List<Annotation> all) {
		Position min = null;
		Annotation result = null;
		for (Annotation ann : all) {
			Position position = model.getPosition(ann);
			if (min == null) {
				min = position;
				result = ann;
			} else if (position.offset < min.offset) {
				min = position;
				result = ann;
			}
		}
		return result;
	}

	private static void addToolbarAction(ToolBarManager tm, String text, ImageDescriptor image, Runnable runnable) {
		String tooltip = text;
		Action action = null;
		if (image != null) {
			action = new Action(null, image) { // $NON-NLS-1$
				@Override
				public void run() {
					setChecked(false);
					runnable.run();
				}
			};
		} else {
			action = new Action(text, SWT.PUSH) { // $NON-NLS-1$
				@Override
				public void run() {
					setChecked(false);
					runnable.run();
				}
			};
		}
		action.setToolTipText(tooltip);
		var actionItem = new ActionContributionItem(action);
		actionItem.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		tm.add(actionItem);
		tm.add(new Separator());
	}

	private static void addToolbarAction(ToolBarManager tm, Action action) {
		Action a = null;
		if (action.getImageDescriptor() != null) {
			a = new Action(null, action.getImageDescriptor()) {
				@Override
				public void run() {
					setChecked(false);
					action.run();
				}
			};
		} else {
			a = new Action(action.getText(), SWT.PUSH) { // $NON-NLS-1$
				@Override
				public void run() {
					setChecked(false);
					action.run();
				}
			};
		}
		a.setToolTipText(action.getToolTipText());
		var actionItem = new ActionContributionItem(a);
		actionItem.setMode(ActionContributionItem.MODE_FORCE_TEXT);
		tm.add(actionItem);
		tm.add(new Separator());
	}

	private static void addMouseMoveListener(ITextViewer tv, IAnnotationModel model) {
		StyledText textWidget = tv.getTextWidget();
		Stream<UnifiedDiffMouseMoveListener> stream = textWidget.getTypedListeners(SWT.MouseMove,
				UnifiedDiffMouseMoveListener.class);
		if (stream != null && stream.count() > 0) {
			return;
		}
		textWidget.addMouseMoveListener(new UnifiedDiffMouseMoveListener(model, tv));
	}

	private static final class UnifiedDiffMouseMoveListener implements MouseMoveListener {
		private final IAnnotationModel model;
		private final ITextViewer tv;
		private final StyledText textWidget;

		public UnifiedDiffMouseMoveListener(IAnnotationModel model, ITextViewer tv) {
			this.model = model;
			this.tv = tv;
			this.textWidget = tv.getTextWidget();
		}

		@Override
		public void mouseMove(MouseEvent e) {
			Iterator<Annotation> it = this.model.getAnnotationIterator();
			while (it.hasNext()) {
				Annotation anno = getUnifiedDiffAnnotationFromIterator(it);
				if (anno == null) {
					continue;
				}
				Position pos = this.model.getPosition(anno);
				int startOffset = pos.offset;
				if (tv instanceof ProjectionViewer pv) {
					startOffset = pv.modelOffset2WidgetOffset(pos.offset);
				}
				int endOffset = startOffset + pos.length;
				try {
					Rectangle startBounds = this.textWidget.getTextBounds(startOffset, startOffset);
					Rectangle endBounds = this.textWidget.getTextBounds(endOffset, endOffset);
					if (startBounds.y == endBounds.y) {
						endBounds.y += endBounds.height;
					}
					if (e.y >= startBounds.y && e.y <= endBounds.y) {
						setToolbarLocationForOneDiff(this.tv, this.model, anno);
						return;
					}
				} catch (IllegalArgumentException ex) { // NOPMD silently ignored
				}
			}
		}
	}

	private static Annotation getFirstAnnotationForUnifiedDiff(IAnnotationModel model, UnifiedDiff unifiedDiff) {
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation anno = getUnifiedDiffAnnotationFromIterator(it);
			if (anno == null) {
				continue;
			}
			UnifiedDiff unifiedDiffForAnno = getUnifiedDiffForAnno(anno);
			if (unifiedDiffForAnno == unifiedDiff) {
				return anno;
			}
		}
		return null;
	}

	static List<Annotation> getAllAnnotationsForUnifiedDiff(IAnnotationModel model, UnifiedDiff unifiedDiff) {
		List<Annotation> result = new ArrayList<>();
		Iterator<Annotation> it = model.getAnnotationIterator();
		while (it.hasNext()) {
			Annotation anno = getUnifiedDiffAnnotationFromIterator(it);
			if (anno == null) {
				continue;
			}
			UnifiedDiff unifiedDiffForAnno = getUnifiedDiffForAnno(anno);
			if (unifiedDiffForAnno == unifiedDiff) {
				result.add(anno);
			}
		}
		return result;
	}

	private static Annotation getUnifiedDiffAnnotationFromIterator(Iterator<Annotation> it) {
		boolean doit = false;
		Annotation anno = it.next();
		if (anno instanceof AbstractInlinedAnnotation inlineAnno) {
			List<ICodeMining> minings = inlineAnno.getMinings();
			if (minings.size() == 1 && minings.get(0) instanceof UnifiedDiffLineHeaderCodeMining) {
				doit = true;
			}
		} else if (ADDITION_ANNO_TYPE.equals(anno.getType()) || DELETION_ANNO_TYPE.equals(anno.getType())
				|| DETAILED_ADDITION_ANNO_TYPE.equals(anno.getType())
				|| DETAILED_DELETION_ANNO_TYPE.equals(anno.getType())) {
			doit = true;
		}
		if (!doit) {
			return null;
		}
		return anno;
	}

	private static void addUndoListener(ITextViewer tv, IDocument document, IAnnotationModel model) {
		IDocumentUndoManager documentUndoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
		var undoListener = (IDocumentUndoListener) event -> {
			UnifiedDiff toBeDeletedDiff = null;
			Iterator<Annotation> it = model.getAnnotationIterator();
			while (it.hasNext()) {
				Annotation anno = getUnifiedDiffAnnotationFromIterator(it);
				if (anno == null) {
					continue;
				}
				Position pos = model.getPosition(anno);
				if (pos.offset == event.getOffset()) {
					UnifiedDiff diff = getUnifiedDiffForAnno(anno);
					if (diff != null && diff.rightStr.equals(event.getPreservedText())) {
						toBeDeletedDiff = diff;
						break;
					}
				}
			}
			if (toBeDeletedDiff == null) {
				return;
			}
			// delete the uni diff
			List<Annotation> all = getAllAnnotationsForUnifiedDiff(model, toBeDeletedDiff);
			for (var lanno : all) {
				model.removeAnnotation(lanno);
			}
			toBeDeletedDiff.container.remove(toBeDeletedDiff);
			if (tv instanceof ISourceViewerExtension5 ext) {
				ext.updateCodeMinings();
			}
			if (toBeDeletedDiff.container.isEmpty()) {
				disposeUnifiedDiff(tv, model, tv.getTextWidget());
			}
		};
		tv.getTextWidget().setData(UNDO_LISTENER_KEY, undoListener);
		documentUndoManager.addDocumentUndoListener(undoListener);
	}

	static void disposeUnifiedDiff(ITextViewer tv, IAnnotationModel model, StyledText tw) {
		disposeToolbarForOneDiff(getToolbarCompositeForOneDiff(tw), tv);
		disposeToolbarForAllDiffs(getToolbarCompositeForAllDiffs(tw), tv);
		var undoListener = (IDocumentUndoListener) tw.getData(UNDO_LISTENER_KEY);
		if (undoListener != null) {
			tw.setData(UNDO_LISTENER_KEY, null);
			IDocument document = tv.getDocument();
			if (document != null) {
				IDocumentUndoManager documentUndoManager = DocumentUndoManagerRegistry.getDocumentUndoManager(document);
				documentUndoManager.removeDocumentUndoListener(undoListener);
			}
		}
		removeAnnotationModelListener(model, tw);

		diffsByViewer.remove(tv);

		if (tw == null || tw.isDisposed()) {
			// SWT removes listeners automatically when the widget is disposed
			return;
		}
		tw.getTypedListeners(SWT.MouseMove, UnifiedDiffMouseMoveListener.class)
				.forEach(tw::removeMouseMoveListener);
		tw.getTypedListeners(SWT.Paint, UnifiedDiffPaintListener.class)
				.forEach(listener -> {
					tw.removePaintListener(listener);
					listener.dispose();
				});
	}

	static void removeAnnotationModelListener(IAnnotationModel model, StyledText tw) {
		if (model == null || tw == null || tw.isDisposed()) {
			return;
		}
		var listener = (UnifiedDiffAnnotationmodelListener) tw.getData(UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY);
		if (listener != null) {
			tw.setData(UNIFIED_DIFF_ANNOTATION_MODEL_LISTENER_KEY, null);
			model.removeAnnotationModelListener(listener);
		}
	}

	private static void addPaintListener(ITextViewer viewer, IAnnotationModel model, UnifiedDiffMode mode) {
		StyledText w = viewer.getTextWidget();
		Stream<UnifiedDiffPaintListener> stream = w.getTypedListeners(SWT.Paint, UnifiedDiffPaintListener.class);
		stream.forEach(e -> {
			w.removePaintListener(e);
			e.dispose();
		});
		w.addPaintListener(new UnifiedDiffPaintListener(viewer, model, mode));
	}

	private static class UnifiedDiffPaintListener implements PaintListener {

		private final IAnnotationModel model;
		private final ITextViewer viewer;
		private final Color additionBackgroundColor;
		private final StyledText w;

		public UnifiedDiffPaintListener(ITextViewer viewer, IAnnotationModel model, UnifiedDiffMode mode) {
			this.model = model;
			this.viewer = viewer;
			w = viewer.getTextWidget();
			RGB color;
			if (mode.equals(UnifiedDiffMode.OVERLAY_MODE) || mode.equals(UnifiedDiffMode.OVERLAY_READ_ONLY_MODE)) {
				color = JFaceResources.getColorRegistry().getRGB("DELETION_COLOR"); //$NON-NLS-1$
			} else {
				color = JFaceResources.getColorRegistry().getRGB("ADDITION_COLOR"); //$NON-NLS-1$
			}
			RGB background = UnifiedDiffCodeMiningProvider.getBackground();
			RGB interpolated = UnifiedDiffCodeMiningProvider.interpolate(color, background, 0.9);
			this.additionBackgroundColor = new Color(interpolated);
		}

		void dispose() {
			if (additionBackgroundColor != null && !additionBackgroundColor.isDisposed()) {
				additionBackgroundColor.dispose();
			}
		}

		@Override
		public void paintControl(PaintEvent e) {
			Rectangle bounds = this.w.getBounds();
			Iterator<Annotation> it = this.model.getAnnotationIterator();
			while (it.hasNext()) {
				Annotation anno = it.next();
				if (!(ADDITION_ANNO_TYPE.equals(anno.getType()) || DELETION_ANNO_TYPE.equals(anno.getType()))) {
					continue;
				}
				// Fill the area between the last character of the line and the widget boundary.
				// This is not done via the Annotations.
				int posOffset;
				int posLength;
				{
					Position pos = this.model.getPosition(anno);
					if (viewer instanceof ProjectionViewer pv) {
						posOffset = pv.modelOffset2WidgetOffset(pos.offset);
						if (posOffset < 0) {
							continue; // not visible
						}
					} else {
						posOffset = pos.offset;
					}
					posLength = pos.length;
				}
				int fromLine = this.w.getLineAtOffset(posOffset);
				int toLine = this.w.getLineAtOffset(posOffset + posLength);
				e.gc.setBackground(this.additionBackgroundColor);
				for (int lineNr = fromLine; lineNr < toLine; lineNr++) {
					String line = this.w.getLine(lineNr);
					int endLineOffset = this.w.getOffsetAtLine(lineNr) + line.length();
					Rectangle endLineBounds = this.w.getTextBounds(endLineOffset, endLineOffset);
					endLineBounds.height += this.w.getLineSpacing();
					if (line.length() == 0) {
						// this.w.getTextBounds seems not to work for empty lines containing only \n in
						// a document with \r\n as line delimeter
						int nextLineOffset = this.w.getOffsetAtLine(lineNr + 1);
						Rectangle nextLineBounds = this.w.getTextBounds(nextLineOffset, nextLineOffset);
						if (endLineBounds.y + endLineBounds.height < nextLineBounds.y) {
							endLineBounds.height = nextLineBounds.y - endLineBounds.y;
						}
					}
					e.gc.fillRectangle(endLineBounds.x + endLineBounds.width, endLineBounds.y,
							bounds.width - (endLineBounds.x + endLineBounds.width), endLineBounds.height);

					// annotation painter is not taking line spacing into account - we therefore
					// need to draw it by our own
					e.gc.fillRectangle(2, endLineBounds.y + endLineBounds.height - this.w.getLineSpacing(),
							endLineBounds.x + endLineBounds.width, this.w.getLineSpacing());
				}
			}
		}
	}

	private static boolean validateEdit(IFile file) {
		final boolean result[] = new boolean[] { false };
		try {
			final IFile fFile = file;
			ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable) monitor -> {
				IStatus status = ResourcesPlugin.getWorkspace().validateEdit(new IFile[] { fFile }, null);
				if (status != null) {
					if (status.isOK()) {
						result[0] = true;
					} else {
						MessageDialog.openError(Display.getDefault().getActiveShell(),
								CompareMessages.UnifiedDiff_cannotEditFile_title,
								NLS.bind(CompareMessages.UnifiedDiff_cannotEditFile_message, fFile.getFullPath(),
										status.getMessage()));
					}
				}
			}, new NullProgressMonitor());
		} catch (CoreException e) {
			error(e);
		}
		return result[0];
	}

	private static ITokenComparator createTokenComparator(String line, TokenComparatorFactory tokenComparatorFactory) {
		ITokenComparator result = null;
		if (tokenComparatorFactory != null) {
			result = tokenComparatorFactory.apply(line);
		}
		if (result == null) {
			result = new TokenComparator(line);
		}
		return result;
	}

	private static int getTokenEnd(ITokenComparator tc, int start, int length) {
		return tc.getTokenStart(start + length);
	}

	static void selectAndRevealAnno(ITextViewer tv, IAnnotationModel model, Annotation nextAnno) {
		disposeToolbarForOneDiff(getToolbarCompositeForOneDiff(tv.getTextWidget()), tv);
		Position pos = model.getPosition(nextAnno);
		tv.revealRange(pos.offset, pos.length);
		tv.setSelectedRange(pos.offset, 0);
		Display.getDefault().asyncExec(() -> setToolbarLocationForOneDiff(tv, model, nextAnno));
	}

	static void runAfterRepaintFinished(StyledText tw, Runnable runnable) {
		tw.addPaintListener(new PaintListener() {
			private final PaintListener me = this;
			private final Runnable r = () -> {
				if (tw.isDisposed()) {
					return;
				}
				tw.removePaintListener(me);
				runnable.run();
			};

			@Override
			public void paintControl(PaintEvent e) {
				Display.getDefault().timerExec(100, this.r);
			}
		});
	}

	public static class UnifiedDiff {
		public IDocument left;
		public int leftStart;
		int leftLength;
		public final String leftStr;

		IDocument right;
		int rightStart;
		int rightLength;
		String rightStr;
		public final List<UnifiedDiff> detailedDiffs = new ArrayList<>();
		final List<UnifiedDiff> container;
		public final UnifiedDiffMode mode;

		public UnifiedDiff(IDocument left, int leftStart, int leftEnd, String leftDiffSource, IDocument right,
				int rightStart, int rightEnd, String rightDiffSource, List<UnifiedDiff> container,
				UnifiedDiffMode mode) {
			this.left = left;
			this.leftStart = leftStart;
			this.leftLength = leftEnd - leftStart;
			this.leftStr = leftDiffSource;
			this.right = right;
			this.rightStart = rightStart;
			this.rightLength = rightEnd - rightStart;
			this.rightStr = rightDiffSource;
			this.container = container;
			this.mode = mode;
		}
	}
}
