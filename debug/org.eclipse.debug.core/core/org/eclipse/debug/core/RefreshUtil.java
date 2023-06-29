/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.debug.core;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.debug.internal.core.DebugCoreMessages;
import org.eclipse.debug.internal.core.IMementoConstants;
import org.eclipse.debug.internal.core.ResourceFactory;
import org.eclipse.debug.internal.core.XMLMemento;

/**
 * Utilities for launch configurations that persist, restore, and refresh
 * collections of resources.
 *
 * @since 3.6
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class RefreshUtil {

	/**
	 * String attribute identifying a scope of resources that should be
	 * refreshed - for example, after an external tool is run. The value is either
	 * a resource memento constant by this class, a resource memento created
	 * via {@link RefreshUtil#toMemento(IResource[])}, <code>null</code>, indicating no
	 * refresh.
	 */
	public static final String ATTR_REFRESH_SCOPE = DebugPlugin.getUniqueIdentifier() + ".ATTR_REFRESH_SCOPE"; //$NON-NLS-1$

	/**
	 * Boolean attribute indicating if a refresh scope is recursive. Default
	 * value is <code>true</code>. When a refresh is recursive, resources are
	 * refreshed to an infinite depth, otherwise they are refreshed to a depth
	 * of one.
	 */
	public static final String ATTR_REFRESH_RECURSIVE = DebugPlugin.getUniqueIdentifier() + ".ATTR_REFRESH_RECURSIVE"; //$NON-NLS-1$

	/**
	 * Resource memento referring to the selected resource's project.
	 * Only works when the debug user interface is running.
	 *
	 * @see #toResources(String)
	 */
	public static final String MEMENTO_SELECTED_PROJECT = "${project}"; //$NON-NLS-1$

	/**
	 * Resource memento referring to the selected resource's container.
	 * Only works when the debug user interface is running.
	 *
	 * @see #toResources(String)
	 */
	public static final String MEMENTO_SELECTED_CONTAINER = "${container}"; //$NON-NLS-1$

	/**
	 * Resource memento referring to the selected resource.
	 * Only works when the debug user interface is running.
	 *
	 * @see #toResources(String)
	 */
	public static final String MEMENTO_SELECTED_RESOURCE = "${resource}"; //$NON-NLS-1$

	/**
	 * Resource memento referring to the workspace root.
	 *
	 * @see #toResources(String)
	 */
	public static final String MEMENTO_WORKSPACE = "${workspace}"; //$NON-NLS-1$

	/**
	 *  Indicates no working set has been selected (for backwards compatibility).
	 *  The new format uses an empty working set
	 */

	private static final String NO_WORKING_SET = "NONE"; //$NON-NLS-1$

	/**
	 * Refreshes the resources as specified by the given launch configuration.
	 *
	 * @param resources
	 *            resources to refresh
	 * @param depth one of {@link IResource#DEPTH_INFINITE}, {@link IResource#DEPTH_ONE},
	 *  or {@link IResource#DEPTH_ZERO}
	 * @param monitor
	 *            progress monitor which may be <code>null</code>
	 * @throws CoreException
	 *             if an exception occurs while refreshing resources
	 */
	public static void refreshResources(IResource[] resources, int depth, IProgressMonitor monitor) throws CoreException {
		if (resources == null || resources.length == 0) {
			return;
		}
		SubMonitor lmonitor = SubMonitor.convert(monitor, DebugCoreMessages.RefreshingResources, resources.length);
		if (lmonitor.isCanceled()) {
			return;
		}
		MultiStatus status = new MultiStatus(DebugPlugin.getUniqueIdentifier(), 0, DebugCoreMessages.RefreshingResourcesError, null);
		try {
			for (IResource resource : resources) {
				if (lmonitor.isCanceled()) {
					break;
				}
				if (resource != null && resource.isAccessible()) {
					try {
						resource.refreshLocal(depth, null);
					} catch (CoreException e) {
						status.merge(e.getStatus());
					}
				}
				lmonitor.worked(1);
			}
		} finally {
			lmonitor.done();
		}
		if (!status.isOK()) {
			throw new CoreException(status);
		}
	}

	/**
	 * Returns a collection of resources referred to by the specified
	 * memento generated via {@link #toMemento(IResource[])}.
	 *
	 * @param memento
	 *            resource memento generated by this manager
	 * @return collection of resources referred to by the memento
	 * @throws CoreException
	 *             if unable to resolve a set of resources
	 */
	public static IResource[] toResources(String memento) throws CoreException {
		if (memento.startsWith("${resource:")) { //$NON-NLS-1$
			// This is an old format that is replaced with 'working_set'
			String pathString = memento.substring(11, memento.length() - 1);
			IPath path = IPath.fromOSString(pathString);
			IResource resource = ResourcesPlugin.getWorkspace().getRoot().findMember(path);
			if (resource == null) {
				throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), IStatus.ERROR, MessageFormat.format(DebugCoreMessages.RefreshUtil_1, pathString), null));
			}
			return new IResource[] { resource };
		} else if (memento.startsWith("${working_set:")) { //$NON-NLS-1$
			String ws = memento.substring(14, memento.length() - 1);
			return getResources(ws);
		} else if (memento.equals(MEMENTO_WORKSPACE)) {
			return new IResource[] { ResourcesPlugin.getWorkspace().getRoot() };
		} else {
			// result the selected resource for backwards compatibility
			IStringVariableManager manager = VariablesPlugin.getDefault().getStringVariableManager();
			IResource resource = null;
			try {
				String pathString = manager.performStringSubstitution("${selected_resource_path}"); //$NON-NLS-1$
				resource = ResourcesPlugin.getWorkspace().getRoot().findMember(IPath.fromOSString(pathString));
			} catch (CoreException e) {
				// unable to resolve a resource
			}
			if (resource == null) {
				// empty selection
				return new IResource[]{};
			} else if (memento.equals(MEMENTO_SELECTED_RESOURCE)) {
				return new IResource[] { resource };
			} else if (memento.equals(MEMENTO_SELECTED_CONTAINER)) {
				return new IResource[] {resource.getParent()};
			} else if (memento.equals(MEMENTO_SELECTED_PROJECT)) {
				return new IResource[] {resource.getProject()};
			}
		}
		throw new CoreException(new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), MessageFormat.format(DebugCoreMessages.RefreshUtil_0, memento)));
	}

	/**
	 * Returns a memento for a collection of resources that can be restored
	 * via {@link #toResources(String)}.
	 *
	 * @param resources resources to create a memento for
	 * @return memento for the given resources
	 */
	public static String toMemento(IResource[] resources) {
		XMLMemento memento = XMLMemento.createWriteRoot("resources"); //$NON-NLS-1$
		for (IResource resource : resources) {
			final XMLMemento itemMemento = memento.createChild(IMementoConstants.MEMENTO_ITEM);
			ResourceFactory.saveState(itemMemento, resource);
		}
		StringWriter writer = new StringWriter();
		try {
			memento.save(writer);
		} catch (IOException e) {
			DebugPlugin.log(e);
		}
		StringBuilder buf = new StringBuilder();
		buf.append("${working_set:"); //$NON-NLS-1$
		buf.append(writer.toString());
		buf.append("}"); //$NON-NLS-1$
		return buf.toString();
	}

	/**
	 * Restores a collection of resources from a working set memento, for backwards
	 * compatibility.
	 *
	 * @param wsMemento working set memento
	 * @return resource collection, possibly empty
	 */
	private static IResource[] getResources(String wsMemento) {

		if (NO_WORKING_SET.equals(wsMemento)) {
			return null;
		}

		List<IAdaptable> resourcesList = new ArrayList<>();
		StringReader reader = new StringReader(wsMemento);

		XMLMemento memento = null;
		try {
			memento = XMLMemento.createReadRoot(reader);
		} catch (Exception e) {
			DebugPlugin.log(e);
			return null;
		}

		XMLMemento[] mementos = memento.getChildren(IMementoConstants.MEMENTO_ITEM);
		for (XMLMemento m : mementos) {
			resourcesList.add(ResourceFactory.createElement(m));
		}

		return resourcesList.toArray(new IResource[resourcesList.size()]);

	}

	/**
	 * Returns whether the refresh scope specified by the given launch
	 * configuration is recursive.
	 *
	 * @param configuration the {@link ILaunchConfiguration}
	 * @return whether the refresh scope is recursive
	 * @throws CoreException
	 *             if unable to access the associated attribute
	 */
	public static  boolean isRefreshRecursive(ILaunchConfiguration configuration) throws CoreException {
		return configuration.getAttribute(ATTR_REFRESH_RECURSIVE, true);
	}

	/**
	 * Refreshes the resources as specified by the given launch configuration via its
	 * {@link RefreshUtil#ATTR_REFRESH_SCOPE} and {@link #ATTR_REFRESH_RECURSIVE} attributes.
	 *
	 * @param configuration launch configuration
	 * @param monitor progress monitor which may be <code>null</code>
	 * @throws CoreException
	 *             if an exception occurs while refreshing resources or accessing launch
	 *             configuration attributes
	 */
	public static void refreshResources(ILaunchConfiguration configuration, IProgressMonitor monitor) throws CoreException {
		String scope = configuration.getAttribute(ATTR_REFRESH_SCOPE, (String) null);
		if (scope != null) {
			IResource[] resources = toResources(scope);
			if (resources != null && resources.length > 0) {
				int depth = IResource.DEPTH_ONE;
				if (isRefreshRecursive(configuration)) {
					depth = IResource.DEPTH_INFINITE;
				}
				refreshResources(resources, depth, monitor);
			}
		}
	}
}
