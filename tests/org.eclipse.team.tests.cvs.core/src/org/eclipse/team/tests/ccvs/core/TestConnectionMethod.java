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
package org.eclipse.team.tests.ccvs.core;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.IServerConnection;

/**
 * @author Administrator
 *
 * To change this generated comment go to 
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class TestConnectionMethod implements IConnectionMethod {
	@Override
	public String getName() {
		return "test";
	}

	@Override
	public IServerConnection createConnection(ICVSRepositoryLocation location, String password) {
		return TestConnection.createConnection(location, password);
	}

	@Override
	public void disconnect(ICVSRepositoryLocation location) {
		// Nothing need to be done
	}
}
