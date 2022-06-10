/*******************************************************************************
 *  Copyright (c) 2019 ArSysOp and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *      Alexander Fedorov <alexander.fedorov@arsysop.ru> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.services.about;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferencesService;
import org.eclipse.e4.core.services.about.AboutSections;
import org.eclipse.e4.core.services.about.ISystemInformation;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Reference;

@Component(service = { ISystemInformation.class }, property = { AboutSections.SECTION + '=' + AboutSections.SECTION_USER_PREFERENCES })
public class UserPreferences implements ISystemInformation {

	private IPreferencesService preferencesService;

	@Reference
	void bindPreferencesService(IPreferencesService service) {
		this.preferencesService = service;
	}

	void unbindPreferencesService() {
		this.preferencesService = null;
	}

	@Override
	public void append(PrintWriter writer) {
		ByteArrayOutputStream stm = new ByteArrayOutputStream();
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			IEclipsePreferences node = preferencesService.getRootNode();
			preferencesService.exportPreferences(node, stm, null);
			try (ByteArrayInputStream in = new ByteArrayInputStream(stm.toByteArray());
					BufferedReader reader = new BufferedReader(new InputStreamReader(in, "8859_1"))) {//$NON-NLS-1$
				char[] chars = new char[8192];
				while (true) {
					int read = reader.read(chars);
					if (read <= 0) {
						break;
					}
					writer.write(chars, 0, read);
				}
			}
		} catch (IOException | CoreException e) {
			writer.println(NLS.bind(AboutMessages.errorReadingPreferences, e));
		}
	}
}
