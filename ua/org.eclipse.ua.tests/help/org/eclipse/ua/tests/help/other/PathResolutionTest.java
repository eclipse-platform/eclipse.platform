/*******************************************************************************
 * Copyright (c) 2009, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.assertEquals;

import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.internal.util.ProductPreferences;
import org.junit.Test;
import org.osgi.framework.Bundle;

public class PathResolutionTest {
	@Test
	public void testResolveNull() {
		assertEquals(null, ProductPreferences.resolveSpecialIdentifiers(null));
	}

	@Test
	public void testResolveSimplePath() {
		assertEquals("/a.b.c/toc.xml", ProductPreferences.resolveSpecialIdentifiers("/a.b.c/toc.xml"));
	}

	@Test
	public void testResolvePluginsRoot() {
		assertEquals("/a.b.c/toc.xml", ProductPreferences.resolveSpecialIdentifiers("PLUGINS_ROOT/a.b.c/toc.xml"));
	}

	@Test
	public void testResolveSlashPluginsRoot() {
		assertEquals("/a.b.c/toc.xml", ProductPreferences.resolveSpecialIdentifiers("/PLUGINS_ROOT/a.b.c/toc.xml"));
	}

	@Test
	public void testResolveEmbeddedPluginsRoot() {
		assertEquals("/a.b.c/toc.xml", ProductPreferences.resolveSpecialIdentifiers("../PLUGINS_ROOT/a.b.c/toc.xml"));
	}

	@Test
	public void testResolvePluginsRootProductPlugin() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			Bundle productBundle = product.getDefiningBundle();
			if (productBundle != null) {
				String bundleName = productBundle.getSymbolicName();
				assertEquals('/' + bundleName + "/toc.xml", ProductPreferences.resolveSpecialIdentifiers("PLUGINS_ROOT/PRODUCT_PLUGIN/toc.xml"));
			}
		}
	}

	@Test
	public void testResolveProductPlugin() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			Bundle productBundle = product.getDefiningBundle();
			if (productBundle != null) {
				String bundleName = productBundle.getSymbolicName();
				assertEquals('/' + bundleName + "/toc.xml", ProductPreferences.resolveSpecialIdentifiers("PRODUCT_PLUGIN/toc.xml"));
			}
		}
	}

	@Test
	public void testResolveSlashProductPlugin() {
		IProduct product = Platform.getProduct();
		if (product != null) {
			Bundle productBundle = product.getDefiningBundle();
			if (productBundle != null) {
				String bundleName = productBundle.getSymbolicName();
				assertEquals('/' + bundleName + "/toc.xml", ProductPreferences.resolveSpecialIdentifiers("/PRODUCT_PLUGIN/toc.xml"));
			}
		}
	}


}
