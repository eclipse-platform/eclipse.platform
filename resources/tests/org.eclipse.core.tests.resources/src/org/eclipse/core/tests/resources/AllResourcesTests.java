/*******************************************************************************
 *  Copyright (c) 2000, 2014 IBM Corporation and others.
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
 *     Ingo Mohr - Issue #166 - Add Preference to Turn Off Warning-Check for Project Specific Encoding
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

@RunWith(Suite.class)
@Suite.SuiteClasses({ CharsetTest.class, ContentDescriptionManagerTest.class, FilteredResourceTest.class,
		HiddenResourceTest.class, VirtualFolderTest.class, IFileTest.class, IFolderTest.class,
		IPathVariableTest.class,
		IProjectDescriptionTest.class, IProjectTest.class, IResourceChangeEventTest.class,
		IResourceChangeListenerTest.class, IResourceDeltaTest.class, IResourceTest.class, ISynchronizerTest.class,
		IWorkspaceRootTest.class, IWorkspaceTest.class, LinkedResourceTest.class,
		LinkedResourceWithPathVariableTest.class, LinkedResourceSyncMoveAndCopyTest.class, MarkerSetTest.class,
		MarkerTest.class, NatureTest.class, NonLocalLinkedResourceTest.class, ProjectEncodingTest.class,
		ProjectOrderTest.class,
		ProjectScopeTest.class, ProjectSnapshotTest.class, ResourceAttributeTest.class, ResourceURLTest.class,
		TeamPrivateMemberTest.class, WorkspaceTest.class })
public class AllResourcesTests {

}
