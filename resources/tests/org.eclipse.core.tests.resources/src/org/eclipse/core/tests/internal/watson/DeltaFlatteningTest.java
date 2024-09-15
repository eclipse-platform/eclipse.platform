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
import org.eclipse.core.internal.watson.ElementTreeWriter;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamReader;
import org.eclipse.core.tests.internal.watson.ElementTreeSerializationTestHelper.StreamWriter;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ArgumentsSource;

public class DeltaFlatteningTest implements IPathConstants {

	private ElementTree prepareTreeForChange() {
		ElementTree tree = TestUtil.createTestElementTree();
		/**
		 * The following changes will be made to the base tree:
		 *	- add project3
		 *  - add folder5 below project3
		 *  - delete file1
		 *  - change data of folder2
		 *	- add file4 below project2
		 *  - add file5 below folder1
		 *  - delete folder3
		 */

		ElementTree newTree = tree.newEmptyDelta();

		IPath project3 = solution.append("project3");
		IPath folder5 = project3.append("folder5");
		IPath file4 = project2.append("file4");
		IPath file5 = folder1.append("file5");

		newTree.createElement(project3, "project3");
		newTree.createElement(folder5, "folder5");
		newTree.deleteElement(file1);
		newTree.createElement(folder2, "ChangedData");
		newTree.createElement(file4, "file4");
		newTree.createElement(file5, "file5");
		newTree.deleteElement(folder3);
		newTree.immutable();

		/* assert the new structure */
		TestUtil.assertHasPaths(newTree, new IPath[] { solution, project1, project2, project3, file2, file4, file5,
				folder1, folder2, folder4, folder5 });
		TestUtil.assertNoPaths(newTree, new IPath[] { file1, file3, folder3 });

		return newTree;
	}

	/**
	 * Tests the reading and writing of element deltas
	 */
	@ParameterizedTest
	@ArgumentsSource(ElementTreeSerializationTestHelper.class)
	public void test0(IPath path, int depth) throws IOException {
		ElementTree tree = TestUtil.createTestElementTree();
		IPath testTreeRootPath = solution;
		ElementTree treeForChange = prepareTreeForChange();

		StreamReader streamReader = (reader, input) -> reader.readDelta(treeForChange, input);
		StreamWriter streamWriter = (writer, output) -> writer.writeDelta(tree, treeForChange, testTreeRootPath,
				ElementTreeWriter.D_INFINITE, output, DefaultElementComparator.getComparator());
		ElementTree newTree = (ElementTree) doPipeTest(streamWriter, streamReader);

		TestUtil.assertEqualTrees(this.getClass() + "test0", tree, newTree, path, depth);
	}
}
