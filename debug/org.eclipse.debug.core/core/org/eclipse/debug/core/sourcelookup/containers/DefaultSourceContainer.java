/*******************************************************************************
 * Copyright (c) 2003, 2013 IBM Corporation and others.
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

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.internal.core.sourcelookup.SourceLookupMessages;

/**
 * A source container that computer the default source lookup path
 * for a launch configuration on each launch using a launch configuration's
 * associated source path computer.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class DefaultSourceContainer extends CompositeSourceContainer {

	/**
	 * Unique identifier for the default source container type
	 * (value <code>org.eclipse.debug.core.containerType.default</code>).
	 */
	public static final String TYPE_ID = DebugPlugin.getUniqueIdentifier() + ".containerType.default"; //$NON-NLS-1$

	/**
	 * Constructs a default source container.
	 */
	public DefaultSourceContainer() {
	}

	@Override
	public boolean equals(Object obj) {
		return obj instanceof DefaultSourceContainer;
	}

	@Override
	public int hashCode() {
		return getClass().hashCode();
	}

	/**
	 * Returns the launch configuration for which a default source lookup
	 * path will be computed, or <code>null</code> if none.
	 *
	 * @return the launch configuration for which a default source lookup
	 * path will be computed, or <code>null</code>
	 */
	protected ILaunchConfiguration getLaunchConfiguration() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.getLaunchConfiguration();
		}
		return null;
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}

	/**
	 * Returns the source path computer to use, or <code>null</code>
	 * if none.
	 *
	 * @return the source path computer to use, or <code>null</code>
	 * if none
	 */
	private ISourcePathComputer getSourcePathComputer() {
		ISourceLookupDirector director = getDirector();
		if (director != null) {
			return director.getSourcePathComputer();
		}
		return null;
	}

	@Override
	public String getName() {
		return SourceLookupMessages.DefaultSourceContainer_0;
	}

	@Override
	protected ISourceContainer[] createSourceContainers() throws CoreException {
		ISourcePathComputer sourcePathComputer = getSourcePathComputer();
		if (sourcePathComputer != null) {
			ILaunchConfiguration config= getLaunchConfiguration();
			if (config != null) {
				return sourcePathComputer.computeSourceContainers(config, null);
			}
		}

		return new ISourceContainer[0];
	}
}