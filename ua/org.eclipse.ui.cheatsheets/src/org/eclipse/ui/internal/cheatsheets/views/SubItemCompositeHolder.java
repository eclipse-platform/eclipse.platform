/*******************************************************************************
 * Copyright (c) 2002, 2017 IBM Corporation and others.
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
 *     Asma Smaoui - CEA LIST - https://bugs.eclipse.org/bugs/show_bug.cgi?id=517379
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import org.eclipse.swt.widgets.*;
import org.eclipse.ui.forms.widgets.ImageHyperlink;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.data.SubItem;

public class SubItemCompositeHolder {
	private Label checkDoneLabel;
	private boolean skipped;
	private boolean completed;
	private ImageHyperlink startButton;
	private String thisValue;
	private SubItem subItem;
	private Control skipButton;
	private Control completeButton;
	private Control subitemLabel;

	SubItemCompositeHolder(SubItem subItem) {
		super();
		this.subItem = subItem;
	}

	/**
	 * @return Label
	 */
	/*package*/ Label getCheckDoneLabel() {
		return checkDoneLabel;
	}

	/**
	 * @return
	 */
	public boolean isCompleted() {
		return completed;
	}

	/**
	 * @return
	 */
	public boolean isSkipped() {
		return skipped;
	}

	/**
	 * @param isCompleted
	 */
	public void setCompleted(boolean isCompleted) {
		completed = isCompleted;
		if (isCompleted && checkDoneLabel != null) {
			checkDoneLabel.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_COMPLETE));
		}
		checkDoneLabel.setVisible(completed || skipped);
	}

	/**
	 * @param isSkipped
	 */
	public void setSkipped(boolean isSkipped) {
		skipped = isSkipped;
		if (isSkipped && checkDoneLabel != null) {
			checkDoneLabel.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_ITEM_SKIP));
		}
		checkDoneLabel.setVisible(completed || skipped);
	}

	/**
	 * @return Returns the thisValue.
	 */
	public String getThisValue() {
		return thisValue;
	}

	/**
	 * @param thisValue The thisValue to set.
	 */
	public void setThisValue(String thisValue) {
		this.thisValue = thisValue;
	}

	/**
	 * @return Returns the subItem.
	 */
	public SubItem getSubItem() {
		return subItem;
	}

	/**
	 * Hide or reveal all the action/complete/skip buttons
	 * @param isVisible
	 */
	public void setButtonsVisible(boolean isVisible) {
		if (startButton != null) {
			startButton.setVisible(isVisible);
		}
		if (skipButton != null) {
			skipButton.setVisible(isVisible);
		}
		if (completeButton != null) {
			completeButton.setVisible(isVisible);
		}
	}

	public void setSubitemLabel(Control label) {
		this.subitemLabel = label;
	}

	public Control getSubitemLabel() {
		return subitemLabel;
	}

	public void setStartButton(ImageHyperlink startButton) {
		this.startButton = startButton;
	}

	public ImageHyperlink getStartButton() {
		return startButton;
	}

	public void setSkipButton(Control skipButton) {
		this.skipButton = skipButton;
	}

	public Control getSkipButton() {
		return skipButton;
	}

	public void setCompleteButton(Control completeButton) {
		this.completeButton = completeButton;
	}

	public Control getCompleteButton() {
		return completeButton;
	}

	public void setCheckDoneLabel(Label checkDoneLabel) {
		this.checkDoneLabel = checkDoneLabel;
	}
}
