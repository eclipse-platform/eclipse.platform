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

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.tests.resources.ResourceTest;

//NOTE: Should not hook this test up until the corresponding bug is fixed.
public class Bug_079398 extends ResourceTest {

	public Bug_079398(String name) {
		super(name);
	}

	public void testBug79398() {
		IProject project = getWorkspace().getRoot().getProject("myproject");
		IFile file1 = project.getFile("myFile.txt");
		IFile file2 = project.getFile("copyOfMyFile.txt");

		/* set local history policies */
		// keep original
		IWorkspaceDescription originalDescription = getWorkspace().getDescription();
		// get another copy for changes
		IWorkspaceDescription description = getWorkspace().getDescription();
		// longevity set to 1 day
		description.setFileStateLongevity(1000 * 3600 * 24);
		// keep a max of 4 file states
		description.setMaxFileStates(4);
		// max size of file = 1 Mb
		description.setMaxFileStateSize(1024 * 1024);
		try {
			getWorkspace().setDescription(description);
		} catch (CoreException e) {
			fail("0.1", e);
		}
		try {
			ensureExistsInWorkspace(file1, getRandomContents());
			try {
				for (int i = 0; i < 10; i++) {
					file1.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
				}
			} catch (CoreException e) {
				fail("0.2", e);
			}

			IFileState[] sourceStates = null;
			try {
				sourceStates = file1.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("0.3", e);
			}
			// just make sure our assumptions are valid
			assertEquals("0.4", 10, sourceStates.length);

			// copy the file - the history should be shared, but the destination
			// will conform to the policy
			try {
				file1.copy(file2.getFullPath(), true, getMonitor());
			} catch (CoreException e) {
				fail("0.4", e);
			}

			assertExistsInWorkspace("1.0", file2);
			try {
				sourceStates = file1.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("1.1", e);
			}
			// the source is unaffected so far
			assertEquals("1.2", 10, sourceStates.length);
			IFileState[] destinationStates = null;
			try {
				destinationStates = file2.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("1.3", e);
			}
			// but the destination conforms to the policy
			assertEquals("1.4", description.getMaxFileStates(), destinationStates.length);

			// now cause the destination to have many more states
			try {
				for (int i = 0; i <= description.getMaxFileStates(); i++) {
					file2.setContents(getRandomContents(), IResource.FORCE | IResource.KEEP_HISTORY, getMonitor());
				}
			} catch (CoreException e) {
				fail("1.5", e);
			}
			IHistoryStore history = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
			// clean history
			history.clean(getMonitor());

			try {
				destinationStates = file2.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("1.6", e);
			}
			// cleaning will remove any states the destination had in common
			// with the source since they don't fit into the policy
			assertEquals("1.7", description.getMaxFileStates(), destinationStates.length);

			try {
				sourceStates = file1.getHistory(getMonitor());
			} catch (CoreException e) {
				fail("1.8", e);
			}
			// the source should have any extra states removed as well,
			// but the ones left should still exist
			assertEquals("1.7", description.getMaxFileStates(), sourceStates.length);
			for (int i = 0; i < sourceStates.length; i++) {
				assertTrue("1.8." + i, sourceStates[i].exists());
			}
		} finally {
			// restore the original description
			try {
				getWorkspace().setDescription(originalDescription);
			} catch (CoreException e) {
				// only log since we don't want to override with any test failure
				log(ResourceTest.PI_RESOURCES_TESTS, e);
			}
		}
	}

}
