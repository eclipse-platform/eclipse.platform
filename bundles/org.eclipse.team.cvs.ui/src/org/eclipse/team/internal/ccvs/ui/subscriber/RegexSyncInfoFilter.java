/*******************************************************************************
 * Copyright (c) 2011 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ccvs.ui.subscriber;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.synchronize.SyncInfo;
import org.eclipse.team.core.synchronize.SyncInfoFilter;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.core.subscribers.AbstractContentComparator;
import org.eclipse.team.internal.ui.synchronize.RegexDiffComparator;

/**
 * Selects <code>SyncInfo</code> whose all diffs match the given pattern.
 * This filter makes use of the <code>IStorage</code> provided by
 * an <code>IResourceVariant</code> to obtain the remote contents.
 * This means that the comparison may contact the server unless the contents
 * were cached locally by a previous operation. The caching of remote
 * contents is subscriber specific.
 * <p>
 * For folders, the comparison always returns <code>true</code>.
 *
 * @since 3.6
 */
public class RegexSyncInfoFilter extends SyncInfoFilter {

	AbstractContentComparator criteria;

	boolean ignoreWhiteSpace;

	/**
	 * Create a filter that does not ignore whitespace.
	 *
	 * @param pattern
	 *            regex pattern
	 */
	public RegexSyncInfoFilter(String pattern) {
		this(false, pattern);
	}

	public RegexSyncInfoFilter(boolean ignoreWhitespace, String pattern) {
		criteria = new RegexDiffComparator(Pattern.compile(pattern,
				Pattern.DOTALL), ignoreWhitespace);
	}

	public boolean select(SyncInfo info, IProgressMonitor monitor) {
		IResourceVariant remote = info.getRemote();
		IResource local = info.getLocal();
		if (local.getType() != IResource.FILE)
			return true;
		if (remote == null)
			return !local.exists();
		if (!local.exists())
			return false;
		return criteria.compare(local, remote, monitor);
	}
}
