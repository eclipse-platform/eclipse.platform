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

public class A_TypeExtender extends PropertyTester {

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (property == null) {
			return false;
		}

		switch (property) {
		case "simple": //$NON-NLS-1$
			return "simple".equals(expectedValue); //$NON-NLS-1$
		case "overridden": //$NON-NLS-1$
			return "A".equals(expectedValue); //$NON-NLS-1$
		case "ordering": //$NON-NLS-1$
			return "A".equals(expectedValue); //$NON-NLS-1$
		case "chainOrdering": //$NON-NLS-1$
			return "A".equals(expectedValue); //$NON-NLS-1$
		default:
			break;
		}
		Assert.isTrue(false);
		return false;
	}
}
