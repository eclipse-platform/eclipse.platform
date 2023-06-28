/*******************************************************************************
 * Copyright (c) 2000, 2016 IBM Corporation and others.
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
package org.eclipse.ui.console;

import org.eclipse.core.runtime.ISafeRunnable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.SafeRunner;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.util.IPropertyChangeListener;
import org.eclipse.jface.util.PropertyChangeEvent;
import org.eclipse.jface.viewers.IBasicPropertyConstants;
import org.eclipse.ui.internal.console.ConsoleMessages;

/**
 * Common function for consoles.
 * <p>
 * Clients implementing consoles should subclass this class.
 * </p>
 * @since 3.0
 */
public abstract class AbstractConsole implements IConsole {

	// property listeners
	private ListenerList<IPropertyChangeListener> fListeners;

	/**
	 * Console name
	 */
	private String fName = null;

	/**
	 * Console image descriptor
	 */
	private ImageDescriptor fImageDescriptor = null;

	/**
	 * Console type identifier
	 */
	private String fType = null;

	/**
	 * Used to notify this console of lifecycle methods <code>init()</code>
	 * and <code>dispose()</code>.
	 */
	class Lifecycle implements IConsoleListener {

		@Override
		public void consolesAdded(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console == AbstractConsole.this) {
					initialize();
				}
			}

		}

		@Override
		public void consolesRemoved(IConsole[] consoles) {
			for (IConsole console : consoles) {
				if (console == AbstractConsole.this) {
					ConsolePlugin.getDefault().getConsoleManager().removeConsoleListener(this);
					destroy();
				}
			}
		}
	}

	/**
	 * Notifies listeners of property changes, handling any exceptions
	 */
	class PropertyNotifier implements ISafeRunnable {

		private IPropertyChangeListener fListener;
		private PropertyChangeEvent fEvent;

		@Override
		public void handleException(Throwable exception) {
			IStatus status = new Status(IStatus.ERROR, ConsolePlugin.getUniqueIdentifier(), IConsoleConstants.INTERNAL_ERROR, ConsoleMessages.AbstractConsole_0, exception);
			ConsolePlugin.log(status);
		}

		@Override
		public void run() throws Exception {
			fListener.propertyChange(fEvent);
		}

		/**
		 * Notifies listeners of the property change
		 *
		 * @param event the event that describes the property that has changed
		 */
		public void notify(PropertyChangeEvent event) {
			if (fListeners == null) {
				return;
			}
			fEvent = event;
			for (IPropertyChangeListener iPropertyChangeListener : fListeners) {
				fListener = iPropertyChangeListener;
				SafeRunner.run(this);
			}
			fListener = null;
		}
	}

	/**
	 * Constructs a new console with the given name and image.
	 *
	 * @param name console name, cannot be <code>null</code>
	 * @param imageDescriptor image descriptor, or <code>null</code> if none
	 * @param autoLifecycle whether this console's lifecycle methods should be called
	 *  automatically when it is added (<code>initialize()</code>) and removed
	 *  (<code>destroy()</code>) from the console manager. When <code>false</code>,
	 *  clients are responsible for calling the lifecycle methods.
	 * @since 3.1
	 */
	public AbstractConsole(String name, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		this(name, null, imageDescriptor, autoLifecycle);
	}

	/**
	 * Constructs a new console with the given name, type, image and lifecycle.
	 *
	 * @param name console name, cannot be <code>null</code>
	 * @param type console type identifier or <code>null</code>
	 * @param imageDescriptor image descriptor, or <code>null</code> if none
	 * @param autoLifecycle whether this console's lifecycle methods should be called
	 *  automatically when it is added (<code>initialize()</code>) and removed
	 *  (<code>destroy()</code>) from the console manager. When <code>false</code>,
	 *  clients are responsible for calling the lifecycle methods.
	 * @since 3.1
	 */
	public AbstractConsole(String name, String type, ImageDescriptor imageDescriptor, boolean autoLifecycle) {
		setName(name);
		setType(type);
		setImageDescriptor(imageDescriptor);
		if (autoLifecycle) {
			ConsolePlugin.getDefault().getConsoleManager().addConsoleListener(new Lifecycle());
		}
	}

	/**
	 * Constructs a new console with the given name and image. The console's lifecycle
	 * methods <code>init()</code> and <code>dispose()</code> will be called when the
	 * console is added and removed from the console manager.
	 *
	 * @param name console name, cannot be <code>null</code>
	 * @param imageDescriptor image descriptor, or <code>null</code> if none
	 */
	public AbstractConsole(String name, ImageDescriptor imageDescriptor) {
		this(name, imageDescriptor, true);
	}

	@Override
	public String getName() {
		return fName;
	}

	/**
	 * Sets the name of this console to the specified value and notifies
	 * property listeners of the change.
	 *
	 * @param name the new name
	 */
	protected void setName(String name) {
		if (!name.equals(fName)) {
			String old = fName;
			fName = name;
			firePropertyChange(this, IBasicPropertyConstants.P_TEXT, old, name);
		}
	}

	@Override
	public ImageDescriptor getImageDescriptor() {
		return fImageDescriptor;
	}

	/**
	 * Sets the image descriptor for this console to the specified value and notifies
	 * property listeners of the change.
	 *
	 * @param imageDescriptor the new image descriptor
	 */
	protected void setImageDescriptor(ImageDescriptor imageDescriptor) {
		ImageDescriptor old = fImageDescriptor;
		fImageDescriptor =imageDescriptor;
		firePropertyChange(this, IBasicPropertyConstants.P_IMAGE, old, imageDescriptor);
	}

	@Override
	public void addPropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners == null) {
			fListeners = new ListenerList<>();
		}
		fListeners.add(listener);
	}

	@Override
	public void removePropertyChangeListener(IPropertyChangeListener listener) {
		if (fListeners != null) {
			fListeners.remove(listener);
		}
	}

	/**
	 * Notify all listeners that the given property has changed.
	 *
	 * @param source the object on which a property has changed
	 * @param property identifier of the property that has changed
	 * @param oldValue the old value of the property, or <code>null</code>
	 * @param newValue the new value of the property, or <code>null</code>
	 */
	public void firePropertyChange(Object source, String property, Object oldValue, Object newValue) {
		if (fListeners == null) {
			return;
		}
		PropertyNotifier notifier = new PropertyNotifier();
		notifier.notify(new PropertyChangeEvent(source, property, oldValue, newValue));
	}

	/**
	 * Initializes this console. This method should only be called by clients managing a
	 * console's lifecycle, otherwise this method will be called automatically when this console
	 * is added to the console manager. The method is called once to initialize this console,
	 * marking the beginning of its lifecycle.
	 *
	 * @since 3.1
	 */
	public final void initialize() {
		init();
	}

	/**
	 * Called when this console is added to the console manager. Default
	 * implementation does nothing. Subclasses may override.
	 * <p>
	 * Since 3.1, this method is only called automatically if this console was
	 * created with an automatic lifecycle.
	 * </p>
	 */
	protected void init() {
	}

	/**
	 * Disposes this console. This method should only be called by clients managing a
	 * console's lifecycle, otherwise this method will be called automatically when this
	 * console is removed from the console manager. The method is called once to dispose
	 * this console, after which this console will no longer be used.
	 *
	 * @since 3.1
	 */
	public final void destroy() {
		dispose();
	}

	/**
	 * Called when this console is removed from the console manager. Default
	 * implementation does nothing. Subclasses may override.
	 * <p>
	 * Since 3.1, this methods is only called automatically if this console was
	 * created with an automatic lifecycle.
	 * </p>
	 */
	protected void dispose() {
	}

	/**
	 * Shows this console in all console views. This console will be become visible
	 * if another console is currently pinned.
	 *
	 * @since 3.1
	 */
	public void activate() {
		ConsolePlugin.getDefault().getConsoleManager().showConsoleView(this);
	}

	/**
	 * Sets this console's type identifier.
	 *
	 * @param typeIdentifier the type identifier for this console
	 * @since 3.1
	 */
	protected void setType(String typeIdentifier) {
		fType = typeIdentifier;
	}

	/**
	 * @since 3.1
	 */
	@Override
	public String getType() {
		return fType;
	}

	/**
	 * Returns the help context identifier for this console, or <code>null</code>
	 * if none. When a non-<code>null</code> value is returned the associated help
	 * will be installed for this console.
	 *
	 * @return help context id or <code>null</code>
	 * @since 3.2
	 */
	public String getHelpContextId() {
		return null;
	}

}
