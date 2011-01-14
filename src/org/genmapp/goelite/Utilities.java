package org.genmapp.goelite;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Vector;


/**
 * Random file utilites
 *
 */
class Utilities {
	// replaces DOS-style carriage-returns with spaces: needed when sending a
	// text file from DOS -> UNIX
	public static byte[] replaceCR(byte[] bytes) {
		byte[] newBytes = new byte[bytes.length];

		int j = 0;
		for (int i = 0; i < bytes.length; i++) {
			if ('\r' != bytes[i]) {
				newBytes[j] = bytes[i];
				j++;
			}
		}
		return (newBytes);
	}
	public static Vector<String> getFileContents(URL u)
	throws MalformedURLException, IOException {
		Vector<String> contents = new Vector<String>();
		
		InputStream is = null;
		BufferedReader bf = null;
		
		// ----------------------------------------------//
		// Step 3: Open an input stream from the
		// url. //
		// ----------------------------------------------//
		
		is = u.openStream(); // throws an
		// IOException
		//-------------------------------------------------------------/
		// /
		
		// Step 4: //
		//-------------------------------------------------------------/
		// /
		// Convert the InputStream to a buffered
		// DataInputStream. //
		// Buffering the stream makes the reading
		// faster; the //
		// readLine() method of the DataInputStream
		// makes the reading //
		// easier. //
		//-------------------------------------------------------------/
		// /
		
		bf = new BufferedReader(new InputStreamReader(is));
		
		//------------------------------------------------------------//
		// Step 5: //
		//------------------------------------------------------------//
		// Now just read each record of the input
		// stream, and print //
		// it out. Note that it's assumed that this
		// problem is run //
		// from a command-line, not from an
		// application or applet. //
		//------------------------------------------------------------//
		
		String s = null;
		while ((s = bf.readLine()) != null) {
			contents.add(s);
		}
		
		return (contents);
	}

	public static byte[] getBytesFromFile(File file) throws IOException {
		InputStream is = new FileInputStream(file);

		// Get the size of the file
		long length = file.length();

		// You cannot create an array using a long type.
		// It needs to be an int type.
		// Before converting to an int type, check
		// to ensure that file is not larger than Integer.MAX_VALUE.
		if (length > Integer.MAX_VALUE) {
			// File is too large
		}

		// Create the byte array to hold the data
		byte[] bytes = new byte[(int) length];

		// Read in the bytes
		int offset = 0;
		int numRead = 0;
		while (offset < bytes.length
				&& (numRead = is.read(bytes, offset, bytes.length - offset)) >= 0) {
			offset += numRead;
		}

		// Ensure all the bytes have been read in
		if (offset < bytes.length) {
			throw new IOException("Could not completely read file "
					+ file.getName());
		}

		// Close the input stream and return bytes
		is.close();
		return bytes;
	}

	public static long countLinesInFile(String filename) {
		// Count the number of lines in the specified file, and
		// print the number to standard output. If an error occurs
		// while processing the file, print an error message instead.
		// Two try...catch statements are used so I can give a
		// different error message in each case.
		long lineCount = 0;
		try {
			BufferedReader br = new BufferedReader(new FileReader(filename));
			String line = "";
			while ((line = br.readLine()) != null) {
				lineCount++;
			}
		} catch (Exception e) {
			return (-1);
		}
		return (lineCount);
	} // end countLines()

} 