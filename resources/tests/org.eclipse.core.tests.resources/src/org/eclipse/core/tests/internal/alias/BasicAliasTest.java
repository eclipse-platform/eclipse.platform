/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *******************************************************************************/
package org.eclipse.core.tests.internal.alias;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.harness.FileSystemHelper.deleteAfterExecution;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.buildResources;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomContentsStream;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.findAvailableDevices;
import static org.eclipse.core.tests.resources.ResourceTestUtil.getFileStore;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForRefresh;
import static org.eclipse.core.tests.resources.ResourceTestUtil.wrapInCanonicalIPath;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.IFileSystem;
import org.eclipse.core.internal.filesystem.NullFileStore;
import org.eclipse.core.internal.filesystem.NullFileSystem;
import org.eclipse.core.internal.resources.AliasManager;
import org.eclipse.core.internal.resources.Folder;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.Resource;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform.OS;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;

/**
 * Tests basic API methods in the face of aliased resources, and ensures that
 * nothing is ever out of sync.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class BasicAliasTest {

	//resource handles (p=project, f=folder, l=file)
	private IProject pNoOverlap;
	private IProject pOverlap;
	private IProject pLinked;
	private IFolder fOverlap;
	private IFile lChildOverlap;
	private IFile lOverlap;
	private IFolder fLinked;
	private IFolder fLinkOverlap1;
	private IFolder fLinkOverlap2;
	private IFile lLinked;
	private IFile lChildLinked;
	private IPath linkOverlapLocation;

	static class BatFS extends NullFileSystem {
		static final BatFS instance = new BatFS();

		private BatFS() {
			super();
			initialize("batfs");
		}
	}

	static class BatFSURI extends NullFileStore {
		private final URI uri;

		public BatFSURI(String uri) {
			this(from(uri));
		}

		private static URI from(String uri) {
			try {
				return new URI(uri);
			} catch (URISyntaxException e) {
				throw new IllegalArgumentException(e);
			}
		}

		public BatFSURI(URI uri) {
			super(IPath.fromOSString("/not/used"));
			this.uri = uri;
		}

		@Override
		public IFileSystem getFileSystem() {
			return BatFS.instance;
		}

		@Override
		public URI toURI() {
			return uri;
		}
	}

	/**
	 * Asserts that the two given resources are duplicates in the file system.
	 * Asserts that both have same location, and same members.  Also asserts
	 * that both resources are in sync with the file system.  The resource names
	 * in the tree may be different.  The resources may not necessarily exist.
	 */
	public void assertOverlap(IResource resource1, IResource resource2) throws CoreException {
		assertThat(resource1.getLocation()).isEqualTo(resource2.getLocation());
		assertThat(resource1).matches(it -> it.isSynchronized(IResource.DEPTH_ZERO), "is synchronized");
		assertThat(resource2).matches(it -> it.isSynchronized(IResource.DEPTH_ZERO), "is synchronized");

		IResource[] children1 = null;
		IResource[] children2 = null;
		children1 = getSortedChildren(resource1);
		children2 = getSortedChildren(resource2);
		assertThat(children1).as("number of children").hasSameSizeAs(children2);
		for (int i = 0; i < children2.length; i++) {
			assertOverlap(children1[i], children2[i]);
		}
	}

	/**
	 * Returns the children of the given resource, sorted in a consistent
	 * alphabetical order.
	 */
	private IResource[] getSortedChildren(IResource resource) throws CoreException {
		if (!(resource instanceof IContainer container)) {
			return new IResource[0];
		}
		IResource[] children = container.members();
		Arrays.sort(children, (arg0, arg1) -> arg0.getFullPath().toString().compareTo(arg1.getFullPath().toString()));
		return children;
	}

	@BeforeEach
	public void setUp(@TempDir Path tempDirectory) throws Exception {
		IWorkspaceRoot root = getWorkspace().getRoot();
		//project with no overlap
		pNoOverlap = root.getProject("NoOverlap");
		createInWorkspace(pNoOverlap);
		createInWorkspace(buildResources(pNoOverlap, new String[] {"/1/", "/1/1", "/1/2", "/2/", "/2/1"}));

		//project with overlap
		pOverlap = root.getProject("Overlap");
		createInWorkspace(pOverlap);
		fOverlap = pOverlap.getFolder("fOverlap");
		IFolder f2 = pOverlap.getFolder("F2");
		lOverlap = f2.getFile("lOverlap");
		lChildOverlap = fOverlap.getFile("lChildOverlap");
		createInWorkspace(new IResource[] {fOverlap, f2, lOverlap, lChildOverlap});
		//create some other random child elements
		createInWorkspace(buildResources(pOverlap, new String[] {"/1/", "/1/1", "/1/2"}));
		createInWorkspace(buildResources(f2, new String[] {"/1/", "/1/1", "/1/2"}));
		createInWorkspace(buildResources(fOverlap, new String[] {"/1/", "/1/1", "/1/2"}));

		//create links
		pLinked = root.getProject("LinkProject");
		createInWorkspace(pLinked);
		fLinked = pLinked.getFolder("LinkedFolder");
		fLinkOverlap1 = pLinked.getFolder("LinkOverlap1");
		fLinkOverlap2 = pLinked.getFolder("LinkOverlap2");
		lLinked = pLinked.getFile("LinkedFile");
		lChildLinked = fLinked.getFile(lChildOverlap.getName());
		fLinked.createLink(fOverlap.getLocation(), IResource.NONE, null);
		lLinked.createLink(lOverlap.getLocation(), IResource.NONE, null);
		createInWorkspace(lChildLinked);
		createInWorkspace(buildResources(pLinked, new String[] {"/a/", "/a/a", "/a/b"}));
		createInWorkspace(buildResources(fLinked, new String[] {"/a/", "/a/a", "/a/b"}));

		linkOverlapLocation = wrapInCanonicalIPath(tempDirectory);
		linkOverlapLocation.toFile().mkdirs();

		fLinkOverlap1.createLink(linkOverlapLocation, IResource.NONE, null);
		fLinkOverlap2.createLink(linkOverlapLocation, IResource.NONE, null);
	}

	/**
	 * This tests regression of bug 32785.  In this bug, moving a linked folder,
	 * then copying a linked folder, resulted in the alias table having a stale entry
	 */
	@Test
	public void testBug32785(@TempDir Path tempDirectory) throws CoreException, IOException {
		IProject project = pNoOverlap;
		IFolder link = project.getFolder("Source");
		IFile child = link.getFile("Child.txt");
		IPath location = wrapInCanonicalIPath(tempDirectory);
		link.createLink(location, IResource.NONE, createTestMonitor());
		createInWorkspace(child, createRandomString());
		// move the link (rename)
		IFolder movedLink = project.getFolder("MovedLink");
		link.move(movedLink.getFullPath(), IResource.SHALLOW, createTestMonitor());
		assertFalse(link.exists());
		assertTrue(movedLink.exists());
		assertEquals(location, movedLink.getLocation());
		assertTrue(movedLink.isSynchronized(IResource.DEPTH_INFINITE));

		// now copy the moved link
		IFolder copiedLink = project.getFolder("CopiedLink");
		movedLink.copy(copiedLink.getFullPath(), IResource.SHALLOW, createTestMonitor());
		assertFalse(link.exists());
		assertTrue(movedLink.exists());
		assertTrue(copiedLink.exists());
		assertEquals(location, movedLink.getLocation());
		assertEquals(location, copiedLink.getLocation());
		assertTrue(movedLink.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue(copiedLink.isSynchronized(IResource.DEPTH_INFINITE));
	}

	/**
	 * Regression test for bug 156082.  A project has aliases to multiple
	 * other projects, but the other projects don't overlap each other.  I.e.,
	 * Project Top overlaps Sub1 and Sub2, but Sub1 and Sub2 do not overlap each other.
	 */
	@Test
	public void testBug156082() throws CoreException {
		IProject top = getWorkspace().getRoot().getProject("Bug156082_Top");
		IProject sub1 = getWorkspace().getRoot().getProject("Bug156082_Sub1");
		IProject sub2 = getWorkspace().getRoot().getProject("Bug156082_Sub2");
		createInWorkspace(top);
		IProjectDescription desc1 = getWorkspace().newProjectDescription(sub1.getName());
		desc1.setLocation(top.getLocation().append(sub1.getName()));
		IProjectDescription desc2 = getWorkspace().newProjectDescription(sub2.getName());
		desc2.setLocation(top.getLocation().append(sub2.getName()));
		sub1.create(desc1, createTestMonitor());
		sub1.open(createTestMonitor());
		sub2.create(desc2, createTestMonitor());
		sub2.open(createTestMonitor());
		IFile sub2File = sub2.getFile("file.txt");
		IFile topFile = top.getFolder(sub2.getName()).getFile(sub2File.getName());
		createInWorkspace(sub2File, createRandomString());
		assertTrue(topFile.exists());
	}

	/**
	 * Regression test for bug 198571.  Device ids should be respected by the comparator
	 * used in the locations map of AliasManager.
	 */
	@Test
	public void testBug198571() throws Exception {
		assumeTrue(OS.isWindows(), "only relevant on Windows");

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		assumeFalse(devices[0] == null || devices[1] == null, "only executable if at least two volumes are present");

		String location = createUniqueString();
		IProject testProject1 = getWorkspace().getRoot().getProject(location + "1");
		IProject testProject2 = getWorkspace().getRoot().getProject(location + "2");

		// the projects have the same segments but different id
		IProjectDescription desc1 = getWorkspace().newProjectDescription(testProject1.getName());
		IPath location1 = IPath.fromOSString(devices[0] + location);
		assertFalse(location1.toFile().exists());
		desc1.setLocation(location1);
		IProjectDescription desc2 = getWorkspace().newProjectDescription(testProject2.getName());
		IPath location2 = IPath.fromOSString(devices[1] + location);
		assertFalse(location2.toFile().exists());
		desc2.setLocation(location2);

		testProject1.create(desc1, createTestMonitor());
		testProject1.open(createTestMonitor());
		testProject2.create(desc2, createTestMonitor());
		testProject2.open(createTestMonitor());

		final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
		// force AliasManager to restart (simulates a shutdown/startup)
		aliasManager.startup(null);

		// new folder in one of the projects
		IFolder folder = testProject2.getFolder("NewFolder");
		createInFileSystem(folder);

		testProject2.refreshLocal(IResource.DEPTH_INFINITE, null);
		IResource[] resources = aliasManager.computeAliases(folder, ((Folder) folder).getStore());
		assertNull(resources);
	}

	private void replaceProject(IProject project, URI newLocation) throws CoreException, IOException {
		IProjectDescription projectDesc = project.getDescription();
		projectDesc.setLocationURI(newLocation);
		project.move(projectDesc, IResource.REPLACE, null);
	}

	/* Bug570896 */
	@Test
	public void testCompareUriAuthorityDistinct() throws URISyntaxException {
		// AliasManager requires that different authority (server:port) are distinct
		// (doesnt actually matter if compare yields +1 or -1 for different values)

		assertComparedDistinct(List.of( //
				"batfs://authority1/path?query#fragment", //
				"batfs://authority2/path?query#fragment" //
		));
	}

	private void assertComparedDistinct(List<String> urisStrings) {
		List<BatFSURI> batfsList = urisStrings.stream().map(BatFSURI::new).toList();
		for (BatFSURI bu1 : batfsList) {
			for (BatFSURI bu2 : batfsList) {
				if (!bu1.equals(bu2)) {
					assertNotEquals(0, bu1.compareTo(bu2));
				}
			}
		}
	}

	/* Bug570896 */
	@Test
	public void testCompareUriPathHierarchy() throws URISyntaxException {
		// AliasManager requires that the path is ordered such path < path%00
		// and that any subpath of path yields path < subpath < path%00
		// for example "foo" < "foo/zzz" < "foo%00"

		assertPreOrdered(List.of( //
				"batfs://Server/Volume:segment1a", //
				"batfs://Server/Volume:segment1a/a", //
				"batfs://Server/Volume:segment1a/a%00", //
				"batfs://Server/Volume:segment1a/segment2a", //
				"batfs://Server/Volume:segment1a/segment2a%00", //
				"batfs://Server/Volume:segment1a/segment2b", //
				"batfs://Server/Volume:segment1a/segment2b%00", //
				"batfs://Server/Volume:segment1a%00", //
				"batfs://Server/Volume:segment2a", //
				"batfs://Server/Volume:segment2a/b", //
				"batfs://Server/Volume:segment2a/segment2a", //
				"batfs://Server/Volume:segment2a/segment2b", //
				"batfs://Server/Volume:segment2a%00" //
		));
	}

	/* Bug570896 */
	@Test
	public void testCompareUriOctets() throws URISyntaxException {
		// uri.getPath() will normalize the octets
		assertPreOrdered(List.of( //
				"http://Server/Volume:A", //
				"http://Server/Volume:%41", // hex 41==Ascii A
				"http://Server/Volume:A", //
				"http://Server/Volume:%41" //
		));
	}

	/* Bug570896 */
	@Test
	public void testCompareUriCase() throws URISyntaxException {
		// its not a requirement but a back compatibility that the order is
		// case sensitive even on case insensitive OSes:
		assertComparedDistinct(List.of( //
				"http://Server/Volume:a", //
				"http://Server/Volume:A" // A>a
		));
	}

	/* Bug570896 */
	@Test
	public void testCompareUriFragment() throws URISyntaxException {
		// fragments should NOT be distinct! Even though they might not be used:
		assertPreOrdered(List.of( //
				"bats://authority/path?query#fragment1", //
				"bats://authority/path?query#fragment2", //
				"bats://authority/path?query#fragment1", //
				"bats://authority/path?query#fragment2" //
		));
	}

	private void assertPreOrdered(List<String> urisStrings) {
		List<BatFSURI> batfsList = urisStrings.stream().map(BatFSURI::new).toList();
		// stable sort:
		List<BatFSURI> sorted = batfsList.stream().sorted(IFileStore::compareTo).toList();
		// proof sort order did not change
		assertEquals(batfsList, sorted);
	}

	@Test
	public void testBug256837(@TempDir Path tempDirectory) throws Throwable {
		final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
		//force AliasManager to restart (simulates a shutdown/startup)
		aliasManager.startup(null);

		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject p1 = root.getProject(createUniqueString());
		IProject p2 = root.getProject(createUniqueString());
		createInWorkspace(new IResource[] {p1, p2});

		IFileStore tempStore = getFileStore(tempDirectory);
		tempStore.mkdir(EFS.NONE, createTestMonitor());
		IFolder link2TempFolder = p1.getFolder("link2TempFolder");
		deleteAfterExecution(p2.getLocation().toPath(), () -> {
			replaceProject(p2, WrapperFileSystem.getWrappedURI(p2.getLocationURI()));
			link2TempFolder.createLink(tempStore.toURI(), IResource.NONE, createTestMonitor());
			// change the location of p2 project to the temp folder
			replaceProject(p2, tempStore.toURI());
			return null;
		});

		// now p2 and link2TempFolder should be aliases
		IResource[] resources = aliasManager.computeAliases(link2TempFolder, ((Folder) link2TempFolder).getStore());
		assertThat(resources).containsExactly(p2);

		resources = aliasManager.computeAliases(p2, ((Project) p2).getStore());
		assertThat(resources).containsExactly(link2TempFolder);
	}

	@Test
	public void testBug258987(@TempDir Path tempDirectory) throws Exception {
		// Create the directory to which you will link. The directory needs a single file.
		IFileStore dirStore = getFileStore(tempDirectory);
		dirStore.mkdir(EFS.NONE, createTestMonitor());
		assertTrue(dirStore.fetchInfo().exists());
		assertTrue(dirStore.fetchInfo().isDirectory());

		IFileStore childStore = dirStore.getChild("child");
		createInFileSystem(childStore);
		assertTrue(childStore.fetchInfo().exists());

		// Create and open the first project. Project links to the directory.
		IProject project1 = ResourcesPlugin.getWorkspace().getRoot().getProject("project1");
		IFolder folder1 = null;
		project1.create(createTestMonitor());
		project1.open(createTestMonitor());
		folder1 = project1.getFolder("subdir");
		folder1.createLink(dirStore.toURI(), IResource.REPLACE, createTestMonitor());

		// Create and open the second project. It also links to the directory.
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject("project2");
		IFolder folder2 = null;
		project2.create(createTestMonitor());
		project2.open(createTestMonitor());
		folder2 = project2.getFolder("subdir");
		folder2.createLink(dirStore.toURI(), IResource.REPLACE, createTestMonitor());

		// Close the second project.
		project2.close(createTestMonitor());

		// Since project2 is closed, folder2 should not be an alias for folder1 anymore
		IResource[] resources = ((Workspace) getWorkspace()).getAliasManager().computeAliases(folder1, ((Folder) folder1).getStore());
		assertNull(resources);
	}

	@Test
	public void testCloseOpenProject() throws CoreException {
		// close the project and make sure aliases in that project are no longer updated
		pOverlap.close(createTestMonitor());
		IFile linkFile = fLinked.getFile("ChildFile.txt");
		linkFile.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		IFile closedFile = fOverlap.getFile(linkFile.getName());
		assertFalse(closedFile.exists());
		pOverlap.open(IResource.NONE, createTestMonitor());
		// should a refresh be needed when a project has been changed while it was
		// closed?
		pOverlap.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertTrue(closedFile.exists());
	}

	/**
	 * Tests adding a file to a duplicate region by copying.
	 */
	@Test
	public void testCopyFile() throws CoreException {
		IFile sourceFile = pNoOverlap.getFile("CopySource");
		createInWorkspace(sourceFile);

		// file in linked folder
		IFile linkDest = fLinked.getFile("CopyDestination");
		IFile overlapDest = fOverlap.getFile(linkDest.getName());

		sourceFile.copy(linkDest.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue(linkDest.exists());
		assertTrue(overlapDest.exists());
		assertOverlap(linkDest, overlapDest);

		linkDest.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkDest.exists());
		assertFalse(overlapDest.exists());
		assertOverlap(linkDest, overlapDest);

		// duplicate file
		linkDest = lLinked;
		overlapDest = lOverlap;
		// first delete the file, then copy it back
		overlapDest.delete(IResource.NONE, createTestMonitor());
		// the link will still exist, but the location won't
		assertTrue(linkDest.exists());
		assertFalse(overlapDest.exists());
		assertFalse(linkDest.getLocation().toFile().exists());
		assertEquals(linkDest.getLocation(), overlapDest.getLocation());

		sourceFile.copy(overlapDest.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue(linkDest.exists());
		assertTrue(overlapDest.exists());
		assertOverlap(linkDest, overlapDest);

		// file in duplicate folder
		linkDest = fLinked.getFile("CopyDestination");
		overlapDest = fOverlap.getFile(linkDest.getName());

		sourceFile.copy(overlapDest.getFullPath(), IResource.NONE, createTestMonitor());
		assertTrue(linkDest.exists());
		assertTrue(overlapDest.exists());
		assertOverlap(linkDest, overlapDest);

		overlapDest.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkDest.exists());
		assertFalse(overlapDest.exists());
		assertOverlap(linkDest, overlapDest);
	}

	@Test
	public void testCopyFolder() throws CoreException {
		IFolder source = pNoOverlap.getFolder("CopyFolder");
		createInWorkspace(source);

		IFolder destFolder1 = fLinkOverlap1.getFolder(source.getName());
		IFolder destFolder2 = fLinkOverlap2.getFolder(source.getName());
		IResource[] allDest = new IResource[] {destFolder1, destFolder2};
		assertDoesNotExistInWorkspace(allDest);

		//copy to dest 1
		source.copy(destFolder1.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(allDest);

		destFolder2.delete(IResource.NONE, createTestMonitor());

		//copy to dest 2
		source.copy(destFolder2.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(allDest);

		destFolder1.delete(IResource.NONE, createTestMonitor());
	}

	/**
	 * Test copying a linked folder into a child of its alias.
	 */
	@Test
	public void testCopyToChild() throws CoreException {
		//copying link to child should fail
		IFolder copyDest = fLinkOverlap1.getFolder("CopyDest");
		assertThrows(CoreException.class,
				() -> fLinkOverlap2.copy(copyDest.getFullPath(), IResource.NONE, createTestMonitor()));
		//moving link to child should fail
		assertThrows(CoreException.class,
				() -> fLinkOverlap2.move(copyDest.getFullPath(), IResource.NONE, createTestMonitor()));

		//copy to self should fail
		IFolder copyDest2 = fLinkOverlap2.getFolder(copyDest.getName());
		copyDest.create(IResource.NONE, true, createTestMonitor());

		assertThrows(CoreException.class, () -> copyDest.copy(copyDest2.getFullPath(), IResource.NONE, createTestMonitor()));
		//moving link to self should fail
		assertThrows(CoreException.class, () -> copyDest.move(copyDest2.getFullPath(), IResource.NONE, createTestMonitor()));
	}

	@Test
	public void testCreateDeleteFile() throws CoreException {
		// file in linked folder
		lChildLinked.delete(IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
		lChildLinked.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.delete(IResource.NONE, createTestMonitor());
		assertEquals(lLinked.getLocation(), lOverlap.getLocation());

		assertFalse(lOverlap.exists());
		assertFalse(lOverlap.getLocation().toFile().exists());
		assertTrue(lOverlap.isSynchronized(IResource.DEPTH_INFINITE));

		// now the linked resource will still exist but its local contents won't
		assertTrue(lLinked.exists());
		assertFalse(lLinked.getLocation().toFile().exists());
		assertTrue(lLinked.isSynchronized(IResource.DEPTH_INFINITE));
		assertThrows(CoreException.class, () -> lLinked.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor()));

		lOverlap.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lLinked, lOverlap);

		//file in duplicate folder
		lChildOverlap.delete(IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
		lChildOverlap.create(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
	}

	@Test
	public void testCreateDeleteFolder() throws CoreException {
		// folder in overlapping project
		fOverlap.delete(IResource.NONE, createTestMonitor());
		// linked resources don't disappear on deletion of underlying file
		assertTrue(fLinked.exists());
		assertFalse(fLinked.getLocation().toFile().exists());

		fOverlap.create(IResource.NONE, true, createTestMonitor());
		assertOverlap(fOverlap, fLinked);

		//linked folder
		fLinked.delete(IResource.NONE, createTestMonitor());
		// deleting a link should not delete file system contents
		assertTrue(fOverlap.exists());
		assertTrue(fOverlap.getLocation().toFile().exists());

		fLinked.createLink(fOverlap.getLocation(), IResource.NONE, createTestMonitor());
		assertOverlap(fOverlap, fLinked);

		//child of linked folders
		IFolder child1 = fLinkOverlap1.getFolder("LinkChild");
		IFolder child2 = fLinkOverlap2.getFolder(child1.getName());

		child1.create(IResource.NONE, true, createTestMonitor());
		assertOverlap(child1, child2);
		child1.delete(IResource.NONE, createTestMonitor());
		assertFalse(child1.exists());
		assertFalse(child2.exists());

		child2.create(IResource.NONE, true, createTestMonitor());
		assertOverlap(child1, child2);
		child2.delete(IResource.NONE, createTestMonitor());
		assertFalse(child1.exists());
		assertFalse(child2.exists());
	}

	@Test
	public void testCreateDeleteLink() throws CoreException {
		IFolder folder = pNoOverlap.getFolder("folder");
		IFile folderChild = folder.getFile("Child.txt");
		IFolder link = pLinked.getFolder("FolderLink");
		IFile linkChild = link.getFile(folderChild.getName());
		createInWorkspace(folder);
		createInWorkspace(folderChild);
		link.createLink(folder.getLocationURI(), IResource.NONE, createTestMonitor());
		assertTrue(linkChild.exists());
		// manipulate file below overlapping folder and make sure alias under link is
		// updated
		folderChild.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
		createInWorkspace(folderChild);
		assertTrue(linkChild.exists());

		link.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
	}

	@Test
	public void testDeepLink() throws Exception {
		IFolder folder = pNoOverlap.getFolder("folder");
		IFile folderChild = folder.getFile("Child.txt");
		final IFolder linkParent = pLinked.getFolder("LinkParent");
		IFolder link = linkParent.getFolder("FolderLink");
		IFile linkChild = link.getFile(folderChild.getName());
		createInWorkspace(new IResource[] {folder, folderChild, linkParent});
		link.createLink(folder.getLocationURI(), IResource.NONE, createTestMonitor());
		assertTrue(linkChild.exists());

		// manipulate file below overlapping folder and make sure alias under link is
		// updated
		folderChild.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
		createInWorkspace(folderChild);
		assertTrue(linkChild.exists());

		link.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
		link.createLink(folder.getLocationURI(), IResource.NONE, createTestMonitor());

		// close and reopen the project and ensure the alias is still updated
		link.getProject().close(createTestMonitor());
		link.getProject().open(createTestMonitor());

		folderChild.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
		createInWorkspace(folderChild);
		assertTrue(linkChild.exists());

		final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
		// force AliasManager to restart (simulates a shutdown/startup)
		aliasManager.startup(null);

		folderChild.delete(IResource.NONE, createTestMonitor());
		assertFalse(linkChild.exists());
		createInWorkspace(folderChild);
		assertTrue(linkChild.exists());

		// delete the project that contains the links
		IFile fileInLinkedProject = pLinked.getFile("fileInLinkedProject.txt");
		createInFileSystem(((Resource) fileInLinkedProject).getStore());
		// failure expected here because it is out of sync
		assertThrows(CoreException.class, () -> getWorkspace().getRoot().delete(IResource.NONE, createTestMonitor()));
		waitForRefresh();

		// ensure aliases are gone (bug 144458)
		final IResource[] aliases = aliasManager.computeAliases(folder, ((Folder) folder).getStore());
		assertNull(aliases, "Unexpected aliases: " + Arrays.toString(aliases));
	}

	@Test
	public void testCreateOpenProject() throws CoreException {
		//test creating a project whose location is within an existing link
		IProject newProject = getWorkspace().getRoot().getProject("createOpenProject");
		IProjectDescription desc = getWorkspace().newProjectDescription(newProject.getName());
		desc.setLocationURI(fLinkOverlap1.getLocationURI());
		newProject.create(desc, createTestMonitor());
		newProject.open(createTestMonitor());

		//.project file should now exist in link
		IFile linkChild = fLinkOverlap1.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue(linkChild.exists());

	}

	@Test
	public void testDeleteLink() throws CoreException {
		//test deletion of a link that overlaps a project location
		IFolder linkOnProject = pLinked.getFolder("LinkOnProject");
		linkOnProject.createLink(pOverlap.getLocation(), IResource.NONE, createTestMonitor());

		linkOnProject.delete(IResource.NONE, createTestMonitor());

		// deletion of a link should not delete the project that it overlaps
		assertFalse(linkOnProject.exists());
		assertTrue(pOverlap.exists());
		assertTrue(fOverlap.exists());
	}

	/**
	 * Tests deletion of a project whose location on disk contains
	 * the location of another project.  The nested project should
	 * be deleted automatically in this case.
	 */
	@Test
	public void testDeleteProjectUnderProject() throws CoreException {
		IProject parent = getWorkspace().getRoot().getProject("parent");
		IProject child = getWorkspace().getRoot().getProject("child");
		createInWorkspace(parent);

		IProjectDescription childDesc = getWorkspace().newProjectDescription(child.getName());
		childDesc.setLocation(parent.getLocation().append(child.getName()));
		child.create(childDesc, createTestMonitor());
		child.open(createTestMonitor());

		IFolder childDirInParent = parent.getFolder(child.getName());
		IFile childProjectFileInParent = childDirInParent.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue(childDirInParent.exists());
		assertTrue(childProjectFileInParent.exists());

		//now delete the child and ensure the resources in the parent are gone
		child.delete(IResource.NONE, createTestMonitor());
		assertFalse(childDirInParent.exists());
		assertFalse(childProjectFileInParent.exists());

		//recreate the child and ensure resources in parent are there
		child.create(childDesc, createTestMonitor());
		child.open(createTestMonitor());
		assertTrue(childDirInParent.exists());
		assertTrue(childProjectFileInParent.exists());

		//delete the parent and ensure child is also deleted
		parent.delete(IResource.NONE, createTestMonitor());

		assertFalse(parent.exists());
		assertFalse(child.exists());
		assertFalse(childDirInParent.exists());
		assertFalse(childProjectFileInParent.exists());
	}

	@Test
	public void testDeleteProjectContents() throws CoreException {
		//delete the overlapping project - it should delete the children of the linked folder
		//but leave the actual links intact in the resource tree
		pOverlap.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, createTestMonitor());
		assertDoesNotExistInWorkspace(new IResource[] { pOverlap, fOverlap, lOverlap, lChildOverlap, lChildLinked });
		assertDoesNotExistInFileSystem(
				new IResource[] { pOverlap, fOverlap, lOverlap, lChildOverlap, lChildLinked, lLinked, fLinked });
		assertExistsInWorkspace(new IResource[] { pLinked, fLinked, lLinked });
	}

	@Test
	public void testFileAppendContents() throws CoreException {
		//linked file
		lLinked.appendContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lLinked, lOverlap);

		//file in linked folder
		lChildLinked.appendContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.appendContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.appendContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
	}

	@Test
	public void testFileSetContents() throws CoreException {
		//linked file
		lLinked.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lLinked, lOverlap);

		//file in linked folder
		lChildLinked.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.setContents(createRandomContentsStream(), IResource.NONE, createTestMonitor());
		assertOverlap(lChildLinked, lChildOverlap);
	}

	/**
	 * Tests moving a file into and out of an overlapping area (similar to
	 * creation/deletion).
	 */
	@Test
	public void testMoveFile() throws CoreException {
		IFile destination = pNoOverlap.getFile("MoveDestination");
		//file in linked folder
		lChildLinked.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertDoesNotExistInWorkspace(lChildLinked);
		assertDoesNotExistInWorkspace(lChildOverlap);
		assertExistsInWorkspace(destination);
		assertOverlap(lChildLinked, lChildOverlap);
		assertTrue(lChildLinked.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue(destination.isSynchronized(IResource.DEPTH_INFINITE));

		destination.move(lChildLinked.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(lChildLinked);
		assertExistsInWorkspace(lChildOverlap);
		assertDoesNotExistInWorkspace(destination);
		assertOverlap(lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertDoesNotExistInWorkspace(lOverlap);
		assertExistsInWorkspace(lLinked);
		assertDoesNotExistInFileSystem(lLinked);
		assertExistsInWorkspace(destination);
		assertEquals(lLinked.getLocation(), lOverlap.getLocation());
		assertTrue(lLinked.isSynchronized(IResource.DEPTH_INFINITE));

		destination.move(lOverlap.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(lLinked);
		assertExistsInWorkspace(lOverlap);
		assertDoesNotExistInWorkspace(destination);
		assertOverlap(lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.move(destination.getFullPath(), IResource.NONE, createTestMonitor());
		assertDoesNotExistInWorkspace(lChildLinked);
		assertDoesNotExistInWorkspace(lChildOverlap);
		assertExistsInWorkspace(destination);
		assertOverlap(lChildLinked, lChildOverlap);

		destination.move(lChildOverlap.getFullPath(), IResource.NONE, createTestMonitor());
		assertExistsInWorkspace(lChildLinked);
		assertExistsInWorkspace(lChildOverlap);
		assertDoesNotExistInWorkspace(destination);
		assertOverlap(lChildLinked, lChildOverlap);
	}

}
