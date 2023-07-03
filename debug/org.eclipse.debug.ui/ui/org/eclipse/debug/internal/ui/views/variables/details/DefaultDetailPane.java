/*******************************************************************************
 *  Copyright (c) 2006, 2019 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *     QNX Software Systems - Mikhail Khodjaiants - Registers View (Bug 53640)
 *     Wind River - Anton Leherbauer - Fix selection provider (Bug 254442)
 *     Patrick Chuong (Texas Instruments) - Improve usability of the breakpoint view (Bug 238956)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.variables.details;

import java.text.MessageFormat;
import java.util.Iterator;
import java.util.ResourceBundle;

import org.eclipse.core.commands.operations.IUndoContext;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugElement;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IDebugHelpContextIds;
import org.eclipse.debug.internal.ui.LazyModelPresentation;
import org.eclipse.debug.internal.ui.VariablesViewModelPresentation;
import org.eclipse.debug.internal.ui.actions.variables.details.DetailPaneAssignValueAction;
import org.eclipse.debug.internal.ui.actions.variables.details.DetailPaneMaxLengthAction;
import org.eclipse.debug.internal.ui.actions.variables.details.DetailPaneWordWrapAction;
import org.eclipse.debug.internal.ui.breakpoints.provisional.IBreakpointContainer;
import org.eclipse.debug.internal.ui.preferences.IDebugPreferenceConstants;
import org.eclipse.debug.internal.ui.views.variables.IndexedValuePartition;
import org.eclipse.debug.ui.IDebugModelPresentation;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDebugView;
import org.eclipse.debug.ui.IDetailPane2;
import org.eclipse.debug.ui.IValueDetailListener;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.StatusLineContributionItem;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.text.ITextOperationTarget;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.IUndoManager;
import org.eclipse.jface.text.IUndoManagerExtension;
import org.eclipse.jface.text.WhitespaceCharacterPainter;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.FocusAdapter;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.operations.OperationHistoryActionHandler;
import org.eclipse.ui.operations.RedoActionHandler;
import org.eclipse.ui.operations.UndoActionHandler;
import org.eclipse.ui.progress.WorkbenchJob;
import org.eclipse.ui.texteditor.AbstractTextEditor;
import org.eclipse.ui.texteditor.FindReplaceAction;
import org.eclipse.ui.texteditor.IAbstractTextEditorHelpContextIds;
import org.eclipse.ui.texteditor.ITextEditorActionConstants;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;

/**
 * This detail pane uses a source viewer to display detailed information about the current
 * selection.  It incorporates a large number of actions into its context menu.  It is the
 * default detail pane.
 *
 * @see DefaultDetailPaneFactory
 * @since 3.3
 *
 */
public class DefaultDetailPane extends AbstractDetailPane implements IDetailPane2, IAdaptable, IPropertyChangeListener{

	/**
	 * These are the IDs for the actions in the context menu
	 */
	protected static final String DETAIL_COPY_ACTION = ActionFactory.COPY.getId() + ".SourceDetailPane"; //$NON-NLS-1$
	protected static final String DETAIL_SELECT_ALL_ACTION = IDebugView.SELECT_ALL_ACTION + ".SourceDetailPane"; //$NON-NLS-1$
	protected static final String DETAIL_PASTE_ACTION = ActionFactory.PASTE.getId();
	protected static final String DETAIL_CUT_ACTION = ActionFactory.CUT.getId();

	protected static final String DETAIL_FIND_REPLACE_TEXT_ACTION = "FindReplaceText"; //$NON-NLS-1$
	protected static final String DETAIL_CONTENT_ASSIST_ACTION = "ContentAssist"; //$NON-NLS-1$
	protected static final String DETAIL_ASSIGN_VALUE_ACTION = "AssignValue"; //$NON-NLS-1$

	protected static final String DETAIL_WORD_WRAP_ACTION = IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP;
	protected static final String DETAIL_MAX_LENGTH_ACTION = "MaxLength"; //$NON-NLS-1$

	/**
	 * The ID, name and description of this pane are stored in constants so that the class
	 * does not have to be instantiated to access them.
	 */
	public static final String ID = DetailMessages.DefaultDetailPane_0;
	public static final String NAME = DetailMessages.DefaultDetailPane_1;
	public static final String DESCRIPTION = DetailMessages.DefaultDetailPane_57;

	/**
	 * Data structure for the position label value.
	 */
	private static class PositionLabelValue {

		public int fValue;

		@Override
		public String toString() {
			return String.valueOf(fValue);
		}
	}

	/**
	 * Internal interface for a cursor listener. I.e. aggregation
	 * of mouse and key listener.
	 * @since 3.0
	 */
	interface ICursorListener extends MouseListener, KeyListener {
	}

	/**
	 * Job to compute the details for a selection
	 */
	class DetailJob extends Job implements IValueDetailListener {

		private IStructuredSelection fElements;
		private IDebugModelPresentation fModel;
		private boolean fFirst = true;
		// whether a result was collected
		private boolean fComputed = false;
		private IProgressMonitor fMonitor;

		public DetailJob(IStructuredSelection elements, IDebugModelPresentation model) {
			super("compute variable details"); //$NON-NLS-1$
			setSystem(true);
			fElements = elements;
			fModel = model;
		}

		@Override
		protected IStatus run(IProgressMonitor monitor) {
			fMonitor = monitor;
			Iterator<?> iterator = fElements.iterator();
			String message = null;
			while (iterator.hasNext()) {
				if (monitor.isCanceled()) {
					break;
				}
				Object element = iterator.next();
				IValue val = null;
				if (element instanceof IVariable) {
					try {
						val = ((IVariable)element).getValue();
					} catch (DebugException e) {
						detailComputed(null, e.getStatus().getMessage());
					}
				} else if (element instanceof IExpression) {
					val = ((IExpression)element).getValue();
				} else if (element instanceof IBreakpoint) {
					IBreakpoint bp = (IBreakpoint) element;
					message = bp.getMarker().getAttribute(IMarker.MESSAGE, ""); //$NON-NLS-1$
				} else if (element instanceof IBreakpointContainer) {
					IBreakpointContainer c = (IBreakpointContainer) element;
					IAdaptable category = c.getCategory();
					if (category != null) {
						IWorkbenchAdapter adapter = category.getAdapter(IWorkbenchAdapter.class);
						if (adapter != null) {
							message = adapter.getLabel(category);
						} else {
							message = c.getOrganizer().getLabel();
						}
					}
				}
				// When selecting a index partition, clear the pane
				if (val instanceof IndexedValuePartition) {
					detailComputed(null, IInternalDebugCoreConstants.EMPTY_STRING);
					val = null;
				}
				if (element instanceof String) {
					message = (String) element;
				}
				if (val != null && !monitor.isCanceled()) {
					fModel.computeDetail(val, this);
					synchronized (this) {
						try {
							// detail could already computed at this point (see bug 252433)
							if (!fComputed) {
								// wait for a max of 30 seconds for result, then cancel
								wait(30000);
								if (!fComputed) {
									fMonitor.setCanceled(true);
								}
							}
						} catch (InterruptedException e) {
							break;
						}
					}
				}
			}
			// If no details were computed for the selected variable, clear the pane
			// or use the message, if the variable was a java.lang.String
			if (!fComputed){
				if (message == null) {
					detailComputed(null,IInternalDebugCoreConstants.EMPTY_STRING);
				} else {
					detailComputed(null, message);
				}
			}
			return Status.OK_STATUS;
		}

		@Override
		protected void canceling() {
			super.canceling();
			synchronized (this) {
				notifyAll();
			}
		}

		@Override
		public void detailComputed(IValue value, final String result) {
			synchronized (this) {
				fComputed = true;
			}
			String valueString = result;
			if (valueString == null){
				try{
					valueString = value.getValueString();
				} catch (DebugException e){
					valueString = e.getMessage();
				}
			}
			final String detail = (valueString != null) ? valueString : DetailMessages.DefaultDetailPane_3;
			if (!fMonitor.isCanceled()) {
				WorkbenchJob append = new WorkbenchJob("append details") { //$NON-NLS-1$
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						if (!fMonitor.isCanceled()) {
							String insert = detail;
							int length = 0;
							if (!fFirst) {
								length = getDetailDocument().getLength();
							}
							if (length > 0) {
								insert = "\n" + result; //$NON-NLS-1$
							}
							try {
								int max = DebugUIPlugin.getDefault().getPreferenceStore().getInt(IDebugUIConstants.PREF_MAX_DETAIL_LENGTH);
								if (max > 0 && insert.length() > max) {
									insert = insert.substring(0, max) + "..."; //$NON-NLS-1$
								}
								int topIndex = -1;
								StyledText styledText = fSourceViewer.getTextWidget();
								if (styledText == null) { // fSourceViewer is disposed
									return Status.OK_STATUS;
								}
								Point selection = null;
								// keep scrolling state in details pane in case
								// of longer string over multiple lines
								if (containsLineDelimiter(insert)) {
									int ti = styledText.getTopIndex();
									if (ti == 0) {
										int caretOffset = styledText.getCaretOffset();
										String topStr = styledText.getText().substring(0, caretOffset);
										if (insert.length() > caretOffset) {
											String newTopStr = insert.substring(0, caretOffset);
											// only when string from start to
											// caretOffset has not changed
											if (topStr.equals(newTopStr)) {
												topIndex = ti;
												selection = styledText.getSelection();
											}
										}
									} else if (ti > 0) {
										int topOffset = styledText.getOffsetAtLine(ti);
										String topStr = styledText.getText().substring(0, topOffset);
										if (insert.length() > topOffset) {
											String newTopStr = insert.substring(0, topOffset);
											// only when string from start to
											// topIndex has not changed
											if (topStr.equals(newTopStr)) {
												topIndex = ti;
												selection = styledText.getSelection();
											}
										}
									}
								}
								IDocument detailDoc = getDetailDocument();
								if (fFirst) {
									if (insert.equals(detailDoc.get())) {
										topIndex = -1; // source not modified,
														// no need to set the
														// document
									} else {
										detailDoc.set(insert);
									}
									fFirst = false;
								} else {
									detailDoc.replace(length, 0, insert);
								}
								if (topIndex > -1) {
									styledText.setTopIndex(topIndex);
									if (selection != null) {
										styledText.setSelection(selection);
									}
								}
							} catch (BadLocationException e) {
								DebugUIPlugin.log(e);
							}
						}
						return Status.OK_STATUS;
					}

					private boolean containsLineDelimiter(String str) {
						if (str.contains("\n") || str.contains("\r")) { //$NON-NLS-1$ //$NON-NLS-2$
							return true;
						}
						return false;
					}
				};
				append.setSystem(true);
				append.schedule();
			}
			synchronized (this) {
				notifyAll();
			}

		}

	}

	/**
	 * The model presentation used to produce the string details for a
	 * selected variable.
	 */
	private VariablesViewModelPresentation fModelPresentation;
	private String fDebugModelIdentifier;

	/**
	 * Controls the status line while the details area has focus.
	 * Displays the current cursor position in the text (line:character).
	 */
	private StatusLineContributionItem fStatusLineItem;

	/**
	 * The source viewer in which the computed string detail
	 * of selected variables will be displayed.
	 */
	private SourceViewer fSourceViewer;

	/**
	 * The last selection displayed in the source viewer.
	 */
	private IStructuredSelection fLastDisplayed = null;

	/**
	 * Flag used to track whether source viewer has focus.  It helps avoid
	 * resetting global actions incorrectly.
	 */
	private boolean fHasFocus = false;

	/**
	 * Variables used to create the detailed information for a selection
	 */
	private IDocument fDetailDocument;
	private DetailJob fDetailJob = null;
	private final String fPositionLabelPattern = DetailMessages.DefaultDetailPane_56;
	private final PositionLabelValue fLineLabel = new PositionLabelValue();
	private final PositionLabelValue fColumnLabel = new PositionLabelValue();
	private final Object[] fPositionLabelPatternArguments = new Object[] {
			fLineLabel, fColumnLabel };
	private ICursorListener fCursorListener;

	/**
	 * Handler activation object so that we can use the global content assist command
	 * and properly deactivate it later.
	 */
	private IHandlerActivation fContentAssistActivation;
	private IPropertyChangeListener fPreferenceStorePropertyChangeListener;
	private WhitespaceCharacterPainter fWhiteSpacePainter;

	@Override
	public Control createControl(Composite parent) {

		fModelPresentation = new VariablesViewModelPresentation();

		createSourceViewer(parent);

		if (isInView()){
			createViewSpecificComponents();
			createActions();
			DebugUIPlugin.getDefault().getPreferenceStore().addPropertyChangeListener(this);
			JFaceResources.getFontRegistry().addListener(this);
		}

		return fSourceViewer.getControl();
	}

	/**
	 * Creates the source viewer in the given parent composite
	 *
	 * @param parent Parent composite to create the source viewer in
	 */
	private void createSourceViewer(Composite parent) {

		// Create & configure a SourceViewer
		fSourceViewer = new SourceViewer(parent, null, SWT.V_SCROLL | SWT.H_SCROLL);
		fSourceViewer.setDocument(getDetailDocument());
		fSourceViewer.getTextWidget().setFont(JFaceResources.getFont(IDebugUIConstants.PREF_DETAIL_PANE_FONT));
		fSourceViewer.getTextWidget().setWordWrap(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP));
		fSourceViewer.setEditable(false);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(fSourceViewer.getTextWidget(), IDebugHelpContextIds.DETAIL_PANE);
		Control control = fSourceViewer.getControl();
		GridData gd = new GridData(GridData.FILL_BOTH);
		control.setLayoutData(gd);
		toggleWhitespaceCharacterPainter();
		installWhitespacePreferenceListener();
	}

	private void installWhitespacePreferenceListener() {
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		fPreferenceStorePropertyChangeListener = event -> {
			String property = event.getProperty();
			if (AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS.equals(property)) {
				toggleWhitespaceCharacterPainter();
			}
		};
		store.addPropertyChangeListener(fPreferenceStorePropertyChangeListener);
	}

	private void removePreferenceListener() {
		if (fPreferenceStorePropertyChangeListener != null) {
			IPreferenceStore store = EditorsUI.getPreferenceStore();
			store.removePropertyChangeListener(fPreferenceStorePropertyChangeListener);
			fPreferenceStorePropertyChangeListener = null;
		}
	}

	private void disposeWhitespaceCharacterPainter() {
		if (fWhiteSpacePainter != null) {
			fSourceViewer.removePainter(fWhiteSpacePainter);
			fWhiteSpacePainter.dispose();
			fWhiteSpacePainter = null;
		}
	}

	private void toggleWhitespaceCharacterPainter() {
		if (fSourceViewer == null || fSourceViewer.getTextWidget() == null) {
			return;
		}
		IPreferenceStore store = EditorsUI.getPreferenceStore();
		boolean checked = store.getBoolean(AbstractTextEditor.PREFERENCE_SHOW_WHITESPACE_CHARACTERS);
		if (checked) {
			fWhiteSpacePainter = new WhitespaceCharacterPainter(fSourceViewer);
			fSourceViewer.addPainter(fWhiteSpacePainter);
		} else {
			if (fWhiteSpacePainter != null) {
				fSourceViewer.removePainter(fWhiteSpacePainter);
				fWhiteSpacePainter = null;
			}
		}
	}

	/**
	 * Creates listeners and other components that should only be added to the
	 * source viewer when this detail pane is inside a view.
	 */
	private void createViewSpecificComponents(){

		// Add a document listener so actions get updated when the document changes
		getDetailDocument().addDocumentListener(new IDocumentListener() {
			@Override
			public void documentAboutToBeChanged(DocumentEvent event) {}
			@Override
			public void documentChanged(DocumentEvent event) {
				updateSelectionDependentActions();
			}
		});

		// Add the selection listener so selection dependent actions get updated.
		fSourceViewer.getSelectionProvider().addSelectionChangedListener(event -> updateSelectionDependentActions());

		// Add a focus listener to update actions when details area gains focus
		fSourceViewer.getControl().addFocusListener(new FocusAdapter() {
			@Override
			public void focusGained(FocusEvent e) {
				setGlobalAction(IDebugView.SELECT_ALL_ACTION, getAction(DETAIL_SELECT_ALL_ACTION));
				setGlobalAction(IDebugView.CUT_ACTION, getAction(DETAIL_CUT_ACTION));
				setGlobalAction(IDebugView.COPY_ACTION, getAction(DETAIL_COPY_ACTION));
				setGlobalAction(IDebugView.PASTE_ACTION, getAction(DETAIL_PASTE_ACTION));
				setGlobalAction(IDebugView.FIND_ACTION, getAction(DETAIL_FIND_REPLACE_TEXT_ACTION));
				IAction action = getAction(DETAIL_ASSIGN_VALUE_ACTION);
				setGlobalAction(action.getActionDefinitionId(), action);
				action = getAction(DETAIL_CONTENT_ASSIST_ACTION);
				setGlobalAction(action.getActionDefinitionId(),action);

				getViewSite().getActionBars().updateActionBars();

				updateAction(DETAIL_FIND_REPLACE_TEXT_ACTION);
				fHasFocus = true;
			}

			@Override
			public void focusLost(FocusEvent e) {
				setGlobalAction(IDebugView.SELECT_ALL_ACTION, null);
				setGlobalAction(IDebugView.CUT_ACTION, null);
				setGlobalAction(IDebugView.COPY_ACTION, null);
				setGlobalAction(IDebugView.PASTE_ACTION, null);
				setGlobalAction(IDebugView.FIND_ACTION, null);
				setGlobalAction(getAction(DETAIL_ASSIGN_VALUE_ACTION).getActionDefinitionId(), null);
				setGlobalAction(getAction(DETAIL_CONTENT_ASSIST_ACTION).getActionDefinitionId(), null);

				getViewSite().getActionBars().updateActionBars();
				fHasFocus = false;
			}
		});

		// disposed controls don't get a FocusOut event, make sure all actions
		// have been deactivated
		fSourceViewer.getControl().addDisposeListener(e -> {
			if (fHasFocus) {
				setGlobalAction(IDebugView.SELECT_ALL_ACTION, null);
				setGlobalAction(IDebugView.CUT_ACTION, null);
				setGlobalAction(IDebugView.COPY_ACTION, null);
				setGlobalAction(IDebugView.PASTE_ACTION, null);
				setGlobalAction(IDebugView.FIND_ACTION, null);
				setGlobalAction(getAction(DETAIL_ASSIGN_VALUE_ACTION)
						.getActionDefinitionId(), null);
				setGlobalAction(getAction(DETAIL_CONTENT_ASSIST_ACTION)
						.getActionDefinitionId(), null);
			}
		});

		// Create a status line item displaying the current cursor location
		fStatusLineItem = new StatusLineContributionItem("ModeContributionItem"); //$NON-NLS-1$
		IStatusLineManager manager= getViewSite().getActionBars().getStatusLineManager();
		manager.add(fStatusLineItem);
		fSourceViewer.getTextWidget().addMouseListener(getCursorListener());
		fSourceViewer.getTextWidget().addKeyListener(getCursorListener());

		// Add a context menu to the detail area
		createDetailContextMenu(fSourceViewer.getTextWidget());

	}

	/**
	 * Creates the actions to add to the context menu
	 */
	private void createActions() {
		TextViewerAction textAction= new TextViewerAction(fSourceViewer, ISourceViewer.CONTENTASSIST_PROPOSALS);
		textAction.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		textAction.configureAction(DetailMessages.DefaultDetailPane_Co_ntent_Assist_3, IInternalDebugCoreConstants.EMPTY_STRING,IInternalDebugCoreConstants.EMPTY_STRING);
		textAction.setImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_ELCL_CONTENT_ASSIST));
		textAction.setHoverImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_LCL_CONTENT_ASSIST));
		textAction.setDisabledImageDescriptor(DebugPluginImages.getImageDescriptor(IDebugUIConstants.IMG_DLCL_CONTENT_ASSIST));
		PlatformUI.getWorkbench().getHelpSystem().setHelp(textAction, IDebugHelpContextIds.DETAIL_PANE_CONTENT_ASSIST_ACTION);
		ActionHandler actionHandler = new ActionHandler(textAction);
		IHandlerService handlerService = getViewSite().getService(IHandlerService.class);
		fContentAssistActivation = handlerService.activateHandler(textAction.getActionDefinitionId(), actionHandler);
		setAction(DETAIL_CONTENT_ASSIST_ACTION, textAction);

		textAction= new TextViewerAction(fSourceViewer, ITextOperationTarget.SELECT_ALL);
		textAction.configureAction(DetailMessages.DefaultDetailPane_Select__All_5, IInternalDebugCoreConstants.EMPTY_STRING,IInternalDebugCoreConstants.EMPTY_STRING);
		textAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_SELECT_ALL);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(textAction, IDebugHelpContextIds.DETAIL_PANE_SELECT_ALL_ACTION);
		setAction(DETAIL_SELECT_ALL_ACTION, textAction);

		textAction= new TextViewerAction(fSourceViewer, ITextOperationTarget.COPY);
		textAction.configureAction(DetailMessages.DefaultDetailPane__Copy_8, IInternalDebugCoreConstants.EMPTY_STRING,IInternalDebugCoreConstants.EMPTY_STRING);
		textAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_COPY);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(textAction, IDebugHelpContextIds.DETAIL_PANE_COPY_ACTION);
		setAction(DETAIL_COPY_ACTION, textAction);

		textAction= new TextViewerAction(fSourceViewer, ITextOperationTarget.CUT);
		textAction.configureAction(DetailMessages.DefaultDetailPane_Cu_t_11, IInternalDebugCoreConstants.EMPTY_STRING,IInternalDebugCoreConstants.EMPTY_STRING);
		textAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_CUT);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(textAction, IDebugHelpContextIds.DETAIL_PANE_CUT_ACTION);
		setAction(DETAIL_CUT_ACTION, textAction);

		textAction= new TextViewerAction(fSourceViewer, ITextOperationTarget.PASTE);
		textAction.configureAction(DetailMessages.DefaultDetailPane__Paste_14, IInternalDebugCoreConstants.EMPTY_STRING,IInternalDebugCoreConstants.EMPTY_STRING);
		textAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_PASTE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(textAction, IDebugHelpContextIds.DETAIL_PANE_PASTE_ACTION);
		setAction(ActionFactory.PASTE.getId(), textAction);

		setSelectionDependantAction(DETAIL_COPY_ACTION);
		setSelectionDependantAction(DETAIL_CUT_ACTION);
		setSelectionDependantAction(DETAIL_PASTE_ACTION);

		// TODO: Still using "old" resource access, find/replace won't work in popup dialogs
		ResourceBundle bundle= ResourceBundle.getBundle("org.eclipse.debug.internal.ui.views.variables.VariablesViewResourceBundleMessages"); //$NON-NLS-1$
		IAction action = new FindReplaceAction(bundle, "find_replace_action_", getWorkbenchPartSite().getShell(), new FindReplaceTargetWrapper(fSourceViewer.getFindReplaceTarget())); //$NON-NLS-1$
		action.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_FIND_AND_REPLACE);
		PlatformUI.getWorkbench().getHelpSystem().setHelp(action, IDebugHelpContextIds.DETAIL_PANE_FIND_REPLACE_ACTION);
		setAction(DETAIL_FIND_REPLACE_TEXT_ACTION, action);

		updateSelectionDependentActions();

		action = new DetailPaneWordWrapAction(fSourceViewer);
		setAction(DETAIL_WORD_WRAP_ACTION, action);

		action = new DetailPaneMaxLengthAction(fSourceViewer.getControl().getShell());
		setAction(DETAIL_MAX_LENGTH_ACTION,action);

		action = new DetailPaneAssignValueAction(fSourceViewer,getViewSite());
		setAction(DETAIL_ASSIGN_VALUE_ACTION, action);
	}

	/**
	 * Create the context menu particular to the detail pane.  Note that anyone
	 * wishing to contribute an action to this menu must use
	 * <code>IDebugUIConstants.VARIABLE_VIEW_DETAIL_ID</code> as the
	 * <code>targetID</code> in the extension XML.
	 * @param menuControl the control to create the context menu on
	 */
	protected void createDetailContextMenu(Control menuControl) {
		MenuManager menuMgr= new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(this::fillDetailContextMenu);
		Menu menu= menuMgr.createContextMenu(menuControl);
		menuControl.setMenu(menu);

		getViewSite().registerContextMenu(IDebugUIConstants.VARIABLE_VIEW_DETAIL_ID, menuMgr, fSourceViewer.getSelectionProvider());

	}

	/**
	* Adds items to the detail pane's context menu including any extension defined
	* actions.
	*
	* @param menu The menu to add the item to.
	*/
	protected void fillDetailContextMenu(IMenuManager menu) {

		menu.add(new Separator(IDebugUIConstants.VARIABLE_GROUP));
		if (isInView()){
			menu.add(getAction(DETAIL_ASSIGN_VALUE_ACTION));
			menu.add(getAction(DETAIL_CONTENT_ASSIST_ACTION));
		}
		menu.add(new Separator());
		menu.add(getAction(DETAIL_CUT_ACTION));
		menu.add(getAction(DETAIL_COPY_ACTION));
		menu.add(getAction(DETAIL_PASTE_ACTION));
		menu.add(getAction(DETAIL_SELECT_ALL_ACTION));
		menu.add(new Separator("FIND")); //$NON-NLS-1$
		if (isInView()){
			menu.add(getAction(DETAIL_FIND_REPLACE_TEXT_ACTION));
		}
		menu.add(new Separator());
		menu.add(getAction(DETAIL_WORD_WRAP_ACTION));
		menu.add(getAction(DETAIL_MAX_LENGTH_ACTION));
		menu.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

	}

	@Override
	public void display(IStructuredSelection selection) {

		if (selection == null){
			clearSourceViewer();
			return;
		}

		fLastDisplayed = selection;
		if (isInView()){
			fSourceViewer.setEditable(true);
		}

		if (selection.isEmpty()){
			clearSourceViewer();
			return;
		}

		Object firstElement = selection.getFirstElement();
		if (firstElement != null && firstElement instanceof IDebugElement) {
			String modelID = ((IDebugElement)firstElement).getModelIdentifier();
			setDebugModel(modelID);
		}

		if (isInView()){
			IAction assignAction = getAction(DETAIL_ASSIGN_VALUE_ACTION);
			if (assignAction instanceof DetailPaneAssignValueAction){
				((DetailPaneAssignValueAction)assignAction).updateCurrentVariable(selection);
			}
		}

		synchronized (this) {
			if (fDetailJob != null) {
				fDetailJob.cancel();
			}
			fDetailJob = new DetailJob(selection,fModelPresentation);
			fDetailJob.schedule();
		}

	}

	@Override
	public boolean setFocus(){
		if (fSourceViewer != null){
			fSourceViewer.getTextWidget().setFocus();
			return true;
		}
		return false;
	}

	@Override
	public void dispose(){
		if (fDetailJob != null) {
			fDetailJob.cancel();
		}
		if (fModelPresentation != null) {
			fModelPresentation.dispose();
		}
		fDebugModelIdentifier = null; // Setting this to null makes sure the source viewer is reconfigured with the model presentation after disposal
		if (fSourceViewer != null && fSourceViewer.getControl() != null) {
			removePreferenceListener();
			disposeWhitespaceCharacterPainter();
			fSourceViewer.getControl().dispose();
		}

		if (isInView()){
			IAction action = getAction(DETAIL_ASSIGN_VALUE_ACTION);
			if (action != null){
				((DetailPaneAssignValueAction)action).dispose();
			}
			if (fContentAssistActivation != null){
				IHandlerService service = getViewSite().getService(IHandlerService.class);
				service.deactivateHandler(fContentAssistActivation);
				fContentAssistActivation = null;
			}

			disposeUndoRedoAction(ITextEditorActionConstants.UNDO);
			disposeUndoRedoAction(ITextEditorActionConstants.REDO);

			getViewSite().getActionBars().getStatusLineManager().remove(fStatusLineItem);

			DebugUIPlugin.getDefault().getPreferenceStore().removePropertyChangeListener(this);
			JFaceResources.getFontRegistry().removeListener(this);
		}

		super.dispose();
	}

	@Override
	public String getDescription() {
		return DESCRIPTION;
	}

	@Override
	public String getID() {
		return ID;
	}

	@Override
	public String getName() {
		return NAME;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> required) {
		if (IFindReplaceTarget.class.equals(required)) {
			return (T) fSourceViewer.getFindReplaceTarget();
		}
		if (ITextViewer.class.equals(required)) {
			return (T) fSourceViewer;
		}
		return null;
	}

	/**
	 * Lazily instantiate and return a Document for the detail pane text viewer.
	 * @return the singleton {@link Document} for this detail pane
	 */
	protected IDocument getDetailDocument() {
		if (fDetailDocument == null) {
			fDetailDocument = new Document();
		}
		return fDetailDocument;
	}

	/**
	 * Clears the source viewer, removes all text.
	 */
	protected void clearSourceViewer(){
		if (fDetailJob != null) {
			fDetailJob.cancel();
		}
		fLastDisplayed = null;
		fDetailDocument.set(IInternalDebugCoreConstants.EMPTY_STRING);
		fSourceViewer.setEditable(false);
	}

	/**
	 * Configures the details viewer for the debug model
	 * currently being displayed
	 */
	protected void configureDetailsViewer() {
		LazyModelPresentation mp = (LazyModelPresentation)fModelPresentation.getPresentation(getDebugModel());
		SourceViewerConfiguration svc = null;
		if (mp != null) {
			try {
				svc = mp.newDetailsViewerConfiguration();
			} catch (CoreException e) {
				DebugUIPlugin.errorDialog(fSourceViewer.getControl().getShell(), DetailMessages.DefaultDetailPane_Error_1, DetailMessages.DefaultDetailPane_2, e);
			}
		}

		if (svc == null) {
			svc = new SourceViewerConfiguration();
			fSourceViewer.setEditable(false);
		}
		fSourceViewer.unconfigure();
		fSourceViewer.configure(svc);
		//update actions that depend on the configuration of the source viewer

		if (isInView()){
			updateAction(DETAIL_ASSIGN_VALUE_ACTION);
			updateAction(DETAIL_CONTENT_ASSIST_ACTION);
		}

		if (isInView()){
			createUndoRedoActions();
		}
	}

	/**
	 * @return The formatted string describing cursor position
	 */
	protected String getCursorPosition() {

		if (fSourceViewer == null) {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}

		StyledText styledText= fSourceViewer.getTextWidget();
		int caret= styledText.getCaretOffset();
		IDocument document= fSourceViewer.getDocument();

		if (document == null) {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}

		try {

			int line= document.getLineOfOffset(caret);

			int lineOffset= document.getLineOffset(line);
			int tabWidth= styledText.getTabs();
			int column= 0;
			for (int i= lineOffset; i < caret; i++) {
				if ('\t' == document.getChar(i)) {
					column += tabWidth - (tabWidth == 0 ? 0 : column % tabWidth);
				} else {
					column++;
				}
			}

			fLineLabel.fValue= line + 1;
			fColumnLabel.fValue= column + 1;
			return MessageFormat.format(fPositionLabelPattern, fPositionLabelPatternArguments);

		} catch (BadLocationException x) {
			return IInternalDebugCoreConstants.EMPTY_STRING;
		}
	}

	/**
	 * Returns this view's "cursor" listener to be installed on the view's
	 * associated details viewer. This listener is listening to key and mouse button events.
	 * It triggers the updating of the status line.
	 *
	 * @return the listener
	 */
	private ICursorListener getCursorListener() {
		if (fCursorListener == null) {
			fCursorListener= new ICursorListener() {

				@Override
				public void keyPressed(KeyEvent e) {
					fStatusLineItem.setText(getCursorPosition());
				}

				@Override
				public void keyReleased(KeyEvent e) {
				}

				@Override
				public void mouseDoubleClick(MouseEvent e) {
				}

				@Override
				public void mouseDown(MouseEvent e) {
				}

				@Override
				public void mouseUp(MouseEvent e) {
					fStatusLineItem.setText(getCursorPosition());
				}
			};
		}
		return fCursorListener;
	}

	/**
	 * Returns the identifier of the debug model being displayed
	 * in this view, or <code>null</code> if none.
	 *
	 * @return debug model identifier
	 */
	protected String getDebugModel() {
		return fDebugModelIdentifier;
	}

	/**
	 * Sets the identifier of the debug model being displayed
	 * in this view, or <code>null</code> if none.
	 *
	 * @param id debug model identifier of the type of debug
	 *  elements being displayed in this view
	 */
	protected void setDebugModel(String id) {
		if (id != fDebugModelIdentifier) {
			fDebugModelIdentifier = id;
			configureDetailsViewer();
		}
	}

	/**
	 * Creates this editor's undo/re-do actions.
	 * <p>
	 * Subclasses may override or extend.</p>
	 *
	 * @since 3.2
	 */
	protected void createUndoRedoActions() {
		disposeUndoRedoAction(ITextEditorActionConstants.UNDO);
		disposeUndoRedoAction(ITextEditorActionConstants.REDO);
		IUndoContext undoContext= getUndoContext();
		if (undoContext != null) {
			// Use actions provided by global undo/re-do

			// Create the undo action
			OperationHistoryActionHandler undoAction= new UndoActionHandler(getViewSite(), undoContext);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(undoAction, IAbstractTextEditorHelpContextIds.UNDO_ACTION);
			undoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_UNDO);
			setAction(ITextEditorActionConstants.UNDO, undoAction);
			setGlobalAction(ITextEditorActionConstants.UNDO, undoAction);

			// Create the re-do action.
			OperationHistoryActionHandler redoAction= new RedoActionHandler(getViewSite(), undoContext);
			PlatformUI.getWorkbench().getHelpSystem().setHelp(redoAction, IAbstractTextEditorHelpContextIds.REDO_ACTION);
			redoAction.setActionDefinitionId(IWorkbenchCommandConstants.EDIT_REDO);
			setAction(ITextEditorActionConstants.REDO, redoAction);
			setGlobalAction(ITextEditorActionConstants.REDO, redoAction);

			getViewSite().getActionBars().updateActionBars();
		}
	}

	/**
	 * Disposes of the action with the specified ID
	 *
	 * @param actionId the ID of the action to disposed
	 */
	protected void disposeUndoRedoAction(String actionId) {
		OperationHistoryActionHandler action = (OperationHistoryActionHandler) getAction(actionId);
		if (action != null) {
			action.dispose();
			setAction(actionId, null);
		}
	}

	/**
	 * Returns this editor's viewer's undo manager undo context.
	 *
	 * @return the undo context or <code>null</code> if not available
	 * @since 3.2
	 */
	private IUndoContext getUndoContext() {
		IUndoManager undoManager= fSourceViewer.getUndoManager();
		if (undoManager instanceof IUndoManagerExtension) {
			return ((IUndoManagerExtension)undoManager).getUndoContext();
		}
		return null;
	}

	@Override
	public void propertyChange(PropertyChangeEvent event) {
		String propertyName= event.getProperty();
		if (propertyName.equals(IDebugUIConstants.PREF_DETAIL_PANE_FONT)) {
			fSourceViewer.getTextWidget().setFont(JFaceResources.getFont(IDebugUIConstants.PREF_DETAIL_PANE_FONT));
		} else if (propertyName.equals(IDebugUIConstants.PREF_MAX_DETAIL_LENGTH)) {
			display(fLastDisplayed);
		} else if (propertyName.equals(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP)) {
			fSourceViewer.getTextWidget().setWordWrap(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP));
			getAction(DETAIL_WORD_WRAP_ACTION).setChecked(DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugPreferenceConstants.PREF_DETAIL_PANE_WORD_WRAP));
		}

	}

	/**
	 * Wrapper class that wraps around an IFindReplaceTarget.  Allows the detail pane to scroll
	 * to text selected by the find/replace action.  The source viewer treats the text as a single
	 * line, even when the text is wrapped onto several lines so the viewer will not scroll properly
	 * on it's own.  See bug 178106.
	 */
	class FindReplaceTargetWrapper implements IFindReplaceTarget{

		private IFindReplaceTarget fTarget;

		/**
		 * Constructor
		 *
		 * @param target find/replace target this class will wrap around.
		 */
		public FindReplaceTargetWrapper(IFindReplaceTarget target){
			fTarget = target;
		}

		@Override
		public boolean canPerformFind() {
			return fTarget.canPerformFind();
		}

		@Override
		public int findAndSelect(int widgetOffset, String findString, boolean searchForward, boolean caseSensitive, boolean wholeWord) {
			int position = fTarget.findAndSelect(widgetOffset, findString, searchForward, caseSensitive, wholeWord);
			// Explicitly tell the widget to show the selection because the viewer thinks the text is all on one line, even if wrapping is turned on.
			if (fSourceViewer != null){
				StyledText text = fSourceViewer.getTextWidget();
				if(text != null && !text.isDisposed()) {
					text.showSelection();
				}
			}
			return position;
		}

		@Override
		public Point getSelection() {
			return fTarget.getSelection();
		}

		@Override
		public String getSelectionText() {
			return fTarget.getSelectionText();
		}

		@Override
		public boolean isEditable() {
			return fTarget.isEditable();
		}

		@Override
		public void replaceSelection(String text) {
			fTarget.replaceSelection(text);
		}
	}

	@Override
	public ISelectionProvider getSelectionProvider() {
		return fSourceViewer.getSelectionProvider();
	}

	/**
	 * @return the fSourceViewer
	 */
	protected SourceViewer getSourceViewer() {
		return fSourceViewer;
	}
}
