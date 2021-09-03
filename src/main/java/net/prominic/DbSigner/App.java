package net.prominic.DbSigner;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Arrays;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import lotus.domino.Database;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.notes.addins.JavaServerAddin;

public class App extends JavaServerAddin {
	// Constants
	private final String		JADDIN_NAME				= "DbSigner";
	private final String		JADDIN_VERSION			= "0.1.0 (first version)";
	private final String		JADDIN_DATE				= "2021-09-03 16:30";

	Session 				m_session				= null;

	private String[] 		args 					= null;
	private int 			dominoTaskID			= 0;

	// constructor if parameters are provided
	public App(String[] args) {
		this.args = args;
	}

	public App() {}

	/* the runNotes method, which is the main loop of the Addin */
	@Override
	public void runNotes () {
		setAddinState("Initializing");

		this.setName(JADDIN_NAME);

		// Create the status line showed in 'Show Task' console command
		this.dominoTaskID = createAddinStatusLine(this.JADDIN_NAME);

		try {
			m_session = NotesFactory.createSession();

			logMessage("version      " + this.JADDIN_VERSION);
			logMessage("date         " + this.JADDIN_DATE);
			logMessage("parameters   " + Arrays.toString(this.args));

			sign();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void sign() throws IOException, ParseException, NotesException {
		JSONParser parser = new JSONParser();

		Reader reader = new FileReader("dbsigner.json");

		JSONObject jsonObject = (JSONObject) parser.parse(reader);
		System.out.println(jsonObject);

		JSONArray dbList = (JSONArray) jsonObject.get("databases");
		for(Object o : dbList){
			JSONObject db = (JSONObject) o;

			String filePath = (String) db.get("filepath");
			System.out.println(filePath);

			Database database = m_session.getDatabase(null, filePath);
			System.out.println(database.getTitle() + " - sign?");
			database.sign();
		}
	}

	/**
	 * Write a log message to the Domino console. The message string will be prefixed with the add-in name
	 * followed by a column, e.g. <code>"AddinName: xxxxxxxx"</code>
	 *
	 * @param	message		Message to be displayed
	 */
	private final void logMessage(String message) {
		AddInLogMessageText(this.JADDIN_NAME + ": " + message, 0);
	}

	/**
	 * Set the text of the add-in which is shown in command <code>"show tasks"</code>.
	 *
	 * @param	text	Text to be set
	 */
	private final void setAddinState(String text) {

		if (this.dominoTaskID == 0)
			return;

		AddInSetStatusLine(this.dominoTaskID, text);
	}

	/**
	 * Create the Domino task status line which is shown in <code>"show tasks"</code> command.
	 *
	 * Note: This method is also called by the JAddinThread and the user add-in
	 *
	 * @param	name	Name of task
	 * @return	Domino task ID
	 */
	public final int createAddinStatusLine(String name) {
		return (AddInCreateStatusLine(name));
	}

	@Override
	public void termThread() {
		terminate();

		super.termThread();
	}

	/**
	 * Terminate all variables
	 */
	private void terminate() {
		try {
			AddInDeleteStatusLine(dominoTaskID);

			if (this.m_session != null) {
				this.m_session.recycle();
				this.m_session = null;
			}

			logMessage("UNLOADED (OK) " + JADDIN_VERSION);
		} catch (NotesException e) {
			logMessage("UNLOADED (**FAILED**) " + JADDIN_VERSION);
		}
	}
}
