//
// Ruslan Ardashev, Duke University - Pratt School of Engineering
// Class of 2015
//
// Bollinger Analyzer
//
// DataGetter
// 2014, September
//


import java.util.*;
import java.net.*;
import java.io.*;

public class DataGetter implements Runnable {

	// Instance Variables
	public String stock;
	public Analyzer delegate;
	public DataCalculator calculator;
	public ArrayList<StockEntry> stockEntries;

	// Simulation Variables
	public Boolean simulate = false;							// Used to synchronize
	Double numberOfShares = new Double(0);						// Used as sim. params
	Double currentCash = new Double(Analyzer.startingMoney);
	Boolean canBuy = new Boolean(false);
	Boolean canSell = new Boolean(false);

	DataGetter (String stock) {

		this.stock = stock;
		this.delegate = Analyzer.mainAnalyzer;
		this.calculator = new DataCalculator(this);
		this.stockEntries = new ArrayList<StockEntry>(2000);

	}

	public void run() {

		this.pullData(this.stock);
		this.calculator.performCalculations();
		this.dispatchAnalysis();

		while (true) {

			synchronized(this.simulate) {

				if (this.simulate) {

					this.simulateTrading();
					break;

				}
			}

		}

	}

	// Pulling Data
	private synchronized void pullData (String symbol) {

		// Date, Open, High, Low, Close, Volume, Adj Close

		try {

			URL yahooFinanceURL = new URL("http://real-chart.finance.yahoo.com/table.csv?s=" + symbol + "&a=00&b=01&c=2000&d=08&e=01&f=2014&g=d");
			URLConnection yc = yahooFinanceURL.openConnection();
			BufferedReader in = new BufferedReader(new InputStreamReader(yc.getInputStream()));
			String inputLine;

			boolean firstRun = true;

			while ((inputLine = in.readLine()) != null) {

				if(firstRun) {			// Don't need first line of CSV output, it's formatting
					firstRun = false;
					inputLine = in.readLine();
				}

				String[] splitInfo = inputLine.split(",");
				StockEntry stockInfo = new StockEntry(splitInfo);	
				this.stockEntries.add(stockInfo);

			}
			in.close();

		} 

		catch (Exception ex) {

			// Stock failed. Don't wait.
			synchronized (Analyzer.readyLimit) {
				
				Analyzer.readyLimit--;					// Don't make main thread wait on a result that will never arrive
				Analyzer.simulationLimit--;				// Don't make main thread wait on a result that will never arrive
				
				String stockThatFailed = this.stock;

				for (int n = 0; n < Analyzer.stocksAnalyzed.size(); n++) {
					if (Analyzer.stocksAnalyzed.get(n).equals(stockThatFailed)) {
						Analyzer.stocksAnalyzed.remove(n);
						break;
					}
				}

			}

		}

		return; 

	}

	private void dispatchAnalysis() {

		synchronized (Analyzer.mainAnalyzer.HighLowAverageResults) {
			Analyzer.mainAnalyzer.HighLowAverageResults.add(this.calculator.totalAverage);
		}

		synchronized (Analyzer.mainAnalyzer.HighPercentagesResults) {
			Analyzer.mainAnalyzer.HighPercentagesResults.add(this.calculator.upperDistPercent);
		}

		synchronized (Analyzer.mainAnalyzer.LowPercentagesResults) {
			Analyzer.mainAnalyzer.LowPercentagesResults.add(this.calculator.lowerDistPercent);
		}

		synchronized(Analyzer.readyCount) {
			Analyzer.readyCount++;					// Synchronizes when all data is retrieved & ready to plot
		}

	}


	// Simulating Trading
	private void simulateTrading() {

		this.setInitialSimulationParameters();

		// Run through all trading days, use close prices
		for (int i=0; i < this.stockEntries.size(); i++) {

			Double currentStockPrice = this.stockEntries.get(i).Close;
			Double currentUpperBollingerBandValue = this.calculator.UpperBBand[i];
			Double currentLowerBollingerBandValue = this.calculator.LowerBBand[i];

			// Use Analyzer's concluded average points for upper and lower to sell
			
			if (this.canBuy) {							// Closing price within lower bound?

				this.assessBuy(currentStockPrice, currentLowerBollingerBandValue, i);
				
			}

			else if (this.canSell) {					// Closing price approaching upper bound?

				this.assessSell(currentStockPrice, currentUpperBollingerBandValue, i);

			}
			
			if (i == (this.stockEntries.size()-1)) {	// Last day, convert valuations to cash for assessing results
				
				if (this.canSell) {
					this.SELL(i);
				}
				
			}

		}
		
		this.reportResults();
		
	}
	
	private void reportResults(){
		
		System.out.printf("%s trades' final value = $%3.2f\n", this.stock, this.currentCash);
		
		synchronized(Analyzer.simulationCount) {
			
			Analyzer.simulationCount++;
			Analyzer.mainAnalyzer.averageFinalMoneyArray.add(this.currentCash);
			
		}
		
	}
	
	private void assessBuy(Double currentStockPrice, Double currentLowerBollingerBandValue, int stockEntryId) {
		
		Double percentDifferenceLowerBBand_ClosingPrice = 100 * (currentStockPrice - currentLowerBollingerBandValue) / currentStockPrice;
		
		if (percentDifferenceLowerBBand_ClosingPrice < Analyzer.finalLowerPercentDistance) {		// BUY

			this.BUY(stockEntryId);
			
		}
		
	}
	
	private void assessSell(Double currentStockPrice, Double currentUpperBollingerBandValue, int stockEntryId) {
		
		Double percentDifferenceUpperBBand_ClosingPrice = 100 * (currentUpperBollingerBandValue - currentStockPrice) / currentStockPrice;

		if (percentDifferenceUpperBBand_ClosingPrice < Analyzer.finalUpperPercentDistance) {		// SELL

			this.SELL(stockEntryId);

		}
		
	}
	
	private void BUY(int stockEntryId) {
		
		this.numberOfShares = this.currentCash / this.stockEntries.get(stockEntryId).Close;
		this.currentCash = 0.0;
		this.canBuy = false;
		this.canSell = true;
		
	}
	
	private void SELL(int stockEntryId) {
		
		this.currentCash = this.numberOfShares * this.stockEntries.get(stockEntryId).Close;
		this.numberOfShares = 0.0;
		this.canBuy = true;
		this.canSell = false;
		
	}

	private void setInitialSimulationParameters() {

		this.canBuy = false;
		this.canSell = true;
		this.numberOfShares = this.currentCash / this.stockEntries.get(0).Close;
		this.currentCash = 0.0;

	}

}












