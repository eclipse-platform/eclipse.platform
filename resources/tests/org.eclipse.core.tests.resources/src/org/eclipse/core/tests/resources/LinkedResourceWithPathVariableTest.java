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

import java.io.*;
import java.net.URI;
import java.util.ArrayList;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
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
		existingProject.getPathVariableManager().setValue(PROJECT_RELATIVE_VARIABLE_NAME, Path.fromPortableString(PROJECT_RELATIVE_VARIABLE_VALUE));
	}

	@Override
	protected void tearDown() throws Exception {
		if (toSetWritable != null) {
			IFileInfo info = toSetWritable.fetchInfo();
			info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, false);
			toSetWritable.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
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

	/**
	 * @see org.eclipse.core.tests.harness.ResourceTest#getRandomLocation()
	 */
	@Override
	public IPath getRandomLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		//low order bits are current time, high order bits are static counter
		IPath parent = new Path(VARIABLE_NAME);
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

	/**
	 * @see org.eclipse.core.tests.harness.ResourceTest#getRandomLocation()
	 */
	public IPath getRandomProjectLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		// low order bits are current time, high order bits are static counter
		IPath parent = new Path(PROJECT_VARIABLE_NAME);
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

	/**
	 * @see org.eclipse.core.tests.harness.ResourceTest#getRandomLocation()
	 */
	public IPath getRandomRelativeProjectLocation() {
		IPathVariableManager pathVars = getWorkspace().getPathVariableManager();
		// low order bits are current time, high order bits are static counter
		IPath parent = new Path(PROJECT_RELATIVE_VARIABLE_NAME);
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

	/**
	 * @see LinkedResourceTest#resolve(org.eclipse.core.runtime.IPath)
	 */
	@Override
	protected IPath resolve(IPath path) {
		return getWorkspace().getPathVariableManager().resolvePath(path);
	}

	/**
	 * @see LinkedResourceTest#resolve(java.net.URI)
	 */
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
	public void testFileVariableRemoved() {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		// removes the variable - the location will be undefined (null)
		try {
			manager.setValue(VARIABLE_NAME, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", file);

		//refresh local - should not fail or make the link disappear
		try {
			file.refreshLocal(IResource.DEPTH_ONE, getMonitor());
			file.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.2");
		}

		assertExistsInWorkspace("3.3", file);

		// try to change resource's contents
		try {
			file.setContents(getContents("new contents"), IResource.NONE, null);
			// Resource has no-defined location - should fail
			fail("3.4");
		} catch (CoreException re) {
			// success: resource had no defined location
		}

		assertExistsInWorkspace("3.5", file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		try {
			manager.setValue(VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		assertExistsInWorkspace("5.0", file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem("5.2", file);
		// the contents must be the original ones
		try {
			assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
		} catch (CoreException e) {
			fail("5.4", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testFileProjectVariableRemoved() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		// removes the variable - the location will be undefined (null)
		try {
			manager.setValue(PROJECT_VARIABLE_NAME, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", file);

		// refresh local - should not fail or make the link disappear
		try {
			file.refreshLocal(IResource.DEPTH_ONE, getMonitor());
			file.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.2");
		}

		assertExistsInWorkspace("3.3", file);

		// try to change resource's contents
		try {
			file.setContents(getContents("new contents"), IResource.NONE, null);
			// Resource has no-defined location - should fail
			fail("3.4");
		} catch (CoreException re) {
			// success: resource had no defined location
		}

		assertExistsInWorkspace("3.5", file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null
		// location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		try {
			manager.setValue(PROJECT_VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		assertExistsInWorkspace("5.0", file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem("5.2", file);
		// the contents must be the original ones
		try {
			assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
		} catch (CoreException e) {
			fail("5.4", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * moved to a new project.
	 * This is a regression test for bug 266679
	 */
	public void testMoveFileToDifferentProject() {

		IFile file = existingProjectInSubDirectory.getFile("my_link");

		// creates a variable-based location
		IPath variableBasedLocation = null;
		IPath targetPath = existingProjectInSubDirectory.getLocation().removeLastSegments(1).append("outside.txt");
		if (!targetPath.toFile().exists()) {
			try {
				targetPath.toFile().createNewFile();
			} catch (IOException e2) {
				fail("0.4", e2);
			}
		}
		toDelete.add(targetPath);

		try {
			variableBasedLocation = convertToRelative(targetPath, file, true, null);
		} catch (CoreException e1) {
			fail("0.99", e1);
		}

		IPath resolvedPath = URIUtil.toPath(file.getPathVariableManager().resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		IFile newFile = nonExistingFileInExistingFolder;
		try {
			file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", newFile);
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
	public void testPROJECT_LOC_MoveFileToDifferentProject() {

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
			try {
				targetPath.toFile().createNewFile();
			} catch (IOException e2) {
				fail("0.4", e2);
			}
		}
		toDelete.add(targetPath);

		try {
			existingProjectInSubDirectory.getPathVariableManager().setValue("P_RELATIVE", Path.fromPortableString("${PARENT-3-PROJECT_LOC}"));
			variableBasedLocation = Path.fromPortableString("P_RELATIVE/outside.txt");
		} catch (CoreException e1) {
			fail("0.99", e1);
		}

		IPath resolvedPath = existingProjectInSubDirectory.getPathVariableManager().resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		IFile newFile = nonExistingFileInExistingFolder;
		try {
			file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", newFile);
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
	public void testMoveFileProjectVariable() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		IPath resolvedPath = manager.resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		IFile newFile = nonExistingFileInExistingFolder;
		// removes the variable - the location will be undefined (null)
		try {
			file.move(newFile.getFullPath(), IResource.SHALLOW, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", newFile);
		assertTrue("3,2", !newFile.getLocation().equals(newFile.getRawLocation()));
		assertTrue("3,3", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,4", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,5", newFile.getLocation().equals(resolvedPath));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testMoveFileToNewProjectProjectVariable() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomRelativeProjectLocation();

		IPath resolvedPath = manager.resolvePath(variableBasedLocation);
		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		IFile newFile = nonExistingFileInOtherExistingProject;
		// moves the variable - the location will be undefined (null)
		try {
			file.move(newFile.getFullPath(), IResource.SHALLOW, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", newFile);
		assertTrue("3,2", !newFile.getLocation().equals(newFile.getRawLocation()));
		assertTrue("3,3", newFile.getRawLocation().equals(variableBasedLocation));
		assertTrue("3,4", newFile.getLocation().equals(resolvedPath));
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * removed.
	 */
	public void testFileProjectRelativeVariableRemoved() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFile file = nonExistingFileInExistingProject;
		IPath existingValue = manager.getValue(PROJECT_RELATIVE_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomRelativeProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		// removes the variable - the location will be undefined (null)
		try {
			manager.setValue(PROJECT_RELATIVE_VARIABLE_NAME, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3,1", file);

		// refresh local - should not fail or make the link disappear
		try {
			file.refreshLocal(IResource.DEPTH_ONE, getMonitor());
			file.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.2");
		}

		assertExistsInWorkspace("3.3", file);

		// try to change resource's contents
		try {
			file.setContents(getContents("new contents"), IResource.NONE, null);
			// Resource has no-defined location - should fail
			fail("3.4");
		} catch (CoreException re) {
			// success: resource had no defined location
		}

		assertExistsInWorkspace("3.5", file);
		// the location is null
		assertNull("3.6", file.getLocation());

		// try validating another link location while there is a link with null
		// location
		IFile other = existingProject.getFile("OtherVar");
		getWorkspace().validateLinkLocation(other, getRandomLocation());

		// re-creates the variable with its previous value
		try {
			manager.setValue(PROJECT_RELATIVE_VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		assertExistsInWorkspace("5.0", file);
		assertNotNull("5.1", file.getLocation());
		assertExistsInFileSystem("5.2", file);
		// the contents must be the original ones
		try {
			assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
		} catch (CoreException e) {
			fail("5.4", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked folder location is
	 * removed.
	 */
	public void testFolderVariableRemoved() {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IPath existingValue = manager.getValue(VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", folder);

		try {
			folder.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
			childFile.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			childFile.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", folder);
		assertExistsInWorkspace("2.1", childFile);
		assertExistsInFileSystem("2.2", folder);
		assertExistsInFileSystem("2.3", childFile);

		// removes the variable - the location will be undefined (null)
		try {
			manager.setValue(VARIABLE_NAME, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3.1", folder);

		//refresh local - should not fail but should cause link's children to disappear
		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.2", e);
		}
		assertExistsInWorkspace("3.3", folder);
		assertDoesNotExistInWorkspace("3.4", childFile);

		//try to copy a file to the folder
		IFile destination = folder.getFile(existingFileInExistingProject.getName());
		try {
			existingFileInExistingProject.copy(destination.getFullPath(), IResource.NONE, getMonitor());
			//should fail
			fail("3.5");
		} catch (CoreException e) {
			//expected
		}
		assertTrue("3.6", !destination.exists());

		//try to create a sub-file
		try {
			destination.create(getRandomContents(), IResource.NONE, getMonitor());
			//should fail
			fail("3.7");
		} catch (CoreException e) {
			//expected
		}

		//try to create a sub-folder
		IFolder subFolder = folder.getFolder("SubFolder");
		try {
			subFolder.create(IResource.NONE, true, getMonitor());
			//should fail
			fail("3.8");
		} catch (CoreException e) {
			//expected
		}

		// try to change resource's contents
		try {
			childFile.setContents(getContents("new contents"), IResource.NONE, null);
			// Resource has no-defined location - should fail
			fail("4.0");
		} catch (CoreException re) {
			// success: resource had no defined location
		}

		assertExistsInWorkspace("4.1", folder);
		// the location is null
		assertNull("4.2", folder.getLocation());

		// re-creates the variable with its previous value
		try {
			manager.setValue(VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertExistsInWorkspace("6.0", folder);
		assertNotNull("6.1", folder.getLocation());
		assertExistsInFileSystem("6.2", folder);
		assertDoesNotExistInWorkspace("6.3", childFile);
		assertExistsInFileSystem("6.4", childFile);

		// refresh should recreate the child
		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
		assertExistsInWorkspace("7.1", folder);
		assertExistsInWorkspace("7.2", childFile);
	}

	/**
	 * Tests importing a project with a Linked Resource and Path Variable,
	 * where the line endings in the .project file do not match the local
	 * Platform.
	 * The .project file must not be modified on import, especially if it
	 * is marked read-only.
	 * See <a href="https://bugs.eclipse.org/bugs/show_bug.cgi?id=210664">Bug 210664</a>.
	 */
	public void testImportWrongLineEndings_Bug210664() throws IOException {
		// Choose a project to work on
		IProject proj = existingProject;
		IFileStore projStore = null;
		IPath randomLocationWithPathVariable = getRandomLocation();

		try {
			projStore = EFS.getStore(proj.getLocationURI());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		// Don't run this test if we cannot set a file read-only
		if ((projStore.getFileSystem().attributes() & EFS.ATTRIBUTE_READ_ONLY) == 0) {
			return;
		}

		try {
			// Create a linked resource with a non-existing path variable
			IFolder folder = proj.getFolder("SOME_LINK");
			folder.createLink(randomLocationWithPathVariable, IResource.ALLOW_MISSING_LOCAL, null);

			// Close the project, and convert line endings
			IFileStore projFile = projStore.getChild(".project");
			proj.delete(IResource.NEVER_DELETE_PROJECT_CONTENT, getMonitor());
			IFileStore projNew = projStore.getChild(".project.new");
			convertLineEndings(projFile, projNew, getMonitor());

			// Set the project read-only
			projNew.move(projFile, EFS.OVERWRITE, getMonitor());
			IFileInfo info = projFile.fetchInfo(EFS.NONE, getMonitor());
			info.setAttribute(EFS.ATTRIBUTE_READ_ONLY, true);
			projFile.putInfo(info, EFS.SET_ATTRIBUTES, getMonitor());
			toSetWritable = projFile; /* for cleanup */
		} catch (CoreException e) {
			fail("2.0", e);
		}

		try {
			//Bug 210664: Open project with wrong line endings and non-existing path variable
			proj.create(null);
			proj.open(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked folder location is
	 * removed.
	 */
	public void testFolderProjectVariableRemoved() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IFolder folder = nonExistingFolderInExistingProject;
		IFile childFile = folder.getFile(childName);
		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", folder);

		try {
			folder.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, null);
			childFile.create(getRandomContents(), IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			childFile.setContents(getContents("contents for a file"), IResource.FORCE, null);
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", folder);
		assertExistsInWorkspace("2.1", childFile);
		assertExistsInFileSystem("2.2", folder);
		assertExistsInFileSystem("2.3", childFile);

		// removes the variable - the location will be undefined (null)
		try {
			manager.setValue(PROJECT_VARIABLE_NAME, null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertExistsInWorkspace("3.1", folder);

		// refresh local - should not fail but should cause link's children to
		// disappear
		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			folder.getProject().refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("3.2", e);
		}
		assertExistsInWorkspace("3.3", folder);
		assertDoesNotExistInWorkspace("3.4", childFile);

		// try to copy a file to the folder
		IFile destination = folder.getFile(existingFileInExistingProject.getName());
		try {
			existingFileInExistingProject.copy(destination.getFullPath(), IResource.NONE, getMonitor());
			// should fail
			fail("3.5");
		} catch (CoreException e) {
			// expected
		}
		assertTrue("3.6", !destination.exists());

		// try to create a sub-file
		try {
			destination.create(getRandomContents(), IResource.NONE, getMonitor());
			// should fail
			fail("3.7");
		} catch (CoreException e) {
			// expected
		}

		// try to create a sub-folder
		IFolder subFolder = folder.getFolder("SubFolder");
		try {
			subFolder.create(IResource.NONE, true, getMonitor());
			// should fail
			fail("3.8");
		} catch (CoreException e) {
			// expected
		}

		// try to change resource's contents
		try {
			childFile.setContents(getContents("new contents"), IResource.NONE, null);
			// Resource has no-defined location - should fail
			fail("4.0");
		} catch (CoreException re) {
			// success: resource had no defined location
		}

		assertExistsInWorkspace("4.1", folder);
		// the location is null
		assertNull("4.2", folder.getLocation());

		// re-creates the variable with its previous value
		try {
			manager.setValue(PROJECT_VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertExistsInWorkspace("6.0", folder);
		assertNotNull("6.1", folder.getLocation());
		assertExistsInFileSystem("6.2", folder);
		assertDoesNotExistInWorkspace("6.3", childFile);
		assertExistsInFileSystem("6.4", childFile);

		// refresh should recreate the child
		try {
			folder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("7.0", e);
		}
		assertExistsInWorkspace("7.1", folder);
		assertExistsInWorkspace("7.2", childFile);
	}

	/**
	 * Tests scenario where links are relative to undefined variables
	 */
	public void testUndefinedVariable() {
		IPath folderLocation = new Path("NOVAR/folder");
		IPath fileLocation = new Path("NOVAR/abc.txt");
		IFile testFile = existingProject.getFile("UndefinedVar.txt");
		IFolder testFolder = existingProject.getFolder("UndefinedVarTest");

		//should fail to create links
		try {
			testFile.createLink(fileLocation, IResource.NONE, getMonitor());
			fail("1.0");
		} catch (CoreException e) {
			//should fail
		}
		try {
			testFolder.createLink(folderLocation, IResource.NONE, getMonitor());
			fail("1.1");
		} catch (CoreException e) {
			//should fail
		}

		//validate method should return warning
		assertTrue("1.2", getWorkspace().validateLinkLocation(testFolder, folderLocation).getSeverity() == IStatus.WARNING);
		assertTrue("1.3", getWorkspace().validateLinkLocation(testFile, fileLocation).getSeverity() == IStatus.WARNING);

		//should succeed with ALLOW_MISSING_LOCAL
		try {
			testFile.createLink(fileLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		try {
			testFolder.createLink(folderLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}

		//copy should fail
		IPath copyFileDestination = existingProject.getFullPath().append("CopyFileDest");
		IPath copyFolderDestination = existingProject.getFullPath().append("CopyFolderDest");

		try {
			testFile.copy(copyFileDestination, IResource.NONE, getMonitor());
			fail("3.0");
		} catch (CoreException e) {
			//should fail
		}
		try {
			testFolder.copy(copyFolderDestination, IResource.NONE, getMonitor());
			fail("3.1");
		} catch (CoreException e) {
			//should fail
		}

		//move should fail
		IPath moveFileDestination = existingProject.getFullPath().append("MoveFileDest");
		IPath moveFolderDestination = existingProject.getFullPath().append("MoveFolderDest");

		try {
			testFile.move(moveFileDestination, IResource.NONE, getMonitor());
			fail("4.0");
		} catch (CoreException e) {
			//should fail
		}
		try {
			testFolder.move(moveFolderDestination, IResource.NONE, getMonitor());
			fail("4.1");
		} catch (CoreException e) {
			//should fail
		}

		//refresh local should succeed
		try {
			testFile.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			testFolder.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
			testFile.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
			testFolder.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
			existingProject.refreshLocal(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("5.0", e);
		}

		//renaming the project shallow is ok
		try {
			IProject project = testFolder.getProject();
			IProjectDescription desc = project.getDescription();
			desc.setName("moveDest");
			project.move(desc, IResource.SHALLOW | IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("6.0");
		}

		//delete should succeed
		try {
			testFile.delete(IResource.NONE, getMonitor());
			testFolder.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("9.0", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * changed.
	 */
	public void testVariableChanged() {
		final IPathVariableManager manager = getWorkspace().getPathVariableManager();

		IPath existingValue = manager.getValue(VARIABLE_NAME);

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		// changes the variable value - the file location will change
		try {
			IPath newLocation = super.getRandomLocation();
			toDelete.add(newLocation);
			manager.setValue(VARIABLE_NAME, newLocation);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// try to change resource's contents
		try {
			file.setContents(getContents("new contents"), IResource.NONE, getMonitor());
			// Resource was out of sync - should not be able to change
			fail("3.0");
		} catch (CoreException e) {
			assertEquals("3.1", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}

		assertExistsInWorkspace("3.2", file);
		// the location is different - does not exist anymore
		assertDoesNotExistInFileSystem("3.3", file);

		// successfully changes resource's contents (using IResource.FORCE)
		try {
			file.setContents(getContents("contents in different location"), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// now the file exists in a different location
		assertExistsInFileSystem("4.1", file);

		// its location must have changed reflecting the variable change
		IPath expectedNewLocation = manager.resolvePath(variableBasedLocation);
		IPath actualNewLocation = file.getLocation();
		assertEquals("4.2", expectedNewLocation, actualNewLocation);

		// its contents are as just set
		try {
			assertTrue("4.3", compareContent(file.getContents(), getContents("contents in different location")));
		} catch (CoreException e) {
			fail("4.4", e);
		}

		// clean-up
		ensureDoesNotExistInFileSystem(file);

		// restore the previous value
		try {
			manager.setValue(VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertExistsInWorkspace("5.1", file);
		assertExistsInFileSystem("5.2", file);
		// the contents must be the original ones
		try {
			assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
		} catch (CoreException e) {
			fail("5.4", e);
		}
	}

	/**
	 * Tests a scenario where a variable used in a linked file location is
	 * changed.
	 */
	public void testProjectVariableChanged() {
		final IPathVariableManager manager = existingProject.getPathVariableManager();

		IPath existingValue = manager.getValue(PROJECT_VARIABLE_NAME);

		IFile file = nonExistingFileInExistingProject;

		// creates a variable-based location
		IPath variableBasedLocation = getRandomProjectLocation();

		// the file should not exist yet
		assertDoesNotExistInWorkspace("1.0", file);

		try {
			file.createLink(variableBasedLocation, IResource.ALLOW_MISSING_LOCAL, getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		try {
			file.setContents(getContents("contents for a file"), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("1.2", e);
		}

		// now the file exists in both workspace and file system
		assertExistsInWorkspace("2.0", file);
		assertExistsInFileSystem("2.1", file);

		// changes the variable value - the file location will change
		try {
			IPath newLocation = super.getRandomLocation();
			toDelete.add(newLocation);
			manager.setValue(PROJECT_VARIABLE_NAME, newLocation);
		} catch (CoreException e) {
			fail("2.2", e);
		}

		// try to change resource's contents
		try {
			file.setContents(getContents("new contents"), IResource.NONE, getMonitor());
			// Resource was out of sync - should not be able to change
			fail("3.0");
		} catch (CoreException e) {
			assertEquals("3.1", IResourceStatus.OUT_OF_SYNC_LOCAL, e.getStatus().getCode());
		}

		assertExistsInWorkspace("3.2", file);
		// the location is different - does not exist anymore
		assertDoesNotExistInFileSystem("3.3", file);

		// successfully changes resource's contents (using IResource.FORCE)
		try {
			file.setContents(getContents("contents in different location"), IResource.FORCE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		}

		// now the file exists in a different location
		assertExistsInFileSystem("4.1", file);

		// its location must have changed reflecting the variable change
		IPath expectedNewLocation = manager.resolvePath(variableBasedLocation);
		IPath actualNewLocation = file.getLocation();
		assertEquals("4.2", expectedNewLocation, actualNewLocation);

		// its contents are as just set
		try {
			assertTrue("4.3", compareContent(file.getContents(), getContents("contents in different location")));
		} catch (CoreException e) {
			fail("4.4", e);
		}

		// clean-up
		ensureDoesNotExistInFileSystem(file);

		// restore the previous value
		try {
			manager.setValue(PROJECT_VARIABLE_NAME, existingValue);
		} catch (CoreException e) {
			fail("5.0", e);
		}

		assertExistsInWorkspace("5.1", file);
		assertExistsInFileSystem("5.2", file);
		// the contents must be the original ones
		try {
			assertTrue("5.3", compareContent(file.getContents(true), getContents("contents for a file")));
		} catch (CoreException e) {
			fail("5.4", e);
		}
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
	public void testNonRedundentPathVariablesGenerated() {
		IFile file = existingProjectInSubDirectory.getFile("my_link");

		IPathVariableManager pathVariableManager = existingProjectInSubDirectory.getPathVariableManager();

		// creates a variable-based location
		IPath variableBasedLocation = null;
		IPath targetPath = existingProjectInSubDirectory.getLocation().removeLastSegments(1).append("outside.txt");
		if (!targetPath.toFile().exists()) {
			try {
				targetPath.toFile().createNewFile();
			} catch (IOException e2) {
				fail("1.0", e2);
			}
		}
		toDelete.add(targetPath);

		try {
			variableBasedLocation = convertToRelative(targetPath, file, true, null);
		} catch (CoreException e1) {
			fail("2.0", e1);
		}
		IPath resolvedPath = URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace("3.0", file);
		assertEquals("3.1", targetPath, resolvedPath);

		try {
			variableBasedLocation = convertToRelative(targetPath, file, true, null);
		} catch (CoreException e1) {
			fail("4.0", e1);
		}

		resolvedPath = URIUtil.toPath(pathVariableManager.resolveURI(URIUtil.toURI(variableBasedLocation)));
		// the file should not exist yet
		assertDoesNotExistInWorkspace("5.0", file);
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
