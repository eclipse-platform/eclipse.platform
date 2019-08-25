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

import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.ui.IWorkbenchPart;

public abstract class RemoteOperation extends CVSOperation {

	private ICVSRemoteResource[] remoteResources;

	protected RemoteOperation(IWorkbenchPart part, ICVSRemoteResource[] remoteResources) {
		super(part);
		this.remoteResources =remoteResources;
	}

	protected ICVSRemoteResource[] getRemoteResources() {
		return remoteResources;
	}
	
	public ICVSResource[] getCVSResources() {
		ICVSResource[] cvsResources = new ICVSResource[remoteResources.length];
		System.arraycopy(remoteResources, 0, cvsResources, 0, remoteResources.length);
		return cvsResources;
	}

}
