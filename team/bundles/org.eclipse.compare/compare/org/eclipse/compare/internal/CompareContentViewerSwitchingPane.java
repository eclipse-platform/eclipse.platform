/*******************************************************************************
 * Copyright (c) 2009, 2020 IBM Corporation and others.
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
import org.eclipse.compare.Splitter;
import org.eclipse.compare.internal.core.CompareSettings;
import org.eclipse.compare.structuremergeviewer.ICompareInput;
import org.eclipse.jface.layout.RowDataFactory;
import org.eclipse.jface.preference.IPreferenceStore;
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
import org.eclipse.swt.layout.RowData;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;

public class CompareContentViewerSwitchingPane extends CompareViewerSwitchingPane {
	private static final String OPTIMIZED_INFO_IMAGE_NAME = "obj16/message_info.png"; //$NON-NLS-1$
	public static final String OPTIMIZED_ALGORITHM_USED = "OPTIMIZED_ALGORITHM_USED"; //$NON-NLS-1$
	public static final String DISABLE_CAPPING_TEMPORARILY = "DISABLE_CAPPING_TEMPORARILY"; //$NON-NLS-1$

	private CompareEditorInput fCompareEditorInput;

	private ViewerDescriptor fSelectedViewerDescriptor;

	private ToolBar toolBar;
	private CLabel labelOptimized;
	private Link recomputeLink;

	private boolean menuShowing;

	public CompareContentViewerSwitchingPane(Splitter parent, int style,
			CompareEditorInput editorInput) {
		super(parent, style);
		fCompareEditorInput = editorInput;
	}

	private CompareConfiguration getCompareConfiguration() {
		return fCompareEditorInput.getCompareConfiguration();
	}

	@Override
	protected Viewer getViewer(Viewer oldViewer, Object input) {
		if (fSelectedViewerDescriptor != null) {
			ViewerDescriptor[] array = CompareUIPlugin.getDefault().findContentViewerDescriptor(
					oldViewer, input, getCompareConfiguration());
			List<ViewerDescriptor> list = array != null ? Arrays.asList(array) : Collections.emptyList();
			if (list.contains(fSelectedViewerDescriptor)) {
				// use selected viewer only when appropriate for the new input
				fCompareEditorInput
						.setContentViewerDescriptor(fSelectedViewerDescriptor);
				Viewer viewer = fCompareEditorInput.findContentViewer(
						oldViewer, (ICompareInput) input, this);
				return viewer;
			}
			// Fallback to default otherwise
			fSelectedViewerDescriptor = null;
		}
		if (input instanceof ICompareInput) {
			fCompareEditorInput.setContentViewerDescriptor(null);
			Viewer viewer =
					fCompareEditorInput.findContentViewer(oldViewer, (ICompareInput) input, this);
			fCompareEditorInput.setContentViewerDescriptor(fSelectedViewerDescriptor);
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
		layout.center = true;
		layout.wrap = false;
		composite.setLayout(layout);

		CLabel cl = new CLabel(composite, SWT.NONE);
		cl.setText(null);

		toolBar = new ToolBar(composite, SWT.FLAT);
		final ToolItem toolItem = new ToolItem(toolBar, SWT.PUSH, 0);
		Utilities.setMenuImage(toolItem);
		toolItem.setToolTipText(CompareMessages.CompareContentViewerSwitchingPane_switchButtonTooltip);
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
		toolBar.setVisible(false); // hide by default
		RowDataFactory.swtDefaults().exclude(true).applyTo(toolBar);

		labelOptimized = new CLabel(composite, SWT.NONE);
		labelOptimized.setToolTipText(CompareMessages.CompareContentViewerSwitchingPane_optimizedTooltip);
		labelOptimized.setImage(CompareUIPlugin.getImageDescriptor(
				OPTIMIZED_INFO_IMAGE_NAME).createImage());
		labelOptimized.addDisposeListener(e -> {
			Image img = labelOptimized.getImage();
			if ((img != null) && (!img.isDisposed())) {
				img.dispose();
			}
		});
		labelOptimized.setVisible(false); // hide by default
		RowDataFactory.swtDefaults().exclude(true).applyTo(labelOptimized);

		recomputeLink = new Link(composite, SWT.NONE);
		recomputeLink.setText(CompareMessages.CompareContentViewerSwitchingPane_optimizedLinkLabel);
		recomputeLink.setToolTipText(CompareMessages.CompareContentViewerSwitchingPane_optimizedTooltip);
		recomputeLink.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				/*
				 * Disable capping temporarily, refresh, restore global state.
				 * The global state is bad, but fixing that would require lots of changes and new APIs.
				 */
				IPreferenceStore preferenceStore = CompareUIPlugin.getDefault().getPreferenceStore();
				boolean wasDisabled = preferenceStore.getBoolean(ComparePreferencePage.CAPPING_DISABLED);
				CompareSettings.getDefault().setCappingDisabled(true);
				preferenceStore.setValue(ComparePreferencePage.CAPPING_DISABLED, true);
				try {
					// Setting this property makes the TextMergeViewer re-compute the diff and
					// refresh itself.
					getCompareConfiguration().setProperty(DISABLE_CAPPING_TEMPORARILY, Boolean.TRUE);
					// Hide the link now.
					labelOptimized.setVisible(false);
					recomputeLink.setVisible(false);
					((RowData) labelOptimized.getLayoutData()).exclude = true;
					((RowData) recomputeLink.getLayoutData()).exclude = true;
					composite.requestLayout();
				} finally {
					if (!wasDisabled) {
						CompareSettings.getDefault().setCappingDisabled(false);
						preferenceStore.setValue(ComparePreferencePage.CAPPING_DISABLED, false);
					}
				}
			}
		});
		recomputeLink.setVisible(false);
		RowDataFactory.swtDefaults().exclude(true).applyTo(recomputeLink);

		return composite;
	}

	@Override
	protected boolean inputChanged(Object input) {
		return getInput() != input
				|| fCompareEditorInput.getContentViewerDescriptor() != fSelectedViewerDescriptor;
	}

	@Override
	public void setInput(Object input) {
		super.setInput(input);
		if (getViewer() == null || !Utilities.okToUse(getViewer().getControl()))
			return;
		ViewerDescriptor[] vd = CompareUIPlugin.getDefault()
				.findContentViewerDescriptor(getViewer(), getInput(), getCompareConfiguration());
		boolean toolbarVisible = vd != null && vd.length > 1;
		toolBar.setVisible(toolbarVisible);
		((RowData) toolBar.getLayoutData()).exclude = !toolbarVisible;
		CompareConfiguration cc = getCompareConfiguration();
		Boolean isOptimized = (Boolean) cc.getProperty(OPTIMIZED_ALGORITHM_USED);
		boolean optimizedVisible = isOptimized != null && isOptimized.booleanValue();
		labelOptimized.setVisible(optimizedVisible);
		recomputeLink.setVisible(optimizedVisible);
		((RowData) labelOptimized.getLayoutData()).exclude = !optimizedVisible;
		((RowData) recomputeLink.getLayoutData()).exclude = !optimizedVisible;
	}

	private void showMenu() {
		if (menuShowing)
			return;
		menuShowing= true;

		ViewerDescriptor[] vd = CompareUIPlugin.getDefault()
				.findContentViewerDescriptor(getViewer(), getInput(),getCompareConfiguration());

		// 1. Create
		final Menu menu = new Menu(getShell(), SWT.POP_UP);

		// Add default
		String label = CompareMessages.CompareContentViewerSwitchingPane_defaultViewer;
		MenuItem defaultItem = new MenuItem(menu, SWT.RADIO);
		defaultItem.setText(label);
		defaultItem.addSelectionListener(createSelectionListener(null));
		defaultItem.setSelection(fSelectedViewerDescriptor == null);

		new MenuItem(menu, SWT.SEPARATOR);

		// Add others
		for (ViewerDescriptor vdi : vd) {
			label = vdi.getLabel();
			if (label == null || label.isEmpty()) {
				String l = CompareUIPlugin.getDefault().findContentTypeNameOrType((ICompareInput) getInput(),
						vdi, getCompareConfiguration());
				if (l == null)
					continue;  // Couldn't figure out the label, skip the viewer
				label = NLS.bind(CompareMessages.CompareContentViewerSwitchingPane_discoveredLabel, l);
			}
			MenuItem item = new MenuItem(menu, SWT.RADIO);
			item.setText(label);
			item.addSelectionListener(createSelectionListener(vdi));
			item.setSelection(vdi == fSelectedViewerDescriptor);
		}

		// 2. Show
		Rectangle bounds = toolBar.getItem(0).getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);

		// 3. Dispose on close
		menu.addMenuListener(new MenuAdapter() {
			@Override
			public void menuHidden(MenuEvent e) {
				menuShowing= false;
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
					CompareContentViewerSwitchingPane.this.setInput(oldViewer.getInput());
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// Nothing to do
			}
		};
	}

	@Override
	public void setText(String text) {
		Composite c = (Composite) getTopLeft();
		for (Control child : c.getChildren()) {
			if (child instanceof CLabel) {
				CLabel label = (CLabel) child;
				if (label != null && !label.isDisposed()) {
					label.setText(text);
					c.layout();
				}
				return;
			}
		}
	}

	@Override
	public void setImage(Image image) {
		Composite c = (Composite) getTopLeft();
		for (Control child : c.getChildren()) {
			if (child instanceof CLabel) {
				CLabel label = (CLabel) child;
				if (label != null && !label.isDisposed())
					label.setImage(image);
				return;
			}
		}
	}

	@Override
	public void addMouseListener(MouseListener listener) {
		Composite c = (Composite) getTopLeft();
		for (Control child : c.getChildren()) {
			if (child instanceof CLabel) {
				CLabel label = (CLabel) child;
				label.addMouseListener(listener);
			}
		}
	}
}
