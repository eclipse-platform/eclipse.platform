/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.history;

import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.ITypedElement;
import org.eclipse.compare.structuremergeviewer.DiffNode;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.util.OpenStrategy;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.core.history.LocalFileHistory;
import org.eclipse.team.internal.core.history.LocalFileRevision;
import org.eclipse.team.internal.ui.IHelpContextIds;
import org.eclipse.team.internal.ui.IPreferenceIds;
import org.eclipse.team.internal.ui.ITeamUIImages;
import org.eclipse.team.internal.ui.Policy;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.actions.CompareRevisionAction;
import org.eclipse.team.internal.ui.actions.OpenRevisionAction;
import org.eclipse.team.internal.ui.actions.OpenWithMenu;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryCompareAdapter;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.team.ui.synchronize.SaveableCompareEditorInput;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.OpenAndLinkWithEditorHelper;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.progress.IProgressConstants;

public class LocalHistoryPage extends HistoryPage implements IHistoryCompareAdapter {
	public static final int ON = 1;
	public static final int OFF = 2;
	public static final int ALWAYS = 4;

	/* private */ IFile file;
	/* private */ IFileRevision currentFileRevision;

	// cached for efficiency
	/* private */ LocalFileHistory localFileHistory;

	/* private */ TreeViewer treeViewer;

	/* private */boolean shutdown = false;

	//grouping on
	private boolean groupingOn = true;

	//toggle constants for default click action
	private int compareMode = OFF;

	protected LocalFileHistoryTableProvider historyTableProvider;
	private RefreshFileHistory refreshFileHistoryJob;
	private Composite localComposite;
	private Action groupByDateMode;
	private Action collapseAll;
	private Action compareModeAction;
	private Action getContentsAction;
	private CompareRevisionAction compareAction;
	private OpenRevisionAction openAction;
	private OpenWithMenu openWithMenu;

	private HistoryResourceListener resourceListener;

	private IFileRevision currentSelection;

	private final class LocalHistoryContentProvider implements ITreeContentProvider {
		@Override
		public Object[] getElements(Object inputElement) {
			if (inputElement instanceof IFileHistory) {
				// The entries of already been fetch so return them
				IFileHistory fileHistory = (IFileHistory) inputElement;
				return fileHistory.getFileRevisions();
			}
			if (inputElement instanceof IFileRevision[]) {
				return (IFileRevision[]) inputElement;
			}
			if (inputElement instanceof AbstractHistoryCategory[]){
				return (AbstractHistoryCategory[]) inputElement;
			}
			return new Object[0];
		}

		@Override
		public void dispose() {
			// Nothing to do
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			// Nothing to do
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement instanceof AbstractHistoryCategory){
				return ((AbstractHistoryCategory) parentElement).getRevisions();
			}

			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			if (element instanceof AbstractHistoryCategory){
				return ((AbstractHistoryCategory) element).hasRevisions();
			}
			return false;
		}
	}

	private class LocalFileHistoryTableProvider extends LocalHistoryTableProvider {
		protected IFileRevision adaptToFileRevision(Object element) {
			// Get the log entry for the provided object
			IFileRevision entry = null;
			if (element instanceof IFileRevision) {
				entry = (IFileRevision) element;
			} else if (element instanceof IAdaptable) {
				entry = ((IAdaptable) element).getAdapter(IFileRevision.class);
			} else if (element instanceof AbstractHistoryCategory){
				IFileRevision[] revisions = ((AbstractHistoryCategory) element).getRevisions();
				if (revisions.length > 0)
					entry = revisions[0];
			}
			return entry;
		}
		private long getCurrentRevision() {
			if (file != null) {
				return file.getLocalTimeStamp();
			}
			return -1;
		}

		@Override
		protected long getModificationDate(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry != null)
				return entry.getTimestamp();
			return -1;
		}

		@Override
		protected boolean isCurrentEdition(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			if (entry == null)
				return false;
			long timestamp = entry.getTimestamp();
			long tempCurrentTimeStamp = getCurrentRevision();
			return (tempCurrentTimeStamp != -1 && tempCurrentTimeStamp==timestamp);
		}

		@Override
		protected boolean isDeletedEdition(Object element) {
			IFileRevision entry = adaptToFileRevision(element);
			return (!entry.exists());
		}
	}

	private class RefreshFileHistory extends Job {
		public RefreshFileHistory() {
			super(TeamUIMessages.LocalHistoryPage_FetchLocalHistoryMessage);
		}
		@Override
		public IStatus run(IProgressMonitor monitor)  {
			try {
				IStatus status = Status.OK_STATUS;
				// Assign the instance variable to a local so it does not get cleared well we are refreshing
				LocalFileHistory fileHistory = localFileHistory;
				if (fileHistory == null || shutdown)
					return status;
				try {
					fileHistory.refresh(Policy.subMonitorFor(monitor, 50));
				} catch (CoreException ex) {
					status = new TeamStatus(ex.getStatus().getSeverity(), TeamUIPlugin.ID, ex.getStatus().getCode(), ex.getMessage(), ex, file);
				}

				update(fileHistory.getFileRevisions(), Policy.subMonitorFor(monitor, 50));

				if (status != Status.OK_STATUS ) {
					this.setProperty(IProgressConstants.KEEP_PROPERTY, Boolean.TRUE);
					this.setProperty(IProgressConstants.NO_IMMEDIATE_ERROR_PROMPT_PROPERTY, Boolean.TRUE);
				}

				if (Policy.DEBUG_HISTORY) {
					String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
					System.out.println(time + ": RefreshFileHistoryJob#run finished, status: " + status); //$NON-NLS-1$
				}

				return status;
			} finally {
				monitor.done();
			}
		}
	}

	private class HistoryResourceListener implements IResourceChangeListener {
		@Override
		public void resourceChanged(IResourceChangeEvent event) {
			IResourceDelta root = event.getDelta();

			if (file == null)
				return;

			IResourceDelta resourceDelta = root.findMember(file.getFullPath());
			if (resourceDelta != null){
				Display.getDefault().asyncExec(() -> refresh());
			}
		}
	}

	public LocalHistoryPage(int compareMode) {
		super();
		this.compareMode = compareMode;
	}

	public LocalHistoryPage() {
		super();
	}

	@Override
	public boolean inputSet() {
		currentFileRevision = null;
		IFile tempFile = getFile();
		this.file = tempFile;
		if (tempFile == null)
			return false;

		//blank current input only after we're sure that we have a file
		//to fetch history for
		this.treeViewer.setInput(null);

		localFileHistory = new LocalFileHistory(file, !getHistoryPageSite().isModal());

		if (refreshFileHistoryJob == null)
			refreshFileHistoryJob = new RefreshFileHistory();

		//always refresh the history if the input gets set
		refreshHistory(true);
		return true;
	}

	protected IFile getFile() {
		return LocalHistoryPageSource.getFile(getInput());
	}

	private void refreshHistory(boolean refetch) {
		if (Policy.DEBUG_HISTORY) {
			String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
			System.out.println(time + ": LocalHistoryPage#refreshHistory, refetch = " + refetch); //$NON-NLS-1$
		}

		if (refreshFileHistoryJob.getState() != Job.NONE){
			refreshFileHistoryJob.cancel();
		}
		IHistoryPageSite parentSite = getHistoryPageSite();
		Utils.schedule(refreshFileHistoryJob, getWorkbenchSite(parentSite));
	}

	private IWorkbenchPartSite getWorkbenchSite(IHistoryPageSite parentSite) {
		IWorkbenchPart part = parentSite.getPart();
		if (part != null)
			return part.getSite();
		return null;
	}

	@Override
	public void createControl(Composite parent) {

		localComposite = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		localComposite.setLayout(layout);
		GridData data = new GridData(GridData.FILL_BOTH);
		data.grabExcessVerticalSpace = true;
		localComposite.setLayoutData(data);

		treeViewer = createTree(localComposite);

		contributeActions();

		IHistoryPageSite parentSite = getHistoryPageSite();
		if (parentSite != null && parentSite instanceof DialogHistoryPageSite && treeViewer != null)
			parentSite.setSelectionProvider(treeViewer);

		resourceListener = new HistoryResourceListener();
		ResourcesPlugin.getWorkspace().addResourceChangeListener(resourceListener, IResourceChangeEvent.POST_CHANGE);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(localComposite, IHelpContextIds.LOCAL_HISTORY_PAGE);
	}

	private void contributeActions() {
		final IPreferenceStore store = TeamUIPlugin.getPlugin().getPreferenceStore();
		//Group by Date
		groupByDateMode = new Action(TeamUIMessages.LocalHistoryPage_GroupRevisionsByDateAction, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY)){
			@Override
			public void run() {
				groupingOn = !groupingOn;
				store.setValue(IPreferenceIds.PREF_GROUPBYDATE_MODE, groupingOn);
				refreshHistory(false);
			}
		};
		groupingOn = store.getBoolean(IPreferenceIds.PREF_GROUPBYDATE_MODE);
		groupByDateMode.setChecked(groupingOn);
		groupByDateMode.setToolTipText(TeamUIMessages.LocalHistoryPage_GroupRevisionsByDateTip);
		groupByDateMode.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY));
		groupByDateMode.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_DATES_CATEGORY));

		//Collapse All
		collapseAll =  new Action(TeamUIMessages.LocalHistoryPage_CollapseAllAction, TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL)) {
			@Override
			public void run() {
				treeViewer.collapseAll();
			}
		};
		collapseAll.setToolTipText(TeamUIMessages.LocalHistoryPage_CollapseAllTip);
		collapseAll.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL));
		collapseAll.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COLLAPSE_ALL));

		IHistoryPageSite historyPageSite = getHistoryPageSite();
		if (!historyPageSite.isModal()) {
			//Compare Mode Action
			if ((compareMode & ALWAYS) == 0) {
				compareModeAction = new Action(TeamUIMessages.LocalHistoryPage_CompareModeAction,TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW)) {
					@Override
					public void run() {
						// switch the mode
						compareMode = compareMode == ON ? OFF : ON;
						compareModeAction.setChecked(compareMode == ON);
					}
				};
				compareModeAction.setToolTipText(TeamUIMessages.LocalHistoryPage_CompareModeTip);
				compareModeAction.setDisabledImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW));
				compareModeAction.setHoverImageDescriptor(TeamUIPlugin.getImageDescriptor(ITeamUIImages.IMG_COMPARE_VIEW));
				compareModeAction.setChecked(compareMode == ON);

				getContentsAction = getContextMenuAction(TeamUIMessages.LocalHistoryPage_GetContents, true /* needs progress */, monitor -> {
					SubMonitor progress = SubMonitor.convert(monitor, 2);
					try {
						if (confirmOverwrite()) {
							IStorage currentStorage = currentSelection.getStorage(progress.split(1));
							InputStream in = currentStorage.getContents();
							file.setContents(in, false, true, progress.split(1));
						}
					} catch (TeamException e) {
						throw new CoreException(e.getStatus());
					} finally {
						monitor.done();
					}
				});
			}

			//TODO: Doc help
			//PlatformUI.getWorkbench().getHelpSystem().setHelp(getContentsAction, );

			// Click Compare action
			compareAction = createCompareAction();
			compareAction.setEnabled(!treeViewer.getSelection().isEmpty());
			treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
				@Override
				public void widgetSelected(SelectionEvent e) {
					compareAction.setCurrentFileRevision(getCurrentFileRevision());
					compareAction.selectionChanged(treeViewer.getStructuredSelection());
				}
			});

			// Only add the open action if compare mode is not always on
			if (!((compareMode & (ALWAYS | ON)) == (ALWAYS | ON))) {
				openAction = new OpenRevisionAction(TeamUIMessages.LocalHistoryPage_OpenAction, this);
				openAction.setEnabled(!treeViewer.getSelection().isEmpty());
				treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						openAction.selectionChanged(treeViewer.getStructuredSelection());
					}
				});

				// Add 'Open With...'  sub-menu
				openWithMenu = new OpenWithMenu(this);
				treeViewer.getTree().addSelectionListener(new SelectionAdapter(){
					@Override
					public void widgetSelected(SelectionEvent e) {
						openWithMenu.selectionChanged(treeViewer.getStructuredSelection());
					}
				});
			}

			new OpenAndLinkWithEditorHelper(treeViewer) {
				@Override
				protected void open(ISelection selection, boolean activate) {
					if (getSite() != null && selection instanceof IStructuredSelection) {
						IStructuredSelection structuredSelection= (IStructuredSelection)selection;
						if ((compareMode & ON) > 0){
							StructuredSelection sel= new StructuredSelection(new Object[] { getCurrentFileRevision(), structuredSelection.getFirstElement() });
							compareAction.selectionChanged(sel);
							compareAction.run();
						} else {
							//Pass in the entire structured selection to allow for multiple editor openings
							if (openAction != null) {
								openAction.selectionChanged(structuredSelection);
								openAction.run();
							}
						}
					}
				}

				@Override
				protected void activate(ISelection selection) {
					int currentMode= OpenStrategy.getOpenMethod();
					try {
						OpenStrategy.setOpenMethod(OpenStrategy.DOUBLE_CLICK);
						open(selection, true);
					} finally {
						OpenStrategy.setOpenMethod(currentMode);
					}
				}

				@Override
				protected void linkToEditor(ISelection selection) {
					// XXX: Not yet implemented, see http://bugs.eclipse.org/324185
				}
			};

			//Contribute actions to popup menu
			MenuManager menuMgr = new MenuManager();
			Menu menu = menuMgr.createContextMenu(treeViewer.getTree());
			menuMgr.addMenuListener(menuMgr1 -> fillTableMenu(menuMgr1));
			menuMgr.setRemoveAllWhenShown(true);
			treeViewer.getTree().setMenu(menu);

			//Don't add the object contribution menu items if this page is hosted in a dialog
			IWorkbenchPart part = historyPageSite.getPart();
			if (part != null) {
				IWorkbenchPartSite workbenchPartSite = part.getSite();
				workbenchPartSite.registerContextMenu(menuMgr, treeViewer);
			}
			IPageSite pageSite = historyPageSite.getWorkbenchPageSite();
			if (pageSite != null) {
				IActionBars actionBars = pageSite.getActionBars();
				// Contribute toggle text visible to the toolbar drop-down
				IMenuManager actionBarsMenu = actionBars.getMenuManager();
				if (actionBarsMenu != null){
					actionBarsMenu.removeAll();
				}
				actionBars.updateActionBars();
			}
		}

		//Create the local tool bar
		IToolBarManager tbm = historyPageSite.getToolBarManager();
		if (tbm != null) {
			String fileNameQualifier = getFileNameQualifier();
			//Add groups
			tbm.add(new Separator(fileNameQualifier+"grouping"));	//$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"grouping", groupByDateMode); //$NON-NLS-1$
			tbm.add(new Separator(fileNameQualifier+"collapse")); //$NON-NLS-1$
			tbm.appendToGroup(fileNameQualifier+"collapse", collapseAll); //$NON-NLS-1$
			if (compareModeAction != null)
				tbm.appendToGroup(fileNameQualifier+"collapse", compareModeAction);  //$NON-NLS-1$
			tbm.update(false);
		}

	}

	protected CompareRevisionAction createCompareAction() {
		return new CompareRevisionAction(this);
	}

	private String getFileNameQualifier() {
		if (file != null)
			return file.getFullPath().toString();

		return ""; //$NON-NLS-1$
	}

	protected void fillTableMenu(IMenuManager manager) {
		// file actions go first (view file)
		IHistoryPageSite parentSite = getHistoryPageSite();
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));

		if (file != null && !parentSite.isModal()){
			if (openAction != null)
				manager.add(openAction);
			if (openWithMenu != null) {
				MenuManager openWithSubmenu = new MenuManager(
						TeamUIMessages.LocalHistoryPage_OpenWithMenu);
				openWithSubmenu.add(openWithMenu);
				manager.add(openWithSubmenu);
			}
			if (compareAction != null)
				manager.add(compareAction);
			if (getContentsAction != null) {
				ISelection sel = treeViewer.getSelection();
				if (!sel.isEmpty()) {
					if (sel instanceof IStructuredSelection) {
						IStructuredSelection tempSelection = (IStructuredSelection) sel;
						if (tempSelection.size() == 1) {
							manager.add(new Separator("getContents")); //$NON-NLS-1$
							manager.add(getContentsAction);
						}
					}
				}
			}
		}
	}

	/**
	 * Creates the tree that displays the local file revisions
	 *
	 * @param parent the parent composite to contain the group
	 * @return the group control
	 */
	protected TreeViewer createTree(Composite parent) {
		historyTableProvider = new LocalFileHistoryTableProvider();
		TreeViewer viewer = historyTableProvider.createTree(parent);
		viewer.setContentProvider(new LocalHistoryContentProvider());
		return viewer;
	}

	@Override
	public Control getControl() {
		return localComposite;
	}

	@Override
	public void setFocus() {
		localComposite.setFocus();
	}

	@Override
	public String getDescription() {
		if (getFile() != null)
			return getFile().getFullPath().toString();
		return null;
	}

	@Override
	public String getName() {
		if (getFile() != null)
			return getFile().getName();
		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean isValidInput(Object object) {
		return object instanceof IFile;
	}

	@Override
	public void refresh() {
		refreshHistory(true);
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T> T getAdapter(Class<T> adapter) {
		if(adapter == IHistoryCompareAdapter.class) {
			return (T) this;
		}
		return null;
	}

	@Override
	public void dispose() {
		shutdown = true;

		if (resourceListener != null){
			ResourcesPlugin.getWorkspace().removeResourceChangeListener(resourceListener);
			resourceListener = null;
		}

		//Cancel any incoming
		if (refreshFileHistoryJob != null) {
			if (refreshFileHistoryJob.getState() != Job.NONE) {
				refreshFileHistoryJob.cancel();
			}
		}
	}

	public IFileRevision getCurrentFileRevision() {
		if (currentFileRevision != null)
			return currentFileRevision;

		if (file != null)
			currentFileRevision = new LocalFileRevision(file);

		return currentFileRevision;
	}

	private Action getContextMenuAction(String title, final boolean needsProgressDialog, final IWorkspaceRunnable action) {
		return new Action(title) {
			@Override
			public void run() {
				try {
					if (file == null) return;
					ISelection selection = treeViewer.getSelection();
					if (!(selection instanceof IStructuredSelection)) return;
					IStructuredSelection ss = (IStructuredSelection)selection;
					Object o = ss.getFirstElement();

					if (o instanceof AbstractHistoryCategory)
						return;

					currentSelection = (IFileRevision)o;
					if(needsProgressDialog) {
						PlatformUI.getWorkbench().getProgressService().run(true, true, monitor -> {
							try {
								action.run(monitor);
							} catch (CoreException e) {
								throw new InvocationTargetException(e);
							}
						});
					} else {
						try {
							action.run(null);
						} catch (CoreException e) {
							throw new InvocationTargetException(e);
						}
					}
				} catch (InvocationTargetException e) {
					IHistoryPageSite parentSite = getHistoryPageSite();
					Utils.handleError(parentSite.getShell(), e, null, null);
				} catch (InterruptedException e) {
					// Do nothing
				}
			}

			@Override
			public boolean isEnabled() {
				ISelection selection = treeViewer.getSelection();
				if (!(selection instanceof IStructuredSelection)) return false;
				IStructuredSelection ss = (IStructuredSelection)selection;
				if(ss.size() != 1) return false;
				return true;
			}
		};
	}

	private boolean confirmOverwrite() {
		if (file != null && file.exists()) {
			String title = TeamUIMessages.LocalHistoryPage_OverwriteTitle;
			String msg = TeamUIMessages.LocalHistoryPage_OverwriteMessage;
			IHistoryPageSite parentSite = getHistoryPageSite();
			final MessageDialog dialog = new MessageDialog(parentSite.getShell(), title, null, msg, MessageDialog.QUESTION, new String[] {IDialogConstants.YES_LABEL, IDialogConstants.CANCEL_LABEL}, 0);
			final int[] result = new int[1];
			parentSite.getShell().getDisplay().syncExec(() -> result[0] = dialog.open());
			if (result[0] != 0) {
				// cancel
				return false;
			}
		}
		return true;
	}

	public void setClickAction(boolean compare) {
		compareMode = compare ? ON : OFF;
		if (compareModeAction != null)
			compareModeAction.setChecked(compareMode == ON);
	}

	@Override
	public ICompareInput getCompareInput(Object object) {
		if (object instanceof IFileRevision){
			IFileRevision selectedFileRevision = (IFileRevision)object;
			ITypedElement fileElement = SaveableCompareEditorInput.createFileElement(file);
			FileRevisionTypedElement right = new FileRevisionTypedElement(selectedFileRevision);
			DiffNode node = new DiffNode(fileElement, right);
			return node;
		}
		return null;
	}

	@Override
	public void prepareInput(ICompareInput input, CompareConfiguration configuration, IProgressMonitor monitor) {
		Object right = input.getRight();
		if (right != null) {
			String label = getLabel(right);
			if (label != null)
				configuration.setRightLabel(label);
			Image image = getImage(right);
			if (image != null)
				configuration.setRightImage(image);
		}
		Object left = input.getLeft();
		if (left != null) {
			String label = getLabel(left);
			if (label != null)
				configuration.setLeftLabel(label);
			Image image = getImage(left);
			if (image != null)
				configuration.setLeftImage(image);
		}
	}

	protected Image getImage(Object right) {
		if (right instanceof FileRevisionTypedElement || right instanceof LocalFileRevision || right instanceof IFileRevision) {
			return historyTableProvider.getRevisionImage();
		}
		if (right instanceof ITypedElement) {
			ITypedElement te = (ITypedElement) right;
			return te.getImage();
		}
		return null;
	}

	protected String getLabel(Object object) {
		if (object instanceof IFileRevision) {
			IFileRevision revision = (IFileRevision) object;
			long timestamp = revision.getTimestamp();
			if (timestamp > 0)
			return NLS.bind(TeamUIMessages.LocalHistoryPage_0, historyTableProvider.getDateFormat().format(new Date(timestamp)));
		}
		if (object instanceof FileRevisionTypedElement) {
			FileRevisionTypedElement e = (FileRevisionTypedElement) object;
			return getLabel(e.getRevision());
		}
		if (object instanceof LocalResourceTypedElement) {
			return TeamUIMessages.LocalHistoryPage_1;
		}
		return null;
	}

	/**
	 * Method invoked from a background thread to update the viewer with the given revisions.
	 * @param revisions the revisions for the file
	 * @param monitor a progress monitor
	 */
	protected void update(final IFileRevision[] revisions, IProgressMonitor monitor) {
		// Group the revisions (if appropriate) before running in the UI thread
		final AbstractHistoryCategory[] categories = groupRevisions(revisions, monitor);
		// Update the tree in the UI thread
		Utils.asyncExec((Runnable) () -> {
			if (Policy.DEBUG_HISTORY) {
				String time = new SimpleDateFormat("m:ss.SSS").format(new Date(System.currentTimeMillis())); //$NON-NLS-1$
				System.out.println(time + ": LocalHistoryPage#update, the tree is being updated in the UI thread"); //$NON-NLS-1$
			}
			if (categories != null) {
				Object[] elementsToExpand = mapExpandedElements(categories, treeViewer.getExpandedElements());
				treeViewer.getTree().setRedraw(false);
				treeViewer.setInput(categories);
				//if user is switching modes and already has expanded elements
				//selected try to expand those, else expand all
				if (elementsToExpand.length > 0)
					treeViewer.setExpandedElements(elementsToExpand);
				else {
					treeViewer.expandAll();
					Object[] el = treeViewer.getExpandedElements();
					if (el != null && el.length > 0) {
						treeViewer.setSelection(new StructuredSelection(el[0]));
						treeViewer.getTree().deselectAll();
					}
				}
				treeViewer.getTree().setRedraw(true);
			} else {
				if (revisions.length > 0) {
					treeViewer.setInput(revisions);
				} else {
					treeViewer.setInput(new AbstractHistoryCategory[] {getErrorMessage()});
				}
			}
		}, treeViewer);
	}

	private AbstractHistoryCategory[] groupRevisions(IFileRevision[] revisions, IProgressMonitor monitor) {
		if (groupingOn)
			return sortRevisions(revisions, monitor);
		return null;
	}

	private Object[] mapExpandedElements(AbstractHistoryCategory[] categories, Object[] expandedElements) {
		// Store the names of the currently expanded categories in a set.
		HashSet<String> names = new HashSet<>();
		for (Object expandedElement : expandedElements) {
			names.add(((DateHistoryCategory) expandedElement).getName());
		}

		//Go through the new categories and keep track of the previously expanded ones
		ArrayList<AbstractHistoryCategory> expandable = new ArrayList<>();
		for (AbstractHistoryCategory category : categories) {
			// Check to see if this category is currently expanded.
			if (names.contains(category.getName())) {
				expandable.add(category);
			}
		}
		return expandable.toArray(new Object[expandable.size()]);
	}

	private AbstractHistoryCategory[] sortRevisions(IFileRevision[] revisions, IProgressMonitor monitor) {

		try {
			monitor.beginTask(null, 100);
			//Create the 4 categories
			DateHistoryCategory[] tempCategories = new DateHistoryCategory[4];
			//Get a calendar instance initialized to the current time
			Calendar currentCal = Calendar.getInstance();
			tempCategories[0] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Today, currentCal, null);
			//Get yesterday
			Calendar yesterdayCal = Calendar.getInstance();
			yesterdayCal.roll(Calendar.DAY_OF_YEAR, -1);
			tempCategories[1] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Yesterday, yesterdayCal, null);
			//Get this month
			Calendar monthCal = Calendar.getInstance();
			monthCal.set(Calendar.DAY_OF_MONTH, 1);
			tempCategories[2] = new DateHistoryCategory(TeamUIMessages.HistoryPage_ThisMonth, monthCal, yesterdayCal);
			//Everything before after week is previous
			tempCategories[3] = new DateHistoryCategory(TeamUIMessages.HistoryPage_Previous, null, monthCal);

			ArrayList<AbstractHistoryCategory> finalCategories = new ArrayList<>();
			for (DateHistoryCategory tempCategory : tempCategories) {
				tempCategory.collectFileRevisions(revisions, false);
				if (tempCategory.hasRevisions()) {
					finalCategories.add(tempCategory);
				}
			}

			if (finalCategories.isEmpty()){
				//no revisions found for the current mode, so add a message category
				finalCategories.add(getErrorMessage());
			}

			return finalCategories.toArray(new AbstractHistoryCategory[finalCategories.size()]);
		} finally {
			monitor.done();
		}
	}

	private MessageHistoryCategory getErrorMessage(){
		MessageHistoryCategory messageCategory = new MessageHistoryCategory(getNoChangesMessage());
		return messageCategory;
	}

	protected String getNoChangesMessage() {
		return TeamUIMessages.LocalHistoryPage_NoRevisionsFound;
	}
}
