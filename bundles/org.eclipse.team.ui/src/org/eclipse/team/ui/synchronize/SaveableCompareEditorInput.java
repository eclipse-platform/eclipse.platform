/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
 *     Stefan Xenos <sxenos@gmail.com> (Google) - bug 448968 - Add diagnostic logging
 *******************************************************************************/
package org.eclipse.team.ui.synchronize;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.ICompareContainer;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.Differencer;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.compare.structuremergeviewer.ICompareInputChangeListener;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.CompareFileRevisionEditorInput;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter.ISharedDocumentAdapterListener;
import org.eclipse.team.internal.ui.synchronize.LocalResourceSaveableComparison;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.SaveablesCompareEditorInput;
import org.eclipse.team.ui.mapping.SaveableComparison;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IPropertyListener;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.services.IDisposable;

/**
 * A compare editor input that makes use of a {@link Saveable} to manage the save
 * lifecycle of the editor input. If the element returned from
 * {@link #createFileElement(IFile)} is used as the left side of the compare input
 * and the default saveable returned from {@link #createSaveable()} is used, then
 * this compare input will provide the complete save lifecycle for the local file.
 * <p>
 * Clients may subclass this class.
 * </p>
 * @since 3.3
 */
public abstract class SaveableCompareEditorInput extends CompareEditorInput implements ISaveablesSource {
	private ICompareInputChangeListener compareInputChangeListener;
	private final IWorkbenchPage page;
	private final ListenerList<ICompareInputChangeListener> inputChangeListeners =
			new ListenerList<>(ListenerList.IDENTITY);
	private Saveable saveable;
	private IPropertyListener propertyListener;

	/**
	 * Returns a typed element that represents a local file. If the element
	 * returned from this method is used as the left contributor of the compare
	 * input for a {@link SaveableCompareEditorInput}, then the file will
	 * be properly saved when the compare editor input or viewers are saved.
	 *
	 * @param file the file
	 * @return a typed element that represents a local file.
	 */
	public static ITypedElement createFileElement(IFile file) {
		return new LocalResourceTypedElement(file);
	}

	private static ITypedElement getFileElement(ICompareInput input, CompareEditorInput editorInput) {
		if (input.getLeft() instanceof LocalResourceTypedElement) {
			return input.getLeft();
		}
		if (editorInput instanceof CompareFileRevisionEditorInput) {
			return ((CompareFileRevisionEditorInput) editorInput).getLocalElement();
		}
		return null;
	}

	private class InternalResourceSaveableComparison extends LocalResourceSaveableComparison implements ISharedDocumentAdapterListener {
		private LocalResourceTypedElement lrte;
		private boolean connected;

		public InternalResourceSaveableComparison(
				ICompareInput input, CompareEditorInput editorInput) {
			super(input, editorInput, SaveableCompareEditorInput.getFileElement(input, editorInput));
			ITypedElement element = SaveableCompareEditorInput.getFileElement(input, editorInput);
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
			SaveableCompareEditorInput.this.fireInputChange();
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
				ISaveablesLifecycleListener lifecycleListener= getSaveablesLifecycleListener(part);
				// Remove this saveable from the lifecycle listener.
				if (!init) {
					lifecycleListener.handleLifecycleEvent(
							new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_CLOSE, new Saveable[] { this }, false));
				}
				// Now fix the hashing so it uses the connected document.
				initializeHashing();
				// Finally, add this saveable back to the listener.
				lifecycleListener.handleLifecycleEvent(
						new SaveablesLifecycleEvent(part, SaveablesLifecycleEvent.POST_OPEN, new Saveable[] { this }, false));
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
	}

	/**
	 * Creates a <code>LocalResourceCompareEditorInput</code> which is initialized with the given
	 * compare configuration.
	 * The compare configuration is passed to subsequently created viewers.
	 *
	 * @param configuration the compare configuration
	 * @param page the workbench page that will contain the editor
	 */
	public SaveableCompareEditorInput(CompareConfiguration configuration, IWorkbenchPage page) {
		super(configuration);
		this.page = page;
		if (Policy.DEBUG_COMPARE_EDITOR_INPUT) {
			logTrace("constructed"); //$NON-NLS-1$
		}
	}

	@Override
	protected void contentsCreated() {
		super.contentsCreated();
		if (Policy.DEBUG_COMPARE_EDITOR_INPUT) {
			logTrace("compareInputChangeListener assigned"); //$NON-NLS-1$
			logStackTrace();
		}
		compareInputChangeListener = source -> {
			if (source == getCompareResult()) {
				boolean closed = false;
				if (source.getKind() == Differencer.NO_CHANGE) {
					closed = closeEditor(true);
				}
				if (!closed) {
					// The editor was closed either because the compare input still has changes
					// or because the editor input is dirty. In either case, fire the changes
					// to the registered listeners.
					propogateInputChange();
				}
			}
		};
		getCompareInput().addCompareInputChangeListener(compareInputChangeListener);

		if (getSaveable() instanceof SaveableComparison) {
			SaveableComparison scm = (SaveableComparison) saveable;
			propertyListener = (source, propId) -> {
				if (propId == SaveableComparison.PROP_DIRTY) {
					setDirty(saveable.isDirty());
				}
			};
			scm.addPropertyListener(propertyListener);
		}
		setDirty(saveable.isDirty());
	}

	private ISaveablesLifecycleListener getSaveablesLifecycleListener(IWorkbenchPart part) {
		ISaveablesLifecycleListener listener = Adapters.adapt(part, ISaveablesLifecycleListener.class);
		if (listener == null)
			listener = part.getSite().getService(ISaveablesLifecycleListener.class);
		return listener;
	}

	@Override
	protected void handleDispose() {
		if (Policy.DEBUG_COMPARE_EDITOR_INPUT) {
			logTrace("handleDispose()"); //$NON-NLS-1$
			logStackTrace();
		}
		super.handleDispose();
		ICompareInput compareInput = getCompareInput();
		if (compareInput != null) {
			if (compareInputChangeListener != null) {
				compareInput.removeCompareInputChangeListener(compareInputChangeListener);
			} else if (Policy.DEBUG_COMPARE_EDITOR_INPUT) {
				logTrace("compareInputChangeListener is unexpectedly null!"); //$NON-NLS-1$
				logStackTrace();
			}
		}
		if (Policy.DEBUG_COMPARE_EDITOR_INPUT) {
			logTrace("compareInputChangeListener = null"); //$NON-NLS-1$
		}
		compareInputChangeListener = null;
		if (saveable instanceof SaveableComparison && propertyListener != null) {
			SaveableComparison scm = (SaveableComparison) saveable;
			scm.removePropertyListener(propertyListener);
			propertyListener = null;
		}
		if (saveable instanceof LocalResourceSaveableComparison) {
			LocalResourceSaveableComparison rsc = (LocalResourceSaveableComparison) saveable;
			rsc.dispose();
		}
		if (getCompareResult() instanceof IDisposable) {
			((IDisposable) getCompareResult()).dispose();
		}
	}

	private void logStackTrace() {
		new Exception("<Fake exception> in " + getClass().getName()).printStackTrace(System.out); //$NON-NLS-1$
	}

	private void logTrace(String string) {
		System.out.println("SaveableCompareEditorInput " + System.identityHashCode(this) + ": " + string);  //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Prepares the compare input of this editor input. This method is not intended to be overridden of
	 * extended by subclasses (but is not final for backwards compatibility reasons).
	 * The implementation of this method in this class
	 * delegates the creation of the compare input to the {@link #prepareCompareInput(IProgressMonitor)}
	 * method which subclasses must implement.
	 * @see CompareEditorInput#prepareInput(IProgressMonitor)
	 */
	@Override
	protected Object prepareInput(IProgressMonitor monitor)
			throws InvocationTargetException, InterruptedException {
		final ICompareInput input = prepareCompareInput(monitor);
		if (input != null)
			setTitle(NLS.bind(TeamUIMessages.SyncInfoCompareInput_title, new String[] { input.getName()}));
		return input;
	}

	/**
	 * Method called from {@link #prepareInput(IProgressMonitor)} to obtain the input. Its purpose
	 * is to ensure that the input is an instance of {@link ICompareInput}.
	 *
	 * @param monitor a progress monitor
	 * @return the compare input
	 * @throws InvocationTargetException if an error occurs
	 * @throws InterruptedException if operation is interrupted
	 */
	protected abstract ICompareInput prepareCompareInput(IProgressMonitor monitor)
		throws InvocationTargetException, InterruptedException;

	/**
	 * Returns the compare input of this editor input.
	 *
	 * @return the compare input of this editor input
	 */
	protected final ICompareInput getCompareInput() {
		return (ICompareInput)getCompareResult();
	}

	/**
	 * Callback from the resource saveable that is invoked when the resource is
	 * saved so that this input can fire a change event for its input. Subclasses
	 * only need this method if the left side of their compare input is
	 * an element returned from {@link #createFileElement(IFile)}.
	 */
	protected abstract void fireInputChange();

	/**
	 * Close the editor if it is not dirty. If it is still dirty, let the
	 * content merge viewer handle the compare input change.
	 *
	 * @param checkForUnsavedChanges whether to check for unsaved changes
	 * @return <code>true</code> if the editor was closed (note that the
	 * close may be asynchronous)
	 */
	protected boolean closeEditor(boolean checkForUnsavedChanges) {
		if (isSaveNeeded() && checkForUnsavedChanges)
			return false;

		final IWorkbenchPage page= getPage();
		if (page == null)
			return false;

		Runnable runnable = () -> {
			Shell shell= page.getWorkbenchWindow().getShell();
			if (shell == null)
				return;

			IEditorPart part= page.findEditor(SaveableCompareEditorInput.this);
			getPage().closeEditor(part, false);
		};
		if (Display.getCurrent() != null) {
			runnable.run();
		} else {
			Shell shell= page.getWorkbenchWindow().getShell();
			if (shell == null)
				return false;
			Display display= shell.getDisplay();
			display.asyncExec(runnable);
		}
		return true;
	}

	private IWorkbenchPage getPage() {
		if (page == null)
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		return page;
	}

	void propogateInputChange() {
		if (!inputChangeListeners.isEmpty()) {
			Object[] allListeners = inputChangeListeners.getListeners();
			for (Object l : allListeners) {
				final ICompareInputChangeListener listener = (ICompareInputChangeListener) l;
				SafeRunner.run(new ISafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.compareInputChanged((ICompareInput)SaveableCompareEditorInput.this.getCompareResult());
					}

					@Override
					public void handleException(Throwable exception) {
						// Logged by the safe runner
					}
				});
			}
		}
	}

	/**
	 * Get the saveable that provides the save behavior for this compare editor input.
	 * The {@link #createSaveable()} is called to create the saveable if it does not yet exist.
	 * This method cannot be called until after the input is prepared (i.e. until after
	 * the {@link #run(IProgressMonitor)} method is called which will in turn will invoke
	 * {@link #prepareCompareInput(IProgressMonitor)}.
	 * @return saveable that provides the save behavior for this compare editor input.
	 */
	protected Saveable getSaveable() {
		if (saveable == null) {
			saveable = createSaveable();
		}
		return saveable;
	}

	/**
	 * Create the saveable that provides the save behavior for this compare editor input.
	 * By default, a saveable that handles local files is returned.
	 *
	 * @return the saveable that provides the save behavior for this compare editor input
	 */
	protected Saveable createSaveable() {
		Object compareResult = getCompareResult();
		Assert.isNotNull(compareResult, "This method cannot be called until after prepareInput is called"); //$NON-NLS-1$
		return new InternalResourceSaveableComparison((ICompareInput)compareResult, this);
	}

	@Override
	public Saveable[] getActiveSaveables() {
		if (getCompareResult() == null)
			return new Saveable[0];
		return new Saveable[] { getSaveable() };
	}

	@Override
	public Saveable[] getSaveables() {
		return getActiveSaveables();
	}

	@Override
	public void addCompareInputChangeListener(ICompareInput input, ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.add(listener);
		} else {
			super.addCompareInputChangeListener(input, listener);
		}
	}

	@Override
	public void removeCompareInputChangeListener(ICompareInput input, ICompareInputChangeListener listener) {
		if (input == getCompareResult()) {
			inputChangeListeners.remove(listener);
		} else {
			super.removeCompareInputChangeListener(input, listener);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if (IFile.class.equals(adapter)) {
			IResource resource = Utils.getResource(getCompareResult());
			if (resource instanceof IFile) {
				return (T) resource;
			}
		}
		return super.getAdapter(adapter);
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
	public Viewer findContentViewer(Viewer oldViewer, ICompareInput input, Composite parent) {
		Viewer newViewer = super.findContentViewer(oldViewer, input, parent);
		boolean isNewViewer= newViewer != oldViewer;
		if (isNewViewer && newViewer instanceof IPropertyChangeNotifier && saveable instanceof IPropertyChangeListener) {
			// Register the model for change events if appropriate.
			final IPropertyChangeNotifier dsp= (IPropertyChangeNotifier) newViewer;
			final IPropertyChangeListener pcl = (IPropertyChangeListener) saveable;
			dsp.addPropertyChangeListener(pcl);
			Control c= newViewer.getControl();
			c.addDisposeListener(
				e -> dsp.removePropertyChangeListener(pcl)
			);
		}
		return newViewer;
	}

	@Override
	public boolean canRunAsJob() {
		return true;
	}

	@Override
	public boolean isDirty() {
		if (saveable != null)
			return saveable.isDirty();
		return super.isDirty();
	}

	@Override
	public void registerContextMenu(final MenuManager menu,	final ISelectionProvider selectionProvider) {
		super.registerContextMenu(menu, selectionProvider);
		final Saveable saveable = getSaveable();
		if (saveable instanceof LocalResourceSaveableComparison) {
			final ITypedElement element= getFileElement(getCompareInput(), this);
			menu.addMenuListener(manager -> SaveablesCompareEditorInput.handleMenuAboutToShow(manager, getContainer(), saveable, element, selectionProvider));
		}
	}
}
