package carbonconfiglib.api;

/**
 * Copyright 2026 Speiger, Meduris
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
		public long length() { return (long)max - (long)min; }
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
