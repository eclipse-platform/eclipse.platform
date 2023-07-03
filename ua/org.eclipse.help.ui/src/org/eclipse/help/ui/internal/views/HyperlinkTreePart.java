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
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.internal.base.scope.ScopeUtils;
import org.eclipse.help.internal.base.scope.WorkingSetScope;
import org.eclipse.help.internal.search.federated.LocalHelpScope;
import org.eclipse.help.internal.workingset.WorkingSet;
import org.eclipse.help.search.ISearchScope;
import org.eclipse.help.ui.internal.HelpUIResources;
import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseTrackAdapter;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.FontMetrics;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.FormToolkit;

public abstract class HyperlinkTreePart extends AbstractFormPart implements
		IHelpPart {

	public class ScopeObserver implements Observer {

		@Override
		public void update(Observable o, Object arg) {
			if (o instanceof ScopeSetManager) {
				refilter();
			}
		}
	}

	private static class ScopeFilter extends ViewerFilter {

		public ScopeFilter(AbstractHelpScope scope) {
			this.scope = scope;
		}

		AbstractHelpScope scope;

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			if (element instanceof IToc) {
				return ScopeUtils.showInTree((IToc)element, scope);
			}
			if (element instanceof ITopic) {
				return ScopeUtils.showInTree((ITopic)element, scope);
			}
			if (element instanceof IIndexEntry) {
				return ScopeUtils.showInTree((IIndexEntry)element, scope);
			}
			if (element instanceof IIndexSee) {
				return ScopeUtils.showInTree((IIndexSee)element, scope);
			}
			return true;
		}

	}

	protected ReusableHelpPart parent;

	private String id;

	//private Composite container;

	protected TreeViewer treeViewer;

	private TreeItem lastItem;

	private ScopeObserver scopeObserver;

	/**
	 * @param parent
	 * @param toolkit
	 * @param style
	 */
	public HyperlinkTreePart(Composite parent, final FormToolkit toolkit,
			IToolBarManager tbm) {
		Cursor handCursor = parent.getDisplay().getSystemCursor(SWT.CURSOR_HAND);
		/*
		container = toolkit.createComposite(parent);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		container.setLayout(layout);
		Composite sep = toolkit.createCompositeSeparator(container);
		GridData gd = new GridData(GridData.HORIZONTAL_ALIGN_FILL);
		gd.heightHint = 1;
		sep.setLayoutData(gd);

		treeViewer = new TreeViewer(container, SWT.H_SCROLL | SWT.V_SCROLL);
		*/
		treeViewer = new TreeViewer(parent, SWT.H_SCROLL | SWT.V_SCROLL);
		treeViewer.getTree().setMenu(parent.getMenu());
		treeViewer.getTree().setForeground(
				toolkit.getHyperlinkGroup().getForeground());
		configureTreeViewer();
		treeViewer.setInput(this);
		treeViewer.getControl().setLayoutData(new GridData(GridData.FILL_BOTH));
		treeViewer.addOpenListener(event -> doOpenSelection((IStructuredSelection) event.getSelection()));
		treeViewer
				.addSelectionChangedListener(event -> handleSelectionChanged(event.getStructuredSelection()));
		treeViewer.getTree().addMouseListener(new MouseAdapter() {
			long lastTime;

			@Override
			public void mouseUp(MouseEvent e) {
				long eventTime = e.time & 0xFFFFFFFFL;
				if (eventTime - lastTime <= e.display.getDoubleClickTime())
					return;
				if (e.button != 1)
					return;
				lastTime = eventTime;
				Point p = new Point(e.x, e.y);
				TreeItem item = treeViewer.getTree().getItem(p);
				if (item != null) {
					Object obj = item.getData();
					if (obj != null) {
						doOpen(obj);
					}
				}
			}
		});

		treeViewer.getTree().addPaintListener(e -> {
			validateLastItem();
			if (lastItem == null)
				return;
			Rectangle bounds = lastItem.getBounds();
			boolean selected = false;
			TreeItem[] items = lastItem.getParent().getSelection();
			for (int i = 0; i < items.length; i++) {
				if (items[i].equals(lastItem)) {
					selected = true;
					break;
				}
			}
			if (selected)
				e.gc.setForeground(e.display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
			else
				e.gc.setForeground(toolkit.getHyperlinkGroup().getActiveForeground());
			FontMetrics fm = e.gc.getFontMetrics();
			int height = fm.getHeight();
			int lineY = bounds.y + height;
			e.gc.drawLine(bounds.x, lineY, bounds.x + bounds.width - 1, lineY);
		});

		treeViewer.getTree().addMouseTrackListener(new MouseTrackAdapter() {

			@Override
			public void mouseExit(MouseEvent e) {
				validateLastItem();
				if (lastItem != null) {
					TreeItem item = lastItem;
					lastItem = null;
					item.setForeground(null);
				}
			}
		});

		treeViewer.getTree().addMouseMoveListener(e -> {
			Point p = new Point(e.x, e.y);
			TreeItem item = treeViewer.getTree().getItem(p);
			validateLastItem();
			if (item != null) {
				if (lastItem != null) {
					if (!lastItem.equals(item)) {
						lastItem.setForeground(null);
						repaintItem(lastItem);
						updateStatus(null);
						lastItem = null;
					} else
						return;
				}
				Object obj = item.getData();
				treeViewer.getTree().setCursor(handCursor);
				IStructuredSelection ssel = treeViewer.getStructuredSelection();
				if (ssel.getFirstElement() == obj)
					item.setForeground(e.display.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
				else
					item.setForeground(toolkit.getHyperlinkGroup().getActiveForeground());
				lastItem = item;
				repaintItem(lastItem);
				if (obj instanceof IHelpResource)
					updateStatus((IHelpResource) obj);
				else
					updateStatus(null);
				return;
			} else if (lastItem != null) {
				lastItem.setForeground(null);
				repaintItem(lastItem);
				lastItem = null;
				updateStatus(null);
			}
			treeViewer.getTree().setCursor(null);
		});
		contributeToToolBar(tbm);
		scopeObserver = new ScopeObserver();
		ScopeState.getInstance().getScopeSetManager().addObserver(scopeObserver);
	}

	@Override
	public void dispose() {
		if (scopeObserver != null) {
			ScopeState.getInstance().getScopeSetManager().deleteObserver(scopeObserver);
		}
		super.dispose();
	}

	private void repaintItem(TreeItem item) {
		Rectangle bounds = item.getBounds();
		item.getParent().redraw(bounds.x, bounds.y, bounds.width,
				bounds.height, false);
	}

	protected void contributeToToolBar(IToolBarManager tbm) {
		Action collapseAllAction = new Action() {

			@Override
			public void run() {
				BusyIndicator.showWhile(getControl().getDisplay(), () -> doCollapseAll());
			}
		};
		collapseAllAction.setImageDescriptor(HelpUIResources
				.getImageDescriptor(IHelpUIConstants.IMAGE_COLLAPSE_ALL));
		collapseAllAction
				.setToolTipText(Messages.AllTopicsPart_collapseAll_tooltip);
		tbm.insertBefore("back", collapseAllAction); //$NON-NLS-1$
		tbm.insertBefore("back", new Separator()); //$NON-NLS-1$
	}

	private void doCollapseAll() {
		Object[] expanded = treeViewer.getExpandedElements();
		treeViewer.collapseAll();
		treeViewer.update(expanded, null);
	}

	@Override
	public Control getControl() {
		return treeViewer.getControl();
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
		refilter();
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		getControl().setVisible(visible);
		if (visible)
			treeViewer.refresh();
	}

	private void doOpenSelection(IStructuredSelection sel) {
		Object obj = sel.getFirstElement();
		if (obj != null) {
			doOpen(obj);
		}
	}

	protected void handleSelectionChanged(IStructuredSelection sel) {
		Object obj = sel.getFirstElement();
		if (lastItem != null && !lastItem.isDisposed()) {
			Object lastObj = lastItem.getData();
			if (lastObj==obj)
				lastItem.setForeground(getControl().getDisplay()
							.getSystemColor(SWT.COLOR_LIST_SELECTION_TEXT));
			else
				lastItem.setForeground(parent.getForm().getToolkit().getHyperlinkGroup()
						.getActiveForeground());
			repaintItem(lastItem);
		}
		if (obj instanceof IHelpResource) {
			IHelpResource res = (IHelpResource) obj;
			updateStatus(res, false);
		} else
			updateStatus(null, false);
	}

	private void updateStatus(IHelpResource res) {
		updateStatus(res, true);
	}

	private void updateStatus(IHelpResource res, boolean defaultToSelection) {
		if (defaultToSelection && res == null) {
			IStructuredSelection ssel = treeViewer.getStructuredSelection();
			Object obj = ssel.getFirstElement();
			if (obj instanceof IHelpResource)
				res = (IHelpResource) obj;
		}
		if (res != null) {
			String label = res.getLabel();
			String href = getHref(res);
			HyperlinkTreePart.this.parent.handleLinkEntered(new HyperlinkEvent(
					treeViewer.getTree(), href, label, SWT.NULL));
		} else {
			HyperlinkTreePart.this.parent.handleLinkExited(null);
		}
	}

	protected String getHref(IHelpResource res) {
		return res.getHref();
	}

	protected abstract void configureTreeViewer();

	protected abstract void doOpen(Object obj);

	protected void postUpdate(final Object obj) {
		treeViewer.getControl().getDisplay().asyncExec(() -> treeViewer.update(obj, null));
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return parent.fillSelectionProviderMenu(treeViewer, manager,
				canAddBookmarks());
	}

	protected abstract boolean canAddBookmarks();

	@Override
	public boolean hasFocusControl(Control focusControl) {
		return treeViewer.getControl().equals(focusControl);
	}

	@Override
	public void setFocus() {
		if (treeViewer != null)
			treeViewer.getTree().setFocus();
	}

	@Override
	public IAction getGlobalAction(String id) {
		if (id.equals(ActionFactory.COPY.getId()))
			return parent.getCopyAction();
		return null;
	}

	@Override
	public void stop() {
	}

	private void validateLastItem() {
		if (lastItem != null && lastItem.isDisposed())
			lastItem = null;
	}

	@Override
	public void refilter() {
		ScopeSetManager manager = ScopeState.getInstance().getScopeSetManager();
		ScopeSet set = manager.getActiveSet();
		EngineDescriptor[] engineDescriptors = parent.getEngineManager().getDescriptors();
		ISearchScope scope = null;
		for (int i = 0; i < engineDescriptors.length; i++) {
			final EngineDescriptor ed = engineDescriptors[i];
			if (ed.getEngineTypeId().equals("org.eclipse.help.ui.localSearch") //$NON-NLS-1$
					&& ed.getEngine() != null) {
				scope = ed.createSearchScope(set.getPreferenceStore());
			}
		}
		WorkingSet workingSet = null;
		LocalHelpScope localScope = (LocalHelpScope) scope;
		workingSet = localScope.getWorkingSet() ;
		treeViewer.resetFilters();
		if (workingSet != null) {
			WorkingSetScope helpScope = new WorkingSetScope(workingSet, set.getName());
			treeViewer.addFilter(new ScopeFilter(helpScope));
		}
		treeViewer.refresh();
	}
}
