/*******************************************************************************
 *  Copyright (c) 2021 John Dallaway and others.
 *
 *  This program and the accompanying materials
 *  are made available under the terms of the Eclipse Public License 2.0
 *  which accompanies this distribution, and is available at
 *  https://www.eclipse.org/legal/epl-2.0/
 *
 *  SPDX-License-Identifier: EPL-2.0
 *
 *  Contributors:
 *     John Dallaway - initial implementation
 *******************************************************************************/
package org.eclipse.debug.tests.view.memory;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.math.BigInteger;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.model.IMemoryBlockExtension;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.internal.ui.views.memory.renderings.AbstractIntegerRendering;
import org.eclipse.debug.internal.ui.views.memory.renderings.HexIntegerRendering;
import org.eclipse.debug.internal.ui.views.memory.renderings.RenderingsUtil;
import org.eclipse.debug.internal.ui.views.memory.renderings.SignedIntegerRendering;
import org.eclipse.debug.internal.ui.views.memory.renderings.UnsignedIntegerRendering;
import org.junit.Test;

/**
 * Tests for translation of memory bytes between in-memory representation and UI
 * presentation
 */
@SuppressWarnings("restriction")
public class TableRenderingTests {

	private static final byte[] BYTES_1 = new byte[] { (byte) 0x87 };
	private static final byte[] BYTES_2 = new byte[] { (byte) 0x98, (byte) 0x76 };
	private static final byte[] BYTES_4 = new byte[] { (byte) 0xba, (byte) 0x98, (byte) 0x76, (byte) 0x54 };
	private static final byte[] BYTES_8 = new byte[] {
			(byte) 0xfe, (byte) 0xdc, (byte) 0xba, (byte) 0x98,
			(byte) 0x76, (byte) 0x54, (byte) 0x32, (byte) 0x10 };
	private static final byte[] BYTES_16 = new byte[16];
	private static final byte[] BYTES_24 = new byte[24];

	static {
		BYTES_16[0] = (byte) 0x80; // 2 ^ 127
		BYTES_24[0] = (byte) 0x80; // 2 ^ 191
	}

	@Test
	public void testHexIntegerRendering() throws DebugException {
		testIntegerRendering(createHexIntegerRendering(1), BYTES_1, "87", "87"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(1), BYTES_2, "9876", "7698"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(2), BYTES_2, "9876", "9876"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(1), BYTES_4, "BA987654", "547698BA"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(2), BYTES_4, "BA987654", "7654BA98"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(4), BYTES_4, "BA987654", "BA987654"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(1), BYTES_8, "FEDCBA9876543210", "1032547698BADCFE"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(4), BYTES_8, "FEDCBA9876543210", "76543210FEDCBA98"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(4), BYTES_16, "80000000000000000000000000000000", "00000000000000000000000080000000"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createHexIntegerRendering(4), BYTES_24, "800000000000000000000000000000000000000000000000", "000000000000000000000000000000000000000080000000"); //$NON-NLS-1$ //$NON-NLS-2$
	}

	@Test
	public void testSignedIntegerRendering() throws DebugException {
		testIntegerRendering(createSignedIntegerRendering(1, BYTES_1.length), BYTES_1, "-121", "-121"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(1, BYTES_2.length), BYTES_2, "-26506", "30360"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(2, BYTES_2.length), BYTES_2, "-26506", "-26506"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(1, BYTES_4.length), BYTES_4, "-1164413356", "1417058490"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(2, BYTES_4.length), BYTES_4, "-1164413356", "1985264280"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(4, BYTES_4.length), BYTES_4, "-1164413356", "-1164413356"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(1, BYTES_8.length), BYTES_8, "-81985529216486896", "1167088121787636990"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(4, BYTES_8.length), BYTES_8, "-81985529216486896", "8526495043095935640"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(8, BYTES_8.length), BYTES_8, "-81985529216486896", "-81985529216486896"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createSignedIntegerRendering(2, BYTES_16.length), BYTES_16, BigInteger.valueOf(2).pow(127).negate().toString(), "32768"); //$NON-NLS-1$
		testIntegerRendering(createSignedIntegerRendering(2, BYTES_24.length), BYTES_24, BigInteger.valueOf(2).pow(191).negate().toString(), "32768"); //$NON-NLS-1$
	}

	@Test
	public void testUnsignedIntegerRendering() throws DebugException {
		testIntegerRendering(createUnsignedIntegerRendering(1, BYTES_1.length), BYTES_1, "135", "135"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(1, BYTES_2.length), BYTES_2, "39030", "30360"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(2, BYTES_2.length), BYTES_2, "39030", "39030"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(1, BYTES_4.length), BYTES_4, "3130553940", "1417058490"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(2, BYTES_4.length), BYTES_4, "3130553940", "1985264280"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(4, BYTES_4.length), BYTES_4, "3130553940", "3130553940"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(1, BYTES_8.length), BYTES_8, "18364758544493064720", "1167088121787636990"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(4, BYTES_8.length), BYTES_8, "18364758544493064720", "8526495043095935640"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(8, BYTES_8.length), BYTES_8, "18364758544493064720", "18364758544493064720"); //$NON-NLS-1$ //$NON-NLS-2$
		testIntegerRendering(createUnsignedIntegerRendering(2, BYTES_16.length), BYTES_16, BigInteger.valueOf(2).pow(127).toString(), "32768"); //$NON-NLS-1$
		testIntegerRendering(createUnsignedIntegerRendering(2, BYTES_24.length), BYTES_24, BigInteger.valueOf(2).pow(191).toString(), "32768"); //$NON-NLS-1$
	}

	private void testIntegerRendering(AbstractIntegerRendering rendering, byte[] bytes, String bigEndianString, String littleEndianString) throws DebugException {
		final IMemoryBlockExtension block = new TableRenderingTestsMemoryBlock(bytes, rendering.getAddressableSize());
		rendering.init(null, block);
		final MemoryByte[] memoryBytes = block.getBytesFromOffset(BigInteger.ZERO, bytes.length / rendering.getAddressableSize());

		rendering.setDisplayEndianess(RenderingsUtil.BIG_ENDIAN);
		assertEquals(bigEndianString, rendering.getString(null, null, memoryBytes));
		assertArrayEquals(bytes, rendering.getBytes(null, null, memoryBytes, bigEndianString));

		rendering.setDisplayEndianess(RenderingsUtil.LITTLE_ENDIAN);
		assertEquals(littleEndianString, rendering.getString(null, null, memoryBytes));
		assertArrayEquals(bytes, rendering.getBytes(null, null, memoryBytes, littleEndianString));
	}

	private HexIntegerRendering createHexIntegerRendering(int addressableSize) {
		return new HexIntegerRendering(null) {
			@Override
			public int getAddressableSize() {
				return addressableSize;
			}
		};
	}

	private SignedIntegerRendering createSignedIntegerRendering(int addressableSize, int bytesPerColumn) {
		return new SignedIntegerRendering(null) {
			@Override
			public int getAddressableSize() {
				return addressableSize;
			}

			@Override
			public int getBytesPerColumn() {
				return bytesPerColumn;
			}
		};
	}

	private UnsignedIntegerRendering createUnsignedIntegerRendering(int addressableSize, int bytesPerColumn) {
		return new UnsignedIntegerRendering(null) {
			@Override
			public int getAddressableSize() {
				return addressableSize;
			}

			@Override
			public int getBytesPerColumn() {
				return bytesPerColumn;
			}
		};
	}

}
