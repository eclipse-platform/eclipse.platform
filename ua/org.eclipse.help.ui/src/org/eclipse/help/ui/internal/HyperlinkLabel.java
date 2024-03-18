/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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

package org.eclipse.help.ui.internal;

import org.eclipse.swt.SWT;
import org.eclipse.swt.accessibility.ACC;
import org.eclipse.swt.accessibility.Accessible;
import org.eclipse.swt.accessibility.AccessibleAdapter;
import org.eclipse.swt.accessibility.AccessibleControlAdapter;
import org.eclipse.swt.accessibility.AccessibleControlEvent;
import org.eclipse.swt.accessibility.AccessibleEvent;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.MouseListener;
import org.eclipse.swt.events.MouseTrackListener;
import org.eclipse.swt.events.PaintEvent;
import org.eclipse.swt.events.PaintListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;

/**
 *
 * A canvas holding a hyperlink label. Need this to deal with focus selection.
 */
public class HyperlinkLabel extends Canvas {
	Label label;
	boolean hasFocus;

	/**
	 * Constructor for Hyperlink.
	 */
	public HyperlinkLabel(Composite parent, int style) {
		super(parent, style);

		GridLayout layout = new GridLayout();
		layout.marginHeight = 3;
		layout.marginWidth = 2;
		layout.numColumns = 1;
		this.setLayout(layout);

		this.label = new Label(this, style);

		addPaintListener(this::paint);

		addKeyListener(new KeyAdapter() {

			@Override
			public void keyPressed(KeyEvent e) {
				if (e.character == '\r') {
					// Activation
					notifyListeners(SWT.DefaultSelection);
				}
			}
		});

		addListener(SWT.Traverse, e -> {
			switch (e.detail) {
			// let arrows move focus
			case SWT.TRAVERSE_ARROW_NEXT:
				e.detail = SWT.TRAVERSE_TAB_NEXT;
				break;
			case SWT.TRAVERSE_ARROW_PREVIOUS:
				e.detail = SWT.TRAVERSE_TAB_PREVIOUS;
				break;

			case SWT.TRAVERSE_PAGE_NEXT:
			case SWT.TRAVERSE_PAGE_PREVIOUS:
			case SWT.TRAVERSE_RETURN:
				e.doit = false;
				return;
			}
			e.doit = true;
		});

		addFocusListener(new FocusListener() {

			@Override
			public void focusGained(FocusEvent e) {
				if (!hasFocus) {
					hasFocus = true;
					notifyListeners(SWT.Selection);
					redraw();
				}
			}

			@Override
			public void focusLost(FocusEvent e) {
				if (hasFocus) {
					hasFocus = false;
					notifyListeners(SWT.Selection);
					redraw();
				}
			}
		});

		GridData data = new GridData();
		data.horizontalAlignment = GridData.HORIZONTAL_ALIGN_BEGINNING;
		data.verticalAlignment = GridData.VERTICAL_ALIGN_BEGINNING;
		label.setLayoutData(data);

		initAccessibleLink();
		initAccessibleLabel();
	}

	public void setText(String text) {
		label.setText(text);
	}

	public boolean getSelection() {
		return hasFocus;
	}

	public Label getLabel() {
		return label;
	}

	void notifyListeners(int eventType) {
		Event event = new Event();
		event.type = eventType;
		event.widget = this;
		notifyListeners(eventType, event);
	}

	protected void paint(PaintEvent e) {
		if (hasFocus) {
			GC gc = e.gc;
			Point size = getSize();
			gc.setForeground(getForeground());
			gc.drawFocus(0, 0, size.x, size.y);
		}
	}

	public void addSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) {
			return;
		}
		addTypedListener(listener, SWT.Selection, SWT.DefaultSelection);
	}

	public void removeSelectionListener(SelectionListener listener) {
		checkWidget();
		if (listener == null) {
			return;
		}
		removeTypedListener(SWT.Selection, listener);
		removeTypedListener(SWT.DefaultSelection, listener);
	}

	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		int innerWidth = wHint;
		if (innerWidth != SWT.DEFAULT)
			innerWidth -= 4;
		Point textSize = label.computeSize(wHint, hHint, changed);//computeTextSize(innerWidth,
		// hHint);
		int textWidth = textSize.x + 4;
		int textHeight = textSize.y + 6;
		return new Point(textWidth, textHeight);
	}

	@Override
	public void addMouseListener(MouseListener l) {
		//super.addMouseListener(l);
		label.addMouseListener(l);
	}

	@Override
	public void addMouseTrackListener(MouseTrackListener l) {
		//super.addMouseTrackListener(l);
		label.addMouseTrackListener(l);
	}

	@Override
	public void addPaintListener(PaintListener l) {
		super.addPaintListener(l);
		label.addPaintListener(l);
	}

	@Override
	public void setBackground(Color c) {
		super.setBackground(c);
		label.setBackground(c);
	}

	@Override
	public void setForeground(Color c) {
		super.setForeground(c);
		label.setForeground(c);
	}

	@Override
	public void setCursor(Cursor c) {
		super.setCursor(c);
		label.setCursor(c);
	}

	private void initAccessibleLink() {
		Accessible accessible = this.getAccessible();
		accessible.addAccessibleListener(new AccessibleAdapter() {

			@Override
			public void getName(AccessibleEvent e) {
				e.result = label.getText();
			}

			@Override
			public void getHelp(AccessibleEvent e) {
				e.result = label.getToolTipText();
			}
		});

		accessible.addAccessibleControlListener(new AccessibleControlAdapter() {

			@Override
			public void getRole(AccessibleControlEvent e) {
				e.detail = ACC.ROLE_LINK;
			}

			@Override
			public void getState(AccessibleControlEvent e) {
				if (hasFocus)
					e.detail = ACC.STATE_FOCUSABLE | ACC.STATE_LINKED
							| ACC.STATE_FOCUSED;
				else
					e.detail = ACC.STATE_FOCUSABLE | ACC.STATE_LINKED;

			}
		});
	}
	private void initAccessibleLabel() {
		Accessible accessible = label.getAccessible();
		accessible.addAccessibleControlListener(new AccessibleControlAdapter() {

			@Override
			public void getState(AccessibleControlEvent e) {
				if (hasFocus)
					e.detail = ACC.STATE_READONLY | ACC.STATE_FOCUSABLE
							| ACC.STATE_SELECTABLE | ACC.STATE_LINKED
							| ACC.STATE_FOCUSED;
				else
					e.detail = ACC.STATE_READONLY | ACC.STATE_FOCUSABLE
							| ACC.STATE_SELECTABLE | ACC.STATE_LINKED;

			}
		});
	}
}
