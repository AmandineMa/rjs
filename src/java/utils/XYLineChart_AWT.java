package utils;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.ChartUtilities;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.annotations.XYPointerAnnotation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.Marker;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYLineAndShapeRenderer;
import org.jfree.data.Range;
import org.jfree.data.xy.XYDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.ApplicationFrame;
import org.jfree.ui.RectangleAnchor;
import org.jfree.ui.TextAnchor;

import jason.asSyntax.Literal;
import jason.asSyntax.NumberTermImpl;
//import javafx.scene.text.Font;

/**
 * @see https://stackoverflow.com/a/21307289/230513
 */

public class XYLineChart_AWT extends ApplicationFrame {

	private static final long serialVersionUID = 1L;

	final XYSeries session = new XYSeries( "Session QoI");  
	final XYSeries task = new XYSeries( "task QoI", false);  
	final XYSeries action = new XYSeries( "action QoI" );
	JFreeChart xylineChart;
	final Marker start;
	double MIN_X = 0;
	final XYPlot plot;
	boolean hasMarker = false;
	boolean first = true;

	public NumberAxis xAxis = new NumberAxis();

	public XYLineChart_AWT( String applicationTitle, String chartTitle ) {
		super(applicationTitle);
		xylineChart = ChartFactory.createXYLineChart(
				chartTitle ,
				"Category" ,
				"Score" ,
				createDataset() ,
				PlotOrientation.VERTICAL ,
				true , true , false);

		ChartPanel chartPanel = new ChartPanel( xylineChart );
//		chartPanel.setSize(new java.awt.Dimension( 560 , 367 ) );
		plot = xylineChart.getXYPlot( );

		//       Create an NumberAxis
		xAxis = new NumberAxis();
		plot.setDomainAxis(xAxis);
		NumberAxis yAxis = new NumberAxis();
		yAxis.setRange(-1.1, 1.1);
		plot.setRangeAxis(yAxis);
		
		start = new ValueMarker(1.1);
	    start.setPaint(Color.red);
	    start.setLabelAnchor(RectangleAnchor.BOTTOM_LEFT);
	    start.setLabelTextAnchor(TextAnchor.TOP_LEFT);
	    plot.addRangeMarker(start);

		XYLineAndShapeRenderer renderer = new XYLineAndShapeRenderer( );
		renderer.setSeriesPaint( 0 , Color.RED );
		renderer.setSeriesPaint( 1 , Color.GREEN );
		renderer.setSeriesPaint( 2 , Color.YELLOW );
		renderer.setSeriesStroke( 0 , new BasicStroke( 1.0f ) );
		renderer.setSeriesStroke( 1 , new BasicStroke( 1.0f ) );
		renderer.setSeriesStroke( 2 , new BasicStroke( 1.0f ) );
		plot.setRenderer( renderer ); 
//		setContentPane( chartPanel ); 
		this.add(chartPanel, BorderLayout.CENTER);
		this.setSize(new java.awt.Dimension( 1700 , 450 ));
	}

	private XYDataset createDataset( ) {        

		final XYSeriesCollection dataset = new XYSeriesCollection( );          
		dataset.addSeries( session );          
		dataset.addSeries( task );          
		dataset.addSeries( action );
		return dataset;
	}

	public void update(Literal s, Literal t, Literal a) {
		double max = MIN_X;
		if(s != null) {
			double session_t = ((NumberTermImpl) s.getAnnot("add_time").getTerm(0)).solve();
			session.add( session_t, ((NumberTermImpl) s.getTerm(1)).solve());
			if(MIN_X == 0)
				MIN_X = session_t;
			max = Math.max(session_t, max);
			
		}
		if(t != null) {
			double task_t = ((NumberTermImpl) t.getAnnot("add_time").getTerm(0)).solve();
			task.add( task_t, ((NumberTermImpl) t.getTerm(1)).solve());
			if(MIN_X == 0)
				MIN_X = task_t;
			max = Math.max(task_t, max);
		}
		if(a != null) {
			double action_t = ((NumberTermImpl) a.getAnnot("add_time").getTerm(0)).solve();
			action.add( action_t, ((NumberTermImpl) a.getTerm(1)).solve());
			if(MIN_X == 0)
				MIN_X = action_t;
			max = Math.max(action_t, max);
		}
		
		xAxis.setAutoRange(true);
		xAxis.setLowerBound(MIN_X);
		xAxis.setRange(new Range(MIN_X, max+100));
	}
	
	public void setOngoingStep(String step) {
		if(!hasMarker) {
			plot.addRangeMarker(start);
			hasMarker = true;
		}
		start.setLabel("guiding task ongoing step: "+step);
		
	}
	
	public void setNoOngoingStep() {
		hasMarker = false;
		plot.removeRangeMarker(start);
	}
	
	public void insert_discontinuity(String type, double time) {
		
		switch(type) {
		case "session":
//			session.add(time, Double.NaN);
			session.clear();
			task.clear();
			action.clear();
			break;
		case "task":
			task.add(time, Double.NaN);
			break;
		case "action":
			action.add(time, Double.NaN);
			break;
		}
	}
	
	public void add_label(String text, Literal point) {
		double x = ((NumberTermImpl) point.getAnnot("add_time").getTerm(0)).solve();
		double y = ((NumberTermImpl) point.getTerm(1)).solve();
		XYPointerAnnotation textAnnotaion;
		if(first) {
			textAnnotaion = new XYPointerAnnotation(text, x, y, Math.PI / 4.0);
			first = false;
		} else
			textAnnotaion = new XYPointerAnnotation(text, x, y, 3 * Math.PI / 4.0);
        plot.addAnnotation(textAnnotaion);
	}
	
	public void saveChart() {
//		setNoOngoingStep();
		try {
			int counter = 1;

	    	File  f = new File("log/charts");
	    	if(!f.exists()){
	    		f.mkdirs();
	    	}
	    	String file_name = "chart";
	    	Path path = Paths.get("log/charts/chart");
	    	while(Files.exists(path)){
	    		file_name = "chart_"+counter;
	    	    path = Paths.get("log/charts/"+file_name);
	    	    counter++;
	    	}
			ChartUtilities.saveChartAsPNG(new File(path.toString()), xylineChart, 1700, 450 );
		} catch (IOException e) {
			System.out.print(Tools.getStackTrace(e));
		}
	}

}
