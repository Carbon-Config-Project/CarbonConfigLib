package carbonconfiglib.utils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringJoiner;

import speiger.src.collections.objects.lists.ObjectArrayList;
import speiger.src.collections.objects.maps.interfaces.Object2ObjectMap;
import speiger.src.collections.objects.sets.ObjectOpenHashSet;

/**
 * Copyright 2023 Speiger, Meduris
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
public class Helpers {
	private static final String[] EMPTY = new String[0];

	public static boolean validateString(CharSequence s) {
		return s == null || s.length() == 0 || s.charAt(0) <= ' ' || s.charAt(s.length() - 1) <= ' ';
	}

	public static String firstLetterUppercase(String string) {
		if (string == null || string.isEmpty()) {
			return string;
		}
		String first = Character.toString(string.charAt(0));
		return string.replaceFirst(first, first.toUpperCase());
	}
	
	public static void ensureFolder(Path folder) {
		if (Files.notExists(folder)) {
			try { Files.createDirectories(folder); }
			catch(Exception e) { e.printStackTrace(); }
		}
	}
	
	public static void copyFile(Path from, Path to) {
		ensureFolder(to.getParent());
		try { Files.copy(from, to, StandardCopyOption.REPLACE_EXISTING); }
		catch(Exception e) { e.printStackTrace(); }
	}
	
	public static String generateIndent(int level) {
		char[] arr = new char[level * 2];
		Arrays.fill(arr, ' ');
		return new String(arr);
	}

	public static String[] validateComments(String[] inputs) {
		if (inputs == null) return null;
		for (int i = 0; i < inputs.length; i++) {
			if (inputs[i] == null) inputs[i] = "";
		}
		return inputs;
	}

	public static String[] splitArray(String value, String splitter) {
		return value.isEmpty() ? EMPTY : trimArray(value.split(splitter));
	}
	
	public static String[] splitCompound(String value) {
		if(value.isEmpty()) return EMPTY;
		return scanForElements(removeLayer(value, 1), ';', '[', ']'); 
	}
	
	public static Map<String, String> splitArguments(String[] entries, List<String> keys) {
		Map<String, String> namedArguments = Object2ObjectMap.builder().linkedMap();
		Set<String> comparator = new ObjectOpenHashSet<>(keys);
		for(int i = 0,m=entries.length;i<m;i++) {
			String[] entry = entries[i].split(":", 2);
			if(entry.length == 1 || !comparator.contains(entry[0])) {
				namedArguments.put(keys.get(i), entries[i].trim());
				continue;
			}
			namedArguments.put(entry[0], entry[1].trim());
		}
		return namedArguments;
	}
	
	public static String removeLayer(String value, int base) {
		boolean starts = value.startsWith("[");
		boolean ends = value.endsWith("]");
		return value.substring(starts ? 1 : 0, value.length() - ((ends ? 1 : 0) + base)); 
	}
	
	public static String[] splitCompoundArray(String value) {
		return scanForElements(value, ',', '[', ']');
	}
	
	public static String[] scanForElements(CharSequence text, char splitter, char opener, char closer) {
		List<String> result = new ObjectArrayList<>();
		StringBuilder builder = new StringBuilder(text.length());
		int recursion = 0;
		for(int i = 0,m=text.length();i<m;i++) {
			char value = text.charAt(i);
			if(value == opener) recursion++;
			else if(value == closer) recursion--;
			else if(value == splitter && recursion == 0) {
				result.add(builder.toString());
				builder.setLength(0);
				continue;
			}
			builder.append(value);
		}
		if(builder.length() > 0) result.add(builder.toString());
		
		return trimArray(result.toArray(String[]::new));
	}
	
	public static String mergeCompound(Map<String, String> entries, boolean newLine, int indentLevel) {
		String lineBreak = newLine ? "\n" : "";
		StringBuilder builder = new StringBuilder("[").append(lineBreak);
		String indent = newLine ? generateIndent(indentLevel+1) : "";
		for(Entry<String, String> entry : entries.entrySet()) {
			builder.append(indent).append(entry.getKey()).append(": ").append(entry.getValue()).append("; ").append(lineBreak);
		}
		if(builder.charAt(builder.length()-1) == ' ') builder.deleteCharAt(builder.length()-1);
		return builder.append("]").toString();
	}
	
	public static String mergeCompoundArray(List<String> list, boolean newLine, int indentLevel) {
		String indent = newLine ? generateIndent(indentLevel+1) : "";
		StringJoiner joiner = new StringJoiner(newLine ? ",\n" : ", ", "["+(newLine ? "\n" : ""), (newLine ? "\n"+generateIndent(indentLevel) : "")+"]");
		for(int i = 0,m=list.size();i<m;i++) {
			joiner.add(indent+list.get(i));
		}
		return joiner.toString();
	}
	
	public static String[] trimArray(String[] array) {
		for (int i = 0, m = array.length; i < m; i++) {
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
	
	public static ParseResult<String> parseString(String value) {
		return ParseResult.success(value);
	}
	
	public static ParseResult<Boolean> parseBoolean(String value) {
		try { return ParseResult.success(Boolean.parseBoolean(value)); }
		catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Boolean"); }
	}
	
	public static ParseResult<Integer> parseInt(String value) {
		try { return ParseResult.success(Integer.parseInt(value)); }
		catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Number"); }
	}

	public static ParseResult<Double> parseDouble(String value) {
		try { return ParseResult.success(Double.parseDouble(value)); } 
		catch (Exception e) { return ParseResult.error(value, e, "Couldn't parse Number"); }
	}
	
	public static <E extends Enum<E>> ParseResult<E> parseEnum(Class<E> enumClass, String value) {
		try { return ParseResult.success(Enum.valueOf(enumClass, value)); }
		catch (Exception e) { return ParseResult.error(value, e, "Enum isn't valid, Must be one of: "+Arrays.toString(toArray(enumClass))); }
	}
	
	public static <E extends Enum<E>> String[] toArray(Class<E> enumClass) {
		E[] array = enumClass.getEnumConstants();
		String[] values = new String[array.length];
		for(int i = 0,m=array.length;i<m;i++) {
			values[i] = array[i].name();
		}
		return values;
	}
}
