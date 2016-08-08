/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
