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

import static org.eclipse.core.resources.ResourcesPlugin.getWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createInWorkspace;
import static org.eclipse.core.tests.resources.ResourceTestUtil.createRandomString;
import static org.junit.Assert.assertThrows;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISynchronizer;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.tests.resources.ResourceTest;
import org.eclipse.core.tests.resources.ResourceVisitorVerifier;

/**
 * Resource#accept doesn't obey member flags for the traversal entry point.
 */

public class Bug_028981 extends ResourceTest {

	public void testBug() throws CoreException {
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

		createInWorkspace(new IResource[] {teamPrivateFile, regularFile});
		synchronizer.setSyncInfo(partner, phantomFile, createRandomString().getBytes());
		teamPrivateFile.setTeamPrivateMember(true);
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
		project.accept(verifier);
		assertTrue("1.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		assertThrows(CoreException.class, () -> phantomFile.accept(verifier));

		verifier.reset();
		verifier.addExpected(phantomFile);
		phantomFile.accept(verifier, IResource.DEPTH_INFINITE, IContainer.INCLUDE_PHANTOMS);
		assertTrue("3.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		// no resources should be visited
		teamPrivateFile.accept(verifier);
		assertTrue("4.1 - " + verifier.getMessage(), verifier.isValid());

		verifier.reset();
		verifier.addExpected(teamPrivateFile);
		teamPrivateFile.accept(verifier, IResource.DEPTH_INFINITE, IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS);
		assertTrue("5.1 - " + verifier.getMessage(), verifier.isValid());
	}
}
