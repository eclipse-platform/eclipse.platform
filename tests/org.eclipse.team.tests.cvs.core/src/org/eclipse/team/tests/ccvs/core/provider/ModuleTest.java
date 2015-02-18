/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.core.provider;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import junit.framework.Assert;
import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteResource;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.client.Command.LocalOption;
import org.eclipse.team.internal.ccvs.core.client.Session;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.resources.RemoteFolder;
import org.eclipse.team.internal.ccvs.core.resources.RemoteModule;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.osgi.framework.Bundle;

/**
 * This class tests the Command framework using simple CVS commands
 */
public class ModuleTest extends EclipseTest {
	
	public static final String RESOURCE_PATH = "resources/CommandTest/";
	
	public ModuleTest() {
		super(null);
	}
	
	public ModuleTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite(ModuleTest.class);
		return new CVSTestSetup(suite);
		//return new CVSTestSetup(new ModuleTest("testAliasForFiles"));
	}
	
	private static boolean isSetUp = false;
	
	private static RemoteModule[] remoteModules;
	
	public void setUp() throws TeamException, CoreException, IOException {
		if (isSetUp) return;
		
		// upload the modules definitions file
		Bundle b = Platform.getBundle("org.eclipse.team.tests.cvs.core");
		URL url = b.getEntry(RESOURCE_PATH + "CVSROOT/modules");

		waitMsec(1000);

		IProject cvsroot = checkoutProject(null, "CVSROOT", null);
		InputStream in = url.openStream();
		try {
			cvsroot.getFile("modules").setContents(in, false, false, DEFAULT_MONITOR);
		} finally {
			in.close();
		}
		commitProject(cvsroot);
		
		uploadProject("common");
		
		remoteModules = RemoteModule.getRemoteModules(getRepository(), null, DEFAULT_MONITOR);
		
		isSetUp = true;
	}
	
	protected void uploadProject(String projectName) throws TeamException, IOException, CoreException {
		// locate the test case contents in the plugin resources
		Bundle bundle = Platform.getBundle("org.eclipse.team.tests.cvs.core");
		URL url = bundle.getEntry(RESOURCE_PATH + projectName);
		url = FileLocator.resolve(url);
		Assert.assertTrue(url.getProtocol().equals("file"));
		IPath path = new Path(url.getPath());
		
		// create a project rooted there
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot workspaceRoot = workspace.getRoot();
		IProject project = workspaceRoot.getProject(projectName);
		IProjectDescription projectDescription = workspace.newProjectDescription(projectName);
		projectDescription.setLocation(path);
		project.create(projectDescription, null);
		project.open(null);

		// import the project into CVS
		Session s = new Session(getRepository(), CVSWorkspaceRoot.getCVSFolderFor(project));
		s.open(DEFAULT_MONITOR, true /* open for modification */);
		try {
			Command.IMPORT.execute(s, Command.NO_GLOBAL_OPTIONS, 
				new LocalOption[] {Command.makeArgumentOption(Command.MESSAGE_OPTION, "")},
				new String[] { projectName, "start", "vendor"},
				null,
				DEFAULT_MONITOR);
		} finally {
			s.close();
		}

		// delete the project locally
		project.delete(false /*deleteContent*/, false /*force*/, null);
	}
	
	// XXX Temporary method of checkout (i.e. with vcm_meta
	protected IProject checkoutProject(String projectName, CVSTag tag) throws TeamException, CoreException {
		IProject project = super.checkoutProject(getWorkspace().getRoot().getProject(projectName), null, tag);
		ICVSFolder parent = (ICVSFolder)CVSWorkspaceRoot.getCVSResourceFor(project);
		ICVSResource vcmmeta = CVSWorkspaceRoot.getCVSResourceFor(project.getFile(".vcm_meta"));
		if ( ! vcmmeta.isManaged() && ! parent.getFolderSyncInfo().getIsStatic()) {
			addResources(new IResource[] {project.getFile(".vcm_meta")});
			waitMsec(1000);
			commitProject(project);
		}
		return project;
	}

	/*
	 * Test the following definition
	 * 
	 *   # self referencing modules
	 *   project1 project1
	 */
	public void testSelfReferencingModule() throws TeamException, CoreException, IOException {
		uploadProject("project1");
		IProject project1 = checkoutProject("project1", null);
		ICVSRemoteResource tree = getRemoteTree(project1, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSResourceFor(project1), tree, false, false);
		RemoteModule module = getRemoteModule("project1");
		assertEquals(Path.EMPTY, (RemoteFolder)tree, module, false);
	}
	
	/*
	 * Test the following definition
	 * 
	 * # checkout docs in flattened structure
	 * docs		-d docs common/docs
	 * macros common/macros
	 */
	public void testFlattenedStructure() throws TeamException, CoreException, IOException {
		
		IProject docs = checkoutProject("docs", null);
		ICVSRemoteResource tree = getRemoteTree(docs, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSResourceFor(docs), tree, false, false);
		RemoteModule module = getRemoteModule("docs");
		assertEquals(Path.EMPTY, (RemoteFolder)tree, module, false);
		
		IProject macros = checkoutProject("macros", null);
		tree = getRemoteTree(macros, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSResourceFor(macros), tree, false, false);
		module = getRemoteModule("macros");
		assertEquals(Path.EMPTY, (RemoteFolder)tree, module, false);

	}
	
	/*
	 * Test the following definition
	 * 
	 * # include docs with project
	 * project2		project2 &docs
	 * # only project2
	 * project2-only project2
	 */
	public void testIncludeAndExcludeDocs() throws TeamException, CoreException, IOException {
		uploadProject("project2");
		IProject project2 = checkoutProject("project2", null);
		ICVSRemoteResource tree = getRemoteTree(project2, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSResourceFor(project2), tree, false, false);

		RemoteModule module = getRemoteModule("project2");
		assertEquals(Path.EMPTY, (RemoteFolder)tree, module, false);

		project2 = checkoutProject("project2-only", null);
		tree = getRemoteTree(project2, CVSTag.DEFAULT, DEFAULT_MONITOR);
		assertEquals(Path.EMPTY, CVSWorkspaceRoot.getCVSResourceFor(project2), tree, false, false);

		module = getRemoteModule("project2-only");
		assertEquals(Path.EMPTY, (RemoteFolder)tree, module, false);

	}
	
	/*
	 * Test the following definition
	 * 
	 * # a use of alias
	 * project3-src  project3/src
	 * project3-src_file -a project3-src/file.c mc-src/file.h
	 * project3-sub  project3/sub &project3-src_file
	 */
//	public void testAliasForFiles() throws TeamException, CoreException, IOException {
//		uploadProject("project3");
//		IProject project3 = checkoutProject("project3-sub", null);
//		ICVSRemoteResource tree = getRemoteTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);
//
//		project3 = checkoutProject("project3-src", null);
//		tree = getRemoteTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);
//
//		project3 = checkoutProject("project3-src_file", null);
//		tree = getRemoteTree(project3, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project3), (ICVSResource)tree.getRemote(), false, false);
//	}
	
	/*
	 * Test the following definition
	 * 
	 * # using aliases to provide packaging
	 * project7-common -a project7/common
	 * project7-pc -a project7-common project7/pc
	 * project7-linux -a project7-common project7/linux
	 */
//	public void testAliases() throws TeamException, CoreException, IOException {
//		uploadProject("project7");
//		IProject project7 = checkoutProject("project7-common", null);
//		ICVSRemoteResource tree = getRemoteTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);
//
//		project7 = checkoutProject("project7-pc", null);
//		tree = getRemoteTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);
//
//		project7 = checkoutProject("project7-linux", null);
//		tree = getRemoteTree(project7, CVSTag.DEFAULT, DEFAULT_MONITOR);
//		assertEquals("Local does not match remote", Session.getManagedResource(project7), (ICVSResource)tree.getRemote(), false, false);
//	}
	

	/*
	 * Test the following definition
	 * 
	 * # simple use of module alias
	 * project8-alias -a project8 common
	 */
	public void testSimpleAlias() throws TeamException, CoreException, IOException {
		uploadProject("project8");
		
		// XXX Module checkout will not work yet
		// IProject project8 = checkoutProject("project8-alias", null);
		
		//RemoteModule module = getRemoteModule("project8-alias");
	}
	
	public RemoteModule getRemoteModule(String moduleName) {
		for (int i = 0; i < remoteModules.length; i++) {
			RemoteModule module = remoteModules[i];
			// XXX shouldn't be getName
			if (module.getName().equals(moduleName))
				return module;
		}
		return null;
	}
}

