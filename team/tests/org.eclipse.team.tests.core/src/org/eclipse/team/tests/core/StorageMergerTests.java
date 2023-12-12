/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import static org.junit.Assert.assertNotNull;

import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.junit.Test;

public class StorageMergerTests {

	@Test
	public void testGetByExtension() {
		IStorageMerger merger = Team.createMerger("blah");
		assertNotNull("Merger for extension is missing", merger);
	}

	@Test
	public void testGetByContentType() {
		IStorageMerger merger = Team.createMerger(
				Platform.getContentTypeManager().getContentType("org.eclipse.team.tests.core.content-type1"));
		assertNotNull("Merger for extension is missing", merger);
	}

}
