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
package org.eclipse.team.tests.ccvs.ui;

import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.ui.operations.CVSOperation;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public abstract class CVSOperationTest extends EclipseTest {
	
	protected CVSOperationTest() {
		super();
	}

	protected CVSOperationTest(String name) {
		super(name);
	}

	protected void run(CVSOperation op) throws CVSException {
		executeHeadless(op);
	}
}
