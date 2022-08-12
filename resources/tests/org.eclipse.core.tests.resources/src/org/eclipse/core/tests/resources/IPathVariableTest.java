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
 *     Alexander Kurtakov <akurtako@redhat.com> - Bug 459343
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import java.io.*;
import java.net.URI;
import java.util.*;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * Tests path variables.
 */
public class IPathVariableTest extends ResourceTest {

	IPathVariableManager manager = null;
	IProject project = null;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		project = getWorkspace().getRoot().getProject("MyProject");
		try {
			project.create(getMonitor());
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("1.3", e);
		}
		assertTrue("1.4", project.exists());
		manager = project.getPathVariableManager();
	}

	static class PathVariableChangeVerifier implements IPathVariableChangeListener {
		static class VerificationFailedException extends Exception {
			/**
			 * All serializable objects should have a stable serialVersionUID
			 */
			private static final long serialVersionUID = 1L;

			VerificationFailedException(String message) {
				super(message);
			}
		}

		static class Event {
			int type;
			String name;
			IPath value;

			Event(int type, String name, IPath value) {
				this.type = type;
				this.name = name;
				this.value = value;
			}

			@Override
			public boolean equals(Object obj) {
				if (obj == null || !(obj instanceof Event)) {
					return false;
				}
				Event that = (Event) obj;
				if (this.type != that.type || !this.name.equals(that.name)) {
					return false;
				}
				return this.value == null ? that.value == null : this.value.equals(that.value);
			}

			@Override
			public String toString() {
				StringBuilder buffer = new StringBuilder();
				buffer.append("Event(");
				buffer.append("type: ");
				buffer.append(stringForType(type));
				buffer.append(" name: ");
				buffer.append(name);
				buffer.append(" value: ");
				buffer.append(value);
				buffer.append(")");
				return buffer.toString();
			}

			String stringForType(int typeValue) {
				switch (typeValue) {
					case IPathVariableChangeEvent.VARIABLE_CREATED :
						return "CREATED";
					case IPathVariableChangeEvent.VARIABLE_CHANGED :
						return "CHANGED";
					case IPathVariableChangeEvent.VARIABLE_DELETED :
						return "DELETED";
					default :
						return "UNKNOWN";
				}
			}
		}

		List<Event> expected = new ArrayList<>();
		List<Event> actual = new ArrayList<>();

		void addExpectedEvent(int type, String name, IPath value) {
			expected.add(new Event(type, name, value));
		}

		void verify() throws VerificationFailedException {
			String message;
			if (expected.size() != actual.size()) {
				message = "Expected size: " + expected.size() + " does not equal actual size: " + actual.size() + "\n";
				message += dump();
				throw new VerificationFailedException(message);
			}
			for (Event event : expected) {
				if (!actual.contains(event)) {
					message = "Expected and actual results differ.\n";
					message += dump();
					throw new VerificationFailedException(message);
				}
			}
		}

		void reset() {
			expected = new ArrayList<>();
			actual = new ArrayList<>();
		}

		@Override
		public void pathVariableChanged(IPathVariableChangeEvent event) {
			actual.add(new Event(event.getType(), event.getVariableName(), event.getValue()));
		}

		String dump() {
			StringBuilder buffer = new StringBuilder();
			buffer.append("Expected:\n");
			for (Event event : expected) {
				buffer.append("\t" + event + "\n");
			}
			buffer.append("Actual:\n");
			for (Event event : actual) {
				buffer.append("\t" + event + "\n");
			}
			return buffer.toString();
		}
	}

	/**
	 * Test IPathVariableManager#getPathVariableNames
	 */
	public void testGetPathVariableNames() {
		String[] names = null;

		// add one
		try {
			manager.setValue("one", getRandomLocation());
		} catch (CoreException e) {
			fail("1.0", e);
		}
		names = manager.getPathVariableNames();
		List<String> list = Arrays.asList(names);
		assertTrue("1.2", list.contains("one"));

		// add another
		try {
			manager.setValue("two", Path.ROOT);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("2.2", list.contains("one"));
		assertTrue("2.3", list.contains("two"));

		// remove one
		try {
			manager.setValue("one", (IPath) null);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("3.2", list.contains("two"));
		assertTrue("3.3", !list.contains("one"));

		// remove the last one
		try {
			manager.setValue("two", (IPath) null);
		} catch (CoreException e) {
			fail("4.0", e);
		}
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("4.2", !list.contains("two"));
		assertTrue("4.3", !list.contains("one"));
	}

	/**
	 * Test IPathVariableManager#getValue and IPathVariableManager#setValue
	 */
	public void testGetSetValue() {
		boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? new Path("C:\\testGetSetValue") : new Path("/testGetSetValue");
		IPath pathTwo = new Path("/blort/backup");
		//add device if necessary
		pathTwo = new Path(pathTwo.toFile().getAbsolutePath());
		IPath pathOneEdit = WINDOWS ? new Path("D:/foobar") : new Path("/foobar");

		// nothing to begin with
		assertNull("0.0", manager.getValue("one"));

		// add a value to the table
		try {
			manager.setValue("one", pathOne);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		IPath value = manager.getValue("one");
		assertNotNull("1.1", value);
		assertEquals("1.2", pathOne, value);

		// add another value
		try {
			manager.setValue("two", pathTwo);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		value = manager.getValue("two");
		assertNotNull("2.1", value);
		assertEquals("2.2", pathTwo, value);

		// edit the first value
		try {
			manager.setValue("one", pathOneEdit);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		value = manager.getValue("one");
		assertNotNull("3.1", value);
		assertTrue("3.2", pathOneEdit.equals(value));

		// setting with value == null will remove
		try {
			manager.setValue("one", (IPath) null);
		} catch (CoreException e) {
			fail("4.0", e);
		}
		assertNull("4.1", manager.getValue("one"));

		// set values with bogus names
		try {
			manager.setValue("ECLIPSE$HOME", Path.ROOT);
			fail("5.0 Accepted invalid variable name in setValue()");
		} catch (CoreException ce) {
			// success
		}

		// set value with relative path
		try {
			manager.setValue("one", new Path("foo/bar"));
		} catch (CoreException ce) {
			fail("5.0 Did not Accepted invalid variable value in setValue()");
		}

		// set invalid value (with invalid segment)
		if (WINDOWS) {
			String invalidPathString = "C:/a/\\::/b";
			IPath invalidPath = Path.fromPortableString(invalidPathString);
			assertTrue("6.0", invalidPath.isAbsolute());
			assertTrue("6.1", !Path.EMPTY.isValidPath(invalidPathString));
			assertTrue("6.2", manager.validateValue(invalidPath).isOK());
			try {
				manager.setValue("one", invalidPath);
			} catch (CoreException ce) {
				fail("6.3 Fail to accept invalid variable value in setValue()");
			}
		}

	}

	/**
	 * Test IPathVariableManager#isDefined
	 */
	public void testIsDefined() {
		assertTrue("0.0", !manager.isDefined("one"));
		try {
			manager.setValue("one", Path.ROOT);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1", manager.isDefined("one"));
		try {
			manager.setValue("one", (IPath) null);
		} catch (CoreException e) {
			fail("2.0", e);
		}
		assertTrue("2.1", !manager.isDefined("one"));
	}

	/**
	 * Test IPathVariableManager#resolvePath
	 */
	public void testResolvePathWithMacro() {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? new Path("c:/testGetSetValue/foo") : new Path("/testGetSetValue/foo");
		IPath pathTwo = WINDOWS ? new Path("c:/tmp/backup") : new Path("/tmp/backup");
		// add device if neccessary
		pathTwo = new Path(pathTwo.toFile().getAbsolutePath());

		try {
			manager.setValue("one", pathOne);
		} catch (CoreException e) {
			fail("0.1", e);
		}
		try {
			manager.setValue("two", pathTwo);
		} catch (CoreException e) {
			fail("0.2", e);
		}

		try {
			manager.setValue("three", Path.fromOSString("${two}/extra"));
		} catch (CoreException e) {
			fail("0.3", e);
		}

		IPath path = new Path("three/bar");
		IPath expected = new Path("/tmp/backup/extra/bar").setDevice(WINDOWS ? "c:" : null);
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	/**
	 */
	public void testProjectLoc() {
		IPath path = new Path("${PROJECT_LOC}/bar");
		IPath projectLocation = project.getLocation();

		IPath expected = projectLocation.append("bar");
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	/**
	 */
	public void testEclipseHome() {
		IPath path = new Path("${ECLIPSE_HOME}/bar");
		IPath expected = new Path(Platform.getInstallLocation().getURL().getPath()).append("bar");
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	/**
	 */
	public void testWorkspaceLocation() {
		IPath path = new Path("${WORKSPACE_LOC}/bar");
		IPath expected = project.getWorkspace().getRoot().getLocation().append("bar");
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	/**
	 * Test IgetVariableRelativePathLocation(project, IPath)
	 */

	public void testGetVariableRelativePathLocation() {
		IPath path = project.getWorkspace().getRoot().getLocation().append("bar");
		IPath actual;
		IPath expected;
		/* Does not work on the test machine because ECLIPSE_HOME and WORKSPACE is the same location
		actual = getVariableRelativePathLocation(project, path);
		expected = new Path("WORKSPACE_LOC/bar");
		assertEquals("1.0", expected, actual);
		 */
		path = new Path(Platform.getInstallLocation().getURL().getPath()).append("bar");
		expected = new Path("ECLIPSE_HOME/bar");
		actual = getVariableRelativePathLocation(project, path);
		assertEquals("2.0", expected, actual);

		path = project.getLocation().append("bar");
		expected = new Path("PROJECT_LOC/bar");
		actual = getVariableRelativePathLocation(project, path);
		assertEquals("3.0", expected, actual);

		actual = getVariableRelativePathLocation(project, new Path("/nonExistentPath/foo"));
		assertEquals("4.0", null, actual);
	}

	private IPath getVariableRelativePathLocation(IProject project, IPath location) {
		URI variableRelativePathLocation = project.getPathVariableManager().getVariableRelativePathLocation(URIUtil.toURI(location));
		if (variableRelativePathLocation != null) {
			return URIUtil.toPath(variableRelativePathLocation);
		}
		return null;
	}

	/**
	 * Test IPathVariableManager#resolvePath
	 */
	public void testResolvePath() {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? new Path("C:/testGetSetValue/foo") : new Path("/testGetSetValue/foo");
		IPath pathTwo = new Path("/blort/backup");
		//add device if necessary
		pathTwo = new Path(pathTwo.toFile().getAbsolutePath());

		try {
			// for WINDOWS - the device id for windows will be changed to upper case
			// in the variable stored in the manager
			manager.setValue("one", pathOne);
		} catch (CoreException e) {
			fail("0.1", e);
		}
		try {
			manager.setValue("two", pathTwo);
		} catch (CoreException e) {
			fail("0.2", e);
		}

		// one substitution
		IPath path = new Path("one/bar");
		IPath expected = new Path("/testGetSetValue/foo/bar").setDevice(WINDOWS ? "C:" : null);
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);

		// another substitution
		path = new Path("two/myworld");
		expected = new Path("/blort/backup/myworld");
		expected = new Path(expected.toFile().getAbsolutePath());
		actual = manager.resolvePath(path);
		assertEquals("2.0", expected, actual);

		// variable not defined
		path = new Path("three/nothere");
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("3.0", expected, actual);

		// device
		path = new Path("/one").setDevice(WINDOWS ? "C:" : null);
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("4.0", expected, actual);

		// device2
		if (WINDOWS) {
			path = new Path("C:two");
			expected = path;
			actual = manager.resolvePath(path);
			assertEquals("5.0", expected, actual);
		}

		// absolute
		path = new Path("/one");
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("6.0", expected, actual);

		// just resolving, check if the variable stored in the manager is canonicalized
		if (WINDOWS) {
			path = new Path("one");
			expected = FileUtil.canonicalPath(pathOne);
			actual = manager.resolvePath(path);
			// the path stored in the manager is canonicalized, so the device id of actual will be upper case
			assertEquals("7.0", expected, actual);
		}

		// null
		path = null;
		assertNull("7.0", manager.resolvePath(path));

	}

	private IPath convertToRelative(IPathVariableManager manager, IPath path, boolean force, String variableHint) throws CoreException {
		return URIUtil.toPath(manager.convertToRelative(URIUtil.toURI(path), force, variableHint));
	}

	/**
	 * Test IPathVariableManager#convertToRelative()
	 */
	public void testConvertToRelative() {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? new Path("c:/foo/bar") : new Path("/foo/bar");
		IPath pathTwo = WINDOWS ? new Path("c:/foo/other") : new Path("/foo/other");
		IPath pathThree = WINDOWS ? new Path("c:/random/other/subpath") : new Path("/random/other/subpath");
		IPath file = WINDOWS ? new Path("c:/foo/other/file.txt") : new Path("/foo/other/file.txt");

		try {
			manager.setValue("ONE", pathOne);
			manager.setValue("THREE", pathThree);
		} catch (CoreException e) {
			fail("0.1", e);
		}

		IPath actual = null;
		try {
			actual = convertToRelative(manager, file, false, "ONE");
		} catch (CoreException e) {
			fail("0.2", e);
		}
		IPath expected = file;
		assertEquals("1.0", expected, actual);

		try {
			manager.setValue("TWO", pathTwo);
		} catch (CoreException e) {
			fail("1.1", e);
		}

		try {
			actual = convertToRelative(manager, file, false, "ONE");
		} catch (CoreException e) {
			fail("1.2", e);
		}
		expected = file;
		assertEquals("2.0", expected, actual);

		try {
			actual = convertToRelative(manager, file, false, "TWO");
		} catch (CoreException e) {
			fail("2.1", e);
		}
		expected = new Path("TWO/file.txt");
		assertEquals("3.0", expected, actual);

		// force the path to be relative to "ONE"
		try {
			actual = convertToRelative(manager, file, true, "ONE");
		} catch (CoreException e) {
			fail("3.1", e);
		}
		expected = new Path("PARENT-1-ONE/other/file.txt");
		assertEquals("4.0", expected, actual);

		// the second time should be re-using "FOO"
		try {
			actual = convertToRelative(manager, file, true, "ONE");
		} catch (CoreException e) {
			fail("4.3", e);
		}
		expected = new Path("PARENT-1-ONE/other/file.txt");
		assertEquals("5.0", expected, actual);

		try {
			actual = convertToRelative(manager, file, true, "TWO");
		} catch (CoreException e) {
			fail("5.4", e);
		}
		expected = new Path("TWO/file.txt");
		assertEquals("6.0", expected, actual);

		try {
			actual = convertToRelative(manager, file, true, "TWO");
		} catch (CoreException e) {
			fail("6.1", e);
		}
		expected = new Path("TWO/file.txt");
		assertEquals("7.0", expected, actual);

		try {
			actual = convertToRelative(manager, file, false, null);
		} catch (CoreException e) {
			fail("7.1", e);
		}
		expected = new Path("TWO/file.txt");
		assertEquals("8.0", expected, actual);

		try {
			manager.setValue("TWO", (IPath) null);
		} catch (CoreException e) {
			fail("8.1", e);
		}

		// now without any direct reference
		try {
			actual = convertToRelative(manager, file, false, null);
		} catch (CoreException e) {
			fail("8.2", e);
		}
		expected = file;
		assertEquals("9.0", expected, actual);

		try {
			actual = convertToRelative(manager, file, true, null);
		} catch (CoreException e) {
			fail("9.1", e);
		}
		expected = new Path("PARENT-1-ONE/other/file.txt");
		assertEquals("10.0", expected, actual);
	}

	/**
	 * Test IPathVariableManager#testValidateName
	 */
	public void testValidateName() {

		// valid names
		assertTrue("0.0", manager.validateName("ECLIPSEHOME").isOK());
		assertTrue("0.1", manager.validateName("ECLIPSE_HOME").isOK());
		assertTrue("0.2", manager.validateName("ECLIPSE_HOME_1").isOK());
		assertTrue("0.3", manager.validateName("_").isOK());

		// invalid names
		assertTrue("1.0", !manager.validateName("1FOO").isOK());
		assertTrue("1.1", !manager.validateName("FOO%BAR").isOK());
		assertTrue("1.2", !manager.validateName("FOO$BAR").isOK());
		assertTrue("1.3", !manager.validateName(" FOO").isOK());
		assertTrue("1.4", !manager.validateName("FOO ").isOK());

	}

	/**
	 * Regression test for Bug 304195
	 */
	public void testEmptyURIResolution() {
		IPath path = new Path(new String());
		URI uri = URIUtil.toURI(path);
		try {
			manager.resolveURI(uri);
		} catch (Throwable e) {
			fail("1.2", e);
		}
		try {
			getWorkspace().getPathVariableManager().resolveURI(uri);
		} catch (Throwable e) {
			fail("1.2", e);
		}
	}

	/**
	 * Test IPathVariableManager#addChangeListener and IPathVariableManager#removeChangeListener
	 */
	public void testListeners() {
		PathVariableChangeVerifier listener = new PathVariableChangeVerifier();
		manager.addChangeListener(listener);
		IPath pathOne = new Path("/blort/foobar");
		//add device if necessary
		pathOne = new Path(pathOne.toFile().getAbsolutePath());
		IPath pathOneEdit = pathOne.append("myworld");

		try {

			// add a variable
			try {
				manager.setValue("one", pathOne);
			} catch (CoreException e) {
				fail("1.0", e);
			}
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_CREATED, "one", pathOne);
			try {
				listener.verify();
			} catch (PathVariableChangeVerifier.VerificationFailedException e) {
				fail("1.1", e);
			}

			// change a variable
			listener.reset();
			try {
				manager.setValue("one", pathOneEdit);
			} catch (CoreException e) {
				fail("2.0", e);
			}
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_CHANGED, "one", pathOneEdit);
			try {
				listener.verify();
			} catch (PathVariableChangeVerifier.VerificationFailedException e) {
				fail("2.1", e);
			}

			// remove a variable
			listener.reset();
			try {
				manager.setValue("one", (IPath) null);
			} catch (CoreException e) {
				fail("3.0", e);
			}
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_DELETED, "one", null);
			try {
				listener.verify();
			} catch (PathVariableChangeVerifier.VerificationFailedException e) {
				fail("3.1", e);
			}

		} finally {
			manager.removeChangeListener(listener);
		}
	}

	boolean contains(Object[] array, Object obj) {
		for (Object element : array) {
			if (element.equals(obj)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Ensure there are no path variables in the workspace.
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		String[] names = manager.getPathVariableNames();
		for (String name : names) {
			manager.setValue(name, (IPath) null);
		}
	}

	@Override
	protected void cleanup() throws CoreException {
		project.delete(true, getMonitor());
		super.cleanup();
	}

	/**
	 * Regression for Bug 308975 - Can't recover from 'invalid' path variable
	 */
	public void testLinkExistInProjectDescriptionButNotInWorkspace() {
		String dorProjectContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" + //
				"<projectDescription>" + //
				"<name>ExistingProject</name>" + //
				"<comment></comment>" + //
				"<projects>" + //
				"</projects>" + //
				"<buildSpec>" + //
				"</buildSpec>" + //
				"<natures>" + //
				"</natures>" + //
				"<variableList>" + //
				"<variable>" + //
				"		<name>PROJ_UP</name>" + //
				"		<value>$%7BPARENT-1-PROJECT_LOC%7D</value>" + //
				"	</variable>" + //
				"	</variableList>" + //
				"</projectDescription>";

		IProject existingProject = getWorkspace().getRoot().getProject("ExistingProject");

		ensureExistsInWorkspace(new IResource[] {existingProject}, true);

		try {
			existingProject.close(getMonitor());
			ProjectInfo info = (ProjectInfo) ((Project) existingProject).getResourceInfo(false, false);
			info.clear(ICoreConstants.M_USED);
			String dotProjectPath = existingProject.getLocation().append(".project").toOSString();
			FileWriter fstream = new FileWriter(dotProjectPath);
			try (BufferedWriter out = new BufferedWriter(fstream)) {
				out.write(dorProjectContent);
			}
			existingProject.open(getMonitor());
		} catch (CoreException | IOException e) {
			fail("1.99", e);
		}

		IPathVariableManager pathVariableManager = existingProject.getPathVariableManager();
		String[] varNames = pathVariableManager.getPathVariableNames();

		for (String varName : varNames) {
			try {
				pathVariableManager.getURIValue(varName);
			} catch (Exception e) {
				fail("3.99", e);
			}
		}
	}

	/**
	 * Regression test for Bug 328045 where a class cast exception is thrown when
	 * attempting to get the location of a resource that would live under an
	 * existing IFile.
	 */
	public void testDiscoverLocationOfInvalidFile() {
		IPath filep = new Path("someFile");
		IPath invalidChild = filep.append("invalidChild");

		// Create filep
		IFile f = project.getFile(filep);
		ensureExistsInWorkspace(f, true);

		// Get a reference to a child
		IFile invalidFile = project.getFile(invalidChild);

		// Don't care about the result, just care there's no exception.
		invalidFile.getLocationURI();
	}
}
