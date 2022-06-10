/*******************************************************************************
 * Copyright (c) 2014 Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.nls;

/**
 * Consumer interface that is used to bind a method of an object (e.g. a SWT
 * control) to a field of a Messages instance. Typically the setter of a
 * localizable property will be called with the given value as parameter (e.g.
 * <code>label.setText(value)</code>)
 * <p>
 * This is a functional interface whose functional method is
 * {@link #accept(String)}.
 * </p>
 * <p>
 * When updating to Java 8 this interface can be removed and replaced with the
 * general <code>java.util.function.Consumer</code>
 * </p>
 *
 * @since 2.0
 *
 */
public interface MessageConsumer {

	/**
	 *
	 * @param value
	 *            The localization value that should be set to the control.
	 */
	void accept(String value);
}
