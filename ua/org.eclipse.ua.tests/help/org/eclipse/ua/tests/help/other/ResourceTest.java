/*******************************************************************************
 * Copyright (c) 2008, 2016 IBM Corporation and others.
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

package org.eclipse.ua.tests.help.other;

import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;

import org.junit.Test;

public class ResourceTest {
	@Test
	public void testHelpUIResources() throws IllegalArgumentException, IllegalAccessException {
		checkFields(org.eclipse.help.ui.internal.Messages.class);
	}

	@Test
	public void testHelpBaseResources() throws IllegalArgumentException, IllegalAccessException {
		checkFields(org.eclipse.help.internal.base.HelpBaseResources.class);
	}

	@Test
	public void testCheatsheetResources() throws IllegalArgumentException, IllegalAccessException {
		checkFields(org.eclipse.ui.internal.cheatsheets.Messages.class);
	}

	@Test
	public void testIntroResources() throws IllegalArgumentException, IllegalAccessException {
		checkFields(org.eclipse.ui.internal.intro.impl.Messages.class);
	}

	@Test
	public void testUniversalIntroResources() throws IllegalArgumentException, IllegalAccessException {
		checkFields(org.eclipse.ui.internal.intro.universal.Messages.class);
	}

	private void checkFields(Class<?> messages) throws IllegalAccessException {
		Field[] fields = messages.getFields();
		for (Field field : fields) {
			int modifiers = field.getModifiers();
			if (Modifier.isPublic(modifiers) && Modifier.isStatic(modifiers)) {
				Object value = field.get(null);
				if (value instanceof String) {
					String stringValue = (String)value;
					if (stringValue.startsWith("NLS missing message")) {
						fail("Missing resource for " + field.getName());
					}
				}
			}
		}
	}

}
