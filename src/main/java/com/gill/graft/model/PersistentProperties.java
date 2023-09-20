package com.gill.graft.model;

/**
 * PersistentProperties
 *
 * @author gill
 * @version 2023/09/13
 **/
public class PersistentProperties {

	private long term = 0L;

	private Integer votedFor = null;

	public long getTerm() {
		return term;
	}

	public Integer getVotedFor() {
		return votedFor;
	}

	public void setVotedFor(Integer votedFor) {
		this.votedFor = votedFor;
	}

	public void setTerm(long term) {
		this.term = term;
	}

	public void set(PersistentProperties properties) {
		term = properties.getTerm();
		votedFor = properties.getVotedFor();
	}

	@Override
	public String toString() {
		return "PersistentProperties{" + "term=" + term + ", votedFor=" + votedFor + '}';
	}
}
