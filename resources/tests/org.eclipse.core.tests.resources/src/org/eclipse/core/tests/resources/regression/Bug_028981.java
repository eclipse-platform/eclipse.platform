/*******************************************************************************
 *  Copyright (c) 2000, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.regression;

import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.ResourceVisitorVerifier;

/**
 * Resource#accept doesn't obey member flags for the traversal entry point.
 */

public class Bug_028981 extends ResourceTest {

	public void testBug() {
		final QualifiedName partner = new QualifiedName("org.eclipse.core.tests.resources", "myTarget");
		final IWorkspace workspace = getWorkspace();
		final ISynchronizer synchronizer = workspace.getSynchronizer();
		synchronizer.add(partner);

		IProject project = workspace.getRoot().getProject("MyProject");
		IFile teamPrivateFile = project.getFile("teamPrivate.txt");
		IFile phantomFile = project.getFile("phantom.txt");
		IFile regularFile = project.getFile("regular.txt");
		IFile projectDescriptionFile = project.getFile(".project");
		IFolder settings = project.getFolder(".settings");
		IFile prefs = settings.getFile("org.eclipse.core.resources.prefs");

		ensureExistsInWorkspace(new IResource[] {teamPrivateFile, regularFile}, true);
		try {
			synchronizer.setSyncInfo(partner, phantomFile, getRandomString().getBytes());
		} catch (CoreException e) {
			e.printStackTrace();
			fail("0.5");
		}
		try {
			teamPrivateFile.setTeamPrivateMember(true);
		} catch (CoreException e) {
			e.printStackTrace();
			fail("0.6");
		}
		assertTrue("0.7", !regularFile.isPhantom() && !regularFile.isTeamPrivateMember());
		assertTrue("0.8", teamPrivateFile.isTeamPrivateMember());
		assertTrue("0.8b", teamPrivateFile.exists());
		assertTrue("0.9", phantomFile.isPhantom());
		assertTrue("0.9b", !phantomFile.exists());

		ResourceVisitorVerifier verifier = new ResourceVisitorVerifier();

		verifier.addExpected(project);
		verifier.addExpected(projectDescriptionFile);
		verifier.addExpected(regularFile);
		verifier.addExpected(settings);
		verifier.addExpected(prefs);
		try {
			project.accept(verifier);
		} catch (CoreException e) {
			fail("1.0", e);
		}
		assertTrue("1.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		try {
			phantomFile.accept(verifier);
			fail("2.0 - should fail");
		} catch (CoreException e) {
			//success
		}

		verifier.reset();
		verifier.addExpected(phantomFile);
		try {
			phantomFile.accept(verifier, IResource.DEPTH_INFINITE, IContainer.INCLUDE_PHANTOMS);
		} catch (CoreException e) {
			fail("3.0", e);
		}
		assertTrue("3.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		// no resources should be visited
		try {
			teamPrivateFile.accept(verifier);
		} catch (CoreException e) {
			fail("4.0", e);
		}
		assertTrue("4.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		verifier.addExpected(teamPrivateFile);
		try {
			teamPrivateFile.accept(verifier, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		} catch (CoreException e) {
			fail("5.0", e);
		}
		assertTrue("5.1 - " + verifier.getMessage(), verifier.isValid());
	}
}
