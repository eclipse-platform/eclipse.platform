/*******************************************************************************
 * Copyright (c) 2008, 2012 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.actions;

import java.util.*;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.*;
import org.eclipse.jface.action.ContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.*;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.AbstractHistoryCategory;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.team.ui.history.HistoryPage;
import org.eclipse.ui.*;
import org.eclipse.ui.dialogs.EditorSelectionDialog;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.statushandlers.*;

import com.ibm.icu.text.Collator;

/**
 * A menu for opening file revisions in the workbench.
 * <p>
 * An <code>OpenWithMenu</code> is used to populate a menu with "Open With"
 * actions. One action is added for each editor which is applicable to the
 * selected file. If the user selects one of these items, the corresponding
 * editor is opened on the file.
 * </p>
 */
public class OpenWithMenu extends ContributionItem {

	private IStructuredSelection selection;

	private HistoryPage page;

	private IEditorRegistry registry = PlatformUI.getWorkbench()
			.getEditorRegistry();

	private static Hashtable imageCache = new Hashtable(11);

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
	private static final Comparator comparer = new Comparator() {
		private Collator collator = Collator.getInstance();

		@Override
		public int compare(Object arg0, Object arg1) {
			String s1 = ((IEditorDescriptor) arg0).getLabel();
			String s2 = ((IEditorDescriptor) arg1).getLabel();
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
		Image image = (Image) imageCache.get(imageDesc);
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
			imageDesc = registry
					.getImageDescriptor(getFileRevision().getName());
			// TODO: is this case valid, and if so, what are the implications
			// for content-type editor bindings?
		} else {
			imageDesc = editorDesc.getImageDescriptor();
		}
		if (imageDesc == null) {
			if (editorDesc.getId().equals(
					IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID)) {
				imageDesc = registry
						.getSystemExternalEditorImageDescriptor(getFileRevision()
								.getName());
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
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						openEditor(descriptor, false);
					}
					break;
				}
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
		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					EditorSelectionDialog dialog = new EditorSelectionDialog(
							menu.getShell());
					dialog
							.setMessage(NLS
									.bind(
											TeamUIMessages.LocalHistoryPage_OpenWithMenu_OtherDialogDescription,
											fileResource.getName()));
					if (dialog.open() == Window.OK) {
						IEditorDescriptor editor = dialog.getSelectedEditor();
						if (editor != null) {
							openEditor(editor, editor.isOpenExternal());
						}
					}
					break;
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

		Object[] editors = Utils.getEditors(fileRevision);
		Collections.sort(Arrays.asList(editors), comparer);
		boolean defaultFound = false;

		// Check that we don't add it twice. This is possible
		// if the same editor goes to two mappings.
		ArrayList alreadyMapped = new ArrayList();

		for (int i = 0; i < editors.length; i++) {
			IEditorDescriptor editor = (IEditorDescriptor) editors[i];
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

		// TODO : We might perhaps enable inplace and system external editors menu items
		/*// Add system editor
		IEditorDescriptor descriptor = registry
				.findEditor(IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID);
		final MenuItem systemEditorMenuItem = createMenuItem(menu, descriptor,
				preferredEditor);
		systemEditorMenuItem.setEnabled(false);

		// Add system in-place editor
		descriptor = registry
				.findEditor(IEditorRegistry.SYSTEM_INPLACE_EDITOR_ID);

		final MenuItem inPlaceEditorMenuItem = (descriptor != null) ? createMenuItem(
				menu, descriptor, preferredEditor)
				: null;
		if (inPlaceEditorMenuItem != null)
			inPlaceEditorMenuItem.setEnabled(false);

		Job job = new Job("updateOpenWithMenu") { //$NON-NLS-1$
			protected IStatus run(IProgressMonitor monitor) {
				try {
					final boolean isFile = fileRevision.getStorage(monitor) instanceof IFile;
					Display.getDefault().asyncExec(new Runnable() {
						public void run() {
							if (inPlaceEditorMenuItem != null
									&& !inPlaceEditorMenuItem.isDisposed())
								inPlaceEditorMenuItem.setEnabled(isFile);
							if (!systemEditorMenuItem.isDisposed())
								systemEditorMenuItem.setEnabled(isFile);
						}
					});
					return Status.OK_STATUS;
				} catch (CoreException e) {
					return new Status(IStatus.WARNING, TeamUIPlugin.ID, null, e);
				}
			};
		};
		job.setSystem(true);
		job.schedule();*/

		createDefaultMenuItem(menu, fileRevision, preferredEditor == null);

		// add Other... menu item
		createOtherMenuItem(menu);
	}

	public void createDefaultMenuItem(Menu menu, final IFileRevision revision, boolean markAsSelected) {
		final MenuItem menuItem = new MenuItem(menu, SWT.RADIO);
		menuItem.setSelection(markAsSelected);
		menuItem
				.setText(TeamUIMessages.LocalHistoryPage_OpenWithMenu_DefaultEditorDescription);

		Listener listener = new Listener() {
			@Override
			public void handleEvent(Event event) {
				switch (event.type) {
				case SWT.Selection:
					if (menuItem.getSelection()) {
						openEditor(Utils.getDefaultEditor(revision), false);
					}
					break;
				}
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
			boolean isFile = storage instanceof IFile;

			if (openUsingDescriptor) {
				// discouraged access to open system editors
				((WorkbenchPage) (page.getSite().getPage()))
						.openEditorFromDescriptor(isFile ? new FileEditorInput(
								(IFile) storage)
								: (IEditorInput) FileRevisionEditorInput
										.createEditorInputFor(fileRevision,
												monitor), editorDescriptor,
								true, null);
			} else {
				String editorId = editorDescriptor == null ? IEditorRegistry.SYSTEM_EXTERNAL_EDITOR_ID
						: editorDescriptor.getId();
				page.getSite().getPage().openEditor(
						isFile ? new FileEditorInput((IFile) storage)
								: (IEditorInput) FileRevisionEditorInput
										.createEditorInputFor(fileRevision,
												monitor), editorId, true,
						MATCH_BOTH);
			}
		} catch (PartInitException e) {
			StatusAdapter statusAdapter = new StatusAdapter(e.getStatus());
			statusAdapter.setProperty(IStatusAdapterConstants.TITLE_PROPERTY,
					TeamUIMessages.LocalHistoryPage_OpenEditorError);
			StatusManager.getManager()
					.handle(statusAdapter, StatusManager.SHOW);
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

		for (int i = 0; i < objArray.length; i++) {
			Object tempRevision = objArray[i];
			// If not a revision, don't try opening
			if (tempRevision instanceof AbstractHistoryCategory)
				continue;

			revision = (IFileRevision) tempRevision;
		}
		return revision;
	}

	/*
	 * (non-Javadoc) Returns whether this menu is dynamic.
	 */
	@Override
	public boolean isDynamic() {
		return true;
	}

	public void selectionChanged(IStructuredSelection selection) {
		if (selection instanceof IStructuredSelection) {
			this.selection = selection;
		} else {
			this.selection = StructuredSelection.EMPTY;
		}
	}
}
