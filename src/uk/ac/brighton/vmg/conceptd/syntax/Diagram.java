package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.HashSet;
import java.util.Set;

public class Diagram {
	private Set<Zone> zones;
	private Set<Zone> shadedZones;
	private Set<String> curves;

	public Diagram(Set<Zone> zs, Set<Zone> sz) {
		zones = zs;
		shadedZones = sz;
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
	
	public String toString() {
		StringBuffer sb = new StringBuffer("D(");
		for(Zone z: zones) {
			sb.append(z.toString()).append(", ");
		}
		sb.append(")");
		return sb.toString();
	}

	public Set<Zone> getShadedZones() {
		return shadedZones;
	}

}
