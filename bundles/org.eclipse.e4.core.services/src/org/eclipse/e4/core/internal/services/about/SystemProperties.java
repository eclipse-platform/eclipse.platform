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

import java.util.TreeMap;
import java.util.stream.Collectors;
import org.eclipse.e4.core.services.about.AboutSections;
import org.eclipse.e4.core.services.about.ISystemInformation;
import org.osgi.service.component.annotations.Component;

@Component(service = { ISystemInformation.class }, property = { AboutSections.SECTION + '=' + AboutSections.SECTION_SYSTEM_PROPERTIES })
public class SystemProperties extends PrintedMap {

	@Override
	protected TreeMap<String, String> source() {
		return new TreeMap<>(System.getProperties().entrySet().stream()
				.collect(Collectors.toMap(e -> String.valueOf(e.getKey()), e -> String.valueOf(e.getValue()))));
	}
}
