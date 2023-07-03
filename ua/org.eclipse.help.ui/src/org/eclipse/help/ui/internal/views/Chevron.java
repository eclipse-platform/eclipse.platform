/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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

import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.forms.widgets.ToggleHyperlink;

public final class Chevron extends ToggleHyperlink {
	private static final int[][] offLines = { { 0, 0, 2, 2, 0, 4 },
			{ 1, 0, 3, 2, 1, 4 }, { 4, 0, 6, 2, 4, 4 }, { 5, 0, 7, 2, 5, 4 } };

	private static final int[][] onLines = { { 2, 0, 0, 2, 2, 4 },
			{ 3, 0, 1, 2, 3, 4 }, { 6, 0, 4, 2, 6, 4 }, { 7, 0, 5, 2, 7, 4 } };

	/**
	 * Creates a control in a provided composite.
	 *
	 * @param parent
	 *            the parent
	 * @param style
	 *            the style
	 */
	public Chevron(Composite parent, int style) {
		super(parent, style);
		innerWidth = 8;
		innerHeight = 5;
		marginWidth = 3;
		marginHeight = 4;
	}

	@Override
	protected void paintHyperlink(GC gc) {
		if (hover && getHoverDecorationColor() != null)
			gc.setForeground(getHoverDecorationColor());
		else if (getDecorationColor() != null)
			gc.setForeground(getDecorationColor());
		int[][] data;
		Rectangle carea = getClientArea();
		int x = (carea.width - innerWidth) /2;
		int y = (carea.height - innerHeight) / 2;
		if (isExpanded())
			data = translate(onLines, x, y);
		else
			data = translate(offLines, x, y);
		for (int i=0; i<data.length; i++) {
			gc.drawPolyline(data[i]);
		}
		gc.setBackground(getBackground());
	}

	private int[][] translate(int[][] data, int x, int y) {
		int[][] target = new int[data.length][];
		for (int i = 0; i<data.length; i++) {
			int [] line = data[i];
			target[i] = new int[line.length];
			for (int j = 0; j < line.length; j += 2) {
				target[i][j] = line[j] + x;
			}
			for (int j = 1; j < line.length; j += 2) {
				target[i][j] = line[j] + y;
			}
		}
		return target;
	}
}