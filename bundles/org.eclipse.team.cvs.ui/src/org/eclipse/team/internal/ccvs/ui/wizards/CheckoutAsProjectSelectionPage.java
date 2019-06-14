/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
 *     Philippe Ombredanne - bug 84808
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.wizards;

import java.util.ArrayList;
import java.util.List;

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
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.ui.PlatformUI;

/**
 * This page allows the user to select the target parent container for
 * the folders being checked out.
 */
public class CheckoutAsProjectSelectionPage extends CVSWizardPage {
	
	public static final String NAME = "CheckoutAsProjectSelectionPage"; //$NON-NLS-1$
	
	private TreeViewer tree;
	private Text nameField;
	private Combo filterList;
	
	private IResource selection;
	private ICVSRemoteFolder[] remoteFolders;
	private String folderName;
	private int filter;

	/**
	 * Constructor for CheckoutIntoProjectSelectionPage.
	 * @param pageName
	 * @param title
	 * @param titleImage
	 */
	public CheckoutAsProjectSelectionPage(ImageDescriptor titleImage, ICVSRemoteFolder[] remoteFolders) {
		super(NAME, CVSUIMessages.CheckoutAsProjectSelectionPage_title, titleImage, CVSUIMessages.CheckoutAsProjectSelectionPage_description); // 
		this.remoteFolders = remoteFolders;
	}

	/**
	 * @return
	 */
	private boolean isSingleFolder() {
		return remoteFolders.length == 1;
	}
	
	/*
	 * For the single folder case, return the name of the folder
	 */
	private String getInputFolderName() {
		return getPreferredFolderName(remoteFolders[0]);
	}
	
	private String getRepository() throws CVSException {
		return remoteFolders[0].getFolderSyncInfo().getRoot();
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite= createComposite(parent, 2, false);
		setControl(composite);
		
		PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, IHelpContextIds.CHECKOUT_PROJECT_SELECTION_PAGE);
		
		if (isSingleFolder()) {
			createLabel(composite, CVSUIMessages.CheckoutAsProjectSelectionPage_name); 
			nameField = createTextField(composite);
			nameField.addListener(SWT.Modify, event -> {
				folderName = nameField.getText();
				updateWidgetEnablements();
			});
		}
		
		createWrappingLabel(composite, CVSUIMessages.CheckoutAsProjectSelectionPage_treeLabel, 0, 2); 
		
		tree = createResourceSelectionTree(composite, IResource.PROJECT | IResource.FOLDER, 2 /* horizontal span */);
		tree.addSelectionChangedListener(event -> handleResourceSelection(event));

		Composite filterComposite = createComposite(composite, 2, false);
		GridData data = new GridData();
		data.verticalAlignment = GridData.FILL;
		data.horizontalAlignment = GridData.FILL;
		data.horizontalSpan = 2;
		filterComposite.setLayoutData(data);
		createLabel(filterComposite, CVSUIMessages.CheckoutAsProjectSelectionPage_showLabel); 
		filterList = createCombo(filterComposite);
		filterList.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				handleFilterSelection();
			}
		});
		
		createWrappingLabel(composite, "", 0, 2); //$NON-NLS-1$
		
		initializeValues();
		updateWidgetEnablements();
		tree.getControl().setFocus();
		Dialog.applyDialogFont(parent);
	}

	/**
	 * Method initializeValues.
	 */
	private void initializeValues() {
		if (isSingleFolder()) {
			nameField.setText(getInputFolderName());
		}
		tree.setInput(ResourcesPlugin.getWorkspace().getRoot());
		filter = 0;
		updateTreeContents(filter);
		filterList.add(CVSUIMessages.CheckoutAsProjectSelectionPage_showAll); 
		filterList.add(CVSUIMessages.CheckoutAsProjectSelectionPage_showUnshared); 
		filterList.add(CVSUIMessages.CheckoutAsProjectSelectionPage_showSameRepo); 
		filterList.select(filter);
	}

	private void handleResourceSelection(SelectionChangedEvent event) {
		ISelection sel = event.getSelection();
		if (sel.isEmpty()) {
			this.selection = null;
		} else if (sel instanceof IStructuredSelection) {
			this.selection = (IResource)((IStructuredSelection)sel).getFirstElement();
		}
		updateWidgetEnablements();
	}
	
	/**
	 * Method updateWidgetEnablement.
	 */
	private void updateWidgetEnablements() {
		if (isSingleFolder() && !Path.EMPTY.isValidSegment(folderName)) {
			setPageComplete(false);
			setErrorMessage(NLS.bind(CVSUIMessages.CheckoutAsProjectSelectionPage_invalidFolderName, new String[] { folderName })); 
			return;
		}
		boolean complete = selection != null && selection.getType() != IResource.FILE;
		setErrorMessage(null);
		setPageComplete(complete);
	}
	
	/**
	 * Returns the selection.
	 * @return IResource
	 */
	public IResource getSelection() {
		return selection;
	}

	private void updateTreeContents(int selected) {
		try {
			if (selected == 0) {
				tree.setInput(new AdaptableResourceList(getProjects(getRepository(), true)));
			} else if (selected == 1) {
				tree.setInput(new AdaptableResourceList(getProjects(null, true)));
			} else if (selected == 2) {
				tree.setInput(new AdaptableResourceList(getProjects(getRepository(), false)));
			}
		} catch (CVSException e) {
			CVSUIPlugin.log(e);
		}
	}
			
	/**
	 * Method getValidTargetProjects returns the set of projects that match the provided criteria.
	 * @return IResource
	 */
	private IProject[] getProjects(String root, boolean unshared) throws CVSException {
		List<IProject> validTargets = new ArrayList<>();
		try {
			IResource[] projects = ResourcesPlugin.getWorkspace().getRoot().members();
			for (IResource resource : projects) {
				if (resource instanceof IProject) {
					IProject project = (IProject) resource;
					if (project.isAccessible()) {
						RepositoryProvider provider = RepositoryProvider.getProvider(project);
						if (provider == null && unshared) {
							validTargets.add(project);
						} else if (provider != null && provider.getID().equals(CVSProviderPlugin.getTypeId())) {
							ICVSFolder cvsFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
							FolderSyncInfo info = cvsFolder.getFolderSyncInfo();
							if (root != null && info != null && root.equals(info.getRoot())) {
								validTargets.add(project);
							}
						}
					}
				}
			}
		} catch (CoreException e) {
			throw CVSException.wrapException(e);
		}
		return validTargets.toArray(new IProject[validTargets.size()]);
	}
	
	public IContainer getLocalFolder() {
		if (Path.EMPTY.isValidSegment(folderName)) {
			return ((IContainer)getSelection()).getFolder(new Path(null, folderName));
		} else {
			return null;
		}
	}
	
	public IContainer getParentFolder() {
		return ((IContainer)getSelection());
	}
	
	private void handleFilterSelection() {
		filter = filterList.getSelectionIndex();
		updateTreeContents(filter);
	}

}
