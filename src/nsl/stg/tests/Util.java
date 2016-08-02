package nsl.stg.tests;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import android.filterfw.geometry.Point;
import android.filterfw.geometry.Rectangle;

public class Util {
	public static void log(Object obj) {
		long ts = System.currentTimeMillis();
		System.out.println(ts + ": " + obj);
		System.out.flush();
	}

	public static void err(Object obj) {
		System.err.println(obj.toString());
	}

	public static void log2File(Object obj) {
		if (handle != null) {
			handle.println(obj);
		}
	}

	private static PrintWriter handle;
	private static String fileName;

	public static void openFile(String fn, boolean append) {
		try {
			fileName = fn;
			handle = new PrintWriter(new BufferedWriter(new FileWriter(fileName, append)));
		} catch (IOException e) {
			fileName = null;
			handle = null;
		}
	}

	public static void closeFile(String fn) {
		if (fileName != null && fileName.equals(fn)) {
			handle.close();
		}
	}
	
	//Ruiyi He Add method for Rectangle overlapping check
	public static boolean isContain(Rectangle r1, Rectangle r2){
		Point centerR1 = r1.center();
		Point centerR2 = r2.center();
		float honrizontalDist = Math.abs(centerR1.x - centerR2.x);
		float verticalDist = Math.abs(centerR1.y - centerR2.y);
		if (honrizontalDist < (r1.getWidth()/2 + r2.getWidth()/2) && verticalDist < (r1.getHeight()/2 + r2.getHeight()/2)){
			return true;
		}
		return false;
	}
}
