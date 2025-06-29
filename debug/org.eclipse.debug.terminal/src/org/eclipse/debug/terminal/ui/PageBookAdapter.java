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

import java.io.IOException;
import java.io.OutputStream;

import org.eclipse.cdt.utils.spawner.Spawner;
import org.eclipse.core.runtime.AdapterTypes;
import org.eclipse.core.runtime.IAdapterFactory;
import org.eclipse.debug.core.model.IBinaryStreamMonitor;
import org.eclipse.debug.core.model.IStreamMonitor;
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
				return adapterType.cast(new TerminalConsolePage(new ConsoleConnector(spawner), terminal -> {
					IStreamMonitor streamMonitor = rt.getStreamsProxy().getOutputStreamMonitor();
					if (streamMonitor instanceof IBinaryStreamMonitor bin) {
						OutputStream outputStream = terminal.getRemoteToTerminalOutputStream();
						bin.addBinaryListener((data, monitor) -> {
							try {
								outputStream.write(data);
							} catch (IOException e1) {
								e1.printStackTrace();
							}
						});
					}
				}));
			}
		}
		return null;
	}

}
