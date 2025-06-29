package org.eclipse.terminal.control;

/**
 * Enum defines terminal title change requestors for
 * setTerminalTitle method.
 *
 */
public enum TerminalTitleRequestor {
	ANSI, // Terminal tab title change requested using ANSI command in terminal.
	MENU, // Terminal tab title change requested from menu.
	OTHER; // Terminal tab title change requested by other requestors.
}