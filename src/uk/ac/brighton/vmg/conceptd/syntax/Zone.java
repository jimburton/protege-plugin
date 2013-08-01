package uk.ac.brighton.vmg.conceptd.syntax;
/**
 * A lightweight container for sets of Strings, representing classes which are
 * inside and outside of this zone.
 * 
 * Copyright (c) 2013 The ConceptViz authors (see the file AUTHORS).
 * See the file LICENSE for copying permission.
 */
import java.util.HashSet;
import java.util.Set;

public class Zone {
	private final Set<String> in, out;
	
	/**
	 * Constructs a new Zone given two sets of Strings, which are intended to be disjoint.
	 * 
	 * @param in the labels of classes which are inside this zone
	 * @param out the labels of classes which are outside this zone
	 */
	public Zone(Set<String> in, Set<String> out) {
		this.in = in;
		this.out = out;
	}
	
	/**
	 * Constructs a new Zone by making a deep copy of an existing one.
	 * 
	 * @param z the Zone to copy
	 */
	public Zone(Zone z) {
		this.in = new HashSet<String>(z.getIn());
		this.out = new HashSet<String>(z.getOut());
	}

	public Set<String> getIn() {
		return in;
	}

	public Set<String> getOut() {
		return out;
	}
	
	@Override
	public String toString() {
		return "("+getIn().toString()+", "+getOut().toString()+")";
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
