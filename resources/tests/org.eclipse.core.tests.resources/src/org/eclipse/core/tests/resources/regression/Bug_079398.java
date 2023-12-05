/*******************************************************************************
 *  Copyright (c) 2004, 2012 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.tests.resources.ResourceTest;

//NOTE: Should not hook this test up until the corresponding bug is fixed.
public class Bug_079398 extends ResourceTest {

	public Bug_079398(String name) {
		super(name);
	}

	public void testBug79398() throws Exception {
		IProject project = getWorkspace().getRoot().getProject("myproject");
		IFile file1 = project.getFile("myFile.txt");
		IFile file2 = project.getFile("copyOfMyFile.txt");

		/* set local history policies */
		IWorkspaceDescription description = getWorkspace().getDescription();
		// longevity set to 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// keep a max of 4 file states
		description.setMaxFileStates(4);
		// max size of file = 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		getWorkspace().setDescription(description);
		ensureExistsInWorkspace(file1, getRandomString());
		for (int i = 0; i < 10; i++) {
			file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		}

		IFileState[] sourceStates = file1.getHistory(createTestMonitor());
		// just make sure our assumptions are valid
		assertEquals("0.4", 10, sourceStates.length);

		// copy the file - the history should be shared, but the destination
		// will conform to the policy
		file1.copy(file2.getFullPath(), true, createTestMonitor());

		assertExistsInWorkspace(file2);
		sourceStates = file1.getHistory(createTestMonitor());
		// the source is unaffected so far
		assertEquals("1.2", 10, sourceStates.length);
		IFileState[] destinationStates = file2.getHistory(createTestMonitor());
		// but the destination conforms to the policy
		assertEquals("1.4", description.getMaxFileStates(), destinationStates.length);

		// now cause the destination to have many more states
		for (int i = 0; i <= description.getMaxFileStates(); i++) {
			file2.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		}
		IHistoryStore history = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
		// clean history
		history.clean(createTestMonitor());

		destinationStates = file2.getHistory(createTestMonitor());
		// cleaning will remove any states the destination had in common
		// with the source since they don't fit into the policy
		assertEquals("1.7", description.getMaxFileStates(), destinationStates.length);

		sourceStates = file1.getHistory(createTestMonitor());
		// the source should have any extra states removed as well,
		// but the ones left should still exist
		assertEquals("1.7", description.getMaxFileStates(), sourceStates.length);
		for (int i = 0; i < sourceStates.length; i++) {
			assertTrue("1.8." + i, sourceStates[i].exists());
		}
	}

}
