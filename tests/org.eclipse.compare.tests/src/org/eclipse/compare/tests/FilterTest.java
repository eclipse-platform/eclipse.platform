/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import org.eclipse.compare.internal.CompareResourceFilter;
import org.junit.Assert;
import org.junit.Test;

public class FilterTest {

	CompareResourceFilter fFilter;

	@Test
	public void testFilterFile() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("*.class"); //$NON-NLS-1$
		Assert.assertTrue("file foo.class should be filtered", f.filter("foo.class", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file foo.java shouldn't be filtered", f.filter("foo.java", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFilterDotFile() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters(".cvsignore"); //$NON-NLS-1$
		Assert.assertTrue("file .cvsignore should be filtered", f.filter(".cvsignore", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file foo.cvsignore shouldn't be filtered", f.filter("foo.cvsignore", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testFilterFolder() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("bin/"); //$NON-NLS-1$
		Assert.assertTrue("folder bin should be filtered", f.filter("bin", true, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file bin shouldn't be filtered", f.filter("bin", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testMultiFilter() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("*.class, .cvsignore, bin/, src/"); //$NON-NLS-1$
		Assert.assertTrue("file foo.class should be filtered", f.filter("foo.class", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file foo.java shouldn't be filtered", f.filter("foo.java", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue("file .cvsignore should be filtered", f.filter(".cvsignore", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file foo.cvsignore shouldn't be filtered", f.filter("foo.cvsignore", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue("folder bin should be filtered", f.filter("bin", true, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file bin shouldn't be filtered", f.filter("bin", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertTrue("folder src should be filtered", f.filter("src", true, false)); //$NON-NLS-1$ //$NON-NLS-2$
		Assert.assertFalse("file src shouldn't be filtered", f.filter("src", false, false)); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testVerify() {
		// Assert.assertNull("filters don't verify",
		// Filter.validateResourceFilters("*.class, .cvsignore, bin/"));
		// Assert.assertNotNull("filters shouldn't verify",
		// Filter.validateResourceFilters("bin//"));
	}
}
