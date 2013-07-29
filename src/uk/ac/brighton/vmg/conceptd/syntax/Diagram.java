package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.HashSet;
import java.util.Set;

public class Diagram {
	private Set<Zone> zones;
	private Set<String> labels;

	public Diagram(Set<Zone> zs) {
		zones = zs;
		for(Zone z: zones) {
			labels = new HashSet<String>(z.getIn());
			labels.addAll(z.getOut());
			break;
		}
	}

	public Set<Zone> getZones() {
		return zones;
	}
	
	public Set<String> getLabels() {
		return labels;
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
