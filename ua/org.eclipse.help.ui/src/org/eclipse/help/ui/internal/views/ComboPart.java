/*******************************************************************************
 * Copyright (c) 2003, 2005 IBM Corporation and others.
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

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.KeyListener;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.FormToolkit;

public class ComboPart {
	private Control combo;

	public ComboPart() {
	}

	public ComboPart(Composite parent, FormToolkit toolkit, int style) {
		createControl(parent, toolkit, style);
	}

	public void addSelectionListener(SelectionListener listener) {
		if (combo instanceof Combo)
			((Combo) combo).addSelectionListener(listener);
		else
			((CCombo) combo).addSelectionListener(listener);
	}

	public void addModifyListener(ModifyListener listener) {
		if (combo instanceof Combo)
			((Combo) combo).addModifyListener(listener);
		else
			((CCombo) combo).addModifyListener(listener);
	}

	public void addKeyListener(KeyListener listener) {
		if (combo instanceof Combo)
			((Combo) combo).addKeyListener(listener);
		else
			((CCombo) combo).addKeyListener(listener);
	}

	public void createControl(Composite parent, FormToolkit toolkit, int style) {
		if (toolkit.getBorderStyle() == SWT.BORDER)
			combo = new Combo(parent, style | SWT.BORDER);
		else
			combo = new CCombo(parent, style | SWT.FLAT);
		toolkit.adapt(combo, true, true);
	}

	public Control getControl() {
		return combo;
	}

	public int getSelectionIndex() {
		if (combo instanceof Combo)
			return ((Combo) combo).getSelectionIndex();
		return ((CCombo) combo).getSelectionIndex();
	}

	public void add(String item, int index) {
		if (combo instanceof Combo)
			((Combo) combo).add(item, index);
		else
			((CCombo) combo).add(item, index);
	}

	public void add(String item) {
		if (combo instanceof Combo)
			((Combo) combo).add(item);
		else
			((CCombo) combo).add(item);
	}

	public void select(int index) {
		if (combo instanceof Combo)
			((Combo) combo).select(index);
		else
			((CCombo) combo).select(index);
	}

	public String getSelection() {
		if (combo instanceof Combo)
			return ((Combo) combo).getItem(getSelectionIndex());
		return ((CCombo) combo).getItem(getSelectionIndex());
	}

	public void setText(String text) {
		if (combo instanceof Combo)
			((Combo) combo).setText(text);
		else
			((CCombo) combo).setText(text);
	}
	public String getText() {
		if (combo instanceof Combo)
			return ((Combo) combo).getText();
		return ((CCombo) combo).getText();
	}

	public void setItems(String[] items) {
		if (combo instanceof Combo)
			((Combo) combo).setItems(items);
		else
			((CCombo) combo).setItems(items);
	}
}
