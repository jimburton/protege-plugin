package uk.ac.brighton.vmg.conceptd.syntax;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
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

public class AbstractDiagramBuilder2 {

	private Set<Zone> zones;
	private Set<Zone> shadedZones;
	private Set<String> inconsistentClasses;
	private HashMap<OWLClass, String> classMap;// store the display names of
												// classes
	private HashMap<String, Set<String>> disjointsInfo;
	HashMap<String, Set<OWLClass>> equivsInfo;
	HashMap<String, Set<OWLClass>> unionsInfo;
	HashMap<String, Set<String>> supersInfo;
	private int hierarchyDepth;
	private final String EMPTY_LABEL = "Nothing";

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
			.getLogger(AbstractDiagramBuilder2.class);

	public AbstractDiagramBuilder2(OWLClass selectedClass, OWLModelManager mgr,
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
			classMap = new HashMap<OWLClass, String>();
			disjointsInfo = new HashMap<String, Set<String>>();
			equivsInfo = new HashMap<String, Set<OWLClass>>();
			unionsInfo = new HashMap<String, Set<OWLClass>>();
			supersInfo = new HashMap<String, Set<String>>();
		}
	}

	/**
	 * Create the diagram
	 */
	public void build() {
		if (topClass != null) {
			setClasses(topClass, hierarchyDepth, new HashSet<String>());
			log.info("classes: " + classMap.values());
			Zone empty = new Zone(new HashSet<String>(), new HashSet<String>());
			HashSet<Zone> emptyZoneSet = new HashSet<Zone>();
			emptyZoneSet.add(empty);
			Diagram d = new Diagram(emptyZoneSet, new HashSet<Zone>(emptyZoneSet));
			HashSet<String> emptyCurveSet = new HashSet<String>();
			d = addCurve(d, topClassName, emptyCurveSet, emptyCurveSet);
			log.info("After first curve: "+d);
			for(String l: classMap.values()) {
				d = addCurve(d, l, getDisjoints(l), getSupers(l));
				log.info(d);
			}
			log.info(d);
			zones = d.getZones();
			shadedZones = d.getShadedZones();
		}
	}
	
	/*
	 * Working Scala implementation of addCurve:

  def addCurve(d: Diagram, l: String, disjoints: List[String], supers: List[String]): Diagram = {
    val zs = d.zones
    val (z_out, z_in, z_split) = zs.foldLeft((List[Zone](), List[Zone](), List[Zone]()))((res, z) => {
      val is_out = !z.in.intersect(disjoints).isEmpty || !z.out.intersect(supers).isEmpty
    	val is_in = !z.in.intersect(supers).isEmpty
    	val o = if (is_out) z :: res._1 else res._1
    	val i = if (is_in) z :: res._2 else res._2
    	val s = if (is_in || is_out) res._3 else z :: res._3
    	(o, i, s)
    })
    val o2 = z_out.map(z => new Zone(z.in, l :: z.out))
    val i2 = z_in.map(z => new Zone(l :: z.in, z.out))
    val s2 = z_split.flatMap(z => Set(new Zone(l :: z.in, z.out), new Zone(z.in, l :: z.out)))
    new Diagram(o2 ++ i2 ++ s2)
  }
  }
	 */
	private Diagram addCurve(Diagram d, String label, Set<String> disjoints, Set<String> supers) {
		log.info("Adding :::::::  "+label+"  :::::::");
		if(d.getLabels().contains(label)) {
			log.info("Nothing to do");
			return d;
		}
		log.info(label + " is disjoint from " + stringSet(disjoints) + " and has supers " + stringSet(supers));
		Set<Zone> zs = d.getZones();
		Set<Zone> z_shaded = new HashSet<Zone>();
		
		Set<Zone> z_in = new HashSet<Zone>();
		Set<Zone> z_out = new HashSet<Zone>();
		Set<Zone> z_split = new HashSet<Zone>();
		Set<Zone> z_all = new HashSet<Zone>();

		boolean is_out, is_in;
		for(Zone z: zs) {
			is_out = !isDisjoint(z.getIn(), disjoints) || !isDisjoint(z.getOut(), supers);
			is_in = !isDisjoint(z.getIn(), supers);
			if(is_in) {
				log.info(z + " is IN");
				z_in.add(z);
			}
			if(is_out) {
				log.info(z + " is OUT");
				z_out.add(z);
			}
			if(!(is_in || is_out)) {
				log.info(z + " is SPLIT");
				z_split.add(z);
			}
		}
		Zone z1, z2;
		Set<Zone> z_tmp = new HashSet<Zone>();
		
		for(Zone z: z_in) {
			z1 = new Zone(z);
			z2 = new Zone(z);
			z1.getIn().add(label);
			z2.getOut().add(label);
			z_tmp.add(z1);
			z_tmp.add(z2);
			if(d.getShadedZones().contains(z)) z_shaded.add(z2);
		}
		z_in = z_tmp;
		z_tmp = new HashSet<Zone>();
		
		for(Zone z: z_out) {
			z1 = new Zone(z);
			z1.getOut().add(label);
			if(d.getShadedZones().contains(z)) z_shaded.add(z1);
		}
		
		for(Zone z: z_split) {
			z1 = new Zone(z);
			z2 = new Zone(z);
			z1.getIn().add(label);
			z2.getOut().add(label);
			z_tmp.add(z1);
			z_tmp.add(z2);
		}
		z_split = z_tmp;
		///////////////////
		log.info("IN");
		for(Zone z: z_in) log.info(z);
		log.info("OUT");
		for(Zone z: z_out) log.info(z);
		log.info("SPLIT");
		for(Zone z: z_split) log.info(z);
		///////////////////
		z_all = z_in;
		z_all.addAll(z_out);
		z_all.addAll(z_split);
		///////////////////
		log.info("EVERYTHING");
		for(Zone z: z_all) log.info(z);
		///////////////////
		// Shading and inconsistency
		for(Zone z: z_all) {
			for(String l: z.getIn()) {
				if(inconsistentClasses.contains(l)) {
					z_shaded.add(z);
					break;
				}
				boolean shadeMe = false;
				if(equivsInfo.containsKey(l)) {
					for(OWLClass e: equivsInfo.get(l)) {
	    				String eNm = classMap.get(e);
	    				if(!z.getIn().contains(eNm)) {
	    					shadeMe = true;
	    					break;
	    				}
	    			}
					if(shadeMe) {
						z_shaded.add(z);
						break;
					}
				}
				if(unionsInfo.containsKey(l)) {//if l is a union of classes, S, regions containing l but not S should be shaded
	    			Set<OWLClass> unions = unionsInfo.get(l);
	    			shadeMe = true;
	    			for(OWLClass u: unions) {
	    				String uNm = classMap.get(u);
	    				if(z.getIn().contains(uNm)) {
	    					shadeMe = false;
	    					break;
	    				}
	    			}
	    			if(shadeMe) {
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
			if (depth == 1) {
				if (theProvider.getChildren(cls).size() > 0)
					nm += "+";
				classMap.put(cls, nm);
			} else {
				classMap.put(cls, nm);
				int newDepth = --depth;
				Set<String> supers2 = new HashSet<String>(supers);
				supers2.add(nm);
				for (OWLClass sub : theProvider.getChildren(cls)) {
					setClasses(sub, newDepth, supers2);
				}
			}
			// collect the disjointness info
			NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(cls);// or use
																		// TR's
																		// method
			Set<String> disjoints = new HashSet<String>();
			Iterator<Node<OWLClass>> it = ds.iterator();
			OWLClass c;
			while (it.hasNext()) {
				c = it.next().getRepresentativeElement();
				disjoints.add(render(c));
			}
			if (disjoints.size() > 0)
				disjointsInfo.put(nm, disjoints);
			// collect the equivalent classes info
			Node<OWLClass> equivs = theReasoner.getEquivalentClasses(cls);
			if (!equivs.isSingleton())
				equivsInfo.put(nm, equivs.getEntities());
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
			if (!theReasoner.isSatisfiable(cls))
				inconsistentClasses.add(classMap.get(cls));
		}
	}

	private String render(OWLClass cls) {
		return ren.render(cls).replaceAll("'", "").replaceAll(" ", "");
	}

	public Set<Zone> getZones() {
		return zones;
	}

	public Set<Zone> getShadedZones() {
		return shadedZones;
	}
	
	public <T> Set<T> intersect(Set<T> set1, Set<T> set2) {
		Set<T> intersection = new HashSet<T>(set1);
		intersection.retainAll(set2);
        return intersection;
    }
	
	public <T> boolean isDisjoint(Set<T> set1, Set<T> set2) {
        return intersect(set1, set2).isEmpty();
    }
	
	private Set<String> getDisjoints(String label) {
		return (disjointsInfo.containsKey(label)) ? disjointsInfo.get(label) : new HashSet<String>();
	}
	
	private Set<String> getSupers(String label) {
		return (supersInfo.containsKey(label)) ? supersInfo.get(label) : new HashSet<String>();
	}
	
	private <T> String stringSet(Set<T> s) {
		StringBuilder sb = new StringBuilder("{");
		for(T e: s) {
			sb.append(e.toString());
			sb.append(", ");
		}
		sb.append("}");
		return sb.toString();
	}

}
