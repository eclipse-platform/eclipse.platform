/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
 *     Stefan Dirix (sdirix@eclipsesource.com) - Bug 473847: Minimum E4 Compatibility of Compare
 *******************************************************************************/
package org.eclipse.compare;

import java.lang.reflect.InvocationTargetException;
import java.util.ResourceBundle;

import org.eclipse.compare.contentmergeviewer.ContentMergeViewer;
import org.eclipse.compare.contentmergeviewer.IFlushable;
import org.eclipse.compare.internal.BinaryCompareViewer;
import org.eclipse.compare.internal.ChangePropertyAction;
import org.eclipse.compare.internal.CompareContentViewerSwitchingPane;
import org.eclipse.compare.internal.CompareEditorInputNavigator;
import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.ComparePreferencePage;
import org.eclipse.compare.internal.CompareStructureViewerSwitchingPane;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.ICompareUIConstants;
import org.eclipse.compare.internal.IFlushable2;
import org.eclipse.compare.internal.OutlineViewerCreator;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.internal.ViewerDescriptor;
import org.eclipse.compare.structuremergeviewer.DiffTreeViewer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.compare.structuremergeviewer.IDiffContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditorExtension3;

/**
 * A compare operation which can present its results in a special editor.
 * Running the compare operation and presenting the results in a compare editor
 * are combined in one class because it allows a client to keep the implementation
 * all in one place while separating it from the innards of a specific UI implementation of compare/merge.
 * <p>
 * A <code>CompareEditorInput</code> defines methods for the following sequence steps:
 * <UL>
 * <LI>running a lengthy compare operation under progress monitor control,
 * <LI>creating a UI for displaying the model and initializing the some widgets with the compare result,
 * <LI>tracking the dirty state of the model in case of merge,
 * <LI>saving the model.
 * </UL>
 * The Compare plug-in's <code>openCompareEditor</code> method takes an <code>CompareEditorInput</code>
 * and starts sequencing through the above steps. If the compare result is not empty a new compare editor
 * is opened and takes over the sequence until eventually closed.
 * <p>
 * The <code>prepareInput</code> method should contain the
 * code of the compare operation. It is executed under control of a progress monitor
 * and can be canceled. If the result of the compare is not empty, that is if there are differences
 * that needs to be presented, the <code>ICompareInput</code> should hold onto them and return them with
 * the <code>getCompareResult</code> method.
 * If the value returned from <code>getCompareResult</code> is not <code>null</code>
 * a compare editor is opened on the <code>ICompareInput</code> with title and title image initialized by the
 * corresponding methods of the <code>ICompareInput</code>.
 * <p>
 * Creation of the editor's SWT controls is delegated to the <code>createContents</code> method.
 * Here the SWT controls must be created and initialized  with the result of the compare operation.
 * <p>
 * If merging is allowed, the modification state of the compared constituents must be tracked and the dirty
 * state returned from method <code>isSaveNeeded</code>. The value <code>true</code> triggers a subsequent call
 * to <code>save</code> where the modified resources can be saved.
 * <p>
 * The most important part of this implementation is the setup of the compare/merge UI.
 * The UI uses a simple browser metaphor to present compare results.
 * The top half of the layout shows the structural compare results (e.g. added, deleted, and changed files),
 * the bottom half the content compare results (e.g. textual differences between two files).
 * A selection in the top pane is fed to the bottom pane. If a content viewer is registered
 * for the type of the selected object, this viewer is installed in the pane.
 * In addition if a structure viewer is registered for the selection type the top pane
 * is split vertically to make room for another pane and the structure viewer is installed
 * in it. When comparing Java files this second structure viewer would show the structural
 * differences within a Java file, e.g. added, deleted or changed methods and fields.
 * <p>
 * Subclasses provide custom setups, e.g. for a Catch-up/Release operation
 * by passing a subclass of <code>CompareConfiguration</code> and by implementing the <code>prepareInput</code> method.
 * If a subclass cannot use the <code>DiffTreeViewer</code> which is installed by default in the
 * top left pane, method <code>createDiffViewer</code> can be overridden.
 * <p>
 * If subclasses of this class implement {@link ISaveablesSource}, the compare editor will
 * pass these models through to the workbench. The editor will still show the dirty indicator
 * if one of these underlying models is dirty. It is the responsibility of subclasses that
 * implement this interface to call {@link #setDirty(boolean)} when the dirty state of
 * any of the models managed by the subclass change dirty state.
 *
 * @see CompareUI
 * @see CompareEditorInput
 */
public abstract class CompareEditorInput extends PlatformObject implements IEditorInput, IPropertyChangeNotifier, IRunnableWithProgress, ICompareContainer {
	private static final boolean DEBUG= false;

	/**
	 * The name of the "dirty" property (value <code>"DIRTY_STATE"</code>).
	 */
	public static final String DIRTY_STATE= "DIRTY_STATE"; //$NON-NLS-1$

	/**
	 * The name of the "title" property. This property is fired when the title
	 * of the compare input changes. Clients should also re-obtain the tool tip
	 * when this property changes.
	 * @see #getTitle()
	 * @since 3.3
	 */
	public static final String PROP_TITLE= ICompareUIConstants.PROP_TITLE;

	/**
	 * The name of the "title image" property. This property is fired when the title
	 * image of the compare input changes.
	 * @see #getTitleImage()
	 * @since 3.3
	 */
	public static final String PROP_TITLE_IMAGE= ICompareUIConstants.PROP_TITLE_IMAGE;

	/**
	 * The name of the "selected edition" property. This property is fired when the selected
	 * edition of the compare input changes.
	 * @see #isEditionSelectionDialog()
	 * @see #getSelectedEdition()
	 * @since 3.3
	 */
	public static final String PROP_SELECTED_EDITION= ICompareUIConstants.PROP_SELECTED_EDITION;

	private static final String COMPARE_EDITOR_IMAGE_NAME= "eview16/compare_view.png"; //$NON-NLS-1$
	private static Image fgTitleImage;

	private Splitter fComposite;
	private CompareConfiguration fCompareConfiguration;
	private CompareViewerPane fStructureInputPane;
	private CompareViewerSwitchingPane fStructurePane1;
	private CompareViewerSwitchingPane fStructurePane2;
	private CompareViewerSwitchingPane fContentInputPane;
	private CompareViewerPane fFocusPane;
	private String fMessage;
	private Object fInput;
	private String fTitle;
	private ListenerList<IPropertyChangeListener> fListenerList= new ListenerList<>();
	private CompareNavigator fNavigator;
	private boolean fLeftDirty;
	private boolean fRightDirty;
	private IPropertyChangeListener fDirtyStateListener;

	boolean fStructureCompareOnSingleClick= true;

	private ICompareContainer fContainer;
	private boolean fContainerProvided;
	private String fHelpContextId;
	private InternalOutlineViewerCreator fOutlineView;
	private ViewerDescriptor fContentViewerDescriptor;
	private ViewerDescriptor fStructureViewerDescriptor;

	private class InternalOutlineViewerCreator extends OutlineViewerCreator {
		private OutlineViewerCreator getWrappedCreator() {
			if (fContentInputPane != null) {
				Viewer v = fContentInputPane.getViewer();
				if (v != null) {
					return Adapters.adapt(v, OutlineViewerCreator.class);
				}
			}
			return null;
		}

		@Override
		public Viewer findStructureViewer(Viewer oldViewer,	ICompareInput input, Composite parent,
				CompareConfiguration configuration) {
			OutlineViewerCreator creator = getWrappedCreator();
			if (creator != null)
				return creator.findStructureViewer(oldViewer, input, parent, configuration);
			return null;
		}

		@Override
		public boolean hasViewerFor(Object input) {
			OutlineViewerCreator creator = getWrappedCreator();
			return creator != null;
		}

		@Override
		public Object getInput() {
			OutlineViewerCreator creator = getWrappedCreator();
			if (creator != null)
				return creator.getInput();
			return null;
		}
	}

	/**
	 * Creates a <code>CompareEditorInput</code> which is initialized with the given
	 * compare configuration.
	 * The compare configuration is passed to subsequently created viewers.
	 *
	 * @param configuration the compare configuration
	 */
	public CompareEditorInput(CompareConfiguration configuration) {
		fCompareConfiguration= configuration;
		Assert.isNotNull(configuration);

		fDirtyStateListener= e -> {
			String propertyName= e.getProperty();
			if (CompareEditorInput.DIRTY_STATE.equals(propertyName)) {
				boolean changed= false;
				Object newValue= e.getNewValue();
				if (newValue instanceof Boolean)
					changed= ((Boolean) newValue).booleanValue();
				setDirty(e.getSource(), changed);
			}
		};

		IPreferenceStore ps= configuration.getPreferenceStore();
		if (ps != null)
			fStructureCompareOnSingleClick= ps.getBoolean(ComparePreferencePage.OPEN_STRUCTURE_COMPARE);

		fContainer = configuration.getContainer();
		configuration.setContainer(this);
	}

	private boolean structureCompareOnSingleClick() {
		return fStructureCompareOnSingleClick;
	}

	private boolean isShowStructureInOutlineView() {
		Object object= getCompareConfiguration().getProperty(CompareConfiguration.USE_OUTLINE_VIEW);
		return object instanceof Boolean && ((Boolean)object).booleanValue();
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (ICompareNavigator.class.equals(adapter) || CompareNavigator.class.equals(adapter)) {
			return (T) getNavigator();
		}
		if (adapter == IShowInSource.class) {
			final IFile file = Adapters.adapt(this, IFile.class);
			if (file != null)
				return (T) (IShowInSource) () -> new ShowInContext(new FileEditorInput(file), StructuredSelection.EMPTY);
		}
		if (adapter == OutlineViewerCreator.class) {
			synchronized (this) {
				if (fOutlineView == null)
					fOutlineView = new InternalOutlineViewerCreator();
				return (T) fOutlineView;
			}
		}
		if (adapter == IFindReplaceTarget.class) {
			if (fContentInputPane != null) {
				Viewer v = fContentInputPane.getViewer();
				if (v != null) {
					return (T) Adapters.adapt(v, IFindReplaceTarget.class);
				}
			}
		}
		if (adapter == IEditorInput.class) {
			if (fContentInputPane != null) {
				Viewer v = fContentInputPane.getViewer();
				if (v != null) {
					return (T) Adapters.adapt(v, IEditorInput.class);
				}
			}
		}

		if (adapter == ITextEditorExtension3.class) {
			if (fContentInputPane != null) {
				Viewer v = fContentInputPane.getViewer();
				if (v != null) {
					return (T) Adapters.adapt(v, ITextEditorExtension3.class);
				}
			}
		}

		return super.getAdapter(adapter);
	}

	@Override
	public synchronized ICompareNavigator getNavigator() {
		if (fNavigator == null)
			fNavigator= new CompareEditorInputNavigator(
				new Object[] {
					fStructureInputPane,
					fStructurePane1,
					fStructurePane2,
					fContentInputPane
				}
			);
		return fNavigator;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getToolTipText() {
		return getTitle();
	}

	@Override
	public String getName() {
		return getTitle();
	}

	/**
	 * Returns <code>null</code> since this editor cannot be persisted.
	 *
	 * @return <code>null</code> because this editor cannot be persisted
	 */
	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	/**
	 * Returns <code>false</code> to indicate that this input
	 * should not appear in the "File Most Recently Used" menu.
	 *
	 * @return <code>false</code>
	 */
	@Override
	public boolean exists() {
		return false;
	}

	/*
	 * FIXME!
	 */
	protected void setMessage(String message) {
		fMessage= message;
	}

	/*
	 * FIXME!
	 */
	public String getMessage() {
		return fMessage;
	}

	/**
	 * Returns the title which will be used in the compare editor's title bar.
	 * It can be set with <code>setTitle</code>.
	 *
	 * @return the title
	 */
	public String getTitle() {
		if (fTitle == null)
			return Utilities.getString("CompareEditorInput.defaultTitle"); //$NON-NLS-1$
		return fTitle;
	}

	/**
	 * Sets the title which will be used when presenting the compare result.
	 * This method must be called before the editor is opened.
	 *
	 * @param title the title to use for the CompareEditor
	 */
	public void setTitle(String title) {
		String oldTitle = fTitle;
		fTitle= title;
		Utilities.firePropertyChange(fListenerList, this, PROP_TITLE, oldTitle, title);
	}

	/**
	 * Returns the title image which will be used in the compare editor's title bar.
	 * Returns the title image which will be used when presenting the compare result.
	 * This implementation returns a generic compare icon.
	 * Subclasses can override.
	 *
	 * @return the title image, or <code>null</code> if none
	 */
	public Image getTitleImage() {
		if (fgTitleImage == null) {
			fgTitleImage= CompareUIPlugin.getImageDescriptor(COMPARE_EDITOR_IMAGE_NAME).createImage();
			CompareUI.disposeOnShutdown(fgTitleImage);
		}
		return fgTitleImage;
	}

	/**
	 * Returns the configuration object for the viewers within the compare editor.
	 * Returns the configuration which was passed to the constructor.
	 *
	 * @return the compare configuration
	 */
	public CompareConfiguration getCompareConfiguration() {
		return fCompareConfiguration;
	}

	/**
	 * Adds standard actions to the given <code>ToolBarManager</code>.
	 * <p>
	 * Subclasses may override to add their own actions.
	 * </p>
	 *
	 * @param toolBarManager the <code>ToolBarManager</code> to which to contribute
	 */
	public void contributeToToolBar(ToolBarManager toolBarManager) {
		ResourceBundle bundle= CompareUI.getResourceBundle();
		ChangePropertyAction ignoreWhitespace= ChangePropertyAction.createIgnoreWhiteSpaceAction(bundle, getCompareConfiguration());
		toolBarManager.getControl().addDisposeListener(ignoreWhitespace);
		ChangePropertyAction showPseudoConflicts= ChangePropertyAction.createShowPseudoConflictsAction(bundle, getCompareConfiguration());
		toolBarManager.getControl().addDisposeListener(showPseudoConflicts);
		toolBarManager.add(new Separator());
		toolBarManager.add(ignoreWhitespace);
		toolBarManager.add(showPseudoConflicts);
	}

	/**
	 * Runs the compare operation and stores the compare result.
	 *
	 * @param monitor the progress monitor to use to display progress and receive
	 *     requests for cancellation
	 * @throws InvocationTargetException if the <code>prepareInput</code> method must propagate a checked exception,
	 * 	   it should wrap it inside an <code>InvocationTargetException</code>; runtime exceptions are automatically
	 *     wrapped in an <code>InvocationTargetException</code> by the calling context
	 * @throws InterruptedException if the operation detects a request to cancel,
	 *     using <code>IProgressMonitor.isCanceled()</code>, it should exit by throwing
	 *     <code>InterruptedException</code>
	 */
	@Override
	public void run(IProgressMonitor monitor) throws InterruptedException, InvocationTargetException {
		fInput= prepareInput(monitor);
	}

	/**
	 * Runs the compare operation and returns the compare result.
	 * If <code>null</code> is returned no differences were found and no compare editor needs to be opened.
	 * Progress should be reported to the given progress monitor.
	 * A request to cancel the operation should be honored and acknowledged
	 * by throwing <code>InterruptedException</code>.
	 * <p>
	 * Note: this method is typically called in a modal context thread which doesn't have a Display assigned.
	 * Implementors of this method shouldn't therefore allocated any SWT resources in this method.
	 * </p>
	 *
	 * @param monitor the progress monitor to use to display progress and receive
	 *   requests for cancellation
	 * @return the result of the compare operation, or <code>null</code> if there are no differences
	 * @exception InvocationTargetException if the <code>prepareInput</code> method must propagate a checked exception,
	 * 	it should wrap it inside an <code>InvocationTargetException</code>; runtime exceptions are automatically
	 *  wrapped in an <code>InvocationTargetException</code> by the calling context
	 * @exception InterruptedException if the operation detects a request to cancel,
	 *  using <code>IProgressMonitor.isCanceled()</code>, it should exit by throwing
	 *  <code>InterruptedException</code>
	 */
	protected abstract Object prepareInput(IProgressMonitor monitor)
				throws InvocationTargetException, InterruptedException;

	/**
	 * Returns the compare result computed by the most recent call to the
	 * <code>run</code> method. Returns <code>null</code> if no
	 * differences were found.
	 *
	 * @return the compare result prepared in method <code>prepareInput</code>
	 *     or <code>null</code> if there were no differences
	 */
	public Object getCompareResult() {
		return fInput;
	}

	/**
	 * Creates the SWT controls that are used to display the result of the compare operation.
	 * Creates the SWT Controls and sets up the wiring between the individual panes.
	 * This implementation creates all four panes but makes only the necessary ones visible.
	 * Finally it feeds the compare result into the top left structure viewer
	 * and the content viewer.
	 * <p>
	 * Subclasses may override if they need to change the layout or wiring between panes.
	 *
	 * @param parent the parent control under which the control must be created
	 * @return the SWT control hierarchy for the compare editor
	 */
	public Control createContents(Composite parent) {
		fComposite= new Splitter(parent, SWT.VERTICAL);
		fComposite.setData(this);

		Control outline= createOutlineContents(fComposite, SWT.HORIZONTAL);

		fContentInputPane= createContentViewerSwitchingPane(fComposite, SWT.BORDER | SWT.FLAT, this);

		if (fFocusPane == null)
			fFocusPane= fContentInputPane;
		if (outline != null)
			fComposite.setVisible(outline, false);
		fComposite.setVisible(fContentInputPane, true);

		if (fStructureInputPane != null && fComposite.getChildren().length == 2)
			fComposite.setWeights(new int[] { 30, 70 });

		feedInput();
		parent.requestLayout();
		fComposite.addDisposeListener(e -> {
			/*
			 * When the UI associated with this compare editor input is
			 * disposed each composite being part of the UI releases its
			 * children first. A dispose listener is added to the last
			 * widget found in that structure. Therefore, compare editor
			 * input is disposed at the end making it possible to refer
			 * during widgets disposal.
			 */
			Composite composite = (Composite) e.widget;
			Control control = composite;
			while (composite.getChildren().length > 0) {
				control = composite.getChildren()[composite.getChildren().length - 1];
				if (control instanceof Composite) {
					composite = (Composite) control;
				} else {
					break;
				}
			}
			control.addDisposeListener(ev -> handleDispose());
		});
		if (fHelpContextId != null && PlatformUI.isWorkbenchRunning())
			PlatformUI.getWorkbench().getHelpSystem().setHelp(fComposite, fHelpContextId);
		contentsCreated();
		return fComposite;
	}

	/**
	 * @param parent the parent control under which the control must be created
	 * @param style  the style of widget to construct
	 * @param cei the compare editor input for the viewer
	 * @return the pane displaying content changes
	 * @nooverride This method is not intended to be re-implemented or extended by clients.
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected CompareViewerSwitchingPane createContentViewerSwitchingPane(Splitter parent, int style, CompareEditorInput cei) {
		return new CompareContentViewerSwitchingPane(parent, style, cei);
	}

	/**
	 * Callback that occurs when the UI associated with this compare editor
	 * input is disposed. This method will only be invoked if the UI has been
	 * created (i.e. after the call to {@link #createContents(Composite)}.
	 * Subclasses can extend this method but ensure that the overridden method
	 * is invoked.
	 *
	 * @since 3.3
	 */
	protected void handleDispose() {
		fContainerProvided = false;
		fContainer = null;
		fComposite = null;
		fStructureInputPane = null;
		fStructurePane1 = null;
		fStructurePane2 = null;
		fContentInputPane = null;
		fFocusPane = null;
		fNavigator = null;
		fCompareConfiguration.dispose();
	}

	/**
	 * Callback that occurs after the control for the input has
	 * been created. If this method gets invoked then {@link #handleDispose()}
	 * will be invoked when the control is disposed. Subclasses may extend this
	 * method to register any listeners that need to be de-registered when the
	 * input is disposed.
	 * @since 3.3
	 */
	protected void contentsCreated() {
		// Default is to do nothing
	}

	/**
	 * @param parent    the parent control under which the control must be created
	 * @param direction the layout direction of the contents, either
	 *                  <code>SWT.HORIZONTAL</code> or <code>SWT.VERTICAL</code>
	 * @return the SWT control hierarchy for the outline part of the compare editor
	 * @since 3.0
	 */
	public Control createOutlineContents(Composite parent, int direction) {
		final Splitter h= new Splitter(parent, direction);

		fStructureInputPane= createStructureInputPane(h);
		if (hasChildren(getCompareResult()))
			fFocusPane= fStructureInputPane;

		fStructurePane1= new CompareStructureViewerSwitchingPane(h, SWT.BORDER | SWT.FLAT, true, this);
		h.setVisible(fStructurePane1, false);

		fStructurePane2= new CompareStructureViewerSwitchingPane(h, SWT.BORDER | SWT.FLAT, true, this);
		h.setVisible(fStructurePane2, false);

		// setup the wiring for top left pane
		fStructureInputPane.addOpenListener(
			oe -> feed1(oe.getSelection())
		);
		fStructureInputPane.addSelectionChangedListener(
			new ISelectionChangedListener() {
				@Override
				public void selectionChanged(SelectionChangedEvent e) {
					ISelection s= e.getSelection();
					if (s == null || s.isEmpty())
						feed1(s);
					if (isEditionSelectionDialog())
						firePropertyChange(new PropertyChangeEvent(this, PROP_SELECTED_EDITION, null, getSelectedEdition()));
				}
			}
		);
		fStructureInputPane.addDoubleClickListener(
			event -> feedDefault1(event.getSelection())
		);

		fStructurePane1.addSelectionChangedListener(
			e -> feed2(e.getSelection())
		);

		fStructurePane2.addSelectionChangedListener(
			e -> feed3(e.getSelection())
		);

		return h;
	}

	/**
	 * Create the pane that will contain the structure input pane (upper left).
	 * By default, a {@link CompareViewerSwitchingPane} is returned. Subclasses
	 * may override to provide an alternate pane.
	 * @param parent the parent composite
	 * @return the structure input pane
	 * @since 3.3
	 */
	protected CompareViewerPane createStructureInputPane(final Composite parent) {
		return new CompareStructureViewerSwitchingPane(parent, SWT.BORDER | SWT.FLAT, true, this) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				if (CompareEditorInput.this.hasChildren(input)) {
					return createDiffViewer(this);
				}
				return super.getViewer(oldViewer, input);
			}
		};
	}

	boolean hasChildren(Object input) {
		if (input instanceof IDiffContainer) {
			IDiffContainer dn= (IDiffContainer) input;
			return dn.hasChildren();
		}
		return false;
	}

	private void feedInput() {
		if (fStructureInputPane != null
				&& (fInput instanceof ICompareInput
						|| isCustomStructureInputPane())) {
			if (hasChildren(fInput) || isCustomStructureInputPane()) {
				// The input has multiple entries so set the input of the structure input pane
				fStructureInputPane.setInput(fInput);
			} else if (!structureCompareOnSingleClick() || isShowStructureInOutlineView()) {
				// We want to avoid showing the structure in the editor if we can so first
				// we'll set the content pane to see if we need to provide a structure
				internalSetContentPaneInput(fInput);
				// If the content viewer is unusable
				if (hasUnusableContentViewer()
						|| (structureCompareOnSingleClick()
								&& isShowStructureInOutlineView()
								&& !hasOutlineViewer(fInput))) {
					fStructureInputPane.setInput(fInput);
				}
			} else {
				fStructureInputPane.setInput(fInput);
			}
			ISelection sel= fStructureInputPane.getSelection();
			if (sel == null || sel.isEmpty())
				feed1(sel);	// we only feed downstream viewers if the top left pane is empty
		}
	}

	private boolean hasOutlineViewer(Object input) {
		if (!isShowStructureInOutlineView())
			return false;
		OutlineViewerCreator creator = getAdapter(OutlineViewerCreator.class);
		if (creator != null)
			return creator.hasViewerFor(input);
		return false;
	}

	private boolean hasUnusableContentViewer() {
		return fContentInputPane.isEmpty() || fContentInputPane.getViewer() instanceof BinaryCompareViewer;
	}

	private boolean isCustomStructureInputPane() {
		return !(fStructureInputPane instanceof CompareViewerSwitchingPane);
	}

	private void feed1(final ISelection selection) {
		BusyIndicator.showWhile(fComposite.getDisplay(),
			() -> {
				if (selection == null || selection.isEmpty()) {
					Object input1= fStructureInputPane.getInput();
					if (input1 != null)
						internalSetContentPaneInput(input1);
					if (!Utilities.okToUse(fStructurePane1) || !Utilities.okToUse(fStructurePane2))
						return;
					fStructurePane2.setInput(null); // clear downstream pane
					fStructurePane1.setInput(null);
				} else {
					Object input2= getElement(selection);
					internalSetContentPaneInput(input2);
					if (!Utilities.okToUse(fStructurePane1) || !Utilities.okToUse(fStructurePane2))
						return;
					if (structureCompareOnSingleClick() || hasUnusableContentViewer())
						fStructurePane1.setInput(input2);
					fStructurePane2.setInput(null); // clear downstream pane
					if (fStructurePane1.getInput() != input2)
						fStructurePane1.setInput(null);
				}
			}
		);
	}

	private void feedDefault1(final ISelection selection) {
		BusyIndicator.showWhile(fComposite.getDisplay(),
			() -> {
				if (!selection.isEmpty())
					fStructurePane1.setInput(getElement(selection));
			}
		);
	}

	private void feed2(final ISelection selection) {
		BusyIndicator.showWhile(fComposite.getDisplay(),
			() -> {
				if (selection.isEmpty()) {
					Object input1= fStructurePane1.getInput();
					internalSetContentPaneInput(input1);
					fStructurePane2.setInput(null);
				} else {
					Object input2= getElement(selection);
					internalSetContentPaneInput(input2);
					fStructurePane2.setInput(input2);
				}
			}
		);
	}

	private void feed3(final ISelection selection) {
		BusyIndicator.showWhile(fComposite.getDisplay(),
			() -> {
				if (selection.isEmpty())
					internalSetContentPaneInput(fStructurePane2.getInput());
				else
					internalSetContentPaneInput(getElement(selection));
			}
		);

	}

	private void internalSetContentPaneInput(Object input) {
		Object oldInput = fContentInputPane.getInput();
		fContentInputPane.setInput(input);
		if (fOutlineView != null)
			fOutlineView.fireInputChange(oldInput, input);
	}

	/**
	 * Returns the first element of the given selection if the selection
	 * is a <code>IStructuredSelection</code> with exactly one element. Returns
	 * <code>null</code> otherwise.
	 *
	 * @param selection the selection
	 * @return the first element of the selection, or <code>null</code>
	 */
	private static Object getElement(ISelection selection) {
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss= (IStructuredSelection) selection;
			if (ss.size() == 1)
				return ss.getFirstElement();
		}
		return null;
	}

	/**
	 * Asks this input to take focus within its container (editor).
	 *
	 * @noreference Clients should not call this method but they may override if
	 *              they implement a different layout with different visual
	 *              components. Clients are free to call the inherited method.
	 *
	 * @deprecated Please use {@link #setFocus2()} instead.
	 */
	@Deprecated
	public void setFocus() {
		setFocus2();
	}

	/**
	 * Asks this input to take focus within its container (editor).
	 *
	 * @noreference Clients should not call this method but they may override if
	 *              they implement a different layout with different visual
	 *              components. Clients are free to call the inherited method.
	 *
	 * @return <code>true</code> if the input got focus, and <code>false</code>
	 *         if it was unable to.
	 * @since 3.5
	 */
	public boolean setFocus2() {
		if (fFocusPane != null) {
			return fFocusPane.setFocus();
		} else if (fComposite != null)
			return fComposite.setFocus();
		return false;
	}

	/**
	 * Factory method for creating a differences viewer for the top left pane.
	 * It is called from <code>createContents</code> and returns a <code>DiffTreeViewer</code>.
	 * <p>
	 * Subclasses may override if they need a different viewer.
	 * </p>
	 *
	 * @param parent the SWT parent control under which to create the viewer's SWT controls
	 * @return a compare viewer for the top left pane
	 */
	public Viewer createDiffViewer(Composite parent) {
		return new DiffTreeViewer(parent, fCompareConfiguration);
	}

	/**
	 * Implements the dynamic viewer switching for structure viewers.
	 * The method must return a compare viewer based on the old (or current) viewer
	 * and a new input object. If the old viewer is suitable for showing the new input the old viewer
	 * can be returned. Otherwise a new viewer must be created under the given parent composite or
	 * <code>null</code> can be returned to indicate that no viewer could be found.
	 * <p>
	 * This implementation forwards the request to <code>CompareUI.findStructureViewer</code>.
	 * <p>
	 * Subclasses may override to implement a different strategy.
	 * </p>
	 * @param oldViewer a new viewer is only created if this old viewer cannot show the given input
	 * @param input the input object for which to find a structure viewer
	 * @param parent the SWT parent composite under which the new viewer is created
	 * @return a compare viewer which is suitable for the given input object or <code>null</code>
	 */
	public Viewer findStructureViewer(Viewer oldViewer, ICompareInput input, Composite parent) {
		return fStructureViewerDescriptor != null ? fStructureViewerDescriptor.createViewer(oldViewer, parent,
				fCompareConfiguration) : CompareUI.findStructureViewer(oldViewer,
				input, parent, fCompareConfiguration);
	}

	/**
	 * Implements the dynamic viewer switching for content viewers.
	 * The method must return a compare viewer based on the old (or current) viewer
	 * and a new input object. If the old viewer is suitable for showing the new input the old viewer
	 * can be returned. Otherwise a new viewer must be created under the given parent composite or
	 * <code>null</code> can be returned to indicate that no viewer could be found.
	 * <p>
	 * This implementation forwards the request to <code>CompareUI.findContentViewer</code>.
	 * <p>
	 * Subclasses may override to implement a different strategy.
	 * </p>
	 * @param oldViewer a new viewer is only created if this old viewer cannot show the given input
	 * @param input the input object for which to find a structure viewer
	 * @param parent the SWT parent composite under which the new viewer is created
	 * @return a compare viewer which is suitable for the given input object or <code>null</code>
	 */
	public Viewer findContentViewer(Viewer oldViewer, ICompareInput input, Composite parent) {

		Viewer newViewer = fContentViewerDescriptor != null ? fContentViewerDescriptor.createViewer(oldViewer, parent,
				fCompareConfiguration) : CompareUI.findContentViewer(oldViewer,
				input, parent, fCompareConfiguration);

		boolean isNewViewer= newViewer != oldViewer;
		if (DEBUG) System.out.println("CompareEditorInput.findContentViewer: " + isNewViewer); //$NON-NLS-1$

		if (isNewViewer && newViewer instanceof IPropertyChangeNotifier) {
			final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
			dsp.addPropertyChangeListener(fDirtyStateListener);

			Control c= newViewer.getControl();
			c.addDisposeListener(
				e -> dsp.removePropertyChangeListener(fDirtyStateListener)
			);
		}

		return newViewer;
	}

	/**
	 * @param vd
	 *            the content viewer descriptor
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public void setContentViewerDescriptor(ViewerDescriptor vd) {
		this.fContentViewerDescriptor = vd;
	}

	/**
	 * @return the content viewer descriptor set for the input
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public ViewerDescriptor getContentViewerDescriptor() {
		return this.fContentViewerDescriptor;
	}

	/**
	 * @param vd
	 *            the structure viewer descriptor
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public void setStructureViewerDescriptor(ViewerDescriptor vd) {
		this.fStructureViewerDescriptor = vd;
	}

	/**
	 * @return the structure viewer descriptor set for the input
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	public ViewerDescriptor getStructureViewerDescriptor() {
		return this.fStructureViewerDescriptor;
	}

	/**
	 * Returns <code>true</code> if there are unsaved changes in either left or
	 * right side. The value returned is the value of the
	 * <code>DIRTY_STATE</code> property of this input object.
	 *
	 * Returns <code>true</code> if left or right side has unsaved changes
	 * Subclasses don't have to override if the functionality provided by
	 * <code>setDirty</code> is sufficient.
	 *
	 * @return <code>true</code> if there are changes that need to be saved
	 */
	public boolean isSaveNeeded() {
		return isLeftSaveNeeded() || isRightSaveNeeded();
	}

	/**
	 * Returns <code>true</code> if there are unsaved changes for left side.
	 *
	 * @return <code>true</code> if there are changes that need to be saved
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected boolean isLeftSaveNeeded() {
		return fLeftDirty;
	}

	/**
	 * Returns <code>true</code> if there are unsaved changes for right side.
	 *
	 * @return <code>true</code> if there are changes that need to be saved
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected boolean isRightSaveNeeded() {
		return fRightDirty;
	}

	/**
	 * Returns <code>true</code> if there are unsaved changes.
	 * The method should be called by any parts or dialogs
	 * that contain the input.
	 * By default, this method calls {@link #isSaveNeeded()}
	 * but subclasses may extend.
	 * @return <code>true</code> if there are unsaved changes
	 * @since 3.3
	 */
	public boolean isDirty() {
		return isSaveNeeded();
	}

	/**
	 * Sets the dirty state of this input to the given value and sends out a
	 * <code>PropertyChangeEvent</code> if the new value differs from the old
	 * value. Direct calling this method with parameter dirty equal to
	 * <code>false</code> when there are unsaved changes in viewers, results in
	 * inconsistent state. The dirty state of compare input should be based only
	 * on the information if there are changes in viewers for left or right
	 * side.
	 *
	 * @param dirty
	 *            the dirty state for this compare input
	 */
	public void setDirty(boolean dirty) {
		boolean oldDirty = isSaveNeeded();
		fLeftDirty = dirty;
		fRightDirty = dirty;

		if (oldDirty != isSaveNeeded()) {
			Utilities.firePropertyChange(fListenerList, this, DIRTY_STATE, Boolean.valueOf(oldDirty), Boolean.valueOf(isSaveNeeded()));
		}
	}

	/**
	 * Sets the dirty state of left site of this input to the given value and
	 * sends out a <code>PropertyChangeEvent</code> if the new value for whole
	 * input differs from the old value. Direct calling this method with
	 * parameter dirty equal to <code>false</code> when there are unsaved
	 * changes in left viewer, results in inconsistent state. The dirty state of
	 * compare input should be based only on the information if there are
	 * changes in viewers for left side.
	 *
	 * @param dirty
	 *            the dirty state for this compare input
	 * @since 3.7
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	protected void setLeftDirty(boolean dirty) {
		boolean oldDirty = isSaveNeeded();
		fLeftDirty = dirty;

		if (oldDirty != isSaveNeeded()) {
			Utilities.firePropertyChange(fListenerList, this, DIRTY_STATE,
					Boolean.valueOf(oldDirty), Boolean.valueOf(isSaveNeeded()));
		}
	}

	/**
	 * Sets the dirty state of right site of this input to the given value and
	 * sends out a <code>PropertyChangeEvent</code> if the new value for whole
	 * input differs from the old value. Direct calling this method with
	 * parameter dirty equal to <code>false</code> when there are unsaved
	 * changes in right viewer, results in inconsistent state. The dirty state
	 * of compare input should be based only on the information if there are
	 * changes in viewers for right side.
	 *
	 * @param dirty
	 *            the dirty state for this compare input
	 * @since 3.7
	 * @noreference This method is not intended to be referenced by clients.
	 * @nooverride This method is not intended to be re-implemented or extended
	 *             by clients.
	 */
	protected void setRightDirty(boolean dirty) {
		boolean oldDirty = isSaveNeeded();
		fRightDirty = dirty;

		if (oldDirty != isSaveNeeded()) {
			Utilities.firePropertyChange(fListenerList, this, DIRTY_STATE,
					Boolean.valueOf(oldDirty), Boolean.valueOf(isSaveNeeded()));
		}
	}

	/**
	 * Method adds or removes viewers that changed left or right side of this
	 * compare input. Any modification of any of the list of viewers may result
	 * in dirty state change.
	 *
	 * @param source
	 *            the object that fired <code>PropertyChangeEvent</code>
	 *            modifying the dirty state
	 * @param dirty
	 *            value that describes if the changes were added or removed
	 */
	private void setDirty(Object source, boolean dirty) {
		Assert.isNotNull(source);
		boolean oldDirty = isSaveNeeded();

		if (source instanceof ContentMergeViewer) {
			ContentMergeViewer cmv = (ContentMergeViewer) source;

			if (dirty == cmv.internalIsLeftDirty()) {
				fLeftDirty = cmv.internalIsLeftDirty();
			}

			if (dirty == cmv.internalIsRightDirty()) {
				fRightDirty = cmv.internalIsRightDirty();
			}
		} else {
			fLeftDirty = dirty;
		}

		boolean newDirty = isSaveNeeded();
		if (DEBUG) {
			System.out.println("setDirty(" + source + ", " + dirty + "): " + newDirty); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
		}
		if (oldDirty != newDirty) {
			Utilities.firePropertyChange(fListenerList, this, DIRTY_STATE, Boolean.valueOf(oldDirty), Boolean.valueOf(newDirty));
		}
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (listener != null)
			fListenerList.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fListenerList != null) {
			fListenerList.remove(listener);
		}
	}

	/**
	 * Saves any unsaved changes.
	 * Empty implementation.
	 * Subclasses must override to save any changes.
	 *
	 * @param pm an <code>IProgressMonitor</code> that the implementation of save may use to show progress
	 * @deprecated Override method saveChanges instead.
	 */
	@Deprecated
	public void save(IProgressMonitor pm) {
		// empty default implementation
	}

	/**
	 * Saves any unsaved changes. Subclasses must override to save any changes. This
	 * implementation tries to flush changes in all viewers by calling
	 * <code>ISavable.save</code> on them.
	 *
	 * @param monitor an <code>IProgressMonitor</code> that the implementation of
	 *                save may use to show progress
	 * @throws CoreException If save fails.
	 * @since 2.0
	 */
	public void saveChanges(IProgressMonitor monitor) throws CoreException {
		flushViewers(monitor);

		save(monitor);
	}

	/**
	 * Flushes the viewer contents into the input.
	 * @param monitor a progress monitor
	 * @since 3.3
	 */
	protected void flushViewers(IProgressMonitor monitor) {
		// flush changes in any dirty viewer
		flushViewer(fStructureInputPane, monitor);
		flushViewer(fStructurePane1, monitor);
		flushViewer(fStructurePane2, monitor);
		flushViewer(fContentInputPane, monitor);
	}

	/**
	 * @param monitor a progress monitor
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void flushLeftViewers(IProgressMonitor monitor) {
		// flush changes in left dirty viewer
		flushViewer(fStructureInputPane, monitor);
		flushViewer(fStructurePane1, monitor);
		flushViewer(fStructurePane2, monitor);
		flushLeftViewer(fContentInputPane, monitor);
	}

	/**
	 * @param monitor a progress monitor
	 * @noreference This method is not intended to be referenced by clients.
	 */
	protected void flushRightViewers(IProgressMonitor monitor) {
		// flush changes in right dirty viewer
		flushViewer(fStructureInputPane, monitor);
		flushViewer(fStructurePane1, monitor);
		flushViewer(fStructurePane2, monitor);
		flushRightViewer(fContentInputPane, monitor);
	}

	private static void flushViewer(CompareViewerPane pane, IProgressMonitor pm) {
		if (pane != null) {
			IFlushable flushable = Adapters.adapt(pane, IFlushable.class);
			if (flushable != null)
				flushable.flush(pm);
		}
	}

	private static void flushLeftViewer(CompareViewerPane pane, IProgressMonitor pm) {
		if (pane != null) {
			IFlushable2 flushable = Adapters.adapt(pane, IFlushable2.class);
			if (flushable != null)
				flushable.flushLeft(pm);
		}
	}

	private static void flushRightViewer(CompareViewerPane pane, IProgressMonitor pm) {
		if (pane != null) {
			IFlushable2 flushable = Adapters.adapt(pane, IFlushable2.class);
			if (flushable != null)
				flushable.flushRight(pm);
		}
	}

	@Override
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (fContainer == null) {
			input.addCompareInputChangeListener(listener);
		} else {
			fContainer.addCompareInputChangeListener(input, listener);
		}
	}

	@Override
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (fContainer == null) {
			input.removeCompareInputChangeListener(listener);
		} else {
			fContainer.removeCompareInputChangeListener(input, listener);
		}
	}

	@Override
	public void registerContextMenu(MenuManager menu, ISelectionProvider selectionProvider) {
		if (fContainer != null)
			fContainer.registerContextMenu(menu, selectionProvider);
	}

	@Override
	public void setStatusMessage(String message) {
		if (!fContainerProvided) {
			// Try the action bars directly
			IActionBars actionBars= getActionBars();
			if (actionBars != null) {
				IStatusLineManager slm= actionBars.getStatusLineManager();
				if (slm != null) {
					slm.setMessage(message);
				}
			}
		} else if (fContainer != null) {
			fContainer.setStatusMessage(message);
		}
	}

	@Override
	public IActionBars getActionBars() {
		if (fContainer != null) {
			IActionBars actionBars = fContainer.getActionBars();
			if (actionBars == null && !fContainerProvided) {
				// The old way to find the action bars
				return Utilities.findActionBars(fComposite);
			}
			return actionBars;
		}
		return null;
	}

	@Override
	public IServiceLocator getServiceLocator() {
		IServiceLocator serviceLocator = fContainer.getServiceLocator();
		if (serviceLocator == null && !fContainerProvided) {
			// The old way to find the service locator
			return Utilities.findSite(fComposite);
		}
		return serviceLocator;
	}

	@Override
	public IWorkbenchPart getWorkbenchPart() {
		if (fContainer != null)
			return fContainer.getWorkbenchPart();
		return null;
	}

	@Override
	public void run(boolean fork, boolean cancelable,
			IRunnableWithProgress runnable) throws InvocationTargetException,
			InterruptedException {
		if (fContainer != null)
			fContainer.run(fork, cancelable, runnable);
	}

	@Override
	public void runAsynchronously(IRunnableWithProgress runnable) {
		if (fContainer != null)
			fContainer.runAsynchronously(runnable);
	}

	/**
	 * Sets the container of this input to the given container
	 * @param container the container
	 * @since 3.3
	 */
	public void setContainer(ICompareContainer container) {
		Assert.isNotNull(container);
		this.fContainer = container;
		fContainerProvided = true;
	}

	/**
	 * Returns the container of this input or <code>null</code> if there is no container
	 * set.
	 * @return the container of this input or <code>null</code>
	 * @since 3.3
	 */
	public final ICompareContainer getContainer() {
		return fContainer;
	}

	/**
	 * Fires the given property change event to all listeners
	 * registered with this compare editor input.
	 * @param event the property change event
	 * @since 3.3
	 */
	protected void firePropertyChange(PropertyChangeEvent event) {
		Utilities.firePropertyChange(fListenerList, event);
	}

	/**
	 * Returns whether this compare editor input can be run as a job.
	 * By default, <code>false</code> is returned since traditionally inputs
	 * were prepared in the foreground (i.e the UI was blocked when the
	 * {@link #run(IProgressMonitor)} method (and indirectly the
	 * {@link #prepareInput(IProgressMonitor)} method) was invoked. Subclasses
	 * may override.
	 * @return whether this compare editor input can be run in the background
	 * @since 3.3
	 */
	public boolean canRunAsJob() {
		return false;
	}

	/**
	 * Returns whether this input belongs to the given family
	 * when it is run as a job.
	 * @see #canRunAsJob()
	 * @see Job#belongsTo(Object)
	 * @param family the job family
	 * @return whether this input belongs to the given family
	 * @since 3.3
	 */
	public boolean belongsTo(Object family) {
		return family == this;
	}

	/**
	 * Returns whether this input is intended to be used to select
	 * a particular edition of an element in a dialog. The result
	 * of this method is only consider if neither sides of the
	 * input are editable. By default, <code>false</code> is returned.
	 * @return whether this input is intended to be used to select
	 * a particular edition of an element in a dialog
	 * @see #getOKButtonLabel()
	 * @see #okPressed()
	 * @see #getSelectedEdition()
	 * @since 3.3
	 */
	public boolean isEditionSelectionDialog() {
		return false;
	}

	/**
	 * Returns the label to be used for the <code>OK</code>
	 * button when this input is displayed in a dialog.
	 * By default, different labels are used depending on
	 * whether the input is editable or is for edition selection
	 * (see {@link #isEditionSelectionDialog()}.
	 * @return the label to be used for the <code>OK</code>
	 * button when this input is displayed in a dialog
	 * @since 3.3
	 */
	public String getOKButtonLabel() {
		if (isEditable())
			return CompareMessages.CompareDialog_commit_button;
		if (isEditionSelectionDialog())
			return CompareMessages.CompareEditorInput_0;
		return IDialogConstants.OK_LABEL;
	}

	/**
	 * Returns the label used for the <code>CANCEL</code>
	 * button when this input is shown in a compare dialog
	 * using {@link CompareUI#openCompareDialog(CompareEditorInput)}.
	 * @return the label used for the <code>CANCEL</code> button
	 * @since 3.3
	 */
	public String getCancelButtonLabel() {
		return IDialogConstants.CANCEL_LABEL;
	}

	private boolean isEditable() {
		return getCompareConfiguration().isLeftEditable()
			|| getCompareConfiguration().isRightEditable();
	}

	/**
	 * The <code>OK</code> button was pressed in a dialog. If one or both of
	 * the sides of the input is editable then any changes will be saved. If the
	 * input is for edition selection (see {@link #isEditionSelectionDialog()}),
	 * it is up to subclasses to override this method in order to perform the
	 * appropriate operation on the selected edition.
	 *
	 * @return whether the dialog should be closed or not.
	 * @since 3.3
	 */
	public boolean okPressed() {
		if (isEditable()) {
			if (!saveChanges())
				return false;
		}
		return true;
	}

	/**
	 * The <code>CANCEL</code> button was pressed in a dialog.
	 * By default, nothing is done. Subclasses may override.
	 * @since 3.3
	 */
	public void cancelPressed() {
		// Do nothing
	}

	private boolean saveChanges() {
		try {
			Utilities.executeRunnable(monitor -> {
				try {
					saveChanges(monitor);
				} catch (CoreException e) {
					throw new InvocationTargetException(e);
				}
			});
			return true;
		} catch (InterruptedException | OperationCanceledException x) {
			// Ignore
		} catch (InvocationTargetException x) {
			ErrorDialog.openError(fComposite.getShell(), CompareMessages.CompareDialog_error_title, null,
				new Status(IStatus.ERROR, CompareUIPlugin.PLUGIN_ID, 0,
					NLS.bind(CompareMessages.CompareDialog_error_message, x.getTargetException().getMessage()), x.getTargetException()));
		}
		return false;
	}

	/**
	 * Returns the selected edition or <code>null</code> if no edition is selected.
	 * The result of this method should only be considered if {@link #isEditionSelectionDialog()}
	 * returns <code>true</code>.
	 * @return the selected edition or <code>null</code>
	 * @since 3.3
	 */
	public Object getSelectedEdition() {
		if (fStructureInputPane != null) {
			ISelection selection = fStructureInputPane.getSelection();
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection) selection;
				if (!ss.isEmpty())
					return ss.getFirstElement();

			}
		}
		return null;
	}

	/**
	 * Sets the help context id for this input.
	 * @param helpContextId the help context id.
	 * @since 3.3
	 */
	public void setHelpContextId(String helpContextId) {
		this.fHelpContextId = helpContextId;
	}
}
