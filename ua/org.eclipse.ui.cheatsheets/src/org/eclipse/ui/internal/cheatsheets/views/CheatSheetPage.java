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
 *******************************************************************************/
package org.eclipse.ui.internal.cheatsheets.views;

import java.util.ArrayList;
import java.util.Iterator;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.ui.forms.FormColors;
import org.eclipse.ui.forms.HyperlinkGroup;
import org.eclipse.ui.forms.HyperlinkSettings;
import org.eclipse.ui.forms.widgets.FormToolkit;
import org.eclipse.ui.forms.widgets.ScrolledForm;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.CheatSheetStopWatch;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;
import org.eclipse.ui.internal.cheatsheets.actions.IMenuContributor;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.Item;

public class CheatSheetPage extends Page implements IMenuContributor {
	// Colors
	private Color introColor;
	private Color activeColor;

	private Color inactiveColor1;
	private Color inactiveColor2;

	private CheatSheet cheatSheet;

	private ArrayList<ViewItem> viewItemList;

	private CheatSheetViewer viewer;

	public CheatSheetPage(CheatSheet cheatSheet, ArrayList<ViewItem> viewItemList,
			CheatSheetViewer cheatSheetViewer) {
		super();
		this.cheatSheet = cheatSheet;
		this.viewItemList = viewItemList;
		this.viewer = cheatSheetViewer;
	}

	@Override
	public void createPart(Composite parent) {
		CheatSheetStopWatch.startStopWatch("CheatSheetPage.createPart()"); //$NON-NLS-1$
		super.createPart(parent);
		HyperlinkGroup hyperlinkGroup = toolkit.getHyperlinkGroup();
		hyperlinkGroup.setHyperlinkUnderlineMode(HyperlinkSettings.UNDERLINE_HOVER);

		CheatSheetStopWatch
				.printLapTime(
						"CheatSheetPage.createPart()", "Time in CheatSheetPage.createInfoArea() after super.createInfoArea(): "); //$NON-NLS-1$ //$NON-NLS-2$
		IntroItem intro = new IntroItem(this, cheatSheet.getIntroItem(),
				introColor, viewer);
		CheatSheetStopWatch
				.printLapTime(
						"CheatSheetPage.createPart()", "Time in CheatSheetPage.createInfoArea() after new IntroItem(): "); //$NON-NLS-1$ //$NON-NLS-2$

		intro.setBold(true);
		viewItemList.add(intro);

		CheatSheetStopWatch
				.printLapTime(
						"CheatSheetPage.createInfoArea()", "Time in CheatSheetPage.createPart() before add loop: "); //$NON-NLS-1$ //$NON-NLS-2$
		// Get the content info from the parser. This makes up all items except
		// the intro item.
		ArrayList<Item> items = cheatSheet.getItems();
		for (int i = 0; i < items.size(); i++) {
			Color color = (i % 2) == 0 ? getInactiveColor1() : getInactiveColor2();

			CoreItem coreItem = new CoreItem(this, items.get(i), color, viewer);
			viewItemList.add(coreItem);
		}
		CheatSheetStopWatch
				.printLapTime(
						"CheatSheetPage.createInfoArea()", "Time in CheatSheetPage.createPart(): "); //$NON-NLS-1$ //$NON-NLS-2$
	}

	private Color getInactiveColor2() {
		return inactiveColor2;
	}

	private Color getInactiveColor1() {
		return inactiveColor1;
	}

	/**
	 * Creates the cheatsheet's title area which will consists of a title and
	 * image.
	 *
	 * @param parent
	 *            the SWT parent for the title area composite
	 */
	@Override
	protected String getTitle() {
		if (cheatSheet != null && cheatSheet.getTitle() != null)
			return cheatSheet.getTitle();
		return ICheatSheetResource.EMPTY_STRING;
	}

	@Override
	public void dispose() {
		super.dispose();
		inactiveColor1 = null;
		inactiveColor2 = null;
		activeColor = null;
		introColor = null;
	}

	@Override
	protected void init(Display display) {
		super.init(display);
		computeColors(display);
	}

	private void computeColors(Display display) {
		RGB rgb;
		RGB white = new RGB(255, 255, 255);
		RGB black = new RGB(0, 0, 0);

		if (isReverseVideo()) {
			computeReverseVideoColors(display);
			return;
		}

		if (toolkit.getColors().isWhiteBackground()) {
			rgb = toolkit.getColors().getSystemColor(SWT.COLOR_LIST_SELECTION);
			// active color is selection + 80% white
			rgb = FormColors.blend(rgb, white, 20);
			// test for bounds
			if (FormColors.testTwoPrimaryColors(rgb, 245, 256)) {
				// too bright - tone down 20%
				rgb = FormColors.blend(rgb, black, 80);
			} else if (FormColors.testAnyPrimaryColor(rgb, 170, 191)) {
				// too dark - brighten 15%
				rgb = FormColors.blend(rgb, white, 85);
			} else if (FormColors.testAnyPrimaryColor(rgb, 190, 215)) {
				// too dark - brighten 10%
				rgb = FormColors.blend(rgb, white, 90);
			}
			// final check - if gray
			if (Math.abs(rgb.blue-rgb.green) <5 &&
					Math.abs(rgb.blue-rgb.red)<5 &&
					Math.abs(rgb.green-rgb.red)<5) {
				// blend with blue
				rgb = FormColors.blend(rgb, new RGB(100, 100, 255), 90);
			}
			introColor = new Color(display, rgb);
			inactiveColor2 = new Color(display, rgb);
		} else {
			// colored background
			rgb = toolkit.getColors().getSystemColor(SWT.COLOR_LIST_SELECTION);
			// active color is selection + 70% white
			rgb = FormColors.blend(rgb, white, 30);
			// If these values are in the range of 201 to 220, then decrease
			// white by 20%
			if (FormColors.testTwoPrimaryColors(rgb, 200, 221))
				rgb = FormColors.blend(rgb, black, 80);
			// If these values are in the range of 221 to 240, then decrease
			// white by 40%
			else if (FormColors.testTwoPrimaryColors(rgb, 222, 241))
				rgb = FormColors.blend(rgb, black, 60);
			// If these values are in the range of 241 to 255, then decrease
			// white by 70%
			else if (FormColors.testTwoPrimaryColors(rgb, 240, 256))
				rgb = FormColors.blend(rgb, black, 30);
			introColor = new Color(display, rgb);
			inactiveColor2 = new Color(display, rgb);
		}
		rgb = inactiveColor2.getRGB();
		rgb = FormColors.blend(rgb, backgroundColor.getRGB(), 40);
		inactiveColor1 = new Color(display, rgb);
		activeColor = new Color(display, backgroundColor.getRGB());
	}

	private void computeReverseVideoColors(Display display) {
		Color background = toolkit.getColors().getBackground();
		RGB white = new RGB(255, 255, 255);
		// Create new colors, they will get disposed
		RGB rgb = background.getRGB();
		activeColor = new Color(display, rgb );
		rgb = FormColors.blend(rgb, white, 85);
		inactiveColor1 = new Color(display, rgb);
		rgb = FormColors.blend(rgb, white, 85);
		inactiveColor2 = new Color(display, rgb );
		introColor = new Color(display, rgb );
	}

	private boolean isReverseVideo() {
		Color bg = toolkit.getColors().getBackground();
		return ((bg.getBlue() + bg.getRed() + bg.getGreen()) < 380);
	}

	@Override
	public void initialized() {
		for (Iterator<ViewItem> iter = viewItemList.iterator(); iter.hasNext();) {
			ViewItem item = iter.next();
			item.initialized();
		}
	}

	public Color getActiveColor() {
		return activeColor;
	}

	public ScrolledForm getForm() {
		return form;
	}

	public FormToolkit getToolkit() {
		return toolkit;
	}

	private int contributeRestartItem(Menu menu, int index) {
		MenuItem item = new MenuItem(menu, SWT.PUSH, index++);
		item.setText(Messages.RESTART_MENU);
		item.setImage(CheatSheetPlugin.getPlugin().getImage(ICheatSheetResource.CHEATSHEET_RETURN));

		item.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				viewer.restart();
			}
		});
		return index;
	}

	@Override
	public int contributeToViewMenu(Menu menu, int index) {
		return contributeRestartItem(menu, index);
	}
}
