/*******************************************************************************
 * Copyright (c) 2000, 2022 IBM Corporation and others.
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
 *     Axel Richard (Obeo) - Bug 41353 - Launch configurations prototypes
 *******************************************************************************/
package org.eclipse.debug.internal.core;


import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The information associated with a launch configuration handle.
 */
public class LaunchConfigurationInfo {

	/**
	 * Constants for XML element names and attributes
	 */
	private static final String KEY = "key"; //$NON-NLS-1$
	private static final String VALUE = "value"; //$NON-NLS-1$
	private static final String SET_ENTRY = "setEntry"; //$NON-NLS-1$
	private static final String LAUNCH_CONFIGURATION = "launchConfiguration"; //$NON-NLS-1$
	private static final String MAP_ENTRY = "mapEntry"; //$NON-NLS-1$
	private static final String LIST_ENTRY = "listEntry"; //$NON-NLS-1$
	private static final String SET_ATTRIBUTE = "setAttribute"; //$NON-NLS-1$
	private static final String MAP_ATTRIBUTE = "mapAttribute"; //$NON-NLS-1$
	private static final String LIST_ATTRIBUTE = "listAttribute"; //$NON-NLS-1$
	private static final String BOOLEAN_ATTRIBUTE = "booleanAttribute"; //$NON-NLS-1$
	private static final String INT_ATTRIBUTE = "intAttribute"; //$NON-NLS-1$
	private static final String STRING_ATTRIBUTE = "stringAttribute"; //$NON-NLS-1$
	private static final String TYPE = "type"; //$NON-NLS-1$
	private static final String PROTOTYPE = "prototype"; //$NON-NLS-1$
	private static final String VISIBLE_ATTRIBUTES = "visibleAttributes"; //$NON-NLS-1$

	/**
	 * This configurations attribute table. Keys are <code>String</code>s and
	 * values are one of <code>String</code>, <code>Integer</code>,
	 * <code>Boolean</code>, <code>Set&lt;String&gt;</code>,
	 * <code>List&lt;String&gt;</code>, or
	 * <code>Map&lt;String, String&gt;</code>
	 */
	private TreeMap<String, Object> fAttributes;

	/**
	 * This launch configuration's type
	 */
	private ILaunchConfigurationType fType;

	/**
	 * Whether this configuration is a prototype
	 */
	private boolean fIsPrototype = false;

	/**
	 * This launch configuration's prototype (can be <code>null</code> if this launch configuration is already a prototype).
	 */
	private ILaunchConfiguration fPrototype;

	/**
	 * This prototype's visible attributes (can be <code>null</code> if launch configuration is not a prototype).
	 */
	private Set<String> fVisibleAttributes;

	/**
	 * Static access to the launch manager.
	 */
	private static LaunchManager fgLaunchManager = (LaunchManager)DebugPlugin.getDefault().getLaunchManager();

	/**
	 * Whether running on Sun 1.4 VM - see bug 110215
	 */
	private static boolean fgIsSun14x = false;

	static {
		String vendor = System.getProperty("java.vm.vendor"); //$NON-NLS-1$
		if (vendor.startsWith("Sun Microsystems")) { //$NON-NLS-1$
			String version = System.getProperty("java.vm.version"); //$NON-NLS-1$
			if (version.startsWith("1.4")) { //$NON-NLS-1$
				fgIsSun14x = true;
			}
		}
	}

	/**
	 * Constructs a new empty info
	 */
	protected LaunchConfigurationInfo() {
		setAttributeTable(new TreeMap<>());
	}

	/**
	 * Returns this configuration's attribute table.
	 *
	 * @return attribute table
	 */
	private TreeMap<String, Object> getAttributeTable() {
		return fAttributes;
	}

	/**
	 * Sets this configuration's attribute table.
	 *
	 * @param table
	 *            attribute table
	 */
	private void setAttributeTable(TreeMap<String, Object> table) {
		fAttributes = table;
	}

	/**
	 * Sets the attributes in this info to those in the given map.
	 *
	 * @param map the {@link Map} of attributes to set
	 */
	protected void setAttributes(Map<String, ?> map) {
		if (map == null) {
			setAttributeTable(new TreeMap<>());
			return;
		}
		setAttributeTable(new TreeMap<>(map));
	}

	/**
	 * Returns the <code>String</code> attribute with the given key or the
	 * given default value if undefined.
	 * @param key the attribute name
	 * @param defaultValue the value to be returned if the given key does not exist in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException
	 *             if the attribute with the given key exists but is not a
	 *             <code>String</code>
	 */
	protected String getStringAttribute(String key, String defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof String) {
				return (String)attr;
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_lang_String__1, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Returns the <code>int</code> attribute with the given key or the given
	 * default value if undefined.
	 * @param key the name of the attribute
	 * @param defaultValue the default value to return if the key does not appear in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException
	 *             if the attribute with the given key exists but is not an
	 *             <code>int</code>
	 */
	protected int getIntAttribute(String key, int defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Integer) {
				return ((Integer)attr).intValue();
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_Attribute__0__is_not_of_type_int__2, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Returns the <code>boolean</code> attribute with the given key or the
	 * given default value if undefined.
	 * @param key the name of the attribute
	 * @param defaultValue the default value to return if the key does not appear in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException
	 *             if the attribute with the given key exists but is not a
	 *             <code>boolean</code>
	 */
	protected boolean getBooleanAttribute(String key, boolean defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Boolean) {
				return ((Boolean)attr).booleanValue();
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_Attribute__0__is_not_of_type_boolean__3, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Returns the <code>java.util.List</code> attribute with the given key or
	 * the given default value if undefined.
	 * @param key the name of the attribute
	 * @param defaultValue the default value to return if the key does not appear in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException
	 *             if the attribute with the given key exists but is not a
	 *             <code>java.util.List</code>
	 */
	@SuppressWarnings("unchecked")
	protected List<String> getListAttribute(String key, List<String> defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof List) {
				return (List<String>) attr;
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_util_List__1, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Returns the <code>java.util.Set</code> attribute with the given key or
	 * the given default value if undefined.
	 *
	 * @param key the name of the attribute
	 * @param defaultValue the default value to return if the key does not exist
	 *            in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException if the attribute with the given key exists but is
	 *             not a <code>java.util.Set</code>
	 *
	 * @since 3.3
	 */
	@SuppressWarnings("unchecked")
	protected Set<String> getSetAttribute(String key, Set<String> defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Set) {
				return (Set<String>) attr;
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_35, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Returns the raw object from the attribute table or <code>null</code> if none.
	 *
	 * @param key attribute key
	 * @return raw attribute value
	 *
	 * @since 3.12
	 */
	protected Object getObjectAttribute(String key) {
		return getAttributeTable().get(key);
	}

	/**
	 * Returns the <code>java.util.Map</code> attribute with the given key or
	 * the given default value if undefined.
	 * @param key the name of the attribute
	 * @param defaultValue the default value to return if the key does not exist in the attribute table
	 *
	 * @return attribute specified by given key or the defaultValue if undefined
	 * @throws CoreException
	 *             if the attribute with the given key exists but is not a
	 *             <code>java.util.Map</code>
	 */
	@SuppressWarnings("unchecked")
	protected Map<String, String> getMapAttribute(String key, Map<String, String> defaultValue) throws CoreException {
		Object attr = getAttributeTable().get(key);
		if (attr != null) {
			if (attr instanceof Map) {
				return (Map<String, String>) attr;
			}
			throw new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
							DebugException.REQUEST_FAILED, MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_Attribute__0__is_not_of_type_java_util_Map__1, key), null
				)
			);
		}
		return defaultValue;
	}

	/**
	 * Sets this configuration's type.
	 *
	 * @param type
	 *            launch configuration type
	 */
	protected void setType(ILaunchConfigurationType type) {
		fType = type;
	}

	/**
	 * Returns this configuration's type.
	 *
	 * @return launch configuration type
	 */
	protected ILaunchConfigurationType getType() {
		return fType;
	}

	/**
	 * Sets this configuration's prototype.
	 *
	 * @param prototype
	 *            launch configuration prototype
	 *
	 * @since 3.12
	 */
	protected void setPrototype(ILaunchConfiguration prototype) {
		fPrototype = prototype;
	}

	/**
	 * Returns this configuration's prototype, if it exists.
	 *
	 * @return launch configuration prototype (can be <code>null</code>)
	 *
	 * @since 3.12
	 */
	protected ILaunchConfiguration getPrototype() {
		return fPrototype;
	}

	/**
	 * Returns a copy of this info object
	 *
	 * @return copy of this info
	 */
	protected LaunchConfigurationInfo getCopy() {
		LaunchConfigurationInfo copy = new LaunchConfigurationInfo();
		copy.setType(getType());
		copy.setAttributeTable(getAttributes());
		copy.setIsPrototype(isPrototype());
		copy.setPrototype(getPrototype());
		copy.setVisibleAttributes(getVisibleAttributes());
		return copy;
	}

	/**
	 * Returns a copy of this info's attribute map.
	 *
	 * @return a copy of this info's attribute map
	 */
	protected TreeMap<String, Object> getAttributes() {
		return new TreeMap<>(getAttributeTable());
	}

	/**
	 * Sets the given attribute to the given value. Only working copy's should
	 * use this API.
	 *
	 * @param key
	 *            attribute key
	 * @param value
	 *            attribute value
	 */
	protected void setAttribute(String key, Object value) {
		if (value == null) {
			getAttributeTable().remove(key);
			setAttributeVisibility(key, false);
		} else {
			Object attribute = getAttributeTable().put(key, value);
			// If attribute is new in the table and the configuration is a
			// prototype, then add it to the visible attributes
			if (attribute == null && fIsPrototype) {
				setAttributeVisibility(key, true);
			}
		}
	}

	/**
	 * Returns the content of this info as XML
	 *
	 * @return the content of this info as XML
	 * @throws CoreException
	 *             if a attribute has been set with a null key
	 * @throws IOException
	 *             if an exception occurs creating the XML
	 * @throws ParserConfigurationException
	 *             if an exception occurs creating the XML
	 * @throws TransformerException
	 *             if an exception occurs creating the XML
	 */
	@SuppressWarnings("unchecked")
	protected String getAsXML(String lineDelimeter) throws CoreException, IOException, ParserConfigurationException, TransformerException {
		Document doc = LaunchManager.getDocument();
		Element configRootElement = doc.createElement(LAUNCH_CONFIGURATION);
		doc.appendChild(configRootElement);

		configRootElement.setAttribute(TYPE, getType().getIdentifier());

		ILaunchConfiguration prototype = getPrototype();
		if (prototype != null) {
			configRootElement.setAttribute(PROTOTYPE, prototype.getName());
		} else if (isPrototype()) {
			configRootElement.setAttribute(VISIBLE_ATTRIBUTES, getVisibleAttributes().stream().collect(Collectors.joining(", "))); //$NON-NLS-1$
		}

		for (String key : getAttributeTable().keySet()) {
			if (key == null) {
				throw new DebugException(
					new Status(
						IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
						DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfigurationInfo_36, null
					)
				);
			}
			Object value = getAttributeTable().get(key);
			if (value == null) {
				continue;
			}
			Element element = null;
			String valueString = null;
			if (value instanceof String) {
				valueString = (String)value;
				element = createKeyValueElement(doc, STRING_ATTRIBUTE, key, valueString);
			} else if (value instanceof Integer) {
				valueString = ((Integer)value).toString();
				element = createKeyValueElement(doc, INT_ATTRIBUTE, key, valueString);
			} else if (value instanceof Boolean) {
				valueString = ((Boolean)value).toString();
				element = createKeyValueElement(doc, BOOLEAN_ATTRIBUTE, key, valueString);
			} else if (value instanceof List) {
				element = createListElement(doc, LIST_ATTRIBUTE, key, (List<String>) value);
			} else if (value instanceof Map) {
				element = createMapElement(doc, MAP_ATTRIBUTE, key, (Map<String, String>) value);
			} else if(value instanceof Set) {
				element = createSetElement(doc, SET_ATTRIBUTE, key, (Set<String>) value);
			}
			configRootElement.appendChild(element);
		}

		return LaunchManager.serializeDocument(doc, lineDelimeter);
	}

	/**
	 * Helper method that creates a 'key value' element of the specified type
	 * with the specified attribute values.
	 * @param doc the {@link Document}
	 * @param elementType the {@link Element} type to create
	 * @param key the {@link Element} key
	 * @param value the {@link Element} value
	 * @return the new {@link Element}
	 */
	protected Element createKeyValueElement(Document doc, String elementType, String key, String value) {
		Element element = doc.createElement(elementType);
		element.setAttribute(KEY, key);
		element.setAttribute(VALUE, value);
		return element;
	}

	/**
	 * Creates a new <code>Element</code> for the specified
	 * <code>java.util.List</code>
	 *
	 * @param doc the doc to add the element to
	 * @param elementType the type of the element
	 * @param listKey the key for the element
	 * @param list the list to fill the new element with
	 * @return the new element
	 */
	protected Element createListElement(Document doc, String elementType, String listKey, List<String> list) {
		Element listElement = doc.createElement(elementType);
		listElement.setAttribute(KEY, listKey);
		for (String value : list) {
			Element element = doc.createElement(LIST_ENTRY);
			element.setAttribute(VALUE, value);
			listElement.appendChild(element);
		}
		return listElement;
	}

	/**
	 * Creates a new <code>Element</code> for the specified
	 * <code>java.util.Set</code>
	 *
	 * @param doc the doc to add the element to
	 * @param elementType the type of the element
	 * @param setKey the key for the element
	 * @param set the set to fill the new element with
	 * @return the new element
	 *
	 * @since 3.3
	 */
	protected Element createSetElement(Document doc, String elementType, String setKey, Set<String> set) {
		Element setElement = doc.createElement(elementType);
		setElement.setAttribute(KEY, setKey);
		// persist in sorted order
		List<String> list = new ArrayList<>(set);
		Collections.sort(list);
		Element element = null;
		for (String str : list) {
			element = doc.createElement(SET_ENTRY);
			element.setAttribute(VALUE, str);
			setElement.appendChild(element);
		}
		return setElement;
	}

	/**
	 * Creates a new <code>Element</code> for the specified
	 * <code>java.util.Map</code>
	 *
	 * @param doc the doc to add the element to
	 * @param elementType the type of the element
	 * @param mapKey the key for the element
	 * @param map the map to fill the new element with
	 * @return the new element
	 *
	 */
	protected Element createMapElement(Document doc, String elementType, String mapKey, Map<String, String> map) {
		Element mapElement = doc.createElement(elementType);
		mapElement.setAttribute(KEY, mapKey);
		// persist in sorted order based on keys
		List<String> keys = new ArrayList<>(map.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			String value = map.get(key);
			Element element = doc.createElement(MAP_ENTRY);
			element.setAttribute(KEY, key);
			element.setAttribute(VALUE, value);
			mapElement.appendChild(element);
		}
		return mapElement;
	}

	/**
	 * Initializes the mapping of attributes from the XML file
	 * @param root the root node from the XML document
	 * @throws CoreException if a problem is encountered
	 */
	protected void initializeFromXML(Element root) throws CoreException {
		initializeFromXML(root, false);
	}

	/**
	 * Initializes the mapping of attributes from the XML file
	 *
	 * @param root the root node from the XML document
	 * @param isPrototype if the XML file corresponds to a prototype
	 * @throws CoreException if a problem is encountered
	 *
	 * @since 3.12
	 */
	protected void initializeFromXML(Element root, boolean isPrototype) throws CoreException {
		if (!root.getNodeName().equalsIgnoreCase(LAUNCH_CONFIGURATION)) {
			throw getInvalidFormatDebugException();
		}

		// read type
		String id = root.getAttribute(TYPE);
		if (id == null) {
			throw getInvalidFormatDebugException();
		}

		ILaunchConfigurationType type = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurationType(id);
		if (type == null) {
			String message = MessageFormat.format(DebugCoreMessages.LaunchConfigurationInfo_missing_type, id);
			throw new DebugException(
					new Status(
					 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
					 DebugException.MISSING_LAUNCH_CONFIGURATION_TYPE, message, null)
				);
		}
		setType(type);

		NodeList list = root.getChildNodes();
		Node node = null;
		Element element = null;
		String nodeName = null;
		for (int i = 0; i < list.getLength(); ++i) {
			node = list.item(i);
			short nodeType = node.getNodeType();
			if (nodeType == Node.ELEMENT_NODE) {
				element = (Element) node;
				nodeName = element.getNodeName();
				if (nodeName.equalsIgnoreCase(STRING_ATTRIBUTE)) {
					setStringAttribute(element);
				} else if (nodeName.equalsIgnoreCase(INT_ATTRIBUTE)) {
					setIntegerAttribute(element);
				} else if (nodeName.equalsIgnoreCase(BOOLEAN_ATTRIBUTE))  {
					setBooleanAttribute(element);
				} else if (nodeName.equalsIgnoreCase(LIST_ATTRIBUTE)) {
					setListAttribute(element);
				} else if (nodeName.equalsIgnoreCase(MAP_ATTRIBUTE)) {
					setMapAttribute(element);
				} else if(nodeName.equalsIgnoreCase(SET_ATTRIBUTE)) {
					setSetAttribute(element);
				}
			}
		}

		if (isPrototype) {
			setIsPrototype(true);
			String visibleAttributes = root.getAttribute(VISIBLE_ATTRIBUTES);
			if (visibleAttributes != null && visibleAttributes.length() > 0) {
				String[] split = visibleAttributes.split(", "); //$NON-NLS-1$
				setVisibleAttributes(new HashSet<>(Arrays.asList(split)));
			}
		} else {
			setIsPrototype(false);
			String prototype = root.getAttribute(PROTOTYPE);
			if (prototype != null && prototype.length() > 0) {
				ILaunchConfiguration[] launchConfigurations = DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(ILaunchConfiguration.PROTOTYPE);
				for (ILaunchConfiguration iLaunchConfiguration : launchConfigurations) {
					if (prototype.equals(iLaunchConfiguration.getName())) {
						setPrototype(iLaunchConfiguration);
						break;
					}
				}
			}
		}
	}

	/**
	 * Loads a <code>String</code> from the specified element into the local attribute mapping
	 * @param element the element to load from
	 * @throws CoreException if a problem is encountered
	 */
	protected void setStringAttribute(Element element) throws CoreException {
		setAttribute(getKeyAttribute(element), getValueAttribute(element));
	}

	/**
	 * Loads an <code>Integer</code> from the specified element into the local attribute mapping
	 * @param element the element to load from
	 * @throws CoreException if a problem is encountered
	 */
	protected void setIntegerAttribute(Element element) throws CoreException {
		setAttribute(getKeyAttribute(element), Integer.valueOf(getValueAttribute(element)));
	}

	/**
	 * Loads a <code>Boolean</code> from the specified element into the local attribute mapping
	 * @param element the element to load from
	 * @throws CoreException if a problem is encountered
	 */
	protected void setBooleanAttribute(Element element) throws CoreException {
		setAttribute(getKeyAttribute(element), Boolean.valueOf(getValueAttribute(element)));
	}

	/**
	 * Reads a <code>List</code> attribute from the specified XML node and
	 * loads it into the mapping of attributes
	 *
	 * @param element the element to read the list attribute from
	 * @throws CoreException if the element has an invalid format
	 */
	protected void setListAttribute(Element element) throws CoreException {
		String listKey = element.getAttribute(KEY);
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		List<String> list = new ArrayList<>(entryCount);
		Node node = null;
		Element selement = null;
		for (int i = 0; i < entryCount; i++) {
			node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				selement = (Element) node;
				if (!selement.getNodeName().equalsIgnoreCase(LIST_ENTRY)) {
					throw getInvalidFormatDebugException();
				}
				list.add(getValueAttribute(selement));
			}
		}
		setAttribute(listKey, list);
	}

	/**
	 * Reads a <code>Set</code> attribute from the specified XML node and
	 * loads it into the mapping of attributes
	 *
	 * @param element the element to read the set attribute from
	 * @throws CoreException if the element has an invalid format
	 *
	 * @since 3.3
	 */
	protected void setSetAttribute(Element element) throws CoreException {
		String setKey = element.getAttribute(KEY);
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		Set<String> set = new HashSet<>(entryCount);
		Node node = null;
		Element selement = null;
		for(int i = 0; i < entryCount; i++) {
			node = nodeList.item(i);
			if(node.getNodeType() == Node.ELEMENT_NODE) {
				selement = (Element)node;
				if(!selement.getNodeName().equalsIgnoreCase(SET_ENTRY)) {
					throw getInvalidFormatDebugException();
				}
				set.add(getValueAttribute(selement));
			}
		}
		setAttribute(setKey, set);
	}

	/**
	 * Reads a <code>Map</code> attribute from the specified XML node and
	 * loads it into the mapping of attributes
	 *
	 * @param element the element to read the map attribute from
	 * @throws CoreException if the element has an invalid format
	 */
	protected void setMapAttribute(Element element) throws CoreException {
		String mapKey = element.getAttribute(KEY);
		NodeList nodeList = element.getChildNodes();
		int entryCount = nodeList.getLength();
		Map<String, String> map = new HashMap<>(entryCount);
		Node node = null;
		Element selement = null;
		for (int i = 0; i < entryCount; i++) {
			node = nodeList.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				selement = (Element) node;
				if (!selement.getNodeName().equalsIgnoreCase(MAP_ENTRY)) {
					throw getInvalidFormatDebugException();
				}
				map.put(getKeyAttribute(selement), getValueAttribute(selement));
			}
		}
		setAttribute(mapKey, map);
	}

	/**
	 * Returns the <code>String</code> representation of the 'key' attribute from the specified element
	 * @param element the element to read from
	 * @return the value
	 * @throws CoreException if a problem is encountered
	 */
	protected String getKeyAttribute(Element element) throws CoreException {
		String key = element.getAttribute(KEY);
		if (key == null) {
			throw getInvalidFormatDebugException();
		}
		return key;
	}

	/**
	 * Returns the <code>String</code> representation of the 'value' attribute from the specified element
	 * @param element the element to read from
	 * @return the value
	 * @throws CoreException if a problem is encountered
	 */
	protected String getValueAttribute(Element element) throws CoreException {
		String value = element.getAttribute(VALUE);
		if (value == null) {
			throw getInvalidFormatDebugException();
		}
		return value;
	}

	/**
	 * Returns an invalid format exception for reuse
	 * @return an invalid format exception
	 */
	protected DebugException getInvalidFormatDebugException() {
		return
			new DebugException(
				new Status(
				 IStatus.ERROR, DebugPlugin.getUniqueIdentifier(),
				 DebugException.REQUEST_FAILED, DebugCoreMessages.LaunchConfigurationInfo_Invalid_launch_configuration_XML__10, null
				)
			);
	}

	/**
	 * Two <code>LaunchConfigurationInfo</code> objects are equal if and only
	 * if they have the same type and they have the same set of attributes with
	 * the same values.
	 *
	 * @see Object#equals(Object)
	 */
	@Override
	public boolean equals(Object obj) {

		// Make sure it's a LaunchConfigurationInfo object
		if (!(obj instanceof LaunchConfigurationInfo)) {
			return false;
		}

		// Make sure the types are the same
		LaunchConfigurationInfo other = (LaunchConfigurationInfo) obj;
		if (!fType.getIdentifier().equals(other.getType().getIdentifier())) {
			return false;
		}

		// In case of a prototype, make sure the visible attributes are the same
		if (isPrototype() != other.isPrototype()) {
			return false;
		} else if (isPrototype() && !getVisibleAttributes().equals(other.getVisibleAttributes())) {
			return false;
		}

		// Make sure the attributes are the same
		return compareAttributes(fAttributes, other.getAttributeTable());
	}

	/**
	 * Returns whether the two attribute maps are equal, consulting registered
	 * comparator extensions.
	 *
	 * @param map1  attribute map
	 * @param map2 attribute map
	 * @return whether the two attribute maps are equal
	 */
	protected boolean compareAttributes(TreeMap<String, Object> map1, TreeMap<String, Object> map2) {
		if (map1.size() == map2.size()) {
			Iterator<String> attributes = map1.keySet().iterator();
			while (attributes.hasNext()) {
				String key = attributes.next();
				Object attr1 = map1.get(key);
				Object attr2 = map2.get(key);
				if (!compareAttribute(key, attr1, attr2)) {
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/**
	 * Returns whether the two attributes are equal, considering comparator extensions.
	 *
	 * @param key attribute key
	 * @param attr1 attribute value
	 * @param attr2 attribute value to compare to, possibly <code>null</code>
	 * @return whether equivalent
	 *
	 * @since 3.12
	 */
	protected static boolean compareAttribute(String key, Object attr1, Object attr2) {
		if (attr2 == null) {
			return false;
		}
		Comparator<Object> comp = fgLaunchManager.getComparator(key);
		if (comp == null) {
			String strAttr1 = null;
			String strAttr2 = null;
			if (fgIsSun14x) {
				if(attr2 instanceof String && attr1 instanceof String) {
					// this is a hack for bug 110215, on SUN 1.4.x, \r
					// is stripped off when the stream is written to the
					// DOM
					// this is not the case for 1.5.x, so to be safe we
					// are stripping \r off all strings before we
					// compare for equality
					strAttr1 = ((String)attr1).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
					strAttr2 = ((String)attr2).replaceAll("\\r", ""); //$NON-NLS-1$ //$NON-NLS-2$
					if (!strAttr1.equals(strAttr2)) {
						return false;
					}
				}
			}
			if (strAttr1 == null && strAttr2 == null && !attr1.equals(attr2)) {
				return false;
			}
		} else if (comp.compare(attr1, attr2) != 0) {
			return false;
		}
		return true;
	}

	/**
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		return fType.hashCode() + fAttributes.size();
	}

	/**
	 * Returns if the attribute map contains the specified key
	 * @param attributeName the name of the attribute to check for
	 * @return true if the attribute map contains the specified key, false otherwise
	 *
	 * @since 3.4.0
	 */
	protected boolean hasAttribute(String attributeName) {
		return fAttributes.containsKey(attributeName);
	}

	/**
	 * Removes the specified attribute from the mapping and returns
	 * its value, or <code>null</code> if none. Does nothing
	 * if the attribute name is <code>null</code>
	 * @param attributeName the name of the attribute to remove
	 * @return attribute value or <code>null</code>
	 *
	 * @since 3.4.0
	 */
	protected Object removeAttribute(String attributeName) {
		if(attributeName != null) {
			return fAttributes.remove(attributeName);
		}
		return null;
	}

	/**
	 * Sets whether this info is a prototype.
	 *
	 * @param isPrototype
	 *
	 * @since 3.12
	 */
	protected void setIsPrototype(boolean isPrototype) {
		fIsPrototype = isPrototype;
	}

	/**
	 * Returns whether this info is a prototype.
	 *
	 * @return whether a prototype
	 *
	 * @since 3.12
	 */
	protected boolean isPrototype() {
		return fIsPrototype;
	}

	/**
	 * Get the visible attributes of this prototype (return <code>null</code> if
	 * the launch configuration is not a prototype).
	 *
	 * @return the visible attributes of this prototype (return
	 *         <code>null</code> if the launch configuration is not a
	 *         prototype).
	 *
	 * @since 3.12
	 */
	protected Set<String> getVisibleAttributes() {
		if (!isPrototype()) {
			return null;
		} else if (fVisibleAttributes == null) {
			initializeVisibleAttributes();
		}
		return fVisibleAttributes;
	}

	/**
	 * Initialize the visible attributes of this launch configuration. All
	 * attributes are visible by default.
	 *
	 * @since 3.12
	 */
	private void initializeVisibleAttributes() {
		fVisibleAttributes = new HashSet<>(getAttributeTable().keySet());
	}

	/**
	 * Set the visible attributes of this prototype. Do not call this method on
	 * a launch configuration that is not a prototype.
	 *
	 * @param visibleAttributes the visible attributes
	 *
	 * @since 3.12
	 */
	protected void setVisibleAttributes(Set<String> visibleAttributes) {
		if (visibleAttributes != null) {
			fVisibleAttributes = new HashSet<>(visibleAttributes);
		}
	}

	/**
	 * Set visibility of the given attribute. Do not call this method on a
	 * launch configuration that is not a prototype.
	 *
	 * @param attribute the given attribute
	 * @param visible the visibility
	 *
	 * @since 3.12
	 */
	protected void setAttributeVisibility(String attribute, boolean visible) {
		if (fVisibleAttributes != null) {
			if (visible) {
				fVisibleAttributes.add(attribute);
			} else {
				fVisibleAttributes.remove(attribute);
			}
		}
	}
}

