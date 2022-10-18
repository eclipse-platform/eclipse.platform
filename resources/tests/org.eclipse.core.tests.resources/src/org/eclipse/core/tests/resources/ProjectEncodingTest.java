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

import static org.hamcrest.MatcherAssert.assertThat;

import org.eclipse.core.internal.resources.PreferenceInitializer;
import org.eclipse.core.internal.resources.ValidateProjectEncoding;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;
import org.hamcrest.*;
import org.junit.Test;

/**
 * Test for integration of marker
 * {@link org.eclipse.core.resources.ResourcesPlugin#PREF_MISSING_ENCODING_MARKER_SEVERITY}.
 */
public class ProjectEncodingTest extends ResourceTest {

	private static final int IGNORE = -1;

	private IProject project;

	@Override
	protected void tearDown() throws Exception {
		if (project != null) {
			project.delete(true, null);
		}
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).putInt(
				ResourcesPlugin.PREF_MISSING_ENCODING_MARKER_SEVERITY,
				PreferenceInitializer.PREF_MISSING_ENCODING_MARKER_SEVERITY_DEFAULT);
		InstanceScope.INSTANCE.getNode(ResourcesPlugin.PI_RESOURCES).flush();

		super.tearDown();
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
		Job.getJobManager().join(ValidateProjectEncoding.class, getMonitor());
	}

	private void whenProjectIsCreated() {
		project = ResourcesPlugin.getWorkspace().getRoot().getProject(getUniqueString());
		ensureExistsInWorkspace(project, true);
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
		assertEquals("Expected to find no marker for project specific file encoding", 0, markers.length);
	}

	private void thenProjectHasEncodingMarkerOfSeverity(int expectedSeverity) throws Exception {
		assertThat(project, hasEncodingMarkerOfSeverity(expectedSeverity));
	}

	private BaseMatcher<IProject> hasEncodingMarkerOfSeverity(final int expectedSeverity) {
		return new DiagnosingMatcher<>() {

			@Override
			public boolean matches(Object item, Description mismatchDescription) {
				IProject theProject = (IProject) item;

				try {
					IMarker[] markers = theProject.findMarkers(ValidateProjectEncoding.MARKER_TYPE, false,
							IResource.DEPTH_ONE);
					if (markers.length == 1) {
						IMarker marker = markers[0];
						String[] attributeNames = { IMarker.MESSAGE, IMarker.SEVERITY, IMarker.LOCATION };
						Object[] values = marker.getAttributes(attributeNames);

						boolean msgOk = getExpectedMarkerMessage().equals(values[0]);
						boolean sevOk = ((Integer) expectedSeverity).equals(values[1]);
						boolean locOk = project.getFullPath().toString().equals(values[2]);

						if (!msgOk) {
							mismatchDescription.appendText("\n has marker message: " + values[0]);
						}
						if (!sevOk) {
							mismatchDescription.appendText("\n has marker severity: " + values[1]);
						}
						if (!locOk) {
							mismatchDescription.appendText("\n has marker location: " + values[2]);
						}

						return msgOk && sevOk && locOk;
					}
					mismatchDescription.appendText("\n has " + markers.length + " encoding markers");

				} catch (CoreException e) {
					mismatchDescription.appendText("\n cannot access markers: " + e.getMessage());
				}
				return false;
			}

			@Override
			public void describeTo(Description description) {
				description.appendText("\n has marker of message: '" + getExpectedMarkerMessage() + "'");
				description.appendText("\n has marker severity: " + expectedSeverity);
				description.appendText("\n has location: <path of the project>");
			}

			private String getExpectedMarkerMessage() {
				return NLS.bind(Messages.resources_checkExplicitEncoding_problemText, project.getName());
			}

		};
	}

	private void buildAndWaitForBuildFinish() {
		buildResources();
		waitForBuild();
	}

}
