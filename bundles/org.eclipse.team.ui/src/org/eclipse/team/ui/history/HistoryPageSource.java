/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.ui.history;

import org.eclipse.core.resources.IResource;
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
					IHistoryPageSource pageSource = (IHistoryPageSource)Utils.getAdapter(fileHistoryProvider, IHistoryPageSource.class);
					if (pageSource != null)
						return pageSource;
				}
				IHistoryPageSource pageSource = (IHistoryPageSource)Utils.getAdapter(provider, IHistoryPageSource.class);
				if (pageSource != null)
					return pageSource;
			}
		}
		IHistoryPageSource pageSource = (IHistoryPageSource)Utils.getAdapter(object, IHistoryPageSource.class);
		return pageSource;
	}

}
