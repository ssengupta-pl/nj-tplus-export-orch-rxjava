/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * @author ssengupta
 */
public class QueryMetadata {
	private int numRows;
	private ArrayList<LinkedHashMap<String, String>> fieldTypes;

	/**
	 * @return the numRows
	 */
	public int getNumRows() {
		return numRows;
	}

	/**
	 * @param numRows
	 *            the numRows to set
	 */
	public void setNumRows(int numRows) {
		this.numRows = numRows;
	}

	/**
	 * @return the fieldTypes
	 */
	public ArrayList<LinkedHashMap<String, String>> getFieldTypes() {
		return fieldTypes;
	}

	/**
	 * @param fieldTypes
	 *            the fieldTypes to set
	 */
	public void setFieldTypes(
			ArrayList<LinkedHashMap<String, String>> fieldTypes) {
		this.fieldTypes = fieldTypes;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ (fieldTypes == null ? 0 : fieldTypes.hashCode());
		result = prime * result + numRows;
		return result;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (!(obj instanceof QueryMetadata)) {
			return false;
		}
		QueryMetadata other = (QueryMetadata) obj;
		if (fieldTypes == null) {
			if (other.fieldTypes != null) {
				return false;
			}
		} else if (!fieldTypes.equals(other.fieldTypes)) {
			return false;
		}
		if (numRows != other.numRows) {
			return false;
		}
		return true;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		final int maxLen = 2;
		StringBuilder builder = new StringBuilder();
		builder.append("QueryMetadata [numRows=");
		builder.append(numRows);
		builder.append(", fieldTypes=");
		builder.append(fieldTypes != null
				? fieldTypes.subList(0, Math.min(fieldTypes.size(), maxLen))
				: null);
		builder.append("]");
		return builder.toString();
	}

}
