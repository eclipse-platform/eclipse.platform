/*******************************************************************************
 * Copyright (c) 2000, 2008 IBM Corporation and others.
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


import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.wizard.IWizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.CVSUIMessages;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.model.RemoteContentProvider;
import org.eclipse.team.internal.ccvs.ui.repo.RepositoryComparator;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.model.WorkbenchLabelProvider;

public class ModuleSelectionPage extends CVSWizardPage {
	Button useProjectNameButton;
	Button useSpecifiedNameButton;
	private Button selectModuleButton;
	private Button useModuleAndProjectNameButton;
	Text text;
	TreeViewer moduleList;
	
	String moduleName;
	
	// The project being associated with the remote module (or null)
	private IProject project;
	private ICVSRepositoryLocation location;
	private boolean badLocation = false;
	private String helpContextId;
	private boolean supportsMultiSelection;
	
	private boolean isFetchingModules = false;
	private Object fetchingModulesLock = new Object();
	
	private String SEPARATOR = "/"; //$NON-NLS-1$

	public ModuleSelectionPage(String pageName, String title, ImageDescriptor titleImage) {
		super(pageName, title, titleImage);
	}
	
	public void setHelpContxtId(String helpContextId) {
		this.helpContextId = helpContextId;
	}
	
	@Override
	public void createControl(Composite parent) {
		Composite composite = createComposite(parent, 2, false);

		if (helpContextId != null)
			PlatformUI.getWorkbench().getHelpSystem().setHelp(composite, helpContextId);
		
		Listener listener = event -> updateEnablements(false);
		
		if (project != null) {
			useProjectNameButton = createRadioButton(composite, CVSUIMessages.ModuleSelectionPage_moduleIsProject, 2); 
			useProjectNameButton.addListener(SWT.Selection, listener);
		}
		useSpecifiedNameButton = createRadioButton(composite, CVSUIMessages.ModuleSelectionPage_specifyModule, 1); 
		useSpecifiedNameButton.addListener(SWT.Selection, listener);

		text = createTextField(composite);
		text.addListener(SWT.Modify, listener);
		
		selectModuleButton = createRadioButton(composite, CVSUIMessages.ModuleSelectionPage_2, 2); 
		selectModuleButton.addListener(SWT.Selection, listener);
		
		if (project != null) {
			useModuleAndProjectNameButton = new Button(composite, SWT.CHECK);
			useModuleAndProjectNameButton.setText(CVSUIMessages.ModuleSelectionPage_3);
			GridData data = new GridData();
			data.horizontalSpan = 2;
			data.horizontalIndent = 10;
			useModuleAndProjectNameButton.setLayoutData(data);
			useModuleAndProjectNameButton.addListener(SWT.Selection, event -> updateText());
		}
		
		moduleList = createModuleTree(composite, 2);
		
		// Set the initial enablement
		if (useProjectNameButton != null) {
			useProjectNameButton.setSelection(true);
			useSpecifiedNameButton.setSelection(false);
		} else {
			useSpecifiedNameButton.setSelection(true);
		}
		selectModuleButton.setSelection(false);
		if (useModuleAndProjectNameButton != null)
			useModuleAndProjectNameButton.setSelection(false);
		updateEnablements(false);
		setControl(composite);
		Dialog.applyDialogFont(parent);
	}
	
	private void updateText() {
		updateEnablements(false);
		ICVSRemoteFolder[] modules = internalGetSelectedModules();
		if (modules.length == 1) {
			// There is at 1 module selected
			ICVSRemoteFolder selectedModule = modules[0];
			String repositoryRelativePath = selectedModule.getRepositoryRelativePath();
			if (!repositoryRelativePath.equals(FolderSyncInfo.VIRTUAL_DIRECTORY)) {
				text.setText(repositoryRelativePath);
			}
		} else {
			text.setText(useModuleAndProjectName() ? project.getName() : ""); //$NON-NLS-1$
		}
	}
	
	private boolean useModuleAndProjectName() {
		return useModuleAndProjectNameButton != null
				&& useModuleAndProjectNameButton.getSelection();
	}
	
	@Override
	public void setVisible(boolean visible) {
		super.setVisible(visible);
		if (visible) {
			IWizard w = getWizard();
			if (w instanceof CheckoutWizard) {
				((CheckoutWizard)w).resetSubwizard();
			}
			if (useProjectNameButton != null && useProjectNameButton.getSelection()) {
				useProjectNameButton.setFocus();
			} else if (useSpecifiedNameButton.getSelection()) {
				text.setFocus();
			} else {
				moduleList.getControl().setFocus();
			}
		}
	}
	
	protected void updateEnablements(boolean updateModulesList) {
		if (useProjectNameButton != null && useProjectNameButton.getSelection()) {
			text.setEnabled(false);
			moduleList.getControl().setEnabled(false);
			if (useModuleAndProjectNameButton != null)
				useModuleAndProjectNameButton.setEnabled(false);
			moduleName = null;
			setPageComplete(true);
		} else if (useSpecifiedNameButton.getSelection()) {
			text.setEnabled(true);
			moduleList.getControl().setEnabled(false);
			if (useModuleAndProjectNameButton != null)
				useModuleAndProjectNameButton.setEnabled(false);
			moduleName = text.getText();
			if (moduleName.length() == 0) {
				moduleName = null;
				setPageComplete(false);
			} else {
				setPageComplete(true);
			}
		} else if (!badLocation){
			text.setEnabled(false);
			if (useModuleAndProjectNameButton != null)
				useModuleAndProjectNameButton.setEnabled(true);
			moduleList.getControl().setEnabled(true);
			moduleName = null;
			if (moduleList.getInput() == null || updateModulesList) {
				boolean fetchModules = false;
				// The input is set after the page is shown to avoid
				// fetching if the user wants to specify the name manually
				try {
					// This can be called from different events in the event loop.
					// Ensure that we only fetch the input once
					synchronized (fetchingModulesLock) {
						if (!isFetchingModules) {
							// This the first thread in so fetch the modules
							fetchModules = true;
							isFetchingModules = true;
						}
					}
					if (fetchModules) {
						// Validate the location first since the module fecthing is
						// done in a deferred fashion
						getContainer().run(true, true, monitor -> {
							try {
								validateLocation(monitor);
							} catch (CVSException e) {
								throw new InvocationTargetException(e);
							}
						});
						setModuleListInput();
					}
				} catch (InvocationTargetException e) {
					if (!badLocation) {
						badLocation = true;
						CVSUIPlugin.openError(getShell(), null, null, e);
						// This will null the module list input
						setModuleListInput();
					}
				} catch (InterruptedException e) {
					// Canceled by the user
				} finally {
					synchronized (fetchingModulesLock) {
						if (fetchModules) {
							isFetchingModules = false;
						}
					}
				}
			}
			setPageComplete(internalGetSelectedModules().length > 0);
		}
	}

	/* package */ ICVSRemoteFolder[] internalGetSelectedModules() {
		if (moduleList != null && moduleList.getControl().isEnabled()) {
			ISelection selection = moduleList.getSelection();
			if (!selection.isEmpty() && selection instanceof IStructuredSelection) {
				IStructuredSelection ss = (IStructuredSelection)selection;
				ArrayList<ICVSRemoteFolder> result = new ArrayList<>();
				for (Iterator iter = ss.iterator(); iter.hasNext();) {
					Object element = iter.next();
					if (element instanceof ICVSRemoteFolder) {
						if (useModuleAndProjectName()) {
							String relativePath = ((ICVSRemoteFolder)element).getRepositoryRelativePath();
							ICVSRemoteFolder remoteFolder = internalCreateModuleHandle(relativePath + SEPARATOR + project.getName())[0];
							result.add(remoteFolder);
						} else {
							result.add((ICVSRemoteFolder) element);
						}
					}
					
				}
				return result.toArray(new ICVSRemoteFolder[result.size()]);
			}
		} else {
			if (moduleName != null) {
				return internalCreateModuleHandle(moduleName);
			} else if (project != null) {
				return internalCreateModuleHandle(project.getName());
			}
		} 
		return new ICVSRemoteFolder[0];
	}
	
	private ICVSRemoteFolder[] internalCreateModuleHandle(String name) {
		ICVSRepositoryLocation location = getLocation();
		if (location == null) return new ICVSRemoteFolder[0];
		String[] names = name.split(","); //$NON-NLS-1$
		int length = names.length;
		java.util.List<ICVSRemoteFolder> folders = new ArrayList<>();
		for (int i = 0; i < length; i++) {
			// call trim() in case the user has added spaces after the commas
			String trimmedName = names[i].trim();
			if (trimmedName.length() > 0)
				folders.add(location.getRemoteFolder(trimmedName, CVSTag.DEFAULT));
		}
		return folders.toArray(new ICVSRemoteFolder[folders.size()]);
	}
	
	/**
	 * Return the selected existing remote folder. If this
	 * method returns <code>null</code>, then <code>getModuleName()</code>
	 * can be used to get the name entered manually by the use.
	 * @return the selected existing remote module
	 */
	public ICVSRemoteFolder getSelectedModule() {
		ICVSRemoteFolder[] selectedModules = getSelectedModules();
		if (selectedModules.length > 0) {
			return selectedModules[0];
		} else {
			return null;
		}
	}
	
	public ICVSRemoteFolder[] getSelectedModules() {
		final ICVSRemoteFolder[][] folder = new ICVSRemoteFolder[][] { null };
		Display.getDefault().syncExec(() -> folder[0] = internalGetSelectedModules());
		return folder[0];
	}
	
	private TreeViewer createModuleTree(Composite composite, int horizontalSpan) {
		Tree tree = new Tree(composite, (supportsMultiSelection ? SWT.MULTI : SWT.SINGLE) | SWT.BORDER);
		GridData data = new GridData(GridData.FILL_BOTH);
				
		// see bug 158380
		data.heightHint = Math.max(composite.getParent().getSize().y, 100);
		
		data.horizontalSpan = horizontalSpan;
		tree.setLayoutData(data);	
		TreeViewer result = new TreeViewer(tree) {
			/*
			 * Fix to allow filtering to be used without triggering fetching 
			 * of the contents of all children (see bug 62268)
			 */
			@Override
			public boolean isExpandable(Object element) {
				ITreeContentProvider cp = (ITreeContentProvider) getContentProvider();
				if(cp == null)
					return false;
				
				return cp.hasChildren(element);
			}
		};
		result.setContentProvider(new RemoteContentProvider());
		result.setLabelProvider(new WorkbenchLabelProvider());
		result.addFilter(new ViewerFilter() {
			@Override
			public boolean select(Viewer viewer, Object parentElement, Object element) {
				return !(element instanceof ICVSRemoteFile);
			}
		});
		result.addSelectionChangedListener(event -> updateText());
		result.getTree().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDoubleClick(MouseEvent e) {
				if (getSelectedModule() != null) {
					gotoNextPage();
				}
			}
		});
		result.setComparator(new RepositoryComparator());
		return result;
	}
	
	private void setModuleListInput() {
		ICVSRepositoryLocation location = getLocation();
		if (location == null || badLocation) return;
		moduleList.setInput(location.getRemoteFolder(ICVSRemoteFolder.REPOSITORY_ROOT_FOLDER_NAME, CVSTag.DEFAULT));
	}

	private ICVSRepositoryLocation getLocation() {
		return location;
	}
	
	public void setLocation(ICVSRepositoryLocation location) {
		boolean refresh = location != null && !location.equals(this.location);
		this.location = location;
		badLocation = false;
		if (moduleList != null) {
			updateEnablements(refresh);
		}
	}

	public void setProject(IProject project) {
		this.project = project;
	}
	public void setSupportsMultiSelection(boolean supportsMultiSelection) {
		this.supportsMultiSelection = supportsMultiSelection;
	}

	/* package */ void gotoNextPage() {
		getContainer().showPage(getNextPage());
	}

	/* package */ void validateLocation(IProgressMonitor monitor) throws CVSException {
		location.validateConnection(monitor);
	}
}
