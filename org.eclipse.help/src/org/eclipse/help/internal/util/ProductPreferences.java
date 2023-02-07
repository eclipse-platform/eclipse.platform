/*******************************************************************************
 * Copyright (c) 2006, 2020 IBM Corporation and others.
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
 *     George Suaridze <suag@1c.ru> (1C-Soft LLC) - Bug 560168
 *******************************************************************************/
package org.eclipse.help.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IProduct;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.help.HelpSystem;
import org.eclipse.help.internal.HelpData;
import org.eclipse.help.internal.HelpPlugin;
import org.osgi.framework.Bundle;


/*
 * Reads and processes product preferences by considering not only the active
 * product, but all installed products.
 *
 * For example, help orders the books in the table of contents in such a way that
 * satisfies the currently running product's preferred order, and as many other product's
 * preferred orderings.
 */
public class ProductPreferences {

	private static Properties[] productPreferences;
	private static SequenceResolver<String> orderResolver;
	private static Map<Properties, String> preferencesToPluginIdMap;
	private static Map<Properties, String> preferencesToProductIdMap;
	private static List<String> primaryTocOrdering;
	private static List<String>[] secondaryTocOrderings;
	private static final String PLUGINS_ROOT_SLASH = "PLUGINS_ROOT/"; //$NON-NLS-1$
	private static boolean rtl;
	private static boolean directionInitialized = false;

	/*
	 * Returns the recommended order to display the given toc entries in. Each
	 * toc entry is a String, either the id of the toc contribution or the
	 * id of the category of tocs.
	 */
	public static List<String> getTocOrder(List<String> itemsToOrder, Map<String, String> nameIdMap) {
		List<String> primaryOrdering = getPrimaryTocOrdering();
		@SuppressWarnings("unchecked")
		List<String>[] secondaryOrdering = new List[0];
		if (primaryOrdering == null || primaryOrdering.isEmpty()) {
			secondaryOrdering = getSecondaryTocOrderings();
		}
		return getOrderedList(itemsToOrder, primaryOrdering, secondaryOrdering, nameIdMap);
	}

	/*
	 * Returns the primary toc ordering. This is the preferred order for the active
	 * product (either specified via help data xml file or deprecated comma-separated
	 * list in plugin_customization.ini). Help data takes precedence.
	 */
	public static List<String> getPrimaryTocOrdering() {
		if (primaryTocOrdering == null) {
			IProduct product = Platform.getProduct();
			String pluginId = null;
			if (product != null) {
				pluginId = product.getDefiningBundle().getSymbolicName();
			}
			String helpDataFile = Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, HelpPlugin.HELP_DATA_KEY, "", null); //$NON-NLS-1$
			String baseTOCS = Platform.getPreferencesService().getString(HelpPlugin.PLUGIN_ID, HelpPlugin.BASE_TOCS_KEY, "", null); //$NON-NLS-1$
			primaryTocOrdering = getTocOrdering(pluginId, helpDataFile, baseTOCS);
			// active product has no preference for toc order
			if (primaryTocOrdering == null) {
				primaryTocOrdering = new ArrayList<>();
			}
		}
		return primaryTocOrdering;
	}

	/*
	 * Returns all secondary toc ordering. These are the preferred toc orders of all
	 * defined products except the active product.
	 */
	@SuppressWarnings("unchecked")
	public static List<String>[] getSecondaryTocOrderings() {
		if (secondaryTocOrderings == null) {
			List<List<String>> list = new ArrayList<>();
			Properties[] productPreferences = getProductPreferences(false);
			for (int i=0;i<productPreferences.length;++i) {
				String pluginId = preferencesToPluginIdMap.get(productPreferences[i]);
				String helpDataFile = (String)productPreferences[i].get(HelpPlugin.PLUGIN_ID + '/' + HelpPlugin.HELP_DATA_KEY);
				String baseTOCS = (String)productPreferences[i].get(HelpPlugin.PLUGIN_ID + '/' + HelpPlugin.BASE_TOCS_KEY);
				List<String> ordering = getTocOrdering(pluginId, helpDataFile, baseTOCS);
				if (ordering != null) {
					list.add(ordering);
				}
			}
			// can't instantiate arrays of generic type
			secondaryTocOrderings = list.toArray(new List[list.size()]);
		}
		return secondaryTocOrderings;
	}

	/*
	 * Returns the preferred toc ordering of the product defined by the given
	 * plug-in that has the given helpDataFile and baseTOCS specified (these last
	 * two may be null if not specified).
	 */
	public static List<String> getTocOrdering(String pluginId, String helpDataFile, String baseTOCS) {
		if (helpDataFile != null && helpDataFile.length() > 0) {
			String helpDataPluginId = pluginId;
			String helpDataPath = helpDataFile;
			if (helpDataFile.startsWith(PLUGINS_ROOT_SLASH)) {
				int nextSlash = helpDataFile.indexOf('/', PLUGINS_ROOT_SLASH.length());
				if (nextSlash > 0) {
					helpDataPluginId = helpDataFile.substring(PLUGINS_ROOT_SLASH.length(), nextSlash);
					helpDataPath = helpDataFile.substring(nextSlash + 1);
				}
			}
			Bundle bundle = null;
			if (helpDataPluginId != null) {
				bundle = Platform.getBundle(helpDataPluginId);
			}
			if (bundle != null) {
				URL helpDataUrl = bundle.getEntry(helpDataPath);
				HelpData helpData = new HelpData(helpDataUrl);
				return helpData.getTocOrder();
			}
		}
		else {
			if (baseTOCS != null) {
				return tokenize(baseTOCS);
			}
		}
		return null;
	}

	/*
	 * Uses the preference service to get the preference. This has changed slightly in Eclipse 3.5.
	 * The old behavior was undocumented and I think incorrect - CG.
	 *
	 * Previous behavior:
	 * Returns the boolean preference for the given key by consulting every
	 * product's preferences. If any of the products want the preference to
	 * be true (or use the default and the default is true), returns true.
	 * Otherwise returns false (if no products want it true).
	 */
	public static boolean getBoolean(Plugin plugin, String key) {
		return Platform.getPreferencesService().getBoolean(plugin.getBundle().getSymbolicName(), key, false, null);
		/*
		Properties[] properties = getProductPreferences(true);
		String defaultValue = plugin.getPluginPreferences().getDefaultString(key);
		String currentValue = plugin.getPluginPreferences().getString(key);
		String pluginId = plugin.getBundle().getSymbolicName();
		if (currentValue != null && currentValue.equalsIgnoreCase(TRUE)) {
			return true;
		}
		for (int i=0;i<properties.length;++i) {
			String value = (String)properties[i].get(pluginId + '/' + key);
			if (value == null) {
				value = defaultValue;
			}
			if (value != null && value.equalsIgnoreCase(TRUE)) {
				return true;
			}
		}
		return false;
		*/
	}

	/*
	 * Returns the given items in the order specified. Items listed in the order
	 * but not present are skipped, and items present but not ordered are added
	 * at the end.
	 */
	public static List<String> getOrderedList(List<String> items, List<String> order) {
		return getOrderedList(items, order, null, null);
	}

	/*
	 * Returns the given items in an order that best satisfies the given orderings.
	 * The primary ordering must be satisfied in all cases. As many secondary orderings
	 * as reasonably possible will be satisfied.
	 */
	public static List<String> getOrderedList(List<String> items, List<String> primary, List<String>[] secondary,
			Map<String, String> nameIdMap) {
		List<String> result = new ArrayList<>();
		List<String> set = new ArrayList<>(items);
		if (orderResolver == null) {
			orderResolver = new SequenceResolver<>();
		}
		List<String> order = orderResolver.getSequence(primary, secondary);
		for (String obj : order) {
			if (set.contains(obj)) {
				result.add(obj);
				set.remove(obj);
			}
		}
		if (HelpData.getProductHelpData().isSortOthers() && nameIdMap != null) {
			List<String> remaining = new ArrayList<>();
			remaining.addAll(set);
			sortByName(remaining, nameIdMap);
			result.addAll(remaining);
		} else {
			result.addAll(set);
		}
		return result;
	}

	private static class NameComparator implements Comparator<String> {

		private Map<String, String> tocNames;

		public NameComparator(Map<String, String> tocNames) {
			this.tocNames = tocNames;
		}

		@Override
		public int compare(String o1, String o2) {
			String name1 = tocNames.get(o1);
			String name2 = tocNames.get(o2);
			if (name1 == null) {
				return (name2 != null) ? -1 : 0;
			}
			if (name2 == null) {
				return 1;
			}
			return Collator.getInstance().compare(name1, name2);
		}

	}

	private static void sortByName(List<String> remaining, Map<String, String> categorized) {
		remaining.sort(new NameComparator(categorized));
	}

	public static synchronized String getPluginId(Properties prefs) {
		return preferencesToPluginIdMap.get(prefs);
	}

	public static synchronized String getProductId(Properties prefs) {
		return preferencesToProductIdMap.get(prefs);
	}

	/*
	 * Returns the preferences for all products in the runtime environment (even if
	 * they are not active).
	 */
	public static synchronized Properties[] getProductPreferences(boolean includeActiveProduct) {
		if (productPreferences == null) {
			String activeProductId = null;
			IProduct activeProduct = Platform.getProduct();
			if (activeProduct != null) {
				activeProductId = activeProduct.getId();
			}
			Collection<Properties> collection = new ArrayList<>();
			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.core.runtime.products"); //$NON-NLS-1$
			for (int i=0;i<elements.length;++i) {
				if (elements[i].getName().equals("product")) { //$NON-NLS-1$
					String productId = elements[i].getDeclaringExtension().getUniqueIdentifier();
					if (includeActiveProduct || activeProductId == null || !activeProductId.equals(productId)) {
						String contributor = elements[i].getContributor().getName();
						IConfigurationElement[] propertyElements = elements[i].getChildren("property"); //$NON-NLS-1$
						for (int j=0;j<propertyElements.length;++j) {
							String name = propertyElements[j].getAttribute("name"); //$NON-NLS-1$
							if (name != null && name.equals("preferenceCustomization")) { //$NON-NLS-1$
								String value = propertyElements[j].getAttribute("value"); //$NON-NLS-1$
								if (value != null) {
									Properties properties = loadPropertiesFile(contributor, value);
									if (properties != null) {
										collection.add(properties);
									}
									if (preferencesToPluginIdMap == null) {
										preferencesToPluginIdMap = new HashMap<>();
									}
									preferencesToPluginIdMap.put(properties, contributor);
									if (preferencesToProductIdMap == null) {
										preferencesToProductIdMap = new HashMap<>();
									}
									preferencesToProductIdMap.put(properties, productId);
								}
							}
						}
					}
				}
			}
			productPreferences = collection.toArray(new Properties[collection.size()]);
		}
		return productPreferences;
	}

	/*
	 * Returns the value for the given key by consulting the given properties, but giving
	 * precedence to the primary properties. If the primary properties has the key, it is
	 * returned. Otherwise, it will return the value of the first secondary properties that
	 * has the key, or null if none of them has it.
	 */
	public static String getValue(String key, Properties primary, Properties[] secondary) {
		String value = null;
		if (primary != null) {
			value = primary.getProperty(key);
		}
		if (value == null) {
			for (int i=0;i<secondary.length;++i) {
				if (secondary[i] != primary) {
					value = secondary[i].getProperty(key);
					if (value != null) {
						break;
					}
				}
			}
		}
		return value;
	}

	/*
	 * Loads and returns the properties in the given properties file. The path is
	 * relative to the bundle with the given id.
	 */
	public static Properties loadPropertiesFile(String bundleId, String path) {
		Bundle bundle = Platform.getBundle(bundleId);
		if (bundle != null) {
			URL url = bundle.getEntry(path);
			if (url != null) {
				try (InputStream in = url.openStream()) {
					Properties properties = new Properties();
					properties.load(in);
					return properties;
				} catch (IOException e) {
					// log the fact that it couldn't load it
					Platform.getLog(ProductPreferences.class)
							.error("Error opening product's plugin customization file: " + bundleId + "/" + path, e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
		}
		return null;
	}

	/*
	 * Tokenizes the given list of items, allowing them to be separated by whitespace, commas,
	 * and/or semicolons.
	 *
	 * e.g. "item1, item2, item3"
	 * would return a list of strings containing "item1", "item2", and "item3".
	 */
	public static List<String> tokenize(String str) {
		if (str != null) {
			StringTokenizer tok = new StringTokenizer(str, " \n\r\t;,"); //$NON-NLS-1$
			List<String> list = new ArrayList<>();
			while (tok.hasMoreElements()) {
				list.add(tok.nextToken());
			}
			return list;
		}
		return new ArrayList<>();
	}

	public int compare(Object o1, Object o2) {
		// TODO Auto-generated method stub
		return 0;
	}

	public static void resetPrimaryTocOrdering() {
		primaryTocOrdering = null;
	}

	public static boolean isRTL() {
		if (!directionInitialized) {
			directionInitialized = true;
			rtl = initializeRTL();
		}
		return rtl;
	}

	private static boolean initializeRTL() {
		// from property
		String orientation = System.getProperty("eclipse.orientation"); //$NON-NLS-1$
		if ("rtl".equals(orientation)) { //$NON-NLS-1$
			return true;
		} else if ("ltr".equals(orientation)) { //$NON-NLS-1$
			return false;
		}
		// from command line
		String[] args = Platform.getCommandLineArgs();
		for (int i = 0; i < args.length; i++) {
			if ("-dir".equalsIgnoreCase(args[i])) { //$NON-NLS-1$
				if ((i + 1) < args.length
						&& "rtl".equalsIgnoreCase(args[i + 1])) { //$NON-NLS-1$
					return true;
				}
				return false;
			}
		}

		// Check if the user property is set. If not do not
		// rely on the vm.
		if (System.getProperty("osgi.nl.user") == null) //$NON-NLS-1$
			return false;

		// guess from default locale
		String locale = Platform.getNL();
		if (locale == null) {
			locale = Locale.getDefault().toString();
		}
		if (locale.startsWith("ar") || locale.startsWith("fa") //$NON-NLS-1$//$NON-NLS-2$
				|| locale.startsWith("he") || locale.startsWith("iw") //$NON-NLS-1$//$NON-NLS-2$
				|| locale.startsWith("ur")) { //$NON-NLS-1$
			return true;
		}
		return false;
	}

	/*
	 * Expand the special identifiers PLUGINS_ROOT and PRODUCT_PLUGIN in a path
	 */
	public static String resolveSpecialIdentifiers(String path) {
		if (path == null) {
			return null;
		}
		int index = path.indexOf("PLUGINS_ROOT"); //$NON-NLS-1$
		if (index != -1) {
			path = path.substring(index + "PLUGINS_ROOT".length()); //$NON-NLS-1$
		}
		index = path.indexOf('/', 1);
		if (index != -1) {
			String bundleName = path.substring(0, index);
			if ("PRODUCT_PLUGIN".equals(bundleName) || "/PRODUCT_PLUGIN".equals(bundleName)) { //$NON-NLS-1$ //$NON-NLS-2$
				IProduct product = Platform.getProduct();
				if (product != null) {
					Bundle productBundle = product.getDefiningBundle();
					if (productBundle != null) {
						bundleName = productBundle.getSymbolicName();
						return '/' + bundleName + path.substring(index);
					}
				}
			}
		}
		return path;
	}

	public static boolean useEnablementFilters() {
		if (!HelpSystem.isShared()) {
			return true;
		}
		return Platform.getPreferencesService().getBoolean(HelpPlugin.PLUGIN_ID, HelpPlugin.FILTER_INFOCENTER_KEY, false, null);
	}
}
