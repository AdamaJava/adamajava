/**
 * © Copyright The University of Queensland 2010-2014.  This code is released under the terms outlined in the included LICENSE file.
 */
package org.qcmg.ensembl.model;

import java.io.Serializable;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;



@Entity
@Table(name = "homo_sapiens_core_55_37.seq_region")
public class SeqRegion implements Serializable {

	private static final long serialVersionUID = -20715904189449716L;

	@Id
	@Column(name = "seq_region_id")
	private Long id;

	@Column
	private String name;

	@Column(name = "coord_system_id")
	private Long coordsystemId;

	@Column
	private Integer length;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getCoordsystemId() {
		return coordsystemId;
	}

	public void setCoordsystemId(Long coordsystemId) {
		this.coordsystemId = coordsystemId;
	}

	public Integer getLength() {
		return length;
	}

	public void setLength(Integer length) {
		this.length = length;
	}
}
