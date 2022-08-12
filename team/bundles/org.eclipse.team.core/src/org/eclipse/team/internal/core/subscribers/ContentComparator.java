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
package org.eclipse.team.internal.core.subscribers;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.core.runtime.IProgressMonitor;

/**
 * This is an internal class that is used by the
 * {@link org.eclipse.team.core.synchronize.SyncInfoFilter.ContentComparisonSyncInfoFilter}
 * and {@link ContentComparisonDiffFilter} to compare the contents of the local
 * and remote resources.
 */
public class ContentComparator extends AbstractContentComparator{

	public ContentComparator(boolean ignoreWhitespace) {
		super(ignoreWhitespace);
	}

	/**
	 * Returns <code>true</code> if both input streams byte contents is
	 * identical.
	 *
	 * @param is1
	 *                   first input to contents compare
	 * @param is2
	 *                   second input to contents compare
	 * @return <code>true</code> if content is equal
	 */
	@Override
	protected boolean contentsEqual(IProgressMonitor monitor, InputStream is1,
			InputStream is2, boolean ignoreWhitespace) {
		try {
			if (is1 == is2)
				return true;
			// no byte contents
			if (is1 == null && is2 == null)
				return true;
			// only one has contents
			if (is1 == null || is2 == null)
				return false;

			while (true) {
				int c1 = is1.read();
				while (shouldIgnoreWhitespace() && isWhitespace(c1))
					c1 = is1.read();
				int c2 = is2.read();
				while (shouldIgnoreWhitespace() && isWhitespace(c2))
					c2 = is2.read();
				if (c1 == -1 && c2 == -1)
					return true;
				if (c1 != c2)
					break;
			}
		} catch (IOException ex) {
		} finally {
			try {
				try {
					if (is1 != null) {
						is1.close();
					}
				} finally {
					if (is2 != null) {
						is2.close();
					}
				}
			} catch (IOException e) {
				// Ignore
			}
		}
		return false;
	}

	private boolean isWhitespace(int c) {
		if (c == -1)
			return false;
		return Character.isWhitespace((char) c);
	}
}
