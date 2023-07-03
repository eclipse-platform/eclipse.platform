/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
package org.eclipse.ui.console.actions;


import java.text.MessageFormat;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.internal.console.ConsoleMessages;


/**
 * Action to position a text viewer to a specific line.
 * <p>
 * Clients may instantiate this class.
 * </p>
 * @since 3.0
 * @noextend This class is not intended to be subclassed by clients.
 */
public class TextViewerGotoLineAction extends TextViewerAction {

	/**
	 * Validates whether the text found in the input field of the
	 * dialog forms a valid line number, i.e. one to which can be
	 * jumped.
	 */
	class NumberValidator implements IInputValidator {

		@Override
		public String isValid(String input) {
			try {
				int i= Integer.parseInt(input);
				if (i <= 0 || fLastLine < i) {
					return ConsoleMessages.TextViewerGotoLineAction_Line_number_out_of_range_1;
				}

			} catch (NumberFormatException x) {
				return ConsoleMessages.TextViewerGotoLineAction_Not_a_number_2;
			}

			return null;
		}
	}

	protected int fLastLine;
	protected ITextViewer fTextViewer;

	/**
	 * Constructs a goto line action for the viewer using the provided resource
	 * bundle.
	 *
	 * @param viewer the viewer to jump in
	 */
	public TextViewerGotoLineAction(ITextViewer viewer) {
		super(viewer, -1);
		fTextViewer= viewer;
		setText(ConsoleMessages.TextViewerGotoLineAction_Go_to__Line____Ctrl_L_4);
		setToolTipText(ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1);
		setDescription(ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1);
	}

	/**
	 * @see TextViewerAction#update()
	 */
	@Override
	public void update() {
	}

	/**
	 * Jumps to the line.
	 *
	 * @param line the target line. First line is line <code>0</code>.
	 */
	protected void gotoLine(int line) {

		IDocument document= fTextViewer.getDocument();
		try {
			int start= document.getLineOffset(line);
			int length= document.getLineLength(line);
			fTextViewer.getTextWidget().setSelection(start, start + length);
			fTextViewer.revealRange(start, length);
		} catch (BadLocationException x) {
			ConsolePlugin.errorDialog(fTextViewer.getTextWidget().getShell(), ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1, ConsoleMessages.TextViewerGotoLineAction_Exceptions_occurred_attempt_to_go_to_line_2, x); //
		}
	}

	@Override
	public void run() {
		try {
			Point selection= fTextViewer.getTextWidget().getSelection();
			IDocument document= fTextViewer.getDocument();
			fLastLine= document.getLineOfOffset(document.getLength()) + 1;
			int startLine= selection == null ? 1 : fTextViewer.getTextWidget().getLineAtOffset(selection.x) + 1;
			String title= ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1;
			String message= MessageFormat.format(ConsoleMessages.TextViewerGotoLineAction_Enter_line_number__8, new Object[] {Integer.valueOf(fLastLine)});
			String value= Integer.toString(startLine);
			Shell activeShell= fTextViewer.getTextWidget().getShell();
			InputDialog d= new InputDialog(activeShell, title, message, value, new NumberValidator());
			if (d.open() == Window.OK) {
				try {
					int line= Integer.parseInt(d.getValue());
					gotoLine(line - 1);
				} catch (NumberFormatException x) {
					ConsolePlugin.errorDialog(activeShell, ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1, ConsoleMessages.TextViewerGotoLineAction_Exceptions_occurred_attempt_to_go_to_line_2, x); //
				}
			}
		} catch (BadLocationException x) {
			ConsolePlugin.errorDialog(fTextViewer.getTextWidget().getShell(), ConsoleMessages.TextViewerGotoLineAction_Go_To_Line_1, ConsoleMessages.TextViewerGotoLineAction_Exceptions_occurred_attempt_to_go_to_line_2, x); //
			return;
		}
	}
}
