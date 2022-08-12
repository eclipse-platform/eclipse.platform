/*******************************************************************************
 * Copyright (c) 2012 IBM Corporation and others.
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
package org.eclipse.core.internal.filesystem.local;

import org.eclipse.core.filesystem.IFileInfo;
import org.eclipse.core.filesystem.provider.FileInfo;

/**
 * This delegate provides the interface for native file attribute support.
 */
public abstract class NativeHandler {
	public abstract int getSupportedAttributes();

	public abstract FileInfo fetchFileInfo(String fileName);

	public abstract boolean putFileInfo(String fileName, IFileInfo info, int options);
}
