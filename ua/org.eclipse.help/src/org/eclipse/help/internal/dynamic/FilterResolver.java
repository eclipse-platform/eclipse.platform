/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
package org.eclipse.help.internal.dynamic;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.osgi.framework.Bundle;

/*
 * Resolves filters by determining whether the element in question should
 * or shouldn't be filtered.
 */
public class FilterResolver {

	private static final String NAME_OS = "os"; //$NON-NLS-1$
	private static final String NAME_WS = "ws"; //$NON-NLS-1$
	private static final String NAME_ARCH = "arch"; //$NON-NLS-1$
	private static final String NAME_PRODUCT = "product"; //$NON-NLS-1$
	private static final String NAME_PLUGIN = "plugin"; //$NON-NLS-1$

	private static Extension extension;
	private static FilterResolver instance;

	public static FilterResolver getInstance() {
		if (instance == null) {
			instance = new FilterResolver();
		}
		return instance;
	}

	/*
	 * Returns whether or not the given filter expression gets rejected by the
	 * filter. e.g. "os=win32", "ws!=gtk"
	 */
	public boolean isFiltered(String expression) {
		String name = null;
		String value = null;
		boolean not = false;
		int index = expression.indexOf("!="); //$NON-NLS-1$
		if (index != -1) {
			name = expression.substring(0, index).trim();
			value = expression.substring(index + 2).trim();
			not = true;
		}
		else {
			index = expression.indexOf('=');
			if (index != -1) {
				name = expression.substring(0, index).trim();
				value = expression.substring(index + 1).trim();
				not = false;
			}
		}
		if (name != null && name.length() > 0 && value != null && value.length() > 0) {
			return isFiltered(name, value, not);
		}
		// don't apply any invalid filters
		return false;
	}

	/*
	 * Returns whether or not the given filter name and value get rejected by
	 * the filter.
	 */
	public boolean isFiltered(String name, String value, boolean not) {
		boolean filtered;
		if (name.equals(NAME_OS)) {
			filtered = filterByWS(value);
		}
		else if (name.equals(NAME_WS)) {
			filtered = filterByOS(value);
		}
		else if (name.equals(NAME_ARCH)) {
			filtered = filterByARCH(value);
		}
		else if (name.equals(NAME_PRODUCT)) {
			filtered = filterByProduct(value);
		}
		else if (name.equals(NAME_PLUGIN)) {
			filtered = filterByPlugin(value);
		}
		else if (extension != null && extension.isHandled(name)) {
			filtered = extension.isFiltered(name, value);
		}
		else {
			filtered = filterBySystemProperty(name, value);
		}
		return not ? !filtered : filtered;
	}

	/*
	 * Hack: We don't have access to UI classes from here; the activity
	 * and category filters are dropped in from org.eclipse.help.ui when it
	 * starts.
	 */
	public static void setExtension(Extension extension) {
		FilterResolver.extension = extension;
	}

	/*
	 * Evaluates the "ws" filter.
	 */
	private boolean filterByWS(String ws) {
		return !ws.equals(Platform.getWS());
	}

	/*
	 * Evaluates the "os" filter.
	 */
	private boolean filterByOS(String os) {
		return !os.equals(Platform.getOS());
	}

	/*
	 * Evaluates the "arch" filter.
	 */
	private boolean filterByARCH(String arch) {
		return !arch.equals(Platform.getOSArch());
	}

	/*
	 * Evaluates the "product" filter.
	 */
	private boolean filterByProduct(String productId) {
		IProduct product = Platform.getProduct();
		if (product != null) {
			return !productId.equals(product.getId());
		}
		return true;
	}

	/*
	 * Evaluates the "plugin" filter.
	 */
	private boolean filterByPlugin(String bundleId) {
		Bundle bundle = Platform.getBundle(bundleId);
		return bundle == null;
	}

	/*
	 * Evaluates the system property filter (when filter name doesn't match
	 * any known filter).
	 */
	private boolean filterBySystemProperty(String property, String value) {
		try {
			String systemValue = System.getProperty(property);
			if (systemValue != null) {
				return !value.equals(systemValue);
			}
		}
		catch (Throwable t) {
			// skip
		}
		return true;
	}

	/*
	 * Hack: A way for the org.eclipse.help.ui plugin to extend the filtering
	 * capability with UI-related filters (this is a core plugin).
	 */
	public static interface Extension {

		/*
		 * Returns whether or not this extension handles the given filter.
		 */
		public boolean isHandled(String name);

		/*
		 * Returns whether the given filter is rejected or not.
		 */
		public boolean isFiltered(String name, String value);
	}
}
