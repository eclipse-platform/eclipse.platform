/*******************************************************************************
 * Copyright (c) 2000, 2024 IBM Corporation and others.
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
 *     channingwalton@mac.com - curved line code
 *     gilles.querret@free.fr - fix for https://bugs.eclipse.org/bugs/show_bug.cgi?id=72995
 *     Max Weninger (max.weninger@windriver.com) - Bug 131895 [Edit] Undo in compare
 *     Max Weninger (max.weninger@windriver.com) - Bug 72936 [Viewers] Show line numbers in comparision
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 178968 [Viewers] Lines scrambled and different font size in compare
 *     Matt McCutchen (hashproduct+eclipse@gmail.com) - Bug 191524 [Viewers] Synchronize horizontal scrolling by # characters, not % of longest line
 *     Stephan Herrmann (stephan@cs.tu-berlin.de) - Bug 291695: Element compare fails to use source range
 *     Robin Stocker (robin@nibor.org) - Bug 398594: [Edit] Enable center arrow buttons when editable and for both sides
 *     Robin Stocker (robin@nibor.org) - Bug 399960: [Edit] Make merge arrow buttons easier to hit
 *     John Hendrikx (hjohn@xs4all.nl) - Bug 541401 - [regression] Vertical scrollbar thumb size is wrong in compare view
 *     Stefan Dirix (sdirix@eclipsesource.com) - Bug 473847: Minimum E4 Compatibility of Compare
 *     Latha Patil (ETAS GmbH) - Issue #504 Show number of differences in the Compare editor
 *******************************************************************************/
package org.eclipse.compare.contentmergeviewer;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareNavigator;
import org.eclipse.compare.CompareUI;
import org.eclipse.compare.ICompareNavigator;
import org.eclipse.compare.IEditableContentExtension;
import org.eclipse.compare.IEncodedStreamContentAccessor;
import org.eclipse.compare.INavigatable;
import org.eclipse.compare.ISharedDocumentAdapter;
import org.eclipse.compare.IStreamContentAccessor;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.LabelContributionItem;
import org.eclipse.compare.SharedDocumentAdapter;
import org.eclipse.compare.internal.ChangeCompareFilterPropertyAction;
import org.eclipse.compare.internal.ChangePropertyAction;
import org.eclipse.compare.internal.CompareContentViewerSwitchingPane;
import org.eclipse.compare.internal.CompareEditor;
import org.eclipse.compare.internal.CompareEditorContributor;
import org.eclipse.compare.internal.CompareEditorSelectionProvider;
import org.eclipse.compare.internal.CompareFilterDescriptor;
import org.eclipse.compare.internal.CompareHandlerService;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.DocumentManager;
import org.eclipse.compare.internal.ICompareContextIds;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.IMergeViewerTestAdapter;
import org.eclipse.compare.internal.MergeSourceViewer;
import org.eclipse.compare.internal.MergeViewerContentProvider;
import org.eclipse.compare.internal.NavigationEndDialog;
import org.eclipse.compare.internal.OutlineViewerCreator;
import org.eclipse.compare.internal.ShowWhitespaceAction;
import org.eclipse.compare.internal.TextEditorPropertyAction;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.merge.DocumentMerger;
import org.eclipse.compare.internal.merge.DocumentMerger.Diff;
import org.eclipse.compare.internal.merge.DocumentMerger.IDocumentMergerInput;
import org.eclipse.compare.patch.IHunk;
import org.eclipse.compare.rangedifferencer.RangeDifference;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.DocumentRangeNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.commands.operations.IOperationHistoryListener;
import org.eclipse.core.commands.operations.IUndoableOperation;
import org.eclipse.core.commands.operations.OperationHistoryEvent;
import org.eclipse.core.commands.operations.OperationHistoryFactory;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.preference.PreferenceConverter;
import org.eclipse.jface.resource.ColorRegistry;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.BadPositionCategoryException;
import org.eclipse.jface.text.CursorLinePainter;
import org.eclipse.jface.text.DefaultPositionUpdater;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentExtension3;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.IFindReplaceTargetExtension;
import org.eclipse.jface.text.IFindReplaceTargetExtension3;
import org.eclipse.jface.text.IPositionUpdater;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.IRewriteTarget;
import org.eclipse.jface.text.ITextPresentationListener;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.TextPresentation;
import org.eclipse.jface.text.TextViewer;
import org.eclipse.jface.text.source.CompositeRuler;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.Util;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseMoveListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.ScrollBar;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IKeyBindingService;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.editors.text.IEncodingSupport;
import org.eclipse.ui.editors.text.IStorageDocumentProvider;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditorPreferenceConstants;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.ChainedPreferenceStore;
import org.eclipse.ui.texteditor.ChangeEncodingAction;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.GotoLineAction;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.IDocumentProviderExtension;
import org.eclipse.ui.texteditor.IElementStateListener;
import org.eclipse.ui.texteditor.IFindReplaceTargetExtension2;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.eclipse.ui.texteditor.SourceViewerDecorationSupport;

/**
 * A text merge viewer uses the <code>RangeDifferencer</code> to perform a
 * textual, line-by-line comparison of two (or three) input documents. It is
 * based on the <code>ContentMergeViewer</code> and uses
 * <code>TextViewer</code>s to implement the ancestor, left, and right content
 * areas.
 * <p>
 * In the three-way compare case ranges of differing lines are highlighted and
 * framed with different colors to show whether the difference is an incoming,
 * outgoing, or conflicting change. The <code>TextMergeViewer</code> supports
 * the notion of a current "differing range" and provides toolbar buttons to
 * navigate from one range to the next (or previous).
 * <p>
 * If there is a current "differing range" and the underlying document is
 * editable the <code>TextMergeViewer</code> enables actions in context menu and
 * toolbar to copy a range from one side to the other side, thereby performing a
 * merge operation.
 * <p>
 * In addition to a line-by-line comparison the <code>TextMergeViewer</code>
 * uses a token based compare on differing lines. The token compare is activated
 * when navigating into a range of differing lines. At first the lines are
 * selected as a block. When navigating into this block the token compare shows
 * for every line the differing token by selecting them.
 * <p>
 * The <code>TextMergeViewer</code>'s default token compare works on characters
 * separated by whitespace. If a different strategy is needed (for example, Java
 * tokens in a Java-aware merge viewer), clients can create their own token
 * comparators by implementing the <code>ITokenComparator</code> interface and
 * overriding the <code>TextMergeViewer.createTokenComparator</code> factory
 * method).
 * <p>
 * Access to the <code>TextMergeViewer</code>'s model is by means of an
 * <code>IMergeViewerContentProvider</code>. Its <code>getXContent</code>
 * methods must return either an <code>IDocument</code>, an
 * <code>IDocumentRange</code>, or an <code>IStreamContentAccessor</code>. In
 * the <code>IDocumentRange</code> case the <code>TextMergeViewer</code> works
 * on a subrange of a document. In the <code>IStreamContentAccessor</code> case
 * a document is created internally and initialized from the stream.
 * <p>
 * A <code>TextMergeViewer</code> can be used as is. However clients may
 * subclass to customize the behavior. For example a
 * <code>MergeTextViewer</code> for Java would override the
 * <code>configureTextViewer</code> method to configure the
 * <code>TextViewer</code> for Java source code, the
 * <code>createTokenComparator</code> method to create a Java specific
 * tokenizer.
 * <p>
 * In 3.5 a new API has been introduced to let clients provide their own source
 * viewers implementation with an option to configure them basing on a
 * corresponding editor input.
 *
 * @see org.eclipse.compare.rangedifferencer.RangeDifferencer
 * @see org.eclipse.jface.text.TextViewer
 * @see ITokenComparator
 * @see IDocumentRange
 * @see org.eclipse.compare.IStreamContentAccessor
 */
public class TextMergeViewer extends ContentMergeViewer implements IAdaptable {
	private static final String COPY_LEFT_TO_RIGHT_INDICATOR = ">"; //$NON-NLS-1$
	private static final String COPY_RIGHT_TO_LEFT_INDICATOR = "<"; //$NON-NLS-1$
	private static final char ANCESTOR_CONTRIBUTOR = MergeViewerContentProvider.ANCESTOR_CONTRIBUTOR;
	private static final char RIGHT_CONTRIBUTOR = MergeViewerContentProvider.RIGHT_CONTRIBUTOR;
	private static final char LEFT_CONTRIBUTOR = MergeViewerContentProvider.LEFT_CONTRIBUTOR;

	private static final String DIFF_RANGE_CATEGORY = CompareUIPlugin.PLUGIN_ID + ".DIFF_RANGE_CATEGORY"; //$NON-NLS-1$

	static final boolean DEBUG= false;

	private static final boolean FIX_47640= true;

	private static final String[] GLOBAL_ACTIONS= {
		ActionFactory.UNDO.getId(),
		ActionFactory.REDO.getId(),
		ActionFactory.CUT.getId(),
		ActionFactory.COPY.getId(),
		ActionFactory.PASTE.getId(),
		ActionFactory.DELETE.getId(),
		ActionFactory.SELECT_ALL.getId(),
		ActionFactory.FIND.getId(),
		ITextEditorActionDefinitionIds.LINE_GOTO,
		ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS
	};
	private static final String[] TEXT_ACTIONS= {
		MergeSourceViewer.UNDO_ID,
		MergeSourceViewer.REDO_ID,
		MergeSourceViewer.CUT_ID,
		MergeSourceViewer.COPY_ID,
		MergeSourceViewer.PASTE_ID,
		MergeSourceViewer.DELETE_ID,
		MergeSourceViewer.SELECT_ALL_ID,
		MergeSourceViewer.FIND_ID,
		MergeSourceViewer.GOTO_LINE_ID,
		ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS
	};

	private static final String BUNDLE_NAME= "org.eclipse.compare.contentmergeviewer.TextMergeViewerResources"; //$NON-NLS-1$

	// the following symbolic constants must match the IDs in Compare's plugin.xml
	private static final String INCOMING_COLOR= "INCOMING_COLOR"; //$NON-NLS-1$
	private static final String OUTGOING_COLOR= "OUTGOING_COLOR"; //$NON-NLS-1$
	private static final String CONFLICTING_COLOR= "CONFLICTING_COLOR"; //$NON-NLS-1$
	private static final String RESOLVED_COLOR= "RESOLVED_COLOR"; //$NON-NLS-1$
	private static final String ADDITION_COLOR = "ADDITION_COLOR"; //$NON-NLS-1$
	private static final String DELETION_COLOR = "DELETION_COLOR"; //$NON-NLS-1$
	private static final String EDITION_COLOR = "EDITION_COLOR"; //$NON-NLS-1$

	// constants
	/** Width of left and right vertical bar */
	private static final int MARGIN_WIDTH= 6;
	/** Width of center bar */
	private static final int CENTER_WIDTH= 34;
	/** Width of birds eye view */
	private static final int BIRDS_EYE_VIEW_WIDTH= 12;
	/** Width of birds eye view */
	private static final int BIRDS_EYE_VIEW_INSET= 2;
	/** */
	private static final int RESOLVE_SIZE= 5;

	/** line width of change borders */
	private static final int LW= 1;

	private boolean fShowCurrentOnly= false;
	private boolean fShowCurrentOnly2= false;
	private int fMarginWidth= MARGIN_WIDTH;
	private int fTopInset;

	// Colors
	private RGB fBackground;
	private RGB fForeground;

	private boolean fIsUsingSystemForeground= true;
	private boolean fIsUsingSystemBackground= true;

	private static final class ColorPalette {
		public final RGB selected;
		public final RGB normal;
		public final RGB fill;
		public final RGB textFill;

		public ColorPalette(RGB background, String registryKey, RGB defaultRGB) {
			RGB registry = JFaceResources.getColorRegistry().getRGB(registryKey);
			if (registry != null) {
				selected = registry;
			} else {
				selected = defaultRGB;
			}
			normal = interpolate(selected, background, 0.6);
			fill = interpolate(selected, background, 0.9);
			textFill = interpolate(selected, background, 0.8);
		}

		private static RGB interpolate(RGB fg, RGB bg, double scale) {
			if (fg != null && bg != null)
				return new RGB((int) ((1.0 - scale) * fg.red + scale * bg.red),
						(int) ((1.0 - scale) * fg.green + scale * bg.green),
						(int) ((1.0 - scale) * fg.blue + scale * bg.blue));
			if (fg != null)
				return fg;
			if (bg != null)
				return bg;
			return new RGB(128, 128, 128); // a gray
		}
	}

	private ColorPalette incomingPalette;
	private ColorPalette conflictPalette;
	private ColorPalette outgoingPalette;
	private ColorPalette additionPalette;
	private ColorPalette deletionPalette;
	private ColorPalette editionPalette;
	private RGB RESOLVED;

	private IPreferenceStore fPreferenceStore;
	private IPropertyChangeListener fPreferenceChangeListener;

	private HashMap<Object, Position> fNewAncestorRanges= new HashMap<>();
	private HashMap<Object, Position> fNewLeftRanges= new HashMap<>();
	private HashMap<Object, Position> fNewRightRanges= new HashMap<>();

	private MergeSourceViewer fAncestor;
	private MergeSourceViewer fLeft;
	private MergeSourceViewer fRight;

	private int fLeftLineCount;
	private int fRightLineCount;

	private boolean fInScrolling;

	private int fPts[]= new int[8];	// scratch area for polygon drawing

	private int fInheritedDirection;	// inherited direction
	private int fTextDirection;			// requested direction for embedded SourceViewer

	private ActionContributionItem fIgnoreAncestorItem;
	private boolean fHighlightRanges;

	private boolean fShowPseudoConflicts= false;

	private boolean fUseSplines= true;
	private boolean fUseSingleLine= true;
	private boolean fHighlightTokenChanges = false;

	private String fSymbolicFontName;

	private ActionContributionItem fNextDiff;	// goto next difference
	private ActionContributionItem fPreviousDiff;	// goto previous difference
	private ActionContributionItem fCopyDiffLeftToRightItem;
	private ActionContributionItem fCopyDiffRightToLeftItem;

	private CompareHandlerService fHandlerService;

	private boolean fSynchronizedScrolling= true;

	private MergeSourceViewer fFocusPart;

	private boolean fSubDoc= true;
	private IPositionUpdater fPositionUpdater;
	private boolean fIsMac;

	private boolean fHasErrors;


	// SWT widgets
	private Canvas fAncestorCanvas;
	private Canvas fLeftCanvas;
	private Canvas fRightCanvas;
	private Canvas fScrollCanvas;
	private ScrollBar fVScrollBar;
	private Canvas fBirdsEyeCanvas;
	private Canvas fSummaryHeader;
	private HeaderPainter fHeaderPainter;

	// SWT resources to be disposed
	private Map<RGB, Color> fColors;

	// points for center curves
	private double[] fBasicCenterCurve;

	private Button fLeftToRightButton;
	private Button fRightToLeftButton;
	private Diff fButtonDiff;

	private ContributorInfo fLeftContributor;
	private ContributorInfo fRightContributor;
	private ContributorInfo fAncestorContributor;
	private int isRefreshing = 0;
	private int fSynchronziedScrollPosition;
	private ActionContributionItem fNextChange;
	private ActionContributionItem fPreviousChange;
	private ShowWhitespaceAction showWhitespaceAction;
	private InternalOutlineViewerCreator fOutlineViewerCreator;
	private TextEditorPropertyAction toggleLineNumbersAction;
	private IFindReplaceTarget fFindReplaceTarget;
	private ChangePropertyAction fIgnoreWhitespace;
	private List<ChangeCompareFilterPropertyAction> fCompareFilterActions = new ArrayList<>();
	private DocumentMerger fMerger;
	/** The current diff */
	private Diff fCurrentDiff;
	private Diff fSavedDiff;

	// Bug 259362 - Update diffs after undo
	private boolean copyOperationInProgress = false;
	private IUndoableOperation copyUndoable = null;
	private IOperationHistoryListener operationHistoryListener;

	/**
	 * Preference key for highlighting current line.
	 */
	private final static String CURRENT_LINE= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE;
	/**
	 * Preference key for highlight color of current line.
	 */
	private final static String CURRENT_LINE_COLOR= AbstractDecoratedTextEditorPreferenceConstants.EDITOR_CURRENT_LINE_COLOR;

	private List<SourceViewerDecorationSupport> fSourceViewerDecorationSupport = new ArrayList<>(3);
	// whether enhanced viewer configuration has been done
	private boolean isConfigured = false;
	private boolean fRedoDiff = false;

	private final class InternalOutlineViewerCreator extends OutlineViewerCreator implements ISelectionChangedListener {
		@Override
		public Viewer findStructureViewer(Viewer oldViewer,
				ICompareInput input, Composite parent,
				CompareConfiguration configuration) {
			if (input != getInput())
				return null;
			final Viewer v = CompareUI.findStructureViewer(oldViewer, input, parent, configuration);
			if (v != null) {
				v.getControl().addDisposeListener(event -> v.removeSelectionChangedListener(InternalOutlineViewerCreator.this));
				v.addSelectionChangedListener(this);
			}

			return v;
		}

		@Override
		public boolean hasViewerFor(Object input) {
			return true;
		}

		@Override
		public void selectionChanged(SelectionChangedEvent event) {
			ISelection s = event.getSelection();
			if (s instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) s;
				Object element = ss.getFirstElement();
				Diff diff = findDiff(element);
				if (diff != null)
					setCurrentDiff(diff, true);
			}
		}

		private Diff findDiff(Object element) {
			if (element instanceof ICompareInput) {
				ICompareInput ci = (ICompareInput) element;
				Position p = getPosition(ci.getLeft());
				if (p != null)
					return findDiff(p, true);
				p = getPosition(ci.getRight());
				if (p != null)
					return findDiff(p, false);
			}
			return null;
		}

		private Diff findDiff(Position p, boolean left) {
			for (Iterator<?> iterator = fMerger.rangesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				Position diffPos;
				if (left) {
					diffPos = diff.getPosition(LEFT_CONTRIBUTOR);
				} else {
					diffPos = diff.getPosition(RIGHT_CONTRIBUTOR);
				}
				// If the element falls within a diff, highlight that diff
				if (diffPos.offset + diffPos.length >= p.offset && diff.getKind() != RangeDifference.NOCHANGE)
					return diff;
				// Otherwise, highlight the first diff after the elements position
				if (diffPos.offset >= p.offset)
					return diff;
			}
			return null;
		}

		private Position getPosition(ITypedElement left) {
			if (left instanceof DocumentRangeNode) {
				DocumentRangeNode drn = (DocumentRangeNode) left;
				return drn.getRange();
			}
			return null;
		}

		@Override
		public Object getInput() {
			return TextMergeViewer.this.getInput();
		}
	}

	class ContributorInfo implements IElementStateListener, VerifyListener, IDocumentListener, IEncodingSupport {
		private final TextMergeViewer fViewer;
		private final Object fElement;
		private char fLeg;
		private String fEncoding;
		private IDocumentProvider fDocumentProvider;
		private IEditorInput fDocumentKey;
		private ISelection fSelection;
		private int fTopIndex = -1;
		private boolean fNeedsValidation = false;
		private MergeSourceViewer fSourceViewer;

		public ContributorInfo(TextMergeViewer viewer, Object element, char leg) {
			fViewer = viewer;
			fElement = element;
			fLeg = leg;
			if (fElement instanceof IEncodedStreamContentAccessor) {
				try {
					fEncoding = ((IEncodedStreamContentAccessor) fElement).getCharset();
				} catch (CoreException e) {
					// silently ignored
				}
			}
		}

		@Override
		public void setEncoding(String encoding) {
			if (fDocumentKey == null || fDocumentProvider == null) {
				return;
			}
			if (fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) fDocumentProvider;
				String current = provider.getEncoding(fDocumentKey);
				boolean dirty = fDocumentProvider.canSaveDocument(fDocumentKey);
				if (!dirty) {
					String internal = encoding == null ? "" : encoding; //$NON-NLS-1$
					if (!internal.equals(current)) {
						provider.setEncoding(fDocumentKey, encoding);
						try {
							fDocumentProvider.resetDocument(fDocumentKey);
						} catch (CoreException e) {
							CompareUIPlugin.log(e);
						} finally {
							update(true);
							updateStructure(fLeg);
						}
					}
				}
			}
		}

		@Override
		public String getEncoding() {
			if (fDocumentProvider != null && fDocumentKey != null
					&& fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) fDocumentProvider;
				return provider.getEncoding(fDocumentKey);
			}
			return null;
		}

		@Override
		public String getDefaultEncoding() {
			if (fDocumentProvider != null && fDocumentKey != null
					&& fDocumentProvider instanceof IStorageDocumentProvider) {
				IStorageDocumentProvider provider = (IStorageDocumentProvider) fDocumentProvider;
				return provider.getDefaultEncoding();
			}
			return null;
		}

		private String internalGetEncoding() {
			if (fElement instanceof IEncodedStreamContentAccessor) {
				try {
					fEncoding = ((IEncodedStreamContentAccessor) fElement)
							.getCharset();
				} catch (CoreException e) {
					// silently ignored
				}
			}
			if (fEncoding != null) {
				return fEncoding;
			}
			return ResourcesPlugin.getEncoding();
		}

		public void setEncodingIfAbsent(ContributorInfo otherContributor) {
			if (fEncoding == null)
				fEncoding = otherContributor.fEncoding;
		}

		public IDocument getDocument() {
			if (fDocumentProvider != null) {
				IDocument document = fDocumentProvider.getDocument(getDocumentKey());
				if (document != null)
					return document;
			}
			if (fElement instanceof IDocument)
				return (IDocument) fElement;
			if (fElement instanceof IDocumentRange)
				return ((IDocumentRange) fElement).getDocument();
			if (fElement instanceof IStreamContentAccessor)
				return DocumentManager.get(fElement);
			return null;
		}

		public void setDocument(MergeSourceViewer viewer, boolean isEditable) {
			// Ensure that this method is only called once
			Assert.isTrue(fSourceViewer == null);
			fSourceViewer = viewer;
			try {
				internalSetDocument(viewer);
			} catch (RuntimeException e) {
				// The error may be due to a stale entry in the DocumentManager (see bug 184489)
				clearCachedDocument();
				throw e;
			}
			setEditable(viewer.getSourceViewer(), isEditable);
			// Verify changes if the document is editable
			if (isEditable) {
				fNeedsValidation = true;
				viewer.getSourceViewer().getTextWidget().addVerifyListener(this);
			}
		}

		/*
		 * Returns true if a new Document could be installed.
		 */
		private boolean internalSetDocument(MergeSourceViewer tp) {

			if (tp == null)
				return false;

			IDocument newDocument = null;
			Position range= null;

			if (fElement instanceof IDocumentRange) {
				newDocument= ((IDocumentRange) fElement).getDocument();
				range= ((IDocumentRange) fElement).getRange();
				connectToSharedDocument();

			} else if (fElement instanceof IDocument) {
				newDocument= (IDocument) fElement;
				setupDocument(newDocument);

			} else if (fElement instanceof IStreamContentAccessor) {
				newDocument= DocumentManager.get(fElement);
				if (newDocument == null) {
					newDocument = createDocument();
					DocumentManager.put(fElement, newDocument);
					setupDocument(newDocument);
				} else if (fDocumentProvider == null) {
					// Connect to a shared document so we can get the proper save synchronization
					connectToSharedDocument();
				}
			} else if (fElement == null) {	// deletion on one side

				ITypedElement parent= this.fViewer.getParent(fLeg);	// we try to find an insertion position within the deletion's parent

				if (parent instanceof IDocumentRange) {
					newDocument= ((IDocumentRange) parent).getDocument();
					newDocument.addPositionCategory(DIFF_RANGE_CATEGORY);
					Object input= this.fViewer.getInput();
					range= this.fViewer.getNewRange(fLeg, input);
					if (range == null) {
						int pos= 0;
						if (input instanceof ICompareInput)
							pos= this.fViewer.findInsertionPosition(fLeg, (ICompareInput) input);
						range= new Position(pos, 0);
						try {
							newDocument.addPosition(DIFF_RANGE_CATEGORY, range);
						} catch (BadPositionCategoryException ex) {
							// silently ignored
							if (TextMergeViewer.DEBUG) System.out.println("BadPositionCategoryException: " + ex);	//$NON-NLS-1$
						} catch (BadLocationException ex) {
							// silently ignored
							if (TextMergeViewer.DEBUG) System.out.println("BadLocationException: " + ex);	//$NON-NLS-1$
						}
						this.fViewer.addNewRange(fLeg, input, range);
					}
				} else if (parent instanceof IDocument) {
					newDocument= ((IDocumentRange) fElement).getDocument();
				}
			}

			boolean enabled= true;
			if (newDocument == null) {
				newDocument= new Document(""); //$NON-NLS-1$
				enabled= false;
			}

			// Update the viewer document or range
			IDocument oldDoc= tp.getSourceViewer().getDocument();
			if (newDocument != oldDoc) {
				updateViewerDocument(tp, newDocument, range);
			} else {	// same document but different range
				updateViewerDocumentRange(tp, range);
			}
			newDocument.addDocumentListener(this);

			tp.setEnabled(enabled);

			return enabled;
		}

		/*
		 * The viewer document is the same but the range has changed
		 */
		private void updateViewerDocumentRange(MergeSourceViewer tp, Position range) {
			tp.setRegion(range);
			if (this.fViewer.fSubDoc) {
				if (range != null) {
					IRegion r= this.fViewer.normalizeDocumentRegion(tp.getSourceViewer().getDocument(), TextMergeViewer.toRegion(range));
					tp.getSourceViewer().setVisibleRegion(r.getOffset(), r.getLength());
				} else
					tp.getSourceViewer().resetVisibleRegion();
			} else
				tp.getSourceViewer().resetVisibleRegion();
		}

		/*
		 * The viewer has a new document
		 */
		private void updateViewerDocument(MergeSourceViewer tp, IDocument document, Position range) {
			unsetDocument(tp);
			if (document == null)
				return;

			connectPositionUpdater(document);

			// install new document
			tp.setRegion(range);
			SourceViewer sourceViewer = tp.getSourceViewer();
			sourceViewer.setRedraw(false);
			try {
				if (this.fViewer.fSubDoc && range != null) {
					IRegion r= this.fViewer.normalizeDocumentRegion(document, TextMergeViewer.toRegion(range));
					sourceViewer.setDocument(document, r.getOffset(), r.getLength());
				} else {
					sourceViewer.setDocument(document);
				}
			} finally {
				sourceViewer.setRedraw(true);
			}

			tp.rememberDocument(document);
		}

		void connectPositionUpdater(IDocument document) {
			document.addPositionCategory(DIFF_RANGE_CATEGORY);
			if (this.fViewer.fPositionUpdater == null)
				this.fViewer.fPositionUpdater= this.fViewer.new ChildPositionUpdater(DIFF_RANGE_CATEGORY);
			else
				document.removePositionUpdater(this.fViewer.fPositionUpdater);
			document.addPositionUpdater(this.fViewer.fPositionUpdater);
		}

		private void unsetDocument(MergeSourceViewer tp) {
			IDocument oldDoc= internalGetDocument(tp);
			if (oldDoc != null) {
				tp.rememberDocument(null);
				try {
					oldDoc.removePositionCategory(DIFF_RANGE_CATEGORY);
				} catch (BadPositionCategoryException ex) {
					// Ignore
				}
				if (fPositionUpdater != null)
					oldDoc.removePositionUpdater(fPositionUpdater);
				oldDoc.removeDocumentListener(this);
			}
		}

		private IDocument createDocument() {
			// If the content provider is a text content provider, attempt to obtain
			// a shared document (i.e. file buffer)
			IDocument newDoc = connectToSharedDocument();

			if (newDoc == null) {
				IStreamContentAccessor sca= (IStreamContentAccessor) fElement;
				String s= null;

				try {
					String encoding = internalGetEncoding();
					s = Utilities.readString(sca, encoding);
				} catch (CoreException ex) {
					this.fViewer.setError(fLeg, ex.getMessage());
				}

				newDoc= new Document(s != null ? s : ""); //$NON-NLS-1$
			}
			return newDoc;
		}

		/**
		 * Connect to a shared document if possible. Return <code>null</code>
		 * if the connection was not possible.
		 * @return the shared document or <code>null</code> if connection to a
		 * shared document was not possible
		 */
		private IDocument connectToSharedDocument() {
			IEditorInput key = getDocumentKey();
			if (key != null) {
				if (fDocumentProvider != null) {
					// We've already connected and setup the document
					return fDocumentProvider.getDocument(key);
				}
				IDocumentProvider documentProvider = getDocumentProvider();
				if (documentProvider != null) {
					try {
						connect(documentProvider, key);
						setCachedDocumentProvider(key,
								documentProvider);
						IDocument newDoc = documentProvider.getDocument(key);
						this.fViewer.updateDirtyState(key, documentProvider, fLeg);
						return newDoc;
					} catch (CoreException e) {
						// Connection failed. Log the error and continue without a shared document
						CompareUIPlugin.log(e);
					}
				}
			}
			return null;
		}

		private void connect(IDocumentProvider documentProvider, IEditorInput input) throws CoreException {
			final ISharedDocumentAdapter sda = Adapters.adapt(fElement, ISharedDocumentAdapter.class);
			if (sda != null) {
				sda.connect(documentProvider, input);
			} else {
				documentProvider.connect(input);
			}
		}

		private void disconnect(IDocumentProvider provider, IEditorInput input) {
			final ISharedDocumentAdapter sda = Adapters.adapt(fElement, ISharedDocumentAdapter.class);
			if (sda != null) {
				sda.disconnect(provider, input);
			} else {
				provider.disconnect(input);
			}
		}

		private void setCachedDocumentProvider(IEditorInput key,
				IDocumentProvider documentProvider) {
			fDocumentKey = key;
			fDocumentProvider = documentProvider;
			documentProvider.addElementStateListener(this);
		}

		public void disconnect() {
			IDocumentProvider provider = null;
			IEditorInput input = getDocumentKey();
			synchronized(this) {
				if (fDocumentProvider != null) {
					provider = fDocumentProvider;
					fDocumentProvider = null;
					fDocumentKey = null;
				}
			}
			if (provider != null) {
				disconnect(provider, input);
				provider.removeElementStateListener(this);
			}
			// If we have a listener registered with the widget, remove it
			if (fSourceViewer != null) {
				StyledText textWidget = fSourceViewer.getSourceViewer().getTextWidget();
				if (textWidget != null && !textWidget.isDisposed()) {
					if (fNeedsValidation) {
						textWidget.removeVerifyListener(this);
						fNeedsValidation = false;
					}
					IDocument oldDoc= internalGetDocument(fSourceViewer);
					if (oldDoc != null) {
						oldDoc.removeDocumentListener(this);
					}
				}
			}
			clearCachedDocument();
		}

		private void clearCachedDocument() {
			// Finally, remove the document from the document manager
			IDocument doc = DocumentManager.get(fElement);
			if (doc != null)
				DocumentManager.remove(doc);
		}

		private IDocument internalGetDocument(MergeSourceViewer tp) {
			IDocument oldDoc= tp.getSourceViewer().getDocument();
			if (oldDoc == null) {
				oldDoc= tp.getRememberedDocument();
			}
			return oldDoc;
		}

		/**
		 * Return the document key used to obtain a shared document. A <code>null</code>
		 * is returned in the following cases:
		 * <ol>
		 * <li>This contributor does not have a shared document adapter.</li>
		 * <li>This text merge viewer has a document partitioner but uses the default partitioning.</li>
		 * <li>This text merge viewer does not use he default content provider.</li>
		 * </ol>
		 * @return the document key used to obtain a shared document or <code>null</code>
		 */
		private IEditorInput getDocumentKey() {
			if (fDocumentKey != null)
				return fDocumentKey;
			if (isUsingDefaultContentProvider() && fElement != null && canHaveSharedDocument()) {
				ISharedDocumentAdapter sda = Adapters.adapt(fElement, ISharedDocumentAdapter.class);
				if (sda != null) {
					return sda.getDocumentKey(fElement);
				}
			}
			return null;
		}

		private IDocumentProvider getDocumentProvider() {
			if (fDocumentProvider != null)
				return fDocumentProvider;
			// We will only use document providers if the content provider is the
			// default content provider
			if (isUsingDefaultContentProvider()) {
				IEditorInput input = getDocumentKey();
				if (input != null)
					return SharedDocumentAdapter.getDocumentProvider(input);
			}
			return null;
		}

		private boolean isUsingDefaultContentProvider() {
			return fViewer.isUsingDefaultContentProvider();
		}

		private boolean canHaveSharedDocument() {
			return fViewer.canHaveSharedDocument();
		}

		boolean hasSharedDocument(Object object) {
			return (fElement == object &&
					fDocumentProvider != null
					&& fDocumentProvider.getDocument(getDocumentKey()) != null);
		}

		public boolean flush() throws CoreException {
			if (fDocumentProvider != null) {
				IEditorInput input = getDocumentKey();
				IDocument document = fDocumentProvider.getDocument(input);
				if (document != null) {
					final ISharedDocumentAdapter sda = Adapters.adapt(fElement, ISharedDocumentAdapter.class);
					if (sda != null) {
						sda.flushDocument(fDocumentProvider, input, document, false);
						return true;
					}
					try {
						fDocumentProvider.aboutToChange(input);
						fDocumentProvider.saveDocument(new NullProgressMonitor(), input, document, false);
						return true;
					} finally {
						fDocumentProvider.changed(input);
					}
				}
			}
			return false;
		}

		@Override
		public void elementMoved(Object originalElement, Object movedElement) {
			IEditorInput input = getDocumentKey();
			if (input != null && input.equals(originalElement)) {
				// This method will only get called if the buffer is not dirty
				resetDocument();
			}
		}

		@Override
		public void elementDirtyStateChanged(Object element, boolean isDirty) {
			if (!checkState())
				return;
			IEditorInput input = getDocumentKey();
			if (input != null && input.equals(element)) {
				this.fViewer.updateDirtyState(input, getDocumentProvider(), fLeg);
			}
		}

		@Override
		public void elementDeleted(Object element) {
			IEditorInput input = getDocumentKey();
			if (input != null && input.equals(element)) {
				// This method will only get called if the buffer is not dirty
				resetDocument();
			}
		}
		private void resetDocument() {
			// Need to remove the document from the manager before refreshing
			// or the old document will still be found
			clearCachedDocument();
			// TODO: This is fine for now but may need to be revisited if a refresh is performed
			// higher up as well (e.g. perhaps a refresh request that waits until after all parties
			// have been notified).
			if (checkState())
				fViewer.refresh();
		}

		private boolean checkState() {
			if (fViewer == null)
				return false;
			Control control = fViewer.getControl();
			if (control == null)
				return false;
			return !control.isDisposed();
		}

		@Override
		public void elementContentReplaced(Object element) {
			if (!checkState())
				return;
			IEditorInput input = getDocumentKey();
			if (input != null && input.equals(element)) {
				this.fViewer.updateDirtyState(input, getDocumentProvider(), fLeg);

				// recalculate diffs and update controls
				new UIJob(CompareMessages.DocumentMerger_0) {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						update(true);
						updateStructure(fLeg);
						return Status.OK_STATUS;
					}
				}.schedule();
			}
		}
		@Override
		public void elementContentAboutToBeReplaced(Object element) {
			// Nothing to do
		}

		public Object getElement() {
			return fElement;
		}

		public void cacheSelection(MergeSourceViewer viewer) {
			if (viewer == null) {
				this.fSelection = null;
				this.fTopIndex = -1;
			} else {
				this.fSelection = viewer.getSourceViewer().getSelection();
				this.fTopIndex = viewer.getSourceViewer().getTopIndex();
			}
		}

		public void updateSelection(MergeSourceViewer viewer, boolean includeScroll) {
			if (fSelection != null)
				viewer.getSourceViewer().setSelection(fSelection);
			if (includeScroll && fTopIndex != -1) {
				viewer.getSourceViewer().setTopIndex(fTopIndex);
			}
		}

		public void transferContributorStateFrom(
				ContributorInfo oldContributor) {
			if (oldContributor != null) {
				fSelection = oldContributor.fSelection;
				fTopIndex = oldContributor.fTopIndex;
				fEncoding = oldContributor.fEncoding;
			}

		}

		public boolean validateChange() {
			if (fElement == null)
				return true;
			if (fDocumentProvider instanceof IDocumentProviderExtension) {
				IDocumentProviderExtension ext = (IDocumentProviderExtension) fDocumentProvider;
				if (ext.isReadOnly(fDocumentKey)) {
					try {
						ext.validateState(fDocumentKey, getControl().getShell());
						ext.updateStateCache(fDocumentKey);
					} catch (CoreException e) {
						ErrorDialog.openError(getControl().getShell(), CompareMessages.TextMergeViewer_12, CompareMessages.TextMergeViewer_13, e.getStatus());
						return false;
					}
				}
				return !ext.isReadOnly(fDocumentKey);
			}
			IEditableContentExtension ext = Adapters.adapt(fElement, IEditableContentExtension.class);
			if (ext != null) {
				if (ext.isReadOnly()) {
					IStatus status = ext.validateEdit(getControl().getShell());
					if (!status.isOK()) {
						if (status.getSeverity() == IStatus.ERROR) {
							ErrorDialog.openError(getControl().getShell(), CompareMessages.TextMergeViewer_14, CompareMessages.TextMergeViewer_15, status);
							return false;
						}
						if (status.getSeverity() == IStatus.CANCEL)
							return false;
					}
				}
			}
			return true;
		}

		@Override
		public void verifyText(VerifyEvent e) {
			if (!validateChange()) {
				e.doit= false;
			}
		}

		@Override
		public void documentAboutToBeChanged(DocumentEvent e) {
			// nothing to do
		}

		@Override
		public void documentChanged(DocumentEvent e) {
			boolean dirty = true;
			if (fDocumentProvider != null && fDocumentKey != null) {
				dirty = fDocumentProvider.canSaveDocument(fDocumentKey);
			}
			TextMergeViewer.this.documentChanged(e, dirty);
			// Remove our verify listener since the document is now dirty
			if (fNeedsValidation && fSourceViewer != null && !fSourceViewer.getSourceViewer().getTextWidget().isDisposed()) {
				fSourceViewer.getSourceViewer().getTextWidget().removeVerifyListener(this);
				fNeedsValidation = false;
			}
		}
	}

	class HeaderPainter implements PaintListener {
		private static final int INSET= BIRDS_EYE_VIEW_INSET;

		private RGB fIndicatorColor;
		private Color fSeparatorColor;

		public HeaderPainter() {
			fSeparatorColor= fSummaryHeader.getDisplay().getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW);
		}

		/*
		 * Returns true on color change
		 */
		public boolean setColor(RGB color) {
			RGB oldColor= fIndicatorColor;
			fIndicatorColor= color;
			if (color == null)
				return oldColor != null;
			if (oldColor != null)
				return !color.equals(oldColor);
			return true;
		}

		private void drawBevelRect(GC gc, int x, int y, int w, int h, Color topLeft, Color bottomRight) {
			gc.setForeground(topLeft);
			gc.drawLine(x, y, x + w -1, y);
			gc.drawLine(x, y, x, y + h -1);

			gc.setForeground(bottomRight);
			gc.drawLine(x + w, y, x + w, y + h);
			gc.drawLine(x, y + h, x + w, y + h);
		}

		@Override
		public void paintControl(PaintEvent e) {
			Point s= fSummaryHeader.getSize();

			if (fIndicatorColor != null) {
				Display d= fSummaryHeader.getDisplay();
				e.gc.setBackground(getColor(d, fIndicatorColor));
				int min= Math.min(s.x, s.y)-2*INSET;
				Rectangle r= new Rectangle((s.x-min)/2, (s.y-min)/2, min, min);
				e.gc.fillRectangle(r);
				if (d != null)
					drawBevelRect(e.gc, r.x, r.y, r.width -1, r.height -1, d.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW), d.getSystemColor(SWT.COLOR_WIDGET_HIGHLIGHT_SHADOW));

				e.gc.setForeground(fSeparatorColor);
				e.gc.setLineWidth(0 /* 1 */);
				e.gc.drawLine(0+1, s.y-1, s.x-1-1, s.y-1);
			}
		}
	}

	/*
	 * The position updater used to adapt the positions representing
	 * the child document ranges to changes of the parent document.
	 */
	class ChildPositionUpdater extends DefaultPositionUpdater {
		/*
		 * Creates the position updated.
		 */
		protected ChildPositionUpdater(String category) {
			super(category);
		}

		/*
		 * Child document ranges cannot be deleted other then by calling
		 * freeChildDocument.
		 */
		@Override
		protected boolean notDeleted() {
			return true;
		}

		/*
		 * If an insertion happens at a child document's start offset, the
		 * position is extended rather than shifted. Also, if something is added
		 * right behind the end of the position, the position is extended rather
		 * than kept stable.
		 */
		@Override
		protected void adaptToInsert() {
			if (fPosition == fLeft.getRegion() || fPosition == fRight.getRegion()) {
				int myStart= fPosition.offset;
				int myEnd=   fPosition.offset + fPosition.length;
				myEnd= Math.max(myStart, myEnd);

				int yoursStart= fOffset;
				int yoursEnd=   fOffset + fReplaceLength -1;
				yoursEnd= Math.max(yoursStart, yoursEnd);

				if (myEnd < yoursStart)
					return;

				if (myStart <= yoursStart)
					fPosition.length += fReplaceLength;
				else
					fPosition.offset += fReplaceLength;
			} else {
				super.adaptToInsert();
			}
		}
	}

	private class ChangeHighlighter implements ITextPresentationListener {
		private final MergeSourceViewer viewer;

		public ChangeHighlighter(MergeSourceViewer viewer) {
			this.viewer = viewer;
		}

		@Override
		public void applyTextPresentation(TextPresentation textPresentation) {
			if (!fHighlightTokenChanges)
				return;
			IRegion region= textPresentation.getExtent();
			Diff[] changeDiffs = fMerger.getChangeDiffs(getLeg(viewer), region);
			boolean allOutgoing = allOutgoing();
			for (Diff diff : changeDiffs) {
				StyleRange range = getStyleRange(diff, region, allOutgoing);
				if (range != null)
					textPresentation.mergeStyleRange(range);
			}
		}

		private StyleRange getStyleRange(Diff diff, IRegion region, boolean showAdditionRemoval) {
			//Color cText = getColor(null, getTextColor());
			Color cTextFill = getColor(null, getTextFillColor(diff, showAdditionRemoval));
			if (cTextFill == null)
				return null;
			Position p = diff.getPosition(getLeg(viewer));
			int start = p.getOffset();
			int length = p.getLength();
			// Don't start before the region
			if (start < region.getOffset()) {
				length = length - (region.getOffset() - start);
				start = region.getOffset();
			}
			// Don't go past the end of the region
			int regionEnd = region.getOffset() + region.getLength();
			if (start + length > regionEnd) {
				length = regionEnd - start;
			}
			if (length < 0)
				return null;

			return new StyleRange(start, length, null, cTextFill);
		}

		private RGB getTextFillColor(Diff diff, boolean showAdditionRemoval) {
			ColorPalette palette = getColorPalette(diff, showAdditionRemoval);
			if (palette == null) {
				return null;
			}
			return palette.textFill;
		}
	}

	private class FindReplaceTarget implements IFindReplaceTarget, IFindReplaceTargetExtension, IFindReplaceTargetExtension2, IFindReplaceTargetExtension3 {
		@Override
		public boolean canPerformFind() {
			return fFocusPart != null;
		}

		@Override
		public int findAndSelect(int widgetOffset, String findString,
				boolean searchForward, boolean caseSensitive, boolean wholeWord) {
			return Optional.ofNullable(getTarget()).map(
					target -> target.findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord))
					.orElse(-1);
		}

		private IFindReplaceTarget getTarget() {
			return Optional.ofNullable(fFocusPart).map(MergeSourceViewer::getSourceViewer)
					.map(SourceViewer::getFindReplaceTarget)
					.orElse(null);
		}

		@Override
		public Point getSelection() {
			return Optional.ofNullable(getTarget()).map(IFindReplaceTarget::getSelection)
					.orElse(new Point(-1, -1));
		}

		@Override
		public String getSelectionText() {
			return Optional.ofNullable(getTarget()).map(IFindReplaceTarget::getSelectionText).orElse(""); //$NON-NLS-1$
		}

		@Override
		public boolean isEditable() {
			return Optional.ofNullable(getTarget()).map(IFindReplaceTarget::isEditable).orElse(false);
		}

		@Override
		public void replaceSelection(String text) {
			Optional.ofNullable(getTarget()).ifPresent(target -> target.replaceSelection(text));
		}

		@Override
		public int findAndSelect(int offset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord, boolean regExSearch) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension3) {
				return ((IFindReplaceTargetExtension3) findReplaceTarget).findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord, regExSearch);
			}

			// fallback like in org.eclipse.ui.texteditor.FindReplaceTarget
			if (!regExSearch && findReplaceTarget != null)
				return findReplaceTarget.findAndSelect(offset, findString, searchForward, caseSensitive, wholeWord);
			return -1;
		}

		@Override
		public void replaceSelection(String text, boolean regExReplace) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension3) {
				((IFindReplaceTargetExtension3) findReplaceTarget).replaceSelection(text, regExReplace);
				return;
			}

			// fallback like in org.eclipse.ui.texteditor.FindReplaceTarget
			if (!regExReplace && findReplaceTarget != null)
				findReplaceTarget.replaceSelection(text);
		}

		@Override
		public boolean validateTargetState() {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension2) {
				return ((IFindReplaceTargetExtension2) findReplaceTarget).validateTargetState();
			}
			// TODO not sure if true when findReplaceTarget is null
			return true;
		}

		@Override
		public void beginSession() {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).beginSession();
			}
		}

		@Override
		public void endSession() {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).endSession();
			}
		}

		@Override
		public IRegion getScope() {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				return ((IFindReplaceTargetExtension) findReplaceTarget).getScope();
			}
			return null;
		}

		@Override
		public void setScope(IRegion scope) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setScope(scope);
			}
		}

		@Override
		public Point getLineSelection() {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				return ((IFindReplaceTargetExtension) findReplaceTarget).getLineSelection();
			}
			return null;
		}

		@Override
		public void setSelection(int offset, int length) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setSelection(offset, length);
			}
		}

		@Override
		public void setScopeHighlightColor(Color color) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setScopeHighlightColor(color);
			}
		}

		@Override
		public void setReplaceAllMode(boolean replaceAll) {
			IFindReplaceTarget findReplaceTarget = getTarget();
			if (findReplaceTarget instanceof IFindReplaceTargetExtension) {
				((IFindReplaceTargetExtension) findReplaceTarget).setReplaceAllMode(replaceAll);
			}
		}

	}

	//---- MergeTextViewer

	/**
	 * Creates a text merge viewer under the given parent control.
	 *
	 * @param parent the parent control
	 * @param configuration the configuration object
	 */
	public TextMergeViewer(Composite parent, CompareConfiguration configuration) {
		this(parent, SWT.NULL, configuration);
	}

	/**
	 * Creates a text merge viewer under the given parent control.
	 *
	 * @param parent the parent control
	 * @param style SWT style bits for top level composite of this viewer
	 * @param configuration the configuration object
	 */
	public TextMergeViewer(Composite parent, int style, CompareConfiguration configuration) {
		super(style, ResourceBundle.getBundle(BUNDLE_NAME), configuration);

		operationHistoryListener = TextMergeViewer.this::historyNotification;
		OperationHistoryFactory.getOperationHistory()
				.addOperationHistoryListener(operationHistoryListener);

		fMerger = new DocumentMerger(new IDocumentMergerInput() {
			@Override
			public ITokenComparator createTokenComparator(String line) {
				return TextMergeViewer.this.createTokenComparator(line);
			}

			@Override
			public Optional<IIgnoreWhitespaceContributor> createIgnoreWhitespaceContributor(IDocument document) {
				return TextMergeViewer.this.createIgnoreWhitespaceContributor(document);
			}

			@Override
			public CompareConfiguration getCompareConfiguration() {
				return TextMergeViewer.this.getCompareConfiguration();
			}
			@Override
			public IDocument getDocument(char contributor) {
				switch (contributor) {
				case LEFT_CONTRIBUTOR:
					return fLeft.getSourceViewer().getDocument();
				case RIGHT_CONTRIBUTOR:
					return fRight.getSourceViewer().getDocument();
				case ANCESTOR_CONTRIBUTOR:
					return fAncestor.getSourceViewer().getDocument();
				default:
					return null;
				}
			}
			@Override
			public int getHunkStart() {
				return TextMergeViewer.this.getHunkStart();
			}
			@Override
			public Position getRegion(char contributor) {
				switch (contributor) {
				case LEFT_CONTRIBUTOR:
					return fLeft.getRegion();
				case RIGHT_CONTRIBUTOR:
					return fRight.getRegion();
				case ANCESTOR_CONTRIBUTOR:
					return fAncestor.getRegion();
				default:
					return null;
				}
			}
			@Override
			public boolean isHunkOnLeft() {
				ITypedElement left = ((ICompareInput) getInput()).getRight();
				return left != null && Adapters.adapt(left, IHunk.class) != null;
			}
			@Override
			public boolean isIgnoreAncestor() {
				return TextMergeViewer.this.isIgnoreAncestor();
			}
			@Override
			public boolean isPatchHunk() {
				return TextMergeViewer.this.isPatchHunk();
			}

			@Override
			public boolean isShowPseudoConflicts() {
				return fShowPseudoConflicts;
			}
			@Override
			public boolean isThreeWay() {
				return TextMergeViewer.this.isThreeWay();
			}
			@Override
			public boolean isPatchHunkOk() {
				return TextMergeViewer.this.isPatchHunkOk();
			}
		});

		int inheritedStyle= parent.getStyle();
		if ((inheritedStyle & SWT.LEFT_TO_RIGHT) != 0)
			fInheritedDirection= SWT.LEFT_TO_RIGHT;
		else if ((inheritedStyle & SWT.RIGHT_TO_LEFT) != 0)
			fInheritedDirection= SWT.RIGHT_TO_LEFT;
		else
			fInheritedDirection= SWT.NONE;

		if ((style & SWT.LEFT_TO_RIGHT) != 0)
			fTextDirection= SWT.LEFT_TO_RIGHT;
		else if ((style & SWT.RIGHT_TO_LEFT) != 0)
			fTextDirection= SWT.RIGHT_TO_LEFT;
		else
			fTextDirection= SWT.NONE;

		fSymbolicFontName= getSymbolicFontName();

		fIsMac= Util.isMac();

		fPreferenceChangeListener= TextMergeViewer.this::handlePropertyChangeEvent;

		fPreferenceStore= createChainedPreferenceStore();
		if (fPreferenceStore != null) {
			fPreferenceStore.addPropertyChangeListener(fPreferenceChangeListener);

			fSynchronizedScrolling= fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING);
			fShowPseudoConflicts= fPreferenceStore.getBoolean(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS);
			//fUseSplines= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SPLINES);
			fUseSingleLine= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SINGLE_LINE);
			fHighlightTokenChanges= fPreferenceStore.getBoolean(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES);
			//fUseResolveUI= fPreferenceStore.getBoolean(ComparePreferencePage.USE_RESOLVE_UI);
		}

		buildControl(parent);

		setColors();

		INavigatable nav= new INavigatable() {
			@Override
			public boolean selectChange(int flag) {
				if (flag == INavigatable.FIRST_CHANGE || flag == INavigatable.LAST_CHANGE) {
					selectFirstDiff(flag == INavigatable.FIRST_CHANGE);
					return false;
				}
				return navigate(flag == INavigatable.NEXT_CHANGE, false, false);
			}
			@Override
			public Object getInput() {
				return TextMergeViewer.this.getInput();
			}
			@Override
			public boolean openSelectedChange() {
				return false;
			}
			@Override
			public boolean hasChange(int flag) {
				return getNextVisibleDiff(flag == INavigatable.NEXT_CHANGE, false) != null;
			}
		};
		fComposite.setData(INavigatable.NAVIGATOR_PROPERTY, nav);

		JFaceResources.getFontRegistry().addListener(fPreferenceChangeListener);
		JFaceResources.getColorRegistry().addListener(fPreferenceChangeListener);
		updateFont();
	}

	private static class LineNumberRulerToggleAction extends TextEditorPropertyAction {
		public LineNumberRulerToggleAction(String label, MergeSourceViewer[] viewers, String preferenceKey) {
			super(label, viewers, preferenceKey);
		}

		@Override
		protected boolean toggleState(boolean checked) {
			return true;
		}
	}

	private ChainedPreferenceStore createChainedPreferenceStore() {
		List<IPreferenceStore> stores= new ArrayList<>(2);
		stores.add(getCompareConfiguration().getPreferenceStore());
		stores.add(EditorsUI.getPreferenceStore());
		return new ChainedPreferenceStore(stores.toArray(new IPreferenceStore[stores.size()]));
	}

	/**
	 * Creates a color from the information stored in the given preference store.
	 * Returns <code>null</code> if there is no such information available.
	 * @param store preference store
	 * @param key preference key
	 * @return the color or <code>null</code>
	 */
	private static RGB createColor(IPreferenceStore store, String key) {
		if (!store.contains(key))
			return null;
		if (store.isDefault(key))
			return PreferenceConverter.getDefaultColor(store, key);
		return PreferenceConverter.getColor(store, key);
	}

	private void setColors() {
		fIsUsingSystemBackground= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
		if (!fIsUsingSystemBackground)
			fBackground= createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND);

		fIsUsingSystemForeground= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT);
		if (!fIsUsingSystemForeground)
			fForeground= createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND);

		updateColors(null);
	}

	private String getSymbolicFontName() {
		Class<?> clazz= getClass();
		do {
			String fontName= clazz.getName();
			if (JFaceResources.getFontRegistry().hasValueFor(fontName))
				return fontName;
			clazz= clazz.getSuperclass();
		} while (clazz != null);
		// use text compare font if no font has been registered for subclass
		return getClass().getName();
	}

	private void updateFont() {
		Font f= JFaceResources.getFont(fSymbolicFontName);
		if (f != null) {
			if (fAncestor != null)
				fAncestor.setFont(f);
			if (fLeft != null)
				fLeft.setFont(f);
			if (fRight != null)
				fRight.setFont(f);
		}
	}

	private void checkForColorUpdate(Display display) {
		if (fIsUsingSystemBackground) {
			RGB bg= display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
			if (!bg.equals(getBackground(display))) {
				updateColors(display);
			}
		}
	}

	/**
	 * Sets the viewer's background color to the given RGB value.
	 * If the value is <code>null</code> the system's default background color is used.
	 * @param background the background color or <code>null</code> to use the system's default background color
	 * @since 2.0
	 */
	public void setBackgroundColor(RGB background) {
		fIsUsingSystemBackground= (background == null);
		fBackground= background;
		updateColors(null);
	}

	private RGB getBackground(Display display) {
		if (fBackground != null)
			return fBackground;

		if (display == null)
			display= fComposite.getDisplay();

		return display.getSystemColor(SWT.COLOR_LIST_BACKGROUND).getRGB();
	}

	/**
	 * Sets the viewer's foreground color to the given RGB value.
	 * If the value is <code>null</code> the system's default foreground color is used.
	 * @param foreground the foreground color or <code>null</code> to use the system's default foreground color
	 * @since 2.0
	 */
	public void setForegroundColor(RGB foreground) {
		fIsUsingSystemForeground= (foreground == null);
		fForeground= foreground;
		updateColors(null);
	}

	private void updateColors(Display display) {

		if (display == null)
			display = fComposite.getDisplay();

		Color bgColor = null;
		if (fBackground != null)
			bgColor = getColor(display, fBackground);

		if (fAncestor != null)
			fAncestor.setBackgroundColor(bgColor);
		if (fLeft != null)
			fLeft.setBackgroundColor(bgColor);
		if (fRight != null)
			fRight.setBackgroundColor(bgColor);

		Color fgColor = null;
		if (fForeground != null)
			fgColor = getColor(display, fForeground);

		if (fAncestor != null)
			fAncestor.setForegroundColor(fgColor);
		if (fLeft != null)
			fLeft.setForegroundColor(fgColor);
		if (fRight != null)
			fRight.setForegroundColor(fgColor);

		ColorRegistry registry= JFaceResources.getColorRegistry();

		RGB bg= getBackground(display);

		incomingPalette = new ColorPalette(bg, INCOMING_COLOR, new RGB(0, 0, 255));
		outgoingPalette = new ColorPalette(bg, OUTGOING_COLOR, new RGB(0, 0, 0));
		conflictPalette = new ColorPalette(bg, CONFLICTING_COLOR, new RGB(255, 0, 0));
		additionPalette = new ColorPalette(bg, ADDITION_COLOR, new RGB(0, 255, 0));
		deletionPalette = new ColorPalette(bg, DELETION_COLOR, new RGB(255, 0, 0));
		editionPalette = new ColorPalette(bg, EDITION_COLOR, new RGB(0, 0, 0));

		RESOLVED= registry.getRGB(RESOLVED_COLOR);
		if (RESOLVED == null)
			RESOLVED= new RGB(0, 255, 0);	// GREEN


		updatePresentation();
	}

	private void updatePresentation() {
		refreshBirdsEyeView();
		invalidateLines();
		invalidateTextPresentation();
	}

	/**
	 * Invalidates the current presentation by invalidating the three text viewers.
	 * @since 2.0
	 */
	public void invalidateTextPresentation() {
		if (fAncestor != null)
			fAncestor.getSourceViewer().invalidateTextPresentation();
		if (fLeft != null)
			fLeft.getSourceViewer().invalidateTextPresentation();
		if (fRight != null)
			fRight.getSourceViewer().invalidateTextPresentation();
	}

	/**
	 * Configures the passed text viewer. This method is called after the three
	 * text viewers have been created for the content areas. The
	 * <code>TextMergeViewer</code> implementation of this method will
	 * configure the viewer with a {@link SourceViewerConfiguration}.
	 * Subclasses may reimplement to provide a specific configuration for the
	 * text viewer.
	 *
	 * @param textViewer
	 *            the text viewer to configure
	 */
	protected void configureTextViewer(TextViewer textViewer) {
		// to get undo for all text files, bugzilla 131895, 33665
		if(textViewer instanceof ISourceViewer){
			SourceViewerConfiguration configuration= new SourceViewerConfiguration();
			((ISourceViewer) textViewer).configure(configuration);
		}
	}

	/**
	 * Creates an <code>ITokenComparator</code> which is used to show the
	 * intra line differences.
	 * The <code>TextMergeViewer</code> implementation of this method returns a
	 * tokenizer that breaks a line into words separated by whitespace.
	 * Subclasses may reimplement to provide a specific tokenizer.
	 * @param line the line for which to create the <code>ITokenComparator</code>
	 * @return a ITokenComparator which is used for a second level token compare.
	 */
	protected ITokenComparator createTokenComparator(String line) {
		return new TokenComparator(line);
	}

	/**
	 * Creates an <code>IIgnoreWhitespaceContributor</code> which allows to hook
	 * into the ignore whitespace logic in the compare viewer. Tool specific
	 * implementations can overrule which whitespaces can be ignored and which not
	 * (e.g. whitespaces in literals).
	 *
	 * @return a IIgnoreWhitespaceContributor which allows to overrule the platform
	 *         based whitespace ignore logic in the compare view. Default
	 *         implementation doesn't supply a contributor.
	 * @since 3.9
	 */
	protected Optional<IIgnoreWhitespaceContributor> createIgnoreWhitespaceContributor(IDocument document) {
		return Optional.empty();
	}

	/**
	 * Setup the given document for use with this viewer. By default,
	 * the partitioner returned from {@link #getDocumentPartitioner()}
	 * is registered as the default partitioner for the document.
	 * Subclasses that return a partitioner must also override
	 * {@link #getDocumentPartitioning()} if they wish to be able to use shared
	 * documents (i.e. file buffers).
	 * @param document the document to be set up
	 *
	 * @since 3.3
	 */
	protected void setupDocument(IDocument document) {
		String partitioning = getDocumentPartitioning();
		if (partitioning == null || !(document instanceof IDocumentExtension3)) {
			if (document.getDocumentPartitioner() == null) {
				IDocumentPartitioner partitioner= getDocumentPartitioner();
				if (partitioner != null) {
					document.setDocumentPartitioner(partitioner);
					partitioner.connect(document);
				}
			}
		} else {
			IDocumentExtension3 ex3 = (IDocumentExtension3) document;
			if (ex3.getDocumentPartitioner(partitioning) == null) {
				IDocumentPartitioner partitioner= getDocumentPartitioner();
				if (partitioner != null) {
					ex3.setDocumentPartitioner(partitioning, partitioner);
					partitioner.connect(document);
				}
			}
		}
	}

	/**
	 * Returns a document partitioner which is suitable for the underlying content type.
	 * This method is only called if the input provided by the content provider is a
	 * <code>IStreamContentAccessor</code> and an internal document must be obtained. This
	 * document is initialized with the partitioner returned from this method.
	 * <p>
	 * The <code>TextMergeViewer</code> implementation of this method returns
	 * <code>null</code>. Subclasses may reimplement to create a partitioner for a
	 * specific content type. Subclasses that do return a partitioner should also
	 * return a partitioning from {@link #getDocumentPartitioning()} in order to make
	 * use of shared documents (e.g. file buffers).
	 *
	 * @return a document partitioner, or <code>null</code>
	 */
	protected IDocumentPartitioner getDocumentPartitioner() {
		return null;
	}

	/**
	 * Return the partitioning to which the partitioner returned from
	 * {@link #getDocumentPartitioner()} is to be associated. Return <code>null</code>
	 * only if partitioning is not needed or if the subclass
	 * overrode {@link #setupDocument(IDocument)} directly.
	 * By default, <code>null</code> is returned which means that shared
	 * documents that return a partitioner from {@link #getDocumentPartitioner()}
	 * will not be able to use shared documents.
	 * @see IDocumentExtension3
	 * @return a partitioning
	 *
	 * @since 3.3
	 */
	protected String getDocumentPartitioning() {
		return null;
	}

	/**
	 * Called on the viewer disposal.
	 * Unregisters from the compare configuration.
	 * Clients may extend if they have to do additional cleanup.
	 */
	@Override
	protected void handleDispose(DisposeEvent event) {
		OperationHistoryFactory.getOperationHistory().removeOperationHistoryListener(operationHistoryListener);

		if (fHandlerService != null)
			fHandlerService.dispose();

		Object input= getInput();
		removeFromDocumentManager(ANCESTOR_CONTRIBUTOR, input);
		removeFromDocumentManager(LEFT_CONTRIBUTOR, input);
		removeFromDocumentManager(RIGHT_CONTRIBUTOR, input);

		if (DEBUG)
			DocumentManager.dump();

		if (fPreferenceChangeListener != null) {
			JFaceResources.getFontRegistry().removeListener(fPreferenceChangeListener);
			JFaceResources.getColorRegistry().removeListener(fPreferenceChangeListener);
			if (fPreferenceStore != null)
				fPreferenceStore.removePropertyChangeListener(fPreferenceChangeListener);
			fPreferenceChangeListener= null;
		}

		fLeftCanvas= null;
		fRightCanvas= null;
		fVScrollBar= null;
		fBirdsEyeCanvas= null;
		fSummaryHeader= null;

		fAncestorContributor.unsetDocument(fAncestor);
		fLeftContributor.unsetDocument(fLeft);
		fRightContributor.unsetDocument(fRight);

		disconnect(fLeftContributor);
		disconnect(fRightContributor);
		disconnect(fAncestorContributor);

		if (showWhitespaceAction != null)
			showWhitespaceAction.dispose();

		if (toggleLineNumbersAction != null)
			toggleLineNumbersAction.dispose();

		if (fIgnoreWhitespace != null)
			fIgnoreWhitespace.dispose();

		getCompareConfiguration().setProperty(
				ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING,
				Boolean.TRUE);
		disposeCompareFilterActions(false);

		if (fSourceViewerDecorationSupport != null) {
			for (SourceViewerDecorationSupport sourceViewerDecorationSupport : fSourceViewerDecorationSupport) {
				sourceViewerDecorationSupport.dispose();
			}
			fSourceViewerDecorationSupport = null;
		}


		if (fAncestor != null)
			fAncestor.dispose();
		fAncestor = null;
		if (fLeft != null)
			fLeft.dispose();
		fLeft = null;
		if (fRight != null)
			fRight.dispose();
		fRight = null;

		if (fColors != null) {
			fColors= null;
		}
		// don't add anything here, disposing colors should be done last
		super.handleDispose(event);
	}

	private void disconnect(ContributorInfo legInfo) {
		if (legInfo != null)
			legInfo.disconnect();
	}

	//-------------------------------------------------------------------------------------------------------------
	//--- internal ------------------------------------------------------------------------------------------------
	//-------------------------------------------------------------------------------------------------------------

	/*
	 * Creates the specific SWT controls for the content areas.
	 * Clients must not call or override this method.
	 */
	@Override
	protected void createControls(Composite composite) {
		if (PlatformUI.isWorkbenchRunning()) {
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, ICompareContextIds.TEXT_MERGE_VIEW);
		}

		// 1st row
		if (fMarginWidth > 0) {
			fAncestorCanvas = new Canvas(composite, SWT.DOUBLE_BUFFERED);
			fAncestorCanvas.addPaintListener(event -> paintSides(event.gc, fAncestor, fAncestorCanvas, false));
			fAncestorCanvas.addMouseListener(
				new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						setCurrentDiff2(handleMouseInSides(fAncestorCanvas, fAncestor, e.y), false);
					}
				}
			);
		}

		fAncestor= createPart(composite);
		setEditable(fAncestor.getSourceViewer(), false);
		fAncestor.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_ancestor, getCompareConfiguration().getAncestorLabel(getInput()));
			}
		});
		fAncestor.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(fAncestor));

		fSummaryHeader= new Canvas(composite, SWT.NONE);
		fHeaderPainter= new HeaderPainter();
		fSummaryHeader.addPaintListener(fHeaderPainter);
		updateResolveStatus();

		// 2nd row
		if (fMarginWidth > 0) {
			fLeftCanvas = new Canvas(composite, SWT.DOUBLE_BUFFERED);
			fLeftCanvas.addPaintListener(event -> paintSides(event.gc, fLeft, fLeftCanvas, false));
			fLeftCanvas.addMouseListener(
				new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						setCurrentDiff2(handleMouseInSides(fLeftCanvas, fLeft, e.y), false);
					}
				}
			);
		}

		fLeft= createPart(composite);
		fLeft.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!fSynchronizedScrolling);
		fLeft.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				// Check for Mirrored status flag before returning the left label's text.
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_left,
						getControl().getData(CompareUI.COMPARE_VIEWER_TITLE),
						getCompareConfiguration().isMirrored() ? getCompareConfiguration().getRightLabel(getInput())
								: getCompareConfiguration().getLeftLabel(getInput()));
			}
		});
		fLeft.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(fLeft));

		fRight= createPart(composite);
		fRight.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!fSynchronizedScrolling);
		fRight.getSourceViewer().getTextWidget().getAccessible().addAccessibleListener(new AccessibleAdapter() {
			@Override
			public void getName(AccessibleEvent e) {
				// Check for Mirrored status flag before returning the right label's text.
				e.result = NLS.bind(CompareMessages.TextMergeViewer_accessible_right,
						getControl().getData(CompareUI.COMPARE_VIEWER_TITLE),
						getCompareConfiguration().isMirrored() ? getCompareConfiguration().getLeftLabel(getInput())
								: getCompareConfiguration().getRightLabel(getInput()));
			}
		});
		fRight.getSourceViewer().addTextPresentationListener(new ChangeHighlighter(fRight));

		IWorkbenchPart part = getCompareConfiguration().getContainer().getWorkbenchPart();
		// part is not available for contexts different than editor
		if (part != null) {
			ISelectionProvider selectionProvider = part.getSite().getSelectionProvider();
			if (selectionProvider instanceof CompareEditorSelectionProvider) {
				CompareEditorSelectionProvider cesp = (CompareEditorSelectionProvider) selectionProvider;
				SourceViewer focusSourceViewer = fFocusPart == null ? null	: fFocusPart.getSourceViewer();
				cesp.setViewers(new SourceViewer[] { fLeft.getSourceViewer(), fRight.getSourceViewer(), fAncestor.getSourceViewer() }, focusSourceViewer);
			}
		}

		hsynchViewport(fAncestor.getSourceViewer(), fLeft.getSourceViewer(), fRight.getSourceViewer());
		hsynchViewport(fLeft.getSourceViewer(), fAncestor.getSourceViewer(), fRight.getSourceViewer());
		hsynchViewport(fRight.getSourceViewer(), fAncestor.getSourceViewer(), fLeft.getSourceViewer());

		if (fMarginWidth > 0) {
			fRightCanvas = new Canvas(composite, SWT.DOUBLE_BUFFERED);
			fRightCanvas.addPaintListener(event -> paintSides(event.gc, fRight, fRightCanvas, fSynchronizedScrolling));
			fRightCanvas.addMouseListener(
				new MouseAdapter() {
					@Override
					public void mouseDown(MouseEvent e) {
						setCurrentDiff2(handleMouseInSides(fRightCanvas, fRight, e.y), false);
					}
				}
			);
		}

		fScrollCanvas= new Canvas(composite, SWT.V_SCROLL);
		Rectangle trim= fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
		fTopInset= trim.y;

		fVScrollBar= fScrollCanvas.getVerticalBar();
		fVScrollBar.setIncrement(1);
		fVScrollBar.setVisible(true);
		fVScrollBar.addListener(SWT.Selection,
			event -> {
				int vpos= ((ScrollBar) event.widget).getSelection();
				synchronizedScrollVertical(vpos);
			}
		);

		fBirdsEyeCanvas = new Canvas(composite, SWT.DOUBLE_BUFFERED);
		fBirdsEyeCanvas.addPaintListener(event -> {
			updateVScrollBar(); // Update scroll bar here as initially viewport height is wrong
			paintBirdsEyeView((Canvas) event.widget, event.gc);
		});

		fBirdsEyeCanvas.addMouseListener(
			new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					setCurrentDiff2(handlemouseInBirdsEyeView(fBirdsEyeCanvas, e.y), true);
				}
			}
		);
		fBirdsEyeCanvas.addMouseMoveListener(
			new MouseMoveListener() {
				private Cursor fLastCursor;

				@Override
				public void mouseMove(MouseEvent e) {
					Cursor cursor= null;
					Diff diff= handlemouseInBirdsEyeView(fBirdsEyeCanvas, e.y);
					if (diff != null && diff.getKind() != RangeDifference.NOCHANGE)
						cursor= e.widget.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
					if (fLastCursor != cursor) {
						fBirdsEyeCanvas.setCursor(cursor);
						fLastCursor= cursor;
					}
				}
			}
		);

		IWorkbenchPart workbenchPart = getCompareConfiguration().getContainer().getWorkbenchPart();
		if (workbenchPart != null) {
			IContextService service = workbenchPart.getSite().getService(IContextService.class);
			if (service != null) {
				service.activateContext("org.eclipse.ui.textEditorScope"); //$NON-NLS-1$
			}
		}
	}

	private void hsynchViewport(final TextViewer tv1, final TextViewer tv2, final TextViewer tv3) {
		final StyledText st1= tv1.getTextWidget();
		final StyledText st2= tv2.getTextWidget();
		final StyledText st3= tv3.getTextWidget();
		final ScrollBar sb1= st1.getHorizontalBar();
		sb1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (fSynchronizedScrolling) {
					int v= sb1.getSelection();
					if (st2.isVisible())
						st2.setHorizontalPixel(v);
					if (st3.isVisible())
						st3.setHorizontalPixel(v);
				}
			}
		});
	}

	private void setCurrentDiff2(Diff diff, boolean reveal) {
		if (diff != null && diff.getKind() != RangeDifference.NOCHANGE) {
			//fCurrentDiff= null;
			setCurrentDiff(diff, reveal);
		}
	}

	private Diff handleMouseInSides(Canvas canvas, MergeSourceViewer tp, int my) {

		int lineHeight= tp.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight= tp.getViewportHeight();

		if (! fHighlightRanges)
			return null;

		if (fMerger.hasChanges()) {
			int shift= tp.getVerticalScrollOffset() + (2-LW);

			Point region= new Point(0, 0);
			char leg = getLeg(tp);
			for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted())
					continue;

				if (fShowCurrentOnly2 && !isCurrentDiff(diff))
					continue;

				tp.getLineRange(diff.getPosition(leg), region);
				int y= (region.x * lineHeight) + shift;
				int h= region.y * lineHeight;

				if (y+h < 0)
					continue;
				if (y >= visibleHeight)
					break;

				if (my >= y && my < y+h)
					return diff;
			}
		}
		return null;
	}

	private Diff getDiffUnderMouse(Canvas canvas, int mx, int my, Rectangle r) {

		if (! fSynchronizedScrolling)
			return null;

		int lineHeight= fLeft.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight= fRight.getViewportHeight();

		Point size= canvas.getSize();
		int w= size.x;

		if (! fHighlightRanges)
			return null;

		if (fMerger.hasChanges()) {
			int lshift= fLeft.getVerticalScrollOffset();
			int rshift= fRight.getVerticalScrollOffset();

			Point region= new Point(0, 0);

			for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted())
					continue;

				if (fShowCurrentOnly2 && !isCurrentDiff(diff))
					continue;

				fLeft.getLineRange(diff.getPosition(LEFT_CONTRIBUTOR), region);
				int ly= (region.x * lineHeight) + lshift;
				int lh= region.y * lineHeight;

				fRight.getLineRange(diff.getPosition(RIGHT_CONTRIBUTOR), region);
				int ry= (region.x * lineHeight) + rshift;
				int rh= region.y * lineHeight;

				if (Math.max(ly+lh, ry+rh) < 0)
					continue;
				if (Math.min(ly, ry) >= visibleHeight)
					break;

				int SIZE = 20;
				int cx= (w-SIZE)/2;
				int cy= ((ly+lh/2) + (ry+rh/2) - SIZE)/2;
				if (my >= cy && my < cy+SIZE && mx >= cx && mx < cx+SIZE) {
					if (r != null) {
						r.x= cx;
						r.y= cy;
						r.width= SIZE;
						r.height= SIZE;
					}
					return diff;
				}
			}
		}
		return null;
	}

	private Diff handlemouseInBirdsEyeView(Canvas canvas, int my) {
		return fMerger.findDiff(getViewportHeight(), fSynchronizedScrolling, canvas.getSize(), my);
	}

	private void paintBirdsEyeView(Canvas canvas, GC gc) {

		Color c;
		Rectangle r= new Rectangle(0, 0, 0, 0);
		int yy, hh;

		Point size= canvas.getSize();

		int virtualHeight= fSynchronizedScrolling ? fMerger.getVirtualHeight() : fMerger.getRightHeight();
		if (virtualHeight < getViewportHeight())
			return;

		Display display= canvas.getDisplay();
		int y= 0;
		boolean allOutgoing = allOutgoing();
		for (Iterator<Diff> iterator = fMerger.rangesIterator(); iterator.hasNext();) {
			Diff diff = iterator.next();
			int h= fSynchronizedScrolling ? diff.getMaxDiffHeight()
											: diff.getRightHeight();

			if (fMerger.useChange(diff)) {

				yy= (y*size.y)/virtualHeight;
				hh= (h*size.y)/virtualHeight;
				if (hh < 3)
					hh= 3;

				c = getColor(display, getFillColor(diff, allOutgoing));
				if (c != null) {
					gc.setBackground(c);
					gc.fillRectangle(BIRDS_EYE_VIEW_INSET, yy, size.x-(2*BIRDS_EYE_VIEW_INSET),hh);
				}
				c = getColor(display, getStrokeColor(diff, allOutgoing));
				if (c != null) {
					gc.setForeground(c);
					r.x= BIRDS_EYE_VIEW_INSET;
					r.y= yy;
					r.width= size.x-(2*BIRDS_EYE_VIEW_INSET)-1;
					r.height= hh;
					if (isCurrentDiff(diff)) {
						gc.setLineWidth(2);
						r.x++;
						r.y++;
						r.width--;
						r.height--;
					} else {
						gc.setLineWidth(0 /* 1 */);
					}
					gc.drawRectangle(r);
				}
			}

			y+= h;
		}
	}

	private void refreshBirdsEyeView() {
		if (fBirdsEyeCanvas != null)
			fBirdsEyeCanvas.redraw();
	}

	/**
	 * Override to give focus to the pane that previously had focus or to a suitable
	 * default pane.
	 * @see org.eclipse.compare.contentmergeviewer.ContentMergeViewer#handleSetFocus()
	 * @since 3.3
	 */
	@Override
	protected boolean handleSetFocus() {
		if (fRedoDiff) {
			new UIJob(CompareMessages.DocumentMerger_0) {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					update(true);
					updateStructure();
					return Status.OK_STATUS;
				}
			}.schedule();
			fRedoDiff = false;
		}
		if (fFocusPart == null) {
			if (fLeft != null && fLeft.getEnabled()) {
				fFocusPart= fLeft;
			} else if (fRight != null && fRight.getEnabled()) {
				fFocusPart= fRight;
			} else if (fAncestor != null && fAncestor.getEnabled()) {
				fFocusPart= fAncestor;
			}
		}
		if (fFocusPart != null) {
			StyledText st= fFocusPart.getSourceViewer().getTextWidget();
			if (st != null)
				return st.setFocus();
		}
		return false;	// could not set focus
	}


	class HoverResizer extends Resizer {
		Canvas fCanvas;
		public HoverResizer(Canvas c, int dir) {
			super(c, dir);
			fCanvas= c;
		}
		@Override
		public void mouseMove(MouseEvent e) {
			if (!fIsDown && fUseSingleLine && isAnySideEditable() && handleMouseMoveOverCenter(fCanvas, e.x, e.y))
				return;
			super.mouseMove(e);
		}
	}

	@Override
	protected final Control createCenterControl(Composite parent) {
		if (fSynchronizedScrolling) {


			final Canvas canvas = new Canvas(parent, SWT.DOUBLE_BUFFERED);

			canvas.addPaintListener(event -> paintCenter((Canvas) event.widget, event.gc));
			new HoverResizer(canvas, HORIZONTAL);

			Cursor normalCursor= canvas.getDisplay().getSystemCursor(SWT.CURSOR_ARROW);
			int style= fIsMac ? SWT.FLAT : SWT.PUSH;

			fLeftToRightButton= new Button(canvas, style);
			fLeftToRightButton.setCursor(normalCursor);
			fLeftToRightButton.setText(COPY_LEFT_TO_RIGHT_INDICATOR);
			fLeftToRightButton.setToolTipText(
					Utilities.getString(getResourceBundle(), "action.CopyDiffLeftToRight.tooltip")); //$NON-NLS-1$
			fLeftToRightButton.setVisible(false);
			fLeftToRightButton.addSelectionListener(
				new SelectionAdapter() {
					@Override
					public void widgetSelected(SelectionEvent e) {
						handleCenterButtonSelection(true);
					}
				}
			);

			fRightToLeftButton= new Button(canvas, style);
			fRightToLeftButton.setCursor(normalCursor);
			fRightToLeftButton.setText(COPY_RIGHT_TO_LEFT_INDICATOR);
			fRightToLeftButton.setToolTipText(
					Utilities.getString(getResourceBundle(), "action.CopyDiffRightToLeft.tooltip")); //$NON-NLS-1$
			fRightToLeftButton.setVisible(false);
			fRightToLeftButton.addSelectionListener(
					new SelectionAdapter() {
						@Override
						public void widgetSelected(SelectionEvent e) {
							handleCenterButtonSelection(false);
						}
					}
			);

			return canvas;
		}
		return super.createCenterControl(parent);
	}

	private void handleCenterButtonSelection(boolean leftToRight) {
		fLeftToRightButton.setVisible(false);
		fRightToLeftButton.setVisible(false);
		if (fButtonDiff != null) {
			setCurrentDiff(fButtonDiff, false);
			copy(fCurrentDiff, leftToRight, false);
		}
	}

	private boolean handleMouseMoveOverCenter(Canvas canvas, int x, int y) {
		Rectangle r= new Rectangle(0, 0, 0, 0);
		Diff diff= getDiffUnderMouse(canvas, x, y, r);
		if (diff != fButtonDiff) {
			if (diff != null) {
				fButtonDiff= diff;
				boolean leftEditable= fLeft.getSourceViewer().isEditable();
				boolean rightEditable= fRight.getSourceViewer().isEditable();
				if (leftEditable && rightEditable) {
					int height= r.height;
					int leftToRightY= r.y - height/2;
					int rightToLeftY= leftToRightY + height;
					Rectangle bounds = canvas.getBounds();
					if (leftToRightY < 0) {
						// button must not be hidden at top
						leftToRightY= 0;
						rightToLeftY= height;
					} else if (rightToLeftY + height > bounds.height) {
						// button must not be hidden at bottom
						leftToRightY= bounds.height - height - height;
						rightToLeftY= leftToRightY + height;
					}
					Rectangle leftToRightBounds= new Rectangle(r.x, leftToRightY, r.width, r.height);
					fLeftToRightButton.setBounds(leftToRightBounds);
					fLeftToRightButton.setVisible(true);
					Rectangle rightToLeftBounds= new Rectangle(r.x, rightToLeftY, r.width, r.height);
					fRightToLeftButton.setBounds(rightToLeftBounds);
					fRightToLeftButton.setVisible(true);
				} else if (leftEditable) {
					fRightToLeftButton.setBounds(r);
					fRightToLeftButton.setVisible(true);
					fLeftToRightButton.setVisible(false);
				} else if (rightEditable) {
					fLeftToRightButton.setBounds(r);
					fLeftToRightButton.setVisible(true);
					fRightToLeftButton.setVisible(false);
				} else
					fButtonDiff= null;
			} else {
				fRightToLeftButton.setVisible(false);
				fLeftToRightButton.setVisible(false);
				fButtonDiff= null;
			}
		}
		return fButtonDiff != null;
	}

	@Override
	protected final int getCenterWidth() {
		if (fSynchronizedScrolling)
			return CENTER_WIDTH;
		return super.getCenterWidth();
	}

	private int getDirection() {
		switch (fTextDirection) {
		case SWT.LEFT_TO_RIGHT:
		case SWT.RIGHT_TO_LEFT:
			if (fInheritedDirection == fTextDirection)
				return SWT.NONE;
			return fTextDirection;
		default:
			return fInheritedDirection;
		}
	}

	/**
	 * Creates a new source viewer. This method is called when creating and
	 * initializing the content areas of the merge viewer (see
	 * {@link #createControls(Composite)}). It is called three
	 * times for each text part of the comparison: ancestor, left, right.
	 * Clients may implement to provide their own type of source viewers. The
	 * viewer is not expected to be configured with a source viewer
	 * configuration.
	 *
	 * @param parent
	 *            the parent of the viewer's control
	 * @param textOrientation
	 *            style constant bit for text orientation
	 * @return Default implementation returns an instance of
	 *         {@link SourceViewer}.
	 * @since 3.5
	 */
	protected SourceViewer createSourceViewer(Composite parent, int textOrientation) {
		return new SourceViewer(parent, new CompositeRuler(), textOrientation | SWT.H_SCROLL | SWT.V_SCROLL);
	}

	/**
	 * Tells whether the given text viewer is backed by an editor.
	 *
	 * @param textViewer the text viewer to check
	 * @return <code>true</code> if the viewer is backed by an editor
	 * @since 3.5
	 */
	protected boolean isEditorBacked(ITextViewer textViewer) {
		return false;
	}

	/**
	 * Returns an editor input for the given source viewer. The method returns
	 * <code>null</code> when no input is available, for example when the input
	 * for the merge viewer has not been set yet.
	 *
	 * @param sourceViewer
	 *            the source viewer to get input for
	 * @return input for the given viewer or <code>null</code> when no input is
	 *         available
	 *
	 * @since 3.5
	 */
	protected IEditorInput getEditorInput(ISourceViewer sourceViewer) {
		if (fLeft != null && sourceViewer == fLeft.getSourceViewer())
			if (fLeftContributor != null)
				return fLeftContributor.getDocumentKey();
		if (fRight != null && sourceViewer == fRight.getSourceViewer())
			if (fRightContributor != null)
				return fRightContributor.getDocumentKey();
		if (fAncestor != null
				&& sourceViewer == fAncestor.getSourceViewer())
			if (fAncestorContributor != null)
				return fAncestorContributor.getDocumentKey();
		return null;
	}

	/*
	 * Creates and initializes a text part.
	 */
	private MergeSourceViewer createPart(Composite parent) {
		final MergeSourceViewer viewer = new MergeSourceViewer(
				createSourceViewer(parent, getDirection()),
				getResourceBundle(), getCompareConfiguration().getContainer());
		final StyledText te= viewer.getSourceViewer().getTextWidget();

		if (!fConfirmSave)
			viewer.hideSaveAction();

		te.addPaintListener(event -> paint(event, viewer));
		te.addKeyListener(
			new KeyAdapter() {
				@Override
				public void keyPressed(KeyEvent e) {
					handleSelectionChanged(viewer);
				}
			}
		);
		te.addMouseListener(
			new MouseAdapter() {
				@Override
				public void mouseDown(MouseEvent e) {
					//syncViewport(part);
					handleSelectionChanged(viewer);
				}
			}
		);

		te.addFocusListener(
			new FocusAdapter() {
				@Override
				public void focusGained(FocusEvent fe) {
					setActiveViewer(viewer, true);
				}
				@Override
				public void focusLost(FocusEvent fe) {
					setActiveViewer(viewer, false);
				}
			}
		);

		viewer.getSourceViewer().addViewportListener(
			verticalPosition -> syncViewport(viewer)
		);

		Font font= JFaceResources.getFont(fSymbolicFontName);
		if (font != null)
			te.setFont(font);

		if (fBackground != null)	// not default
			te.setBackground(getColor(parent.getDisplay(), fBackground));

		// Add the find action to the popup menu of the viewer
		contributeFindAction(viewer);

		contributeGotoLineAction(viewer);

		contributeChangeEncodingAction(viewer);

		// showWhiteSpaceAction is added in createToolItems when fAncestor, fLeft and
		// fRight are initialized

		contributeDiffBackgroundListener(viewer);

		return viewer;
	}

	private void setActiveViewer(MergeSourceViewer viewer, boolean activate) {
		connectContributedActions(viewer, activate);
		if (activate) {
			fFocusPart= viewer;
			connectGlobalActions(fFocusPart);
		} else {
			connectGlobalActions(null);
		}
	}

	private SourceViewerDecorationSupport getSourceViewerDecorationSupport(ISourceViewer viewer) {
		SourceViewerDecorationSupport support = new SourceViewerDecorationSupport(viewer, null, null, EditorsUI.getSharedTextColors());
		support.setCursorLinePainterPreferenceKeys(CURRENT_LINE, CURRENT_LINE_COLOR);
		fSourceViewerDecorationSupport.add(support);
		return support;
	}

	private void contributeFindAction(MergeSourceViewer viewer) {
		IAction action;
		IWorkbenchPart wp = getCompareConfiguration().getContainer().getWorkbenchPart();
		if (wp != null)
			action = new FindReplaceAction(getResourceBundle(), "Editor.FindReplace.", wp); //$NON-NLS-1$
		else
			action = new FindReplaceAction(getResourceBundle(), "Editor.FindReplace.", viewer.getSourceViewer().getControl().getShell(), getFindReplaceTarget()); //$NON-NLS-1$
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		viewer.addAction(MergeSourceViewer.FIND_ID, action);
	}

	private void contributeGotoLineAction(MergeSourceViewer viewer) {
		IAction action = new GotoLineAction(viewer.getAdapter(ITextEditor.class));
		action.setActionDefinitionId(ITextEditorActionDefinitionIds.LINE_GOTO);
		viewer.addAction(MergeSourceViewer.GOTO_LINE_ID, action);
	}

	private void contributeChangeEncodingAction(MergeSourceViewer viewer) {
		IAction action = new ChangeEncodingAction(getTextEditorAdapter());
		viewer.addAction(MergeSourceViewer.CHANGE_ENCODING_ID, action);
	}

	private void contributeDiffBackgroundListener(final MergeSourceViewer viewer) {
		viewer.getSourceViewer().getTextWidget().addLineBackgroundListener(
				event -> {
					StyledText textWidget = viewer.getSourceViewer().getTextWidget();
					if (textWidget != null) {

						int caret = textWidget.getCaretOffset();
						int length = event.lineText.length();

						if (event.lineOffset <= caret
								&& caret <= event.lineOffset + length) {
							// current line, do nothing
							// decorated by CursorLinePainter
						} else {
							// find diff for the event line
							Diff diff = findDiff(viewer, event.lineOffset,
									event.lineOffset + length);
							if (diff != null && updateDiffBackground(diff)) {
								// highlights only the event line, not the
								// whole diff
								event.lineBackground = getColor(fComposite
										.getDisplay(), getFillColor(diff, allOutgoing()));
							}
						}
					}
				});
	}

	private void connectGlobalActions(final MergeSourceViewer part) {
		if (fHandlerService != null) {
			if (part != null)
				part.updateActions();
			fHandlerService.updatePaneActionHandlers(() -> {
				for (int i= 0; i < GLOBAL_ACTIONS.length; i++) {
					IAction action= null;
					if (part != null) {
						action= part.getAction(TEXT_ACTIONS[i]);
					}
					fHandlerService.setGlobalActionHandler(GLOBAL_ACTIONS[i], action);
				}
			});
		}
	}

	private void connectContributedActions(final MergeSourceViewer viewer, final boolean connect) {
		if (fHandlerService != null) {
			fHandlerService.updatePaneActionHandlers(() -> {
				if (viewer != null) {
					setActionsActivated(viewer.getSourceViewer(), connect);
					if (isEditorBacked(viewer.getSourceViewer()) && connect) {
						/*
						 * If editor backed, activating contributed actions
						 * might have disconnected actions provided in
						 * CompareEditorContributor => when connecting,
						 * refresh active editor in the contributor, when
						 * disconnecting do nothing. See bug 261229.
						 */
						IWorkbenchPart part = getCompareConfiguration().getContainer().getWorkbenchPart();
						if (part instanceof CompareEditor) {
							((CompareEditor) part).refreshActionBarsContributor();
						}
					}
				}
			});
		}
	}

	/**
	 * Activates or deactivates actions of the given source viewer.
	 * <p>
	 * The default implementation does nothing, but clients should override to properly react to
	 * viewers switching.
	 * </p>
	 *
	 * @param sourceViewer the source viewer
	 * @param state <code>true</code> if activated
	 * @since 3.5
	 */
	protected void setActionsActivated(SourceViewer sourceViewer, boolean state) {
		// default implementation does nothing
	}

	private IDocument getElementDocument(char type, Object element) {
		if (element instanceof IDocument) {
			return (IDocument) element;
		}
		ITypedElement te= Utilities.getLeg(type, element);
		// First check the contributors for the document
		IDocument document = null;
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			document = getDocument(te, fAncestorContributor);
			break;
		case LEFT_CONTRIBUTOR:
			document = getDocument(te, fLeftContributor);
			break;
		case RIGHT_CONTRIBUTOR:
			document = getDocument(te, fRightContributor);
			break;
		default:
			break;
		}
		if (document != null)
			return document;
		// The document is not associated with the input of the viewer so try to find the document
		return Utilities.getDocument(type, element, isUsingDefaultContentProvider(), canHaveSharedDocument());
	}

	private boolean isUsingDefaultContentProvider() {
		return getContentProvider() instanceof MergeViewerContentProvider;
	}

	private boolean canHaveSharedDocument() {
		return getDocumentPartitioning() != null
			|| getDocumentPartitioner() == null;
	}

	private IDocument getDocument(ITypedElement te, ContributorInfo info) {
		if (info != null && info.getElement() == te)
			return info.getDocument();
		return null;
	}

	IDocument getDocument(char type, Object input) {
		IDocument doc= getElementDocument(type, input);
		if (doc != null)
			return doc;

		if (input instanceof IDiffElement) {
			IDiffContainer parent= ((IDiffElement) input).getParent();
			return getElementDocument(type, parent);
		}
		return null;
	}

	/*
	 * Returns true if the given inputs map to the same documents
	 */
	boolean sameDoc(char type, Object newInput, Object oldInput) {
		IDocument newDoc= getDocument(type, newInput);
		IDocument oldDoc= getDocument(type, oldInput);
		return newDoc == oldDoc;
	}

	/**
	 * Overridden to prevent save confirmation if new input is sub document of current input.
	 * @param newInput the new input of this viewer, or <code>null</code> if there is no new input
	 * @param oldInput the old input element, or <code>null</code> if there was previously no input
	 * @return <code>true</code> if saving was successful, or if the user didn't want to save (by pressing 'NO' in the confirmation dialog).
	 * @since 2.0
	 */
	@Override
	protected boolean doSave(Object newInput, Object oldInput) {
		// TODO: Would be good if this could be restated in terms of Saveables and moved up
		if (oldInput != null && newInput != null) {
			// check whether underlying documents have changed.
			if (sameDoc(ANCESTOR_CONTRIBUTOR, newInput, oldInput) &&
					sameDoc(LEFT_CONTRIBUTOR, newInput, oldInput) &&
						sameDoc(RIGHT_CONTRIBUTOR, newInput, oldInput)) {
				if (DEBUG) System.out.println("----- Same docs !!!!");	//$NON-NLS-1$
				return false;
			}
		}

		if (DEBUG) System.out.println("***** New docs !!!!");	//$NON-NLS-1$

		removeFromDocumentManager(ANCESTOR_CONTRIBUTOR, oldInput);
		removeFromDocumentManager(LEFT_CONTRIBUTOR, oldInput);
		removeFromDocumentManager(RIGHT_CONTRIBUTOR, oldInput);

		if (DEBUG)
			DocumentManager.dump();

		return super.doSave(newInput, oldInput);
	}

	private void removeFromDocumentManager(char leg, Object oldInput) {
		IDocument document= getDocument(leg, oldInput);
		if (document != null)
			DocumentManager.remove(document);
	}

	private ITypedElement getParent(char type) {
		Object input= getInput();
		if (input instanceof IDiffElement) {
			IDiffContainer parent= ((IDiffElement) input).getParent();
			return Utilities.getLeg(type, parent);
		}
		return null;
	}

	/*
	 * Initializes the text viewers of the three content areas with the given input objects.
	 * Subclasses may extend.
	 */
	@Override
	protected void updateContent(Object ancestor, Object left, Object right) {
		boolean emptyInput= (ancestor == null && left == null && right == null);

		Object input= getInput();

		configureCompareFilterActions(input, ancestor, left, right);

		Position leftRange= null;
		Position rightRange= null;

		// if one side is empty use container
		if (FIX_47640 && !emptyInput && (left == null || right == null)) {
			if (input instanceof IDiffElement) {
				IDiffContainer parent= ((IDiffElement) input).getParent();
				if (parent instanceof ICompareInput) {
					ICompareInput ci= (ICompareInput) parent;

					if (ci.getAncestor() instanceof IDocumentRange
							|| ci.getLeft() instanceof IDocumentRange
							|| ci.getRight() instanceof IDocumentRange) {
						if (left instanceof IDocumentRange)
							leftRange= ((IDocumentRange) left).getRange();
						if (right instanceof IDocumentRange)
							rightRange= ((IDocumentRange) right).getRange();

						ancestor= ci.getAncestor();
						left= getCompareConfiguration().isMirrored() ? ci.getRight() : ci.getLeft();
						right= getCompareConfiguration().isMirrored() ? ci.getLeft() : ci.getRight();
					}
				}
			}
		}

		fHighlightRanges= left != null && right != null;

		resetDiffs();
		fHasErrors= false; // start with no errors

		IMergeViewerContentProvider cp= getMergeContentProvider();

		if (cp instanceof MergeViewerContentProvider) {
			MergeViewerContentProvider mcp= (MergeViewerContentProvider) cp;
			mcp.setAncestorError(null);
			mcp.setLeftError(null);
			mcp.setRightError(null);
		}

		// Record current contributors so we disconnect after creating the new ones.
		// This is done in case the old and new use the same document.
		ContributorInfo oldLeftContributor = fLeftContributor;
		ContributorInfo oldRightContributor = fRightContributor;
		ContributorInfo oldAncestorContributor = fAncestorContributor;

		// Create the new contributor
		fLeftContributor = createLegInfoFor(left, LEFT_CONTRIBUTOR);
		fRightContributor = createLegInfoFor(right, RIGHT_CONTRIBUTOR);
		fAncestorContributor = createLegInfoFor(ancestor, ANCESTOR_CONTRIBUTOR);

		fLeftContributor.transferContributorStateFrom(oldLeftContributor);
		fRightContributor.transferContributorStateFrom(oldRightContributor);
		fAncestorContributor.transferContributorStateFrom(oldAncestorContributor);

		// Now disconnect the old ones
		disconnect(oldLeftContributor);
		disconnect(oldRightContributor);
		disconnect(oldAncestorContributor);

		// Get encodings from streams. If an encoding is null, abide by the other one
		// Defaults to workbench encoding only if both encodings are null
		fLeftContributor.setEncodingIfAbsent(fRightContributor);
		fRightContributor.setEncodingIfAbsent(fLeftContributor);
		fAncestorContributor.setEncodingIfAbsent(fLeftContributor);

		if (!isConfigured) {
			configureSourceViewer(fAncestor.getSourceViewer(), false, null);
			configureSourceViewer(fLeft.getSourceViewer(), isLeftEditable() && cp.isLeftEditable(input), fLeftContributor);
			configureSourceViewer(fRight.getSourceViewer(), isRightEditable() && cp.isRightEditable(input), fRightContributor);
			isConfigured = true; // configure once
		}

		// set new documents
		fLeftContributor.setDocument(fLeft, isLeftEditable() && cp.isLeftEditable(input));
		fLeftLineCount= fLeft.getLineCount();

		fRightContributor.setDocument(fRight, isRightEditable() && cp.isRightEditable(input));
		fRightLineCount= fRight.getLineCount();

		fAncestorContributor.setDocument(fAncestor, false);

		setSyncScrolling(fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING));

		update(false);

		if (!fHasErrors && !emptyInput && !fComposite.isDisposed()) {
			if (isRefreshing()) {
				fLeftContributor.updateSelection(fLeft, !fSynchronizedScrolling);
				fRightContributor.updateSelection(fRight, !fSynchronizedScrolling);
				fAncestorContributor.updateSelection(fAncestor, !fSynchronizedScrolling);
				if (fSynchronizedScrolling && fSynchronziedScrollPosition != -1) {
					synchronizedScrollVertical(fSynchronziedScrollPosition);
				}
			} else {
				if (isPatchHunk()) {
					if (right != null && Adapters.adapt(right, IHunk.class) != null)
						fLeft.getSourceViewer().setTopIndex(getHunkStart());
					else
						fRight.getSourceViewer().setTopIndex(getHunkStart());
				} else {
					Diff selectDiff= null;
					if (FIX_47640) {
						if (leftRange != null)
							selectDiff= fMerger.findDiff(LEFT_CONTRIBUTOR, leftRange);
						else if (rightRange != null)
							selectDiff= fMerger.findDiff(RIGHT_CONTRIBUTOR, rightRange);
					}
					if (selectDiff != null)
						setCurrentDiff(selectDiff, true);
					else
						selectFirstDiff(true);
				}
			}
		}

	}

	private void configureSourceViewer(SourceViewer sourceViewer, boolean editable, ContributorInfo contributor) {
		setEditable(sourceViewer, editable);
		configureTextViewer(sourceViewer);
		if (editable && contributor != null) {
			IDocument document = sourceViewer.getDocument();
			if (document != null)
				contributor.connectPositionUpdater(document);
		}
		if (!isCursorLinePainterInstalled(sourceViewer))
			getSourceViewerDecorationSupport(sourceViewer).install(fPreferenceStore);
	}

	private boolean isCursorLinePainterInstalled(SourceViewer viewer) {
		return viewer.getTextWidget().getTypedListeners(ST.LineGetBackground, CursorLinePainter.class) //
				.findFirst().isPresent();
	}

	/**
	 * Sets the editable state of the given source viewer.
	 *
	 * @param sourceViewer
	 *            the source viewer
	 * @param state
	 *            the state
	 * @since 3.5
	 */
	protected void setEditable(ISourceViewer sourceViewer, boolean state) {
		sourceViewer.setEditable(state);
	}

	private boolean isRefreshing() {
		return isRefreshing > 0;
	}

	private ContributorInfo createLegInfoFor(Object element, char leg) {
		return new ContributorInfo(this, element, leg);
	}

	private boolean updateDiffBackground(Diff diff) {

		if (!fHighlightRanges)
			return false;

		if (diff == null || diff.isToken())
			return false;

		if (fShowCurrentOnly && !isCurrentDiff(diff))
			return false;

		return true;
	}

	/*
	 * Called whenever one of the documents changes.
	 * Sets the dirty state of this viewer and updates the lines.
	 * Implements IDocumentListener.
	 */
	private void documentChanged(DocumentEvent e, boolean dirty) {
		final IDocument doc= e.getDocument();

		if (doc == fLeft.getSourceViewer().getDocument()) {
			setLeftDirty(dirty);
		} else if (doc == fRight.getSourceViewer().getDocument()) {
			setRightDirty(dirty);
		}
		if (!isLeftDirty() && !isRightDirty()) {
			fRedoDiff = false;
			final Diff oldDiff = getLastDiff();
			new UIJob(CompareMessages.DocumentMerger_0) {
				@Override
				public IStatus runInUIThread(IProgressMonitor monitor) {
					if (!getControl().isDisposed()) {
						doDiff();
						if (!getControl().isDisposed()) {
							Diff newDiff = findNewDiff(oldDiff);
							if (newDiff != null) {
								updateStatus(newDiff);
								setCurrentDiff(newDiff, true);
							}
							invalidateLines();
							updateLines(doc);
						}
					}
					return Status.OK_STATUS;
				}
			}.schedule();
		} else {
			updateLines(doc);
		}
	}


	private void saveDiff() {
			fSavedDiff = fCurrentDiff;
	}

	private Diff getLastDiff() {
		if (fCurrentDiff != null) {
			return fCurrentDiff;
		}
		return fSavedDiff;
	}


	private Diff findNewDiff(Diff oldDiff) {
		if (oldDiff == null)
			return null;
		Diff newDiff = findNewDiff(oldDiff, LEFT_CONTRIBUTOR);
		if (newDiff == null) {
			newDiff = findNewDiff(oldDiff, RIGHT_CONTRIBUTOR);
		}
		return newDiff;
	}

	private Diff findNewDiff(Diff oldDiff, char type) {
		int offset = oldDiff.getPosition(type).offset;
		int length = oldDiff.getPosition(type).length;

		// DocumentMerger.findDiff method doesn't really work well with 0-length
		// diffs
		if (length == 0) {
			if (offset > 0)
				offset--;
			length = 1;
		}
		return fMerger.findDiff(type, offset, offset + length);
	}

	/*
	 * This method is called if a range of text on one side is copied into an empty sub-document
	 * on the other side. The method returns the position where the sub-document is placed into the base document.
	 * This default implementation determines the position by using the text range differencer.
	 * However this position is not always optimal for specific types of text.
	 * So subclasses (which are aware of the type of text they are dealing with)
	 * may override this method to find a better position where to insert a newly added
	 * piece of text.
	 * @param type the side for which the insertion position should be determined: 'A' for ancestor, 'L' for left hand side, 'R' for right hand side.
	 * @param input the current input object of this viewer
	 * @since 2.0
	 */
	protected int findInsertionPosition(char type, ICompareInput input) {

		ITypedElement other= null;
		char otherType= 0;

		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			other= input.getLeft();
			otherType= LEFT_CONTRIBUTOR;
			if (other == null) {
				other= input.getRight();
				otherType= RIGHT_CONTRIBUTOR;
			}
			break;
		case LEFT_CONTRIBUTOR:
			other= input.getRight();
			otherType= RIGHT_CONTRIBUTOR;
			if (other == null) {
				other= input.getAncestor();
				otherType= ANCESTOR_CONTRIBUTOR;
			}
			break;
		case RIGHT_CONTRIBUTOR:
			other= input.getLeft();
			otherType= LEFT_CONTRIBUTOR;
			if (other == null) {
				other= input.getAncestor();
				otherType= ANCESTOR_CONTRIBUTOR;
			}
			break;
		default:
			break;
		}

		if (other instanceof IDocumentRange) {
			IDocumentRange dr= (IDocumentRange) other;
			Position p= dr.getRange();
			Diff diff= findDiff(otherType, p.offset);
			return fMerger.findInsertionPoint(diff, type);
		}
		return 0;
	}

	private void setError(char type, String message) {
		IMergeViewerContentProvider cp= getMergeContentProvider();
		if (cp instanceof MergeViewerContentProvider) {
			MergeViewerContentProvider mcp= (MergeViewerContentProvider) cp;
			switch (type) {
			case ANCESTOR_CONTRIBUTOR:
				mcp.setAncestorError(message);
				break;
			case LEFT_CONTRIBUTOR:
				mcp.setLeftError(message);
				break;
			case RIGHT_CONTRIBUTOR:
				mcp.setRightError(message);
				break;
			default:
				break;
			}
		}
		fHasErrors= true;
	}

	private void updateDirtyState(IEditorInput key,
			IDocumentProvider documentProvider, char type) {
		boolean dirty = documentProvider.canSaveDocument(key);
		boolean oldLeftDirty = isLeftDirty();
		boolean oldRightDirty = isRightDirty();
		if (type == LEFT_CONTRIBUTOR)
			setLeftDirty(dirty);
		else if (type == RIGHT_CONTRIBUTOR)
			setRightDirty(dirty);
		if ((oldLeftDirty && !isLeftDirty())
				|| (oldRightDirty && !isRightDirty())) {
			/*
			 * Dirty state has changed from true to false, combined dirty state
			 * is false. _In most cases_ this means that save has taken place
			 * outside compare editor. Ask to redo diff calculation when the
			 * editor gets focus.
			 *
			 * However, undoing all the changes made in another editor would
			 * result in asking for redo diff as well. In this case, we set the
			 * flag back to false, see
			 * TextMergeViewer.documentChanged(DocumentEvent, boolean)
			 */
			fRedoDiff = true;
		}
	}

	private Position getNewRange(char type, Object input) {
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			return fNewAncestorRanges.get(input);
		case LEFT_CONTRIBUTOR:
			return fNewLeftRanges.get(input);
		case RIGHT_CONTRIBUTOR:
			return fNewRightRanges.get(input);
		default:
			return null;
		}
	}

	private void addNewRange(char type, Object input, Position range) {
		switch (type) {
		case ANCESTOR_CONTRIBUTOR:
			fNewAncestorRanges.put(input, range);
			break;
		case LEFT_CONTRIBUTOR:
			fNewLeftRanges.put(input, range);
			break;
		case RIGHT_CONTRIBUTOR:
			fNewRightRanges.put(input, range);
			break;
		default:
			break;
		}
	}

	/**
	 * Returns the contents of the underlying document as an array of bytes using the current workbench encoding.
	 *
	 * @param left if <code>true</code> the contents of the left side is returned; otherwise the right side
	 * @return the contents of the left or right document or null
	 */
	@Override
	protected byte[] getContents(boolean left) {
		MergeSourceViewer v= left ? fLeft : fRight;
		if (v != null) {
			IDocument d= v.getSourceViewer().getDocument();
			if (d != null) {
				String contents= d.get();
				if (contents != null) {
					byte[] bytes;
					try {
						bytes= contents.getBytes(left ? fLeftContributor.internalGetEncoding() : fRightContributor.internalGetEncoding());
					} catch(UnsupportedEncodingException ex) {
						// use default encoding
						bytes= contents.getBytes();
					}
					return bytes;
				}
			}
		}
		return null;
	}

	private IRegion normalizeDocumentRegion(IDocument doc, IRegion region) {

		if (region == null || doc == null)
			return region;

		int maxLength= doc.getLength();

		int start= region.getOffset();
		if (start < 0)
			start= 0;
		else if (start > maxLength)
			start= maxLength;

		int length= region.getLength();
		if (length < 0)
			length= 0;
		else if (start + length > maxLength)
			length= maxLength - start;

		return new Region(start, length);
	}

	@Override
	protected final void handleResizeAncestor(int x, int y, int width, int height) {
		if (width > 0) {
			Rectangle trim= fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
			int scrollbarHeight= trim.height;
			if (Utilities.okToUse(fAncestorCanvas))
				fAncestorCanvas.setVisible(true);
			if (fAncestor.isControlOkToUse())
				fAncestor.getSourceViewer().getTextWidget().setVisible(true);

			if (fAncestorCanvas != null) {
				fAncestorCanvas.setBounds(x, y, fMarginWidth, height-scrollbarHeight);
				x+= fMarginWidth;
				width-= fMarginWidth;
			}
			fAncestor.setBounds(x, y, width, height);
		} else {
			if (Utilities.okToUse(fAncestorCanvas))
				fAncestorCanvas.setVisible(false);
			if (fAncestor.isControlOkToUse()) {
				StyledText t= fAncestor.getSourceViewer().getTextWidget();
				t.setVisible(false);
				fAncestor.setBounds(0, 0, 0, 0);
				if (fFocusPart == fAncestor) {
					fFocusPart= fLeft;
					fFocusPart.getSourceViewer().getTextWidget().setFocus();
				}
			}
		}
	}

	@Override
		protected final void handleResizeLeftRight(int x, int y, int width1, int centerWidth, int width2,  int height) {
			if (fBirdsEyeCanvas != null)
				width2-= BIRDS_EYE_VIEW_WIDTH;

		Rectangle trim= fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
		int scrollbarHeight= trim.height + trim.x;

		Composite composite= (Composite) getControl();

		int leftTextWidth= width1;
		if (fLeftCanvas != null) {
			fLeftCanvas.setBounds(x, y, fMarginWidth, height-scrollbarHeight);
			x+= fMarginWidth;
			leftTextWidth-= fMarginWidth;
		}

		fLeft.setBounds(x, y, leftTextWidth, height);
		x+= leftTextWidth;

		if (fCenter == null || fCenter.isDisposed())
			fCenter= createCenterControl(composite);
		fCenter.setBounds(x, y, centerWidth, height-scrollbarHeight);
		x+= centerWidth;

		if (!fSynchronizedScrolling) {	// canvas is to the left of text
			if (fRightCanvas != null) {
				fRightCanvas.setBounds(x, y, fMarginWidth, height-scrollbarHeight);
				fRightCanvas.redraw();
				x+= fMarginWidth;
			}
			// we draw the canvas to the left of the text widget
		}

		int scrollbarWidth= 0;
		if (fSynchronizedScrolling && fScrollCanvas != null) {
			trim= fLeft.getSourceViewer().getTextWidget().computeTrim(0, 0, 0, 0);
				// one pixel was cut off
			scrollbarWidth= trim.width + 2*trim.x+1;
		}
		int rightTextWidth= width2-scrollbarWidth;
		if (fRightCanvas != null)
			rightTextWidth-= fMarginWidth;
		fRight.setBounds(x, y, rightTextWidth, height);
		x+= rightTextWidth;

		if (fSynchronizedScrolling) {
			if (fRightCanvas != null) {	// canvas is to the right of the text
				fRightCanvas.setBounds(x, y, fMarginWidth, height-scrollbarHeight);
				x+= fMarginWidth;
			}
			if (fScrollCanvas != null)
				fScrollCanvas.setBounds(x, y, scrollbarWidth, height-scrollbarHeight);
		}

			if (fBirdsEyeCanvas != null) {
				int verticalScrollbarButtonHeight= scrollbarWidth;
			int horizontalScrollbarButtonHeight= scrollbarHeight;
			if (fIsMac) {
				verticalScrollbarButtonHeight+= 2;
				horizontalScrollbarButtonHeight= 18;
			}
				if (fSummaryHeader != null)
				fSummaryHeader.setBounds(x+scrollbarWidth, y, BIRDS_EYE_VIEW_WIDTH, verticalScrollbarButtonHeight);
				y+= verticalScrollbarButtonHeight;
				fBirdsEyeCanvas.setBounds(x+scrollbarWidth, y, BIRDS_EYE_VIEW_WIDTH, height-(2*verticalScrollbarButtonHeight+horizontalScrollbarButtonHeight));
			}

		// doesn't work since TextEditors don't have their correct size yet.
		updateVScrollBar();
		refreshBirdsEyeView();
	}

	/*
	 * Track selection changes to update the current Diff.
	 */
	private void handleSelectionChanged(MergeSourceViewer tw) {
		Point p= tw.getSourceViewer().getSelectedRange();
		Diff d= findDiff(tw, p.x, p.x+p.y);
		updateStatus(d);
		setCurrentDiff(d, false);	// don't select or reveal
	}

	private static IRegion toRegion(Position position) {
		if (position != null)
			return new Region(position.getOffset(), position.getLength());
		return null;
	}

	//---- the differencing

	/**
	 * Perform a two level 2- or 3-way diff.
	 * The first level is based on line comparison, the second level on token comparison.
	 */
	private void doDiff() {
		IDocument lDoc= fLeft.getSourceViewer().getDocument();
		IDocument rDoc= fRight.getSourceViewer().getDocument();
		if (lDoc == null || rDoc == null)
			return;
		fAncestor.resetLineBackground();
		fLeft.resetLineBackground();
		fRight.resetLineBackground();
		saveDiff();
		fCurrentDiff= null;
		try {
			fMerger.doDiff();
		} catch (CoreException e) {
			CompareUIPlugin.log(e.getStatus());
			String title= Utilities.getString(getResourceBundle(), "tooComplexError.title"); //$NON-NLS-1$
			String msg= Utilities.getString(getResourceBundle(), "tooComplexError.message"); //$NON-NLS-1$
			MessageDialog.openError(fComposite.getShell(), title, msg);
		}

		invalidateTextPresentation();
	}

	private Diff findDiff(char type, int pos) {
		try {
			return fMerger.findDiff(type, pos);
		} catch (CoreException e) {
			CompareUIPlugin.log(e.getStatus());
			String title= Utilities.getString(getResourceBundle(), "tooComplexError.title"); //$NON-NLS-1$
			String msg= Utilities.getString(getResourceBundle(), "tooComplexError.message"); //$NON-NLS-1$
			MessageDialog.openError(fComposite.getShell(), title, msg);
			return null;
		}
	}

	private void resetPositions(IDocument doc) {
		if (doc == null)
			return;
		try {
			doc.removePositionCategory(DIFF_RANGE_CATEGORY);
		} catch (BadPositionCategoryException e) {
			// Ignore
		}
		doc.addPositionCategory(DIFF_RANGE_CATEGORY);
	}

	//---- update UI stuff

	private void updateControls() {
		if (getControl().isDisposed())
			return;

		boolean leftToRight= false;
		boolean rightToLeft= false;

		updateStatus(fCurrentDiff);
		updateResolveStatus();

		if (fCurrentDiff != null) {
			IMergeViewerContentProvider cp= getMergeContentProvider();
			if (cp != null) {
				if (!isPatchHunk()) {
					rightToLeft= cp.isLeftEditable(getInput());
					leftToRight= cp.isRightEditable(getInput());
				}
			}
		}

		if (fDirectionLabel != null) {
			if (fHighlightRanges && fCurrentDiff != null && isThreeWay() && !isIgnoreAncestor()) {
				fDirectionLabel.setImage(fCurrentDiff.getImage());
			} else {
				fDirectionLabel.setImage(null);
			}
		}

		if (fCopyDiffLeftToRightItem != null)
			fCopyDiffLeftToRightItem.getAction().setEnabled(leftToRight);
		if (fCopyDiffRightToLeftItem != null)
			fCopyDiffRightToLeftItem.getAction().setEnabled(rightToLeft);

		if (fNextDiff != null) {
			IAction a = fNextDiff.getAction();
			a.setEnabled(isNavigationButtonEnabled(true, false));
		}
		if (fPreviousDiff != null) {
			IAction a = fPreviousDiff.getAction();
			a.setEnabled(isNavigationButtonEnabled(false, false));
		}
		if (fNextChange != null) {
			IAction a = fNextChange.getAction();
			a.setEnabled(isNavigationButtonEnabled(true, true));
		}
		if (fPreviousChange != null) {
			IAction a = fPreviousChange.getAction();
			a.setEnabled(isNavigationButtonEnabled(false, true));
		}
	}

	private boolean isNavigationButtonEnabled(boolean down, boolean deep) {
		String value = fPreferenceStore
				.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		switch (value) {
		case ICompareUIConstants.PREF_VALUE_DO_NOTHING:
			return getNextVisibleDiff(down, deep) != null;
		case ICompareUIConstants.PREF_VALUE_LOOP:
			return isNavigationPossible();
		case ICompareUIConstants.PREF_VALUE_NEXT:
			return getNextVisibleDiff(down, deep) != null || hasNextElement(down);
		case ICompareUIConstants.PREF_VALUE_PROMPT:
			return isNavigationPossible() || hasNextElement(true);
		default:
			break;
		}
		Assert.isTrue(false);
		return false;
	}

	private void updateResolveStatus() {

		RGB rgb= null;

		if (showResolveUI()) {
			// we only show red or green if there is at least one incoming or conflicting change
			int unresolvedIncoming= 0;
			int unresolvedConflicting= 0;

			if (fMerger.hasChanges()) {
				for (Iterator<?> iterator = fMerger.changesIterator(); iterator	.hasNext();) {
					Diff d = (Diff) iterator.next();
						if (!d.isResolved()) {
							if (d.getKind() == RangeDifference.CONFLICT) {
								unresolvedConflicting++;
								break; // we can stop here because a conflict has the maximum priority
							}
							unresolvedIncoming++;
						}
				}
			}

			if (unresolvedConflicting > 0)
				rgb= conflictPalette.selected;
			else if (unresolvedIncoming > 0)
				rgb= incomingPalette.selected;
			else
				rgb= RESOLVED;
		}

		if (fHeaderPainter.setColor(rgb))
			fSummaryHeader.redraw();
	}

	private void updateStatus(Diff diff) {

		String diffDescription;

		if (diff == null) {
			diffDescription= CompareMessages.TextMergeViewer_diffDescription_noDiff_format;
		} else {

			if (diff.isToken())		// we don't show special info for token diffs
				diff= diff.getParent();

			String format= CompareMessages.TextMergeViewer_diffDescription_diff_format;
			diffDescription= MessageFormat.format(format,
				getDiffType(diff),						// 0: diff type
				getDiffNumber(diff),					// 1: diff number
				getDiffRange(fLeft, diff.getPosition(LEFT_CONTRIBUTOR)),		// 2: left start line
				getDiffRange(fRight, diff.getPosition(RIGHT_CONTRIBUTOR))	// 3: left end line
			);
		}

		String format= CompareMessages.TextMergeViewer_statusLine_format;
		String s= MessageFormat.format(format,
			getCursorPosition(fLeft),	// 0: left column
			getCursorPosition(fRight),	// 1: right column
			diffDescription				// 2: diff description
		);

		getCompareConfiguration().getContainer().setStatusMessage(s);
	}

	private String getDiffType(Diff diff) {
		String s= ""; 	//$NON-NLS-1$
		switch(diff.getKind()) {
		case RangeDifference.LEFT:
			s= getCompareConfiguration().isMirrored() ?
					CompareMessages.TextMergeViewer_direction_incoming :
					CompareMessages.TextMergeViewer_direction_outgoing;
			break;
		case RangeDifference.RIGHT:
			s= getCompareConfiguration().isMirrored() ?
					CompareMessages.TextMergeViewer_direction_outgoing :
					CompareMessages.TextMergeViewer_direction_incoming;
			break;
		case RangeDifference.CONFLICT:
			s= CompareMessages.TextMergeViewer_direction_conflicting;
			break;
		default:
			break;
		}
		String format= CompareMessages.TextMergeViewer_diffType_format;
		return MessageFormat.format(format, s, diff.changeType());
	}

	private String getDiffNumber(Diff diff) {
		// find the diff's number
		int diffNumber= 0;
		if (fMerger.hasChanges()) {
			for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
				Diff d = (Diff) iterator.next();
				diffNumber++;
				if (d == diff)
					break;
			}
		}
		return Integer.toString(diffNumber);
	}

	private String getDiffRange(MergeSourceViewer v, Position pos) {
		Point p= v.getLineRange(pos, new Point(0, 0));
		int startLine= p.x+1;
		int endLine= p.x+p.y;

		String format;
		if (endLine < startLine)
			format= CompareMessages.TextMergeViewer_beforeLine_format;
		else
			format= CompareMessages.TextMergeViewer_range_format;
		return MessageFormat.format(format, Integer.toString(startLine), Integer.toString(endLine));
	}

	/*
	 * Returns a description of the cursor position.
	 *
	 * @return a description of the cursor position
	 */
	private String getCursorPosition(MergeSourceViewer v) {
		if (v != null) {
			StyledText styledText= v.getSourceViewer().getTextWidget();

			IDocument document= v.getSourceViewer().getDocument();
			if (document != null) {
				int offset= v.getSourceViewer().getVisibleRegion().getOffset();
				int caret= offset + styledText.getCaretOffset();

				try {
					int line=document.getLineOfOffset(caret);

					int lineOffset= document.getLineOffset(line);
					int occurrences= 0;
					for (int i= lineOffset; i < caret; i++)
						if ('\t' == document.getChar(i))
							++ occurrences;

					int tabWidth= styledText.getTabs();
					int column= caret - lineOffset + (tabWidth -1) * occurrences;

					String format= CompareMessages.TextMergeViewer_cursorPosition_format;
					return MessageFormat.format(format,
						Integer.toString(line + 1), Integer.toString(column + 1) );
				} catch (BadLocationException x) {
					// silently ignored
				}
			}
		}
		return "";	//$NON-NLS-1$
	}

	@Override
	protected void updateHeader() {
		super.updateHeader();

		updateControls();
	}

	/*
	 * Creates the two items for copying a difference range from one side to the other
	 * and adds them to the given toolbar manager.
	 */
	@Override
	protected void createToolItems(ToolBarManager tbm) {
		fHandlerService= CompareHandlerService.createFor(getCompareConfiguration().getContainer(), fLeft.getSourceViewer().getControl().getShell());

		final String ignoreAncestorActionKey= "action.IgnoreAncestor.";	//$NON-NLS-1$
		Action ignoreAncestorAction= new Action() {
			@Override
			public void run() {
				// First make sure the ancestor is hidden
				if (!isIgnoreAncestor())
					getCompareConfiguration().setProperty(ICompareUIConstants.PROP_ANCESTOR_VISIBLE, Boolean.FALSE);
				// Then set the property to ignore the ancestor
				getCompareConfiguration().setProperty(ICompareUIConstants.PROP_IGNORE_ANCESTOR, Boolean.valueOf(!isIgnoreAncestor()));
				Utilities.initToggleAction(this, getResourceBundle(), ignoreAncestorActionKey, isIgnoreAncestor());
			}
		};
		ignoreAncestorAction.setChecked(isIgnoreAncestor());
		Utilities.initAction(ignoreAncestorAction, getResourceBundle(), ignoreAncestorActionKey);
		Utilities.initToggleAction(ignoreAncestorAction, getResourceBundle(), ignoreAncestorActionKey, isIgnoreAncestor());

		fIgnoreAncestorItem= new ActionContributionItem(ignoreAncestorAction);
		fIgnoreAncestorItem.setVisible(false);
		tbm.appendToGroup("modes", fIgnoreAncestorItem); //$NON-NLS-1$

		tbm.add(new Separator());

		Action a= new Action() {
			@Override
			public void run() {
				if (navigate(true, false, false)) {
					endOfDocumentReached(true);
				}
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.NextDiff."); //$NON-NLS-1$
		fNextDiff= new ActionContributionItem(a);
		tbm.appendToGroup("navigation", fNextDiff); //$NON-NLS-1$
		// Don't register this action since it is probably registered by the container

		a= new Action() {
			@Override
			public void run() {
				if (navigate(false, false, false)) {
					endOfDocumentReached(false);
				}
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.PrevDiff."); //$NON-NLS-1$
		fPreviousDiff= new ActionContributionItem(a);
		tbm.appendToGroup("navigation", fPreviousDiff); //$NON-NLS-1$
		// Don't register this action since it is probably registered by the container

		a= new Action() {
			@Override
			public void run() {
				if (navigate(true, false, true)) {
					endOfDocumentReached(true);
				}
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.NextChange."); //$NON-NLS-1$
		fNextChange= new ActionContributionItem(a);
		tbm.appendToGroup("navigation", fNextChange); //$NON-NLS-1$
		fHandlerService.registerAction(a, "org.eclipse.compare.selectNextChange");	//$NON-NLS-1$

		a= new Action() {
			@Override
			public void run() {
				if (navigate(false, false, true)) {
					endOfDocumentReached(false);
				}
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.PrevChange."); //$NON-NLS-1$
		fPreviousChange= new ActionContributionItem(a);
		tbm.appendToGroup("navigation", fPreviousChange); //$NON-NLS-1$
		fHandlerService.registerAction(a, "org.eclipse.compare.selectPreviousChange");	//$NON-NLS-1$

		a= new Action() {
			@Override
			public void run() {
				copyDiffLeftToRight();
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.CopyDiffLeftToRight."); //$NON-NLS-1$
		fCopyDiffLeftToRightItem= new ActionContributionItem(a);
		fCopyDiffLeftToRightItem.setVisible(isRightEditable());
		tbm.appendToGroup("merge", fCopyDiffLeftToRightItem); //$NON-NLS-1$
		fHandlerService.registerAction(a, "org.eclipse.compare.copyLeftToRight");	//$NON-NLS-1$

		a= new Action() {
			@Override
			public void run() {
				copyDiffRightToLeft();
			}
		};
		Utilities.initAction(a, getResourceBundle(), "action.CopyDiffRightToLeft."); //$NON-NLS-1$
		fCopyDiffRightToLeftItem= new ActionContributionItem(a);
		fCopyDiffRightToLeftItem.setVisible(isLeftEditable());
		tbm.appendToGroup("merge", fCopyDiffRightToLeftItem); //$NON-NLS-1$
		fHandlerService.registerAction(a, "org.eclipse.compare.copyRightToLeft");	//$NON-NLS-1$

		fIgnoreWhitespace= ChangePropertyAction.createIgnoreWhiteSpaceAction(getResourceBundle(), getCompareConfiguration());
		fIgnoreWhitespace.setActionDefinitionId(ICompareUIConstants.COMMAND_IGNORE_WHITESPACE);
		fLeft.addTextAction(fIgnoreWhitespace);
		fRight.addTextAction(fIgnoreWhitespace);
		fAncestor.addTextAction(fIgnoreWhitespace);
		fHandlerService.registerAction(fIgnoreWhitespace, fIgnoreWhitespace.getActionDefinitionId());

		boolean needsLeftPainter= !isEditorBacked(fLeft.getSourceViewer());
		boolean needsRightPainter= !isEditorBacked(fRight.getSourceViewer());
		boolean needsAncestorPainter= !isEditorBacked(fAncestor.getSourceViewer());
		showWhitespaceAction = new ShowWhitespaceAction(
				new MergeSourceViewer[] {fLeft, fRight, fAncestor},
				new boolean[] {needsLeftPainter, needsRightPainter, needsAncestorPainter });
		// showWhitespaceAction is registered as global action in connectGlobalActions
		fLeft.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, showWhitespaceAction);
		fRight.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, showWhitespaceAction);
		fAncestor.addAction(ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS, showWhitespaceAction);
		fHandlerService.registerAction(showWhitespaceAction, ITextEditorActionDefinitionIds.SHOW_WHITESPACE_CHARACTERS);

		toggleLineNumbersAction = new LineNumberRulerToggleAction(CompareMessages.TextMergeViewer_16,
				new MergeSourceViewer[] { fLeft, fRight, fAncestor },
				AbstractDecoratedTextEditorPreferenceConstants.EDITOR_LINE_NUMBER_RULER);
		fHandlerService.registerAction(toggleLineNumbersAction, ITextEditorActionDefinitionIds.LINENUMBER_TOGGLE);
	}

	private void configureCompareFilterActions(Object input, Object ancestor,
			Object left, Object right) {
		if (getCompareConfiguration() == null) {
			return;
		}

		CompareFilterDescriptor[] compareFilterDescriptors = CompareUIPlugin.getDefault().findCompareFilters(input);
		Object current = getCompareConfiguration()
				.getProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS);
		boolean currentFiltersMatch = false;
		if (current instanceof List && ((List<?>) current).size() == compareFilterDescriptors.length) {
			currentFiltersMatch = true;
			@SuppressWarnings("unchecked")
			List<ChangeCompareFilterPropertyAction> currentFilterActions = (List<ChangeCompareFilterPropertyAction>) current;
			for (CompareFilterDescriptor compareFilterDescriptor : compareFilterDescriptors) {
				boolean match = false;
				for (ChangeCompareFilterPropertyAction currentFilterAction : currentFilterActions) {
					if (compareFilterDescriptor.getFilterId().equals(currentFilterAction.getFilterId())) {
						match = true;
						break;
					}
				}
				if (!match) {
					currentFiltersMatch = false;
					break;
				}
			}
		}
		if (!currentFiltersMatch) {
			getCompareConfiguration().setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING,
					Boolean.TRUE);
			disposeCompareFilterActions(true);
			fCompareFilterActions.clear();
			for (CompareFilterDescriptor compareFilterDescriptor : compareFilterDescriptors) {
				ChangeCompareFilterPropertyAction compareFilterAction = new ChangeCompareFilterPropertyAction(compareFilterDescriptor, getCompareConfiguration());
				compareFilterAction.setInput(input, ancestor, left, right);
				fCompareFilterActions.add(compareFilterAction);
				fLeft.addTextAction(compareFilterAction);
				fRight.addTextAction(compareFilterAction);
				fAncestor.addTextAction(compareFilterAction);

				if (getCompareConfiguration().getContainer().getActionBars() != null) {
					getCompareConfiguration().getContainer().getActionBars().getToolBarManager()
							.appendToGroup(CompareEditorContributor.FILTER_SEPARATOR, compareFilterAction);
					if (compareFilterAction.getActionDefinitionId() != null)
						getCompareConfiguration()
								.getContainer()
								.getActionBars()
								.setGlobalActionHandler(compareFilterAction.getActionDefinitionId(),
										compareFilterAction);
				}
			}
			if (!fCompareFilterActions.isEmpty() && getCompareConfiguration().getContainer().getActionBars() != null) {
				getCompareConfiguration().getContainer().getActionBars().getToolBarManager().markDirty();
				getCompareConfiguration().getContainer().getActionBars().getToolBarManager().update(true);
				getCompareConfiguration().getContainer().getActionBars().updateActionBars();
			}
			getCompareConfiguration().setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS,
					fCompareFilterActions);
			getCompareConfiguration().setProperty(ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING, null);
		} else {
			for (ChangeCompareFilterPropertyAction action : fCompareFilterActions) {
				action.setInput(input, ancestor, left, right);
			}
		}
	}

	private void disposeCompareFilterActions(boolean updateActionBars) {
		for (ChangeCompareFilterPropertyAction compareFilterAction : fCompareFilterActions) {
			fLeft.removeTextAction(compareFilterAction);
			fRight.removeTextAction(compareFilterAction);
			fAncestor.removeTextAction(compareFilterAction);
			if (updateActionBars
					&& getCompareConfiguration().getContainer().getActionBars() != null) {
				getCompareConfiguration().getContainer().getActionBars()
						.getToolBarManager()
						.remove(compareFilterAction.getId());
				if (compareFilterAction.getActionDefinitionId() != null)
					getCompareConfiguration()
							.getContainer()
							.getActionBars()
							.setGlobalActionHandler(
									compareFilterAction.getActionDefinitionId(),
									null);
			}
			compareFilterAction.dispose();
		}
		if (updateActionBars
				&& !fCompareFilterActions.isEmpty()
				&& getCompareConfiguration().getContainer().getActionBars() != null) {
			getCompareConfiguration().getContainer().getActionBars()
					.getToolBarManager().markDirty();
			getCompareConfiguration().getContainer().getActionBars()
					.getToolBarManager().update(true);
		}
		fCompareFilterActions.clear();
		getCompareConfiguration().setProperty(
				ChangeCompareFilterPropertyAction.COMPARE_FILTERS, null);
		getCompareConfiguration().setProperty(
				ChangeCompareFilterPropertyAction.COMPARE_FILTER_ACTIONS, null);
	}

	@Override
	protected void handlePropertyChangeEvent(PropertyChangeEvent event) {
		String key= event.getProperty();

		if (key.equals(CompareConfiguration.IGNORE_WHITESPACE)
				|| key.equals(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS)
				|| (key.equals(ChangeCompareFilterPropertyAction.COMPARE_FILTERS) && getCompareConfiguration()
						.getProperty(
								ChangeCompareFilterPropertyAction.COMPARE_FILTERS_INITIALIZING) == null)) {

			fShowPseudoConflicts= fPreferenceStore.getBoolean(ComparePreferencePage.SHOW_PSEUDO_CONFLICTS);

			update(true);
			// selectFirstDiff(true);
			if (fFocusPart != null)
				handleSelectionChanged(fFocusPart);

//		} else if (key.equals(ComparePreferencePage.USE_SPLINES)) {
//			fUseSplines= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SPLINES);
//			invalidateLines();

		} else if (key.equals(ComparePreferencePage.USE_SINGLE_LINE)) {
			fUseSingleLine= fPreferenceStore.getBoolean(ComparePreferencePage.USE_SINGLE_LINE);
//			fUseResolveUI= fUseSingleLine;
			fBasicCenterCurve= null;
			updateControls();
			invalidateLines();

		} else if (key.equals(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES)) {
			fHighlightTokenChanges= fPreferenceStore.getBoolean(ComparePreferencePage.HIGHLIGHT_TOKEN_CHANGES);
			updateControls();
			updatePresentation();

//		} else if (key.equals(ComparePreferencePage.USE_RESOLVE_UI)) {
//			fUseResolveUI= fPreferenceStore.getBoolean(ComparePreferencePage.USE_RESOLVE_UI);
//			updateResolveStatus();
//			invalidateLines();

		} else if (key.equals(fSymbolicFontName)) {
			updateFont();
			invalidateLines();

		} else if (key.equals(INCOMING_COLOR) || key.equals(OUTGOING_COLOR) || key.equals(CONFLICTING_COLOR)
				|| key.equals(RESOLVED_COLOR) || key.equals(ADDITION_COLOR) || key.equals(DELETION_COLOR)
				|| key.equals(EDITION_COLOR)) {
			updateColors(null);
			invalidateLines();
			invalidateTextPresentation();

		} else if (key.equals(ComparePreferencePage.SYNCHRONIZE_SCROLLING)) {
			boolean b= fPreferenceStore.getBoolean(ComparePreferencePage.SYNCHRONIZE_SCROLLING);
			setSyncScrolling(b);

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND)) {
			if (!fIsUsingSystemBackground) {
				setBackgroundColor(createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND));
			}

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT)) {
			fIsUsingSystemBackground= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND_SYSTEM_DEFAULT);
			if (fIsUsingSystemBackground) {
				setBackgroundColor(null);
			} else {
				setBackgroundColor(createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_BACKGROUND));
			}
		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND)) {
			if (!fIsUsingSystemForeground) {
				setForegroundColor(createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND));
			}

		} else if (key.equals(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT)) {
			fIsUsingSystemForeground= fPreferenceStore.getBoolean(AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND_SYSTEM_DEFAULT);
			if (fIsUsingSystemForeground) {
				setForegroundColor(null);
			} else {
				setForegroundColor(createColor(fPreferenceStore, AbstractTextEditor.PREFERENCE_COLOR_FOREGROUND));
			}
		} else if (key.equals(ICompareUIConstants.PREF_NAVIGATION_END_ACTION)) {
			updateControls();
		} else if (key.equals(CompareContentViewerSwitchingPane.DISABLE_CAPPING_TEMPORARILY)) {
			if (Boolean.TRUE.equals(event.getNewValue())) {
				getCompareConfiguration().setProperty(CompareContentViewerSwitchingPane.DISABLE_CAPPING_TEMPORARILY, null);
				handleCompareInputChange();
			}
		} else {
			super.handlePropertyChangeEvent(event);

			if (key.equals(ICompareUIConstants.PROP_IGNORE_ANCESTOR)) {
				update(true);
				selectFirstDiff(true);
			}
		}
	}

	private void selectFirstDiff(boolean first) {

		if (fLeft == null || fRight == null) {
			return;
		}
		if (fLeft.getSourceViewer().getDocument() == null || fRight.getSourceViewer().getDocument() == null) {
			return;
		}

		Diff firstDiff= null;
		if (first)
			firstDiff= findNext(fRight, -1, -1, false);
		else
			firstDiff= findPrev(fRight, 9999999, 9999999, false);
		setCurrentDiff(firstDiff, true);
	}



	private void setSyncScrolling(boolean newMode) {
		if (fSynchronizedScrolling != newMode) {
			fSynchronizedScrolling= newMode;

			scrollVertical(0, 0, 0, null);

			// throw away central control (Sash or Canvas)
			Control center= getCenterControl();
			if (center != null && !center.isDisposed())
				center.dispose();

			fLeft.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!fSynchronizedScrolling);
			fRight.getSourceViewer().getTextWidget().getVerticalBar().setVisible(!fSynchronizedScrolling);

			fComposite.layout(true);
		}
	}

	@Override
	protected void updateToolItems() {
		if (fCopyDiffLeftToRightItem != null) {
			fCopyDiffLeftToRightItem.setVisible(isRightEditable());
		}
		if (fCopyDiffRightToLeftItem != null) {
			fCopyDiffRightToLeftItem.setVisible(isLeftEditable());
		}

		//only update toolbar items if diffs need to be calculated (which
		//dictates whether a toolbar gets added at all)
		if (!isPatchHunk()){
			if (fIgnoreAncestorItem != null)
				fIgnoreAncestorItem.setVisible(isThreeWay());

			if (fCopyDiffLeftToRightItem != null) {
				IAction a= fCopyDiffLeftToRightItem.getAction();
				if (a != null)
					a.setEnabled(a.isEnabled() && !fHasErrors);
			}
			if (fCopyDiffRightToLeftItem != null) {
				IAction a= fCopyDiffRightToLeftItem.getAction();
				if (a != null)
					a.setEnabled(a.isEnabled() && !fHasErrors);
			}
		}
		super.updateToolItems();
	}

	//---- painting lines

	private void updateLines(IDocument d) {
		boolean left= false;
		boolean right= false;

		// FIXME: this optimization is incorrect because
		// it doesn't take replace operations into account where
		// the old and new line count does not differ
		if (d == fLeft.getSourceViewer().getDocument()) {
			int l= fLeft.getLineCount();
			left= fLeftLineCount != l;
			fLeftLineCount= l;
		} else if (d == fRight.getSourceViewer().getDocument()) {
			int l= fRight.getLineCount();
			right= fRightLineCount != l;
			fRightLineCount= l;
		}

		if (left || right) {
			if (left) {
				if (fLeftCanvas != null)
					fLeftCanvas.redraw();
			} else {
				if (fRightCanvas != null)
					fRightCanvas.redraw();
			}
			Control center= getCenterControl();
			if (center != null)
				center.redraw();

			updateVScrollBar();
			refreshBirdsEyeView();
		}
	}

	private void invalidateLines() {
		if (isThreeWay() && isAncestorVisible()) {
			if (Utilities.okToUse(fAncestorCanvas))
				fAncestorCanvas.redraw();
			if (fAncestor != null && fAncestor.isControlOkToUse())
				fAncestor.getSourceViewer().getTextWidget().redraw();
		}

		if (Utilities.okToUse(fLeftCanvas))
			fLeftCanvas.redraw();

		if (fLeft != null && fLeft.isControlOkToUse())
			fLeft.getSourceViewer().getTextWidget().redraw();

		if (Utilities.okToUse(getCenterControl()))
			getCenterControl().redraw();

		if (fRight != null && fRight.isControlOkToUse())
			fRight.getSourceViewer().getTextWidget().redraw();

		if (Utilities.okToUse(fRightCanvas))
			fRightCanvas.redraw();
	}

	private boolean showResolveUI() {
		if (!isThreeWay() || isIgnoreAncestor())
			return false;
		return isAnySideEditable();
	}

	private boolean isAnySideEditable() {
		// we only enable the new resolve UI if exactly one side is editable
		return isLeftEditable() || isRightEditable();
	}

	private void paintCenter(Canvas canvas, GC g) {
		if (fLeft == null || fRight == null) {
			// The paint event for the center control can occur before the left / right controls are created.
			// simply ignore it in this case.
			return;
		}

		Display display= canvas.getDisplay();

		checkForColorUpdate(display);

		if (! fSynchronizedScrolling)
			return;

		int lineHeightLeft= fLeft.getSourceViewer().getTextWidget().getLineHeight();
		int lineHeightRight= fRight.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight= fRight.getViewportHeight();

		Point size= canvas.getSize();
		int x= 0;
		int w= size.x;

		g.setBackground(canvas.getBackground());
		g.fillRectangle(x+1, 0, w-2, size.y);

		// draw thin line between center ruler and both texts
		g.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		g.fillRectangle(0, 0, 1, size.y);
		g.fillRectangle(w - 1, 0, 1, size.y);

		if (! fHighlightRanges)
			return;

		if (fMerger.hasChanges()) {
			int lshift= fLeft.getVerticalScrollOffset();
			int rshift= fRight.getVerticalScrollOffset();

			Point region= new Point(0, 0);

			boolean allOutgoing = allOutgoing();
			for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = (Diff) iterator.next();
				if (diff.isDeleted())
					continue;

				if (fShowCurrentOnly2 && !isCurrentDiff(diff))
					continue;

				fLeft.getLineRange(diff.getPosition(LEFT_CONTRIBUTOR), region);
				int ly= (region.x * lineHeightLeft) + lshift;
				int lh= region.y * lineHeightLeft;

				fRight.getLineRange(diff.getPosition(RIGHT_CONTRIBUTOR), region);
				int ry= (region.x * lineHeightRight) + rshift;
				int rh= region.y * lineHeightRight;

				if (Math.max(ly+lh, ry+rh) < 0)
					continue;
				if (Math.min(ly, ry) >= visibleHeight)
					break;

				fPts[0]= x;	fPts[1]= ly;	fPts[2]= w;	fPts[3]= ry;
				fPts[6]= x;	fPts[7]= ly+lh;	fPts[4]= w;	fPts[5]= ry+rh;

				Color fillColor = getColor(display, getFillColor(diff, allOutgoing));
				Color strokeColor = getColor(display, getStrokeColor(diff, allOutgoing));

				if (fUseSingleLine) {
					int w2= 3;

					g.setBackground(fillColor);
					g.fillRectangle(0, ly, w2, lh);		// left
					g.fillRectangle(w-w2, ry, w2, rh);	// right

					g.setLineWidth(0 /* LW */);
					g.setForeground(strokeColor);
					g.drawRectangle(0-1, ly, w2, lh);	// left
					g.drawRectangle(w-w2, ry, w2, rh);	// right

					if (fUseSplines) {
						int[] points= getCenterCurvePoints(w2, ly+lh/2, w-w2, ry+rh/2);
						for (int i= 1; i < points.length; i++)
							g.drawLine(w2+i-1, points[i-1], w2+i, points[i]);
					} else {
						g.drawLine(w2, ly+lh/2, w-w2, ry+rh/2);
					}
				} else {
					// two lines
					if (fUseSplines) {
						g.setBackground(fillColor);

						g.setLineWidth(0 /* LW */);
						g.setForeground(strokeColor);

						int[] topPoints= getCenterCurvePoints(fPts[0], fPts[1], fPts[2], fPts[3]);
						int[] bottomPoints= getCenterCurvePoints(fPts[6], fPts[7], fPts[4], fPts[5]);
						g.setForeground(fillColor);
						g.drawLine(0, bottomPoints[0], 0, topPoints[0]);
						for (int i= 1; i < bottomPoints.length; i++) {
							g.setForeground(fillColor);
							g.drawLine(i, bottomPoints[i], i, topPoints[i]);
							g.setForeground(strokeColor);
							g.drawLine(i-1, topPoints[i-1], i, topPoints[i]);
							g.drawLine(i-1, bottomPoints[i-1], i, bottomPoints[i]);
						}
					} else {
						g.setBackground(fillColor);
						g.fillPolygon(fPts);

						g.setLineWidth(0 /* LW */);
						g.setForeground(strokeColor);
						g.drawLine(fPts[0], fPts[1], fPts[2], fPts[3]);
						g.drawLine(fPts[6], fPts[7], fPts[4], fPts[5]);
					}
				}

				if (fUseSingleLine && isAnySideEditable()) {
					// draw resolve state
					int cx= (w-RESOLVE_SIZE)/2;
					int cy= ((ly+lh/2) + (ry+rh/2) - RESOLVE_SIZE)/2;

					g.setBackground(fillColor);
					g.fillRectangle(cx, cy, RESOLVE_SIZE, RESOLVE_SIZE);

					g.setForeground(strokeColor);
					g.drawRectangle(cx, cy, RESOLVE_SIZE, RESOLVE_SIZE);
				}
			}
		}
	}

	private int[] getCenterCurvePoints(int startx, int starty, int endx, int endy) {
		if (fBasicCenterCurve == null)
			buildBaseCenterCurve(endx-startx);
		double height= endy - starty;
		height= height/2;
		int width= endx-startx;
		int[] points= new int[width];
		for (int i= 0; i < width; i++) {
			points[i]= (int) (-height * fBasicCenterCurve[i] + height + starty);
		}
		return points;
	}

	private void buildBaseCenterCurve(int w) {
		double width= w;
		fBasicCenterCurve= new double[getCenterWidth()];
		for (int i= 0; i < getCenterWidth(); i++) {
			double r= i / width;
			fBasicCenterCurve[i]= Math.cos(Math.PI * r);
		}
	}

	private void paintSides(GC g, MergeSourceViewer tp, Canvas canvas, boolean right) {

		Display display= canvas.getDisplay();

		int lineHeight= tp.getSourceViewer().getTextWidget().getLineHeight();
		int visibleHeight= tp.getViewportHeight();

		Point size= canvas.getSize();
		int x= 0;
		int w= fMarginWidth;
		int w2= w/2;

		g.setBackground(canvas.getBackground());
		g.fillRectangle(x, 0, w, size.y);

		// draw thin line between ruler and text
		g.setBackground(display.getSystemColor(SWT.COLOR_WIDGET_NORMAL_SHADOW));
		if (right)
			g.fillRectangle(0, 0, 1, size.y);
		else
			g.fillRectangle(size.x - 1, 0, 1, size.y);

		if (! fHighlightRanges)
			return;

		if (fMerger.hasChanges()) {
			boolean allOutgoing = allOutgoing();
			int shift= tp.getVerticalScrollOffset() + (2-LW);

			Point region= new Point(0, 0);
			char leg = getLeg(tp);
			for (Iterator<Diff> iterator = fMerger.changesIterator(); iterator.hasNext();) {
				Diff diff = iterator.next();
				if (diff.isDeleted())
					continue;

				if (fShowCurrentOnly2 && !isCurrentDiff(diff))
					continue;

				tp.getLineRange(diff.getPosition(leg), region);
				int y= (region.x * lineHeight) + shift;
				int h= region.y * lineHeight;

				if (y+h < 0)
					continue;
				if (y >= visibleHeight)
					break;

				g.setBackground(getColor(display, getFillColor(diff, allOutgoing)));
				if (right)
					g.fillRectangle(x, y, w2, h);
				else
					g.fillRectangle(x+w2, y, w2, h);

				g.setLineWidth(0 /* LW */);
				g.setForeground(getColor(display, getStrokeColor(diff, allOutgoing)));
				if (right)
					g.drawRectangle(x-1, y-1, w2, h);
				else
					g.drawRectangle(x+w2, y-1, w2, h);
			}
		}
	}

	private boolean allOutgoing() {
		boolean allOutgoing = !isThreeWay();
		for (Iterator<Diff> iterator = fMerger.changesIterator(); allOutgoing && iterator.hasNext();) {
			Diff diff = iterator.next();
			allOutgoing &= !diff.isDeleted()
					&& (diff.getKind() == RangeDifference.NOCHANGE || diff.getKind() == RangeDifference.CHANGE);
		}
		return allOutgoing;
	}

	private void paint(PaintEvent event, MergeSourceViewer tp) {

		if (! fHighlightRanges)
			return;
		if (!fMerger.hasChanges())
			return;

		Control canvas= (Control) event.widget;
		GC g= event.gc;

		Display display= canvas.getDisplay();

		int lineHeight= tp.getSourceViewer().getTextWidget().getLineHeight();
		int w= canvas.getSize().x;
		int shift= tp.getVerticalScrollOffset() + (2-LW);
		int maxh= event.y+event.height; 	// visibleHeight

		shift += fTopInset;

		Point range= new Point(0, 0);

		char leg = getLeg(tp);
		boolean allOutgoing = allOutgoing();
		for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
			Diff diff = (Diff) iterator.next();
			if (diff.isDeleted())
				continue;

			if (fShowCurrentOnly && !isCurrentDiff(diff))
				continue;

			tp.getLineRange(diff.getPosition(leg), range);
			int y= (range.x * lineHeight) + shift;
			int h= range.y * lineHeight;

			if (y+h < event.y)
				continue;
			if (y > maxh)
				break;

			g.setBackground(getColor(display, getStrokeColor(diff, allOutgoing)));
			g.fillRectangle(0, y-1, w, LW);
			g.fillRectangle(0, y+h-1, w, LW);
		}
	}

	private RGB getFillColor(Diff diff, boolean showAdditionRemoval) {
		boolean selected= fCurrentDiff != null && fCurrentDiff.getParent() == diff;
		if (selected) {
			return getBackground(null);
		}
		ColorPalette palette = getColorPalette(diff, showAdditionRemoval);
		if (palette == null) {
			return null;
		}
		return palette.fill;
	}

	private ColorPalette getColorPalette(Diff diff, boolean showAdditionRemoval) {
		if (isThreeWay() && !isIgnoreAncestor()) {
			switch (diff.getKind()) {
			case RangeDifference.RIGHT:
				if (!getCompareConfiguration().isMirrored())
					return incomingPalette;
				return outgoingPalette;
			case RangeDifference.ANCESTOR:
			case RangeDifference.CONFLICT:
				return conflictPalette;
			case RangeDifference.LEFT:
				if (!getCompareConfiguration().isMirrored())
					return outgoingPalette;
				return incomingPalette;
			default:
				return null;
			}
		}
		if (showAdditionRemoval) {
			if (diff.getPosition(LEFT_CONTRIBUTOR).getLength() == 0) {
				if (getCompareConfiguration().isMirrored()) {
					return additionPalette;
				}
				return deletionPalette;
			}
			if (diff.getPosition(RIGHT_CONTRIBUTOR).getLength() == 0) {
				if (getCompareConfiguration().isMirrored()) {
					return deletionPalette;
				}
				return additionPalette;
			}
			return editionPalette;
		}
		return outgoingPalette;
	}

	private RGB getStrokeColor(Diff diff, boolean showAdditionRemoval) {
		ColorPalette palette = getColorPalette(diff, showAdditionRemoval);
		if (palette == null) {
			return null;
		}
		boolean selected = fCurrentDiff != null && fCurrentDiff.getParent() == diff;
		return selected ? palette.selected : palette.normal;
	}

	private Color getColor(Display display, RGB rgb) {
		if (rgb == null)
			return null;
		if (fColors == null)
			fColors= new HashMap<>(20);
		Color c= fColors.get(rgb);
		if (c == null) {
			c= new Color(display, rgb);
			fColors.put(rgb, c);
		}
		return c;
	}

	//---- Navigating and resolving Diffs

	private Diff getNextVisibleDiff(boolean down, boolean deep) {
		Diff diff= null;
		MergeSourceViewer part= getNavigationPart();
		if (part == null)
			return null;
		Point s = part.getSourceViewer().getSelectedRange();
		char leg = getLeg(part);
		for (;;) {
			diff = null;
			diff = internalGetNextDiff(down, deep, part, s);
			if (diff != null && diff.getKind() == RangeDifference.ANCESTOR
					&& !isAncestorVisible()) {
				Position position = diff.getPosition(leg);
				s = new Point(position.getOffset(), position.getLength());
				diff= null;
				continue;
			}
			break;
		}
		return diff;
	}

	private Diff internalGetNextDiff(boolean down, boolean deep, MergeSourceViewer part, Point s) {
		if (fMerger.hasChanges()) {
			if (down)
				return findNext(part, s.x, s.x+s.y, deep);
			return findPrev(part, s.x, s.x+s.y, deep);
		}
		return null;
	}

	private MergeSourceViewer getNavigationPart() {
		MergeSourceViewer part= fFocusPart;
		if (part == null)
			part= fRight;
		return part;
	}

	private Diff getWrappedDiff(Diff diff, boolean down) {
		return fMerger.getWrappedDiff(diff, down);
	}

	/*
	 * Returns true if end (or beginning) of document reached.
	 */
	private boolean navigate(boolean down, boolean wrap, boolean deep) {
		Diff diff= null;
		boolean wrapped = false;
		for (;;) {
			diff = getNextVisibleDiff(down, deep);
			if (diff == null && wrap) {
				if (wrapped)
					// We've already wrapped once so break out
					break;
				wrapped = true;
				diff = getWrappedDiff(diff, down);
			}
			if (diff != null)
				setCurrentDiff(diff, true, deep);
			if (diff != null && diff.getKind() == RangeDifference.ANCESTOR
					&& !isAncestorVisible())
				continue;
			break;
		}
		return diff == null;
	}

	private void endOfDocumentReached(boolean down) {
		Control c= getControl();
		if (Utilities.okToUse(c)) {
			handleEndOfDocumentReached(c.getShell(), down);
		}
	}

	private void handleEndOfDocumentReached(Shell shell, boolean next) {
		IPreferenceStore store = CompareUIPlugin.getDefault().getPreferenceStore();
		String value = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
		if (!value.equals(ICompareUIConstants.PREF_VALUE_PROMPT)) {
			performEndOfDocumentAction(shell, store, ICompareUIConstants.PREF_NAVIGATION_END_ACTION, next);
		} else {
			shell.getDisplay().beep();
			String loopMessage;
			String nextMessage;
			String message;
			String title;
			if (next) {
				title = CompareMessages.TextMergeViewer_0;
				message = CompareMessages.TextMergeViewer_1;
				loopMessage = CompareMessages.TextMergeViewer_2;
				nextMessage = CompareMessages.TextMergeViewer_3;
			} else {
				title = CompareMessages.TextMergeViewer_4;
				message = CompareMessages.TextMergeViewer_5;
				loopMessage = CompareMessages.TextMergeViewer_6;
				nextMessage = CompareMessages.TextMergeViewer_7;
			}
			String[] localLoopOption = new String[] { loopMessage, ICompareUIConstants.PREF_VALUE_LOOP };
			String[] nextElementOption = new String[] { nextMessage, ICompareUIConstants.PREF_VALUE_NEXT};
			String[] doNothingOption = new String[] { CompareMessages.TextMergeViewer_17, ICompareUIConstants.PREF_VALUE_DO_NOTHING};
			NavigationEndDialog dialog = new NavigationEndDialog(shell,
					title,
					null,
					message,
					new String[][] {
					localLoopOption,
					nextElementOption,
					doNothingOption
			});
			int result = dialog.open();
			if (result == Window.OK) {
				performEndOfDocumentAction(shell, store, ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL, next);
				if (dialog.getToggleState()) {
					String oldValue = store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION);
					store.putValue(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL));
					store.firePropertyChangeEvent(ICompareUIConstants.PREF_NAVIGATION_END_ACTION, oldValue, store.getString(ICompareUIConstants.PREF_NAVIGATION_END_ACTION_LOCAL));
				}
			}
		}
	}

	private void performEndOfDocumentAction(Shell shell, IPreferenceStore store, String key, boolean next) {
		String value = store.getString(key);
		if (value.equals(ICompareUIConstants.PREF_VALUE_DO_NOTHING)) {
			return;
		}
		if (value.equals(ICompareUIConstants.PREF_VALUE_NEXT)) {
			ICompareNavigator navigator = getCompareConfiguration()
					.getContainer().getNavigator();
			if (hasNextElement(next)) {
				navigator.selectChange(next);
			}
		} else {
			selectFirstDiff(next);
		}
	}

	private boolean hasNextElement(boolean down) {
		ICompareNavigator navigator = getCompareConfiguration().getContainer().getNavigator();
		if (navigator instanceof CompareNavigator) {
			CompareNavigator n = (CompareNavigator) navigator;
			return n.hasChange(down);
		}
		return false;
	}

	/*
	 * Find the Diff that overlaps with the given TextPart's text range.
	 * If the range doesn't overlap with any range <code>null</code>
	 * is returned.
	 */
	private Diff findDiff(MergeSourceViewer tp, int rangeStart, int rangeEnd) {
		char contributor = getLeg(tp);
		return fMerger.findDiff(contributor, rangeStart, rangeEnd);
	}

	private Diff findNext(MergeSourceViewer tp, int start, int end, boolean deep) {
		return fMerger.findNext(getLeg(tp), start, end, deep);
	}

	private Diff findPrev(MergeSourceViewer tp, int start, int end, boolean deep) {
		return fMerger.findPrev(getLeg(tp), start, end, deep);
	}

	/*
	 * Set the currently active Diff and update the toolbars controls and lines.
	 * If <code>revealAndSelect</code> is <code>true</code> the Diff is revealed and
	 * selected in both TextParts.
	 */
	private void setCurrentDiff(Diff d, boolean revealAndSelect) {
		setCurrentDiff(d, revealAndSelect, false);
	}

	/*
	 * Set the currently active Diff and update the toolbars controls and lines.
	 * If <code>revealAndSelect</code> is <code>true</code> the Diff is revealed and
	 * selected in both TextParts.
	 */
	private void setCurrentDiff(Diff d, boolean revealAndSelect, boolean deep) {

//		if (d == fCurrentDiff)
//			return;
		boolean diffChanged = fCurrentDiff != d;

		if (fLeftToRightButton != null && !fLeftToRightButton.isDisposed())
			fLeftToRightButton.setVisible(false);
		if (fRightToLeftButton != null && !fRightToLeftButton.isDisposed())
			fRightToLeftButton.setVisible(false);

		if (d != null && revealAndSelect) {

			// before we set fCurrentDiff we change the selection
			// so that the paint code uses the old background colors
			// otherwise selection isn't drawn correctly
			if (d.isToken() || !fHighlightTokenChanges || deep || !d.hasChildren()) {
				if (isThreeWay() && !isIgnoreAncestor())
					fAncestor.setSelection(d.getPosition(ANCESTOR_CONTRIBUTOR));
				fLeft.setSelection(d.getPosition(LEFT_CONTRIBUTOR));
				fRight.setSelection(d.getPosition(RIGHT_CONTRIBUTOR));
			} else {
				if (isThreeWay() && !isIgnoreAncestor())
					fAncestor.setSelection(new Position(d.getPosition(ANCESTOR_CONTRIBUTOR).offset, 0));
				fLeft.setSelection(new Position(d.getPosition(LEFT_CONTRIBUTOR).offset, 0));
				fRight.setSelection(new Position(d.getPosition(RIGHT_CONTRIBUTOR).offset, 0));
			}

			// now switch diffs
			saveDiff();
			fCurrentDiff= d;
			revealDiff(d, d.isToken());
		} else {
			saveDiff();
			fCurrentDiff= d;
		}

		updateControls();
		if (diffChanged)
			invalidateLines();
		refreshBirdsEyeView();
	}

	/*
	 * Smart determines whether
	 */
	private void revealDiff(Diff d, boolean smart) {

		boolean ancestorIsVisible= false;
		boolean leftIsVisible= false;
		boolean rightIsVisible= false;

		if (smart) {
			Point region= new Point(0, 0);
			// find the starting line of the diff in all text widgets
			int ls= fLeft.getLineRange(d.getPosition(LEFT_CONTRIBUTOR), region).x;
			int rs= fRight.getLineRange(d.getPosition(RIGHT_CONTRIBUTOR), region).x;

			if (isThreeWay() && !isIgnoreAncestor()) {
				int as= fAncestor.getLineRange(d.getPosition(ANCESTOR_CONTRIBUTOR), region).x;
				if (as >= fAncestor.getSourceViewer().getTopIndex() && as <= fAncestor.getSourceViewer().getBottomIndex())
					ancestorIsVisible= true;
			}

			if (ls >= fLeft.getSourceViewer().getTopIndex() && ls <= fLeft.getSourceViewer().getBottomIndex())
				leftIsVisible= true;

			if (rs >= fRight.getSourceViewer().getTopIndex() && rs <= fRight.getSourceViewer().getBottomIndex())
				rightIsVisible= true;
		}

		// vertical scrolling
		if (!leftIsVisible || !rightIsVisible) {
			int avpos= 0, lvpos= 0, rvpos= 0;

			MergeSourceViewer allButThis= null;
			if (leftIsVisible) {
				avpos= lvpos= rvpos= realToVirtualPosition(LEFT_CONTRIBUTOR, fLeft.getSourceViewer().getTopIndex());
				allButThis= fLeft;
			} else if (rightIsVisible) {
				avpos= lvpos= rvpos= realToVirtualPosition(RIGHT_CONTRIBUTOR, fRight.getSourceViewer().getTopIndex());
				allButThis= fRight;
			} else if (ancestorIsVisible) {
				avpos= lvpos= rvpos= realToVirtualPosition(ANCESTOR_CONTRIBUTOR, fAncestor.getSourceViewer().getTopIndex());
				allButThis= fAncestor;
			} else {
				int vpos= 0;
				for (Iterator<?> iterator = fMerger.rangesIterator(); iterator.hasNext();) {
					Diff diff = (Diff) iterator.next();
					if (diff == d)
						break;
					if (fSynchronizedScrolling) {
						vpos+= diff.getMaxDiffHeight();
					} else {
						avpos+= diff.getAncestorHeight();
						lvpos+= diff.getLeftHeight();
						rvpos+= diff.getRightHeight();
					}
				}
				if (fSynchronizedScrolling)
					avpos= lvpos= rvpos= vpos;
				int delta= fRight.getViewportLines()/4;
				avpos-= delta;
				if (avpos < 0)
					avpos= 0;
				lvpos-= delta;
				if (lvpos < 0)
					lvpos= 0;
				rvpos-= delta;
				if (rvpos < 0)
					rvpos= 0;
			}

			scrollVertical(avpos, lvpos, rvpos, allButThis);

			if (fVScrollBar != null)
				fVScrollBar.setSelection(avpos);
		}

		// horizontal scrolling
		if (d.isToken()) {
			// we only scroll horizontally for token diffs
			reveal(fAncestor, d.getPosition(ANCESTOR_CONTRIBUTOR));
			reveal(fLeft, d.getPosition(LEFT_CONTRIBUTOR));
			reveal(fRight, d.getPosition(RIGHT_CONTRIBUTOR));
		} else {
			// in all other cases we reset the horizontal offset
			hscroll(fAncestor);
			hscroll(fLeft);
			hscroll(fRight);
		}
	}

	private static void reveal(MergeSourceViewer v, Position p) {
		if (v != null && p != null) {
			StyledText st= v.getSourceViewer().getTextWidget();
			if (st != null) {
				Rectangle r= st.getClientArea();
				if (!r.isEmpty())	// workaround for #7320: Next diff scrolls when going into current diff
					v.getSourceViewer().revealRange(p.offset, p.length);
			}
		}
	}

	private static void hscroll(MergeSourceViewer v) {
		if (v != null) {
			StyledText st= v.getSourceViewer().getTextWidget();
			if (st != null)
				st.setHorizontalIndex(0);
		}
	}

	//--------------------------------------------------------------------------------

	void copyAllUnresolved(boolean leftToRight) {
		if (fMerger.hasChanges() && isThreeWay() && !isIgnoreAncestor()) {
			IRewriteTarget target= leftToRight ? fRight.getSourceViewer().getRewriteTarget() : fLeft.getSourceViewer().getRewriteTarget();
			boolean compoundChangeStarted= false;
			try {
				for (Iterator<?> iterator = fMerger.changesIterator(); iterator.hasNext();) {
					Diff diff = (Diff) iterator.next();
					switch (diff.getKind()) {
					case RangeDifference.LEFT:
						if (leftToRight) {
							if (!compoundChangeStarted) {
								target.beginCompoundChange();
								compoundChangeStarted= true;
							}
							copy(diff, leftToRight);
						}
						break;
					case RangeDifference.RIGHT:
						if (!leftToRight) {
							if (!compoundChangeStarted) {
								target.beginCompoundChange();
								compoundChangeStarted= true;
							}
							copy(diff, leftToRight);
						}
						break;
					default:
						continue;
					}
				}
			} finally {
				if (compoundChangeStarted) {
					target.endCompoundChange();
				}
			}
		}
	}

	/*
	 * Copy whole document from one side to the other.
	 */
	@Override
	protected void copy(boolean leftToRight) {
		if (!validateChange(!leftToRight))
			return;
		if (showResolveUI()) {
			copyAllUnresolved(leftToRight);
			invalidateLines();
			return;
		}
		copyOperationInProgress = true;
		if (leftToRight) {
			if (fLeft.getEnabled()) {
				// copy text
				String text= fLeft.getSourceViewer().getTextWidget().getText();
				fRight.getSourceViewer().getTextWidget().setText(text);
				fRight.setEnabled(true);
			} else {
				// delete
				fRight.getSourceViewer().getTextWidget().setText(""); //$NON-NLS-1$
				fRight.setEnabled(false);
			}
			fRightLineCount= fRight.getLineCount();
			setRightDirty(true);
		} else {
			if (fRight.getEnabled()) {
				// copy text
				String text= fRight.getSourceViewer().getTextWidget().getText();
				fLeft.getSourceViewer().getTextWidget().setText(text);
				fLeft.setEnabled(true);
			} else {
				// delete
				fLeft.getSourceViewer().getTextWidget().setText(""); //$NON-NLS-1$
				fLeft.setEnabled(false);
			}
			fLeftLineCount= fLeft.getLineCount();
			setLeftDirty(true);
		}
		copyOperationInProgress = false;
		update(false);
		selectFirstDiff(true);
	}

	private void historyNotification(OperationHistoryEvent event) {
		switch (event.getEventType()) {
		case OperationHistoryEvent.OPERATION_ADDED:
			if (copyOperationInProgress) {
				copyUndoable = event.getOperation();
			}
			break;
		case OperationHistoryEvent.UNDONE:
			if (copyUndoable == event.getOperation()) {
				update(false);
			}
			break;
		default:
			// Nothing to do
			break;
		}
	}

	private void copyDiffLeftToRight() {
		copy(fCurrentDiff, true, false);
	}

	private void copyDiffRightToLeft() {
		copy(fCurrentDiff, false, false);
	}

	/*
	 * Copy the contents of the given diff from one side to the other.
	 */
	private void copy(Diff diff, boolean leftToRight, boolean gotoNext) {
		if (copy(diff, leftToRight)) {
			if (gotoNext) {
				navigate(true, true, false /* don't step in */);
			} else {
				revealDiff(diff, true);
				updateControls();
			}
		}
	}

	/*
	 * Copy the contents of the given diff from one side to the other but
	 * doesn't reveal anything.
	 * Returns true if copy was successful.
	 */
	private boolean copy(Diff diff, boolean leftToRight) {

		if (diff != null) {
			if (!validateChange(!leftToRight))
				return false;
			if (leftToRight) {
				fRight.setEnabled(true);
			} else {
				fLeft.setEnabled(true);
			}
			boolean result = fMerger.copy(diff, leftToRight);
			if (result)
				updateResolveStatus();
			return result;
		}
		return false;
	}

	private boolean validateChange(boolean left) {
		ContributorInfo info;
		if (left)
			info = fLeftContributor;
		else
			info = fRightContributor;

		return info.validateChange();
	}

	//---- scrolling

	/*
	 * The height of the TextEditors in lines.
	 */
	private int getViewportHeight() {
		StyledText te= fLeft.getSourceViewer().getTextWidget();

		int vh= te.getClientArea().height;
		if (vh == 0) {
			Rectangle trim= te.computeTrim(0, 0, 0, 0);
			int scrollbarHeight= trim.height;

			int headerHeight= getHeaderHeight();

			Composite composite= (Composite) getControl();
			Rectangle r= composite.getClientArea();

			vh= r.height-headerHeight-scrollbarHeight;
		}

		return vh / te.getLineHeight();
	}

	/*
	 * Returns the virtual position for the given view position.
	 */
	private int realToVirtualPosition(char contributor, int vpos) {
		if (! fSynchronizedScrolling)
			return vpos;
		return fMerger.realToVirtualPosition(contributor, vpos);
	}

	private void scrollVertical(int avpos, int lvpos, int rvpos, MergeSourceViewer allBut) {

		int s= 0;

		if (fSynchronizedScrolling) {
			s= fMerger.getVirtualHeight() - rvpos;
			int height= fRight.getViewportLines()/4;
			if (s < 0)
				s= 0;
			if (s > height)
				s= height;
		}

		fInScrolling= true;

		if (isThreeWay() && allBut != fAncestor) {
			if (fSynchronizedScrolling || allBut == null) {
				int y= virtualToRealPosition(ANCESTOR_CONTRIBUTOR, avpos+s)-s;
				fAncestor.vscroll(y);
			}
		}

		if (allBut != fLeft) {
			if (fSynchronizedScrolling || allBut == null) {
				int y= virtualToRealPosition(LEFT_CONTRIBUTOR, lvpos+s)-s;
				fLeft.vscroll(y);
			}
		}

		if (allBut != fRight) {
			if (fSynchronizedScrolling || allBut == null) {
				int y= virtualToRealPosition(RIGHT_CONTRIBUTOR, rvpos+s)-s;
				fRight.vscroll(y);
			}
		}

		fInScrolling= false;

		if (isThreeWay() && fAncestorCanvas != null)
			fAncestorCanvas.redraw();

		if (fLeftCanvas != null)
			fLeftCanvas.redraw();

		Control center= getCenterControl();
		if (center instanceof Canvas)
			((Canvas) center).redraw();

		if (fRightCanvas != null)
			fRightCanvas.redraw();
	}

	/*
	 * Updates Scrollbars with viewports.
	 */
	private void syncViewport(MergeSourceViewer w) {

		if (fInScrolling)
			return;

		int ix= w.getSourceViewer().getTopIndex();
		int ix2= w.getDocumentRegionOffset();

		int viewPosition= realToVirtualPosition(getLeg(w), ix-ix2);

		scrollVertical(viewPosition, viewPosition, viewPosition, w);	// scroll all but the given views

		if (fVScrollBar != null) {
			int value= Math.max(0, Math.min(viewPosition, fMerger.getVirtualHeight() - getViewportHeight()));
			fVScrollBar.setSelection(value);
			//refreshBirdEyeView();
		}
	}

	private void updateVScrollBar() {

		if (Utilities.okToUse(fVScrollBar) && fSynchronizedScrolling) {
			int virtualHeight= fMerger.getVirtualHeight();
			int viewPortHeight= getViewportHeight();
			int pageIncrement= viewPortHeight-1;
			int thumb= (viewPortHeight > virtualHeight) ? virtualHeight : viewPortHeight;

			fVScrollBar.setPageIncrement(pageIncrement);
			fVScrollBar.setMaximum(virtualHeight);
			fVScrollBar.setThumb(thumb);
		}
	}

	/*
	 * maps given virtual position into a real view position of this view.
	 */
	private int virtualToRealPosition(char contributor, int v) {
		if (! fSynchronizedScrolling)
			return v;
		return fMerger.virtualToRealPosition(contributor, v);
	}

	@Override
	void flushLeftSide(Object oldInput, IProgressMonitor monitor){
		IMergeViewerContentProvider content= getMergeContentProvider();
		Object leftContent = content.getLeftContent(oldInput);

		if (leftContent != null && isLeftEditable() && isLeftDirty()) {
			if (fLeftContributor.hasSharedDocument(leftContent)) {
				if (flush(fLeftContributor))
					setLeftDirty(false);
			}
		}

		if (!(content instanceof MergeViewerContentProvider) || isLeftDirty()) {
			super.flushLeftSide(oldInput, monitor);
		}
	}

	@Override
	void flushRightSide(Object oldInput, IProgressMonitor monitor){
		IMergeViewerContentProvider content= getMergeContentProvider();
		Object rightContent = content.getRightContent(oldInput);

		if (rightContent != null && isRightEditable() && isRightDirty()) {
			if (fRightContributor.hasSharedDocument(rightContent)) {
				if (flush(fRightContributor))
					setRightDirty(false);
			}
		}

		if (!(content instanceof MergeViewerContentProvider) || isRightDirty()) {
			super.flushRightSide(oldInput, monitor);
		}
	}

	@Override
	protected void flushContent(Object oldInput, IProgressMonitor monitor) {
		flushLeftSide(oldInput, monitor);
		flushRightSide(oldInput, monitor);

		IMergeViewerContentProvider content = getMergeContentProvider();

		if (!(content instanceof MergeViewerContentProvider) || isLeftDirty() || isRightDirty()) {
			super.flushContent(oldInput, monitor);
		}
	}

	private boolean flush(final ContributorInfo info) {
		try {
			return info.flush();
		} catch (CoreException e) {
			handleException(e);
		}
		return false;
	}

	private void handleException(Throwable throwable) {
		// TODO: Should show error to the user
		if (throwable instanceof InvocationTargetException) {
			InvocationTargetException ite = (InvocationTargetException) throwable;
			handleException(ite.getTargetException());
			return;
		}
		CompareUIPlugin.log(throwable);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IMergeViewerTestAdapter.class) {
			return (T) new IMergeViewerTestAdapter() {
				@Override
				public IDocument getDocument(char leg) {
					switch (leg) {
					case LEFT_CONTRIBUTOR:
						return fLeft.getSourceViewer().getDocument();
					case RIGHT_CONTRIBUTOR:
						return fRight.getSourceViewer().getDocument();
					case ANCESTOR_CONTRIBUTOR:
						return fAncestor.getSourceViewer().getDocument();
					default:
						return null;
					}
				}

				@Override
				public int getChangesCount() {
					return fMerger.changesCount();
				}
			};
		}
		if (adapter == OutlineViewerCreator.class) {
			if (fOutlineViewerCreator == null)
				fOutlineViewerCreator = new InternalOutlineViewerCreator();
			return (T) fOutlineViewerCreator;

		}
		if (adapter == IFindReplaceTarget.class)
			return (T) getFindReplaceTarget();
		if (adapter == CompareHandlerService.class)
			return (T) fHandlerService;
		if (adapter == CompareHandlerService[].class) {
			return (T) new CompareHandlerService[] { fHandlerService,
					super.getCompareHandlerService() };
		}
		if (adapter == IEditorInput.class) {
			// return active editor input
			if (fLeft != null && fLeft == fFocusPart)
				if (fLeftContributor != null)
					return (T) fLeftContributor.getDocumentKey();
			if (fRight != null && fRight == fFocusPart)
				if (fRightContributor != null)
					return (T) fRightContributor.getDocumentKey();
			if (fAncestor != null && fAncestor == fFocusPart)
				if (fAncestorContributor != null)
					return (T) fAncestorContributor.getDocumentKey();
		}
		return null;
	}

	@Override
	protected void handleCompareInputChange() {
		try {
			beginRefresh();
			super.handleCompareInputChange();
		} finally {
			endRefresh();
		}
	}

	private void beginRefresh() {
		isRefreshing++;
		fLeftContributor.cacheSelection(fLeft);
		fRightContributor.cacheSelection(fRight);
		fAncestorContributor.cacheSelection(fAncestor);
		if (fSynchronizedScrolling) {
			fSynchronziedScrollPosition = fVScrollBar.getSelection();
		}

	}

	private void endRefresh() {
		isRefreshing--;
		fLeftContributor.cacheSelection(null);
		fRightContributor.cacheSelection(null);
		fAncestorContributor.cacheSelection(null);
		fSynchronziedScrollPosition = -1;
	}

	private void synchronizedScrollVertical(int vpos) {
		scrollVertical(vpos, vpos, vpos, null);
	}

	private boolean isIgnoreAncestor() {
		return Utilities.getBoolean(getCompareConfiguration(), ICompareUIConstants.PROP_IGNORE_ANCESTOR, false);
	}

	/* package */ void update(boolean includeControls) {
		if (getControl().isDisposed())
			return;
		if (fHasErrors) {
			resetDiffs();
		} else {
			doDiff();
		}

		if (includeControls)
			updateControls();

		updateVScrollBar();
		updatePresentation();
		updateToolbarLabel();
	}

	private void updateToolbarLabel() {
		final String DIFF_COUNT_ID = "DiffCount"; //$NON-NLS-1$
		boolean isUpdateNeeded = false;
		ToolBarManager tbm = (ToolBarManager) getToolBarManager(fComposite.getParent());
		int differenceCount = fMerger.changesCount();
		if (tbm != null && tbm.getItems().length > 0) {

			String label = MessageFormat.format(CompareMessages.TextMergeViewer_differences, differenceCount);
			LabelContributionItem labelContributionItem = new LabelContributionItem(DIFF_COUNT_ID, label);

			if (tbm.find(DIFF_COUNT_ID) != null) {
				tbm.replaceItem(DIFF_COUNT_ID, labelContributionItem);
				isUpdateNeeded = true;
			} else if (tbm.find("diffLabel") != null) { //$NON-NLS-1$
				tbm.appendToGroup("diffLabel", labelContributionItem); //$NON-NLS-1$
				isUpdateNeeded = true;
			}
			if (isUpdateNeeded) {
				fComposite.getDisplay().asyncExec(() -> {
					// relayout in next tick
					ToolBar control = tbm.getControl();
					if (control != null && !control.isDisposed()) {
						tbm.update(true);
					}
				});
			}
		}
	}

	private void resetDiffs() {
		// clear stuff
		saveDiff();
		fCurrentDiff= null;
		fMerger.reset();
		resetPositions(fLeft.getSourceViewer().getDocument());
		resetPositions(fRight.getSourceViewer().getDocument());
		resetPositions(fAncestor.getSourceViewer().getDocument());
	}

	private boolean isPatchHunk() {
		return Utilities.isHunk(getInput());
	}

	private boolean isPatchHunkOk() {
		if (isPatchHunk())
			return Utilities.isHunkOk(getInput());
		return false;
	}

	/**
	 * Return the provided start position of the hunk in the target file.
	 * @return the provided start position of the hunk in the target file
	 */
	private int getHunkStart() {
		Object input = getInput();
		if (input instanceof DiffNode){
			ITypedElement right = ((DiffNode) input).getRight();
			if (right != null) {
				Object element = Adapters.adapt(right, IHunk.class);
				if (element instanceof IHunk)
					return ((IHunk) element).getStartPosition();
			}
			ITypedElement left = ((DiffNode) input).getLeft();
			if (left != null) {
				Object element = Adapters.adapt(left, IHunk.class);
				if (element instanceof IHunk)
					return ((IHunk) element).getStartPosition();
			}
		}
		return 0;
	}

	private IFindReplaceTarget getFindReplaceTarget() {
		if (fFindReplaceTarget == null)
			fFindReplaceTarget= new FindReplaceTarget();
		return fFindReplaceTarget;
	}

	/* package */ char getLeg(MergeSourceViewer w) {
		if (w == fLeft)
			return LEFT_CONTRIBUTOR;
		if (w == fRight)
			return RIGHT_CONTRIBUTOR;
		if (w == fAncestor)
			return ANCESTOR_CONTRIBUTOR;
		return ANCESTOR_CONTRIBUTOR;
	}

	private boolean isCurrentDiff(Diff diff) {
		if (diff == null)
			return false;
		if (diff == fCurrentDiff)
			return true;
		if (fCurrentDiff != null && fCurrentDiff.getParent() == diff)
			return true;
		return false;
	}

	private boolean isNavigationPossible() {
		if (fCurrentDiff == null && fMerger.hasChanges())
			return true;
		else if (fMerger.changesCount() > 1)
			return true;
		else if (fCurrentDiff != null && fCurrentDiff.hasChildren())
			return true;
		else if (fCurrentDiff != null && fCurrentDiff.isToken())
			return true;
		return false;
	}

	/**
	 * This method returns {@link ITextEditor} used in the
	 * {@link ChangeEncodingAction}. It provides implementation of methods that
	 * are used by the action by delegating them to {@link ContributorInfo} that
	 * corresponds to the side that has focus.
	 *
	 * @return the text editor adapter
	 */
	private ITextEditor getTextEditorAdapter() {
		return new ITextEditor() {
			@Override
			public void close(boolean save) {
			}
			@Override
			public void doRevertToSaved() {
			}
			@Override
			public IAction getAction(String actionId) {
				return null;
			}
			@Override
			public IDocumentProvider getDocumentProvider() {
				return null;
			}
			@Override
			public IRegion getHighlightRange() {
				return null;
			}
			@Override
			public ISelectionProvider getSelectionProvider() {
				return null;
			}

			@Override
			public boolean isEditable() {
				return false;
			}

			@Override
			public void removeActionActivationCode(String actionId) {
			}

			@Override
			public void resetHighlightRange() {
			}

			@Override
			public void selectAndReveal(int offset, int length) {
			}

			@Override
			public void setAction(String actionId, IAction action) {
			}

			@Override
			public void setActionActivationCode(String actionId,
					char activationCharacter, int activationKeyCode,
					int activationStateMask) {
			}

			@Override
			public void setHighlightRange(int offset, int length, boolean moveCursor) {
			}

			@Override
			public void showHighlightRangeOnly(boolean showHighlightRangeOnly) {
			}

			@Override
			public boolean showsHighlightRangeOnly() {
				return false;
			}

			@Override
			public IEditorInput getEditorInput() {
				if (fFocusPart == fAncestor && fAncestorContributor != null) {
					return fAncestorContributor.getDocumentKey();
				} else if (fFocusPart == fLeft && fLeftContributor != null) {
					return fLeftContributor.getDocumentKey();
				} else if (fFocusPart == fRight && fRightContributor != null) {
					return fRightContributor.getDocumentKey();
				} else {
					return null;
				}
			}

			@Override
			public IEditorSite getEditorSite() {
				return null;
			}

			@Override
			public void init(IEditorSite site, IEditorInput input)
					throws PartInitException {
			}

			@Override
			public void addPropertyListener(IPropertyListener listener) {
			}

			@Override
			public void createPartControl(Composite parent) {
			}

			@Override
			public void dispose() {
			}

			@Override
			public IWorkbenchPartSite getSite() {
				return new IWorkbenchPartSite() {
					@Override
					public String getId() {
						return null;
					}
					@Override
					@Deprecated
					public IKeyBindingService getKeyBindingService() {
						return null;
					}
					@Override
					public IWorkbenchPart getPart() {
						return null;
					}
					@Override
					public String getPluginId() {
						return null;
					}
					@Override
					public String getRegisteredName() {
						return null;
					}
					@Override
					public void registerContextMenu(MenuManager menuManager,
							ISelectionProvider selectionProvider) {
					}
					@Override
					public void registerContextMenu(String menuId,
							MenuManager menuManager,
							ISelectionProvider selectionProvider) {
					}
					@Override
					public IWorkbenchPage getPage() {
						return null;
					}
					@Override
					public ISelectionProvider getSelectionProvider() {
						return null;
					}
					@Override
					public Shell getShell() {
						return fComposite.getShell();
					}
					@Override
					public IWorkbenchWindow getWorkbenchWindow() {
						return null;
					}
					@Override
					public void setSelectionProvider(ISelectionProvider provider) {
					}
					@Override
					public <T> T getAdapter(Class<T> adapter) {
						return null;
					}
					@Override
					public <T> T getService(Class<T> api) {
						return null;
					}
					@Override
					public boolean hasService(Class<?> api) {
						return false;
					}
				};
			}

			@Override
			public String getTitle() {
				return null;
			}

			@Override
			public Image getTitleImage() {
				return null;
			}

			@Override
			public String getTitleToolTip() {
				return null;
			}

			@Override
			public void removePropertyListener(IPropertyListener listener) {
			}

			@Override
			public void setFocus() {
			}

			@Override
			@SuppressWarnings("unchecked")
			public <T> T getAdapter(Class<T> adapter) {
				if (adapter == IEncodingSupport.class) {
					if (fFocusPart == fAncestor) {
						return (T) getEncodingSupport(fAncestorContributor);
					} else if (fFocusPart == fLeft) {
						return (T) getEncodingSupport(fLeftContributor);
					} else if (fFocusPart == fRight) {
						return (T) getEncodingSupport(fRightContributor);
					}
				}
				return null;
			}

			private IEncodingSupport getEncodingSupport(ContributorInfo contributor) {
				if (contributor != null && contributor.getDefaultEncoding() != null) {
					return contributor;
				}
				return null;
			}

			@Override
			public void doSave(IProgressMonitor monitor) {
			}

			@Override
			public void doSaveAs() {
			}

			@Override
			public boolean isDirty() {
				if (fFocusPart == fLeft) {
					return isLeftDirty();
				} else if (fFocusPart == fRight) {
					return isRightDirty();
				}
				return false;
			}

			@Override
			public boolean isSaveAsAllowed() {
				// Implementing interface method
				return false;
			}

			@Override
			public boolean isSaveOnCloseNeeded() {
				// Implementing interface method
				return false;
			}
		};
	}

	private void updateStructure() {
		getCompareConfiguration().setProperty("ALL_STRUCTURE_REFRESH", null); //$NON-NLS-1$
	}

	private void updateStructure(char leg) {
		String key = null;
		switch (leg) {
		case ANCESTOR_CONTRIBUTOR:
			key = "ANCESTOR_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		case LEFT_CONTRIBUTOR:
			key = "LEFT_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		case RIGHT_CONTRIBUTOR:
			key = "RIGHT_STRUCTURE_REFRESH"; //$NON-NLS-1$
			break;
		default:
			break;
		}
		Assert.isNotNull(key);
		getCompareConfiguration().setProperty(key, null);
	}
}
