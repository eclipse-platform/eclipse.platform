/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.io.BufferedInputStream;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.compare.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Widget;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Select the files to restore
 */
public class RestoreFromRepositoryFileSelectionPage extends CVSWizardPage {
	private TreeViewer fileTree;
	private CompareViewerPane fileSelectionPane;
	private CompareViewerPane revisionSelectionPane;
	private CheckboxTableViewer revisionsTable;
	private CompareViewerSwitchingPane fileContentPane;
	
	private HistoryTableProvider historyTableProvider;
	private AdaptableHierarchicalResourceList treeInput = new AdaptableHierarchicalResourceList(ResourcesPlugin.getWorkspace().getRoot(), new IResource[0]);
	
	private IContainer folder;
	private IFile selectedFile;
	private ILogEntry selectedRevision;
	private Map<IFile, ILogEntry[]> entriesCache = new HashMap<>();
	private Map<IFile, Object> filesToRestore = new HashMap<>();

	private static final int WIZARD_WIDTH = 550;
	
	class HistoryInput implements ITypedElement, IEncodedStreamContentAccessor, IModificationDate {
		IFile file;
		ILogEntry logEntry;
		
		HistoryInput(IFile file, ILogEntry logEntry) {
			this.file= file;
			this.logEntry = logEntry;
		}
		@Override
		public InputStream getContents() throws CoreException {
			IStorage s = getStorageFromLogEntry(logEntry);
			if (s == null) return null;
			return new BufferedInputStream(s.getContents());
		}
		@Override
		public String getName() {
			return file.getName();
		}
		@Override
		public String getType() {
			return file.getFileExtension();
		}
		@Override
		public Image getImage() {
			return CompareUI.getImage(file);
		}
		@Override
		public long getModificationDate() {
			return logEntry.getDate().getTime();
		}
		@Override
		public String getCharset() throws CoreException {
			IStorage s = getStorageFromLogEntry(logEntry);
			if (s instanceof IEncodedStorage) {
				return ((IEncodedStorage)s).getCharset();
			}
			return null;
		}
	}
	
	/**
	 * Constructor for RestoreFromRepositoryFileSelectionPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public RestoreFromRepositoryFileSelectionPage(
		String pageName,
		String title,
		ImageDescriptor titleImage,
		String description) {
		super(pageName, title, titleImage, description);
	}

	@Override
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 1, false);
		setControl(composite);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.RESTORE_FROM_REPOSITORY_FILE_SELECTION_PAGE);
		
		// Top and bottom panes: top is the two selection panes, bottom is the file content viewer
		Splitter vsplitter= new Splitter(composite,  SWT.VERTICAL);
		GridData data = new GridData(GridData.HORIZONTAL_ALIGN_FILL | GridData.GRAB_HORIZONTAL
					| GridData.VERTICAL_ALIGN_FILL | GridData.GRAB_VERTICAL);
		// Set the width to be extra wide to accomodate the two selection lists
		data.widthHint = WIZARD_WIDTH;
		vsplitter.setLayoutData(data);
		
		// Top left and top right panes: the left for the files, the right for the log entries
		Splitter hsplitter= new Splitter(vsplitter,  SWT.HORIZONTAL);

		// Top left: file selection pane
		fileSelectionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		fileSelectionPane.setLayoutData(data);
		fileTree = createFileSelectionTree(fileSelectionPane);
		
		// Top right: Revision selection pane
		revisionSelectionPane = new CompareViewerPane(hsplitter, SWT.BORDER | SWT.FLAT);
		data = new GridData(GridData.FILL_HORIZONTAL | GridData.FILL_VERTICAL);
		revisionSelectionPane.setLayoutData(data);
		historyTableProvider = new HistoryTableProvider();
		revisionsTable = createRevisionSelectionTable(revisionSelectionPane, historyTableProvider);
		revisionSelectionPane.setText(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_emptyRevisionPane);
		
		// Bottom: File content viewer
		fileContentPane = new CompareViewerSwitchingPane(vsplitter, SWT.BORDER | SWT.FLAT) {
			@Override
			protected Viewer getViewer(Viewer oldViewer, Object input) {
				return CompareUI.findContentViewer(oldViewer, input, this, null);
			}
		};

		hsplitter.setWeights(new int[] { 40, 60 });

		initializeValues();
		updateWidgetEnablements();
		Dialog.applyDialogFont(parent);
	}

	protected CheckboxTableViewer createRevisionSelectionTable(CompareViewerPane composite, HistoryTableProvider tableProvider) {
		CheckboxTableViewer table = tableProvider.createCheckBoxTable(composite);
		table.setContentProvider(new IStructuredContentProvider() {
			@Override
			public Object[] getElements(Object inputElement) {
				ILogEntry[] entries = getSelectedEntries();
				if (entries != null) return entries;
				return new Object[0];
			}
			@Override
			public void dispose() {
			}
			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
			}
		});
		table.setInput(this);
		table.getTable().addSelectionListener(
			new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					// Handle check selection in the check state listener
					if (e.detail == SWT.CHECK) return;
					handleRevisionSelection(e.item);
				}
			}
		);
		table.addCheckStateListener(event -> handleRevisionChecked(event));
		composite.setContent(table.getControl());
		return table;
	}
	
	protected TreeViewer createFileSelectionTree(CompareViewerPane composite) {
		TreeViewer tree = new TreeViewer(composite, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		tree.setUseHashlookup(true);
		tree.setContentProvider(treeInput.getTreeContentProvider());
		tree.setLabelProvider(
			new DecoratingLabelProvider(
				new WorkbenchLabelProvider() {
					@Override
					protected String decorateText(String input, Object element) {
						String text;
						if (element instanceof IFolder && element.equals(folder)) {
							text = super.decorateText(folder.getProjectRelativePath().toString(), element);
						} else {
							ILogEntry entry = (ILogEntry)filesToRestore.get(element);
							text = super.decorateText(input, element);
							if (entry != null) {
								text = NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_fileToRestore, new String[] { text, entry.getRevision() });
							}
						}
						return text;
					}
				},
				PlatformUI.getWorkbench().getDecoratorManager().getLabelDecorator()));
		tree.setComparator(new ResourceComparator(ResourceComparator.NAME));
		tree.setInput(treeInput);
		
		GridData data = new GridData(GridData.FILL_BOTH | GridData.GRAB_VERTICAL);
		tree.getTree().setLayoutData(data);
		tree.addPostSelectionChangedListener(event -> handleFileSelection(event));
		composite.setContent(tree.getControl());
		return tree;
	}
	
	/**
	 * Method updateWidgetEnablements.
	 */
	private void updateWidgetEnablements() {
		
		if (filesToRestore.isEmpty()) {
			setPageComplete(false);
			setErrorMessage(null);
			return;
		}
		
		for (Iterator iter = filesToRestore.keySet().iterator(); iter.hasNext();) {
			IFile file = (IFile) iter.next();
			if (file.exists()) {
				setPageComplete(false);
				setErrorMessage(NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_fileExists, new String[] { file.getName() }));
				return;
			}
			
			ILogEntry entry = (ILogEntry) filesToRestore.get(file);
			if (entry.isDeletion())  {
				setPageComplete(false);
				setErrorMessage(NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_revisionIsDeletion, new String[] { entry.getRevision(), file.getName() }));
				return;
			}
		}
		setPageComplete(true);
		setErrorMessage(null);
	}
	
	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		refresh();
	}

	/**
	 * Sets the folder.
	 * @param folder The folder to set
	 */
	public void setInput(IContainer folder, ICVSFile[] files) {
		if (folder.equals(this.folder)) return;
		this.folder = folder;
		setTreeInput(folder, files);
		initializeValues();
		updateWidgetEnablements();
	}
	
	/*
	 * Set the resource tree input to the files that were deleted
	 */
	private void setTreeInput(IContainer folder, ICVSFile[] cvsFiles) {
		reset();
		IResource[] files = new IResource[cvsFiles.length];
		for (int i = 0; i < cvsFiles.length; i++) {
			files[i] = cvsFiles[i].getIResource();
		}
		treeInput.setResources(files);
		// kludge to avoid auto-selection of first element
		// set the root to the folder's parent so the folder appears in the tree
		treeInput.setRoot(folder.getParent());
		refresh();
	}

	private void reset() {
		this.selectedFile = null;
		this.selectedRevision = null;
		treeInput.setResources(null);
		filesToRestore = new HashMap();
		if (fileContentPane != null && !fileContentPane.isDisposed()) {
			fileContentPane.setInput(null);
		}
		updateWidgetEnablements();
	}
	
	/**
	 * Method refresh.
	 */
	private void refresh() {
		if (folder == null) return;
		
		if (fileSelectionPane != null && !fileSelectionPane.isDisposed()) {
			fileSelectionPane.setText(NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_fileSelectionPaneTitle, new String[] { folder.getProject().getName() }));
			fileSelectionPane.setImage(CompareUI.getImage(folder.getProject()));
		}
		
		if (revisionSelectionPane != null && !revisionSelectionPane.isDisposed()) {
			if (selectedFile == null) {
				revisionSelectionPane.setText(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_emptyRevisionPane);
				revisionSelectionPane.setImage(null);
			}
		}
		
		// Empty the file content viewer
		if (fileContentPane != null && !fileContentPane.isDisposed()) {
			fileContentPane.setInput(null);
		}
		
		// refresh the tree
		if (fileTree != null) {
			// If the parent folder is in the tree, make sure it is expanded
			fileTree.setExpandedState(folder, true);
			fileTree.refresh();
		}
		if (revisionsTable != null)
			revisionsTable.refresh();
	}
	
	/*
	 * Set the log entry table input to the fetched entries  in response to a file selection
	 */
	private void setLogEntryTableInput(ILogEntry[] entries) {
		this.selectedRevision = null;
		// Refresh the table so it picks up the selected entries through its content provider
		revisionsTable.refresh();
		// Check the previously checked entry if one exists
		ILogEntry selectedEntry = (ILogEntry)filesToRestore.get(selectedFile);
		if (selectedEntry != null) {
			revisionsTable.setChecked(selectedEntry, true);
		}
		// Disable entries that represent deletions since they can't be loaded
		for (ILogEntry entry : entries) {
			if (entry.isDeletion()) {
				revisionsTable.setGrayed(entry, true);
			}
		}
		// Set the titlebar text for the revisions table
		revisionSelectionPane.setText(NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_revisionSelectionPaneTitle, new String[] { selectedFile.getName() }));
		revisionSelectionPane.setImage(CompareUI.getImage(selectedFile));
		// Clear the file content pane
		fileContentPane.setInput(null);
	}
	
	private void handleFileSelection(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		if (selection == null || selection.isEmpty()) {
			clearSelection();
		} else {
			if (selection instanceof IStructuredSelection) {
				IStructuredSelection structuredSelection = (IStructuredSelection) selection;
				IResource resource = (IResource)structuredSelection.getFirstElement();
				if (resource instanceof IFile) {
					handleFileSelection((IFile) resource);
				} else {
					clearSelection();
				}
			}
		}
	}
	
	/**
	 * Method handleFileSelection.
	 * @param file
	 */
	private void handleFileSelection(IFile file) {
		if (this.selectedFile == file) return;
		this.selectedFile = file;
		if (entriesCache.get(file) == null) {
			try {
				
				// First, we need to create a remote file handle so we can get the log entries
				ICVSFolder parent = CVSWorkspaceRoot.getCVSFolderFor(file.getParent());
				FolderSyncInfo info = parent.getFolderSyncInfo();
				ICVSRepositoryLocation location = KnownRepositories.getInstance().getRepository(info.getRoot());
				final ICVSRemoteFile remoteFile = location.getRemoteFile(new Path(null, info.getRepository()).append(file.getName()).toString(), CVSTag.DEFAULT);
				
				// Then we need to fetch the log entries
				getContainer().run(true, true, monitor -> {
					try {
						// fetch the entries
						ILogEntry[] entries = remoteFile.getLogEntries(monitor);
						// cache the entries with the selected file
						entriesCache.put(selectedFile, entries);
					} catch (TeamException e) {
						throw new InvocationTargetException(e);
					}
				});
			} catch (CVSException e) {
				setErrorMessage(
					CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
						.getMessage());
				return;
			} catch (InvocationTargetException e) {
				setErrorMessage(
					CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
						.getMessage());
				return;
			} catch (InterruptedException e) {
				return;
			}
		}
		
		// Set the log table to display the entries for the selected file
		setLogEntryTableInput(getSelectedEntries());
	}

	private ILogEntry[] getSelectedEntries() {
		return entriesCache.get(selectedFile);
	}
	
	private IStorage getStorageFromLogEntry(final ILogEntry logEntry) {
		final IStorage[] s = new IStorage[] { null };
		try {
			getContainer().run(true, true, monitor -> {
				try {
					ICVSRemoteFile remoteFile = logEntry.getRemoteFile();
					s[0] = ((IResourceVariant) remoteFile).getStorage(monitor);
				} catch (TeamException e) {
					throw new InvocationTargetException(e);
				}
			});
		} catch (InvocationTargetException e) {
			setErrorMessage(
				CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
					.getMessage());
			return null;
		} catch (InterruptedException e) {
			return null;
		}
		return s[0];
	}

	private void handleRevisionChecked(CheckStateChangedEvent event) {
		if (event.getChecked()) {
			revisionsTable.setCheckedElements(new Object[] {event.getElement()});
			filesToRestore.put(selectedFile, event.getElement());
		}
		if (revisionsTable.getCheckedElements().length == 0) {
			filesToRestore.remove(selectedFile);
		}
		fileTree.refresh();
		updateWidgetEnablements();
	}
				
	/*
	 * A revision in the revision table has been selected.
	 * Populate the file contents pane with the selected log entry.
	 */
	private void handleRevisionSelection(Widget w) {
		if (fileContentPane != null && !fileContentPane.isDisposed()) {
			Object o= w.getData();
			if (o instanceof ILogEntry) {
				ILogEntry selected = (ILogEntry) o;
				if (this.selectedRevision == selected) return;
				this.selectedRevision = selected;
				if (selected.isDeletion()) {
					fileContentPane.setInput(null);
				} else {
					fileContentPane.setInput(new HistoryInput(selectedFile, selected));
					fileContentPane.setText(getEditionLabel(selectedFile, selected));
					fileContentPane.setImage(CompareUI.getImage(selectedFile));
				}
			} else {
				fileContentPane.setInput(null);
			}
		}
	}
	/**
	 * Method getEditionLabel.
	 * @param selectedFile
	 * @param selected
	 * @return String
	 */
	private String getEditionLabel(IFile selectedFile, ILogEntry selected) {
		return NLS.bind(CVSUIMessages.RestoreFromRepositoryFileSelectionPage_fileContentPaneTitle, (new Object[] { selectedFile.getName(), selected.getRevision(), selectedFile.getFullPath().makeRelative().removeLastSegments(1).toString() }));
	}
	
	public boolean restoreSelectedFiles() {
		try {
			getContainer().run(true, true, monitor -> {
				try {
					monitor.beginTask(null, 100 * filesToRestore.size());
					for (Iterator iter = filesToRestore.keySet().iterator(); iter.hasNext();) {
						IFile file = (IFile) iter.next();
						ILogEntry entry = (ILogEntry) filesToRestore.get(file);
						ensureParentExists(file);
						file.create(entry.getRemoteFile().getContents(Policy.subMonitorFor(monitor, 50)), false,
								Policy.subMonitorFor(monitor, 50));
					}
				} catch (TeamException e1) {
					throw new InvocationTargetException(e1);
				} catch (CoreException e2) {
					throw new InvocationTargetException(e2);
				} finally {
					monitor.done();
				}
			});
		} catch (InvocationTargetException e) {
			setErrorMessage(
				CVSUIPlugin.openError(getShell(), null, null, e, CVSUIPlugin.PERFORM_SYNC_EXEC)
					.getMessage());
			return false;
		} catch (InterruptedException e) {
			return false;
		}
		return true;
	}

	/**
	 * Method ensureParentExists.
	 * @param file
	 */
	private void ensureParentExists(IResource resource) throws CoreException {
		IContainer parent = resource.getParent();
		if (!parent.exists() && parent.getType() == IResource.FOLDER) {
			ensureParentExists(parent);
			((IFolder)parent).create(false, true, null);
		}
	}

	private void clearSelection() {
		this.selectedFile = null;
		this.selectedRevision = null;
		refresh();
	}
}
