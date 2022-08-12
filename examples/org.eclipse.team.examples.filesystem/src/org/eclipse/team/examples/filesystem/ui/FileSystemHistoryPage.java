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
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.filesystem.ui;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistory;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.examples.filesystem.FileSystemProvider;
import org.eclipse.team.examples.filesystem.history.FileSystemHistory;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.team.ui.history.IHistoryPageSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;

public class FileSystemHistoryPage extends HistoryPage {

	/* private */IFile file;
	/* private */FileSystemHistory fileSystemHistory;
	/* private */IFileRevision[] entries;
	/* private */IFileRevision currentSelection;

	private FileSystemTableProvider fileSystemTableProvider;
	/* private */TableViewer tableViewer;
	private Composite localComposite;

	/* private */OpenFileSystemRevisionAction openAction;

	boolean shutdown = false;

	private RefreshFileHistory refreshFileHistoryJob;

	private class RefreshFileHistory extends Job {
		/* private */FileSystemHistory fileHistory;

		public RefreshFileHistory() {
			super("Fetching FileSystem revisions...");  //$NON-NLS-1$
		}

		public void setFileHistory(FileSystemHistory fileHistory) {
			this.fileHistory = fileHistory;
		}

		@Override
		public IStatus run(IProgressMonitor monitor) {

			IStatus status = Status.OK_STATUS;

			if (fileHistory != null && !shutdown) {
				fileHistory.refresh(monitor);
				//Internal code used for convenience - you can use
				//your own here
				Utils.asyncExec((Runnable) () -> tableViewer.setInput(fileHistory), tableViewer);
			}

			return status;
		}
	}

	@Override
	public boolean inputSet() {
		IFile tempFile = getFile();
		this.file = tempFile;
		if (tempFile == null)
			return false;

		//blank current input only after we're sure that we have a file
		//to fetch history for
		this.tableViewer.setInput(null);

		fileSystemHistory = new FileSystemHistory(file);

		refreshHistory();
		return true;
	}

	private IWorkbenchPartSite getWorkbenchSite(IHistoryPageSite parentSite) {
		IWorkbenchPart part = parentSite.getPart();
		if (part != null)
			return part.getSite();
		return null;
	}

	private IFile getFile() {
		Object obj = getInput();
		if (obj instanceof IFile)
			return (IFile) obj;

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

		tableViewer = createTable(localComposite);

		contributeActions();

	}

	private void contributeActions() {
		openAction = new OpenFileSystemRevisionAction("Open");  //$NON-NLS-1$
		tableViewer.getTable().addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				openAction.selectionChanged(tableViewer.getStructuredSelection());
			}
		});
		openAction.setPage(this);
		//Contribute actions to popup menu
		MenuManager menuMgr = new MenuManager();
		Menu menu = menuMgr.createContextMenu(tableViewer.getTable());
		menuMgr.addMenuListener(menuMgr1 -> {
			menuMgr1.add(new Separator(IWorkbenchActionConstants.GROUP_FILE));
			menuMgr1.add(openAction);
		});
		menuMgr.setRemoveAllWhenShown(true);
		tableViewer.getTable().setMenu(menu);
	}

	private TableViewer createTable(Composite parent) {
		fileSystemTableProvider = new FileSystemTableProvider();
		TableViewer viewer = fileSystemTableProvider.createTable(parent);
		viewer.setContentProvider(new IStructuredContentProvider() {

			@Override
			public Object[] getElements(Object inputElement) {
				// The entries of already been fetch so return them
				if (entries != null)
					return entries;

				final IFileHistory fileHistory = (IFileHistory) inputElement;
				entries = fileHistory.getFileRevisions();

				return entries;
			}

			@Override
			public void dispose() {
				// TODO Auto-generated method stub

			}

			@Override
			public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
				entries = null;
			}

		});
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
		if (file != null)
			return file.getFullPath().toString();

		return null;
	}

	@Override
	public String getName() {
		if (file != null)
			return file.getName();

		return ""; //$NON-NLS-1$
	}

	@Override
	public boolean isValidInput(Object object) {

		if (object instanceof IResource && ((IResource) object).getType() == IResource.FILE) {
			RepositoryProvider provider = RepositoryProvider.getProvider(((IFile) object).getProject());
			if (provider != null && provider instanceof FileSystemProvider)
				return true;
		}

		return false;
	}

	@Override
	public void refresh() {
		refreshHistory();
	}

	private void refreshHistory() {
		if (refreshFileHistoryJob == null)
			refreshFileHistoryJob = new RefreshFileHistory();

		if (refreshFileHistoryJob.getState() != Job.NONE) {
			refreshFileHistoryJob.cancel();
		}
		refreshFileHistoryJob.setFileHistory(fileSystemHistory);
		IHistoryPageSite parentSite = getHistoryPageSite();
		//Internal code used for convenience - you can use your own here
		Utils.schedule(refreshFileHistoryJob, getWorkbenchSite(parentSite));
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		return null;
	}

}
