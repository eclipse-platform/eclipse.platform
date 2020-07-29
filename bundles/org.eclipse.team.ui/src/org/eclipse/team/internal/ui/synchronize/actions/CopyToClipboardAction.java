/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.synchronize.actions;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.eclipse.compare.ITypedElement;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreePathLabelProvider;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.ViewerLabel;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.FileTransfer;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.TeamUIPlugin;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.ui.actions.SelectionListenerAction;
import org.eclipse.ui.navigator.INavigatorContentService;
import org.eclipse.ui.part.ResourceTransfer;

/**
 * Based on org.eclipse.ui.views.navigator.CopyAction with the additional support for
 * copying any non-resource object in the selection and putting the toString() as
 * a text transfer.
 *
 * @since 3.1
 */
class CopyToClipboardAction extends SelectionListenerAction {

	private static final String EOL = System.getProperty("line.separator", "\n"); //$NON-NLS-1$ //$NON-NLS-2$

	private final static String ID= TeamUIPlugin.PLUGIN_ID + ".synchronize.action.copy";  //$NON-NLS-1$

	private final Shell fShell;
	private final Clipboard fClipboard;

	private final INavigatorContentService navigatorContentService;

	protected CopyToClipboardAction(Shell shell, INavigatorContentService navigatorContentService) {
		super(TeamUIMessages.CopyToClipboardAction_1);
		this.navigatorContentService = navigatorContentService;
		Assert.isNotNull(shell);
		fShell= shell;
		fClipboard= new Clipboard(shell.getDisplay());
		setToolTipText(TeamUIMessages.CopyToClipboardAction_2);
		setId(ID);
	}

	@Override
	public void run() {
		copyResources(getSelectedResources(), getTextualClipboardContents());
	}

	/*
	 * Return a text representation of all selected elements.
	 * Use the name from the tree node so what is copied
	 * matches what appears in the tree.
	 */
	private String getTextualClipboardContents() {
		StringBuilder buf = new StringBuilder();
		int i = 0;
		IStructuredSelection structuredSelection = getStructuredSelection();
		if (structuredSelection instanceof TreeSelection) {
			TreeSelection ts = (TreeSelection) structuredSelection;
			TreePath[] paths = ts.getPaths();
			for (TreePath path : paths) {
				String text = getTextFor(path);
				if (text != null && text.length() > 0) {
					if (i > 0)
						buf.append(EOL);
					buf.append(text);
					i++;
				}
			}
		} else {
			for (Object element : structuredSelection) {
				if (element instanceof ITypedElement) {
					if (i > 0)
						buf.append(EOL);
					buf.append(((ITypedElement)element).getName());
					i++;
				} else {
					IResource resource = Utils.getResource(element);
					if (resource != null) {
						if (i > 0)
							buf.append(EOL);
						buf.append(resource.getName());
						i++;
					}
				}
			}
		}
		return buf.toString();
	}

	private String getTextFor(TreePath path) {
		Object element = path.getLastSegment();
		if (element instanceof ITypedElement) {
			return ((ITypedElement)element).getName();
		}
		INavigatorContentService service = getNavigatorContentService();
		if (service != null) {
			ILabelProvider provider = service.createCommonLabelProvider();
			if (provider instanceof ITreePathLabelProvider) {
				ITreePathLabelProvider tplp = (ITreePathLabelProvider) provider;
				ViewerLabel viewerLabel = new ViewerLabel("", null); //$NON-NLS-1$
				tplp.updateLabel(viewerLabel, path);
				return viewerLabel.getText();
			}
			return provider.getText(element);
		}
		if (element instanceof IResource) {
			IResource resource = (IResource) element;
			return resource.getName();
		}
		return null;
	}

	private INavigatorContentService getNavigatorContentService() {
		return navigatorContentService;
	}

	private void copyResources(List<? extends IResource> selectedResources, String text) {
		IResource[] resources = selectedResources.toArray(new IResource[selectedResources.size()]);
		// Get the file names and a string representation
		final int length = resources.length;
		int actualLength = 0;
		String[] fileNames = new String[length];
		for (int i = 0; i < length; i++) {
			final IPath location = resources[i].getLocation();
			// location may be null. See bug 29491.
			if (location != null)
				fileNames[actualLength++] = location.toOSString();
		}
		// was one or more of the locations null?
		if (actualLength < length) {
			String[] tempFileNames = fileNames;
			fileNames = new String[actualLength];
			System.arraycopy(tempFileNames, 0, fileNames, 0, actualLength);
		}
		setClipboard(resources, fileNames, text);
	}

	/**
	 * Set the clipboard contents. Prompt to retry if clipboard is busy.
	 *
	 * @param resources the resources to copy to the clipboard
	 * @param fileNames file names of the resources to copy to the clipboard
	 * @param names string representation of all names
	 */
	private void setClipboard(IResource[] resources, String[] fileNames, String names) {
		try {
			// set the clipboard contents
			List data = new ArrayList();
			List<Transfer> dataTypes = new ArrayList<>();
			if (resources.length > 0) {
				data.add(resources);
				dataTypes.add(ResourceTransfer.getInstance());
			}
			if (fileNames.length > 0) {
				data.add(fileNames);
				dataTypes.add(FileTransfer.getInstance());
			}
			if (names != null && names.length() > 0) {
				data.add(names);
				dataTypes.add(TextTransfer.getInstance());
			}
			if (!data.isEmpty())
				fClipboard.setContents(
						data.toArray(),
						dataTypes.toArray(new Transfer[dataTypes.size()]));
		} catch (SWTError e) {
			if (e.code != DND.ERROR_CANNOT_SET_CLIPBOARD)
				throw e;
			if (MessageDialog.openQuestion(fShell, TeamUIMessages.CopyToClipboardAction_3, TeamUIMessages.CopyToClipboardAction_4))
				setClipboard(resources, fileNames, names);
		}
	}

	@Override
	protected boolean updateSelection(IStructuredSelection selection) {
		if (!super.updateSelection(selection))
			return false;
		// Calling our own selection utility because the elements in the
		// synchronize view can't adapt to IResource because we don't want the usual object
		// contribution/ on them.
		List selectedResources = getSelectedResources();
		List selectedNonResources = getSelectedNonResources();
		if (selectedResources.size() > 0 && selectedNonResources.isEmpty()) {
			boolean projSelected = selectionIsOfType(IResource.PROJECT);
			boolean fileFoldersSelected = selectionIsOfType(IResource.FILE | IResource.FOLDER);
			if (!projSelected && !fileFoldersSelected)
				return false;
			// selection must be homogeneous
			if (projSelected && fileFoldersSelected)
				return false;
			// must have a common parent
			IContainer firstParent = ((IResource) selectedResources.get(0)).getParent();
			if (firstParent == null)
				return false;
			Iterator resourcesEnum = selectedResources.iterator();
			while (resourcesEnum.hasNext()) {
				IResource currentResource = (IResource) resourcesEnum.next();
				if (!currentResource.getParent().equals(firstParent))
					return false;
				// resource location must exist
				if (currentResource.getLocation() == null)
					return false;
			}
			return true;
		} else if (selectedNonResources.size() > 0 && selectedResources.isEmpty()) {
			return true;
		}
		return false;
	}

	@Override
	protected List<?> getSelectedNonResources() {
		return Arrays.asList(Utils.getNonResources(getStructuredSelection().toArray()));
	}

	@Override
	protected List<? extends IResource> getSelectedResources() {
		// Calling our own selection utility because the elements in the
		// synchronize view can't adapt to IResource because we don't want the usual object
		// contribution/ on them.
		return Arrays.asList(Utils.getResources(getStructuredSelection().toArray()));
	}

	public void dispose() {
		fClipboard.dispose();
	}
}
