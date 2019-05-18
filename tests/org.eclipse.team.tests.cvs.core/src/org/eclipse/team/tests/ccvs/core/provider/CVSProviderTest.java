/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.provider;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.team.core.IFileTypeInfo;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSStatus;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProvider;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.KSubstOption;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.client.Update;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.core.syncinfo.ResourceSyncInfo;
import org.eclipse.team.internal.ccvs.core.util.Util;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.ccvs.ui.ICVSUIConstants;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

import junit.framework.Test;

/*
 * This class tests both the CVSProvider and the CVSTeamProvider
 */
public class CVSProviderTest extends EclipseTest {

	/**
	 * Constructor for CVSProviderTest
	 */
	public CVSProviderTest() {
		super();
	}

	/**
	 * Constructor for CVSProviderTest
	 */
	public CVSProviderTest(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(CVSProviderTest.class);
	}
	
	public void testAdd() {
		
		// Test add with cvsignores
		/*
		IProject project = createProject("testAdd", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		IFile file = project.getFile(".cvsignore");
		file.create(new ByteArrayInputStream("ignored.txt".getBytes()), false, null);
		file = project.getFile("ignored.txt");
		file.create(new ByteArrayInputStream("some text".getBytes()), false, null);
		file = project.getFile("notignored.txt");
		file.create(new ByteArrayInputStream("some more text".getBytes()), false, null);
		file = project.getFile("folder1/.cvsignore");
		file.create(new ByteArrayInputStream("ignored.txt".getBytes()), false, null);
		file = project.getFile("folder1/ignored.txt");
		file.create(new ByteArrayInputStream("some text".getBytes()), false, null);
		file = project.getFile("folder1/notignored.txt");
		file.create(new ByteArrayInputStream("some more text".getBytes()), false, null);
		
		getProvider(project).add(new IResource[] {project}, IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
		
		assertTrue( ! CVSWorkspaceRoot.getCVSResourceFor(project.getFile("ignored.txt")).isManaged());
		assertTrue( ! CVSWorkspaceRoot.getCVSResourceFor(project.getFile("folder1/ignored.txt")).isManaged());
		
		assertTrue(CVSWorkspaceRoot.getCVSResourceFor(project.getFile("notignored.txt")).isManaged());
		assertTrue(CVSWorkspaceRoot.getCVSResourceFor(project.getFile("folder1/notignored.txt")).isManaged());
		assertTrue(CVSWorkspaceRoot.getCVSResourceFor(project.getFile(".cvsignore")).isManaged());
		assertTrue(CVSWorkspaceRoot.getCVSResourceFor(project.getFile("folder1/.cvsignore")).isManaged());
		*/
	}
	
	public void testDeleteHandling() throws TeamException, CoreException {
		
		IProject project = createProject("testDeleteHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Delete a file and ensure that it is an outgoing deletion
		project.getFile("deleted.txt").delete(false, false, null);
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("deleted.txt"));
		assertTrue("File is not outgoing deletion", file.getSyncInfo().isDeleted());
		
		// Delete a folder and ensure that the file is managed but doesn't exist
		// (Special behavior is provider by the CVS move/delete hook but this is not part of CVS core)
		project.getFolder("folder1").delete(false, false, null);
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("folder1"));
		assertTrue("Deleted folder not in proper state", ! folder.exists() && folder.isManaged());
	}
	
	public void testCheckin() throws TeamException, CoreException, IOException {
		IProject project = createProject("testCheckin", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Perform some operations on the project
		IResource[] newResources = buildResources(project, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
		setContentsAndEnsureModified(project.getFile("changed.txt"));
		addResources(newResources);
		deleteResources(new IResource[] {project.getFile("deleted.txt")});
		assertIsModified("testDeepCheckin: ", newResources);
		assertIsModified("testDeepCheckin: ", new IResource[] {project.getFile("deleted.txt"), project.getFile("changed.txt")});
		commitResources(new IResource[] {project}, IResource.DEPTH_INFINITE);
		assertLocalStateEqualsRemote(project);
	}
	
	public void testMoveHandling() throws TeamException, CoreException {
		IProject project = createProject("testMoveHandling", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Move a file and ensure that it is an outgoing deletion at the source and unmanaged at the destination
		project.getFile("deleted.txt").move(new Path("moved.txt"), false, false, null);
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("deleted.txt"));
		assertTrue("Source is not outgoing deletion", file.getSyncInfo().isDeleted());
		file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("moved.txt"));
		assertTrue("Destination not in proper state", ! file.isManaged());
		
		// Move a folder and ensure the source is deleted
		project.getFolder("folder1").move(new Path("moved"), false, false, null);
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("folder1"));
		assertTrue("Deleted folder not in proper state", ! folder.exists() && folder.isManaged());
		folder = CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("moved"));
		assertTrue("Moved folder should not be managed", ! folder.isManaged());
		assertTrue("Moved folder should not be a CVS folder", ! folder.isCVSFolder());
	}
	
	public void testUpdate() throws TeamException, CoreException, IOException {
		// Create a test project, import it into cvs and check it out
		IProject project = createProject("testUpdate", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });

		// Check the project out under a different name
		IProject copy = checkoutCopy(project, "-copy");
		
		// Perform some operations on the copy
		addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
		setContentsAndEnsureModified(copy.getFile("changed.txt"));
		deleteResources(new IResource[] {copy.getFile("deleted.txt")});
		
		// Commit the copy and update the project
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE);
		updateProject(project, null, false);
		assertEquals(project, copy);
	}
	
	public void testUpdate123280() throws CoreException {
		IProject project = createProject(new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Make a remote change
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
		commitProject(copy);
		
		// Delete locally and then update
		project.getFile("deleted.txt").delete(false, null);
		updateProject(project, null, false);
		// Ensure that the file is still an outgoing deletion
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(project.getFile("deleted.txt"));
		assertTrue(!file.exists() && file.isManaged());
	}
	
	public void testVersionTag() throws TeamException, CoreException, IOException {
		
		// Create a test project, import it into cvs and check it out
		IProject project = createProject("testVersionTag", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Perform some operations on the copy and commit
		IProject copy = checkoutCopy(project, "-copy");
		addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
		changeResources(copy, new String[] {"changed.txt"}, false);
		deleteResources(copy, new String[] {"deleted.txt"}, false);
		commitResources(copy, true);
		
		// Tag the original, checkout the tag and compare with original
		CVSTag v1Tag = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, v1Tag, false);
		IProject v1 = checkoutCopy(project, v1Tag);
		assertEquals(project, v1);
		
		// Update original to HEAD and compare with copy including tags
		updateProject(project, null, false);
		assertEquals(project, copy, false, true);
		
		// Update copy to v1 and compare with the copy (including tag)
		updateProject(copy, v1Tag, false);
		assertEquals(copy, v1, false, true);
		
		// Update copy back to HEAD and compare with project (including tag)
		updateProject(copy, CVSTag.DEFAULT, false);
		assertEquals(project, copy, false, true);
	}
	
	public void testMakeBranch() throws TeamException, CoreException, IOException {
		// Create a test project
		IProject project = createProject("testMakeBranch", new String[] { "file1.txt", "file2.txt", "file3.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});

		// Make some local modifications including "cvs adds" and "cvs removes"
		addResources(project, new String[] {"folder1/c.txt"}, false);
		deleteResources(project, new String[] {"folder1/b.txt"}, false);
		changeResources(project, new String[] {"file2.txt"}, false);
		
		// Make the branch including a pre-version
		CVSTag version = new CVSTag("v1", CVSTag.BRANCH);
		CVSTag branch = new CVSTag("branch1", CVSTag.BRANCH);
		makeBranch(new IResource[] {project}, version, branch, true);

		// Checkout a copy from the branch and version and compare
		IProject branchCopy = checkoutCopy(project, branch);
		IProject versionCopy = checkoutCopy(project, version);
		assertEquals(branchCopy, versionCopy, true, false);
		
		// Commit the project, update the branch and compare
		commitProject(project);
		updateProject(branchCopy, null, false);
		assertEquals(branchCopy, project, false, true);
	}
	
	public void testDuplicatedBranch() throws TeamException, CoreException {
		// Create a test project
		IProject project = createProject("testDuplicatedBranch", new String[] {
				"file1.txt", "file2.txt", "file3.txt", "folder1/",
				"folder1/a.txt", "folder1/b.txt" });

		// Create a branch
		CVSTag version = new CVSTag("v1", CVSTag.BRANCH);
		CVSTag branch = new CVSTag("branch1", CVSTag.BRANCH);
		makeBranch(new IResource[] { project }, version, branch, false);

		// Create the second branch using the same names
		try {
			makeBranch(new IResource[] { project }, version, branch, false);
			fail();
		} catch (CVSException e) {

		}
	}

	public void testPruning() throws TeamException, CoreException, IOException {
		// Create a project with empty folders
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(false);
		IProject project = createProject("testPruning", new String[] { "file.txt", "folder1/", "folder2/folder3/" });

		// Disable pruning, checkout a copy and ensure original and copy are the same
		IProject copy = checkoutCopy(project, "-copy");
		assertEquals(project, copy); 

		// Enable pruning, update copy and ensure emtpy folders are gone
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(true);
		updateProject(copy, null, false);
		assertDoesNotExistInFileSystem(new IResource[] {copy.getFolder("folder1"), copy.getFolder("folder2"), copy.getFolder("folder2/folder3")});
		
		// Checkout another copy and ensure that the two copies are the same (with pruning enabled)
		IProject copy2 = checkoutCopy(project, "-copy2");
		assertEquals(copy, copy2); 
		
		// Disable pruning, update copy and ensure directories come back
		// TODO: disabled until bug 125037 can be resolved
//		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(false);
//		updateProject(copy, null, false);
//		assertEquals(project, copy);
		
		// Enable pruning again since it's the default
		CVSProviderPlugin.getPlugin().setPruneEmptyDirectories(true);
	}

	public void testGet() throws TeamException, CoreException, IOException {
		
		// Create a project
		IProject project = createProject("testGet", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		
		// Checkout a copy and modify locally
		IProject copy = checkoutCopy(project, "-copy");
		//addResources(copy, new String[] { "added.txt", "folder2/", "folder2/added.txt" }, false);
		deleteResources(copy, new String[] {"deleted.txt"}, false);
		setContentsAndEnsureModified(copy.getFile("changed.txt"));

		// get the remote conetns
		replace(new IResource[] {copy}, null, true);
		assertEquals(project, copy);
	}
	
	public void testReadOnly() {
		// IProject project = createProject("testReadOnly", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt" });
		// Need to check the project out as read-only
	}
	
	public void testCleanLineDelimiters() throws TeamException, CoreException, IOException {
		// Create a project
		IProject project = getUniqueTestProject("testCleanLineDelimiters");
		IFile file = project.getFile("testfile");
		IProgressMonitor monitor = new NullProgressMonitor();

		// empty file
		setFileContents(file, "");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "");
		
		// one byte
		setFileContents(file, "a");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "a");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "a");
		
		// single orphan carriage return (should be preserved)
		setFileContents(file, "\r");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "\r");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "\r");

		// single line feed
		setFileContents(file, "\n");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "\n");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "\r\n");
		
		// single carriage return line feed
		setFileContents(file, "\r\n");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "\r\n");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "\n");
		
		// mixed text with orphaned CR's
		setFileContents(file, "The \r\n quick brown \n fox \r\r\r\n jumped \n\n over \r\n the \n lazy dog.\r\n");
		CVSTeamProvider.cleanLineDelimiters(file, false, monitor);
		assertEqualsFileContents(file, "The \n quick brown \n fox \r\r\n jumped \n\n over \n the \n lazy dog.\n");
		setFileContents(file, "The \r\n quick brown \n fox \r\r\r\n jumped \n\n over \r\n the \n lazy dog.\r\n");
		CVSTeamProvider.cleanLineDelimiters(file, true, monitor);
		assertEqualsFileContents(file, "The \r\n quick brown \r\n fox \r\r\r\n jumped \r\n\r\n over \r\n the \r\n lazy dog.\r\n");
	}
	
	public void testKeywordSubstitution() throws TeamException, CoreException {
		testKeywordSubstitution(Command.KSUBST_BINARY); // -kb
		testKeywordSubstitution(Command.KSUBST_TEXT); // -ko
		testKeywordSubstitution(Command.KSUBST_TEXT_EXPAND); // -kkv
	}

	private void testKeywordSubstitution(KSubstOption ksubst) throws TeamException, CoreException {
		// setup some known file types
		Team.setAllTypes( new String[] {"xbin", "xtxt"}, new int[] {Team.BINARY, Team.TEXT});
		
		// create a test project
		IProject project = createProject("testKeywordSubstitution", new String[] { "dummy" });
		addResources(project, new String[] { "binary.xbin", "text.xtxt", "folder1/", "folder1/a.xtxt" }, true);
		addResources(project, new String[] { "added.xbin", "added.xtxt" }, false);
		assertHasKSubstOption(project, "binary.xbin", Command.KSUBST_BINARY);
		assertHasKSubstOption(project, "added.xbin", Command.KSUBST_BINARY);
		assertHasKSubstOption(project, "text.xtxt", CVSProviderPlugin.DEFAULT_TEXT_KSUBST_OPTION);
		assertHasKSubstOption(project, "folder1/a.xtxt", CVSProviderPlugin.DEFAULT_TEXT_KSUBST_OPTION);
		assertHasKSubstOption(project, "added.xtxt", CVSProviderPlugin.DEFAULT_TEXT_KSUBST_OPTION);
		
		// change keyword substitution
		Map<IFile, KSubstOption> map = new HashMap<>();
		map.put(project.getFile("binary.xbin"), ksubst);
		map.put(project.getFile("added.xbin"), ksubst);
		map.put(project.getFile("text.xtxt"), ksubst);
		map.put(project.getFile("folder1/a.xtxt"), ksubst);
		map.put(project.getFile("added.xtxt"), ksubst);
		
		waitMsec(1500);
		IStatus status = getProvider(project).setKeywordSubstitution(map, null, null);
		assertTrue("Status should be ok, was: " + status.toString(), status.isOK());
		assertHasKSubstOption(project, "binary.xbin", ksubst);
		assertHasKSubstOption(project, "text.xtxt", ksubst);
		assertHasKSubstOption(project, "folder1/a.xtxt", ksubst);
		assertHasKSubstOption(project, "added.xtxt", ksubst);
		assertHasKSubstOption(project, "added.xbin", ksubst);

		// verify that substitution mode changed remotely and "added.xtxt", "added.xbin" don't exist
		IProject copy = checkoutCopy(project, "-copy");
		assertHasKSubstOption(copy, "binary.xbin", ksubst);
		assertHasKSubstOption(copy, "text.xtxt", ksubst);
		assertHasKSubstOption(copy, "folder1/a.xtxt", ksubst);
		assertDoesNotExistInWorkspace(copy.getFile("added.xtxt"));
		assertDoesNotExistInWorkspace(copy.getFile("added.xbin"));
		
		// commit added files then checkout the copy again
		commitResources(project, new String[] { "added.xbin", "added.xtxt" });
		IProject copy2 = checkoutCopy(project, "-copy2");
		assertHasKSubstOption(copy2, "added.xtxt", ksubst);
		assertHasKSubstOption(copy2, "added.xbin", ksubst);
		
		IFileTypeInfo[] infos = Team.getDefaultTypes();
		String[] extensions = new String[infos.length];
		int[] types = new int[infos.length];
		for (int i = 0; i < infos.length; i++) {
			IFileTypeInfo info = infos[i];
			extensions[i] = info.getExtension();
			types[i] = info.getType();
		}
		Team.setAllTypes(extensions, types);
	}
	
	public void testKeywordSubsBinToText() throws TeamException, CoreException, IOException {
		//create a test project
		KSubstOption ksubst = Command.KSUBST_TEXT;
		IProject project = createProject("testKeywordSubsBinToText", new String[] { "dummy" });
		assertHasKSubstOption(project, "dummy", Command.KSUBST_BINARY);
	
		// change keyword substitution
		Map<IFile, KSubstOption> map = new HashMap<>();
		map.put(project.getFile("dummy"), ksubst);
		
		// change from binary to text should commit a new file with 
		waitMsec(1500);
		IStatus status = getProvider(project).setKeywordSubstitution(map, null, null);
		assertTrue("Status should be ok, was: " + status.toString(), status.isOK());
		assertHasKSubstOption(project, "dummy", ksubst);

		IProject copy = checkoutCopy(project, "-copy");
		assertHasKSubstOption(copy, "dummy", ksubst);
		assertEquals(project, copy);		 		
	}
	
	public static void setFileContents(IFile file, String string) throws CoreException {
		InputStream is = new ByteArrayInputStream(string.getBytes());
		if (file.exists()) {
			file.setContents(is, false /*force*/, true /*keepHistory*/, null);
		} else {
			file.create(is, false /*force*/, null);
		}
	}
	
	public static void assertEqualsFileContents(IFile file, String string) throws CoreException, IOException {
		String other = getFileContents(file);
		assertEquals(string, other);
	}
	
	public static void assertHasKSubstOption(IContainer container, String filename, KSubstOption ksubst)
		throws TeamException {
		IFile file = container.getFile(new Path(filename));
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(file);
		ResourceSyncInfo info = cvsFile.getSyncInfo();
		assertEquals(ksubst, info.getKeywordMode());
	}
	
	public void testUnmap() throws CoreException, TeamException {
		// Create a project
		IProject project = createProject("testUnmap", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/", "folder1/folder2/deep.txt", "folder2/b.txt" });
		// delete a file and folder to create phantoms
		project.getFile("deleted.txt").delete(false, false, null);
		assertTrue(project.getFile("deleted.txt").isPhantom());
		project.getFolder("folder2").delete(false, false, null);
		assertTrue(project.getFolder("folder2").isPhantom());
		// unmap
		RepositoryProvider.unmap(project);
		// ensure that phantoms for the resoucrs no longer exist
		assertFalse(project.getFile("deleted.txt").isPhantom());
		assertFalse(project.getFolder("folder2").isPhantom());
		
		// Create a project
		project = createProject("testUnmap2", new String[] { "changed.txt", "deleted.txt", "folder1/", "folder1/a.txt", "folder1/", "folder1/folder2/deep.txt", "folder2/b.txt" });
		// delete a deep folder to create phantoms
		project.getFolder("folder1/folder2").delete(false, false, null);
		assertTrue(project.getFolder("folder1/folder2").isPhantom());
		// unmap
		RepositoryProvider.unmap(project);
		// ensure that phantoms for the resources no longer exist
		assertFalse(project.getFolder("folder1/folder2").isPhantom());
	}
	
	public void testForBinaryLinefeedCorruption() throws CoreException, TeamException, IOException {
		String EOL = "\n";
		IProject project = createProject("testForBinaryLinefeedCorruption", new String[] { "binaryFile" });
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(project.getFile("binaryFile"));
		assertTrue(ResourceSyncInfo.isBinary(cvsFile.getSyncBytes()));
		setContentsAndEnsureModified(project.getFile("binaryFile"), "line 1" + EOL + "line 2");
		commitProject(project);
		
		// Checkout a copy and ensure the file was not corrupted
		IProject copy = checkoutCopy(project, "-copy");
		assertEquals(project, copy);
	}
	
	public void test33984CannotCommitAfterConflictsMergedLocally() throws CoreException, TeamException, IOException {
			String EOL = System.getProperty("line.separator");
			
			IProject project = createProject("test33984", new String[] { "a.txt", "b.txt" });
			setContentsAndEnsureModified(project.getFile("a.txt"), "line 1");
			setContentsAndEnsureModified(project.getFile("b.txt"), ("line 1" + EOL + "line 2" + EOL + "line3"));

		Map<IFile, KSubstOption> kMode = new HashMap<>();
			kMode.put(project.getFile("a.txt"), Command.KSUBST_TEXT);
			kMode.put(project.getFile("b.txt"), Command.KSUBST_TEXT);
			getProvider(project).setKeywordSubstitution(kMode, "", null);
			
			commitProject(project);
			

		
			// Checkout a copy and ensure the file was not corrupted
			IProject copy = checkoutCopy(project, "-copy");
			assertEquals(project, copy);
			
			// TEST 1: simulate modifying same file by different users
			// b.txt has non-conflicting changes 
			setContentsAndEnsureModified(copy.getFile("b.txt"), ("line 1a" + EOL + "line 2" + EOL + "line3"));
			
			commitProject(copy);
			
			// user updates which would cause a merge with conflict, a commit should not be allowed
			
			setContentsAndEnsureModified(project.getFile("b.txt"), ("line 1" + EOL + "line 2" + EOL + "line3a"));
			updateProject(project, CVSTag.DEFAULT, false /* don't ignore local changes */);
			commitProject(project);

			// TEST 2: a.txt has conflicting changes
			setContentsAndEnsureModified(copy.getFile("a.txt"), "line 1dfgdfne3");
	
			commitProject(copy);
			
			// user updates which would cause a merge with conflict, a commit should not be allowed
			setContentsAndEnsureModified(project.getFile("a.txt"), "some other text");
			updateProject(project, CVSTag.DEFAULT, false /* don't ignore local changes */);
			try {
				commitProject(project);
				fail("should not be allowed to commit a resource with merged conflicts");
			} catch(TeamException e) {
			}
	}
	
	public void testTagExistsFailure() throws TeamException, CoreException, IOException {
		IProject project = createProject(new String[] { "a.txt", "b.txt" });
		CVSTag tag = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, tag, false);
		
		setContentsAndEnsureModified(project.getFile("a.txt"));
		commitProject(project);
		
		try {
			tagProject(project, tag, false/* don't force */);
			fail("The tag should have failed since the tag already exists.");
		} catch (TeamException e) {
			// This is what we expected
			assertTrue("This exception should be an error", e.getStatus().getSeverity() == IStatus.ERROR);
		}
		
		tagProject(project, tag, true /* force */);
		IProject copy = checkoutCopy(project, tag);
		assertEquals(project, copy);
	}
	
	public void testUpdateWithOverwrite() throws TeamException, CoreException {
		// Create a project and ad an unmanaged resource
		IProject project = createProject(new String[] { "a.txt", "b.txt" });
		buildResources(project, new String[] { "new.txt" }, false);
		// Checkout a copy and commit the same resource
		IProject copy = checkoutCopy(project, "-copy");
		addResources(copy, new String[] { "new.txt" }, true);
		// Use the regular update and ensure that it fails
		IStatus status = executeCommand(project, Command.UPDATE, Command.NO_LOCAL_OPTIONS);
		assertStatusContainsCode(status, CVSStatus.INVALID_LOCAL_RESOURCE_PATH);
		// Use the update and overwrite and ensure that it works
		status = executeCommand(project, Command.REPLACE, Command.NO_LOCAL_OPTIONS);
		assertTrue(status.isOK());
	}

	private IStatus executeCommand(IProject project, Update update, LocalOption[] options) throws CVSException {
		Session session = new Session(getRepository(), CVSWorkspaceRoot.getCVSFolderFor(project));
		session.open(DEFAULT_MONITOR);
		try {
			return update.execute(
					session,
					Command.NO_GLOBAL_OPTIONS, 
					options, 
					new String[] { "." },
					null,
					DEFAULT_MONITOR);
		} finally {
			session.close();
		}
		
	}
	
	private void setOnlyLookAtTimestamps(boolean b) {
		CVSUIPlugin.getPlugin().getPreferenceStore().setValue(ICVSUIConstants.PREF_CONSIDER_CONTENTS, !b);
	}

	public void testUpdateWithNoChange() throws TeamException, CoreException {
		IProject project = createProject(new String[] { "a.txt"});
		setContentsAndEnsureModified(project.getFile("a.txt"), "contents");
		commitProject(project);
		setOnlyLookAtTimestamps(true);
		// set the contents to the same value but ensure the local timestamp is different
		setContentsAndEnsureModified(project.getFile("a.txt"), "contents");
		// Update and ensure file timestamp is what is was before out edit
		updateProject(project, null, false);
		Date modDate = CVSWorkspaceRoot.getCVSFileFor(project.getFile("a.txt")).getSyncInfo().getTimeStamp();
		assertEquals("Timestamp was not properly reset", modDate, CVSWorkspaceRoot.getCVSFileFor(project.getFile("a.txt")).getTimeStamp());
		setOnlyLookAtTimestamps(false);
	}
	
	public void testBinaryAddition() throws CoreException {
		// See bug 132255
		KSubstOption option = CVSProviderPlugin.getPlugin().getDefaultTextKSubstOption();
		try {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(Command.KSUBST_TEXT_KEYWORDS_ONLY);
			IProject project = createProject(new String[] { "a.txt"});
			IProject copy = checkoutCopy(project, "-copy");
			create(copy.getFile("binaryFile"), true);
			setContentsAndEnsureModified(copy.getFile("binaryFile"), "/n/n\n\n");
			addResources(new IResource[] { copy.getFile("binaryFile") });
			commitProject(copy);
			updateProject(project, null, false);
			assertContentsEqual(copy.getFile("binaryFile"), project.getFile("binaryFile"));
		} finally {
			CVSProviderPlugin.getPlugin().setDefaultTextKSubstOption(option);
		}
	}
	
	public void testDeletedPhantom139250() throws CoreException {
		IProject project = createProject(new String[] { "a.txt"});
		// Create a phantom folder that is mapped to a remote folder
		// but for which no remote exists
		ICVSFolder folder = CVSWorkspaceRoot.getCVSFolderFor(project.getFolder("phantom"));
		ICVSFolder projectFolder = CVSWorkspaceRoot.getCVSFolderFor(project);
		FolderSyncInfo projectInfo = projectFolder.getFolderSyncInfo();
		String repo = Util.appendPath(projectInfo.getRepository(), "phantom");
		FolderSyncInfo info = new FolderSyncInfo(repo, projectInfo.getRoot(), projectInfo.getTag(), false);
		folder.setFolderSyncInfo(info);
		updateProject(project, null, false);
	}
	
	public void testUpdateOfDeletedFile140007() throws CoreException {
		IProject project = createProject(new String[] { "a.txt"});
		IProject copy = checkoutCopy(project, "-copy");
		deleteResources(copy, new String[] { "a.txt"}, true);
		updateResources(project, new String[] { "a.txt"}, false);
	}
	
	public void testUpdateOfRemotelyRemovedFile() throws CoreException, IOException {
		IProject project = createProject(new String[] { "a.txt"});
		IProject copy = checkoutCopy(project, "-copy");
		//Create a file and add it to version control (but don't commit)
		addResources(copy, new String[] { "b.txt"}, false);
		// Change the revision of the file so it appears to exist remotely
		ICVSFile file = CVSWorkspaceRoot.getCVSFileFor(copy.getFile("b.txt"));
		byte[] syncBytes = file.getSyncBytes();
		syncBytes = ResourceSyncInfo.setRevision(syncBytes, "1.1");
		file.setSyncBytes(syncBytes, ICVSFile.UNKNOWN);
		file.checkedIn(null, true);
		// Update the project and verify that the file gets removed
		updateProject(copy, null, false);
		assertEquals(project, copy);
		
	}
	
	public void testMergeWithTrailingLineFeeds() throws CoreException, IOException {
		IProject project = createProject("testFileConflict", new String[] { "file1.txt"});
		// Set the contents of file1.txt to ensure proper merging 
		// Ensure there is a trailing LF
		setContentsAndEnsureModified(project.getFile("file1.txt"), "line1" + eol + "line2" + eol + "line3" + eol);
		commitProject(project);
		
		// Checkout and modify a copy
		IProject copy = checkoutCopy(project, "-copy");
		appendText(copy.getFile("file1.txt"), "line0" + eol, true);
		commitProject(copy);
		
		// Modify the original in a non-conflicting way
		setContentsAndEnsureModified(project.getFile("file1.txt"), "line1" + eol + "line2" + eol + "line2.5" + eol + "line3" + eol);
		
		// Update and ensure the contents are what we expect
		updateProject(project, null, false);
		assertTrue("File contents are not correct after merge", compareContent(
				new ByteArrayInputStream(("line0" + eol + "line1" + eol + "line2" + eol + "line2.5" + eol + "line3" + eol).getBytes()), 
						project.getFile("file1.txt").getContents()));
	}
	
	public void testRevertToBaseHeadTag() throws CoreException, IOException{
		IProject project = createProject("testRevertToBase", new String[] {"file1.txt"});
		setContentsAndEnsureModified(project.getFile("file1.txt"), "line1" + eol + "line2" + eol + "line3" + eol);
		commitProject(project);
		IProject copy = checkoutCopy(project, "-copy");
		appendText(project.getFile("file1.txt"), "line0" + eol, true);
		updateProject(project, CVSTag.BASE, true);
		assertTrue("File has changed after revert to base",
				compareContent(new ByteArrayInputStream(("line1" + eol + "line2" + eol + "line3" + eol).getBytes()),
						project.getFile("file1.txt").getContents()));
		assertEquals(getRepositoryProvider(copy), getRepositoryProvider(project), false, true);
	}
	
	private CVSTeamProvider getRepositoryProvider(IProject project) {
		return (CVSTeamProvider)RepositoryProvider.getProvider(project);
	}

	public void testRevertToBaseNonHeadTag() throws CoreException, IOException{
		IProject project = createProject("testRevertToBaseNonHead", new String[] {"file1.txt"});
		setContentsAndEnsureModified(project.getFile("file1.txt"), "line1" + eol + "line2" + eol);
		commitProject(project);
		tagProject(project, new CVSTag("testtag", CVSTag.BRANCH), true);
		appendText(project.getFile("file1.txt"), "line3" + eol, true);
		commitProject(project);
		IProject copy = checkoutCopy(project, "-copy");
		appendText(copy.getFile("file1.txt"), "line0" + eol, true);
		updateProject(copy, CVSTag.BASE, true);
		assertTrue("File has changed after revert to base",
				compareContent(new ByteArrayInputStream(("line3" + eol + "line1" + eol + "line2" + eol).getBytes()),
						copy.getFile("file1.txt").getContents()));
		assertEquals(getRepositoryProvider(project), getRepositoryProvider(copy), false, true);
	}
	
	public void testSwitchTagForModifiedFile()throws CoreException, IOException {
		// it's a similar scenario as in https://bugs.eclipse.org/bugs/show_bug.cgi?id=192392
		// create a project
		IProject project = createProject("testSwitchTagForModifiedFile", new String[] {"file"});
		commitProject(project);
		// tag it
		CVSTag tag = new CVSTag("A", CVSTag.BRANCH);
		tagProject(project, tag, true);
		// modify a file
		appendText(project.getFile("file"), "changed in head" + eol, false);
		// switch to the tag
		updateProject(project, tag, false);
		ICVSFile cvsFile = CVSWorkspaceRoot.getCVSFileFor(project.getFile("file"));
		// we expect the file to have the same tag used when switching 
		assertEquals(tag, cvsFile.getSyncInfo().getTag());
	}
}

