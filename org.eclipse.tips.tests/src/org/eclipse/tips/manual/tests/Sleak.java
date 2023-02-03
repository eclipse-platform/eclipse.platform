/*******************************************************************************
 * Copyright (c) 2000, 2023 IBM Corp.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.tips.manual.tests;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Cursor;
import org.eclipse.swt.graphics.DeviceData;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.graphics.Region;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.List;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;

public class Sleak {
	Display display;
	Shell shell;
	List list;
	Canvas canvas;
	Button start, stop, check;
	Text text;
	Label label;

	Object[] oldObjects = new Object[0];
	Error[] oldErrors = new Error[0];
	Object[] objects = new Object[0];
	Error[] errors = new Error[0];

	public void open() {
		display = Display.getCurrent();
		shell = new Shell(display);
		shell.setText("S-Leak");
		list = new List(shell, SWT.BORDER | SWT.V_SCROLL);
		list.addListener(SWT.Selection, event -> refreshObject());
		text = new Text(shell, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
		canvas = new Canvas(shell, SWT.BORDER);
		canvas.addListener(SWT.Paint, this::paintCanvas);
		check = new Button(shell, SWT.CHECK);
		check.setText("Stack");
		check.addListener(SWT.Selection, e -> toggleStackTrace());
		start = new Button(shell, SWT.PUSH);
		start.setText("Snap");
		start.addListener(SWT.Selection, event -> refreshAll());
		stop = new Button(shell, SWT.PUSH);
		stop.setText("Diff");
		stop.addListener(SWT.Selection, event -> refreshDifference());
		label = new Label(shell, SWT.BORDER);
		label.setText("0 object(s)");
		shell.addListener(SWT.Resize, e -> layout());
		check.setSelection(false);
		text.setVisible(false);
		Point size = shell.getSize();
		shell.setSize(size.x / 2, size.y / 2);
		shell.open();
		refreshAll();
	}

	void refreshLabel() {
		int colors = 0, cursors = 0, fonts = 0, gcs = 0, images = 0;
		for (Object object : objects) {
			if (object instanceof Color) {
				colors++;
			}
			if (object instanceof Cursor) {
				cursors++;
			}
			if (object instanceof Font) {
				fonts++;
			}
			if (object instanceof GC) {
				gcs++;
			}
			if (object instanceof Image) {
				images++;
			}
		}
		String string = "";
		if (colors != 0) {
			string += colors + " Color(s)\n";
		}
		if (cursors != 0) {
			string += cursors + " Cursor(s)\n";
		}
		if (fonts != 0) {
			string += fonts + " Font(s)\n";
		}
		if (gcs != 0) {
			string += gcs + " GC(s)\n";
		}
		if (images != 0) {
			string += images + " Image(s)\n";
		}
		/* Currently regions are not counted. */
		// if (regions != 0) string += regions + " Region(s)\n";
		if (string.length() != 0) {
			string = string.substring(0, string.length() - 1);
		}
		label.setText(string);
	}

	void refreshDifference() {
		DeviceData info = display.getDeviceData();
		if (!info.tracking) {
			MessageBox dialog = new MessageBox(shell, SWT.ICON_WARNING | SWT.OK);
			dialog.setText(shell.getText());
			dialog.setMessage("Warning: Device is not tracking resource allocation");
			dialog.open();
		}
		Object[] newObjects = info.objects;
		Error[] newErrors = info.errors;
		Object[] diffObjects = new Object[newObjects.length];
		Error[] diffErrors = new Error[newErrors.length];
		int count = 0;
		for (int i = 0; i < newObjects.length; i++) {
			int index = 0;
			while (index < oldObjects.length) {
				if (newObjects[i] == oldObjects[index]) {
					break;
				}
				index++;
			}
			if (index == oldObjects.length) {
				diffObjects[count] = newObjects[i];
				diffErrors[count] = newErrors[i];
				count++;
			}
		}
		objects = new Object[count];
		errors = new Error[count];
		System.arraycopy(diffObjects, 0, objects, 0, count);
		System.arraycopy(diffErrors, 0, errors, 0, count);
		list.removeAll();
		text.setText("");
		canvas.redraw();
		for (Object object : objects) {
			list.add(objectName(object));
		}
		refreshLabel();
		layout();
	}

	String objectName(Object object) {
		String string = object.toString();
		int index = string.lastIndexOf('.');
		if (index == -1) {
			return string;
		}
		return string.substring(index + 1);
	}

	void toggleStackTrace() {
		refreshObject();
		layout();
	}

	void paintCanvas(Event event) {
		canvas.setCursor(null);
		int index = list.getSelectionIndex();
		if (index == -1) {
			return;
		}
		GC gc = event.gc;
		Object object = objects[index];
		if (object instanceof Color color) {
			if (color.isDisposed()) {
				return;
			}
			gc.setBackground(color);
			gc.fillRectangle(canvas.getClientArea());
			return;
		}
		if (object instanceof Cursor cursor) {
			if (cursor.isDisposed()) {
				return;
			}
			canvas.setCursor(cursor);
			return;
		}
		if (object instanceof Font font) {
			if (font.isDisposed()) {
				return;
			}
			gc.setFont(font);
			FontData[] array = gc.getFont().getFontData();
			String string = "";
			String lf = text.getLineDelimiter();
			for (FontData data : array) {
				String style = "NORMAL";
				int bits = data.getStyle();
				if (bits != 0) {
					if ((bits & SWT.BOLD) != 0) {
						style = "BOLD ";
					}
					if ((bits & SWT.ITALIC) != 0) {
						style += "ITALIC";
					}
				}
				string += data.getName() + " " + data.getHeight() + " " + style + lf;
			}
			gc.drawString(string, 0, 0);
			return;
		}
		// NOTHING TO DRAW FOR GC
		// if (object instanceof GC) {
		// return;
		// }
		if (object instanceof Image image) {
			if (image.isDisposed()) {
				return;
			}
			gc.drawImage(image, 0, 0);
			return;
		}
		if (object instanceof Region region) {
			if (region.isDisposed()) {
				return;
			}
			String string = region.getBounds().toString();
			gc.drawString(string, 0, 0);
			return;
		}
	}

	void refreshObject() {
		int index = list.getSelectionIndex();
		if (index == -1) {
			return;
		}
		if (check.getSelection()) {
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
			PrintStream s = new PrintStream(stream);
			errors[index].printStackTrace(s);
			text.setText(stream.toString());
			text.setVisible(true);
			canvas.setVisible(false);
		} else {
			canvas.setVisible(true);
			text.setVisible(false);
			canvas.redraw();
		}
	}

	void refreshAll() {
		oldObjects = new Object[0];
		oldErrors = new Error[0];
		refreshDifference();
		oldObjects = objects;
		oldErrors = errors;
	}

	void layout() {
		Rectangle rect = shell.getClientArea();
		int width = 0;
		String[] items = list.getItems();
		GC gc = new GC(list);
		for (int i = 0; i < objects.length; i++) {
			width = Math.max(width, gc.stringExtent(items[i]).x);
		}
		gc.dispose();
		Point size1 = start.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size2 = stop.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size3 = check.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		Point size4 = label.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		width = Math.max(size1.x, Math.max(size2.x, Math.max(size3.x, width)));
		width = Math.max(64, Math.max(size4.x, list.computeSize(width, SWT.DEFAULT).x));
		start.setBounds(0, 0, width, size1.y);
		stop.setBounds(0, size1.y, width, size2.y);
		check.setBounds(0, size1.y + size2.y, width, size3.y);
		label.setBounds(0, rect.height - size4.y, width, size4.y);
		int height = size1.y + size2.y + size3.y;
		list.setBounds(0, height, width, rect.height - height - size4.y);
		text.setBounds(width, 0, rect.width - width, rect.height);
		canvas.setBounds(width, 0, rect.width - width, rect.height);
	}

	public static void main(String[] args) {
		Display display = new Display();
		Sleak sleak = new Sleak();
		sleak.open();
		while (!sleak.shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
		display.dispose();
	}

}
