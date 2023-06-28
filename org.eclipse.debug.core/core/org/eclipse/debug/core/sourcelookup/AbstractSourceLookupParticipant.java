/*******************************************************************************
 *  Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;


/**
 * Common super class for implementations of source lookup participants.
 * <p>
 * Clients implementing source lookup participants should subclass this class.
 * </p>
 * @since 3.0
 */
public abstract class AbstractSourceLookupParticipant implements ISourceLookupParticipant {

	private ISourceLookupDirector fDirector;

	protected static final Object[] EMPTY = new Object[0];

	@Override
	public void init(ISourceLookupDirector director) {
		fDirector = director;
	}

	@Override
	public void dispose() {
		fDirector = null;
	}

	@Override
	public Object[] findSourceElements(Object object) throws CoreException {
		List<Object> results = null;
		CoreException single = null;
		MultiStatus multiStatus = null;
		if (isFindDuplicates()) {
			results = new ArrayList<>();
		}
		String name = getSourceName(object);
		if (name != null) {
			ISourceContainer[] containers = getSourceContainers();
			for (ISourceContainer c : containers) {
				try {
					ISourceContainer container = getDelegateContainer(c);
					if (container != null) {
						Object[] objects = container.findSourceElements(name);
						if (objects.length > 0) {
							//it will only not be null when we care about duplicates
							//saves the computation in isFindDuplicates()
							if (results != null) {
								Collections.addAll(results, objects);
							} else {
								if (objects.length == 1) {
									return objects;
								}
								return new Object[]{objects[0]};
							}
						}
					}
				} catch (CoreException e) {
					if (single == null) {
						single = e;
					} else if (multiStatus == null) {
						multiStatus = new MultiStatus(DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, new IStatus[]{single.getStatus()}, SourceLookupMessages.Source_Lookup_Error, null);
						multiStatus.add(e.getStatus());
					} else {
						multiStatus.add(e.getStatus());
					}
				}
			}
		}
		if (results == null) {
			if (multiStatus != null) {
				throw new CoreException(multiStatus);
			} else if (single != null) {
				throw single;
			}
			return EMPTY;
		}
		return results.toArray();
	}

	/**
	 * Returns the source container to search in place of the given source
	 * container, or <code>null</code> if the given source container is not
	 * to be searched. The default implementation does not translate source
	 * containers. Subclasses should override if required.
	 *
	 * @param container the source container about to be searched (proxy)
	 * @return the source container to be searched (delegate), or <code>null</code>
	 * 	if the source container should not be searched
	 */
	protected ISourceContainer getDelegateContainer(ISourceContainer container) {
		return container;
	}

	/**
	 * Returns the source lookup director this participant is registered with
	 * or <code>null</code> if none.
	 *
	 * @return the source lookup director this participant is registered with
	 *  or <code>null</code> if none
	 */
	protected ISourceLookupDirector getDirector() {
		return fDirector;
	}

	/**
	 * Returns whether this participant's source lookup director is configured
	 * to search for duplicate source elements.
	 *
	 * @return whether this participant's source lookup director is configured
	 * to search for duplicate source elements
	 * @since 3.5
	 */
	public boolean isFindDuplicates() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.isFindDuplicates();
		}
		return false;
	}

	/**
	 * Returns the source containers currently registered with this participant's
	 * source lookup director.
	 *
	 * @return the source containers currently registered with this participant's
	 * source lookup director
	 */
	protected ISourceContainer[] getSourceContainers() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.getSourceContainers();
		}
		return new ISourceContainer[0];
	}

	@Override
	public void sourceContainersChanged(ISourceLookupDirector director) {
	}
}
