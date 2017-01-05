package com.gokulnc.ums_universal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;

public class RootShell {

	ProcessBuilder builder;
	Process process;
	BufferedReader reader;
	BufferedWriter writer;
	
	 Boolean getNewShell() throws IOException {
		
		 builder = new ProcessBuilder("su");
			builder.redirectErrorStream(true);
			process = builder.start();
			
			reader = new BufferedReader (new InputStreamReader(process.getInputStream()));
			writer = new BufferedWriter(new OutputStreamWriter(process.getOutputStream()));
			
			return true;
		 
	}
	 
	 String execute(String cmd) throws IOException {
		 if(process == null || writer == null) {
			 return null;
		 }
		 
		 writer.write("(("+cmd+") && echo --EOF--) || echo --EOF--\n");
		 writer.flush();
		 //refer http://stackoverflow.com/questions/3643939/java-process-with-input-output-stream
			 
		 String buffer = reader.readLine();
		 String output = "";
		 
		 while( buffer!=null && !buffer.trim().equals("--EOF--") ) {
			if(output != "") output += "\n";
			output += buffer;
			buffer = reader.readLine();
		 }
		 
		 if(output != "") return output;
		 else return null;
		 
	 }
	
	Boolean close() throws IOException  {
		execute("exit");
		process.destroy();
		reader.close();
		writer.close();
		return true;
	}

	/* I could have done like this: http://forum.xda-developers.com/showthread.php?t=2226664
	But I don't wish to close the shell each time..

	I have to check how RootTools does it without dropping the shell so nicely..
	"Why not use RootTools then?" Nah, I waan't comfortable with getting the output. */
}
