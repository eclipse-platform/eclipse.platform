/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.ui.internal.net.auth;

import java.net.*;

public class NetAuthenticator extends Authenticator {

	@Override
	protected PasswordAuthentication getPasswordAuthentication() {
		// String protocol = getRequestingProtocol();
		InetAddress address = getRequestingSite(); // can be null;
		// int port = getRequestingPort();
		String prompt = getRequestingPrompt(); // realm or message, not documented that can be null
		// String scheme = getRequestingScheme(); // not documented that can be null

		 // get the host name from the address since #getRequestingHost
		 // is not available in the foundation 1.0 class libraries
		String hostString = null;
		if (address != null) {
			hostString = address.getHostName();
		}
		if (hostString == null) {
			hostString = ""; //$NON-NLS-1$
		}
		String promptString = prompt;
		if (prompt == null) {
			promptString = ""; //$NON-NLS-1$
		}

		Authentication auth = UserValidationDialog.getAuthentication(
				hostString, promptString);
		if (auth != null)
			return new PasswordAuthentication(auth.getUser(), auth
					.getPassword().toCharArray());
		else
			return null;
	}
}
