package uk.ac.brighton.vmg.conceptd.ui;
/**
 * The ViewComponent of the ConceptViz plugin.
 * 
 * Copyright (c) 2013 The ConceptViz authors (see the file AUTHORS).
 * See the file LICENSE for copying permission.
 */
import icircles.concreteDiagram.ConcreteDiagram;
import icircles.concreteDiagram.DiagramCreator;
import icircles.gui.CirclesPanel;
import icircles.input.AbstractDiagram;
import icircles.input.Spider;
import icircles.util.CannotDrawException;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.apache.log4j.Logger;
import org.protege.editor.owl.ui.view.cls.AbstractOWLClassViewComponent;
import org.semanticweb.owlapi.model.OWLClass;

import uk.ac.brighton.vmg.conceptd.syntax.AbstractDiagramBuilder;

public class ViewComponent extends AbstractOWLClassViewComponent {
	private static final long serialVersionUID = -4515710047558710080L;
	private JPanel cdPanel;
	private JComboBox<String> depthPicker;
	private OWLClass theSelectedClass;

	private int DIAG_SIZE;
	private final double DIAG_SCALE = 0.9;
	private int hierarchyDepth = 2;

	private static final Logger log = Logger.getLogger(ViewComponent.class);
	private static final int IC_VERSION = 1;

	/**
	 * Not used.
	 */
	@Override
	public void disposeView() {
		//
	}

	/**
	 * The Protege API callback when the view is first loaded. Sets up the GUI 
	 * but doesn't start the process of drawing anything.
	 */
	@Override
	public void initialiseClassView() throws Exception {
		getView().setSyncronizing(true);
		setLayout(new BorderLayout(6, 6));

		JPanel topPanel = new JPanel();
		JLabel depthLabel = new JLabel("Depth:");
		String[] depths = { "1", "2", "3", "4", "5" };
		depthPicker = new JComboBox<String>(depths);
		depthPicker.setSelectedIndex(1);
		depthPicker.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hierarchyDepth = Integer.parseInt((String) depthPicker
						.getSelectedItem());
				if (theSelectedClass != null)
					updateView(theSelectedClass);
			}
		});
		topPanel.add(depthLabel);
		topPanel.add(depthPicker);
		add(topPanel, BorderLayout.NORTH);
		cdPanel = new JPanel();
		cdPanel.setBackground(Color.WHITE);
		add(cdPanel, BorderLayout.CENTER);

		log.debug("CD View Component initialized");
	}

	/**
	 * Callback when user selects a class in a hierarchy viewer pane. Constructs
	 * the diagram with the selectedClass as its top-level element using an 
	 * AbstractDiagramBuilder.
	 */
	@Override
	protected OWLClass updateView(OWLClass selectedClass) {
		// DIAG_SIZE = Math.max(getHeight(), getWidth()) - 100;
		theSelectedClass = selectedClass;
		DIAG_SIZE = getHeight() - 50;
		if (selectedClass != null) {
			AbstractDiagramBuilder builder2 = new AbstractDiagramBuilder(
					selectedClass, getOWLModelManager(), hierarchyDepth);
			try {
				builder2.build();
			} catch (CannotDrawException e) {
				log.info("Too many curves to draw");
				displayGUIMessage(e.message);
				return selectedClass;
			}
			String[] cs = builder2.getCurves();
			icircles.input.Zone[] zs = builder2.getZones();
			icircles.input.Zone[] szs = builder2.getShadedZones();
			debug("Curves", cs);
			debug("Zones", zs);
			debug("Shaded zones", szs);
			drawCD(cs, zs, szs);
		}
		return selectedClass;
	}

	/**
	 * Pass the generated abstract description to iCircles and display the result
	 * in a JPanel.
	 * 
	 * @param c the abstract curves
	 * @param z the abstract zones
	 * @param sz the abstract shaded zones
	 */
	private void drawCD(String[] c, icircles.input.Zone[] z,
			icircles.input.Zone[] sz) {
		Font font = new Font("Helvetica", Font.BOLD | Font.ITALIC, 16);

		ConcreteDiagram cd = null;
		String failureMessage = null;
		CirclesPanel cp = null;
		try {
			AbstractDiagram ad = new AbstractDiagram(IC_VERSION, c, z, sz,
					new Spider[] {});
			DiagramCreator dc = new DiagramCreator(ad.toAbstractDescription());
			cd = dc.createDiagram(DIAG_SIZE);
			cd.setFont(font);
			cp = new CirclesPanel("", failureMessage, cd, true);// do use colours
			cp.setScaleFactor(DIAG_SCALE);
		} catch (CannotDrawException x) {
			log.error(x.message);
			displayGUIMessage(x.message);
			return;
		}
		cdPanel.removeAll();
		cdPanel.add(cp);
	}
	
	/**
	 * Display an error message or other warning to the user in the main panel.
	 * 
	 * @param message the message to display
	 */
	private void displayGUIMessage(String message) {
		JTextField tf = new JTextField(message);
		cdPanel.removeAll();
		cdPanel.add(tf);	
	}

	private <T> void debug(String name, Object[] xs) {
		log.info("::::::::::   " + name + "   ::::::::::");
		for (Object x : xs)
			log.info(x);
	}

}
