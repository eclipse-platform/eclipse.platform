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
package org.eclipse.team.internal.core.subscribers;

import org.eclipse.core.resources.IResource;

public class WorkingSetSyncSetInput extends SyncSetInputFromSyncSet {

	private SyncInfoWorkingSetFilter workingSetFilter = new SyncInfoWorkingSetFilter();

	public WorkingSetSyncSetInput(SubscriberSyncInfoSet set, SubscriberEventHandler handler) {
		super(set, handler);
		setFilter(workingSetFilter);
	}

	public void setWorkingSet(IResource[] resources) {
		workingSetFilter.setWorkingSet(resources);
	}

	public IResource[] getWorkingSet() {
		return workingSetFilter.getWorkingSet();
	}
}
