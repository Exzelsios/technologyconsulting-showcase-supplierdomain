package de.novatec.showcase.supplier.dto;

import java.io.Serializable;
import java.util.Objects;

public class SupplierComponentPK implements Serializable {

	private static final long serialVersionUID = 1L;

	private String componentId;

	private int supplierId;

	public SupplierComponentPK() {
	}

	public String getComponentId() {
		return this.componentId;
	}

	public void setComponentId(String componentId) {
		this.componentId = componentId;
	}

	public int getSupplierId() {
		return this.supplierId;
	}

	public void setSupplierId(int supplierId) {
		this.supplierId = supplierId;
	}

	@Override
	public String toString() {
		return "SupplierComponentPK [componentId=" + componentId + ", supplierId=" + supplierId + "]";
	}

	@Override
	public int hashCode() {
		return Objects.hash(componentId, supplierId);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof SupplierComponentPK)) {
			return false;
		}
		SupplierComponentPK other = (SupplierComponentPK) obj;
		return Objects.equals(componentId, other.componentId) && supplierId == other.supplierId;
	}
}