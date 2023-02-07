/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.examples.swttip;

import java.util.Date;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.tips.core.Tip;
import org.eclipse.tips.examples.DateUtil;
import org.eclipse.tips.ui.ISwtTip;

public class SwtTipImpl extends Tip implements ISwtTip {

	private static final class Beeper extends SelectionAdapter {
		@Override
		public void widgetSelected(SelectionEvent e) {
			e.widget.getDisplay().beep();
			if (((Button) e.widget).getText().contains("2")) {
				try {
					Thread.sleep(600);
				} catch (InterruptedException e1) {
				}
				e.widget.getDisplay().beep();
			}
		}
	}

	private String fSubject;

	public SwtTipImpl(String providerId, long number) {
		super(providerId);
		fSubject = "This is a tip " + number;
	}

	@Override
	public Date getCreationDate() {
		return DateUtil.getDateFromYYMMDD("10/01/2018");
	}

	@Override
	public String getSubject() {
		return fSubject;
	}

	@Override
	public void createControl(Composite pParent) {
		pParent.setLayout(new GridLayout(2, false));
		Group group = new Group(pParent, SWT.NONE);
		GridDataFactory.fillDefaults().span(2, SWT.DEFAULT).grab(true, true).applyTo(group);
		group.setLayout(new FillLayout());
		group.setText(fSubject);
		Text text = new Text(group, SWT.MULTI | SWT.WRAP);
		text.setText(fSubject + System.lineSeparator() + System.lineSeparator()
				+ "There are thousands of these tips. Just press next tip and you will see.");
		Button button1 = new Button(pParent, SWT.PUSH);
		button1.setText("Beep once");
		button1.addSelectionListener(new Beeper());
		GridDataFactory.fillDefaults().applyTo(button1);
		Button button2 = new Button(pParent, SWT.PUSH);
		button2.setText("Beep 2 times");
		button2.addSelectionListener(new Beeper());
		GridDataFactory.fillDefaults().applyTo(button2);
	}
}