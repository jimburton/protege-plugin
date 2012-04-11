package uk.ac.brighton.vmg.conceptd.ui;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.apache.log4j.Logger;
import org.protege.editor.owl.model.hierarchy.OWLObjectHierarchyProvider;
import org.protege.editor.owl.ui.renderer.OWLModelManagerEntityRenderer;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

public class ViewComponent extends AbstractOWLClassViewComponent {
    private static final long serialVersionUID = -4515710047558710080L;
    private JTextArea namesComponent;
    // convenience class for querying the asserted subsumption hierarchy directly
    private OWLObjectHierarchyProvider<OWLClass> assertedHierarchyProvider;
    // provides string renderings of Classes/Properties/Individuals, reflecting the current output settings
    private OWLModelManagerEntityRenderer ren;
    private static final Logger log = Logger.getLogger(ViewComponent.class);

	@Override
	public void disposeView() {
		//
	}

	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BorderLayout(6, 6));
        // in our implementation, just create a simple text area in a scrollpane
        namesComponent = new JTextArea();
        namesComponent.setTabSize(2);
        add(new JScrollPane(namesComponent), BorderLayout.CENTER);
        log.debug("CD View Component initialized");//
	}

	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		namesComponent.setText("");
        if (selectedClass != null){
            //assertedHierarchyProvider = getOWLModelManager().getOWLClassHierarchyProvider();
        	assertedHierarchyProvider = 
        			getOWLModelManager().getOWLHierarchyManager().getOWLClassHierarchyProvider();
            ren = getOWLModelManager().getOWLEntityRenderer();
            render(selectedClass, 0);
        }
        return selectedClass;
	}
	
	// render the class and recursively all of its subclasses
    private void render(OWLClass selectedClass, int indent) {
        for (int i=0; i<indent; i++){
            namesComponent.append("\t");
        }
        namesComponent.append(ren.render(selectedClass));
        namesComponent.append("\n");
        // the hierarchy provider gets subclasses for us
        for (OWLClass sub: assertedHierarchyProvider.getChildren(selectedClass)){
            render(sub, indent+1);
        }
    }
}
