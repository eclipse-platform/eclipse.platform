/*******************************************************************************
 * Copyright (c) 2006, 2008 IBM Corporation and others.
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
package org.eclipse.core.internal.expressions.propertytester;

import org.osgi.framework.Bundle;

import org.eclipse.core.expressions.PropertyTester;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;

/**
 * A property tester for testing platform properties. Can test whether or
 * not a given bundle is installed in the running environment, as well as
 * the id of the currently active product.
 * <p>
 * For example:
 * </p>
 * <pre>
 * &lt;test property="org.eclipse.core.runtime.product" value="org.eclipse.sdk.ide"/&gt;
 * &lt;test property="org.eclipse.core.runtime.isBundleInstalled" args="org.eclipse.core.expressions"/&gt;
 * &lt;test property="org.eclipse.core.runtime.bundleState" args="org.eclipse.core.expressions" value="ACTIVE"/&gt;
 * </pre>
 */
public class PlatformPropertyTester extends PropertyTester {

	private static final String PROPERTY_PRODUCT = "product"; //$NON-NLS-1$
	private static final String PROPERTY_IS_BUNDLE_INSTALLED = "isBundleInstalled"; //$NON-NLS-1$
	private static final String PROPERTY_BUNDLE_STATE = "bundleState"; //$NON-NLS-1$

	@Override
	public boolean test(Object receiver, String property, Object[] args, Object expectedValue) {
		if (Platform.class.equals(receiver)) {
			if (PROPERTY_PRODUCT.equals(property)) {
				IProduct product= Platform.getProduct();
				if (product != null) {
					return product.getId().equals(expectedValue);
				}
				return false;
			} else if (PROPERTY_IS_BUNDLE_INSTALLED.equals(property) && args.length >= 1 && args[0] instanceof String) {
				return Platform.getBundle((String)args[0]) != null;
			} else if (PROPERTY_BUNDLE_STATE.equals(property)
					&& args.length >= 1 && args[0] instanceof String) {
				Bundle b = Platform.getBundle((String) args[0]);
				if (b != null) {
					return bundleState(b.getState(), expectedValue);
				}
				return false;
			}
		}
		return false;
	}

	private boolean bundleState(int bundleState, Object expectedValue) {
		if ("UNINSTALLED".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.UNINSTALLED;
		}
		if ("INSTALLED".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.INSTALLED;
		}
		if ("RESOLVED".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.RESOLVED;
		}
		if ("STARTING".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.STARTING;
		}
		if ("STOPPING".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.STOPPING;
		}
		if ("ACTIVE".equals(expectedValue)) { //$NON-NLS-1$
			return bundleState == Bundle.ACTIVE;
		}
		return false;
	}
}
