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
package org.eclipse.core.tests.internal.builders;

import java.io.ByteArrayInputStream;
import java.util.*;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Tests that deltas supplied to the builder are accurate
 */
public class BuildDeltaVerificationTest extends AbstractBuilderTest {
	DeltaVerifierBuilder verifier;
	/* some random resource handles */
	protected static final String PROJECT1 = "Project1";
	protected static final String PROJECT2 = "Project2";
	protected static final String FOLDER1 = "Folder1";
	protected static final String FOLDER2 = "Folder2";
	protected static final String FILE1 = "File1";
	protected static final String FILE2 = "File2";
	protected static final String FILE3 = "File3";
	IProject project1;
	IProject project2;
	IFolder folder1;//below project2
	IFolder folder2;//below folder1
	IFolder folder3;//same as file1
	IFile file1;//below folder1
	IFile file2;//below folder1
	IFile file3;//below folder2

	/**
	 * Creates a new instance of BuildDeltaVerificationTest.
	 * @param name java.lang.String
	 */
	public BuildDeltaVerificationTest(String name) {
		super(name);
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void assertDelta() {
		if (!verifier.isDeltaValid()) {
			//		System.out.println(verifier.getMessage());
		}
		assertTrue("Should be an incremental build", verifier.wasIncrementalBuild());
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Runs code to handle a core exception
	 */
	protected void handleCoreException(CoreException e) {
		assertTrue("CoreException: " + e.getMessage(), false);
	}

	/**
	 * Rebuilds the solution.
	 */
	protected void rebuild() throws CoreException {
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());
	}

	/**
	 * Sets up the fixture, for example, open a network connection.
	 * This method is called before a test is executed.
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();

		// Turn auto-building off
		IWorkspace workspace = getWorkspace();
		setAutoBuilding(false);

		// Create some resource handles
		project1 = workspace.getRoot().getProject(PROJECT1);
		project2 = workspace.getRoot().getProject(PROJECT2);
		folder1 = project1.getFolder(FOLDER1);
		folder2 = folder1.getFolder(FOLDER2);
		folder3 = folder1.getFolder(FILE1);
		file1 = folder1.getFile(FILE1);
		file2 = folder1.getFile(FILE2);
		file3 = folder2.getFile(FILE1);

		// Create and open a project, folder and file
		try {
			project1.create(getMonitor());
			project1.open(getMonitor());
			folder1.create(true, true, getMonitor());
			file1.create(getRandomContents(), true, getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// Create and set a build spec for the project
		try {
			IProjectDescription desc = project1.getDescription();
			ICommand command = desc.newCommand();
			command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
			desc.setBuildSpec(new ICommand[] {command});
			project1.setDescription(desc, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// Build the project
		try {
			project1.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());
		} catch (CoreException e) {
			fail("3.3", e);
		}

		verifier = DeltaVerifierBuilder.getInstance();
		assertNotNull("Builder was not instantiated", verifier);
		assertTrue("First build should be a batch build", verifier.wasFullBuild());
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddAndRemoveFile() {
		try {
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file2.create(in, true, getMonitor());
			file2.delete(true, getMonitor());
			rebuild();
			//builder for project1 may not even be called (empty delta)
			if (verifier.wasFullBuild()) {
				verifier.emptyBuild();
			}
			assertTrue(verifier.getMessage(), verifier.isDeltaValid());
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddAndRemoveFolder() {
		try {
			folder2.create(true, true, getMonitor());
			folder2.delete(true, getMonitor());
			rebuild();
			//builder for project1 may not even be called (empty delta)
			if (verifier.wasFullBuild()) {
				verifier.emptyBuild();
			}
			assertTrue(verifier.getMessage(), verifier.isDeltaValid());
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddFile() {
		try {
			verifier.addExpectedChange(file2, project1, IResourceDelta.ADDED, 0);
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file2.create(in, true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddFileAndFolder() {
		try {
			verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
			verifier.addExpectedChange(file3, project1, IResourceDelta.ADDED, 0);
			folder2.create(true, true, getMonitor());
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file3.create(in, true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddFolder() {
		try {
			verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
			folder2.create(true, true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testAddProject() {
		try {
			//should not affect project1's delta
			project2.create(getMonitor());
			rebuild();
			//builder for project1 should not even be called
			assertTrue(verifier.getMessage(), verifier.isDeltaValid());
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testChangeFile() {
		try {
			/* change file1's contents */
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file1.setContents(in, true, false, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testChangeFileToFolder() {
		try {
			/* change file1 into a folder */
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
			file1.delete(true, getMonitor());
			folder3.create(true, true, getMonitor());

			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testChangeFolderToFile() {
		try {
			/* change to a folder */
			file1.delete(true, getMonitor());
			folder3.create(true, true, getMonitor());
			rebuild();

			/* now change back to a file and verify */
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
			folder3.delete(true, getMonitor());
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {1, 2});
			file1.create(in, true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testCloseOpenReplaceFile() {
		try {

			rebuild();
			project1.close(null);
			project1.open(null);

			/* change file1's contents */
			verifier = DeltaVerifierBuilder.getInstance();
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.CONTENT);
			file1.delete(true, null);
			file1.create(getRandomContents(), true, null);
			rebuild();
			//new builder gets instantiated so grab a reference to the latest builder
			verifier = DeltaVerifierBuilder.getInstance();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testMoveFile() {
		try {
			verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
			verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null, file3.getFullPath());
			verifier.addExpectedChange(file3, project1, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM, file1.getFullPath(), null);

			folder2.create(true, true, getMonitor());
			file1.move(file3.getFullPath(), true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testRemoveFile() {
		try {
			verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, 0);
			file1.delete(true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testRemoveFileAndFolder() {
		try {
			verifier.addExpectedChange(folder1, project1, IResourceDelta.REMOVED, 0);
			verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, 0);
			folder1.delete(true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testReplaceFile() {
		try {
			/* change file1's contents */
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.REPLACED | IResourceDelta.CONTENT);
			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file1.delete(true, getMonitor());
			file1.create(in, true, getMonitor());
			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 * @see SortBuilderPlugin
	 * @see SortBuilder
	 */
	public void testTwoFileChanges() {
		try {
			verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
			verifier.addExpectedChange(file2, project1, IResourceDelta.ADDED, 0);

			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file1.setContents(in, true, false, getMonitor());

			ByteArrayInputStream in2 = new ByteArrayInputStream(new byte[] {4, 5, 6});
			file2.create(in2, true, getMonitor());

			rebuild();
			assertDelta();
		} catch (CoreException e) {
			handleCoreException(e);
		}
	}

	public void testReuseCachedDelta() {
		try {
			IProject project = getWorkspace().getRoot().getProject("delta-cache");
			create(project, false);

			IProjectDescription description = project.getDescription();
			description.setBuildSpec(new ICommand[] { createCommand(description, EmptyDeltaBuilder.BUILDER_NAME, null),
					createCommand(description, EmptyDeltaBuilder2.BUILDER_NAME, null) });
			project.setDescription(description, getMonitor());

			project.build(IncrementalProjectBuilder.FULL_BUILD, getMonitor());

			List<IResourceDelta> deltas = new ArrayList<>();

			TestBuilder.BuilderRuleCallback captureDelta = new TestBuilder.BuilderRuleCallback() {
				@Override
				public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor)
						throws CoreException {
					deltas.add(getDelta(project));
					return super.build(kind, args, monitor);
				}
			};

			EmptyDeltaBuilder.getInstance().setRuleCallback(captureDelta);
			EmptyDeltaBuilder2.getInstance().setRuleCallback(captureDelta);

			ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
			project.getFile("test").create(in, true, getMonitor());

			project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, getMonitor());

			assertSame("both builders should receive the same cached delta ", deltas.get(0), deltas.get(1));

		} catch (CoreException e) {
			handleCoreException(e);
		}
	}
}
