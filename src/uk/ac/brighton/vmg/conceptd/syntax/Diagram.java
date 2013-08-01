package uk.ac.brighton.vmg.conceptd.syntax;
/**
 * A lightweight container for sets of curves and zones, used by {@link AbstractDiagramBuilder}
 * while building up the diagram.
 * 
 * Copyright (c) 2013 The ConceptViz authors (see the file AUTHORS).
 * See the file LICENSE for copying permission.
 */

import icircles.input.Spider;

import java.util.HashSet;
import java.util.Set;

public class Diagram {
	private final Set<Zone> zones;
	private final Set<Zone> shadedZones;
	private final Set<Spider> spiders;
	private Set<String> curves;//should be final but java complains because it might never be initialised

	/**
	 * The sole constructor.
	 * 
	 * @param zs the set of zones in this diagram
	 * @param sz the set of shaded zones in this diagram
	 */
	public Diagram(Set<Zone> zs, Set<Zone> sz, Set<Spider> sp) {
		zones = zs;
		shadedZones = sz;
		spiders = sp;
		for(Zone z: zones) {
			curves = new HashSet<String>(z.getIn());
			curves.addAll(z.getOut());
			break;
		}
	}

	public Set<Zone> getZones() {
		return zones;
	}
	
	public Set<String> getCurves() {
		return curves;
	}
	
	public Set<Zone> getShadedZones() {
		return shadedZones;
	}
	
	public Set<Spider> getSpiders() {
		return spiders;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("D(");
		for(Zone z: zones) {
			sb.append(z.toString()).append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

}
