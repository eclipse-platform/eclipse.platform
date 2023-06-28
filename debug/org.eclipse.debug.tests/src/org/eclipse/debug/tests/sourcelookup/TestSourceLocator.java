/*******************************************************************************
 * Copyright (c) Jul 30, 2014 IBM Corporation and others.
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
package org.eclipse.debug.tests.sourcelookup;

import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.model.IStackFrame;

/**
 * Test source locator
 */
public class TestSourceLocator implements ISourceLocator {

	@Override
	public Object getSourceElement(IStackFrame stackFrame) {
		return stackFrame.getModelIdentifier() + System.currentTimeMillis();
	}

}
