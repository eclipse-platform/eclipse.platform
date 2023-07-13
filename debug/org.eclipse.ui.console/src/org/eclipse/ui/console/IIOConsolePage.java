package org.eclipse.ui.console;

import org.eclipse.ui.part.IPageBookViewPage;

/**
 * An IO console page appears in a page book and can be used to enable command
 * line history for the console.
 *
 * @since 3.13
 */
public interface IIOConsolePage extends IPageBookViewPage {
	/**
	 * Enable command line history. Command lines (lines entered by the user) will
	 * be collected and maintained in a history. Ctrl-up arrow and ctrl-down arrow
	 * can be used to move through the history. The history line will be shown as
	 * user input.
	 * <p>
	 * Generally called from an {@link IConsolePageParticipant}
	 *
	 * @param enable enable command line history.
	 */
	void setEnableCommandLineHistory(boolean enable);
}
