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
 *******************************************************************************/
package org.eclipse.debug.internal.core;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IExpressionListener;
import org.eclipse.debug.core.IExpressionManager;
import org.eclipse.debug.core.IExpressionsListener;
import org.eclipse.debug.core.model.IExpression;
import org.eclipse.debug.core.model.IWatchExpression;
import org.eclipse.debug.core.model.IWatchExpressionDelegate;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * The expression manager manages all registered expressions
 * for the debug plug-in. It is instantiated by the debug plug-in
 * at startup.
 *
 * @see IExpressionManager
 */
public class ExpressionManager extends PlatformObject implements IExpressionManager {

	/**
	 * Ordered collection of registered expressions.
	 */
	private Vector<IExpression> fExpressions = null;

	/**
	 * List of expression listeners
	 */
	private ListenerList<IExpressionListener> fListeners = null;

	/**
	 * List of expressions listeners (plural)
	 */
	private ListenerList<IExpressionsListener> fExpressionsListeners = null;

	/**
	 * Mapping of debug model identifiers (String) to
	 * expression delegate extensions (IConfigurationElement)
	 */
	private Map<String, IConfigurationElement> fWatchExpressionDelegates = new HashMap<>();

	// Constants for add/remove/change/insert/move notification
	private static final int ADDED = 1;
	private static final int CHANGED = 2;
	private static final int REMOVED = 3;
	private static final int INSERTED = 4;
	private static final int MOVED = 5;

	// Preference for persisted watch expressions
	private static final String PREF_WATCH_EXPRESSIONS= "prefWatchExpressions"; //$NON-NLS-1$
	// Persisted watch expression XML tags
	private static final String WATCH_EXPRESSIONS_TAG= "watchExpressions"; //$NON-NLS-1$
	private static final String EXPRESSION_TAG= "expression"; //$NON-NLS-1$
	private static final String TEXT_TAG= "text"; //$NON-NLS-1$
	private static final String ENABLED_TAG= "enabled"; //$NON-NLS-1$
	// XML values
	private static final String TRUE_VALUE= "true"; //$NON-NLS-1$
	private static final String FALSE_VALUE= "false"; //$NON-NLS-1$

	public ExpressionManager() {
		loadPersistedExpressions();
		loadWatchExpressionDelegates();
	}

	/**
	 * Loads the mapping of debug models to watch expression delegates
	 * from the org.eclipse.debug.core.watchExpressionDelegates
	 * extension point.
	 */
	private void loadWatchExpressionDelegates() {
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugPlugin.getUniqueIdentifier(), "watchExpressionDelegates"); //$NON-NLS-1$
		for (IConfigurationElement element : extensionPoint.getConfigurationElements()) {
			if (element.getName().equals("watchExpressionDelegate")) { //$NON-NLS-1$
				String debugModel = element.getAttribute("debugModel"); //$NON-NLS-1$
				if (debugModel == null || debugModel.length() == 0) {
					continue;
				}
				fWatchExpressionDelegates.put(debugModel, element);
			}
		}
	}

	@Override
	public IWatchExpressionDelegate newWatchExpressionDelegate(String debugModel) {
		try {
			IConfigurationElement element= fWatchExpressionDelegates.get(debugModel);
			if (element != null) {
				return (IWatchExpressionDelegate) element.createExecutableExtension(IConfigurationElementConstants.DELEGATE_CLASS);
			}
			return null;
		} catch (CoreException e) {
			DebugPlugin.log(e);
			return null;
		}
	}

	@Override
	public boolean hasWatchExpressionDelegate(String id) {
		IConfigurationElement element= fWatchExpressionDelegates.get(id);
		return element != null;
	}

	/**
	 * Loads any persisted watch expressions from the preferences.
	 * NOTE: It's important that no setter methods are called on
	 * 		the watchpoints which will fire change events as this
	 * 		will cause an infinite loop (see Bug 27281).
	 */
	private void loadPersistedExpressions() {
		String expressionsString = Platform.getPreferencesService().getString(DebugPlugin.getUniqueIdentifier(), PREF_WATCH_EXPRESSIONS, IInternalDebugCoreConstants.EMPTY_STRING, null);
		if (expressionsString.length() == 0) {
			return;
		}
		Element root;
		try {
			root = DebugPlugin.parseDocument(expressionsString);
		} catch (CoreException e) {
			DebugPlugin.logMessage("An exception occurred while loading watch expressions.", e); //$NON-NLS-1$
			return;
		}
		if (!root.getNodeName().equals(WATCH_EXPRESSIONS_TAG)) {
			DebugPlugin.logMessage("Invalid format encountered while loading watch expressions.", null); //$NON-NLS-1$
			return;
		}
		NodeList list= root.getChildNodes();
		for (int i= 0, numItems= list.getLength(); i < numItems; i++) {
			Node node= list.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element= (Element) node;
				if (!element.getNodeName().equals(EXPRESSION_TAG)) {
					DebugPlugin.logMessage(MessageFormat.format("Invalid XML element encountered while loading watch expressions: {0}", new Object[] { node.getNodeName() }), null); //$NON-NLS-1$
					continue;
				}
				String expressionText= element.getAttribute(TEXT_TAG);
				if (expressionText.length() > 0) {
					boolean enabled= TRUE_VALUE.equals(element.getAttribute(ENABLED_TAG));
					IWatchExpression expression= newWatchExpression(expressionText, enabled);
					if (fExpressions == null) {
						fExpressions = new Vector<>(list.getLength());
					}
					fExpressions.add(expression);
				} else {
					DebugPlugin.logMessage("Invalid expression entry encountered while loading watch expressions. Expression text is empty.", null); //$NON-NLS-1$
				}
			}
		}
	}

	/**
	 * Creates a new watch expression with the given expression
	 * and the given enablement;
	 *
	 * @param expressionText the text of the expression to be evaluated
	 * @param enabled whether or not the new expression should be enabled
	 * @return the new watch expression
	 */
	private IWatchExpression newWatchExpression(String expressionText, boolean enabled) {
		return new WatchExpression(expressionText, enabled);
	}

	@Override
	public IWatchExpression newWatchExpression(String expressionText) {
		return new WatchExpression(expressionText);
	}

	/**
	 * Persists this manager's watch expressions as XML in the
	 * preference store.
	 */
	public void storeWatchExpressions() {
		String expressionString = IInternalDebugCoreConstants.EMPTY_STRING;
		try {
			expressionString= getWatchExpressionsAsXML();
		} catch (IOException e) {
			DebugPlugin.log(e);
		} catch (ParserConfigurationException e) {
			DebugPlugin.log(e);
		} catch (TransformerException e) {
			DebugPlugin.log(e);
		}
		Preferences.setString(DebugPlugin.getUniqueIdentifier(), PREF_WATCH_EXPRESSIONS, expressionString, null);
	}

	/**
	 * Returns this manager's watch expressions as XML.
	 * @return this manager's watch expressions as XML
	 * @throws IOException if an exception occurs while creating
	 * 		the XML document.
	 * @throws ParserConfigurationException if an exception occurs while creating
	 * 		the XML document.
	 * @throws TransformerException if an exception occurs while creating
	 * 		the XML document.
	 */
	private String getWatchExpressionsAsXML() throws IOException, ParserConfigurationException, TransformerException {
		IExpression[] expressions= getExpressions();
		Document document= LaunchManager.getDocument();
		Element rootElement= document.createElement(WATCH_EXPRESSIONS_TAG);
		document.appendChild(rootElement);
		for (IExpression expression : expressions) {
			if (expression instanceof IWatchExpression) {
				Element element= document.createElement(EXPRESSION_TAG);
				element.setAttribute(TEXT_TAG, expression.getExpressionText());
				element.setAttribute(ENABLED_TAG, ((IWatchExpression) expression).isEnabled() ? TRUE_VALUE : FALSE_VALUE);
				rootElement.appendChild(element);
			}
		}
		return LaunchManager.serializeDocument(document);
	}

	@Override
	public void addExpression(IExpression expression) {
		addExpressions(new IExpression[]{expression});
	}

	@Override
	public void addExpressions(IExpression[] expressions) {
		List<IExpression> added = doAdd(expressions);
		if (!added.isEmpty()) {
			fireUpdate(added.toArray(new IExpression[added.size()]), ADDED);
		}
	}

	/**
	 * Adds the given expressions to the list of managed expressions, and returns a list
	 * of expressions that were actually added. Expressions that already exist in the
	 * managed list are not added.
	 *
	 * @param expressions expressions to add
	 * @return list of expressions that were actually added.
	 */
	private List<IExpression> doAdd(IExpression[] expressions) {
		List<IExpression> added = new ArrayList<>(expressions.length);
		synchronized (this) {
			if (fExpressions == null) {
				fExpressions = new Vector<>(expressions.length);
			}
			for (IExpression expression : expressions) {
				if (fExpressions.indexOf(expression) == -1) {
					added.add(expression);
					fExpressions.add(expression);
				}
			}
		}
		return added;
	}

	@Override
	public synchronized IExpression[] getExpressions() {
		if (fExpressions == null) {
			return new IExpression[0];
		}
		IExpression[] temp= new IExpression[fExpressions.size()];
		fExpressions.copyInto(temp);
		return temp;
	}

	@Override
	public synchronized IExpression[] getExpressions(String modelIdentifier) {
		if (fExpressions == null) {
			return new IExpression[0];
		}
		ArrayList<IExpression> temp = new ArrayList<>(fExpressions.size());
		for (IExpression expression : fExpressions) {
			String id = expression.getModelIdentifier();
			if (id != null && id.equals(modelIdentifier)) {
				temp.add(expression);
			}
		}
		return temp.toArray(new IExpression[temp.size()]);
	}

	/**
	 * Adds the given expressions to the collection of registered expressions
	 * in the workspace and notifies all registered listeners. The expressions
	 * are inserted in the same order as the passed array at the index of the
	 * specified expressions (before or after it depending on the boolean argument).
	 * If no valid insertion location could be found, the expressions are added
	 * to the end of the collection. Has no effect on expressions already registered.
	 *
	 * @param expressions expressions to insert into the collection
	 * @param insertionLocation the expression at the location where expressions will be inserted
	 * @param insertBefore whether to insert the expressions before or after the given insertion location
	 * @since 3.4
	 */
	public void insertExpressions(IExpression[] expressions, IExpression insertionLocation, boolean insertBefore){
		List<IExpression> added = null;
		List<IExpression> inserted = null;
		int insertionIndex = -1;
		synchronized (this) {
			if (fExpressions == null || ((insertionIndex = fExpressions.indexOf(insertionLocation)) < 0)) {
				added = doAdd(expressions);
			} else {
				if (!insertBefore){
					insertionIndex++;
				}
				inserted = new ArrayList<>(expressions.length);
				for (IExpression expression : expressions) {
					if (fExpressions.indexOf(expression) == -1) {
						//Insert in the same order as the array is passed
						fExpressions.add(insertionIndex+inserted.size(), expression);
						inserted.add(expression);
					}
				}
			}
		}
		if (added != null) {
			if (!added.isEmpty()) {
				fireUpdate(added.toArray(new IExpression[added.size()]), ADDED);
			}
			return;
		}
		if (inserted != null) {
			if (!inserted.isEmpty()) {
				fireUpdate(inserted.toArray(new IExpression[inserted.size()]), INSERTED, insertionIndex);
			}
		}
	}

	/**
	 * Moves the given expressions from their location in the collection
	 * of registered expressions in the workspace to the specified insertion
	 * location.  Notifies all registered listeners.  This method has no effect
	 * if an expression does not exist in the collection or if no valid insertion
	 * location could be determined.
	 *
	 * @param expressions expressions to move
	 * @param insertionLocation the expression at the location to insert the moved expressions
	 * @param insertBefore whether to insert the moved expressions before or after the given insertion location
	 * @since 3.4
	 */
	public void moveExpressions(IExpression[] expressions, IExpression insertionLocation, boolean insertBefore){
		List<IExpression> movedExpressions = new ArrayList<>(expressions.length);
		int insertionIndex = -1;
		IExpression[] movedExpressionsArray = null;
		synchronized (this) {
			if (fExpressions == null){
				return;
			}
			insertionIndex = fExpressions.indexOf(insertionLocation);
			if (insertionIndex < 0){
				return;
			}
			if (!insertBefore){
				insertionIndex++;
			}

			for (IExpression expression : expressions) {
				int removeIndex = fExpressions.indexOf(expression);
				if (removeIndex >= 0){
					movedExpressions.add(expression);
					if (removeIndex < insertionIndex){
						insertionIndex--;
					}
					fExpressions.remove(removeIndex);
				}
			}
			movedExpressionsArray = movedExpressions.toArray(new IExpression[movedExpressions.size()]);
			for (int i = 0; i < movedExpressionsArray.length; i++) {
				// Insert the expressions in the same order as the passed array
				fExpressions.add(insertionIndex+i,movedExpressionsArray[i]);
			}
		}

		if (!movedExpressions.isEmpty()) {
			fireUpdate(movedExpressionsArray, MOVED, insertionIndex);
		}
	}

	@Override
	public void removeExpression(IExpression expression) {
		removeExpressions(new IExpression[] {expression});
	}

	@Override
	public void removeExpressions(IExpression[] expressions) {
		List<IExpression> removed = new ArrayList<>(expressions.length);
		synchronized (this) {
			if (fExpressions == null) {
				return;
			}
			for (IExpression expression : expressions) {
				if (fExpressions.remove(expression)) {
					removed.add(expression);
				}
			}
		}
		// dispose outside of the synchronized block
		if (!removed.isEmpty()) {
			for (IExpression expression : removed) {
				expression.dispose();
			}
			fireUpdate(removed.toArray(new IExpression[removed.size()]), REMOVED);
		}
	}

	@Override
	public void addExpressionListener(IExpressionListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList<>();
		}
		fListeners.add(listener);
	}

	@Override
	public void removeExpressionListener(IExpressionListener listener) {
		if (fListeners == null) {
			return;
		}
		fListeners.remove(listener);
	}

	/**
	 * The given watch expression has changed. Update the persisted
	 * expressions to store this change as indicated
	 *
	 * @param expression the changed expression
	 */
	protected void watchExpressionChanged(IWatchExpression expression) {
		boolean notify = false;
		synchronized (this) {
			if (fExpressions != null && fExpressions.contains(expression)) {
				notify = true;
			}
		}
		if (notify) {
			fireUpdate(new IExpression[]{expression}, CHANGED);
		}
	}

	/**
	 * Notifies listeners of the adds/removes/changes
	 *
	 * @param expressions expressions that were modified
	 * @param update update flags
	 */
	private void fireUpdate(IExpression[] expressions, int update){
		fireUpdate(expressions, update, -1);
	}

	/**
	 * Notifies listeners of the adds/removes/changes/insertions/moves
	 *
	 * @param expressions expressions that were modified
	 * @param update update flags
	 * @param index index where expressions were inserted/moved to or <code>-1</code>
	 */
	private void fireUpdate(IExpression[] expressions, int update, int index){
		// single listeners
		getExpressionNotifier().notify(expressions, update);

		// multi listeners
		getExpressionsNotifier().notify(expressions, update, index);
	}

	@Override
	public synchronized boolean hasExpressions() {
		return fExpressions != null && !fExpressions.isEmpty();
	}

	@Override
	public void addExpressionListener(IExpressionsListener listener) {
		if (fExpressionsListeners == null) {
			fExpressionsListeners = new ListenerList<>();
		}
		fExpressionsListeners.add(listener);
	}

	@Override
	public void removeExpressionListener(IExpressionsListener listener) {
		if (fExpressionsListeners == null) {
			return;
		}
		fExpressionsListeners.remove(listener);
	}

	private ExpressionNotifier getExpressionNotifier() {
		return new ExpressionNotifier();
	}

	/**
	 * Notifies an expression listener (single expression) in a safe runnable to
	 * handle exceptions.
	 */
	class ExpressionNotifier implements ISafeRunnable {

		private IExpressionListener fListener;
		private int fType;
		private IExpression fExpression;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during expression change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case ADDED:
				case INSERTED:
					fListener.expressionAdded(fExpression);
					break;
				case REMOVED:
					fListener.expressionRemoved(fExpression);
					break;
				case CHANGED:
					fListener.expressionChanged(fExpression);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies listeners of the add/change/remove
		 *
		 * @param expressions the expressions that have changed
		 * @param update the type of change
		 */
		public void notify(IExpression[] expressions, int update) {
			if (fListeners != null) {
				fType = update;
				for (IExpressionListener iExpressionListener : fListeners) {
					fListener = iExpressionListener;
					for (IExpression expression : expressions) {
						fExpression = expression;
						SafeRunner.run(this);
					}
				}
			}
			fListener = null;
			fExpression = null;
		}
	}

	/**
	 * Returns the expressions notifier
	 * @return the expressions notifier
	 */
	private ExpressionsNotifier getExpressionsNotifier() {
		return new ExpressionsNotifier();
	}

	/**
	 * Notifies an expression listener (multiple expressions) in a safe runnable
	 * to handle exceptions.
	 */
	class ExpressionsNotifier implements ISafeRunnable {

		private IExpressionsListener fListener;
		private int fType;
		private int fIndex;
		private IExpression[] fNotifierExpressions;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, DebugPlugin.getUniqueIdentifier(), DebugPlugin.INTERNAL_ERROR, "An exception occurred during expression change notification.", exception);  //$NON-NLS-1$
			DebugPlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			switch (fType) {
				case MOVED:
					// If the listener doesn't know about moves or the insertion location is unknown, do nothing.
					if (fIndex >= 0 && fListener instanceof IExpressionsListener2){
						((IExpressionsListener2)fListener).expressionsMoved(fNotifierExpressions, fIndex);
					}
					break;
				case INSERTED:
					// If the listener doesn't know about insertions or the insertion location is unknown, notify of an ADD
					if (fIndex >= 0 && fListener instanceof IExpressionsListener2){
						((IExpressionsListener2)fListener).expressionsInserted(fNotifierExpressions, fIndex);
					} else {
						fListener.expressionsAdded(fNotifierExpressions);
					}
					break;
				case ADDED:
					fListener.expressionsAdded(fNotifierExpressions);
					break;
				case REMOVED:
					fListener.expressionsRemoved(fNotifierExpressions);
					break;
				case CHANGED:
					fListener.expressionsChanged(fNotifierExpressions);
					break;
				default:
					break;
			}
		}

		/**
		 * Notifies listeners of the adds/changes/removes
		 *
		 * @param expressions the expressions that changed
		 * @param update the type of change
		 * @param index the index of the first change
		 */
		public void notify(IExpression[] expressions, int update, int index) {
			if (fExpressionsListeners != null) {
				fNotifierExpressions = expressions;
				fType = update;
				fIndex = index;
				for (IExpressionsListener iExpressionsListener : fExpressionsListeners) {
					fListener = iExpressionsListener;
					SafeRunner.run(this);
				}
			}
			fNotifierExpressions = null;
			fListener = null;
		}
	}
}
