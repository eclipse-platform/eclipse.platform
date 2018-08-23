/*******************************************************************************
 * Copyright (c) 2005, 2010 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.wizards;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;

public class PsfFilenameStore extends PsfStore {

	private static final String FILENAMES = "filenames"; //$NON-NLS-1$
	private static final String PREVIOUS = "previous"; //$NON-NLS-1$

	// If a PSF file was selected when the wizard was opened, then this is it.
	// This is only a cache; it is not part of the history until the user has used it.
	private String _selectedFilename = null;

	private static PsfFilenameStore instance;

	public static PsfFilenameStore getInstance(){
		if(instance==null){
			instance = new PsfFilenameStore();
		}
		return instance;
	}

	private PsfFilenameStore() {
		// Singleton
	}

	public void setDefaultFromSelection(IWorkbench workbench) {
		// Scan the workbench for a selected PSF file
		IWorkbenchWindow wnd = workbench.getActiveWorkbenchWindow();
		IWorkbenchPage pg = wnd.getActivePage();
		ISelection sel = pg.getSelection();

		if (!(sel instanceof IStructuredSelection)) {
			return;
		}
		IStructuredSelection selection = (IStructuredSelection)sel;

		Object firstElement = selection.getFirstElement();
		if (!(firstElement instanceof IAdaptable)) {
			return;
		}
		Object o = ((IAdaptable) firstElement).getAdapter(IResource.class);
		if (o == null) {
			return;
		}
		IResource resource = (IResource) o;

		if (resource.getType() != IResource.FILE) {
			return;
		}

		if (!resource.isAccessible()) {
			return;
		}

		String extension = resource.getFileExtension();
		if (extension == null || !extension.equalsIgnoreCase("psf")) { //$NON-NLS-1$
			return;
		}

		IWorkspace workspace = resource.getWorkspace();
		workspace.getRoot().getFullPath();

		IPath path = resource.getLocation();
		_selectedFilename = path.toOSString();
	}

	@Override
	public String getSuggestedDefault() {
		if (_selectedFilename != null) {
			return _selectedFilename;
		}
		return getPrevious();
	}

	@Override
	protected String getPreviousTag() {
		return PREVIOUS;
	}

	@Override
	protected String getListTag() {
		return FILENAMES;
	}


}
