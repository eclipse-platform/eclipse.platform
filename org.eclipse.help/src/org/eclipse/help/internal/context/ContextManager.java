/*******************************************************************************
 * Copyright (c) 2000, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractContextProvider;
import org.eclipse.help.IContext;
import org.eclipse.help.internal.HelpPlugin;

/*
 * Manages all context-sensitive help data for the help system.
 */
public class ContextManager {

	private static final String EXTENSION_POINT_ID_CONTEXT = HelpPlugin.PLUGIN_ID + ".contexts"; //$NON-NLS-1$
	private static final String ELEMENT_NAME_CONTEXT_PROVIDER = "contextProvider"; //$NON-NLS-1$
	private static final String ATTRIBUTE_NAME_CLASS = "class"; //$NON-NLS-1$

	private Map<String, List<AbstractContextProvider>> providersByPluginId;
	private Map<String, List<String>> contextIDsByPluginId;
	private List<AbstractContextProvider> globalProviders;

	public ContextManager()
	{
		if (HelpPlugin.DEBUG_CONTEXT)
			checkContextProviders();
	}

	/*
	 * Returns the Context for the given id and locale.
	 */
	public IContext getContext(String contextId, String locale) {


		if (HelpPlugin.DEBUG_CONTEXT  && contextId != null) {
			System.out.println("ContextManager.getContext(\"" + contextId + "\")"); //$NON-NLS-1$ //$NON-NLS-2$
		}

		// ask the providers
		int index = contextId.lastIndexOf('.');
		if (index != -1) {
			String pluginId = contextId.substring(0, index);
			Iterator<AbstractContextProvider> iter = getContextProviders(pluginId).iterator();
			while (iter.hasNext()) {
				AbstractContextProvider provider = iter.next();
				try {
					IContext context = provider.getContext(contextId, locale);
					if (context != null) {
						if (HelpPlugin.DEBUG_CONTEXT) {
							System.out.println("ContextManager.getContext found context, description = \"" + context.getText() + '"'); //$NON-NLS-1$
						}
						return new Context(context, contextId);
					}
				}
				catch (Throwable t) {
					// log and skip
					String msg = "Error querying context provider (" + provider.getClass().getName() + ") with context Id: " + contextId; //$NON-NLS-1$ //$NON-NLS-2$
					ILog.of(getClass()).error(msg, t);
				}
			}
		}

		if (HelpPlugin.DEBUG_CONTEXT) {
			System.out.println("ContextManager.getContext - no context found"); //$NON-NLS-1$

			String id = contextId;
			ArrayList<String> potentialMatches = new ArrayList<>();
			if ((index = contextId.lastIndexOf('.'))>-1)
				id = contextId.substring(index+1);

			String warning = "Registered Context Provider IDs:\n"; //$NON-NLS-1$
			Iterator<String> iter = contextIDsByPluginId.keySet().iterator();
			warning+="--------------------------------\n"; //$NON-NLS-1$
			while (iter.hasNext()) {
				String pluginID = iter.next();
				List<String> contextIDList = contextIDsByPluginId.get(pluginID);
				for (int c = 0; c < contextIDList.size(); c++) {
					if (contextIDList.get(c).equalsIgnoreCase(id)) {
						potentialMatches.add(pluginID+'.'+contextIDList.get(c));
						break;
					}
				}

				warning+=pluginID+' '+contextIDList.toString()+'\n';
			}
			warning+="--------------------------------"; //$NON-NLS-1$
			System.out.println(warning);

			if (!potentialMatches.isEmpty())
				System.out.println("The ID searched is "+contextId+".  Did you mean to call setHelp with:\n"+potentialMatches); //$NON-NLS-1$ //$NON-NLS-2$
		}

		return null;
	}

	/*
	 * Returns all registered context providers (potentially cached) for the
	 * given plug-in id.
	 */
	private List<AbstractContextProvider> getContextProviders(String pluginId) {
		if (providersByPluginId == null) {
			loadContextProviders();
		}
		List<AbstractContextProvider> list = new ArrayList<>();
		List<AbstractContextProvider> forPlugin = providersByPluginId.get(pluginId);
		if (forPlugin != null) {
			list.addAll(forPlugin);
		}
		list.addAll(globalProviders);
		return list;
	}

	/*
	 * Finds and instantiates all registered context-sensitive help providers.
	 */
	private void loadContextProviders() {
		providersByPluginId = new HashMap<>();
		globalProviders = new ArrayList<>();

		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry.getConfigurationElementsFor(EXTENSION_POINT_ID_CONTEXT);
		for (int i=0;i<elements.length;++i) {
			IConfigurationElement elem = elements[i];
			if (elem.getName().equals(ELEMENT_NAME_CONTEXT_PROVIDER)) {
				try {
					AbstractContextProvider provider = (AbstractContextProvider)elem.createExecutableExtension(ATTRIBUTE_NAME_CLASS);
					String[] plugins = provider.getPlugins();
					if (plugins != null) {
						for (int j=0;j<plugins.length;++j) {
							List<AbstractContextProvider> list = providersByPluginId.get(plugins[j]);
							if (list == null) {
								list = new ArrayList<>();
								providersByPluginId.put(plugins[j], list);
							}
							list.add(provider);
						}
					} else {
						globalProviders.add(provider);
					}
				}
				catch (CoreException e) {
					// log and skip
					String msg = "Error instantiating context-sensitive help provider class \"" + elem.getAttribute(ATTRIBUTE_NAME_CLASS) + '"'; //$NON-NLS-1$
					ILog.of(getClass()).error(msg, e);
				}
			}
		}
	}

	/*
	 * Method only called when debugging context
	 * sensitive help.
	 *
	 * Checks to see if there are any duplicate ids
	 *
	 */
	private void checkContextProviders()
	{
		contextIDsByPluginId = new Hashtable<>();
		Hashtable<String, Context> contextByContextID = new Hashtable<>();

		if (providersByPluginId == null) {
			loadContextProviders();
		}

		Iterator<String> i = providersByPluginId.keySet().iterator();

		while (i.hasNext())
		{
			String pluginID = i.next();
			List<AbstractContextProvider> providers = providersByPluginId.get(pluginID);

			for (int p = 0; p < providers.size(); p++) {
				ContextFileProvider provider = (ContextFileProvider)providers.get(p);
				Map<String, Context>[] maps = provider.getPluginContexts(pluginID, Platform.getNL());

				for (int m = 0; m < maps.length; m++) {
					Iterator<String> i2 = maps[m].keySet().iterator();
					while (i2.hasNext()) {
						String contextID = i2.next();
						String fullID = pluginID+'.'+contextID;
						Context currentContext = maps[m].get(contextID);

						if (!contextByContextID.containsKey(fullID)) {
							contextByContextID.put(fullID,currentContext);
						} else if (HelpPlugin.DEBUG_CONTEXT) {
							Context initialContext = contextByContextID.get(fullID);
							String error = "Context Help ID '"+contextID+"' is found in multiple context files in plugin '"+pluginID+"'\n"+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
								" Description 1: "+initialContext.getText()+'\n'+ //$NON-NLS-1$
								" Description 2: "+currentContext.getText(); //$NON-NLS-1$

							System.out.println(error);
						}

						List<String> list = contextIDsByPluginId.get(pluginID);
						if (list==null){
							list = new ArrayList<>();
							contextIDsByPluginId.put(pluginID,list);
						}
						list.add(contextID);
					}
				}
			}
		}
	}
}
