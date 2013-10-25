package org.qcmg.common.stream;

public interface Operation<DataType> {
	public boolean applyTo(final DataType data);
}
