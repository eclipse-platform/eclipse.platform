/*******************************************************************************
 * Copyright (c) 2024 Vector Informatik GmbH and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.core.tests.harness.session.customization;

import static java.util.Comparator.reverseOrder;
import static org.eclipse.core.tests.harness.TestHarnessPlugin.PI_HARNESS;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

final class SessionCustomizationUtil {
	private SessionCustomizationUtil() {
	}

	static void deleteOnShutdownRecursively(Path path) {
		Runnable deleteDirectory = () -> {
			try {
				Files.walk(path) //
						.sorted(reverseOrder()) //
						.forEach(SessionCustomizationUtil::deleteSilently);
			} catch (IOException exception) {
				ILog.get().log(new Status(IStatus.WARNING, PI_HARNESS,
						"Error when removing test directory: " + path, exception));
			}
		};
		Runtime.getRuntime().addShutdownHook(new Thread(deleteDirectory));
	}

	private static void deleteSilently(Path path) {
		try {
			Files.delete(path);
		} catch (IOException exception) {
			ILog.get().log(new Status(IStatus.WARNING, PI_HARNESS,
					"Test file or directory could not be removed: " + path, exception));
		}
	}

}
