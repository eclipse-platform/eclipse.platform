/*******************************************************************************
 * Copyright (c) 2006, 2016 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation 2008 - Bug 248079 [Help][Index] unicode sort issue in index view
 *******************************************************************************/
package org.eclipse.help.ui.internal.views;

import java.text.Collator;

import org.eclipse.help.ui.internal.IHelpUIConstants;
import org.eclipse.help.ui.internal.Messages;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.forms.AbstractFormPart;
import org.eclipse.ui.forms.widgets.FormText;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.forms.widgets.TableWrapLayout;

public class IndexTypeinPart extends AbstractFormPart implements IHelpPart, IHelpUIConstants {
	private ReusableHelpPart parent;
	String id;
	private Composite container;
	private FormText indexInstructions;
	private Text indexText;
	private Button indexButton;
	private IndexPart indexPart;
	private Tree indexTree;
	private String[] rootItems;
	private int currentIndex;

	public IndexTypeinPart(Composite parent, FormToolkit toolkit, IToolBarManager tbm) {
		container = toolkit.createComposite(parent);

		TableWrapLayout layout = new TableWrapLayout();
		layout.numColumns = 2;
		container.setLayout(layout);

		// Instructions
		indexInstructions = toolkit.createFormText(container, false);
		indexInstructions.setText(Messages.IndexInstructions, false, false);
		TableWrapData td = new TableWrapData();
		td.colspan = 2;
		indexInstructions.setLayoutData(td);

		// Index typein
		indexText = toolkit.createText(container, null);
		td = new TableWrapData(TableWrapData.FILL_GRAB);
		td.maxWidth = 100;
		td.valign = TableWrapData.MIDDLE;
		indexText.setLayoutData(td);
		indexButton = toolkit.createButton(container, Messages.IndexButton, SWT.PUSH);
		indexButton.setEnabled(false);

		indexText.addModifyListener(e -> {
			indexButton.setEnabled(indexText.getText().length() > 0);// !!!
			doNavigate(indexText.getText());
		});
		indexText.addKeyListener(new KeyListener() {

			@Override
			public void keyReleased(KeyEvent e) {
				if (e.character == '\r' && indexButton.isEnabled()) {
						doOpen();
				}
			}

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.keyCode == SWT.ARROW_UP) {
					e.doit = false;
					doUp();
				} else if (e.keyCode == SWT.ARROW_DOWN) {
					e.doit = false;
					doDown();
				}
			}});
		indexButton.addSelectionListener(new SelectionAdapter() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				doOpen();
			}});

		toolkit.paintBordersFor(container);

		currentIndex = -1;
	}

	protected void doUp() {
		checkTree();
		if (indexTree == null) return;

		int index = 0;
		TreeItem[] items = indexTree.getSelection();
		if (items.length > 0) {
			index = indexTree.indexOf(items[0]) - 1;
			if (index < 0) {
				return;
			}
		}
		TreeItem item = indexTree.getItem(index);
		indexTree.setSelection(new TreeItem[] { item });
		String text = item.getText();
		indexText.setText(text);
		indexText.setSelection(0, text.length());
	}

	protected void doDown() {
		checkTree();
		if (indexTree == null) return;

		int index = 0;
		TreeItem[] items = indexTree.getSelection();
		if (items.length > 0) {
			index = indexTree.indexOf(items[0]) + 1;
			if (index >= indexTree.getItemCount()) {
				return;
			}
		}
		TreeItem item = indexTree.getItem(index);
		indexTree.setSelection(new TreeItem[] { item });
		String text = item.getText();
		indexText.setText(text);
		indexText.setSelection(0, text.length());
	}

	protected void doNavigate(String text) {
		checkTree();
		if (rootItems == null) return;

		int index = searchPattern(text);
		if (index != -1 && index != currentIndex) {
			indexTree.setSelection(new TreeItem[] { indexTree.getItem(index) });
			currentIndex = index;
		}
	}

	private void checkTree() {
		if (rootItems != null) return;

		indexPart = (IndexPart)parent.findPart(HV_INDEX);
		if (indexPart == null) return;

		indexTree = indexPart.getTreeWidget();
		if (indexTree == null) return;

		int itemCount = indexTree.getItemCount();
		if (itemCount == 0) {
			indexTree = null;
		} else {
			rootItems = new String[itemCount];
			for (int i = 0; i < itemCount; i++) {
				rootItems[i] = indexTree.getItem(i).getText();
			}
		}
	}

	/*
	 * TODO optimize
	 */
	private int searchPattern(String pattern) {
		int from = 0;
		int to = rootItems.length;
		int i;
		int res;

		while (to > from) {
			i = (to + from) / 2;
			res = compare(rootItems[i], pattern);
			if (res == 0) {
				while (i > 0) {
					if (compare(rootItems[--i], pattern) != 0) {
						i++;
						break;
					}
				}
				return i;
			}

			if (res < 0) {
				from = i + 1;
			} else {
				to = i;
			}
		}
		if (from >= rootItems.length) {
			return rootItems.length - 1;
		} else {
			return from;
		}
	}

	int compare(String keyword, String pattern) {
		return Collator.getInstance().compare(keyword, pattern);
	}

	protected void doOpen() {
		checkTree();
		if (indexTree == null) return;

		TreeItem items[] = indexTree.getSelection();
		if (items.length == 0) return;

		Object obj = items[0].getData();
		if (obj != null) {
			indexPart.doOpen(obj);
		}
	}

	@Override
	public void init(ReusableHelpPart parent, String id, IMemento memento) {
		this.parent = parent;
		this.id = id;
	}

	@Override
	public void saveState(IMemento memento) {
	}

	@Override
	public Control getControl() {
		return container;
	}

	@Override
	public String getId() {
		return id;
	}

	@Override
	public void setVisible(boolean visible) {
		getControl().setVisible(visible);
	}

	@Override
	public boolean hasFocusControl(Control control) {
		return false;
	}

	@Override
	public boolean fillContextMenu(IMenuManager manager) {
		return false;
	}

	@Override
	public IAction getGlobalAction(String id) {
		return null;
	}

	@Override
	public void stop() {
	}

	@Override
	public void toggleRoleFilter() {
	}

	@Override
	public void refilter() {
	}

	@Override
	public void setFocus() {
		indexText.setFocus();
	}

	public Text getTextWidget() {
		return indexText;
	}
}
