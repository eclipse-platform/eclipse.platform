/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *     Joerg Kubitz    - caching
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.io.*;
import java.lang.ref.SoftReference;
import java.util.*;
import org.eclipse.core.internal.resources.ResourceException;
import org.eclipse.core.internal.resources.ResourceStatus;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.*;
import org.eclipse.osgi.util.NLS;

/**
 * A bucket is a persistent dictionary having paths as keys. Values are determined
 * by subclasses.
 *
 *  @since 3.1
 */
public abstract class Bucket {

	public static abstract class Entry {
		/**
		 * This entry has not been modified in any way so far.
		 *
		 * @see #state
		 */
		private final static int STATE_CLEAR = 0;
		/**
		 * This entry has been requested for deletion.
		 *
		 * @see #state
		 */
		private final static int STATE_DELETED = 0x02;
		/**
		 * This entry has been modified.
		 *
		 * @see #state
		 */
		private final static int STATE_DIRTY = 0x01;

		/**
		 * Logical path of the object we are storing history for. This does not
		 * correspond to a file system path.
		 */
		private IPath path;

		/**
		 * State for this entry. Possible values are STATE_CLEAR, STATE_DIRTY and STATE_DELETED.
		 *
		 * @see #STATE_CLEAR
		 * @see #STATE_DELETED
		 * @see #STATE_DIRTY
		 */
		private byte state = STATE_CLEAR;

		protected Entry(IPath path) {
			this.path = path;
		}

		public void delete() {
			state = STATE_DELETED;
		}

		public abstract int getOccurrences();

		public IPath getPath() {
			return path;
		}

		public abstract Object getValue();

		public boolean isDeleted() {
			return state == STATE_DELETED;
		}

		public boolean isDirty() {
			return state == STATE_DIRTY;
		}

		public boolean isEmpty() {
			return getOccurrences() == 0;
		}

		public void markDirty() {
			Assert.isTrue(state != STATE_DELETED);
			state = STATE_DIRTY;
		}

		/**
		 * Called on the entry right after the visitor has visited it.
		 */
		public void visited() {
			// does not do anything by default
		}
	}

	/**
	 * A visitor for bucket entries.
	 */
	public static abstract class Visitor {
		// should continue the traversal
		public final static int CONTINUE = 0;
		// should stop looking at any states immediately
		public final static int STOP = 1;
		// should stop looking at states for files in this container (or any of its children)
		public final static int RETURN = 2;

		/**
		 * Called after the bucket has been visited and saved.
		 *
		 * @throws CoreException allows implementation to throw on error
		 */
		public void afterSaving(Bucket bucket) throws CoreException {
			// empty implementation, subclasses to override
		}

		/**
		 * Called after the bucket has been visited but before saved.
		 *
		 * @throws CoreException allows implementation to throw on error. Throwing an
		 *                       exception prevents saving.
		 */
		public void beforeSaving(Bucket bucket) throws CoreException {
			// empty implementation, subclasses to override
		}

		/**
		 * @return either STOP, CONTINUE or RETURN
		 */
		public abstract int visit(Entry entry);
	}

	/**
	 * The segment name for the root directory for index files.
	 */
	static final String INDEXES_DIR_NAME = ".indexes"; //$NON-NLS-1$

	/**
	 * Map of the history entries in this bucket. Maps (String -&gt; byte[][] or String[][]),
	 * where the key is the path of the object we are storing history for, and
	 * the value is the history entry data (UUID,timestamp) pairs.
	 */
	private final Map<String, Object> entries;
	private SoftReference<Map<Object, Map<String, Object>>> entriesCache;

	/**
	 * The file system location of this bucket index file.
	 */
	private File location;
	/**
	 * Whether the in-memory bucket is dirty and needs saving
	 */
	private boolean needSaving = false;
	/**
	 * The project name for the bucket currently loaded. <code>null</code> if this is the root bucket.
	 */
	protected String projectName;

	public Bucket() {
		this(false);
	}

	public Bucket(boolean cacheEntries) {
		this.entries = new HashMap<>();
		if (cacheEntries) {
			entriesCache = new SoftReference<>(null);
		}
	}

	/**
	 * Applies the given visitor to this bucket index and save changes.
	 *
	 * @param visitor the processor for the bucket entries
	 * @param filter  a filter to skip bucket entries
	 * @param depth   the number of trailing segments that can differ from the
	 *                filter
	 * @return one of STOP, RETURN or CONTINUE constants
	 * @exception CoreException thrown by the visitor or from a failed save
	 */
	public final int accept(Visitor visitor, IPath filter, int depth) throws CoreException {
		if (entries.isEmpty())
			return Visitor.CONTINUE;
		try {
			for (Iterator<Map.Entry<String, Object>> i = entries.entrySet().iterator(); i.hasNext();) {
				Map.Entry<String, Object> mapEntry = i.next();
				IPath path = new Path(mapEntry.getKey());
				// check whether the filter applies
				int matchingSegments = filter.matchingFirstSegments(path);
				if (!filter.isPrefixOf(path) || path.segmentCount() - matchingSegments > depth)
					continue;
				// apply visitor
				Entry bucketEntry = createEntry(path, mapEntry.getValue());
				// calls the visitor passing all uuids for the entry
				int outcome = visitor.visit(bucketEntry);
				// notify the entry it has been visited
				bucketEntry.visited();
				if (bucketEntry.isDeleted()) {
					needSaving = true;
					i.remove();
				} else if (bucketEntry.isDirty()) {
					needSaving = true;
					mapEntry.setValue(bucketEntry.getValue());
				}
				if (outcome != Visitor.CONTINUE)
					return outcome;
			}
			return Visitor.CONTINUE;
		} finally {
			visitor.beforeSaving(this);
			save();
			visitor.afterSaving(this);
		}
	}

	/**
	 * Tries to delete as many empty levels as possible.
	 */
	private void cleanUp(File toDelete) {
		if (!toDelete.delete())
			// if deletion didn't go well, don't bother trying to delete the parent dir
			return;
		// don't try to delete beyond the root for bucket indexes
		if (toDelete.getName().equals(INDEXES_DIR_NAME))
			return;
		// recurse to parent directory
		cleanUp(toDelete.getParentFile());
	}

	/**
	 * Factory method for creating entries. Subclasses to override.
	 */
	protected abstract Entry createEntry(IPath path, Object value);

	/**
	 * Flushes this bucket so it has no contents and is not associated to any
	 * location. Any uncommitted changes are lost.
	 */
	public void flush() {
		if (isCachingEnabled()) {
			entriesCache.clear();
		}
		projectName = null;
		location = null;
		entries.clear();
		needSaving = false;
	}

	/**
	 * Returns how many entries there are in this bucket.
	 */
	public final int getEntryCount() {
		return entries.size();
	}

	/**
	 * Returns the value for entry corresponding to the given path (null if none found).
	 */
	public final Object getEntryValue(String path) {
		return entries.get(path);
	}

	/**
	 * Returns the file name used to persist the index for this bucket.
	 */
	protected abstract String getIndexFileName();

	/**
	 * Returns the version number for the file format used to persist this bucket.
	 */
	protected abstract byte getVersion();

	/**
	 * Returns the file name to be used to store bucket version information
	 */
	protected abstract String getVersionFileName();

	/**
	 * Loads the contents from a file under the given directory.
	 */
	public void load(String newProjectName, File baseLocation) throws CoreException {
		load(newProjectName, baseLocation, false);
	}

	/**
	 * Loads the contents from a file under the given directory. If <code>force</code> is
	 * <code>false</code>, if this bucket already contains the contents from the current location,
	 * avoids reloading.
	 */
	public void load(String newProjectName, File baseLocation, boolean force) throws CoreException {
		try {
			// avoid reloading
			if (!force && this.location != null && baseLocation.equals(this.location.getParentFile()) && (projectName == null ? (newProjectName == null) : projectName.equals(newProjectName))) {
				this.projectName = newProjectName;
				return;
			}
			// previously loaded bucket may not have been saved... save before loading new one
			save();
			this.projectName = newProjectName;
			this.location = new File(baseLocation, getIndexFileName());
			Map<String, Object> loadedEntries = null;
			this.entries.clear();
			if (force) {
				loadedEntries = loadEntries(this.location);
			} else {
				if (isCachingEnabled()) {
					Map<Object, Map<String, Object>> cache = entriesCache.get();
					if (cache != null) {
						loadedEntries = cache.get(createBucketKey());
					}
				}
				// errors are not cached, so
				// loadedEntries == null means cached value is not present:
				if (loadedEntries == null) {
					loadedEntries = loadEntries(this.location);
				}
			}
			this.entries.putAll(loadedEntries);
		} catch (IOException ioe) {
			String message = NLS.bind(Messages.resources_readMeta, location.getAbsolutePath());
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, null, message, ioe);
			throw new ResourceException(status);
		}
	}

	boolean isCachingEnabled() {
		return entriesCache != null;
	}

	private Object createBucketKey() {
		return this.location == null ? null : this.location.getAbsolutePath();
	}

	private Map<String, Object> loadEntries(File indexFile) throws CoreException, IOException {
		if (!indexFile.isFile()) {
			return Collections.EMPTY_MAP; // remember file does not exist
		}
		Map<String, Object> resultEntries = new HashMap<>();
		try (DataInputStream source = new DataInputStream(
				new BufferedInputStream(new FileInputStream(indexFile), 8192))) {
			int version = source.readByte();
			if (version != getVersion()) {
				// unknown version
				String message = NLS.bind(Messages.resources_readMetaWrongVersion, location.getAbsolutePath(), Integer.toString(version));
				ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_READ_METADATA, message);
				throw new ResourceException(status);
			}
			int entryCount = source.readInt();
			for (int i = 0; i < entryCount; i++) {
				resultEntries.put(readEntryKey(source), readEntryValue(source));
			}
			return resultEntries;
		}
	}

	private String readEntryKey(DataInputStream source) throws IOException {
		if (projectName == null)
			return source.readUTF();
		return IPath.SEPARATOR + projectName + source.readUTF();
	}

	/**
	 * Defines how data for a given entry is to be read from a bucket file. To be implemented by subclasses.
	 */
	protected abstract Object readEntryValue(DataInputStream source) throws IOException, CoreException;

	/**
	 * Saves this bucket's contents back to its location.
	 */
	public void save() throws CoreException {
		if (isCachingEnabled()) {
			Object key = createBucketKey();
			if (key != null) {
				// we do need to make a copy from this.entries because that instance is reused
				@SuppressWarnings("unchecked")
				java.util.Map.Entry<String, Object>[] a = new java.util.Map.Entry[0];
				java.util.Map<String, Object> denseCopy = java.util.Map.ofEntries(this.entries.entrySet().toArray(a));
				Map<Object, Map<String, Object>> cache = entriesCache.get();
				if (cache == null) {
					cache = new WeakHashMap<>();
					entriesCache = new SoftReference<>(cache);
				}
				cache.put(key, denseCopy); // remember the entries in cache
			}
		}
		if (!needSaving)
			return;
		try {
			if (entries.isEmpty()) {
				needSaving = false;
				cleanUp(location);
				return;
			}
			// ensure the parent location exists
			File parent = location.getParentFile();
			if (parent == null)
				throw new IOException();//caught and rethrown below
			parent.mkdirs();
			try (DataOutputStream destination = new DataOutputStream(new BufferedOutputStream(new FileOutputStream(location), 8192))) {
				destination.write(getVersion());
				destination.writeInt(entries.size());
				for (java.util.Map.Entry<String, Object> entry : entries.entrySet()) {
					writeEntryKey(destination, entry.getKey());
					writeEntryValue(destination, entry.getValue());
				}
			}
			needSaving = false;
		} catch (IOException ioe) {
			String message = NLS.bind(Messages.resources_writeMeta, location.getAbsolutePath());
			ResourceStatus status = new ResourceStatus(IResourceStatus.FAILED_WRITE_METADATA, null, message, ioe);
			throw new ResourceException(status);
		}
	}

	/**
	 * Sets the value for the entry with the given path. If <code>value</code> is <code>null</code>,
	 * removes the entry.
	 */
	public final void setEntryValue(String path, Object value) {
		if (value == null)
			entries.remove(path);
		else
			entries.put(path, value);
		needSaving = true;
	}

	private void writeEntryKey(DataOutputStream destination, String path) throws IOException {
		if (projectName == null) {
			destination.writeUTF(path);
			return;
		}
		// omit the project name
		int pathLength = path.length();
		int projectLength = projectName.length();
		String key = (pathLength == projectLength + 1) ? "" : path.substring(projectLength + 1); //$NON-NLS-1$
		destination.writeUTF(key);
	}

	/**
	 * Defines how an entry is to be persisted to the bucket file.
	 */
	protected abstract void writeEntryValue(DataOutputStream destination, Object entryValue) throws IOException, CoreException;
}
