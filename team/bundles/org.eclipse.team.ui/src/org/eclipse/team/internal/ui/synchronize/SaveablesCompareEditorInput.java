/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.IEditableContent;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.compare.IResourceProvider;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.team.internal.ui.mapping.AbstractCompareInput;
import org.eclipse.team.internal.ui.mapping.CompareInputChangeNotifier;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchSite;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.actions.ContributionItemFactory;
import org.eclipse.ui.actions.OpenFileAction;
import org.eclipse.ui.actions.OpenWithMenu;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.ide.IGotoMarker;
import org.eclipse.ui.keys.IBindingService;
import org.eclipse.ui.services.IDisposable;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * A compare editor input that makes use of a {@link Saveable} to manage the
 * save lifecycle of the left and right sides of the comparison. The ancestor
 * part of the comparison is not editable.
 */
public class SaveablesCompareEditorInput extends CompareEditorInput implements
		ISaveablesSource {

	private IPropertyListener fLeftPropertyListener;
	private IPropertyListener fRightPropertyListener;

	private Saveable fLeftSaveable;
	private Saveable fRightSaveable;

	private ITypedElement fAncestorElement;
	private ITypedElement fLeftElement;
	private ITypedElement fRightElement;

	private final IWorkbenchPage page;
	private final ListenerList<ICompareInputChangeListener> inputChangeListeners = new ListenerList<>(
			ListenerList.IDENTITY);
	private ICompareInputChangeListener compareInputChangeListener;

	public SaveablesCompareEditorInput(ITypedElement ancestor,
			ITypedElement left, ITypedElement right, IWorkbenchPage page) {
		super(new CompareConfiguration());
		this.page = page;
		this.fAncestorElement = ancestor;
		this.fLeftElement = left;
		this.fRightElement = right;
	}

	private static ITypedElement getFileElement(ITypedElement element,
			CompareEditorInput editorInput) {
		if (element instanceof LocalResourceTypedElement) {
			return element;
		}
		if (editorInput instanceof CompareFileRevisionEditorInput) {
			return ((CompareFileRevisionEditorInput) editorInput)
					.getLocalElement();
		}
		return null;
	}

	/**
	 * Return a typed element that represents a local file. If the element
	 * returned from this method is used as the left contributor of the compare
	 * input for a {@link SaveableCompareEditorInput}, then the file will be
	 * properly saved when the compare editor input or viewers are saved.
	 *
	 * @param file
	 *            the file
	 * @return a typed element that represents a local file.
	 */
	public static ITypedElement createFileElement(IFile file) {
		return new LocalResourceTypedElement(file);
	}

	private ISaveablesLifecycleListener getSaveablesLifecycleListener(
			IWorkbenchPart part) {
		ISaveablesLifecycleListener listener = Adapters.adapt(part, ISaveablesLifecycleListener.class);
		if (listener == null)
			listener = part.getSite().getService(ISaveablesLifecycleListener.class);
		return listener;
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		compareInputChangeListener = source -> {
			if (source == getCompareResult()) {
				boolean closed = false;
				if (source.getKind() == Differencer.NO_CHANGE) {
					closed = closeEditor(true);
				}
				if (!closed) {
					// The editor was not closed either because the compare
					// input still has changes or because the editor input
					// is dirty. In either case, fire the changes
					// to the registered listeners
					propogateInputChange();
				}
			}
		};
		getCompareInput().addCompareInputChangeListener(
				compareInputChangeListener);

		if (getLeftSaveable() instanceof SaveableComparison) {
			SaveableComparison lscm = (SaveableComparison) fLeftSaveable;
			fLeftPropertyListener = (source, propId) -> {
				if (propId == SaveableComparison.PROP_DIRTY) {
					setLeftDirty(fLeftSaveable.isDirty());
				}
			};
			lscm.addPropertyListener(fLeftPropertyListener);
		}

		if (getRightSaveable() instanceof SaveableComparison) {
			SaveableComparison rscm = (SaveableComparison) fRightSaveable;
			fRightPropertyListener = (source, propId) -> {
				if (propId == SaveableComparison.PROP_DIRTY) {
					setRightDirty(fRightSaveable.isDirty());
				}
			};
			rscm.addPropertyListener(fRightPropertyListener);
		}

		setLeftDirty(fLeftSaveable.isDirty());
		setRightDirty(fRightSaveable.isDirty());
	}

	@Override
	protected void handleDispose() {
		super.handleDispose();
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null)
			compareInput
					.removeCompareInputChangeListener(compareInputChangeListener);
		compareInputChangeListener = null;
		if (fLeftSaveable instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) fLeftSaveable;
			scm.removePropertyListener(fLeftPropertyListener);
		}
		if (fLeftSaveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) fLeftSaveable;
			rsc.dispose();
		}
		if (fRightSaveable instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) fRightSaveable;
			scm.removePropertyListener(fRightPropertyListener);
		}
		if (fRightSaveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) fRightSaveable;
			rsc.dispose();
		}

		if (getCompareResult() instanceof IDisposable) {
			((IDisposable) getCompareResult()).dispose();
		}
	}

	private String[] getLabels() {
		IResource leftResource = getResource(fLeftElement);
		IResource rightResource = getResource(fRightElement);
		if (leftResource != null && rightResource != null) {
			String leftLabel = leftResource.getFullPath().makeRelative().toString();
			String rightLabel = rightResource.getFullPath().makeRelative().toString();
			if (fAncestorElement != null) {
				IResource ancestorResource = getResource(fAncestorElement);
				if (ancestorResource != null) {
					String ancestorLabel = rightResource.getFullPath().makeRelative().toString();
					return new String[] { ancestorLabel, leftLabel, rightLabel };
				}
			}
			return new String[] { leftLabel, rightLabel };
		}
		if (fAncestorElement != null) {
			return new String[] { fAncestorElement.getName(), fLeftElement.getName(), fRightElement.getName() };
		}
		return new String[] { fLeftElement.getName(), fRightElement.getName() };
	}

	@Override
	public String getToolTipText() {
		String[] labels = getLabels();
		if (labels.length == 3)
			return NLS.bind(TeamUIMessages.SaveablesCompareEditorInput_threeWayTooltip, labels);
		return NLS.bind(TeamUIMessages.SaveablesCompareEditorInput_twoWayTooltip, labels);
	}

	@Override
	public String getTitle() {
		String[] labels = getLabels();
		if (labels.length == 3)
			return NLS.bind(TeamUIMessages.SaveablesCompareEditorInput_threeWayTitle, labels);
		return NLS.bind(TeamUIMessages.SaveablesCompareEditorInput_twoWayTitle, labels);
	}

	private IWorkbenchPage getPage() {
		if (page == null)
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow()
					.getActivePage();
		return page;
	}

	/**
	 * Return the compare input of this editor input.
	 *
	 * @return the compare input of this editor input
	 */
	protected final ICompareInput getCompareInput() {
		return (ICompareInput) getCompareResult();
	}

	/**
	 * Callback from the resource saveable that is invoked when the resource is
	 * saved so that this input can fire a change event for its input.
	 * Subclasses only need this method if the left side of their compare input
	 * is an element returned from
	 * {@link SaveableCompareEditorInput#createFileElement(IFile)}.
	 */
	protected void fireInputChange() {
		((MyDiffNode) getCompareResult()).fireChange();
	}

	protected Saveable getLeftSaveable() {
		if (fLeftSaveable == null) {
			fLeftSaveable = createLeftSaveable();
		}
		return fLeftSaveable;
	}

	protected Saveable getRightSaveable() {
		if (fRightSaveable == null) {
			fRightSaveable = createRightSaveable();
		}
		return fRightSaveable;
	}

	protected Saveable createLeftSaveable() {
		Object compareResult = getCompareResult();
		Assert
				.isNotNull(compareResult,
						"This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		ITypedElement leftFileElement = getFileElement(getCompareInput()
				.getLeft(), this);
		return new InternalResourceSaveableComparison(
				(ICompareInput) compareResult, this, leftFileElement);
	}

	protected Saveable createRightSaveable() {
		Object compareResult = getCompareResult();
		Assert
				.isNotNull(compareResult,
						"This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		ITypedElement rightFileElement = getFileElement(getCompareInput()
				.getRight(), this);
		return new InternalResourceSaveableComparison(
				(ICompareInput) compareResult, this, rightFileElement);
	}

	@Override
	public Saveable[] getActiveSaveables() {
		if (getCompareResult() == null)
			return new Saveable[0];
		return new Saveable[] { getLeftSaveable(), getRightSaveable() };
	}

	@Override
	public Saveable[] getSaveables() {
		return getActiveSaveables();
	}

	@Override
	public Viewer findContentViewer(Viewer pOldViewer, ICompareInput pInput,
			Composite pParent) {
		Viewer newViewer = super.findContentViewer(pOldViewer, pInput, pParent);
		boolean isNewViewer = newViewer != pOldViewer;
		if (isNewViewer && newViewer instanceof IPropertyChangeNotifier
				&& fLeftSaveable instanceof IPropertyChangeListener
				&& fRightSaveable instanceof IPropertyChangeListener) {
			// Register the model for change events if appropriate
			final IPropertyChangeNotifier dsp = (IPropertyChangeNotifier) newViewer;
			final IPropertyChangeListener lpcl = (IPropertyChangeListener) fLeftSaveable;
			final IPropertyChangeListener rpcl = (IPropertyChangeListener) fRightSaveable;
			dsp.addPropertyChangeListener(lpcl);
			dsp.addPropertyChangeListener(rpcl);
			Control c = newViewer.getControl();
			c.addDisposeListener(e -> {
				dsp.removePropertyChangeListener(lpcl);
				dsp.removePropertyChangeListener(rpcl);
			});
		}
		return newViewer;
	}

	@Override
	public boolean isDirty() {
		if (fLeftSaveable != null && fLeftSaveable.isDirty())
			return true;
		if (fRightSaveable != null && fRightSaveable.isDirty())
			return true;
		return super.isDirty();
	}

	/**
	 * Returns <code>true</code> if the given saveable contains any unsaved
	 * changes. If the saveable doesn't match either left nor right side of the
	 * current editor input {@link CompareEditorInput#isSaveNeeded()} is called.
	 * <p>
	 * This method is preferred to {@link CompareEditorInput#isSaveNeeded()}.
	 *
	 * @param the
	 *            the saveable to check
	 * @return <code>true</code> if there are changes that need to be saved
	 * @since 3.7
	 */
	boolean isSaveNeeded(Saveable saveable) {
		if (saveable == null) {
			return isSaveNeeded();
		}
		if (saveable.equals(fLeftSaveable)) {
			return isLeftSaveNeeded();
		}
		if (saveable.equals(fRightSaveable)) {
			return isRightSaveNeeded();
		}
		// Fallback call returning true if there are unsaved changes in either
		// left or right side
		return isSaveNeeded();
	}

	void setDirty(boolean dirty, Saveable saveable) {
		if (saveable.equals(fLeftSaveable)) {
			setLeftDirty(dirty);
		}
		if (saveable.equals(fRightSaveable)) {
			setRightDirty(dirty);
		}
	}

	void saveChanges(IProgressMonitor monitor, Saveable saveable)
			throws CoreException {
		if (saveable.equals(fLeftSaveable)) {
			flushLeftViewers(monitor);
			return;
		} else if (saveable.equals(fRightSaveable)) {
			flushRightViewers(monitor);
			return;
		}
		Assert.isTrue(false, "invalid saveable parameter"); //$NON-NLS-1$
	}

	/**
	 * Close the editor if it is not dirty. If it is still dirty, let the
	 * content merge viewer handle the compare input change.
	 *
	 * @param checkForUnsavedChanges
	 *            whether to check for unsaved changes
	 * @return <code>true</code> if the editor was closed (note that the close
	 *         may be asynchronous)
	 */
	protected boolean closeEditor(boolean checkForUnsavedChanges) {
		if (isSaveNeeded() && checkForUnsavedChanges) {
			return false;
		} else {
			Runnable runnable = () -> {
				IEditorPart part = getPage().findEditor(
						SaveablesCompareEditorInput.this);
				getPage().closeEditor(part, false);
			};
			if (Display.getCurrent() != null) {
				runnable.run();
			} else {
				Display display = getPage().getWorkbenchWindow().getShell()
						.getDisplay();
				display.asyncExec(runnable);
			}
			return true;
		}
	}

	/**
	 * Prepare the compare input of this editor input. This method is not
	 * intended to be overridden of extended by subclasses (but is not final for
	 * backwards compatibility reasons). The implementation of this method in
	 * this class delegates the creation of the compare input to the
	 * {@link #prepareCompareInput(IProgressMonitor)} method which subclasses
	 * must implement.
	 *
	 * @see org.eclipse.compare.CompareEditorInput#prepareInput(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		final ICompareInput input = prepareCompareInput(monitor);
		if (input != null)
			setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title,
					new String[] { input.getName() }));
		return input;
	}

	/**
	 * Method called from {@link #prepareInput(IProgressMonitor)} to obtain the input. Its purpose
	 * is to ensure that the input is an instance of {@link ICompareInput}.
	 *
	 * @param monitor a progress monitor
	 * @return the compare input
	 * @throws InvocationTargetException
	 * @throws InterruptedException
	 */
	protected ICompareInput prepareCompareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		ICompareInput input = createCompareInput();
		getCompareConfiguration().setLeftEditable(isEditable(input.getLeft()));
		getCompareConfiguration()
				.setRightEditable(isEditable(input.getRight()));
		initLabels();
		return input;
	}

	private boolean isEditable(Object obj) {
		if (obj instanceof IEditableContent) {
			return ((IEditableContent) obj).isEditable();
		}
		return false;
	}

	private void initLabels() {
		CompareConfiguration cc = getCompareConfiguration();

		IResource ancestorResource = getResource(fAncestorElement);
		IResource leftResource = getResource(fLeftElement);
		IResource rightResource = getResource(fRightElement);

		if (ancestorResource != null) {
			String ancestorLabel = ancestorResource.getFullPath()
					.makeRelative().toString();

			cc.setAncestorLabel(ancestorLabel);
		}

		if (leftResource != null && rightResource != null) {
			String leftLabel = leftResource.getFullPath().makeRelative()
					.toString();
			String rightLabel = rightResource.getFullPath().makeRelative()
					.toString();

			cc.setLeftLabel(leftLabel);
			cc.setRightLabel(rightLabel);
		}
	}

	private ICompareInput createCompareInput() {
		return fAncestorElement == null ? new MyDiffNode(fLeftElement,
				fRightElement) : new MyDiffNode(fAncestorElement, fLeftElement,
				fRightElement);
	}

	private CompareInputChangeNotifier notifier = new CompareInputChangeNotifier() {
		@Override
		protected IResource[] getResources(ICompareInput input) {
			IResource leftResource = getResource(fLeftElement);
			IResource rightResource = getResource(fRightElement);
			if (leftResource == null && rightResource == null)
				return new IResource[0];
			if (leftResource == null && rightResource != null)
				return new IResource[] { rightResource };
			if (leftResource != null && rightResource == null)
				return new IResource[] { leftResource };
			return new IResource[] { leftResource, rightResource };
		}
	};

	private class MyDiffNode extends AbstractCompareInput {
		public MyDiffNode(ITypedElement left, ITypedElement right) {
			super(Differencer.CHANGE, null, left, right);
		}

		public MyDiffNode(ITypedElement ancestor, ITypedElement left,
				ITypedElement right) {
			super(Differencer.CONFLICTING, ancestor, left, right);
		}

		@Override
		public void fireChange() {
			super.fireChange();
		}

		@Override
		protected CompareInputChangeNotifier getChangeNotifier() {
			return notifier;
		}

		@Override
		public boolean needsUpdate() {
			return true;
		}

		@Override
		public void update() {
			fireChange();
		}
	}

	private IResource getResource(ITypedElement pElement) {
		if (pElement instanceof LocalResourceTypedElement
				&& pElement instanceof IResourceProvider) {
			return ((IResourceProvider) pElement).getResource();
		}
		return null;
	}

	@Override
	public void registerContextMenu(final MenuManager pMenuManager,
			final ISelectionProvider pSelectionProvider) {
		super.registerContextMenu(pMenuManager, pSelectionProvider);
		final Saveable lLeftSaveable = getLeftSaveable();
		final ITypedElement lLeftElement = getFileElement(getCompareInput()
				.getLeft(), this);
		if (lLeftSaveable instanceof LocalResourceSaveableComparison) {
			pMenuManager.addMenuListener(manager -> handleMenuAboutToShow(manager, getContainer(), lLeftSaveable, lLeftElement, pSelectionProvider));
		}
		final Saveable lRightSaveable = getRightSaveable();
		final ITypedElement lRightElement = getFileElement(getCompareInput()
				.getRight(), this);
		if (lRightSaveable instanceof LocalResourceSaveableComparison) {
			pMenuManager.addMenuListener(manager -> handleMenuAboutToShow(manager, getContainer(), lRightSaveable, lRightElement, pSelectionProvider));
		}
	}

	@Override
	public void addCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.add(listener);
		} else {
			super.addCompareInputChangeListener(input, listener);
		}
	}

	@Override
	public void removeCompareInputChangeListener(ICompareInput input,
			ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.remove(listener);
		} else {
			super.removeCompareInputChangeListener(input, listener);
		}
	}

	private void propogateInputChange() {
		if (!inputChangeListeners.isEmpty()) {
			Object[] allListeners = inputChangeListeners.getListeners();
			final ICompareInput compareResult = (ICompareInput) getCompareResult();
			for (Object l : allListeners) {
				final ICompareInputChangeListener listener = (ICompareInputChangeListener) l;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.compareInputChanged(compareResult);
					}

					@Override
					public void handleException(Throwable exception) {
						// Logged by the safe runner
					}
				});
			}
		}
	}

	@Override
	public Image getTitleImage() {
		ImageRegistry reg = TeamUIPlugin.getPlugin().getImageRegistry();
		Image image = reg.get(ITeamUIImages.IMG_SYNC_VIEW);
		if (image == null) {
			image = getImageDescriptor().createImage();
			reg.put(ITeamUIImages.IMG_SYNC_VIEW, image);
		}
		return image;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_SYNC_VIEW);
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	private static String getShowInMenuLabel() {
		String keyBinding = null;

		IBindingService bindingService = PlatformUI.getWorkbench().getAdapter(IBindingService.class);
		if (bindingService != null)
			keyBinding = bindingService
					.getBestActiveBindingFormattedFor(IWorkbenchCommandConstants.NAVIGATE_SHOW_IN_QUICK_MENU);

		if (keyBinding == null)
			keyBinding = ""; //$NON-NLS-1$

		return NLS
				.bind(TeamUIMessages.SaveableCompareEditorInput_0, keyBinding);
	}

	// TODO: add getAdapter for IFile[]

	private class InternalResourceSaveableComparison extends
			LocalResourceSaveableComparison implements
			ISharedDocumentAdapterListener {
		private LocalResourceTypedElement lrte;
		private boolean connected = false;

		public InternalResourceSaveableComparison(ICompareInput input,
				CompareEditorInput editorInput, ITypedElement element) {
			super(input, editorInput, element);
			if (element instanceof LocalResourceTypedElement) {
				lrte = (LocalResourceTypedElement) element;
				if (lrte.isConnected()) {
					registerSaveable(true);
				} else {
					lrte.setSharedDocumentListener(this);
				}
			}
		}

		@Override
		protected void fireInputChange() {
			SaveablesCompareEditorInput.this.fireInputChange();
		}

		@Override
		public void dispose() {
			super.dispose();
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}

		@Override
		public void handleDocumentConnected() {
			if (connected)
				return;
			connected = true;
			registerSaveable(false);
			if (lrte != null)
				lrte.setSharedDocumentListener(null);
		}

		private void registerSaveable(boolean init) {
			ICompareContainer container = getContainer();
			IWorkbenchPart part = container.getWorkbenchPart();
			if (part != null) {
				ISaveablesLifecycleListener lifecycleListener = getSaveablesLifecycleListener(part);
				// Remove this saveable from the lifecycle listener
				if (!init)
					lifecycleListener
							.handleLifecycleEvent(new SaveablesLifecycleEvent(
									part, SaveablesLifecycleEvent.POST_CLOSE,
									new Saveable[] { this }, false));
				// Now fix the hashing so it uses the connected document
				initializeHashing();
				// Finally, add this saveable back to the listener
				lifecycleListener
						.handleLifecycleEvent(new SaveablesLifecycleEvent(part,
								SaveablesLifecycleEvent.POST_OPEN,
								new Saveable[] { this }, false));
			}
		}

		@Override
		public void handleDocumentDeleted() {
			// Ignore
		}

		@Override
		public void handleDocumentDisconnected() {
			// Ignore
		}

		@Override
		public void handleDocumentFlushed() {
			// Ignore
		}

		@Override
		public void handleDocumentSaved() {
			// Ignore
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;

			if (!(obj instanceof Saveable))
				return false;

			Object document = getAdapter(IDocument.class);

			if (document != null) {
				Object otherDocument = ((Saveable) obj)
						.getAdapter(IDocument.class);
				return document.equals(otherDocument);
			}

			if (obj instanceof InternalResourceSaveableComparison) {
				InternalResourceSaveableComparison rscm = (InternalResourceSaveableComparison) obj;
				return rscm.getInput().equals(getInput()) && rscm.lrte.equals(lrte);
			}
			return false;
		}
	}

	public static void handleMenuAboutToShow(IMenuManager manager, ICompareContainer container, Saveable saveable, ITypedElement element, ISelectionProvider provider) {
		if (provider instanceof ITextViewer) {
			final ITextViewer v= (ITextViewer)provider;
			IDocument d= v.getDocument();
			IDocument other= Adapters.adapt(saveable, IDocument.class);
			if (d == other) {
				if (element instanceof IResourceProvider) {
					IResourceProvider rp= (IResourceProvider)element;
					IResource resource= rp.getResource();
					StructuredSelection selection= new StructuredSelection(resource);
					IWorkbenchPart workbenchPart= container.getWorkbenchPart();
					if (workbenchPart != null) {
						final IWorkbenchSite ws= workbenchPart.getSite();

						MenuManager submenu1= new MenuManager(getShowInMenuLabel());
						IContributionItem showInMenu= ContributionItemFactory.VIEWS_SHOW_IN.create(ws.getWorkbenchWindow());
						submenu1.add(showInMenu);
						manager.insertAfter("file", submenu1); //$NON-NLS-1$
						MenuManager submenu2= new MenuManager(TeamUIMessages.OpenWithActionGroup_0);

						// XXX: Internal reference will get fixed during 3.7, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=307026
						submenu2.add(new OpenWithMenu(ws.getPage(), resource) {
							@Override
							protected void openEditor(IEditorDescriptor editorDescriptor, boolean openUsingDescriptor) {
								super.openEditor(editorDescriptor, openUsingDescriptor);
								IEditorPart editor= ws.getPage().getActiveEditor();
								Point selectedRange= v.getSelectedRange();
								revealInEditor(editor, selectedRange.x, selectedRange.y);
							}
						});
						manager.insertAfter("file", submenu2); //$NON-NLS-1$

						// XXX: Internal reference will get fixed during 3.7, see https://bugs.eclipse.org/bugs/show_bug.cgi?id=307026
						OpenFileAction openFileAction= new OpenFileAction(ws.getPage()) {
							@Override
							public void run() {
								super.run();
								IEditorPart editor= ws.getPage().getActiveEditor();
								Point selectedRange= v.getSelectedRange();
								revealInEditor(editor, selectedRange.x, selectedRange.y);
							}
						};
						openFileAction.selectionChanged(selection);
						manager.insertAfter("file", openFileAction); //$NON-NLS-1$
					}
				}
			}
		}
	}

	/**
	 * Selects and reveals the given offset and length in the given editor part.
	 *
	 * @param editor the editor part
	 * @param offset the offset
	 * @param length the length
	 * @since 3.6
	 */
	private static void revealInEditor(IEditorPart editor, final int offset, final int length) {
		if (editor instanceof ITextEditor) {
			((ITextEditor)editor).selectAndReveal(offset, length);
			return;
		}

		// Support for non-text editor - try IGotoMarker interface
		final IGotoMarker gotoMarkerTarget;
		if (editor instanceof IGotoMarker)
			gotoMarkerTarget= (IGotoMarker)editor;
		else
			gotoMarkerTarget= editor != null ? (IGotoMarker)editor.getAdapter(IGotoMarker.class) : null;
		if (gotoMarkerTarget != null) {
			final IEditorInput input= editor.getEditorInput();
			if (input instanceof IFileEditorInput) {
				WorkspaceModifyOperation op= new WorkspaceModifyOperation() {
					@Override
					protected void execute(IProgressMonitor monitor) throws CoreException {
						IMarker marker= null;
						try {
							marker= ((IFileEditorInput)input).getFile().createMarker(IMarker.TEXT);
							marker.setAttribute(IMarker.CHAR_START, offset);
							marker.setAttribute(IMarker.CHAR_END, offset + length);

							gotoMarkerTarget.gotoMarker(marker);

						} finally {
							if (marker != null)
								marker.delete();
						}
					}
				};

				try {
					op.run(null);
				} catch (InvocationTargetException ex) {
					// reveal failed
				} catch (InterruptedException e) {
					Assert.isTrue(false, "this operation can not be canceled"); //$NON-NLS-1$
				}
			}
			return;
		}
	}

}
