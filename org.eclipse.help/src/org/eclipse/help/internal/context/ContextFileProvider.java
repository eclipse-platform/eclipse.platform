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
package org.eclipse.help.internal.context;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.AbstractContextProvider;
import org.eclipse.help.IContext;
import org.eclipse.help.IUAElement;
import org.eclipse.help.internal.HelpPlugin;
import org.eclipse.help.internal.Topic;
import org.eclipse.help.internal.UAElement;
import org.eclipse.help.internal.dynamic.DocumentProcessor;
import org.eclipse.help.internal.dynamic.DocumentReader;
import org.eclipse.help.internal.dynamic.DocumentWriter;
import org.eclipse.help.internal.dynamic.ExtensionHandler;
import org.eclipse.help.internal.dynamic.IncludeHandler;
import org.eclipse.help.internal.dynamic.ProcessorHandler;
import org.eclipse.help.internal.dynamic.ValidationHandler;
import org.eclipse.help.internal.toc.HrefUtil;
import org.eclipse.help.internal.util.ResourceLocator;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/*
 * Provides context-sensitive help data to the help system, contributed from
 * context XML files.
 */
public class ContextFileProvider extends AbstractContextProvider {

	private static final String EXTENSION_POINT_CONTEXTS = "org.eclipse.help.contexts"; //$NON-NLS-1$
	private static final String ELEMENT_CONTEXTS = "contexts"; //$NON-NLS-1$
	private static final String ATTRIBUTE_FILE = "file"; //$NON-NLS-1$
	private static final String ATTRIBUTE_PLUGIN = "plugin"; //$NON-NLS-1$

	// locale -> Map(pluginId -> Map(shortContextId -> Context)[])
	private Map<String, Map<String, Map<String, Context>[]>> pluginContextsByLocale;

	// pluginId -> ContextFile[]
	private Map<String, ContextFile[]> descriptorsByPluginId;

	// locale -> Map(ContextFile -> Map(shortContextId -> Context))
	private Map<String, Map<ContextFile, Map<String, Context>>> contextFilesByLocale;

	private DocumentProcessor processor;
	private DocumentReader reader;
	private DocumentWriter writer;
	private Map<String, String[]> requiredAttributes;

	@Override
	public IContext getContext(String contextId, String locale) {
		int index = contextId.lastIndexOf('.');
		String pluginId = contextId.substring(0, index);
		String shortContextId = contextId.substring(index + 1);

		if (pluginContextsByLocale == null) {
			pluginContextsByLocale = new HashMap<>();
		}
		Map<String, Map<String, Context>[]> pluginContexts = pluginContextsByLocale.get(locale);
		if (pluginContexts == null) {
			pluginContexts = new HashMap<>();
			pluginContextsByLocale.put(locale, pluginContexts);
		}
		Map<String, Context>[] contexts = pluginContexts.get(pluginId);
		if (contexts == null) {
			contexts = getPluginContexts(pluginId, locale);
			pluginContexts.put(pluginId, contexts);
		}
		ArrayList<IContext> matches = new ArrayList<>();
		for (int i=0;i<contexts.length;++i) {
			// Search for contexts
			Context context = contexts[i].get(shortContextId);
			if (context != null) {
				matches.add(context);
			}
		}
		switch (matches.size()) {
		case 0:
			return null;
		case 1:
			return matches.get(0);
		default:
			// Merge the contexts - this is the least common case
			Context newContext = new Context(matches.get(0), shortContextId);
			for (int i = 1; i < matches.size(); i++) {
				newContext.mergeContext(matches.get(i));
			}
			return newContext;
		}
	}

	@Override
	public String[] getPlugins() {
		Map<String, ContextFile[]> associations = getPluginAssociations();
		return associations.keySet().toArray(new String[associations.size()]);
	}

	/*
	 * Returns a mapping of plug-in IDs to arrays of context files that apply
	 * to that plug-in (pluginId -> ContextFile[]).
	 */
	private Map<String, ContextFile[]> getPluginAssociations() {
		if (descriptorsByPluginId == null) {
			descriptorsByPluginId = new HashMap<>();
			IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor(EXTENSION_POINT_CONTEXTS);
			for (int i=0;i<elements.length;++i) {
				if (ELEMENT_CONTEXTS.equals(elements[i].getName())) {
					String declaringPluginId = elements[i].getDeclaringExtension().getContributor().getName();
					String file = elements[i].getAttribute(ATTRIBUTE_FILE);
					String plugin = elements[i].getAttribute(ATTRIBUTE_PLUGIN);
					String targetPluginId = (plugin == null ? declaringPluginId : plugin);
					ContextFile descriptor = new ContextFile(declaringPluginId, file);
					ContextFile[] descriptors = descriptorsByPluginId.get(targetPluginId);
					if (descriptors == null) {
						descriptors = new ContextFile[] { descriptor };
					}
					else {
						ContextFile[] temp = new ContextFile[descriptors.length + 1];
						System.arraycopy(descriptors, 0, temp, 0, descriptors.length);
						temp[descriptors.length] = descriptor;
						descriptors = temp;
					}
					descriptorsByPluginId.put(targetPluginId, descriptors);
				}
			}
		}
		return descriptorsByPluginId;
	}

	/*
	 * Returns the context definitions for the given plug-in and locale,
	 * as a mapping of short IDs to Context objects (shortContextId -> Context).
	 */
	@SuppressWarnings("unchecked")
	public Map<String, Context>[] getPluginContexts(String pluginId, String locale) {
		List<Map<String, Context>> maps = new ArrayList<>();
		Map<String, ContextFile[]> associations = getPluginAssociations();
		ContextFile[] descriptors = associations.get(pluginId);
		for (int i=0;i<descriptors.length;++i) {
			Map<String, Context> contexts = getContexts(descriptors[i], locale);
			if (contexts != null) {
				maps.add(contexts);
			}
		}
		return maps.toArray(new Map[maps.size()]);
	}

	/*
	 * Returns the context definitions stored in the given file for the given
	 * locale (shortContextId -> Context).
	 */
	private Map<String, Context> getContexts(ContextFile descriptor, String locale) {
		if (contextFilesByLocale == null) {
			contextFilesByLocale = new HashMap<>();
		}
		Map<ContextFile, Map<String, Context>> contextsByDescriptor = contextFilesByLocale.get(locale);
		if (contextsByDescriptor == null) {
			contextsByDescriptor = new HashMap<>();
			contextFilesByLocale.put(locale, contextsByDescriptor);
		}
		Map<String, Context> contexts = contextsByDescriptor.get(descriptor);
		if (contexts == null) {
			contexts = loadContexts(descriptor, locale);
			if (contexts != null) {
				contextsByDescriptor.put(descriptor, contexts);
			}
		}
		return contexts;
	}

	/*
	 * Loads the given context file for the given locale, and returns its
	 * contents as a mapping from short context ids to Context objects
	 * (shortContextId -> Context).
	 */
	private Map<String, Context> loadContexts(ContextFile descriptor, String locale) {
		// load the file
		try (InputStream in = ResourceLocator.openFromPlugin(descriptor.getBundleId(), descriptor.getFile(), locale)) {
			if (in != null) {
				return loadContextsFromInputStream(descriptor, locale, in);
			} else {
				throw new FileNotFoundException();
			}
		} catch (Throwable t) {
			String msg = "Error reading context-sensitive help file /\"" + getErrorPath(descriptor, locale) + "\" (skipping file)"; //$NON-NLS-1$ //$NON-NLS-2$
			Platform.getLog(getClass()).error(msg, t);
		}
		return null;
	}


	private Map<String, Context> loadContextsFromInputStream(ContextFile descriptor, String locale, InputStream in)
			throws Exception {
		if (reader == null) {
			reader = new DocumentReader();
		}
		UAElement root = reader.read(in);
		if ("contexts".equals(root.getElementName())) { //$NON-NLS-1$
			// process dynamic content
			if (processor == null) {
				processor = new DocumentProcessor(new ProcessorHandler[] {
					new ValidationHandler(getRequiredAttributes()),
					new NormalizeHandler(),
					new IncludeHandler(reader, locale),
					new ExtensionHandler(reader, locale)
				});
			}
			processor.process(root, '/' + descriptor.getBundleId() + '/' + descriptor.getFile());

			// build map
			IUAElement[] children = root.getChildren();
			Map<String, Context> contexts = new HashMap<>();
			for (int i=0;i<children.length;++i) {
				if (children[i] instanceof Context) {
					Context context = (Context)children[i];
					String id = context.getId();
					if (id != null) {
						Object existingContext =  contexts.get(id);
						if (existingContext==null)
							contexts.put(id, context);
						else
						{
							((Context)existingContext).mergeContext(context);

							if (HelpPlugin.DEBUG_CONTEXT)
							{
								String error = "Context help ID '"+id+"' is found multiple times in file '"+descriptor.getBundleId()+'/'+descriptor.getFile()+"'\n"+ //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									" Description 1: "+((Context)existingContext).getText()+'\n'+ //$NON-NLS-1$
									" Description 2: "+context.getText(); //$NON-NLS-1$
								System.out.println(error);
							}
						}
					}
				}
			}
			return contexts;
		}
		else {
			String msg = "Required root element \"contexts\" missing from context-sensitive help file \"/" + getErrorPath(descriptor, locale) + "\" (skipping)"; //$NON-NLS-1$ //$NON-NLS-2$
			Platform.getLog(getClass()).error(msg);
		}
		return null;
	}

	private String getErrorPath(ContextFile descriptor, String locale) {
		return ResourceLocator.getErrorPath(descriptor.getBundleId(), descriptor.getFile(), locale);
	}

	private Map<String, String[]> getRequiredAttributes() {
		if (requiredAttributes == null) {
			requiredAttributes = new HashMap<>();
			requiredAttributes.put(Context.NAME, new String[] { Context.ATTRIBUTE_ID });
			requiredAttributes.put(Topic.NAME, new String[] { Topic.ATTRIBUTE_LABEL, Topic.ATTRIBUTE_HREF });
			requiredAttributes.put("anchor", new String[] { "id" }); //$NON-NLS-1$ //$NON-NLS-2$
			requiredAttributes.put("include", new String[] { "path" }); //$NON-NLS-1$ //$NON-NLS-2$
		}
		return requiredAttributes;
	}

	/*
	 * Handler that normalizes:
	 * 1. Descriptions - any child elements like bold tags are serialized and inserted into the
	 *    text node under the description element.
	 * 2. Related topic hrefs - convert from relative (e.g. "path/file.html") to absolute hrefs
	 *    (e.g. "/plugin.id/path/file.html").
	 */
	private class NormalizeHandler extends ProcessorHandler {
		@Override
		public short handle(UAElement element, String id) {
			if (element instanceof Context) {
				Context context = (Context)element;
				IUAElement[] children = context.getChildren();
				if (children.length > 0 && Context.ELEMENT_DESCRIPTION.equals(((UAElement)children[0]).getElementName())) {
					StringBuilder buf = new StringBuilder();
					Element description = ((UAElement)children[0]).getElement();
					Node node = description.getFirstChild();
					while (node != null) {
						if (node.getNodeType() == Node.TEXT_NODE) {
							buf.append(node.getNodeValue());
						}
						else if (node.getNodeType() == Node.ELEMENT_NODE) {
							if (writer == null) {
								writer = new DocumentWriter();
							}
							try {
								buf.append(writer.writeString((Element)node, false));
							}
							catch (TransformerException e) {
								String msg = "Internal error while normalizing context-sensitive help descriptions"; //$NON-NLS-1$
								Platform.getLog(getClass()).error(msg, e);
							}
						}
						Node old = node;
						node = node.getNextSibling();
						description.removeChild(old);
					}
					Document document = description.getOwnerDocument();
					description.appendChild(document.createTextNode(buf.toString()));
				}
			}
			else if (element instanceof Topic) {
				Topic topic = (Topic)element;
				String href = topic.getHref();
				if (href != null) {
					int index = id.indexOf('/', 1);
					if (index != -1) {
						String pluginId = id.substring(1, index);
						topic.setHref(HrefUtil.normalizeHref(pluginId, href));
					}
				}
			}
			// give other handlers an opportunity to process
			return UNHANDLED;
		}
	}
}
