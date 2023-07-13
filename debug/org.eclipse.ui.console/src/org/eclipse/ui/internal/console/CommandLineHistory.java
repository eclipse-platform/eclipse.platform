package org.eclipse.ui.internal.console;

import java.util.ArrayList;
import java.util.List;

/**
 * Maintains a history of ProcessConsole commands entered by the user.
 */
public class CommandLineHistory {
	/** The list of commands that have been executed. (no nulls) */
	private List<String> _history = new ArrayList<>();

	/**
	 * The currently selected command in the list. Always >= 0 and <=
	 * _commandHistory.size().
	 */
	private int _currentIndex = 0;

	public void addCommand(String command) {
		assert command != null;
		synchronized (this) {
			_history.add(command.replaceFirst("^(.*)(\r|\n|\r\n)$", "$1")); //$NON-NLS-1$ //$NON-NLS-2$
			_currentIndex = _history.size();
		}
	}

	/**
	 * Retreat to the previous command in the given document's command history and
	 * return it. Returns an empty string if the history is empty. Does not retreat
	 * if the current command is the first in the history.
	 *
	 * @return The previous command in the history.
	 */
	public String prevCommand() {
		synchronized (this) {
			if (_currentIndex > 0) {
				--_currentIndex;
			}
			return currentCommand();
		}
	}

	/**
	 * Advance (if possible) to the next command in the given document's command
	 * history and return it. Returns an empty string if the history is empty. Can
	 * advance to at most one position past the last command in the history (which
	 * will result in an empty string).
	 *
	 * @return The next command in the history.
	 */
	public String nextCommand() {
		synchronized (this) {
			if (_currentIndex < _history.size()) {
				++_currentIndex;
			}
			return currentCommand();
		}
	}

	/**
	 * @return Return the current command if it's within the history, or an empty
	 *         string otherwise.
	 */
	private String currentCommand() {
		return (_currentIndex < _history.size()) ? _history.get(_currentIndex) : ""; //$NON-NLS-1$
	}
}
