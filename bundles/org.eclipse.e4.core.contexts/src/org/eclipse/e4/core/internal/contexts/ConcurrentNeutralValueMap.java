/*******************************************************************************
 * Copyright (c) 2021 Joerg Kubitz.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Joerg Kubitz              - initial API and implementation
 *******************************************************************************/
package org.eclipse.e4.core.internal.contexts;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiConsumer;

/**
 * A wrapper around ConcurrentHashMap which allows null values.
 *
 * For null values a neutral Element is internally used.
 *
 * Faster then a synchronized Map.
 */
public class ConcurrentNeutralValueMap<K, V> {// implements subset of Map<K, V>
	final ConcurrentHashMap<K, V> delegate = new ConcurrentHashMap<>();
	/** a value that is used for null elements **/
	final private V neutralValue;
	final private static NullValue NULL = new NullValue();

	/**
	 * @param neutralValue a Element that does not equal any other Value
	 * @see #neutralObject()
	 */
	public ConcurrentNeutralValueMap(V neutralValue) {
		this.neutralValue = neutralValue;
	}

	@SuppressWarnings("unchecked")
	public ConcurrentNeutralValueMap() {
		this((V) NULL);
	}

	private V wrapValue(V value) {
		return value == null ? neutralValue : value;
	}

	private V unwrapValue(V v) {
		return v == neutralValue ? null : v;
	}

	public V get(Object key) {
		return unwrapValue(delegate.get(key));
	}

	private static final class NullValue {
		/** for {@link ConcurrentNeutralValueMap#toString()} **/
		@Override
		public String toString() {
			return "null"; //$NON-NLS-1$
		}

		@Override
		public boolean equals(Object obj) {
			// currently not called since ConcurrentHashMap checks for value1==value2
			// but just in case the Implementation changes:
			// in means of ConcurrentNeutralValueMap#equals() every NullValue equals
			return obj instanceof NullValue;
			// like java.util.Objects.equals(null, null) would return true,
		}

		/** for {@link ConcurrentNeutralValueMap#hashCode()} **/
		@Override
		public int hashCode() {
			// in means of ConcurrentNeutralValueMap#hashCode() we need a hashCode
			// even tough null.hashCode() would normally throw NullPointerException.
			return 0x55555555;
			// like java.util.Objects.hashCode(null) would return an int,
		}
	}

	/*
	 * Method overloads from java.lang.Object:
	 */

	/** return uses hashCode() of the neutralValue **/
	@Override
	public int hashCode() {
		return delegate.hashCode();
	}

	/** return does NOT use equals() of the neutralValue **/
	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof ConcurrentNeutralValueMap))
			return false;
		return delegate.equals(((ConcurrentNeutralValueMap<?, ?>) obj).delegate);
	}

	/** return uses toString() of the neutralValue **/
	@Override
	public String toString() {
		return delegate.toString();
	}

	/*
	 * Method overloads from java.util.Map:
	 */

	/**
	 * Like {@link java.util.Map#put} but we do not return a value.
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see ConcurrentHashMap#put(Object, Object)
	 * @see #putAndGetOld(Object, Object)
	 **/
	public void put(K key, V value) {
		delegate.put(key, wrapValue(value));
	}

	/**
	 * Like {@link java.util.Map#put} but we do not return a wrapped value.
	 *
	 * @see java.util.Map#put(Object, Object)
	 * @see Value
	 **/
	public Value<V> putAndGetOld(K key, V value) {
		return new Wrapped(delegate.put(key, wrapValue(value)));
	}

	/**
	 * @see java.util.Map#remove(Object)
	 * @see ConcurrentHashMap#remove(Object)
	 **/
	public V remove(Object key) {
		return unwrapValue(delegate.remove(key));
	}

	/**
	 * @see java.util.Map#size()
	 **/
	public int size() {
		return delegate.size();
	}

	/**
	 * @see java.util.Map#isEmpty()
	 **/
	public boolean isEmpty() {
		return delegate.isEmpty();
	}

	/**
	 * @see java.util.Map#clear()
	 **/
	public void clear() {
		delegate.clear();
	}

	/**
	 * Use is discouraged as the outcome might change concurrently
	 *
	 * @see java.util.Map#containsKey(Object)
	 * @see ConcurrentHashMap#containsKey(Object)
	 **/
	public boolean containsKey(Object key) {
		return delegate.containsKey(key);
	}

	/*
	 * Some helpful methods from ConcurrentHashMap which do not return V:
	 */

	/** @see ConcurrentHashMap#forEach(BiConsumer) **/
	public void forEach(BiConsumer<? super K, ? super V> action) {
		delegate.forEach( //
				(k, v) -> action.accept(k, unwrapValue(v) //
				));
	}

	/*
	 * Some methods from ConcurrentHashMap which return V. returning null is
	 * discouraged because it is not clear if that was a value or not:
	 */

	/**
	 * Like {@link ConcurrentHashMap#putIfAbsent(Object, Object)} but we do not
	 * return anything.
	 *
	 * @see ConcurrentHashMap#putIfAbsent(Object, Object)
	 **/
	public void putIfAbsent(K key, V value) {
		delegate.putIfAbsent(key, wrapValue(value));
	}

	public interface Value<V> {
		/**
		 * @return the result of map.contains(key)
		 */
		boolean isPresent();

		/**
		 * Callers are supposed to check #isPresent first to determine whether a
		 * returned null was a null value or the key was not present.
		 *
		 * @return the unwrapped result of map.get(key). May return null.
		 */
		V unwrapped();
	}

	private final class Wrapped implements Value<V> {
		V wrappedValue;

		private Wrapped(V wrappedValue) {
			this.wrappedValue = wrappedValue;
		}

		@Override
		public boolean isPresent() {
			return wrappedValue != null;
		}

		@Override
		public V unwrapped() {
			return unwrapValue(wrappedValue);
		}
	}

	/**
	 * @return a value that is wrapped into a Value Object.
	 * @see Value#isPresent()
	 * @see Value#unwrapped()
	 */
	public Value<V> getValue(K key) {
		return new Wrapped(delegate.get(key));
	}

}
