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
package org.eclipse.team.tests.ccvs.core;

import java.util.List;
import java.util.Map;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.IUserAuthenticator;
import org.eclipse.team.internal.ccvs.core.IUserInfo;

/**
 * A test authenticator that provide defaults for all methods.
 */
public class TestsUserAuthenticator implements IUserAuthenticator {
	@Override
	public void promptForUserInfo(ICVSRepositoryLocation location, IUserInfo userInfo, String message) throws CVSException {
	}

	@Override
	public int prompt(ICVSRepositoryLocation location, int promptType, String title, String message, int[] promptResponses, int defaultResponseIndex) {
		return defaultResponseIndex;
	}

	@Override
	public String[] promptForKeyboradInteractive(ICVSRepositoryLocation location, String destination, String name, String instruction, String[] prompt, boolean[] echo) throws CVSException {
		return prompt;
	}

	@Override
	public boolean promptForHostKeyChange(ICVSRepositoryLocation location) {
		return false;
	}

	@Override
	public Map<ICVSRepositoryLocation, List<String>> promptToConfigureRepositoryLocations(Map<ICVSRepositoryLocation, List<String>> alternativeMap) {
		return null;
	}
}
