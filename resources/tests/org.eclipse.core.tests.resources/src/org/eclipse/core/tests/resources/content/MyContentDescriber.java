/*******************************************************************************
 * Copyright (c) 2004, 2015 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.tests.resources.content;

import static org.eclipse.core.tests.resources.AutomatedResourceTests.PI_RESOURCES_TESTS;

import java.io.IOException;
import java.io.InputStream;
import org.eclipse.core.runtime.QualifiedName;
import org.eclipse.core.runtime.content.IContentDescriber;
import org.eclipse.core.runtime.content.IContentDescription;

public class MyContentDescriber implements IContentDescriber {
	public static final Object[] MY_OPTION_VALUES = { "FOO", null, "BAR" };
	public static final String SIGNATURE = "MY_CONTENTS";

	public static final QualifiedName[] MY_OPTIONS = { new QualifiedName(PI_RESOURCES_TESTS, "my_option_1"),
			new QualifiedName(PI_RESOURCES_TESTS,
					"my_option_2"),
			new QualifiedName(PI_RESOURCES_TESTS, "my_option_3") };

	@Override
	public int describe(InputStream contents, IContentDescription description) throws IOException {
		byte[] signature = SIGNATURE.getBytes("US-ASCII");
		byte[] buffer = new byte[signature.length];
		if (contents.read(buffer) != buffer.length) {
			return INVALID;
		}
		for (int i = 0; i < signature.length; i++) {
			if (signature[i] != buffer[i]) {
				return INVALID;
			}
		}
		if (description == null) {
			return VALID;
		}
		for (int i = 0; i < MY_OPTIONS.length; i++) {
			setIfRequested(description, i);
		}
		return VALID;
	}

	@Override
	public QualifiedName[] getSupportedOptions() {
		return MY_OPTIONS;
	}

	private void setIfRequested(IContentDescription description, int option) {
		if (description.isRequested(MY_OPTIONS[option])) {
			description.setProperty(MY_OPTIONS[option], MY_OPTION_VALUES[option]);
		}
	}
}
