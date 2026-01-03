/*******************************************************************************
 * Copyright (c) 2022 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Ingo Mohr - Issue #166 - Add Preference to Turn Off Warning-Check for Project Specific Encoding
 *******************************************************************************/
package org.eclipse.core.tests.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createTestMonitor;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createUniqueString;
import static org.eclipse.core.tests.resources.ResourceTestUtil.waitForBuild;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assertions;
import org.assertj.core.api.SoftAssertions;
import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.internal.resources.ValidateProjectEncoding;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.tests.resources.util.WorkspaceResetExtension;
import org.eclipse.osgi.util.NLS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * Test for integration of marker
 * {@link org.eclipse.core.resources.ResourcesPlugin#PREF_MISSING_ENCODING_MARKER_SEVERITY}.
 */
@ExtendWith(WorkspaceResetExtension.class)
public class ProjectEncodingTest {

	private static final int IGNORE = -1;

	private IProject project;

	@AfterEach
	public void tearDown() throws Exception {
		if (project != null) {
			project.delete(true, true, null);
		}
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(
				ResourcesPlugin.PREF_MISSING_ENCODING_MARKER_SEVERITY,
				PreferenceInitializer.PREF_MISSING_ENCODING_MARKER_SEVERITY_DEFAULT);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();
	}

	@Test
	public void test_ProjectWithoutEncoding_PreferenceIsSetToIgnore_NoMarkerIsPlaced() throws Exception {
		givenPreferenceIsSetTo(IGNORE);
		whenProjectIsCreated();
		whenProjectSpecificEncodingWasRemoved();
		thenProjectHasNoEncodingMarker();
	}

	@Test
	public void test_ProjectWithoutEncoding_PreferenceIsSetToInfo_InfoMarkerIsPlaced() throws Exception {
		verifyMarkerIsAddedToProjectWithNoEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_INFO);
	}

	@Test
	public void test_ProjectWithoutEncoding_PreferenceIsSetToWarning_WarningMarkerIsPlaced() throws Exception {
		verifyMarkerIsAddedToProjectWithNoEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_WARNING);
	}

	@Test
	public void test_ProjectWithoutEncoding_PreferenceIsSetToError_ErrorMarkerIsPlaced() throws Exception {
		verifyMarkerIsAddedToProjectWithNoEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_ERROR);
	}

	private void verifyMarkerIsAddedToProjectWithNoEncodingIfPreferenceIsSetTo(int severity) throws Exception {
		givenPreferenceIsSetTo(severity);
		whenProjectIsCreated();
		whenProjectSpecificEncodingWasRemoved();
		thenProjectHasEncodingMarkerOfSeverity(severity);
	}

	private void verifyNoMarkerIsAddedToProjectWithEncodingIfPreferenceIsSetTo(int severity) throws Exception {
		givenPreferenceIsSetTo(severity);
		whenProjectIsCreated();
		whenProjectSpecificEncodingWasSet();
		thenProjectHasNoEncodingMarker();
	}

	@Test
	public void test_ProjectWithEncoding_PreferenceIsSetToIgnore_NoMarkerIsPlaced() throws Exception {
		verifyNoMarkerIsAddedToProjectWithEncodingIfPreferenceIsSetTo(IGNORE);
	}

	@Test
	public void test_ProjectWithEncoding_PreferenceIsSetToInfo_NoMarkerIsPlaced() throws Exception {
		verifyNoMarkerIsAddedToProjectWithEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_INFO);
	}

	@Test
	public void test_ProjectWithEncoding_PreferenceIsSetToWarning_NoMarkerIsPlaced() throws Exception {
		verifyNoMarkerIsAddedToProjectWithEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_WARNING);
	}

	@Test
	public void test_ProjectWithEncoding_PreferenceIsSetToError_NoMarkerIsPlaced() throws Exception {
		verifyNoMarkerIsAddedToProjectWithEncodingIfPreferenceIsSetTo(IMarker.SEVERITY_ERROR);
	}

	@Test
	public void test_ProjectWithoutEncoding_PreferenceChanges_MarkerIsReplacedAccordingly() throws Exception {
		givenPreferenceIsSetTo(IMarker.SEVERITY_WARNING);
		whenProjectIsCreated();
		whenProjectSpecificEncodingWasRemoved();
		thenProjectHasEncodingMarkerOfSeverity(IMarker.SEVERITY_WARNING);

		whenPreferenceIsChangedTo(IMarker.SEVERITY_INFO);
		thenProjectHasEncodingMarkerOfSeverity(IMarker.SEVERITY_INFO);

		whenPreferenceIsChangedTo(IMarker.SEVERITY_ERROR);
		thenProjectHasEncodingMarkerOfSeverity(IMarker.SEVERITY_ERROR);

		whenPreferenceIsChangedTo(IGNORE);
		thenProjectHasNoEncodingMarker();

		whenPreferenceIsChangedTo(IMarker.SEVERITY_WARNING);
		thenProjectHasEncodingMarkerOfSeverity(IMarker.SEVERITY_WARNING);
	}

	@Test
	public void test_ProjectEncodingWasAdded_ProblemMarkerIsGone() throws Exception {
		givenPreferenceIsSetTo(IMarker.SEVERITY_WARNING);
		whenProjectIsCreated();
		whenProjectSpecificEncodingWasRemoved();

		thenProjectHasEncodingMarkerOfSeverity(IMarker.SEVERITY_WARNING);

		whenProjectSpecificEncodingWasSet();

		thenProjectHasNoEncodingMarker();
	}

	private void whenPreferenceIsChangedTo(int severity) throws Exception {
		givenPreferenceIsSetTo(severity);
	}

	private void givenPreferenceIsSetTo(int value) throws Exception {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES);
		node.putInt(ResourcesPlugin.PREF_MISSING_ENCODING_MARKER_SEVERITY, value);
		node.flush();
		Job.getJobManager().wakeUp(ValidateProjectEncoding.class);
		Job.getJobManager().join(ValidateProjectEncoding.class, createTestMonitor());
	}

	private void whenProjectIsCreated() throws CoreException {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(createUniqueString());
		createInWorkspace(project);
	}

	private void whenProjectSpecificEncodingWasRemoved() throws Exception {
		project.setDefaultCharset(null, null);
		buildAndWaitForBuildFinish();
	}

	private void whenProjectSpecificEncodingWasSet() throws Exception {
		project.setDefaultCharset("UTF-8", null);
		buildAndWaitForBuildFinish();
	}

	private void thenProjectHasNoEncodingMarker() throws Exception {
		IMarker[] markers = project.findMarkers(ValidateProjectEncoding.MARKER_TYPE, false, IResource.DEPTH_ONE);
		assertThat(markers).describedAs("Expected to find no marker for project specific file encoding").isEmpty();
	}

	private void thenProjectHasEncodingMarkerOfSeverity(int expectedSeverity) throws Exception {
		IProjectMatcher.assertThat(project).hasEncodingMarkerOfSeverity(expectedSeverity);
	}

	private static class IProjectMatcher extends AbstractAssert<IProjectMatcher, IProject> {

		private IProjectMatcher(IProject actual) {
			super(actual, IProjectMatcher.class);
		}

		public static IProjectMatcher assertThat(IProject project) {
			return new IProjectMatcher(project);
		}

		public void hasEncodingMarkerOfSeverity(int expectedSeverity) throws CoreException {
			isNotNull();

			IMarker[] markers = actual.findMarkers(ValidateProjectEncoding.MARKER_TYPE, false, IResource.DEPTH_ONE);
			Assertions.assertThat(markers).hasSize(1).allSatisfy(marker -> {
				String[] attributeNames = { IMarker.MESSAGE, IMarker.SEVERITY, IMarker.LOCATION };
				Object[] values = marker.getAttributes(attributeNames);

				SoftAssertions softAssert = new SoftAssertions();
				softAssert.assertThat(values[0]).as("marker message").isEqualTo(getExpectedMarkerMessage());
				softAssert.assertThat(values[1]).as("marker severity").isEqualTo(expectedSeverity);
				softAssert.assertThat(values[2]).as("marker location").isEqualTo(actual.getFullPath().toString());
				softAssert.assertAll();
			});
		}

		private String getExpectedMarkerMessage() {
			return NLS.bind(Messages.resources_checkExplicitEncoding_problemText, actual.getName());
		}

	}

	private void buildAndWaitForBuildFinish() {
		waitForBuild();
	}

}
