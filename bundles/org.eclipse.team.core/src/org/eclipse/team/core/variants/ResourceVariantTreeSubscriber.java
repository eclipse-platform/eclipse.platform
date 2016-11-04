/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.core.variants;

import java.util.*;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.TeamStatus;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.subscribers.SubscriberChangeEvent;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.core.*;

/**
 * A specialization of Subscriber that uses <code>IResourceVariantTree</code> objects
 * to manage the base (for three-way) and remote trees. Refreshing and obtaining the subscriber
 * members and resource variants is delegated to the resource variant trees.
 *
 * @since 3.0
 */
public abstract class ResourceVariantTreeSubscriber extends Subscriber {

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.Subscriber#getSyncInfo(org.eclipse.core.resources.IResource)
	 */
	public SyncInfo getSyncInfo(IResource resource) throws TeamException {
		if (!isSupervised(resource)) return null;
		IResourceVariant remoteResource = getRemoteTree().getResourceVariant(resource);
		IResourceVariant baseResource;
		if (getResourceComparator().isThreeWay()) {
			baseResource= getBaseTree().getResourceVariant(resource);
		} else {
			baseResource = null;
		}
		return getSyncInfo(resource, baseResource, remoteResource);
	}

	/**
	 * Method that creates an instance of SyncInfo for the provided local, base and remote
	 * resource variants.
	 * Can be overridden by subclasses.
	 * @param local the local resource
	 * @param base the base resource variant or <code>null</code>
	 * @param remote the remote resource variant or <code>null</code>
	 * @return the <code>SyncInfo</code> containing the provided resources
	 */
	protected SyncInfo getSyncInfo(IResource local, IResourceVariant base, IResourceVariant remote) throws TeamException {
		SyncInfo info = new SyncInfo(local, base, remote, this.getResourceComparator());
		info.init();
		return info;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.Subscriber#members(org.eclipse.core.resources.IResource)
	 */
	public IResource[] members(IResource resource) throws TeamException {
		if(resource.getType() == IResource.FILE) {
			return new IResource[0];
		}
		try {
			Set allMembers = new HashSet();
			try {
				allMembers.addAll(Arrays.asList(((IContainer)resource).members()));
			} catch (CoreException e) {
				if (e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
					// The resource is no longer exists so ignore the exception
				} else {
					throw e;
				}
			}
			allMembers.addAll(Arrays.asList(internalMembers(getRemoteTree(), resource)));
			if (getResourceComparator().isThreeWay()) {
				allMembers.addAll(Arrays.asList(internalMembers(getBaseTree(), resource)));
			}
			for (Iterator iterator = allMembers.iterator(); iterator.hasNext();) {
				IResource member = (IResource) iterator.next();
				if(!member.exists() && !getRemoteTree().hasResourceVariant(member)) {
					// Remove deletion conflicts
					iterator.remove();
				} else if (!isSupervised(member)) {
					// Remove unsupervised resources
					iterator.remove();
				}
			}
			return (IResource[]) allMembers.toArray(new IResource[allMembers.size()]);
		} catch (CoreException e) {
			throw TeamException.asTeamException(e);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.core.subscribers.Subscriber#refresh(org.eclipse.core.resources.IResource[], int, org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void refresh(IResource[] resources, int depth, IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		List errors = new ArrayList();
		List cancels = new ArrayList();
		try {
			monitor.beginTask(null, 1000 * resources.length);
			for (int i = 0; i < resources.length; i++) {
				IResource resource = resources[i];
				if (resource.getProject().isAccessible()) {
					IStatus status = refresh(resource, depth, Policy.subMonitorFor(monitor, 1000));
					if (status.getSeverity() == IStatus.CANCEL) {
						cancels.add(status);
					} else if (!status.isOK()) {
						errors.add(status);
					}
				}
			}
		} finally {
			monitor.done();
		}
		if (!errors.isEmpty()) {
			int numSuccess = resources.length - errors.size() - cancels.size();
			if (!cancels.isEmpty()) {
				errors.addAll(cancels);
				throw new TeamException(new MultiStatus(TeamPlugin.ID, 0,
						(IStatus[]) errors.toArray(new IStatus[errors.size()]),
						NLS.bind(
								Messages.ResourceVariantTreeSubscriber_3,
								(new Object[] { getName(),
										Integer.toString(numSuccess),
										Integer.toString(resources.length),
										Integer.toString(cancels.size()) })),
						null) {
					public int getSeverity() {
						// we want to display status as an error
						return IStatus.ERROR;
					}
				});
			}
			throw new TeamException(new MultiStatus(TeamPlugin.ID, 0,
					(IStatus[]) errors.toArray(new IStatus[errors.size()]),
					NLS.bind(Messages.ResourceVariantTreeSubscriber_1, (new Object[] {getName(), Integer.toString(numSuccess), Integer.toString(resources.length)})), null));
		}
		if (!cancels.isEmpty()) {
			throw new OperationCanceledException(
					((IStatus) cancels.get(0)).getMessage());
		}
	}

	/**
	 * Return the base resource variant tree.
	 */
	protected abstract IResourceVariantTree getBaseTree();

	/**
	 * Return the remote resource variant tree.
	 */
	protected abstract IResourceVariantTree getRemoteTree();

	private IStatus refresh(IResource resource, int depth, IProgressMonitor monitor) {
		monitor = Policy.monitorFor(monitor);
		try {
			monitor.beginTask(null, 100);
			Set allChanges = new HashSet();
			if (getResourceComparator().isThreeWay()) {
				IResource[] baseChanges = getBaseTree().refresh(new IResource[] {resource}, depth, Policy.subMonitorFor(monitor, 25));
				allChanges.addAll(Arrays.asList(baseChanges));
			}
			IResource[] remoteChanges = getRemoteTree().refresh(new IResource[] {resource}, depth, Policy.subMonitorFor(monitor, 75));
			allChanges.addAll(Arrays.asList(remoteChanges));
			IResource[] changedResources = (IResource[]) allChanges.toArray(new IResource[allChanges.size()]);
			fireTeamResourceChange(SubscriberChangeEvent.asSyncChangedDeltas(this, changedResources));
			return Status.OK_STATUS;
		} catch (TeamException e) {
			return new TeamStatus(IStatus.ERROR, TeamPlugin.ID, 0, NLS.bind(Messages.ResourceVariantTreeSubscriber_2, new String[] { resource.getFullPath().toString(), e.getMessage() }), e, resource);
		} catch (OperationCanceledException e) {
			return new TeamStatus(IStatus.CANCEL, TeamPlugin.ID, 0, NLS.bind(
					Messages.ResourceVariantTreeSubscriber_4,
					new String[] { resource.getFullPath().toString() }), e,
					resource);
		} finally {
			monitor.done();
		}
	}

	private IResource[] internalMembers(IResourceVariantTree tree, IResource resource) throws TeamException, CoreException {
		// Filter and return only phantoms associated with the remote synchronizer.
		IResource[] members;
		try {
			members = tree.members(resource);
		} catch (CoreException e) {
			if (!isSupervised(resource) || e.getStatus().getCode() == IResourceStatus.RESOURCE_NOT_FOUND) {
				// The resource is no longer supervised or doesn't exist in any form
				// so ignore the exception and return that there are no members
				return new IResource[0];
			}
			throw e;
		}
		return members;
	}
}
