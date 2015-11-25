//
// Ruslan Ardashev, Duke University - Pratt School of Engineering
// Class of 2015
//
// Bollinger Analyzer
//
// StockEntry
// 2014, September
//

public class StockEntry {

	public String Date;
	public double Open, High, Low, Close;
	public int Volume;
	public double adjClose;
	
	public static String[] format = {"Date", "Open", "High", "Low", "Close", "Volume", "adjClose"};	// debugging purposes

	StockEntry (String[] input) {

		this.Date = input[0];
		this.Open = Double.parseDouble(input[1]);
		this.High =  Double.parseDouble(input[2]);
		this.Low   = Double.parseDouble(input[3]);
		this.Close = Double.parseDouble(input[4]);
		this.Volume = Integer.parseInt(input[5]);
		this.adjClose = Double.parseDouble(input[6]);

	}

}
