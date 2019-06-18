/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.team.internal.ccvs.ui.repo;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.*;

import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.ccvs.core.*;
import org.eclipse.team.internal.ccvs.core.client.Command;
import org.eclipse.team.internal.ccvs.core.resources.CVSWorkspaceRoot;
import org.eclipse.team.internal.ccvs.core.syncinfo.FolderSyncInfo;
import org.eclipse.team.internal.ccvs.ui.*;
import org.eclipse.team.internal.ccvs.ui.Policy;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation;
import org.eclipse.team.internal.ccvs.ui.operations.RemoteLogOperation.LogEntryCache;

public class RepositoryRoot extends PlatformObject {

	public static final String[] DEFAULT_AUTO_REFRESH_FILES = { ".project" }; //$NON-NLS-1$ 
	private static final String DEFINED_MODULE_PREFIX = "module:"; //$NON-NLS-1$
	
	ICVSRepositoryLocation root;
	String name;
	TagCacheEntry rootTagCacheEntry = new TagCacheEntry(Path.ROOT, null);
	// Map of String (remote folder path) -> Set (file paths that are project relative)
	Map<String, Set<String>> autoRefreshFiles = new HashMap<>();
	// Map of String (module name) -> ICVSRemoteFolder (that is a defined module)
	Map modulesCache;
	Object modulesCacheLock = new Object();
	// List of date tags
	List<CVSTag> dateTags = new ArrayList<>();
	
	public static class TagCacheEntry {
		IPath path;
		// Set of tags found for this path
		private Set<CVSTag> tags = new HashSet<>();
		// String(last path segment) -> TagCacheEntry for child paths
		Map<String, TagCacheEntry> children = new HashMap<>();
		TagCacheEntry parent;
		long lastAccessTime;
		private static final int CACHE_LIFESPAN_IN_DAYS = 7;

		public TagCacheEntry(IPath path, TagCacheEntry parent) {
			this.path = path;
			this.parent = parent;
			accessed();
		}

		public Set<CVSTag> getTags() {
			accessed();
			return tags;
		}

		public Iterator getChildrenIterator() {
			return this.children.values().iterator();
		}

		public boolean isEmpty() {
			return getTags().isEmpty() && children.isEmpty();
		}

		public boolean isExpired() {
			long currentTime = System.currentTimeMillis();
			long ms = currentTime - lastAccessTime;
			int seconds = (int)ms / 1000;
			int hours = seconds / 60 / 60;
			int days = hours / 24;
			return days > CACHE_LIFESPAN_IN_DAYS;
		}

		private void accessed() {
			lastAccessTime = System.currentTimeMillis();
		}
	}
	
	public RepositoryRoot(ICVSRepositoryLocation root) {
		this.root = root;
	}
	
	/**
	 * Returns the name.
	 * @return String
	 */
	public String getName() {
		return name;
	}

	/**
	 * Method getRemoteFolder.
	 * @param path
	 * @param tag
	 * @return ICVSRemoteFolder
	 */
	public ICVSRemoteFolder getRemoteFolder(String path, CVSTag tag, IProgressMonitor monitor) {
		if (isDefinedModuleName(path)) {
			return getDefinedModule(getDefinedModuleName(path), tag, monitor);
		} else {
			return root.getRemoteFolder(path, tag);
		}
	}

	static boolean isDefinedModuleName(String path) {
		return path.startsWith(DEFINED_MODULE_PREFIX);
	}

	static String getDefinedModuleName(String path) {
		return path.substring(DEFINED_MODULE_PREFIX.length());
	}
	
	static String asDefinedModulePath(String path) {
		return DEFINED_MODULE_PREFIX + path;
	}
	
	/**
	 * Method getDefinedModule.
	 * @param path
	 * @param tag
	 * @param monitor
	 * @return ICVSRemoteFolder
	 */
	private ICVSRemoteFolder getDefinedModule(String path, CVSTag tag, IProgressMonitor monitor) {
		Map cache = getDefinedModulesCache(tag, monitor);
		ICVSRemoteFolder folder = (ICVSRemoteFolder)cache.get(path);
		if (folder != null) {
			folder = (ICVSRemoteFolder)folder.forTag(tag);
		}
		return folder;
	}
	
	private Map getDefinedModulesCache(CVSTag tag, IProgressMonitor monitor) {
		if (modulesCache == null) {
			try {
				// Fetch the modules before locking the cache (to avoid deadlock)
				ICVSRemoteResource[] folders = root.members(CVSTag.DEFAULT, true, monitor);
				synchronized(modulesCacheLock) {
					modulesCache = new HashMap();
					for (ICVSRemoteResource resource : folders) {
						modulesCache.put(resource.getName(), resource);
					}
				}
			} catch (CVSException e) {
				// we could't fetch the modules. Log the problem and continue
				CVSUIPlugin.log(e);
				// Return an empty map but don't save it so the fetching of 
				// the modules will occur again
				return new HashMap();
			}
		}
		return modulesCache;
	}
	
	public ICVSRemoteResource[] getDefinedModules(CVSTag tag, IProgressMonitor monitor) {
		Map cache = getDefinedModulesCache(tag, monitor);
		return (ICVSRemoteResource[]) cache.values().toArray(new ICVSRemoteResource[cache.size()]);
	}
	
	public static String getRemotePathFor(ICVSResource resource) throws CVSException {
		if (resource.isFolder()) {
			if (resource instanceof ICVSRemoteFolder) {
				ICVSRemoteFolder remoteFolder = (ICVSRemoteFolder) resource;
				if (remoteFolder.isDefinedModule()) {
					return asDefinedModulePath(remoteFolder.getName());
				}
			}
			FolderSyncInfo info = ((ICVSFolder)resource).getFolderSyncInfo();
			if (info == null)
				throw new CVSException(NLS.bind(CVSUIMessages.RepositoryRoot_folderInfoMissing, new String[] { resource.getName() })); 
			return info.getRepository();
		} else {
			FolderSyncInfo info = resource.getParent().getFolderSyncInfo();
			if (info == null)
				throw new CVSException(NLS.bind(CVSUIMessages.RepositoryRoot_folderInfoMissing, new String[] { resource.getParent().getName() })); 
			String path = new Path(null, info.getRepository()).append(resource.getName()).toString();
			return path;
		}
	}
	
	/**
	 * Returns the root.
	 * @return ICVSRepositoryLocation
	 */
	public ICVSRepositoryLocation getRoot() {
		return root;
	}

	/**
	 * Sets the name.
	 * @param name The name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * Accept the tags for any remote path that represents a folder. However, for the time being,
	 * the given version tags are added to the list of known tags for the 
	 * remote ancestor of the resource that is a direct child of the remote root.
	 * 
	 * It is the responsibility of the caller to ensure that the given remote path is valid.
	 */
	public void addTags(String remotePath, CVSTag[] tags) {
		if (tags.length == 0) {
			return;
		}
		addDateTags(tags);
		addVersionAndBranchTags(remotePath, tags);
	}

	private void addDateTags(CVSTag[] tags){
		for (CVSTag tag : tags) {
			if (tag.getType() == CVSTag.DATE) {
				dateTags.add(tag);
			}
		}
	}

	private void addVersionAndBranchTags(String remotePath, CVSTag[] tags) {
		TagCacheEntry entry = getTagCacheEntryFor(remotePath, true);

		// Store the tag with the appropriate ancestor
		for (CVSTag tag : tags) {
			if (tag.getType() != CVSTag.DATE) {
				Set parentTags = new HashSet();
				addAllKnownTagsForParents(entry, parentTags);
				if (!parentTags.contains(tag)) {
					entry.getTags().add(tag);
					removeTagFromChildrenCacheEntries(entry, tag);
				}
			}
		}
	}

	/**
	 * Add the given date tag to the list of date tags associated with the repository.
	 * @param tag a date tag
	 */
	public void addDateTag(CVSTag tag) {
		if (!dateTags.contains(tag)) {
			dateTags.add(tag);
		}
	}
	public void removeDateTag(CVSTag tag) {
		if (dateTags.contains(tag)) {
			dateTags.remove(tag);
		}
	}
	/**
	 * Return the list of date tags associated with the repository.
	 * @return the list of date tags
	 */
	public CVSTag[] getDateTags() {
		return dateTags.toArray(new CVSTag[dateTags.size()]);
	}
	
	/**
	 * Remove the given tags from the receiver
	 * @param remotePath
	 * @param tags
	 */
	public void removeTags(String remotePath, CVSTag[] tags) {
		removeDateTags(tags);
		removeVersionAndBranchTags(remotePath, tags);
	}
	
	private void removeDateTags(CVSTag[] tags) {		
		if(dateTags.isEmpty())return;
		// Store the tag with the appropriate ancestor
		for (CVSTag tag : tags) {
			dateTags.remove(tag);
		}
	}

	private void removeVersionAndBranchTags(String remotePath, CVSTag[] tags) {
		TagCacheEntry entry = getTagCacheEntryFor(remotePath, false);
		// remove tags from this path and its children
		if (entry != null) {
			removeTagsFromChildrenCacheEntries(entry, tags);
		}

		// remove tags from all parents of this path
		entry = getKnownParentTagCacheEntryFor(remotePath);
		for (CVSTag tag : tags) {
			if (entry.getTags().contains(tag)) {
				entry.getTags().remove(tag);
				continue;
			}
			TagCacheEntry currentEntry = entry;
			while (currentEntry.parent != null) {
				if (currentEntry.parent.getTags().contains(tag)) {
					// remove tag from parent and add it to siblings
					currentEntry.parent.getTags().remove(tag);
					Iterator siblingIterator = currentEntry.parent
							.getChildrenIterator();
					while (siblingIterator.hasNext()) {
						TagCacheEntry sibling = (TagCacheEntry) siblingIterator
								.next();
						if (!sibling.equals(currentEntry)) {
							sibling.getTags().add(tag);
						}
					}
					break;
				} else {
					currentEntry = currentEntry.parent;
				}
			}
		}
		if (entry.isEmpty()) {
			removeTagCacheEntry(entry);
		}
	}

	/**
	 * Returns the absolute paths of the auto refresh files relative to the
	 * repository.
	 * 
	 * @return String[]
	 */
	public String[] getAutoRefreshFiles(String remotePath) {
		Set<String> files = autoRefreshFiles.get(remotePath);
		if (files == null || files.isEmpty()) {
			// convert the default relative file paths to full paths
			if (isDefinedModuleName(remotePath)) {
				return new String[0];
			}
			List<String> result = new ArrayList<>();
			for (String relativePath : DEFAULT_AUTO_REFRESH_FILES) {
				result.add(new Path(null, remotePath).append(relativePath).toString());
			}
			return result.toArray(new String[result.size()]);
		} else {
			return files.toArray(new String[files.size()]);
		}
	}
	
	/**
	 * Sets the auto refresh files for the given remote path to the given
	 * string values which are absolute file paths (relative to the receiver).
	 * 
	 * @param autoRefreshFiles The autoRefreshFiles to set
	 */
	public void setAutoRefreshFiles(String remotePath, String[] autoRefreshFiles) {
		Set<String> newFiles = new HashSet<>(Arrays.asList(autoRefreshFiles));
		// Check to see if the auto-refresh files are the default files
		if (autoRefreshFiles.length == DEFAULT_AUTO_REFRESH_FILES.length) {
			boolean isDefault = true;
			for (String filePath : DEFAULT_AUTO_REFRESH_FILES) {
				if (!newFiles.contains(new Path(null, remotePath).append(filePath).toString())) {
					isDefault = false;
					break;
				}
			}
			if (isDefault) {
				this.autoRefreshFiles.remove(remotePath);
				return;
			}
		}
		this.autoRefreshFiles.put(remotePath, newFiles);
	}

	/**
	 * Fetches tags from auto-refresh files.
	 */
	public CVSTag[] refreshDefinedTags(ICVSFolder folder, boolean recurse,
			IProgressMonitor monitor) throws TeamException {
		monitor = Policy.monitorFor(monitor);
		monitor.beginTask(null, recurse ? 210 : 100);
		try {
			CVSTag[] tags = null;
			if (!folder.getFolderSyncInfo().isVirtualDirectory()) {
				// Only try the auto-refresh file(s) if we are not recursing
				// into sub-folders
				tags = fetchTagsUsingAutoRefreshFiles(folder,
						Policy.subMonitorFor(monitor, 50));
			}
			if (tags == null || tags.length == 0) {
				// There we're no tags found on the auto-refresh files or we
				// we're asked to go deep
				// Try using the log command
				tags = fetchTagsUsingLog(folder,
						Policy.subMonitorFor(monitor, 50));
			}
			if (tags != null && tags.length > 0) {
				String remotePath = getRemotePathFor(folder);
				addTags(remotePath, tags);
				return tags;
			}
			if (recurse) {
				Set<CVSTag> tagsSet = new HashSet<>();
				folder.fetchChildren(Policy.subMonitorFor(monitor, 10));
				ICVSResource[] children = folder
						.members(ICVSFolder.FOLDER_MEMBERS);
				for (ICVSResource child : children) {
					tagsSet.addAll(Arrays.asList(refreshDefinedTags((ICVSFolder) child, recurse, Policy
							.subMonitorFor(monitor,
									100 / children.length))));
				}
				tags = tagsSet.toArray(new CVSTag[tagsSet.size()]);
			}
			return tags;
		} finally {
			monitor.done();
		}
	}

	private CVSTag[] fetchTagsUsingLog(ICVSFolder folder, IProgressMonitor monitor) throws CVSException {
		LogEntryCache logEntries = new LogEntryCache();
		RemoteLogOperation operation = new RemoteLogOperation(null, new ICVSRemoteResource[] { asRemoteResource(folder) }, null, null, logEntries) {
			@Override
			protected Command.LocalOption[] getLocalOptions(CVSTag tag1,CVSTag tag2) {
				Command.LocalOption[] options = new Command.LocalOption[] {};
				Command.LocalOption[] newOptions = new Command.LocalOption[options.length + 1];
				System.arraycopy(options, 0, newOptions, 0, options.length);
				newOptions[options.length] = Command.DO_NOT_RECURSE;
				return newOptions;
			}
		};
		try {
			operation.run(monitor);
		} catch (InvocationTargetException e) {
			throw CVSException.wrapException(e);
		} catch (InterruptedException e) {
			// Ignore;
		}
		String[] keys = logEntries.getCachedFilePaths();
		Set<CVSTag> tags = new HashSet<>();
		for (String key : keys) {
			ILogEntry[] entries = logEntries.getLogEntries(key);
			for (ILogEntry entry : entries) {
				tags.addAll(Arrays.asList(entry.getTags()));
			}
		}
		return tags.toArray(new CVSTag[tags.size()]);
	}

	private ICVSRemoteResource asRemoteResource(ICVSFolder folder) throws CVSException {
		if (folder instanceof ICVSRemoteResource) {
			return (ICVSRemoteResource)folder;
		}
		return CVSWorkspaceRoot.getRemoteResourceFor(folder);
	}

	/**
	 * Fetches tags from auto-refresh files.
	 */
	private CVSTag[] fetchTagsUsingAutoRefreshFiles(ICVSFolder folder, IProgressMonitor monitor) throws TeamException {
		String remotePath = getRemotePathFor(folder);
		String[] filesToRefresh = getAutoRefreshFiles(remotePath);
		try {
			monitor.beginTask(null, filesToRefresh.length * 10); 
			List<CVSTag> tags = new ArrayList<>();
			for (String f : filesToRefresh) {
				ICVSRemoteFile file = root.getRemoteFile(f, CVSTag.DEFAULT);
				try {
					tags.addAll(Arrays.asList(fetchTags(file, Policy.subMonitorFor(monitor, 5))));
				} catch (TeamException e) {
					IStatus status = e.getStatus();
					boolean doesNotExist = false;
					if (status.getCode() == CVSStatus.SERVER_ERROR && status.isMultiStatus()) {
						IStatus[] children = status.getChildren();
						if (children.length == 1 && children[0].getCode() == CVSStatus.DOES_NOT_EXIST) {
							// Don't throw an exception if the file does no exist
							doesNotExist = true;
						}
					}
					if (!doesNotExist) {
						throw e;
					}
				}
			}
			return tags.toArray(new CVSTag[tags.size()]);
		} finally {
			monitor.done();
		}
	}
	
	/**
	 * Returns Branch and Version tags for the given files
	 */	
	private CVSTag[] fetchTags(ICVSRemoteFile file, IProgressMonitor monitor) throws TeamException {
		Set<CVSTag> tagSet = new HashSet<>();
		ILogEntry[] entries = file.getLogEntries(monitor);
		for (ILogEntry entry : entries) {
			CVSTag[] tags = entry.getTags();
			Collections.addAll(tagSet, tags);
		}
		return tagSet.toArray(new CVSTag[0]);
	}

	private TagCacheEntry getTagCacheEntryFor(String remotePath, boolean create) {
		String[] segments = new Path(null, remotePath).segments();
		TagCacheEntry currentTagCacheEntry = rootTagCacheEntry;
		for (int i = 0; i < segments.length; i++) {
			String segment = segments[i];
			if (currentTagCacheEntry.children.containsKey(segment)) {
				currentTagCacheEntry = currentTagCacheEntry.children
						.get(segment);
				continue;
			}
			if (!create) {
				return null;
			}
			TagCacheEntry newTagCacheEntry = new TagCacheEntry(new Path(null,
					remotePath).removeLastSegments(segments.length - (i + 1)),
					currentTagCacheEntry);
			currentTagCacheEntry.children.put(segment, newTagCacheEntry);
			currentTagCacheEntry = newTagCacheEntry;
		}
		return currentTagCacheEntry;
	}

	private TagCacheEntry getKnownParentTagCacheEntryFor(String remotePath) {
		String[] segments = new Path(null, remotePath).segments();
		TagCacheEntry currentTagCacheEntry = rootTagCacheEntry;
		for (String segment : segments) {
			if (currentTagCacheEntry.children.containsKey(segment)) {
				currentTagCacheEntry = currentTagCacheEntry.children
						.get(segment);
			} else {
				break; // we reached the last existing parent
			}
		}
		return currentTagCacheEntry;
	}

	private void removeTagsFromChildrenCacheEntries(TagCacheEntry entry,
			CVSTag[] tags) {
		for (CVSTag tag : tags) {
			removeTagFromChildrenCacheEntries(entry, tag);
		}
		entry.getTags().removeAll(Arrays.asList(tags));
		if (entry.isEmpty()) {
			// remove this entry when the last tags where removed
			// keep the entry if there are any children that have tags
			removeTagCacheEntry(entry);
		}
	}

	private void removeTagCacheEntry(TagCacheEntry entry) {
		if (entry.parent == null) {
			// We cannot remove root tag cache entry
			return;
		}
		Map newParentChildren = new HashMap();
		newParentChildren.putAll(entry.parent.children);
		newParentChildren.remove(entry.path.lastSegment());
		entry.parent.children = newParentChildren;
		if (entry.parent.isEmpty()) {
			// remove parents if they no longer carry any value
			removeTagCacheEntry(entry.parent);
		}
	}

	private void removeTagFromChildrenCacheEntries(TagCacheEntry entry,
			CVSTag tag) {
		Iterator childrenIterator = entry.getChildrenIterator();
		while (childrenIterator.hasNext()) {
			TagCacheEntry child = (TagCacheEntry) childrenIterator.next();
			removeTagFromChildrenCacheEntries(child, tag);
			child.getTags().remove(tag);
			if (child.isEmpty()) {
				// remove this entry when the last tag was removed
				// keep the entry if there are any children that have tags
				removeTagCacheEntry(child);
			}
		}
	}
	
	/**
	 * Write out the state of the receiver as XML on the given XMLWriter.
	 * 
	 * @param writer
	 * @throws IOException
	 */
	public void writeState(XMLWriter writer) {

		HashMap<String, String> attributes = new HashMap<>();

		attributes.clear();
		attributes.put(RepositoriesViewContentHandler.ID_ATTRIBUTE, root.getLocation(false));
		if (name != null) {
			attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, name);
		}
		
		writer.startTag(RepositoriesViewContentHandler.REPOSITORY_TAG, attributes, true);

		//put date tag under repository
		if(!dateTags.isEmpty()){
			writer.startTag(RepositoriesViewContentHandler.DATE_TAGS_TAG, attributes, true);
			Iterator iter = dateTags.iterator();
			while(iter.hasNext()){
				CVSTag tag = (CVSTag)iter.next();
				writeATag(writer, attributes, tag, RepositoriesViewContentHandler.DATE_TAG_TAG);
			}
			writer.endTag(RepositoriesViewContentHandler.DATE_TAGS_TAG);
		}
		
		// Gather all the modules that have tags and/or auto-refresh files
		// for each module, write the moduel, tags and auto-refresh files.
		String[] paths = getKnownRemotePaths();
		for (String path : paths) {
			attributes.clear();
			String name = path;
			if (isDefinedModuleName(path)) {
				name = getDefinedModuleName(path);
				
				attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.DEFINED_MODULE_TYPE);
			}
			attributes.put(RepositoriesViewContentHandler.PATH_ATTRIBUTE, name);
			TagCacheEntry entry = getTagCacheEntryFor(path, false);
			boolean writeOutTags = entry != null && !entry.isExpired();
			if (writeOutTags)
				attributes.put(RepositoriesViewContentHandler.LAST_ACCESS_TIME_ATTRIBUTE, Long.toString(entry.lastAccessTime));
			writer.startTag(RepositoriesViewContentHandler.MODULE_TAG, attributes, true);
			if (writeOutTags) {
				Iterator tagIt = entry.getTags().iterator();
				while (tagIt.hasNext()) {
					CVSTag tag = (CVSTag)tagIt.next();
					writeATag(writer, attributes, tag, RepositoriesViewContentHandler.TAG_TAG);
				}
			}
			Set refreshSet = autoRefreshFiles.get(path);
			if (refreshSet != null) {
				Iterator filenameIt = refreshSet.iterator();
				while (filenameIt.hasNext()) {
					String filename = (String)filenameIt.next();
					attributes.clear();
					attributes.put(RepositoriesViewContentHandler.FULL_PATH_ATTRIBUTE, filename);
					writer.startAndEndTag(RepositoriesViewContentHandler.AUTO_REFRESH_FILE_TAG, attributes, true);
				}
			}
			writer.endTag(RepositoriesViewContentHandler.MODULE_TAG);
		}
		writer.endTag(RepositoriesViewContentHandler.REPOSITORY_TAG);
	}

	private void writeATag(XMLWriter writer, HashMap attributes, CVSTag tag, String s) {
		attributes.clear();
		attributes.put(RepositoriesViewContentHandler.NAME_ATTRIBUTE, tag.getName());
		attributes.put(RepositoriesViewContentHandler.TYPE_ATTRIBUTE, RepositoriesViewContentHandler.TAG_TYPES[tag.getType()]);
		writer.startAndEndTag(s, attributes, true);
	}

	private void addAllKnownTagsForParents(TagCacheEntry entry, Set<CVSTag> tags) {
		if (entry.parent != null) {
			addAllKnownTagsForParents(entry.parent, tags);
		}
		tags.addAll(entry.getTags());
	}

	private void addAllKnownTagsForChildren(TagCacheEntry entry, Set<CVSTag> tags) {
		Iterator childrenIterator = entry.getChildrenIterator();
		while (childrenIterator.hasNext()) {
			TagCacheEntry child = (TagCacheEntry) childrenIterator.next();
			addAllKnownTagsForChildren(child, tags);
		}
		tags.addAll(entry.getTags());
	}

	/**
	 * Method getKnownTags.
	 * @param remotePath
	 * @return CVSTag[]
	 */
	public CVSTag[] getAllKnownTags(String remotePath) {
		Set<CVSTag> tags = new HashSet<>(dateTags);
		addAllKnownTagsForParents(getKnownParentTagCacheEntryFor(remotePath),
				tags);
		TagCacheEntry entry = getTagCacheEntryFor(remotePath, false);
		if (entry != null) {
			addAllKnownTagsForChildren(entry, tags);
		}
		return tags.toArray(new CVSTag[tags.size()]);
	}

	public CVSTag[] getAllKnownTags() {
		Set<CVSTag> tags = new HashSet<>(dateTags);
		addAllKnownTagsForChildren(rootTagCacheEntry, tags);
		return tags.toArray(new CVSTag[tags.size()]);
	}

	public String[] getRemoteChildrenForTag(String remotePath, CVSTag tag) {
		TagCacheEntry entry;
		if (remotePath == null) {
			entry = rootTagCacheEntry;
		} else {
			entry = getTagCacheEntryFor(remotePath, false);
		}
		if (entry == null) {
			return new String[0];
		}

		Set<String> paths = new HashSet<>();
		Iterator childrenIterator = entry.getChildrenIterator();
		while (childrenIterator.hasNext()) {
			TagCacheEntry child = (TagCacheEntry) childrenIterator.next();
			Set childTags = new HashSet();
			addAllKnownTagsForChildren(child, childTags);
			if (childTags.contains(tag)) {
				paths.add(child.path.toString());
			}
		}
		return paths.toArray(new String[paths.size()]);
	}

	private Set getKnownRemotePaths(TagCacheEntry entry) {
		Set<String> paths = new HashSet<>();
		Iterator childrenIterator = entry.getChildrenIterator();
		while (childrenIterator.hasNext()) {
			paths.addAll(getKnownRemotePaths((TagCacheEntry) childrenIterator
					.next()));
		}
		paths.add(entry.path.toString());
		return paths;
	}

	public String[] getKnownRemotePaths() {
		Set paths = getKnownRemotePaths(rootTagCacheEntry);
		paths.addAll(autoRefreshFiles.keySet());
		return (String[]) paths.toArray(new String[paths.size()]);
	}
	
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (ICVSRepositoryLocation.class.equals(adapter)) return adapter.cast(getRoot());
		return super.getAdapter(adapter);
	}
	
	/**
	 * Method tagIsKnown.
	 * @param remoteResource
	 * @return boolean
	 */
	public boolean tagIsKnown(ICVSRemoteResource remoteResource) {
		if (remoteResource instanceof ICVSRemoteFolder) {
			ICVSRemoteFolder folder = (ICVSRemoteFolder) remoteResource;
			String path = folder.getRepositoryRelativePath();
			CVSTag[] tags = getAllKnownTags(path);
			CVSTag tag = folder.getTag();
			for (CVSTag knownTag : tags) {
				if (knownTag.equals(tag)) return true;
			}
		}
		return false;
	}
	
	/**
	 * This method is invoked whenever the refresh button in the
	 * RepositoriesView is pressed.
	 */
	void clearCache() {
		synchronized(modulesCacheLock) {
			if (modulesCache != null)
				modulesCache = null;
		}
	}

	/**
	 * Sets the root.
	 * @param root The root to set
	 */
	void setRepositoryLocation(ICVSRepositoryLocation root) {
		this.root = root;
	}

	/*
	 * Set the last access time of the cache entry for the given path
	 * as it was read from the persistent store.
	 */
	/* package */ void setLastAccessedTime(String remotePath, long lastAccessTime) {
		TagCacheEntry entry = getTagCacheEntryFor(remotePath, false);
		if(entry != null){
			entry.lastAccessTime = lastAccessTime;
		}
	}

}
