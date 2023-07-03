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
package org.eclipse.tips.core.internal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.osgi.framework.FrameworkUtil;

public class LogUtil {

	public static IStatus getStatus(int severity, String bundleId, String message, Throwable throwable) {
		return new Status(severity, bundleId, message, throwable);
	}

	public static IStatus error(Throwable throwable) {
		return getStatus(IStatus.ERROR, "org.eclipse.tips.core", throwable.getMessage(), throwable); //$NON-NLS-1$
	}

	public static IStatus warn(Throwable throwable) {
		return getStatus(IStatus.WARNING, "org.eclipse.tips.core", throwable.getMessage(), throwable); //$NON-NLS-1$
	}

	public static IStatus info(Throwable throwable) {
		return getStatus(IStatus.INFO, "org.eclipse.tips.core", throwable.getMessage(), throwable); //$NON-NLS-1$
	}

	public static IStatus error(Class<?> clazz, Throwable throwable) {
		return getStatus(IStatus.ERROR, getBundleId(clazz), throwable.getMessage(), throwable);
	}

	public static IStatus warn(Class<?> clazz, Throwable throwable) {
		return getStatus(IStatus.WARNING, getBundleId(clazz), throwable.getMessage(), throwable);
	}

	public static IStatus info(Class<?> clazz, Throwable throwable) {
		return getStatus(IStatus.INFO, getBundleId(clazz), throwable.getMessage(), throwable);
	}

	public static IStatus info(Class<?> clazz, String message) {
		return getStatus(IStatus.INFO, getBundleId(clazz), message, null);
	}

	public static IStatus warn(Class<?> clazz, String pessage) {
		return getStatus(IStatus.WARNING, getBundleId(clazz), pessage, null);
	}

	public static IStatus error(Class<?> clazz, String message) {
		return getStatus(IStatus.ERROR, getBundleId(clazz), message, null);
	}

	private static String getBundleId(Class<?> clazz) {
		if (FrameworkUtil.getBundle(clazz) != null) {
			return FrameworkUtil.getBundle(clazz).getSymbolicName();
		}
		return "osgi.not.running"; //$NON-NLS-1$
	}

	public static IStatus info(String message) {
		return getStatus(IStatus.INFO, "org.eclipse.tips.core", message, null); //$NON-NLS-1$
	}
}