package org.eclipse.debug.tests.logicalstructure;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate;
import org.eclipse.debug.core.model.ILogicalStructureTypeDelegate3;
import org.eclipse.debug.core.model.IValue;

public class TestLogicalStructureTypeDelegate implements ILogicalStructureTypeDelegate, ILogicalStructureTypeDelegate3 {

	@Override
	public boolean providesLogicalStructure(IValue value) {
		if (value instanceof TestValue) {
			TestValue testValue = (TestValue) value;
			return "raw".equals(testValue.getValueString());
		}
		return false;
	}

	@Override
	public IValue getLogicalStructure(IValue value) throws CoreException {
		return new TestValue("logical structure");
	}

	@Override
	public void releaseValue(IValue logicalStructure) {
		((TestValue) logicalStructure).release();
	}

}
