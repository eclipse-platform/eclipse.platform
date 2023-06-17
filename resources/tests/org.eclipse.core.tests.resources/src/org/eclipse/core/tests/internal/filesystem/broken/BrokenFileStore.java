/*******************************************************************************
 * Copyright (c) 2007, 2015 IBM Corporation and others.
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
package org.eclipse.core.tests.internal.filesystem.broken;

import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.provider.FileStore;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.junit.Assert;

/**
 *
 */
public class BrokenFileStore extends FileStore {

	private URI uri;

	public BrokenFileStore(URI uri) {
		this.uri = uri;
	}

	public BrokenFileStore(IPath path) {
		try {
			uri = new URI(uri.getScheme(), path.toString(), null);
		} catch (URISyntaxException e) {
			Assert.fail(e.getMessage());
		}
	}

	@Override
	public String[] childNames(int options, IProgressMonitor monitor) throws CoreException {
		fail();
		return null;
	}

	private void fail() throws CoreException {
		throw new CoreException(new Status(IStatus.ERROR, "org.eclipse.core.tests.resources", 1, "This exception is thrown on purpose as part of a test", null));
	}

	@Override
	public IFileInfo fetchInfo(int options, IProgressMonitor monitor) throws CoreException {
		fail();
		return null;
	}

	private IPath getPath() {
		return IPath.fromOSString(uri.getSchemeSpecificPart());
	}

	@Override
	public IFileStore getChild(String name) {
		return new BrokenFileStore(getPath().append(name));
	}

	@Override
	public String getName() {
		return getPath().lastSegment();
	}

	@Override
	public IFileStore getParent() {
		IPath path = getPath();
		if (path.segmentCount() == 0) {
			return null;
		}
		return new BrokenFileStore(path.removeLastSegments(1));
	}

	@Override
	public InputStream openInputStream(int options, IProgressMonitor monitor) throws CoreException {
		fail();
		return null;
	}

	@Override
	public URI toURI() {
		return uri;
	}

}
