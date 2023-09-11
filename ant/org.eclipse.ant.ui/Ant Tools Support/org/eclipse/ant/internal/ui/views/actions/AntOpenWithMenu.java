/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
package org.eclipse.ant.internal.ui.views.actions;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import org.eclipse.ant.internal.ui.AntUIPlugin;
import org.eclipse.ant.internal.ui.AntUtil;
import org.eclipse.ant.internal.ui.IAntUIConstants;
import org.eclipse.ant.internal.ui.model.AntElementNode;
import org.eclipse.core.resources.IFile;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;

/**
 * 
 * Code mostly a copy of the OpenWithMenu which cannot be effectively sub-classed
 */
public class AntOpenWithMenu extends ContributionItem {

	private final IWorkbenchPage fPage;
	private final IEditorRegistry fRegistry = PlatformUI.getWorkbench().getEditorRegistry();
	private static final String SYSTEM_EDITOR_ID = PlatformUI.PLUGIN_ID + ".SystemEditor"; //$NON-NLS-1$

	private static Map<ImageDescriptor, Image> imageCache = new Hashtable<>(11);

	private AntElementNode fNode;

	/**
	 * The id of this action.
	 */
	public static final String ID = IAntUIConstants.PLUGIN_ID + ".AntOpenWithMenu"; //$NON-NLS-1$

	public AntOpenWithMenu(IWorkbenchPage page) {
		super(ID);
		this.fPage = page;
	}

	@Override
	public void dispose() {
		super.dispose();
		for (Image image : imageCache.values()) {
			image.dispose();
		}
		imageCache.clear();
	}

	/**
	 * Returns an image to show for the corresponding editor descriptor.
	 * 
	 * @param editorDesc
	 *            the editor descriptor, or <code>null</code> for the system editor
	 * @return the image or <code>null</code>
	 */
	private Image getImage(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = getImageDescriptor(editorDesc);
		if (imageDesc == null) {
			return null;
		}
		Image image = imageCache.get(imageDesc);
		if (image == null) {
			image = imageDesc.createImage();
			imageCache.put(imageDesc, image);
		}
		return image;
	}

	/**
	 * Returns the image descriptor for the given editor descriptor, or <code>null</code> if it has no image.
	 */
	private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = null;
		if (editorDesc == null) {
			imageDesc = fRegistry.getImageDescriptor(fNode.getIFile().getName());
		} else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null && editorDesc != null) {
			if (editorDesc.getId().equals(SYSTEM_EDITOR_ID)) {
				imageDesc = getSystemEditorImageDescriptor(fNode.getIFile().getFileExtension());
			}
		}
		return imageDesc;
	}

	/**
	 * Return the image descriptor of the system editor that is registered with the OS to edit files of this type. <code>null</code> if none can be
	 * found.
	 */
	private ImageDescriptor getSystemEditorImageDescriptor(String extension) {
		Program externalProgram = null;
		if (extension != null) {
			externalProgram = Program.findProgram(extension);
		}
		if (externalProgram == null) {
			return null;
		}
		return new EditorImageDescriptor(externalProgram);
	}

	/**
	 * Creates the menu item for the editor descriptor.
	 * 
	 * @param menu
	 *            the menu to add the item to
	 * @param descriptor
	 *            the editor descriptor, or null for the system editor
	 * @param preferredEditor
	 *            the descriptor of the preferred editor, or <code>null</code>
	 */
	private void createMenuItem(Menu menu, final IEditorDescriptor descriptor, final IEditorDescriptor preferredEditor) {
		// XXX: Would be better to use bold here, but SWT does not support it.
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		boolean isPreferred = preferredEditor != null && descriptor.getId().equals(preferredEditor.getId());
		menuItem.setSelection(isPreferred);
		menuItem.setText(descriptor.getLabel());
		Image image = getImage(descriptor);
		if (image != null) {
			menuItem.setImage(image);
		}
		Listener listener = event -> {
			switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						openEditor(descriptor);
					}
					break;
				default:
					break;
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	@Override
	public void fill(Menu menu, int index) {
		IFile fileResource = fNode.getIFile();
		if (fileResource == null) {
			return;
		}

		IEditorDescriptor defaultEditor = fRegistry.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID); // should not be null
		IEditorDescriptor preferredEditor = IDE.getDefaultEditor(fileResource); // may be null

		IEditorDescriptor[] editors = fRegistry.getEditors(fileResource.getName());
		Arrays.sort(editors, (o1, o2) -> {
			String s1 = o1.getLabel();
			String s2 = o2.getLabel();
			// Return true if elementTwo is 'greater than' elementOne
			return s1.compareToIgnoreCase(s2);
		});
		IEditorDescriptor antEditor = fRegistry.findEditor("org.eclipse.ant.internal.ui.editor.AntEditor"); //$NON-NLS-1$

		boolean defaultFound = false;
		boolean antFound = false;
		List<String> alreadyAddedEditors = new ArrayList<>(editors.length);
		for (IEditorDescriptor editor : editors) {
			if (alreadyAddedEditors.contains(editor.getId())) {
				continue;
			}
			createMenuItem(menu, editor, preferredEditor);
			if (defaultEditor != null && editor.getId().equals(defaultEditor.getId())) {
				defaultFound = true;
			}
			if (antEditor != null && editor.getId().equals(antEditor.getId())) {
				antFound = true;
			}
			alreadyAddedEditors.add(editor.getId());

		}

		// Only add a separator if there is something to separate
		if (editors.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		// Add ant editor.
		if (!antFound && antEditor != null) {
			createMenuItem(menu, antEditor, preferredEditor);
		}

		// Add default editor.
		if (!defaultFound && defaultEditor != null) {
			createMenuItem(menu, defaultEditor, preferredEditor);
		}

		// Add system editor.
		IEditorDescriptor descriptor = fRegistry.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		createMenuItem(menu, descriptor, preferredEditor);
		createDefaultMenuItem(menu, fileResource);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	/**
	 * Opens the given editor on the selected file.
	 * 
	 * @param editorDescriptor
	 *            the editor descriptor, or <code>null</code> for the system editor
	 */
	private void openEditor(IEditorDescriptor editorDescriptor) {
		AntUtil.openInEditor(fPage, editorDescriptor, fNode);
	}

	/**
	 * Creates the menu item for the default editor
	 * 
	 * @param menu
	 *            the menu to add the item to
	 * @param file
	 *            the file being edited
	 * @param registry
	 *            the editor registry
	 */
	private void createDefaultMenuItem(Menu menu, final IFile fileResource) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setSelection(IDE.getDefaultEditor(fileResource) == null);
		menuItem.setText(AntViewActionMessages.AntViewOpenWithMenu_Default_Editor_4);

		Listener listener = event -> {
			switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						IDE.setDefaultEditor(fileResource, null);
						try {
							IDE.openEditor(fPage, fileResource, true);
						}
						catch (PartInitException e) {
							AntUIPlugin.log(MessageFormat.format(AntViewActionMessages.AntViewOpenWithMenu_Editor_failed, new Object[] {
									fileResource.getLocation().toOSString() }), e);
						}
					}
					break;
				default:
					break;
			}
		};

		menuItem.addListener(SWT.Selection, listener);
	}

	public void setNode(AntElementNode node) {
		fNode = node;
	}
}
