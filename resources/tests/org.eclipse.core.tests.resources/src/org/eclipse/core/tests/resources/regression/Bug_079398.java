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

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import org.eclipse.core.internal.localstore.IHistoryStore;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

public class Bug_079398 {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	@Test
	@Ignore("Bug 78398 needs to be fixed")
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
		createInWorkspace(file1, createRandomString());
		for (int i = 0; i < 10; i++) {
			file1.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		}

		IFileState[] sourceStates = file1.getHistory(createTestMonitor());
		// just make sure our assumptions are valid
		assertThat(sourceStates).hasSize(10);

		// copy the file - the history should be shared, but the destination
		// will conform to the policy
		file1.copy(file2.getFullPath(), true, createTestMonitor());

		assertExistsInWorkspace(file2);
		sourceStates = file1.getHistory(createTestMonitor());
		// the source is unaffected so far
		assertThat(sourceStates).hasSize(10);
		IFileState[] destinationStates = file2.getHistory(createTestMonitor());
		// but the destination conforms to the policy
		assertThat(destinationStates).hasSize(description.getMaxFileStates());

		// now cause the destination to have many more states
		for (int i = 0; i <= description.getMaxFileStates(); i++) {
			file2.setContents(createRandomContentsStream(), IResource.FORCE | IResource.KEEP_HISTORY, createTestMonitor());
		}
		IHistoryStore history = ((Workspace) getWorkspace()).getFileSystemManager().getHistoryStore();
		// clean history
		history.clean(createTestMonitor());

		destinationStates = file2.getHistory(createTestMonitor());
		// cleaning will remove any states the destination had in common
		// with the source since they don't fit into the policy
		assertThat(destinationStates).hasSize(description.getMaxFileStates());

		sourceStates = file1.getHistory(createTestMonitor());
		// the source should have any extra states removed as well,
		// but the ones left should still exist
		assertThat(sourceStates).hasSize(description.getMaxFileStates()).allMatch(IFileState::exists);
	}

}
