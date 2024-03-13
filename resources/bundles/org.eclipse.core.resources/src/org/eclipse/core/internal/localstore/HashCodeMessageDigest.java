/*******************************************************************************
 * Copyright (c) 2024 Christoph Läubrich and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Christoph Läubrich - initial API and implementation
 *******************************************************************************/
package org.eclipse.core.internal.localstore;

import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;

/**
 * A message digest that computes the hash value of a stream like
 * {@link Arrays#hashCode()}
 *
 * @since 3.2
 *
 */
class HashCodeMessageDigest extends MessageDigest {

	int result = 1;
	int bytes = 0;

	public HashCodeMessageDigest() {
		super("Arrays.hashCode"); //$NON-NLS-1$
	}

	@Override
	protected void engineUpdate(byte element) {
		result = 31 * result + element;
		bytes++;
	}

	@Override
	protected void engineUpdate(byte[] input, int offset, int len) {
		for (int i = offset; i < offset + len; i++) {
			result = 31 * result + input[i];
		}
		bytes += len;
	}

	@Override
	protected byte[] engineDigest() {
		byte[] array = ByteBuffer.allocate(4).putInt(result).array();
		engineReset();
		return array;
	}

	@Override
	protected void engineReset() {
		result = 1;
		bytes = 0;
	}

}
