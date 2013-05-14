/*******************************************************************************
 * Copyright (c) 2007, 2013 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.core.ProjectSetCapability;
import org.eclipse.team.core.ProjectSetSerializationContext;
import org.eclipse.team.core.RepositoryProviderType;
import org.eclipse.team.core.ScmUrlImportDescription;
import org.eclipse.team.core.Team;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.importing.provisional.BundleImporterDelegate;
import org.eclipse.team.core.importing.provisional.IBundleImporter;
import org.eclipse.team.internal.ccvs.core.CVSProviderPlugin;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.CVSTeamProviderType;
import org.eclipse.team.internal.ccvs.ui.wizards.CVSScmUrlImportWizardPage;
import org.eclipse.team.internal.ui.ProjectSetImporter;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.ui.IScmUrlImportWizardPage;
import org.eclipse.team.ui.TeamUI;
import org.eclipse.ui.PlatformUI;

public class ProjectSetImporterTests extends EclipseTest {

	private final static String PSF_FILENAME = "temp.psf";
	private final static File PSF_FILE = new File(PSF_FILENAME);

	private final static String psf_header_0 = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>";
	private final static String psf_header_1 = "<psf version=\"2.0\">";
	private final static String psf_header_2 = "<provider id=\"org.eclipse.team.cvs.core.cvsnature\">";
	private final static String psf_0 = "<project reference=\"1.0,"
			+ CVSTestSetup.REPOSITORY_LOCATION + ",";
	private final static String psf_1 = ",";
	private final static String psf_2 = "\"/>";
	private final static String psf_footer_0 = "</provider>";
	private final static String psf_footer_1 = "</psf>";

	public ProjectSetImporterTests() {
		super();
	}

	public ProjectSetImporterTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(ProjectSetImporterTests.class);
		return new CVSTestSetup(suite);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		PSF_FILE.delete();
	}

	public void testImportProject() throws TeamException, CoreException {
		IProject project1 = createProject("testImportProject",
				new String[] { "file.txt", "folder1/", "folder1/a.txt" });
		IProject project2 = createProject("testImportProject",
				new String[] { "file.txt", "folder1/", "folder1/a.txt" });

		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(PSF_FILE)),
					true);

			out.println(psf_header_0);
			out.println(psf_header_1);
			out.println("\t" + psf_header_2);
			out.println("\t\t" + psf_0 + project1.getName() /* module */+ psf_1
					+ project1.getName() /* project */+ psf_2);
			out.println("\t\t" + psf_0 + project2.getName() /* module */+ psf_1
					+ project2.getName() /* project */+ psf_2);
			out.println("\t" + psf_footer_0);
			out.println(psf_footer_1);

			project1.delete(true, null);
			project2.delete(true, null);

			IProject[] importProjectSet = ProjectSetImporter.importProjectSet(
					PSF_FILENAME, PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell(), null);
			
			assertEquals(2, importProjectSet.length);
			assertEquals(project1, importProjectSet[0]);
			assertEquals(project2, importProjectSet[1]);
		} catch (InvocationTargetException e) {
			fail("1.", e.getCause());
		} catch (IOException e) {
			fail("2.", e);
		} finally {
			if (out != null)
				out.close();
		}
	}

	public void testBug234149_aFewProviders() throws TeamException,
			CoreException {
		IProject project = createProject("testBug234149_aFewProviders",
				new String[0]);
		IProject project2 = createProject("testBug234149_aFewProviders",
				new String[0]);

		// create psf with two providers
		PrintWriter out = null;
		try {
			out = new PrintWriter(new BufferedWriter(new FileWriter(PSF_FILE)),
					true);

			// add first provider to psf
			out.println(psf_header_0);
			out.println(psf_header_1);
			out.println("\t" + psf_header_2);
			out.println("\t\t" + psf_0 + project.getName() /* module */+ psf_1
					+ project.getName() /* project */+ psf_2);
			out.println("\t" + psf_footer_0);

			// add second provider to psf
			out.println("\t" + psf_header_2);
			out.println("\t\t" + psf_0 + project2.getName() /* module */+ psf_1
					+ project2.getName() /* project */+ psf_2);
			out.println("\t" + psf_footer_0);

			out.println(psf_footer_1);

			project.delete(true, null);
			project2.delete(true, null);

			IProject[] importProjectSet = null;
			importProjectSet = ProjectSetImporter.importProjectSet(
					PSF_FILENAME, PlatformUI.getWorkbench()
							.getActiveWorkbenchWindow().getShell(), null);

			assertEquals(project, importProjectSet[0]);
			assertEquals(project2, importProjectSet[1]);
		} catch (InvocationTargetException e) {
			fail("1.", e.getCause());
		} catch (IOException e) {
			fail("2.", e);
		} finally {
			if (out != null)
				out.close();
		}
	}

	public void testBug298925_noToAll() throws TeamException, CoreException {
		IProject project = createProject("testBug298925_noToAll",
				new String[0]);
		String[] referenceStrings = new String[] { "1.0,"
				+ CVSTestSetup.REPOSITORY_LOCATION + "," + project.getName() /* module */
				+ psf_1 + project.getName() /* project */};
		RepositoryProviderType type = RepositoryProviderType
				.getProviderType(CVSProviderPlugin.getTypeId());
		ProjectSetCapability c = type.getProjectSetCapability();
		/*
		 * ProjectSetSerializationContext.confirmOverwrite gives the same result
		 * as UIProjectSetSerializationContext when there is no project to
		 * overwrite ('No to All' selected).
		 */
		c.addToWorkspace(referenceStrings,
				new ProjectSetSerializationContext(), null);
		// If we got here and no NPE was thrown, we're good.
	}

	public void testScmUrlImport() throws TeamException, CoreException {
		IProject project = createProject("testScmUrlImport", new String[0]);
		project.delete(true, true, null);
		ensureDoesNotExistInWorkspace(project);

		IScmUrlImportWizardPage[] pages = TeamUI.getPages("org.eclipse.team.core.cvs.importer");
		assertEquals(1, pages.length);
		String s = ProjectSetCapability.SCHEME_SCM + ":cvs:" + CVSTestSetup.REPOSITORY_LOCATION + ":" + project.getName();
		ScmUrlImportDescription d = new ScmUrlImportDescription(s, project.getName());
		ScmUrlImportDescription[] selection = new ScmUrlImportDescription[] {d};
		ProjectSetCapability c = new CVSTeamProviderType().getProjectSetCapability();
		
		// this is what every bundle importer should do, should this be in PDE?
		List references = new ArrayList();
		for (int i = 0; i < selection.length; i++) {
			references.add(c.asReference(selection[i].getUri(), selection[i].getProject()));
		}
		c.addToWorkspace((String[]) references.toArray(new String[references.size()]), new ProjectSetSerializationContext(), null);
		assertExistsInWorkspace(project);
	}

	public void testScmUrlImportWithName() throws TeamException, CoreException {
		IProject project = createProject("testScmUrlImportWithName", new String[0]);
		project.delete(true, true, null);
		ensureDoesNotExistInWorkspace(project);

		IScmUrlImportWizardPage[] pages = TeamUI.getPages("org.eclipse.team.core.cvs.importer");
		assertEquals(1, pages.length);
		String s = ProjectSetCapability.SCHEME_SCM + ":cvs:" + CVSTestSetup.REPOSITORY_LOCATION + ":" + project.getName() + ";project=project1";
		ScmUrlImportDescription d = new ScmUrlImportDescription(s, project.getName());
		ScmUrlImportDescription[] selection = new ScmUrlImportDescription[] {d};
		ProjectSetCapability c = new CVSTeamProviderType().getProjectSetCapability();

		// this is what every bundle importer should do, should this be in PDE?
		List references = new ArrayList();
		for (int i = 0; i < selection.length; i++) {
			references.add(c.asReference(selection[i].getUri(), /*selection[i].getProject()*/ null));
		}
		c.addToWorkspace((String[]) references.toArray(new String[references.size()]), new ProjectSetSerializationContext(), null);
		IProject project1 = ResourcesPlugin.getWorkspace().getRoot().getProject("project1");
		assertExistsInWorkspace(project1);
	}

	public void testScmUrlImportWithTag() throws TeamException, CoreException, IOException {
		IProject project = createProject("testScmUrlImportWithTag", new String[0]);
		tagProject(project, new CVSTag("tag", CVSTag.VERSION), false);
		project.delete(true, true, null);
		ensureDoesNotExistInWorkspace(project);

		final IScmUrlImportWizardPage[] pages = TeamUI.getPages("org.eclipse.team.core.cvs.importer");
		assertEquals(1, pages.length);
		String s = ProjectSetCapability.SCHEME_SCM + ":cvs:" + CVSTestSetup.REPOSITORY_LOCATION + ":" + project.getName() + ";tag=tag";;
		ScmUrlImportDescription d = new ScmUrlImportDescription(s, project.getName());
		ScmUrlImportDescription[] selection = new ScmUrlImportDescription[] {d};
		pages[0].setSelection(selection);

		assertTrue(pages[0] instanceof CVSScmUrlImportWizardPage);
		Wizard wizard = new Wizard() {
			public boolean performFinish() {
				// update SCM URLs in descriptions
				pages[0].finish();
				return true;
			}
		};
		wizard.addPage(pages[0]);
		WizardDialog wizardDialog = new WizardDialog(new Shell(Display.getCurrent()), wizard);
		wizardDialog.setBlockOnOpen(false);
		wizardDialog.open();
		// simulate clicking "Import from HEAD" on the CVS import page
		Button useHead = (Button) ReflectionUtils.getField(pages[0], "useHead");
		useHead.setSelection(true);
		wizard.performFinish();
		wizardDialog.close();

		// altered selection, check out from HEAD
		selection = pages[0].getSelection();
		IBundleImporter cvsBundleImporter = getBundleImporter("org.eclipse.team.core.cvs.importer");
		cvsBundleImporter.performImport(selection, null);

		assertExistsInWorkspace(project);
		IProject copy = checkoutCopy(project, CVSTag.DEFAULT);
		// expecting the project to be checked out from HEAD
		assertEquals(project, copy, false, false);
	}

	public void testCvsBundleImporter() throws TeamException, CoreException {
		IBundleImporter cvsBundleImporter = getBundleImporter("org.eclipse.team.core.cvs.importer");;
		// CVS Bundle Importer should be available
		assertNotNull(cvsBundleImporter);

		IProject project = createProject("testCvsBundleImporter", new String[0]);
		project.delete(true, true, null);
		ensureDoesNotExistInWorkspace(project);

		String s = ProjectSetCapability.SCHEME_SCM + ":cvs:" + CVSTestSetup.REPOSITORY_LOCATION + ":" + project.getName();

		Map[] manifests = new Map[1];
		Map map = new HashMap();
		map.put(BundleImporterDelegate.ECLIPSE_SOURCE_REFERENCES, s);
		manifests[0] = map;

		ScmUrlImportDescription[] descriptions = cvsBundleImporter.validateImport(manifests);
		assertEquals(1, descriptions.length);

		cvsBundleImporter.performImport(descriptions, null);
		assertExistsInWorkspace(project);
	}

	private static IBundleImporter getBundleImporter(final String id) {
		IBundleImporter[] importers = Team.getBundleImporters();
		for (int i = 0; i < importers.length; i++) {
			if (importers[i].getId().equals(id))
				return importers[i];
		}
		return null;
	}
}
