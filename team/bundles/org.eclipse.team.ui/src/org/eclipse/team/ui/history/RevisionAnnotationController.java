/*******************************************************************************
 * Copyright (c) 2006, 2017 IBM Corporation and others.
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
package org.eclipse.team.ui.history;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IStorage;
import org.eclipse.core.runtime.Adapters;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.text.revisions.IRevisionRulerColumn;
import org.eclipse.jface.text.revisions.IRevisionRulerColumnExtension;
import org.eclipse.jface.text.revisions.Revision;
import org.eclipse.jface.text.revisions.RevisionInformation;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.team.core.history.IFileRevision;
import org.eclipse.team.core.variants.IResourceVariant;
import org.eclipse.team.internal.ui.TeamUIMessages;
import org.eclipse.team.internal.ui.Utils;
import org.eclipse.team.internal.ui.history.FileRevisionEditorInput;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.editors.text.EditorsUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.texteditor.AbstractDecoratedTextEditor;
import org.eclipse.ui.texteditor.ITextEditor;

/**
 * Helper class that coordinates the selection behavior between an editor
 * revision ruler and a history list such as one shown in the history view. In
 * other words, the selection in the history list will be reflected in the
 * revision rule and vice versa.
 *
 * @see Revision
 * @see RevisionInformation
 * @since 3.3
 */
public abstract class RevisionAnnotationController {

	private ISelectionProvider fRulerSelectionProvider;
	private ISelectionProvider fHistoryListSelectionProvider;
	private ISelectionChangedListener rulerListener = event -> {
		ISelection selection= event.getSelection();
		Revision selected= null;
		if (selection instanceof IStructuredSelection)
			selected= (Revision) ((IStructuredSelection) selection).getFirstElement();

		if (selected == null)
			return;

		revisionSelected(selected);
	};
	private ISelectionChangedListener historyListListener = event -> {
		ISelection selection= event.getSelection();
		if (selection instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) selection;
			if (ss.size() == 1) {
				Object first= ss.getFirstElement();
				if (first != null)
					historyEntrySelected(first);
			}
		}
	};

	/**
	 * Open a text editor that supports the use of a revision ruler on the given
	 * file. If an appropriate editor is already open, it is returned. Otherwise
	 * a new editor is opened.
	 *
	 * @param page
	 *            the page in which the editor is to be opened
	 * @param file
	 *            the file to be edited
	 * @return the open editor on the file
	 * @throws PartInitException swallowed
	 */
	public static AbstractDecoratedTextEditor openEditor(IWorkbenchPage page, IFile file) throws PartInitException {
		if (file == null)
			return null;
		FileEditorInput input = new FileEditorInput(file);
		IEditorPart[] openEditors = findOpenEditorsForFile(page, input);
		if (openEditors.length > 0) {
			AbstractDecoratedTextEditor te= findTextEditor(page, openEditors, input);
			if (te != null) {
				return te;
			}
		}

		// No existing editor references found, try to open a new editor for the file
		try {
			IEditorDescriptor descrptr = IDE.getEditorDescriptor(file, true, true);
			// Try to open the associated editor only if its an internal editor
			// Also, if a non-text editor is already open, there is no need to try and open
			// an editor since the open will find the non-text editor
			if (descrptr.isInternal() && openEditors.length == 0){
				IEditorPart part = page.openEditor(input, IDE.getEditorDescriptor(file, true, true).getId(), true, IWorkbenchPage.MATCH_INPUT);
				AbstractDecoratedTextEditor te = findTextEditorPart(page, part, input);
				if (te != null)
					return te;

				//editor opened is not a text editor - close it
				page.closeEditor(part, false);
			}
			//open file in default text editor
			IEditorPart part = page.openEditor(input, EditorsUI.DEFAULT_TEXT_EDITOR_ID, true, IWorkbenchPage.MATCH_INPUT | IWorkbenchPage.MATCH_ID);
			AbstractDecoratedTextEditor te = findTextEditorPart(page, part, input);
			if (te != null)
				return te;

		} catch (PartInitException e) {
		}

		return null;
	}

	/**
	 * Open a text editor that supports the use of a revision ruler on the given
	 * file. If an appropriate editor is already open, it is returned. Otherwise
	 * a new editor is opened.
	 *
	 * @param page
	 *            the page in which the editor is to be opened
	 * @param fileRevision
	 *            the file revision object
	 * @param storage
	 *            the storage that provides access to the contents of the file revision
	 * @return the open editor on the file revision
	 * @throws PartInitException if an error occurs
	 */
	public static AbstractDecoratedTextEditor openEditor(IWorkbenchPage page,
			Object fileRevision, IStorage storage) throws PartInitException {
		String id = getEditorId(storage);
		ITextEditor editor = getEditor(id, fileRevision, storage);
		if (editor instanceof AbstractDecoratedTextEditor)
			return (AbstractDecoratedTextEditor) editor;
		return null;
	}


	private static ITextEditor getEditor(String id, Object fileRevision, IStorage storage) throws PartInitException {
		final IWorkbench workbench= PlatformUI.getWorkbench();
		final IWorkbenchWindow window= workbench.getActiveWorkbenchWindow();
		IWorkbenchPage page= window.getActivePage();
		IEditorPart part = page.openEditor(new FileRevisionEditorInput(fileRevision, storage), id);
		if (part instanceof ITextEditor) {
			return (ITextEditor)part;
		} else {
			// We asked for a text editor but didn't get one
			// so open a vanilla text editor
			page.closeEditor(part, false);
			part = page.openEditor(new FileRevisionEditorInput(fileRevision, storage), EditorsUI.DEFAULT_TEXT_EDITOR_ID);
			if (part instanceof ITextEditor) {
				return (ITextEditor)part;
			} else {
				// There is something really wrong so just bail
				throw new PartInitException(TeamUIMessages.RevisionAnnotationController_0);
			}
		}
	}

	private static String getEditorId(IStorage storage) {
		String id;
		IEditorRegistry registry = PlatformUI.getWorkbench().getEditorRegistry();
		IEditorDescriptor descriptor = registry.getDefaultEditor(storage.getName());
		if (descriptor == null || !descriptor.isInternal()) {
			id = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
		} else {
			try {
				if (Utils.isTextEditor(descriptor)) {
					id = descriptor.getId();
				} else {
					id = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
				}
			} catch (CoreException e) {
				id = EditorsUI.DEFAULT_TEXT_EDITOR_ID;
			}
		}
		return id;
	}



	private static AbstractDecoratedTextEditor findOpenTextEditorForFile(IWorkbenchPage page, IFile file) {
		if (file == null)
			return null;
		FileEditorInput input = new FileEditorInput(file);
		IEditorPart[] editors = findOpenEditorsForFile(page, input);
		return findTextEditor(page, editors, input);
	}

	private static AbstractDecoratedTextEditor findTextEditor(IWorkbenchPage page, IEditorPart[] editors, IEditorInput input) {
		for (IEditorPart editor : editors) {
			AbstractDecoratedTextEditor te = findTextEditorPart(page, editor, input);
			if (te != null)
				return te;
		}
		return null;
	}

	private static AbstractDecoratedTextEditor findTextEditorPart(IWorkbenchPage page, IEditorPart editor, IEditorInput input) {
		if (editor instanceof AbstractDecoratedTextEditor)
			return (AbstractDecoratedTextEditor) editor;
		if (editor instanceof MultiPageEditorPart) {
			MultiPageEditorPart mpep = (MultiPageEditorPart) editor;
			IEditorPart[] parts = mpep.findEditors(input);
			for (IEditorPart editorPart : parts) {
				if (editorPart instanceof AbstractDecoratedTextEditor) {
					page.activate(mpep);
					mpep.setActiveEditor(editorPart);
					return (AbstractDecoratedTextEditor) editorPart;
				}
			}
		}
		return null;
	}

	private static IEditorPart[] findOpenEditorsForFile(IWorkbenchPage page, FileEditorInput input) {
		final IEditorReference[] references= page.findEditors(input, null, IWorkbenchPage.MATCH_INPUT);
		final List<IEditorPart> editors = new ArrayList<>();
		for (IEditorReference reference : references) {
			IEditorPart editor= reference.getEditor(false);
			editors.add(editor);
		}

		return editors.toArray(new IEditorPart[editors.size()]);
	}

	private static AbstractDecoratedTextEditor findOpenTextEditorFor(IWorkbenchPage page, Object object) {
		if (object == null)
			return null;
		if (object instanceof IFile) {
			IFile file = (IFile) object;
			return findOpenTextEditorForFile(page, file);
		}
		final IWorkbench workbench= PlatformUI.getWorkbench();
		final IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		IEditorReference[] references= window.getActivePage().getEditorReferences();
		for (IEditorReference reference : references) {
			try {
				if (object.equals(reference.getEditorInput())) {
					IEditorPart editor= reference.getEditor(false);
					if (editor instanceof AbstractDecoratedTextEditor)
						return (AbstractDecoratedTextEditor) editor;
				}
			} catch (PartInitException e) {
				// ignore
			}
		}

		return null;
	}

	private static IRevisionRulerColumnExtension findEditorRevisonRulerColumn(IWorkbenchPage page, Object object) {
		ITextEditor editor= findOpenTextEditorFor(page, object);
		if (editor == null)
			return null;

		IRevisionRulerColumn column= editor.getAdapter(IRevisionRulerColumn.class);
		if (column instanceof IRevisionRulerColumnExtension) {
			if (column.getControl() != null && column.getControl().isDisposed())
				return null;
			return (IRevisionRulerColumnExtension) column;
		}

		return null;
	}

	private RevisionAnnotationController(IRevisionRulerColumnExtension revisionRuler, ISelectionProvider historyList) {
		fHistoryListSelectionProvider = historyList;
		if (revisionRuler == null) {
			fRulerSelectionProvider = null;
			return;
		}

		fRulerSelectionProvider= revisionRuler.getRevisionSelectionProvider();
		fRulerSelectionProvider.addSelectionChangedListener(rulerListener);
		fHistoryListSelectionProvider.addSelectionChangedListener(historyListListener);

		((IRevisionRulerColumn)revisionRuler).getControl().addDisposeListener(e -> dispose());
	}

	/**
	 * Create a controller that links an editor on a local file to a history list.
	 * @param page the workbench page
	 * @param file the local file
	 * @param historyList the history list selection provider
	 */
	public RevisionAnnotationController(IWorkbenchPage page, IFile file, ISelectionProvider historyList) {
		this(findEditorRevisonRulerColumn(page, file), historyList);
	}

	/**
	 * Create a controller that links an editor input on a remote file to a history list.
	 * @param page the workbench page
	 * @param editorInput the editor input for the remote file
	 * @param historyList the history list selection provider
	 */
	public RevisionAnnotationController(IWorkbenchPage page, IStorageEditorInput editorInput,
			ISelectionProvider historyList) {
		this(findEditorRevisonRulerColumn(page, editorInput), historyList);
	}

	/**
	 * Dispose of the controller.
	 */
	public void dispose() {
		if (fRulerSelectionProvider != null) {
			fRulerSelectionProvider.removeSelectionChangedListener(rulerListener);
			fHistoryListSelectionProvider.removeSelectionChangedListener(historyListListener);
			rulerListener= null;
			fRulerSelectionProvider= null;
			historyListListener= null;
			fHistoryListSelectionProvider= null;
		}
	}

	/**
	 * Callback from the ruler when a particular revision has been selected by the user.
	 * By default, this method will set the selection of the history list selection
	 * provider that was passed in the constructor using the history entry returned
	 * by {@link #getHistoryEntry(Revision)}. Subclasses may override.
	 * @param selected the selected revision
	 */
	protected void revisionSelected(Revision selected) {
		Object entry= getHistoryEntry(selected);

		if (entry != null) {
			IStructuredSelection selection = new StructuredSelection(entry);
			if (fHistoryListSelectionProvider instanceof Viewer) {
				Viewer v = (Viewer) fHistoryListSelectionProvider;
				v.setSelection(selection, true);
			} else if (fHistoryListSelectionProvider != null) {
				fHistoryListSelectionProvider.setSelection(selection);
			}
		}
	}

	/**
	 * Return the history list entry corresponding to the provided revision.
	 * THis method is called by the {@link #revisionSelected(Revision)} method in
	 * order to determine what the selection of the history list selection provider
	 * should be set to.
	 * @param selected the selected revision.
	 * @return the history list entry that corresponds to the provided revision.
	 */
	protected abstract Object getHistoryEntry(Revision selected);

	/**
	 * Callback that is invoked when the selection in the history list changes.
	 * @param historyEntry the history entry
	 */
	/* package */ void historyEntrySelected(Object historyEntry) {
		String id = getRevisionId(historyEntry);
		if (id != null && fRulerSelectionProvider != null) {
			fRulerSelectionProvider.setSelection(new StructuredSelection(id));
		}
	}

	/**
	 * Return the revision id associated with the given history list entry.
	 * This method is used to determine which revision in the revision ruler should
	 * be highlighted when the history list selection provider fires a selection changed event.
	 * By default, this method tries to adapt the entry to either {@link IFileRevision} or
	 * {@link IResourceVariant} in order to obtain the content identifier. Subclasses may override.
	 *
	 * @param historyEntry the history list entry
	 * @return the id of the entry
	 */
	protected String getRevisionId(Object historyEntry) {
		IFileRevision revision= Adapters.adapt(historyEntry, IFileRevision.class);
		if (revision != null) {
			return revision.getContentIdentifier();
		}
		IResourceVariant variant = Adapters.adapt(historyEntry, IResourceVariant.class);
		if (variant != null)
			return variant.getContentIdentifier();
		return null;
	}

}
