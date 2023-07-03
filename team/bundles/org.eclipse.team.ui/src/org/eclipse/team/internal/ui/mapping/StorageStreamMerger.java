/*******************************************************************************
 * Copyright (c) 2000, 2006 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.mapping;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.eclipse.compare.IStreamMerger;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.team.core.mapping.IStorageMerger;
import org.eclipse.team.internal.ui.TeamUIPlugin;

/**
 * A merge context that performs three-way merges using the {@link IStreamMerger}
 * interface.
 * @since 3.2
 */
public class StorageStreamMerger implements IStorageMerger {

	private IStreamMerger merger;

	public StorageStreamMerger(IStreamMerger merger) {
		this.merger = merger;
	}

	@Override
	public IStatus merge(OutputStream output, String outputEncoding, IStorage ancestorStorage, IStorage targetStorage, IStorage otherStorage, IProgressMonitor monitor) throws CoreException {
		IStatus status = null;
		try (
			InputStream ancestorStream = new BufferedInputStream(ancestorStorage.getContents());
			InputStream remoteStream = new BufferedInputStream(otherStorage.getContents());
			InputStream targetStream = new BufferedInputStream(targetStorage.getContents())
				){
			status = merger.merge(output, outputEncoding,
					ancestorStream, getEncoding(ancestorStorage, outputEncoding),
					targetStream, getEncoding(targetStorage, outputEncoding),
					remoteStream, getEncoding(otherStorage, outputEncoding),
					monitor);
			if (status.isOK())
				return status;
			if (status.getCode() == IStreamMerger.CONFLICT)
				return new Status(status.getSeverity(), status.getPlugin(), CONFLICT, status.getMessage(), status.getException());
		} catch (IOException closeException) {
			// Ignore
		}
		return status;
	}

	private String getEncoding(IStorage ancestorStorage, String outputEncoding) {
		if (ancestorStorage instanceof IEncodedStorage) {
			IEncodedStorage es = (IEncodedStorage) ancestorStorage;
			try {
				String charSet = es.getCharset();
				if (charSet != null)
					return charSet;
			} catch (CoreException e) {
				TeamUIPlugin.log(e);
			}
		}
		return outputEncoding;
	}

	@Override
	public boolean canMergeWithoutAncestor() {
		return false;
	}

}
