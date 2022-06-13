/*******************************************************************************
 * Copyright (c) 2013 Dirk Fauth and others.
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

import java.util.Locale;
import org.eclipse.e4.core.contexts.ContextFunction;

/**
 * Service that is used to change the {@link Locale} at runtime.
 * <p>
 * <b>Note:</b> This is not an OSGi service! It is created by a {@link ContextFunction}.
 * </p>
 *
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 * @since 1.2
 */
public interface ILocaleChangeService {

	/**
	 * Base name for all locale change events.
	 */
	String TOPIC = "org/eclipse/e4/core/NLS"; //$NON-NLS-1$
	/**
	 * Locale change event: event that gets fired on Locale changes.
	 */
	String LOCALE_CHANGE = TOPIC + "/" + "LOCALE_CHANGE"; //$NON-NLS-1$ //$NON-NLS-2$

	/**
	 * Change the {@link Locale} in the application context.
	 *
	 * @param locale
	 *            The {@link Locale} to set to the application context.
	 */
	void changeApplicationLocale(Locale locale);

	/**
	 * Change the {@link Locale} in the application context.
	 * <p>
	 * If the given String is not a valid {@link Locale} String representation, there will be no
	 * locale change performed.
	 *
	 * @param localeString
	 *            The String representation of the {@link Locale} to set to the application context.
	 */
	void changeApplicationLocale(String localeString);
}
