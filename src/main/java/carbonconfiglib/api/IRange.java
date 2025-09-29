package carbonconfiglib.api;

public interface IRange
{
	public static class IntegerRange implements IRange {
		final int min;
		final int max;
		
		public IntegerRange(int min, int max) {
			this.min = min;
			this.max = max;
		}
		
		public int min() { return min; }
		public int max() { return max; }
	}
	
	public static class DoubleRange implements IRange {
		final double min;
		final double max;
		
		public DoubleRange(double min, double max) {
			this.min = min;
			this.max = max;
		}
		
		public double min() { return min; }
		public double max() { return max; }
	}
}
