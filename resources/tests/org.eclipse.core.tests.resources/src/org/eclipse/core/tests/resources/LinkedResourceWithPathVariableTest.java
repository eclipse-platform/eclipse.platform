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
 *     Martin Oberhuber (Wind River) - testImportWrongLineEndings() for bug [210664]
 *     Serge Beauchamp (Freescale Semiconductor) - [229633] Add Project Path Variables
 *******************************************************************************/

package org.eclipse.core.tests.resources;

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertDoesNotExistInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInFileSystem;
import static org.eclipse.core.tests.resources.ResourceTestUtil.assertExistsInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.compareContent;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.util.ArrayList;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.tests.harness.FileSystemHelper;

/**
 * This class extends <code>LinkedResourceTest</code> in order to use
 * randomly generated locations that are always variable-based.
 * TODO: add tests specific to linking resources using path variables (then
 * removing the variable, change the variable value, etc)
 */
public class LinkedResourceWithPathVariableTest extends LinkedResourceTest {

	private final static String VARIABLE_NAME = "ROOT";
	private final static String PROJECT_VARIABLE_NAME = "PROOT";
	private final static String PROJECT_RELATIVE_VARIABLE_NAME = "RELATIVE_PROOT";
	private final static String PROJECT_RELATIVE_VARIABLE_VALUE = "${PROOT}";
	private final ArrayList<IPath> toDelete = new ArrayList<>();
	private IFileStore toSetWritable = null;

	@Override
	protected void setUp() throws Exception {
		IPath base = super.getRandomLocation();
		toDelete.add(base);
		getWorkspace().getPathVariableManager().setValue(VARIABLE_NAME, base);
		base = super.getRandomLocation();
		toDelete.add(base);
		super.setUp();
		existingProject.getPathVariableManager().setValue(PROJECT_VARIABLE_NAME, base);
		existingProject.getPathVariableManager().setValue(PROJECT_RELATIVE_VARIABLE_NAME,
				IPath.fromPortableString(PROJECT_RELATIVE_VARIABLE_VALUE));
	}

	@Override
	protected void tearDown() throws Exception {
		if (toSetWritable != null) {
			IFileInfo info = toSetWritable.fetchInfo();
			info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
			toSetWritable.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());
			toSetWritable = null;
		}
		getWorkspace().getPathVariableManager().setValue(VARIABLE_NAME, null);
		IPath[] paths = toDelete.toArray(new IPath[toDelete.size()]);
		toDelete.clear();
		for (IPath path : paths) {
			Workspace.clear(path.toFile());
		}
		super.tearDown();
	}

	/**
	 * Copy file "inStore" into file "outStore", converting line endings as follows:
	 * <ul>
	 *   <li>DOS, or MAC (CR or CRLF) into UNIX (LF)</li>
	 *   <li>UNIX (LF) into DOS (CRLF)</li>
	 * </ul>
	 * @param inStore handle to an existing text file to convert
	 * @param outStore handle to non-existing file which will be written
	 */
	protected void convertLineEndings(IFileStore inStore, IFileStore outStore, IProgressMonitor mon) throws CoreException, IOException {
		InputStream is = null;
		OutputStream os = null;
		try {
			is = inStore.openInputStream(EFS.NONE, mon);
			os = outStore.openOutputStream(EFS.NONE, new NullProgressMonitor());
			int prevb = 0;
			int ib = is.read();
			while (ib >= 0) {
				switch (ib) {
					case '\r' :
						os.write('\n');
						break;
					case '\n' :
						if (prevb != '\r') { /* not converted already */
							os.write('\r');
							os.write('\n');
						}
						break;
					default :
						os.write(ib);
						break;
				}
				prevb = ib;
				ib = is.read();
			}
		} finally {
			if (is != null) {
				is.close();
			}
			if (os != null) {
				os.close();
			}
		}
	}

	@Override
	public IPath getRandomLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		//low order bits are current time, high order bits are static counter
		IPath parent = IPath.fromOSString(VARIABLE_NAME);
		IPath path = FileSystemHelper.computeRandomLocation(parent);
		while (pathVars.resolvePath(path).toFile().exists()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// ignore
			}
			path = FileSystemHelper.computeRandomLocation(parent);
		}
		toDelete.add(pathVars.resolvePath(path));
		return path;
	}

	public IPath getRandomProjectLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		// low order bits are current time, high order bits are static counter
		IPath parent = IPath.fromOSString(PROJECT_VARIABLE_NAME);
		IPath path = FileSystemHelper.computeRandomLocation(parent);
		while (pathVars.resolvePath(path).toFile().exists()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// ignore
			}
			path = FileSystemHelper.computeRandomLocation(parent);
		}
		toDelete.add(pathVars.resolvePath(path));
		return path;
	}

	public IPath getRandomRelativeProjectLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		// low order bits are current time, high order bits are static counter
		IPath parent = IPath.fromOSString(PROJECT_RELATIVE_VARIABLE_NAME);
		IPath path = FileSystemHelper.computeRandomLocation(parent);
		while (pathVars.resolvePath(path).toFile().exists()) {
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// ignore
			}
			path = FileSystemHelper.computeRandomLocation(parent);
		}
		toDelete.add(pathVars.resolvePath(path));
		return path;
	}

	@Override
	protected IPath resolve(IPath path) {
		return getWorkspace().getPathVariableManager().resolvePath(path);
	}

	@Override
	protected URI resolve(URI uri) {
		return getWorkspace().getPathVariableManager().resolveURI(uri);
	}

	public void testProjectResolution() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();
		IPath value = manager.getValue(PROJECT_VARIABLE_NAME);
		IPath relativeValue = manager.getValue(PROJECT_RELATIVE_VARIABLE_NAME);

		assertTrue("1.0", !value.equals(relativeValue));

		IPath resolvedValue = manager.resolvePath(value);
		assertTrue("1.1", value.equals(resolvedValue));

		IPath resolvedRelativeValue = manager.resolvePath(relativeValue);
		assertTrue("1.2", !relativeValue.equals(resolvedRelativeValue));

		assertTrue("1.3", resolvedValue.equals(resolvedRelativeValue));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testFileVariableRemoved() throws Exception {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		file.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		// removes the variable - the location will be undefined (null)
		manager.setValue(VARIABLE_NAME, null);
		assertExistsInWorkspace(file);

		//refresh local - should not fail or make the link disappear
		file.refreshLocal(IResource.DEPTH_ONE, createTestMonitor());
		file.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertExistsInWorkspace(file);

		// try to change resource's contents
		// Resource has no-defined location - should fail
		assertThrows(CoreException.class, () -> file.setContents(getContents("new contents"), IResource.NONE, null));

		assertExistsInWorkspace(file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		manager.setValue(VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem(file);
		// the contents must be the original ones
		assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testFileProjectVariableRemoved() throws Exception {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		file.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		// removes the variable - the location will be undefined (null)
		manager.setValue(PROJECT_VARIABLE_NAME, null);
		assertExistsInWorkspace(file);

		// refresh local - should not fail or make the link disappear
		file.refreshLocal(IResource.DEPTH_ONE, createTestMonitor());
		file.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertExistsInWorkspace(file);

		// try to change resource's contents
		// Resource has no-defined location - should fail
		assertThrows(CoreException.class, () -> file.setContents(getContents("new contents"), IResource.NONE, null));

		assertExistsInWorkspace(file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null
		// location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		manager.setValue(PROJECT_VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem(file);
		// the contents must be the original ones
		assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * moved to a new project.
	 * This is a regression test for bug 266679
	 */
	public void testMoveFileToDifferentProject() throws Exception {

		IFile file = existingProjectInSubDirectory.getFile("my_link");

		// creates a variable-based location
		IPath variableBasedLocation = null;
		IPath targetPath = existingProjectInSubDirectory.getLocation().removeLastSegments(1).append("outside.txt");
		if (!targetPath.toFile().exists()) {
			targetPath.toFile().createNewFile();
		}
		toDelete.add(targetPath);

		variableBasedLocation = convertToRelative(targetPath, file, true, null);

		IPath resolvedPath = URIUtil.toPath(file.getPathVariableManager().resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);

		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		IFile newFile = nonExistingFileInExistingFolder;
		file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		assertExistsInWorkspace(newFile);
		assertTrue("3,2", !newFile.getLocation().equals(newFile.getRawLocation()));
		assertEquals("3,3", newFile.getLocation(), resolvedPath);
	}

	private IPath convertToRelative(IPath path, IResource res, boolean force, String variableHint) throws CoreException {
		return URIUtil.toPath(res.getPathVariableManager().convertToRelative(URIUtil.toURI(path), force, variableHint));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location that is
	 * relative to PROJECT_LOC is moved to a different project.
	 * This is a regression test for bug 266679
	 */
	public void testPROJECT_LOC_MoveFileToDifferentProject() throws Exception {

		String[] existingVariables = nonExistingFileInExistingFolder.getProject().getPathVariableManager().getPathVariableNames();
		for (String existingVariable : existingVariables) {
			try {
				nonExistingFileInExistingFolder.getProject().getPathVariableManager().setValue(existingVariable, null);
			} catch (CoreException e) {
			}
		}
		IFile file = existingProjectInSubDirectory.getFile("my_link2");

		// creates a variable-based location
		IPath variableBasedLocation = null;
		IPath targetPath = existingProjectInSubDirectory.getLocation().removeLastSegments(3).append("outside.txt");
		if (!targetPath.toFile().exists()) {
			targetPath.toFile().createNewFile();
		}
		toDelete.add(targetPath);

		existingProjectInSubDirectory.getPathVariableManager().setValue("P_RELATIVE",
				IPath.fromPortableString("${PARENT-3-PROJECT_LOC}"));
		variableBasedLocation = IPath.fromPortableString("P_RELATIVE/outside.txt");

		IPath resolvedPath = existingProjectInSubDirectory.getPathVariableManager().resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);

		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		IFile newFile = nonExistingFileInExistingFolder;
		file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		assertExistsInWorkspace(newFile);
		IPath newLocation = newFile.getLocation();
		assertTrue("3,2", !newLocation.equals(newFile.getRawLocation()));
		IPath newRawLocation = newFile.getRawLocation();
		/* we cannot test the value of the location since the test machines generate an incorrect value
		IPath newValue = newFile.getProject().getPathVariableManager().getValue("P_RELATIVE");
		assertEquals("3,3", Path.fromPortableString("${PARENT-1-PROJECT_LOC}/sub"), newValue);
		*/
		assertTrue("3,4", newRawLocation.equals(variableBasedLocation));
		assertTrue("3,5", newLocation.equals(resolvedPath));
	}

	/**
	 * Tests a scenario where a linked file location is
	 * is moved to a new project.
	 */
	public void testMoveFileProjectVariable() throws CoreException {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		IPath resolvedPath = manager.resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		file.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		IFile newFile = nonExistingFileInExistingFolder;
		// removes the variable - the location will be undefined (null)
		file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		assertExistsInWorkspace(newFile);
		assertTrue("3,2", !newFile.getLocation().equals(newFile.getRawLocation()));
		assertTrue("3,3", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,4", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,5", newFile.getLocation().equals(resolvedPath));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testMoveFileToNewProjectProjectVariable() throws CoreException {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomRelativeProjectLocation();

		IPath resolvedPath = manager.resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		file.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		IFile newFile = nonExistingFileInOtherExistingProject;
		// moves the variable - the location will be undefined (null)
		file.move(newFile.getFullPath(), IResource.SHALLOW, createTestMonitor());
		assertExistsInWorkspace(newFile);
		assertTrue("3,2", !newFile.getLocation().equals(newFile.getRawLocation()));
		assertTrue("3,3", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,4", newFile.getLocation().equals(resolvedPath));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testFileProjectRelativeVariableRemoved() throws Exception {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(PROJECT_RELATIVE_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomRelativeProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		file.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		// removes the variable - the location will be undefined (null)
		manager.setValue(PROJECT_RELATIVE_VARIABLE_NAME, null);
		assertExistsInWorkspace(file);

		// refresh local - should not fail or make the link disappear
		file.refreshLocal(IResource.DEPTH_ONE, createTestMonitor());
		file.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());

		assertExistsInWorkspace(file);

		// try to change resource's contents
		// Resource has no-defined location - should fail
		assertThrows(CoreException.class, () -> file.setContents(getContents("new contents"), IResource.NONE, null));

		assertExistsInWorkspace(file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null
		// location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		manager.setValue(PROJECT_RELATIVE_VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem(file);
		// the contents must be the original ones
		assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
	}

	/**
	 * Tests a scenario where a variable used in a linked folder location is
	 * removed.
	 */
	public void testFolderVariableRemoved() throws CoreException {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IPath existingValue = manager.getValue(VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(folder);

		folder.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		childFile.create(getRandomContents(), IResource.NONE, createTestMonitor());
		childFile.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(folder);
		assertExistsInWorkspace(childFile);
		assertExistsInFileSystem(folder);
		assertExistsInFileSystem(childFile);

		// removes the variable - the location will be undefined (null)
		manager.setValue(VARIABLE_NAME, null);
		assertExistsInWorkspace(folder);

		//refresh local - should not fail but should cause link's children to disappear
		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(folder);
		assertDoesNotExistInWorkspace(childFile);

		//try to copy a file to the folder
		IFile destination = folder.getFile(existingFileInExistingProject.getName());
		assertThrows(CoreException.class,
				() -> existingFileInExistingProject.copy(destination.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("3.6", !destination.exists());

		//try to create a sub-file
		assertThrows(CoreException.class, () -> destination.create(getRandomContents(), IResource.NONE, createTestMonitor()));

		//try to create a sub-folder
		IFolder subFolder = folder.getFolder("SubFolder");
		assertThrows(CoreException.class, () -> subFolder.create(IResource.NONE, true, createTestMonitor()));

		// try to change resource's contents
		// Resource has no-defined location - should fail
		assertThrows(CoreException.class,
				() -> childFile.setContents(getContents("new contents"), IResource.NONE, null));

		assertExistsInWorkspace(folder);
		// the location is null
		assertNull("4.2", folder.getLocation());

		// re-creates the variable with its previous value
		manager.setValue(VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(folder);
		assertNotNull("6.1", folder.getLocation());
		assertExistsInFileSystem(folder);
		assertDoesNotExistInWorkspace(childFile);
		assertExistsInFileSystem(childFile);

		// refresh should recreate the child
		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(folder);
		assertExistsInWorkspace(childFile);
	}

	/**
	 * Tests importing a project with a Linked Resource and Path Variable,
	 * where the line endings in the .project file do not match the local
	 * Platform.
	 * The .project file must not be modified on import, especially if it
	 * is marked read-only.
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=210664">Bug 210664</a>.
	 */
	public void testImportWrongLineEndings_Bug210664() throws Exception {
		// Choose a project to work on
		IProject proj = existingProject;
		IPath randomLocationWithPathVariable = getRandomLocation();
		IFileStore projStore = EFS.getStore(proj.getLocationURI());

		// Don't run this test if we cannot set a file read-only
		if ((projStore.getFileSystem().attributes() & EFS.ATTRIBUTE_READ_ONLY) == 0) {
			return;
		}

		// Create a linked resource with a non-existing path variable
		IFolder folder = proj.getFolder("SOME_LINK");
		folder.createLink(randomLocationWithPathVariable, IResource.ALLOW_MISSING_LOCAL, null);

		// Close the project, and convert line endings
		IFileStore projFile = projStore.getChild(".project");
		proj.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, createTestMonitor());
		IFileStore projNew = projStore.getChild(".project.new");
		convertLineEndings(projFile, projNew, createTestMonitor());

		// Set the project read-only
		projNew.move(projFile, EFS.OVERWRITE, createTestMonitor());
		IFileInfo info = projFile.fetchInfo(EFS.NONE, createTestMonitor());
		info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
		projFile.putInfo(info, EFS.SET_ATTRIBUTES, createTestMonitor());
		toSetWritable = projFile; /* for cleanup */

		// Bug 210664: Open project with wrong line endings and non-existing path
		// variable
		proj.create(null);
		proj.open(IResource.NONE, createTestMonitor());
	}

	/**
	 * Tests a scenario where a variable used in a linked folder location is
	 * removed.
	 */
	public void testFolderProjectVariableRemoved() throws CoreException {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(folder);

		folder.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		childFile.create(getRandomContents(), IResource.NONE, createTestMonitor());
		childFile.setContents(getContents("contents for a file"), IResource.FORCE, null);

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(folder);
		assertExistsInWorkspace(childFile);
		assertExistsInFileSystem(folder);
		assertExistsInFileSystem(childFile);

		// removes the variable - the location will be undefined (null)
		manager.setValue(PROJECT_VARIABLE_NAME, null);
		assertExistsInWorkspace(folder);

		// refresh local - should not fail but should cause link's children to
		// disappear
		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(folder);
		assertDoesNotExistInWorkspace(childFile);

		// try to copy a file to the folder
		IFile destination = folder.getFile(existingFileInExistingProject.getName());
		assertThrows(CoreException.class,
				() -> existingFileInExistingProject.copy(destination.getFullPath(), IResource.NONE, createTestMonitor()));
		assertTrue("3.6", !destination.exists());

		// try to create a sub-file
		assertThrows(CoreException.class, () -> destination.create(getRandomContents(), IResource.NONE, createTestMonitor()));

		// try to create a sub-folder
		IFolder subFolder = folder.getFolder("SubFolder");
		assertThrows(CoreException.class, () -> subFolder.create(IResource.NONE, true, createTestMonitor()));

		// try to change resource's contents
		// Resource has no-defined location - should fail
		assertThrows(CoreException.class,
				() -> childFile.setContents(getContents("new contents"), IResource.NONE, null));

		assertExistsInWorkspace(folder);
		// the location is null
		assertNull("4.2", folder.getLocation());

		// re-creates the variable with its previous value
		manager.setValue(PROJECT_VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(folder);
		assertNotNull("6.1", folder.getLocation());
		assertExistsInFileSystem(folder);
		assertDoesNotExistInWorkspace(childFile);
		assertExistsInFileSystem(childFile);

		// refresh should recreate the child
		folder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		assertExistsInWorkspace(folder);
		assertExistsInWorkspace(childFile);
	}

	/**
	 * Tests scenario where links are relative to undefined variables
	 */
	public void testUndefinedVariable() throws CoreException {
		IPath folderLocation = IPath.fromOSString("NOVAR/folder");
		IPath fileLocation = IPath.fromOSString("NOVAR/abc.txt");
		IFile testFile = existingProject.getFile("UndefinedVar.txt");
		IFolder testFolder = existingProject.getFolder("UndefinedVarTest");

		//should fail to create links
		assertThrows(CoreException.class, () -> testFile.createLink(fileLocation, IResource.NONE, createTestMonitor()));
		assertThrows(CoreException.class, () -> testFolder.createLink(folderLocation, IResource.NONE, createTestMonitor()));

		//validate method should return warning
		assertTrue("1.2", getWorkspace().validateLinkLocation(testFolder, folderLocation).getSeverity() == IStatus.WARNING);
		assertTrue("1.3", getWorkspace().validateLinkLocation(testFile, fileLocation).getSeverity() == IStatus.WARNING);

		//should succeed with ALLOW_MISSING_LOCAL
		testFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		testFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());

		//copy should fail
		IPath copyFileDestination = existingProject.getFullPath().append("CopyFileDest");
		IPath copyFolderDestination = existingProject.getFullPath().append("CopyFolderDest");

		assertThrows(CoreException.class, () -> testFile.copy(copyFileDestination, IResource.NONE, createTestMonitor()));
		assertThrows(CoreException.class, () -> testFolder.copy(copyFolderDestination, IResource.NONE, createTestMonitor()));

		//move should fail
		IPath moveFileDestination = existingProject.getFullPath().append("MoveFileDest");
		IPath moveFolderDestination = existingProject.getFullPath().append("MoveFolderDest");

		assertThrows(CoreException.class, () -> testFile.move(moveFileDestination, IResource.NONE, createTestMonitor()));
		assertThrows(CoreException.class, () -> testFolder.move(moveFolderDestination, IResource.NONE, createTestMonitor()));

		//refresh local should succeed
		testFile.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		testFolder.refreshLocal(IResource.DEPTH_INFINITE, createTestMonitor());
		testFile.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		testFolder.refreshLocal(IResource.DEPTH_ZERO, createTestMonitor());
		existingProject.refreshLocal(IResource.NONE, createTestMonitor());

		//renaming the project shallow is ok
		IProject project = testFolder.getProject();
		IProjectDescription desc = project.getDescription();
		desc.setName("moveDest");
		project.move(desc, IResource.SHALLOW | IResource.FORCE, createTestMonitor());

		//delete should succeed
		testFile.delete(IResource.NONE, createTestMonitor());
		testFolder.delete(IResource.NONE, createTestMonitor());
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * changed.
	 */
	public void testVariableChanged() throws Exception {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IPath existingValue = manager.getValue(VARIABLE_NAME);

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		file.setContents(getContents("contents for a file"), IResource.FORCE, createTestMonitor());

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		// changes the variable value - the file location will change
		IPath newLocation = super.getRandomLocation();
		toDelete.add(newLocation);
		manager.setValue(VARIABLE_NAME, newLocation);

		// try to change resource's contents
		// Resource was out of sync - should not be able to change
		CoreException exception = assertThrows(CoreException.class,
				() -> file.setContents(getContents("new contents"), IResource.NONE, createTestMonitor()));
		assertEquals("3.1", IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		assertExistsInWorkspace(file);
		// the location is different - does not exist anymore
		assertDoesNotExistInFileSystem(file);

		// successfully changes resource's contents (using IResource.FORCE)
		file.setContents(getContents("contents in different location"), IResource.FORCE, createTestMonitor());

		// now the file exists in a different location
		assertExistsInFileSystem(file);

		// its location must have changed reflecting the variable change
		IPath expectedNewLocation = manager.resolvePath(variableBasedLocation);
		IPath actualNewLocation = file.getLocation();
		assertEquals("4.2", expectedNewLocation, actualNewLocation);

		// its contents are as just set
		assertTrue("4.3", compareContent(file.getContents(), getContents("contents in different location")));

		// clean-up
		removeFromFileSystem(file);

		// restore the previous value
		manager.setValue(VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);
		// the contents must be the original ones
		assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * changed.
	 */
	public void testProjectVariableChanged() throws Exception {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);

		file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, createTestMonitor());
		file.setContents(getContents("contents for a file"), IResource.FORCE, createTestMonitor());

		// now the file exists in both workspace and file system
		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);

		// changes the variable value - the file location will change
		IPath newLocation = super.getRandomLocation();
		toDelete.add(newLocation);
		manager.setValue(PROJECT_VARIABLE_NAME, newLocation);

		// try to change resource's contents
		// Resource was out of sync - should not be able to change
		CoreException exception = assertThrows(CoreException.class,
				() -> file.setContents(getContents("new contents"), IResource.NONE, createTestMonitor()));
		assertEquals("3.1", IResourceStatus.OUT_OF_SYNC_LOCAL, exception.getStatus().getCode());

		assertExistsInWorkspace(file);
		// the location is different - does not exist anymore
		assertDoesNotExistInFileSystem(file);

		// successfully changes resource's contents (using IResource.FORCE)
		file.setContents(getContents("contents in different location"), IResource.FORCE, createTestMonitor());

		// now the file exists in a different location
		assertExistsInFileSystem(file);

		// its location must have changed reflecting the variable change
		IPath expectedNewLocation = manager.resolvePath(variableBasedLocation);
		IPath actualNewLocation = file.getLocation();
		assertEquals("4.2", expectedNewLocation, actualNewLocation);

		// its contents are as just set
		assertTrue("4.3", compareContent(file.getContents(), getContents("contents in different location")));

		// clean-up
		removeFromFileSystem(file);

		// restore the previous value
		manager.setValue(PROJECT_VARIABLE_NAME, existingValue);

		assertExistsInWorkspace(file);
		assertExistsInFileSystem(file);
		// the contents must be the original ones
		assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * changed.
	 */
	//	public void testGetPathVariable() {
	//		final IPathVariableManager manager = existingProject.getPathVariableManager();
	//		IPathVariable variable = manager.getPathVariable("PROJECT_LOC");
	//		assertTrue("1.0", variable != null);
	//		assertTrue("1.1", variable.isReadOnly());
	//		assertEquals("1.2", null, variable.getVariableHints());
	//
	//		variable = manager.getPathVariable("PROJECT_LOC_does_not_exist");
	//		assertTrue("2.0", variable == null);
	//
	//		variable = manager.getPathVariable("PARENT");
	//		assertTrue("3.0", variable != null);
	//		assertTrue("3.1", variable.isReadOnly());
	//		Object[] extensions = variable.getVariableHints();
	//		assertTrue("3.2", extensions == null);
	//
	//		try {
	//			IPath newLocation = super.getRandomLocation();
	//			toDelete.add(newLocation);
	//			manager.setValue(PROJECT_VARIABLE_NAME, newLocation);
	//		} catch (CoreException e) {
	//			fail("4.1", e);
	//		}
	//		variable = manager.getPathVariable(PROJECT_VARIABLE_NAME);
	//		assertTrue("4.0", variable != null);
	//		assertTrue("4.1", !variable.isReadOnly());
	//		assertEquals("4.2", null, variable.getVariableHints());
	//	}

	/**
	 * Test Bug 288880 - Redundant path variables generated when converting some linked resources to path variable-relative
	 */
	public void testNonRedundentPathVariablesGenerated() throws Exception {
		IFile file = existingProjectInSubDirectory.getFile("my_link");

		IPathVariableManager pathVariableManager = existingProjectInSubDirectory.getPathVariableManager();

		// creates a variable-based location
		IPath variableBasedLocation = null;
		IPath targetPath = existingProjectInSubDirectory.getLocation().removeLastSegments(1).append("outside.txt");
		if (!targetPath.toFile().exists()) {
			targetPath.toFile().createNewFile();
		}
		toDelete.add(targetPath);

		variableBasedLocation = convertToRelative(targetPath, file, true, null);
		IPath resolvedPath = URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);
		assertEquals("3.1", targetPath, resolvedPath);

		variableBasedLocation = convertToRelative(targetPath, file, true, null);

		resolvedPath = URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace(file);
		assertEquals("5.1", targetPath, resolvedPath);
	}

	public void testConvertToUserEditableFormat() {
		IPathVariableManager pathVariableManager = existingProject.getPathVariableManager();

		String[][] table = { // format: {internal-format, user-editable-format [, internal-format-reconverted]
				{"C:\\foo\\bar", "C:\\foo\\bar", "C:/foo/bar"}, //
				{"C:/foo/bar", "C:/foo/bar"}, //
				{"VAR/foo/bar", "VAR/foo/bar"}, //
				{"${VAR}/foo/bar", "${VAR}/foo/bar"}, //
				{"${VAR}/../foo/bar", "${VAR}/../foo/bar", "${PARENT-1-VAR}/foo/bar"}, //
				{"${PARENT-1-VAR}/foo/bar", "${VAR}/../foo/bar"}, //
				{"${PARENT-0-VAR}/foo/bar", "${VAR}/foo/bar", "${VAR}/foo/bar"}, //
				{"${PARENT-VAR}/foo/bar", "${PARENT-VAR}/foo/bar"}, //
				{"${PARENT-2}/foo/bar", "${PARENT-2}/foo/bar"}, //
				{"${PARENT}/foo/bar", "${PARENT}/foo/bar"}, //
				{"${PARENT-2-VAR}/foo/bar", "${VAR}/../../foo/bar"}, //
				{"${PARENT-2-VAR}/foo/${PARENT-4-BAR}", "${VAR}/../../foo/${BAR}/../../../.."}, //
				{"${PARENT-2-VAR}/foo${PARENT-4-BAR}", "${VAR}/../../foo${BAR}/../../../.."}, //
				{"${PARENT-2-VAR}/${PARENT-4-BAR}/foo", "${VAR}/../../${BAR}/../../../../foo"}, //
				{"${PARENT-2-VAR}/f${PARENT-4-BAR}/oo", "${VAR}/../../f${BAR}/../../../../oo"} //
		};

		for (int i = 0; i < table.length; i++) {
			String result = pathVariableManager.convertToUserEditableFormat(toOS(table[i][0]), false);
			assertEquals("1." + i, toOS(table[i][1]), result);
			String original = pathVariableManager.convertFromUserEditableFormat(result, false);
			assertEquals("2." + i, toOS(table[i].length == 2 ? table[i][0] : table[i][2]), original);
		}

		String[][] tableLocationFormat = { // format: {internal-format, user-editable-format [, internal-format-reconverted]
				{"C:\\foo\\bar", "C:\\foo\\bar", "C:/foo/bar"}, //
				{"C:/foo/bar", "C:/foo/bar"}, //
				{"VAR/foo/bar", "VAR/foo/bar"}, //
				{"${VAR}/../foo/bar", "${VAR}/../foo/bar", "PARENT-1-VAR/foo/bar"}, //
				{"PARENT-1-VAR/foo/bar", "VAR/../foo/bar"}, //
				{"PARENT-0-VAR/foo/bar", "VAR/foo/bar", "VAR/foo/bar"}, //
				{"PARENT-VAR/foo/bar", "PARENT-VAR/foo/bar"}, //
				{"PARENT-2/foo/bar", "PARENT-2/foo/bar"}, //
				{"PARENT/foo/bar", "PARENT/foo/bar"}, //
				{"PARENT-2-VAR/foo/bar", "VAR/../../foo/bar"}, //
				{"PARENT-2-VAR/foo/PARENT-4-BAR", "VAR/../../foo/PARENT-4-BAR"}, //
				{"PARENT-2-VAR/fooPARENT-4-BAR", "VAR/../../fooPARENT-4-BAR"}, //
				{"PARENT-2-VAR/PARENT-4-BAR/foo", "VAR/../../PARENT-4-BAR/foo"}, //
				{"PARENT-2-VAR/fPARENT-4-BAR/oo", "VAR/../../fPARENT-4-BAR/oo"}, //
				{"/foo/bar", "/foo/bar"}, //
		};

		for (int i = 0; i < table.length; i++) {
			String result = pathVariableManager.convertToUserEditableFormat(toOS(tableLocationFormat[i][0]), true);
			assertEquals("3." + i, toOS(tableLocationFormat[i][1]), result);
			String original = pathVariableManager.convertFromUserEditableFormat(result, true);
			assertEquals("4." + i, toOS(tableLocationFormat[i].length == 2 ? tableLocationFormat[i][0] : tableLocationFormat[i][2]), original);
		}
	}

	private String toOS(String path) {
		return path.replace('/', File.separatorChar);
	}

	/**
	 * Regression for Bug 305676 - Selecting PARENT_LOC as the relative path variable in the ImportTypeDialog causes an error
	 */
	public void testPrefixVariablesAreNotConfused() {
		URI uri = nonExistingFileInExistingFolder.getPathVariableManager().getURIValue("PARENT");
		assertEquals("1.0", uri, null);
		uri = nonExistingFileInExistingFolder.getPathVariableManager().getURIValue("PARENT_LOC");
		assertNotNull("1.1", uri);
	}

	/**
	* Regression test for Bug 338185 - Core Resource Variable Resolvers that do not specify the 'class' attribute are not displayed
	*/
	public void test338185() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();
		String[] variables = manager.getPathVariableNames();
		boolean found = false;

		for (String variable : variables) {
			if (variable.equals("Test338185")) {
				found = true;
			}
		}
		assertTrue(found);
	}
}
