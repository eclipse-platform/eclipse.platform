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
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.operations;

import java.util.*;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.ui.IWorkbenchPart;

/**
 * Operation that divides the resources by location
 */
public abstract class RepositoryLocationOperation extends RemoteOperation {
	
	protected RepositoryLocationOperation(IWorkbenchPart part, ICVSRemoteResource[] remoteResources) {
		super(part, remoteResources);
	}
	
	@Override
	public void execute(IProgressMonitor monitor) throws CVSException, InterruptedException {
		Map table = getLocationMapping(getRemoteResources());
		Set keySet = table.keySet();
		monitor.beginTask(null, keySet.size() * 100);
		Iterator iterator = keySet.iterator();
		while (iterator.hasNext()) {
			ICVSRepositoryLocation location = (ICVSRepositoryLocation)iterator.next();
			List list = (List)table.get(location);
			ICVSRemoteResource[] remoteResources = (ICVSRemoteResource[])list.toArray(new ICVSRemoteResource[list.size()]);
			execute(location, remoteResources, Policy.subMonitorFor(monitor, 100));
		}
	}
	
	/**
	 * Perform the operation for the given resources found on the
	 * given repository.
	 * @param location the repository location
	 * @param resources the resources of this operation found in the repository
	 * @param monitor a progres monitor
	 */
	protected abstract void execute(ICVSRepositoryLocation location, ICVSRemoteResource[] resources, IProgressMonitor monitor) throws CVSException;

	/*
	 * Return a map that maps a location to all the resources
	 * from the given list that are located in that repository.
	 */
	private Map getLocationMapping(ICVSRemoteResource[] remoteResources) {
		Map locationsMap = new HashMap();
		for (ICVSRemoteResource resource : remoteResources) {
			ICVSRepositoryLocation location = resource.getRepository();
			List resources = (List)locationsMap.get(location);
			if (resources == null) {
				resources = new ArrayList();
				locationsMap.put(location, resources);
			}
			resources.add(resource);
		}
		return locationsMap;
	}

}
