/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
package org.eclipse.help.ui.internal.views;

import java.util.Observable;
import java.util.Observer;

import org.eclipse.help.IHelpResource;
import org.eclipse.help.internal.base.BaseHelpSystem;
import org.eclipse.help.internal.base.BookmarkManager;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class BookmarksPart extends HyperlinkTreePart implements Observer {
	private Action deleteAction;

	class BookmarksProvider implements ITreeContentProvider {

		@Override
		public Object[] getChildren(Object parentElement) {
			if (parentElement == BookmarksPart.this)
				return new Object[] { BaseHelpSystem.getBookmarkManager() };
			if (parentElement instanceof BookmarkManager)
				return ((BookmarkManager) parentElement).getBookmarks();
			return new Object[0];
		}

		@Override
		public Object getParent(Object element) {
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return getChildren(element).length > 0;
		}

		@Override
		public Object[] getElements(Object inputElement) {
			return getChildren(inputElement);
		}

		@Override
		public void dispose() {
		}

		@Override
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput) {
		}
	}

	class BookmarksLabelProvider extends LabelProvider {

		@Override
		public String getText(Object obj) {
			if (obj instanceof BookmarkManager)
				return Messages.BookmarksPart_savedTopics;
			if (obj instanceof IHelpResource)
				return ((IHelpResource) obj).getLabel();
			return super.getText(obj);
		}

		@Override
		public Image getImage(Object obj) {
			if (obj instanceof BookmarkManager)
				return HelpUIResources
						.getImage(IHelpUIConstants.IMAGE_BOOKMARKS);
			if (obj instanceof IHelpResource)
				return HelpUIResources
						.getImage(IHelpUIConstants.IMAGE_BOOKMARK);
			return super.getImage(obj);
		}
	}

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public BookmarksPart(Composite parent, final FormToolkit toolkit,
			IToolBarManager tbm) {
		super(parent, toolkit, tbm);
		BaseHelpSystem.getBookmarkManager().addObserver(this);
	}

	@Override
	public void dispose() {
		BaseHelpSystem.getBookmarkManager().deleteObserver(this);
		super.dispose();
	}

	@Override
	protected void configureTreeViewer() {
		treeViewer.setContentProvider(new BookmarksProvider());
		treeViewer.setLabelProvider(new BookmarksLabelProvider());
		treeViewer.setAutoExpandLevel(TreeViewer.ALL_LEVELS);
		deleteAction = new Action("") { //$NON-NLS-1$

			@Override
			public void run() {
				Object obj = treeViewer.getStructuredSelection().getFirstElement();
				if (obj instanceof BookmarkManager.Bookmark) {
					BookmarkManager.Bookmark b = (BookmarkManager.Bookmark)obj;
					BaseHelpSystem.getBookmarkManager().removeBookmark(b);
				}
			}
		};
		deleteAction.setText(Messages.BookmarksPart_delete);
		deleteAction.setEnabled(false);
	}

	@Override
	protected void handleSelectionChanged(IStructuredSelection sel) {
		Object obj = sel.getFirstElement();
		deleteAction.setEnabled(obj!=null && obj instanceof BookmarkManager.Bookmark);
		super.handleSelectionChanged(sel);
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		boolean value = super.fillContextMenu(manager);
		IStructuredSelection selection = treeViewer.getStructuredSelection();
		boolean canDeleteAll=false;
		int count = BaseHelpSystem.getBookmarkManager().getBookmarks().length;
		canDeleteAll = count>0;

		if (canDelete(selection)) {
			if (value)
				manager.add(new Separator());
			manager.add(deleteAction);
			value=true;
		}
		if (canDeleteAll) {
			Action action = new Action("") { //$NON-NLS-1$

				@Override
				public void run() {
					BusyIndicator.showWhile(getControl().getDisplay(),
							() -> BaseHelpSystem.getBookmarkManager().removeAllBookmarks());
				}
			};
			action.setText(Messages.BookmarksPart_deleteAll);
			manager.add(action);
			value=true;
		}
		if (value==true)
			manager.add(new Separator());
		return value;
	}

	private boolean canDelete(IStructuredSelection ssel) {
		Object obj = ssel.getFirstElement();
		return obj instanceof BookmarkManager.Bookmark;
	}

	@Override
	protected void doOpen(Object obj) {
		if (obj instanceof BookmarkManager) {
			treeViewer.setExpandedState(obj, !treeViewer.getExpandedState(obj));
		} else if (obj instanceof IHelpResource) {
			IHelpResource res = (IHelpResource) obj;
			if (res.getHref() != null)
				parent.showURL(res.getHref());
		}
	}

	@Override
	public void update(final Observable o, final Object arg) {
		treeViewer.getControl().getDisplay().asyncExec(() -> asyncUpdate(o, arg));
	}

	private void asyncUpdate(Observable o, Object arg) {
		BookmarkManager.BookmarkEvent event = (BookmarkManager.BookmarkEvent) arg;
		switch (event.getType()) {
		case BookmarkManager.ADD:
			treeViewer.add(BaseHelpSystem.getBookmarkManager(), event
					.getBookmark());
			break;
		case BookmarkManager.REMOVE:
			treeViewer.remove(event.getBookmark());
			break;
		case BookmarkManager.REMOVE_ALL:
		case BookmarkManager.WORLD_CHANGED:
			treeViewer.refresh();
			break;
		}
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.DELETE.getId()))
			return deleteAction;
		return super.getGlobalAction(id);
	}

	@Override
	protected boolean canAddBookmarks() {
		return false;
	}

	@Override
	public void toggleRoleFilter() {
	}

	@Override
	public void refilter() {
	}

	@Override
	public void saveState(IMemento memento) {
	}
}
