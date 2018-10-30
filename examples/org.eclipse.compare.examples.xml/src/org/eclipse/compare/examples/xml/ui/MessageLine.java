/*******************************************************************************
 * Copyright (c) 2000, 2005 IBM Corporation and others.
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
package org.eclipse.compare.examples.xml.ui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.RGB;
import org.eclipse.swt.widgets.Composite;

/**
 * A message line. It distinguishs between "normal" messages and errors. 
 * Setting an error message hides a currently displayed message until 
 * <code>clearErrorMessage</code> is called.
 */
public class MessageLine extends CLabel {

	public static final RGB RED= new RGB(200, 0, 0);
	private static RGB fgDefaultErrorRGB= RED;

	private String fMessageText;
	private String fErrorText;

	private Color fDefaultColor;
	private RGB fErrorRGB;
	private Color fErrorColor;

	/*
	 * Creates a new message line as a child of the given parent.
	 * Error message will be shown in <code>RED</code>.
	 */
	public MessageLine(Composite parent) {
		this(parent, SWT.LEFT);
	}

	/*
	 * Creates a new message line as a child of the parent and with the given SWT stylebits.
	 * Error message will be shown in  <code>RED</code>.
	 */
	public MessageLine(Composite parent, int style) {
		super(parent, style);
		fDefaultColor= getForeground();
		fErrorRGB= fgDefaultErrorRGB;
	}

	/*
	 * Creates a new message line as a child of the parent and with the given SWT stylebits.
	 * Error message will be shown with in the given rgb color.
	 */
	public MessageLine(Composite parent, int style, RGB errorRGB) {
		super(parent, style);
		fDefaultColor= getForeground();
		fErrorRGB= errorRGB;
	}

	/**
	 * Clears the currently displayed error message and redisplayes
	 * the message which was active before the error message was set.
	 */
	public void clearErrorMessage() {
		setErrorMessage(null);
	}

	/**
	 * Clears the currently displayed message.
	 */
	public void clearMessage() {
		setMessage(null);
	}

	/**
	 * Get the currently displayed error text.
	 * @return The error message. If no error message is displayed <code>null</code> is returned.
	 */
	public String getErrorMessage() {
		return fErrorText;
	}

	/**
	 * Get the currently displayed message.
	 * @return The message. If no message is displayed <code>null<code> is returned.
	 */
	public String getMessage() {
		return fMessageText;
	}

	/*
	 * Sets the default error color used by all message lines.
	 * Note: a call to this method only affects newly created MessageLines not existing ones. 
	 */
	public static void setDefaultErrorColor(RGB color) {
		fgDefaultErrorRGB= color;
	}

	/*
	 * Display the given error message. A currently displayed message
	 * is saved and will be redisplayed when the error message is cleared.
	 */
	public void setErrorMessage(String message) {
		fErrorText= message;

		if (message == null) {
			setMessage(fMessageText);
		} else {
			if (fErrorColor == null) {
				fErrorColor= new Color(getDisplay(), fErrorRGB);
				addDisposeListener(e -> fErrorColor.dispose());
			}
			setForeground(fErrorColor);
			setText(message);
		}
	}

	/*
	 * Set the message text. If the message line currently displays an error,
	 * the message is stored and will be shown after a call to clearErrorMessage
	 */
	public void setMessage(String message) {
		fMessageText= message;
		if (message == null)
			message= ""; //$NON-NLS-1$
		if (fErrorText == null) {
			setForeground(fDefaultColor);
			setText(message);
		}
	}

}
