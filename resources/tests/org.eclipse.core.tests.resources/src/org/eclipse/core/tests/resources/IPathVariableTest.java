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

import static org.junit.Assert.assertThrows;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.internal.resources.ICoreConstants;
import org.eclipse.core.internal.resources.Project;
import org.eclipse.core.internal.resources.ProjectInfo;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IPathVariableChangeEvent;
import org.eclipse.core.resources.IPathVariableChangeListener;
import org.eclipse.core.resources.IPathVariableManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;

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
		project.create(getMonitor());
		project.open(getMonitor());
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
				if (obj == null || !(obj instanceof Event that)) {
					return false;
				}
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
	public void testGetPathVariableNames() throws CoreException {
		String[] names = null;

		// add one
		manager.setValue("one", getRandomLocation());
		names = manager.getPathVariableNames();
		List<String> list = Arrays.asList(names);
		assertTrue("1.2", list.contains("one"));

		// add another
		manager.setValue("two", IPath.ROOT);
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("2.2", list.contains("one"));
		assertTrue("2.3", list.contains("two"));

		// remove one
		manager.setValue("one", (IPath) null);
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("3.2", list.contains("two"));
		assertTrue("3.3", !list.contains("one"));

		// remove the last one
		manager.setValue("two", (IPath) null);
		names = manager.getPathVariableNames();
		list = Arrays.asList(names);
		assertTrue("4.2", !list.contains("two"));
		assertTrue("4.3", !list.contains("one"));
	}

	/**
	 * Test IPathVariableManager#getValue and IPathVariableManager#setValue
	 */
	public void testGetSetValue() throws CoreException {
		boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? IPath.fromOSString("C:\\testGetSetValue") : IPath.fromOSString("/testGetSetValue");
		IPath pathTwo = IPath.fromOSString("/blort/backup");
		//add device if necessary
		pathTwo = IPath.fromOSString(pathTwo.toFile().getAbsolutePath());
		IPath pathOneEdit = WINDOWS ? IPath.fromOSString("D:/foobar") : IPath.fromOSString("/foobar");

		// nothing to begin with
		assertNull("0.0", manager.getValue("one"));

		// add a value to the table
		manager.setValue("one", pathOne);
		IPath value = manager.getValue("one");
		assertNotNull("1.1", value);
		assertEquals("1.2", pathOne, value);

		// add another value
		manager.setValue("two", pathTwo);
		value = manager.getValue("two");
		assertNotNull("2.1", value);
		assertEquals("2.2", pathTwo, value);

		// edit the first value
		manager.setValue("one", pathOneEdit);
		value = manager.getValue("one");
		assertNotNull("3.1", value);
		assertTrue("3.2", pathOneEdit.equals(value));

		// setting with value == null will remove
		manager.setValue("one", (IPath) null);
		assertNull("4.1", manager.getValue("one"));

		// set values with bogus names
		assertThrows("Accepted invalid variable name in setValue()", CoreException.class,
				() -> manager.setValue("ECLIPSE$HOME", IPath.ROOT));

		// set value with relative path
		manager.setValue("one", IPath.fromOSString("foo/bar"));

		// set invalid value (with invalid segment)
		if (WINDOWS) {
			String invalidPathString = "C:/a/\\::/b";
			IPath invalidPath = IPath.fromPortableString(invalidPathString);
			assertTrue("6.0", invalidPath.isAbsolute());
			assertTrue("6.1", !IPath.EMPTY.isValidPath(invalidPathString));
			assertTrue("6.2", manager.validateValue(invalidPath).isOK());
			manager.setValue("one", invalidPath);
		}

	}

	/**
	 * Test IPathVariableManager#isDefined
	 */
	public void testIsDefined() throws CoreException {
		assertTrue("0.0", !manager.isDefined("one"));
		manager.setValue("one", IPath.ROOT);
		assertTrue("1.1", manager.isDefined("one"));
		manager.setValue("one", (IPath) null);
		assertTrue("2.1", !manager.isDefined("one"));
	}

	/**
	 * Test IPathVariableManager#resolvePath
	 */
	public void testResolvePathWithMacro() throws CoreException {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? IPath.fromOSString("c:/testGetSetValue/foo") : IPath.fromOSString("/testGetSetValue/foo");
		IPath pathTwo = WINDOWS ? IPath.fromOSString("c:/tmp/backup") : IPath.fromOSString("/tmp/backup");
		// add device if neccessary
		pathTwo = IPath.fromOSString(pathTwo.toFile().getAbsolutePath());

		manager.setValue("one", pathOne);
		manager.setValue("two", pathTwo);
		manager.setValue("three", IPath.fromOSString("${two}/extra"));

		IPath path = IPath.fromOSString("three/bar");
		IPath expected = IPath.fromOSString("/tmp/backup/extra/bar").setDevice(WINDOWS ? "c:" : null);
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	public void testProjectLoc() {
		IPath path = IPath.fromOSString("${PROJECT_LOC}/bar");
		IPath projectLocation = project.getLocation();

		IPath expected = projectLocation.append("bar");
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	public void testEclipseHome() {
		IPath path = IPath.fromOSString("${ECLIPSE_HOME}/bar");
		IPath expected = IPath.fromOSString(Platform.getInstallLocation().getURL().getPath()).append("bar");
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);
	}

	public void testWorkspaceLocation() {
		IPath path = IPath.fromOSString("${WORKSPACE_LOC}/bar");
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
		expected = IPath.fromOSString("WORKSPACE_LOC/bar");
		assertEquals("1.0", expected, actual);
		 */
		path = IPath.fromOSString(Platform.getInstallLocation().getURL().getPath()).append("bar");
		expected = IPath.fromOSString("ECLIPSE_HOME/bar");
		actual = getVariableRelativePathLocation(project, path);
		assertEquals("2.0", expected, actual);

		path = project.getLocation().append("bar");
		expected = IPath.fromOSString("PROJECT_LOC/bar");
		actual = getVariableRelativePathLocation(project, path);
		assertEquals("3.0", expected, actual);

		actual = getVariableRelativePathLocation(project, IPath.fromOSString("/nonExistentPath/foo"));
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
	public void testResolvePath() throws CoreException {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? IPath.fromOSString("C:/testGetSetValue/foo") : IPath.fromOSString("/testGetSetValue/foo");
		IPath pathTwo = IPath.fromOSString("/blort/backup");
		//add device if necessary
		pathTwo = IPath.fromOSString(pathTwo.toFile().getAbsolutePath());

		// for WINDOWS - the device id for windows will be changed to upper case
		// in the variable stored in the manager
		manager.setValue("one", pathOne);
		manager.setValue("two", pathTwo);

		// one substitution
		IPath path = IPath.fromOSString("one/bar");
		IPath expected = IPath.fromOSString("/testGetSetValue/foo/bar").setDevice(WINDOWS ? "C:" : null);
		IPath actual = manager.resolvePath(path);
		assertEquals("1.0", expected, actual);

		// another substitution
		path = IPath.fromOSString("two/myworld");
		expected = IPath.fromOSString("/blort/backup/myworld");
		expected = IPath.fromOSString(expected.toFile().getAbsolutePath());
		actual = manager.resolvePath(path);
		assertEquals("2.0", expected, actual);

		// variable not defined
		path = IPath.fromOSString("three/nothere");
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("3.0", expected, actual);

		// device
		path = IPath.fromOSString("/one").setDevice(WINDOWS ? "C:" : null);
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("4.0", expected, actual);

		// device2
		if (WINDOWS) {
			path = IPath.fromOSString("C:two");
			expected = path;
			actual = manager.resolvePath(path);
			assertEquals("5.0", expected, actual);
		}

		// absolute
		path = IPath.fromOSString("/one");
		expected = path;
		actual = manager.resolvePath(path);
		assertEquals("6.0", expected, actual);

		// just resolving, check if the variable stored in the manager is canonicalized
		if (WINDOWS) {
			path = IPath.fromOSString("one");
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
	public void testConvertToRelative() throws CoreException {
		final boolean WINDOWS = java.io.File.separatorChar == '\\';
		IPath pathOne = WINDOWS ? IPath.fromOSString("c:/foo/bar") : IPath.fromOSString("/foo/bar");
		IPath pathTwo = WINDOWS ? IPath.fromOSString("c:/foo/other") : IPath.fromOSString("/foo/other");
		IPath pathThree = WINDOWS ? IPath.fromOSString("c:/random/other/subpath") : IPath.fromOSString("/random/other/subpath");
		IPath file = WINDOWS ? IPath.fromOSString("c:/foo/other/file.txt") : IPath.fromOSString("/foo/other/file.txt");

		manager.setValue("ONE", pathOne);
		manager.setValue("THREE", pathThree);

		IPath actual = convertToRelative(manager, file, false, "ONE");
		IPath expected = file;
		assertEquals("1.0", expected, actual);

		manager.setValue("TWO", pathTwo);
		actual = convertToRelative(manager, file, false, "ONE");

		expected = file;
		assertEquals("2.0", expected, actual);

		actual = convertToRelative(manager, file, false, "TWO");
		expected = IPath.fromOSString("TWO/file.txt");
		assertEquals("3.0", expected, actual);

		// force the path to be relative to "ONE"
		actual = convertToRelative(manager, file, true, "ONE");
		expected = IPath.fromOSString("PARENT-1-ONE/other/file.txt");
		assertEquals("4.0", expected, actual);

		// the second time should be re-using "FOO"
		actual = convertToRelative(manager, file, true, "ONE");
		expected = IPath.fromOSString("PARENT-1-ONE/other/file.txt");
		assertEquals("5.0", expected, actual);

		actual = convertToRelative(manager, file, true, "TWO");
		expected = IPath.fromOSString("TWO/file.txt");
		assertEquals("6.0", expected, actual);

		actual = convertToRelative(manager, file, true, "TWO");
		expected = IPath.fromOSString("TWO/file.txt");
		assertEquals("7.0", expected, actual);

		actual = convertToRelative(manager, file, false, null);
		expected = IPath.fromOSString("TWO/file.txt");
		assertEquals("8.0", expected, actual);

		manager.setValue("TWO", (IPath) null);

		// now without any direct reference
		actual = convertToRelative(manager, file, false, null);
		expected = file;
		assertEquals("9.0", expected, actual);

		actual = convertToRelative(manager, file, true, null);
		expected = IPath.fromOSString("PARENT-1-ONE/other/file.txt");
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
		IPath path = IPath.fromOSString(new String());
		URI uri = URIUtil.toURI(path);
		manager.resolveURI(uri);
		getWorkspace().getPathVariableManager().resolveURI(uri);
	}

	/**
	 * Test IPathVariableManager#addChangeListener and IPathVariableManager#removeChangeListener
	 */
	public void testListeners() throws Exception {
		PathVariableChangeVerifier listener = new PathVariableChangeVerifier();
		manager.addChangeListener(listener);
		IPath pathOne = IPath.fromOSString("/blort/foobar");
		//add device if necessary
		pathOne = IPath.fromOSString(pathOne.toFile().getAbsolutePath());
		IPath pathOneEdit = pathOne.append("myworld");

		try {
			// add a variable
			manager.setValue("one", pathOne);
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_CREATED, "one", pathOne);
			listener.verify();

			// change a variable
			listener.reset();
			manager.setValue("one", pathOneEdit);
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_CHANGED, "one", pathOneEdit);
			listener.verify();

			// remove a variable
			listener.reset();
			manager.setValue("one", (IPath) null);
			listener.addExpectedEvent(IPathVariableChangeEvent.VARIABLE_DELETED, "one", null);
			listener.verify();
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

	/**
	 * Regression for Bug 308975 - Can't recover from 'invalid' path variable
	 */
	public void testLinkExistInProjectDescriptionButNotInWorkspace() throws Exception {
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

		existingProject.close(getMonitor());
		ProjectInfo info = (ProjectInfo) ((Project) existingProject).getResourceInfo(false, false);
		info.clear(ICoreConstants.M_USED);
		String dotProjectPath = existingProject.getLocation().append(".project").toOSString();
		FileWriter fstream = new FileWriter(dotProjectPath);
		try (BufferedWriter out = new BufferedWriter(fstream)) {
			out.write(dorProjectContent);
		}
		existingProject.open(getMonitor());

		IPathVariableManager pathVariableManager = existingProject.getPathVariableManager();
		String[] varNames = pathVariableManager.getPathVariableNames();

		for (String varName : varNames) {
			pathVariableManager.getURIValue(varName);
		}
	}

	/**
	 * Regression test for Bug 328045 where a class cast exception is thrown when
	 * attempting to get the location of a resource that would live under an
	 * existing IFile.
	 */
	public void testDiscoverLocationOfInvalidFile() throws CoreException {
		IPath filep = IPath.fromOSString("someFile");
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
