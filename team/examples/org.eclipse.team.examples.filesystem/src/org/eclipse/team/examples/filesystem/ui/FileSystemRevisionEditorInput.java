/*******************************************************************************
 * Copyright (c) 2006, 2018 IBM Corporation and others.
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
package org.eclipse.team.examples.filesystem.ui;

import java.text.DateFormat;
import java.util.Date;

import org.eclipse.core.resources.IFileState;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.model.IWorkbenchAdapter;

public class FileSystemRevisionEditorInput extends PlatformObject implements IWorkbenchAdapter, IStorageEditorInput {

	private IFileRevision fileRevision;
	private IStorage storage;

	public FileSystemRevisionEditorInput(IFileRevision revision) {
		this.fileRevision = revision;
		try {
			this.storage = revision.getStorage(new NullProgressMonitor());
		} catch (CoreException e) {
			// ignore
		}
	}

	@Override
	public Object[] getChildren(Object o) {
		return new Object[0];
	}

	@Override
	public ImageDescriptor getImageDescriptor(Object object) {
		return null;
	}

	@Override
	public String getLabel(Object o) {
		if (storage != null) {
			return storage.getName();
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public Object getParent(Object o) {
		return null;
	}

	@Override
	public IStorage getStorage() {
		return storage;
	}

	@Override
	public boolean exists() {
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return null;
	}

	@Override
	public String getName() {
		if (fileRevision != null)
			return fileRevision.getName() + " " + fileRevision.getContentIdentifier();  //$NON-NLS-1$

		if (storage != null) {
			return storage.getName() + " " + DateFormat.getInstance().format(new Date(((IFileState) storage).getModificationTime())); //$NON-NLS-1$
		}
		return ""; //$NON-NLS-1$
	}

	@Override
	public IPersistableElement getPersistable() {
		return null;
	}

	@Override
	public String getToolTipText() {
		if (fileRevision != null)
			return getStorage().getFullPath().toString();

		if (storage != null)
			return storage.getFullPath().toString();

		return ""; //$NON-NLS-1$
	}

	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter == IWorkbenchAdapter.class) {
			return adapter.cast(this);
		}
		if (adapter == IFileRevision.class)
			return adapter.cast(fileRevision);
		else if  (adapter == IFileState.class){
			if (storage != null && storage instanceof IFileState)
				return adapter.cast(storage);
		}
		return super.getAdapter(adapter);
	}

}
