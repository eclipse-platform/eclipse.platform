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
 
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.IServerConnection;
public class PServerConnectionMethod implements IConnectionMethod {
	@Override
	public IServerConnection createConnection(ICVSRepositoryLocation location, String password) {
		if(password==null){
			password=""; //$NON-NLS-1$
		}
		return new PServerConnection(location, password);
	}
	@Override
	public String getName() {
		return "pserver";//$NON-NLS-1$
	}
	
	@Override
	public void disconnect(ICVSRepositoryLocation location) {
	}
}
