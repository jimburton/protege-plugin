package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.TreeSet;

public class Zone implements Comparable {

	private TreeSet<String> labels;
	
	public Zone() {
		labels = new TreeSet<String>();
	}
	
	private Zone(TreeSet<String> labels) {
		this.labels = labels;
	}
	
	public boolean add(String s) {
		//if (labels == null) labels = new TreeSet<String>();
		return labels.add(s);
	}
	
	public boolean contains(String s) {
		return labels.contains(s);
	}
	
	public Zone sub(String s) {
		labels.remove(s);
		return this;
	}
	
	public Zone union(Zone z) {
		Zone copy = z.clone();
		copy.addAll(labels);
		return copy;
	}
	
	private boolean addAll(TreeSet<String> labels) {
		return this.labels.addAll(labels);
	}

	public Iterator<String> iterator() {
		return labels.descendingIterator();
	}
	
	public String toString() {
		return labels.toString();
	}
	
	public Zone clone() {
		if(labels == null) return new Zone();
		return new Zone((TreeSet<String>)labels.clone());
	}

	@Override
	public int compareTo(Object o) {
		return this.toString().compareTo(o.toString());
	}

	public Zone addGetZone(String s) {
		Zone z = this.clone();
		z.add(s);
		return z;
	}

	public ArrayList<String> intersect(Zone z) {
		TreeSet<String> copy = (TreeSet<String>)labels.clone();
		copy.retainAll(z.getLabels());
		String[] intersectArray = new String[copy.size()];
		copy.toArray(intersectArray);
		return new ArrayList<String>(Arrays.asList(intersectArray));
	}
	
	protected TreeSet<String> getLabels() {
		return labels;
	}
	
	public String[] getLabelsArray() {
		String[] labelsArray = new String[labels.size()];
		labels.toArray(labelsArray);
		return labelsArray;
	}

}
