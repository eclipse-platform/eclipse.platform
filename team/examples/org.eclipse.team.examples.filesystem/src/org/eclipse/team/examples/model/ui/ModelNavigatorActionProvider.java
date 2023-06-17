/*******************************************************************************
 * Copyright (c) 2006, 2007 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.examples.model.ui;

import java.io.ByteArrayInputStream;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.team.examples.model.ModelContainer;
import org.eclipse.team.examples.model.ModelObject;
import org.eclipse.team.examples.model.ModelObjectDefinitionFile;
import org.eclipse.ui.navigator.CommonActionProvider;
import org.eclipse.ui.navigator.ICommonActionExtensionSite;
import org.eclipse.ui.navigator.SaveablesProvider;

/**
 * Model action provider for use with the Common Navigator framework. The
 * purpose of this example is to illustrate logical model integration support in
 * Eclipse and, more specifically, Team. It should not be taken as an
 * illustration of other features (e.g. UI responsiveness, etc).
 */
public class ModelNavigatorActionProvider extends CommonActionProvider {

	private Action newModAction;
	private Action newFolderAction;
	private Action newMoeAction;
	private Action deleteAction;
	private Action makeDirty;

	public ModelNavigatorActionProvider() {
		super();
	}

	@Override
	public void init(ICommonActionExtensionSite aSite) {
		super.init(aSite);
		createActions();
	}

	private void createActions() {
		deleteAction = new Action("Delete") {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)getContext().getSelection();
				try {
					for (Iterator iter = selection.iterator(); iter.hasNext();) {
						Object element = iter.next();
						if (element instanceof ModelObject) {
							ModelObject mo = (ModelObject) element;
							mo.delete();
						}
					}
				} catch (CoreException e) {
					ErrorDialog.openError(getShell(), null, null, e.getStatus());
				}
			}
		};
		newFolderAction = new Action("Create Folder") {
			@Override
			public void run() {
				IContainer container = getSelectedContainer();
				if (container != null) {
					String name = promptForName();
					if (name == null)
						return;
					IFolder folder = container.getFolder(IPath.fromOSString(name));
					try {
						folder.create(false, true, null);
					} catch (CoreException e) {
						ErrorDialog.openError(getShell(), null, null, e.getStatus());
					}
				}
			}

			private String promptForName() {
				InputDialog dialog = new InputDialog(getShell(), "Enter Name", "Enter the name of the new folder", "New Folder", null);
				int result = dialog.open();
				if (result == Window.OK) {
					return dialog.getValue();
				}
				return null;
			}
		};
		newModAction = new Action("Create MOD File") {
			@Override
			public void run() {
				IContainer container = getSelectedContainer();
				if (container != null) {
					String name = promptForName();
					if (name == null)
						return;
					if (!name.endsWith(".mod"))
						name += ".mod";
					IFile file = container.getFile(IPath.fromOSString(name));
					try {
						file.create(new ByteArrayInputStream("".getBytes()), false, null);
					} catch (CoreException e) {
						ErrorDialog.openError(getShell(), null, null, e.getStatus());
					}
				}
			}

			private String promptForName() {
				InputDialog dialog = new InputDialog(getShell(), "Enter Name", "Enter the name of the new model object", "New Object", null);
				int result = dialog.open();
				if (result == Window.OK) {
					return dialog.getValue();
				}
				return null;
			}
		};
		newMoeAction = new Action("Create MOE File") {
			@Override
			public void run() {
				ModelObjectDefinitionFile modFile = getSelectedModFile();
				if (modFile != null) {
					String path = promptForPath((ModelContainer)modFile.getParent());
					if (path == null)
						return;
					if (!path.endsWith(".moe"))
						path += ".moe";
					ModelContainer parent = (ModelContainer)modFile.getParent();
					IFile file = ((IContainer)parent.getResource()).getFile(IPath.fromOSString(path));
					try {
						file.create(new ByteArrayInputStream("".getBytes()), false, null);
						modFile.addMoe(file);
					} catch (CoreException e) {
						ErrorDialog.openError(getShell(), null, null, e.getStatus());
					}
				}
			}

			private String promptForPath(ModelContainer parent) {
				InputDialog dialog = new InputDialog(getShell(), "Enter Path", "Enter the path of the new model element relative to " + parent.getPath(), "New Element", null);
				int result = dialog.open();
				if (result == Window.OK) {
					return dialog.getValue();
				}
				return null;
			}
		};
		makeDirty = new Action("Make Dirty") {
			@Override
			public void run() {
				IStructuredSelection selection = (IStructuredSelection)getContext().getSelection();
				for (Iterator iter = selection.iterator(); iter.hasNext();) {
					Object element = iter.next();
					if (element instanceof ModelObjectDefinitionFile) {
						ModelObjectDefinitionFile mo = (ModelObjectDefinitionFile) element;
						ModelSaveablesProvider provider = getSaveablesProvider();
						provider.makeDirty(mo);
					}
				}
			}

			private ModelSaveablesProvider getSaveablesProvider() {
				ITreeContentProvider provider = getActionSite().getContentService().getContentExtensionById("org.eclipse.team.examples.model.navigator").getContentProvider();
				return (ModelSaveablesProvider)Adapters.adapt(provider, SaveablesProvider.class);
			}
		};
	}

	protected Shell getShell() {
		return getActionSite().getViewSite().getShell();
	}

	@Override
	public void fillContextMenu(IMenuManager menu) {
		super.fillContextMenu(menu);
		menu.add(deleteAction);
		IContainer container = getSelectedContainer();
		if (container != null) {
			menu.add(newFolderAction);
			menu.add(newModAction);
		}
		ModelObjectDefinitionFile modFile = getSelectedModFile();
		if (modFile != null) {
			menu.add(newMoeAction);
			menu.add(makeDirty);
		}
	}

	IContainer getSelectedContainer() {
		IStructuredSelection selection = (IStructuredSelection)getContext().getSelection();
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			if (o instanceof ModelContainer) {
				ModelContainer mc = (ModelContainer) o;
				return (IContainer)mc.getResource();
			}
		}
		return null;
	}

	ModelObjectDefinitionFile getSelectedModFile() {
		IStructuredSelection selection = (IStructuredSelection)getContext().getSelection();
		if (selection.size() == 1) {
			Object o = selection.getFirstElement();
			if (o instanceof ModelObjectDefinitionFile) {
				return (ModelObjectDefinitionFile) o;
			}
		}
		return null;
	}

}
