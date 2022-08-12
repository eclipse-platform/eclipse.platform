/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 488937
 *******************************************************************************/
package org.eclipse.core.internal.resources;

import java.io.DataInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.eclipse.core.internal.utils.Messages;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.osgi.util.NLS;

/**
 * This class is used to read sync info from disk. Subclasses implement
 * version specific reading code.
 */
public class SyncInfoReader {
	protected Workspace workspace;
	protected Synchronizer synchronizer;

	public SyncInfoReader(Workspace workspace, Synchronizer synchronizer) {
		super();
		this.workspace = workspace;
		this.synchronizer = synchronizer;
	}

	/**
	 * Returns the appropriate reader for the given version.
	 */
	protected SyncInfoReader getReader(int formatVersion) throws IOException {
		switch (formatVersion) {
			case 2 :
				return new SyncInfoReader_2(workspace, synchronizer);
			case 3 :
				return new SyncInfoReader_3(workspace, synchronizer);
			default :
				throw new IOException(NLS.bind(Messages.resources_format, formatVersion));
		}
	}

	public void readPartners(DataInputStream input) throws CoreException {
		try {
			int size = input.readInt();
			Set<QualifiedName> registry = new HashSet<>(size);
			for (int i = 0; i < size; i++) {
				String qualifier = input.readUTF();
				String local = input.readUTF();
				registry.add(new QualifiedName(qualifier, local));
			}
			synchronizer.setRegistry(registry);
		} catch (IOException e) {
			String message = NLS.bind(Messages.resources_readSync, e);
			throw new ResourceException(new ResourceStatus(IResourceStatus.INTERNAL_ERROR, message));
		}
	}

	public void readSyncInfo(DataInputStream input) throws IOException, CoreException {
		// dispatch to the appropriate reader depending
		// on the version of the file
		int formatVersion = readVersionNumber(input);
		SyncInfoReader reader = getReader(formatVersion);
		reader.readSyncInfo(input);
	}

	protected static int readVersionNumber(DataInputStream input) throws IOException {
		return input.readInt();
	}
}
