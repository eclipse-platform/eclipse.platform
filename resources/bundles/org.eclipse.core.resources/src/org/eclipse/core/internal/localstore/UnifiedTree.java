/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     Martin Oberhuber (Wind River) - [105554] handle cyclic symbolic links
 *     Martin Oberhuber (Wind River) - [232426] shared prefix histories for symlinks
 *     Serge Beauchamp (Freescale Semiconductor) - [252996] add resource filtering
 *     Martin Oberhuber (Wind River) - [292267] OutOfMemoryError due to leak in UnifiedTree
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Sergey Prigogin (Google) -  ongoing development
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.IOException;
import java.nio.file.Path;
import java.util.*;
import java.util.regex.Pattern;
import org.eclipse.core.filesystem.*;
import org.eclipse.core.internal.refresh.RefreshJob;
import org.eclipse.core.internal.resources.*;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.*;
import org.eclipse.core.runtime.jobs.Job;

/**
 * Represents the workspace's tree merged with the file system's tree.
 */
public class UnifiedTree {

	/** Skip advanced link checking, see bug 537449 */
	private static boolean disable_advanced_recursive_link_checks = System.getProperty("org.eclipse.core.resources.disable_advanced_recursive_link_checks") != null; //$NON-NLS-1$

	/** special node to mark the separation of a node's children */
	protected static final UnifiedTreeNode childrenMarker = new UnifiedTreeNode(null, null, null, null, false);

	private static final Iterator<UnifiedTreeNode> EMPTY_ITERATOR = Collections.EMPTY_LIST.iterator();

	/** special node to mark the beginning of a level in the tree */
	protected static final UnifiedTreeNode levelMarker = new UnifiedTreeNode(null, null, null, null, false);

	private static final IFileInfo[] NO_CHILDREN = {};

	/** Singleton to indicate no local children */
	private static final IResource[] NO_RESOURCES = {};

	/**
	 * True if the level of the children of the current node are valid according
	 * to the requested refresh depth, false otherwise
	 */
	protected boolean childLevelValid;

	/** an IFileTree which can be used to build a unified tree*/
	protected IFileTree fileTree;

	/** Spare node objects available for reuse */
	protected ArrayList<UnifiedTreeNode> freeNodes = new ArrayList<>();
	/** tree's actual level */
	protected int level;
	/** our queue */
	protected LinkedList<UnifiedTreeNode> queue;

	/** path prefixes for checking symbolic link cycles */
	protected PrefixPool pathPrefixHistory, rootPathHistory;

	/** tree's root */
	protected IResource root;

	/**
	 * The root must only be a file or a folder.
	 */
	public UnifiedTree(IResource root) {
		setRoot(root);
	}

	/**
	 * Pass in a a root for the tree, a file tree containing all of the entries for
	 * this tree and a flag indicating whether the UnifiedTree should consult the
	 * fileTree where possible for entries
	 *
	 * @param root     root of the tree. Must be file or folder.
	 * @param fileTree an {@link IFileTree} which is used to build the unified tree
	 */
	public UnifiedTree(IResource root, IFileTree fileTree) {
		this(root);
		this.fileTree = fileTree;
	}

	public void accept(IUnifiedTreeVisitor visitor) throws CoreException {
		accept(visitor, IResource.DEPTH_INFINITE);
	}

	/**
	 * Performs a breadth-first traversal of the unified tree, passing each
	 * node to the provided visitor.
	 */
	public void accept(IUnifiedTreeVisitor visitor, int depth) throws CoreException {
		Assert.isNotNull(root);
		initializeQueue();
		setLevel(0, depth);
		while (!queue.isEmpty()) {
			UnifiedTreeNode node = queue.remove();
			if (isChildrenMarker(node))
				continue;
			if (isLevelMarker(node)) {
				if (!setLevel(getLevel() + 1, depth))
					break;
				continue;
			}
			if (visitor.visit(node))
				addNodeChildrenToQueue(node);
			else
				removeNodeChildrenFromQueue(node);
			//allow reuse of the node, but don't let the freeNodes list grow infinitely
			if (freeNodes.size() < 32767) {
				//free memory-consuming elements of the node for garbage collection
				node.releaseForGc();
				freeNodes.add(node);
			}
			//else, the whole node will be garbage collected since there is no
			//reference to it any more.
		}
	}

	protected void addChildren(UnifiedTreeNode node) {
		Resource parent = (Resource) node.getResource();

		// is there a possibility to have children?
		int parentType = parent.getType();
		if (parentType == IResource.FILE && !node.isFolder())
			return;

		//don't refresh resources in closed or non-existent projects
		if (!parent.getProject().isAccessible())
			return;

		// get the list of resources in the file system
		// don't ask for local children if we know it doesn't exist locally
		IFileInfo[] list = node.existsInFileSystem() ? getLocalList(node) : NO_CHILDREN;
		int localIndex = 0;

		// See if the children of this resource have been computed before
		ResourceInfo resourceInfo = parent.getResourceInfo(false, false);
		int flags = parent.getFlags(resourceInfo);
		boolean unknown = ResourceInfo.isSet(flags, ICoreConstants.M_CHILDREN_UNKNOWN);

		// get the list of resources in the workspace
		if (!unknown && (parentType == IResource.FOLDER || parentType == IResource.PROJECT) && parent.exists(flags, true)) {
			IResource target = null;
			UnifiedTreeNode child = null;
			IResource[] members;
			try {
				members = ((IContainer) parent).members(IContainer.INCLUDE_TEAM_PRIVATE_MEMBERS | IContainer.INCLUDE_HIDDEN);
			} catch (CoreException e) {
				members = NO_RESOURCES;
			}
			int workspaceIndex = 0;
			//iterate simultaneously over file system and workspace members
			while (workspaceIndex < members.length) {
				target = members[workspaceIndex];
				String name = target.getName();
				IFileInfo localInfo = localIndex < list.length ? list[localIndex] : null;
				int comp = localInfo != null ? name.compareTo(localInfo.getName()) : -1;
				//special handling for linked resources
				if (target.isLinked()) {
					//child will be null if location is undefined
					child = createChildForLinkedResource(target);
					workspaceIndex++;
					//if there is a matching local file, skip it - it will be blocked by the linked resource
					if (comp == 0)
						localIndex++;
				} else if (comp == 0) {
					// resource exists in workspace and file system --> localInfo is non-null
					//create workspace-only node for symbolic link that creates a cycle
					if (localInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK) && localInfo.isDirectory() && isRecursiveLink(node.getStore(), localInfo))
						child = createNode(target, null, null, true);
					else
						child = createNode(target, null, localInfo, true);
					localIndex++;
					workspaceIndex++;
				} else if (comp > 0) {
					// resource exists only in file system
					//don't create a node for symbolic links that create a cycle
					if (localInfo.getAttribute(EFS.ATTRIBUTE_SYMLINK) && localInfo.isDirectory() && isRecursiveLink(node.getStore(), localInfo))
						child = null;
					else
						child = createChildNodeFromFileSystem(node, localInfo);
					localIndex++;
				} else {
					// resource exists only in the workspace
					child = createNode(target, null, null, true);
					workspaceIndex++;
				}
				if (child != null)
					addChildToTree(node, child);
			}
		}

		/* process any remaining resource from the file system */
		addChildrenFromFileSystem(node, list, localIndex);

		/* Mark the children as now known */
		if (unknown) {
			// Don't open the info - we might not be inside a workspace-modifying operation
			resourceInfo = parent.getResourceInfo(false, false);
			if (resourceInfo != null)
				resourceInfo.clear(ICoreConstants.M_CHILDREN_UNKNOWN);
		}

		/* if we added children, add the childMarker separator */
		if (node.getFirstChild() != null)
			addChildrenMarker();
	}

	protected void addChildrenFromFileSystem(UnifiedTreeNode node, IFileInfo[] childInfos, int index) {
		if (childInfos == null)
			return;
		for (int i = index; i < childInfos.length; i++) {
			IFileInfo info = childInfos[i];
			//don't create a node for symbolic links that create a cycle
			if (!info.getAttribute(EFS.ATTRIBUTE_SYMLINK) || !info.isDirectory() || !isRecursiveLink(node.getStore(), info))
				addChildToTree(node, createChildNodeFromFileSystem(node, info));
		}
	}

	protected void addChildrenMarker() {
		addElementToQueue(childrenMarker);
	}

	protected void addChildToTree(UnifiedTreeNode node, UnifiedTreeNode child) {
		if (node.getFirstChild() == null)
			node.setFirstChild(child);
		addElementToQueue(child);
	}

	protected void addElementToQueue(UnifiedTreeNode target) {
		queue.add(target);
	}

	protected void addNodeChildrenToQueue(UnifiedTreeNode node) {
		/* if the first child is not null we already added the children */
		/* If the children won't be at a valid level for the refresh depth, don't bother adding them */
		if (!childLevelValid || node.getFirstChild() != null)
			return;
		addChildren(node);
		if (queue.isEmpty())
			return;
		//if we're about to change levels, then the children just added
		//are the last nodes for their level, so add a level marker to the queue
		UnifiedTreeNode nextNode = queue.peek();
		if (isChildrenMarker(nextNode))
			queue.remove();
		nextNode = queue.peek();
		if (isLevelMarker(nextNode))
			addElementToQueue(levelMarker);
	}

	protected void addRootToQueue() {
		//don't refresh in closed projects
		if (!root.getProject().isAccessible())
			return;
		IFileStore store = ((Resource) root).getStore();
		IFileInfo fileInfo = fileTree != null ? fileTree.getFileInfo(store) : store.fetchInfo();
		UnifiedTreeNode node = createNode(root, store, fileInfo, root.exists());
		if (node.existsInFileSystem() || node.existsInWorkspace())
			addElementToQueue(node);
	}

	/**
	 * Creates a tree node for a resource that is linked in a different file system location.
	 */
	protected UnifiedTreeNode createChildForLinkedResource(IResource target) {
		IFileStore store = ((Resource) target).getStore();
		return createNode(target, store, store.fetchInfo(), true);
	}

	/**
	 * Creates a child node for a location in the file system. Does nothing and returns null if the location does not correspond to a valid file/folder.
	 */
	protected UnifiedTreeNode createChildNodeFromFileSystem(UnifiedTreeNode parent, IFileInfo info) {
		IPath childPath = parent.getResource().getFullPath().append(info.getName());
		int type = info.isDirectory() ? IResource.FOLDER : IResource.FILE;
		IResource target = getWorkspace().newResource(childPath, type);
		return createNode(target, null, info, target.exists());
	}

	/**
	 * Factory method for creating a node for this tree.  If the file exists on
	 * disk, either the parent store or child store can be provided. Providing
	 * only the parent store avoids creation of the child store in cases where
	 * it is not needed. The store object is only needed for directories for
	 * simple file system traversals, so this avoids creating store objects
	 * for all files.
	 */
	protected UnifiedTreeNode createNode(IResource resource, IFileStore store, IFileInfo info, boolean existsWorkspace) {
		//first check for reusable objects
		UnifiedTreeNode node = null;
		int size = freeNodes.size();
		if (size > 0) {
			node = freeNodes.remove(size - 1);
			node.reuse(this, resource, store, info, existsWorkspace);
			return node;
		}
		//none available, so create a new one
		return new UnifiedTreeNode(this, resource, store, info, existsWorkspace);
	}

	protected Iterator<UnifiedTreeNode> getChildren(UnifiedTreeNode node) {
		/* if first child is null we need to add node's children to queue */
		if (node.getFirstChild() == null)
			addNodeChildrenToQueue(node);

		/* if the first child is still null, the node does not have any children */
		if (node.getFirstChild() == null)
			return EMPTY_ITERATOR;

		/* get the index of the first child */
		int index = queue.indexOf(node.getFirstChild());

		/* if we do not have children, just return an empty enumeration */
		if (index == -1)
			return EMPTY_ITERATOR;

		/* create an enumeration with node's children */
		List<UnifiedTreeNode> result = new ArrayList<>(10);
		while (true) {
			UnifiedTreeNode child = queue.get(index);
			if (isChildrenMarker(child))
				break;
			result.add(child);
			if (index == queue.size() - 1) {
				index = 0;
			} else {
				index++;
			}
		}
		return result.iterator();
	}

	protected int getLevel() {
		return level;
	}

	protected IFileInfo[] getLocalList(UnifiedTreeNode node) {
		try {
			final IFileStore store = node.getStore();
			IFileInfo[] list;
			if (fileTree != null && (fileTree.getTreeRoot().equals(store) || fileTree.getTreeRoot().isParentOf(store)))
				list = fileTree.getChildInfos(store);
			else
				list = store.childInfos(EFS.NONE, null);

			if (list == null || list.length == 0)
				return NO_CHILDREN;
			list = ((Resource) node.getResource()).filterChildren(list, false);
			int size = list.length;
			if (size > 1)
				quickSort(list, 0, size - 1);
			return list;
		} catch (CoreException e) {
			//treat failure to access the directory as a non-existent directory
			return NO_CHILDREN;
		}
	}

	protected Workspace getWorkspace() {
		return (Workspace) root.getWorkspace();
	}

	protected void initializeQueue() {
		//initialize the queue
		if (queue == null)
			queue = new LinkedList<>();
		else
			queue.clear();
		//initialize the free nodes list
		if (freeNodes == null)
			freeNodes = new ArrayList<>(100);
		else
			freeNodes.clear();
		addRootToQueue();
		addElementToQueue(levelMarker);
	}

	protected boolean isChildrenMarker(UnifiedTreeNode node) {
		return node == childrenMarker;
	}

	protected boolean isLevelMarker(UnifiedTreeNode node) {
		return node == levelMarker;
	}

	private static class PatternHolder {
		//Initialize-on-demand Holder class to avoid compiling Pattern if never needed
		//Pattern: A UNIX or Windows relative path that just points backward
		private static final String REGEX = Platform.getOS().equals(Platform.OS_WIN32) ? "\\.[.\\\\]*" : "\\.[./]*"; //$NON-NLS-1$ //$NON-NLS-2$
		public static final Pattern TRIVIAL_SYMLINK_PATTERN = Pattern.compile(REGEX);

		private static final String REGEX_BACK_REPEATING = Platform.getOS().equals(Platform.OS_WIN32) ? "(\\.\\.\\\\)+.*" : "(\\.\\./)+.*"; //$NON-NLS-1$ //$NON-NLS-2$
		public static final Pattern REPEATING_BACKWARDS_PATTERN = Pattern.compile(REGEX_BACK_REPEATING);
	}

	/**
	 * Initialize history stores for symbolic links.
	 * This may be done when starting a visitor, or later on demand.
	 */
	protected void initLinkHistoriesIfNeeded() {
		if (pathPrefixHistory == null) {
			//Bug 232426: Check what life cycle we need for the histories
			Job job = Job.getJobManager().currentJob();
			if (job instanceof RefreshJob) {
				//we are running from the RefreshJob: use the path history of the job
				RefreshJob refreshJob = (RefreshJob) job;
				pathPrefixHistory = refreshJob.getPathPrefixHistory();
				rootPathHistory = refreshJob.getRootPathHistory();
			} else {
				//Local Histories
				pathPrefixHistory = new PrefixPool(20);
				rootPathHistory = new PrefixPool(20);
			}
		}
		if (rootPathHistory.size() == 0) {
			//add current root to history
			IFileStore rootStore = ((Resource) root).getStore();
			try {
				java.io.File rootFile = rootStore.toLocalFile(EFS.NONE, null);
				if (rootFile != null) {
					IPath rootProjPath = root.getProject().getLocation();
					if (rootProjPath != null) {
						try {
							java.io.File rootProjFile = new java.io.File(rootProjPath.toOSString());
							rootPathHistory.insertShorter(rootProjFile.getCanonicalPath() + '/');
						} catch (IOException ioe) {
							/*ignore here*/
						}
					}
					rootPathHistory.insertShorter(rootFile.getCanonicalPath() + '/');
				}
			} catch (CoreException | IOException e) {
				/*ignore*/
			}
		}
	}

	/**
	 * Check if the given child represents a recursive symbolic link.
	 * <p>
	 * On remote EFS stores, this check is not exhaustive and just
	 * finds trivial recursive symbolic links pointing up in the tree.
	 * </p><p>
	 * On local stores, where {@link java.io.File#getCanonicalPath()}
	 * is available, the test is exhaustive but may also find some
	 * false positives with transitive symbolic links. This may lead
	 * to suppressing duplicates of already known resources in the
	 * tree, but it will never lead to not finding a resource at
	 * all. See bug 105554 for details.
	 * </p>
	 * @param parentStore EFS IFileStore representing the parent folder
	 * @param localInfo child representing a symbolic link
	 * @return <code>true</code> if the given child represents a
	 *     recursive symbolic link.
	 */
	private boolean isRecursiveLink(IFileStore parentStore, IFileInfo localInfo) {
		//Try trivial pattern first - works also on remote EFS stores
		String linkTarget = localInfo.getStringAttribute(EFS.ATTRIBUTE_LINK_TARGET);
		if (linkTarget != null && PatternHolder.TRIVIAL_SYMLINK_PATTERN.matcher(linkTarget).matches()) {
			return true;
		}
		//Need canonical paths to check all other possibilities
		try {
			java.io.File parentFile = parentStore.toLocalFile(EFS.NONE, null);
			//If this store cannot be represented as a local file, there is nothing we can do
			//In the future, we could try to resolve the link target
			//against the remote file system to do more checks.
			if (parentFile == null) {
				return false;
			}

			Path parent = parentFile.toPath();
			Path realParentPath = parent.toRealPath();
			if (disable_advanced_recursive_link_checks) {
				// Multiple ../ backwards links can go outside the project tree
				if (linkTarget != null && PatternHolder.REPEATING_BACKWARDS_PATTERN.matcher(linkTarget).matches()) {
					Path targetPath = parent.resolve(linkTarget).normalize();

					// Recursive if literal target points to the literal parent of this tree
					if (parent.normalize().startsWith(targetPath)) {
						return true;
					}

					// Recursive if resolved target points to the resolved parent of this tree
					Path realTargetPath = targetPath.toRealPath();
					if (realParentPath.startsWith(realTargetPath)) {
						return true;
					}

					// If link is outside the project tree, consider as non recursive
					// The link still can create recursion in the tree, but we can't detect it here.
				}
				// Skip advanced link checking, it may hide valid directories, see bug 537449
				return false;
			}
			//get canonical path for both child and parent
			java.io.File childFile = new java.io.File(parentFile, localInfo.getName());
			String parentPath = realParentPath.toString() + java.io.File.separatorChar;
			String childPath = childFile.toPath().toRealPath().toString() + java.io.File.separatorChar;
			//get or instantiate the prefix and root path histories.
			//Might be done earlier - for now, do it on demand.
			initLinkHistoriesIfNeeded();
			//insert the parent for checking loops
			pathPrefixHistory.insertLonger(parentPath);
			if (pathPrefixHistory.containsAsPrefix(childPath)) {
				//found a potential loop: is it spanning up a new tree?
				if (!rootPathHistory.insertShorter(childPath)) {
					//not spanning up a new tree, so it is a real loop.
					return true;
				}
			} else if (rootPathHistory.hasPrefixOf(childPath)) {
				//child points into a different portion of the tree that we visited already before, or will certainly visit.
				//This does not introduce a loop yet, but introduces duplicate resources.
				//TODO Ideally, such duplicates should be modelled as linked resources. See bug 105534
				return false;
			} else {
				//child neither introduces a loop nor points to a known tree.
				//It probably spans up a new tree of potential prefixes.
				rootPathHistory.insertShorter(childPath);
			}
		} catch (IOException | CoreException e) {
			//ignore
		}
		return false;
	}

	protected boolean isValidLevel(int currentLevel, int depth) {
		switch (depth) {
			case IResource.DEPTH_INFINITE :
				return true;
			case IResource.DEPTH_ONE :
				return currentLevel <= 1;
			case IResource.DEPTH_ZERO :
				return currentLevel == 0;
			default :
				return currentLevel + 1000 <= depth;
		}
	}

	/**
	 * Sorts the given array of strings in place.  This is
	 * not using the sorting framework to avoid casting overhead.
	 */
	protected void quickSort(IFileInfo[] infos, int left, int right) {
		int originalLeft = left;
		int originalRight = right;
		IFileInfo mid = infos[(left + right) / 2];
		do {
			while (mid.compareTo(infos[left]) > 0)
				left++;
			while (infos[right].compareTo(mid) > 0)
				right--;
			if (left <= right) {
				IFileInfo tmp = infos[left];
				infos[left] = infos[right];
				infos[right] = tmp;
				left++;
				right--;
			}
		} while (left <= right);
		if (originalLeft < right)
			quickSort(infos, originalLeft, right);
		if (left < originalRight)
			quickSort(infos, left, originalRight);
		return;
	}

	/**
	 * Remove from the last element of the queue to the first child of the
	 * given node.
	 */
	protected void removeNodeChildrenFromQueue(UnifiedTreeNode node) {
		UnifiedTreeNode first = node.getFirstChild();
		if (first == null)
			return;
		while (true) {
			if (first.equals(queue.pollLast()))
				break;
		}
		node.setFirstChild(null);
	}

	/**
	 * Increases the current tree level by one. Returns true if the new
	 * level is still valid for the given depth
	 */
	protected boolean setLevel(int newLevel, int depth) {
		level = newLevel;
		childLevelValid = isValidLevel(level + 1, depth);
		return isValidLevel(level, depth);
	}

	private void setRoot(IResource root) {
		this.root = root;
	}
}
