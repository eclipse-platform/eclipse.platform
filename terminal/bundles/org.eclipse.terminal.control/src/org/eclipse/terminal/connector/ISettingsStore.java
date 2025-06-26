/*******************************************************************************
 * Copyright (c) 2006, 2018 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * Contributors:
 * Michael Scharf (Wind River) - initial API and implementation
 * Martin Oberhuber (Wind River) - fixed copyright headers and beautified
 *******************************************************************************/
package org.eclipse.terminal.connector;

/**
 * A simple interface to a store to persist the state of a connection.
 *
 */
public interface ISettingsStore {
	/**
	 * @param key alpha numeric key, may contain dots (.)
	 * @return value
	 */
	String get(String key);

	/**
	 * @param key alpha numeric key, may contain dots (.)
	 * @param defaultValue
	 * @return the value or the default
	 */
	String get(String key, String defaultValue);

	/**
	 * Save a string value
	 * @param key alpha numeric key, may contain dots (.)
	 * @param value
	 */
	void put(String key, String value);
}
