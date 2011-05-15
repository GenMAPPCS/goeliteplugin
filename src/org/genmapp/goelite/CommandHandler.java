package org.genmapp.goelite;

import java.net.URL;
import java.util.HashMap;
import java.util.Collection;
import java.util.Map;
import java.util.Vector;

import javax.swing.JDialog;
import javax.swing.JTextArea;

import cytoscape.command.AbstractCommandHandler;
import cytoscape.command.CyCommandException;
import cytoscape.command.CyCommandManager;
import cytoscape.command.CyCommandResult;
import cytoscape.layout.LayoutProperties;
import cytoscape.layout.Tunable;
import edu.sdsc.nbcr.opal.types.StatusOutputType;


/**
 *  Registers CyCommands to Cytoscape, and handles CyCommand events/requests
 *
 */
class CommandHandler extends AbstractCommandHandler {

	protected static final String GETDATA = "get data";
	protected static final String LAUNCH = "launch";
	protected static final String OPENDIALOG = "open dialog";
	protected static final String STATUS = "status";

	protected static final String ARG_ID = "id"; // argument name
	protected static final String ARG_STATUS_CODE = "status_code";
	protected static final String ARG_STATUS_MSG = "status_msg";
	protected static final String ARG_RESULTS_FILE = "results_file";
	protected static final String ARG_LOG_FILE = "log_file";

	//EXTERNAL
	protected final static String GET_ALL_DATASET_NODES = "get all dataset nodes";
	
	LayoutProperties props = null;

	public CommandHandler() {
		super(CyCommandManager.reserveNamespace("goelite"));

		// *** functions definitions for the plugin to expose to the world
		// GETDATA:
		// get data id="id"
		addDescription(GETDATA, "");
		addArgument(GETDATA, ARG_ID);

		// LAUNCH
		// to get the arguments, we just use the properties passed in from the
		// InputDialogBox
		// however, we must also manually add two params that are not in the
		// tunable list
		addDescription(LAUNCH, "");
		for (String t : WebService.getArgs() ) {
			addArgument(LAUNCH, t);
		}

		// OPENDIALOG
		addDescription(OPENDIALOG, "");
		addArgument(OPENDIALOG);

		// STATUS
		addDescription(STATUS, "");
		addArgument(STATUS, ARG_ID);
	}

	public CyCommandResult execute(String command, Collection<Tunable> args)
			throws CyCommandException {
		return execute(command, createKVMap(args));
	}

	public CyCommandResult execute(String command, Map<String, Object> args )
			throws CyCommandException {
		CyCommandResult result = new CyCommandResult();
		result.addMessage("test");
		for ( String t : args.keySet() )
		{
		   result.addMessage( "Arg: " + t + " = " + args.get( t ) );
		}

		if (LAUNCH.equals(command)) {
			// convert to map of String,String
			Map< String, String > stringArgs = new HashMap< String, String >();
			for ( String t : args.keySet() )
			{
 				stringArgs.put( t, (String) args.get( t ) );
			}
			
			JDialog debugDialog = new JDialog();
			JTextArea debugTextArea = new JTextArea(80, 80);
			debugDialog.add( debugTextArea );
			debugDialog.show();
			String jobId = WebService.launchJob(stringArgs, null, debugTextArea);

			result.addMessage("job id = " + jobId);
			result.addResult(ARG_ID, jobId);
		} else if (OPENDIALOG.equals(command)) {
			InputDialog dialog = new InputDialog();
			dialog.setVisible(true);

			result.addMessage("Opened dialog");
		} else if (STATUS.equals(command)) {
			
			String jobId = getArg(command, ARG_ID, args);

			StatusOutputType status = WebService.getStatus(jobId, null);
			result.addMessage("GOElite status for run id " + jobId + " = "
					+ status.getCode() + "[ " + status.getMessage() + " ]");
			result.addResult(ARG_STATUS_CODE, status.getCode());
			result.addResult(ARG_STATUS_MSG, status.getMessage());
		} else if (GETDATA.equals(command)) {
			// returns the result file URLs off the server
			String jobId = getArg(command, ARG_ID, args);
			Vector<URL> vURL = WebService.getResults(jobId, null, null);
			if ( vURL.size() != 2 )
			{
				result.addMessage( "insufficient return values from server: " + vURL.size() );
			}
			else
			{
				result.addMessage("URL found");
				result.addMessage("ARG_RESULTS_FILE = " + vURL.get(0) );
				result.addResult(ARG_RESULTS_FILE, vURL.get(0) );
				
				result.addMessage("ARG_LOG_FILE = " + vURL.get(1) );
				result.addResult(ARG_LOG_FILE, vURL.get(1) );
			}
		}
		return (result);
	}
}