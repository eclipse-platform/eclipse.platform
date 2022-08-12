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
package org.eclipse.core.tests.internal.watson;

import java.io.*;
import org.eclipse.core.internal.watson.*;
import org.eclipse.core.runtime.IPath;
import org.junit.Before;
import org.junit.Test;

public class DeltaChainFlatteningTest extends ElementTreeSerializationTest {
	protected ElementTree[] fDeltaChain;
	protected ElementTree[] fRefriedDeltaChain;

	/**
	 * doRead method comment.
	 */
	@Override
	public Object doRead(ElementTreeReader reader, DataInputStream input) throws IOException {
		return reader.readDeltaChain(input);
	}

	/**
	 * Runs a test for this class at a certain depth and path
	 */
	@Override
	public void doTest(IPath path, int depth) {
		fSubtreePath = path;
		fDepth = depth;
		fDeltaChain = TestUtil.doRoutineOperations(fTree, project1);
		TestUtil.scramble(fDeltaChain);

		ElementTree[] refried = (ElementTree[]) doPipeTest();
		for (int j = 0; j < refried.length; j++) {
			TestUtil.assertEqualTrees("Same after delta chain serialize", fDeltaChain[j], refried[j], fSubtreePath, fDepth);
		}
	}

	/**
	 * doWrite method comment.
	 */
	@Override
	public void doWrite(ElementTreeWriter writer, DataOutputStream output) throws IOException {
		writer.writeDeltaChain(fDeltaChain, fSubtreePath, fDepth, output, DefaultElementComparator.getComparator());
	}

	/**
	 * Sets up the delta chain to be serialized
	 */
	@Override
	@Before
	public void setUp() throws Exception {
		super.setUp();
		fTree = TestUtil.createTestElementTree();
		/* do a bunch of operations on fTree to build a delta chain */
		fDeltaChain = TestUtil.doManyRoutineOperations(fTree, project1);
	}

	/**
	 * Tests the reading and writing of element deltas
	 */
	@Test
	public void test0() {
		doExhaustiveTests();
	}
}
