/*******************************************************************************
 * Copyright (c) 2003, 2018 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * A source container of source containers.
 * <p>
 * Clients implementing composite source containers should subclass
 * this class.
 * </p>
 * @since 3.0
 */
public abstract class CompositeSourceContainer extends AbstractSourceContainer {

	private ISourceContainer[] fContainers;

	@Override
	public boolean isComposite() {
		return true;
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		return findSourceElements(name, getSourceContainers());
	}

	/**
	 * Returns a collection of source elements in the given containers corresponding to
	 * the given name. Returns an empty collection if no source elements are found.
	 * This source container's source lookup director specifies if duplicate
	 * source elements should be searched for, via <code>isFindDuplicates()</code>.
	 * When <code>false</code> the returned collection should contain at most one
	 * source element. If this is a composite container, the containers contained
	 * by this container are also searched.
	 * <p>
	 * The format of the given name is implementation specific but generally conforms
	 * to the format of a file name. If a source container does not recognize the
	 * name format provided, an empty collection should be returned. A source container
	 * may or may not require names to be fully qualified (i.e. be qualified with directory
	 * names).
	 * </p>
	 * @param name the name of the source element to search for
	 * @param containers the containers to search
	 * @return a collection of source elements corresponding to the given name
	 * @exception CoreException if an exception occurs while searching for source elements
	 */
	protected Object[] findSourceElements(String name, ISourceContainer[] containers) throws CoreException {
		List<Object> results = null;
		CoreException single = null;
		MultiStatus multiStatus = null;
		if (isFindDuplicates()) {
			results = new ArrayList<>();
		}
		for (ISourceContainer container : containers) {
			try {
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
	 * Creates the source containers in this composite container.
	 * Subclasses should override this methods.
	 * @return the array of {@link ISourceContainer}s
	 *
	 * @throws CoreException if unable to create the containers
	 */
	protected abstract ISourceContainer[] createSourceContainers() throws CoreException;

	@Override
	public synchronized ISourceContainer[] getSourceContainers() throws CoreException {
		if (fContainers == null) {
			fContainers = createSourceContainers();
			for (ISourceContainer container : fContainers) {
				container.init(getDirector());
			}
		}
		return fContainers;
	}

	@Override
	public void dispose() {
		super.dispose();
		if (fContainers != null) {
			for (ISourceContainer container : fContainers) {
				container.dispose();
			}
		}
		fContainers = null;
	}
}
