package org.genmapp.goelite;

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.Map;
import java.util.Vector;

import javax.swing.JTextArea;

import edu.sdsc.nbcr.opal.AppServiceLocator;
import edu.sdsc.nbcr.opal.AppServicePortType;
import edu.sdsc.nbcr.opal.types.FaultType;
import edu.sdsc.nbcr.opal.types.InputFileType;
import edu.sdsc.nbcr.opal.types.JobInputType;
import edu.sdsc.nbcr.opal.types.JobOutputType;
import edu.sdsc.nbcr.opal.types.JobSubOutputType;
import edu.sdsc.nbcr.opal.types.OutputFileType;
import edu.sdsc.nbcr.opal.types.StatusOutputType;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;



//this class talks to the opal server
class WebService {	
	
	// arguments to expose for launchJob()
	public static String ARG_GENELIST_FILE = "genelistfile";
	public static String ARG_DENOM_FILE = "denomfile";
	public static String ARG_SPECIES = "species";
	public static String ARG_ID_SYSTEM = "idsystem";
	public static String ARG_NUM_PERMUTATIONS = "numperm";
	public static String ARG_PRUNING_ALGORITHM = "pruningalgorithm";
	public static String ARG_PVAL_THRESH = "pvalthresh";
	public static String ARG_ANALYSIS_TYPE = "analysistype";
	public static String ARG_MIN_GENES_CHANGED = "mingeneschanged";
	public static String ARG_ZSCORE_THRESH = "zscorethresh";
	public static String ARG_MOD_ID_SYSTEM = "modidsystem";
	
	public static List< String > getArgs() {
		List< String > l = new ArrayList< String >();
		l.add( ARG_GENELIST_FILE );
		l.add( ARG_DENOM_FILE ); 
		l.add( ARG_SPECIES );
		l.add( ARG_ID_SYSTEM );
		l.add( ARG_NUM_PERMUTATIONS );
		l.add( ARG_PRUNING_ALGORITHM );
		l.add( ARG_PVAL_THRESH );
		l.add( ARG_ANALYSIS_TYPE );
		l.add( ARG_MIN_GENES_CHANGED );
		l.add( ARG_ZSCORE_THRESH );
		l.add( ARG_MOD_ID_SYSTEM );

		return( l );
	}
	static String APP_SERVICE_URL = "http://webservices.cgl.ucsf.edu/opal/services/GOEliteService";
	public static AppServicePortType getService()
			throws javax.xml.rpc.ServiceException {
		AppServiceLocator findService = new AppServiceLocator();
		findService
				.setAppServicePortEndpointAddress( APP_SERVICE_URL );

		AppServicePortType service = findService.getAppServicePort();
		return (service);
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	public static String launchJob(Map<String, String> args,
			AppServicePortType service) {
		return (launchJob(args, service, null));
	}

	public static String launchJob(Map<String, String> args,
			AppServicePortType service, JTextArea statusWindow) {
		statusWindow.append("Parameters:\n");
		try {
			statusWindow.append("1:\n");
			if (service == null) {
				service = getService();
			}
			
			statusWindow.append("service found = " + service + "\n");
			
			// *** Parse dialog box inputs
			// Process the gene list file
			File geneListFile = new File((String) args
					.get( ARG_GENELIST_FILE ));
			statusWindow.append("genelistfile opened\n");

			InputFileType geneListOpalFile = new InputFileType();

			// extract the name portion of the full path
			geneListOpalFile.setName(geneListFile.getName()); 
			statusWindow.append("processing gene list file " + geneListOpalFile.getName() + "\n");
			byte[] geneListFileBytes = Utilities.getBytesFromFile(geneListFile);

			geneListOpalFile
					.setContents(Utilities.replaceCR(geneListFileBytes));
			
			File denomFile = new File((String) args
					.get( ARG_DENOM_FILE ) );
			statusWindow.append("denomfile opened\n");
			InputFileType denomOpalFile = new InputFileType();

			// extract the name portion of the full path
			denomOpalFile.setName(denomFile.getName()); 
			statusWindow.append("processing denom list file " + denomOpalFile.getName() + "\n");
			byte[] denomFileBytes = Utilities.getBytesFromFile(denomFile);
			denomOpalFile.setContents(Utilities.replaceCR(denomFileBytes));
			
			JobInputType launchJobInput = new JobInputType();

			// *** Launch webservice
			// web service arguments are sent in as one long string
			// Example: "--species Mm --denom denom.txt --input probesets.txt
			// --mod EntrezGene
			// --permutations 2 --method z-score --pval 0.05 --num 3"
			// statusWindow.append( "args count: " + args.size() + "\n" );
			for (String key : args.keySet()) {
				statusWindow.append("  " + key + ": ");
				statusWindow.append(args.get(key) + "\n");
			}
			statusWindow.append("\n");
			String argList = 
				     "--species " + (String) args.get( ARG_SPECIES ) + " " +
					 "--denom " + denomOpalFile.getName() + " " + 
					 "--input " + geneListFile.getName() + " " + 
					 "--mod " + (String) args.get( ARG_MOD_ID_SYSTEM ) + " " +
					 "--permutations " + (String) args.get( ARG_NUM_PERMUTATIONS ) + " " +
					 "--method " + (String) args.get( ARG_PRUNING_ALGORITHM ) + " " + 
					 "--pval " + (String) args.get( ARG_PVAL_THRESH ) + " " + 
					 "--dataToAnalyze " + (String) args.get( ARG_ANALYSIS_TYPE ) + " " + 
					 "--num " + (String) args.get( ARG_MIN_GENES_CHANGED ) + " " +
					 "--zscore " + (String) args.get( ARG_ZSCORE_THRESH ) + " ";
			
			// if ( null != statusWindow ) { statusWindow.append( argList ); }
			launchJobInput.setArgList(argList);

			// *** Wait for results, update running status
			InputFileType[] list = { geneListOpalFile, denomOpalFile };
			launchJobInput.setInputFile(list);
			JobSubOutputType output = service.launchJob(launchJobInput);

			return (output.getJobID());
		} catch (javax.xml.rpc.ServiceException e) {
			statusWindow.append( "Could not connect to service at " + APP_SERVICE_URL +  " : " + e.getMessage() );
			
			return (null);			
		} catch (FaultType e) {
			statusWindow.append( "Error during job launch/communication with webservice: " + e.getMessage() );
			return( null );
		} catch( RemoteException e ) {
			statusWindow.append( "Error during job launch/communication with webservice: " + e.getMessage() );
			return( null );
		} catch( java.io.IOException e ) {
			statusWindow.append( "Could not convert input data files to webservice-compatible format: " + e.getMessage() );
			
			return( null );
		}
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	public static StatusOutputType getStatus(String jobID,
			AppServicePortType service) {
		try {
			if (service == null) {
				service = getService();
			}

			return (service.queryStatus(jobID));
		} catch (Exception e) {
			return (null);
		}
	}

	// service can be set to null; this will cause a new service to be located
	// and returned ( slow )
	/*
	 * returns a vector of URLs with the following elements at their respective
	 * indices: 0 - results file url 1 - log file url
	 */
	public static Vector<URL> getResults(String jobID,
			AppServicePortType service, JTextArea statusWindow) {
		try {
			Vector<URL> vResultURL = new Vector<URL>();
			if (service == null) {
				service = getService();
			}

			JobOutputType outputs = service.getOutputs(jobID);
			OutputFileType[] files = outputs.getOutputFile();

			URL resultsFileURL = null, logFileURL = null;

			// process each output file that's sitting on server
			for (int i = 0; i < files.length; i++) {
				URL u = null;
				System.out.println(files[i].getName());
				if (files[i].getName().contains("GO-Elite_results")) {
					// dig into the results folder to get the
					// file we care about
					u = new URL(files[i].getUrl().toString()
							+ "/pruned-results_z-score_elite.txt");

					vResultURL.add( u );

				} 
				else if ( files[i].getName().contains("GO-Elite_report.log") )
				{
					// these files reside at the top-level dir
					u = new URL(files[i].getUrl().toString());
					vResultURL.add( u );
					statusWindow.append( "Go-elite-report.log: " + u );

				}
			} // ... end of "for"

			// stdout.txt/stderr.txt
			// XXX refactor this url, or better yet get it from appservice somehow
			String topLevelDirPath = "http://webservices.rbvi.ucsf.edu:8080/" + jobID;
			URL u = new URL( topLevelDirPath + "/stdout.txt" );
			statusWindow.append( "u: " + u );
			vResultURL.add( u );
			
			u = new URL( topLevelDirPath + "/stderr.txt" );
			statusWindow.append( "u: " + u);
			vResultURL.add( u );

			return (vResultURL);
		} catch (Exception e) {
			return (null);
		}
	}

} // end of class Client