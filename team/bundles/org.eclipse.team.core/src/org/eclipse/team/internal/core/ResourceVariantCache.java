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
package org.eclipse.team.internal.core;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.ILock;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.core.variants.CachedResourceVariant;

/**
 * This class implements a caching facility that can be used by TeamProviders to cache contents
 */
public class ResourceVariantCache {

	// Directory to cache file contents
	private static final String CACHE_DIRECTORY = ".cache"; //$NON-NLS-1$
	// Maximum lifespan of local cache file, in milliseconds
	private static final long CACHE_FILE_LIFESPAN = 60*60*1000; // 1hr

	// Map of registered caches indexed by local name of a QualifiedName
	private static Map<String, ResourceVariantCache> caches = new HashMap<>(); // String (local name) > RemoteContentsCache

	private String name;
	private Map<String, ResourceVariantCacheEntry> cacheEntries;
	private long lastCacheCleanup;
	private int cacheDirSize;

	// Lock used to serialize the writing of cache contents
	private ILock lock = Job.getJobManager().newLock();

	/**
	 * Enables the use of remote contents caching for the given cacheId. The cache ID must be unique.
	 * A good candidate for this ID is the plugin ID of the plugin performing the caching.
	 *
	 * @param cacheId the unique Id of the cache being enabled
	 */
	public static synchronized void enableCaching(String cacheId) {
		if (isCachingEnabled(cacheId)) return;
		ResourceVariantCache cache = new ResourceVariantCache(cacheId);
		cache.createCacheDirectory();
		caches.put(cacheId, cache);
	}

	/**
	 * Returns whether caching has been enabled for the given Id. A cache should only be enabled once.
	 * It is conceivable that a cache be persisted over workbench invocations thus leading to a cache that
	 * is enabled on startup without intervention by the owning plugin.
	 *
	 * @param cacheId the unique Id of the cache
	 * @return true if caching for the given Id is enabled
	 */
	public static boolean isCachingEnabled(String cacheId) {
		return getCache(cacheId) != null;
	}

	/**
	 * Disable the cache, disposing of any file contents in the cache.
	 *
	 * @param cacheId the unique Id of the cache
	 */
	public static void disableCache(String cacheId) {
		ResourceVariantCache cache = getCache(cacheId);
		if (cache == null) {
			// There is no cache to dispose of
			return;
		}
		caches.remove(cacheId);
		cache.deleteCacheDirectory();
	}

	/**
	 * Return the cache for the given id or null if caching is not enabled for the given id.
	 * @param cacheId
	 * @return the cache
	 */
	public static synchronized ResourceVariantCache getCache(String cacheId) {
		return caches.get(cacheId);
	}

	public static synchronized void shutdown() {
		String[] keys = caches.keySet().toArray(new String[caches.size()]);
		for (String id : keys) {
			disableCache(id);
		}
	}

	private ResourceVariantCache(String name) {
		this.name = name;
	}

	/**
	 * Return whether the cache contains an entry for the given id. Register a hit if it does.
	 * @param id the id of the cache entry
	 * @return true if there are contents cached for the id
	 */
	public boolean hasEntry(String id) {
		return internalGetCacheEntry(id) != null;
	}

	protected IPath getCachePath() {
		return getStateLocation().append(CACHE_DIRECTORY).append(name);
	}

	private IPath getStateLocation() {
		return TeamPlugin.getPlugin().getStateLocation();
	}

	private synchronized void clearOldCacheEntries() {
		long current = new Date().getTime();
		if ((lastCacheCleanup!=-1) && (current - lastCacheCleanup < CACHE_FILE_LIFESPAN)) return;
		List<ResourceVariantCacheEntry> stale = new ArrayList<>();
		for (ResourceVariantCacheEntry entry : cacheEntries.values()) {
			long lastHit = entry.getLastAccessTimeStamp();
			if ((current - lastHit) > CACHE_FILE_LIFESPAN){
				stale.add(entry);
			}
		}
		for (ResourceVariantCacheEntry entry : stale) {
			entry.dispose();
		}
	}

	private synchronized void purgeFromCache(String id) {
		ResourceVariantCacheEntry entry = cacheEntries.get(id);
		File f = entry.getFile();
		try {
			deleteFile(f);
		} catch (TeamException e) {
			// Ignore the deletion failure.
			// A failure only really matters when purging the directory on startup
		}
		cacheEntries.remove(id);
	}

	private synchronized void createCacheDirectory() {
		IPath cacheLocation = getCachePath();
		File file = cacheLocation.toFile();
		if (file.exists()) {
			try {
				deleteFile(file);
			} catch (TeamException e) {
				// Check to see if were in an acceptable state
				File[] fileList = file.listFiles();
				if (file.exists() && (!file.isDirectory() || (fileList != null && fileList.length != 0))) {
					TeamPlugin.log(e);
				}
			}
		}
		if (! file.exists() && ! file.mkdirs()) {
			TeamPlugin.log(new TeamException(NLS.bind(Messages.RemoteContentsCache_fileError, new String[] { file.getAbsolutePath() })));
		}
		cacheEntries = new HashMap<>();
		lastCacheCleanup = -1;
		cacheDirSize = 0;
	}

	private synchronized void deleteCacheDirectory() {
		cacheEntries = null;
		lastCacheCleanup = -1;
		cacheDirSize = 0;
		IPath cacheLocation = getCachePath();
		File file = cacheLocation.toFile();
		if (file.exists()) {
			try {
				deleteFile(file);
			} catch (TeamException e) {
				// Don't worry about problems deleting.
				// The only case that matters is when the cache directory is created
			}
		}
	}

	private void deleteFile(File file) throws TeamException {
		if (file.isDirectory()) {
			File[] children = file.listFiles();
			if(children == null) {
				throw new TeamException(NLS.bind(Messages.RemoteContentsCache_fileError, new String[] { file.getAbsolutePath() }));
			}
			for (File f : children) {
				deleteFile(f);
			}
		}
		if (! file.delete()) {
			throw new TeamException(NLS.bind(Messages.RemoteContentsCache_fileError, new String[] { file.getAbsolutePath() }));
		}
	}

	/**
	 * Purge the given cache entry from the cache. This method should only be invoked from
	 * an instance of ResourceVariantCacheEntry after it has set it's state to DISPOSED.
	 * @param entry
	 */
	protected void purgeFromCache(ResourceVariantCacheEntry entry) {
		purgeFromCache(entry.getId());
	}

	private synchronized ResourceVariantCacheEntry internalGetCacheEntry(String id) {
		if (cacheEntries == null) {
			// This probably means that the cache has been disposed
			throw new IllegalStateException(NLS.bind(Messages.RemoteContentsCache_cacheDisposed, new String[] { name }));
		}
		ResourceVariantCacheEntry entry = cacheEntries.get(id);
		if (entry != null) {
			entry.registerHit();
		}
		return entry;
	}

	/**
	 * @param id the id that uniquely identifies the remote resource that is cached.
	 * @return the cache entry
	 */
	public ResourceVariantCacheEntry getCacheEntry(String id) {
		return internalGetCacheEntry(id);
	}

	public synchronized ResourceVariantCacheEntry add(String id, CachedResourceVariant resource) {
		clearOldCacheEntries();
		String filePath = String.valueOf(cacheDirSize++);
		ResourceVariantCacheEntry entry = new ResourceVariantCacheEntry(this, lock, id, filePath);
		entry.setResourceVariant(resource);
		cacheEntries.put(id, entry);
		return entry;
	}

	public String getName() {
		return name;
	}

	/*
	 * Method used for testing only
	 */
	public ResourceVariantCacheEntry[] getEntries() {
		return cacheEntries.values().toArray(new ResourceVariantCacheEntry[cacheEntries.size()]);
	}

}
