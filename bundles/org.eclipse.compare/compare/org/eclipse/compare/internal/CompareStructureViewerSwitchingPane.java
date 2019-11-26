/*******************************************************************************
 * Copyright (c) 2009, 2017 IBM Corporation and others.
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
 *     Stefan Dirix (sdirix@eclipsesource.com) - Bug 473847: Minimum E4 Compatibility of Compare
 *******************************************************************************/
package org.eclipse.compare.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.compare.CompareConfiguration;
import org.eclipse.compare.CompareEditorInput;
import org.eclipse.compare.CompareViewerSwitchingPane;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.MenuAdapter;
import org.eclipse.swt.events.MenuEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class CompareStructureViewerSwitchingPane extends
		CompareViewerSwitchingPane {

	private CompareEditorInput fCompareEditorInput;

	private ViewerDescriptor fSelectedViewerDescriptor;

	private ToolBar toolBar;

	public CompareStructureViewerSwitchingPane(Composite parent, int style,
			boolean visibility, CompareEditorInput cei) {
		super(parent, style, visibility);
		fCompareEditorInput = cei;
	}

	private CompareConfiguration getCompareConfiguration() {
		return fCompareEditorInput.getCompareConfiguration();
	}

	@Override
	protected Viewer getViewer(Viewer oldViewer, Object input) {
		if (input instanceof ICompareInput) {
			if (fSelectedViewerDescriptor != null) {
				ViewerDescriptor[] array = CompareUIPlugin.getDefault().findStructureViewerDescriptor(
						oldViewer, (ICompareInput)input, getCompareConfiguration());
				List<ViewerDescriptor> list = array != null ? Arrays.asList(array) : Collections.emptyList();
				if (list.contains(fSelectedViewerDescriptor)) {
					// use selected viewer only when appropriate for the new input
					fCompareEditorInput
							.setStructureViewerDescriptor(fSelectedViewerDescriptor);
					Viewer viewer = fCompareEditorInput.findStructureViewer(
							oldViewer, (ICompareInput) input, this);
					return viewer;
				}
				// fallback to default otherwise
				fSelectedViewerDescriptor = null;
			}

			fCompareEditorInput.setStructureViewerDescriptor(null);
			Viewer viewer = fCompareEditorInput.findStructureViewer(oldViewer,
					(ICompareInput) input, this);
			fCompareEditorInput.setStructureViewerDescriptor(fSelectedViewerDescriptor);
			return viewer;
		}
		return null;
	}

	@Override
	protected Control createTopLeft(Composite p) {
		final Composite composite = new Composite(p, SWT.NONE) {
			@Override
			public Point computeSize(int wHint, int hHint, boolean changed) {
				return super.computeSize(wHint, Math.max(24, hHint), changed);
			}
		};

		RowLayout layout = new RowLayout();
		layout.marginTop = 0;
		composite.setLayout(layout);

		CLabel cl = new CLabel(composite, SWT.NONE);
		cl.setText(null);

		toolBar = new ToolBar(composite, SWT.FLAT);
		toolBar.setVisible(false); // hide by default
		final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH, 0);
		Utilities.setMenuImage(toolItem);
		toolItem
				.setToolTipText(CompareMessages.CompareStructureViewerSwitchingPane_switchButtonTooltip);
		toolItem.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				showMenu();
			}
		});
		toolBar.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				showMenu();
			}
		});
		return composite;
	}

	@Override
	protected boolean inputChanged(Object input) {
		return getInput() != input
				|| fCompareEditorInput.getStructureViewerDescriptor() != fSelectedViewerDescriptor;
	}

	@Override
	public void setInput(Object input) {
		super.setInput(input);
		if (getViewer() == null || !Utilities.okToUse(getViewer().getControl()))
			return;
		ViewerDescriptor[] vd = null;
		if (getInput() instanceof ICompareInput) {
			vd = CompareUIPlugin.getDefault().findStructureViewerDescriptor(
					getViewer(), (ICompareInput) getInput(),
					getCompareConfiguration());
		}
		toolBar.setVisible(vd != null && vd.length > 1);
	}

	private void showMenu() {
		if (!(getInput() instanceof ICompareInput))
			return;

		ViewerDescriptor[] vd = CompareUIPlugin.getDefault()
				.findStructureViewerDescriptor(getViewer(),
						(ICompareInput) getInput(), getCompareConfiguration());

		// 1. create
		final Menu menu = new Menu(getShell(), SWT.POP_UP);

		// add default
		String label = CompareMessages.CompareStructureViewerSwitchingPane_defaultViewer;
		MenuItem defaultItem = new MenuItem(menu, SWT.RADIO);
		defaultItem.setText(label);
		defaultItem.addSelectionListener(createSelectionListener(null));
		defaultItem.setSelection(fSelectedViewerDescriptor == null);

		new MenuItem(menu, SWT.SEPARATOR);

		// add others
		for (ViewerDescriptor vdi : vd) {
			label = vdi.getLabel();
			if (label == null || label.isEmpty()) {
				String l = CompareUIPlugin.getDefault().findStructureTypeNameOrType((ICompareInput) getInput(), vdi, getCompareConfiguration());
				if (l == null)
					// couldn't figure out the label, skip the viewer
					continue;
				label = NLS.bind(CompareMessages.CompareStructureViewerSwitchingPane_discoveredLabel, new Object[] {l});
			}
			MenuItem item = new MenuItem(menu, SWT.RADIO);
			item.setText(label);
			item.addSelectionListener(createSelectionListener(vdi));
			item.setSelection(vdi == fSelectedViewerDescriptor);
		}

		// 2. show
		Rectangle bounds = toolBar.getItem(0).getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);

		// 3. dispose on close
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(MenuEvent e) {
				e.display.asyncExec(() -> menu.dispose());
			}
		});
	}

	private SelectionListener createSelectionListener(final ViewerDescriptor vd) {
		return new SelectionListener() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				MenuItem mi = (MenuItem) e.widget;
				if (mi.getSelection()) {
					Viewer oldViewer = getViewer();
					fSelectedViewerDescriptor = vd;
					CompareStructureViewerSwitchingPane.this.setInput(oldViewer
							.getInput());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// nothing to do
			}
		};
	}

	@Override
	public void setText(String label) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (Control child : children) {
			if (child instanceof CLabel) {
				CLabel cl = (CLabel) child;
				if (cl != null && !cl.isDisposed()) {
					cl.setText(label);
					c.layout();
				}
				return;
			}
		}
	}

	@Override
	public void setImage(Image image) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (Control child : children) {
			if (child instanceof CLabel) {
				CLabel cl = (CLabel) child;
				if (cl != null && !cl.isDisposed())
					cl.setImage(image);
				return;
			}
		}
	}

	@Override
	public void addMouseListener(MouseListener listener) {
		Composite c = (Composite) getTopLeft();
		Control[] children = c.getChildren();
		for (Control child : children) {
			if (child instanceof CLabel) {
				CLabel cl = (CLabel) child;
				cl.addMouseListener(listener);
			}
		}
	}
}
