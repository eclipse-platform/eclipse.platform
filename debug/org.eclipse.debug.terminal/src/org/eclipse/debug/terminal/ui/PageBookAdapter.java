/*******************************************************************************
 * Copyright (c) 2025 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.terminal.ui;

import org.eclipse.cdt.utils.spawner.Spawner;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.terminal.PtyRuntimeProcess;
import org.eclipse.ui.part.IPageBookViewPage;
import org.osgi.service.component.annotations.Component;

@Component
@AdapterTypes(adaptableClass = PtyRuntimeProcess.class, adapterNames = { IPageBookViewPage.class })
public class PageBookAdapter implements IAdapterFactory {

	@Override
	public <T> T getAdapter(Object adaptableObject, Class<T> adapterType) {
		if (adaptableObject instanceof PtyRuntimeProcess rt) {
			Spawner spawner = rt.getSpawner();
			if (spawner != null) {
				return adapterType.cast(new TerminalConsolePage(spawner, rt.getStreamsProxy()));
			}
		}
		return null;
	}

}
