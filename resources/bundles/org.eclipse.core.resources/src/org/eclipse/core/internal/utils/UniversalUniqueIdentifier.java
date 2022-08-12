/*******************************************************************************
 * Copyright (c) 2000, 2019 IBM Corporation and others.
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
 *     Mickael Istria (Red Hat Inc.) - Bug 488938, 488937
 *******************************************************************************/
package org.eclipse.core.internal.utils;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.GregorianCalendar;
import java.util.Random;

public class UniversalUniqueIdentifier implements java.io.Serializable {

	/**
	 * All serializable objects should have a stable serialVersionUID
	 */
	private static final long serialVersionUID = 1L;

	/* INSTANCE FIELDS =============================================== */

	private byte[] fBits = new byte[BYTES_SIZE];

	/* NON-FINAL PRIVATE STATIC FIELDS =============================== */

	private static BigInteger fgPreviousClockValue;
	private static int fgClockAdjustment = 0;
	private static int fgClockSequence = -1;
	private static byte[] nodeAddress;

	static {
		nodeAddress = computeNodeAddress();
	}

	/* PRIVATE STATIC FINAL FIELDS =================================== */

	private static Random fgRandomNumberGenerator = new Random();

	/* PUBLIC STATIC FINAL FIELDS ==================================== */

	public static final int BYTES_SIZE = 16;
	public static final byte[] UNDEFINED_UUID_BYTES = new byte[16];
	public static final int MAX_CLOCK_SEQUENCE = 0x4000;
	public static final int MAX_CLOCK_ADJUSTMENT = 0x7FFF;
	public static final int TIME_FIELD_START = 0;
	public static final int TIME_FIELD_STOP = 6;
	public static final int TIME_HIGH_AND_VERSION = 7;
	public static final int CLOCK_SEQUENCE_HIGH_AND_RESERVED = 8;
	public static final int CLOCK_SEQUENCE_LOW = 9;
	public static final int NODE_ADDRESS_START = 10;
	public static final int NODE_ADDRESS_BYTE_SIZE = 6;

	public static final int BYTE_MASK = 0xFF;

	public static final int HIGH_NIBBLE_MASK = 0xF0;

	public static final int LOW_NIBBLE_MASK = 0x0F;

	public static final int SHIFT_NIBBLE = 4;

	public static final int ShiftByte = 8;

	/**
	 UniversalUniqueIdentifier default constructor returns a
	 new instance that has been initialized to a unique value.
	 */
	public UniversalUniqueIdentifier() {
		this.setVersion(1);
		this.setVariant(1);
		this.setTimeValues();
		this.setNode(getNodeAddress());
	}

	/**
	 Constructor that accepts the bytes to use for the instance.&nbsp;&nbsp; The format
	 of the byte array is compatible with the <code>toBytes()</code> method.

	 <p>The constructor returns the undefined uuid if the byte array is invalid.

	 @see #toBytes()
	 @see #BYTES_SIZE
	 */
	public UniversalUniqueIdentifier(byte[] byteValue) {
		fBits = new byte[BYTES_SIZE];
		if (byteValue.length >= BYTES_SIZE)
			System.arraycopy(byteValue, 0, fBits, 0, BYTES_SIZE);
	}

	private void appendByteString(StringBuilder buffer, byte value) {
		String hexString;

		if (value < 0)
			hexString = Integer.toHexString(256 + value);
		else
			hexString = Integer.toHexString(value);
		if (hexString.length() == 1)
			buffer.append("0"); //$NON-NLS-1$
		buffer.append(hexString);
	}

	private static BigInteger clockValueNow() {
		GregorianCalendar now = new GregorianCalendar();
		BigInteger nowMillis = BigInteger.valueOf(now.getTime().getTime());
		BigInteger baseMillis = BigInteger.valueOf(now.getGregorianChange().getTime());

		return (nowMillis.subtract(baseMillis).multiply(BigInteger.valueOf(10000L)));
	}

	public static int compareTime(byte[] fBits1, byte[] fBits2) {
		for (int i = TIME_FIELD_STOP; i >= 0; i--)
			if (fBits1[i] != fBits2[i])
				return (0xFF & fBits1[i]) - (0xFF & fBits2[i]);
		return 0;
	}

	/**
	 * Answers the node address attempting to mask the IP
	 * address of this machine.
	 *
	 * @return byte[] the node address
	 */
	private static byte[] computeNodeAddress() {

		byte[] address = new byte[NODE_ADDRESS_BYTE_SIZE];
		SecureRandom randomizer = new SecureRandom();
		randomizer.nextBytes(address);

		// set the MSB of the first octet to 1 to distinguish from IEEE node addresses
		address[0] = (byte) (address[0] | (byte) 0x80);

		return address;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (!(obj instanceof UniversalUniqueIdentifier))
			return false;

		byte[] other = ((UniversalUniqueIdentifier) obj).fBits;
		if (fBits == other)
			return true;
		if (fBits.length != other.length)
			return false;
		for (int i = 0; i < fBits.length; i++) {
			if (fBits[i] != other[i])
				return false;
		}
		return true;
	}

	private static byte[] getNodeAddress() {
		return nodeAddress;
	}

	@Override
	public int hashCode() {
		return fBits[0] + fBits[3] + fBits[7] + fBits[11] + fBits[15];
	}

	private static int nextClockSequence() {

		if (fgClockSequence == -1)
			fgClockSequence = (int) (fgRandomNumberGenerator.nextDouble() * MAX_CLOCK_SEQUENCE);

		fgClockSequence = (fgClockSequence + 1) % MAX_CLOCK_SEQUENCE;

		return fgClockSequence;
	}

	private static BigInteger nextTimestamp() {

		BigInteger timestamp = clockValueNow();
		int timestampComparison;

		timestampComparison = timestamp.compareTo(fgPreviousClockValue);

		if (timestampComparison == 0) {
			if (fgClockAdjustment == MAX_CLOCK_ADJUSTMENT) {
				while (timestamp.compareTo(fgPreviousClockValue) == 0)
					timestamp = clockValueNow();
				timestamp = nextTimestamp();
			} else
				fgClockAdjustment++;
		} else {
			fgClockAdjustment = 0;

			if (timestampComparison < 0)
				nextClockSequence();
		}

		return timestamp;
	}

	private void setClockSequence(int clockSeq) {
		int clockSeqHigh = (clockSeq >>> ShiftByte) & LOW_NIBBLE_MASK;
		int reserved = fBits[CLOCK_SEQUENCE_HIGH_AND_RESERVED] & HIGH_NIBBLE_MASK;

		fBits[CLOCK_SEQUENCE_HIGH_AND_RESERVED] = (byte) (reserved | clockSeqHigh);
		fBits[CLOCK_SEQUENCE_LOW] = (byte) (clockSeq & BYTE_MASK);
	}

	protected void setNode(byte[] bytes) {

		System.arraycopy(bytes, 0, fBits, NODE_ADDRESS_START, NODE_ADDRESS_BYTE_SIZE);
	}

	private void setTimestamp(BigInteger timestamp) {
		BigInteger value = timestamp;
		BigInteger bigByte = BigInteger.valueOf(256L);
		BigInteger[] results;
		int version;
		int timeHigh;

		for (int index = TIME_FIELD_START; index < TIME_FIELD_STOP; index++) {
			results = value.divideAndRemainder(bigByte);
			value = results[0];
			fBits[index] = (byte) results[1].intValue();
		}
		version = fBits[TIME_HIGH_AND_VERSION] & HIGH_NIBBLE_MASK;
		timeHigh = value.intValue() & LOW_NIBBLE_MASK;
		fBits[TIME_HIGH_AND_VERSION] = (byte) (timeHigh | version);
	}

	protected synchronized void setTimeValues() {
		this.setTimestamp(timestamp());
		this.setClockSequence(fgClockSequence);
	}

	protected int setVariant(int variantIdentifier) {
		int clockSeqHigh = fBits[CLOCK_SEQUENCE_HIGH_AND_RESERVED] & LOW_NIBBLE_MASK;
		int variant = variantIdentifier & LOW_NIBBLE_MASK;

		fBits[CLOCK_SEQUENCE_HIGH_AND_RESERVED] = (byte) ((variant << SHIFT_NIBBLE) | clockSeqHigh);
		return (variant);
	}

	protected void setVersion(int versionIdentifier) {
		int timeHigh = fBits[TIME_HIGH_AND_VERSION] & LOW_NIBBLE_MASK;
		int version = versionIdentifier & LOW_NIBBLE_MASK;

		fBits[TIME_HIGH_AND_VERSION] = (byte) (timeHigh | (version << SHIFT_NIBBLE));
	}

	private static BigInteger timestamp() {
		BigInteger timestamp;

		if (fgPreviousClockValue == null) {
			fgClockAdjustment = 0;
			nextClockSequence();
			timestamp = clockValueNow();
		} else
			timestamp = nextTimestamp();

		fgPreviousClockValue = timestamp;
		return fgClockAdjustment == 0 ? timestamp : timestamp.add(BigInteger.valueOf(fgClockAdjustment));
	}

	/**
	 This representation is compatible with the (byte[]) constructor.

	 @see #UniversalUniqueIdentifier(byte[])
	 */
	public byte[] toBytes() {
		byte[] result = new byte[fBits.length];

		System.arraycopy(fBits, 0, result, 0, fBits.length);
		return result;
	}

	@Override
	public String toString() {
		StringBuilder buffer = new StringBuilder();
		for (byte bit : fBits)
			appendByteString(buffer, bit);
		return buffer.toString();
	}

	public String toStringAsBytes() {
		StringBuilder result = new StringBuilder("{"); //$NON-NLS-1$

		for (int i = 0; i < fBits.length; i++) {
			result.append(fBits[i]);
			if (i < fBits.length + 1)
				result.append(',');
		}
		result.append('}');
		return result.toString();
	}
}
