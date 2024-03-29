/*******************************************************************************
 *  Copyright (c) 2021 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     Mykola Zakharchuk <zakharchuk.vn@gmail.com> - Bug 576169
 *******************************************************************************/
package org.eclipse.core.tests.internal.mapping;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.junit.Assert.assertEquals;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.resources.mapping.ResourceChangeValidator;
import org.eclipse.core.tests.resources.WorkspaceTestRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test to validate project kind and flags on deletion.
 */
public class TestProjectDeletion {

	@Rule
	public WorkspaceTestRule workspaceRule = new WorkspaceTestRule();

	private IResourceChangeDescriptionFactory factory;
	private IProject project;
	private static int MASK = 0xFFFFFF;
	private static int KIND_MASK = 0xFF;
	private static int FLAGS_MASK = MASK ^= KIND_MASK;

	@Before
	public void setUp() throws Exception {
		project = getWorkspace().getRoot().getProject("Project");
		IResource[] resources = buildResources(project, new String[] { "a/", "a/b/", "a/c/", "a/d", "a/b/e", "a/b/f" });
		createInWorkspace(resources);
		assertExistsInWorkspace(resources);
		factory = ResourceChangeValidator.getValidator().createDeltaFactory();
		int kind = factory.getDelta().getKind();
		int flags = factory.getDelta().getFlags();
		assertEquals("Projects delta kind should not contain any bits before refactoring.", 0, kind &= ~KIND_MASK);
		assertEquals("Projects delta flags should not be set before refactoring.", 0, flags &= ~FLAGS_MASK);
	}

	@Test
	public void testDeletionWithContents() {
		testDeletion(true);
	}

	@Test
	public void testDeletionWithoutContents() {
		testDeletion(false);
	}

	private void testDeletion(boolean deleteContents) {
		factory.delete(project, deleteContents);
		checkAffectedChildrenStatus(factory.getDelta().getAffectedChildren(), deleteContents);
	}

	private void checkAffectedChildrenStatus(IResourceDelta[] affectedChildren, boolean deleteContents) {
		for (IResourceDelta iResourceDelta : affectedChildren) {
			assertEquals("IResourceDelta.REMOVED kind is expected on project deletion.", IResourceDelta.REMOVED,
					iResourceDelta.getKind());
			if (deleteContents) {
				assertEquals("IResourceDelta.DELETE_CONTENT_PROPOSED flag should be set on project contents deletion.",
						IResourceDelta.DELETE_CONTENT_PROPOSED,
						iResourceDelta.getFlags());
			} else {
				assertEquals("No flags should be set on project deletion from workspace.", 0,
						iResourceDelta.getFlags());
			}
			checkAffectedChildrenStatus(iResourceDelta.getAffectedChildren(), deleteContents);
		}
	}

}
