/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corporation and others.
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

import java.util.*;
import junit.framework.ComparisonFailure;
import junit.framework.Test;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.resources.WorkspacePreferences;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.core.tests.resources.AutomatedResourceTests;
import org.eclipse.core.tests.resources.WorkspaceSessionTest;
import org.eclipse.core.tests.session.WorkspaceSessionTestSuite;

public class WorkspacePreferencesTest extends WorkspaceSessionTest {
	private IWorkspace workspace;
	private Preferences preferences;

	/**
	 * @see TestCase#setUp()
	 */
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		workspace = ResourcesPlugin.getWorkspace();
		preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
		workspace.setDescription(Workspace.defaultWorkspaceDescription());
	}

	/**
	 * @see TestCase#tearDown()
	 */
	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		workspace.setDescription(Workspace.defaultWorkspaceDescription());
	}

	/**
	 * Tests properties state in a brand new workspace (must match defaults).
	 */
	public void testDefaults() {
		IWorkspaceDescription description = Workspace.defaultWorkspaceDescription();

		assertEquals("1.0", description, preferences);

		// ensures that all properties in the default workspace description
		// appear as non-default-default properties in the property store
		// Don't include the default build order here as it is equivalent to the
		// String default-default (ResourcesPlugin.PREF_BUILD_ORDER).
		String[] descriptionProperties = {ResourcesPlugin.PREF_AUTO_BUILDING, ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, ResourcesPlugin.PREF_FILE_STATE_LONGEVITY, ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS, ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE, ResourcesPlugin.PREF_MAX_FILE_STATES, ResourcesPlugin.PREF_SNAPSHOT_INTERVAL};
		List<String> defaultPropertiesList = Arrays.asList(preferences.defaultPropertyNames());
		for (String property : descriptionProperties) {
			assertTrue("2.0 - Description property is not default: " + property, defaultPropertiesList.contains(property));
		}
	}

	/**
	 * Makes changes in the preferences and ensure they are reflected in the
	 * workspace description.
	 */
	public void testSetPreferences() {
		preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, true);
		assertTrue("1.0", workspace.getDescription().isAutoBuilding());

		preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, false);
		assertTrue("1.1", !workspace.getDescription().isAutoBuilding());

		preferences.setValue(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, true);
		assertTrue("2.0", workspace.getDescription().getBuildOrder() == null);

		preferences.setValue(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, false);
		assertTrue("2.1", workspace.getDescription().getBuildOrder() != null);

		preferences.setValue(ResourcesPlugin.PREF_BUILD_ORDER, "x/y,:z/z");
		List<String> expectedList = Arrays.asList(new String[] {"x", "y,:z", "z"});
		List<String> actualList = Arrays.asList(workspace.getDescription().getBuildOrder());
		assertEquals("2.2", expectedList, actualList);

		preferences.setValue(ResourcesPlugin.PREF_BUILD_ORDER, "");
		assertTrue("2.3", workspace.getDescription().getBuildOrder().length == 0);

		long snapshotInterval = 800000000L;
		preferences.setValue(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, snapshotInterval);
		assertEquals("3.0", snapshotInterval, workspace.getDescription().getSnapshotInterval());

		long defaultSnapshotInterval = preferences.getDefaultLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL);
		preferences.setValue(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, defaultSnapshotInterval);
		assertEquals("3.1", defaultSnapshotInterval, workspace.getDescription().getSnapshotInterval());

		preferences.setToDefault(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL);
		assertEquals("3.2", defaultSnapshotInterval, workspace.getDescription().getSnapshotInterval());
		assertEquals("Description not synchronized", workspace.getDescription(), preferences);

		preferences.setValue(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, false);
		assertFalse("4.0", workspace.getDescription().isKeepDerivedState());

		preferences.setValue(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, true);
		assertTrue("4.1", workspace.getDescription().isKeepDerivedState());
	}

	/**
	 * Ensures property change events are properly fired when setting workspace description.
	 */
	public void testEvents() {
		IWorkspaceDescription original = workspace.getDescription();

		IWorkspaceDescription modified = workspace.getDescription();
		// 1 - PREF_AUTO_BUILDING
		modified.setAutoBuilding(!original.isAutoBuilding());
		// 2 - PREF_DEFAULT_BUILD_ORDER and 3 - PREF_BUILD_ORDER
		modified.setBuildOrder(new String[] {"a", "b", "c"});
		// 3 - PREF_APPLY_FILE_STATE_POLICY
		modified.setApplyFileStatePolicy(!original.isApplyFileStatePolicy());
		// 4 - PREF_FILE_STATE_LONGEVITY
		modified.setFileStateLongevity((original.getFileStateLongevity() + 1) * 2);
		// 5 - PREF_MAX_BUILD_ITERATIONS
		modified.setMaxBuildIterations((original.getMaxBuildIterations() + 1) * 2);
		// 6 - PREF_MAX_FILE_STATES
		modified.setMaxFileStates((original.getMaxFileStates() + 1) * 2);
		// 7 - PREF_MAX_FILE_STATE_SIZE
		modified.setMaxFileStateSize((original.getMaxFileStateSize() + 1) * 2);
		// 8 - PREF_SNAPSHOT_INTERVAL
		modified.setSnapshotInterval((original.getSnapshotInterval() + 1) * 2);
		// 9 - PREF_SNAPSHOT_INTERVAL
		modified.setKeepDerivedState(!original.isKeepDerivedState());

		final List<String> changedProperties = new LinkedList<>();
		Preferences.IPropertyChangeListener listener = event -> changedProperties.add(event.getProperty());
		try {
			preferences.addPropertyChangeListener(listener);
			try {
				workspace.setDescription(original);
			} catch (CoreException e) {
				fail("1.0", e);
			}
			// no events should have been fired
			assertEquals("1.1 - wrong number of properties changed ", 0, changedProperties.size());
			try {
				workspace.setDescription(modified);
			} catch (CoreException e) {
				fail("2.0", e);
			}
			// the right number of events should have been fired
			assertEquals("2.1 - wrong number of properties changed ", 10, changedProperties.size());
		} finally {
			preferences.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Ensures preferences with both default/non-default values are properly exported/imported.
	 */
	public void testImportExport() {
		IPath originalPreferencesFile = getRandomLocation().append("original.epf");
		IPath modifiedPreferencesFile = getRandomLocation().append("modified.epf");
		try {
			// saves the current preferences (should be the default ones)
			IWorkspaceDescription original = workspace.getDescription();

			// sets a non-used preference to a non-default value so a
			// preferences file can be generated
			preferences.setValue("foo.bar", getRandomString());

			// exports original preferences (only default values - except for bogus preference above)
			try {
				Preferences.exportPreferences(originalPreferencesFile);
			} catch (CoreException e) {
				fail("1.0", e);
			}

			// creates a modified description
			IWorkspaceDescription modified = workspace.getDescription();
			modified.setAutoBuilding(!original.isAutoBuilding());
			modified.setBuildOrder(new String[] {"a", "b", "c"});
			modified.setApplyFileStatePolicy(!original.isApplyFileStatePolicy());
			modified.setFileStateLongevity((original.getFileStateLongevity() + 1) * 2);
			modified.setMaxBuildIterations((original.getMaxBuildIterations() + 1) * 2);
			modified.setMaxFileStates((original.getMaxFileStates() + 1) * 2);
			modified.setMaxFileStateSize((original.getMaxFileStateSize() + 1) * 2);
			modified.setSnapshotInterval((original.getSnapshotInterval() + 1) * 2);
			modified.setKeepDerivedState(!original.isKeepDerivedState());

			// sets modified description
			try {
				workspace.setDescription(modified);
			} catch (CoreException ce) {
				fail("2.0", ce);
			}
			assertEquals("2.1", modified, workspace.getDescription());

			// exports modified preferences
			try {
				Preferences.exportPreferences(modifiedPreferencesFile);
			} catch (CoreException e) {
				fail("3.0", e);
			}

			// imports original preferences
			try {
				Preferences.importPreferences(originalPreferencesFile);
			} catch (CoreException e) {
				fail("4.0", e);
			}
			// ensures preferences exported match the imported ones
			assertEquals("4.1", original, workspace.getDescription());

			// imports modified preferences
			try {
				Preferences.importPreferences(modifiedPreferencesFile);
			} catch (CoreException e) {
				fail("5.0", e);
			}
			// ensures preferences exported match the imported ones
			assertEquals("5.1", modified, workspace.getDescription());
		} finally {
			ensureDoesNotExistInFileSystem(originalPreferencesFile.removeLastSegments(1).toFile());
			ensureDoesNotExistInFileSystem(modifiedPreferencesFile.removeLastSegments(1).toFile());
		}

	}

	/**
	 * Makes changes through IWorkspace#setDescription and checks if the changes
	 * are reflected in the preferences.
	 */
	public void testSetDescription() {
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(false);
		description.setBuildOrder(new String[] {"a", "b,c", "c"});
		description.setFileStateLongevity(60000 * 5);
		description.setMaxBuildIterations(35);
		description.setMaxFileStates(16);
		description.setMaxFileStateSize(100050);
		description.setSnapshotInterval(1234567);
		description.setKeepDerivedState(true);
		try {
			workspace.setDescription(description);
		} catch (CoreException ce) {
			fail("2.0", ce);
		}
		assertEquals("2.1 - Preferences not synchronized", description, preferences);

		// try to make changes without committing them

		// sets current state to a known value
		description.setFileStateLongevity(90000);
		try {
			workspace.setDescription(description);
		} catch (CoreException ce) {
			fail("3.0", ce);
		}
		// try to make a change
		description.setFileStateLongevity(100000);
		// the original value should remain set
		assertEquals("3.1", 90000, workspace.getDescription().getFileStateLongevity());
		assertEquals("3.2", 90000, preferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY));
	}

	/**
	 * Compares the values in a workspace description with the corresponding
	 * properties in a preferences object.
	 */
	public void assertEquals(String message, IWorkspaceDescription description, Preferences preferences) throws ComparisonFailure {
		assertEquals(message + " - 1", description.isAutoBuilding(), preferences.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING));
		assertEquals(message + " - 2", description.getBuildOrder() == null, preferences.getBoolean(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER));
		assertEquals(message + " - 3", WorkspacePreferences.convertStringArraytoString(description.getBuildOrder()), preferences.getString(ResourcesPlugin.PREF_BUILD_ORDER));
		assertEquals(message + " - 4", description.isApplyFileStatePolicy(), preferences.getBoolean(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY));
		assertEquals(message + " - 5", description.getFileStateLongevity(), preferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY));
		assertEquals(message + " - 6", description.getMaxFileStates(), preferences.getInt(ResourcesPlugin.PREF_MAX_FILE_STATES));
		assertEquals(message + " - 7", description.getMaxFileStateSize(), preferences.getLong(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE));
		assertEquals(message + " - 8", description.getSnapshotInterval(), preferences.getLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL));
		assertEquals(message + " - 9", description.getMaxBuildIterations(), preferences.getLong(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS));
		assertEquals(message + " -10", description.isKeepDerivedState(),
				preferences.getBoolean(ResourcesPlugin.PREF_KEEP_DERIVED_STATE));
	}

	/**
	 * Compares two workspace description objects..
	 */
	public void assertEquals(String message, IWorkspaceDescription description1, IWorkspaceDescription description2) throws ComparisonFailure {
		assertEquals(message + " - 1", description1.isAutoBuilding(), description2.isAutoBuilding());
		assertEquals(message + " - 2", description1.getBuildOrder(), description2.getBuildOrder());
		assertEquals(message + " - 3", WorkspacePreferences.convertStringArraytoString(description1.getBuildOrder()), WorkspacePreferences.convertStringArraytoString(description2.getBuildOrder()));
		assertEquals(message + " - 4", description1.isApplyFileStatePolicy(), description2.isApplyFileStatePolicy());
		assertEquals(message + " - 5", description1.getFileStateLongevity(), description2.getFileStateLongevity());
		assertEquals(message + " - 6", description1.getMaxFileStates(), description2.getMaxFileStates());
		assertEquals(message + " - 7", description1.getMaxFileStateSize(), description2.getMaxFileStateSize());
		assertEquals(message + " - 8", description1.getSnapshotInterval(), description2.getSnapshotInterval());
		assertEquals(message + " - 9", description1.getMaxBuildIterations(), description2.getMaxBuildIterations());
		assertEquals(message + " -10", description1.isKeepDerivedState(), description2.isKeepDerivedState());
	}

	public static Test suite() {
		return new WorkspaceSessionTestSuite(AutomatedResourceTests.PI_RESOURCES_TESTS, WorkspacePreferencesTest.class);
	}

}
