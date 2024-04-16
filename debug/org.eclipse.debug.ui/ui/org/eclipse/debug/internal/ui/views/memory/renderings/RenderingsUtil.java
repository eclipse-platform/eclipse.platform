/*******************************************************************************
 * Copyright (c) 2004, 2021 IBM Corporation and others.
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
 *     John Dallaway - Accommodate addressableSize != 1 (bug 577106)
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory.renderings;

import java.math.BigInteger;


/**
 * Util functions for data conversions
 */
public class RenderingsUtil {

	public static final int LITTLE_ENDIAN = 0;
	public static final int BIG_ENDIAN = 1;
	public static final int ENDIANESS_UNKNOWN = 2;

	/**
	 * Pad byte array with zero's with the byte array's length
	 * is shorter that what's expected the conversion functions.
	 * @return an array of bytes
	 */
	protected static byte[] fillArray(byte[] array, int size, int endianess)
	{
		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			byte[] temp = new byte[size];

			for (int i=0; i<array.length; i++)
			{
				temp[i] = array[i];
			}

			// fill up the rest of the array
			for (int i=array.length; i<size; i++)
			{
				temp[i] = 0;
			}

			array = temp;
			return array;
		}
		byte[] temp = new byte[size];

		for (int i=0; i<size - array.length; i++)
		{
			temp[i] = 0;
		}

		int j=0;
		// fill up the rest of the array
		for (int i=size - array.length; i<size; i++)
		{
			temp[i] = array[j];
			j++;
		}

		array = temp;
		return array;
	}

	static public BigInteger convertByteArrayToUnsignedLong(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 8)
		{
			array = fillArray(array, 8, endianess);
		}

		BigInteger value = BigInteger.ZERO;
		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int i = 0; i < 8; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					byte[] temp = new byte[1];
					temp[0] = array[i + j];
					BigInteger b = new BigInteger(temp);
					b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
					b = b.shiftLeft((i + addressableSize - j - 1) * 8);
					value = value.or(b);
				}
			}
		}
		else
		{
			for (int i=0; i< 8; i++)
			{
				byte[] temp = new byte[1];
				temp[0] = array[i];
				BigInteger b = new BigInteger(temp);
				b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
				b = b.shiftLeft((7-i)*8);
				value = value.or(b);
			}
		}
		return value;
	}

	/**
	 * Convert byte array to long.
	 * @return result of the conversion in long
	 */
	static public long convertByteArrayToLong(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 8)
		{
			array = fillArray(array, 8, endianess);
		}

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			long value = 0;
			for (int i = 0; i < 8; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					long b = array[i + j];
					b &= 0xff;
					value |= (b << ((i + addressableSize - j - 1) * 8));
				}
			}
			return value;
		}
		long value = 0;
		for (int i=0; i< 8; i++)
		{
			long b = array[i];
			b &= 0xff;
			value |= (b<<((7-i)*8));
		}

		return value;
	}

	static public BigInteger convertByteArrayToSignedBigInt(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 16)
		{
			array = fillArray(array, 16, endianess);
		}

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			// reverse bytes
			byte[] holder = new byte[16];
			for (int i = 0; i < 16; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					holder[i + j] = array[16 + j - i - addressableSize];
				}
			}

			// create BigInteger
			BigInteger value = new BigInteger(holder);
			return value;
		}
		BigInteger value = new BigInteger(array);
		return value;
	}

	static public BigInteger convertByteArrayToSignedBigInt(byte[] array, int endianess, int arraySize, int addressableSize)
	{
		if (array.length < arraySize)
		{
			array = fillArray(array, arraySize, endianess);
		}

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			// reverse bytes
			byte[] holder = new byte[arraySize];
			for (int i = 0; i < arraySize; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					holder[i + j] = array[arraySize + j - i - addressableSize];
				}
			}

			// create BigInteger
			BigInteger value = new BigInteger(holder);
			return value;
		}
		BigInteger value = new BigInteger(array);
		return value;
	}

	static public BigInteger convertByteArrayToUnsignedBigInt(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 16)
		{
			array = fillArray(array, 16, endianess);
		}

		BigInteger value = BigInteger.ZERO;
		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int i = 0; i < 16; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					byte[] temp = new byte[1];
					temp[0] = array[i + j];
					BigInteger b = new BigInteger(temp);
					b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
					b = b.shiftLeft((i + addressableSize - j - 1) * 8);
					value = value.or(b);
				}
			}
		}
		else
		{
			for (int i=0; i< 16; i++)
			{
				byte[] temp = new byte[1];
				temp[0] = array[i];
				BigInteger b = new BigInteger(temp);
				b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
				b = b.shiftLeft((15-i)*8);
				value = value.or(b);
			}
		}
		return value;
	}

	static public BigInteger convertByteArrayToUnsignedBigInt(byte[] array, int endianess, int arraySize, int addressableSize)
	{
		if (array.length < arraySize)
		{
			array = fillArray(array, arraySize, endianess);
		}

		BigInteger value = BigInteger.ZERO;
		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int i = 0; i < arraySize; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					byte[] temp = new byte[1];
					temp[0] = array[i + j];
					BigInteger b = new BigInteger(temp);
					b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
					b = b.shiftLeft((i + addressableSize - j - 1) * 8);
					value = value.or(b);
				}
			}
		}
		else
		{
			for (int i=0; i< arraySize; i++)
			{
				byte[] temp = new byte[1];
				temp[0] = array[i];
				BigInteger b = new BigInteger(temp);
				b = b.and(new BigInteger("ff", 16)); //$NON-NLS-1$
				b = b.shiftLeft((arraySize-1-i)*8);
				value = value.or(b);
			}
		}
		return value;
	}

	/**
	 * Convert byte array to integer.
	 * @return result of the conversion in int
	 */
	static public int convertByteArrayToInt(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 4)
		{
			array = fillArray(array, 4, endianess);
		}

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			int value = 0;
			for (int i = 0; i < 4; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					int b = array[i + j];
					b &= 0xff;
					value |= (b << ((i + addressableSize - j - 1) * 8));
				}
			}
			return value;
		}
		int value = 0;
		for (int i=0; i< 4; i++)
		{
			int b = array[i];
			b &= 0xff;
			value |= (b<<((3-i)*8));
		}

		return value;
	}

	/**
	 * Convert byte array to short.
	 * @return result of teh conversion in short
	 */
	static public short convertByteArrayToShort(byte[] array, int endianess, int addressableSize)
	{
		if (array.length < 2)
		{
			array = fillArray(array, 2, endianess);
		}

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			short value = 0;
			for (int i = 0; i < 2; i += addressableSize) {
				for (int j = 0; j < addressableSize; j++) {
					short b = array[i + j];
					b &= 0xff;
					value |= (b << ((i + addressableSize - j - 1) * 8));
				}
			}
			return value;
		}
		short value = 0;
		for (int i=0; i< 2; i++)
		{
			short b = array[i];
			b &= 0xff;
			value |= (b<<((1-i)*8));
		}
		return value;
	}

	/**
	 * Convert big integer to byte array.
	 * @return result of the conversion in raw byte array
	 */
	static public byte[] convertBigIntegerToByteArray(BigInteger i, int endianess, int addressableSize)
	{
		byte buf[]=new byte[16];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < 16; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					BigInteger x = i.shiftRight((j + addressableSize - k - 1) * 8);
					buf[j + k] = x.byteValue();
				}
			}
			return buf;
		}
		for (int j=15; j>=0; j--)
		{
			BigInteger x = i.shiftRight((15-j)*8);
			buf[j] = x.byteValue();
		}
		return buf;
	}

	static public byte[] convertSignedBigIntToByteArray(BigInteger i, int endianess, int arraySize, int addressableSize)
	{
		byte buf[]=new byte[arraySize];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < arraySize; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					BigInteger x = i.shiftRight((j + addressableSize - k - 1) * 8);
					buf[j + k] = x.byteValue();
				}
			}
			return buf;
		}
		for (int j=arraySize-1; j>=0; j--)
		{
			BigInteger x = i.shiftRight((arraySize-1-j)*8);
			buf[j] = x.byteValue();
		}
		return buf;
	}

	/**
	 * Convert big integer to byte array.
	 * @return result of the conversion in raw byte array
	 */
	static public byte[] convertUnsignedBigIntegerToByteArray(BigInteger i, int endianess, int addressableSize)
	{
		byte buf[]=new byte[32];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < 32; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					BigInteger x = i.shiftRight((j + addressableSize - k - 1) * 8);
					buf[j + k] = x.byteValue();
				}
			}
			return buf;
		}
		for (int j=31; j>=0; j--)
		{
			BigInteger x = i.shiftRight((31-j)*8);
			buf[j] = x.byteValue();
		}
		return buf;
	}

	static public byte[] convertUnsignedBigIntToByteArray(BigInteger i, int endianess, int arraySize, int addressableSize)
	{
		byte buf[]=new byte[arraySize*2];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < arraySize * 2; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					BigInteger x = i.shiftRight((j + addressableSize - k - 1) * 8);
					buf[j + k] = x.byteValue();
				}
			}
			return buf;
		}
		for (int j=(arraySize*2)-1; j>=0; j--)
		{
			BigInteger x = i.shiftRight(((arraySize*2)-1-j)*8);
			buf[j] = x.byteValue();
		}
		return buf;
	}

	/**
	 * Convert long to byte array.
	 * @return result of the conversion in raw byte array
	 */
	static public byte[] convertLongToByteArray(long i, int endianess, int addressableSize)
	{
		byte buf[]=new byte[8];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < 8; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					buf[j + k] = Long.valueOf(i >> (j + addressableSize - k - 1) * 8).byteValue();
				}
			}
			return buf;
		}
		for (int j=7; j>=0; j--)
		{
			buf[j] = Long.valueOf(i>>(7-j)*8).byteValue();
		}
		return buf;
	}

	/**
	 * Convert integer to byte array.
	 * @return result of the conversion in raw byte array
	 */
	static public byte[] convertIntToByteArray(int i, int endianess, int addressableSize)
	{
		byte buf[]=new byte[4];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (int j = 0; j < 4; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					buf[j + k] = Integer.valueOf(i >> (j + addressableSize - k - 1) * 8).byteValue();
				}
			}
			return buf;
		}
		for (int j=3; j>=0; j--)
		{
			buf[j] = Integer.valueOf(i>>(3-j)*8).byteValue();
		}
		return buf;
	}

	/**
	 * Convert short to byte array.
	 * @return result of the conversion in raw byte array
	 */
	static public byte[] convertShortToByteArray(short i, int endianess, int addressableSize)
	{
		byte buf[]=new byte[2];

		if (endianess == RenderingsUtil.LITTLE_ENDIAN)
		{
			for (short j = 0; j < 2; j += addressableSize) {
				for (int k = 0; k < addressableSize; k++) {
					buf[j + k] = Integer.valueOf(i >> (j + addressableSize - k - 1) * 8).byteValue();
				}
			}
			return buf;
		}
		for (short j=1; j>=0; j--)
		{
			buf[j] = Integer.valueOf(i>>(1-j)*8).byteValue();
		}
		return buf;
	}

	/**
	 * byte array to Hex string helper
	 * replaces the Integer.toHexString() which can't convert byte values properly
	 * (always pads with FFFFFF)
	 */
	static public String convertByteArrayToHexString(byte[] byteArray)
	{
		StringBuilder strBuffer = new StringBuilder();
		char charArray[];

		for (byte element : byteArray) {
			charArray = RenderingsUtil.convertByteToCharArray(element);
			strBuffer.append(charArray);
		}

		return strBuffer.toString();
	}

	static public char[] convertByteToCharArray(byte aByte)
	{
		char charArray[] = new char[2];
		int val = aByte;
		if (val<0) {
			val += 256;
		}
		charArray[0] = Character.forDigit(val/16, 16);
		charArray[1] = Character.forDigit(val%16, 16);

		return charArray;
	}

	/**
	 * Convert raw memory data to byte array
	 * @param numCharsPerByte - number of characters per byte of data
	 * @return an array of byte, converted from a hex string
	 */
	public static byte[] convertHexStringToByteArray(String str, int numBytes, int numCharsPerByte) throws NumberFormatException
	{
		if (str.length() == 0) {
			return null;
		}

		StringBuilder buf = new StringBuilder(str);

		// pad string with zeros
		int requiredPadding =  numBytes * numCharsPerByte - str.length();
		while (requiredPadding > 0) {
			buf.insert(0, "0"); //$NON-NLS-1$
			requiredPadding--;
		}

		byte[] bytes = new byte[numBytes];
		str = buf.toString();

		// set data in memory
		for (int i=0; i<bytes.length; i++)
		{
			// convert string to byte
			String oneByte = str.substring(i*2, i*2+2);
			bytes[i] = (byte) Integer.parseInt(oneByte, 16);
		}

		return bytes;
	}
}
