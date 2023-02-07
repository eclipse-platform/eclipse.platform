/*******************************************************************************
 * Copyright (c) 2002, 2019 IBM Corporation and others.
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
package org.eclipse.ui.internal.cheatsheets.registry;

import java.text.Collator;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtensionDelta;
import org.eclipse.core.runtime.IExtensionRegistry;
import org.eclipse.core.runtime.IRegistryChangeEvent;
import org.eclipse.core.runtime.IRegistryChangeListener;
import org.eclipse.core.runtime.Platform;
import org.eclipse.ui.internal.cheatsheets.CheatSheetPlugin;
import org.eclipse.ui.internal.cheatsheets.ICheatSheetResource;
import org.eclipse.ui.internal.cheatsheets.Messages;

/**
 *  Instances access the registry that is provided at creation time
 *  in order to determine the contained CheatSheet Contents
 */
public class CheatSheetRegistryReader extends RegistryReader implements IRegistryChangeListener {

	private static class CategoryNode {
		private Category category;
		private String path;
		public CategoryNode(Category cat) {
			category = cat;
			path = ICheatSheetResource.EMPTY_STRING;
			String[] categoryPath = category.getParentPath();
			if (categoryPath != null) {
				for (int nX = 0; nX < categoryPath.length; nX++) {
					path += categoryPath[nX] + '/';
				}
			}
			path += cat.getId();
		}
		public Category getCategory() {
			return category;
		}
		public String getPath() {
			return path;
		}
	}

	/**
	 * Represents a taskEditor entry in the registry
	 */
	public static class TaskEditorNode {
		private String className;
		private String iconPath;
		private String id;
		private String pluginId;
		public void setClassName(String className) {
			this.className = className;
		}
		public String getClassName() {
			return className;
		}
		public void setIconPath(String iconPath) {
			this.iconPath = iconPath;
		}
		public String getIconPath() {
			return iconPath;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
		public void setPluginId(String pluginId) {
			this.pluginId = pluginId;
		}
		public String getPluginId() {
			return pluginId;
		}
	}

	/**
	 * Represents a taskExplorer entry in the registry
	 */
	public static class TaskExplorerNode {
		private String className;
		private String iconPath;
		private String name;
		private String id;
		private String pluginId;
		public void setClassName(String className) {
			this.className = className;
		}
		public String getClassName() {
			return className;
		}
		public void setIconPath(String iconPath) {
			this.iconPath = iconPath;
		}
		public String getIconPath() {
			return iconPath;
		}
		public void setName(String name) {
			this.name = name;
		}
		public String getName() {
			return name;
		}
		public void setId(String id) {
			this.id = id;
		}
		public String getId() {
			return id;
		}
		public void setPluginId(String pluginId) {
			this.pluginId = pluginId;
		}
		public String getPluginId() {
			return pluginId;
		}
	}

	// constants
	private final static String ATT_CATEGORY = "category"; //$NON-NLS-1$
	public final static String ATT_CONTENTFILE = "contentFile"; //$NON-NLS-1$
	protected final static String ATT_ICON = "icon"; //$NON-NLS-1$
	protected final static String ATT_ID = "id"; //$NON-NLS-1$
	protected final static String ATT_LISTENERCLASS = "listener"; //$NON-NLS-1$
	protected final static String ATT_NAME = "name"; //$NON-NLS-1$
	protected final static String ATT_CLASS = "class"; //$NON-NLS-1$
	private final static String ATT_COMPOSITE = "composite"; //$NON-NLS-1$
	private final static String CATEGORY_SEPARATOR = "/"; //$NON-NLS-1$
	private final static String ATT_ITEM_ATTRIBUTE = "itemAttribute"; //$NON-NLS-1$
	private static CheatSheetRegistryReader instance;
	private final static String TAG_CATEGORY = "category"; //$NON-NLS-1$
	public final static String TAG_CHEATSHEET = "cheatsheet"; //$NON-NLS-1$
	protected final static String TAG_ITEM_EXTENSION = "itemExtension"; //$NON-NLS-1$
	protected final static String TAG_TASK_EDITOR = "taskEditor"; //$NON-NLS-1$
	protected final static String TAG_TASK_EXPLORER = "taskExplorer"; //$NON-NLS-1$
	protected final static String trueString = "TRUE"; //$NON-NLS-1$
	private final static String UNCATEGORIZED_CHEATSHEET_CATEGORY = "org.eclipse.ui.Other"; //$NON-NLS-1$
	private final static String UNCATEGORIZED_CHEATSHEET_CATEGORY_LABEL = Messages.CHEAT_SHEET_OTHER_CATEGORY;
	public final static String CHEAT_SHEET_CONTENT = "cheatSheetContent"; //$NON-NLS-1$

	/**
	 * Returns a list of cheatsheets, project and not.
	 *
	 * The return value for this method is cached since computing its value
	 * requires non-trivial work.
	 */
	public static CheatSheetRegistryReader getInstance() {
		if (instance == null) {
			instance = new CheatSheetRegistryReader();
			IExtensionRegistry xregistry = Platform.getExtensionRegistry();
			xregistry.addRegistryChangeListener(instance, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID);
		}

		return instance;
	}

	protected ArrayList<CheatSheetItemExtensionElement> cheatsheetItemExtensions;
	protected CheatSheetCollectionElement cheatsheets;
	private ArrayList<Category> deferCategories = null;
	private ArrayList<CheatSheetElement> deferCheatSheets = null;
	private final String csItemExtension = "cheatSheetItemExtension"; //$NON-NLS-1$
	protected Map<String, TaskExplorerNode> taskExplorers = new HashMap<>();
	protected Map<String, TaskEditorNode> taskEditors = new HashMap<>();
	private Map<String, Object> nestedCategoryIds = new HashMap<>();

	/**
	 *	Create an instance of this class.
	 */
	private CheatSheetRegistryReader() {
	}

	/**
	 * Adds new cheatsheet to the provided collection. Override to
	 * provide more logic.
	 * <p>
	 * This implementation uses a defering strategy.  For more info see
	 * <code>readCheatSheets</code>.
	 * </p>
	 */
	protected void addNewElementToResult(CheatSheetElement element, IConfigurationElement config, CheatSheetCollectionElement cheatsheets2) {
		deferCheatSheet(element);
	}

	/**
	 * Returns a new CheatSheetElement configured according to the parameters
	 * contained in the passed Registry.
	 *
	 * May answer null if there was not enough information in the Extension to create
	 * an adequate cheatsheet
	 */
	protected CheatSheetElement createCheatSheetElement(IConfigurationElement element) {
		// CheatSheetElements must have a name attribute
		String nameString = element.getAttribute(ATT_NAME);
		if (nameString == null) {
			logMissingAttribute(element, ATT_NAME);
			return null;
		}
		CheatSheetElement result = new CheatSheetElement(nameString);
		if (initializeCheatSheet(result, element))
			return result; // ie.- initialization was successful

		return null;
	}

	/**
	 *	Create and answer a new CheatSheetCollectionElement, configured as a
	 *	child of <code>parent</code>
	 *
	 *	@return org.eclipse.ui.internal.model.CheatSheetCollectionElement
	 *	@param parent org.eclipse.ui.internal.model.CheatSheetCollectionElement
	 *	@param childName java.lang.String
	 */
	protected CheatSheetCollectionElement createCollectionElement(CheatSheetCollectionElement parent, String pluginId, String id, String label) {
		CheatSheetCollectionElement newElement = new CheatSheetCollectionElement(pluginId, id, label, parent);

		parent.add(newElement);
		return newElement;
	}

	/**
	 * Creates empty element collection. Overrider to fill
	 * initial elements, if needed.
	 */
	protected CheatSheetCollectionElement createEmptyCheatSheetCollection() {
		return new CheatSheetCollectionElement(null, "root", "root", null); //$NON-NLS-1$//$NON-NLS-2$
	}

	/**
	 * Stores a category element for deferred addition.
	 */
	private void deferCategory(IConfigurationElement config) {
		// Create category.
		Category category = null;
		try {
			category = new Category(config);
		} catch (CoreException e) {
			CheatSheetPlugin.getPlugin().getLog().log(e.getStatus());
			return;
		}

		// Defer for later processing.
		if (deferCategories == null)
			deferCategories = new ArrayList<>(20);
		deferCategories.add(category);
	}

	/**
	 * Stores a cheatsheet element for deferred addition.
	 */
	private void deferCheatSheet(CheatSheetElement element) {
		if (deferCheatSheets == null)
			deferCheatSheets = new ArrayList<>(50);
		deferCheatSheets.add(element);
	}

	/**
	 *	Returns the first cheatsheet
	 *  with a given id.
	 */
	public CheatSheetElement findCheatSheet(String id) {
		Object[] cheatsheetsList = getCheatSheets().getChildren();
		for (Object element2 : cheatsheetsList) {
			CheatSheetCollectionElement collection = (CheatSheetCollectionElement) element2;
			CheatSheetElement element = collection.findCheatSheet(id, true);
			if (element != null)
				return element;
		}
		return null;
	}

	/**
	 *	Returns the first task editor
	 *  with a given id.
	 */
	public TaskEditorNode findTaskEditor(String id) {
		if (cheatsheets == null) {
			readCheatSheets(); // Ensure that the registry has been read
		}
		return taskEditors.get(id);
	}

	/**
	 *	Returns the first task explorer
	 *  with a given id.
	 */
	public TaskExplorerNode findTaskExplorer(String id) {
		if (cheatsheets == null) {
			readCheatSheets(); // Ensure that the registry has been read
		}
		return taskExplorers.get(id);
	}

	/**
	 * Get the list of explorer ids
	 * @return an iterator for the explorer ids
	 */
	public String[] getExplorerIds() {
		if (cheatsheets == null) {
			readCheatSheets(); // Ensure that the registry has been read
		}
		Set<String> keys = taskExplorers.keySet();
		return keys.toArray(new String[keys.size()]);
	}

	/**
	 * Finishes the addition of categories.  The categories are sorted and
	 * added in a root to depth traversal.
	 */
	private void finishCategories() {
		// If no categories just return.
		if (deferCategories == null)
			return;

		// Sort categories by flattened name.
		CategoryNode[] flatArray = new CategoryNode[deferCategories.size()];
		for (int i = 0; i < deferCategories.size(); i++) {
			flatArray[i] = new CategoryNode(deferCategories.get(i));
		}
		Sorter sorter = new Sorter() {
			private Collator collator = Collator.getInstance();

			@Override
			public boolean compare(Object o1, Object o2) {
				String s1 = ((CategoryNode) o1).getPath();
				String s2 = ((CategoryNode) o2).getPath();
				return collator.compare(s2, s1) > 0;
			}
		};
		Object[] sortedCategories = sorter.sort(flatArray);

		// Add each category.
		for (int nX = 0; nX < sortedCategories.length; nX++) {
			Category cat = ((CategoryNode) sortedCategories[nX]).getCategory();
			finishCategory(cat);
		}

		// Cleanup.
		deferCategories = null;
	}

	/**
	 * Save new category definition.
	 */
	private void finishCategory(Category category) {
		CheatSheetCollectionElement currentResult = cheatsheets;

		String[] categoryPath = category.getParentPath();
		CheatSheetCollectionElement parent = currentResult; // ie.- root

		// Traverse down into parent category.
		if (categoryPath != null) {
			nestedCategoryIds.put(category.getId(), category);
			for (String element : categoryPath) {
				CheatSheetCollectionElement tempElement = getChildWithID(parent, element);
				if (tempElement == null) {
					// The parent category is invalid.  By returning here the
					// category will be dropped and any cheatsheet within the category
					// will be added to the "Other" category.
					return;
				}
				parent = tempElement;
			}
		}

		// If another category already exists with the same id ignore this one.
		Object test = getChildWithID(parent, category.getId());
		if (test != null)
			return;

		if (parent != null) {
			CheatSheetCollectionElement collectionElement =
				createCollectionElement(parent, category.getPluginId(), category.getId(), category.getLabel());
			if (categoryPath != null) {
				nestedCategoryIds.put(category.getId(), collectionElement);
			}
		}
	}

	/**
	 *	Insert the passed cheatsheet element into the cheatsheet collection appropriately
	 *	based upon its defining extension's CATEGORY tag value
	 *
	 *	@param element CheatSheetElement
	 *	@param extension
	 *	@param currentResult CheatSheetCollectionElement
	 */
	private void finishCheatSheet(CheatSheetElement element, IConfigurationElement config, CheatSheetCollectionElement result) {
		CheatSheetCollectionElement currentResult = result;
		String category = getCategoryStringFor(config);
		StringTokenizer familyTokenizer = new StringTokenizer(category, CATEGORY_SEPARATOR);

		// use the period-separated sections of the current CheatSheet's category
		// to traverse through the NamedSolution "tree" that was previously created
		CheatSheetCollectionElement currentCollectionElement = currentResult; // ie.- root
		boolean moveToOther = false;

		while (familyTokenizer.hasMoreElements()) {
			CheatSheetCollectionElement tempCollectionElement = getChildWithID(currentCollectionElement, familyTokenizer.nextToken());

			if (tempCollectionElement == null) { // can't find the path; look for a simple path

				moveToOther = true;
				break;
			}
			currentCollectionElement = tempCollectionElement;
		}

		if (moveToOther) {
			if (nestedCategoryIds.containsKey(category)) {
				currentCollectionElement = (CheatSheetCollectionElement) nestedCategoryIds.get(category);
				currentCollectionElement.add(element);
			} else {
				moveElementToUncategorizedCategory(currentResult, element);
			}
		} else {
			currentCollectionElement.add(element);
		}
	}

	/**
	 * Finishes the addition of cheatsheets.  The cheatsheets are processed and categorized.
	 */
	private void finishCheatSheets() {
		if (deferCheatSheets != null) {
			Iterator<CheatSheetElement> iter = deferCheatSheets.iterator();
			while (iter.hasNext()) {
				CheatSheetElement cheatsheet = iter.next();
				IConfigurationElement config = cheatsheet.getConfigurationElement();
				finishCheatSheet(cheatsheet, config, cheatsheets);
			}
			deferCheatSheets = null;
		}
		nestedCategoryIds = null;
	}

	/**
	 *	Return the appropriate category (tree location) for this CheatSheet.
	 *	If a category is not specified then return a default one.
	 */
	protected String getCategoryStringFor(IConfigurationElement config) {
		String result = config.getAttribute(ATT_CATEGORY);
		if (result == null)
			result = UNCATEGORIZED_CHEATSHEET_CATEGORY;

		return result;
	}

	/**
	 * Returns a list of cheatsheets, project and not.
	 *
	 * The return value for this method is cached since computing its value
	 * requires non-trivial work.
	 */
	public CheatSheetCollectionElement getCheatSheets() {
		if (cheatsheets == null)
			readCheatSheets();
		return cheatsheets;
	}

	/**
	 *	Go through the children of  the passed parent and answer the child
	 *	with the passed name.  If no such child is found then return null.
	 *
	 *	@return org.eclipse.ui.internal.model.CheatSheetCollectionElement
	 *	@param parent org.eclipse.ui.internal.model.CheatSheetCollectionElement
	 *	@param childName java.lang.String
	 */
	protected CheatSheetCollectionElement getChildWithID(CheatSheetCollectionElement parent, String id) {
		Object[] children = parent.getChildren();
		for (int i = 0; i < children.length; ++i) {
			CheatSheetCollectionElement currentChild = (CheatSheetCollectionElement) children[i];
			if (currentChild.getId().equals(id))
				return currentChild;
		}
		return null;
	}

	/**
	 *	Initialize the passed element's properties based on the contents of
	 *	the passed registry.  Answer a boolean indicating whether the element
	 *	was able to be adequately initialized.
	 *
	 *	@return boolean
	 *	@param element CheatSheetElement
	 *	@param extension Extension
	 */
	protected boolean initializeCheatSheet(CheatSheetElement element, IConfigurationElement config) {
		element.setID(config.getAttribute(ATT_ID));
		element.setDescription(getDescription(config));
		element.setConfigurationElement(config);
		element.setRegistered(true);

		String contentFile = config.getAttribute(ATT_CONTENTFILE);
		if (contentFile != null) {
			element.setContentFile(contentFile);
		}

		// ensure that a contentfile was specified
		if (element.getConfigurationElement() == null || element.getContentFile() == null) {
			logMissingAttribute(config, ATT_CONTENTFILE);
			return false;
		}

		String listenerClass = config.getAttribute(ATT_LISTENERCLASS);
		if (listenerClass != null) {
			element.setListenerClass(listenerClass);
		}
		String composite = config.getAttribute(ATT_COMPOSITE);
		if (composite != null) {
			element.setComposite(composite.equalsIgnoreCase(trueString));
		}
		return true;
	}

	/**
	 *	Moves given element to "Other" category, previously creating one if missing.
	 */
	protected void moveElementToUncategorizedCategory(CheatSheetCollectionElement root, CheatSheetElement element) {
		CheatSheetCollectionElement otherCategory = getChildWithID(root, UNCATEGORIZED_CHEATSHEET_CATEGORY);

		if (otherCategory == null)
			otherCategory = createCollectionElement(root, null, UNCATEGORIZED_CHEATSHEET_CATEGORY, UNCATEGORIZED_CHEATSHEET_CATEGORY_LABEL);

		otherCategory.add(element);
	}

	/**
	 * Removes the empty categories from a cheatsheet collection.
	 */
	private void pruneEmptyCategories(CheatSheetCollectionElement parent) {
		Object[] children = parent.getChildren();
		for (Object element : children) {
			CheatSheetCollectionElement child = (CheatSheetCollectionElement) element;
			pruneEmptyCategories(child);
		}
	}

	/**
	 * Reads the cheatsheets in a registry.
	 * <p>
	 * This implementation uses a defering strategy.  All of the elements
	 * (categories, cheatsheets) are read.  The categories are created as the read occurs.
	 * The cheatsheets are just stored for later addition after the read completes.
	 * This ensures that cheatsheet categorization is performed after all categories
	 * have been read.
	 * </p>
	 */
	protected void readCheatSheets() {
		IExtensionRegistry xregistry = Platform.getExtensionRegistry();

		if (cheatsheets == null) {
			cheatsheets = createEmptyCheatSheetCollection();
			readRegistry(xregistry, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, CHEAT_SHEET_CONTENT);
		}

		finishCategories();
		finishCheatSheets();

		if (cheatsheets != null) {
			CheatSheetCollectionElement parent = cheatsheets;
			pruneEmptyCategories(parent);
		}
	}

	public ArrayList<CheatSheetItemExtensionElement> readItemExtensions() {
		if (cheatsheetItemExtensions == null) {
			cheatsheetItemExtensions = new ArrayList<>();

			IExtensionRegistry xregistry = Platform.getExtensionRegistry();
			//Now read the cheat sheet extensions.
			readRegistry(xregistry, ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, csItemExtension);
		}

		return cheatsheetItemExtensions;
	}

	private void createItemExtensionElement(IConfigurationElement element) {
		String className = element.getAttribute(ATT_CLASS);
		String itemAttribute = element.getAttribute(ATT_ITEM_ATTRIBUTE);

		// ensure that a class was specified
		if (className == null) {
			logMissingAttribute(element, ATT_CLASS);
			return;
		}
		// ensure that a itemAttribute was specified
		if (itemAttribute == null) {
			logMissingAttribute(element, ATT_ITEM_ATTRIBUTE);
			return;
		}

		CheatSheetItemExtensionElement itemExtensionElement = new CheatSheetItemExtensionElement();
		itemExtensionElement.setClassName(className);
		itemExtensionElement.setItemAttribute(itemAttribute);
		itemExtensionElement.setConfigurationElement(element);

		cheatsheetItemExtensions.add(itemExtensionElement);
	}

	/*
	 * Get a required attribute. Log an error if it has no value.
	 */
	private String getAndCheckAttribute(IConfigurationElement element, String name) {
		String result = element.getAttribute(name);
		if (result == null) {
			logMissingAttribute(element, name);
		}
		return result;
	}

	private void createTaskExplorerElement(IConfigurationElement element) {
		String icon = element.getAttribute(ATT_ICON);
		String className = getAndCheckAttribute(element, ATT_CLASS);
		String name = getAndCheckAttribute(element, ATT_NAME);
		String id = getAndCheckAttribute(element, ATT_ID);
		String pluginId = element.getContributor().getName();
		if (id != null && className != null && name != null ) {
			TaskExplorerNode node = new TaskExplorerNode();
			node.setId(id);
			node.setIconPath(icon);
			node.setClassName(className);
			node.setName(name);
			node.setPluginId(pluginId);
			taskExplorers.put(id, node);
		}
	}

	private void createTaskEditorElement(IConfigurationElement element) {
		String icon = getAndCheckAttribute(element, ATT_ICON);
		String className = getAndCheckAttribute(element, ATT_CLASS);
		String id = getAndCheckAttribute(element, ATT_ID);
		String pluginId = element.getContributor().getName();
		if (id != null && className != null && icon != null ) {
			TaskEditorNode node = new TaskEditorNode();
			node.setId(id);
			node.setIconPath(icon);
			node.setClassName(className);
			node.setPluginId(pluginId);
			taskEditors.put(id, node);
		}
	}

	/**
	 * Implement this method to read element attributes.
	 */
	@Override
	protected boolean readElement(IConfigurationElement element) {
		if (element.getName().equals(TAG_CATEGORY)) {
			deferCategory(element);
			return true;
		} else if (element.getName().equals(TAG_ITEM_EXTENSION)) {
			createItemExtensionElement(element);
			return true;
		} else if (element.getName().equals(TAG_TASK_EDITOR)) {
			createTaskEditorElement(element);
			return true;
		} else if (element.getName().equals(TAG_TASK_EXPLORER)) {
			createTaskExplorerElement(element);
			return true;
		} else {
			if (!element.getName().equals(TAG_CHEATSHEET))
				return false;

			CheatSheetElement cheatsheet = createCheatSheetElement(element);
			if (cheatsheet != null)
				addNewElementToResult(cheatsheet, element, cheatsheets);
			return true;
		}
	}

	@Override
	public void registryChanged(IRegistryChangeEvent event) {
		IExtensionDelta[] cheatSheetDeltas = event.getExtensionDeltas(ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, CHEAT_SHEET_CONTENT);
		if (cheatSheetDeltas.length > 0) {
			// reset the list of cheat sheets, it will be build on demand
			cheatsheets = null;
		}

		IExtensionDelta[] itemExtensionDeltas = event.getExtensionDeltas(ICheatSheetResource.CHEAT_SHEET_PLUGIN_ID, csItemExtension);
		if (itemExtensionDeltas.length > 0) {
			// reset the list of cheat sheets item extensions, it will be build on demand
			cheatsheetItemExtensions = null;
		}
	}

	public void stop() {
		IExtensionRegistry xregistry = Platform.getExtensionRegistry();
		xregistry.removeRegistryChangeListener(instance);

		instance = null;
	}
}
