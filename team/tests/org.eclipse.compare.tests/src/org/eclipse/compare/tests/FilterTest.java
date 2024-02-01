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

import static org.assertj.core.api.Assertions.assertThat;

import org.eclipse.compare.internal.CompareResourceFilter;
import org.junit.Test;

public class FilterTest {

	CompareResourceFilter fFilter;

	@Test
	public void testFilterFile() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("*.class");
		assertThat(f.filter("foo.class", false, false)).as("file foo.class should be filtered").isTrue();
		assertThat(f.filter("foo.java", false, false)).as("file foo.java should not be filtered").isFalse();
	}

	@Test
	public void testFilterDotFile() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters(".cvsignore");
		assertThat(f.filter(".cvsignore", false, false)).as("file .cvsignore should be filtered").isTrue();
		assertThat(f.filter("foo.cvsignore", false, false)).as("file foo.cvsignore should not be filtered").isFalse();
	}

	@Test
	public void testFilterFolder() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("bin/");
		assertThat(f.filter("bin", true, false)).as("folder bin should be filtered").isTrue();
		assertThat(f.filter("bin", false, false)).as("file bin should not be filtered").isFalse();
	}

	@Test
	public void testMultiFilter() {
		CompareResourceFilter f = new CompareResourceFilter();
		f.setFilters("*.class, .cvsignore, bin/, src/");
		assertThat(f.filter("foo.class", false, false)).as("file foo.class should be filtered").isTrue();
		assertThat(f.filter("foo.java", false, false)).as("file foo.java should not be filtered").isFalse();
		assertThat(f.filter(".cvsignore", false, false)).as("file .cvsignore should be filtered").isTrue();
		assertThat(f.filter("foo.cvsignore", false, false)).as("file foo.cvsignore should not be filtered").isFalse();
		assertThat(f.filter("bin", true, false)).as("folder bin should be filtered").isTrue();
		assertThat(f.filter("bin", false, false)).as("file bin should not be filtered").isFalse();
		assertThat(f.filter("src", true, false)).as("folder src should be filtered").isTrue();
		assertThat(f.filter("src", false, false)).as("file src should not be filtered").isFalse();
	}

	@Test
	public void testVerify() {
		assertThat(CompareResourceFilter.validateResourceFilters("*.class, .cvsignore, bin/"))
				.as("filters don't verify").isNull();
		assertThat(CompareResourceFilter.validateResourceFilters("bin//")).as("filters shouldn't verify").isNotNull();
	}
}
