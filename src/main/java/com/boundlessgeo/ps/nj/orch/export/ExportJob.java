/**
 *
 */
package com.boundlessgeo.ps.nj.orch.export;

/**
 * @author ssengupta
 */
public class ExportJob {
	private String user;

	private String password;

	private String token;

	private String requestedResource;

	private String requestedOperation;

	private String subscriptionLevel;

	private String query;

	private String icao;

	private Double distance;

	private String jobid;

	private String jobstatus;








	public String getJobstatus() {
		return jobstatus;
	}

	public void setJobstatus(String jobstatus) {
		this.jobstatus = jobstatus;
	}

	public String getJobid() {
		return jobid;
	}

	public void setJobid(String jobid) {
		this.jobid = jobid;
	}

	public String getToken() {
		return token;
	}

	public void setToken(String token) {
		this.token = token;
	}

	public String getIcao() {
		return icao;
	}

	public void setIcao(String icao) {
		this.icao = icao;
	}



	public Double getDistance() {
		return distance;
	}

	public void setDistance(Double distance) {
		this.distance = distance;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	/**
	 * @return the user
	 */
	public String getUser() {
		return user;
	}

	/**
	 * @param user
	 *            the user to set
	 */
	public void setUser(String user) {
		this.user = user;
	}

	/**
	 * @return the requestedResource
	 */
	public String getRequestedResource() {
		return requestedResource;
	}

	/**
	 * @param requestedResource
	 *            the requestedResource to set
	 */
	public void setRequestedResource(String requestedResource) {
		this.requestedResource = requestedResource;
	}

	/**
	 * @return the requestedOperation
	 */
	public String getRequestedOperation() {
		return requestedOperation;
	}

	/**
	 * @param requestedOperation
	 *            the requestedOperation to set
	 */
	public void setRequestedOperation(String requestedOperation) {
		this.requestedOperation = requestedOperation;
	}

	/**
	 * @return the subscriptionLevel
	 */
	public String getSubscriptionLevel() {
		return subscriptionLevel;
	}

	/**
	 * @param subscriptionLevel
	 *            the subscriptionLevel to set
	 */
	public void setSubscriptionLevel(String subscriptionLevel) {
		this.subscriptionLevel = subscriptionLevel;
	}

	/**
	 * @return the query
	 */
	public String getQuery() {
		return query;
	}

	/**
	 * @param query
	 *            the query to set
	 */
	public void setQuery(String query) {
		this.query = query;
	}

	/*
	 * (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + (query == null ? 0 : query.hashCode());
		result = prime * result + (requestedOperation == null ? 0
				: requestedOperation.hashCode());
		result = prime * result + (requestedResource == null ? 0
				: requestedResource.hashCode());
		result = prime * result + (subscriptionLevel == null ? 0
				: subscriptionLevel.hashCode());
		result = prime * result + (user == null ? 0 : user.hashCode());
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
		if (!(obj instanceof ExportJob)) {
			return false;
		}
		ExportJob other = (ExportJob) obj;
		if (query == null) {
			if (other.query != null) {
				return false;
			}
		} else if (!query.equals(other.query)) {
			return false;
		}
		if (requestedOperation == null) {
			if (other.requestedOperation != null) {
				return false;
			}
		} else if (!requestedOperation.equals(other.requestedOperation)) {
			return false;
		}
		if (requestedResource == null) {
			if (other.requestedResource != null) {
				return false;
			}
		} else if (!requestedResource.equals(other.requestedResource)) {
			return false;
		}
		if (subscriptionLevel == null) {
			if (other.subscriptionLevel != null) {
				return false;
			}
		} else if (!subscriptionLevel.equals(other.subscriptionLevel)) {
			return false;
		}
		if (user == null) {
			if (other.user != null) {
				return false;
			}
		} else if (!user.equals(other.user)) {
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
		StringBuilder builder = new StringBuilder();
		builder.append("ExportJob [user=");
		builder.append(user);
		builder.append(", requestedResource=");
		builder.append(requestedResource);
		builder.append(", requestedOperation=");
		builder.append(requestedOperation);
		builder.append(", subscriptionLevel=");
		builder.append(subscriptionLevel);
		builder.append(", query=");
		builder.append(query);
		builder.append("]");
		return builder.toString();
	}
}
