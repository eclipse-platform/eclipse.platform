/*******************************************************************************
 * Copyright (c) 2009, 2018 IBM Corporation and others.
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
package org.eclipse.compare.tests;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class ReflectionUtils {

	public static Object callMethod(Object object, String name, Object... args)
			throws IllegalArgumentException, IllegalAccessException,
			InvocationTargetException, NoSuchMethodException {
		Class<?> types[] = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			types[i] = args[i].getClass();
		}
		Method method = null;
		Class<?> clazz = object.getClass();
		NoSuchMethodException ex = null;
		while (method == null && clazz != null) {
			try {
				method = clazz.getDeclaredMethod(name, types);
			} catch (NoSuchMethodException e) {
				if (ex == null) {
					ex = e;
				}
				clazz = clazz.getSuperclass();
			}
		}
		if (method == null) {
			throw ex;
		}
		method.setAccessible(true);
		Object ret = method.invoke(object, args);
		return ret;
	}

	public static Object getField(Object object, String name)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException {
		Field field;
		try {
			field = object.getClass().getDeclaredField(name);
		} catch (NoSuchFieldException e) {
			field = object.getClass().getSuperclass().getDeclaredField(name);
		}
		field.setAccessible(true);
		Object ret = field.get(object);
		return ret;
	}

	public static Object getField(Object object, String name, boolean deep)
			throws IllegalArgumentException, IllegalAccessException,
			SecurityException, NoSuchFieldException {
		Class<?> clazz = object.getClass();
		NoSuchFieldException ex = null;
		while (clazz != null) {
			try {
				Field field = clazz.getDeclaredField(name);
				field.setAccessible(true);
				return field.get(object);
			} catch (NoSuchFieldException e) {
				if (ex == null) {
					ex = e;
				}
				if (!deep)
					break;
				clazz = clazz.getSuperclass();
			}
		}
		throw ex;
	}
}