/*******************************************************************************
 * Copyright (c) 2005, 2017 IBM Corporation and others.
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
package org.eclipse.team.internal.ui.dialogs;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.window.IShellProvider;
import org.eclipse.swt.widgets.Shell;

/**
 * This class provides a Yes/No prompter that can be used for multiple questions
 * during the same operation. It can be used for a single prompt (in which case
 * OK and Cancel are presented) or multiple (in which case Yes, Yes to All, No
 * and No to All are presented). It uses the previous selection as appropriate.
 */
public class MultipleYesNoPrompter {
	private static final int ALWAYS_ASK = 0;
	private static final int YES_TO_ALL = 1;
	private static final int NO_TO_ALL = 2;
	private String[] buttons;
	private int confirmation = ALWAYS_ASK;
	private String title;
	private boolean hasMultiple;
	private boolean allOrNothing;
	private IShellProvider shellProvider;

	/**
	 * Prompt for the given resources using the specific condition. The prompt dialog will
	 * have the title specified.
	 */
	public MultipleYesNoPrompter(IShellProvider provider, String title,
			boolean hasMultiple, boolean allOrNothing) {
		this.title = title;
		this.shellProvider = provider;
		this.hasMultiple = hasMultiple;
		this.allOrNothing = allOrNothing;
		if (hasMultiple) {
			if (allOrNothing) {
				buttons = new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.CANCEL_LABEL};
			} else {
				buttons = new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.YES_TO_ALL_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.NO_TO_ALL_LABEL,
					IDialogConstants.CANCEL_LABEL};
			}
		} else {
			buttons = new String[] {
					IDialogConstants.YES_LABEL,
					IDialogConstants.NO_LABEL,
					IDialogConstants.CANCEL_LABEL
			};
		}
	}

	/**
	 * Return whether the given resource should be included in the
	 * target set.
	 * @param message the message
	 * @return whether the resource should be included
	 * @throws InterruptedException if the user choose to cancel
	 */
	public boolean shouldInclude(String message) throws InterruptedException {
		if (confirmation == YES_TO_ALL) {
			return true;
		} else {
			switch (confirmation) {
			case ALWAYS_ASK:
				// This call has the nasty side effect of changing the
				// instance scoped "confirmation"
				if (confirmOverwrite(message)) {
					return true;
				}
				return false;

			case YES_TO_ALL:
				return true;

			case NO_TO_ALL:
				// Don't overwrite
				return false;

			default:
				// If we get here, the user said no or not_to_all.
				return false;
			}
		}
	}

	/**
	 * Opens the confirmation dialog based on the prompt condition settings.
	 */
	private boolean confirmOverwrite(String msg) throws InterruptedException {
		Shell shell = shellProvider.getShell();
		if (shell == null) return false;
		final MessageDialog dialog =
			new MessageDialog(shell, title, null, msg, MessageDialog.QUESTION, buttons, 0);

		// run in syncExec because callback is from an operation,
		// which is probably not running in the UI thread.
		shell.getDisplay().syncExec(
			() -> dialog.open());
		if (hasMultiple) {
			switch (dialog.getReturnCode()) {
				case 0:// Yes
					return true;
				case 1:// Yes to all
					confirmation = YES_TO_ALL;
					return true;
				case 2:// No (or CANCEL for all-or-nothing)
					if (allOrNothing) {
						throw new InterruptedException();
					}
					return false;
				case 3:// No to all
					confirmation = NO_TO_ALL;
					return false;
				case 4:// Cancel
				default:
					throw new InterruptedException();
			}
		} else {
			switch (dialog.getReturnCode()) {
			case 0:// Yes
				return true;
			case 1:// No
				return false;
			case 2:// Cancel
			default:
				throw new InterruptedException();
			}
		}
	}

	public void setTitle(String title) {
		this.title = title;
	}

}
