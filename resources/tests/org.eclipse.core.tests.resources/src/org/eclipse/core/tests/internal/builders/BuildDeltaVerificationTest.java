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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
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
		project1.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
		project2.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());
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
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		folder1.create(true, true, createTestMonitor());
		file1.create(getRandomContents(), true, createTestMonitor());

		// Create and set a build spec for the project
		IProjectDescription desc = project1.getDescription();
		ICommand command = desc.newCommand();
		command.setBuilderName(DeltaVerifierBuilder.BUILDER_NAME);
		desc.setBuildSpec(new ICommand[] { command });
		project1.setDescription(desc, createTestMonitor());

		// Build the project
		project1.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		verifier = DeltaVerifierBuilder.getInstance();
		assertNotNull("Builder was not instantiated", verifier);
		assertTrue("First build should be a batch build", verifier.wasFullBuild());
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddAndRemoveFile() throws CoreException {
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file2.create(in, true, createTestMonitor());
		file2.delete(true, createTestMonitor());
		rebuild();
		// builder for project1 may not even be called (empty delta)
		if (verifier.wasFullBuild()) {
			verifier.emptyBuild();
		}
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddAndRemoveFolder() throws CoreException {
		folder2.create(true, true, createTestMonitor());
		folder2.delete(true, createTestMonitor());
		rebuild();
		// builder for project1 may not even be called (empty delta)
		if (verifier.wasFullBuild()) {
			verifier.emptyBuild();
		}
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddFile() throws CoreException {
		verifier.addExpectedChange(file2, project1, IResourceDelta.ADDED, 0);
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file2.create(in, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddFileAndFolder() throws CoreException {
		verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file3, project1, IResourceDelta.ADDED, 0);
		folder2.create(true, true, createTestMonitor());
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file3.create(in, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddFolder() throws CoreException {
		verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
		folder2.create(true, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testAddProject() throws CoreException {
		// should not affect project1's delta
		project2.create(createTestMonitor());
		rebuild();
		// builder for project1 should not even be called
		assertTrue(verifier.getMessage(), verifier.isDeltaValid());
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testChangeFile() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file1.setContents(in, true, false, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testChangeFileToFolder() throws CoreException {
		/* change file1 into a folder */
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED,
				IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
		file1.delete(true, createTestMonitor());
		folder3.create(true, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testChangeFolderToFile() throws CoreException {
		/* change to a folder */
		file1.delete(true, createTestMonitor());
		folder3.create(true, true, createTestMonitor());
		rebuild();

		/* now change back to a file and verify */
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED,
				IResourceDelta.CONTENT | IResourceDelta.TYPE | IResourceDelta.REPLACED);
		folder3.delete(true, createTestMonitor());
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 1, 2 });
		file1.create(in, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testCloseOpenReplaceFile() throws CoreException {
		rebuild();
		project1.close(null);
		project1.open(null);

		/* change file1's contents */
		verifier = DeltaVerifierBuilder.getInstance();
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED,
				IResourceDelta.REPLACED | IResourceDelta.CONTENT);
		file1.delete(true, null);
		file1.create(getRandomContents(), true, null);
		rebuild();
		// new builder gets instantiated so grab a reference to the latest builder
		verifier = DeltaVerifierBuilder.getInstance();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testMoveFile() throws CoreException {
		verifier.addExpectedChange(folder2, project1, IResourceDelta.ADDED, 0);
		verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, IResourceDelta.MOVED_TO, null,
				file3.getFullPath());
		verifier.addExpectedChange(file3, project1, IResourceDelta.ADDED, IResourceDelta.MOVED_FROM,
				file1.getFullPath(), null);

		folder2.create(true, true, createTestMonitor());
		file1.move(file3.getFullPath(), true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testRemoveFile() throws CoreException {
		verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, 0);
		file1.delete(true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testRemoveFileAndFolder() throws CoreException {
		verifier.addExpectedChange(folder1, project1, IResourceDelta.REMOVED, 0);
		verifier.addExpectedChange(file1, project1, IResourceDelta.REMOVED, 0);
		folder1.delete(true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testReplaceFile() throws CoreException {
		/* change file1's contents */
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED,
				IResourceDelta.REPLACED | IResourceDelta.CONTENT);
		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file1.delete(true, createTestMonitor());
		file1.create(in, true, createTestMonitor());
		rebuild();
		assertDelta();
	}

	/**
	 * Tests that the builder is receiving an appropriate delta
	 */
	public void testTwoFileChanges() throws CoreException {
		verifier.addExpectedChange(file1, project1, IResourceDelta.CHANGED, IResourceDelta.CONTENT);
		verifier.addExpectedChange(file2, project1, IResourceDelta.ADDED, 0);

		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file1.setContents(in, true, false, createTestMonitor());

		ByteArrayInputStream in2 = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		file2.create(in2, true, createTestMonitor());

		rebuild();
		assertDelta();
	}

	public void testReuseCachedDelta() throws CoreException {
		IProject project = getWorkspace().getRoot().getProject("delta-cache");
		createInWorkspace(project);

		IProjectDescription description = project.getDescription();
		description.setBuildSpec(new ICommand[] { createCommand(description, EmptyDeltaBuilder.BUILDER_NAME, null),
				createCommand(description, EmptyDeltaBuilder2.BUILDER_NAME, null) });
		project.setDescription(description, createTestMonitor());

		project.build(IncrementalProjectBuilder.FULL_BUILD, createTestMonitor());

		List<IResourceDelta> deltas = new ArrayList<>();

		TestBuilder.BuilderRuleCallback captureDelta = new TestBuilder.BuilderRuleCallback() {
			@Override
			public IProject[] build(int kind, Map<String, String> args, IProgressMonitor monitor) throws CoreException {
				deltas.add(getDelta(project));
				return super.build(kind, args, monitor);
			}
		};

		EmptyDeltaBuilder.getInstance().setRuleCallback(captureDelta);
		EmptyDeltaBuilder2.getInstance().setRuleCallback(captureDelta);

		ByteArrayInputStream in = new ByteArrayInputStream(new byte[] { 4, 5, 6 });
		project.getFile("test").create(in, true, createTestMonitor());

		project.build(IncrementalProjectBuilder.INCREMENTAL_BUILD, createTestMonitor());

		assertSame("both builders should receive the same cached delta ", deltas.get(0), deltas.get(1));
	}
}
