/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.cvsresources;

import junit.framework.Test;
import junit.framework.TestSuite;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.CVSException;
import org.eclipse.team.internal.ccvs.core.ICVSFolder;
import org.eclipse.team.internal.ccvs.core.ICVSResource;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

/**
 * What does this class do?
 */
public class EclipseFolderTest extends EclipseTest {
	public EclipseFolderTest() {
		super();
	}
	
	public EclipseFolderTest(String name) {
		super(name);
	}
	
	public static Test suite() {
		TestSuite suite = new TestSuite();
		suite.addTestSuite(EclipseFolderTest.class);
		return new CVSTestSetup(suite);
	}
	
	protected void assertChildrenHaveSync(IContainer root, final boolean hasSync) throws CoreException, CVSException {
		root.accept(new IResourceVisitor() {
			@Override
			public boolean visit(IResource resource) throws CoreException {
				try {
					ICVSResource cvsResource = CVSWorkspaceRoot.getCVSResourceFor(resource);
					if(!cvsResource.isIgnored()) {
						if(resource.getType()==IResource.FILE) {
							assertTrue((cvsResource.getSyncInfo()!=null) == hasSync);
						} else {
							assertTrue((((ICVSFolder)cvsResource).getFolderSyncInfo()!=null) == hasSync);
						}
					}
				} catch(CVSException e) {
					throw new CoreException(e.getStatus());
				}
				return true;
			}
		});
	}
	
	public void testUnmanageFolder() throws CoreException, TeamException {
		IProject project = createProject("testUnmanageFolder_A", new String[] {"a.txt", "folder1/", "folder1/b.txt", "folder1/folder2/", "folder1/folder2/c.txt"});
		ICVSFolder cvsProject = CVSWorkspaceRoot.getCVSFolderFor(project);
		assertChildrenHaveSync(project, true);
		
		// test that unmanaging the project flushes sync info
		cvsProject.unmanage(null);
		assertChildrenHaveSync(project, false);
		
		final IProject projectB = createProject("testUnmanageFolder_B", new String[] {"a.txt", "folder1/", "folder1/b.txt", "folder1/folder2/", "folder1/folder2/c.txt"});
		final ICVSFolder cvsProjectB = CVSWorkspaceRoot.getCVSFolderFor(projectB);
		assertChildrenHaveSync(projectB, true);
		
		// test that unmanaging in a CVS runnable flushes too
		cvsProjectB.run(monitor -> {
			try {
				assertChildrenHaveSync(projectB, true);
				cvsProjectB.unmanage(null);
				assertChildrenHaveSync(projectB, false);
			} catch (CoreException e) {
				throw CVSException.wrapException(e);
			}
		}, null);	
		assertChildrenHaveSync(projectB, false);
	}
}
