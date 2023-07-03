/*******************************************************************************
 * Copyright (c) 2010, 2020 IBM Corporation and others.
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

package org.eclipse.help.internal.base.scope;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.Platform;
import org.eclipse.help.base.AbstractHelpScope;
import org.eclipse.help.base.IHelpScopeProducer;
import org.eclipse.help.base.IScopeHandle;

public class ScopeRegistry {

	public static final String SCOPE_XP_NAME = "org.eclipse.help.base.scope"; //$NON-NLS-1$
	public static final String ENABLEMENT_SCOPE_ID = "org.eclipse.help.enablement"; //$NON-NLS-1$
	public static final String SEARCH_SCOPE_SCOPE_ID = "org.eclipse.help.searchscope"; //$NON-NLS-1$

	public static final String SCOPE_AND = "^"; //$NON-NLS-1$
	public static final String SCOPE_OR = "|"; //$NON-NLS-1$

	private static List<IScopeHandle> scopes = null;

	private boolean initialized = false;

	private static class RegistryHolder {
		static final ScopeRegistry instance = new ScopeRegistry();
	}

	private ScopeRegistry() {
	}

	public static ScopeRegistry getInstance() {
		return RegistryHolder.instance;
	}

	public AbstractHelpScope getScope(String id) {
		if (id == null) {
			return new UniversalScope();
		}
		readScopes();


		// Lookup in scope registry
		for (IScopeHandle handle : scopes) {
			if (id.equals(handle.getId())) {
				return handle.getScope();
			}
		}
		return null;
	}

	synchronized private void readScopes() {
		if (initialized ) {
			return;
		}
		scopes = new ArrayList<>();
		IExtensionRegistry registry = Platform.getExtensionRegistry();
		IConfigurationElement[] elements = registry
				.getConfigurationElementsFor(SCOPE_XP_NAME);
		for (IConfigurationElement element : elements) {

			Object obj = null;
			try {
				obj = element.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				ILog.of(getClass()).error("Create extension failed:[" + SCOPE_XP_NAME + "].", e); //$NON-NLS-1$ //$NON-NLS-2$
			}
			if (obj instanceof AbstractHelpScope) {
				String id = element.getAttribute("id"); //$NON-NLS-1$
				IScopeHandle filter = new ScopeHandle(id, (AbstractHelpScope) obj);
				scopes.add(filter);
			}
			else if (obj instanceof IHelpScopeProducer)
			{
				IScopeHandle dynamicScopes[] = ((IHelpScopeProducer)obj).getScopeHandles();
				Collections.addAll(scopes, dynamicScopes);
			}
		}
		initialized = true;
	}

	public IScopeHandle[] getScopes() {
		readScopes();
		return scopes.toArray(new IScopeHandle[scopes.size()]);
	}

	/**
	 * Parse logical sets of Scopes.  All phrases in the
	 * array are intersected together
	 *
	 * @param phrases
	 * @return
	 */
	public AbstractHelpScope parseScopePhrases(String phrases[])
	{
		ArrayList<AbstractHelpScope> scopes = new ArrayList<>();

		for (String phrase : phrases) {
			AbstractHelpScope scope = parseScopePhrase(phrase);
			if (scope!=null)
				scopes.add(scope);
		}

		if (scopes.isEmpty())
			return null;
		if (scopes.size()==1)
			return scopes.get(0);
		return new IntersectionScope(
				scopes.toArray(
						new AbstractHelpScope[scopes.size()]));
	}

	/**
	 * Parse a logical phrase of scope names.  i.e.:
	 * (A^B)|C
	 *
	 * @param phrase
	 * @return
	 */
	public AbstractHelpScope parseScopePhrase(String phrase)
	{
		if (!(phrase.startsWith("(") && !phrase.startsWith("("))) //$NON-NLS-1$ //$NON-NLS-2$
			phrase = '('+phrase+')';

		Stack<TempScope> scopeStack = new Stack<>();
		ScopePhrase scopePhrase = new ScopePhrase(phrase);

		String elem;

		while ((elem = scopePhrase.getNextElement())!=null)
		{
			if (elem.equals("(")) //$NON-NLS-1$
			{
				TempScope scope = new TempScope();
				scope.setType(TempScope.SELF);
				scopeStack.push(scope);
			}
			else if (elem.equals(")")) //$NON-NLS-1$
			{
				TempScope scope = scopeStack.pop();
				if (scopeStack.isEmpty())
					return scope.getScope();
				else{
					TempScope parent = scopeStack.peek();
					parent.add(scope.getScope());
				}
			}
			else if (elem.equals(SCOPE_AND))
			{
				TempScope scope = scopeStack.peek();
				scope.setType(TempScope.INTERSECTION);
			}
			else if (elem.equals(SCOPE_OR))
			{
				TempScope scope = scopeStack.peek();
				scope.setType(TempScope.UNION);
			}
			else
			{
				TempScope scope = scopeStack.peek();
				AbstractHelpScope helpScope = getScope(elem);
				if (helpScope!=null)
					scope.add(helpScope);
			}
		}
		return null;
	}

	/**
	 * A class used to parse a logical scope phrase, by
	 * returning each part of the phrase as a separate element
	 *
	 */
	static class ScopePhrase{

		private String phrase;
		private int cursor;

		public ScopePhrase(String phrase)
		{
			this.phrase = phrase;
			this.cursor = 0;
		}

		public String getNextElement()
		{
			String next = ""; //$NON-NLS-1$

			for (;cursor<phrase.length();cursor++)
			{
				char current = phrase.charAt(cursor);
				if (current=='(')
					return format(next,current);
				if (current==')')
					return format(next,current);
				if ((current+"").equals(SCOPE_AND)) //$NON-NLS-1$
					return format(next,current);
				if ((current+"").equals(SCOPE_OR)) //$NON-NLS-1$
					return format(next,current);
				next+=current;
			}
			if (next.isEmpty())
				return null;
			return next;
		}

		private String format(String next,char current)
		{
			if (next.isEmpty())
			{
				cursor++;
				return current+""; //$NON-NLS-1$
			}
			else
				return next;
		}
	}

	/**
	 * A class used to contruct a logical AbstractHelpScope based
	 * on one Scope, or a union/intersection of scopes.
	 *
	 */
	private static class TempScope
	{
		public final static int SELF=0;
		public final static int UNION=1;
		public final static int INTERSECTION=2;

		private ArrayList<AbstractHelpScope> kids = new ArrayList<>();
		private int type;

		public void setType(int type)
		{
			this.type = type;
		}

		public void add(AbstractHelpScope kid)
		{
			kids.add(kid);
		}

		public AbstractHelpScope getScope()
		{
			switch (type){
			case UNION:
				return new UnionScope(
						kids.toArray(
								new AbstractHelpScope[kids.size()]));
			case INTERSECTION:
				return new IntersectionScope(
						kids.toArray(
								new AbstractHelpScope[kids.size()]));
			default:
				if (kids.size()>=1)
					return kids.get(0);
				else
					return null;
			}
		}
	}
}
