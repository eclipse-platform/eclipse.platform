/*******************************************************************************
 * Copyright (c) 2006, 2015 IBM Corporation and others.
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
 *******************************************************************************/
package org.eclipse.core.tests.internal.filesystem.wrapper;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileSystem;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.harness.CoreTest;
import org.eclipse.core.tests.resources.ResourceTest;

/**
 * A simple file system implementation that acts as a wrapper around the
 * local file system.
 * <p>
 * Also allows tests to inject a custom FileStore template class (derived from
 * {@link WrapperFileStore}). Tests can use {@link #setCustomFileStore(Class)}
 * to override default {@link WrapperFileStore} behaviour.
 * </p>
 */
public class WrapperFileSystem extends FileSystem {
	protected static final IFileStore NULL_ROOT = EFS.getNullFileSystem().getStore(IPath.ROOT);

	private static final String SCHEME_WRAPPED = "wrapped";

	private static volatile WrapperFileSystem instance;

	/** Custom file-store wrapper */
	private static volatile Class<? extends WrapperFileStore> customFS = WrapperFileStore.class;

	public static URI getBasicURI(URI wrappedURI) {
		Assert.isLegal(SCHEME_WRAPPED.equals(wrappedURI.getScheme()));
		return URI.create(wrappedURI.getQuery());
	}

	public static synchronized WrapperFileSystem getInstance() {
		if (instance != null) {
			return instance;
		}
		return instance = new WrapperFileSystem();
	}

	/**
	 * Use fs as the WrapperFileStore to use in this filesystem.
	 * Allows tests to easily override existing IFileStore behaviour.
	 * By extending {@link WrapperFileStore} conditions difficult to simulate
	 * on the LocalFileSystem can be provoked.
	 *
	 * @param fs filestore, or null to use default {@link WrapperFileStore}
	 *        based implementation.
	 */
	public static void setCustomFileStore(Class<? extends WrapperFileStore> fs) {
		if (fs == null) {
			customFS = WrapperFileStore.class;
		} else {
			customFS = fs;
		}
	}

	public static URI getWrappedURI(URI baseURI) {
		try {
			return new URI(SCHEME_WRAPPED, null, baseURI.getPath(), baseURI.toString(), null);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException(e.toString());
		}
	}

	public WrapperFileSystem() {
		instance = this;
	}

	@Override
	public IFileStore getStore(URI uri) {
		Assert.isLegal(SCHEME_WRAPPED.equals(uri.getScheme()));
		IFileStore baseStore;
		try {
			baseStore = EFS.getStore(getBasicURI(uri));
		} catch (CoreException e) {
			CoreTest.log(ResourceTest.PI_RESOURCES_TESTS, e);
			return NULL_ROOT;
		}
		return WrapperFileStore.newInstance(customFS, baseStore);
	}
}
