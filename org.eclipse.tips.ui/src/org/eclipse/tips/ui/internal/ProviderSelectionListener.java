/*******************************************************************************
 * Copyright (c) 2018 Remain Software
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     wim.jongman@remainsoftware.com - initial API and implementation
 *******************************************************************************/
package org.eclipse.tips.ui.internal;

import org.eclipse.swt.widgets.Slider;
import org.eclipse.tips.core.TipProvider;

/**
 * Interface for TipProvider listeners.
 */
@FunctionalInterface
public interface ProviderSelectionListener {

	/**
	 * Is called when the provider is selected in the {@link Slider}.
	 *
	 * @param provider
	 *            the {@link TipProvider} that was selected
	 */
	void selected(TipProvider provider);
}