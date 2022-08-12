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
package org.eclipse.team.core.variants;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.osgi.util.NLS;
import org.eclipse.team.core.TeamException;
import org.eclipse.team.internal.core.Messages;
import org.eclipse.team.internal.core.ResourceVariantCache;
import org.eclipse.team.internal.core.ResourceVariantCacheEntry;
import org.eclipse.team.internal.core.TeamPlugin;

/**
 * A resource variant is a partial implementation of a remote resource
 * whose contents and handle are cached locally. It is assumed that a
 * resource variant is an immutable version or revision of a resource.
 * Therefore, once the contents are cached they cannot be replaced.
 * However, the cached handle can be replaced to allow clients to
 * cache addition state or properties for a resource variant.
 * <p>
 * Overriding subclasses need to provide a cache Id for all their resource variants
 * and a cache path for each resource variant that uniquely identifies it. In addition,
 * they must implement <code>fetchContents</code> to retrieve the contents of the
 * resource variant and then call <code>setContents</code> to place these contents in the cache.
 * Subclasses may also call <code>cacheHandle</code> in order to place the handle in the
 * cache so that it can be retrieved later by calling <code>getCachedHandle</code> on any
 * resource variant whose cache path is the same as the cached handle. This allows subclasses to
 * cache additional resource variant properties such as author, comment, etc.
 * </p>
 * <p>
 * The <code>IStorage</code> instance returned by this class will be
 * an {@link org.eclipse.core.resources.IEncodedStorage}.
 * <p>
 * The cache in which the resource variants reside will occasionally clear
 * cached entries if they have not been accessed for a certain amount of time.
 * </p>
 *
 * @since 3.0
 */
public abstract class CachedResourceVariant extends PlatformObject implements IResourceVariant {

	// holds the storage instance for this resource variant
	private IStorage storage;

	/*
	 * Internal class which provides access to the cached contents
	 * of this resource variant
	 */
	class ResourceVariantStorage implements IEncodedStorage {
		@Override
		public InputStream getContents() throws CoreException {
			if (!isContentsCached()) {
				// The cache may have been cleared if someone held
				// on to the storage too long
				throw new TeamException(NLS.bind(Messages.CachedResourceVariant_0, new String[] { getCachePath() }));
			}
			return getCachedContents();
		}
		@Override
		public IPath getFullPath() {
			return getDisplayPath();
		}
		@Override
		public String getName() {
			return CachedResourceVariant.this.getName();
		}
		@Override
		public boolean isReadOnly() {
			return true;
		}
		@Override
		public <T> T getAdapter(Class<T> adapter) {
			return CachedResourceVariant.this.getAdapter(adapter);
		}
		@Override
		public String getCharset() throws CoreException {
			InputStream contents = getContents();
			try {
				String charSet = TeamPlugin.getCharset(getName(), contents);
				return charSet;
			} catch (IOException e) {
				throw new TeamException(new Status(IStatus.ERROR, TeamPlugin.ID, IResourceStatus.FAILED_DESCRIBING_CONTENTS, NLS.bind(Messages.CachedResourceVariant_1, new String[] { getFullPath().toString() }), e));
			} finally {
				try {
					contents.close();
				} catch (IOException e1) {
					// Ignore
				}
			}
		}
	}

	@Override
	public IStorage getStorage(IProgressMonitor monitor) throws TeamException {
		if (isContainer()) return null;
		ensureContentsCached(monitor);
		if (storage == null) {
			storage = new ResourceVariantStorage();
		}
		return storage;
	}

	private void ensureContentsCached(IProgressMonitor monitor) throws TeamException {
		// Ensure that the contents are cached from the server
		if (!isContentsCached()) {
			fetchContents(monitor);
		}
	}

	/**
	 * Method that is invoked when the contents of the resource variant need to
	 * be fetched. This method will only be invoked for files (i.e.
	 * <code>isContainer()</code> returns <code>false</code>.
	 * Subclasses should override this method and invoke <code>setContents</code>
	 * with a stream containing the fetched contents.
	 * @param monitor a progress monitor
	 */
	protected abstract void fetchContents(IProgressMonitor monitor) throws TeamException;

	/**
	 * This method should be invoked by subclasses from within their <code>fetchContents</code>
	 * method in order to cache the contents for this resource variant.
	 * <p>
	 * This method is not intended to be overridden by clients.
	 * @param stream the stream containing the contents of the resource variant
	 * @param monitor a progress monitor
	 * @throws TeamException if an error occurs
	 */
	protected void setContents(InputStream stream, IProgressMonitor monitor) throws TeamException {
		// Ensure that there is a cache entry to receive the contents
		Assert.isTrue(!isContainer());
		if (!isHandleCached()) cacheHandle();
		getCacheEntry().setContents(stream, monitor);
	}

	private ResourceVariantCacheEntry getCacheEntry() {
		return getCache().getCacheEntry(this.getCachePath());
	}

	/**
	 * Return whether there are already contents cached for this resource variant.
	 * This method will return <code>false</code> even if the contents are currently
	 * being cached by another thread. The consequence of this is that the contents
	 * may be fetched twice in the rare case where two threads request the same contents
	 * concurrently. For containers, this method will always return <code>false</code>.
	 * <p>
	 * This method is not intended to be overridden by clients.
	 * @return whether there are contents cached for this resource variant
	 */
	public boolean isContentsCached() {
		if (isContainer() || !isHandleCached()) {
			return false;
		}
		ResourceVariantCacheEntry entry = getCache().getCacheEntry(getCachePath());
		return entry.getState() == ResourceVariantCacheEntry.READY;
	}

	/**
	 * Return the cached contents for this resource variant or <code>null</code>
	 * if the contents have not been cached.
	 * For containers, this method will always return <code>null</code>.
	 * <p>
	 * This method is not intended to be overridden by clients.
	 * @return the cached contents or <code>null</code>
	 * @throws TeamException if an error occurs
	 */
	protected InputStream getCachedContents() throws TeamException {
		if (isContainer() || !isContentsCached()) return null;
		return getCache().getCacheEntry(getCachePath()).getContents();
	}

	/**
	 * Return <code>true</code> if the cache contains an entry for this resource
	 * variant. It is possible that another instance of this variant is cached.
	 * To get the cached instance, call <code>getCachedHandle()</code>. Note that
	 * cached contents can be retrieved from any handle to a resource variant whose
	 * cache path (as returned by <code>getCachePath()</code>) match but other
	 * state information may only be accessible from the cached copy.
	 *
	 * @return whether the variant is cached
	 * @nooverride This method is not intended to be overridden by clients.
	 */
	protected boolean isHandleCached() {
		return (getCache().hasEntry(getCachePath()));
	}

	/**
	 * Get the path that uniquely identifies the remote resource
	 * variant. This path describes the remote location where
	 * the remote resource is stored and also uniquely identifies
	 * each resource variant. It is used to uniquely identify this
	 * resource variant when it is stored in the resource variant cache.
	 * This path is also returned as the full path of the <code>IStorage</code>
	 * returned from this variant so the path could be converted to an
	 * <code>IPath</code> and displayed to the user.
	 * @return the full path of the remote resource variant
	 */
	protected abstract String getCachePath();

	/**
	 * Return the size (in bytes) of the contents of this resource variant.
	 * The method will return 0 if the contents have not yet been cached
	 * locally.
	 * For containers, this method will always return 0.
	 * @return the size (in bytes) of the contents of this resource variant
	 */
	public long getSize() {
		if (isContainer() || !isContentsCached()) return 0;
		ResourceVariantCacheEntry entry = getCacheEntry();
		if (entry == null || entry.getState() != ResourceVariantCacheEntry.READY) {
			return 0;
		}
		return entry.getSize();
	}

	/*
	 * Return the cache that is used to cache this resource variant and its contents.
	 * @return Returns the cache.
	 */
	private ResourceVariantCache getCache() {
		ResourceVariantCache.enableCaching(getCacheId());
		return ResourceVariantCache.getCache(getCacheId());
	}

	/**
	 * Return the ID that uniquely identifies the cache in which this resource variant
	 * is to be cache. The ID of the plugin that provides the resource variant subclass
	 * is a good candidate for this ID. The creation, management and disposal of the cache
	 * is managed by Team.
	 * @return the cache ID
	 */
	protected abstract String getCacheId();

	/**
	 * Return the cached handle for this resource variant if there is
	 * one. If there isn't one, then <code>null</code> is returned.
	 * If there is no cached handle and one is desired, then <code>cacheHandle()</code>
	 * should be called.
	 *
	 * @return a cached copy of this resource variant or <code>null</code>
	 * @nooverride This method is not intended to be overridden by clients.
	 */
	protected CachedResourceVariant getCachedHandle() {
		ResourceVariantCacheEntry entry = getCacheEntry();
		if (entry == null) return null;
		return entry.getResourceVariant();
	}

	/**
	 * Cache this handle in the cache, replacing any previously cached handle.
	 * Note that caching this handle will replace any state associated with a
	 * previously cached handle, if there is one, but the contents will remain.
	 * The reason for this is the assumption that the cache path for a resource
	 * variant (as returned by <code>getCachePath()</code> identifies an immutable
	 * resource version (or revision). The ability to replace the handle itself
	 * is provided so that additional state may be cached before or after the contents
	 * are fetched.
	 *
	 * @nooverride This method is not intended to be overridden by clients.
	 */
	protected void cacheHandle() {
		getCache().add(getCachePath(), this);
	}

	/**
	 * Return the full path of this resource that should be displayed to the
	 * user. This path is also used as the path of the <code>IStorage</code> that
	 * is returned by this instance.
	 * Subclasses may override.
	 * @return the full path of this resource that should be displayed to the
	 * user
	 *
	 * @since 3.1
	 */
	public IPath getDisplayPath() {
		return new Path(null, getCachePath());
	}

}
