/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
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

package org.eclipse.core.internal.propertytester;

import org.eclipse.core.resources.IProject;

/**
 * A property tester for various properties of projects.
 *
 * @since 3.2
 */
public class ProjectPropertyTester extends ResourcePropertyTester {

	/**
	 * A property indicating whether the project is open (value <code>"open"</code>).
	 */
	private static final String OPEN = "open"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if ((receiver instanceof IProject) && method.equals(OPEN))
			return ((IProject) receiver).isOpen() == toBoolean(expectedValue);
		return false;
	}
}
