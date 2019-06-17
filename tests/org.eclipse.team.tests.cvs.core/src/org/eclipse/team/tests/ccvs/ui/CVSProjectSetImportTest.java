/*******************************************************************************
 * Copyright (c) 2007, 2010 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ccvs.core.CVSProjectSetCapability;
import org.eclipse.team.internal.ccvs.core.CVSRepositoryLocationMatcher;
import org.eclipse.team.internal.ccvs.core.ICVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.connection.CVSRepositoryLocation;
import org.eclipse.team.internal.ccvs.core.util.KnownRepositories;
import org.eclipse.team.internal.ccvs.ui.ConfigureRepositoryLocationsDialog;

/**
 * This class tests an algorithm which suggests alternative repository locations
 * when importing a project set.
 */
public class CVSProjectSetImportTest extends TestCase {

	private KnownRepositories knownRepositories;
	private ICVSRepositoryLocation[] savedRepositories;
	
	public CVSProjectSetImportTest() {
		super();
	}

	public CVSProjectSetImportTest(String name) {
		super(name);
	}

	public static Test suite() {
		return new TestSuite(CVSProjectSetImportTest.class);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		knownRepositories = KnownRepositories.getInstance();
		savedRepositories = knownRepositories.getRepositories();
		// dispose all known repositories
		for (ICVSRepositoryLocation savedRepository : savedRepositories) {
			knownRepositories.disposeRepository(savedRepository);
		}
	}

	// Tests of the Alternative Map. The map contains suggested, alternative
	// repositories. It's used to create combo-boxes on the Import Project Set
	// dialog.

	public void testEmptyInfoMap() {
		IProject[] projects = new IProject[] { null };
		Map infoMap = new HashMap();
		Map alternativeMap = CVSRepositoryLocationMatcher
				.prepareSuggestedRepositoryLocations(projects, infoMap);
		assertNull(alternativeMap);

		assertEquals(0, knownRepositories.getRepositories().length);
	}

	public void testEmptyReferenceStrings() throws Exception {
		_testPrepareSuggestedRepositoryLocations(new String[] {}, new String[] {}, new String[][] { {} });
		// There is nothing we can suggest.
	}

	public void testMalformedReferenceString() throws Exception {
		_testPrepareSuggestedRepositoryLocations(new String[] {},
				new String[] { "Hi, I'm a malformed reference string." }, new String[][] { {} });
	}

	public void testUnknownSingleReferenceString() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] {},
				new String[] { "1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { { ":pserver:dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testSelectionForUnknownSingleReferenceString() throws Exception {
		_testDialogDefaultSelection(
				new String[] {},
				new String[] { "1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse" });
	}

	public void testKnownSingleReferenceString() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] { "1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { {} });
	}

	// test for https://bugs.eclipse.org/bugs/show_bug.cgi?id=199108
	public void testSingleKnownRepositoryWithUsername() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:extssh:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[][] {
						{ ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
						{ ":extssh:dev.eclipse.org:/cvsroot/eclipse",
								":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" } });
	}
	
	public void testSingleKnownRepositoryMatchesTwoReferenceStrings() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[][] {
						{ ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
						{ ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testOneMatchingReferenceString() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:extssh:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[][] {
						{ ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" },
						{ ":extssh:dev.eclipse.org:/cvsroot/eclipse",
								":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testTwoMatchingKnownRepositories() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:joe@dev.eclipse.org:/cvsroot/eclipse",
						":pserver:ann@dev.eclipse.org:/cvsroot/eclipse" },
				new String[] { "1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { {
						":pserver:ann@dev.eclipse.org:/cvsroot/eclipse",
						":pserver:joe@dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testThreeKindsOfSuggestions() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:localhost:/cvsroot/project" },
				new String[] { "1.0,:extssh:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { {
						":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:localhost:/cvsroot/project" } });
	}

	public void testOneCompatibleOfTwoKnown() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:joe@dev.eclipse.org:/cvsroot/TECHNOLOGY" },
				new String[] { "1.0,:extssh:joe@dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { {
						":extssh:joe@dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:joe@dev.eclipse.org:/cvsroot/TECHNOLOGY" } });
	}

	public void testTwoUnknownOneCompatibleReferenceStrings() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":extssh:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:ext:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:pserver:dev.eclipse.org:/cvsroot/TECHNOLOGY,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[][] {
						{ ":ext:dev.eclipse.org:/cvsroot/eclipse",
								":extssh:dev.eclipse.org:/cvsroot/eclipse" },
						{ ":pserver:dev.eclipse.org:/cvsroot/TECHNOLOGY",
								":extssh:dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testSelectionForTwoUnknownOneCompatibleReferenceStrings()
			throws Exception {
		_testDialogDefaultSelection(
				new String[] { ":extssh:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:ext:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:pserver:dev.eclipse.org:/cvsroot/TECHNOLOGY,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[] { ":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/TECHNOLOGY" });
	}

	public void testSelectionForOneCompatibleOfTwoKnown() throws Exception {
		_testDialogDefaultSelection(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:joe@dev.eclipse.org:/cvsroot/technology" },
				new String[] { "1.0,:extssh:joe@dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse" });
	}

	public void testCompatibleSuggestionsOrder() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":ext:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] { "1.0,:pserverssh2:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh" },
				new String[][] { {
						":pserverssh2:dev.eclipse.org:/cvsroot/eclipse",
						":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/eclipse",
						":ext:dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testSelectionForCompatibleSuggestionsOrder() throws Exception {
		_testDialogDefaultSelection(
				new String[] { ":ext:dev.eclipse.org:/cvsroot/eclipse",
						":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] { "1.0,:pserverssh2:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[] { ":extssh:dev.eclipse.org:/cvsroot/eclipse" });
	}

	public void testCompatibleSuggestionsOrder2() throws Exception {
		_testPrepareSuggestedRepositoryLocations(
				new String[] { ":ext:dev.eclipse.org:/cvsroot/eclipse",
						":extssh:dev.eclipse.org:/cvsroot/eclipse",
						":pserver:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.cvs.ssh,org.eclipse.team.cvs.ssh",
						"1.0,:pserverssh2:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[][] {
						{ ":pserver:dev.eclipse.org:/cvsroot/eclipse" },
						{ ":pserverssh2:dev.eclipse.org:/cvsroot/eclipse",
								":extssh:dev.eclipse.org:/cvsroot/eclipse",
								":pserver:dev.eclipse.org:/cvsroot/eclipse",
								":ext:dev.eclipse.org:/cvsroot/eclipse" } });
	}

	public void testSelectionForOnlyOneReferenceStringNeedsAdditionalInfo()
			throws Exception {
		_testDialogDefaultSelection(
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse" },
				new String[] {
						"1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core",
						"1.0,:extssh:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[] { null,
						":pserver:dev.eclipse.org:/cvsroot/eclipse" });
	}

	public void testSelectionForUnknownReferenceString() throws Exception {
		_testDialogDefaultSelection(
				new String[] { ":pserver:LOCALHOST:/cvsroot/path" },
				new String[] { "1.0,:pserver:dev.eclipse.org:/cvsroot/eclipse,org.eclipse.team.tests.cvs.core,org.eclipse.team.tests.cvs.core" },
				new String[] { ":pserver:dev.eclipse.org:/cvsroot/eclipse" });

	}

	/**
	 * The main method to test the
	 * <code>CVSProjectSetCapability.isAdditionalRepositoryInformationRequired</code>
	 * method.
	 * 
	 * @param knownLocations
	 *            Array of known repositories as strings. The format used:
	 *            <p>
	 *            <code>:method:[[user][:password]@]hostname[:[port]]/path/to/repository</code>
	 *            </p>
	 * @param referenceStrings
	 *            Array of reference strings as they would exist in a project
	 *            set. The valid format is:
	 *            <p>
	 *            <code>1.0,:method:[[user][:password]@]hostname[:[port]]/path/to/repository,project.name,module.name[,tagname]</code>
	 *            </p>
	 * @param expectedSuggestions
	 *            Two-dimensional array of expected suggestions. Each entry of
	 *            the array will be matched against actual suggestion from the
	 *            alternative map.
	 * @throws Exception
	 */
	private void _testPrepareSuggestedRepositoryLocations(String[] knownLocations,
			String[] referenceStrings, String[][] expectedSuggestions) throws Exception {
		for (String knownLocation : knownLocations) {
			knownRepositories.addRepository(CVSRepositoryLocation.fromString(knownLocation), false);
		}

		Map infoMap = new HashMap(referenceStrings.length);
		IProject[] projects = CVSProjectSetCapability.asProjects(
				referenceStrings, infoMap);

		Map/* <IProject, List<ICVSRepositoryLocation>> */suggestedRepositoryLocations = CVSRepositoryLocationMatcher
				.prepareSuggestedRepositoryLocations(projects, infoMap);
		
		if (suggestedRepositoryLocations != null) {
			for (int i = 0; i < referenceStrings.length; i++) {
				StringTokenizer st = new StringTokenizer(referenceStrings[i],
						",");
				st.nextToken(); // skip "1.0"
				String locationString = st.nextToken();
				CVSRepositoryLocation referenceLocation = CVSRepositoryLocation
						.fromString(locationString);
				List suggestedList = (List) suggestedRepositoryLocations
						.get(referenceLocation);

				if (suggestedList == null) {
					isItReallyAPerfectMatch(referenceLocation);
					assertEquals(0, expectedSuggestions[i].length);
				} else {
					assertEquals(expectedSuggestions[i].length, suggestedList
							.size());

					for (int j = 0; j < expectedSuggestions[i].length; j++) {
						ICVSRepositoryLocation actualSuggestion = (ICVSRepositoryLocation) suggestedList
								.get(j);
						ICVSRepositoryLocation expectedSuggestion = CVSRepositoryLocation
								.fromString(expectedSuggestions[i][j]);
						assertEquals(expectedSuggestion, actualSuggestion);
					}
				}
			}
		}
		assertEquals(knownLocations.length,
				knownRepositories.getRepositories().length);
	}
	
	private static void assertEquals(ICVSRepositoryLocation expected,
			ICVSRepositoryLocation actual) {
		if (expected == actual)
			return;
		assertTrue("expected:<" + expected + "> but was:<" + actual + ">.",
				expected.equals(actual));
		assertEquals("expected:<" + expected + "> but was:<" + actual + ">.",
				expected.getLocation(true), actual.getLocation(true));
	}

	/**
	 * Test the dialog default selection - preselection of a compatible location
	 * if such exists. Match pick using the connection method. Starting from
	 * extssh, followed by pserver and finally ext. If a compatible location is
	 * not available select the location from the project set (position 0).
	 * 
	 * @param knownLocations
	 *            Array of known repositories as strings. The format used:
	 *            <p>
	 *            <code>:method:[[user][:password]@]hostname[:[port]]/path/to/repository</code>
	 *            </p>
	 * @param referenceStrings
	 *            Array of reference strings as they would exist in a project
	 *            set. The valid format is:
	 *            <p>
	 *            <code>1.0,:method:[[user][:password]@]hostname[:[port]]/path/to/repository,project.name,module.name[,tagname]</code>
	 *            </p>
	 * @param expectedSelections
	 *            Array of expected suggestions. Each entry of the array will be
	 *            matched against actual suggestion from the alternative map.
	 * @throws Exception
	 */
	private void _testDialogDefaultSelection(String[] knownLocations,
			String[] referenceStrings, String[] expectedSelections)
			throws Exception {
		// set up values to test
		for (String knownLocation : knownLocations) {
			knownRepositories.addRepository(CVSRepositoryLocation.fromString(knownLocation), false);
		}

		Map infoMap = new HashMap(referenceStrings.length);
		IProject[] projects = CVSProjectSetCapability.asProjects(
				referenceStrings, infoMap);

		Map alternativeMap = CVSRepositoryLocationMatcher
				.prepareSuggestedRepositoryLocations(projects, infoMap);
		assertFalse(alternativeMap.isEmpty());

		// prepare the dialog
		Display display = Display.getCurrent();
		Shell shell = new Shell(display);

		ConfigureRepositoryLocationsDialog dialog = new ConfigureRepositoryLocationsDialog(
				shell, alternativeMap);
		dialog.setBlockOnOpen(false);
		dialog.open();
		Map selected = dialog.getSelected();

		for (int i = 0; i < referenceStrings.length; i++) {
			StringTokenizer st = new StringTokenizer(referenceStrings[i], ",");
			st.nextToken(); // skip "1.0"
			CVSRepositoryLocation referenceLocation = CVSRepositoryLocation
					.fromString(st.nextToken());
			ICVSRepositoryLocation selectedAlternativeRepository = (ICVSRepositoryLocation) selected
					.get(referenceLocation);
			if (expectedSelections[i] == null) {
				isItReallyAPerfectMatch(referenceLocation);
			} else {
				ICVSRepositoryLocation expectedSelection = CVSRepositoryLocation
						.fromString(expectedSelections[i]);
				assertEquals(expectedSelection, selectedAlternativeRepository);
			}
		}

		// clean up after myself
		dialog.getShell().dispose();
		dialog.close();
		dialog = null;
	}

	private void isItReallyAPerfectMatch(
			ICVSRepositoryLocation referenceLocation) {
		// It looks that we'd found a (single!) perfect match for
		// referenceLocation, let's double check if this is true.
		ICVSRepositoryLocation[] repositories = knownRepositories
				.getRepositories();
		boolean matchFound = false;
		for (ICVSRepositoryLocation rl : repositories) {
			if (CVSRepositoryLocationMatcher.isMatching(referenceLocation, rl)) {
				assertFalse("There should be only one perfect match.",
						matchFound);
				matchFound = true;
			}
		}
		assertTrue(matchFound);
	}

}
