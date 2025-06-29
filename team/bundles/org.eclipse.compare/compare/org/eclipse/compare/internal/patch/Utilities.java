/*******************************************************************************
 * Copyright (c) 2008, 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.compare.internal.patch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;

import org.eclipse.compare.internal.CompareMessages;
import org.eclipse.compare.internal.CompareUIPlugin;
import org.eclipse.compare.internal.core.patch.DiffProject;
import org.eclipse.compare.patch.ReaderCreator;
import org.eclipse.core.resources.IEncodedStorage;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

public class Utilities {

	public static String getCharset(Object resource) {
		if (resource instanceof IEncodedStorage) {
			try {
				return ((IEncodedStorage) resource).getCharset();
			} catch (CoreException ex) {
				CompareUIPlugin.log(ex);
			}
		}
		return ResourcesPlugin.getEncoding();
	}

	public static IProject getProject(DiffProject diffProject) {
		return ResourcesPlugin.getWorkspace().getRoot().getProject(diffProject.getName());
	}

	public static ReaderCreator getReaderCreator(final IStorage storage) {
		return new ReaderCreator() {
			@Override
			public Reader createReader() throws CoreException {
				return Utilities.createReader(storage);
			}

			@Override
			public boolean canCreateReader() {
				if (storage == null
						|| (storage instanceof IFile && !((IFile) storage).isAccessible())) {
					return false;
				}
				return true;
			}
		};
	}

	public static BufferedReader createReader(IStorage storage)
			throws CoreException {
		if (storage == null
				|| (storage instanceof IFile && !((IFile) storage).isAccessible())) {
			throw new CoreException(new Status(IStatus.WARNING,
					CompareUIPlugin.PLUGIN_ID,
					CompareMessages.ReaderCreator_fileIsNotAccessible));
		}
		String charset = null;
		if (storage instanceof IEncodedStorage es) {
			charset = es.getCharset();
		}
		InputStreamReader in = null;
		if (charset != null) {
			InputStream contents = storage.getContents();
			try {
				in = new InputStreamReader(contents, charset);
			} catch (UnsupportedEncodingException e) {
				CompareUIPlugin.log(e);
				try {
					contents.close();
				} catch (IOException e1) {
					// Ignore
				}
			}
		}
		if (in == null) {
			in = new InputStreamReader(storage.getContents());
		}
		return new BufferedReader(in);
	}
}
