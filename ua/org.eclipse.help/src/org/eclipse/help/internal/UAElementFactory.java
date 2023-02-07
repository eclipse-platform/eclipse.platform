/*******************************************************************************
 * Copyright (c) 2007, 2020 IBM Corporation and others.
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
package org.eclipse.help.internal;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.core.runtime.Platform;
import org.eclipse.help.IAnchor;
import org.eclipse.help.ICommandLink;
import org.eclipse.help.IContentExtension;
import org.eclipse.help.IContext;
import org.eclipse.help.ICriteria;
import org.eclipse.help.ICriteriaDefinition;
import org.eclipse.help.ICriterionDefinition;
import org.eclipse.help.ICriterionValueDefinition;
import org.eclipse.help.IInclude;
import org.eclipse.help.IIndex;
import org.eclipse.help.IIndexEntry;
import org.eclipse.help.IIndexSee;
import org.eclipse.help.IIndexSubpath;
import org.eclipse.help.ILink;
import org.eclipse.help.IToc;
import org.eclipse.help.ITopic;
import org.eclipse.help.IUAElement;
import org.eclipse.help.UAContentFilter;
import org.eclipse.help.internal.context.Context;
import org.eclipse.help.internal.criteria.Criteria;
import org.eclipse.help.internal.criteria.CriteriaDefinition;
import org.eclipse.help.internal.criteria.CriterionDefinition;
import org.eclipse.help.internal.criteria.CriterionValueDefinition;
import org.eclipse.help.internal.extension.ContentExtension;
import org.eclipse.help.internal.index.Index;
import org.eclipse.help.internal.index.IndexEntry;
import org.eclipse.help.internal.index.IndexSee;
import org.eclipse.help.internal.index.IndexSubpath;
import org.eclipse.help.internal.toc.Link;
import org.eclipse.help.internal.toc.Toc;
import org.w3c.dom.Element;

/*
 * Constructs typed model elements from DOM elements or interface
 * implementations.
 */
public class UAElementFactory {

	private static final Class<?>[][] interfaceTable = new Class[][] {
		{ ITopic.class, Topic.class },
		{ IIndexEntry.class, IndexEntry.class },
		{ IContext.class, Context.class },
		{ IAnchor.class, Anchor.class },
		{ IInclude.class, Include.class },
		{ ILink.class, Link.class },
		{ IIndexSee.class, IndexSee.class },
		{ IIndexSubpath.class, IndexSubpath.class },
		{ IToc.class, Toc.class },
		{ ICommandLink.class, CommandLink.class },
		{ IIndex.class, Index.class },
		{ IContentExtension.class, ContentExtension.class },
		{ ICriteria.class, Criteria.class },
		{ ICriteriaDefinition.class, CriteriaDefinition.class },
		{ ICriterionDefinition.class, CriterionDefinition.class },
		{ ICriterionValueDefinition.class, CriterionValueDefinition.class },
	};

	private static final Map<String, Class<?>> classByElementName;

	static {
		classByElementName = Collections.synchronizedMap(new HashMap<>());
		classByElementName.put(Anchor.NAME, Anchor.class);
		classByElementName.put(Include.NAME, Include.class);
		classByElementName.put(Toc.NAME, Toc.class);
		classByElementName.put(Topic.NAME, Topic.class);
		classByElementName.put(Index.NAME, Index.class);
		classByElementName.put(IndexEntry.NAME, IndexEntry.class);
		classByElementName.put(Context.NAME, Context.class);
		classByElementName.put(CommandLink.NAME, CommandLink.class);
		classByElementName.put(Link.NAME, Link.class);
		classByElementName.put(IndexSee.NAME, IndexSee.class);
		classByElementName.put(IndexSubpath.NAME, IndexSubpath.class);
		classByElementName.put(Criteria.NAME, Criteria.class);
		classByElementName.put(CriteriaDefinition.NAME, CriteriaDefinition.class);
		classByElementName.put(CriterionDefinition.NAME, CriterionDefinition.class);
		classByElementName.put(CriterionValueDefinition.NAME, CriterionValueDefinition.class);
		classByElementName.put(ContentExtension.NAME_CONTRIBUTION, ContentExtension.class);
		classByElementName.put(ContentExtension.NAME_CONTRIBUTION_LEGACY, ContentExtension.class);
		classByElementName.put(ContentExtension.NAME_REPLACEMENT, ContentExtension.class);
		classByElementName.put(ContentExtension.NAME_REPLACEMENT_LEGACY, ContentExtension.class);
	}

	public static UAElement newElement(Element element) {
		String name = element.getNodeName();
		Class<?> clazz = classByElementName.get(name);
		if (clazz != null) {
			try {
				Constructor<?> constructor = clazz.getConstructor(Element.class);
				return (UAElement) constructor.newInstance(element);
			}
			catch (Exception e) {
				String msg = "Error creating document model element"; //$NON-NLS-1$
				Platform.getLog(UAElementFactory.class).error(msg, e);
			}
		}
		return new UAElement(element);
	}

	public static UAElement newElement(IUAElement src) {
		for (int i=0;i<interfaceTable.length;++i) {
			Class<?> interfaze = interfaceTable[i][0];
			Class<?> clazz = interfaceTable[i][1];
			if (interfaze.isAssignableFrom(src.getClass())) {
				try {
					Constructor<?> constructor = clazz.getConstructor(interfaze);
					return (UAElement) constructor.newInstance(src);
				}
				catch (Exception e) {
					String msg = "Error creating document model element"; //$NON-NLS-1$
					Platform.getLog(UAContentFilter.class).error(msg, e);
				}
			}
		}
		return null;
	}
}
