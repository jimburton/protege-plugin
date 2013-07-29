package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.HashSet;

public class Zone {
	private HashSet<String> in, out;

	public Zone(HashSet<String> in, HashSet<String> out) {
		this.setIn(in);
		this.setOut(out);
	}
	
	public Zone(Zone z) {
		this.setIn(new HashSet<String>(z.getIn()));
		this.setOut(new HashSet<String>(z.getOut()));
	}

	public HashSet<String> getIn() {
		return in;
	}

	public HashSet<String> getOut() {
		return out;
	}
	
	public String toString() {
		return "("+getIn().toString()+", "+getOut().toString()+")";
	}

	private void setIn(HashSet<String> in) {
		this.in = in;
	}

	private void setOut(HashSet<String> out) {
		this.out = out;
	}
}
