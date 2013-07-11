package uk.ac.brighton.vmg.conceptd.syntax;

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

	private ArrayList<ArrayList<String>> zones;
	private ArrayList<ArrayList<String>> shadedZones;
	private ArrayList<String> inconsistentClasses;
	private HashMap<OWLClass, String> classMap;// store the display names of
												// classes
	private HashMap<String, ArrayList<String>> disjointsInfo;
	HashMap<String, Set<OWLClass>> equivsInfo;
	HashMap<String, Set<OWLClass>> unionsInfo;
	HashMap<String, ArrayList<String>> supersInfo;
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

			shadedZones = new ArrayList<ArrayList<String>>();
			zones = new ArrayList<ArrayList<String>>();
			inconsistentClasses = new ArrayList<String>();
			classMap = new HashMap<OWLClass, String>();
			disjointsInfo = new HashMap<String, ArrayList<String>>();
			equivsInfo = new HashMap<String, Set<OWLClass>>();
			unionsInfo = new HashMap<String, Set<OWLClass>>();
			supersInfo = new HashMap<String, ArrayList<String>>();
		}
	}

	/**
	 * Create the diagram
	 */
	public void build() {
		if (topClass != null) {
			setClasses(topClass, hierarchyDepth, new ArrayList<String>());
			log.info("classes: " + classMap.values());
			Zone empty = new Zone(new HashSet<String>(), new HashSet<String>());
			HashSet<Zone> zs = new HashSet<Zone>();
			zs.add(empty);
			Diagram d = new Diagram(zs);
			d = addCurve(d, topClassName);
			System.out.println("After first curve: "+d);
			for(String l: classMap.values()) {
				d = addCurve(d, l);
				System.out.println(d);
			}
			System.out.println(d);
		}
	}

	private class Diagram {
		private HashSet<Zone> zones;

		public Diagram(HashSet<Zone> zs) {
			zones = zs;
		}

		public HashSet<Zone> getZones() {
			return zones;
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

	private class Zone {
		private HashSet<String> in, out;

		public Zone(HashSet<String> in, HashSet<String> out) {
			this.in = in;
			this.out = out;
		}

		public HashSet<String> getIn() {
			return in;
		}

		public HashSet<String> getOut() {
			return out;
		}
		
		public String toString() {
			return "("+in.toString()+", "+out.toString()+")";
		}
	}
	
	public <T> Set<T> intersect(Set<T> list1, List<T> list2) {
        Set<T> list = new HashSet<T>();

        for (T t : list1) {
            if(list2.contains(t)) {
                list.add(t);
            }
        }

        return list;
    }
	
	/*
	 * Scala implementation that works:

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
    new Diagram(o2 union i2 union s2)
  }
	 */

	private Diagram addCurve(Diagram d, String l) {
		System.out.println("Adding: "+l);
		System.out.println("topClassName: "+topClassName);
		HashSet<Zone> zs = d.getZones();
		ArrayList<String> disjoints = disjointsInfo.get(l);
		ArrayList<String> supers = supersInfo.get(l);
		HashSet<Zone> z_in = new HashSet<Zone>();
		HashSet<Zone> z_out = new HashSet<Zone>();
		HashSet<Zone> z_split = new HashSet<Zone>();
		boolean is_out, is_in;
		HashSet<String> old_out, old_in;
		for (Zone z : zs) {
			old_in = z.getIn();
			old_out = z.getOut();
			is_out = !intersect(old_in, disjoints).isEmpty()
					|| !intersect(old_out, supers).isEmpty();
			is_in = !intersect(old_in, supers).isEmpty();
			if (is_out) {
				z_out.add(z);
			}
			if (is_in) {
				z_in.add(z);
			}
			if (!(is_out || is_in)) {
				z_split.add(z);
			}
		}
		for(Zone z: z_out) {
			
		}
		z_out.addAll(z_in);
		z_out.addAll(z_split);
		return new Diagram(z_out);
	}

	/**
	 * Collect the list of curve labels in the diagram, along with info on
	 * disjoint and union classes
	 * 
	 * @param selectedClass
	 */
	private void setClasses(OWLClass cls, int depth, ArrayList<String> supers) {

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
				ArrayList<String> supers2 = (ArrayList<String>) supers.clone();
				supers2.add(nm);
				for (OWLClass sub : theProvider.getChildren(cls)) {
					setClasses(sub, newDepth, supers2);
				}
			}
			// collect the disjointness info
			NodeSet<OWLClass> ds = theReasoner.getDisjointClasses(cls);// or use
																		// TR's
																		// method
			ArrayList<String> disjoints = new ArrayList<String>();
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

	public ArrayList<ArrayList<String>> getZones() {
		return zones;
	}

	public ArrayList<ArrayList<String>> getShadedZones() {
		return shadedZones;
	}

}
