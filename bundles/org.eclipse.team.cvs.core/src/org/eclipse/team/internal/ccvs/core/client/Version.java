/*******************************************************************************
 * Copyright (c) 2000, 2007 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.core.client;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.client.listeners.ICommandOutputListener;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;

/**
 * Here are some of the output formats we know about:
 * 
 * Concurrent Versions System (CVS) 1.11.1p1 (client/server)
 * Concurrent Versions System (CVS) NT 1.11.1.1 (Build 27)
 * Concurrent Versions System (CVSNT) 1.11.1.3  (Build 57a) (client/server)
 */
public class Version extends RemoteCommand {

	private static final String CVS_NT_PREFIX_1 = "Concurrent Versions System (CVS) NT "; //$NON-NLS-1$
	private static final String CVS_NT_PREFIX_2 = "Concurrent Versions System (CVSNT) "; //$NON-NLS-1$
	private static final String CVS_PREFIX = "Concurrent Versions System (CVS) "; //$NON-NLS-1$
	@Override
	protected String getRequestId() {
		return "version"; //$NON-NLS-1$
	}

	public IStatus execute(Session session, final ICVSRepositoryLocation location, IProgressMonitor monitor) throws CVSException {
		
		// The server may not support the version request
		if ( ! session.isValidRequest(getRequestId())) {
			IStatus status = new CVSStatus(IStatus.WARNING, CVSStatus.SERVER_IS_UNKNOWN, NLS.bind(CVSMessages.Version_versionNotValidRequest, new String[] { location.getHost() }), session.getLocalRoot());
			((CVSRepositoryLocation)location).setServerPlaform(CVSRepositoryLocation.UNKNOWN_SERVER);
			CVSProviderPlugin.log(status);
			return status;
		}
		
		ICommandOutputListener listener = new ICommandOutputListener() {
			public IStatus messageLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
				String knownPrefix = null;
				boolean isCVSNT = false;
				if (line.startsWith(CVS_NT_PREFIX_1)) {
					isCVSNT = true;
					knownPrefix = CVS_NT_PREFIX_1;
				} else if (line.startsWith(CVS_NT_PREFIX_2)) {
					isCVSNT = true;
					knownPrefix = CVS_NT_PREFIX_2;
				} else if (line.startsWith(CVS_PREFIX)) {
					knownPrefix = CVS_PREFIX;
				}
				IStatus status = OK;
				int serverType = CVSRepositoryLocation.CVS_SERVER;
				if (knownPrefix != null) {
					String versionNumber = line.substring(knownPrefix.length(), line.indexOf(' ', knownPrefix.length() + 1));
					if (versionNumber.startsWith("1.10") || versionNumber.equals("1.11") || versionNumber.equals("1.11.1")) { //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
						serverType = CVSRepositoryLocation.UNSUPPORTED_SERVER;
						status = new CVSStatus(IStatus.WARNING, CVSStatus.UNSUPPORTED_SERVER_VERSION, NLS.bind(CVSMessages.Version_unsupportedVersion, new String[] { location.getHost(), versionNumber }),location);
					} else if (isCVSNT) {
						serverType = CVSRepositoryLocation.CVSNT_SERVER;
					}
				} else {
					serverType = CVSRepositoryLocation.UNKNOWN_SERVER;
					status = new CVSStatus(IStatus.INFO, CVSStatus.SERVER_IS_UNKNOWN, NLS.bind(CVSMessages.Version_unknownVersionFormat, new String[] { location.getHost(), line }), location);
				}
				((CVSRepositoryLocation)location).setServerPlaform(serverType);
				return status;
			}
			public IStatus errorLine(String line, ICVSRepositoryLocation location, ICVSFolder commandRoot, IProgressMonitor monitor) {
				return new CVSStatus(IStatus.ERROR, CVSStatus.ERROR_LINE, line, commandRoot);
			}
		};
		
		return execute(session, NO_GLOBAL_OPTIONS, NO_LOCAL_OPTIONS, new String[] {}, listener, monitor);
	}
	
	@Override
	protected boolean isWorkspaceModification() {
		return false;
	}
	
}
