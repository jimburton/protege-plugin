package uk.ac.brighton.vmg.cviz.diagrambuilder;

/**
 * A utility class for 2-tuples. Amazing that java doesn't have one of these...
 * 
 * Copyright (c) 2013 The ConceptViz authors (see the file AUTHORS).
 * See the file LICENSE for copying permission.
 * 
 * @param <S> the type of the first element
 * @param <T> the type of the second element
 */

public class Pair<S, T> {
	final S fst;
	final T snd;

	/**
	 * The sole constructor.
	 * 
	 * @param fst
	 * @param snd
	 */
	public Pair(S fst, T snd) {
		this.fst = fst;
		this.snd = snd;
	}

	@Override
	public int hashCode() {
		return fst.hashCode() ^ snd.hashCode();
	}

	@Override
	public boolean equals(Object o) {
		if (o == null)
			return false;
		if (!(o instanceof Pair))
			return false;
		Pair<?, ?> that = (Pair<?, ?>) o;
		return this.fst.equals(that.fst)
				&& this.snd.equals(that.snd);
	}
}
