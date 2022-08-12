/*******************************************************************************
 * Copyright (c) 2000, 2009 IBM Corporation and others.
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
package org.eclipse.team.examples.pessimistic;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.CheckedTreeSelectionDialog;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * The <code>ResourceChangeListener</code> listens for resource changes
 * and (optionally) prompts the user to add the new resources to the
 * control of the repository provider.
 */
public class ResourceChangeListener implements IResourceDeltaVisitor, IResourceChangeListener {
	/*
	 * Set of added resources
	 */
	private Set<IResource> fAdded;
	/*
	 * Set of removed resources
	 */
	private Set<IResource> fRemoved;

	public ResourceChangeListener() {
		fAdded = new HashSet<>(1);
		fRemoved = new HashSet<>(1);
	}

	/**
	 * Looks for the following changes:
	 * <ul>
	 *   <li>Resources that are controlled and are removed</li>
	 *   <li>Resources that are added under a managed project</li>
	 * </ul>
	 * @see org.eclipse.core.resources.IResourceDeltaVisitor#visit(IResourceDelta)
	 */
	@Override
	public boolean visit(IResourceDelta delta) {
		IResource resource= delta.getResource();
		if (resource != null) {
			IProject project= resource.getProject();
			if (project != null) {
				PessimisticFilesystemProvider provider= (PessimisticFilesystemProvider)RepositoryProvider.getProvider(project, PessimisticFilesystemProviderPlugin.NATURE_ID);
				if (provider == null)
					return false;
				if (provider.isControlled(resource)) {
					switch (delta.getKind()) {
					case IResourceDelta.CHANGED:
					case IResourceDelta.ADDED:
						return true;
					case IResourceDelta.REMOVED:
						fRemoved.add(resource);
						return false;
					}
				} else {
					switch (delta.getKind()) {
					case IResourceDelta.CHANGED:
					case IResourceDelta.REMOVED:
						return true;
					case IResourceDelta.ADDED:
						// don't prompt for ignored resources
						if (!provider.isIgnored(resource)) {
							fAdded.add(resource);
						}
						return true;
					}
				}
			} else {
				return true;
			}
		}
		return false;
	}

	/*
	 * Convenience method to return a resource array from a collection
	 */
	private IResource[] toResourceArray(Collection<IResource> collection) {
		if (collection.isEmpty()) {
			return new IResource[0];
		}
		IResource[] resources= new IResource[collection.size()];
		collection.toArray(resources);
		return resources;
	}

	@Override
	public void resourceChanged (IResourceChangeEvent event) {
		try {
			event.getDelta().accept(this);
		} catch (CoreException e) {
			e.printStackTrace();
			PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Exceptions during resource callback");
		}

		if (!fRemoved.isEmpty() || !fAdded.isEmpty()) {
			final IWorkspaceRunnable workspaceRunnable= monitor -> {
				if (!fRemoved.isEmpty()) {
					remove(monitor);
				}

				if (!fAdded.isEmpty()) {
					add(monitor);
				}
			};
			// must fork since we are in resource callback.
			Runnable run= () -> {
				try {
					IWorkspace workspace= ResourcesPlugin.getWorkspace();
					if (workspace != null) {
						workspace.run(workspaceRunnable, null);
					}
				} catch (CoreException e) {
					PessimisticFilesystemProviderPlugin.getInstance().logError(e, "Problems encountered during attempt to add/remove control.");
				}
			};
			new Thread(run).start();
		}
	}

	/*
	 * Convenience method to get the preference for what to do
	 * when new resource have been detected.
	 */
	private int getAddToControlPreference() {
		Preferences preferences= PessimisticFilesystemProviderPlugin.getInstance().getPluginPreferences();
		return preferences.getInt(IPessimisticFilesystemConstants.PREF_ADD_TO_CONTROL);
	}

	/*
	 * Adds the resources to the control of the provider.
	 * If the add to control preference is:
	 *	do nothing - does not add
	 *  automatic - adds all resources
	 *  prompt - brings up a prompt which requests that the user
	 * 				select which resources to add
	 */
	private void add(final IProgressMonitor monitor) {
		switch (getAddToControlPreference()) {
		case IPessimisticFilesystemConstants.OPTION_DO_NOTHING:
			break;
		case IPessimisticFilesystemConstants.OPTION_AUTOMATIC:
			addToControl(fAdded, monitor);
			break;
		case IPessimisticFilesystemConstants.OPTION_PROMPT:
			final Shell shell= getShell();
			if (shell != null && !shell.isDisposed()) {
				final Set<IResource> resources = new HashSet<>(fAdded);
				Runnable run= () -> {
					CheckedTreeSelectionDialog dialog= new CheckedTreeSelectionDialog(shell, new WorkbenchLabelProvider(), new ResourceSetContentProvider(resources));
					dialog.setMessage("Select the resources to be added to the control of the repository.");
					dialog.setTitle("Add resources to control");
					dialog.setContainerMode(true);
					dialog.setBlockOnOpen(true);
					dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));
					Object[] resourceArray= resources.toArray();
					dialog.setExpandedElements(resourceArray);
					dialog.setInitialSelections(resourceArray);
					dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
					int status= dialog.open();

					if (status == Window.OK) {
						Object[] results= dialog.getResult();
						if (results != null) {
							Set<IResource> resources1 = new HashSet<>(results.length);
							for (Object result : results) {
								resources1.add((IResource) result);
							}
							addToControl(resources1, monitor);
						}
					}
				};

				Display display= shell.getDisplay();
				display.asyncExec(run);
			} else {
				PessimisticFilesystemProviderPlugin.getInstance().logError(null, "Could not aquire a shell");
			}
			break;
		}
		fAdded.clear();
	}

	/*
	 * Adds the resources to the control of the provider.
	 */
	private void addToControl(Collection<IResource> resources, final IProgressMonitor monitor) {
		Map<IProject, Set<IResource>> byProject = sortByProject(resources);
		for (IProject project : byProject.keySet()) {
			PessimisticFilesystemProvider provider= (PessimisticFilesystemProvider)RepositoryProvider.getProvider(project, PessimisticFilesystemProviderPlugin.NATURE_ID);
			if (provider != null) {
				provider.addToControl(toResourceArray(byProject.get(project)), monitor);
			}

		}
	}

	/*
	 * Removes the resources from the control of the provider.
	 */
	private void remove(IProgressMonitor monitor) {
		Map<IProject, Set<IResource>> byProject = sortByProject(fRemoved);
		for (IProject project : byProject.keySet()) {
			PessimisticFilesystemProvider provider= (PessimisticFilesystemProvider)RepositoryProvider.getProvider(project, PessimisticFilesystemProviderPlugin.NATURE_ID);
			if (provider != null) {
				provider.removeFromControl(toResourceArray(byProject.get(project)), monitor);
			}
		}
		fRemoved.clear();
	}

	/*
	 * Convenience method to sort the resources by project
	 */
	private Map<IProject, Set<IResource>> sortByProject(Collection<IResource> resources) {
		Map<IProject, Set<IResource>> byProject = new HashMap<>();
		for (IResource resource : resources) {
			IProject project= resource.getProject();
			Set<IResource> set = byProject.get(project);
			if (set == null) {
				set = new HashSet<>(1);
				byProject.put(project, set);
			}
			set.add(resource);
		}
		return byProject;
	}

	/*
	 * Convenience method which answers a shell with which to prompt.
	 */
	private Shell getShell() {
		IWorkbench workbench= PlatformUI.getWorkbench();
		if (workbench != null) {
			IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
			if (window == null) {
				IWorkbenchWindow[] windows= workbench.getWorkbenchWindows();
				if (windows != null && windows.length > 0) {
					window= windows[0];
				}
			}
			if (window != null) {
				Shell shell= window.getShell();
				if (shell == null)
					return null;
				if (shell.isDisposed())
					return null;
				return shell;
			}
		}
		return null;
	}

	/**
	 * Starts listening for changes.
	 */
	public void startup() {
		ResourcesPlugin.getWorkspace().addResourceChangeListener(this, IResourceChangeEvent.POST_CHANGE);
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging())
			System.out.println ("Resource callback registered");
	}

	/**
	 * Stops listening for changes.
	 */
	public void shutdown() {
		ResourcesPlugin.getWorkspace().removeResourceChangeListener(this);
		if (PessimisticFilesystemProviderPlugin.getInstance().isDebugging())
			System.out.println ("Resource callback unregistered");
	}
}
