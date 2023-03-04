package carbonconfiglib;

import java.util.Arrays;

public class Helpers {
	public static boolean validateString(CharSequence s) {
		return s == null || s.length() == 0 || s.charAt(0) <= ' ' || s.charAt(s.length() - 1) <= ' ';
	}

	public static String generateIndent(int level) {
		char[] arr = new char[level * 2];
		Arrays.fill(arr, ' ');
		return new String(arr);
	}
	
	public static String[] validateComments(String[] inputs) {
		if(inputs == null) return null;
		for(int i = 0;i<inputs.length;i++) {
			if(inputs[i] == null) inputs[i] = "";
		}
		return inputs;
	}
	
	public static String[] trimArray(String[] array) {
		for(int i = 0,m=array.length;i<m;i++) {
			array[i] = array[i].trim();
		}
		return array;
	}
	
	public static int clamp(int value, int min, int max) {
		return Math.min(Math.max(value, min), max);
	}

	public static double clamp(double value, double min, double max) {
		return Math.min(Math.max(value, min), max);
	}
}
