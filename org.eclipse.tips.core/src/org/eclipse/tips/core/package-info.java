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
/**
 * Tip Framework core API. A UI agnostic API for the Tip of the Day. Known
 * implementation is in package org.eclipse.tips.ui. If you run in the Eclipse
 * IDE you may use the tips extension point to register a new
 * {@link org.eclipse.tips.core.TipProvider}.If you want to run the Tip
 * framework outside of the IDE (e.g. in an RCP application) then you should
 * also implement your own {@link org.eclipse.tips.core.internal.TipManager} and start
 * that with some action or at startup of your application.
 */
package org.eclipse.tips.core;