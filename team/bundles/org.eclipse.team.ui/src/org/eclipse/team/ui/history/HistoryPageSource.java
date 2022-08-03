/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.history;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.core.history.IFileHistoryProvider;
import org.eclipse.team.internal.ui.Utils;

/**
 * Abstract HistoryPageSource class.
 * @see IHistoryPageSource
 * @since 3.2
 */
public abstract class HistoryPageSource implements IHistoryPageSource {

	/**
	 * Convenience method that returns the history page source for the
	 * given object. This method only finds a source. It does not query the source
	 * to see if the source can display history for the given object.
	 * @param object the object
	 * @return he history page source for the
	 * given object
	 */
	public static IHistoryPageSource getHistoryPageSource(Object object) {
		IResource resource = Utils.getResource(object);
		if (resource != null) {
			RepositoryProvider provider = RepositoryProvider.getProvider(resource.getProject());
			if (provider != null) {
				IFileHistoryProvider fileHistoryProvider = provider.getFileHistoryProvider();
				if (fileHistoryProvider != null) {
					IHistoryPageSource pageSource = Adapters.adapt(fileHistoryProvider, IHistoryPageSource.class);
					if (pageSource != null)
						return pageSource;
				}
				IHistoryPageSource pageSource = Adapters.adapt(provider, IHistoryPageSource.class);
				if (pageSource != null)
					return pageSource;
			}
		}
		IHistoryPageSource pageSource = Adapters.adapt(object, IHistoryPageSource.class);
		return pageSource;
	}

}
