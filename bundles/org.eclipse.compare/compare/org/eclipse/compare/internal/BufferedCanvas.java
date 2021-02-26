/*******************************************************************************
 * Copyright (c) 2000, 2017 IBM Corporation and others.
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
package org.eclipse.compare.internal;

import org.eclipse.jface.util.Util;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

/**
 * A Canvas which reduces flicker by drawing in an off screen buffer.
 */
public abstract class BufferedCanvas extends Canvas {

	/** The drawable for double buffering */
	Image fBuffer;

	public BufferedCanvas(Composite parent, int flags) {
		super(parent, flags | SWT.NO_BACKGROUND);

		addPaintListener(
			event -> doubleBufferPaint(event.gc)
		);

		addDisposeListener(
			e -> {
				if (fBuffer != null) {
					fBuffer.dispose();
					fBuffer= null;
				}
			}
		);
	}

	public void repaint() {
		if (!isDisposed()) {
			GC gc= new GC(this);
			doubleBufferPaint(gc);
			gc.dispose();
			if (Util.isGtk()) {
				redraw();
			}
		}
	}

	/*
	 * Double buffer drawing.
	 */
	void doubleBufferPaint(GC dest) {

		Point size= getSize();

		if (size.x <= 1 || size.y <= 1) // we test for <= 1 because on X11 controls have initial size 1,1
			return;

		if (fBuffer != null) {
			Rectangle r= fBuffer.getBounds();
			if (r.width != size.x || r.height != size.y) {
				fBuffer.dispose();
				fBuffer= null;
			}
		}
		if (fBuffer == null)
			fBuffer= new Image(getDisplay(), size.x, size.y);

		GC gc= new GC(fBuffer);
		try {
			gc.setBackground(getBackground());
			gc.fillRectangle(0, 0, size.x, size.y);
			doPaint(gc);
		} finally {
			gc.dispose();
		}

		dest.drawImage(fBuffer, 0, 0);
	}

	abstract public void doPaint(GC gc);
}
