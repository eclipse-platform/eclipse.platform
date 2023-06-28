/*******************************************************************************
 * Copyright (c) 2004, 2005 IBM Corporation and others.
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
package org.eclipse.debug.ui.sourcelookup;

import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.swt.widgets.Shell;

/**
 * Common implementation for source container browsers.
 * <p>
 * Clients implementing <code>ISourceContainerBrowser</code> should
 * subclass this class.
 * </p>
 * @since 3.0
 */
public class AbstractSourceContainerBrowser implements ISourceContainerBrowser {

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		return new ISourceContainer[0];
	}

	/*
	 * Generally, a source container browser can add source containers. Subclasses
	 * should override as required.
	 */
	@Override
	public boolean canAddSourceContainers(ISourceLookupDirector director) {
		return true;
	}

	@Override
	public ISourceContainer[] editSourceContainers(Shell shell, ISourceLookupDirector director, ISourceContainer[] containers) {
		return new ISourceContainer[0];
	}

	/*
	 * Not all source containers can be edited. Subclasses should override as
	 * required.
	 */
	@Override
	public boolean canEditSourceContainers(ISourceLookupDirector director, ISourceContainer[] containers) {
		return false;
	}
}
