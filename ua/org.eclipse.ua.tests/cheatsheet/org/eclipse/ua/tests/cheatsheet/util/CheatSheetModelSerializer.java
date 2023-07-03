/*******************************************************************************
 * Copyright (c) 2004, 2019 IBM Corporation and others.
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
package org.eclipse.ua.tests.cheatsheet.util;

import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import org.eclipse.ui.cheatsheets.AbstractItemExtensionElement;
import org.eclipse.ui.internal.cheatsheets.data.AbstractSubItem;
import org.eclipse.ui.internal.cheatsheets.data.Action;
import org.eclipse.ui.internal.cheatsheets.data.CheatSheet;
import org.eclipse.ui.internal.cheatsheets.data.ConditionalSubItem;
import org.eclipse.ui.internal.cheatsheets.data.Item;
import org.eclipse.ui.internal.cheatsheets.data.PerformWhen;
import org.eclipse.ui.internal.cheatsheets.data.RepeatedSubItem;
import org.eclipse.ui.internal.cheatsheets.data.SubItem;

/*
 * Serializes cheat sheets as XML.
 */
public class CheatSheetModelSerializer {

	/*
	 * Serializes the given cheat sheet model as XML.
	 */
	public static String serialize(CheatSheet sheet) {
		StringBuilder buf = new StringBuilder();
		buf.append("<?xml version=\"1.0\" encoding=\"UTF-8\" ?>\n");
		if (sheet == null) {
			buf.append("<nullCheatSheet/>\n");
		}
		else {
			buf.append("<cheatsheet\n");
			buf.append("      title=\"" + sheet.getTitle() + "\">\n");
			buf.append(serialize(sheet.getIntroItem(), "   "));
			buf.append(serialize(sheet.getItems(), "   "));
			buf.append("</cheatsheet>");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given Item with the specified indentation.
	 */
	public static String serialize(Item item, String indent) {
		StringBuilder buf = new StringBuilder();
		if (item == null) {
			buf.append(indent + "<nullItem/>\n");
		}
		else {
			buf.append(indent + "<item\n");
			buf.append(indent + "      title=\"" + item.getTitle() + "\"\n");
			buf.append(indent + "      description=\"" + item.getDescription() + "\"\n");
			buf.append(indent + "      Href=\"" + item.getHref() + "\"\n");
			buf.append(indent + "      contextId=\"" + item.getContextId() + "\">\n");
			buf.append(serialize((Action)item.getExecutable(), indent + "   "));
			buf.append(serialize(item.getItemExtensions(), indent + "   "));
			buf.append(serialize(item.getPerformWhen(), indent + "   "));
			buf.append(serialize(item.getSubItems(), indent + "   "));
			buf.append(indent + "</item>\n");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given Action with the specified indentation.
	 */
	public static String serialize(Action action, String indent) {
		StringBuilder buf = new StringBuilder();
		if (action == null) {
			buf.append(indent + "<nullAction/>\n");
		}
		else {
			buf.append(indent + "<action\n");
			buf.append(indent + "      class=\"" + action.getActionClass() + "\"\n");
			buf.append(indent + "      pluginId=\"" + action.getPluginID() + "\"\n");
			buf.append(indent + "      when=\"" + action.getWhen() + "\"\n");
			buf.append(indent + "      isConfirm=\"" + action.isConfirm() + "\">\n");
			buf.append(serialize(action.getParams(), indent + "   "));
			buf.append(indent + "</action>\n");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given String with the specified indentation.
	 */
	public static String serialize(String string, String indent) {
		StringBuilder buf = new StringBuilder();
		if (string == null) {
			buf.append(indent + "<nullString/>\n");
		}
		else {
			buf.append(indent + "<string\n");
			buf.append(indent + "      value=\"" + string + "\">\n");
			buf.append(indent + "</string>\n");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given AbstractItemExtensionElement with the specified indentation.
	 */
	public static String serialize(AbstractItemExtensionElement ext, String indent) {
		StringBuilder buf = new StringBuilder();
		buf.append(indent + "<itemExtension\n");
		buf.append(indent + "      attributeName=\"" + ext.getAttributeName() + "\">\n");
		buf.append(indent + "</itemExtension>\n");
		return buf.toString();
	}

	/*
	 * Serializes the given PerformWhen with the specified indentation.
	 */
	public static String serialize(PerformWhen performWhen, String indent) {
		StringBuilder buf = new StringBuilder();
		if (performWhen == null) {
			buf.append(indent + "<nullPerformWhen/>\n");
		}
		else {
			buf.append(indent + "<performWhen\n");
			buf.append(indent + "      condition=\"" + performWhen.getCondition() + "\">\n");
			buf.append(serialize(performWhen.getExecutables(), indent + "   "));
			buf.append(indent + "</performWhen>\n");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given AbstractSubItem with the specified indentation.
	 */
	public static String serialize(AbstractSubItem subItem, String indent) {
		StringBuilder buf = new StringBuilder();
		if (subItem == null) {
			buf.append(indent + "<nullSubItem/>\n");
		}
		else {
			if (subItem instanceof ConditionalSubItem) {
				ConditionalSubItem c = (ConditionalSubItem)subItem;
				buf.append(indent + "<conditionalSubItem\n");
				buf.append(indent + "      condition=\"" + c.getCondition() + "\">\n");
				buf.append(serialize(c.getSubItems(), indent + "   "));
				buf.append(indent + "</conditionalSubItem>\n");
			}
			else if (subItem instanceof RepeatedSubItem) {
				RepeatedSubItem r = (RepeatedSubItem)subItem;
				buf.append(indent + "<repeatedSubItem\n");
				buf.append(indent + "      values=\"" + r.getValues() + "\">\n");
				buf.append(serialize(r.getSubItems(), indent + "   "));
				buf.append(indent + "</repeatedSubItem>\n");
			}
			else if (subItem instanceof SubItem) {
				SubItem s = (SubItem)subItem;
				buf.append(indent + "<subItem\n");
				buf.append(indent + "      label=\"" + s.getLabel() + "\"\n");
				buf.append(indent + "      when=\"" + s.getWhen() + "\"\n");
				buf.append(indent + "      isSkip=\"" + s.isSkip() + "\">\n");
				buf.append(serialize((Action)s.getExecutable(), indent + "   "));
				buf.append(serialize(s.getPerformWhen(), indent + "   "));
				buf.append(indent + "</subItem>\n");
			}
			else {
				throw new IllegalArgumentException("Unknown sub item type: " + subItem.getClass());
			}
		}
		return buf.toString();
	}

	/*
	 * Serializes the given array with the specified indentation.
	 */
	public static String serialize(Object[] array, String indent) {
		StringBuilder buf = new StringBuilder();
		if (array == null) {
			buf.append(indent + "<nullArray/>\n");
		} else if (array.length == 0) {
			buf.append(indent + "<array/>\n");
		}
		else {
			buf.append(indent + "<array>\n");
			for (Object obj : array) {
				if (obj != null) {
					Class<?> c = obj.getClass();
					/*
					 * Find a serializer method that knows how to serialize this
					 * object.
					 */
					boolean found = false;
					Method[] methods = CheatSheetModelSerializer.class.getMethods();
					for (Method method : methods) {
						Class<?>[] params = method.getParameterTypes();
						if (params.length == 2 && params[0].isAssignableFrom(c) && params[1].equals(String.class)) {
							try {
								buf.append(method.invoke(null, obj, indent + "   "));
							}
							catch(Exception e) {
								buf.append(indent + "   " + e + ", cause: " + e.getCause());
								e.printStackTrace();
							}
							found = true;
							break;
						}
					}
					if (!found) {
						System.err.println("Could not find serializer for: " + c);
					}
				}
				else {
					buf.append(indent + "   <nullObject/>\n");
				}
			}
			buf.append(indent + "</array>\n");
		}
		return buf.toString();
	}

	/*
	 * Serializes the given List with the specified indentation.
	 */
	public static String serialize(List<?> list, String indent) {
		StringBuilder buf = new StringBuilder();
		if (list == null) {
			buf.append(indent + "<nullList/>\n");
		}
		else if (list.isEmpty()) {
			buf.append(indent + "<list/>\n");
		}
		else {
			buf.append(indent + "<list>\n");
			Iterator<?> iter = list.iterator();
			while (iter.hasNext()) {
				Object obj = iter.next();
				if (obj != null) {
					Class<?> c = obj.getClass();
					/*
					 * Find a serializer method that knows how to serialize this
					 * object.
					 */
					boolean found = false;
					Method[] methods = CheatSheetModelSerializer.class.getMethods();
					for (Method method : methods) {
						Class<?>[] params = method.getParameterTypes();
						if (params.length == 2 && params[0].isAssignableFrom(c) && params[1].equals(String.class)) {
							try {
								buf.append(method.invoke(null, obj, indent + "   "));
							}
							catch(Exception e) {
								buf.append(indent + "   " + e + ", cause: " + e.getCause());
								e.printStackTrace();
							}
							found = true;
							break;
						}
					}
					if (!found) {
						System.err.println("Could not find serializer for: " + c);
					}
				}
				else {
					buf.append(indent + "   <nullObject/>\n");
				}
			}
			buf.append(indent + "</list>\n");
		}
		return buf.toString();
	}
}
