package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.HashSet;
import java.util.Set;

public class Zone {
	private Set<String> in, out;

	public Zone(Set<String> in, Set<String> out) {
		this.setIn(in);
		this.setOut(out);
	}
	
	public Zone(Zone z) {
		this.setIn(new HashSet<String>(z.getIn()));
		this.setOut(new HashSet<String>(z.getOut()));
	}

	public Set<String> getIn() {
		return in;
	}

	public Set<String> getOut() {
		return out;
	}
	
	public String toString() {
		return "("+getIn().toString()+", "+getOut().toString()+")";
	}

	private void setIn(Set<String> in) {
		this.in = in;
	}

	private void setOut(Set<String> out) {
		this.out = out;
	}
	
	@Override
	public boolean equals(Object o) {
		if(!(o instanceof Zone)) return false;
		Zone that = (Zone) o;
		if(!(this.in.size()==that.in.size())) {
			return false;
		}
		for(String s: this.in) {
			if(!that.in.contains(s)) return false;
		}
		return true;
	}
	
	@Override public int hashCode() {
        StringBuilder sb = new StringBuilder();
        for(String s: this.in) sb.append(s);
        return sb.toString().intern().hashCode();
    }
}
