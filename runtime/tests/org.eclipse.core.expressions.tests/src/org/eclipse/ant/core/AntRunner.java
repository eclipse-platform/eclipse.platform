/*******************************************************************************
 * Copyright (c) 2024 IBM Corporation and others.
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
package org.eclipse.ant.core;

import java.io.Serializable;

/**
 * Used by the
 * {@link org.eclipse.core.internal.expressions.tests.ExpressionTestsPluginUnloading}
 * test.
 * <p>
 * <strong>Note:</strong> This class uses the 'org.eclipse.ant.core' namespace
 * for test purposes only and does not copy or implement anything from the real
 * AntRunner class.
 * </p>
 */
public class AntRunner implements Runnable, Serializable {
	private static final long serialVersionUID = 1L;

	@Override
	public void run() {
		// Empty implementation for testing
	}
}
