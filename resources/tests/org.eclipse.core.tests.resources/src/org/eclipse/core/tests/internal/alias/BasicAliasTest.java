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

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertThrows;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

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
import org.eclipse.core.runtime.Path;
import org.eclipse.core.tests.internal.filesystem.wrapper.WrapperFileSystem;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * Tests basic API methods in the face of aliased resources, and ensures that
 * nothing is ever out of sync.
 */
public class BasicAliasTest extends ResourceTest {
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
			super(new Path("/not/used"));
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
	public void assertOverlap(String message, IResource resource1, IResource resource2) {
		String errMsg = message + resource1.getFullPath().toString();
		assertEquals(errMsg + "(location)", resource1.getLocation(), resource2.getLocation());
		assertTrue(errMsg + "(sync)", resource1.isSynchronized(IResource.DEPTH_ZERO));
		assertTrue(errMsg + "(sync)", resource2.isSynchronized(IResource.DEPTH_ZERO));

		IResource[] children1 = null;
		IResource[] children2 = null;
		try {
			children1 = getSortedChildren(resource1);
			children2 = getSortedChildren(resource2);
		} catch (CoreException e) {
			fail(errMsg, e);
		}
		assertEquals(errMsg + "(child count)", children1.length, children2.length);
		for (int i = 0; i < children2.length; i++) {
			assertOverlap(message, children1[i], children2[i]);
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

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		IWorkspaceRoot root = getWorkspace().getRoot();
		//project with no overlap
		pNoOverlap = root.getProject("NoOverlap");
		ensureExistsInWorkspace(pNoOverlap, true);
		ensureExistsInWorkspace(buildResources(pNoOverlap, new String[] {"/1/", "/1/1", "/1/2", "/2/", "/2/1"}), true);

		//project with overlap
		pOverlap = root.getProject("Overlap");
		ensureExistsInWorkspace(pOverlap, true);
		fOverlap = pOverlap.getFolder("fOverlap");
		IFolder f2 = pOverlap.getFolder("F2");
		lOverlap = f2.getFile("lOverlap");
		lChildOverlap = fOverlap.getFile("lChildOverlap");
		ensureExistsInWorkspace(new IResource[] {fOverlap, f2, lOverlap, lChildOverlap}, true);
		//create some other random child elements
		ensureExistsInWorkspace(buildResources(pOverlap, new String[] {"/1/", "/1/1", "/1/2"}), true);
		ensureExistsInWorkspace(buildResources(f2, new String[] {"/1/", "/1/1", "/1/2"}), true);
		ensureExistsInWorkspace(buildResources(fOverlap, new String[] {"/1/", "/1/1", "/1/2"}), true);

		//create links
		pLinked = root.getProject("LinkProject");
		ensureExistsInWorkspace(pLinked, true);
		fLinked = pLinked.getFolder("LinkedFolder");
		fLinkOverlap1 = pLinked.getFolder("LinkOverlap1");
		fLinkOverlap2 = pLinked.getFolder("LinkOverlap2");
		lLinked = pLinked.getFile("LinkedFile");
		lChildLinked = fLinked.getFile(lChildOverlap.getName());
		fLinked.createLink(fOverlap.getLocation(), IResource.NONE, null);
		lLinked.createLink(lOverlap.getLocation(), IResource.NONE, null);
		ensureExistsInWorkspace(lChildLinked, true);
		ensureExistsInWorkspace(buildResources(pLinked, new String[] {"/a/", "/a/a", "/a/b"}), true);
		ensureExistsInWorkspace(buildResources(fLinked, new String[] {"/a/", "/a/a", "/a/b"}), true);

		linkOverlapLocation = getRandomLocation();
		linkOverlapLocation.toFile().mkdirs();
		fLinkOverlap1.createLink(linkOverlapLocation, IResource.NONE, null);
		fLinkOverlap2.createLink(linkOverlapLocation, IResource.NONE, null);
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		Workspace.clear(linkOverlapLocation.toFile());
	}

	/**
	 * This tests regression of bug 32785.  In this bug, moving a linked folder,
	 * then copying a linked folder, resulted in the alias table having a stale entry
	 */
	public void testBug32785() throws CoreException {
		IProject project = pNoOverlap;
		IFolder link = project.getFolder("Source");
		IFile child = link.getFile("Child.txt");
		IPath location = getRandomLocation();
		location.toFile().mkdirs();
		try {
			link.createLink(location, IResource.NONE, getMonitor());
			ensureExistsInWorkspace(child, getRandomContents());
			//move the link (rename)
			IFolder movedLink = project.getFolder("MovedLink");
			link.move(movedLink.getFullPath(), IResource.SHALLOW, getMonitor());
			assertFalse("3.0", link.exists());
			assertTrue("3.1", movedLink.exists());
			assertEquals("3.2", location, movedLink.getLocation());
			assertTrue("3.3", movedLink.isSynchronized(IResource.DEPTH_INFINITE));

			//now copy the moved link
			IFolder copiedLink = project.getFolder("CopiedLink");
			movedLink.copy(copiedLink.getFullPath(), IResource.SHALLOW, getMonitor());
			assertFalse("4.0", link.exists());
			assertTrue("4.1", movedLink.exists());
			assertTrue("4.2", copiedLink.exists());
			assertEquals("4.3", location, movedLink.getLocation());
			assertEquals("4.4", location, copiedLink.getLocation());
			assertTrue("4.5", movedLink.isSynchronized(IResource.DEPTH_INFINITE));
			assertTrue("4.6", copiedLink.isSynchronized(IResource.DEPTH_INFINITE));
		} finally {
			Workspace.clear(location.toFile());
		}
	}

	/**
	 * Regression test for bug 156082.  A project has aliases to multiple
	 * other projects, but the other projects don't overlap each other.  I.e.,
	 * Project Top overlaps Sub1 and Sub2, but Sub1 and Sub2 do not overlap each other.
	 */
	public void testBug156082() throws CoreException {
		IProject top = getWorkspace().getRoot().getProject("Bug156082_Top");
		IProject sub1 = getWorkspace().getRoot().getProject("Bug156082_Sub1");
		IProject sub2 = getWorkspace().getRoot().getProject("Bug156082_Sub2");
		ensureExistsInWorkspace(top, true);
		IProjectDescription desc1 = getWorkspace().newProjectDescription(sub1.getName());
		desc1.setLocation(top.getLocation().append(sub1.getName()));
		IProjectDescription desc2 = getWorkspace().newProjectDescription(sub2.getName());
		desc2.setLocation(top.getLocation().append(sub2.getName()));
		sub1.create(desc1, getMonitor());
		sub1.open(getMonitor());
		sub2.create(desc2, getMonitor());
		sub2.open(getMonitor());
		IFile sub2File = sub2.getFile("file.txt");
		IFile topFile = top.getFolder(sub2.getName()).getFile(sub2File.getName());
		ensureExistsInWorkspace(sub2File, getRandomContents());
		assertTrue("1.0", topFile.exists());
	}

	/**
	 * Regression test for bug 198571.  Device ids should be respected by the comparator
	 * used in the locations map of AliasManager.
	 */
	public void testBug198571() {
		if (!isWindows()) {
			return;
		}

		/* look for the adequate environment */
		String[] devices = findAvailableDevices();
		if (devices[0] == null || devices[1] == null) {
			return;
		}

		String location = getUniqueString();
		IProject testProject1 = getWorkspace().getRoot().getProject(location + "1");
		IProject testProject2 = getWorkspace().getRoot().getProject(location + "2");

		// the projects have the same segments but different id
		IProjectDescription desc1 = getWorkspace().newProjectDescription(testProject1.getName());
		IPath location1 = new Path(devices[0] + location);
		assertTrue("0.1", !location1.toFile().exists());
		desc1.setLocation(location1);
		IProjectDescription desc2 = getWorkspace().newProjectDescription(testProject2.getName());
		IPath location2 = new Path(devices[1] + location);
		assertTrue("0.2", !location2.toFile().exists());
		desc2.setLocation(location2);

		try {
			try {
				testProject1.create(desc1, getMonitor());
				testProject1.open(getMonitor());
				testProject2.create(desc2, getMonitor());
				testProject2.open(getMonitor());
			} catch (CoreException e) {
				fail("1.0", e);
			}

			final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
			//force AliasManager to restart (simulates a shutdown/startup)
			aliasManager.startup(null);

			// new folder in one of the projects
			IFolder folder = testProject2.getFolder("NewFolder");
			ensureExistsInFileSystem(folder);

			try {
				testProject2.refreshLocal(IResource.DEPTH_INFINITE, null);
				IResource[] resources = aliasManager.computeAliases(folder, ((Folder) folder).getStore());
				assertNull(resources);
			} catch (CoreException e) {
				fail("2.0", e);
			}
		} finally {
			//make sure we don't leave behind a mess on the test machine
			clear(EFS.getLocalFileSystem().getStore(location1));
			clear(EFS.getLocalFileSystem().getStore(location2));
		}
	}

	private void replaceProject(IProject project, URI newLocation) throws CoreException {
		IProjectDescription projectDesc = project.getDescription();
		projectDesc.setLocationURI(newLocation);
		project.move(projectDesc, IResource.REPLACE, null);
	}

	/* Bug570896 */
	public void testCompareUriAuthorityDistinct() throws URISyntaxException {
		// AliasManager requires that different authority (server:port) are distinct
		// (doesnt actually matter if compare yields +1 or -1 for different values)

		assertComparedDistinct(List.of( //
				"batfs://authority1/path?query#fragment", //
				"batfs://authority2/path?query#fragment" //
		));
	}

	private void assertComparedDistinct(List<String> urisStrings) {
		List<BatFSURI> batfsList = urisStrings.stream().map(BatFSURI::new).collect(Collectors.toList());
		for (BatFSURI bu1 : batfsList) {
			for (BatFSURI bu2 : batfsList) {
				if (!bu1.equals(bu2)) {
					assertNotEquals("1.0", 0, bu1.compareTo(bu2));
				}
			}
		}
	}

	/* Bug570896 */
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
	public void testCompareUriCase() throws URISyntaxException {
		// its not a requirement but a back compatibility that the order is
		// case sensitive even on case insensitive OSes:
		assertComparedDistinct(List.of( //
				"http://Server/Volume:a", //
				"http://Server/Volume:A" // A>a
		));
	}

	/* Bug570896 */
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
		List<BatFSURI> batfsList = urisStrings.stream().map(BatFSURI::new).collect(Collectors.toList());
		// stable sort:
		List<BatFSURI> sorted = batfsList.stream().sorted(IFileStore::compareTo).collect(Collectors.toList());
		// proof sort order did not change
		assertEquals("1.0", batfsList, sorted);
	}

	public void testBug256837() throws CoreException {
		final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
		//force AliasManager to restart (simulates a shutdown/startup)
		aliasManager.startup(null);

		IWorkspaceRoot root = getWorkspace().getRoot();
		IProject p1 = root.getProject(getUniqueString());
		IProject p2 = root.getProject(getUniqueString());
		ensureExistsInWorkspace(new IResource[] {p1, p2}, true);

		IFileStore tempStore = getTempStore();
		tempStore.mkdir(EFS.NONE, getMonitor());

		replaceProject(p2, WrapperFileSystem.getWrappedURI(p2.getLocationURI()));

		IFolder link2TempFolder = p1.getFolder("link2TempFolder");
		link2TempFolder.createLink(tempStore.toURI(), IResource.NONE, getMonitor());

		// change the location of p2 project to the temp folder
		replaceProject(p2, tempStore.toURI());

		// now p2 and link2TempFolder should be aliases
		IResource[] resources = aliasManager.computeAliases(link2TempFolder, ((Folder) link2TempFolder).getStore());
		assertEquals("5.0", 1, resources.length);
		assertEquals("6.0", p2, resources[0]);

		resources = aliasManager.computeAliases(p2, ((Project) p2).getStore());
		assertEquals("7.0", 1, resources.length);
		assertEquals("8.0", link2TempFolder, resources[0]);
	}

	public void testBug258987() throws CoreException {
		// Create the directory to which you will link. The directory needs a single file.
		IFileStore dirStore = getTempStore();
		dirStore.mkdir(EFS.NONE, getMonitor());
		assertTrue("2.0", dirStore.fetchInfo().exists());
		assertTrue("3.0", dirStore.fetchInfo().isDirectory());

		IFileStore childStore = dirStore.getChild("child");
		createFileInFileSystem(childStore);
		assertTrue("4.0", childStore.fetchInfo().exists());

		// Create and open the first project. Project links to the directory.
		IProject project1 = ResourcesPlugin.getWorkspace().getRoot().getProject("project1");
		IFolder folder1 = null;
		project1.create(getMonitor());
		project1.open(getMonitor());
		folder1 = project1.getFolder("subdir");
		folder1.createLink(dirStore.toURI(), IResource.REPLACE, getMonitor());

		// Create and open the second project. It also links to the directory.
		IProject project2 = ResourcesPlugin.getWorkspace().getRoot().getProject("project2");
		IFolder folder2 = null;
		project2.create(getMonitor());
		project2.open(getMonitor());
		folder2 = project2.getFolder("subdir");
		folder2.createLink(dirStore.toURI(), IResource.REPLACE, getMonitor());

		// Close the second project.
		project2.close(getMonitor());

		// Since project2 is closed, folder2 should not be an alias for folder1 anymore
		IResource[] resources = ((Workspace) getWorkspace()).getAliasManager().computeAliases(folder1, ((Folder) folder1).getStore());
		assertNull("8.0", resources);
	}

	public void testCloseOpenProject() throws CoreException {
		// close the project and make sure aliases in that project are no longer updated
		pOverlap.close(getMonitor());
		IFile linkFile = fLinked.getFile("ChildFile.txt");
		linkFile.create(getRandomContents(), IResource.NONE, getMonitor());
		IFile closedFile = fOverlap.getFile(linkFile.getName());
		assertFalse("1.0", closedFile.exists());
		pOverlap.open(IResource.NONE, getMonitor());
		// should a refresh be needed when a project has been changed while it was
		// closed?
		pOverlap.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		assertTrue("1.1", closedFile.exists());
	}

	/**
	 * Tests adding a file to a duplicate region by copying.
	 */
	public void testCopyFile() throws CoreException {
		IFile sourceFile = pNoOverlap.getFile("CopySource");
		ensureExistsInWorkspace(sourceFile, true);

		// file in linked folder
		IFile linkDest = fLinked.getFile("CopyDestination");
		IFile overlapDest = fOverlap.getFile(linkDest.getName());

		sourceFile.copy(linkDest.getFullPath(), IResource.NONE, getMonitor());
		assertTrue("1.1", linkDest.exists());
		assertTrue("1.2", overlapDest.exists());
		assertOverlap("1.3", linkDest, overlapDest);

		linkDest.delete(IResource.NONE, getMonitor());
		assertFalse("1.4", linkDest.exists());
		assertFalse("1.5", overlapDest.exists());
		assertOverlap("1.6", linkDest, overlapDest);

		// duplicate file
		linkDest = lLinked;
		overlapDest = lOverlap;
		// first delete the file, then copy it back
		overlapDest.delete(IResource.NONE, getMonitor());
		// the link will still exist, but the location won't
		assertTrue("2.1", linkDest.exists());
		assertFalse("2.2", overlapDest.exists());
		assertFalse("2.3", linkDest.getLocation().toFile().exists());
		assertEquals("2.4", linkDest.getLocation(), overlapDest.getLocation());

		sourceFile.copy(overlapDest.getFullPath(), IResource.NONE, getMonitor());
		assertTrue("2.4", linkDest.exists());
		assertTrue("2.5", overlapDest.exists());
		assertOverlap("2.6", linkDest, overlapDest);

		// file in duplicate folder
		linkDest = fLinked.getFile("CopyDestination");
		overlapDest = fOverlap.getFile(linkDest.getName());

		sourceFile.copy(overlapDest.getFullPath(), IResource.NONE, getMonitor());
		assertTrue("3.1", linkDest.exists());
		assertTrue("3.2", overlapDest.exists());
		assertOverlap("3.3", linkDest, overlapDest);

		overlapDest.delete(IResource.NONE, getMonitor());
		assertFalse("3.4", linkDest.exists());
		assertFalse("3.5", overlapDest.exists());
		assertOverlap("3.6", linkDest, overlapDest);
	}

	public void testCopyFolder() throws CoreException {
		IFolder source = pNoOverlap.getFolder("CopyFolder");
		ensureExistsInWorkspace(source, true);

		IFolder destFolder1 = fLinkOverlap1.getFolder(source.getName());
		IFolder destFolder2 = fLinkOverlap2.getFolder(source.getName());
		IResource[] allDest = new IResource[] {destFolder1, destFolder2};
		assertDoesNotExistInWorkspace("1.0", allDest);

		//copy to dest 1
		source.copy(destFolder1.getFullPath(), IResource.NONE, getMonitor());
		assertExistsInWorkspace("1.2", allDest);

		destFolder2.delete(IResource.NONE, getMonitor());

		//copy to dest 2
		source.copy(destFolder2.getFullPath(), IResource.NONE, getMonitor());
		assertExistsInWorkspace("1.5", allDest);

		destFolder1.delete(IResource.NONE, getMonitor());
	}

	/**
	 * Test copying a linked folder into a child of its alias.
	 */
	public void testCopyToChild() throws CoreException {
		//copying link to child should fail
		IFolder copyDest = fLinkOverlap1.getFolder("CopyDest");
		assertThrows(CoreException.class,
				() -> fLinkOverlap2.copy(copyDest.getFullPath(), IResource.NONE, getMonitor()));
		//moving link to child should fail
		assertThrows(CoreException.class,
				() -> fLinkOverlap2.move(copyDest.getFullPath(), IResource.NONE, getMonitor()));

		//copy to self should fail
		IFolder copyDest2 = fLinkOverlap2.getFolder(copyDest.getName());
		copyDest.create(IResource.NONE, true, getMonitor());

		assertThrows(CoreException.class, () -> copyDest.copy(copyDest2.getFullPath(), IResource.NONE, getMonitor()));
		//moving link to self should fail
		assertThrows(CoreException.class, () -> copyDest.move(copyDest2.getFullPath(), IResource.NONE, getMonitor()));
	}

	public void testCreateDeleteFile() throws CoreException {
		// file in linked folder
		lChildLinked.delete(IResource.NONE, getMonitor());
		assertOverlap("1.1", lChildLinked, lChildOverlap);
		lChildLinked.create(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("1.2", lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.delete(IResource.NONE, getMonitor());
		assertEquals("2.0", lLinked.getLocation(), lOverlap.getLocation());

		assertFalse("2.1", lOverlap.exists());
		assertFalse("2.2", lOverlap.getLocation().toFile().exists());
		assertTrue("2.3", lOverlap.isSynchronized(IResource.DEPTH_INFINITE));

		// now the linked resource will still exist but its local contents won't
		assertTrue("2.4", lLinked.exists());
		assertFalse("2.5", lLinked.getLocation().toFile().exists());
		assertTrue("2.6", lLinked.isSynchronized(IResource.DEPTH_INFINITE));
		assertThrows(CoreException.class, () -> lLinked.setContents(getRandomContents(), IResource.NONE, getMonitor()));

		lOverlap.create(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("2.8", lLinked, lOverlap);

		//file in duplicate folder
		lChildOverlap.delete(IResource.NONE, getMonitor());
		assertOverlap("1.1", lChildLinked, lChildOverlap);
		lChildOverlap.create(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("1.2", lChildLinked, lChildOverlap);
	}

	public void testCreateDeleteFolder() throws CoreException {
		// folder in overlapping project
		fOverlap.delete(IResource.NONE, getMonitor());
		// linked resources don't disappear on deletion of underlying file
		assertTrue("1.0", fLinked.exists());
		assertFalse("1.1", fLinked.getLocation().toFile().exists());

		fOverlap.create(IResource.NONE, true, getMonitor());
		assertOverlap("1.2", fOverlap, fLinked);

		//linked folder
		fLinked.delete(IResource.NONE, getMonitor());
		// deleting a link should not delete file system contents
		assertTrue("2.0", fOverlap.exists());
		assertTrue("2.1", fOverlap.getLocation().toFile().exists());

		fLinked.createLink(fOverlap.getLocation(), IResource.NONE, getMonitor());
		assertOverlap("1.4", fOverlap, fLinked);

		//child of linked folders
		IFolder child1 = fLinkOverlap1.getFolder("LinkChild");
		IFolder child2 = fLinkOverlap2.getFolder(child1.getName());

		child1.create(IResource.NONE, true, getMonitor());
		assertOverlap("3.0", child1, child2);
		child1.delete(IResource.NONE, getMonitor());
		assertFalse("3.1", child1.exists());
		assertFalse("3.2", child2.exists());

		child2.create(IResource.NONE, true, getMonitor());
		assertOverlap("3.3", child1, child2);
		child2.delete(IResource.NONE, getMonitor());
		assertFalse("3.4", child1.exists());
		assertFalse("3.5", child2.exists());
	}

	public void testCreateDeleteLink() throws CoreException {
		IFolder folder = pNoOverlap.getFolder("folder");
		IFile folderChild = folder.getFile("Child.txt");
		IFolder link = pLinked.getFolder("FolderLink");
		IFile linkChild = link.getFile(folderChild.getName());
		ensureExistsInWorkspace(folder, true);
		ensureExistsInWorkspace(folderChild, true);
		link.createLink(folder.getLocationURI(), IResource.NONE, getMonitor());
		assertTrue("1.0", linkChild.exists());
		// manipulate file below overlapping folder and make sure alias under link is
		// updated
		folderChild.delete(IResource.NONE, getMonitor());
		assertFalse("1.1", linkChild.exists());
		ensureExistsInWorkspace(folderChild, true);
		assertTrue("1.2", linkChild.exists());

		link.delete(IResource.NONE, getMonitor());
		assertFalse("1.3", linkChild.exists());
	}

	public void testDeepLink() throws CoreException {
		IFolder folder = pNoOverlap.getFolder("folder");
		IFile folderChild = folder.getFile("Child.txt");
		final IFolder linkParent = pLinked.getFolder("LinkParent");
		IFolder link = linkParent.getFolder("FolderLink");
		IFile linkChild = link.getFile(folderChild.getName());
		ensureExistsInWorkspace(new IResource[] {folder, folderChild, linkParent}, true);
		link.createLink(folder.getLocationURI(), IResource.NONE, getMonitor());
		assertTrue("1.0", linkChild.exists());

		// manipulate file below overlapping folder and make sure alias under link is
		// updated
		folderChild.delete(IResource.NONE, getMonitor());
		assertFalse("1.1", linkChild.exists());
		ensureExistsInWorkspace(folderChild, true);
		assertTrue("1.2", linkChild.exists());

		link.delete(IResource.NONE, getMonitor());
		assertFalse("1.3", linkChild.exists());
		link.createLink(folder.getLocationURI(), IResource.NONE, getMonitor());

		// close and reopen the project and ensure the alias is still updated
		link.getProject().close(getMonitor());
		link.getProject().open(getMonitor());

		folderChild.delete(IResource.NONE, getMonitor());
		assertFalse("2.1", linkChild.exists());
		ensureExistsInWorkspace(folderChild, true);
		assertTrue("2.2", linkChild.exists());

		final AliasManager aliasManager = ((Workspace) getWorkspace()).getAliasManager();
		// force AliasManager to restart (simulates a shutdown/startup)
		aliasManager.startup(null);

		folderChild.delete(IResource.NONE, getMonitor());
		assertFalse("3.1", linkChild.exists());
		ensureExistsInWorkspace(folderChild, true);
		assertTrue("3.2", linkChild.exists());

		// delete the project that contains the links
		IFile fileInLinkedProject = pLinked.getFile("fileInLinkedProject.txt");
		createFileInFileSystem(((Resource) fileInLinkedProject).getStore(), getRandomContents());
		// failure expected here because it is out of sync
		assertThrows(CoreException.class, () -> getWorkspace().getRoot().delete(IResource.NONE, getMonitor()));
		waitForRefresh();

		// ensure aliases are gone (bug 144458)
		final IResource[] aliases = aliasManager.computeAliases(folder, ((Folder) folder).getStore());
		assertNull("Unexpected aliases: " + Arrays.toString(aliases), aliases);
	}

	public void testCreateOpenProject() throws CoreException {
		//test creating a project whose location is within an existing link
		IProject newProject = getWorkspace().getRoot().getProject("createOpenProject");
		IProjectDescription desc = getWorkspace().newProjectDescription(newProject.getName());
		desc.setLocationURI(fLinkOverlap1.getLocationURI());
		newProject.create(desc, getMonitor());
		newProject.open(getMonitor());

		//.project file should now exist in link
		IFile linkChild = fLinkOverlap1.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue("1.0", linkChild.exists());

	}

	public void testDeleteLink() throws CoreException {
		//test deletion of a link that overlaps a project location
		IFolder linkOnProject = pLinked.getFolder("LinkOnProject");
		linkOnProject.createLink(pOverlap.getLocation(), IResource.NONE, getMonitor());

		linkOnProject.delete(IResource.NONE, getMonitor());

		// deletion of a link should not delete the project that it overlaps
		assertFalse("2.1", linkOnProject.exists());
		assertTrue("2.2", pOverlap.exists());
		assertTrue("2.3", fOverlap.exists());
	}

	/**
	 * Tests deletion of a project whose location on disk contains
	 * the location of another project.  The nested project should
	 * be deleted automatically in this case.
	 */
	public void testDeleteProjectUnderProject() throws CoreException {
		IProject parent = getWorkspace().getRoot().getProject("parent");
		IProject child = getWorkspace().getRoot().getProject("child");
		ensureExistsInWorkspace(parent, true);

		IProjectDescription childDesc = getWorkspace().newProjectDescription(child.getName());
		childDesc.setLocation(parent.getLocation().append(child.getName()));
		child.create(childDesc, getMonitor());
		child.open(getMonitor());

		IFolder childDirInParent = parent.getFolder(child.getName());
		IFile childProjectFileInParent = childDirInParent.getFile(IProjectDescription.DESCRIPTION_FILE_NAME);
		assertTrue("1.0", childDirInParent.exists());
		assertTrue("1.1", childProjectFileInParent.exists());

		//now delete the child and ensure the resources in the parent are gone
		child.delete(IResource.NONE, getMonitor());
		assertFalse("2.0", childDirInParent.exists());
		assertFalse("2.1", childProjectFileInParent.exists());

		//recreate the child and ensure resources in parent are there
		child.create(childDesc, getMonitor());
		child.open(getMonitor());
		assertTrue("3.0", childDirInParent.exists());
		assertTrue("3.1", childProjectFileInParent.exists());

		//delete the parent and ensure child is also deleted
		parent.delete(IResource.NONE, getMonitor());

		assertFalse("4.0", parent.exists());
		assertFalse("4.1", child.exists());
		assertFalse("4.2", childDirInParent.exists());
		assertFalse("4.3", childProjectFileInParent.exists());
	}

	public void testDeleteProjectContents() throws CoreException {
		//delete the overlapping project - it should delete the children of the linked folder
		//but leave the actual links intact in the resource tree
		pOverlap.delete(IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
		assertDoesNotExistInWorkspace("1.1", new IResource[] {pOverlap, fOverlap, lOverlap, lChildOverlap, lChildLinked});
		assertDoesNotExistInFileSystem("1.2", new IResource[] {pOverlap, fOverlap, lOverlap, lChildOverlap, lChildLinked, lLinked, fLinked});
		assertExistsInWorkspace("1.3", new IResource[] {pLinked, fLinked, lLinked});
	}

	public void testFileAppendContents() throws CoreException {
		//linked file
		lLinked.appendContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("1.1", lLinked, lOverlap);

		//file in linked folder
		lChildLinked.appendContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("2.1", lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.appendContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("3.1", lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.appendContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("3.1", lChildLinked, lChildOverlap);
	}

	public void testFileSetContents() throws CoreException {
		//linked file
		lLinked.setContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("1.1", lLinked, lOverlap);

		//file in linked folder
		lChildLinked.setContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("2.1", lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.setContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("3.1", lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.setContents(getRandomContents(), IResource.NONE, getMonitor());
		assertOverlap("3.1", lChildLinked, lChildOverlap);
	}

	/**
	 * Tests moving a file into and out of an overlapping area (similar to
	 * creation/deletion).
	 */
	public void testMoveFile() throws CoreException {
		IFile destination = pNoOverlap.getFile("MoveDestination");
		//file in linked folder
		lChildLinked.move(destination.getFullPath(), IResource.NONE, getMonitor());
		assertDoesNotExistInWorkspace("1.1", lChildLinked);
		assertDoesNotExistInWorkspace("1.2", lChildOverlap);
		assertExistsInWorkspace("1.3", destination);
		assertOverlap("1.4", lChildLinked, lChildOverlap);
		assertTrue("1.5", lChildLinked.isSynchronized(IResource.DEPTH_INFINITE));
		assertTrue("1.6", destination.isSynchronized(IResource.DEPTH_INFINITE));

		destination.move(lChildLinked.getFullPath(), IResource.NONE, getMonitor());
		assertExistsInWorkspace("2.1", lChildLinked);
		assertExistsInWorkspace("2.2", lChildOverlap);
		assertDoesNotExistInWorkspace("2.3", destination);
		assertOverlap("2.4", lChildLinked, lChildOverlap);
		//duplicate file
		lOverlap.move(destination.getFullPath(), IResource.NONE, getMonitor());
		assertDoesNotExistInWorkspace("3.1", lOverlap);
		assertExistsInWorkspace("3.2", lLinked);
		assertDoesNotExistInFileSystem("3.25", lLinked);
		assertExistsInWorkspace("3.3", destination);
		assertEquals("3.4", lLinked.getLocation(), lOverlap.getLocation());
		assertTrue("3.4.1", lLinked.isSynchronized(IResource.DEPTH_INFINITE));

		destination.move(lOverlap.getFullPath(), IResource.NONE, getMonitor());
		assertExistsInWorkspace("3.5", lLinked);
		assertExistsInWorkspace("3.6", lOverlap);
		assertDoesNotExistInWorkspace("3.7", destination);
		assertOverlap("3.8", lLinked, lOverlap);
		//file in duplicate folder
		lChildOverlap.move(destination.getFullPath(), IResource.NONE, getMonitor());
		assertDoesNotExistInWorkspace("3.1", lChildLinked);
		assertDoesNotExistInWorkspace("3.2", lChildOverlap);
		assertExistsInWorkspace("3.3", destination);
		assertOverlap("3.4", lChildLinked, lChildOverlap);

		destination.move(lChildOverlap.getFullPath(), IResource.NONE, getMonitor());
		assertExistsInWorkspace("3.5", lChildLinked);
		assertExistsInWorkspace("3.6", lChildOverlap);
		assertDoesNotExistInWorkspace("3.7", destination);
		assertOverlap("3.8", lChildLinked, lChildOverlap);
	}
}
