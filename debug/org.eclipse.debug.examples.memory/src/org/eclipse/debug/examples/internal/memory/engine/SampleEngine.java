/*******************************************************************************
 * Copyright (c) 2013, 2018 IBM Corporation and others.
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

package org.eclipse.debug.examples.internal.memory.engine;

import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Random;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.MemoryByte;
import org.eclipse.debug.examples.internal.memory.core.SampleDebugTarget;
import org.eclipse.debug.examples.internal.memory.core.SampleMemoryBlock;
import org.eclipse.debug.examples.internal.memory.core.SampleStackFrame;
import org.eclipse.debug.examples.internal.memory.core.SampleThread;

/**
 * Sample engine for sample deug adapter This engine randomly generates content
 * for a memory block. To get to this engine, call
 * {@link SampleDebugTarget#getEngine()};
 */
public class SampleEngine {

	Random fRandom = new Random();
	byte[] fMemory;
	Hashtable<BigInteger, SampleMemoryUnit> memoryBlockTable;
	Hashtable<String, BigInteger> expressionAddressTable = new Hashtable<>();
	Hashtable<SampleDebugTarget, Object> threadTable = new Hashtable<>();
	Hashtable<SampleThread, Object> stackframeTable = new Hashtable<>();

	Random random = new Random();

	/**
	 * Allow debug adapters to get memory from an address
	 *
	 * @param address
	 * @param length
	 * @return memory byte from an address
	 * @throws RuntimeException
	 */
	synchronized public MemoryByte[] getBytesFromAddress(BigInteger address, long length) throws RuntimeException {

		if (memoryBlockTable == null) {
			// create new memoryBlock table
			memoryBlockTable = new Hashtable<>();
			byte[] bytes = new byte[(int) length * getAddressableSize()];
			BigInteger addressKey = address;

			random.nextBytes(bytes);

			for (int i = 0; i < bytes.length; i = i + getAddressableSize()) {
				addressKey = addressKey.add(BigInteger.valueOf(1));

				MemoryByte[] byteUnit = new MemoryByte[getAddressableSize()];
				for (int j = 0; j < getAddressableSize(); j++) {
					MemoryByte oneByte = new MemoryByte(bytes[i + j]);
					oneByte.setBigEndian(isBigEndian(addressKey));
					oneByte.setWritable(isWritable(addressKey));
					oneByte.setReadable(isReadable(addressKey));
					byteUnit[j] = oneByte;
				}
				SampleMemoryUnit unit = new SampleMemoryUnit(byteUnit);
				memoryBlockTable.put(addressKey, unit);
			}
		}

		MemoryByte[] returnBytes = new MemoryByte[(int) length * getAddressableSize()];
		BigInteger addressKey;

		for (int i = 0; i < returnBytes.length; i = i + getAddressableSize()) {
			addressKey = address.add(BigInteger.valueOf(i / getAddressableSize()));
			SampleMemoryUnit temp = (memoryBlockTable.get(addressKey));

			// if memoryBlock does not already exist in the table, generate a
			// value
			if (temp == null) {
				byte[] x = new byte[getAddressableSize()];
				random.nextBytes(x);
				byte flag = 0;
				flag |= MemoryByte.READABLE;
				flag |= MemoryByte.ENDIANESS_KNOWN;
				flag |= MemoryByte.WRITABLE;

				MemoryByte[] byteUnit = new MemoryByte[getAddressableSize()];
				for (int j = 0; j < getAddressableSize(); j++) {
					MemoryByte oneByte = new MemoryByte(x[j], flag);
					byteUnit[j] = oneByte;
					byteUnit[j].setBigEndian(isBigEndian(addressKey));
					byteUnit[j].setWritable(isWritable(addressKey));
					byteUnit[j].setReadable(isReadable(addressKey));
					returnBytes[i + j] = oneByte;
				}
				SampleMemoryUnit unit = new SampleMemoryUnit(byteUnit);
				memoryBlockTable.put(addressKey, unit);

			} else {
				MemoryByte[] bytes = temp.getBytes();

				for (int j = 0; j < bytes.length; j++) {
					MemoryByte oneByte = new MemoryByte(bytes[j].getValue(), bytes[j].getFlags());
					returnBytes[i + j] = oneByte;
					returnBytes[i + j].setBigEndian(isBigEndian(addressKey));
					returnBytes[i + j].setWritable(isWritable(addressKey));
				}
			}
		}

		return returnBytes;
	}

	/**
	 * Run the debuggee
	 */
	public void resume() {
		changeValue();
	}

	/**
	 * Convenience function to cause changes in a memoryBlock block. Changes
	 * could result from running the program, changing a variable, etc.
	 */
	synchronized public void changeValue() {
		if (memoryBlockTable == null) {
			return;
		}

		Enumeration<BigInteger> enumeration = memoryBlockTable.keys();
		long randomChange = random.nextInt(37);

		while (randomChange <= 5) {
			randomChange = random.nextInt(37);
		}

		while (enumeration.hasMoreElements()) {
			BigInteger key = enumeration.nextElement();
			if (key.remainder(BigInteger.valueOf(randomChange)).equals(BigInteger.ZERO)) {
				byte[] x = new byte[getAddressableSize()];
				random.nextBytes(x);

				MemoryByte unitBytes[] = new MemoryByte[getAddressableSize()];
				for (int i = 0; i < x.length; i++) {
					MemoryByte oneByte = new MemoryByte();
					oneByte.setValue(x[i]);
					oneByte.setReadable(true);
					oneByte.setChanged(true);
					oneByte.setHistoryKnown(true);
					oneByte.setBigEndian(isBigEndian(key));
					oneByte.setWritable(isWritable(key));
					oneByte.setReadable(isReadable(key));
					unitBytes[i] = oneByte;
				}

				SampleMemoryUnit unit = new SampleMemoryUnit(unitBytes);

				memoryBlockTable.put(key, unit);
			} else {
				SampleMemoryUnit unit = memoryBlockTable.get(key);

				MemoryByte[] bytes = unit.getBytes();

				for (MemoryByte b : bytes) {
					b.setChanged(false);
					b.setHistoryKnown(true);
				}

				unit.setBytes(bytes);

				memoryBlockTable.put(key, unit);
			}
		}
	}

	/**
	 * Simulates evaluation of an expression. Given an expression, return ad
	 * address
	 *
	 * @param expression
	 * @param evalContext
	 * @return the address the expression is evaluated to
	 */
	public BigInteger evaluateExpression(String expression, Object evalContext) {
		BigInteger expAddress = expressionAddressTable.get(expression);
		if (expAddress == null) {
			int address = random.nextInt();

			// make sure number is positive
			if (address < 0) {
				address = address * -1;
			}

			expAddress = BigInteger.valueOf(address);
			expressionAddressTable.put(expression, expAddress);
		}
		return expAddress;
	}

	/**
	 * Simulates checking if storage retrieval is supported
	 *
	 * @return if the engine supports storage retrieval
	 */
	public boolean supportsStorageRetrieval() {
		return true;
	}

	/**
	 * Simulates modifying memory using BigInteger as the address
	 *
	 * @param address
	 * @param bytes
	 * @throws RuntimeException
	 */
	public void setValue(BigInteger address, byte[] bytes) throws RuntimeException {
		BigInteger convertedAddress = address;

		for (int i = 0; i < bytes.length; i = i + getAddressableSize()) {
			SampleMemoryUnit unit = memoryBlockTable.get(convertedAddress);

			MemoryByte[] unitBytes = unit.getBytes();
			for (int j = 0; j < unitBytes.length; j++) {
				unitBytes[j].setValue(bytes[i + j]);
				unitBytes[j].setChanged(true);
				unitBytes[j].setHistoryKnown(true);
			}
			convertedAddress = convertedAddress.add(BigInteger.valueOf(1));
		}
	}

	/**
	 * @return addrssablesize of the debuggee
	 */
	public int getAddressableSize() {
		return 1;
	}

	/**
	 * @param address
	 * @return true if the debuggee is big endian, false otherwise
	 */
	public boolean isBigEndian(BigInteger address) {
		// simulate mixed endianess in a memory block
		// memory before the boundary address is little endian
		// memory after the boundaress is big endian
		BigInteger boundary = new BigInteger("12345678", 16); //$NON-NLS-1$
		if (address.compareTo(boundary) > 0) {
			return true;
		}
		return false;
	}

	/**
	 * @param address
	 * @return true if the address is writable, false otherwise Read only
	 *         segment: 0xab123456 to 0xab123556
	 */
	public boolean isWritable(BigInteger address) {
		BigInteger boundary = new BigInteger("ab123456", 16); //$NON-NLS-1$
		BigInteger boundaryEnd = new BigInteger("ab123556", 16); //$NON-NLS-1$
		if (address.compareTo(boundary) > 0 && address.compareTo(boundaryEnd) < 0) {
			return false;
		}

		boundary = new BigInteger("cd123456", 16); //$NON-NLS-1$
		boundaryEnd = new BigInteger("cd123576", 16); //$NON-NLS-1$
		if (address.compareTo(boundary) > 0 && address.compareTo(boundaryEnd) < 0) {
			return false;
		}

		return true;

	}

	/**
	 * @param address
	 * @return
	 */
	public boolean isReadable(BigInteger address) {
		BigInteger boundary = new BigInteger("cd123456", 16); //$NON-NLS-1$
		BigInteger boundaryEnd = new BigInteger("cd123576", 16); //$NON-NLS-1$
		if (address.compareTo(boundary) > 0 && address.compareTo(boundaryEnd) < 0) {
			return false;
		}
		return true;
	}

	/**
	 * @param target
	 * @return
	 */
	public SampleThread[] getThreads(SampleDebugTarget target) {
		Object thread = threadTable.get(target);
		if (thread == null) {
			thread = new SampleThread(target);
			threadTable.put(target, thread);
		}
		return new SampleThread[] { (SampleThread) thread };
	}

	/**
	 * @param thread
	 * @return
	 */
	public SampleStackFrame[] getStackframes(SampleThread thread) {
		Object stackframes = stackframeTable.get(thread);
		if (stackframes == null) {
			stackframes = createStackframes(thread);
			stackframeTable.put(thread, stackframes);
		}
		return (SampleStackFrame[]) stackframes;
	}

	/**
	 *
	 */
	private SampleStackFrame[] createStackframes(SampleThread thread) {
		SampleStackFrame[] stackframes = new SampleStackFrame[2];
		stackframes[0] = new SampleStackFrame(thread, "Frame1"); //$NON-NLS-1$
		stackframes[1] = new SampleStackFrame(thread, "Frame2"); //$NON-NLS-1$
		return stackframes;
	}

	/**
	 * @param mb
	 * @return true if memory block is to support base address modification,
	 *         false otherwise
	 */
	public boolean suppostsBaseAddressModification(SampleMemoryBlock mb) {
		return false;
	}

	/**
	 * Sets the base address of this memory block
	 *
	 * @param mb the memory block to change base address
	 * @param address the new base address of the memory block
	 * @throws CoreException
	 */
	public void setBaseAddress(SampleMemoryBlock mb, BigInteger address) throws CoreException {
	}

	/**
	 * @param mb
	 * @return true if this memory block supports value modification, false
	 *         otherwise
	 * @throws CoreException
	 */
	public boolean supportsValueModification(SampleMemoryBlock mb) {
		return true;
	}

	/**
	 * @return address size of the debuggee
	 * @throws CoreException
	 */
	public int getAddressSize() throws CoreException {
		return 4;
	}
}
