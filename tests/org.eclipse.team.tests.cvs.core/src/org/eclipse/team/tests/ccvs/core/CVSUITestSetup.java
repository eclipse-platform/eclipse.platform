/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
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

import junit.framework.Test;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.ui.PlatformUI;

public class CVSUITestSetup extends CVSTestSetup implements Test {

	public CVSUITestSetup(Test test) {
		super(test);
	}
	
	@Override
	public void setUp() throws CoreException {
		super.setUp();
		PlatformUI.getWorkbench().getDecoratorManager().setEnabled(CVSUIPlugin.DECORATOR_ID, true);
	}
}
