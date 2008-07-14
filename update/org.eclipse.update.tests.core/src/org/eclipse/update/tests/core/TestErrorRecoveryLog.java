/*******************************************************************************
 * Copyright (c) 2007 The Eclipse Foundation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *    The Eclipse Foundation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.tests.core;

import java.io.File;

import org.eclipse.update.internal.core.ErrorRecoveryLog;
import org.eclipse.update.tests.UpdateManagerTestCase;

public class TestErrorRecoveryLog extends UpdateManagerTestCase {
	
	public TestErrorRecoveryLog(String name) {
		super(name);
		// TODO Auto-generated constructor stub
	}

	public void testGetLocalRandomIdentifier() throws Exception {
		for (int i = 0; i < 10; i++) {
			String fname = ErrorRecoveryLog.getLocalRandomIdentifier(System.getProperty("java.io.tmpdir") + "/feature.xml");
			File f = new File(fname);
			assertTrue(f.createNewFile());
		}
	}
}
