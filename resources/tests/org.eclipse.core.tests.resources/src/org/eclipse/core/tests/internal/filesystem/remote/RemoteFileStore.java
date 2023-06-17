/*******************************************************************************
 *  Copyright (c) 2012, 2015 IBM Corporation and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.internal.filesystem.remote;

import java.net.URI;
import java.net.URISyntaxException;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.tests.internal.filesystem.ram.MemoryFileStore;

/**
 * A test file store that mocks remote file store and keeps everything in memory.
 */
public class RemoteFileStore extends MemoryFileStore {
	private static String createAuthoritySegment(String userInfo, String host, int port) {
		String authority = host;
		if (userInfo != null) {
			authority = userInfo + "___" + authority;
		}
		if (port != -1) {
			authority += "___" + port;
		}
		return authority;
	}

	private String userInfo;
	private String host;
	private int port;
	private IPath remotePath;

	public RemoteFileStore(String userInfo, String host, int port, IPath path) {
		super(IPath.ROOT.append(createAuthoritySegment(userInfo, host, port)).append(path));
		this.userInfo = userInfo;
		this.host = host;
		this.port = port;
		this.remotePath = path;
	}

	@Override
	public IFileStore getChild(String name) {
		return new RemoteFileStore(userInfo, host, port, remotePath.append(name));
	}

	@Override
	public IFileStore getParent() {
		if (remotePath.segmentCount() == 0) {
			return null;
		}
		return new RemoteFileStore(userInfo, host, port, remotePath.removeLastSegments(1));
	}

	@Override
	public URI toURI() {
		try {
			return new URI(RemoteFileSystem.SCHEME_REMOTE, userInfo, host, port, remotePath.toString(), null, null);
		} catch (URISyntaxException e) {
			//should not happen
			throw new RuntimeException(e);
		}
	}
}
