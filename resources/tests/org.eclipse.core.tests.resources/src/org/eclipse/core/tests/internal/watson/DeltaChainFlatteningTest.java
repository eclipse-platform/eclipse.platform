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

import static org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.doPipeTest;

import java.io.IOException;
import org.eclipse.core.internal.watson.DefaultElementComparator;
import org.eclipse.core.internal.watson.ElementTree;
import org.eclipse.core.internal.watson.ElementTreeReader;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamReader;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamWriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class DeltaChainFlatteningTest implements IPathConstants {
	/**
	 * Tests the reading and writing of element deltas
	 */
	@ParameterizedTest
	@ArgumentsSource(ElementTreeSerializationTestHelper.class)
	public void test0(IPath path, int depth) throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		ElementTree[] fDeltaChain = TestUtil.doRoutineOperations(tree, project1);
		TestUtil.scramble(fDeltaChain);

		StreamWriter streamWriter = (writer, output) -> writer.writeDeltaChain(fDeltaChain, path, depth, output,
				DefaultElementComparator.getComparator());
		StreamReader streamReader = ElementTreeReader::readDeltaChain;
		ElementTree[] refried = (ElementTree[]) doPipeTest(streamWriter, streamReader);

		for (int j = 0; j < refried.length; j++) {
			TestUtil.assertEqualTrees("Same after delta chain serialize", fDeltaChain[j], refried[j], path, depth);
		}
	}
}
