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
package org.eclipse.debug.internal.ui.launchConfigurations;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.ParserConfigurationException;

import org.eclipse.core.expressions.IEvaluationContext;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ISaveContext;
import org.eclipse.core.resources.ISaveParticipant;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchDelegate;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.internal.core.IConfigurationElementConstants;
import org.eclipse.debug.internal.core.LaunchManager;
import org.eclipse.debug.internal.ui.DebugPluginImages;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.IInternalDebugUIConstants;
import org.eclipse.debug.internal.ui.ILaunchHistoryChangedListener;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;
import org.eclipse.debug.ui.ILaunchConfigurationTab;
import org.eclipse.debug.ui.ILaunchGroup;
import org.eclipse.jface.resource.ImageRegistry;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.activities.IWorkbenchActivitySupport;
import org.eclipse.ui.activities.WorkbenchActivityHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Manages UI related launch configuration artifacts
 *
 * Since 3.3 the Launch Configuration Manager is an <code>ISaveParticipant</code>, allowing it to participate in
 * workspace persistence life-cycles.
 *
 * @see ISaveParticipant
 * @see org.eclipse.debug.ui.ILaunchShortcut
 * @see ILaunchGroup
 * @see ILaunchListener
 * @see ILaunchHistoryChangedListener
 * @see DebugUIPlugin
 * @see LaunchHistory
 */
public class LaunchConfigurationManager implements ILaunchListener, ISaveParticipant {
	/**
	 * A comparator for the ordering of launch shortcut extensions
	 * @since 3.3
	 */
	static class ShortcutComparator implements Comparator<LaunchShortcutExtension> {
		/**
		 * @see Comparator#compare(Object, Object)
		 */
		@Override
		public int compare(LaunchShortcutExtension a, LaunchShortcutExtension b) {
			LaunchShortcutExtension shorcutA = a;
			String labelA = shorcutA.getLabel();
			String pathA = shorcutA.getMenuPath();
			LaunchShortcutExtension shortcutB = b;
			String labelB = shortcutB.getLabel();
			String pathB = shortcutB.getMenuPath();

			// group by path, then sort by label
			// a null path sorts last (i.e. highest)
			if (nullOrEqual(pathA, pathB)) {
				// null labels sort last (i.e. highest)
				if (labelA == labelB) {
					return 0;
				}
				if (labelA == null) {
					return 1;
				}
				if (labelB == null) {
					return -1;
				}
				return labelA.compareToIgnoreCase(labelB);
			}
			// compare paths
			if (pathA == null) {
				return 1;
			}
			if (pathB == null) {
				return -1;
			}
			return pathA.compareToIgnoreCase(pathB);
		}

		private boolean nullOrEqual(String a, String b) {
			if (a == null) {
				return b == null;
			}
			return a.equals(b);
		}

	}

	/**
	 * Launch group extensions, keyed by launch group identifier.
	 */
	protected Map<String, LaunchGroupExtension> fLaunchGroups;

	/**
	 * Launch histories keyed by launch group identifier
	 */
	protected Map<String, LaunchHistory> fLaunchHistories;

	/**
	 * The list of registered implementors of <code>ILaunchHistoryChangedListener</code>
	 */
	protected ListenerList<ILaunchHistoryChangedListener> fLaunchHistoryChangedListeners = new ListenerList<>();

	/**
	 * Launch shortcuts
	 */
	private List<LaunchShortcutExtension> fLaunchShortcuts = null;

	/**
	 * Launch shortcuts, cached by perspective ids
	 */
	private Map<String, List<LaunchShortcutExtension>> fLaunchShortcutsByPerspective = null;

	/**
	 * Cache of launch configuration tab images with error overlays
	 */
	protected ImageRegistry fErrorImages = null;

	/**
	 * true when restoring launch history
	 */
	protected boolean fRestoring = false;

	/**
	 * The name of the file used to persist the launch history.
	 */
	private static final String LAUNCH_CONFIGURATION_HISTORY_FILENAME = "launchConfigurationHistory.xml"; //$NON-NLS-1$

	/**
	 * performs initialization of the manager when it is started
	 */
	public void startup() {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.addLaunchListener(this);
		DebugUIPlugin.getDefault().addSaveParticipant(this);
		//update histories for launches already registered
		for (ILaunch launch : launchManager.getLaunches()) {
			launchAdded(launch);
		}
	}

	/**
	 * Returns whether any launch config supports the given mode.
	 *
	 * @param mode launch mode
	 * @return whether any launch config supports the given mode
	 */
	public boolean launchModeAvailable(String mode) {
		return ((LaunchManager)DebugPlugin.getDefault().getLaunchManager()).launchModeAvailable(mode);
	}

	/**
	 * Returns whether the given launch configuration should be visible in the
	 * debug UI. If the config is marked as private, or belongs to a different
	 * category (i.e. non-null), then this configuration should not be displayed
	 * in the debug UI.
	 *
	 * @param launchConfiguration the configuration to check for the {@link IDebugUIConstants#ATTR_PRIVATE} attribute
	 * @return boolean
	 */
	public static boolean isVisible(ILaunchConfiguration launchConfiguration) {
		try {
			return !(launchConfiguration.getAttribute(IDebugUIConstants.ATTR_PRIVATE, false));
		} catch (CoreException e) {
		}
		return false;
	}

	/**
	 * Returns a collection of launch configurations that does not contain
	 * configurations from disabled activities.
	 *
	 * @param configurations a collection of configurations
	 * @return the given collection minus any configurations from disabled activities
	 */
	public static ILaunchConfiguration[] filterConfigs(ILaunchConfiguration[] configurations) {
		IWorkbenchActivitySupport activitySupport = PlatformUI.getWorkbench().getActivitySupport();
		if (activitySupport == null) {
			return configurations;
		}
		List<ILaunchConfiguration> filteredConfigs = new ArrayList<>();
		for (ILaunchConfiguration configuration : configurations) {
			try {
				ILaunchConfigurationType type = configuration.getType();
				LaunchConfigurationTypeContribution contribution = new LaunchConfigurationTypeContribution(type);
				if (DebugUIPlugin.doLaunchConfigurationFiltering(configuration) & !WorkbenchActivityHelper.filterItem(contribution)) {
					filteredConfigs.add(configuration);
				}
			}
			catch (CoreException e) {DebugUIPlugin.log(e.getStatus());}
		}
		return filteredConfigs.toArray(new ILaunchConfiguration[filteredConfigs.size()]);
	}

	/**
	 * Returns a listing of <code>IlaunchDeleagtes</code> that does not contain any delegates from disabled activities
	 * @param type the type to get the delegates from
	 * @param modes the set of launch modes to get delegates for
	 * @return the filtered listing of <code>ILaunchDelegate</code>s or an empty array, never <code>null</code>.
	 * @throws CoreException if an exception occurs
	 * @since 3.3
	 */
	public static ILaunchDelegate[] filterLaunchDelegates(ILaunchConfigurationType type, Set<String> modes) throws CoreException {
		IWorkbenchActivitySupport as = PlatformUI.getWorkbench().getActivitySupport();
		ILaunchDelegate[] delegates = type.getDelegates(modes);
		if(as == null) {
			return delegates;
		}
		HashSet<ILaunchDelegate> set = new HashSet<>();
		for (ILaunchDelegate delegate : delegates) {
			//filter by capabilities
			if(!WorkbenchActivityHelper.filterItem(new LaunchDelegateContribution(delegate))) {
				set.add(delegate);
			}
		}
		return set.toArray(new ILaunchDelegate[set.size()]);
	}

	/**
	 * Performs cleanup operations when the manager is being disposed of.
	 */
	public void shutdown() {
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		launchManager.removeLaunchListener(this);
		if (fLaunchHistories != null) {
			for (LaunchHistory history : fLaunchHistories.values()) {
				history.dispose();
			}
		}
		DebugUIPlugin.getDefault().removeSaveParticipant(this);
	}

	/**
	 * @see ILaunchListener#launchRemoved(ILaunch)
	 */
	@Override
	public void launchRemoved(ILaunch launch) {}

	/**
	 * @see ILaunchListener#launchChanged(ILaunch)
	 */
	@Override
	public void launchChanged(ILaunch launch) {}

	/**
	 * Must not assume that will only be called from the UI thread.
	 *
	 * @see ILaunchListener#launchAdded(ILaunch)
	 */
	@Override
	public void launchAdded(final ILaunch launch) {
		removeTerminatedLaunches(launch);
	}

	/**
	 * Removes terminated launches from the launch view, leaving the specified launch in the view
	 * @param newLaunch the newly added launch to leave in the view
	 */
	protected void removeTerminatedLaunches(ILaunch newLaunch) {
		if (DebugUIPlugin.getDefault().getPreferenceStore().getBoolean(IDebugUIConstants.PREF_AUTO_REMOVE_OLD_LAUNCHES)) {
			ILaunchManager lManager= DebugPlugin.getDefault().getLaunchManager();
			for (ILaunch launch : lManager.getLaunches()) {
				if (launch != newLaunch && launch.isTerminated()) {
					lManager.removeLaunch(launch);
				}
			}
		}
	}

	/**
	 * Returns the most recent launch for the given group, or <code>null</code>
	 * if none. This method does not include any filtering for the returned launch configuration.
	 *
	 * This method is exposed via DebugTools.getLastLaunch
	 * @param groupId the identifier of the {@link ILaunchGroup} to get the last launch from
	 *
	 * @return the last launch, or <code>null</code> if none
	 */
	public ILaunchConfiguration getLastLaunch(String groupId) {
		LaunchHistory history = getLaunchHistory(groupId);
		if (history != null) {
			return history.getRecentLaunch();
		}
		return null;
	}

	/**
	 * Returns the most recent launch for the given group taking launch configuration
	 * filters into account, or <code>null</code> if none.
	 *
	 * @param groupId launch group
	 * @return the most recent, un-filtered launch
	 */
	public ILaunchConfiguration getFilteredLastLaunch(String groupId) {
		LaunchHistory history = getLaunchHistory(groupId);
		if (history != null) {
			ILaunchConfiguration[] filterConfigs = history.getCompleteLaunchHistory();
			if (filterConfigs.length > 0) {
				return filterConfigs[0];
			}
		}
		return null;
	}

	/**
	 * Add the specified listener to the list of listeners that will be notified when the
	 * launch history changes.
	 * @param listener the listener to add - adding a duplicate listener has no effect
	 */
	public void addLaunchHistoryListener(ILaunchHistoryChangedListener listener) {
		fLaunchHistoryChangedListeners.add(listener);
	}

	/**
	 * Remove the specified listener from the list of listeners that will be notified when the
	 * launch history changes.
	 * @param listener the listener to remove
	 */
	public void removeLaunchHistoryListener(ILaunchHistoryChangedListener listener) {
		fLaunchHistoryChangedListeners.remove(listener);
	}

	/**
	 * Notify all launch history listeners that the launch history has changed in some way.
	 */
	protected void fireLaunchHistoryChanged() {
		for (ILaunchHistoryChangedListener listener : fLaunchHistoryChangedListeners) {
			listener.launchHistoryChanged();
		}
	}

	/**
	 * Returns the history listing as XML
	 * @return the history listing as XML
	 * @throws CoreException if an exception occurs
	 * @throws ParserConfigurationException if there is a problem creating the XML for the launch history
	 */
	protected String getHistoryAsXML() throws CoreException, ParserConfigurationException {
		Document doc = DebugUIPlugin.getDocument();
		Element historyRootElement = doc.createElement(IConfigurationElementConstants.LAUNCH_HISTORY);
		doc.appendChild(historyRootElement);
		for (LaunchHistory history : fLaunchHistories.values()) {
			Element groupElement = doc.createElement(IConfigurationElementConstants.LAUNCH_GROUP);
			groupElement.setAttribute(IConfigurationElementConstants.ID, history.getLaunchGroup().getIdentifier());
			historyRootElement.appendChild(groupElement);
			Element historyElement = doc.createElement(IConfigurationElementConstants.MRU_HISTORY);
			groupElement.appendChild(historyElement);
			createEntry(doc, historyElement, history.getCompleteLaunchHistory());
			Element favs = doc.createElement(IConfigurationElementConstants.FAVORITES);
			groupElement.appendChild(favs);
			createEntry(doc, favs, history.getFavorites());
			history.setSaved(true);
		}
		return DebugPlugin.serializeDocument(doc);
	}

	/**
	 * Creates a new launch history element and adds it to the specified <code>Document</code>
	 * @param doc the <code>Document</code> to add the new element to
	 * @param historyRootElement the root element
	 * @param configurations the configurations to create entries for
	 * @throws CoreException is an exception occurs
	 */
	protected void createEntry(Document doc, Element historyRootElement, ILaunchConfiguration[] configurations) throws CoreException {
		for (ILaunchConfiguration configuration : configurations) {
			if (configuration.exists()) {
				Element launch = doc.createElement(IConfigurationElementConstants.LAUNCH);
				launch.setAttribute(IConfigurationElementConstants.MEMENTO, configuration.getMemento());
				historyRootElement.appendChild(launch);
			}
		}
	}

	/**
	 * Returns the path to the local file for the launch history
	 * @return the file path for the launch history file
	 */
	protected IPath getHistoryFilePath() {
		return DebugUIPlugin.getDefault().getStateLocation().append(LAUNCH_CONFIGURATION_HISTORY_FILENAME);
	}

	/**
	 * Write out an XML file indicating the entries on the run & debug history lists and
	 * the most recent launch.
	 * @throws IOException if writing the history file fails
	 * @throws CoreException is an exception occurs
	 * @throws ParserConfigurationException if there is a problem reading the XML
	 */
	protected void persistLaunchHistory() throws IOException, CoreException, ParserConfigurationException {
		synchronized (this) {
			if (fLaunchHistories == null || fRestoring) {
				return;
			}
		}
		boolean shouldsave = false;
		for (LaunchHistory history : fLaunchHistories.values()) {
			shouldsave |= history.needsSaving();
		}
		if(shouldsave) {
			IPath historyPath = getHistoryFilePath();
			String osHistoryPath = historyPath.toOSString();
			String xml = getHistoryAsXML();
			File file = new File(osHistoryPath);
			file.createNewFile();

			try (FileOutputStream stream = new FileOutputStream(file)) {
				stream.write(xml.getBytes(StandardCharsets.UTF_8));
			}
		}
	}

	/**
	 * Find the XML history file and parse it.  Place the corresponding configurations
	 * in the appropriate history, and set the most recent launch.
	 */
	private void restoreLaunchHistory() {
		// Find the history file
		IPath historyPath = getHistoryFilePath();
		String osHistoryPath = historyPath.toOSString();
		File file = new File(osHistoryPath);
		// If no history file, nothing to do
		if (!file.exists()) {
			return;
		}

		Element rootHistoryElement= null;
		try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
			// Parse the history file
			try {
				@SuppressWarnings("restriction")
				DocumentBuilder parser = org.eclipse.core.internal.runtime.XmlProcessorFactory
						.createDocumentBuilderWithErrorOnDOCTYPE();
				parser.setErrorHandler(new DefaultHandler());
				rootHistoryElement = parser.parse(new InputSource(stream)).getDocumentElement();
			} catch (SAXException e) {
				DebugUIPlugin.log(e);
				return;
			} catch (ParserConfigurationException e) {
				DebugUIPlugin.log(e);
				return;
			}
		} catch (IOException exception) {
			DebugUIPlugin.log(exception);
			return;
		}
		// If root node isn't what we expect, return
		if (!rootHistoryElement.getNodeName().equalsIgnoreCase(IConfigurationElementConstants.LAUNCH_HISTORY)) {
			return;
		}
		// For each child of the root node, construct a launch config handle and add it to
		// the appropriate history, or set the most recent launch
		LaunchHistory[] histories = fLaunchHistories.values().toArray(new LaunchHistory[fLaunchHistories.size()]);
		NodeList list = rootHistoryElement.getChildNodes();
		int length = list.getLength();
		Node node = null;
		Element entry = null;
		for (int i = 0; i < length; ++i) {
			node = list.item(i);
			short type = node.getNodeType();
			if (type == Node.ELEMENT_NODE) {
				entry = (Element) node;
				if (entry.getNodeName().equalsIgnoreCase(IConfigurationElementConstants.LAUNCH)) {
					createHistoryElement(entry, histories, false);
				} else if (entry.getNodeName().equalsIgnoreCase(IConfigurationElementConstants.LAST_LAUNCH)) {
					createHistoryElement(entry, histories, true);
				} else if (entry.getNodeName().equals(IConfigurationElementConstants.LAUNCH_GROUP)) {
					String id = entry.getAttribute(IConfigurationElementConstants.ID);
					if (id != null) {
						LaunchHistory history = getLaunchHistory(id);
						if (history != null) {
							restoreHistory(entry, history);
						}
					}
				}
			}
		}
	}

	/**
	 * Restores the given launch history.
	 *
	 * @param groupElement launch group history
	 * @param history associated history cache
	 */
	private void restoreHistory(Element groupElement, LaunchHistory history) {
		NodeList nodes = groupElement.getChildNodes();
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element)node;
				if (element.getNodeName().equals(IConfigurationElementConstants.MRU_HISTORY)) {
					for (ILaunchConfiguration config : getLaunchConfigurations(element)) {
						history.addHistory(config, false);
					}
				} else if (element.getNodeName().equals(IConfigurationElementConstants.FAVORITES)) {
					ILaunchConfiguration[] favs = getLaunchConfigurations(element);
					history.setFavorites(favs);
					// add any favorites that have been added to the workspace before this plug-in
					// was loaded - @see bug 231600
					for (ILaunchConfiguration configuration : getLaunchManager().getLaunchConfigurations()) {
						history.checkFavorites(configuration);
					}
				}
			}
		}
	}

	/**
	 * Restores a list of configurations.
	 * @param root element
	 * @return list of configurations under the element
	 */
	private ILaunchConfiguration[] getLaunchConfigurations(Element root) {
		List<ILaunchConfiguration> configs = new ArrayList<>();
		NodeList nodes = root.getChildNodes();
		int length = nodes.getLength();
		for (int i = 0; i < length; i++) {
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE) {
				Element element = (Element) node;
				if (element.getNodeName().equals(IConfigurationElementConstants.LAUNCH)) {
					String memento = element.getAttribute(IConfigurationElementConstants.MEMENTO);
					if (memento != null) {
						try {
							ILaunchConfiguration configuration = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(memento);
							//touch the config to see if its type exists
							configuration.getType();
							if (configuration.exists()) {
								configs.add(configuration);
							}
						} catch (CoreException e) {
							//do nothing as we don't care about non-existent, or configs with no type
						}
					}
				}
			}
		}
		return configs.toArray(new ILaunchConfiguration[configs.size()]);
	}

	/**
	 * Construct a launch configuration corresponding to the specified XML
	 * element, and place it in the appropriate history.
	 * @param entry the XML entry to read from
	 * @param histories the array of histories to try and add the restored configurations to
	 * @param prepend if any restored items should be added to to top of the launch history
	 */
	private void createHistoryElement(Element entry, LaunchHistory[] histories, boolean prepend) {
		String memento = entry.getAttribute(IConfigurationElementConstants.MEMENTO);
		String mode = entry.getAttribute(IConfigurationElementConstants.MODE);
		try {
			ILaunchConfiguration launchConfig = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(memento);
			//touch the type to see if its type exists
			launchConfig.getType();
			if (launchConfig.exists()) {
				for (LaunchHistory history : histories) {
					if (history.accepts(launchConfig) && history.getLaunchGroup().getMode().equals(mode)) {
						history.addHistory(launchConfig, prepend);
					}
				}
			}
		} catch (CoreException e) {
			//do nothing, as we want to throw away invalid launch history entries silently
		}
	}

	/**
	 * Load all registered extensions of the 'launch shortcut' extension point.
	 */
	private synchronized void loadLaunchShortcuts() {
		if(fLaunchShortcuts == null) {
			// Get the configuration elements
			IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_LAUNCH_SHORTCUTS);
			IConfigurationElement[] infos = extensionPoint.getConfigurationElements();

			// Load the configuration elements into a Map
			fLaunchShortcuts = new ArrayList<>(infos.length);
			for (IConfigurationElement info : infos) {
				fLaunchShortcuts.add(new LaunchShortcutExtension(info));
			}
			Collections.sort(fLaunchShortcuts, new ShortcutComparator());
		}
	}

	/**
	 * Load all registered extensions of the 'launch groups' extension point.
	 */
	private synchronized void loadLaunchGroups() {
		if (fLaunchGroups == null) {
			// Get the configuration elements
			IExtensionPoint extensionPoint= Platform.getExtensionRegistry().getExtensionPoint(DebugUIPlugin.getUniqueIdentifier(), IDebugUIConstants.EXTENSION_POINT_LAUNCH_GROUPS);
			IConfigurationElement[] infos= extensionPoint.getConfigurationElements();

			// Load the configuration elements into a Map
			fLaunchGroups = new HashMap<>(infos.length);
			LaunchGroupExtension ext = null;
			for (IConfigurationElement info : infos) {
				ext = new LaunchGroupExtension(info);
				fLaunchGroups.put(ext.getIdentifier(), ext);
			}
		}
	}

	/**
	 * Returns all launch shortcuts
	 *
	 * @return all launch shortcuts
	 */
	public List<LaunchShortcutExtension> getLaunchShortcuts() {
		if (fLaunchShortcuts == null) {
			loadLaunchShortcuts();
		}
		return fLaunchShortcuts;
	}

	/**
	 * Creates a listing of the launch shortcut extensions that are applicable to the underlying resource
	 * @param resource the underlying resource
	 * @return a listing of applicable launch shortcuts or an empty list, never <code>null</code>
	 * @since 3.3
	 */
	public List<LaunchShortcutExtension> getLaunchShortcuts(IResource resource) {
		List<LaunchShortcutExtension> list = new ArrayList<>();
		List<LaunchShortcutExtension> sc = getLaunchShortcuts();
		List<IResource> ctxt = new ArrayList<>();
		if(resource != null) {
			ctxt.add(resource);
		}
		IEvaluationContext context = DebugUIPlugin.createEvaluationContext(ctxt);
		context.addVariable("selection", ctxt); //$NON-NLS-1$
		for (LaunchShortcutExtension ext : sc) {
			try {
				if(ext.evalEnablementExpression(context, ext.getContextualLaunchEnablementExpression()) && !WorkbenchActivityHelper.filterItem(ext)) {
					if(!list.contains(ext)) {
						list.add(ext);
					}
				}
			}
			catch(CoreException ce) {/*do nothing*/}
		}
		return list;
	}

	/**
	 * Returns an array of all of the ids of the <code>ILaunchConfigurationType</code>s that apply to the currently
	 * specified <code>IResource</code>.
	 *
	 * @param resource the resource context
	 * @return an array of applicable <code>ILaunchConfigurationType</code>  ids, or an empty array, never <code>null</code>
	 * @since 3.3
	 */
	public String[] getApplicableConfigurationTypes(IResource resource) {
		List<LaunchShortcutExtension> exts = getLaunchShortcuts();
		List<IResource> list = new ArrayList<>();
		list.add(resource);
		IEvaluationContext context = DebugUIPlugin.createEvaluationContext(list);
		context.setAllowPluginActivation(true);
		context.addVariable("selection", list); //$NON-NLS-1$
		HashSet<String> contributedTypeIds = new HashSet<>();
		for (Iterator<LaunchShortcutExtension> iter = exts.listIterator(); iter.hasNext();) {
			LaunchShortcutExtension ext = iter.next();
			try {
				if(ext.evalEnablementExpression(context, ext.getContextualLaunchEnablementExpression())) {
					contributedTypeIds.addAll(ext.getAssociatedConfigurationTypes());
				}
			}
			catch(CoreException ce) {
				IStatus status = new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Launch shortcut '" + ext.getId() + "' enablement expression caused exception. Shortcut was removed.", ce); //$NON-NLS-1$ //$NON-NLS-2$
				DebugUIPlugin.log(status);
				iter.remove();
			}
		}
		List<String> typeIds = new ArrayList<>();
		LaunchManager lm = (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
		for (String id : contributedTypeIds) {
			ILaunchConfigurationType type = lm.getLaunchConfigurationType(id);
			if(type != null) {
				String identifier = type.getIdentifier();
				if (!typeIds.contains(identifier) && type.isPublic() && !"org.eclipse.ui.externaltools.builder".equals(type.getCategory())) { //$NON-NLS-1$
					typeIds.add(identifier);
				}
			}
		}
		return typeIds.toArray(new String[typeIds.size()]);
	}

	/**
	 * Returns an array of the <code>ILaunchConfiguration</code>s that apply to the specified <code>IResource</code>
	 * @param types the array of launch configuration type identifiers
	 * @param resource the resource
	 * @return an array of applicable <code>ILaunchConfiguration</code>s for the specified <code>IResource</code> or an empty
	 * array if none, never <code>null</code>
	 * @since 3.3
	 */
	public ILaunchConfiguration[] getApplicableLaunchConfigurations(String[] types, IResource resource) {
		ArrayList<ILaunchConfiguration> list = new ArrayList<>();
		try {
			if(resource != null) {
				String[] ctypes = types;
				if(ctypes == null) {
					ctypes = getApplicableConfigurationTypes(resource);
				}
				//copy into collection for hashcode matching
				HashSet<String> typeset = new HashSet<>(ctypes.length);
				Collections.addAll(typeset, ctypes);
				for (ILaunchConfiguration configuration : filterConfigs(getLaunchManager().getLaunchConfigurations())) {
					if(typeset.contains(configuration.getType().getIdentifier()) && acceptConfiguration(configuration)) {
						IResource[] resrcs = configuration.getMappedResources();
						if (resrcs != null) {
							for (IResource res : resrcs) {
								if (resource.equals(res) || resource.getFullPath().isPrefixOf(res.getFullPath())) {
									list.add(configuration);
									break;
								}
							}
						}
						else {
							//in the event the config has no mapping
							list.add(configuration);
						}
					}
				}
			}
		} catch (CoreException e) {
			list.clear();
			DebugPlugin.log(e);
		}
		return list.toArray(new ILaunchConfiguration[list.size()]);
	}

	/**
	 * Returns if the specified configuration should be considered as a potential candidate
	 * @param config to configuration
	 * @return if the specified configuration should be considered as a potential candidate
	 * @throws CoreException if an exception occurs
	 */
	private boolean acceptConfiguration(ILaunchConfiguration config) throws CoreException {
		if(config != null && !DebugUITools.isPrivate(config)) {
			if(!"org.eclipse.ui.externaltools".equals(config.getType().getCategory())) { //$NON-NLS-1$
				return true;
			}
			else {
				IResource[] res = config.getMappedResources();
				if(res != null) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Returns all launch shortcuts for the given category
	 * @param category the identifier of the category
	 *
	 * @return all launch shortcuts
	 */
	public List<LaunchShortcutExtension> getLaunchShortcuts(String category) {
		return filterShortcuts(getLaunchShortcuts(), category);
	}

	/**
	 * Return a list of filtered launch shortcuts, based on the given category.
	 *
	 * @param unfiltered the raw list of shortcuts to filter
	 * @param category the category to filter by
	 * @return List
	 */
	protected List<LaunchShortcutExtension> filterShortcuts(List<LaunchShortcutExtension> unfiltered, String category) {
		List<LaunchShortcutExtension> filtered = new ArrayList<>(unfiltered.size());
		for (LaunchShortcutExtension extension : unfiltered) {
			if (category == null) {
				if (extension.getCategory() == null) {
					filtered.add(extension);
				}
			} else if (category.equals(extension.getCategory())){
				filtered.add(extension);
			}
		}
		return filtered;
	}

	/**
	 * Returns all launch shortcuts defined for the given perspective,
	 * empty list if none.
	 *
	 * @param perpsective perspective identifier
	 * @param category the category for the shortcut
	 * @return all launch shortcuts defined for the given perspective,
	 * empty list if none.
	 * @deprecated the use of perspectives for launch shortcuts has been
	 * deprecated since 3.1, use a contextualLaunch element instead
	 */
	@Deprecated
	public List<LaunchShortcutExtension> getLaunchShortcuts(String perpsective, String category) {
		if (fLaunchShortcutsByPerspective == null) {
			fLaunchShortcutsByPerspective = new HashMap<>(10);
		}
		for (LaunchShortcutExtension ext : getLaunchShortcuts()) {
			for (String id : ext.getPerspectives()) {
				List<LaunchShortcutExtension> list = fLaunchShortcutsByPerspective.get(id);
				if (list == null) {
					list = new ArrayList<>(4);
					fLaunchShortcutsByPerspective.put(id, list);
				}
				list.add(ext);
			}
		}
		List<LaunchShortcutExtension> list = fLaunchShortcutsByPerspective.get(perpsective);
		if (list == null) {
			return Collections.EMPTY_LIST;
		}
		return filterShortcuts(list, category);
	}

	/**
	 * Returns the first occurrence of any one of the configurations in the provided list, if they are found in the launch history
	 * for the corresponding launch group
	 * @param configurations the raw list of configurations to examine
	 * @param group the launch group to get the launch history from
	 * @param resource the {@link IResource} context
	 * @return the associated launch configuration from the MRU listing or <code>null</code> if there isn't one
	 * @since 3.3
	 */
	public ILaunchConfiguration getMRUConfiguration(List<ILaunchConfiguration> configurations, ILaunchGroup group, IResource resource) {
		if(group != null) {
			ArrayList<ILaunchConfiguration> candidates = new ArrayList<>();
			LaunchHistory history = getLaunchHistory(group.getIdentifier());
			if(history != null) {
				ILaunchConfiguration[] configs = history.getCompleteLaunchHistory();
				for (ILaunchConfiguration config : configs) {
					if(configurations.contains(config)) {
						if(resource instanceof IContainer) {
							return config;
						}
						else {
							candidates.add(config);
						}
					}
				}
				if(resource != null) {
					//first try to find a config that exactly matches the resource mapping, and collect partial matches
					IResource[] res = null;
					for (ILaunchConfiguration config : candidates) {
						try {
							res = config.getMappedResources();
							if(res != null) {
								for (IResource re : res) {
									if(re.equals(resource)) {
										return config;
									}
								}
							}
						}
						catch(CoreException ce) {}
					}
				}
				for (ILaunchConfiguration config : configs) {
					if(candidates.contains(config)) {
						return config;
					}
				}
			}
		}
		return null;
	}

	/**
	 * Returns the shared config from the selected resource or <code>null</code> if the selected resources is not a shared config
	 * @param receiver the object to test if it is a shared launch configuration
	 * @return the shared config from the selected resource or <code>null</code> if the selected resources is not a shared config
	 * @since 3.3
	 */
	public ILaunchConfiguration isSharedConfig(Object receiver) {
		if(receiver instanceof IFile) {
			IFile file = (IFile) receiver;
			String ext = file.getFileExtension();
			if(ext == null) {
				return null;
			}
			if(ext.equals("launch")) { //$NON-NLS-1$
				ILaunchConfiguration config = DebugPlugin.getDefault().getLaunchManager().getLaunchConfiguration(file);
				if(config != null && config.exists()) {
					return config;
				}
			}
		}
		else if(receiver instanceof IFileEditorInput) {
			IFileEditorInput input = (IFileEditorInput) receiver;
			return isSharedConfig(input.getFile());
		}
		else if(receiver instanceof IEditorPart) {
			return isSharedConfig(((IEditorPart) receiver).getEditorInput());
		}
		else if (receiver instanceof IAdaptable) {
			IFile file = ((IAdaptable)receiver).getAdapter(IFile.class);
			if (file != null) {
				return isSharedConfig(file);
			}
		}
		return null;
	}

	/**
	 * Returns the image used to display an error in the given tab
	 * @param tab the tab to get the error image for
	 * @return the error image associated with the given tab
	 */
	public Image getErrorTabImage(ILaunchConfigurationTab tab) {
		if (fErrorImages == null) {
			fErrorImages = new ImageRegistry();
		}
		String key = tab.getClass().getName();
		Image image = fErrorImages.get(key);
		if (image == null) {
			// create image
			Image base = tab.getImage();
			if (base == null) {
				base = DebugPluginImages.getImage(IInternalDebugUIConstants.IMG_OVR_TRANSPARENT);
			}
			base = new Image(Display.getCurrent(), base, SWT.IMAGE_COPY);
			// register the base image, to avoid nondisposed resource error
			fErrorImages.put(key + "-baseImage", base); //$NON-NLS-1$

			LaunchConfigurationTabImageDescriptor desc = new LaunchConfigurationTabImageDescriptor(base, LaunchConfigurationTabImageDescriptor.ERROR);
			image = desc.createImage();
			fErrorImages.put(key, image);
		}
		return image;
	}

	/**
	 * Return the launch group with the given id, or <code>null</code>
	 * @param id the identifier of the {@link LaunchGroupExtension}
	 *
	 * @return the launch group with the given id, or <code>null</code>
	 */
	public LaunchGroupExtension getLaunchGroup(String id) {
		if (fLaunchGroups == null) {
			loadLaunchGroups();
		}
		return fLaunchGroups.get(id);
	}

	/**
	 * Return all defined launch groups
	 *
	 * @return all defined launch groups
	 */
	public ILaunchGroup[] getLaunchGroups() {
		if (fLaunchGroups == null) {
			loadLaunchGroups();
		}
		return fLaunchGroups.values().toArray(new ILaunchGroup[fLaunchGroups.size()]);
	}

	/**
	 * Return the launch history with the given group id, or <code>null</code>
	 * @param id the identifier of the launch history
	 * @return the launch history with the given group id, or <code>null</code>
	 */
	public LaunchHistory getLaunchHistory(String id) {
		loadLaunchHistories();
		return fLaunchHistories.get(id);
	}

	/**
	 * Returns the singleton instance of the launch manager
	 * @return the singleton instance of the launch manager
	 * @since 3.3
	 */
	private LaunchManager getLaunchManager() {
		return (LaunchManager) DebugPlugin.getDefault().getLaunchManager();
	}

	/**
	 * Restore launch history
	 */
	private synchronized void loadLaunchHistories() {
		if (fLaunchHistories == null) {
			fRestoring = true;
			ILaunchGroup[] groups = getLaunchGroups();
			fLaunchHistories = new HashMap<>(groups.length);
			ILaunchGroup extension = null;
			for (ILaunchGroup group : groups) {
				extension = group;
				if (extension.isPublic()) {
					fLaunchHistories.put(extension.getIdentifier(), new LaunchHistory(extension));
				}
			}
			restoreLaunchHistory();
			fRestoring = false;
		}
	}

	/**
	 * Returns the default launch group for the given mode.
	 *
	 * @param mode the mode identifier
	 * @return launch group
	 */
	public LaunchGroupExtension getDefaultLaunchGroup(String mode) {
		if (mode.equals(ILaunchManager.DEBUG_MODE)) {
			return getLaunchGroup(IDebugUIConstants.ID_DEBUG_LAUNCH_GROUP);
		}
		return getLaunchGroup(IDebugUIConstants.ID_RUN_LAUNCH_GROUP);
	}

	/**
	 * Returns the launch group the given launch configuration type belongs to, in
	 * the specified mode, or <code>null</code> if none.
	 *
	 * @param type the type
	 * @param mode the mode
	 * @return the launch group the given launch configuration belongs to, in
	 * the specified mode, or <code>null</code> if none
	 */
	public ILaunchGroup getLaunchGroup(ILaunchConfigurationType type, String mode) {
		if (!type.supportsMode(mode)) {
			return null;
		}
		String category = type.getCategory();
		ILaunchGroup extension = null;
		for (ILaunchGroup group : getLaunchGroups()) {
			extension = group;
			if (category == null) {
				if (extension.getCategory() == null && extension.getMode().equals(mode)) {
					return extension;
				}
			} else if (category.equals(extension.getCategory())) {
				if (extension.getMode().equals(mode)) {
					return extension;
				}
			}
		}
		return null;
	}

	/**
	 * Returns the {@link ILaunchGroup} for the given mode set and
	 * {@link ILaunchConfigurationType}.
	 * @param type the type
	 * @param modeset the set of modes, which are combined to one mode string
	 * @return the associated {@link ILaunchGroup} or <code>null</code>
	 *
	 * @since 3.4.0
	 */
	public ILaunchGroup getLaunchGroup(ILaunchConfigurationType type, Set<String> modeset) {
		StringBuilder buff = new StringBuilder();
		for (Iterator<String> iter = modeset.iterator(); iter.hasNext();) {
			buff.append(iter.next());
			if (iter.hasNext()) {
				buff.append(","); //$NON-NLS-1$
			}
		}
		return getLaunchGroup(type, buff.toString());
	}

	/**
	 * Returns the private launch configuration used as a place-holder to represent/store
	 * the information associated with a launch configuration type.
	 *
	 * @param type launch configuration type
	 * @return launch configuration
	 * @throws CoreException if an excpetion occurs
	 * @since 3.0
	 */
	public static ILaunchConfiguration getSharedTypeConfig(ILaunchConfigurationType type) throws CoreException {
		String id = type.getIdentifier();
		String name = id + ".SHARED_INFO"; //$NON-NLS-1$
		ILaunchConfiguration shared = null;
		for (ILaunchConfiguration configuration : DebugPlugin.getDefault().getLaunchManager().getLaunchConfigurations(type)) {
			if (configuration.getName().equals(name)) {
				shared = configuration;
				break;
			}
		}

		if (shared == null) {
			// create a new shared config
			ILaunchConfigurationWorkingCopy workingCopy;
			workingCopy = type.newInstance(null, name);
			workingCopy.setAttribute(IDebugUIConstants.ATTR_PRIVATE, true);
			// null entries indicate default settings
			// save
			shared = workingCopy.doSave();
		}
		return shared;
	}


	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#doneSaving(org.eclipse.core.resources.ISaveContext)
	 */
	@Override
	public void doneSaving(ISaveContext context) {}

	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#prepareToSave(org.eclipse.core.resources.ISaveContext)
	 */
	@Override
	public void prepareToSave(ISaveContext context) throws CoreException {}

	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#rollback(org.eclipse.core.resources.ISaveContext)
	 */
	@Override
	public void rollback(ISaveContext context) {}

	/**
	 * @see org.eclipse.core.resources.ISaveParticipant#saving(org.eclipse.core.resources.ISaveContext)
	 */
	@Override
	public void saving(ISaveContext context) throws CoreException {
		try {
			persistLaunchHistory();
		}  catch (IOException e) {
			throw new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Internal error saving launch history", e)); //$NON-NLS-1$
		} catch (ParserConfigurationException e) {
			throw new CoreException(new Status(IStatus.ERROR, DebugUIPlugin.getUniqueIdentifier(), "Internal error saving launch history", e)); //$NON-NLS-1$
		}
	}

	/**
	 * Sets the given launch to be the most recent launch in the launch
	 * history (for applicable histories).
	 * <p>
	 * @param launch the launch to prepend to its associated histories
	 * @since 3.3
	 */
	public void setRecentLaunch(ILaunch launch) {
		ILaunchGroup[] groups = DebugUITools.getLaunchGroups();
		int size = groups.length;
		for (int i = 0; i < size; i++) {
			String id = groups[i].getIdentifier();
			LaunchHistory history = getLaunchHistory(id);
			if (history != null) {
				history.launchAdded(launch);
			}
		}
	}

}
