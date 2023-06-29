/*******************************************************************************
 * Copyright (c) 2005, 2016 IBM Corporation and others.
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
 *     Wind River Systems - added saving and restoring properties
 *******************************************************************************/
package org.eclipse.debug.internal.ui.viewers.model.provisional;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.ui.IElementFactory;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

/**
 * Presentation context.
 * <p>
 * Clients may instantiate and subclass this class.
 * </p>
 * @since 3.2
 */
public class PresentationContext implements IPresentationContext {

	private static final String PRESENTATION_CONTEXT_PROPERTIES = "PRESENTATION_CONTEXT_PROPERTIES";  //$NON-NLS-1$
	private static final String BOOLEAN = "BOOLEAN";  //$NON-NLS-1$
	private static final String STRING = "STRING";  //$NON-NLS-1$
	private static final String INTEGER = "INTEGER";  //$NON-NLS-1$
	private static final String PERSISTABLE = "PERSISTABLE";  //$NON-NLS-1$

	final private String fId;
	final private ListenerList<IPropertyChangeListener> fListeners = new ListenerList<>();
	final private Map<String, Object> fProperties = new HashMap<>();
	private IWorkbenchWindow fWindow;
	private IWorkbenchPart fPart;

	/**
	 * Constructs a presentation context for the given id.
	 *
	 * @param id presentation context id
	 */
	public PresentationContext(String id) {
		this (id, null, null);
	}

	/**
	 * Constructs a presentation context for the given id and window.
	 *
	 * @param id presentation context id
	 * @param window presentation context window, may be <code>null</code>
	 */
	public PresentationContext(String id, IWorkbenchWindow window) {
		this (id, window, null);
	}

	/**
	 * Constructs a presentation context for the given id and part.
	 * The presentation context window is derived from the part.
	 *
	 * @param id presentation context id
	 * @param part presentation context part, may be <code>null</code>
	 */
	public PresentationContext(String id, IWorkbenchPart part) {
		this (id, part == null ? null : part.getSite().getWorkbenchWindow(), part);
	}

	/**
	 * Constructs a presentation context for the given id and part.
	 * The presentation context id and window are derived from the part.
	 *
	 * @param part presentation context part, can NOT be <code>null</code>
	 */
	public PresentationContext(IWorkbenchPart part) {
		this (part.getSite().getId(), part.getSite().getWorkbenchWindow(), part);
	}

	private PresentationContext(String id, IWorkbenchWindow window, IWorkbenchPart part) {
		fId = id;
		fWindow = window;
		fPart = part;
	}

	@Override
	public String[] getColumns() {
		return (String[]) getProperty(IPresentationContext.PROPERTY_COLUMNS);
	}

	/**
	 * Fires a property change event to all registered listeners
	 *
	 * @param property property name
	 * @param oldValue old value or <code>null</code>
	 * @param newValue new value or <code>null</code>
	 */
	protected void firePropertyChange(String property, Object oldValue, Object newValue) {
		if (!fListeners.isEmpty()) {
			final PropertyChangeEvent event = new PropertyChangeEvent(this, property, oldValue, newValue);
			for (IPropertyChangeListener iPropertyChangeListener : fListeners) {
				final IPropertyChangeListener listener = iPropertyChangeListener;
				SafeRunner.run(new SafeRunnable() {
					@Override
					public void run() throws Exception {
						listener.propertyChange(event);
					}
				});
			}
		}
	}

	/**
	 * Sets the visible column ids.
	 *
	 * @param ids column identifiers
	 */
	public void setColumns(String[] ids) {
		setProperty(IPresentationContext.PROPERTY_COLUMNS, ids);
	}

	@Override
	public void dispose() {
		fProperties.clear();
		setProperty(PROPERTY_DISPOSED, Boolean.TRUE);
		fListeners.clear();
		// Free the reference to fWindow (Bug 321658).
		fWindow = null;
		fPart = null;
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		fListeners.remove(listener);
	}

	@Override
	public String getId() {
		return fId;
	}

	@Override
	public Object getProperty(String property) {
		synchronized (fProperties) {
			return fProperties.get(property);
		}
	}

	@Override
	public void setProperty(String property, Object value) {
		Object oldValue = null;
		boolean propertySet = false;
		synchronized (fProperties) {
			oldValue = fProperties.get(property);
			if (!isEqual(oldValue, value)) {
				propertySet = true;
				fProperties.put(property, value);
			}
		}

		if (propertySet) {
			firePropertyChange(property, oldValue, value);
		}
	}

	/**
	 * Restores the presentation context properties from the given memento.
	 * @param memento Memento to restore from.
	 */
	public void initProperties(IMemento memento) {
		IMemento presentationMemento = null;

		for (IMemento childMemento : memento.getChildren(PRESENTATION_CONTEXT_PROPERTIES)) {
			if (getId().equals(childMemento.getID())) {
				presentationMemento = childMemento;
				break;
			}
		}

		if (presentationMemento != null) {
			for (IMemento stringProperty : presentationMemento.getChildren(STRING)) {
				fProperties.put(stringProperty.getID(), stringProperty.getString(STRING));
			}

			for (IMemento integerMemento : presentationMemento.getChildren(INTEGER)) {
				fProperties.put(integerMemento.getID(), integerMemento.getInteger(INTEGER));
			}

			for (IMemento booleanMemento : presentationMemento.getChildren(BOOLEAN)) {
				fProperties.put(booleanMemento.getID(), booleanMemento.getBoolean(BOOLEAN));
			}

			for (IMemento persistableMemento : presentationMemento.getChildren(PERSISTABLE)) {
				String factoryID = persistableMemento.getString(PERSISTABLE);
				if (factoryID != null) {
					IElementFactory factory = PlatformUI.getWorkbench().getElementFactory(factoryID);
					if (factory != null) {
						Object element = factory.createElement(persistableMemento);
						if (element != null) {
							fProperties.put(persistableMemento.getID(), element);
						}
					}
				}
			}
		}
	}

	/**
	 * Saves the current presentation context properties to the given memento.
	 * @param memento Memento to save to.
	 */
	public void saveProperites(IMemento memento) {
		if (fProperties.isEmpty()) {
			return;
		}
		IMemento properties = memento.createChild(PRESENTATION_CONTEXT_PROPERTIES, getId());
		for (Entry<String, Object> entry : fProperties.entrySet()) {
			if (entry.getValue() instanceof String) {
				IMemento value = properties.createChild(STRING, entry.getKey());
				value.putString(STRING, (String)entry.getValue());
			} else if (entry.getValue() instanceof Integer) {
				IMemento value = properties.createChild(INTEGER, entry.getKey());
				value.putInteger(INTEGER, ((Integer)entry.getValue()).intValue());
			} else if (entry.getValue() instanceof Boolean) {
				IMemento value = properties.createChild(BOOLEAN, entry.getKey());
				value.putBoolean(BOOLEAN, ((Boolean)entry.getValue()).booleanValue());
			} else if (entry.getValue() instanceof IPersistableElement) {
				IPersistableElement persistable = (IPersistableElement)entry.getValue();
				IMemento value = properties.createChild(PERSISTABLE, entry.getKey());
				value.putString(PERSISTABLE, persistable.getFactoryId());
				persistable.saveState(value);
			}
		}
	}

	private boolean isEqual(Object a, Object b) {
		if (a == null) {
			return b == null;
		}
		return a.equals(b);
	}

	@Override
	public String[] getProperties() {
		synchronized (fProperties) {
			Set<String> keys = fProperties.keySet();
			return keys.toArray(new String[keys.size()]);
		}
	}

	@Override
	public IWorkbenchPart getPart() {
		return fPart;
	}

	@Override
	public IWorkbenchWindow getWindow() {
		return fWindow;
	}


}
