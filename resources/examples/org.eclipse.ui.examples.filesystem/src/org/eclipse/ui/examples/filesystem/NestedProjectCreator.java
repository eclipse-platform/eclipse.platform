/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
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
package org.eclipse.ui.examples.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.operation.IRunnableContext;
import org.eclipse.jface.viewers.*;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.WorkspaceModifyOperation;
import org.eclipse.ui.dialogs.ListSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;

/**
 * A utility class for locating uncreated projects that are physically
 * within the location of an existing project.  The locator prompts the
 * user to ask which sub-projects they want to create, and then creates them.
 * @since 3.2
 */
public class NestedProjectCreator {
	private IRunnableContext context = PlatformUI.getWorkbench().getProgressService();
	protected boolean excludeOverlap = true;
	protected final IWorkspace workspace = ResourcesPlugin.getWorkspace();

	/**
	 * This is the main entry point for the project locator. This method performs
	 * the search for nested projects, prompts the user to ask which ones they
	 * want to create, and creates those that the user asked to create.
	 * <p>
	 * This method must be called from the UI thread.
	 * </p>
	 * 
	 * @param projects The projects to locate nested projects in
	 * @param parentShell The shell for parenting dialogs
	 */
	public void createNestedProjects(IProject[] projects, Shell parentShell) {
		try {
			doCreateNestedProjects(projects, parentShell);
		} catch (InvocationTargetException e) {
			final IStatus status = getStatus(e.getTargetException());
			Policy.log(status);
			ErrorDialog.openError(parentShell, null, null, status);
		} catch (InterruptedException e) {
			//just abort from cancelation
		}
	}

	private void doCreateNestedProjects(final IProject[] projects, Shell shell) throws InvocationTargetException, InterruptedException {
		final Object[] result = new Object[1];
		context.run(true, true, monitor -> result[0] = findNestedProjects(projects));
		if (result[0] == null)
			return;
		IProjectDescription[] rawDescriptions = (IProjectDescription[]) result[0];
		if (rawDescriptions.length == 0)
			return;
		final IProjectDescription[] finalDescriptions = promptForCreation(shell, rawDescriptions);
		if (finalDescriptions.length == 0)
			return;
		String message = "Should the folder corresponding to the created projects be excluded from the existing parent project?";
		excludeOverlap = MessageDialog.openQuestion(shell, "Exclude overlapping resources?", message);
		//create the projects
		final IWorkspaceRoot root = workspace.getRoot();
		context.run(true, true, new WorkspaceModifyOperation() {
			@Override
			protected void execute(IProgressMonitor monitor) throws CoreException {
				try {
					monitor.beginTask("Creating Projects", finalDescriptions.length * 2);
					for (int i = 0; i < finalDescriptions.length; i++) {
						IProject project = root.getProject(finalDescriptions[i].getName());
						if (excludeOverlap)
							excludeOverlap(finalDescriptions[i]);
						project.create(finalDescriptions[i], SubMonitor.convert(monitor, 1));
						project.open(SubMonitor.convert(monitor, 1));
					}
				} finally {
					monitor.done();
				}
			}
		});

	}

	/**
	 * The given project is about to be created.  Exclude any corresponding
	 * resources in an overlapping parent project.
	 */
	protected void excludeOverlap(IProjectDescription description) throws CoreException {
		URI location = description.getLocationURI();
		if (location == null) {
			//default location for this project is below the root location
			URI rootLocation = workspace.getRoot().getLocationURI();
			location = EFS.getStore(rootLocation).getChild(description.getName()).toURI();
		}
		IContainer[] containers = workspace.getRoot().findContainersForLocationURI(location);
		URI nullURI;
		try {
			nullURI = new URI(EFS.SCHEME_NULL, null, "/", null, null); //$NON-NLS-1$
		} catch (URISyntaxException e) {
			Policy.log("Cannot exclude resource due to missing null file system", e); //$NON-NLS-1$
			//if this happens we cannot exclude the resource
			return;
		}
		for (int i = 0; i < containers.length; i++) {
			if (containers[i].getType() != IResource.FOLDER)
				continue;
			IFolder folder = (IFolder) containers[i];
			//don't replace links
			if (!folder.isLinked())
				folder.createLink(nullURI, IResource.REPLACE | IResource.ALLOW_MISSING_LOCAL, null);
		}
	}

	/**
	 * Returns descriptions of projects physically nested within the
	 * given projects that do not yet exist in the workspace.  This method
	 * does not create any projects.
	 * <p>
	 * This method does not need to be called in the UI thread.
	 * </p>
	 * 
	 * @param projects The projects to search in.
	 * @return The descriptions of projects physically located within
	 * the given project.
	 */
	public IProjectDescription[] findNestedProjects(IProject[] projects) {
		final ArrayList<IProjectDescription> descriptions = new ArrayList<>();
		for (int i = 0; i < projects.length; i++) {
			if (!projects[i].isAccessible())
				continue;
			try {
				projects[i].accept(new IResourceProxyVisitor() {
					private void addDescription(final ArrayList<IProjectDescription> descriptions, IProjectDescription description) {
						IProject project = workspace.getRoot().getProject(description.getName());
						if (!project.exists())
							descriptions.add(description);
					}

					/**
					 * This linked resource may be blocking resources in the project's
					 * location from the workspace.  Search for project descriptions
					 * in the sub-tree of the project location corresponding to this resource.
					 */
					private void searchInLink(IResource link) {
						IProject project = link.getProject();
						if (project == null || !project.isAccessible())
							return;
						IFileStore projectStore;
						try {
							projectStore = EFS.getStore(project.getLocationURI());
						} catch (CoreException e) {
							//ignore projects in invalid locations
							return;
						}
						IFileStore linkStore = projectStore.getFileStore(link.getProjectRelativePath());
						searchInStore(linkStore);

					}

					/**
					 * Searches for project description files within the given store,
					 * and adds any found descriptions to the supplied list.
					 */
					private void searchInStore(IFileStore store) {
						try {
							if (store.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
								IFileInfo info = store.fetchInfo();
								if (!info.isDirectory()) {
									try (InputStream input = store.openInputStream(EFS.NONE, null)) {
										IProjectDescription description = workspace.loadProjectDescription(input);
										description.setLocationURI(store.getParent().toURI());
										addDescription(descriptions, description);
									} catch(IOException e) {
										// ignore
									}
									return;
								}
							}
							//recurse on children
							IFileStore[] children = store.childStores(EFS.NONE, null);
							for (int j = 0; j < children.length; j++)
								searchInStore(children[j]);
						} catch (CoreException e) {
							//ignore if there are problems accessing the file system
						}
					}

					@Override
					public boolean visit(IResourceProxy proxy) throws CoreException {
						if (proxy.getType() == IResource.FILE && proxy.getName().equals(IProjectDescription.DESCRIPTION_FILE_NAME)) {
							IFile file = (IFile) proxy.requestResource();
							try (InputStream input = file.getContents()) {
								IProjectDescription description = workspace.loadProjectDescription(input);
								description.setLocationURI(file.getParent().getLocationURI());
								addDescription(descriptions, description);
							} catch (CoreException | IOException e) {
								//ignore this project
							}
						} else if (proxy.isLinked()) {
							//search in the directory hidden by the link
							searchInLink(proxy.requestResource());
							return false;
						}
						return true;
					}
				}, IResource.NONE);
			} catch (CoreException e) {
				//if the project cannot be visited just ignore it
			}
		}
		return descriptions.toArray(new IProjectDescription[descriptions.size()]);
	}

	/**
	 * Returns a label provider that is capable of displaying project descriptions.
	 * This provider just converts IProjectDescription->IProject, and then
	 * uses the standard {@link WorkbenchLabelProvider}.
	 */
	private ILabelProvider getProjectDescriptionLabelProvider() {
		return new LabelProvider() {
			private final LabelProvider realProvider = new WorkbenchLabelProvider();
			private final IWorkspaceRoot root = workspace.getRoot();

			@Override
			public Image getImage(Object element) {
				return realProvider.getImage(getProject(element));
			}

			private Object getProject(Object element) {
				if (element instanceof IProjectDescription)
					return root.getProject(((IProjectDescription) element).getName());
				return null;
			}

			@Override
			public String getText(Object element) {
				return realProvider.getText(getProject(element));
			}
		};
	}

	/**
	 * Returns a status corresponding to the given throwable.
	 */
	private IStatus getStatus(Throwable t) {
		if (t instanceof CoreException)
			return ((CoreException) t).getStatus();
		return new Status(IStatus.ERROR, Policy.PI_FILESYSTEM_EXAMPLE, 1, "Internal Error", t); //$NON-NLS-1$
	}

	/**
	 * Given a set of project descriptions, prompt the user to ask if they want
	 * to create them.  Returns the project descriptions that the user asks to create.
	 * 
	 * @param parentShell The shell to use when parenting dialogs
	 * @param descriptions The descriptions that can be created
	 * @return The descriptions that the user has selected for creation
	 */
	public IProjectDescription[] promptForCreation(Shell parentShell, IProjectDescription[] descriptions) {
		String message = "The following projects were found. Select the projects to be created.";
		ListSelectionDialog dialog = ListSelectionDialog.of(descriptions)
				.contentProvider(new ArrayContentProvider())
				.labelProvider(getProjectDescriptionLabelProvider())
				.message(message)
				.create(parentShell);
		dialog.open();
		Object[] result = dialog.getResult();
		IProjectDescription[] castedResult = new IProjectDescription[result.length];
		System.arraycopy(result, 0, castedResult, 0, result.length);
		return castedResult;
	}

	/**
	 * Sets the runnable context that should be used to show progress during 
	 * any long running operations. 
	 * @param rc The context to use
	 */
	public void setRunnableContext(IRunnableContext rc) {
		context = rc != null ? rc : PlatformUI.getWorkbench().getProgressService();
	}
}
