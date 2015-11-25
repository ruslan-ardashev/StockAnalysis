
// Ruslan Ardashev, Duke University 
// Pratt School of Engineering
// Class of 2015
//
// RSI Analyzer
//
// Analyzer - Top level of project
// 2014, September
//

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartFrame;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.plot.ValueMarker;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;


public class Analyzer implements Runnable {

	// Selective debugging
	public static boolean DEBUG = false;

	// Static, define here. Stocks to analyze go here. 
	public static String[] stocksToAnalyze = {"AAPL", "MSFT", "S", "ARR", "FB", "F", "BAC", "YHOO", "C", "TSLA", "KO"};

	// Dynamic, final stocks analyzed. if web fetch fails, remove from here. otherwise identical to array above.
	public static ArrayList<String> stocksAnalyzed = Analyzer.populateStocksToAanalyzeWithStaticArray(stocksToAnalyze);		

	// Class variables
	public static Analyzer mainAnalyzer;									// Main instance - "delegate"

	public static double finalUpperPercentDistance;							// Final Average results
	public static double finalLowerPercentDistance;

	// MultiCore Synchronization
	public static Integer readyLimit = new Integer(stocksToAnalyze.length);			// Synchronization of threads
	public static Integer readyCount = new Integer(0);								// Incremented by getters

	public static Integer simulationLimit = new Integer(stocksToAnalyze.length);	// Synchronization of threads
	public static Integer simulationCount = new Integer(0);							// Incremented by getters

	// Instance variables
	private ArrayList<DataGetter> dataGetters;										// All DataGetters, belong to main instance of Analyzer 
	public ArrayList<Double[]> HighLowAverageResults;								// index 0 is upper bound percent distance from RSI Bands at pivots 
																					// index 1 is lower bound percent distance from RSI Bands at pivots
	public ArrayList<Double[]> HighPercentagesResults;								// Contain all arrays that contain 0 if no pivot, and a double value 
	public ArrayList<Double[]> LowPercentagesResults;								// with % distance to RSI Band if pivot occurs

	// Simulation Variables
	public static Double startingMoney = new Double(100000);
	public static Double finalAverageMoney = new Double(0.0);						// Will be set at the end of simulation
	public ArrayList<Double> averageFinalMoneyArray = new ArrayList<Double>();		// All data getters report here at end of simulation

	Analyzer () {

		dataGetters = new ArrayList<DataGetter>(stocksToAnalyze.length);
		this.HighLowAverageResults = new ArrayList<Double[]>(stocksToAnalyze.length);
		this.HighPercentagesResults = new ArrayList<Double[]>(2000);
		this.LowPercentagesResults = new ArrayList<Double[]>(2000);

	}


	// Initializers
	private void initDataGetters() {

		for (String s : Analyzer.stocksToAnalyze) {

			if (DEBUG) System.out.printf("		Initializing DataGetter for stock: %s.\n", s);

			DataGetter newGetter = new DataGetter(s);
			this.dataGetters.add(newGetter);
			new Thread(newGetter).start();

		}

	}

	public static ArrayList<String> populateStocksToAanalyzeWithStaticArray (String[] arrayIn) {

		ArrayList<String> arrayListToReturn = new ArrayList<String>(arrayIn.length);

		for (int i=0; i < arrayIn.length; i++) {
			arrayListToReturn.add(arrayIn[i]);
		}

		return arrayListToReturn;

	}


	// Main Run Methods
	public static void main(String[] args) {

		System.out.println("Running Bollinger Analyzer.");

		if (mainAnalyzer == null) mainAnalyzer = new Analyzer();

		new Thread(mainAnalyzer).start();

	}

	public void run() {

		if (Analyzer.stocksToAnalyze.length == 0) return;		// No stocks? Done!

		this.initDataGetters();				// Creates independent "getters" that go online, pull data. Separate threads.
		this.waitForOnlineData();			// Pull data 		
		this.waitForSimulationResults();	// Simulate trading

	}

	// Data Averaging Method
	private void waitForOnlineData() {

		while (true) {		// Waiting for online data

			synchronized (readyCount) {								// Wait for all getters to complete.

				if (readyCount.equals(readyLimit)) {

					System.out.println("All results pulled! Proceed.");

					this.averageData();
					this.plotAllResults();
					this.dispatchTradingSimulation();

					break;
				}

			}

		}

	}
	
	private void averageData() {

		// Do work with data here.
		System.out.println("Let's average all data!");

		double runningSumHigh=0;
		double runningSumLow=0;

		for (int i = 0; i < HighLowAverageResults.size(); i++) {
			runningSumHigh += HighLowAverageResults.get(i)[0];
			runningSumLow += HighLowAverageResults.get(i)[1];
		}

		Analyzer.finalUpperPercentDistance = runningSumHigh / HighLowAverageResults.size();
		Analyzer.finalLowerPercentDistance = runningSumLow / HighLowAverageResults.size();

		System.out.printf("Average high percent: %3.3f%%\n", Analyzer.finalUpperPercentDistance);
		System.out.printf("Average low percent: %3.3f%%\n", Analyzer.finalLowerPercentDistance);

	}


	// Plotting Methods
	@SuppressWarnings("unused")
	private void plotAllResults() {

		this.plotScatterResults(HighPercentagesResults, "Upper");
		this.plotScatterResults(LowPercentagesResults, "Lower");
		this.plotCurveResults(HighPercentagesResults, "Upper");
		this.plotCurveResults(LowPercentagesResults, "Lower");

	}

	private void plotScatterResults(ArrayList<Double[]> arrayListIn, String upperOrLowerBound) {

		XYSeriesCollection xySeries = this.populateScatterSeriesCollection(arrayListIn, upperOrLowerBound);

		String title = "Distances from " + upperOrLowerBound + " RSI Band at Reversals";
		String xAxisLabel = "Days since January 1st, 2000";
		String yAxisLabel = "Percentage (of Closing Price) Distance";

		JFreeChart stockChart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, xySeries);

		this.addAverageMarker(stockChart, upperOrLowerBound, "Scatter");

		ValueAxis yAxis = stockChart.getXYPlot().getRangeAxis();
		yAxis.setUpperBound(50);

		ChartFrame frame = new ChartFrame("Scatter Plots", stockChart);
		frame.pack();
		frame.setVisible(true);

	}

	private void addAverageMarker (JFreeChart stockChart, String upperOrLowerBound, String scatterOrCurvePlot) {

		ValueMarker marker;

		if (upperOrLowerBound.equals("Upper")) {
			marker = new ValueMarker(finalUpperPercentDistance);
		}

		else {
			marker = new ValueMarker(finalLowerPercentDistance);
		}

		marker.setPaint(Color.black);
		XYPlot plot = (XYPlot) stockChart.getPlot();

		if (scatterOrCurvePlot.equals("Scatter")) {
			plot.addRangeMarker(marker);
		}

		if (scatterOrCurvePlot.equals("Curve")) {
			plot.addDomainMarker(marker);
		}

	}

	private XYSeriesCollection populateScatterSeriesCollection(ArrayList<Double[]> arrayListIn, String upperOrLowerBound) {

		XYSeriesCollection returnSeries = new XYSeriesCollection();

		for (int i=0; i < stocksAnalyzed.size(); i++) {

			Double[] arrayIn = arrayListIn.get(i);
			String stockName = stocksAnalyzed.get(i);

			XYSeries ScatterDataset = this.fillScatterValuesWithArrayForStock(arrayIn, stockName);
			returnSeries.addSeries(ScatterDataset);

		}

		return returnSeries;

	}

	private void plotCurveResults(ArrayList<Double[]> arrayListIn, String upperOrLowerBound) {

		XYSeriesCollection xySeries = this.populateCurveSeriesCollection(arrayListIn, upperOrLowerBound);
		String title = "Frequency of Distances from " + upperOrLowerBound + " RSI Band at Reversals";
		String xAxisLabel = "Percentage of Closing Price Away From RSI Band";
		String yAxisLabel = "Frequency";

		JFreeChart stockChart = ChartFactory.createScatterPlot(title, xAxisLabel, yAxisLabel, xySeries);

		ValueAxis xAxis = stockChart.getXYPlot().getDomainAxis();
		xAxis.setUpperBound(50);

		this.addAverageMarker(stockChart, upperOrLowerBound, "Curve");

		ChartFrame frame = new ChartFrame("Scatter Plots", stockChart);
		frame.pack();
		frame.setVisible(true);

	}

	private XYSeriesCollection populateCurveSeriesCollection(ArrayList<Double[]> arrayListIn, String upperOrLowerBound) {

		XYSeriesCollection returnSeries = new XYSeriesCollection();		// XYSeriesCollection adheres to the interface XYDataset

		for (int i=0; i < stocksAnalyzed.size(); i++) {

			HashMap<Double, Integer> frequencyData = this.fillFrequencyHashMapFromDoubleArray(arrayListIn.get(i));
			String stockName = stocksAnalyzed.get(i);

			XYSeries CurveDataset = this.fillFrequencyValuesWithHashMap(frequencyData, stockName);
			returnSeries.addSeries(CurveDataset);

		}

		return returnSeries;
	}

	private XYSeries fillScatterValuesWithArrayForStock(Double[] arrayIn, String stockName) {

		XYSeries ScatterDataset = new XYSeries(stockName, true, true);

		for (int x=0; x < arrayIn.length; x++) {
			ScatterDataset.add((double)x, arrayIn[x]);
		}

		return ScatterDataset;

	}

	private HashMap<Double, Integer> fillFrequencyHashMapFromDoubleArray(Double[] arrayIn) {

		HashMap<Double, Integer> returnHashMap = new HashMap<Double, Integer>();

		for (int j=0; j <arrayIn.length; j++) {

			Double currentEntry = arrayIn[j];

			if (currentEntry == null) continue;

			else {

				Double roundedEntry = Math.round(currentEntry * 10.0) / 10.0;

				if (returnHashMap.get(roundedEntry) == null) {
					returnHashMap.put(roundedEntry, new Integer(0));
				}

				else {
					Integer existingFrequency = returnHashMap.get(roundedEntry);
					existingFrequency++;
					returnHashMap.put(roundedEntry, existingFrequency);
				}

			}

		}

		return returnHashMap;
	}

	private XYSeries fillFrequencyValuesWithHashMap(HashMap<Double, Integer> hashmapIn, String stockName) {

		XYSeries frequencyDataset = new XYSeries(stockName, true, true);

		for (Map.Entry<Double, Integer> entry : hashmapIn.entrySet()) {

			Double x = entry.getKey();
			Double y = (double)((entry.getValue()).intValue());

			frequencyDataset.add(x, y);

		}

		return frequencyDataset;

	}


	// Trading Simulation Methods
	private void waitForSimulationResults() {

		while (true) {		// Waiting for simulation data

			synchronized (simulationCount) {

				if (simulationCount.equals(simulationLimit)) {

					System.out.println("All trades simulated! Proceed.");

					Analyzer.finalAverageMoney = this.averageTradingResults();
					Double percentGained = 100 * (Analyzer.finalAverageMoney - Analyzer.startingMoney) / Analyzer.startingMoney;

					System.out.println("Completely done.");
					
					System.out.printf("Starting money: $%3.2f\n", Analyzer.startingMoney);
					System.out.printf("Final money: $%3.2f\n", Analyzer.finalAverageMoney);
					
					System.out.printf("Percent Earned: %3.2f%%\n",percentGained);
					
					System.out.println("Happy with results? Try adding a stock, and see if this performs better!");
					
					break;

				}

			}

		}

	}
	
	private void dispatchTradingSimulation() {		// Dispatch our getters for fast simulation

		for (DataGetter getter : dataGetters) {

			synchronized(getter.simulate) {
				getter.simulate = true;
			}

		}

	}

	private Double averageTradingResults() {

		Double runningMoneySum = 0.0;
		
		for (Double finalCashValueForCertainStock : this.averageFinalMoneyArray) {
			runningMoneySum += finalCashValueForCertainStock;
		}
		
		return (runningMoneySum / Analyzer.simulationLimit);

	}


}










