/*******************************************************************************
 *  Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.internal.mapping;

import java.util.ArrayList;
import java.util.List;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.IResourceChangeDescriptionFactory;
import org.eclipse.core.resources.mapping.ModelStatus;
import org.eclipse.core.resources.mapping.ResourceChangeValidator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests for change validation
 */
public class ChangeValidationTest extends ResourceTest {
	private IResourceChangeDescriptionFactory factory;
	private IProject project;

	private void assertStatusEqual(IStatus status, String[] expectedMessages) {
		List<String> actualMessages = new ArrayList<>();
		if (status.isMultiStatus()) {
			IStatus[] children = status.getChildren();
			for (IStatus element : children) {
				String message = getModelMessage(element);
				if (message != null) {
					actualMessages.add(message);
				}
			}
		} else {
			String message = getModelMessage(status);
			if (message != null) {
				actualMessages.add(message);
			}
		}
		if (expectedMessages.length < actualMessages.size()) {
			for (String actual : actualMessages) {
				boolean found = false;
				for (String expected : expectedMessages) {
					if (actual.equals(expected)) {
						found = true;
						break;
					}
				}
				if (!found) {
					fail("Unexpected message returned: " + actual);
				}
			}
		} else {
			for (String expectedMessage : expectedMessages) {
				if (!actualMessages.contains(expectedMessage)) {
					fail("Expect message missing: " + expectedMessage);
				}
			}
		}
	}

	private IResourceChangeDescriptionFactory createEmptyChangeDescription() {
		return ResourceChangeValidator.getValidator().createDeltaFactory();
	}

	/*
	 * Only return the message of the status if it
	 * came from our test model provider
	 */
	private String getModelMessage(IStatus status) {
		if (status instanceof ModelStatus ms) {
			String id = ms.getModelProviderId();
			if (id.equals(TestModelProvider.ID)) {
				return status.getMessage();
			}
		}
		return null;
	}

	@Override
	protected void setUp() throws Exception {
		TestModelProvider.enabled = true;
		super.setUp();
		project = getWorkspace().getRoot().getProject("Project");
		IResource[] before = buildResources(project, new String[] {"c/", "c/b/", "c/a/", "c/x", "c/b/y", "c/b/z"});
		ensureExistsInWorkspace(before, true);
		assertExistsInWorkspace(before);
		factory = createEmptyChangeDescription();
	}

	@Override
	protected void tearDown() throws Exception {
		TestModelProvider.enabled = false;
		super.tearDown();
	}

	public void testCopyReplaceDeletedFolder() {
		// Copy folder to replace a deleted folder
		final IResource folder = project.findMember("c/b/");
		IFolder destination = project.getFolder("/c/a/");
		factory.delete(destination);
		factory.copy(folder, destination.getFullPath());
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.COPIED, folder),});
	}

	public void testFileChanges() {
		factory.change((IFile) project.findMember("c/x"));
		factory.change((IFile) project.findMember("c/b/y"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.CHANGED, project.findMember("c/x")), ChangeDescription.getMessageFor(ChangeDescription.CHANGED, project.findMember("c/b/y"))});
	}

	public void testFileCopy() {
		factory.copy(project.findMember("c/x"), IPath.fromOSString("c/x2"));
		factory.copy(project.findMember("c/b/y"), IPath.fromOSString("c/y"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.COPIED, project.findMember("c/x")), ChangeDescription.getMessageFor(ChangeDescription.COPIED, project.findMember("c/b/y"))});
	}

	public void testFileCreate() {
		IFile file = project.getFile("file");
		factory.create(file);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.ADDED, file)});
	}

	public void testFileInFolderCreate() {
		IFolder folder = project.getFolder("folder");
		IFile file = folder.getFile("file");
		factory.create(folder);
		factory.create(file);
		IStatus status = validateChange(factory);
		//this isn't very accurate, but ChangeDescription doesn't currently record recursive creates
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.ADDED, folder)});

	}

	public void testFileDeletion() {
		factory.delete(project.findMember("c/x"));
		factory.delete(project.findMember("c/b/y"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project.findMember("c/x")), ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project.findMember("c/b/y"))});

		// Check if the given delta also indicates contents deletion
		assertContentDeletionFlag("Validation should return error status on contents deletion.", true);
	}

	public void testFileMoves() {
		factory.move(project.findMember("c/x"), IPath.fromOSString("c/x2"));
		factory.move(project.findMember("c/b/y"), IPath.fromOSString("c/y"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.MOVED, project.findMember("c/x")), ChangeDescription.getMessageFor(ChangeDescription.MOVED, project.findMember("c/b/y"))});
	}

	public void testFolderCopy() {
		final IResource folder = project.findMember("c/b/");
		factory.copy(folder, IPath.fromOSString("c/d"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.COPIED, folder),});
	}

	public void testFolderDeletion() {
		final IResource folder = project.findMember("c/b/");
		factory.delete(folder);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project.findMember("c/b")),});

		// Check if the given delta also indicates contents deletion
		assertContentDeletionFlag("Validation should return error status on contents deletion.", true);
	}

	public void testFolderMove() {
		final IResource folder = project.findMember("c/b/");
		factory.move(folder, IPath.fromOSString("c/d"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.MOVED, folder),});
	}

	public void testMoveReplaceDeletedFolder() {
		// Move to replace a deleted folder
		final IResource folder = project.findMember("c/b/");
		IFolder destination = project.getFolder("/c/a/");
		factory.delete(destination);
		factory.move(folder, destination.getFullPath());
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.MOVED, folder),});
	}

	public void testProjectClose() {
		factory.close(project);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.CLOSED, project)});

		// Check if the given delta does not indicate contents removal
		assertContentDeletionFlag("Validation should return warning status on project close.", false);
	}

	public void testProjectCopy() {
		// A project copy
		factory.copy(project, IPath.fromOSString("MovedProject"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.COPIED, project)});
	}

	public void testProjectDeletion() {
		// A project deletion
		factory.delete(project);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project)});

		// Check if the given delta does not indicate contents removal on project deletion without arguments
		assertContentDeletionFlag("Validation should return warning status on simple deletion.", false);
	}

	public void testProjectDeletionWithContents() {
		factory.delete(project, true);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] { ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project) });
		// Check if the given delta also indicates contents deletion
		assertContentDeletionFlag("Validation should return error status on contents deletion.", true);
	}

	public void testProjectDeletionWithoutContents() {
		factory.delete(project, false);
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] { ChangeDescription.getMessageFor(ChangeDescription.REMOVED, project) });
		// Check if the given delta does not indicate contents removal
		assertContentDeletionFlag("Validation should return warning status on project removal from workspace.", false);
	}

	public void testProjectMove() {
		factory.move(project, IPath.fromOSString("MovedProject"));
		IStatus status = validateChange(factory);
		assertStatusEqual(status, new String[] {ChangeDescription.getMessageFor(ChangeDescription.MOVED, project)});
	}

	private void assertContentDeletionFlag(String assertMessage, boolean expectFlag) {
		int expectedStatus = expectFlag ? IStatus.ERROR : IStatus.WARNING;
		try {
			TestModelProvider.checkContentsDeletion = true;
			IStatus status = validateChange(factory);
			assertEquals(assertMessage, expectedStatus, status.getSeverity());
		} finally {
			TestModelProvider.checkContentsDeletion = false;
		}
	}

	private IStatus validateChange(IResourceChangeDescriptionFactory f) {
		return ResourceChangeValidator.getValidator().validateChange(f.getDelta(), getMonitor());
	}

}
