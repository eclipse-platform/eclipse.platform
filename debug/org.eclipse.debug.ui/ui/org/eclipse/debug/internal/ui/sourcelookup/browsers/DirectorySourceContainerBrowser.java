/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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
 *     QNX Software Systems - Mikhail Khodjaiants - Bug 114664
 *******************************************************************************/
package org.eclipse.debug.internal.ui.sourcelookup.browsers;

import org.eclipse.core.runtime.IPath;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.DirectorySourceContainer;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;

/**
 * The browser for adding an external folder source container.
 * @since 3.0
 */
public class DirectorySourceContainerBrowser extends AbstractSourceContainerBrowser {

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		ISourceContainer[] containers = new ISourceContainer[1];
		DirectorySourceContainerDialog dialog = new DirectorySourceContainerDialog(shell);
		if (dialog.open() == Window.OK) {
			String directory = dialog.getDirectory();
			if(directory !=null) {
				containers[0] = new DirectorySourceContainer(IPath.fromOSString(directory),
						dialog.isSearchSubfolders());
				return containers;
			}
		}
		return new ISourceContainer[0];
	}

	@Override
	public boolean canEditSourceContainers(ISourceLookupDirector director, ISourceContainer[] containers) {
		return containers.length == 1 && DirectorySourceContainer.TYPE_ID.equals(containers[0].getType().getId());
	}

	@Override
	public ISourceContainer[] editSourceContainers(Shell shell, ISourceLookupDirector director, ISourceContainer[] containers) {
		if (containers.length == 1 && DirectorySourceContainer.TYPE_ID.equals(containers[0].getType().getId()) ) {
			DirectorySourceContainer c = (DirectorySourceContainer)containers[0];
			DirectorySourceContainerDialog dialog = new DirectorySourceContainerDialog(shell, c.getDirectory().getPath(), c.isComposite());
			if (dialog.open() == Window.OK) {
				String directory = dialog.getDirectory();
				if(directory !=null) {
					containers[0].dispose();
					return new ISourceContainer[] {
							new DirectorySourceContainer(IPath.fromOSString(directory), dialog.isSearchSubfolders()) };
				}
			}
		}
		return containers;
	}

}
