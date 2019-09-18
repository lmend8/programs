
/**
 * Web worker: an object of this class executes in its own new thread
 * to receive and respond to a single HTTP request. After the constructor
 * the object executes on its "run" method, and leaves when it is done.
 *
 * One WebWorker object is only responsible for one client connection. 
 * This code uses Java threads to parallelize the handling of clients:
 * each WebWorker runs in its own thread. This means that you can essentially
 * just think about what is happening on one client at a time, ignoring 
 * the fact that the entirety of the webserver execution might be handling
 * other clients, too. 
 *
 * This WebWorker class (i.e., an object of this class) is where all the
 * client interaction is done. The "run()" method is the beginning -- think
 * of it as the "main()" for a client interaction. It does three things in
 * a row, invoking three methods in this class: it reads the incoming HTTP
 * request; it writes out an HTTP header to begin its response, and then it
 * writes out some HTML content for the response content. HTTP requests and
 * responses are just lines of text (in a very particular format). 
 *
 **/

import java.net.Socket;
import java.lang.Runnable;
import java.io.*;
import java.util.Date;
import java.text.DateFormat;
import java.util.TimeZone;

public class WebWorker implements Runnable {

	/**
	 *  global variables
	 *  @param OK_status 
	 *  	string varible for the ok status 200
	 *  @param NOT_FOUND_STATUS 
	 *  	variable for the not found error 404
	 *  
	 */
	private String OK_STATUS = "HTTP/1.1 200 OK\n";
	private String NOT_FOUND_STATUS = "HTTP/1.1 404 Not Found\n";
	private Socket socket;
	private String htmlFilePath;

	/**
	 * Constructor: must have a valid open socket
	 **/
	public WebWorker(Socket s) {
		socket = s;
	}

	/**
	 * Worker thread starting point. Each worker handles just one HTTP 
	 * request and then returns, which destroys the thread. This method
	 * assumes that whoever created the worker created it with a valid
	 * open socket object.
	 **/
	public void run() {
		System.err.println("Handling connection...");
		try {
			InputStream  is = socket.getInputStream();
			OutputStream os = socket.getOutputStream();
			// check if we have a html file 
			if (readHTTPRequest(is)) {
				writeHTTPHeader(os,"text/html", true);
				writeContent(os, htmlFilePath, true);
			} else {
				writeHTTPHeader(os,"text/html", false);
				writeContent(os, htmlFilePath, false);
			}
			
			os.flush();
			socket.close();
		} catch (Exception e) {
			System.err.println("Output error: "+e);
		}
		System.err.println("Done handling connection.");
		return;
	}

	/**
	 * Read the HTTP request header.
	 **/
	private boolean readHTTPRequest(InputStream is) {
		String line;
		BufferedReader r = new BufferedReader(new InputStreamReader(is));
		boolean returnValue = false;
		
		while (true) {
			try {
				while (!r.ready())
					Thread.sleep(1);
				line = r.readLine();

				// In here I have to intercept referer to know the relative path,
				// if the path doesn't exist return 404.
				if (line.contains("GET")) {
					String[] tokens = line.split(" ");
					htmlFilePath = tokens[1];
					// remove the first slash since this is a relative path
					htmlFilePath = htmlFilePath.replaceFirst("/", "");
					System.out.println("Path=" + htmlFilePath);
					
					// check if file exists
					File file = new File(htmlFilePath);
					if (file.exists())
						returnValue = true;
				}
				
				System.out.println("Request line: (" + line + ")");// this should be System.out.printline
				if (line.length() == 0)
					break;
			} catch (Exception e) {
				System.err.println("Request error: " + e);
				break;
			}
		}
		
		return returnValue;
	}

	/**
	 * Write the HTTP header lines to the client network connection.
	 * 
	 * @param os
	 *            is the OutputStream object to write to
	 * @param contentType
	 *            is the string MIME content type (e.g. "text/html")
	 **/
	private void writeHTTPHeader(OutputStream os, String contentType, boolean okStatus) throws Exception {
		Date d = new Date();
		DateFormat df = DateFormat.getDateTimeInstance();
		df.setTimeZone(TimeZone.getTimeZone("GMT"));

		if (okStatus)
			os.write(OK_STATUS.getBytes());
		else
			os.write(NOT_FOUND_STATUS.getBytes());

		//System.out.println("This should be 404"); debuggin purposes. 
		os.write("Date: ".getBytes());
		os.write((df.format(d)).getBytes());
		os.write("\n".getBytes());
		os.write("Server: Jon's very own server\n".getBytes());
		// os.write("Last-Modified: Wed, 08 Jan 2003 23:11:55 GMT\n".getBytes());
		// os.write("Content-Length: 438\n".getBytes());
		os.write("Connection: close\n".getBytes());
		os.write("Content-Type: ".getBytes());
		os.write(contentType.getBytes());
		os.write("\n\n".getBytes()); // HTTP header ends with 2 newlines
		return;
	}

	/**
	 * Write the data content to the client network connection. This MUST be done
	 * after the HTTP header has been written out.
	 * 
	 * @param os
	 *            is the OutputStream object to write to
	 * @param okStatus
	 * 			boolean to check if we have an ok status
	 **/
	private void writeContent(OutputStream os, String relativePath, boolean okStatus) throws Exception {
		if (okStatus) {
			/// return the html file indicated in the path
			//System.out.println("Relative path=" + relativePath);// print the relative path. 
			InputStream is = new FileInputStream(relativePath);
			BufferedReader r = new BufferedReader(new InputStreamReader(is));
			String line = r.readLine();
			
			while (line != null) {	
				// in here I need to search for specific tags and replace them with String.replace
				//System.out.println("line=" + line); debugging
				Date date = new Date();
				String dateStr = date.toString();
				String serverName = "Luis Server";
				//System.out.println("Date = " + date); debugging
				//System.out.println("ServerName = " + serverName ); debugging
				// replace the tag with date and server name 
				line = line.replace("<cs371date>", dateStr);
				line = line.replace("<cs371server>" , serverName);
				os.write(line.getBytes());
				line = r.readLine();
			}
		} else {
			os.write("<html><head></head><body>\n".getBytes());
			os.write("<h3>File not found</h3>\n".getBytes());
			os.write("</body></html>\n".getBytes());
		}
	}

} // end class
