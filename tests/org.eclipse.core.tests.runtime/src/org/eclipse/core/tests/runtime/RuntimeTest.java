/*******************************************************************************
 * Copyright (c) 2000, 2012 IBM Corporation and others.
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
package org.eclipse.core.tests.runtime;

import org.eclipse.core.tests.harness.CoreTest;
import org.osgi.framework.BundleContext;

/**
 * Common superclass for all runtime tests.
 */
public abstract class RuntimeTest extends CoreTest {
	public static final String PI_RUNTIME_TESTS = RuntimeTestsPlugin.PI_RUNTIME_TESTS;

	/**
	 * Constructor required by test framework.
	 */
	public RuntimeTest(String name) {
		super(name);
	}

	/**
	 * Constructor required by test framework.
	 */
	public RuntimeTest() {
		super();
	}

	public BundleContext getContext() {
		return RuntimeTestsPlugin.getContext();
	}

}
