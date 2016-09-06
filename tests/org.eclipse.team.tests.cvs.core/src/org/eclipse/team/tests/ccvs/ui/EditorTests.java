/*******************************************************************************
 * Copyright (c) 2007, 2016 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.tests.ccvs.ui;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ccvs.core.ICVSFile;
import org.eclipse.team.internal.ccvs.core.ICVSRemoteFile;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.tests.ccvs.core.CVSTestSetup;
import org.eclipse.team.tests.ccvs.core.EclipseTest;
import org.eclipse.team.ui.history.IHistoryPageSource;

import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;

import org.eclipse.core.resources.IProject;

import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.model.IWorkbenchAdapter;

import org.eclipse.ui.texteditor.ITextEditor;

import junit.framework.Test;
import junit.framework.TestSuite;

public class EditorTests extends EclipseTest {

	public EditorTests() {
		super();
	}

	public EditorTests(String name) {
		super(name);
	}

	public static Test suite() {
		TestSuite suite = new TestSuite(EditorTests.class);
		return new CVSTestSetup(suite);
	}
	
	public void testOpenEditorOnRevision() throws CoreException, InvocationTargetException {
		IProject project = createProject(new String[] { "file.cvsTest" });
		IEditorPart localPart = IDE.openEditor(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage(), project.getFile("file.cvsTest"));
		assertTrue("The proper local editor was not opened", localPart instanceof TestEditor);
		ICVSRemoteFile remoteFile = (ICVSRemoteFile)CVSWorkspaceRoot.getRemoteResourceFor(project.getFile("file.cvsTest"));
		IEditorPart part = CVSUIPlugin.getPlugin().openEditor(remoteFile, new NullProgressMonitor());
		assertTrue("The proper remote editor was not opened", !(part instanceof TestEditor) && (part instanceof ITextEditor));
		assertNotNull(Adapters.adapt(part.getEditorInput(), IFileRevision.class));
		assertNotNull(Adapters.adapt(part.getEditorInput(), ICVSFile.class));
		assertNotNull(Adapters.adapt(part.getEditorInput(), IResourceVariant.class));
		assertNotNull(Adapters.adapt(part.getEditorInput(), IHistoryPageSource.class));
		assertNotNull(Adapters.adapt(part.getEditorInput(), IWorkbenchAdapter.class));
	}
	
}
