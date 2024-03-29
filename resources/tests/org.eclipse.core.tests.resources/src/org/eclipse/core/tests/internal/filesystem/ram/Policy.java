/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.ram;

import org.eclipse.core.runtime.*;

public class Policy {

	public static void error(String message) throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", 1, message, null));
	}

	private Policy() {
		super();
	}

}
