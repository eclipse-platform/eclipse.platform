/********************************************************************************
 * Copyright (c) 2023 Christoph Läubrich and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Christoph Läubrich - initial API and implementation
 ********************************************************************************/
package org.eclipse.core.internal.resources.refresh.linux;

import org.eclipse.core.resources.refresh.RefreshProvider;

/**
 * The default implementation simply do nothing what will result in fallback to
 * the plain polling
 */
public class LinuxRefreshProvider extends RefreshProvider {

}
