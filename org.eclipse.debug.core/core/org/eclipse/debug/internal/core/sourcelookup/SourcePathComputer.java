/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
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
package org.eclipse.debug.internal.core.sourcelookup;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputer;
import org.eclipse.debug.core.sourcelookup.ISourcePathComputerDelegate;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;

/**
 * Proxy to contributed source path computer extension.
 *
 * @see IConfigurationElementConstants
 */
public class SourcePathComputer implements ISourcePathComputer {

	// lazily instantiated delegate
	private ISourcePathComputerDelegate fDelegate = null;

	// extension definition
	private IConfigurationElement fElement = null;

	/**
	 * Constructs a source path computer on the given extension.
	 *
	 * @param element extension definition
	 */
	public SourcePathComputer(IConfigurationElement element) {
		fElement = element;
	}

	@Override
	public String getId() {
		return fElement.getAttribute(IConfigurationElementConstants.ID);
	}

	/**
	 * Lazily instantiates and returns the underlying source container type.
	 * @return the {@link ISourcePathComputerDelegate}
	 * @exception CoreException if unable to instantiate
	 */
	private ISourcePathComputerDelegate getDelegate() throws CoreException {
		if (fDelegate == null) {
			fDelegate = (ISourcePathComputerDelegate) fElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
		}
		return fDelegate;
	}

	@Override
	public ISourceContainer[] computeSourceContainers(ILaunchConfiguration configuration, IProgressMonitor monitor) {
		try {
			return getDelegate().computeSourceContainers(configuration, monitor);
		} catch (CoreException e) {
			DebugPlugin.log(e);
		}
		return new ISourceContainer[0];
	}
}
