/*******************************************************************************
 * Copyright (c) 2000, 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.update.ui.forms.internal;

import org.eclipse.swt.graphics.*;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.*;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.*;
import org.eclipse.swt.events.*;
import org.eclipse.jface.resource.*;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.swt.custom.*;

/**
 * This form implementation assumes that it contains
 * children that do not have independent dimensions.
 * In other words, these widgets are not capable
 * of answering their preferred size. Instead,
 * desired width must be supplied to get the
 * preferred height. These forms are layed out
 * top to bottom, left to right and use
 * a layout algorithm very similar to
 * HTML tables. Scrolling is not optional
 * for this type of presentation - 
 * scroll bars will show up when needed.
 */

public class WebForm extends AbstractSectionForm {
	protected ScrolledComposite scrollComposite;
	private Composite control;
	private Composite client;
	private final static int HMARGIN = 5;
	private final static int VMARGIN = 5;
	private Image headingUnderlineImage;

	class WebFormLayout extends Layout {

		protected void layout(Composite parent, boolean changed) {
			Rectangle bounds = parent.getClientArea();
			int x = 0;
			int y = 0;
			if (isHeadingVisible()) {
				y = getHeadingHeight(parent);
			}
			Point csize;

			Layout layout = client.getLayout();
			if (layout != null && layout instanceof HTMLTableLayout) {
				HTMLTableLayout hlayout = (HTMLTableLayout) layout;
				csize = hlayout.computeSize(client, bounds.width, SWT.DEFAULT, true);
				if (csize.x < bounds.width)
					csize.x = bounds.width;
				Rectangle trim = control.computeTrim(0, 0, csize.x, csize.y);
				csize = new Point(trim.width, trim.height);
			} else {
				csize = client.computeSize(bounds.width, SWT.DEFAULT, changed);
			}
			client.setBounds(x, y, csize.x, csize.y);
		}

		protected Point computeSize(
			Composite parent,
			int wHint,
			int hHint,
			boolean changed) {
			int width = wHint;
			int height = 0;
			if (isHeadingVisible()) {
				height = getHeadingHeight(parent);
			}
			Point csize;
			Layout layout = client.getLayout();

			if (layout != null && layout instanceof HTMLTableLayout) {
				HTMLTableLayout hlayout = (HTMLTableLayout) layout;
				csize = hlayout.computeSize(client, width, SWT.DEFAULT, true);
				if (csize.x < width)
					csize.x = width;
				Rectangle trim = control.computeTrim(0, 0, csize.x, csize.y);
				csize = new Point(trim.width, trim.height);
			} else {
				csize = client.computeSize(width, SWT.DEFAULT, changed);
			}
			width = csize.x;
			height += csize.y;
			return new Point(width, height);
		}
	}

	public WebForm() {
	}

	public Control createControl(Composite parent) {
		scrollComposite = new ScrolledComposite(parent, SWT.V_SCROLL | SWT.H_SCROLL);
		scrollComposite.setBackground(factory.getBackgroundColor());
		scrollComposite.setMenu(parent.getMenu());
		final Composite form = factory.createComposite(scrollComposite);

		scrollComposite.setContent(form);
		scrollComposite.addListener(SWT.Resize, new Listener() {
			public void handleEvent(Event e) {
				updateSize();
			}
		});
		WebFormLayout layout = new WebFormLayout();
		form.setLayout(layout);
		form.addPaintListener(new PaintListener() {
			public void paintControl(PaintEvent e) {
				paint(e);
			}
		});
		this.control = form;
		client = factory.createComposite(form);
		createContents(client);
		initializeScrollBars(scrollComposite);
		//form.setFocus();
		return scrollComposite;
	}
	private void initializeScrollBars(ScrolledComposite scomp) {
		ScrollBar hbar = scomp.getHorizontalBar();
		if (hbar != null) {
			hbar.setIncrement(H_SCROLL_INCREMENT);
		}
		ScrollBar vbar = scomp.getVerticalBar();
		if (vbar != null) {
			vbar.setIncrement(V_SCROLL_INCREMENT);
		}
		updatePageIncrement(scomp);
	}

	public int getHeadingHeight(Composite parent) {
		int width = parent.getSize().x;
		int height = 0;
		int imageHeight = 0;
		if (getHeadingImage() != null) {
			Rectangle ibounds = getHeadingImage().getBounds();
			imageHeight = ibounds.height;
		}
		GC gc = new GC(parent);
		gc.setFont(titleFont);
		int textWidth = width - 2 * HMARGIN;
		height = FormLabel.computeWrapHeight(gc, getHeadingText(), textWidth);
		height += 2 * VMARGIN;
		height = Math.max(height, imageHeight);
		if (headingUnderlineImage != null) {
			Rectangle ibounds = headingUnderlineImage.getBounds();
			height += ibounds.height;
		}
		gc.dispose();
		return height;
	}

	protected void createContents(Composite parent) {
	}

	public Control getControl() {
		return control;
	}

	public void setHeadingVisible(boolean newHeadingVisible) {
		super.setHeadingVisible(newHeadingVisible);
		if (control != null)
			control.layout();
	}

	public Image getHeadingUnderlineImage() {
		return headingUnderlineImage;
	}

	public void setHeadingUnderlineImage(Image image) {
		this.headingUnderlineImage = image;
	}

	public void propertyChange(PropertyChangeEvent event) {
		titleFont = JFaceResources.getHeaderFont();
		if (control != null && !control.isDisposed()) {
			control.layout();
		}
	}
	
	protected void updateHyperlinkColors() {
		factory.updateHyperlinkColors();
		if (control != null && !control.isDisposed()) {
			control.redraw();
		}
	}

	public void updateSize() {
		Rectangle ssize = scrollComposite.getClientArea();
		int swidth = ssize.width;
		WebFormLayout layout = (WebFormLayout) control.getLayout();
		Point size = layout.computeSize(control, swidth, SWT.DEFAULT, true);
		if (size.x < swidth)
			size.x = swidth;
		Rectangle trim = control.computeTrim(0, 0, size.x, size.y);
		size = new Point(trim.width, trim.height);
		control.setSize(size);
		updatePageIncrement(scrollComposite);
	}

	private void paint(PaintEvent e) {
		if (isHeadingVisible() == false)
			return;
		GC gc = e.gc;
		if (headingImage != null) {
			gc.drawImage(headingImage, 0, 0);
		}
		Point size = control.getSize();
		if (getHeadingBackground() != null)
			gc.setBackground(getHeadingBackground());
		if (getHeadingForeground() != null)
			gc.setForeground(getHeadingForeground());
		gc.setFont(titleFont);
		FormLabel.paintWrapText(gc, size, getHeadingText(), HMARGIN, VMARGIN);
		if (headingUnderlineImage != null) {
			int y =
				getHeadingHeight(control)
					- headingUnderlineImage.getBounds().height;
			gc.drawImage(headingUnderlineImage, 0, y);
		}
	}
	public void setHeadingText(String headingText) {
		super.setHeadingText(headingText);
		if (control!=null)
			control.redraw();
	}

}
