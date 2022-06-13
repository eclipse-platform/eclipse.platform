/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions.tests;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.core.runtime.Assert;

public class A_TypeExtender2 extends PropertyTester {

	@Override
	public boolean test(Object receiver, String method, Object[] args, Object expectedValue) {
		if ("chaining".equals(method)) { //$NON-NLS-1$
			return "A2".equals(expectedValue); //$NON-NLS-1$
		} else if ("chainOrdering".equals(method)) { //$NON-NLS-1$
			return "A2".equals(expectedValue); //$NON-NLS-1$
		}
		Assert.isTrue(false);
		return false;
	}

}
