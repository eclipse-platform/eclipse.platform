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
 *     CSC - Intial implementation
 *     IBM Corporation - ongoing maintenance
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.core.client;


/**
 * 
 * The editors command
 *
 *  @author <a href="mailto:kohlwes@gmx.net">Gregor Kohlwes</a>
 */
public class Editors extends AbstractMessageCommand {

	@Override
	protected String getRequestId() {
		return "editors"; //$NON-NLS-1$
	}
	
	@Override
	protected boolean isWorkspaceModification() {
		return false;
	}

}
