/*******************************************************************************
 * Copyright (c) 2005, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.core;

import junit.framework.Test;

import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.mapping.IStorageMerger;

public class StorageMergerTests extends TeamTest {

	public StorageMergerTests() {
		super();
	}

	public StorageMergerTests(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(StorageMergerTests.class);
	}

	public void testGetByExtension() {
		IStorageMerger merger = new Team().createStorageMerger("blah");
		assertNotNull("Merger for extension is missing", merger);
	}

	public void testGetByContentType() {
		IStorageMerger merger = new Team().createStorageMerger(Platform.getContentTypeManager().getContentType("org.eclipse.team.tests.core.content-type1"));
		assertNotNull("Merger for extension is missing", merger);
	}
}
