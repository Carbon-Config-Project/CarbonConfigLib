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
		public int length() { return max - min; }
	}
	
	public static class LongRange implements IRange {
		final long min;
		final long max;
		
		public LongRange(long min, long max) {
			this.min = min;
			this.max = max;
		}
		
		public long min() { return min; }
		public long max() { return max; }
		public long length() { return max - min; }
	}
	
	public static class FloatRange implements IRange {
		final float min;
		final float max;
		
		public FloatRange(float min, float max) {
			this.min = min;
			this.max = max;
		}
		
		public float min() { return min; }
		public float max() { return max; }
		public float length() { return max - min; }
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
		public double length() { return max - min; }
	}
}
