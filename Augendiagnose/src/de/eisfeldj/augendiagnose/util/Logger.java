package de.eisfeldj.augendiagnose.util;

import de.eisfeldj.augendiagnose.Application;
import android.util.Log;

/**
 * Utility class for debugging
 */
public class Logger {
	/**
	 * Make a log entry
	 */
	public static void log(String output) {
		Log.i(Application.TAG, output);
	}
}
