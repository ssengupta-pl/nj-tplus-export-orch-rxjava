/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;

/**
 * @author ssengupta
 */
public class GenericResponse {
	public static final String ERROR = "ERROR";

	public static final String SUCCESS = "SUCCESS";

	public static final String COMPLETED = "COMPLETED";

	private String source;

	private String result;

	private String message;

	private HashMap<String, Object> information = new HashMap<String, Object>();

	/**
	 * @return the source
	 */
	public String getSource() {
		return source;
	}

	/**
	 * @param source
	 *            the source to set
	 */
	public void setSource(String source) {
		this.source = source;
	}

	/**
	 * @return the result
	 */
	public String getResult() {
		return result;
	}

	/**
	 * @param result
	 *            the result to set
	 */
	public void setResult(String result) {
		this.result = result;
	}

	/**
	 * @return the message
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message
	 *            the message to set
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return the information
	 */
	public HashMap<String, Object> getInformation() {
		return information;
	}

	/**
	 * @param information
	 *            the information to set
	 */
	public void setInformation(HashMap<String, Object> information) {
		if (this.information == null) {
			this.information = new HashMap<String, Object>();
		}
		this.information = information;
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
				+ (information == null ? 0 : information.hashCode());
		result = prime * result + (message == null ? 0 : message.hashCode());
		result = prime * result
				+ (this.result == null ? 0 : this.result.hashCode());
		result = prime * result + (source == null ? 0 : source.hashCode());
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
		if (!(obj instanceof GenericResponse)) {
			return false;
		}
		GenericResponse other = (GenericResponse) obj;
		if (information == null) {
			if (other.information != null) {
				return false;
			}
		} else if (!information.equals(other.information)) {
			return false;
		}
		if (message == null) {
			if (other.message != null) {
				return false;
			}
		} else if (!message.equals(other.message)) {
			return false;
		}
		if (result == null) {
			if (other.result != null) {
				return false;
			}
		} else if (!result.equals(other.result)) {
			return false;
		}
		if (source == null) {
			if (other.source != null) {
				return false;
			}
		} else if (!source.equals(other.source)) {
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
		builder.append("GenericResponse [source=");
		builder.append(source);
		builder.append(", result=");
		builder.append(result);
		builder.append(", message=");
		builder.append(message);
		builder.append(", information=");
		builder.append(information != null
				? toString(information.entrySet(), maxLen) : null);
		builder.append("]");
		return builder.toString();
	}

	private String toString(Collection<?> collection, int maxLen) {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		int i = 0;
		for (Iterator<?> iterator = collection.iterator(); iterator.hasNext()
				&& i < maxLen; i++) {
			if (i > 0) {
				builder.append(", ");
			}
			builder.append(iterator.next());
		}
		builder.append("]");
		return builder.toString();
	}
}
