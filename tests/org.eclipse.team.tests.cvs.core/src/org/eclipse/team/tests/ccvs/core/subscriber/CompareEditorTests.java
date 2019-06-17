/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.core.subscriber;

import java.io.ByteArrayInputStream;

import junit.framework.Test;

import org.eclipse.compare.*;
import org.eclipse.compare.internal.Utilities;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionTypedElement;
import org.eclipse.team.internal.ui.mapping.ModelCompareEditorInput;
import org.eclipse.team.internal.ui.mapping.ResourceDiffCompareInput;
import org.eclipse.team.internal.ui.synchronize.EditableSharedDocumentAdapter;
import org.eclipse.team.internal.ui.synchronize.LocalResourceTypedElement;
import org.eclipse.team.internal.ui.synchronize.actions.OpenInCompareAction;
import org.eclipse.team.tests.ccvs.ui.ModelParticipantSyncInfoSource;
import org.eclipse.ui.*;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.texteditor.IDocumentProvider;

/**
 * Test the behavior of compare editors opened on diffs.
 */
public class CompareEditorTests extends CVSSyncSubscriberTest {

	public static Test suite() {
		return suite(CompareEditorTests.class);
	}
	
	public CompareEditorTests() {
		super();
	}
	
	public CompareEditorTests(String name) {
		super(name);
	}
	
	protected CVSSyncTreeSubscriber getSubscriber() throws TeamException {
		return (CVSSyncTreeSubscriber)getWorkspaceSubscriber();
	}
	
	private IEditorInput openEditor(Subscriber subscriber, Object element) {
		IEditorInput input = OpenInCompareAction.openCompareEditor(((ModelParticipantSyncInfoSource)getSyncInfoSource()).getConfiguration(subscriber), element, false, false);
		assertNotNull(input);
		assertEditorOpen(input, subscriber);
		return input;
	}
	
	private IEditorInput openEditor(Object element) throws TeamException {
		return openEditor(getSubscriber(), element);
	}
	
	private void assertEditorOpen(IEditorInput input, Subscriber subscriber) {
		waitForCollectors(subscriber);
		while (Display.getCurrent().readAndDispatch()) {};
		IEditorPart part = findOpenEditor(input);
		assertNotNull("The editor is not open", part);
	}

	private void assertEditorClosed(IEditorInput input) {
		while (Display.getCurrent().readAndDispatch()) {};
		IEditorPart part = findOpenEditor(input);
		assertNull("The editor is not closed", part);
	}
	
	private void assertRevisionsEquals(IEditorInput input, IFile file) throws CVSException {
		while (Display.getCurrent().readAndDispatch()) {};
		CompareEditorInput cei = (CompareEditorInput)input;
		ICompareInput node = (ICompareInput)cei.getCompareResult();
		String remoteRevision = ((FileRevisionTypedElement)node.getRight()).getContentIdentifier();
		String localRevision = CVSWorkspaceRoot.getCVSFileFor(file).getSyncInfo().getRevision();
		assertEquals(localRevision, remoteRevision);
	}
	
	private void assertEditorState(IEditorInput input) throws TeamException {
		waitForCollectors();
		while (Display.getCurrent().readAndDispatch()) {};
		CompareEditorInput cei = (CompareEditorInput)input;
		if (cei instanceof ModelCompareEditorInput) {
			ModelCompareEditorInput mcei = (ModelCompareEditorInput) cei;
			ICompareInput ci = (ICompareInput)mcei.getCompareResult();
			if (ci instanceof ResourceDiffCompareInput) {
				ResourceDiffCompareInput rdci = (ResourceDiffCompareInput) ci;
				IFile file = (IFile)rdci.getResource();
				LocalResourceTypedElement element = (LocalResourceTypedElement)rdci.getLeft();
				EditableSharedDocumentAdapter adapter = (EditableSharedDocumentAdapter)element.getAdapter(ISharedDocumentAdapter.class);
				assertTrue(element.exists() == file.exists());
				assertTrue(file.exists() == (adapter != null));
				if (file.exists())
					assertTrue(adapter.isConnected());
				return;
			}
		}
		fail("Unexpected compare input type");
	}
	
	private void closeAllEditors() {
		PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().closeAllEditors(false);
	}
	
	private IEditorPart findOpenEditor(IEditorInput input) {
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IEditorReference[] editorRefs = page.getEditorReferences();						
		for (IEditorReference editorRef : editorRefs) {
			final IEditorPart part = editorRef.getEditor(false /* don't restore editor */);
			if(part != null) {
				IEditorInput testInput = part.getEditorInput();
				if (testInput == input)
					return part;
			}
		}
		return null;
	}
	
	private void dirtyEditor(IFile file, IEditorInput input, String string) {
		FileEditorInput fileEditorInput = new FileEditorInput(file);
		IDocumentProvider provider = SharedDocumentAdapter.getDocumentProvider(fileEditorInput);
		IDocument document = provider.getDocument(fileEditorInput);
		document.set(string);
	}
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		// Need to set both the Compare and Team test flags to true
		Utilities.RUNNING_TESTS = true;
		Utils.RUNNING_TESTS = true;
	}
	
	protected void setTestingFlushOnCompareInputChange(boolean b) {
		// Need to set both the Compare and Team test flags
		Utilities.TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE = b;
		Utils.TESTING_FLUSH_ON_COMPARE_INPUT_CHANGE = b;
	}
	
	@Override
	protected void tearDown() throws Exception {
		closeAllEditors();
		super.tearDown();
	}
	
	private void waitForCollectors() throws TeamException {
		waitForCollectors(getSubscriber());
	}
	
	private void waitForCollectors(Subscriber subcriber) {
		((ModelParticipantSyncInfoSource)getSyncInfoSource()).waitForCollectionToFinish(subcriber);
	}

	public void testCloseOnUpdate() throws CoreException {
		// setup a project with an incoming change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("file1.txt"));
		commitProject(copy);
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Update and assert that the editor gets closed
		updateProject(project, null, false);
		waitForCollectors();
		assertEditorClosed(input);
	}
	
	public void testCloseOnCommit() throws CoreException {
		// Setup a project with an outgoing change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Commit and assert that the editor gets closed
		commitProject(project);
		waitForCollectors();
		assertEditorClosed(input);
	}
	
	public void testSaveOnUpdateWhenDirty() throws CoreException {
		// setup a project with an incoming change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("file1.txt"));
		commitProject(copy);
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Dirty the editor
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		
		// Update and ensure that the contents are written and the editor remains open
		setTestingFlushOnCompareInputChange(true);
		updateProject(project, null, false);
		waitForCollectors();
		assertContentsEqual(project.getFile("file1.txt"), contents);
		// We would like the editor to stay open but its too complicated
		// assertEditorOpen(input);
	}
	
	public void testCloseOnUpdateWhenDirty() throws CoreException {
		// setup a project with an incoming change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		IProject copy = checkoutCopy(project, "-copy");
		String incomingContents = "Incoming change";
		setContentsAndEnsureModified(copy.getFile("file1.txt"), incomingContents);
		commitProject(copy);
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Dirty the editor
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		
		// Update and ensure that the editor is closed
		setTestingFlushOnCompareInputChange(false);
		updateProject(project, null, false);
		waitForCollectors();
		assertContentsEqual(project.getFile("file1.txt"), incomingContents);
		// We would like the editor to close but its too complicated to guarantee
		// assertEditorClosed(input);
	}

	public void testSaveOnCommitWhenDirty() throws CoreException {
		// Setup a project with an outgoing change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Dirty the editor
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		
		// Commit and ensure that the contents are written and the editor remains open
		setTestingFlushOnCompareInputChange(true);
		commitProject(project);
		waitForCollectors();
		assertContentsEqual(project.getFile("file1.txt"), contents);
		// We would like the editor to stay open but its too complicated
		// assertEditorOpen(input);
	}
	
	public void testCloseOnCommitWhenDirty() throws CoreException {
		// Setup a project with an outgoing change that is open in an editor
		IProject project = createProject(new String[] { "file1.txt"});
		String committedContents = "Committed contents";
		setContentsAndEnsureModified(project.getFile("file1.txt"), committedContents);
		refresh(getSubscriber(), project);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		
		// Dirty the editor
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		
		// Commit and ensure that the editor is closed
		setTestingFlushOnCompareInputChange(false);
		commitProject(project);
		waitForCollectors();
		assertContentsEqual(project.getFile("file1.txt"), committedContents);
		// We would like the editor to close but its too complicated to guarantee
		// assertEditorClosed(input);
	}
	
	public void testCloseOnParticipantDispose() throws CoreException {
		IProject project = createProject(new String[] { "file1.txt"});
		CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, v1, true);
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		commitProject(project);
		
		Subscriber subscriber = getSyncInfoSource().createCompareSubscriber(project, v1);
		
		refresh(subscriber, project);
		IEditorInput input = openEditor(subscriber, project.getFile("file1.txt"));
		getSyncInfoSource().disposeSubscriber(subscriber);
		assertEditorClosed(input);
	}
	
	public void testStayOpenOnParticipantDisposeWhenDirty() throws CoreException {
		IProject project = createProject(new String[] { "file1.txt"});
		CVSTag v1 = new CVSTag("v1", CVSTag.VERSION);
		tagProject(project, v1, true);
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		commitProject(project);
		
		Subscriber subscriber = getSyncInfoSource().createCompareSubscriber(project, v1);
		
		refresh(subscriber, project);
		IEditorInput input = openEditor(subscriber, project.getFile("file1.txt"));
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		setTestingFlushOnCompareInputChange(true);
		getSyncInfoSource().disposeSubscriber(subscriber);
		assertEditorOpen(input, subscriber);
	}
	
	public void testUpdateOnRemoteChange() throws CoreException {
		IProject project = createProject(new String[] { "file1.txt"});
		
		// First open the editor on an outgoing change
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		assertRevisionsEquals(input, project.getFile("file1.txt"));
		
		// Now change the remote and refresh the project
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("file1.txt"));
		commitProject(copy);
		refresh(getSubscriber(), project);
		
		// The input revision should now match the remote revision
		assertRevisionsEquals(input, copy.getFile("file1.txt"));
		
	}
	
	public void testUpdateOnRemoteChangeWhenDirty() throws CoreException {
		IProject project = createProject(new String[] { "file1.txt"});
		
		// First open the editor and dirty it
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		assertRevisionsEquals(input, project.getFile("file1.txt"));
		String contents = "this is the file contents";
		dirtyEditor(project.getFile("file1.txt"), input, contents);
		
		// Now change the remote and refresh the project
		IProject copy = checkoutCopy(project, "-copy");
		setContentsAndEnsureModified(copy.getFile("file1.txt"));
		commitProject(copy);
		setTestingFlushOnCompareInputChange(true);
		refresh(getSubscriber(), project);
		
		// The revision should be changed and the contents written to disk
		assertRevisionsEquals(input, copy.getFile("file1.txt"));
		assertContentsEqual(project.getFile("file1.txt"), contents);
		
	}
	
	public void testFileCreation() throws CoreException {
		// Create an outgoing deletion and open an editor
		IProject project = createProject(new String[] { "file1.txt"});
		project.getFile("file1.txt").delete(false, null);
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		assertEditorState(input);
		
		// Recreate the file
		project.getFile("file1.txt").create(new ByteArrayInputStream("Recreated file".getBytes()), false, null);
		assertEditorState(input);
	}
	
	public void testFileDeletion() throws CoreException {
		// Create an outgoing change and open an editor
		IProject project = createProject(new String[] { "file1.txt"});
		setContentsAndEnsureModified(project.getFile("file1.txt"));
		IEditorInput input = openEditor(project.getFile("file1.txt"));
		assertEditorState(input);
		
		// Delete the file
		project.getFile("file1.txt").delete(false, null);
		assertEditorState(input);
	}
	
}
