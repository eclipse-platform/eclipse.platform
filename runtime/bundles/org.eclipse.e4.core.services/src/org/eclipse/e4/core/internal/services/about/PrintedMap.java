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

import java.io.PrintWriter;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.eclipse.e4.core.services.about.ISystemInformation;

abstract class PrintedMap implements ISystemInformation {

	@Override
	public void append(PrintWriter writer) {
		source().entrySet().stream().flatMap(this::mapEntry).forEach((String s) -> writer.println(s));
	}

	protected abstract TreeMap<String, String> source();

	private Stream<String> mapEntry(Entry<String, String> entry) {
		String key = entry.getKey();
		String value = entry.getValue();
		if (needsSecurity(key)) {
			return Stream.of(pair(key, secure(value)));
		} else {
			return Stream.of(plain(pair(key, value)));
		}
	}

	private boolean needsSecurity(String key) {
		return key.toUpperCase().contains("PASSWORD"); //$NON-NLS-1$
	}

	private String pair(String key, String value) {
		return String.format("%s=%s", key, value); //$NON-NLS-1$
	}
	private String secure(String raw) {
		return IntStream.range(0, raw.length()).mapToObj(i -> "*").collect(Collectors.joining()); //$NON-NLS-1$
	}

	private String[] plain(String raw) {
		return raw.split("\n"); //$NON-NLS-1$
	}
}