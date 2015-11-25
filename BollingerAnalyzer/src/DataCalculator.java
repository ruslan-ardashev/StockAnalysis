//
// Ruslan Ardashev, Duke University - Pratt School of Engineering
// Class of 2015
//
// Bollinger Analyzer
//
// StockEntry
// 2014, September
//
// Uses Lazy Instantiation... a lot of it. :)
//


import java.lang.Math;

public class DataCalculator {
	
	public static int Interval = 20;
	
	public DataGetter parent;
	public Double[] upperDistPercent;	// Size of sizeNewArrays, 0 if no pivot and not dealing with upper, else, has a value
	public Double[] lowerDistPercent;	// Size of sizeNewArrays, 0 if no pivot and not dealing with lower, else, has a value
	public Double[] totalAverage;		// index 0: average upper bound percent difference, index 1: average lower bound percent difference
	
	private int sizeNewArrays;
	private double[] SMA;
	
	public double[] UpperBBand;
	public double[] LowerBBand;
	
	private boolean[] pivotPoints;
	private double[] upperDist;
	private double[] lowerDist;
	
	DataCalculator (DataGetter dataGetter) {
		
		this.parent = dataGetter;
		
	}
	
	public void performCalculations() {
		
		if (sizeNewArrays == 0) this.sizeNewArrays = parent.stockEntries.size();
		
		if (Analyzer.DEBUG) System.out.println("	Launching DataCalculator for: "+this.parent.stock);
		this.calculateBollingerBands();
		this.pivotLocations();
		this.distancesFromBandsAtReversals();
		this.percentageDistancesAtReversals();
		this.averageResults();
		
	}
	
	public void averageResults() {
		
		int upperCount = 0;
		int lowerCount = 0;
		
		double upperRunningSum = 0;
		double lowerRunningSum = 0;
		
		for (int i= (Interval-1); i < sizeNewArrays; i++) {
			
			if (upperDistPercent[i] != null) {
				upperCount++;
				upperRunningSum += upperDistPercent[i];
			}
			
			if (lowerDistPercent[i] != null) {
				lowerCount++;
				lowerRunningSum += lowerDistPercent[i];
			}
			
		}
		
		double averageResultUpperPercent = upperRunningSum / (float)upperCount;
		double averageResultLowerPercent = lowerRunningSum / (float)lowerCount;
		
		if (this.totalAverage == null) this.totalAverage = new Double[2];
		
		this.totalAverage[0] = averageResultUpperPercent;
		this.totalAverage[1] = averageResultLowerPercent;
		
	}
	
	public void percentageDistancesAtReversals() {
		
		if (upperDistPercent == null) upperDistPercent = new Double[sizeNewArrays];
		if (lowerDistPercent == null) lowerDistPercent = new Double[sizeNewArrays];
		
		for (int i = (Interval-1); i < sizeNewArrays; i++) {
			
			if (pivotPoints[i]) {
				
				double parentClose = this.parent.stockEntries.get(i).Close;
				
				if (upperDist[i] != 0) {
					upperDistPercent[i] = 100 * upperDist[i] / parentClose;
//					System.out.println("Percent dist upper: "+upperDistPercent[i]+" %");
				}
				
				else if (lowerDist[i] != 0) {
					lowerDistPercent[i] = 100 * lowerDist[i] / parentClose;
//					System.out.println("Percent dist lower: "+lowerDistPercent[i]+" %");
				}
				
			}
			
		}
		
	}
	
	public void distancesFromBandsAtReversals() {

		if (upperDist == null) upperDist = new double[sizeNewArrays]; 
		if (lowerDist == null) lowerDist = new double[sizeNewArrays]; 
		
		for (int i= (Interval-1); i < sizeNewArrays; i++) {
			
			double distUpper = Math.abs(UpperBBand[i] - this.parent.stockEntries.get(i).Close);
			double distLower = Math.abs(LowerBBand[i] - this.parent.stockEntries.get(i).Close);
			
			if (distUpper < distLower) {
				upperDist[i] = distUpper;
			} 
			else if (distLower < distUpper) {
				lowerDist[i] = distLower;
			}
			
		}
		
		return;

	}

	public void calculateBollingerBands() {

		if (Analyzer.DEBUG) System.out.println(parent.stock+" getter, number of stock entries here: " + parent.stockEntries.size());
		
		if (this.UpperBBand == null) this.UpperBBand = new double[sizeNewArrays];
		if (this.LowerBBand == null) this.LowerBBand = new double[sizeNewArrays];
		
		this.SMA = this.calculateSMA();

		double[] stdDeviation = this.calculateStdDev();
		
		for (int i = (Interval-1); i < sizeNewArrays; i ++) {
			
			this.UpperBBand[i] = this.SMA[i] + 2 * stdDeviation[i];
			this.LowerBBand[i] = this.SMA[i] - 2 * stdDeviation[i];
			
		}
		
		return;

	}

	public void pivotLocations() {
		
		boolean[] increasingArray = new boolean[sizeNewArrays];
		
		if (pivotPoints == null) pivotPoints = new boolean[sizeNewArrays]; 
		
		for (int i=1; i < this.parent.stockEntries.size(); i++) {
			
			if (this.parent.stockEntries.get(i).Close > this.parent.stockEntries.get(i-1).Close) {
				increasingArray[i] = true;
			} 
			else if (this.parent.stockEntries.get(i).Close < this.parent.stockEntries.get(i-1).Close) {
				increasingArray[i] = false;
			}
			
			if (increasingArray[i] != increasingArray[i-1]) {
				this.pivotPoints[i] = true;
			}
			
		}
		
	}
	
	private double[] calculateSMA() {

		double[] returnArray = new double[sizeNewArrays];

		for (int i = (Interval-1); i < sizeNewArrays; i++) {

			double runningSum = 0;

			for (int j = 0; j < Interval; j++) {
				runningSum = runningSum + parent.stockEntries.get(i-j).Close;
			}
			
			double averageForTwentyDays = runningSum / (double)Interval;
			
			returnArray[i] = averageForTwentyDays;

		}

		if (Analyzer.DEBUG) this.validateReturnArray(returnArray);
		
		return returnArray;

	}
	
	private double[] calculateStdDev() {
		
		double[] returnArray = new double[sizeNewArrays];
		
		for (int i = (Interval-1); i < sizeNewArrays; i++) {

			double runningStdDeviation = 0;

			for (int j = 0; j < Interval; j++) {
				double x_i_minus_mean = (parent.stockEntries.get(i-j).Close - SMA[i]);
				runningStdDeviation = runningStdDeviation + Math.pow(x_i_minus_mean, 2);
			}
			
			double averagedSumWithoutSqrt = runningStdDeviation / (double)Interval;
			double finalStdDev = Math.pow(averagedSumWithoutSqrt, 0.5);
			
			returnArray[i] = finalStdDev;

		}
		
		if (Analyzer.DEBUG) this.validateReturnArray(returnArray); 
		
		return returnArray;
		
	}
	
	private void validateReturnArray(double[] arrayToTest) {
		
		// If anything other than indices 0 through Interval-2 are null, error. Return false.
		// If everything is okay, return true.
		
		for (int i=0; i<arrayToTest.length; i++) {
			
			if ((i >= 0) && (i < (Interval-1))) {
				if (arrayToTest[i] != 0) {
					System.out.println("ERROR IN ValidateReturnArray().");
					return;
				}
			} 
			
			else {
				if (arrayToTest[i] == 0) {
					System.out.println("ERROR IN ValidateReturnArray().");
					return;
				}
			}
		}
		
	}
	
}







