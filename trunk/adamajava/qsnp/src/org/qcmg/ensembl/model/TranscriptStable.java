/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.model;

import java.io.Serializable;
import java.sql.Timestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "homo_sapiens_core_55_37.transcript_stable_id")
public class TranscriptStable implements Serializable{
	
	private static final long serialVersionUID = 2019403864455876937L;

//	@Id
//	@OneToOne
//	@JoinColumn(name="transcript_id")
//	private Transcript transcript;
	@Id
	@Column(name="transcript_id")
	private Integer id;
	
	@Column(name="stable_id")
	private String stableId;
	
	@Column
	private Integer version;
	
	@Column(name="created_date")
	private Timestamp created;
	
	@Column(name="modified_date")
	private Timestamp modified;

	public Integer getId() {
		return id;
	}

	public void setTranscript(Integer id) {
		this.id = id;
	}
//	public Transcript getTranscript() {
//		return transcript;
//	}
//	
//	public void setTranscript(Transcript transcript) {
//		this.transcript = transcript;
//	}

	public String getStableId() {
		return stableId;
	}

	public void setStableId(String stableId) {
		this.stableId = stableId;
	}

	public Integer getVersion() {
		return version;
	}

	public void setVersion(Integer version) {
		this.version = version;
	}

	public Timestamp getCreated() {
		return created;
	}

	public void setCreated(Timestamp created) {
		this.created = created;
	}

	public Timestamp getModified() {
		return modified;
	}

	public void setModified(Timestamp modified) {
		this.modified = modified;
	}
	
}
