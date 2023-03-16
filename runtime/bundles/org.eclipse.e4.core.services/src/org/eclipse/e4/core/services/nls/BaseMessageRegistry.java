/*******************************************************************************
 * Copyright (c) 2014, 2018 Dirk Fauth and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Dirk Fauth <dirk.fauth@googlemail.com> - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.services.nls;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import org.eclipse.e4.core.di.annotations.Optional;
import org.osgi.service.log.Logger;
import org.osgi.service.log.LoggerFactory;

/**
 * Using this MessageRegistry allows to register controls for attributes in a
 * Messages class. These controls will automatically get updated in case of
 * Locale changes.
 * <p>
 * When updating the dependencies from Java 7 to Java 8, this class can be
 * replaced by a more modern variant that makes use of functional interfaces and
 * method references as shown in the above linked blog post.
 * </p>
 *
 * <p>
 * To use the registry you need to implement a subclass of
 * <code>BaseMessageRegistry</code> that is typed to the messages class that it
 * is related to. The main thing to do is to override
 * <code>updateMessages(M)</code> while getting the messages instance injected.
 * </p>
 *
 * <pre>
 * &#064;Creatable
 * public class ExampleMessageRegistry
 * 		extends
 * 			BaseMessageRegistry&lt;ExampleMessages&gt; {
 *
 * 	&#064;Override
 * 	&#064;Inject
 * 	public void updateMessages(@Translation ExampleMessages messages) {
 * 		super.updateMessages(messages);
 * 	}
 * }
 * </pre>
 *
 * <p>
 * Note that the registry instance is annotated with &#064;Creatable so it is
 * created per requestor and is making use of DI.
 * </p>
 *
 * @param <M>
 *            the message class type
 * @since 2.0
 */
@SuppressWarnings("removal") // SecurityManager
public class BaseMessageRegistry<M> {

	private M messages;

	private final Map<MessageConsumer, MessageSupplier> bindings = new HashMap<>();

	private Logger registryLogger;
	private Logger consumerLogger;
	private Logger supplierLogger;

	@Inject
	@Optional
	void setLoggerFactory(LoggerFactory factory) {
		if (factory != null) {
			this.registryLogger = factory.getLogger(BaseMessageRegistry.class);
			this.consumerLogger = factory.getLogger(MessageConsumerImplementation.class);
			this.supplierLogger = factory.getLogger(MessageSupplierImplementation.class);
		} else {
			this.registryLogger = null;
			this.consumerLogger = null;
			this.supplierLogger = null;
		}
	}

	/**
	 * Register a consumer and a function that is acting as the supplier of the translation value.
	 * <p>
	 * This method allows to register a binding using method references and lambdas if used in an
	 * environment that already uses Java 8.
	 * </p>
	 *
	 * <pre>
	 * &#064;Inject
	 * ExampleMessageRegistry registry;
	 *
	 * Label myFirstLabel = new Label(parent, SWT.WRAP);
	 * registry.register(myFirstLabel::setText, (m) -&gt; m.firstLabelMessage);
	 * </pre>
	 *
	 * @param consumer
	 *            The consumer of the message.
	 * @param function
	 *            The function that supplies the message.
	 */
	public void register(MessageConsumer consumer, final MessageFunction<M> function) {
		register(consumer, () -> function.apply(messages));
	}

	/**
	 * Register a binding for the given consumer and supplier.
	 *
	 * <p>
	 * Unless you don't want to anonymously implement the consumer and supplier interfaces yourself,
	 * use the register methods that take the Control instance and String(s) as parameters.
	 * </p>
	 *
	 * @param consumer
	 *            The consumer of the message.
	 * @param supplier
	 *            The supplier of the message.
	 *
	 * @see BaseMessageRegistry#register(Object, String, String)
	 * @see BaseMessageRegistry#registerProperty(Object, String, String)
	 */
	public void register(MessageConsumer consumer, MessageSupplier supplier) {
		//set the value to the control
		consumer.accept(supplier.get());
		//remember the control and the supplier
		bindings.put(consumer, supplier);
	}

	/**
	 * Binds a method of an object to a message. Doing this the specified method will be called on
	 * the instance with the message String as parameter that is retrieved via message key out of
	 * the local Messages instance.
	 *
	 * @param control
	 *            The control for which a message binding should be created
	 * @param method
	 *            The method that should be bound. Methods that can be bound need to accept one
	 *            String parameter.
	 * @param messageKey
	 *            The key of the message property that should be bound
	 *
	 * @see BaseMessageRegistry#registerProperty(Object, String, String)
	 */
	public void register(final Object control, final String method, final String messageKey) {
		MessageConsumer consumer = createConsumer(control, method);
		MessageSupplier supplier = createSupplier(messageKey);
		//only register if consumer and supplier were created
		if (consumer != null && supplier != null)
			register(consumer, supplier);
	}

	/**
	 * Binds the setter of a property of an object to a message. Doing this the setter of the given
	 * property will be called on the instance with the message String as parameter that is
	 * retrieved via message key out of the local Messages instance.
	 *
	 * @param control
	 *            The control for which a message binding should be created
	 * @param property
	 *            The property of the control which should be bound
	 * @param messageKey
	 *            The key of the message property that should be bound
	 *
	 * @see BaseMessageRegistry#register(Object, String, String)
	 */
	public void registerProperty(final Object control, final String property, final String messageKey) {
		MessageConsumer consumer = createConsumer(control, "set" + Character.toUpperCase(property.charAt(0)) + property.substring(1));
		MessageSupplier supplier = createSupplier(messageKey);
		//only register if consumer and supplier were created
		if (consumer != null && supplier != null)
			register(consumer, supplier);
	}

	/**
	 * This method performs the localization update for all bound objects.
	 * <p>
	 * Typically this method is overriden by a concrete implementation where the Messages instance
	 * is injected via &#064;Inject and &#064;Translation.
	 * </p>
	 *
	 * @param messages
	 *            The new Messages instance that should be used to update the localization.
	 */
	public void updateMessages(M messages) {
		//remember the current message instance
		this.messages = messages;
		//iterate over all registered consumer
		for (Map.Entry<MessageConsumer, MessageSupplier> entry : bindings.entrySet()) {
			entry.getKey().accept(entry.getValue().get());
		}
	}

	/**
	 *
	 * @param control
	 *            The control on which the created consumer should operate
	 * @param method
	 *            The method the created consumer should call to set the new
	 *            value
	 * @return A MessageConsumer that sets a value to the property of the given
	 *         control, or {@code null} in case of any exception
	 */
	protected MessageConsumer createConsumer(final Object control, final String method) {

		try {
			final Method m = control.getClass().getMethod(method, String.class);
			if (m != null) {
				return new MessageConsumerImplementation(m, control);
			}
		} catch (NoSuchMethodException e) {
			Logger log = this.registryLogger;
			if (log != null) {
				log.warn("The method '{}' does not exist. Binding is not created!", e.getMessage());
			}
		} catch (SecurityException e) {
			Logger log = this.registryLogger;
			if (log != null) {
				log.warn(
						"Error on accessing method '{}' on class '{}' with error message '{}'. Binding is not created!",
						method, control.getClass(), e.getMessage());
			}
		}

		return null;
	}

	private final class MessageConsumerImplementation implements MessageConsumer {
		private final Method m;
		private final Object control;

		private MessageConsumerImplementation(Method m, Object control) {
			this.m = m;
			this.control = control;
		}

		@Override
		public void accept(final String value) {
			try {
				// ensure the method is accessible so the registry
				// also works well with protected or package
				// protected classes
				if (System.getSecurityManager() == null) {
					m.setAccessible(true);
					m.invoke(control, value);
				} else {
					AccessController.doPrivileged((PrivilegedAction<Object>) () -> {
						m.setAccessible(true);
						try {
							m.invoke(control, value);
						} catch (Exception e) {
							// if anything fails on invoke we unregister the
							// binding to avoid further issues e.g. this can
							// happen in case of disposed SWT controls
							bindings.remove(MessageConsumerImplementation.this);
							Logger log = consumerLogger;
							if (log != null) {
								log.info(
										"Error on invoke '{}' on '{}' with error message '{}'. Binding is removed.",
										m.getName(), control.getClass(), e.getMessage());
							}
						}
						return null;
					});
				}
			} catch (Exception e) {
				// if anything fails on invoke we unregister the binding to
				// avoid further issues
				// e.g. this can happen in case of disposed SWT controls
				bindings.remove(this);
				Logger log = consumerLogger;
				if (log != null) {
					log.info("Error on invoke '{}' on '{}' with error message '{}'. Binding is removed.",
							m.getName(), control.getClass(), e.getMessage());
				}
			}
		}
	}

	/**
	 *
	 * @param messageKey
	 *            The name of the field that should be accessed
	 * @return A MessageSupplier that returns the message value for the given message key
	 */
	protected MessageSupplier createSupplier(final String messageKey) {
		try {
			final Field f = messages.getClass().getField(messageKey);
			if (f != null) {
				return new MessageSupplierImplementation(f);
			}
		} catch (NoSuchFieldException e) {
			Logger log = this.registryLogger;
			if (log != null) {
				log.warn(
						"The class '{}' does not contain a field with name '{}'. Binding is not created!",
						this.messages.getClass().getName(), e.getMessage());
			}
		} catch (SecurityException e) {
			Logger log = this.registryLogger;
			if (log != null) {
				log.warn(
						"Error on accessing field '{}' on class '{}' with error message '{}'. Binding is not created!",
						messageKey, messages.getClass(), e.getMessage());
			}
		}
		return null;
	}

	private final class MessageSupplierImplementation implements MessageSupplier {
		private final Field f;

		private MessageSupplierImplementation(Field f) {
			this.f = f;
		}

		@Override
		public String get() {
			String message = null;
			try {
				message = (String) f.get(messages);
			} catch (Exception e) {
				// if anything fails on invoke we unregister the binding to
				// avoid further issues
				// e.g. this can happen in case of disposed SWT controls
				Iterator<Entry<MessageConsumer, MessageSupplier>> iterator = bindings.entrySet().iterator();
				iterator.forEachRemaining(entry -> {
					if (entry.getValue() == MessageSupplierImplementation.this) {
						iterator.remove();
					}
				});
				Logger log = supplierLogger;
				if (log != null) {
					log.info("Error on invoke '{}' on '{}' with error message '{}'. Binding is removed.",
							f.getName(), messages.getClass(), e.getMessage());
				}
			}
			return message;
		}
	}

	@PreDestroy
	void unregister() {
		this.bindings.clear();
	}
}
