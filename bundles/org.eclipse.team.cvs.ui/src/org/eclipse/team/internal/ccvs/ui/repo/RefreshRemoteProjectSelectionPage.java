/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.util.Arrays;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSWizardPage;
import org.eclipse.ui.IWorkingSet;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.IWorkbenchAdapter;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * Page that allows the user to select the remote projects whose tags should be
 * refreshed.
 */
public class RefreshRemoteProjectSelectionPage extends CVSWizardPage {
	
	private ICVSRemoteResource[] rootFolders;
	private ListSelectionArea listArea;
	private WorkingSetSelectionArea workingSetArea;
	private IWorkingSet workingSet;
	private IDialogSettings settings;

	/**
	 * Custom input provider which returns the list of root folders
	 */
	private class InputElement implements IWorkbenchAdapter, IAdaptable {
		@Override
		public Object[] getChildren(Object o) {
			return rootFolders;
		}
		@Override
		public ImageDescriptor getImageDescriptor(Object object) {
			return null;
		}
		@Override
		public String getLabel(Object o) {
			return null;
		}
		@Override
		public Object getParent(Object o) {
			return null;
		}
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			if (adapter == IWorkbenchAdapter.class) return adapter.cast(this);
			return null;
		}
	}
	
	/**
	 * Constructor for RemoteProjectSelectionPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 * @param description
	 */
	public RefreshRemoteProjectSelectionPage(
			String pageName,
			String title,
			ImageDescriptor titleImage,
			String description, 
			IDialogSettings settings,
			ICVSRepositoryLocation root,
			ICVSRemoteResource[] rootFolders) {
		super(pageName, title, titleImage, description);
		this.settings = settings;
		this.rootFolders = rootFolders;
	}

	@Override
	public void createControl(Composite parent) {
		
		final Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(1, false));
		
		setControl(composite);
		
		// set F1 help
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.REFRESH_REMOTE_PROJECT_SELECTION_PAGE);
		
		listArea = new ListSelectionArea( 
			new InputElement(), 
			new RemoteContentProvider(), 
			new WorkbenchLabelProvider(), 
			CVSUIMessages.RefreshRemoteProjectSelectionPage_selectRemoteProjects); 
		listArea.createArea(composite);

		listArea.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				updateEnablement();
			}
		});
		listArea.getViewer().setComparator(new RepositoryComparator());
		
		workingSetArea = new WorkingSetSelectionArea(getShell(), CVSUIMessages.RefreshRemoteProjectSelectionPage_noWorkingSet, CVSUIMessages.RefreshRemoteProjectSelectionPage_workingSet, settings); // 
		setWorkingSet(workingSet);
		workingSetArea.addPropertyChangeListener(new IPropertyChangeListener() {
			@Override
			public void propertyChange(PropertyChangeEvent event) {
				workingSet = (IWorkingSet)event.getNewValue();
				handleWorkingSetChange();
			}
		});
		workingSetArea.createArea(composite);
		Dialog.applyDialogFont(parent);
	}

	/**
	 * Sets the working set that should be selected in the most recently
	 * used working set list.
	 *
	 * @param workingSet the working set that should be selected.
	 * 	has to exist in the list returned by
	 * 	org.eclipse.ui.IWorkingSetManager#getRecentWorkingSets().
	 * 	Must not be null.
	 */
	public void setWorkingSet(IWorkingSet workingSet) {
		this.workingSet = workingSet;

		if (workingSetArea != null) {
			workingSetArea.setWorkingSet(workingSet);
		}
	}
	
	private void handleWorkingSetChange() {
		if (workingSet != null) {
			// check any projects in the working set
			listArea.getViewer().setAllChecked(false);
			RepositoryManager manager = CVSUIPlugin.getPlugin().getRepositoryManager();
			ICVSRemoteResource[] resources = manager.filterResources(workingSet, rootFolders);
			for (ICVSRemoteResource resource : resources) {
				listArea.getViewer().setChecked(resource, true);
			}
		}
	}
	
	private void updateEnablement() {
		boolean atLeastOne = listArea.getViewer().getCheckedElements().length > 0;
		setPageComplete(atLeastOne);
	}
	
	/**
	 * Method getSelectedRemoteProject.
	 * @return ICVSRemoteResource[]
	 */
	public ICVSRemoteResource[] getSelectedRemoteProject() {
		Object[] checked = listArea.getViewer().getCheckedElements();
		return Arrays.asList(checked).toArray(new ICVSRemoteResource[checked.length]);
	}
}
