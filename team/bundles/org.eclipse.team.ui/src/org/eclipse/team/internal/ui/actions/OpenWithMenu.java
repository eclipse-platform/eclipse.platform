/*******************************************************************************
 * Copyright (c) 2008, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.actions;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.IStatusAdapterConstants;
import org.eclipse.ui.statushandlers.StatusAdapter;
import org.eclipse.ui.statushandlers.StatusManager;

/**
 * A menu for opening file revisions in the workbench.
 * <p>
 * An <code>OpenWithMenu</code> is used to populate a menu with "Open With"
 * actions. One action is added for each editor which is applicable to the
 * selected file. If the user selects one of these items, the corresponding
 * editor is opened on the file.
 * </p>
 */
@SuppressWarnings("restriction")
public class OpenWithMenu extends ContributionItem {
	private IStructuredSelection selection;

	private final HistoryPage page;

	private final IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();

	private static Hashtable<ImageDescriptor, Image> imageCache = new Hashtable<>(11);

	/**
	 * The id of this action.
	 */
	public static final String ID = PlatformUI.PLUGIN_ID + ".OpenWithMenu";//$NON-NLS-1$

	/**
	 * Match both the input and id, so that different types of editor can be
	 * opened on the same input.
	 */
	private static final int MATCH_BOTH = IWorkbenchPage.MATCH_INPUT
			| IWorkbenchPage.MATCH_ID;

	/*
	 * Compares the labels from two IEditorDescriptor objects
	 */
	private static final Comparator<IEditorDescriptor> comparer = new Comparator<>() {
		private final Collator collator = Collator.getInstance();

		@Override
		public int compare(IEditorDescriptor arg0, IEditorDescriptor arg1) {
			String s1 = arg0.getLabel();
			String s2 = arg1.getLabel();
			return collator.compare(s1, s2);
		}
	};

	/**
	 * Constructs a new instance of <code>OpenWithMenu</code>.
	 *
	 * @param page
	 *            the page where the editor is opened if an item within the menu
	 *            is selected
	 */
	public OpenWithMenu(HistoryPage page) {
		super(ID);
		this.page = page;
	}

	/**
	 * Returns an image to show for the corresponding editor descriptor.
	 *
	 * @param editorDesc
	 *            the editor descriptor, or null for the system editor
	 * @return the image or null
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
	 * Returns the image descriptor for the given editor descriptor, or null if
	 * it has no image.
	 */
	private ImageDescriptor getImageDescriptor(IEditorDescriptor editorDesc) {
		ImageDescriptor imageDesc = null;
		if (editorDesc == null) {
			imageDesc = registry.getImageDescriptor(getFileRevision().getName());
			// TODO: is this case valid, and if so, what are the implications
			// for content-type editor bindings?
		} else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null) {
			if (editorDesc.getId().equals(
					IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID)) {
				imageDesc = registry.getSystemExternalEditorImageDescriptor(getFileRevision().getName());
			}
		}
		return imageDesc;
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
	private MenuItem createMenuItem(Menu menu,
			final IEditorDescriptor descriptor,
			final IEditorDescriptor preferredEditor) {
		// XXX: Would be better to use bold here, but SWT does not support it.
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		boolean isPreferred = preferredEditor != null
				&& descriptor.getId().equals(preferredEditor.getId());
		menuItem.setSelection(isPreferred);
		menuItem.setText(descriptor.getLabel());
		Image image = getImage(descriptor);
		if (image != null) {
			menuItem.setImage(image);
		}
		Listener listener = event -> {
			if (event.type == SWT.Selection && menuItem.getSelection()) {
				openEditor(descriptor, false);
			}
		};
		menuItem.addListener(SWT.Selection, listener);
		return menuItem;
	}

	/**
	 * Creates the Other... menu item
	 *
	 * @param menu
	 *            the menu to add the item to
	 */
	private void createOtherMenuItem(final Menu menu) {
		final IFileRevision fileResource = getFileRevision();
		if (fileResource == null) {
			return;
		}
		new MenuItem(menu, SWT.SEPARATOR);
		final MenuItem menuItem = new MenuItem(menu, SWT.PUSH);
		menuItem.setText(TeamUIMessages.LocalHistoryPage_OpenWithMenu_Other);
		Listener listener = event -> {
			if (event.type == SWT.Selection) {
				EditorSelectionDialog dialog = new EditorSelectionDialog(menu.getShell());
				dialog.setMessage(NLS.bind(
						TeamUIMessages.LocalHistoryPage_OpenWithMenu_OtherDialogDescription,
						fileResource.getName()));
				if (dialog.open() == Window.OK) {
					IEditorDescriptor editor = dialog.getSelectedEditor();
					if (editor != null) {
						openEditor(editor, editor.isOpenExternal());
					}
				}
			}
		};
		menuItem.addListener(SWT.Selection, listener);
	}

	@Override
	public void fill(Menu menu, int index) {
		final IFileRevision fileRevision = getFileRevision();
		if (fileRevision == null) {
			return;
		}

		IEditorDescriptor defaultTextEditor = registry
				.findEditor("org.eclipse.ui.DefaultTextEditor"); //$NON-NLS-1$
		IEditorDescriptor preferredEditor = Utils
				.getDefaultEditor(fileRevision);

		IEditorDescriptor[] editors = Utils.getEditors(fileRevision);
		Collections.sort(Arrays.asList(editors), comparer);
		boolean defaultFound = false;

		// Check that we don't add it twice. This is possible
		// if the same editor goes to two mappings.
		ArrayList<IEditorDescriptor> alreadyMapped = new ArrayList<>();

		for (IEditorDescriptor editor : editors) {
			if (!alreadyMapped.contains(editor)) {
				createMenuItem(menu, editor, preferredEditor);
				if (defaultTextEditor != null
						&& editor.getId().equals(defaultTextEditor.getId())) {
					defaultFound = true;
				}
				alreadyMapped.add(editor);
			}
		}

		// Only add a separator if there is something to separate
		if (editors.length > 0) {
			new MenuItem(menu, SWT.SEPARATOR);
		}

		// Add default editor. Check it if it is saved as the preference.
		if (!defaultFound && defaultTextEditor != null) {
			createMenuItem(menu, defaultTextEditor, preferredEditor);
		}

		createDefaultMenuItem(menu, fileRevision, preferredEditor == null);

		// add Other... menu item
		createOtherMenuItem(menu);
	}

	public void createDefaultMenuItem(Menu menu, final IFileRevision revision, boolean markAsSelected) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setSelection(markAsSelected);
		menuItem.setText(TeamUIMessages.LocalHistoryPage_OpenWithMenu_DefaultEditorDescription);

		Listener listener = event -> {
			if (event.type == SWT.Selection && menuItem.getSelection()) {
				openEditor(Utils.getDefaultEditor(revision), false);
			}
		};

		menuItem.addListener(SWT.Selection, listener);
	}

	/**
	 * Opens the given editor on the selected file revision.
	 */
	protected void openEditor(IEditorDescriptor editorDescriptor,
			boolean openUsingDescriptor) {
		IFileRevision fileRevision = getFileRevision();
		if (fileRevision == null) {
			return;
		}
		try {
			IProgressMonitor monitor = new NullProgressMonitor();
			IStorage storage = fileRevision.getStorage(monitor);
			IEditorInput editorInput = storage instanceof IFile ?
					new FileEditorInput((IFile) storage) :
					FileRevisionEditorInput.createEditorInputFor(fileRevision, monitor);
			if (openUsingDescriptor) {
				// Discouraged access to open system editors.
				WorkbenchPage workbenchPage = (WorkbenchPage) (page.getSite().getPage());
				workbenchPage.openEditorFromDescriptor(editorInput, editorDescriptor, true, null);
			} else {
				String editorId = editorDescriptor == null ?
						IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID : editorDescriptor.getId();
				page.getSite().getPage().openEditor(editorInput, editorId, true, MATCH_BOTH);
			}
		} catch (PartInitException e) {
			StatusAdapter statusAdapter = new StatusAdapter(e.getStatus());
			statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
					TeamUIMessages.LocalHistoryPage_OpenEditorError);
			StatusManager.getManager().handle(statusAdapter, StatusManager.SHOW);
		} catch (CoreException e) {
			StatusAdapter statusAdapter = new StatusAdapter(e.getStatus());
			statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
					TeamUIMessages.LocalHistoryPage_OpenEditorError);
			StatusManager.getManager().handle(statusAdapter, StatusManager.LOG);
		}
	}

	private IFileRevision getFileRevision() {
		IStructuredSelection structSel = selection;

		IFileRevision revision = null;

		if (structSel == null)
			return null;

		Object[] objArray = structSel.toArray();

		for (Object tempRevision : objArray) {
			// If not a revision, don't try opening
			if (tempRevision instanceof AbstractHistoryCategory)
				continue;

			revision = (IFileRevision) tempRevision;
		}
		return revision;
	}

	@Override
	public boolean isDynamic() {
		return true;
	}

	public void selectionChanged(IStructuredSelection selection) {
		this.selection = selection;
	}
}
