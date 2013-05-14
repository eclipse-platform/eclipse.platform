/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.IPropertyChangeNotifier;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.IFindReplaceTarget;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTException;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorActionBarContributor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IReusableEditor;
import org.eclipse.ui.ISaveablesLifecycleListener;
import org.eclipse.ui.ISaveablesSource;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartConstants;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.Saveable;
import org.eclipse.ui.SaveablesLifecycleEvent;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.contexts.IContextService;
import org.eclipse.ui.part.EditorPart;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.PageBook;
import org.eclipse.ui.services.IServiceLocator;
import org.eclipse.ui.texteditor.ITextEditorExtension3;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;

/**
 * A CompareEditor takes a ICompareEditorInput as input.
 * Most functionality is delegated to the ICompareEditorInput.
 */
public class CompareEditor extends EditorPart implements IReusableEditor, ISaveablesSource, IPropertyChangeListener, ISaveablesLifecycleListener {

	public final static String CONFIRM_SAVE_PROPERTY= "org.eclipse.compare.internal.CONFIRM_SAVE_PROPERTY"; //$NON-NLS-1$

	private static final int UNINITIALIZED = 0;
	private static final int INITIALIZING = 1;
	private static final int NO_DIFF = 2;
	private static final int CANCELED = 3;
	private static final int INITIALIZED = 4;
	private static final int ERROR = 5;
	private static final int STILL_INITIALIZING = 6;
	private static final int CREATING_CONTROL = 7;
	private static final int DONE = 8;
	
	private IActionBars fActionBars;
	
	private PageBook fPageBook;
	
	/** the SWT control from the compare editor input*/
	private Control fControl;
	/** the outline page */
	private CompareOutlinePage fOutlinePage;

	private CompareSaveable fSaveable;

	private Control initializingPage;
	private Control emptyPage;
	
	private int state = UNINITIALIZED;
	private HashSet knownSaveables;

	private final EditorCompareContainer fContainer = new EditorCompareContainer();

	private class EditorCompareContainer extends CompareContainer {
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.ICompareContainer#registerContextMenu(org.eclipse.jface.action.MenuManager, org.eclipse.jface.viewers.ISelectionProvider)
		 */
		public void registerContextMenu(MenuManager menu, ISelectionProvider provider) {
			if (getSite() instanceof IEditorSite) {
				IEditorSite es = (IEditorSite) getSite();
				es.registerContextMenu(menu, provider, true);
			}
		}

		/* (non-Javadoc)
		 * @see org.eclipse.compare.ICompareContainer#setStatusMessage(java.lang.String)
		 */
		public void setStatusMessage(String message) {
			if (fActionBars != null) {
				IStatusLineManager slm= fActionBars.getStatusLineManager();
				if (slm != null) {
					slm.setMessage(message);
				}
			}
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.ICompareContainer#getServiceLocator()
		 */
		public IServiceLocator getServiceLocator() {
			return getSite();
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.internal.CompareContainer#createWorkerJob()
		 */
		protected WorkerJob createWorkerJob() {
			WorkerJob workerJob = new WorkerJob(getWorkerJobName()) {
				public boolean belongsTo(Object family) {
					if (family == CompareEditor.this)
						return true;
					return super.belongsTo(family);
				}
			};
			return workerJob;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.internal.CompareContainer#getWorkerJobName()
		 */
		protected String getWorkerJobName() {
			return NLS.bind(CompareMessages.CompareEditor_2, getTitle());
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.internal.CompareContainer#getWorkbenchPart()
		 */
		public IWorkbenchPart getWorkbenchPart() {
			return CompareEditor.this;
		}
		
		/* (non-Javadoc)
		 * @see org.eclipse.compare.internal.CompareContainer#getActionBars()
		 */
		public IActionBars getActionBars() {
			return CompareEditor.this.getActionBars();
		}
	}
	
	/**
	 * No-argument constructor required for extension points.
	 */
	public CompareEditor() {
		// empty default implementation
	}
	
	/* (non-Javadoc)
	 * Method declared on IAdaptable
	 */
	public Object getAdapter(Class key) {
		
		if (key.equals(IContentOutlinePage.class)) {
			Object object= getCompareConfiguration().getProperty(CompareConfiguration.USE_OUTLINE_VIEW);
			if (object instanceof Boolean && ((Boolean)object).booleanValue()) {
				if (fOutlinePage != null) {
					if (fOutlinePage.getControl() != null && fOutlinePage.getControl().isDisposed()) {
						fOutlinePage = null;
					} else {
						return fOutlinePage;
					}
				}
				fOutlinePage= new CompareOutlinePage(this);
				return fOutlinePage;
			}
		}
		
		if (key == IShowInSource.class
				|| key == OutlineViewerCreator.class
				|| key == IFindReplaceTarget.class) {
			Object input = getEditorInput();
			if (input != null) {
				return Utilities.getAdapter(input, key);
			}
		}
		
		if (key == IEditorInput.class) {
			return getEditorInput().getAdapter(IEditorInput.class);
		}
		
		if (key == ITextEditorExtension3.class) {
			return getEditorInput().getAdapter(ITextEditorExtension3.class);
		}

		return super.getAdapter(key);
	}
	
	/*
	 * Helper method used by ComapreEditorConfiguration to get at the compare configuration of the editor
	 */
	/* package */ CompareConfiguration getCompareConfiguration() {
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput)
			return ((CompareEditorInput)input).getCompareConfiguration();
		return null;
	}
				
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#init(org.eclipse.ui.IEditorSite, org.eclipse.ui.IEditorInput)
	 */
	public void init(IEditorSite site, IEditorInput input) throws PartInitException {
		
		if (!(input instanceof CompareEditorInput))
			throw new PartInitException(Utilities.getString("CompareEditor.invalidInput")); //$NON-NLS-1$
				
		setSite(site);
		setInput(input);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#setInput(org.eclipse.ui.IEditorInput)
	 */
	public void setInput(IEditorInput input) {
		if (!(input instanceof CompareEditorInput)) {
			IStatus s= new Status(IStatus.ERROR, PlatformUI.PLUGIN_ID, IStatus.OK, Utilities.getString("CompareEditor.invalidInput"), null); //$NON-NLS-1$
			String title= Utilities.getString("CompareEditor.error.setinput.title"); //$NON-NLS-1$
			String msg= Utilities.getString("CompareEditor.error.setinput.message"); //$NON-NLS-1$
			ErrorDialog.openError(getSite().getShell(), title, msg, s);
			return;
		}
        doSetInput(input);
        // Need to refresh the contributor (see #67888)
        refreshActionBarsContributor();
	}

	public void refreshActionBarsContributor() {
		IEditorSite editorSite= getEditorSite();
        if (editorSite != null) {
	        IEditorActionBarContributor actionBarContributor= editorSite.getActionBarContributor();
	        if (actionBarContributor != null) {
	        		actionBarContributor.setActiveEditor(null);
	        		actionBarContributor.setActiveEditor(this);
	        }
        }
	}
	
	private void doSetInput(IEditorInput input) {
		IEditorInput oldInput= getEditorInput();
		disconnectFromInput(oldInput);
		Point oldSize = null;
		boolean hadPreviousInput = oldInput != null;
		if (hadPreviousInput) {
			// Cancel any jobs associated with the old input
			Job.getJobManager().cancel(this);
			if (fControl != null && !fControl.isDisposed()) {
				oldSize= fControl.getSize();
				if (emptyPage == null)
					emptyPage = new Composite(fPageBook, SWT.NONE);
				fPageBook.showPage(emptyPage);
				fControl.dispose();
				fControl = null;
			}
		}
			
		super.setInput(input);
		
		if (fOutlinePage != null)
			fOutlinePage.reset();
		
		final CompareEditorInput cei= (CompareEditorInput) input;
		cei.setContainer(fContainer);
		setTitleImage(cei.getTitleImage());
		setPartName(cei.getTitle());
		setTitleToolTip(cei.getToolTipText());
				
		if (input instanceof IPropertyChangeNotifier)
			((IPropertyChangeNotifier)input).addPropertyChangeListener(this);
			
		setState(cei.getCompareResult() == null ? INITIALIZING : INITIALIZED);
		if (fPageBook != null)
			createCompareControl();
		if (fControl != null && oldSize != null)
			fControl.setSize(oldSize);
		
		boolean hasResult = cei.getCompareResult() != null;
		if (!hasResult) {
			initializeInBackground(cei, hadPreviousInput);
		}
        
        firePropertyChange(IWorkbenchPartConstants.PROP_INPUT);
        
        // We only need to notify of new Saveables if we are changing inputs
        if (hadPreviousInput && hasResult) {
        	registerSaveable();
        }
	}

	private void registerSaveable() {
		ISaveablesLifecycleListener lifecycleListener= (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);
		lifecycleListener.handleLifecycleEvent(
			new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_OPEN, internalGetSaveables(true), false));
	}

	private void disconnectFromInput(IEditorInput oldInput) {
		if (oldInput != null) {
			
			if (oldInput instanceof IPropertyChangeNotifier)
				((IPropertyChangeNotifier)oldInput).removePropertyChangeListener(this);
			
			// Let the workbench know that the old input's saveables are no longer needed
			if (knownSaveables != null && !knownSaveables.isEmpty()) {
				ISaveablesLifecycleListener lifecycleListener= (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);
				lifecycleListener.handleLifecycleEvent(
						new SaveablesLifecycleEvent(this, SaveablesLifecycleEvent.POST_CLOSE, (Saveable[]) knownSaveables.toArray(new Saveable[knownSaveables.size()]), false));
				knownSaveables.clear();
			}
		}
	}
	
	protected void initializeInBackground(final CompareEditorInput cei, final boolean hadPreviousInput) {
		// Need to cancel any running jobs associated with the oldInput
		Job job = new Job(NLS.bind(CompareMessages.CompareEditor_0, cei.getTitle())) {
			protected IStatus run(final IProgressMonitor monitor) {
				final int[] newState = new int[] { ERROR };
				try {
					IStatus status = CompareUIPlugin.getDefault().prepareInput(cei, monitor);
					if (status.isOK()) {
						// We need to update the saveables list
						newState[0] = INITIALIZED;
						return Status.OK_STATUS;
					}
					if (status.getCode() == CompareUIPlugin.NO_DIFFERENCE) {
						newState[0] = NO_DIFF;
						return Status.OK_STATUS;
					}
					newState[0] = ERROR;
					return status;
				} catch (OperationCanceledException e) {
					newState[0] = CANCELED;
					return Status.CANCEL_STATUS;
				} finally {
					if (monitor.isCanceled())
						newState[0] = CANCELED;
					Display.getDefault().syncExec(new Runnable() {
						public void run() {
							if (fPageBook.isDisposed())
								return;
							// we need to register the saveable if we had a previous input or if
							// there are knownSaveables (which means that the workbench called
							// getSaveables and got an empty list
							if (hadPreviousInput || (knownSaveables != null && !isAllSaveablesKnown())) {
								registerSaveable();
							}
							setState(newState[0]);
							createCompareControl();
						}
					});
					monitor.done();
				}
			}
			public boolean belongsTo(Object family) {
				if (family == CompareEditor.this || family == cei)
					return true;
				return cei.belongsTo(family);
			}
		};
		job.setUser(true);
		Utilities.schedule(job, getSite());
	}

	/*
	 * Helper method used to find an action bars using the Utilities#findActionsBars(Control)
	 */
	public IActionBars getActionBars() {
		return fActionBars;
	}
	
	/*
	 * Set the action bars so the Utilities class can access it.
	 */
	/* package */ void setActionBars(IActionBars actionBars) {
		fActionBars= actionBars;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#createPartControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createPartControl(Composite parent) {
		parent.setData(this);
		fPageBook = new PageBook(parent, SWT.NONE);
		createCompareControl();
		IContextService service = (IContextService)getSite().getService(IContextService.class);
		if (service != null) {
			service.activateContext("org.eclipse.compare.compareEditorScope"); //$NON-NLS-1$
			service.activateContext("org.eclipse.ui.textEditorScope"); //$NON-NLS-1$
		}
	}

	private void createCompareControl() {
		if (fPageBook.isDisposed())
			return;
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput) {
			CompareEditorInput ci = (CompareEditorInput) input;
			if (ci.getCompareResult() == null) {
				if (getState() == INITIALIZING) {
					getSite().setSelectionProvider(new CompareEditorSelectionProvider());
					setPageLater();
				} else if (getState() == STILL_INITIALIZING) {
					if (initializingPage == null) {
						initializingPage = getInitializingMessagePane(fPageBook);
					}
					fPageBook.showPage(initializingPage);
				} else if (getState() == CANCELED) {
					// Close the editor when we are canceled
					closeEditor();
				} else if (getState() == NO_DIFF) {
					// Prompt and close the editor as well
					setState(DONE);
					closeEditor();
					CompareUIPlugin.getDefault().handleNoDifference();
				} else if (getState() == ERROR) {
					// If an error occurred, close the editor
					// (the message would be displayed by the progress view)
					closeEditor();
				}
			} else if (fControl == null && getState() != CREATING_CONTROL) {
				if (getState() == CANCELED) {
					// Close the editor when we are canceled, even when compare
					// result has been already prepared
					closeEditor();
					return;
				}
				// Set the state in case this method gets called again
				setState(CREATING_CONTROL);
				if (getSite().getSelectionProvider() == null)
					getSite().setSelectionProvider(new CompareEditorSelectionProvider());
				try {
					fControl = ci.createContents(fPageBook);
				} catch (SWTException e) {
					// closed while creating
					if (e.code == SWT.ERROR_WIDGET_DISPOSED) {
						setState(CANCELED);
						return;
					}
				}
				fPageBook.showPage(fControl);
				PlatformUI.getWorkbench().getHelpSystem().setHelp(fControl, ICompareContextIds.COMPARE_EDITOR);
				if (isActive()) {
					setFocus();
				}
				setState(INITIALIZED);
			}
		}
	}
	
	private boolean isActive() {
		return getSite().getPage().getActivePart() == this;
	}

	private void setPageLater() {
		Display.getCurrent().timerExec(1000, new Runnable() {
			public void run() {
				synchronized(CompareEditor.this) {
					if (getState() == INITIALIZING) {
						setState(STILL_INITIALIZING);
						createCompareControl();
					}
				}
			}
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	public void dispose() {
		IEditorInput input= getEditorInput();
		if (input instanceof IPropertyChangeNotifier)
			((IPropertyChangeNotifier)input).removePropertyChangeListener(this);
		super.dispose();
	}
			
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.WorkbenchPart#setFocus()
	 */
	public void setFocus() {
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput)
			if (!((CompareEditorInput)input).setFocus2())
				fPageBook.setFocus();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isSaveAsAllowed()
	 */
	public boolean isSaveAsAllowed() {
		return false;
	}
	
	/* (non-Javadoc)
	 * Always throws an AssertionFailedException.
	 * @see org.eclipse.ui.part.EditorPart#doSaveAs()
	 */
	public void doSaveAs() {
		Assert.isTrue(false); // Save As not supported for CompareEditor
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#doSave(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void doSave(IProgressMonitor progressMonitor) {
		
		final IEditorInput input= getEditorInput();
		
		WorkspaceModifyOperation operation= new WorkspaceModifyOperation() {
			public void execute(IProgressMonitor pm) throws CoreException {
				if (input instanceof CompareEditorInput)
					((CompareEditorInput)input).saveChanges(pm);
			}
		};

		Shell shell= getSite().getShell();
		
		try {
			
			operation.run(progressMonitor);
									
			firePropertyChange(PROP_DIRTY);
			
		} catch (InterruptedException x) {
			// NeedWork
		} catch (OperationCanceledException x) {
			// NeedWork
		} catch (InvocationTargetException x) {
			String title= Utilities.getString("CompareEditor.saveError.title"); //$NON-NLS-1$
			String reason= x.getTargetException().getMessage();
			MessageDialog.openError(shell, title, Utilities.getFormattedString("CompareEditor.cantSaveError", reason));	//$NON-NLS-1$
		}
	}
		
	/* (non-Javadoc)
	 * @see org.eclipse.ui.part.EditorPart#isDirty()
	 */
	public boolean isDirty() {
		IEditorInput input= getEditorInput();
		if (input instanceof CompareEditorInput)
			return ((CompareEditorInput)input).isDirty();
		return false;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.util.IPropertyChangeListener#propertyChange(org.eclipse.jface.util.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent event) {
		if (event.getProperty().equals(CompareEditorInput.DIRTY_STATE)) {
			Object old_value= event.getOldValue();
			Object new_value= event.getNewValue();
			if (old_value == null || new_value == null || !old_value.equals(new_value))
				firePropertyChange(PROP_DIRTY);
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE)) {
			setPartName(((CompareEditorInput)getEditorInput()).getTitle());
			setTitleToolTip(((CompareEditorInput)getEditorInput()).getToolTipText());
		} else if (event.getProperty().equals(CompareEditorInput.PROP_TITLE_IMAGE)) {
			setTitleImage(((CompareEditorInput)getEditorInput()).getTitleImage());
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getModels()
	 */
	public Saveable[] getSaveables() {
		return internalGetSaveables(knownSaveables == null);
	}

	private Saveable[] internalGetSaveables(boolean init) {
		IEditorInput input= getEditorInput();
		Saveable[] sourceSaveables = getSaveables(input);
		if (init || knownSaveables == null) {
			recordSaveables(sourceSaveables);
		} else {
			for (int i = 0; i < sourceSaveables.length; i++) {
				Saveable saveable = sourceSaveables[i];
				if (!knownSaveables.contains(saveable)) {
					CompareUIPlugin.logErrorMessage(NLS.bind("Saveable {0} was not added using a saveables lifecycle event.", saveable.getName())); //$NON-NLS-1$
					knownSaveables.add(saveable);
				}
			}
			if (sourceSaveables.length != knownSaveables.size()) {
				CompareUIPlugin.logErrorMessage("Saveables were removed without an appropriate event"); //$NON-NLS-1$
				knownSaveables.clear();
				recordSaveables(sourceSaveables);
			}
		}
		return sourceSaveables;
	}
	
	private boolean isAllSaveablesKnown() {
		IEditorInput input= getEditorInput();
		Saveable[] sourceSaveables = getSaveables(input);
		if (knownSaveables == null) {
			return sourceSaveables.length == 0;
		}
		if (sourceSaveables.length != knownSaveables.size()) {
			return false;
		}
		for (int i = 0; i < sourceSaveables.length; i++) {
			Saveable saveable = sourceSaveables[i];
			if (!knownSaveables.contains(saveable)) {
				return false;
			}
		}
		return true;
	}

	private void recordSaveables(Saveable[] sourceSaveables) {
		if (knownSaveables == null)
			knownSaveables = new HashSet();
		for (int i = 0; i < sourceSaveables.length; i++) {
			Saveable saveable = sourceSaveables[i];
			knownSaveables.add(saveable);
		}
	}

	private Saveable[] getSaveables(IEditorInput input) {
		if (input instanceof ISaveablesSource) {
			ISaveablesSource source= (ISaveablesSource) input;
			return source.getSaveables();
		}
		return new Saveable[] { getSaveable() };
	}

	private Saveable getSaveable() {
		if (fSaveable == null) {
			fSaveable= new CompareSaveable();
		}
		return fSaveable;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISaveablesSource#getActiveModels()
	 */
	public Saveable[] getActiveSaveables() {
		IEditorInput input= getEditorInput();
		if (input instanceof ISaveablesSource) {
			ISaveablesSource source= (ISaveablesSource) input;
			return source.getActiveSaveables();
		}
		return new Saveable[] { getSaveable() };
	}
	
	private class CompareSaveable extends Saveable {

		public String getName() {
			return CompareEditor.this.getPartName();
		}

		public String getToolTipText() {
			return CompareEditor.this.getTitleToolTip();
		}

		public ImageDescriptor getImageDescriptor() {
			return ImageDescriptor.createFromImage(CompareEditor.this.getTitleImage());
		}

		public void doSave(IProgressMonitor monitor) throws CoreException {
			CompareEditor.this.doSave(monitor);
		}

		public boolean isDirty() {
			return CompareEditor.this.isDirty();
		}

		public boolean equals(Object object) {
			return object == this;
		}

		public int hashCode() {
			return CompareEditor.this.hashCode();
		}
	}
	
	private Composite getInitializingMessagePane(Composite parent) {
		Composite composite = new Composite(parent, SWT.NONE);
		composite.setBackground(getBackgroundColor(parent));
		GridLayout layout = new GridLayout();
		layout.numColumns = 3;
		composite.setLayout(layout);
		
		createDescriptionLabel(composite, CompareMessages.CompareEditor_1);
		return composite;
	}
	
	private Color getBackgroundColor(Composite parent) {
		return parent.getDisplay().getSystemColor(SWT.COLOR_LIST_BACKGROUND);
	}
	
	private Label createDescriptionLabel(Composite parent, String text) {
		Label description = new Label(parent, SWT.WRAP);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		data.horizontalSpan = 2;
		description.setLayoutData(data);
		description.setText(text);
		description.setBackground(getBackgroundColor(parent));
		return description;
	}
	
	private void closeEditor() {
		getSite().getPage().closeEditor(CompareEditor.this, false);
	}

	private synchronized void setState(int state) {
		this.state = state;
	}

	private int getState() {
		return state;
	}

	public void handleLifecycleEvent(SaveablesLifecycleEvent event) {
		ISaveablesLifecycleListener lifecycleListener= (ISaveablesLifecycleListener) getSite().getService(ISaveablesLifecycleListener.class);
		if (event.getEventType() == SaveablesLifecycleEvent.POST_CLOSE) {
			// We may get a post close for a saveable that is not known to the workbench.
			// Only pass on the event for known saveables
			if (knownSaveables == null || knownSaveables.isEmpty())
				return;
			java.util.List result = new ArrayList();
			Saveable[] all = event.getSaveables();
			for (int i = 0; i < all.length; i++) {
				Saveable saveable = all[i];
				if (knownSaveables.contains(saveable))
					result.add(saveable);
					knownSaveables.remove(saveable);
			}
			if (result.isEmpty())
				return;
			event = new SaveablesLifecycleEvent(this,
					SaveablesLifecycleEvent.POST_CLOSE,
					(Saveable[]) result.toArray(new Saveable[result.size()]),
					false);
		} else if (event.getEventType() == SaveablesLifecycleEvent.POST_OPEN) {
			recordSaveables(event.getSaveables());
		}
		lifecycleListener.handleLifecycleEvent(event);
	}
	
}

