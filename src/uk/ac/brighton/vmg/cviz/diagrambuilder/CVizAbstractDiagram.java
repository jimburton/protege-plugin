package uk.ac.brighton.vmg.cviz.diagrambuilder;
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

public class CVizAbstractDiagram {
	private final Set<CVizZone> zones;
	private final Set<CVizZone> shadedZones;
	private final Set<Spider> spiders;
	private Set<String> curves;//should be final but java complains because it might never be initialised

	/**
	 * The sole constructor.
	 * 
	 * @param zs the set of zones in this diagram
	 * @param sz the set of shaded zones in this diagram
	 */
	public CVizAbstractDiagram(Set<CVizZone> zs, Set<CVizZone> sz, Set<Spider> sp) {
		zones = zs;
		shadedZones = sz;
		spiders = sp;
		for(CVizZone z: zones) {
			curves = new HashSet<String>(z.getIn());
			curves.addAll(z.getOut());
			break;
		}
	}

	public Set<CVizZone> getZones() {
		return zones;
	}
	
	public Set<String> getCurves() {
		return curves;
	}
	
	public Set<CVizZone> getShadedZones() {
		return shadedZones;
	}
	
	public Set<Spider> getSpiders() {
		return spiders;
	}
	
	public String toString() {
		StringBuffer sb = new StringBuffer("D(");
		for(CVizZone z: zones) {
			sb.append(z.toString()).append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

}
