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
package org.eclipse.team.internal.ccvs.core.syncinfo;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.subscribers.BatchingLock;

/**
 * Provides a per-thread nested locking mechanism. A thread can acquire a
 * lock on a specific resource by calling acquire(). Subsequently, acquire() can be called
 * multiple times on the resource or any of its children from within the same thread
 * without blocking. Other threads that try
 * and acquire the lock on those same resources will be blocked until the first 
 * thread releases all it's nested locks.
 * <p>
 * The locking is managed by the platform via scheduling rules. This class simply 
 * provides the nesting mechnism in order to allow the client to determine when
 * the lock for the thread has been released. Therefore, this lock will block if
 * another thread already locks the same resource.</p>
 */
public class ReentrantLock extends BatchingLock {
	
	public class CVSThreadInfo extends ThreadInfo{
		private Set changedFolders = new HashSet();
		public CVSThreadInfo(IFlushOperation operation) {
			super(operation);
		}
		public void addChangedFolder(IContainer container) {
			changedFolders.add(container);
		}
		public boolean isEmpty() {
			return changedFolders.isEmpty() && super.isEmpty();
		}
		public IContainer[] getChangedFolders() {
			return (IContainer[]) changedFolders.toArray(new IContainer[changedFolders.size()]);
		}
		public void flush(IProgressMonitor monitor) throws TeamException {
			try {
				super.flush(monitor);
			} finally {
				// We have to clear the resources no matter what since the next attempt
				// to flush may not have an appropriate scheduling rule
				changedFolders.clear();
			}
		}
	}
	
	@Override
	protected ThreadInfo createThreadInfo(IFlushOperation operation) {
		return new CVSThreadInfo(operation);
	}
	
	public void folderChanged(IContainer folder) {
		CVSThreadInfo info = (CVSThreadInfo)getThreadInfo();
		Assert.isNotNull(info, "Folder changed outside of resource lock"); //$NON-NLS-1$
		info.addChangedFolder(folder);
	}

}
