/****************************************************************************
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
 *    Wim Jongman <wim.jongman@remainsoftware.com> - initial API and implementation
 *****************************************************************************/
package org.eclipse.tips.core;

/**
 * Provides an action to be executed by a Tip.
 *
 */
public class TipAction {

	private final String fText;
	private final TipImage fTipImage;
	private final Runnable fRunner;
	private final String fTooltip;

	/**
	 * Creates a new TipAction. Tip actions can be executed in the tip UI when the
	 * associated Tip is displayed.
	 *
	 * @param text
	 *            a very short description to be used on buttons and menus.
	 * @param tooltip
	 *            a longer description to be shown as tool tip when possible.
	 * @param runner
	 *            the actual code to run
	 * @param image
	 *            the image to be shown when possible.
	 *
	 */
	public TipAction(String text, String tooltip, Runnable runner, TipImage image) {
		fText = text;
		fTooltip = tooltip;
		fRunner = runner;
		fTipImage = image;
	}

	/**
	 * The short description of the action to be shown as button text or menu entry
	 * when possible.
	 *
	 * @return the text
	 */
	public String getText() {
		return fText;
	}

	/**
	 * A longer description to be shown as tool tip when possible.
	 *
	 * @return the tool tip.
	 */
	public String getTooltip() {
		return fTooltip;
	}

	/**
	 * The icon of the image wrapped in a TipImage.
	 *
	 * @return the icon
	 */
	public TipImage getTipImage() {
		return fTipImage;
	}

	/**
	 * The actual code to run when this action is executed.
	 *
	 * @return the runner.
	 */
	public Runnable getRunner() {
		return fRunner;
	}
}