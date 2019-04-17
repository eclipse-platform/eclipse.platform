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
package org.eclipse.team.internal.ccvs.core.connection;

 
import org.eclipse.team.internal.ccvs.core.*;

public class ExtConnectionMethod implements IConnectionMethod {
	@Override
	public String getName() {
		return "ext"; //$NON-NLS-1$
	}
	
	@Override
	public IServerConnection createConnection(ICVSRepositoryLocation repositoryRoot, String password) {
		if(password==null){
			password=""; //$NON-NLS-1$
		}
		return new ExtConnection(repositoryRoot, password);
	}
	
	public void disconnect(ICVSRepositoryLocation location) {
	}
}
