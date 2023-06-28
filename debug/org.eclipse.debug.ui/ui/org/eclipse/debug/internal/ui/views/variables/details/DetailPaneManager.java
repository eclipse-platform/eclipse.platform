/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
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
package org.eclipse.debug.internal.ui.views.variables.details;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.expressions.EvaluationResult;
import org.eclipse.core.expressions.Expression;
import org.eclipse.core.expressions.ExpressionConverter;
import org.eclipse.core.expressions.ExpressionTagNames;
import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.IDetailPane;
import org.eclipse.debug.ui.IDetailPaneFactory;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.osgi.service.prefs.BackingStoreException;

/**
 * Organizes the detail factories contributed through the extension point and keeps
 * track of the detail panes the factories produce.  Accessed as a singleton through
 * the <code>getDefault()</code> method.
 *
 * @see IDetailPaneFactory
 * @see IDetailPane
 * @since 3.3
 */
public class DetailPaneManager {

	/**
	 * Acts as a proxy between the detail pane manager and the factories contributed
	 * to the extension point.  Only loads information from the plug-in xml and only
	 * instantiates the specified factory if required (lazy loading).
	 */
	private static class DetailPaneFactoryExtension implements IDetailPaneFactory{

		private IConfigurationElement fConfigElement;
		private IDetailPaneFactory fFactory;
		private Expression fEnablementExpression;

		public DetailPaneFactoryExtension(IConfigurationElement configElement){
			fConfigElement = configElement;
		}

		/**
		 * Instantiates the factory and asks it to produce the IDetailPane for
		 * the given ID
		 * @param paneID the identifier of the detail pane to create
		 * @return the new detail pane or <code>null</code> if the backing {@link IDetailPaneFactory} is <code>null</code>
		 */
		@Override
		public IDetailPane createDetailPane(String paneID){
			if (getFactory() != null){
				return getFactory().createDetailPane(paneID);
			}
			return null;
		}

		/**
		 * Instantiates the factory and asks it for the set of detail pane
		 * IDs that the factory can produce for the given selection.
		 * @param selection the current view selection
		 * @return the set of detail pane type for the given selection or an empty set, never <code>null</code>
		 */
		@Override
		public Set<String> getDetailPaneTypes(IStructuredSelection selection) {
			if (getFactory() != null){
				return getFactory().getDetailPaneTypes(selection);
			}
			return Collections.EMPTY_SET;
		}

		/**
		 * Instantiates the factory and asks it for the detail pane ID
		 * that the factory considers the default for the given selection.
		 * @param selection the current view selection
		 * @return the identifier of the default detail pane or <code>null</code> if the backing {@link IDetailPaneFactory} is <code>null</code>
		 */
		@Override
		public String getDefaultDetailPane(IStructuredSelection selection) {
			if (getFactory() != null){
				return getFactory().getDefaultDetailPane(selection);
			}
			return null;
		}

		/**
		 * Instantiates the factory and asks it to produce the name of the detail pane
		 * for the given ID.
		 * @param paneID the detail pane identifier
		 * @return the name of the detail pane or <code>null</code> if the backing {@link IDetailPaneFactory} is <code>null</code>
		 */
		@Override
		public String getDetailPaneName(String paneID) {
			if (getFactory() != null){
				return getFactory().getDetailPaneName(paneID);
			}
			return null;
		}

		/**
		 * Instantiates the factory and asks it to produce the description of the
		 * detail pane for the given ID.
		 * @param paneID the detail pane identifier
		 * @return the description of the detail pane or <code>null</code> if the backing {@link IDetailPaneFactory} is <code>null</code>
		 */
		@Override
		public String getDetailPaneDescription(String paneID) {
			if (getFactory() != null){
				return getFactory().getDetailPaneDescription(paneID);
			}
			return null;
		}

		/**
		 * Returns the instantiated factory specified by the class property.
		 * @return the singleton {@link IDetailPaneFactory}
		 */
		private IDetailPaneFactory getFactory(){
			if (fFactory != null) {
				return fFactory;
			}
			try{
				Object obj = fConfigElement.createExecutableExtension(IConfigurationElementConstants.CLASS);
				if(obj instanceof IDetailPaneFactory) {
					fFactory = (IDetailPaneFactory)obj;
				} else {
					throw new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.INTERNAL_ERROR, "org.eclipse.debug.ui.detailFactories extension failed to load a detail factory because the specified class does not implement org.eclipse.debug.ui.IDetailPaneFactory.  Class specified was: " + obj, null)); //$NON-NLS-1$
				}
			} catch (CoreException e){
				DebugUIPlugin.log(e.getStatus());
				fFactory = null;
			}
			return fFactory;
		}

		/**
		 * Checks if the enablement expression for the factory evaluates to true for the
		 * given selection.
		 * @param selection the current view selection
		 * @return <code>true</code> if the backing {@link IDetailPaneFactory} applies to the given selection, <code>false</code> otherwise
		 */
		public boolean isEnabled(IStructuredSelection selection) {
			boolean enabled = false;
			// Only the default factory should be enabled for null selections
			if (selection == null || selection.isEmpty()){
				return "org.eclipse.debug.ui.defaultDetailPaneFactory".equals(fConfigElement.getAttribute(IConfigurationElementConstants.ID)); //$NON-NLS-1$
			}
			Expression expression = getEnablementExpression();
			if (expression != null) {
				List<?> list = selection.toList();
				IEvaluationContext context = DebugUIPlugin.createEvaluationContext(list);
				context.addVariable("selection", list); //$NON-NLS-1$
				enabled = evalEnablementExpression(context, expression);
			} else {
				enabled = true;
			}
			return enabled;
		}

		/**
		 * Evaluate the given expression within the given context and return
		 * the result. Returns <code>true</code> iff result is either TRUE or NOT_LOADED.
		 * This allows optimistic inclusion of shortcuts before plug-ins are loaded.
		 * Returns <code>false</code> if expression is <code>null</code>.
		 *
		 * @param exp the enablement expression to evaluate or <code>null</code>
		 * @param context the context of the evaluation. Usually, the
		 *  user's selection.
		 * @return the result of evaluating the expression
		 */
		private boolean evalEnablementExpression(IEvaluationContext context, Expression exp) {
			try{
				if (exp != null){
					EvaluationResult result = exp.evaluate(context);
					if (result == EvaluationResult.TRUE || result == EvaluationResult.NOT_LOADED){
						return true;
					}
				}
			} catch (CoreException e){
				DebugUIPlugin.log(e.getStatus());
			}
			return false;
		}

		/**
		 * Returns an expression that represents the enablement logic for the
		 * detail pane factory or <code>null</code> if none.
		 *
		 * @return an evaluatable expression or <code>null</code>
		 */
		private Expression getEnablementExpression(){
			// all of this stuff is optional, so...tedious testing is required
			if (fEnablementExpression == null) {
				try{
					IConfigurationElement[] elements = fConfigElement.getChildren(ExpressionTagNames.ENABLEMENT);
					IConfigurationElement enablement = elements.length > 0 ? elements[0] : null;
					if (enablement != null) {
						fEnablementExpression = ExpressionConverter.getDefault().perform(enablement);
					}
				} catch (CoreException e){
					DebugUIPlugin.log(e.getStatus());
					fEnablementExpression = null;
				}
			}
			return fEnablementExpression;
		}

	}

	/**
	 * There should only ever be once instance of this manager for the workbench.
	 */
	private static DetailPaneManager fgSingleton;

	/**
	 * Maps the IDs of types of detail panes to the factory that can create them.
	 * There can currently only be one factory for a given type of details pane.
	 */
	private Map<String, IDetailPaneFactory> fFactoriesByPaneID;

	/**
	 * Maps a Set of detail pane id's to the one detail pane id that is preferred.
	 */
	private Map<Set<String>, String> fPreferredDetailPanes;

	/**
	 * The set of all factories that have been loaded from the extension point.
	 */
	private List<DetailPaneFactoryExtension> fKnownFactories;

	/**
	 * Preference key for storing the preferred detail panes map.
	 * @see #storePreferredDetailsAreas()
	 * @see #loadPreferredDetailsAreas()
	 */
	public static final String PREF_DETAIL_AREAS = "preferredDetailPanes"; //$NON-NLS-1$

	private DetailPaneManager(){
		fFactoriesByPaneID = new HashMap<>();
		fFactoriesByPaneID.put(MessageDetailPane.ID, new DefaultDetailPaneFactory());
	}

	public static DetailPaneManager getDefault(){
		if (fgSingleton == null) {
			fgSingleton = new DetailPaneManager();
		}
		return fgSingleton;
	}

	/**
	 * Returns the ID of the preferred detail pane for the given selection.
	 *
	 * @param selection The selection to display in the detail pane
	 * @return The ID of the preferred detail pane or null
	 */
	public String getPreferredPaneFromSelection(IStructuredSelection selection){
		List<IDetailPaneFactory> possibleFactories = getEnabledFactories(selection);
		Set<String> possiblePaneIDs = getPossiblePaneIDs(possibleFactories, selection);
		return chooseDetailsAreaIDInSet(possiblePaneIDs, possibleFactories, selection);
	}

	/**
	 * Returns the set of all possible detail panes the can display the given
	 * selection.
	 *
	 * @param selection The selection to display in the detail pane
	 * @return The set of IDs of all possible detail panes for the given selection
	 */
	public Set<String> getAvailablePaneIDs(IStructuredSelection selection) {
		List<IDetailPaneFactory> possibleFactories = getEnabledFactories(selection);
		return getPossiblePaneIDs(possibleFactories, selection);
	}

	/**
	 * Given the ID of a details pane, this method will try to find the factory
	 * that creates it and return an instantiation of that area.
	 * <p>
	 * This method will not call the init() method of the IDetailsPane.
	 * </p>
	 *
	 * @param ID The ID of the requested pane
	 * @return The instantiated pane or null
	 */
	public IDetailPane getDetailPaneFromID(String ID){
		IDetailPaneFactory factory = fFactoriesByPaneID.get(ID);
		if (factory != null){
			return factory.createDetailPane(ID);
		}
		return null;
	}

	/**
	 * Given the ID of a details pane, this method will try to find the factory
	 * that creates it and ask it for the name of the details pane.
	 *
	 * @param ID The ID of the requested pane
	 * @return The name of the details pane or null
	 */
	public String getNameFromID(String ID){
		IDetailPaneFactory factory = fFactoriesByPaneID.get(ID);
		if (factory != null){
			return factory.getDetailPaneName(ID);
		}
		return null;
	}

	/**
	 * Given the ID of a details pane, this method will try to find the factory
	 * that creates it and ask it for the description of the details pane.
	 *
	 * @param ID The ID of the requested pane
	 * @return The description of the details pane or null
	 */
	public String getDescriptionFromID(String ID){
		IDetailPaneFactory factory = fFactoriesByPaneID.get(ID);
		if (factory != null){
			return factory.getDetailPaneDescription(ID);
		}
		return null;
	}


	/**
	 * Returns the set of IDetailPaneFactories (they will be DetailPaneFactoryDelegates) that were
	 * contributed to the extension point and are enabled for the given selection
	 * (enabled if the factory does not have an enablement expression or if the
	 * enablement expression evaluates to true).
	 * @param selection  the current view selection
	 *
	 * @return The factories enabled for the selection or an empty collection.
	 */
	private List<IDetailPaneFactory> getEnabledFactories(IStructuredSelection selection) {
		List<IDetailPaneFactory> factoriesForSelection = new ArrayList<>();
		if (fKnownFactories == null) {
			initializeDetailFactories();
		}
		for (DetailPaneFactoryExtension currentFactory : fKnownFactories) {
			if (currentFactory.isEnabled(selection)) {
				factoriesForSelection.add(currentFactory);
			}
		}
		return factoriesForSelection;
	}

	/**
	 * Produces the set of IDs for all possible detail panes that can be used to display
	 * the given selection.
	 *
	 * @param factoriesToQuery The collection of factories to check
	 * @param selection The selection to be displayed
	 * @return Set of pane IDs or an empty set
	 */
	private Set<String> getPossiblePaneIDs(List<IDetailPaneFactory> factoriesToQuery, IStructuredSelection selection) {
		Set<String> idsForSelection = new LinkedHashSet<>();
		for (IDetailPaneFactory currentFactory : factoriesToQuery) {
			for (String currentAreaTypeID : currentFactory.getDetailPaneTypes(selection)) {
				fFactoriesByPaneID.put(currentAreaTypeID, currentFactory);
				idsForSelection.add(currentAreaTypeID);
			}
		}
		return idsForSelection;
	}

	/**
	 * Given a set of possible detail pane IDs, this method will determine which pane is
	 * preferred and should be used to display the selection.  This method chooses a pane
	 * by storing previous choices and can be set using a context menu.
	 *
	 * @param possiblePaneIDs The set of possible detail pane IDs
	 * @param enabledFactories the complete listing of enable {@link IDetailPaneFactory}s
	 * @param selection the current selection from the variables view
	 * @return The preferred detail pane ID or null
	 */
	private String chooseDetailsAreaIDInSet(Set<String> possiblePaneIDs, List<IDetailPaneFactory> enabledFactories, IStructuredSelection selection) {
		if (possiblePaneIDs == null || possiblePaneIDs.isEmpty()){
			return null;
		}

		String preferredID = getUserPreferredDetailPane(possiblePaneIDs);

		if (preferredID == null){
			// If there is no preferred pane already set, check the factories to see there is a default pane
			for (IDetailPaneFactory currentFactory : enabledFactories) {
				preferredID = currentFactory.getDefaultDetailPane(selection);
				if (preferredID != null) {
					break;
				}
			}
			// If the factories don't have a default, try to choose the DefaultDetailPane
			if (preferredID == null){
				Iterator<String> paneIterator = possiblePaneIDs.iterator();
				// If the DefaultDetailPane is not in the set, just use the first in the set
				preferredID = paneIterator.next();
				while (paneIterator.hasNext() && preferredID != DefaultDetailPaneFactory.DEFAULT_DETAIL_PANE_ID) {
					String currentID = paneIterator.next();
					if (currentID.equals(DefaultDetailPaneFactory.DEFAULT_DETAIL_PANE_ID)){
						preferredID = currentID;
					}
				}
			}
			setPreferredDetailPane(possiblePaneIDs, preferredID);
		}

		return preferredID;
	}

	/**
	 * Initializes the collection of known factories from extension point contributions.
	 */
	private synchronized void initializeDetailFactories(){
		if (fKnownFactories == null){
			fKnownFactories = new ArrayList<>();
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_DETAIL_FACTORIES);
			IConfigurationElement[] infos = extensionPoint.getConfigurationElements();
			for (IConfigurationElement info : infos) {
				DetailPaneFactoryExtension delegate = new DetailPaneFactoryExtension(info);
				fKnownFactories.add(delegate);
			}
		}
	}

	/**
	 * Returns the preferred pane ID from the given set if the mapping has been set.
	 *
	 * @param possibleDetailsAreaIDs Set of possible pane IDs
	 * @return The preferred ID or null
	 */
	public String getUserPreferredDetailPane(Set<String> possibleDetailsAreaIDs) {
		if (fPreferredDetailPanes == null){
			loadPreferredDetailsAreas();
		}
		return fPreferredDetailPanes.get(possibleDetailsAreaIDs);

	}

	/**
	 * Adds or updates the mapping to set which pane ID is preferred for a certain
	 * set of possible IDs.
	 *
	 * @param possibleDetailsAreaIDs The set of possible IDs
	 * @param preferredDetailsAreaID The preferred ID in the set.
	 */
	public void setPreferredDetailPane(Set<String> possibleDetailsAreaIDs, String preferredDetailsAreaID) {
		if (possibleDetailsAreaIDs == null) {
			return;
		}
		if (fPreferredDetailPanes == null){
			loadPreferredDetailsAreas();
		}
		String currentKey = fPreferredDetailPanes.get(possibleDetailsAreaIDs);
		if (currentKey == null || !currentKey.equals(preferredDetailsAreaID)){
			fPreferredDetailPanes.put(possibleDetailsAreaIDs, preferredDetailsAreaID);
			storePreferredDetailsAreas();
		}

	}

	/**
	 * Stores the map of preferred detail pane IDs to the preference store in the format:
	 *
	 * Key1A,Key1B:Value1|Key2A,Key2B,Key2C:Value2|
	 *
	 * Where the sub keys (Key1A, Key1B, etc.) are the elements of the set used at the
	 * key in the mapping and the values are the associated String value in the mapping.
	 */
	private void storePreferredDetailsAreas() {
		StringBuilder buffer= new StringBuilder();
		for (Entry<Set<String>, String> entry : fPreferredDetailPanes.entrySet()) {
			for (String currentID : entry.getKey()) {
				buffer.append(currentID);
				buffer.append(',');
			}
			buffer.deleteCharAt(buffer.length()-1);
			buffer.append(':');
			buffer.append(entry.getValue());
			buffer.append('|');
		}
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(DebugUIPlugin.getUniqueIdentifier());
		if(node != null) {
			node.put(PREF_DETAIL_AREAS, buffer.toString());
			try {
				node.flush();
			} catch (BackingStoreException e) {
				DebugUIPlugin.log(e);
			}
		}
	}

	/**
	 * Loads the map of preferred detail pane IDs from the preference store.
	 *
	 * @see #storePreferredDetailsAreas()
	 */
	private void loadPreferredDetailsAreas() {
		fPreferredDetailPanes = new HashMap<>();
		String preferenceValue = Platform.getPreferencesService().getString(DebugUIPlugin.getUniqueIdentifier(),
				PREF_DETAIL_AREAS,
				"",  //$NON-NLS-1$
				null);
		StringTokenizer entryTokenizer = new StringTokenizer(preferenceValue,"|"); //$NON-NLS-1$
		while (entryTokenizer.hasMoreTokens()){
			String token = entryTokenizer.nextToken();
			int valueStart = token.indexOf(':');
			StringTokenizer keyTokenizer = new StringTokenizer(token.substring(0,valueStart),","); //$NON-NLS-1$
			Set<String> keys = new LinkedHashSet<>();
			while (keyTokenizer.hasMoreTokens()){
				keys.add(keyTokenizer.nextToken());
			}
			fPreferredDetailPanes.put(keys, token.substring(valueStart+1));
		}
	}

}
