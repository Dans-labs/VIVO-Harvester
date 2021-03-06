/*******************************************************************************
 * Copyright (c) 2010-2011 VIVO Harvester Team. For full list of contributors, please see the AUTHORS file provided.
 * All rights reserved.
 * This program and the accompanying materials are made available under the terms of the new BSD license which accompanies this distribution, and is available at http://www.opensource.org/licenses/bsd-license.html
 ******************************************************************************/
package org.vivoweb.harvester.translate;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vivoweb.harvester.util.FileAide;
import org.vivoweb.harvester.util.InitLog;
import org.vivoweb.harvester.util.args.ArgDef;
import org.vivoweb.harvester.util.args.ArgList;
import org.vivoweb.harvester.util.args.ArgParser;
import org.vivoweb.harvester.util.args.UsageException;
import org.vivoweb.harvester.util.repo.JenaConnect;
import org.vivoweb.harvester.util.repo.RecordHandler;
import com.hp.hpl.jena.query.QuerySolution;
import com.hp.hpl.jena.query.ResultSet;

/**
 * Takes XML Files and uses an XSL file to translate the data into the desired ontology
 * @author Stephen V. Williams swilliams@ctrip.ufl.edu
 * @FIXME svw: this tool seems inopperative
 */
@Deprecated
public class SPARQLTranslator {
	/**
	 * the log property for logging errors, information, debugging
	 */
	private static Logger log = LoggerFactory.getLogger(SPARQLTranslator.class);
	/**
	 * The translation file is the map that will reconstruct our input stream's document into the appropriate format
	 */
	// private String sparqlConstruct;
	/**
	 * in stream is the stream containing the file (xml) that we are going to translate
	 * @TODO possibly remove and switch to passing streams to xmlTranslate
	 */
	protected JenaConnect inputJC;
	/**
	 * out stream is the stream that the controller will be handling and were we will dump the translation
	 * @TODO possibly remove and switch to passing streams to xmlTranslate
	 */
	protected JenaConnect outputJC;
	/**
	 * 
	 */
	protected String sparqlQuery;
	/**
	 * 
	 */
	protected RecordHandler outputRH;
	
	/**
	 * Constructor
	 * @param args commandline arguments
	 * @throws IOException error creating task
	 * @throws UsageException user requested usage message
	 */
	private SPARQLTranslator(String[] args) throws IOException, UsageException {
		this(getParser().parse(args));
	}
	
	/**
	 * Constructor
	 * @param argumentList <ul>
	 *        <li>translationFile the file that details the translation from the original xml to the target format</li>
	 *        <li>inRecordHandler the files/records that require translation</li>
	 *        <li>outRecordHandler the output record for the translated files</li>
	 *        </ul>
	 * @throws IOException error reading files
	 */
	private SPARQLTranslator(ArgList argumentList) throws IOException {
		this(
			JenaConnect.parseConfig(argumentList.get("i"), argumentList.getValueMap("I")), 
			JenaConnect.parseConfig(argumentList.get("o"), argumentList.getValueMap("O")), 
			RecordHandler.parseConfig(argumentList.get("h"), argumentList.getValueMap("H")), 
			FileAide.getTextContent(argumentList.get("s"))
		);
	}
	
	/**
	 * Constructor
	 * @param inputJC the input model
	 * @param outputJC the output model
	 * @param outputRH the output recordhandler
	 * @param sparqlQuery the sparql query
	 */
	public SPARQLTranslator(JenaConnect inputJC, JenaConnect outputJC, RecordHandler outputRH, String sparqlQuery) {
		this.inputJC = inputJC;
		this.outputJC = outputJC;
		this.outputRH = outputRH;
		this.sparqlQuery = sparqlQuery;
	}
	
	/**
	 * Checks again for the necessary file and makes sure that they exist
	 * @throws IOException error accessing file stream
	 */
	public void execute() throws IOException {
		// checking for valid input parameters
		log.trace(this.sparqlQuery);
		ResultSet rs = this.inputJC.executeSelectQuery(this.sparqlQuery);
		
		if(!rs.hasNext()) {
			log.info("No Results");
		} else {
			log.info("Processing Results");
		}
		
		while(rs.hasNext()) {
			QuerySolution qs = rs.next();
			log.info(qs.toString());
		}
		this.inputJC.sync();
		this.outputJC.sync();
		
		log.info("Translation: End");
	}
	
	/**
	 * Get the ArgParser for this task
	 * @return the ArgParser
	 */
	private static ArgParser getParser() {
		ArgParser parser = new ArgParser("SPARQLTranslator");
		parser.addArgument(new ArgDef().setShortOption('i').setLongOpt("input").withParameter(true, "CONFIG_FILE").setDescription("config file for input record handler").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('I').setLongOpt("inputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of input recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('o').setLongOpt("output").withParameter(true, "CONFIG_FILE").setDescription("config file for output record handler").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('O').setLongOpt("outputOverride").withParameterValueMap("RH_PARAM", "VALUE").setDescription("override the RH_PARAM of output recordhandler using VALUE").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('s').setLongOpt("sparqlConstruct").withParameter(true, "SPARQL_CONSTRUCT_FILE").setDescription("the sparql construct to run").setRequired(true));
		parser.addArgument(new ArgDef().setShortOption('h').setLongOpt("outputToRH").withParameter(true, "RECORD_HANDLER").setDescription("output the sparql statment to a record handler").setRequired(false));
		parser.addArgument(new ArgDef().setShortOption('H').setLongOpt("outputToRHOverride").withParameterValueMap("RH_PARAM", "RECORD_HANDLER").setDescription("override the RH_PARAM of recordhandler using VALUE").setRequired(false));
		return parser;
	}
	
	/**
	 * Main Method
	 * @param args commandline arguments
	 */
	public static void main(String... args) {
		Exception error = null;
		try {
			InitLog.initLogger(args, getParser());
			log.info(getParser().getAppName() + ": Start");
			new SPARQLTranslator(args).execute();
		} catch(IllegalArgumentException e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			System.out.println(getParser().getUsage());
			error = e;
		} catch(UsageException e) {
			log.info("Printing Usage:");
			System.out.println(getParser().getUsage());
			error = e;
		} catch(Exception e) {
			log.error(e.getMessage());
			log.debug("Stacktrace:",e);
			error = e;
		} finally {
			log.info(getParser().getAppName() + ": End");
			if(error != null) {
				System.exit(1);
			}
		}
	}
	
}
