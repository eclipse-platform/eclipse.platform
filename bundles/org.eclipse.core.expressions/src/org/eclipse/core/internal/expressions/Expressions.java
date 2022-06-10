/*******************************************************************************
 * Copyright (c) 2000, 2013 IBM Corporation and others.
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
 *     Sergey Prigogin (Google) - Bug 421375
 *******************************************************************************/
package org.eclipse.core.internal.expressions;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleEvent;
import org.osgi.framework.BundleListener;
import org.osgi.framework.FrameworkUtil;

import org.w3c.dom.Element;

import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ICountable;
import org.eclipse.core.expressions.IIterable;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdapterManager;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;

public class Expressions {

	/**
	 * Cache to optimize instanceof computation. Weak Map of Class-&gt;Map(String, Boolean). Avoid
	 * conflicts caused by multiple classloader contributions with the same class name. It's a rare
	 * occurrence but is supported by the OSGi classloader.
	 */
	private static WeakHashMap<Class<?>, Map<String, Boolean>> fgKnownClasses;

	/**
	 * Cache to optimize loading of classes for evaluation of adapt expressions. Keys are class
	 * loaders. Values are sets of qualified class names that the corresponding class loader was
	 * not able to find.
	 */
	private static WeakHashMap<ClassLoader, Set<String>> fgNotFoundClasses;

	/* debugging flag to enable tracing */
	public static final boolean TRACING= "true".equalsIgnoreCase(Platform.getDebugOption("org.eclipse.core.expressions/tracePropertyResolving")); //$NON-NLS-1$ //$NON-NLS-2$


	private Expressions() {
		// no instance
	}

	public static boolean isInstanceOf(Object element, String type) {
		// null isn't an instanceof of anything.
		if (element == null)
			return false;
		return isSubtype(element.getClass(), type);
	}

	private static synchronized boolean isSubtype(Class<?> clazz, String type) {
		WeakHashMap<Class<?>, Map<String, Boolean>> knownClassesMap= getKnownClasses();
		Map<String, Boolean> nameMap = knownClassesMap.get(clazz);
		if (nameMap != null) {
			Object obj = nameMap.get(type);
			if (obj != null)
				return ((Boolean)obj).booleanValue();
		}
		if (nameMap == null) {
			nameMap = new HashMap<>();
			knownClassesMap.put(clazz, nameMap);
		}
		boolean isSubtype = uncachedIsSubtype(clazz, type);
		nameMap.put(type, isSubtype ? Boolean.TRUE : Boolean.FALSE);
		return isSubtype;
	}

	/**
	 * Loads the given class using the given class loader. Uses {@link #fgNotFoundClasses} for
	 * performance.
	 *
	 * @param classLoader the class loader to use
	 * @param className the qualified name of the class
	 * @return the loaded class, or {@code null} if the class was not found
	 */
	static Class<?> loadClass(ClassLoader classLoader, String className) {
		/*
		 * Class.forName is pretty slow when it throws a ClassNotFoundException. Since expression
		 * evaluation is done very often, we use a cache of names of classes that failed to load.
		 */
		WeakHashMap<ClassLoader, Set<String>> cache;
		synchronized (Expressions.class) {
			cache = getNotFoundClasses();
			Set<String> classNames= cache.get(classLoader);
			if (classNames != null && classNames.contains(className)) {
				return null;
			}
		}

		try {
			return Class.forName(className, false, classLoader);
		} catch (ClassNotFoundException e) {
			synchronized (Expressions.class) {
				Set<String> classNames= cache.get(classLoader);
				if (classNames == null) {
					classNames= new HashSet<>();
					cache.put(classLoader, classNames);
				}
				classNames.add(className);
			}
		}
		return null;
	}

	private static WeakHashMap<Class<?>, Map<String, Boolean>> getKnownClasses() {
		createClassCaches();
		return fgKnownClasses;
	}

	private static WeakHashMap<ClassLoader, Set<String>> getNotFoundClasses() {
		createClassCaches();
		return fgNotFoundClasses;
	}

	private static void createClassCaches() {
		if (fgKnownClasses == null) {
			fgKnownClasses= new WeakHashMap<>();
			fgNotFoundClasses = new WeakHashMap<>();
			BundleContext bundleContext = FrameworkUtil.getBundle(Expressions.class).getBundleContext();
			BundleListener listener= (BundleEvent event) -> {
				// Invalidate the caches if any of the bundles is stopped
				if (event.getType() == BundleEvent.STOPPED) {
					synchronized (Expressions.class) {
						fgKnownClasses.clear();
						fgNotFoundClasses.clear();
					}
				}
			};
			bundleContext.addBundleListener(listener);
		}
	}

	public static boolean uncachedIsSubtype(Class<?> clazz, String type) {
		if (clazz.getName().equals(type))
			return true;
		Class<?> superClass= clazz.getSuperclass();
		if (superClass != null && uncachedIsSubtype(superClass, type))
			return true;
		Class<?>[] interfaces= clazz.getInterfaces();
		for (Class<?> interfaze : interfaces) {
			if (uncachedIsSubtype(interfaze, type))
				return true;
		}
		return false;
	}

	public static void checkAttribute(String name, String value) throws CoreException {
		if (value == null) {
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.MISSING_ATTRIBUTE,
				Messages.format(ExpressionMessages.Expression_attribute_missing, name)));
		}
	}

	public static void checkAttribute(String name, String value, String[] validValues) throws CoreException {
		checkAttribute(name, value);
		for (String validValue : validValues) {
			if (value.equals(validValue))
				return;
		}
		throw new CoreException(new ExpressionStatus(
			ExpressionStatus.WRONG_ATTRIBUTE_VALUE,
			Messages.format(ExpressionMessages.Expression_attribute_invalid_value, value)));
	}

	public static void checkCollection(Object var, Expression expression) throws CoreException {
		if (var instanceof Collection)
			return;
		throw new CoreException(new ExpressionStatus(
			ExpressionStatus.VARIABLE_IS_NOT_A_COLLECTION,
			Messages.format(ExpressionMessages.Expression_variable_not_a_collection, expression.toString())));
	}

	public static void checkList(Object var, Expression expression) throws CoreException {
		if (var instanceof List)
			return;
		throw new CoreException(new ExpressionStatus(
			ExpressionStatus.VARIABLE_IS_NOT_A_LIST,
			Messages.format(ExpressionMessages.Expression_variable_not_a_list, expression.toString())));
	}

	/**
	 * Converts the given variable into an <code>IIterable</code>. If a corresponding adapter can't be found an
	 * exception is thrown. If the corresponding adapter isn't loaded yet, <code>null</code> is returned.
	 *
	 * @param var the variable to turn into an <code>IIterable</code>
	 * @param expression the expression referring to the variable
	 *
	 * @return the <code>IIterable</code> or <code>null</code> if a corresponding adapter isn't loaded yet
	 *
	 * @throws CoreException if the var can't be adapted to an <code>IIterable</code>
	 */
	public static IIterable<?> getAsIIterable(Object var, Expression expression) throws CoreException {
		if (var instanceof IIterable) {
			return (IIterable<?>)var;
		} else {
			IAdapterManager manager= Platform.getAdapterManager();
			IIterable<?> result= manager.getAdapter(var, IIterable.class);
			if (result != null)
				return result;

			if (manager.queryAdapter(var, IIterable.class.getName()) == IAdapterManager.NOT_LOADED)
				return null;

			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.VARIABLE_IS_NOT_A_COLLECTION,
				Messages.format(ExpressionMessages.Expression_variable_not_iterable, expression.toString())));
		}
	}

	/**
	 * Converts the given variable into an <code>ICountable</code>. If a corresponding adapter can't be found an
	 * exception is thrown. If the corresponding adapter isn't loaded yet, <code>null</code> is returned.
	 *
	 * @param var the variable to turn into an <code>ICountable</code>
	 * @param expression the expression referring to the variable
	 *
	 * @return the <code>ICountable</code> or <code>null</code> if a corresponding adapter isn't loaded yet
	 *
	 * @throws CoreException if the var can't be adapted to an <code>ICountable</code>
	 */
	public static ICountable getAsICountable(Object var, Expression expression) throws CoreException {
		if (var instanceof ICountable) {
			return (ICountable)var;
		} else {
			IAdapterManager manager= Platform.getAdapterManager();
			ICountable result= manager.getAdapter(var, ICountable.class);
			if (result != null)
				return result;

			if (manager.queryAdapter(var, ICountable.class.getName()) == IAdapterManager.NOT_LOADED)
				return null;

			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.VARIABLE_IS_NOT_A_COLLECTION,
				Messages.format(ExpressionMessages.Expression_variable_not_countable, expression.toString())));
		}
	}

	public static boolean getOptionalBooleanAttribute(IConfigurationElement element, String attributeName) {
		String value= element.getAttribute(attributeName);
		if (value == null)
			return false;
		return Boolean.parseBoolean(value);
	}

	public static boolean getOptionalBooleanAttribute(Element element, String attributeName) {
		String value= element.getAttribute(attributeName);
		if (value.isEmpty())
			return false;
		return Boolean.parseBoolean(value);
	}

	//---- Argument parsing --------------------------------------------

	public static final Object[] EMPTY_ARGS= new Object[0];

	public static Object[] getArguments(IConfigurationElement element, String attributeName) throws CoreException {
		String args= element.getAttribute(attributeName);
		if (args != null) {
			return parseArguments(args);
		} else {
			return EMPTY_ARGS;
		}
	}

	public static Object[] getArguments(Element element, String attributeName) throws CoreException {
		String args= element.getAttribute(attributeName);
		if (!args.isEmpty()) {
			return parseArguments(args);
		} else {
			return EMPTY_ARGS;
		}
	}

	public static Object[] parseArguments(String args) throws CoreException {
		List<Object> result= new ArrayList<>();
		int start= 0;
		int comma;
		while ((comma= findNextComma(args, start)) != -1) {
			result.add(convertArgument(args.substring(start, comma).trim()));
			start= comma + 1;
		}
		result.add(convertArgument(args.substring(start).trim()));
		return result.toArray();
	}

	private static int findNextComma(String str, int start) throws CoreException {
		boolean inString= false;
		for (int i= start; i < str.length(); i++) {
			char ch= str.charAt(i);
			if (ch == ',' && ! inString)
				return i;
			if (ch == '\'') {
				if (!inString) {
					inString= true;
				} else if (i + 1 < str.length() && str.charAt(i + 1) == '\'') {
					i++;
				} else {
					inString= false;
				}
			} else if (ch == ',' && !inString) {
				return i;
			}
		}
		if (inString)
			throw new CoreException(new ExpressionStatus(
				ExpressionStatus.STRING_NOT_TERMINATED,
				Messages.format(ExpressionMessages.Expression_string_not_terminated, str)));

		return -1;
	}

	public static Object convertArgument(String arg) throws CoreException {
		if (arg == null) {
			return null;
		} else if (arg.isEmpty()) {
			return arg;
		} else if (arg.charAt(0) == '\'' && arg.charAt(arg.length() - 1) == '\'') {
			return unEscapeString(arg.substring(1, arg.length() - 1));
		} else if ("true".equals(arg)) { //$NON-NLS-1$
			return Boolean.TRUE;
		} else if ("false".equals(arg)) { //$NON-NLS-1$
			return Boolean.FALSE;
		} else if (arg.indexOf('.') != -1) {
			try {
				return Float.valueOf(arg);
			} catch (NumberFormatException e) {
				return arg;
			}
		} else {
			try {
				return Integer.valueOf(arg);
			} catch (NumberFormatException e) {
				return arg;
			}
		}
	}

	public static String unEscapeString(String str) throws CoreException {
		StringBuilder result= new StringBuilder();
		for (int i= 0; i < str.length(); i++) {
			char ch= str.charAt(i);
			if (ch == '\'') {
				if (i == str.length() - 1 || str.charAt(i + 1) != '\'')
					throw new CoreException(new ExpressionStatus(
						ExpressionStatus.STRING_NOT_CORRECT_ESCAPED,
						Messages.format(ExpressionMessages.Expression_string_not_correctly_escaped, str)));
				result.append('\'');
				i++;
			} else {
				result.append(ch);
			}
		}
		return result.toString();
	}
}
