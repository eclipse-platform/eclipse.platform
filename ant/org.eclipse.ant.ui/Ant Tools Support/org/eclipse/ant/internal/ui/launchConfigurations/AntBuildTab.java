/*******************************************************************************
 * Copyright (c) 2000, 2004 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ant.internal.ui.launchConfigurations;


import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ant.internal.ui.model.AntUIImages;
import org.eclipse.ant.internal.ui.model.AntUIPlugin;
import org.eclipse.ant.internal.ui.model.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.IAntUIHelpContextIds;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.help.WorkbenchHelp;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A launch configuration tab which allows the user to specify
 * which resources should be built before the Ant build (a build scope)
 * <p>
 * This class may be instantiated; this class is not intended
 * to be subclassed.
 * </p>
 * @since 3.0
 */
public class AntBuildTab extends AbstractLaunchConfigurationTab {

	/**
	 * String attribute identifying the build scope for this launch configuration.
	 * <code>null</code> indicates the default workspace build.
	 */
	public static final String ATTR_BUILD_SCOPE = AntUIPlugin.getUniqueIdentifier() + ".ATTR_BUILD_SCOPE"; //$NON-NLS-1$

	/**
	 * Attribute identifier specifying whether referenced projects should be 
	 * considered when computing the projects to build. Default value is
	 * <code>true</code>.
	 */
	public static final String ATTR_INCLUDE_REFERENCED_PROJECTS = AntUIPlugin.getUniqueIdentifier() + ".ATTR_INCLUDE_REFERENCED_PROJECTS"; //$NON-NLS-1$
	
	// Check Buttons
	private Button fBuildButton;
	
	// Group box
	private Group fGroup;
	
	// Radio Buttons
	private Button fProjectButton;
	private Button fSpecificProjectsButton;
	private Button fWorkspaceButton;
	
	// Push Button
	private Button fSelectButton;
	
	// whether to include referenced projects
	private Button fReferencedProjects;
	
	// projects to build (empty if none)
	private List fProjects = new ArrayList();
	
	class ProjectsContentProvider implements IStructuredContentProvider {

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement) {
			return ((IWorkspace)inputElement).getRoot().getProjects();
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose() {
		}

		/* (non-Javadoc)
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
		
	}
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#createControl(org.eclipse.swt.widgets.Composite)
	 */
	public void createControl(Composite parent) {
		Composite mainComposite = new Composite(parent, SWT.NONE);
		setControl(mainComposite);
		WorkbenchHelp.setHelp(getControl(), IAntUIHelpContextIds.ANT_BUILD_TAB);
		
		GridLayout layout = new GridLayout();
		GridData gd = new GridData(GridData.FILL_HORIZONTAL);
		mainComposite.setLayout(layout);
		mainComposite.setLayoutData(gd);
		mainComposite.setFont(parent.getFont());
		
		fBuildButton = createCheckButton(mainComposite, AntLaunchConfigurationMessages.getString("AntBuildTab.1")); //$NON-NLS-1$
		fBuildButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				updateEnabledState();
				updateLaunchConfigurationDialog();
			}
		});
		
		fGroup = new Group(mainComposite, SWT.NONE);
		fGroup.setFont(mainComposite.getFont());
		layout = new GridLayout();
		layout.numColumns = 2;
		layout.makeColumnsEqualWidth = false;
		fGroup.setLayout(layout);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fGroup.setLayoutData(gd);

		SelectionAdapter adapter = new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				if (((Button)e.getSource()).getSelection()) {
					updateEnabledState();
					updateLaunchConfigurationDialog();
				}
			}
		};
		
		fWorkspaceButton = createRadioButton(fGroup, AntLaunchConfigurationMessages.getString("AntBuildTab.2")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fWorkspaceButton.setLayoutData(gd);
		fWorkspaceButton.addSelectionListener(adapter);
		
		fProjectButton = createRadioButton(fGroup, AntLaunchConfigurationMessages.getString("AntBuildTab.3")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		fProjectButton.setLayoutData(gd);		
		fProjectButton.addSelectionListener(adapter);
				
		fSpecificProjectsButton = createRadioButton(fGroup, AntLaunchConfigurationMessages.getString("AntBuildTab.4")); //$NON-NLS-1$
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 1;
		fSpecificProjectsButton.setLayoutData(gd);
		fSpecificProjectsButton.addSelectionListener(adapter);		
		
		fSelectButton = createPushButton(fGroup, AntLaunchConfigurationMessages.getString("AntBuildTab.5"), null); //$NON-NLS-1$
		gd = (GridData)fSelectButton.getLayoutData();
		gd.horizontalAlignment = GridData.HORIZONTAL_ALIGN_END;
		fSelectButton.addSelectionListener(new SelectionAdapter() {
			public void widgetSelected(SelectionEvent e) {
				selectResources();
			}
		});
		
		createVerticalSpacer(mainComposite, 1);
		fReferencedProjects = createCheckButton(mainComposite, AntLaunchConfigurationMessages.getString("AntBuildTab.6")); //$NON-NLS-1$
	}

	/**
	 * Prompts the user to select the projects to build.
	 */
	private void selectResources() {
		ListSelectionDialog dialog = new ListSelectionDialog(getShell(), ResourcesPlugin.getWorkspace(), new ProjectsContentProvider(), new WorkbenchLabelProvider(), AntLaunchConfigurationMessages.getString("AntBuildTab.7")); //$NON-NLS-1$
		dialog.setInitialElementSelections(fProjects);
		if (dialog.open() == Window.CANCEL) {
			return;
		}
		Object[] res = dialog.getResult();
		fProjects = new ArrayList(res.length);
		for (int i = 0; i < res.length; i++) {
			fProjects.add(res[i]);
		}
		updateLaunchConfigurationDialog();
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#setDefaults(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#initializeFrom(org.eclipse.debug.core.ILaunchConfiguration)
	 */
	public void initializeFrom(ILaunchConfiguration configuration) {
		updateScope(configuration);
		updateReferencedProjects(configuration);
		updateEnabledState();		
	}
	
	private void updateReferencedProjects(ILaunchConfiguration configuration) {
		boolean ref = false;
		try {
			ref = configuration.getAttribute(ATTR_INCLUDE_REFERENCED_PROJECTS, true);
		} catch (CoreException e) {
			AntUIPlugin.log(AntUIPlugin.newErrorStatus("Exception reading launch configuration", e)); //$NON-NLS-1$
		}
		fReferencedProjects.setSelection(ref);
	}

	/**
	 * Updates the tab to display the build scope specified by the launch config
	 */
	private void updateScope(ILaunchConfiguration configuration) {
		String scope = null;
		try {
			scope= configuration.getAttribute(ATTR_BUILD_SCOPE, (String)null);
		} catch (CoreException ce) {
			AntUIPlugin.log(AntUIPlugin.newErrorStatus("Exception reading launch configuration", ce)); //$NON-NLS-1$
		}
		fBuildButton.setSelection(scope != null);
		fWorkspaceButton.setSelection(false);
		fProjectButton.setSelection(false);
		fSpecificProjectsButton.setSelection(false);
		fProjects.clear();
		if (scope == null) {
			// select the workspace by default
			fBuildButton.setSelection(true);
			fWorkspaceButton.setSelection(true);
		} else {
			if (scope.equals("${none}")) { //$NON-NLS-1$
				fBuildButton.setSelection(false);
			} else if (scope.equals("${project}")) { //$NON-NLS-1$
				fProjectButton.setSelection(true);
			} else if (scope.startsWith("${projects:")) { //$NON-NLS-1$
				fSpecificProjectsButton.setSelection(true);
				IProject[] projects = getBuildProjects(scope);
				fProjects = new ArrayList(projects.length);
				for (int i = 0; i < projects.length; i++) {
					fProjects.add(projects[i]);
				}
			}
		}
	}
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#performApply(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		String scope = generateScopeMemento();
		configuration.setAttribute(ATTR_BUILD_SCOPE, scope);
		if (fReferencedProjects.getSelection()) {
			// default is true
			configuration.setAttribute(ATTR_INCLUDE_REFERENCED_PROJECTS, (String)null);
		} else {
			configuration.setAttribute(ATTR_INCLUDE_REFERENCED_PROJECTS, false);
		}
	}

	/**
	 * Generates a memento for the build scope.
	 */
	private String generateScopeMemento() {
		if (fBuildButton.getSelection()) {
			if (fWorkspaceButton.getSelection()) {
				return null;
			}
			if (fProjectButton.getSelection()) {
				return "${project}"; //$NON-NLS-1$
			}
			if (fSpecificProjectsButton.getSelection()) {
				return getBuildScopeAttribute(fProjects);
			}
			return null;
			
		}
		return "${none}"; //$NON-NLS-1$
	}

	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getName()
	 */
	public String getName() {
		return AntLaunchConfigurationMessages.getString("AntBuildTab.8"); //$NON-NLS-1$
	}
	
	/**
	 * Updates the enablement state of the fields.
	 */
	private void updateEnabledState() {
		boolean enabled= fBuildButton.getSelection();
		fGroup.setEnabled(enabled);
		fWorkspaceButton.setEnabled(enabled);
		fProjectButton.setEnabled(enabled);
		fSpecificProjectsButton.setEnabled(enabled);
		fSelectButton.setEnabled(enabled && fSpecificProjectsButton.getSelection());
		if (!enabled) {
			super.setErrorMessage(null);
		}
		if (enabled) {
			if (!fWorkspaceButton.getSelection() && !fProjectButton.getSelection() &&
					!fSpecificProjectsButton.getSelection()) {
				fWorkspaceButton.setSelection(true);
			}
		}
		fReferencedProjects.setEnabled(fBuildButton.getSelection() && (fProjectButton.getSelection() || fSpecificProjectsButton.getSelection()));
	}
	
	/**
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#getImage()
	 */
	public Image getImage() {
		return AntUIImages.getImage(IAntUIConstants.IMG_ANT_BUILD_TAB);
	}

	public boolean isValid(ILaunchConfiguration launchConfig) {
		setErrorMessage(null);
		setMessage(null);
		if (fBuildButton.getSelection() && fSpecificProjectsButton.getSelection() && fProjects.isEmpty()) {
			setErrorMessage(AntLaunchConfigurationMessages.getString("AntBuildTab.9")); //$NON-NLS-1$
			return false;
		}
		return true;
	}
	
	/**
	 * Returns a collection of projects referenced by a build scope attribute.
	 * 
	 * @param scope build scope attribute (<code>ATTR_BUILD_SCOPE</code>)
	 * @return collection of porjects referred to by the scope attribute
	 */
	public static IProject[] getBuildProjects(String scope) {
		if (scope.startsWith("${projects:")) { //$NON-NLS-1$
			String pathString = scope.substring(11, scope.length() - 1);
			if (pathString.length() > 1) {
				String[] names = pathString.split(","); //$NON-NLS-1$
				IProject[] projects = new IProject[names.length];
				IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
				for (int i = 0; i < names.length; i++) {
					projects[i] = root.getProject(names[i]);
				}
				return projects;
			}
		} else if (scope.equals("${project}")) { //$NON-NLS-1$
			IResource resource = DebugUITools.getSelectedResource();
			if (resource != null) {
				return new IProject[]{resource.getProject()};
			}
		}
		return new IProject[0];
	}
	
	/**
	 * Returns the build scope attribute specified by the given launch configuration
	 * or <code>null</code> if none.
	 * 
	 * @param configuration launch configuration
	 * @return build scope attribute (<code>ATTR_BUILD_SCOPE</code>)
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static String getBuildScope(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_BUILD_SCOPE, (String) null);
	}
	
	/**
	 * Whether referenced projects should be considered when building. Only valid
	 * when a set of projects is to be built.
	 * 
	 * @param configuration
	 * @return whether referenced projects should be considerd when building
	 * @throws CoreException if unable to access the associated attribute
	 */
	public static boolean isIncludeReferencedProjects(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_INCLUDE_REFERENCED_PROJECTS, true);
	}
	
	/**
	 * Creates and returns a memento for the given project set, to be used as a
	 * build scope attribute.
	 * 
	 * @param projects list of projects
	 * @return an equivalent refresh attribute
	 */
	public static String getBuildScopeAttribute(List projects) {
		StringBuffer buf = new StringBuffer();
		buf.append("${projects:"); //$NON-NLS-1$
		Iterator iterator = projects.iterator();
		while (iterator.hasNext()) {
			IProject project = (IProject) iterator.next();
			buf.append(project.getName());
			if (iterator.hasNext()) {
				buf.append(","); //$NON-NLS-1$
			}
		}
		buf.append("}"); //$NON-NLS-1$
		return buf.toString();
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#activated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void activated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on activation
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.ui.ILaunchConfigurationTab#deactivated(org.eclipse.debug.core.ILaunchConfigurationWorkingCopy)
	 */
	public void deactivated(ILaunchConfigurationWorkingCopy workingCopy) {
		// do nothing on deactivation
	}
}