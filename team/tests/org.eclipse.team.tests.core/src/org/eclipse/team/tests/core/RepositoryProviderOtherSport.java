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
package org.eclipse.team.tests.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.RepositoryProvider;

public class RepositoryProviderOtherSport extends RepositoryProvider {

	final public static String NATURE_ID = "org.eclipse.team.tests.core.other";

	@Override
	public void configureProject() throws CoreException {
	}

	@Override
	public String getID() {
		return NATURE_ID;
	}

	@Override
	public void deconfigure() throws CoreException {
	}
}
