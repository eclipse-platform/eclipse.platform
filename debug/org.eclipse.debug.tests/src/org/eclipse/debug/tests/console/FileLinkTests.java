/*******************************************************************************
 * Copyright (c) 2020 Paul Pazderski and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Paul Pazderski - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.tests.console;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.tests.AbstractDebugTest;
import org.eclipse.debug.ui.console.FileLink;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.junit.Test;

/**
 * Tests for {@link FileLink}.
 */
public class FileLinkTests extends AbstractDebugTest {

	private IProject testProject;
	private IFile testFile;

	@Override
	public void setUp() throws Exception {
		super.setUp();

		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		testProject = workspace.getRoot().getProject("FileLinkTest-" + UUID.randomUUID());
		testProject.create(null);
		testProject.open(null);
		testFile = testProject.getFile("filelinktest.txt");
		setTestContent("Test file\nSecond line");
	}

	@Override
	public void tearDown() throws Exception {
		if (testProject.exists()) {
			testProject.delete(true, true, null);
		}

		super.tearDown();
	}

	private void setTestContent(String fileContent) throws UnsupportedEncodingException, CoreException {
		ByteArrayInputStream data = new ByteArrayInputStream(fileContent.getBytes(testFile.getCharset()));
		if (testFile.exists()) {
			testFile.setContents(data, IResource.FORCE, null);
		} else {
			testFile.create(data, IResource.FORCE, null);
		}
	}

	@Test
	public void testFileLink() throws Exception {
		FileLink link = new FileLink(testFile, null, -1, -1, -1);
		link.linkActivated();
		assertEquals(testFile.getName(), getActiveEditorFilename());
	}

	@Test
	public void testFileLinkWithOffset() throws Exception {
		FileLink link = new FileLink(testFile, null, 4, 0, -1);
		link.linkActivated();
		assertEquals(testFile.getName(), getActiveEditorFilename());
		ITextSelection selection = getCurrentTextSelection();
		assertNotNull("No selection.", selection);
		assertEquals(4, selection.getOffset());
		assertEquals(0, selection.getLength());
		assertEquals(0, selection.getStartLine());
		assertEquals(0, selection.getEndLine());
	}

	@Test
	public void testFileLinkWithSelection() throws Exception {
		FileLink link = new FileLink(testFile, null, 7, 5, -1);
		link.linkActivated();
		assertEquals(testFile.getName(), getActiveEditorFilename());
		ITextSelection selection = getCurrentTextSelection();
		assertNotNull("No selection.", selection);
		assertEquals(7, selection.getOffset());
		assertEquals(5, selection.getLength());
		assertEquals(0, selection.getStartLine());
		assertEquals(1, selection.getEndLine());

		// if offset + length and line is specified the line should be ignored
		link = new FileLink(testFile, null, 7, 5, 1);
		link.linkActivated();
		assertEquals(testFile.getName(), getActiveEditorFilename());
		selection = getCurrentTextSelection();
		assertNotNull("No selection.", selection);
		assertEquals(7, selection.getOffset());
		assertEquals(5, selection.getLength());
		assertEquals(0, selection.getStartLine());
		assertEquals(1, selection.getEndLine());
	}

	@Test
	public void testFileLinkWithLine() throws Exception {
		FileLink link = new FileLink(testFile, null, -1, -1, 2);
		link.linkActivated();
		assertEquals(testFile.getName(), getActiveEditorFilename());
		ITextSelection selection = getCurrentTextSelection();
		assertNotNull("No selection.", selection);
		assertEquals(10, selection.getOffset());
		assertEquals(11, selection.getLength());
		assertEquals(1, selection.getStartLine());
		assertEquals(1, selection.getEndLine());
	}

	private String getActiveEditorFilename() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		IEditorPart editor = window.getActivePage().getActiveEditor();
		return editor != null ? editor.getEditorInput().getName() : null;
	}

	private ITextSelection getCurrentTextSelection() {
		IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		ISelection selection = window.getSelectionService().getSelection();
		if (selection instanceof ITextSelection) {
			return (ITextSelection) selection;
		}
		return null;
	}
}
