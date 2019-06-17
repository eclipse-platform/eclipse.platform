/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
import java.util.*;
import java.util.List;

import junit.framework.Test;

import org.eclipse.compare.structuremergeviewer.IDiffElement;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.diff.IDiff;
import org.eclipse.team.core.subscribers.Subscriber;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.internal.ccvs.core.CVSTag;
import org.eclipse.team.internal.ccvs.ui.CVSUIPlugin;
import org.eclipse.team.internal.core.subscribers.*;
import org.eclipse.team.internal.ui.synchronize.*;
import org.eclipse.team.tests.ccvs.ui.SubscriberParticipantSyncInfoSource;
import org.eclipse.team.ui.synchronize.*;
import org.eclipse.ui.PartInitException;

/**
 * Tests the change set mode of the synchronize view
 */
public class CVSChangeSetTests extends CVSSyncSubscriberTest {

	public static Test suite() {
		return suite(CVSChangeSetTests.class);
	}
	
	public CVSChangeSetTests() {
		super();
	}
	
	public CVSChangeSetTests(String name) {
		super(name);
	}
	
	private void assertIncomingChangesInSets(IFile[][] files, String[] messages) throws CoreException {
		// Get the workspace subscriber which also creates a participant and page in the sync view
		Subscriber workspaceSubscriber = getWorkspaceSubscriber();
		refresh(workspaceSubscriber);
		ISynchronizeModelElement root = getModelRoot(workspaceSubscriber);
		ChangeSetDiffNode[] nodes = getCheckedInChangeSetNodes(root);
		assertNodesInViewer(workspaceSubscriber, nodes);
		assertEquals("The number of change sets in the sync view do not match the expected number", messages.length, nodes.length);
		for (int i = 0; i < messages.length; i++) {
			String message = messages[i];
			ChangeSetDiffNode node = getCommitSetFor(root, message);
			assertNotNull("The commit set for '" + message + "' is not in the sync view", node);
			List<IResource> filesInSet = new ArrayList<>();
			getFileChildren(node, filesInSet);
			assertTrue("The number of files in the set do not match the expected number", files[i].length == filesInSet.size());
			for (IFile file : files[i]) {
				assertTrue("File " + file.getFullPath() + " is not in the set", filesInSet.contains(file));
			}
		}
	}

	private void assertNodesInViewer(Subscriber workspaceSubscriber, ChangeSetDiffNode[] nodes) throws PartInitException {
		ISynchronizeParticipant participant = SubscriberParticipantSyncInfoSource.getParticipant(workspaceSubscriber);
		SubscriberParticipantPage page = (SubscriberParticipantPage)SubscriberParticipantSyncInfoSource.getSyncViewPage(participant);
		TreeViewer viewer = (TreeViewer)page.getViewer();
		Tree tree = viewer.getTree();
		List<ChangeSetDiffNode> nodeList = new ArrayList<>();
		nodeList.addAll(Arrays.asList(nodes));
		TreeItem[] items = tree.getItems();
		removeTreeItemsFromList(nodeList, items);
		assertTrue("Not all nodes are visible in the view", nodeList.isEmpty());
	}

	private void removeTreeItemsFromList(List<?> nodeList, TreeItem[] items) {
		for (TreeItem item : items) {
			nodeList.remove(item.getData());
			TreeItem[] children = item.getItems();
			removeTreeItemsFromList(nodeList, children);
		}
	}

	private ChangeSetDiffNode[] getCheckedInChangeSetNodes(ISynchronizeModelElement root) {
		List<ChangeSetDiffNode> result = new ArrayList<>();
		IDiffElement[] children = root.getChildren();
		for (IDiffElement element : children) {
			if (element instanceof ChangeSetDiffNode) {
				ChangeSetDiffNode node = (ChangeSetDiffNode)element;
				if (node.getSet() instanceof CheckedInChangeSet) {
					result.add(node);
				}
			}
		}
		return result.toArray(new ChangeSetDiffNode[result.size()]);
	}
	
	private ChangeSetDiffNode[] getActiveChangeSetNodes(ISynchronizeModelElement root) {
		List<ChangeSetDiffNode> result = new ArrayList<>();
		IDiffElement[] children = root.getChildren();
		for (IDiffElement element : children) {
			if (element instanceof ChangeSetDiffNode) {
				ChangeSetDiffNode node = (ChangeSetDiffNode)element;
				if (node.getSet() instanceof ActiveChangeSet) {
					result.add(node);
				}
			}
		}
		return result.toArray(new ChangeSetDiffNode[result.size()]);
	}

	/**
	 * Adds IFiles to the list
	 */
	private void getFileChildren(ISynchronizeModelElement node, List<IResource> list) {
		IResource resource = node.getResource();
		if (resource != null && resource.getType() == IResource.FILE) {
			list.add(resource);
		}
		IDiffElement[] children = node.getChildren();
		for (IDiffElement child : children) {
			getFileChildren((ISynchronizeModelElement)child, list);
		}
		return;
	}

	private ChangeSetDiffNode getCommitSetFor(ISynchronizeModelElement root, String message) {
		IDiffElement[] children = root.getChildren();
		for (IDiffElement element : children) {
			if (element instanceof ChangeSetDiffNode) {
				ChangeSetDiffNode node = (ChangeSetDiffNode)element;
				if (node.getSet().getComment().equals(message)) {
					return node;
				}
			}
		}
		return null;
	}

	private void refresh(Subscriber workspaceSubscriber) throws TeamException {
		workspaceSubscriber.refresh(workspaceSubscriber.roots(), IResource.DEPTH_INFINITE, DEFAULT_MONITOR);
	}

	private void enableChangeSets(Subscriber workspaceSubscriber) throws PartInitException {
		ISynchronizeParticipant participant = SubscriberParticipantSyncInfoSource.getParticipant(workspaceSubscriber);
		SubscriberParticipantPage page = (SubscriberParticipantPage)SubscriberParticipantSyncInfoSource.getSyncViewPage(participant);
		ChangeSetModelManager manager = (ChangeSetModelManager)page.getConfiguration().getProperty(SynchronizePageConfiguration.P_MODEL_MANAGER);
		manager.setCommitSetsEnabled(true);
		page.getConfiguration().setMode(ISynchronizePageConfiguration.BOTH_MODE);
	}

	private void enableCheckedInChangeSets(Subscriber workspaceSubscriber) throws PartInitException {
		enableChangeSets(workspaceSubscriber);
		ISynchronizeParticipant participant = SubscriberParticipantSyncInfoSource.getParticipant(workspaceSubscriber);
		SubscriberParticipantPage page = (SubscriberParticipantPage)SubscriberParticipantSyncInfoSource.getSyncViewPage(participant);
		page.getConfiguration().setMode(ISynchronizePageConfiguration.INCOMING_MODE);
	}
	
	private void enableActiveChangeSets(Subscriber workspaceSubscriber) throws PartInitException {
		enableChangeSets(workspaceSubscriber);
		ISynchronizeParticipant participant = SubscriberParticipantSyncInfoSource.getParticipant(workspaceSubscriber);
		SubscriberParticipantPage page = (SubscriberParticipantPage)SubscriberParticipantSyncInfoSource.getSyncViewPage(participant);
		page.getConfiguration().setMode(ISynchronizePageConfiguration.OUTGOING_MODE);
	}
	
	/*
	 * Wait until all the background handlers have settled and then return the root element in the sync view
	 */
	private ISynchronizeModelElement getModelRoot(Subscriber workspaceSubscriber) throws CoreException {
		IProgressMonitor eventLoopProgressMonitor = new IProgressMonitor() {
			@Override
			public void beginTask(String name, int totalWork) {
			}
			@Override
			public void done() {
			}
			@Override
			public void internalWorked(double work) {
			}
			@Override
			public boolean isCanceled() {
				return false;
			}
			@Override
			public void setCanceled(boolean value) {
			}
			@Override
			public void setTaskName(String name) {
			}
			@Override
			public void subTask(String name) {
			}
			@Override
			public void worked(int work) {
				while (Display.getCurrent().readAndDispatch()) {}
			}
		};
		SubscriberParticipantSyncInfoSource.getCollector(workspaceSubscriber);
		ISynchronizeParticipant participant = SubscriberParticipantSyncInfoSource.getParticipant(workspaceSubscriber);
		ChangeSetCapability capability = ((IChangeSetProvider)participant).getChangeSetCapability();
		SubscriberChangeSetManager activeManager = (SubscriberChangeSetManager)capability.getActiveChangeSetManager();
		activeManager.waitUntilDone(eventLoopProgressMonitor);
		SubscriberParticipantPage page = (SubscriberParticipantPage)SubscriberParticipantSyncInfoSource.getSyncViewPage(participant);
		ChangeSetModelManager manager = (ChangeSetModelManager)page.getConfiguration().getProperty(SynchronizePageConfiguration.P_MODEL_MANAGER);
		AbstractSynchronizeModelProvider provider = (AbstractSynchronizeModelProvider)manager.getActiveModelProvider();
		provider.waitUntilDone(eventLoopProgressMonitor);
		return provider.getModelRoot();
	}

	private ActiveChangeSetManager getActiveChangeSetManager() {
		return CVSUIPlugin.getPlugin().getChangeSetManager();
	}
	
	/*
	 * Assert that the given resources make up the given set both directly
	 * and by what is displayed in the sync view.
	 */
	private void assertInActiveSet(IResource[] resources, ActiveChangeSet set) throws CoreException {
		assertResourcesAreTheSame(resources, set.getResources(), true);
		ISynchronizeModelElement root = getModelRoot(((SubscriberChangeSetManager)getActiveChangeSetManager()).getSubscriber());
		ChangeSetDiffNode node = getChangeSetNodeFor(root, set);
		assertNotNull("Change set " + set.getTitle() + " did not appear in the sync view", node);
		IResource[] outOfSync = getOutOfSyncResources(node);
		assertResourcesAreTheSame(resources, outOfSync, true);
		// Assert that all active sets are visible in the view
		ChangeSet[] sets = getActiveChangeSetManager().getSets();
		for (ChangeSet changeSet : sets) {
			node = getChangeSetNodeFor(root, changeSet);
			assertNotNull("The node for set " + set.getName() + " is not in the view", node);
		}
		ChangeSetDiffNode[] nodes = getActiveChangeSetNodes(root);
		assertNodesInViewer(getWorkspaceSubscriber(), nodes);
	}
	
	private ChangeSetDiffNode getChangeSetNodeFor(ISynchronizeModelElement root, ChangeSet set) {
		IDiffElement[] children = root.getChildren();
		for (IDiffElement element : children) {
			if (element instanceof ChangeSetDiffNode) {
				ChangeSetDiffNode node = (ChangeSetDiffNode)element;
				if (node.getSet() == set) {
					return node;
				}
			}
		}
		return null;
	}

	private IResource[] getOutOfSyncResources(ISynchronizeModelElement element) {
		ArrayList<SyncInfo> arrayList = new ArrayList<>();
		getOutOfSync(element, arrayList);
		SyncInfo[] infos = arrayList.toArray(new SyncInfo[arrayList.size()]);
		IResource[] resources = getResources(infos);
		return resources;
	}

	private IResource[] getResources(SyncInfo[] infos) {
		IResource[] resources = new IResource[infos.length];
		for (int i = 0; i < resources.length; i++) {
			resources[i] = infos[i].getLocal();
		}
		return resources;
	}

	private void getOutOfSync(ISynchronizeModelElement node, List<SyncInfo> list) {
		SyncInfo info = getSyncInfo(node);
		if (info != null && info.getKind() != SyncInfo.IN_SYNC) {
			list.add(info);
		}
		IDiffElement[] children = node.getChildren();
		for (IDiffElement child : children) {
			getOutOfSync((ISynchronizeModelElement)child, list);
		}
		return;
	}
	
	private SyncInfo getSyncInfo(ISynchronizeModelElement node) {
		if (node instanceof IAdaptable) {
			return ((IAdaptable)node).getAdapter(SyncInfo.class);
		}
		return null;
	}

	private void assertResourcesAreTheSame(IResource[] resources1, IResource[] resources2, boolean doNotAllowExtra) {
		if (doNotAllowExtra) {
			if (resources1.length != resources2.length) {
				System.out.println("Expected");
				for (IResource resource : resources1) {
					System.out.println(resource.getFullPath().toString());
				}
				System.out.println("Actual");
				for (IResource resource : resources2) {
					System.out.println(resource.getFullPath().toString());
				}
			}
			assertEquals("The number of resources do not match the expected number", resources1.length, resources2.length);
		}
		for (IResource resource : resources1) {
			boolean found = false;
			for (IResource resource2 : resources2) {
				if (resource2.equals(resource)) {
					found = true;
					break;
				}
			}
			assertTrue("Expected resource " + resource.getFullPath().toString() + " was not present", found);
		}
	}

	/*
	 * Assert that the given resources make up the root set
	 * displayed in the sync view. The root set is those 
	 * resources that are not part of an active change set.
	 */
	private void assertInRootSet(IResource[] resources) throws CoreException {
		ISynchronizeModelElement[] nodes = getNonChangeSetRoots(getModelRoot(((SubscriberChangeSetManager)getActiveChangeSetManager()).getSubscriber()));
		List<SyncInfo> list = new ArrayList<>();
		for (ISynchronizeModelElement element : nodes) {
			getOutOfSync(element, list);
		}
		IResource[] outOfSync = getResources(list.toArray(new SyncInfo[list.size()]));
		// Only require that the expected resources are there but allow extra.
		// This is required because of junk left over from previous tests.
		// This means there is a bug somewhere. But where?
		assertResourcesAreTheSame(resources, outOfSync, false /* allow extra out-of-sync resources */);
		
	}
	
	private ISynchronizeModelElement[] getNonChangeSetRoots(ISynchronizeModelElement modelRoot) {
		List<ISynchronizeModelElement> result = new ArrayList<>();
		IDiffElement[] children = modelRoot.getChildren();
		for (IDiffElement element : children) {
			if (!(element instanceof ChangeSetDiffNode)) {
				result.add((ISynchronizeModelElement) element);
			}
		}
		return result.toArray(new ISynchronizeModelElement[result.size()]);
	}

	public void testSimpleCommit() throws CoreException {
		enableCheckedInChangeSets(getWorkspaceSubscriber());
		
		IProject project = createProject(new String[] { "file1.txt", "file2.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		
		// Modify a file in a copy
		IProject copy = checkoutCopy(project, CVSTag.DEFAULT);
		setContentsAndEnsureModified(copy.getFile("file1.txt"));
		String message1 = "Commit 1";
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE, message1);
		assertIncomingChangesInSets(new IFile[][] {{ project.getFile("file1.txt") }}, new String[] {message1});
		
		// Modify the copy some more
		setContentsAndEnsureModified(copy.getFile("file2.txt"));
		setContentsAndEnsureModified(copy.getFile("folder1/a.txt"));
		String message2 = "Commit 2";
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE, message2);
		assertIncomingChangesInSets(new IFile[][] {
				{ project.getFile("file1.txt") },
				{ project.getFile("file2.txt"), project.getFile("folder1/a.txt") }
				}, new String[] {message1, message2});
		
		// Modify the copy some more
		setContentsAndEnsureModified(copy.getFile("file2.txt"));
		String message3 = "Commit 3";
		commitResources(new IResource[] {copy}, IResource.DEPTH_INFINITE, message3);
		assertIncomingChangesInSets(new IFile[][] {
				{ project.getFile("file1.txt") },
				{ project.getFile("folder1/a.txt") },
				{ project.getFile("file2.txt")}
				}, new String[] {message1, message2, message3});
		
		// Now commit the files in one of the sets and ensure it is removed from the view
		updateResources(new IResource[] { project.getFile("file1.txt")}, false);
		assertIncomingChangesInSets(new IFile[][] {
				{ project.getFile("folder1/a.txt") },
				{ project.getFile("file2.txt")}
				}, new String[] {message2, message3});
	}
	
	public void testSimpleActiveChangeSet() throws CoreException {
		IProject project = createProject(new String[] { "file1.txt", "file2.txt", "folder1/", "folder1/a.txt", "folder1/b.txt"});
		// Enable Change Sets
		enableActiveChangeSets(getWorkspaceSubscriber());
		// Add a folder and file
		IFolder newFolder = project.getFolder("folder2");
		newFolder.create(false, true, null);
		IFile newFile = newFolder.getFile("file.txt");
		newFile.create(new ByteArrayInputStream("Hi There".getBytes()), false, null);
		// Create an active commit set and assert that it appears in the sync view
		ActiveChangeSetManager manager = getActiveChangeSetManager();
		ActiveChangeSet set = manager.createSet("test", new IDiff[0]);
		manager.add(set);
		assertInActiveSet(new IResource[] { }, set);
		assertInRootSet(new IResource[] {newFolder, newFile});
		// Add the new file to the set and assert that the file is in the set and the folder is still at the root
		set.add(new IResource[] { newFile });
		assertInActiveSet(new IResource[] { newFile }, set);
		assertInRootSet(new IResource[] {newFolder });
		// Add the folder to the set
		set.add(new IResource[] { newFolder });
		assertInActiveSet(new IResource[] { newFolder, newFile }, set);
	}
}
