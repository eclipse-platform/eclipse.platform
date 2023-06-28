/*******************************************************************************
 * Copyright (c) 2000, 2018 IBM Corporation and others.
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
 *     Stefan Liebig  - Bug 242685 StringVariableManager - Variable contributions may silently override existing variables
 *******************************************************************************/
package org.eclipse.core.internal.variables;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.core.variables.IDynamicVariable;
import org.eclipse.core.variables.IStringVariable;
import org.eclipse.core.variables.IStringVariableManager;
import org.eclipse.core.variables.IValueVariable;
import org.eclipse.core.variables.IValueVariableListener;
import org.eclipse.core.variables.VariablesPlugin;
import org.eclipse.osgi.util.NLS;
import org.osgi.service.prefs.BackingStoreException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Singleton string variable manager.
 */
public class StringVariableManager implements IStringVariableManager, IPreferenceChangeListener {

	/**
	 * Dynamic variables - maps variable names to variables.
	 */
	private Map<String, IDynamicVariable> fDynamicVariables;

	/**
	 * Value variables - maps variable names to variables.
	 */
	private Map<String, IStringVariable> fValueVariables;

	/**
	 * Variable listeners
	 */
	private ListenerList<IValueVariableListener> fListeners;

	// notifications
	private static final int ADDED = 0;
	private static final int CHANGED = 1;
	private static final int REMOVED = 2;

	/**
	 * Singleton variable manager.
	 */
	private static StringVariableManager fgManager;

	// true during internal updates indicates that change notification
	// should be suppressed/ignored.
	private boolean fInternalChange = false;

	// Variable extension point constants
	private static final String ATTR_NAME= "name"; //$NON-NLS-1$
	private static final String ATTR_DESCRIPTION="description"; //$NON-NLS-1$
	private static final String ATTR_READ_ONLY="readOnly"; //$NON-NLS-1$
	// Persisted variable XML constants
	private static final String VALUE_VARIABLES_TAG= "valueVariables"; //$NON-NLS-1$
	private static final String VALUE_VARIABLE_TAG= "valueVariable"; //$NON-NLS-1$
	private static final String NAME_TAG= "name"; //$NON-NLS-1$
	private static final String VALUE_TAG= "value"; //$NON-NLS-1$
	private static final String DESCRIPTION_TAG="description"; //$NON-NLS-1$
	private static final String READ_ONLY_TAG="readOnly"; //$NON-NLS-1$
	// XML values
	private static final String TRUE_VALUE= "true"; //$NON-NLS-1$
	private static final String FALSE_VALUE= "false"; //$NON-NLS-1$
	// preference store key for value variables
	private static final String PREF_VALUE_VARIABLES= VariablesPlugin.getUniqueIdentifier() + ".valueVariables"; //$NON-NLS-1$

	/**
	 * Notifies a string variable listener in a safe runnable to handle
	 * exceptions.
	 */
	class StringVariableNotifier implements ISafeRunnable {

		private IValueVariableListener fListener;
		private int fType;
		private IValueVariable[] fVariables;

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#handleException(java.lang.Throwable)
		 */
		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, "An exception occurred during string variable change notification", exception); //$NON-NLS-1$
			VariablesPlugin.log(status);
		}

		/**
		 * @see org.eclipse.core.runtime.ISafeRunnable#run()
		 */
		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
					fListener.variablesAdded(fVariables);
					break;
				case REMOVED:
					fListener.variablesRemoved(fVariables);
					break;
				case CHANGED:
					fListener.variablesChanged(fVariables);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies the given listener of the add/change/remove
		 *
		 * @param variables the {@link IValueVariable}s to notify about
		 * @param update the type of change
		 */
		public void notify(IValueVariable[] variables, int update) {
			fVariables = variables;
			fType = update;
			for (IValueVariableListener iValueVariableListener : fListeners) {
				fListener = iValueVariableListener;
				SafeRunner.run(this);
			}
			fVariables = null;
			fListener = null;
			// persist variables whenever there is an add/change/remove
			storeValueVariables();
		}
	}

	/**
	 * Returns a new notifier.
	 *
	 * @return a new notifier
	 */
	private StringVariableNotifier getNotifier() {
		return new StringVariableNotifier();
	}

	/**
	 * Returns the default string variable manager
	 *
	 * @return string variable manager
	 */
	public static StringVariableManager getDefault() {
		if (fgManager == null) {
			fgManager = new StringVariableManager();
		}
		return fgManager;
	}

	/**
	 * Constructs a new string variable manager.
	 */
	private StringVariableManager() {
		fListeners = new ListenerList<>();
	}

	/**
	 * Load contributed variables and persisted variables
	 */
	private synchronized void initialize() {
		if (fDynamicVariables == null) {
			fInternalChange = true;
			fDynamicVariables = new HashMap<>(5);
			fValueVariables = new HashMap<>(5);
			loadContributedValueVariables();
			loadPersistedValueVariables();
			loadDynamicVariables();
			InstanceScope.INSTANCE.getNode(VariablesPlugin.PI_CORE_VARIABLES).addPreferenceChangeListener(this);
			fInternalChange = false;
		}
	}

	/**
	 * Loads contributed dynamic variables
	 */
	private void loadDynamicVariables() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(VariablesPlugin.PI_CORE_VARIABLES, EXTENSION_POINT_DYNAMIC_VARIABLES);
		IConfigurationElement elements[]= point.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			String name= element.getAttribute(ATTR_NAME);
			if (name == null) {
				VariablesPlugin.logMessage(NLS.bind("Variable extension missing required 'name' attribute: {0}", new String[] {element.getDeclaringExtension().getLabel()}), null); //$NON-NLS-1$
				continue;
			}
			String description= element.getAttribute(ATTR_DESCRIPTION);
			DynamicVariable variable= new DynamicVariable(name, description, element);
			Object old = fDynamicVariables.put(variable.getName(), variable);
			if (old != null) {
				DynamicVariable oldVariable = (DynamicVariable)old;
				VariablesPlugin.logMessage(NLS.bind("Dynamic variable extension from bundle ''{0}'' overrides existing extension variable ''{1}'' from bundle ''{2}''", //$NON-NLS-1$
						new String[] {element.getDeclaringExtension().getContributor().getName(),oldVariable.getName(),
							oldVariable.getConfigurationElement().getDeclaringExtension().getContributor().getName()}), null);
			}
		}
	}

	/**
	 * Loads contributed value variables. This is done before loading persisted values.
	 */
	private void loadContributedValueVariables() {
		IExtensionPoint point = Platform.getExtensionRegistry().getExtensionPoint(VariablesPlugin.PI_CORE_VARIABLES, EXTENSION_POINT_VALUE_VARIABLES);
		IConfigurationElement elements[]= point.getConfigurationElements();
		for (IConfigurationElement element : elements) {
			String name= element.getAttribute(ATTR_NAME);
			if (name == null) {
				VariablesPlugin.logMessage(NLS.bind("Variable extension missing required 'name' attribute: {0}", new String[] {element.getDeclaringExtension().getLabel()}), null); //$NON-NLS-1$
				continue;
			}
			String description= element.getAttribute(ATTR_DESCRIPTION);
			boolean isReadOnly = TRUE_VALUE.equals(element.getAttribute(ATTR_READ_ONLY));

			IValueVariable variable = new ContributedValueVariable(name, description, isReadOnly, element);
			Object old = fValueVariables.put(name, variable);
			if (old != null) {
				StringVariable oldVariable = (StringVariable)old;
				VariablesPlugin.logMessage(NLS.bind("Contributed variable extension from bundle ''{0}'' overrides existing extension variable ''{1}'' from  bundle ''{2}''", //$NON-NLS-1$
						new String[] {element.getDeclaringExtension().getContributor().getName(),oldVariable.getName(),
							oldVariable.getConfigurationElement().getDeclaringExtension().getContributor().getName()}), null);
			}
		}
	}

	/**
	 * Loads persisted value variables from the preference store.  This is done after
	 * loading value variables from the extension point.  If a persisted variable has the
	 * same name as a extension contributed variable the variable's value will be set to
	 * the persisted value unless either a) The persisted value is <code>null</code>, or
	 * b) the variable is read-only.
	 */
	private void loadPersistedValueVariables() {
		String variablesString = Platform.getPreferencesService().getString(VariablesPlugin.PI_CORE_VARIABLES, PREF_VALUE_VARIABLES, "", null); //$NON-NLS-1$
		if (variablesString.length() == 0) {
			return;
		}
		Element root= null;
		try {
			ByteArrayInputStream stream = new ByteArrayInputStream(variablesString.getBytes(StandardCharsets.UTF_8));
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			parser.setErrorHandler(new DefaultHandler());
			root = parser.parse(stream).getDocumentElement();
		} catch (Exception e) {
			VariablesPlugin.logMessage("An exception occurred while loading persisted value variables.", e); //$NON-NLS-1$
			return;
		}
		if (!root.getNodeName().equals(VALUE_VARIABLES_TAG)) {
			VariablesPlugin.logMessage("Invalid format encountered while loading persisted value variables.", null); //$NON-NLS-1$
			return;
		}
		NodeList list= root.getChildNodes();
		for (int i= 0, numItems= list.getLength(); i < numItems; i++) {
			Node node= list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element= (Element) node;
				if (!element.getNodeName().equals(VALUE_VARIABLE_TAG)) {
					VariablesPlugin.logMessage(NLS.bind("Invalid XML element encountered while loading value variables: {0}", new String[] {node.getNodeName()}), null); //$NON-NLS-1$
					continue;
				}
				String name= element.getAttribute(NAME_TAG);
				if (name.length() > 0) {
					String value= element.getAttribute(VALUE_TAG);
					String description= element.getAttribute(DESCRIPTION_TAG);
					boolean readOnly= TRUE_VALUE.equals(element.getAttribute(READ_ONLY_TAG));

					IValueVariable existing = getValueVariable(name);
					if (existing == null){
						ValueVariable variable = new ValueVariable(name, description, readOnly, value);
						fValueVariables.put(name, variable);
					} else if (!existing.isReadOnly() && value != null){
						existing.setValue(value);
					}
				} else {
					VariablesPlugin.logMessage("Invalid variable entry encountered while loading value variables. Variable name is null.", null); //$NON-NLS-1$
				}
			}
		}
	}

	@Override
	public synchronized IStringVariable[] getVariables() {
		initialize();
		List<IStringVariable> list = new ArrayList<>(fDynamicVariables.size() + fValueVariables.size());
		list.addAll(fDynamicVariables.values());
		list.addAll(fValueVariables.values());
		return list.toArray(new IStringVariable[list.size()]);
	}

	@Override
	public synchronized IValueVariable[] getValueVariables() {
		initialize();
		return fValueVariables.values().toArray(new IValueVariable[fValueVariables.size()]);
	}

	@Override
	public synchronized IDynamicVariable[] getDynamicVariables() {
		initialize();
		return fDynamicVariables.values().toArray(new IDynamicVariable[fDynamicVariables.size()]);
	}

	@Override
	public String performStringSubstitution(String expression) throws CoreException {
		return performStringSubstitution(expression, true);
	}

	@Override
	public IValueVariable newValueVariable(String name, String description) {
		return newValueVariable(name, description, false, null);
	}

	@Override
	public IValueVariable newValueVariable(String name, String description, boolean readOnly, String value) {
		return new ValueVariable(name, description, readOnly, value);
	}

	@Override
	public synchronized void addVariables(IValueVariable[] variables) throws CoreException {
		initialize();
		MultiStatus status = new MultiStatus(VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, VariablesMessages.StringVariableManager_26, null);
		for (IValueVariable variable : variables) {
			if (getValueVariable(variable.getName()) != null) {
				status.add(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), VariablesPlugin.INTERNAL_ERROR, NLS.bind(VariablesMessages.StringVariableManager_27, new String[]{variable.getName()}), null));
			}
		}
		if (status.isOK()) {
			for (IValueVariable variable : variables) {
				fValueVariables.put(variable.getName(), variable);
			}
			IValueVariable[] copy = new IValueVariable[variables.length];
			System.arraycopy(variables, 0, copy, 0, variables.length);
			getNotifier().notify(copy, ADDED);
			return;
		}
		throw new CoreException(status);
	}

	@Override
	public synchronized void removeVariables(IValueVariable[] variables) {
		initialize();
		List<IValueVariable> removed = new ArrayList<>(variables.length);
		for (IValueVariable variable : variables) {
			if (fValueVariables.remove(variable.getName()) != null) {
				removed.add(variable);
			}
		}
		if (removed.size() > 0) {
			getNotifier().notify(removed.toArray(new IValueVariable[removed.size()]), REMOVED);
		}
	}

	@Override
	public synchronized IDynamicVariable getDynamicVariable(String name) {
		initialize();
		return fDynamicVariables.get(name);
	}

	@Override
	public synchronized IValueVariable getValueVariable(String name) {
		initialize();
		return (IValueVariable) fValueVariables.get(name);
	}


	@Override
	public void addValueVariableListener(IValueVariableListener listener) {
		fListeners.add(listener);
	}

	@Override
	public void removeValueVariableListener(IValueVariableListener listener) {
		fListeners.remove(listener);
	}

	/**
	 * Returns a memento representing the value variables currently registered.
	 *
	 * @return memento representing the value variables currently registered
	 * @throws IOException if an I/O exception occurs while creating the XML.
	 * @throws ParserConfigurationException if an I/O exception occurs while creating the XML.
	 * @throws TransformerException if an exception occurs while creating the XML.
	 */
	private String getValueVariablesAsXML() throws IOException, ParserConfigurationException, TransformerException {
		IValueVariable[] variables = getValueVariables();

		Document document= getDocument();
		Element rootElement= document.createElement(VALUE_VARIABLES_TAG);
		document.appendChild(rootElement);
		for (IValueVariable variable : variables) {
			if (!variable.isReadOnly()){
				// don't persist read-only variables or un-initialized contributed variables
				if (!variable.isContributed() || ((ContributedValueVariable)variable).isInitialized()) {
					Element element= document.createElement(VALUE_VARIABLE_TAG);
					element.setAttribute(NAME_TAG, variable.getName());
					String value= variable.getValue();
					if (value != null) {
						element.setAttribute(VALUE_TAG, value);
					}
					element.setAttribute(READ_ONLY_TAG, variable.isReadOnly() ? TRUE_VALUE : FALSE_VALUE);
					String description= variable.getDescription();
					if (description != null) {
						element.setAttribute(DESCRIPTION_TAG, description);
					}
					rootElement.appendChild(element);
				}
			}
		}
		return serializeDocument(document);
	}

	private Document getDocument() throws ParserConfigurationException {
		DocumentBuilderFactory dfactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder docBuilder = dfactory.newDocumentBuilder();
		Document doc =docBuilder.newDocument();
		return doc;
	}

	/**
	 * Serializes a XML document into a string - encoded in UTF8 format,
	 * with platform line separators.
	 *
	 * @param doc document to serialize
	 * @return the document as a string
	 * @throws TransformerException if an unrecoverable error occurs during the serialization
	 * @throws UnsupportedEncodingException if the encoding attempted to be used is not supported
	 */
	private String serializeDocument(Document doc) throws TransformerException, UnsupportedEncodingException {
		ByteArrayOutputStream s= new ByteArrayOutputStream();

		TransformerFactory factory= TransformerFactory.newInstance();
		Transformer transformer= factory.newTransformer();
		transformer.setOutputProperty(OutputKeys.METHOD, "xml"); //$NON-NLS-1$
		transformer.setOutputProperty(OutputKeys.INDENT, "yes"); //$NON-NLS-1$

		DOMSource source= new DOMSource(doc);
		StreamResult outputTarget= new StreamResult(s);
		transformer.transform(source, outputTarget);

		return s.toString("UTF8"); //$NON-NLS-1$
	}

	/**
	 * Saves the value variables currently registered in the
	 * preference store.
	 */
	private synchronized void storeValueVariables() {
		String variableString= ""; //$NON-NLS-1$
		if (!fValueVariables.isEmpty()) {
			try {
				variableString= getValueVariablesAsXML();
			} catch (IOException e) {
				VariablesPlugin.log(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), IStatus.ERROR, "An exception occurred while storing launch configuration variables.", e)); //$NON-NLS-1$
				return;
			} catch (ParserConfigurationException e) {
				VariablesPlugin.log(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), IStatus.ERROR, "An exception occurred while storing launch configuration variables.", e)); //$NON-NLS-1$
				return;
			} catch (TransformerException e) {
				VariablesPlugin.log(new Status(IStatus.ERROR, VariablesPlugin.getUniqueIdentifier(), IStatus.ERROR, "An exception occurred while storing launch configuration variables.", e)); //$NON-NLS-1$
				return;
			}
		}
		fInternalChange = true;
		try {
			IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(VariablesPlugin.PI_CORE_VARIABLES);
			prefs.put(PREF_VALUE_VARIABLES, variableString);
			prefs.flush();
		}
		catch(BackingStoreException bse) {
			VariablesPlugin.log(bse);
		}
		fInternalChange = false;
	}

	/**
	 * Fire a change notification for the given variable.
	 *
	 * @param variable the variable that has changed
	 */
	protected void notifyChanged(IValueVariable variable) {
		if (!fInternalChange) {
			IValueVariable existing = getValueVariable(variable.getName());
			if (variable.equals(existing)) {
				// do not do change notification for unregistered variables
				getNotifier().notify(new IValueVariable[]{variable}, CHANGED);
			}
		}
	}

	@Override
	public String generateVariableExpression(String varName, String arg) {
		StringBuilder buffer = new StringBuilder();
		buffer.append("${"); //$NON-NLS-1$
		buffer.append(varName);
		if (arg != null) {
			buffer.append(":"); //$NON-NLS-1$
			buffer.append(arg);
		}
		buffer.append("}"); //$NON-NLS-1$
		return buffer.toString();
	}

	@Override
	public String performStringSubstitution(String expression,	boolean reportUndefinedVariables) throws CoreException {
		return new StringSubstitutionEngine().performStringSubstitution(expression, reportUndefinedVariables, true, this);
	}

	@Override
	public void validateStringVariables(String expression) throws CoreException {
		new StringSubstitutionEngine().validateStringVariables(expression, this);
	}

	@Override
	public String getContributingPluginId(IStringVariable variable) {
		if (variable instanceof StringVariable) {
			return ((StringVariable) variable).getConfigurationElement().getContributor().getName();
		}
		return null;
	}

	@Override
	public void preferenceChange(PreferenceChangeEvent event) {
		if (PREF_VALUE_VARIABLES.equals(event.getKey())) {
			synchronized (this) {
				if (!fInternalChange) {
					fValueVariables.clear();
					loadPersistedValueVariables();
					loadContributedValueVariables();
				}
			}
		}
	}
}
