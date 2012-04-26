package org.vivoweb.harvester.util;

import java.io.File;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;

/**
 * @author kuppuraj, Mayank Saini
 *
 */
public class XMLGrep {
	
	/**
	 * 
	 */
	protected static Logger log = LoggerFactory.getLogger(XMLGrep.class);
	/**
	 * Input src directory
	 */
	private String src;
	/**
	 * Input dest directory
	 */
	private String dest = "";
	
	/**
	 * xpath expression to filter files
	 */
	private String exp;
	
	/**
	 * Constructor
	 * @param src directory to read files from
	 * @param dest directory to move files from
	 * @param xpath expression to filter
	 */
	public XMLGrep(String src, String dest, String value, String name) {
		this.src = src;
		this.dest = dest;
		//"//author[. = 'Kurt Cagle']"
		//this.exp = (name == null ) ? "" : "//" + name;
		//if(name != null && value == null) this.exp = "//" + name + "[. = '']";
		//this.exp = (value == null) ? exp : exp + "[. = '" + value + "']";
		if(value == null) 
		{
			this.exp = "";
			log.error("Value is required");
			System.exit(0);
		}
		else 
		{
			if(name == null)
			{
				this.exp = "//*[. ='"+ value + "']";
			} 
			else 
			{
				this.exp = "//" + name + "[. = '" + value + "']";
			}
			
		}
		
		
	}
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private XMLGrep(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Constructor
	 * @param argList arguments
	 */
	private XMLGrep(ArgList argList) {
		this(argList.get("s"), argList.get("d"), argList.get("v"), argList.get("n"));
	}
	
	/**
	 * @param src
	 * @param dest
	 * @param xpathExpression
	 */
	@SuppressWarnings("javadoc")
	public static void grepXML(String src, String dest, String xpathExpression) {
		String newpath = "";
		try {
			if(FileAide.isFolder(src)) {
				File dir = new File(src);
				File[] files = dir.listFiles();
				for(File file : files) {
					String result = XPathTool.getXPathResult(file.getPath(), xpathExpression);
					if(result != null && !result.isEmpty()) {
						if(!FileAide.exists(dest)) {
							FileAide.createFolder(dest);
						}
						if(!dest.endsWith("/")) {
							dest.concat("/").concat(file.getName());
						}
						else {
							newpath = dest.concat(file.getName());
						}
						FileAide.createFile(newpath);
						FileAide.setTextContent(newpath, FileAide.getTextContent(file.getPath()));
						FileAide.delete(file.getPath());
					}
				}
				
			} else if(FileAide.isFile(src)) {
				String result = XPathTool.getXPathResult(src, xpathExpression);
				if(result != null && !result.isEmpty()) {
					if(!FileAide.exists(dest)) {
						FileAide.createFile(dest);
					}
					FileAide.setTextContent(dest, FileAide.getTextContent(src));
					FileAide.delete(src);
				}
			}
		} catch(IOException e) {
			System.out.println(e);
			log.error(e.getMessage());
		}
	}
	
	/**
	 * Runs the XMLGrep
	 * @throws IOException error executing
	 */
	public void execute() {
		grepXML(this.src, this.dest, this.exp);
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("XMLGrep");
		// src
		parser.addArgument(new ArgDef().setShortOption('s').setLongOpt("src-dir").withParameter(true, "SRC_DIRECTORY").setDescription("SRC directory to read files from").setRequired(true));
		// dest
		parser.addArgument(new ArgDef().setShortOption('d').setLongOpt("dest-dir").withParameter(true, "DEST_DIRECTORY").setDescription("DEST directory to write files to").setRequired(true));
		// exp
		parser.addArgument(new ArgDef().setShortOption('n').setLongOpt("tag-name").withParameter(true, "TAG_NAME").setDescription("TAG Name to Search for").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('v').setLongOpt("tag-value").withParameter(true, "TAG_VALUE").setDescription("TAG value to Search for").setRequired(true));
		
		return parser;
	}
	
	/**
	 * Main method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			String harvLev = System.getProperty("console-log-level");
			System.setProperty("console-log-level", "OFF");
			InitLog.initLogger(args, getParser(), "h");
			if(harvLev == null) {
				System.clearProperty("console-log-level");
			} else {
				System.setProperty("console-log-level", harvLev);
			}
			log.info(getParser().getAppName() + ": Start");
			new XMLGrep(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:", e);
			System.out.println(getParser().getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser().getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:", e);
			error = e;
		} finally {
			log.info(getParser().getAppName() + ": End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
	
}