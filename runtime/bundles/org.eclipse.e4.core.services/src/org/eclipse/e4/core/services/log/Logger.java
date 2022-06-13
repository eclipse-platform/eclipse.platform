/*******************************************************************************
 * Copyright (c) 2009, 2010 IBM Corporation and others.
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
package org.eclipse.e4.core.services.log;

import org.eclipse.e4.core.services.statusreporter.StatusReporter;

/**
 * Logging warnings, errors, information, as well as capturing debug and trace information.
 * Everything done through this interface is not meant for normal end users. Strings are not
 * expected to be translated.
 *
 * @see StatusReporter
 */
public abstract class Logger {
	public abstract boolean isErrorEnabled();

	public abstract void error(Throwable t, String message);

	public abstract boolean isWarnEnabled();

	public abstract void warn(Throwable t, String message);

	public abstract boolean isInfoEnabled();

	public abstract void info(Throwable t, String message);

	public abstract boolean isTraceEnabled();

	public abstract void trace(Throwable t, String message);

	public abstract boolean isDebugEnabled();

	public abstract void debug(Throwable t);

	public abstract void debug(Throwable t, String message);

	public void debug(String message) {
		debug((Throwable) null, message);
	}

	public void debug(String format, Object arg) {
		debug(internalBind(format, null, String.valueOf(arg), null));
	}

	public void debug(String format, Object arg1, Object arg2) {
		debug(internalBind(format, null, String.valueOf(arg1), String.valueOf(arg2)));
	}

	public void debug(String format, Object[] args) {
		debug(internalBind(format, args, null, null));
	}

	public void error(Throwable t) {
		error(t, null);
	}

	public void error(String message) {
		error((Throwable) null, message);
	}

	public void error(String format, Object arg) {
		error(internalBind(format, null, String.valueOf(arg), null));
	}

	public void error(String format, Object arg1, Object arg2) {
		error(internalBind(format, null, String.valueOf(arg1), String.valueOf(arg2)));
	}

	public void error(String format, Object[] args) {
		error(internalBind(format, args, null, null));
	}

	public void info(Throwable t) {
		info(t, null);
	}

	public void info(String message) {
		info((Throwable) null, message);
	}

	public void info(String format, Object arg) {
		info(internalBind(format, null, String.valueOf(arg), null));
	}

	public void info(String format, Object arg1, Object arg2) {
		info(internalBind(format, null, String.valueOf(arg1), String.valueOf(arg2)));
	}

	public void info(String format, Object[] args) {
		info(internalBind(format, args, null, null));
	}

	public void trace(Throwable t) {
		trace(t, null);
	}

	public void trace(String message) {
		trace((Throwable) null, message);
	}

	public void trace(String format, Object arg) {
		trace(internalBind(format, null, String.valueOf(arg), null));
	}

	public void trace(String format, Object arg1, Object arg2) {
		trace(internalBind(format, null, String.valueOf(arg1), String.valueOf(arg2)));
	}

	public void trace(String format, Object[] args) {
		trace(internalBind(format, args, null, null));
	}

	public void warn(Throwable t) {
		warn(t, null);
	}

	public void warn(String message) {
		warn((Throwable) null, message);
	}

	public void warn(String format, Object arg) {
		warn(internalBind(format, null, String.valueOf(arg), null));
	}

	public void warn(String format, Object arg1, Object arg2) {
		warn(internalBind(format, null, String.valueOf(arg1), String.valueOf(arg2)));
	}

	public void warn(String format, Object[] args) {
		warn(internalBind(format, args, null, null));
	}

	private static final Object[] EMPTY_ARGS = new Object[0];

	/*
	 * Perform the string substitution on the given message with the specified args. See the class
	 * comment for exact details.
	 */
	private static String internalBind(String message, Object[] args, String argZero, String argOne) {
		if (message == null)
			return "No message available."; //$NON-NLS-1$
		if (args == null || args.length == 0)
			args = EMPTY_ARGS;

		int length = message.length();
		// estimate correct size of string buffer to avoid growth
		int bufLen = length + (args.length * 5);
		if (argZero != null)
			bufLen += argZero.length() - 3;
		if (argOne != null)
			bufLen += argOne.length() - 3;
		StringBuilder buffer = new StringBuilder(bufLen < 0 ? 0 : bufLen);
		for (int i = 0; i < length; i++) {
			char c = message.charAt(i);
			switch (c) {
			case '{':
				int index = message.indexOf('}', i);
				// if we don't have a matching closing brace then...
				if (index == -1) {
					buffer.append(c);
					break;
				}
				i++;
				if (i >= length) {
					buffer.append(c);
					break;
				}
				// look for a substitution
				int number = -1;
				try {
					number = Integer.parseInt(message.substring(i, index));
				} catch (NumberFormatException e) {
					throw new IllegalArgumentException();
				}
				if (number == 0 && argZero != null)
					buffer.append(argZero);
				else if (number == 1 && argOne != null)
					buffer.append(argOne);
				else {
					if (number >= args.length || number < 0) {
						buffer.append("<missing argument>"); //$NON-NLS-1$
						i = index;
						break;
					}
					buffer.append(args[number]);
				}
				i = index;
				break;
			case '\'':
				// if a single quote is the last char on the line then skip it
				int nextIndex = i + 1;
				if (nextIndex >= length) {
					buffer.append(c);
					break;
				}
				char next = message.charAt(nextIndex);
				// if the next char is another single quote then write out one
				if (next == '\'') {
					i++;
					buffer.append(c);
					break;
				}
				// otherwise we want to read until we get to the next single
				// quote
				index = message.indexOf('\'', nextIndex);
				// if there are no more in the string, then skip it
				if (index == -1) {
					buffer.append(c);
					break;
				}
				// otherwise write out the chars inside the quotes
				buffer.append(message.substring(nextIndex, index));
				i = index;
				break;
			default:
				buffer.append(c);
			}
		}
		return buffer.toString();
	}
}
