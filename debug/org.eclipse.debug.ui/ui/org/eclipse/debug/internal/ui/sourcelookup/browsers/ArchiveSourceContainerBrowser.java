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
package org.eclipse.debug.internal.ui.sourcelookup.browsers;


import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.ArchiveSourceContainer;
import org.eclipse.debug.internal.core.IInternalDebugCoreConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.sourcelookup.SourceLookupUIMessages;
import org.eclipse.debug.ui.sourcelookup.AbstractSourceContainerBrowser;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.ElementTreeSelectionDialog;
import org.eclipse.ui.dialogs.ISelectionStatusValidator;
import org.eclipse.ui.model.WorkbenchContentProvider;
import org.eclipse.ui.model.WorkbenchLabelProvider;
import org.eclipse.ui.views.navigator.ResourceComparator;

/**
 * Adds an internal jar to the runtime class path.
 */
public class ArchiveSourceContainerBrowser extends AbstractSourceContainerBrowser {

	private ISelectionStatusValidator validator= selection -> {
		if (selection.length == 0) {
			return new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0, IInternalDebugCoreConstants.EMPTY_STRING, null);
		}
		for (Object f : selection) {
			if (!(f instanceof IFile)) {
				return new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), 0, IInternalDebugCoreConstants.EMPTY_STRING, null);
			}
		}
		return new Status(IStatus.OK, DebugUIPlugin.getUniqueIdentifier(), 0, IInternalDebugCoreConstants.EMPTY_STRING, null);
	};

	/**
	 * Returns internal jars (source containers) currently used by the
	 * given source lookup director.
	 *
	 * @param director source lookup director jars are being added to
	 * @return the list of any found {@link ArchiveSourceContainer}s
	 */
	protected List<ISourceContainer> getSelectedJars(ISourceLookupDirector director) {
		ISourceContainer[] containers = director.getSourceContainers();
		List<ISourceContainer> jars = new ArrayList<>();
		for (ISourceContainer container : containers) {
			if (container.getType().getId().equals(ArchiveSourceContainer.TYPE_ID)) {
				jars.add(container);
			}
		}
		return jars;
	}

	@Override
	public ISourceContainer[] addSourceContainers(Shell shell, ISourceLookupDirector director) {
		ViewerFilter filter= new ArchiveFilter(getSelectedJars(director));

		ILabelProvider lp= new WorkbenchLabelProvider();
		ITreeContentProvider cp= new WorkbenchContentProvider();

		ElementTreeSelectionDialog dialog= new ElementTreeSelectionDialog(shell, lp, cp);
		dialog.setValidator(validator);
		dialog.setTitle(SourceLookupUIMessages.ArchiveSourceContainerBrowser_3);
		dialog.setMessage(SourceLookupUIMessages.ArchiveSourceContainerBrowser_4);
		dialog.addFilter(filter);
		dialog.setInput(ResourcesPlugin.getWorkspace().getRoot());
		dialog.setComparator(new ResourceComparator(ResourceComparator.NAME));

		if (dialog.open() == Window.OK) {
			Object[] result = dialog.getResult();
			ISourceContainer[] containers = new ISourceContainer[result.length];
			for (int i = 0; i < containers.length; i++) {
				containers[i] = new ArchiveSourceContainer((IFile)result[i], true);
			}
			return containers;
		}
		return new ISourceContainer[0];
	}
}
