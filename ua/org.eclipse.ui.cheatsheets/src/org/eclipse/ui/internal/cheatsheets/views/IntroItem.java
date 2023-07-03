/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.views;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.forms.widgets.TableWrapData;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.data.Item;

public class IntroItem extends ViewItem {
	private ImageHyperlink startButton;

	/**
	 * Constructor for IntroItem.
	 * @param parent
	 * @param contentItem
	 */
	public IntroItem(CheatSheetPage page, Item contentItem, Color itemColor, CheatSheetViewer viewer) {
		super(page, contentItem, itemColor, viewer);
	}


	/*package*/ @Override
	void setStartImage() {
		startButton.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_START));
		startButton.setText(Messages.START_CHEATSHEET_TOOLTIP);
		startButton.setToolTipText(Messages.START_CHEATSHEET_TOOLTIP);
//		startButton.setFAccessibleName(startButton.getToolTipText());
	}

	/*package*/ @Override
	void setRestartImage() {
		startButton.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_RETURN));
		startButton.setText(Messages.RESTART_CHEATSHEET_TOOLTIP);
		startButton.setToolTipText(Messages.RESTART_CHEATSHEET_TOOLTIP);
//		startButton.setFAccessibleName(startButton.getToolTipText());
	}

	@Override
	/*package*/ void handleButtons() {
		buttonComposite = page.getToolkit().createComposite(bodyWrapperComposite);
		GridLayout buttonlayout = new GridLayout(4, false);
		buttonlayout.marginHeight = 2;
		buttonlayout.marginWidth = 2;
		buttonlayout.verticalSpacing = 2;

		TableWrapData buttonData = new TableWrapData(TableWrapData.FILL);

		buttonComposite.setLayout(buttonlayout);
		buttonComposite.setLayoutData(buttonData);
		buttonComposite.setBackground(itemColor);
		Label filllabel = page.getToolkit().createLabel(buttonComposite, null);
		filllabel.setBackground(itemColor);
		GridData filldata = new GridData();
		filldata.widthHint = 16;
		filllabel.setLayoutData(filldata);

		startButton = createButtonWithText(buttonComposite, CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.COMPOSITE_TASK_START), this, itemColor, Messages.START_CHEATSHEET_TOOLTIP);
		startButton.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		startButton.addHyperlinkListener(new HyperlinkAdapter() {
			@Override
			public void linkActivated(HyperlinkEvent e) {
				viewer.advanceIntroItem();
			}
		});
	}

	@Override
	boolean hasCompletionMessage() {
		return false;
	}

	@Override
	void createCompletionComposite(boolean isFinalItem) {
		// Not called
	}

}
