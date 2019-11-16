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
 *     Atsuhiko Yamanaka, JCraft,Inc. - initial API and implementation.
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ssh2;

import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IConnectionMethod;
import org.eclipse.team.internal.ccvs.core.IServerConnection;

public class PServerSSH2Method implements IConnectionMethod {
	@Override
	public String getName() {
		return "pserverssh2"; //$NON-NLS-1$
	}
	@Override
	public IServerConnection createConnection(ICVSRepositoryLocation root, String password) {
		return new PServerSSH2ServerConnection(root, password);
	}
	@Override
	public void disconnect(ICVSRepositoryLocation location) {
	}
}
