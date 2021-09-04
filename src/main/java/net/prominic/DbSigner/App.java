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
import lotus.domino.Document;
import lotus.domino.NoteCollection;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.notes.addins.JavaServerAddin;

public class App extends JavaServerAddin {
	// Constants
	private final String		JADDIN_NAME				= "DbSigner";
	private final String		JADDIN_VERSION			= "0.1.0 (sign all design elements in database)";
	private final String		JADDIN_DATE				= "2021-09-05 16:30";

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

			processJSON();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void processJSON() throws IOException, ParseException, NotesException {
		JSONParser parser = new JSONParser();

		Reader reader = new FileReader("dbsigner.json");
		JSONObject jsonObject = (JSONObject) parser.parse(reader);

		JSONArray dbList = (JSONArray) jsonObject.get("databases");
		logMessage(String.valueOf(dbList.size()).concat(" - database objects found in JSON"));

		for(Object o : dbList){
			JSONObject db = (JSONObject) o;

			String filePath = (String) db.get("filepath");

			Database database = m_session.getDatabase(null, filePath);
			if (database != null && database.isOpen()) {
				sign(database);
			}
			else {
				this.logMessage(filePath.concat(" - can't be initialized"));
			}
		}

		reader.close();
	}

	private void sign(Database database) throws NotesException {
		logMessage(database.getTitle().concat(" - is going to be signed"));

		NoteCollection nc = database.createNoteCollection(true);
		nc.buildCollection();

		String noteid = nc.getFirstNoteID();
		while (noteid.length() > 0) {
			Document doc = database.getDocumentByID(noteid);

			doc.sign();
			doc.save();
			doc.recycle();

			noteid = nc.getNextNoteID(noteid);
		}

		database.recycle();
		logMessage(database.getTitle().concat(" - has been signed (").concat(String.valueOf(nc.getCount())) + " design elements)");
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
