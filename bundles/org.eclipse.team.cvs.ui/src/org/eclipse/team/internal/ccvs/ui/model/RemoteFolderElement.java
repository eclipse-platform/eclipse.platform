/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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

package org.eclipse.team.internal.ccvs.ui.model;
import java.lang.reflect.InvocationTargetException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.FetchMembersOperation;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.progress.IDeferredWorkbenchAdapter;
import org.eclipse.ui.progress.IElementCollector;

public class RemoteFolderElement extends RemoteResourceElement implements IDeferredWorkbenchAdapter {

	/**
	 * Overridden to append the version name to remote folders which
	 * have version tags and are top-level folders.
	 */
	public String getLabel(Object o) {
		if (!(o instanceof ICVSRemoteFolder))
			return null;
		ICVSRemoteFolder folder = (ICVSRemoteFolder) o;
		CVSTag tag = folder.getTag();
		if (tag != null && tag.getType() != CVSTag.HEAD) {
			if (folder.getRemoteParent() == null) {
				return NLS.bind(CVSUIMessages.RemoteFolderElement_nameAndTag, new String[] { folder.getName(), tag.getName() }); 
			}
		}
		return folder.getName();
	}

	public ImageDescriptor getImageDescriptor(Object object) {
		if (!(object instanceof ICVSRemoteFolder))
			return null;
		ICVSRemoteFolder folder = (ICVSRemoteFolder) object;
		if (folder.isDefinedModule()) {
			return CVSUIPlugin.getPlugin().getImageDescriptor(ICVSUIConstants.IMG_MODULE);
		}
		return PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER);
	}

	public Object[] fetchChildren(Object o, IProgressMonitor monitor) throws TeamException {
		if (!(o instanceof ICVSRemoteFolder))
			return new Object[0];
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(NLS.bind(
					CVSUIMessages.RemoteFolderElement_fetchingRemoteChildren,
					new String[] { getLabel(o) }), 100);
			ICVSRemoteFolder folder = (ICVSRemoteFolder) o;
			ICVSRemoteResource[] cachedChildren = CVSUIPlugin
					.getPlugin()
					.getRepositoryManager()
					.getCachedChildrenForTag(folder.getRepository(), folder,
							folder.getTag(), Policy.subMonitorFor(monitor, 50));
			if (cachedChildren.length > 0) {
				return cachedChildren;
			}
			return folder.members(Policy.subMonitorFor(monitor, 50));
		} finally {
			monitor.done();
		}
	}

	public void fetchDeferredChildren(Object o, IElementCollector collector, IProgressMonitor monitor) {
		// If it's not a folder, return an empty array
		if (!(o instanceof ICVSRemoteFolder)) {
			collector.add(new Object[0], monitor);
		}
		try {
			monitor = Policy.monitorFor(monitor);
			monitor.beginTask(NLS.bind(
					CVSUIMessages.RemoteFolderElement_fetchingRemoteChildren,
					new String[] { getLabel(o) }), 100);
			ICVSRemoteFolder folder = (ICVSRemoteFolder) o;
			ICVSRemoteResource[] cachedChildren = CVSUIPlugin
					.getPlugin()
					.getRepositoryManager()
					.getCachedChildrenForTag(folder.getRepository(), folder,
							folder.getTag(), Policy.subMonitorFor(monitor, 50));
			if (cachedChildren.length > 0) {
				collector.add(cachedChildren, monitor);
				return;
			}
			FetchMembersOperation operation = new FetchMembersOperation(null,
					folder, collector);
			operation.run(Policy.subMonitorFor(monitor, 50));
		} catch (InvocationTargetException e) {
			handle(collector, e);
		} catch (InterruptedException e) {
			// Cancelled by the user;
		} catch (CVSException e) {
			handle(collector, e);
		} finally {
			monitor.done();
		}
	}

	public ISchedulingRule getRule(Object element) {
		ICVSRepositoryLocation location = getRepositoryLocation(element);
		return new RepositoryLocationSchedulingRule(location); 
	}

	private ICVSRepositoryLocation getRepositoryLocation(Object o) {
		if (!(o instanceof ICVSRemoteFolder))
			return null;
		return ((ICVSRemoteFolder)o).getRepository();
	}

	public boolean isContainer() {
		return true;
	}
}
