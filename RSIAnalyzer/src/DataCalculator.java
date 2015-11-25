//
// Ruslan Ardashev, Duke University - Pratt School of Engineering
// Class of 2015
//
// RSI Analyzer
//
// StockEntry
// 2014, September
//
// Uses Lazy Instantiation... a lot of it. :)
//


import java.lang.Math;

public class DataCalculator {

	public static int Interval = 14;	// Standard RSI interval

	public DataGetter parent;
	public Double[] upperDistPercent;	// Size of sizeNewArrays, 0 if no pivot and not dealing with upper, else, has a value
	public Double[] lowerDistPercent;	// Size of sizeNewArrays, 0 if no pivot and not dealing with lower, else, has a value
	public Double[] totalAverage;		// index 0: average upper bound percent difference, index 1: average lower bound percent difference

	private int sizeNewArrays;

	private double[] changeBetweenDays;	// price difference between closing(i) and closing(i-1), index 0 is 0.
	private boolean[] didGainsOccur;	// true if Close(i) - Open(i) > 0
	private boolean[] didLossesOccur;	// true if Close(i) - Open(i) < 0
	private double[] upwardsChanges;	// 0.0 if loss day, (double value) if gain days
	private double[] downwardsChanges;	// 0.0 if loss day, (double value) if gain days

	private double[] EMAGains;			// 14 days, incorporates 0 and non-0 values of upwardsChanges
	private double[] EMALosses;			// 14 days, incorporates 0 and non-0 values of downwardsChanges

	private double[] RSI;

	private boolean[] pivotPoints;
	private double[] upperDist;
	private double[] lowerDist;

	DataCalculator (DataGetter dataGetter) {

		this.parent = dataGetter;

	}


	public void performCalculations() {

		if (sizeNewArrays == 0) this.sizeNewArrays = parent.stockEntries.size();

		if (Analyzer.DEBUG) System.out.println("	Launching DataCalculator for: "+this.parent.stock);
		this.dispatchRSIBandCalculations();
		this.pivotLocations();
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

	public void dispatchRSIBandCalculations() {

		if (this.RSI == null) this.RSI = new double[this.sizeNewArrays];

		this.calculateChangeInPriceAcrossEachDay();
		this.determineIfGainsOrLossesDays();
		this.populateGainsLossesArrays();
		this.calculateEMAs();
		this.calculateRSIBands();

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

	// Helper Methods
	private void calculateChangeInPriceAcrossEachDay() {

		if (this.changeBetweenDays == null) this.changeBetweenDays = new double[this.sizeNewArrays];

		for (int i=1; i < this.sizeNewArrays; i++) {

			changeBetweenDays[i] = this.parent.stockEntries.get(i).Close - this.parent.stockEntries.get(i-1).Close;

		}

	}

	private void determineIfGainsOrLossesDays() {

		// Lazy instantiation
		if (this.didGainsOccur == null) this.didGainsOccur = new boolean[this.sizeNewArrays]; 
		if (this.didLossesOccur == null) this.didLossesOccur = new boolean[this.sizeNewArrays]; 

		for (int i=0; i < this.sizeNewArrays ; i++) {

			// Can be done in less lines of code. Expanded for clarity.

			double openPrice = this.parent.stockEntries.get(i).Open;
			double closePrice = this.parent.stockEntries.get(i).Close;

			if (closePrice > openPrice) {
				didGainsOccur[i] = true; 
				didLossesOccur[i] = false;
			}

			else {
				didGainsOccur[i] = false; 
				didLossesOccur[i] = true;
			}

		}

	}

	private void populateGainsLossesArrays() {

		if (upwardsChanges == null) upwardsChanges = new double[this.sizeNewArrays]; 
		if (downwardsChanges == null) downwardsChanges = new double[this.sizeNewArrays]; 

		for (int i = 0; i < this.sizeNewArrays; i++) {

			if (didGainsOccur[i]) {

				upwardsChanges[i] = Math.abs(changeBetweenDays[i]);
				downwardsChanges[i] = 0.0;

			}

			if (didLossesOccur[i]) {

				downwardsChanges[i] = Math.abs(changeBetweenDays[i]);
				upwardsChanges[i] = 0.0;

			}

		}

	}

	private void calculateEMAs() {

		if (this.EMAGains == null) EMAGains = new double[this.sizeNewArrays]; 
		if (this.EMALosses == null) EMALosses = new double[this.sizeNewArrays]; 

		//		EMA = Price(t) * k + EMA(t-1) * (1 – k)
		//		t = today, N = number of days in EMA, k = 2/(N+1)
		//		Courtesy of http://www.iexplain.org/ema-how-to-calculate/

		double k = 2 / (Interval+1);

		for (int i = (Interval-1); i < this.sizeNewArrays; i ++) {
			
			double runningSumUpper = 0.0, runningSumLower = 0.0;

			if (i == Interval-1) {									// First run

				for (int n = 0; n < (Interval-1); n++) {			

					runningSumUpper += upwardsChanges[n];
					runningSumLower += downwardsChanges[n];
					
					System.out.println("Upwards: "+upwardsChanges[n]);
//					System.out.println();
				
				}
				
				try {
					this.wait(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				
				// First run is a simple average of first 14 or whatever days
				EMAGains[i] = runningSumUpper / (Interval-1);
				EMALosses[i] = runningSumLower / (Interval-1);

			}

			else {													// All other runs.
				
				// All subsequent runs are k * price[i] + (1-k) EMA(i-1)
				EMAGains[i] = (k) * this.parent.stockEntries.get(i).Close + (1-k) * EMAGains[i-1]; 
				EMALosses[i] = (k) * this.parent.stockEntries.get(i).Close + (1-k) * EMALosses[i-1]; 
				
			}
			
		}

	}

	private void calculateRSIBands() {

		//		Formulas:
		//		RSI = 100 - 100/(1 + RS)
		//		RS = 14-day EMA of upday closing gains / 14-day EMA of downday closing losses
		//		Courtesy of http://www.iexplain.org/rsi-how-to-calculate-it/
		
		double RS = 0.0;
		
		for (int i = (Interval-1); i < this.sizeNewArrays; i ++) {

			System.out.println("EmaGains["+i+"]: "+EMAGains[i]);
			System.out.println("EmaLosses["+i+"]: "+EMALosses[i]);
			RS = this.EMAGains[i] / this.EMALosses[i];
			RSI[i] = 100 - 100 / (1 + RS);
			System.out.println("Calculator for: "+this.parent.stock+", RSI: "+RSI[i]+", for day: "+i);

		}

	}

}







