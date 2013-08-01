package uk.ac.brighton.vmg.conceptd.syntax;

import icircles.abstractDescription.AbstractCurve;
import icircles.abstractDescription.CurveLabel;
import icircles.util.CannotDrawException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.OWLModelManager;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.semanticweb.owlapi.model.ClassExpressionType;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLClassExpression;
import org.semanticweb.owlapi.model.OWLEquivalentClassesAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.Node;
import org.semanticweb.owlapi.reasoner.NodeSet;
import org.semanticweb.owlapi.reasoner.OWLReasoner;

public class AbstractDiagramBuilder {

	private Set<Zone> zones;
	private Set<Zone> shadedZones;
	private Set<String> curves;

	private Set<String> inconsistentClasses;
	private Map<String, Pair<String, OWLClass>> classMap;// map from each class
															// name to a pair of
															// its real name and
															// OWL class
	private Map<String, Set<String>> disjointsInfo;
	private Map<String, Set<String>> supersInfo;
	private Map<String, Set<String>> childrenInfo;
	private Map<String, Set<OWLClass>> equivsInfo;
	private Map<String, Set<OWLClass>> unionsInfo;

	private int hierarchyDepth;
	private final String EMPTY_LABEL = "Nothing";
	public static final int MAX_CURVES = 10;

	private OWLClass topClass;
	private String topClassName;
	private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;

	private OWLObjectHierarchyProvider<OWLClass> inferredHierarchyProvider;
	private OWLObjectHierarchyProvider<OWLClass> theProvider;
	private OWLReasoner theReasoner;
	private OWLModelManager mgr;
	private Set<OWLOntology> activeOntologies;

	// provides string renderings of Classes/Properties/Individuals, reflecting
	// the current output settings
	private OWLModelManagerEntityRenderer ren;

	private enum MODEL {
		INFERRED, ASSERTED
	};

	// private MODEL theModel = MODEL.INFERRED;
	private MODEL theModel = MODEL.ASSERTED;

	private static final Logger log = Logger
			.getLogger(AbstractDiagramBuilder.class);

	public AbstractDiagramBuilder(OWLClass selectedClass, OWLModelManager mgr,
			int d) {
		log.info("new builder for " + selectedClass);
		if (selectedClass != null) {
			this.mgr = mgr;
			hierarchyDepth = d;
			topClass = selectedClass;

			assertedHierarchyProvider = mgr.getOWLHierarchyManager()
					.getOWLClassHierarchyProvider();
			inferredHierarchyProvider = mgr.getOWLHierarchyManager()
					.getInferredOWLClassHierarchyProvider();
			theProvider = (theModel == MODEL.INFERRED ? inferredHierarchyProvider
					: assertedHierarchyProvider);
			theReasoner = mgr.getOWLReasonerManager().getCurrentReasoner();
			ren = mgr.getOWLEntityRenderer();
			topClassName = render(topClass);
			activeOntologies = mgr.getActiveOntologies();

			shadedZones = new HashSet<Zone>();
			zones = new HashSet<Zone>();
			inconsistentClasses = new HashSet<String>();
			classMap = new HashMap<String, Pair<String, OWLClass>>();
			disjointsInfo = new HashMap<String, Set<String>>();
			equivsInfo = new HashMap<String, Set<OWLClass>>();
			unionsInfo = new HashMap<String, Set<OWLClass>>();
			supersInfo = new HashMap<String, Set<String>>();
			childrenInfo = new HashMap<String, Set<String>>();
		}
	}

	/**
	 * Create the diagram
	 */
	public void build() throws CannotDrawException {
		if (topClass != null) {
			setClasses(topClass, hierarchyDepth, new HashSet<String>());
			if (classMap.size() > MAX_CURVES) {
				throw new CannotDrawException("Too many curves");
			}
			restrictClasses();
			// log.info("classes: " + classMap.values());
			Zone empty = new Zone(new HashSet<String>(), new HashSet<String>());
			Set<Zone> emptyZoneSet = new HashSet<Zone>();
			emptyZoneSet.add(empty);
			Diagram d = new Diagram(emptyZoneSet, new HashSet<Zone>(
					emptyZoneSet));
			Set<String> emptyCurveSet = new HashSet<String>();
			d = addCurve(d, topClassName, emptyCurveSet, emptyCurveSet, curves);
			// log.info("After first curve: "+d);
			for (String l : classMap.keySet()) {
				d = addCurve(d, l, getInfo(disjointsInfo, l),
						getInfo(supersInfo, l), getInfo(childrenInfo, l));
				// log.info(d);
			}
			d = setShading(d);
			// log.info(d);
			curves = d.getCurves();
			zones = d.getZones();
			shadedZones = d.getShadedZones();

		}
	}

	/**
	 * There are sometimes hundreds of classes in the disjoint and super
	 * classes, but only a few in the classMap (those we actually want to draw).
	 * We often have to check the contents of these maps, so it's worth it to
	 * restrict them. There seem to be far fewer in the equivs and unionInfo
	 * maps for most ontologies, so don't bother.
	 */
	private void restrictClasses() {
		Set<String> classes = classMap.keySet();
		Set<String> intersect;
		for (String s : disjointsInfo.keySet()) {
			intersect = disjointsInfo.get(s);
			intersect.retainAll(classes);
			disjointsInfo.put(s, intersect);
		}
		for (String s : supersInfo.keySet()) {
			intersect = supersInfo.get(s);
			intersect.retainAll(classes);
			supersInfo.put(s, intersect);
		}
	}

	private Diagram addCurve(Diagram d, String label, Set<String> disjoints,
			Set<String> supers, Set<String> children) {
		// log.info("Adding :::::::  "+label+"  :::::::");
		if (d.getCurves().contains(label)) {
			// log.info("Nothing to do");
			return d;
		}
		log.info(label + " is disjoint from " + disjoints + " and has supers "
				+ supers + " and children " + children);
		Set<Zone> zs = d.getZones();
		Set<Zone> z_all = new HashSet<Zone>();

		boolean is_out, is_in;
		Zone z1, z2;

		for (Zone z : zs) {
			log.info("Adding " + label + " to " + z);
			/*is_split = isDisjoint(z.getOut(), supers)
					&& isDisjoint(z.getIn(), disjoints);
			is_out = !isDisjoint(z.getIn(), disjoints);
			is_in = false;
			if (!is_split && !is_out) {
				Set<String> supers_tmp;
				for (String s : z.getIn()) {
					supers_tmp = supersInfo.get(s);
					if (supers_tmp.contains(label)) {
						is_in = true;
						break;
					}
				}
			}
			if(is_split) {
				log.info(label+" is SPLIT");
				z1 = new Zone(z);
				z1.getIn().add(label);
				z_all.add(z1);
				z2 = new Zone(z);
				z2.getOut().add(label);
				z_all.add(z2);
			} else if (is_out) {
				log.info(label+" is OUT");
				z1 = new Zone(z);
				z1.getOut().add(label);
				z_all.add(z1);
			} else if (is_in) {
				log.info(label+" is IN");
				z1 = new Zone(z);
				z1.getIn().add(label);
				z_all.add(z1);
			} else {
				log.warn(label+" is not IN, OUT or SPLIT");
			}*/
			
			is_in = !isDisjoint(z.getIn(), children) || (isDisjoint(z.getIn(), disjoints)
					&& !isDisjoint(z.getIn(), supers));
			is_out = !isDisjoint(z.getIn(), disjoints)
					|| !isDisjoint(z.getOut(), supers);

			if (is_in) {
				log.info(z + " is IN");
				if(isDisjoint(z.getOut(),  supers)) { //TODO incorporate these conditions in the boolean
					z1 = new Zone(z);
					z1.getIn().add(label);
					z_all.add(z1);
				}
				if(z.getIn().isEmpty() || isDisjoint(z.getIn(), children)) {//TODO incorporate these conditions in the boolean
					z2 = new Zone(z);
					z2.getOut().add(label);
					z_all.add(z2);
				}
			}
			if (is_out) {
				log.info(z + " is OUT");
				z1 = new Zone(z);
				z1.getOut().add(label);
				z_all.add(z1);
			}
			if (!(is_in || is_out)) {
				log.info(z + " is SPLIT");
				z1 = new Zone(z);
				z2 = new Zone(z);
				z1.getIn().add(label);
				z2.getOut().add(label);
				z_all.add(z1);
				z_all.add(z2);
			}
			log.info("z_all: " + z_all);
		}
		return new Diagram(z_all, null);
	}

	private Diagram setShading(Diagram d) {
		// /////////////////
		// Shading and inconsistency
		Set<Zone> z_all = d.getZones();
		Set<Zone> z_shaded = new HashSet<Zone>();
		for (Zone z : z_all) {
			for (String l : z.getIn()) {
				if (inconsistentClasses.contains(l)) {
					z_shaded.add(z);
					break;
				}
				boolean shadeMe = false;
				if (equivsInfo.containsKey(l)) {
					for (OWLClass e : equivsInfo.get(l)) {
						if (!z.getIn().contains(classMap.get(render(e)).fst)) {
							shadeMe = true;
							break;
						}
					}
					if (shadeMe) {
						z_shaded.add(z);
						break;
					}
				}
				if (unionsInfo.containsKey(l)) {// if l is a union of classes,
												// S, regions containing l but
												// not S should be shaded
					Set<OWLClass> unions = unionsInfo.get(l);
					shadeMe = true;
					for (OWLClass u : unions) {
						String uNm = render(u);
						if (!classMap.containsKey(uNm) /* lowest level class */
								|| z.getIn().contains(uNm)) {
							shadeMe = false;
							break;
						}
					}
					if (shadeMe) {
						z_shaded.add(z);
						break;
					}
				}
			}
		}
		return new Diagram(z_all, z_shaded);
	}

	/**
	 * Collect the list of curve labels in the diagram, along with info on
	 * disjoint and union classes
	 * 
	 * @param selectedClass
	 */
	private void setClasses(OWLClass cls, int depth, Set<String> supers) {

		if (depth > 0) {
			String nm = render(cls);
			supersInfo.put(nm, supers);
			Set<OWLClass> children = theProvider.getDescendants(cls);
			String lbl = (children.size() > 0 && depth == 1) ? nm + "+" : nm;
			Pair<String, OWLClass> nameInfo = new Pair<String, OWLClass>(lbl,
					cls);
			classMap.put(nm, nameInfo);
			if (depth > 1) {
				Set<String> supers2 = new HashSet<String>(supers);
				supers2.add(nm);
				int newDepth = --depth;
				for (OWLClass sub : theProvider.getChildren(cls)) {
					setClasses(sub, newDepth, supers2);
				}
			}
			// collect the children info
			Set<String> childrenStr = new HashSet<String>();
			Iterator<OWLClass> it = children.iterator();
			OWLClass c;
			while (it.hasNext()) {
				childrenStr.add(render(it.next()));
			}
			childrenInfo.put(nm, childrenStr);

			// collect the disjointness info
			NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(cls);// or use
																		// TR's
																		// method
			Set<String> disjoints = new HashSet<String>();
			Iterator<Node<OWLClass>> it2 = ds.iterator();
			while (it2.hasNext()) {
				c = it2.next().getRepresentativeElement();
				disjoints.add(render(c));
			}
			if (disjoints.size() > 0)
				disjointsInfo.put(nm, disjoints);
			// collect the equivalent classes info
			Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
			if (!equivs.isSingleton())
				equivsInfo.put(nm, new HashSet<OWLClass>(equivs.getEntities()));
			// collect the union classes info
			Set<OWLEquivalentClassesAxiom> eq;
			for (OWLOntology ont : activeOntologies) {
				eq = ont.getEquivalentClassesAxioms(cls);
				if (eq.size() > 0) {
					for (OWLEquivalentClassesAxiom a : eq) {
						boolean isUnion = true;
						ClassExpressionType t;
						for (OWLClassExpression e : a.getClassExpressions()) {
							t = e.getClassExpressionType();
							if (!(t.equals(ClassExpressionType.OWL_CLASS) || t
									.equals(ClassExpressionType.OBJECT_UNION_OF))) {
								isUnion = false;
								break;
							}
						}
						if (isUnion) {
							Set<OWLClass> us = a.getClassesInSignature();
							us.remove(cls);
							if (us.size() > 0)
								unionsInfo.put(render(cls), us);
						}
					}
				}
			}
			// collect the inconsistent classes info
			if (!theReasoner.isSatisfiable(cls)) {
				inconsistentClasses.add(nm);
			}
		}
	}

	private String render(OWLClass cls) {
		return ren.render(cls).replaceAll("'", "").replaceAll(" ", "");
	}

	public <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
		Set<T> intersection = new HashSet<T>(set1);
		intersection.retainAll(set2);
		return intersection;
	}

	private <T> boolean isDisjoint(Set<T> set1, Set<T> set2) {
		return intersect(set1, set2).isEmpty();
	}

	private <T> boolean isSuperSet(Set<T> supr, Set<T> sub) {
		return supr.containsAll(sub);
	}

	private Set<String> getInfo(Map<String, Set<String>> info, String label) {
		return (info.containsKey(label)) ? info.get(label)
				: new HashSet<String>();
	}

	private <T> String stringSet(List<T> s) {
		StringBuilder sb = new StringBuilder("{");
		for (T e : s) {
			sb.append(e.toString());
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

	private Set<AbstractCurve> stringsToCurves(Set<String> in) {
		Set<AbstractCurve> res = new HashSet<AbstractCurve>();
		for (String s : in) {
			AbstractCurve c = new AbstractCurve(CurveLabel.get(s));
			res.add(c);
			// log.info("str2Curve "+c);
		}
		return res;
	}

	public String[] getCurves() {
		String[] res = new String[curves.size()];
		int i = 0;
		for (String c : curves) {
			res[i++] = classMap.get(c).fst;
		}
		return res;
	}

	public icircles.input.Zone[] getZones() {
		List<icircles.input.Zone> res = new ArrayList<icircles.input.Zone>();
		Set<String> rawNames;
		String[] namesForDisplay;
		for (Zone z : zones) {
			rawNames = z.getIn();
			int size = rawNames.size();
			namesForDisplay = new String[size];
			int i = 0;
			for (String nm : rawNames) {
				namesForDisplay[i++] = classMap.get(nm).fst;
			}
			res.add(new icircles.input.Zone(namesForDisplay));
		}
		res.add(new icircles.input.Zone(new String[] {}));
		return res.toArray(new icircles.input.Zone[res.size()]);
	}

	public icircles.input.Zone[] getShadedZones() {
		List<icircles.input.Zone> res = new ArrayList<icircles.input.Zone>();
		Set<String> rawNames;
		String[] namesForDisplay;
		for (Zone z : shadedZones) {
			rawNames = z.getIn();
			int size = rawNames.size();
			namesForDisplay = new String[size];
			int i = 0;
			for (String nm : rawNames) {
				namesForDisplay[i++] = classMap.get(nm).fst;
			}
			res.add(new icircles.input.Zone(namesForDisplay));
		}
		return res.toArray(new icircles.input.Zone[res.size()]);
	}

	private icircles.input.Zone[] namesToZones(Set<Zone> input, boolean in) {
		List<icircles.input.Zone> res = new ArrayList<icircles.input.Zone>();
		Set<String> rawNames;
		String[] namesForDisplay;
		for (Zone z : input) {
			rawNames = z.getIn();
			int size = rawNames.size();
			namesForDisplay = new String[size];
			int i = 0;
			for (String nm : rawNames) {
				namesForDisplay[i++] = classMap.get(nm).fst;
			}
			res.add(new icircles.input.Zone(namesForDisplay));
		}
		return res.toArray(new icircles.input.Zone[res.size()]);
	}

}
