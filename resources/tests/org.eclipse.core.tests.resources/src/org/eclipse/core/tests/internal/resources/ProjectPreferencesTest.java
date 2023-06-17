/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.resources;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import org.eclipse.core.internal.preferences.EclipsePreferences;
import org.eclipse.core.internal.resources.ProjectPreferences;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILogListener;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.ResourceTest;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

/**
 * @since 3.0
 */
public class ProjectPreferencesTest extends ResourceTest {

	private static final String DIR_NAME = ".settings";
	private static final String FILE_EXTENSION = "prefs";

	static class Tracer implements IEclipsePreferences.IPreferenceChangeListener {
		public StringBuilder log = new StringBuilder();

		private String typeCode(Object value) {
			if (value == null) {
				return "";
			}
			if (value instanceof Boolean) {
				return "B";
			}
			if (value instanceof Integer) {
				return "I";
			}
			if (value instanceof Long) {
				return "L";
			}
			if (value instanceof Float) {
				return "F";
			}
			if (value instanceof Double) {
				return "D";
			}
			if (value instanceof String) {
				return "S";
			}
			assertTrue("0.0", false);
			return null;
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			log.append("[");
			log.append(event.getKey());
			log.append(":");
			log.append(typeCode(event.getOldValue()));
			log.append(event.getOldValue() == null ? "null" : event.getOldValue());
			log.append("->");
			log.append(typeCode(event.getNewValue()));
			log.append(event.getNewValue() == null ? "null" : event.getNewValue());
			log.append("]");
		}
	}

	public void testSimple() {
		IProject project = getProject("foo");
		String qualifier = "org.eclipse.core.tests.resources";
		String key = "key" + getUniqueString();
		String instanceValue = "instance" + getUniqueString();
		String projectValue = "project" + getUniqueString();
		IScopeContext projectContext = new ProjectScope(project);
		IScopeContext instanceContext = InstanceScope.INSTANCE;
		ensureExistsInWorkspace(project, true);

		ArrayList<IScopeContext[]> list = new ArrayList<>();
		list.add(null);
		list.add(new IScopeContext[0]);
		list.add(new IScopeContext[] {null});
		IScopeContext[][] contextsWithoutScope = list.toArray(new IScopeContext[list.size()][]);

		list = new ArrayList<>();
		list.add(new IScopeContext[] {projectContext});
		list.add(new IScopeContext[] {null, projectContext});
		IScopeContext[][] contextsWithScope = list.toArray(new IScopeContext[list.size()][]);

		// set a preference value in the instance scope
		IPreferencesService service = Platform.getPreferencesService();
		Preferences node = instanceContext.getNode(qualifier);
		node.put(key, instanceValue);
		String actual = node.get(key, null);
		assertNotNull("1.0", actual);
		assertEquals("1.1", instanceValue, actual);

		// get the value through service searching
		for (int i = 0; i < contextsWithoutScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithoutScope[i]);
			assertNotNull("2.0." + i, actual);
			assertEquals("2.1." + i, instanceValue, actual);
		}
		for (int i = 0; i < contextsWithScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithScope[i]);
			assertNotNull("2.2." + i, actual);
			assertEquals("2.3." + i, instanceValue, actual);
		}

		// set a preference value in the project scope
		node = projectContext.getNode(qualifier);
		node.put(key, projectValue);
		actual = node.get(key, null);
		assertNotNull("3.0", actual);
		assertEquals("3.1", projectValue, actual);

		// get the value through service searching
		for (int i = 0; i < contextsWithoutScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithoutScope[i]);
			assertNotNull("4.0." + i, actual);
			assertEquals("4.1." + i, instanceValue, actual);
		}
		for (int i = 0; i < contextsWithScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithScope[i]);
			assertNotNull("4.2." + i, actual);
			assertEquals("4.3." + i, projectValue, actual);
		}

		// remove the project scope value
		node = projectContext.getNode(qualifier);
		node.remove(key);
		actual = node.get(key, null);
		assertNull("5.0", actual);

		// get the value through service searching
		for (int i = 0; i < contextsWithoutScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithoutScope[i]);
			assertNotNull("6.0." + i, actual);
			assertEquals("6.1." + i, instanceValue, actual);
		}
		for (int i = 0; i < contextsWithScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithScope[i]);
			assertNotNull("6.2." + i, actual);
			assertEquals("6.3." + i, instanceValue, actual);
		}

		// remove the instance value so there is nothing
		node = instanceContext.getNode(qualifier);
		node.remove(key);
		actual = node.get(key, null);
		for (int i = 0; i < contextsWithoutScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithoutScope[i]);
			assertNull("7.0." + i, actual);
		}
		for (int i = 0; i < contextsWithScope.length; i++) {
			actual = service.getString(qualifier, key, null, contextsWithScope[i]);
			assertNull("7.1." + i, actual);
		}
	}

	public void testListener() {
		// setup
		IProject project = getProject(getUniqueString());
		String qualifier = "org.eclipse.core.tests.resources";
		String key = "key" + getUniqueString();
		String value = "value" + getUniqueString();
		IScopeContext projectContext = new ProjectScope(project);
		// create project
		ensureExistsInWorkspace(project, true);
		// set preferences
		Preferences node = projectContext.getNode(qualifier);
		node.put(key, value);
		String actual = node.get(key, null);
		assertNotNull("1.0", actual);
		assertEquals("1.1", value, actual);
		try {
			// flush
			node.flush();
		} catch (BackingStoreException e) {
			fail("0.0", e);
		}

		// get settings filename
		File file = getFileInFilesystem(project, qualifier);
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			props.load(input);
		} catch (IOException e) {
			fail("1.0", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		// change settings in the file
		String newKey = "newKey" + getUniqueString();
		String newValue = "newValue" + getUniqueString();
		props.put(newKey, newValue);

		// save the file and ensure timestamp is different
		OutputStream output = null;
		try {
			output = new BufferedOutputStream(new FileOutputStream(file));
			props.store(output, null);
		} catch (IOException e) {
			fail("2.0", e);
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		IFile workspaceFile = getFileInWorkspace(project, qualifier);

		// ensure that the file is out-of-sync with the workspace
		// by changing the lastModified time
		touchInFilesystem(workspaceFile);

		// resource change is fired
		try {
			workspaceFile.refreshLocal(IResource.DEPTH_ZERO, getMonitor());
		} catch (CoreException e) {
			fail("3.1", e);
		}

		// validate new settings
		actual = node.get(key, null);
		assertEquals("4.1", value, actual);
		actual = node.get(newKey, null);
		assertEquals("4.2", newValue, actual);
	}

	/**
	 * Regression test for bug 60896 - Project preferences remains when deleting/creating project
	 */
	public void testProjectDelete() {
		// create the project
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		// set some settings
		String qualifier = getUniqueString();
		String key = getUniqueString();
		String value = getUniqueString();
		IScopeContext context = new ProjectScope(project);
		Preferences node = context.getNode(qualifier);
		Preferences parent = node.parent().parent();
		node.put(key, value);
		assertEquals("1.0", value, node.get(key, null));

		try {
			// delete the project
			project.delete(IResource.FORCE | IResource.ALWAYS_DELETE_PROJECT_CONTENT, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		try {
			// project pref should not exist
			assertTrue("3.0", !parent.nodeExists(project.getName()));
		} catch (BackingStoreException e) {
			fail("3.1", e);
		}

		// create a project with the same name
		ensureExistsInWorkspace(project, true);

		// ensure that the preference value is not set
		assertNull("4.0", context.getNode(qualifier).get(key, null));
	}

	/** See bug 91244, bug 93398 and bug 211006. */
	public void testProjectMove() {
		IProject project1 = getProject(getUniqueString());
		IProject project2 = getProject(getUniqueString());

		ensureExistsInWorkspace(new IResource[] {project1}, true);
		String qualifier = getUniqueString();
		String key = getUniqueString();
		String value = getUniqueString();
		Preferences node = new ProjectScope(project1).getNode(qualifier);
		node.put(key, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}
		// move project
		try {
			project1.move(IPath.fromOSString(project2.getName()), false, null);
		} catch (CoreException e) {
			fail("2.0", e);
		}

		// ensure that preferences for the old project are removed
		node = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE);
		assertNotNull("2.1", node);
		try {
			assertTrue("2.2", !node.nodeExists(project1.getName()));
		} catch (BackingStoreException e) {
			fail("2.3", e);
		}

		// ensure preferences are preserved
		node = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE);
		assertNotNull("2.3", node);
		try {
			assertTrue("2.4", node.nodeExists(project2.getName()));
		} catch (BackingStoreException e) {
			fail("2.5", e);
		}
		node = node.node(project2.getName());
		assertNotNull("3.1", node);
		try {
			assertTrue("3.2", node.nodeExists(qualifier));
		} catch (BackingStoreException e) {
			fail("3.3", e);
		}
		node = node.node(qualifier);
		assertNotNull("4.1", node);
		assertEquals("4.2", value, node.get(key, null));
	}

	/**
	 * Regression test for Bug 60925 - project preferences do not show up in workspace.
	 *
	 * Initially we were using java.io.File APIs and writing the preferences files
	 * directly to disk. We need to convert to use Resource APIs so changes
	 * show up in the workspace immediately.
	 */
	public void test_60925() {
		// setup
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		String qualifier = getUniqueString();
		IFile file = getFileInWorkspace(project, qualifier);

		// should be nothing in the file system
		assertTrue("0.0", !file.exists());
		assertTrue("0.1", !file.getLocation().toFile().exists());

		// store a preference key/value pair
		IScopeContext context = new ProjectScope(project);
		String key = getUniqueString();
		String value = getUniqueString();
		Preferences node = context.getNode(qualifier);
		node.put(key, value);

		// flush changes to disk
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}

		// changes should appear in the workspace
		assertTrue("2.0", file.exists());
		assertTrue("2.1", file.isSynchronized(IResource.DEPTH_ZERO));
	}

	/**
	 * Bug 55410 - [runtime] prefs: keys and valid chars
	 *
	 * Problems with a dot "." as a key name
	 */
	public void test_55410() {
		IProject project1 = getProject(getUniqueString());
		ensureExistsInWorkspace(new IResource[] {project1}, true);
		Preferences node = new ProjectScope(project1).getNode(ResourcesPlugin.PI_RESOURCES).node("subnode");
		String key1 = ".";
		String key2 = "x";
		String value1 = getUniqueString();
		String value2 = getUniqueString();
		node.put(key1, value1);
		node.put(key2, value2);
		assertEquals("0.8", value1, node.get(key1, null));
		assertEquals("0.9", value2, node.get(key2, null));
		IFile prefsFile = getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES);
		assertTrue("1.0", prefsFile.exists());
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.1", e);
		}
		assertTrue("1.1", prefsFile.exists());
		Properties props = new Properties();
		InputStream contents = null;
		try {
			contents = prefsFile.getContents();
		} catch (CoreException e) {
			fail("1.2", e);
		}
		try {
			props.load(contents);
		} catch (IOException e) {
			fail("1.3", e);
		} finally {
			if (contents != null) {
				try {
					contents.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}
		assertEquals("2.0", value2, props.getProperty("subnode/" + key2));
		assertEquals("2.1", value1, props.getProperty("subnode/" + key1));
	}

	/**
	 * Bug 61277 - preferences and project moves
	 *
	 * Investigate what happens with project preferences when the
	 * project is moved.
	 */
	public void test_61277a() {
		IProject project = getProject(getUniqueString());
		IProject destProject = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		ensureDoesNotExistInWorkspace(destProject);
		IScopeContext context = new ProjectScope(project);
		String qualifier = getUniqueString();
		Preferences node = context.getNode(qualifier);
		String key = getUniqueString();
		String value = getUniqueString();
		node.put(key, value);
		assertEquals("1.0", value, node.get(key, null));

		try {
			// save the prefs
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.1", e);
		}

		// rename the project
		try {
			project.move(destProject.getFullPath(), true, getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}

		context = new ProjectScope(destProject);
		node = context.getNode(qualifier);
		assertEquals("3.0", value, node.get(key, null));
	}

	/**
	 * Bug 61277 - preferences and project moves
	 *
	 * Investigate what happens with project preferences when the
	 * project is moved.
	 */
	public void test_61277b() {
		IProject project1 = getProject(getUniqueString());
		IProject project2 = getProject(getUniqueString());
		ensureExistsInWorkspace(new IResource[] {project1}, true);
		Preferences node = new ProjectScope(project1).getNode(ResourcesPlugin.PI_RESOURCES);
		assertTrue("1.0", getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES).exists());
		node.put("key", "value");
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}
		assertTrue("1.1", getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES).exists());
		// move project and ensures charsets settings are preserved
		try {
			project1.move(project2.getFullPath(), false, null);
		} catch (CoreException e) {
			fail("2.99", e);
		}
		assertTrue("2.0", getFileInWorkspace(project2, ResourcesPlugin.PI_RESOURCES).exists());
		node = new ProjectScope(project2).getNode(ResourcesPlugin.PI_RESOURCES);
		assertEquals("2.1", "value", node.get("key", null));
	}

	/**
	 * Bug 61277 - preferences and project moves
	 *
	 * Investigate what happens with project preferences when the
	 * project is moved.
	 *
	 * Problems with a key which is the empty string.
	 */
	public void test_61277c() {
		IProject project1 = getProject(getUniqueString());
		ensureExistsInWorkspace(new IResource[] {project1}, true);
		assertTrue("1.0", getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES).exists());
		Preferences node = new ProjectScope(project1).getNode(ResourcesPlugin.PI_RESOURCES);
		String key1 = "key";
		String emptyKey = "";
		String value1 = getUniqueString();
		String value2 = getUniqueString();
		node.put(key1, value1);
		node.put(emptyKey, value2);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.1", e);
		}
		assertTrue("1.2", getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES).exists());

		// move project and ensures charsets settings are preserved
		IProject project2 = getProject(getUniqueString());
		try {
			project1.move(project2.getFullPath(), false, null);
		} catch (CoreException e) {
			fail("2.99", e);
		}
		assertTrue("2.0", getFileInWorkspace(project2, ResourcesPlugin.PI_RESOURCES).exists());

		node = new ProjectScope(project2).getNode(ResourcesPlugin.PI_RESOURCES);
		assertEquals("2.1", value1, node.get(key1, null));
		assertEquals("2.2", value2, node.get(emptyKey, null));
	}

	/*
	 * Bug 61843 - Saving project preferences failed
	 *
	 * The project preferences are being accessing (for the first time) from
	 * within a resource change listener reacting to a change in the workspace.
	 */
	public void test_61843() {
		// create the project and manually give it a settings file
		final String qualifier = getUniqueString();
		final IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		IFile settingsFile = getFileInWorkspace(project, qualifier);

		// write some property values in the settings file
		Properties properties = new Properties();
		properties.put("key", "value");
		OutputStream output = null;
		try {
			File file = settingsFile.getLocation().toFile();
			file.getParentFile().mkdirs();
			output = new BufferedOutputStream(new FileOutputStream(file));
			properties.store(output, null);
		} catch (FileNotFoundException e) {
			fail("1.0", e);
		} catch (IOException e) {
			fail("1.1", e);
		} finally {
			try {
				if (output != null) {
					output.close();
				}
			} catch (IOException e) {
				// ignore
			}
		}

		// add a log listener to ensure that no errors are reported silently
		ILogListener logListener = (status, plugin) -> {
			Throwable exception = status.getException();
			if (exception == null || !(exception instanceof CoreException coreException)) {
				return;
			}
			if (IResourceStatus.WORKSPACE_LOCKED == coreException.getStatus().getCode()) {
				fail("3.0");
			}
		};

		// listener to react to changes in the workspace
		IResourceChangeListener rclistener = event -> new ProjectScope(project).getNode(qualifier);

		// add the listeners
		Platform.addLogListener(logListener);
		getWorkspace().addResourceChangeListener(rclistener, IResourceChangeEvent.POST_CHANGE);

		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("4.0", e);
		} finally {
			Platform.removeLogListener(logListener);
			getWorkspace().removeResourceChangeListener(rclistener);
		}
	}

	/*
	 * Bug 65068 - When the preferences file is deleted, the corresponding preferences
	 * should be forgotten.
	 */
	public void test_65068() {
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		Preferences node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		String key = "key";
		String value = getUniqueString();
		node.put(key, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.1", e);
		}
		assertTrue("1.2", getFileInWorkspace(project, ResourcesPlugin.PI_RESOURCES).exists());
		node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		assertEquals("1.3", value, node.get(key, null));
		ensureDoesNotExistInWorkspace(project.getFolder(DIR_NAME));
		node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		assertNull("2.0", node.get(key, null));
	}

	/*
	 * Bug 95052 - external property removals are not detected.
	 */
	public void test_95052() {
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		Preferences node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		node.put("key1", "value1");
		node.put("key2", "value2");
		node.put("key3", "value3");
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.1", e);
		}
		IFile prefFile = getFileInWorkspace(project, ResourcesPlugin.PI_RESOURCES);
		assertTrue("1.2", prefFile.exists());
		Properties properties = new Properties();
		InputStream contents = null;
		try {
			contents = prefFile.getContents();
		} catch (CoreException e) {
			fail("1.3", e);
		}
		try {
			properties.load(contents);
		} catch (IOException e) {
			fail("1.4", e);
		} finally {
			try {
				contents.close();
			} catch (IOException e) {
				//
			}
		}
		assertEquals("2.0", "value1", properties.get("key1"));
		assertEquals("2.1", "value2", properties.get("key2"));
		assertEquals("2.2", "value3", properties.get("key3"));
		// add a new property
		properties.put("key0", "value0");
		// change an existing property
		properties.put("key2", "value2".toUpperCase());
		// removes a property
		properties.remove("key3");
		ByteArrayOutputStream tempOutput = new ByteArrayOutputStream();
		try {
			properties.store(tempOutput, null);
		} catch (IOException e) {
			// should never happen, we are not doing I/O
			fail("2.4", e);
		}
		ByteArrayInputStream tempInput = new ByteArrayInputStream(tempOutput.toByteArray());
		try {
			prefFile.setContents(tempInput, false, false, getMonitor());
		} catch (CoreException e) {
			fail("2.5", e);
		}

		// here, project preferences should have caught up with the changes
		node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		// property was added
		assertEquals("3.0", "value0", node.get("key0", null));
		// property value was not changed
		assertEquals("3.1", "value1", node.get("key1", null));
		// property value was changed to upper case
		assertEquals("3.2", "value2".toUpperCase(), node.get("key2", null));
		// property was deleted
		assertNull("3.3", node.get("key3", null));
	}

	/*
	 * Bug 579372 - property removals are not detected.
	 */
	public void test_579372() throws Exception {
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		Preferences node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		node.put("key1", "value1");
		node.node("child").put("key", "childValue1");
		node.node("child").node("node").put("key", "childValue2");
		node.flush();
		IFile prefFile = getFileInWorkspace(project, ResourcesPlugin.PI_RESOURCES);
		assertTrue("Preferences missing", prefFile.exists());
		Properties properties = new Properties();
		try (InputStream contents = prefFile.getContents()) {
			properties.load(contents);
		}
		assertEquals("value1", properties.get("key1"));
		assertEquals("childValue1", properties.get("child/key"));
		assertEquals("childValue2", properties.get("child/node/key"));
		assertEquals(ResourcesPlugin.getEncoding(), properties.get("encoding/<project>"));

		// adds property
		properties.put("key0", "value0");
		// adds property
		properties.put("child2/key", "childValue3");
		// changes property
		properties.put("key1", "value2");
		// removes a property
		properties.remove("child/key");
		properties.remove("child/node/key");
		properties.remove("encoding/<project>");

		ByteArrayOutputStream tempOutput = new ByteArrayOutputStream();
		properties.store(tempOutput, null);
		ByteArrayInputStream tempInput = new ByteArrayInputStream(tempOutput.toByteArray());
		prefFile.setContents(tempInput, false, false, getMonitor());

		// here, project preferences should have caught up with the changes
		node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		// property was added
		assertEquals("value0", node.get("key0", null));
		// property value changed
		assertEquals("value2", node.get("key1", null));

		List<String> children = List.of(node.childrenNames());
		assertTrue(children.contains("child"));
		assertTrue(children.contains("child2"));
		assertTrue(children.contains("encoding"));

		// Added "child2/key" property
		assertEquals(Arrays.asList("key"), Arrays.asList(node.node("child2").keys()));
		assertEquals("childValue3", node.node("child2").get("key", null));

		// Removed "child/key" and "child/node/key" have no values anymore (only
		// structure)
		assertEquals(Arrays.asList(), Arrays.asList(node.node("child").keys()));
		assertEquals(Arrays.asList("node"), Arrays.asList(node.node("child").childrenNames()));
		assertEquals(Arrays.asList(), Arrays.asList(node.node("child").node("node").keys()));
		assertEquals(Arrays.asList(), Arrays.asList(node.node("child").node("node").childrenNames()));

		// Removed "encoding/<project>" has no value anymore (only structure)
		assertEquals(Arrays.asList(), Arrays.asList(node.node("encoding").keys()));
		assertEquals(Arrays.asList(), Arrays.asList(node.node("encoding").childrenNames()));
	}

	/*
	 * Bug 256900 - When the preferences file is copied between projects, the corresponding preferences
	 * should be updated.
	 */
	public void test_256900() {
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);

		// create the destination project and the .settings folder inside
		IProject project2 = getProject(getUniqueString());
		ensureExistsInWorkspace(project2, true);
		ensureExistsInWorkspace(project2.getFolder(DIR_NAME), true);

		// get the pref node for the project and add a sample key/value to it
		Preferences node = new ProjectScope(project).getNode(ResourcesPlugin.PI_RESOURCES);
		String key = "key";
		String value = getUniqueString();
		node.put(key, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}

		IFile prefFile = getFileInWorkspace(project, ResourcesPlugin.PI_RESOURCES);
		assertTrue("2.0", prefFile.exists());

		// get the pref node for the destination project
		Preferences project2Node = new ProjectScope(project2).getNode(ResourcesPlugin.PI_RESOURCES);
		assertNull("3.0", project2Node.get(key, null));

		// copy the pref file to the destination project
		try {
			getFileInWorkspace(project2, ResourcesPlugin.PI_RESOURCES).delete(true, null);
			prefFile.copy(getFileInWorkspace(project2, ResourcesPlugin.PI_RESOURCES).getFullPath(), true, null);
		} catch (CoreException e) {
			fail("4.0", e);
		}

		assertEquals("5.0", value, project2Node.get(key, null));
	}

	/**
	 * Bug 325000 Project properties not sorted on IBM VMs
	 * Creates property file with various characters on front and verifies that they are written in alphabetical order.
	 */
	public void test_325000() {
		IProject project1 = getProject(getUniqueString());
		ensureExistsInWorkspace(new IResource[] {project1}, true);
		Preferences node = new ProjectScope(project1).getNode(ResourcesPlugin.PI_RESOURCES).node("subnode");

		List<String> keys = new ArrayList<>();
		keys.add("z" + getUniqueString());
		keys.add("a" + getUniqueString());
		keys.add("1" + getUniqueString());
		keys.add("z" + getUniqueString());
		keys.add("_" + getUniqueString());
		keys.add(getUniqueString());
		for (String key : keys) {
			node.put(key, getUniqueString());
		}
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}

		IFile prefsFile = getFileInWorkspace(project1, ResourcesPlugin.PI_RESOURCES);
		BufferedReader reader = null;
		try {
			reader = new BufferedReader(new InputStreamReader(prefsFile.getContents()));
			String currentLine = null;
			String prevLine = null;
			while ((currentLine = reader.readLine()) != null) {
				boolean isUserProperty = false;
				for (String key : keys) {
					if (currentLine.contains(key)) {
						isUserProperty = true;
						break;
					}
				}
				if (!isUserProperty) {
					continue;
				}
				if (prevLine == null) {
					prevLine = currentLine;
					continue;
				}
				if (prevLine.compareTo(currentLine) > 0) {
					fail("1.1");
				}
				prevLine = currentLine;
			}
		} catch (CoreException e) {
			fail("1.2", e);
		} catch (IOException e) {
			fail("1.3", e);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
					fail("1.4", e);
				}
			}
		}

	}

	public void test_335591() {
		String projectName = getUniqueString();
		String nodeName = "node";
		IProject project = getProject(projectName);

		//create project but do not open it yet
		try {
			project.create(getMonitor());
		} catch (CoreException e) {
			fail("1.0", e);
		}

		//create file with preferences that will be discovered during refresh
		try {
			File folder = new File(project.getLocation().toOSString() + "/.settings");
			folder.mkdir();
			try (BufferedWriter bw = new BufferedWriter(new FileWriter(folder.getPath() + "/" + nodeName + ".prefs"))) {
				bw.write("#Fri Jan 28 10:28:45 CET 2011\neclipse.preferences.version=1\nKEY=VALUE");
			}
		} catch (IOException e) {
			fail("2.0", e);
		}

		//create /project/<projectName> preference node on closed project so that the node will not get initialized
		//we cannot call new ProjectScope(project) because this does not create /project/<projectName> preference node
		//we could use new ProjectScope(project).getNode("dummyNode") instead - this is the case that happened in bug 334241
		Preferences projectNode = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE).node(projectName);

		//open the project. the new file will be found during refresh and preferences will be loaded into nodes
		try {
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("3.0", e);
		}

		//the node was created on refresh so we can now take the node without creating it
		Preferences node = projectNode.node(nodeName);

		//correct value should be available
		assertEquals("VALUE", node.get("KEY", null));

		//add some preference to make the node dirty
		node.put("NEW_KEY", "NEW_VALUE");

		//node is dirty so we can flush it and the flush should change the content of the file
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("4.0", e);
		}

		//preferences were changed so the new file should contain two lines: 'KEY=VALUE' and 'NEW_KEY=NEW_VALUE'
		try {
			File folder = new File(project.getLocation().toOSString() + "/.settings");
			List<String> lines;
			try (BufferedReader br = new BufferedReader(new FileReader(folder.getPath() + "/" + nodeName + ".prefs"))) {
				lines = new ArrayList<>();
				String line = br.readLine();
				while (line != null) {
					if ((!line.startsWith("#")) && (!line.startsWith("eclipse.preferences.version"))) {
						lines.add(line);
					}
					line = br.readLine();
				}
			}
			assertEquals(2, lines.size());
			lines.sort(null);
			assertTrue(lines.get(0).equals("KEY=VALUE"));
			assertTrue(lines.get(1).equals("NEW_KEY=NEW_VALUE"));
		} catch (IOException e) {
			fail("5.0", e);
		}

		//call sync to reload the node from file
		try {
			node.sync();
		} catch (BackingStoreException e) {
			fail("6.0", e);
		}

		//after reloading both preferences should be available
		assertEquals("VALUE", node.get("KEY", null));
		assertEquals("NEW_VALUE", node.get("NEW_KEY", null));
	}

	public void test_384151() throws BackingStoreException, CoreException {
		// make sure each line separator is different
		String systemValue = System.lineSeparator();
		String newInstanceValue;
		String newProjectValue;
		if (systemValue.equals("\n")) {
			// for unix "\n"
			newInstanceValue = "\r";
			newProjectValue = "\r\n";
		} else if (systemValue.equals("\r")) {
			// for macos "\r"
			newInstanceValue = "\n";
			newProjectValue = "\r\n";
		} else {
			// for windows "\r\n"
			newInstanceValue = "\r";
			newProjectValue = "\n";
		}

		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);

		Preferences rootNode = Platform.getPreferencesService().getRootNode();
		Preferences instanceNode = rootNode.node(InstanceScope.SCOPE).node(Platform.PI_RUNTIME);
		Preferences projectNode = rootNode.node(ProjectScope.SCOPE).node(project.getName()).node(Platform.PI_RUNTIME);
		String oldInstanceValue = instanceNode.get(Platform.PREF_LINE_SEPARATOR, null);
		String oldProjectValue = projectNode.get(Platform.PREF_LINE_SEPARATOR, null);

		String qualifier = "qualifier";
		IFile file = project.getFile(IPath.fromOSString(".settings/" + qualifier + ".prefs"));
		Preferences node = rootNode.node(ProjectScope.SCOPE).node(project.getName()).node(qualifier);
		String key = "key";
		try {
			node.put(key, getUniqueString());
			node.flush();
			// if there is no preference, OS default line separator should be used
			assertEquals("1.0", systemValue, getLineSeparatorFromFile(file));
			file.delete(true, getMonitor());

			instanceNode.put(Platform.PREF_LINE_SEPARATOR, newInstanceValue);
			instanceNode.flush();
			node.put(key, getUniqueString());
			node.flush();
			// if there is instance preference then it should be used
			assertEquals("2.0", newInstanceValue, getLineSeparatorFromFile(file));
			file.delete(true, getMonitor());

			projectNode.put(Platform.PREF_LINE_SEPARATOR, newProjectValue);
			projectNode.flush();
			node.put(key, getUniqueString());
			node.flush();
			// if there is project preference then it should be used
			String recentlyUsedLineSeparator = getLineSeparatorFromFile(file);
			assertEquals("3.0", newProjectValue, recentlyUsedLineSeparator);
			// don't delete the prefs file, it will be used in the next step

			// remove preferences for the next step
			if (oldInstanceValue == null) {
				instanceNode.remove(Platform.PREF_LINE_SEPARATOR);
			} else {
				instanceNode.put(Platform.PREF_LINE_SEPARATOR, oldInstanceValue);
			}
			if (oldProjectValue == null) {
				projectNode.remove(Platform.PREF_LINE_SEPARATOR);
			} else {
				projectNode.put(Platform.PREF_LINE_SEPARATOR, oldProjectValue);
			}
			instanceNode.flush();
			projectNode.flush();
			node.put(key, getUniqueString());
			node.flush();
			// if the prefs file exists, line delimiter from the existing file should be used
			assertEquals("4.0", recentlyUsedLineSeparator, getLineSeparatorFromFile(file));
		} finally {
			// revert instance preference to original value
			if (oldInstanceValue == null) {
				instanceNode.remove(Platform.PREF_LINE_SEPARATOR);
			} else {
				instanceNode.put(Platform.PREF_LINE_SEPARATOR, oldInstanceValue);
			}
			instanceNode.flush();
			project.delete(true, getMonitor());
		}
	}

	public void test_336211() throws BackingStoreException, CoreException, IOException {
		String projectName = getUniqueString();
		String nodeName = "node";
		IProject project = getProject(projectName);

		//create project but do not open it yet
		project.create(getMonitor());

		//create file with preferences that will be discovered during refresh
		File folder = new File(project.getLocation().toOSString() + "/.settings");
		folder.mkdir();
		try (BufferedWriter bw = new BufferedWriter(new FileWriter(folder.getPath() + "/" + nodeName + ".prefs"))) {
			bw.write("#Fri Jan 28 10:28:45 CET 2011\neclipse.preferences.version=1\nKEY=VALUE");
		}

		//create project preference node
		Preferences projectNode = Platform.getPreferencesService().getRootNode().node(ProjectScope.SCOPE).node(projectName);
		assertFalse(projectNode.nodeExists(nodeName));

		//create node for storing preferences
		Preferences node = projectNode.node(nodeName);

		//open the project; the new file will be found during refresh
		project.open(getMonitor());

		//loading preferences from a file must not remove nodes that were previously created
		assertTrue(node == projectNode.node(nodeName));
		assertEquals("VALUE", node.get("KEY", null));
	}

	public void testProjectOpenClose() {
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		String qualifier = getUniqueString();
		String key = getUniqueString();
		String value = getUniqueString();
		Preferences node = new ProjectScope(project).getNode(qualifier);
		node.put(key, value);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.0", e);
		}
		// close the project
		try {
			project.close(getMonitor());
		} catch (CoreException e) {
			fail("1.1", e);
		}
		// now reopen the project and ensure the settings were not forgotten
		try {
			project.open(getMonitor());
		} catch (CoreException e) {
			fail("2.0", e);
		}
		node = new ProjectScope(project).getNode(qualifier);
		assertEquals("2.1", value, node.get(key, null));
	}

	public void testContentType() {
		IContentType prefsType = Platform.getContentTypeManager().getContentType(ResourcesPlugin.PI_RESOURCES + ".preferences");
		assertNotNull("1.0", prefsType);
		IContentType associatedType = Platform.getContentTypeManager().findContentTypeFor("some.qualifier." + EclipsePreferences.PREFS_FILE_EXTENSION);
		assertEquals("1.1", prefsType, associatedType);
	}

	public void testListenerOnChangeFile() {
		// setup
		IProject project = getProject(getUniqueString());
		String qualifier = "org.eclipse.core.tests.resources";
		String key = "key" + getUniqueString();
		String value = "value" + getUniqueString();
		IScopeContext projectContext = new ProjectScope(project);
		// create project
		ensureExistsInWorkspace(project, true);
		// set preferences
		Preferences node = projectContext.getNode(qualifier);
		node.put(key, value);
		Tracer tracer = new Tracer();
		((IEclipsePreferences) node).addPreferenceChangeListener(tracer);
		String actual = node.get(key, null);
		assertNotNull("1.0", actual);
		assertEquals("1.1", value, actual);
		try {
			// flush
			node.flush();
		} catch (BackingStoreException e) {
			fail("0.0", e);
		}

		// get settings filename
		File file = getFileInFilesystem(project, qualifier);
		Properties props = new Properties();
		InputStream input = null;
		try {
			input = new BufferedInputStream(new FileInputStream(file));
			props.load(input);
		} catch (IOException e) {
			fail("1.0", e);
		} finally {
			if (input != null) {
				try {
					input.close();
				} catch (IOException e) {
					// ignore
				}
			}
		}

		// reset the listener
		tracer.log.setLength(0);
		// change settings in the file
		String newKey = "newKey" + getUniqueString();
		String newValue = "newValue" + getUniqueString();
		props.put(newKey, newValue);

		// save the file via the IFile API
		IFile workspaceFile = getFileInWorkspace(project, qualifier);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		try {
			props.store(output, null);
			// don't need to close stream since its a byte array
		} catch (IOException e) {
			fail("2.0", e);
		}
		input = new ByteArrayInputStream(output.toByteArray());
		try {
			workspaceFile.setContents(input, IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("2.1", e);
		}

		// validate new settings
		actual = node.get(key, null);
		assertEquals("4.1", value, actual);
		actual = node.get(newKey, null);
		assertEquals("4.2", newValue, actual);

		// validate the change events
		assertEquals("4.3", "[" + newKey + ":null->S" + newValue + "]", tracer.log.toString());
	}

	private static IProject getProject(String name) {
		return getWorkspace().getRoot().getProject(name);
	}

	private static IFile getFileInWorkspace(IProject project, String qualifier) {
		return project.getFile(IPath.fromOSString(DIR_NAME).append(qualifier).addFileExtension(FILE_EXTENSION));
	}

	private static File getFileInFilesystem(IProject project, String qualifier) {
		return project.getLocation().append(DIR_NAME).append(qualifier).addFileExtension(FILE_EXTENSION).toFile();
	}

	/*
	 * Test to ensure that discovering a new pref file (e.g. loading from a repo)
	 * is the same as doing an import. (ensure the modify listeners are called)
	 */
	public void testLoadIsImport() {

		// setup
		IProject project = getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
		IScopeContext context = new ProjectScope(project);
		String qualifier = "test.load.is.import";
		IEclipsePreferences node = context.getNode(qualifier);
		String key = "key";
		String oldValue = "old value";
		String newValue = "new value";
		IPreferencesService service = Platform.getPreferencesService();

		// set the values in the nodes and flush the values to the file system
		node.put(key, oldValue);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			fail("1.99", e);
		}
		assertEquals("1.00", oldValue, node.get(key, null));

		// copy the data into a buffer for later use
		File fileInFS = getFileInFilesystem(project, qualifier);
		InputStream input = null;
		OutputStream output = null;
		byte[] buffer = null;
		try {
			input = new BufferedInputStream(new FileInputStream(fileInFS));
			output = new ByteArrayOutputStream(1024);
			transferData(input, output);
			buffer = ((ByteArrayOutputStream) output).toByteArray();
		} catch (IOException e) {
			fail("2.99", e);
		}

		// remove the file from the project
		IFile fileInWS = getFileInWorkspace(project, qualifier);
		try {
			fileInWS.delete(IResource.NONE, getMonitor());
		} catch (CoreException e) {
			fail("3.90", e);
		}
		assertTrue("3.0", !fileInWS.exists());
		assertTrue("3.1", !fileInFS.exists());
		IEclipsePreferences projectNode = (IEclipsePreferences) service.getRootNode().node(ProjectScope.SCOPE).node(project.getName());
		try {
			// when the pref file is deleted, the node will be cleared, but not removed
			assertTrue("3.2", isNodeCleared(projectNode, new String[] {qualifier}));
		} catch (BackingStoreException e) {
			fail("3.91", e);
		}
		//		assertNull("3.3", projectNode.node(qualifier).get(oldKey, null));

		// create the file in the project and discover it via a refresh local
		try {
			output = new BufferedOutputStream(new FileOutputStream(fileInFS));
		} catch (FileNotFoundException e) {
			fail("4.90", e);
		}
		input = new BufferedInputStream(new ByteArrayInputStream(buffer));
		transferData(input, output);
		try {
			project.refreshLocal(IResource.DEPTH_INFINITE, getMonitor());
		} catch (CoreException e) {
			fail("4.91", e);
		}
		// ensure that the resource changes happen
		waitForBuild();

		// verification - note that the preference modify listener gets called
		// here so that's why we are checking for "new value" and not the original one
		node = context.getNode(qualifier);
		assertEquals("5.0", newValue, node.get(key, null));
	}

	/**
	 * @param node the node to check
	 * @param childrenNames the names of children to check
	 * @return true, if the node and its children have no associated values
	 * @throws BackingStoreException
	 */
	private boolean isNodeCleared(Preferences node, String[] childrenNames) throws BackingStoreException {
		// check if the node has associate values
		if (node.keys().length != 0) {
			return false;
		}

		// perform a subsequent check for the node children
		Preferences childNode = null;
		for (String childrenName : childrenNames) {
			childNode = node.node(childrenName);
			if (!isNodeCleared(childNode, childNode.childrenNames())) {
				return false;
			}
		}
		return true;
	}

	public void testChildrenNamesAgainstInitialize() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode("");

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode("");
		String[] childrenNames = node.childrenNames();
		assertEquals(2, childrenNames.length);
		assertEquals(nodeA, childrenNames[1]);
		node = node.node(nodeA);
		childrenNames = node.childrenNames();
		assertEquals(1, childrenNames.length);
		assertEquals(nodeB, childrenNames[0]);

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testChildrenNamesAgainstLoad() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA);
		String[] childrenNames = node.childrenNames();
		assertEquals(1, childrenNames.length);
		assertEquals(nodeB, childrenNames[0]);

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testClear() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node(nodeB);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		node.clear();
		assertEquals(0, node.keys().length);
		assertNull(node.get(key, null));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testGet() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node(nodeB);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		assertEquals(value, node.get(key, null));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testKeys() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node(nodeB);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		String[] keys = node.keys();
		assertEquals(1, keys.length);
		assertEquals(key, keys[0]);

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testNodeExistsAgainstInitialize() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode("nodeC");

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode("");
		assertTrue(node.nodeExists(nodeA));
		node = node.node(nodeA);
		assertTrue(node.nodeExists(nodeB));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testNodeExistsAgainstLoad() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node("nodeC");

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA);
		assertTrue(node.nodeExists(nodeB));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testPut() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";
		String anotherValue = "anotherValue";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node(nodeB);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		node.put(key, anotherValue);
		assertEquals(anotherValue, node.get(key, null));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testRemove() throws BackingStoreException, CoreException {
		String nodeA = "nodeA";
		String nodeB = "nodeB";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

		IProject project2 = getProject(getUniqueString());
		project1.move(project2.getFullPath(), IResource.NONE, getMonitor());

		Preferences prefs2 = new ProjectScope(project2).getNode(nodeA).node(nodeB);
		assertEquals(value, prefs2.get(key, null));

		// this will trigger the creation of "phantom" preferences node for the now-missing project
		new ProjectScope(project1).getNode(nodeA).node(nodeB);

		project2.move(project1.getFullPath(), IResource.NONE, getMonitor());

		Preferences node = new ProjectScope(project1).getNode(nodeA).node(nodeB);
		node.remove(key);
		assertNull(node.get(key, null));

		project1.delete(true, getMonitor());
		project2.delete(true, getMonitor());
	}

	public void testDeleteOnFilesystemAndLoad() throws CoreException, BackingStoreException {
		String nodeA = "nodeA";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA);
		prefs1.put(key, value);
		prefs1.flush();

		IFile prefsFile = project1.getFile(".settings/nodeA.prefs");
		File settingsFile = new File(prefsFile.getLocationURI());
		assertTrue(settingsFile.exists());

		ProjectPreferences.updatePreferences(prefsFile);

		settingsFile.delete(); // delete the preference file on file system
		assertFalse(settingsFile.exists());

		// will cause a FNFE in FileSystemResourceManager#read, see bug 521490
		// but it should be handled silently
		ProjectPreferences.updatePreferences(prefsFile);
	}

	public void testSettingsFolderCreatedOutsideWorkspace() throws CoreException, BackingStoreException, IOException {
		String nodeA = "nodeA";
		String key = "key";
		String value = "value";

		IProject project1 = getProject(getUniqueString());
		project1.create(getMonitor());
		project1.open(getMonitor());

		// create the project settings folder on disk, it will be out of sync with the workspace
		// see bug#522214
		File projectFolder = new File(project1.getLocationURI());
		File settingsFolder = new File(projectFolder, ".settings");
		assertTrue(projectFolder.exists());
		assertTrue(settingsFolder.exists());

		// create the preference file also out of synch with the workspace
		File prefsFile = new File(settingsFolder, "nodeA.prefs");
		prefsFile.createNewFile();

		// now add some project preference and save them
		Preferences prefs1 = new ProjectScope(project1).getNode(nodeA);
		prefs1.put(key, value);
		prefs1.flush();
		assertEquals(value, prefs1.get(key, null));

	}
}
