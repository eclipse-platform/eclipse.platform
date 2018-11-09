/*******************************************************************************
 * Copyright (c) 2009, 2011 IBM Corporation and others.
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
package org.eclipse.team.tests.ccvs.ui;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.eclipse.core.runtime.Assert;
import org.eclipse.team.tests.ccvs.core.EclipseTest;

public class ReflectionUtils {

	public static Object construct(String className, ClassLoader classLoader,
			Class<?>[] constructorTypes, Object[] constructorArgs) {
		Class<?> clazz = null;
		try {
			clazz = Class.forName(className, true, classLoader);
		} catch (ClassNotFoundException e) {
			EclipseTest.fail(e.getMessage());
		} catch (ExceptionInInitializerError e) {
			EclipseTest.fail(e.getMessage());
		}
		Constructor<?> constructor = null;
		try {
			constructor = clazz.getDeclaredConstructor(constructorTypes);
		} catch (SecurityException e) {
			EclipseTest.fail(e.getMessage());
		} catch (NoSuchMethodException e) {
			EclipseTest.fail(e.getMessage());
		}
		Assert.isNotNull(constructor);
		constructor.setAccessible(true);
		try {
			return constructor.newInstance(constructorArgs);
		} catch (IllegalArgumentException | InvocationTargetException | InstantiationException | IllegalAccessException e) {
			EclipseTest.fail(e.getMessage());
		}
		return null;
	}

	public static Object callMethod(Object object, String name, Class<?> types[],
			Object args[]) {
		try {
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
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchMethodException | InvocationTargetException e) {
			EclipseTest.fail(e.getMessage());
		}
		return null;
	}

	public static Object callMethod(Object object, String name, Object args[]) {
		Class<?> types[] = new Class[args.length];
		for (int i = 0; i < args.length; i++) {
			types[i] = args[i].getClass();
		}
		return callMethod(object, name, types, args);
	}

	public static Object getField(Object object, String name) {
		try {
			Field field = null;
			Class<?> clazz = object.getClass();
			NoSuchFieldException ex = null;
			while (field == null && clazz != null) {
				try {
					field = clazz.getDeclaredField(name);
				} catch (NoSuchFieldException e) {
					if (ex == null) {
						ex = e;
					}
					clazz = clazz.getSuperclass();
				}
			}
			if (field == null) {
				throw ex;
			}
			field.setAccessible(true);
			Object ret = field.get(object);
			return ret;
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
			EclipseTest.fail(e.getMessage());
		}
		return null;
	}

	public static void setField(Object object, String name, Object value) {
		try {
			Field field = null;
			Class<?> clazz = object.getClass();
			NoSuchFieldException ex = null;
			while (field == null && clazz != null) {
				try {
					field = clazz.getDeclaredField(name);
				} catch (NoSuchFieldException e) {
					if (ex == null) {
						ex = e;
					}
					clazz = clazz.getSuperclass();
				}
			}
			if (field == null) {
				throw ex;
			}
			field.setAccessible(true);
			field.set(object, value);
		} catch (IllegalArgumentException | IllegalAccessException | SecurityException | NoSuchFieldException e) {
			EclipseTest.fail(e.getMessage());
		}
	}

}