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

import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.harness.FileSystemHelper.getRandomLocation;
import static org.eclipse.core.tests.resources.ResourceTestPluginConstants.PI_RESOURCES_TESTS;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.removeFromFileSystem;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import org.eclipse.core.internal.resources.Workspace;
import org.eclipse.core.internal.resources.WorkspacePreferences;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Preferences;
import org.eclipse.core.tests.harness.session.SessionTestExtension;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

@SuppressWarnings("deprecation")
public class WorkspacePreferencesTest {

	@RegisterExtension
	SessionTestExtension sessionTestExtension = SessionTestExtension.forPlugin(PI_RESOURCES_TESTS)
			.withCustomization(SessionTestExtension.createCustomWorkspace()).create();

	private IWorkspace workspace;
	private Preferences preferences;

	@BeforeEach
	public void retrieveWorkspaceAndPreferences() throws CoreException {
		workspace = ResourcesPlugin.getWorkspace();
		preferences = ResourcesPlugin.getPlugin().getPluginPreferences();
	}

	@AfterEach
	public void restoreWorkspaceDescription() throws CoreException {
		workspace.setDescription(Workspace.defaultWorkspaceDescription());
	}

	/**
	 * Tests properties state in a brand new workspace (must match defaults).
	 */
	@Test
	public void testDefaults() throws CoreException {
		IWorkspaceDescription description = Workspace.defaultWorkspaceDescription();

		assertMatchesPreferences(preferences, description);

		// ensures that all properties in the default workspace description
		// appear as non-default-default properties in the property store
		// Don't include the default build order here as it is equivalent to the
		// String default-default (ResourcesPlugin.PREF_BUILD_ORDER).
		String[] descriptionProperties = { ResourcesPlugin.PREF_AUTO_BUILDING, ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER,
				ResourcesPlugin.PREF_FILE_STATE_LONGEVITY, ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS,
				ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE, ResourcesPlugin.PREF_MAX_FILE_STATES,
				ResourcesPlugin.PREF_SNAPSHOT_INTERVAL };
		List<String> defaultPropertiesList = Arrays.asList(preferences.defaultPropertyNames());
		for (String property : descriptionProperties) {
			assertThat(defaultPropertiesList).as("check description properties are default").contains(property);
		}
	}

	/**
	 * Makes changes in the preferences and ensure they are reflected in the
	 * workspace description.
	 */
	@Test
	public void testSetPreferences() throws CoreException {
		preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, true);
		assertThat(workspace.getDescription()).matches(IWorkspaceDescription::isAutoBuilding, "is auto-building");

		preferences.setValue(ResourcesPlugin.PREF_AUTO_BUILDING, false);
		assertThat(workspace.getDescription()).matches(not(IWorkspaceDescription::isAutoBuilding),
				"is not auto-building");

		preferences.setValue(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, true);
		assertThat(workspace.getDescription().getBuildOrder()).isNull();

		preferences.setValue(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER, false);
		assertThat(workspace.getDescription().getBuildOrder()).isNotNull();

		preferences.setValue(ResourcesPlugin.PREF_BUILD_ORDER, "x/y,:z/z");
		assertThat(workspace.getDescription().getBuildOrder()).containsExactly("x", "y,:z", "z");

		preferences.setValue(ResourcesPlugin.PREF_BUILD_ORDER, "");
		assertThat(workspace.getDescription().getBuildOrder()).isEmpty();

		long snapshotInterval = 800000000L;
		preferences.setValue(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, snapshotInterval);
		assertThat(workspace.getDescription().getSnapshotInterval()).isEqualTo(snapshotInterval);

		long defaultSnapshotInterval = preferences.getDefaultLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL);
		preferences.setValue(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL, defaultSnapshotInterval);
		assertThat(workspace.getDescription().getSnapshotInterval()).isEqualTo(defaultSnapshotInterval);

		preferences.setToDefault(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL);
		assertThat(workspace.getDescription().getSnapshotInterval()).isEqualTo(defaultSnapshotInterval);
		assertMatchesPreferences(preferences, workspace.getDescription());

		preferences.setValue(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, false);
		assertThat(workspace.getDescription()).matches(not(IWorkspaceDescription::isKeepDerivedState),
				"does not keep derived state");

		preferences.setValue(ResourcesPlugin.PREF_KEEP_DERIVED_STATE, true);
		assertThat(workspace.getDescription()).matches(IWorkspaceDescription::isKeepDerivedState,
				"keeps derived state");
	}

	/**
	 * Ensures property change events are properly fired when setting workspace
	 * description.
	 */
	@Test
	public void testEvents() throws CoreException {
		IWorkspaceDescription original = workspace.getDescription();

		IWorkspaceDescription modified = workspace.getDescription();
		// 1 - PREF_AUTO_BUILDING
		modified.setAutoBuilding(!original.isAutoBuilding());
		// 2 - PREF_DEFAULT_BUILD_ORDER and 3 - PREF_BUILD_ORDER
		modified.setBuildOrder(new String[] { "a", "b", "c" });
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
			workspace.setDescription(original);
			assertThat(changedProperties).as("check no events have been fired").isEmpty();
			workspace.setDescription(modified);
			assertThat(changedProperties).as("check right number of events has been fired").hasSize(10);
		} finally {
			preferences.removePropertyChangeListener(listener);
		}
	}

	/**
	 * Ensures preferences with both default/non-default values are properly
	 * exported/imported.
	 */
	@Test
	public void testImportExport() throws CoreException {
		IPath originalPreferencesFile = getRandomLocation().append("original.epf");
		IPath modifiedPreferencesFile = getRandomLocation().append("modified.epf");
		try {
			// saves the current preferences (should be the default ones)
			IWorkspaceDescription original = workspace.getDescription();

			// sets a non-used preference to a non-default value so a
			// preferences file can be generated
			preferences.setValue("foo.bar", createRandomString());

			// exports original preferences (only default values - except for bogus
			// preference above)
			Preferences.exportPreferences(originalPreferencesFile);

			// creates a modified description
			IWorkspaceDescription modified = workspace.getDescription();
			modified.setAutoBuilding(!original.isAutoBuilding());
			modified.setBuildOrder(new String[] { "a", "b", "c" });
			modified.setApplyFileStatePolicy(!original.isApplyFileStatePolicy());
			modified.setFileStateLongevity((original.getFileStateLongevity() + 1) * 2);
			modified.setMaxBuildIterations((original.getMaxBuildIterations() + 1) * 2);
			modified.setMaxFileStates((original.getMaxFileStates() + 1) * 2);
			modified.setMaxFileStateSize((original.getMaxFileStateSize() + 1) * 2);
			modified.setSnapshotInterval((original.getSnapshotInterval() + 1) * 2);
			modified.setKeepDerivedState(!original.isKeepDerivedState());

			// sets modified description
			workspace.setDescription(modified);
			assertDescriptionEquals(modified, workspace.getDescription());

			// exports modified preferences
			Preferences.exportPreferences(modifiedPreferencesFile);

			// imports original preferences
			Preferences.importPreferences(originalPreferencesFile);
			// ensures preferences exported match the imported ones
			assertDescriptionEquals(original, workspace.getDescription());

			// imports modified preferences
			Preferences.importPreferences(modifiedPreferencesFile);

			// ensures preferences exported match the imported ones
			assertDescriptionEquals(modified, workspace.getDescription());
		} finally {
			removeFromFileSystem(originalPreferencesFile.removeLastSegments(1).toFile());
			removeFromFileSystem(modifiedPreferencesFile.removeLastSegments(1).toFile());
		}
	}

	/**
	 * Makes changes through IWorkspace#setDescription and checks if the changes are
	 * reflected in the preferences.
	 */
	@Test
	public void testSetDescription() throws CoreException {
		IWorkspaceDescription description = workspace.getDescription();
		description.setAutoBuilding(false);
		description.setBuildOrder(new String[] { "a", "b,c", "c" });
		description.setFileStateLongevity(60000 * 5);
		description.setMaxBuildIterations(35);
		description.setMaxFileStates(16);
		description.setMaxFileStateSize(100050);
		description.setSnapshotInterval(1234567);
		description.setKeepDerivedState(true);
		workspace.setDescription(description);
		assertMatchesPreferences(preferences, description);

		// try to make changes without committing them

		// sets current state to a known value
		description.setFileStateLongevity(90000);
		workspace.setDescription(description);

		// try to make a change
		description.setFileStateLongevity(100000);
		// the original value should remain set
		assertThat(workspace.getDescription().getFileStateLongevity()).isEqualTo(90000);
		assertThat(preferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY)).isEqualTo(90000);
	}

	/**
	 * Compares the values in a workspace description with the corresponding
	 * properties in a preferences object.
	 */
	private void assertMatchesPreferences(Preferences expectedPreferences, IWorkspaceDescription actualDescription) {
		assertThat(actualDescription.isAutoBuilding()).as("check auto building")
				.isEqualTo(expectedPreferences.getBoolean(ResourcesPlugin.PREF_AUTO_BUILDING));
		assertThat(actualDescription.getBuildOrder() == null).as("check default build order")
				.isEqualTo(expectedPreferences.getBoolean(ResourcesPlugin.PREF_DEFAULT_BUILD_ORDER));
		assertThat(WorkspacePreferences.convertStringArraytoString(actualDescription.getBuildOrder()))
				.as("check build order").isEqualTo(expectedPreferences.getString(ResourcesPlugin.PREF_BUILD_ORDER));
		assertThat(actualDescription.isApplyFileStatePolicy()).as("check apply file state policy")
				.isEqualTo(expectedPreferences.getBoolean(ResourcesPlugin.PREF_APPLY_FILE_STATE_POLICY));
		assertThat(actualDescription.getFileStateLongevity()).as("check file state longevity")
				.isEqualTo(expectedPreferences.getLong(ResourcesPlugin.PREF_FILE_STATE_LONGEVITY));
		assertThat(actualDescription.getMaxFileStates()).as("check max files states")
				.isEqualTo(expectedPreferences.getInt(ResourcesPlugin.PREF_MAX_FILE_STATES));
		assertThat(actualDescription.getMaxFileStateSize()).as("check max file state size")
				.isEqualTo(expectedPreferences.getLong(ResourcesPlugin.PREF_MAX_FILE_STATE_SIZE));
		assertThat(actualDescription.getSnapshotInterval()).as("check snapshot interval")
				.isEqualTo(expectedPreferences.getLong(ResourcesPlugin.PREF_SNAPSHOT_INTERVAL));
		assertThat(actualDescription.getMaxBuildIterations()).as("check max build iterations")
				.isEqualTo(expectedPreferences.getLong(ResourcesPlugin.PREF_MAX_BUILD_ITERATIONS));
		assertThat(actualDescription.isKeepDerivedState()).as("check keep derived state")
				.isEqualTo(expectedPreferences.getBoolean(ResourcesPlugin.PREF_KEEP_DERIVED_STATE));
	}

	/**
	 * Compares two workspace description objects..
	 */
	private void assertDescriptionEquals(IWorkspaceDescription expectedDescription,
			IWorkspaceDescription actualDescription) {
		assertThat(actualDescription.isAutoBuilding()).as("check auto building")
				.isEqualTo(expectedDescription.isAutoBuilding());
		assertThat(actualDescription.getBuildOrder()).as("check default build order")
				.isEqualTo(expectedDescription.getBuildOrder());
		assertThat(WorkspacePreferences.convertStringArraytoString(actualDescription.getBuildOrder()))
				.as("check build order")
				.isEqualTo(WorkspacePreferences.convertStringArraytoString(expectedDescription.getBuildOrder()));
		assertThat(actualDescription.isApplyFileStatePolicy()).as("check apply file state policy")
				.isEqualTo(expectedDescription.isApplyFileStatePolicy());
		assertThat(actualDescription.getFileStateLongevity()).as("check file state longevity")
				.isEqualTo(expectedDescription.getFileStateLongevity());
		assertThat(actualDescription.getMaxFileStates()).as("check max files states")
				.isEqualTo(expectedDescription.getMaxFileStates());
		assertThat(actualDescription.getMaxFileStateSize()).as("check max file state size")
				.isEqualTo(expectedDescription.getMaxFileStateSize());
		assertThat(actualDescription.getSnapshotInterval()).as("check snapshot interval")
				.isEqualTo(expectedDescription.getSnapshotInterval());
		assertThat(actualDescription.getMaxBuildIterations()).as("check max build iterations")
				.isEqualTo(expectedDescription.getMaxBuildIterations());
		assertThat(actualDescription.isKeepDerivedState()).as("check keep derived state")
				.isEqualTo(expectedDescription.isKeepDerivedState());
	}

}
