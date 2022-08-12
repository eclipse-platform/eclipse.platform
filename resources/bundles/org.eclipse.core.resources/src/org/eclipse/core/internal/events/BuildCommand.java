/*******************************************************************************
 * Copyright (c) 2000, 2015 IBM Corporation and others.
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
 *     James Blackburn (Broadcom Corp.) - Custom trigger builder #equals
 *     Broadcom Corporation - ongoing development
 *     Lars Vogel <Lars.Vogel@vogella.com> - Bug 473427
 *******************************************************************************/
package org.eclipse.core.internal.events;

import java.util.*;
import org.eclipse.core.internal.resources.ModelObject;
import org.eclipse.core.resources.*;
import org.eclipse.core.runtime.*;

/**
 * The concrete implementation of <tt>ICommand</tt>.  This object
 * stores information about a particular type of builder.
 *
 *  If the builder has been instantiated, a reference to the builder is held.
 *  If the builder supports multiple build configurations, a reference to the
 *  builder for each configuration is held.
 */
public class BuildCommand extends ModelObject implements ICommand {
	/**
	 * Internal flag masks for different build triggers.
	 */
	private static final int MASK_AUTO = 0x01;
	private static final int MASK_INCREMENTAL = 0x02;
	private static final int MASK_FULL = 0x04;
	private static final int MASK_CLEAN = 0x08;

	/**
	 * Flag bit indicating if this build command is configurable
	 */
	private static final int MASK_CONFIGURABLE = 0x10;

	/**
	 * Flag bit indicating if the configurable bit has been loaded from
	 * the builder extension declaration in XML yet.
	 */
	private static final int MASK_CONFIG_COMPUTED = 0x20;

	private static final int ALL_TRIGGERS = MASK_AUTO | MASK_CLEAN | MASK_FULL | MASK_INCREMENTAL;

	protected HashMap<String, String> arguments = new HashMap<>(0);

	/** Have we checked the supports configurations flag */
	private boolean supportsConfigurationsCalculated;
	/** Does this builder support configurations */
	private boolean supportsConfigurations;
	/**
	 * The builder instance for this command. Null if the builder has
	 * not yet been instantiated.
	 */
	private IncrementalProjectBuilder builder;
	/**
	 * The builders for this command if the builder supports multiple configurations
	 */
	private HashMap<IBuildConfiguration, IncrementalProjectBuilder> builders;

	/**
	 * The triggers that this builder will respond to.  Since build triggers are not
	 * bit-maskable, we use internal bit masks to represent each
	 * trigger (MASK_* constants). By default, a command responds to all
	 * build triggers.
	 */
	private int triggers = ALL_TRIGGERS;

	/**
	 * Lock used to synchronize access to {@link #builder} and {@link #builders}.
	 */
	private final Object builderLock = new Object();

	/**
	 * Returns the trigger bit mask for the given trigger constant.
	 */
	private static int maskForTrigger(int trigger) {
		switch (trigger) {
			case IncrementalProjectBuilder.AUTO_BUILD :
				return MASK_AUTO;
			case IncrementalProjectBuilder.INCREMENTAL_BUILD :
				return MASK_INCREMENTAL;
			case IncrementalProjectBuilder.FULL_BUILD :
				return MASK_FULL;
			case IncrementalProjectBuilder.CLEAN_BUILD :
				return MASK_CLEAN;
		}
		return 0;
	}

	public BuildCommand() {
		super(""); //$NON-NLS-1$
	}

	@Override
	public Object clone() {
		BuildCommand result = (BuildCommand) super.clone();
		if (result == null)
			return null;
		result.setArguments(getArguments());
		//don't let references to builder instances leak out because they reference trees
		result.setBuilders(null);
		return result;
	}

	/**
	 * Computes whether this build command allows configuration of its
	 * triggers, based on information in the builder extension declaration.
	 */
	private void computeIsConfigurable() {
		triggers |= MASK_CONFIG_COMPUTED;
		IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS, name);
		if (extension != null) {
			IConfigurationElement[] configs = extension.getConfigurationElements();
			if (configs.length != 0) {
				String value = configs[0].getAttribute("isConfigurable"); //$NON-NLS-1$
				setConfigurable(value != null && value.equalsIgnoreCase(Boolean.TRUE.toString()));
			}
		}
	}

	@Override
	public boolean equals(Object object) {
		if (this == object)
			return true;
		if (!(object instanceof BuildCommand))
			return false;
		BuildCommand command = (BuildCommand) object;
		// equal if same builder name, arguments, and triggers
		return getBuilderName().equals(command.getBuilderName()) && getArguments(false).equals(command.getArguments(false)) && (triggers & ALL_TRIGGERS) == (command.triggers & ALL_TRIGGERS);
	}

	@Override
	public Map<String, String> getArguments() {
		return getArguments(true);
	}

	@SuppressWarnings({"unchecked"})
	public Map<String, String> getArguments(boolean makeCopy) {
		return arguments == null ? null : (makeCopy ? (Map<String, String>) arguments.clone() : arguments);
	}

	/**
	 * @return A copy of the internal map {@link IBuildConfiguration} -&gt; {@link IncrementalProjectBuilder} if
	 * this build command supports multiple configurations. Otherwise return the {@link IncrementalProjectBuilder}
	 * associated with this build command.
	 */
	public Object getBuilders() {
		synchronized (builderLock) {
			if (supportsConfigs()) {
				return builders == null ? null : new HashMap<>(builders);
			}
			return builder;
		}
	}

	/**
	 * Return the {@link IncrementalProjectBuilder} for the
	 * {@link IBuildConfiguration} If this builder is configuration agnostic, the
	 * same {@link IncrementalProjectBuilder} is returned for all configurations.
	 *
	 * @param config the config to get a builder for
	 * @return {@link IncrementalProjectBuilder} corresponding to config
	 */
	public IncrementalProjectBuilder getBuilder(IBuildConfiguration config) {
		synchronized (builderLock) {
			if (builders != null && supportsConfigs())
				return builders.get(config);
			return builder;
		}
	}

	@Override
	public String getBuilderName() {
		return getName();
	}

	@Override
	public int hashCode() {
		// hash on name and trigger
		return 37 * getName().hashCode() + (ALL_TRIGGERS & triggers);
	}

	@Override
	public boolean isBuilding(int trigger) {
		return (triggers & maskForTrigger(trigger)) != 0;
	}

	@Override
	public boolean isConfigurable() {
		if ((triggers & MASK_CONFIG_COMPUTED) == 0)
			computeIsConfigurable();
		return (triggers & MASK_CONFIGURABLE) != 0;
	}

	public boolean supportsConfigs() {
		if (!supportsConfigurationsCalculated) {
			IExtension extension = Platform.getExtensionRegistry().getExtension(ResourcesPlugin.PI_RESOURCES, ResourcesPlugin.PT_BUILDERS, name);
			if (extension != null) {
				IConfigurationElement[] configs = extension.getConfigurationElements();
				if (configs.length != 0) {
					String value = configs[0].getAttribute("supportsConfigurations"); //$NON-NLS-1$
					supportsConfigurations = (value != null && value.equalsIgnoreCase(Boolean.TRUE.toString()));
				}
			}
			supportsConfigurationsCalculated = true;
		}
		return supportsConfigurations;
	}

	@Override
	public void setArguments(Map<String, String> value) {
		// copy parameter for safety's sake
		arguments = value == null ? null : new HashMap<>(value);
	}

	/**
	 * Set the IncrementalProjectBuilder(s) for this command
	 *
	 * @param value a single {@link IncrementalProjectBuilder} or a {@link Map} of
	 *              {@link IncrementalProjectBuilder} indexed by
	 *              {@link IBuildConfiguration}
	 */
	@SuppressWarnings("unchecked")
	public void setBuilders(Object value) {
		synchronized (builderLock) {
			if (value == null) {
				builder = null;
				builders = null;
			} else {
				if (value instanceof IncrementalProjectBuilder)
					builder = (IncrementalProjectBuilder) value;
				else
					builders = new HashMap<>((Map<IBuildConfiguration, IncrementalProjectBuilder>) value);
			}
		}
	}

	/**
	 * Add an IncrementalProjectBuilder for the given configuration.
	 *
	 * For builders which don't respond to multiple configurations, there's only one builder
	 * instance.
	 *
	 * Does nothing if a builder was already added for the specified configuration,
	 * or if a builder was added and this builder does not support multiple configurations.
	 *
	 * @param config
	 * @param newBuilder
	 */
	public void addBuilder(IBuildConfiguration config, IncrementalProjectBuilder newBuilder) {
		synchronized (builderLock) {
			// Builder shouldn't already exist in this build command
			IncrementalProjectBuilder configBuilder = builders == null ? null : builders.get(config);
			if (configBuilder == null && builder == null) {
				if (supportsConfigs()) {
					if (builders == null)
						builders = new HashMap<>(1);
					builders.put(config, newBuilder);
				} else
					builder = newBuilder;
			}
		}
	}

	@Override
	public void setBuilderName(String value) {
		//don't allow builder name to be null
		setName(value == null ? "" : value); //$NON-NLS-1$
	}

	@Override
	public void setBuilding(int trigger, boolean value) {
		if (!isConfigurable())
			return;
		if (value)
			triggers |= maskForTrigger(trigger);
		else
			triggers &= ~maskForTrigger(trigger);
	}

	/**
	 * Sets whether this build command allows its build triggers to be configured.
	 * This value should only be set when the builder extension declaration is
	 * read from the registry, or when a build command is read from the project
	 * description file on disk.  The value is not otherwise mutable.
	 */
	public void setConfigurable(boolean value) {
		triggers |= MASK_CONFIG_COMPUTED;
		if (value)
			triggers |= MASK_CONFIGURABLE;
		else
			triggers = ALL_TRIGGERS;
	}

	/**
	 * For debugging purposes only
	 */
	@Override
	public String toString() {
		return "BuildCommand(" + getName() + ")";//$NON-NLS-1$ //$NON-NLS-2$
	}
}
