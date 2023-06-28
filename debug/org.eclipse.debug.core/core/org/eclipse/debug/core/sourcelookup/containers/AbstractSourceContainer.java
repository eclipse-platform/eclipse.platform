/*******************************************************************************
 *  Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.debug.core.sourcelookup.containers;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupParticipant;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceLookupParticipant;

/**
 * Common function for source containers.
 * <p>
 * Clients implementing source containers should subclass this class.
 * </p>
 * @since 3.0
 */
public abstract class AbstractSourceContainer extends PlatformObject implements ISourceContainer {

	public static final Object[] EMPTY = new Object[0];

	private ISourceLookupDirector fDirector;

	/**
	 * Throws an error exception with the given message and underlying exception.
	 *
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 */
	protected void abort(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, message, exception);
		throw new CoreException(status);
	}

	/**
	 * Throws a warning exception with the given message and underlying exception.
	 *
	 * @param message error message
	 * @param exception underlying exception, or <code>null</code>
	 * @throws CoreException if a problem is encountered
	 * @since 3.3
	 */
	protected void warn(String message, Throwable exception) throws CoreException {
		IStatus status = new Status(IStatus.WARNING, DebugPlugin.getUniqueIdentifier(), DebugPlugin.ERROR, message, exception);
		throw new CoreException(status);
	}

	/*
	 * By default, do nothing. Subclasses should override as required.
	 * @see org.eclipse.debug.core.sourcelookup.ISourceContainer#dispose()
	 */
	@Override
	public void dispose() {
		fDirector = null;
	}

	@Override
	public ISourceContainer[] getSourceContainers() throws CoreException {
		return new ISourceContainer[0];
	}

	@Override
	public boolean isComposite() {
		return false;
	}

	@Override
	public void init(ISourceLookupDirector director) {
		fDirector = director;
	}

	/**
	 * Returns the source lookup director this source container registered
	 * in, or <code>null</code> if none.
	 *
	 * @return the source lookup director this source container registered
	 * in, or <code>null</code> if none
	 */
	protected ISourceLookupDirector getDirector() {
		return fDirector;
	}

	/**
	 * Returns whether this container's source should search for duplicate source
	 * elements. Since 3.5, the current participant is consulted to determine if
	 * duplicates should be found. Fall back to querying the source lookup director
	 * if the participant is not an {@link AbstractSourceLookupParticipant}.
	 *
	 * @return whether to search for duplicate source elements
	 */
	protected boolean isFindDuplicates() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			if (director instanceof AbstractSourceLookupDirector) {
				AbstractSourceLookupDirector asld = (AbstractSourceLookupDirector) director;
				ISourceLookupParticipant participant = asld.getCurrentParticipant();
				if (participant instanceof AbstractSourceLookupParticipant	) {
					AbstractSourceLookupParticipant aslp = (AbstractSourceLookupParticipant) participant;
					return aslp.isFindDuplicates();
				}
			}
			return director.isFindDuplicates();
		}
		return false;
	}

	/**
	 * Returns the source container type identified by the given id,
	 * or <code>null</code> if none.
	 *
	 * @param id source container type identifier
	 * @return source container type or <code>null</code>
	 */
	protected ISourceContainerType getSourceContainerType(String id) {
		return DebugPlugin.getDefault().getLaunchManager().getSourceContainerType(id);
	}
}
