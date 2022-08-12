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

/**
 * This class is used to text resource linking
 */
public class RepositoryProviderWithLinking extends RepositoryProvider {

	final public static String TYPE_ID = "org.eclipse.team.tests.core.linking";

	private static boolean canHandleLinking = false;

	private static boolean canHandleLinkedURI;

	@Override
	public void configureProject() throws CoreException {
	}
	@Override
	public String getID() {
		return TYPE_ID;
	}
	@Override
	public void deconfigure() throws CoreException {
	}

	public static void setCanHandleLinking(boolean canHandleLinking) {
		RepositoryProviderWithLinking.canHandleLinking = canHandleLinking;
	}

	@Override
	public boolean canHandleLinkedResources() {
		return canHandleLinking;
	}

	public static void setCanHandleLinkedURI(boolean canHandleLinkedURI) {
		RepositoryProviderWithLinking.canHandleLinkedURI = canHandleLinkedURI;
	}

	@Override
	public boolean canHandleLinkedResourceURI() {
		return RepositoryProviderWithLinking.canHandleLinkedURI;
	}

}
