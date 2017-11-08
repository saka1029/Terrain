package saka1029.terrain;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Log {

    private Log() {}
    
    static {
        System.setProperty("java.util.logging.SimpleFormatter.format",
            "%1$tFT%1$tT.%1$tL %4$s %5$s %6$s%n");
    }

	public static void info(Logger logger, String format, Object... args) {
	    if (logger.isLoggable(Level.INFO))
	        logger.info(String.format(format, args));
	}

	public static void fine(Logger logger, String format, Object... args) {
	    if (logger.isLoggable(Level.FINE))
	        logger.fine(String.format(format, args));
	}

	public static void finest(Logger logger, String format, Object... args) {
	    if (logger.isLoggable(Level.FINEST))
	        logger.finest(String.format(format, args));
	}

	public static void warning(Logger logger, String format, Object... args) {
	    if (logger.isLoggable(Level.WARNING))
	        logger.warning(String.format(format, args));
	}

	public static void fatal(Logger logger, String format, Object... args) {
	    if (logger.isLoggable(Level.SEVERE))
	        logger.severe(String.format(format, args));
	}

}
