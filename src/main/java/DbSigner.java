import java.util.Arrays;
import java.util.Calendar;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NoteCollection;
import lotus.domino.NotesException;
import lotus.domino.NotesFactory;
import lotus.domino.Session;
import lotus.notes.addins.JavaServerAddin;
import lotus.notes.internal.MessageQueue;

public class DbSigner extends JavaServerAddin {
	// Constants
	private final String		JADDIN_NAME				= "DbSigner";
	private final String		JADDIN_VERSION			= "0.2.0 (sign all design elements in a database)";
	private final String		JADDIN_DATE				= "2022-02-22 23:30";

	// MessageQueue Constants
	// Message Queue name for this Addin (normally uppercase);
	// MSG_Q_PREFIX is defined in JavaServerAddin.class
	private static final int 	MQ_MAX_MSGSIZE 			= 1024;
	private final String 		qName 					= MSG_Q_PREFIX + JADDIN_NAME.toUpperCase();
	MessageQueue 				mq						= null;
	Session 					m_session				= null;

	private String[] 			args 					= null;
	private int 				dominoTaskID			= 0;

	// constructor if parameters are provided
	public DbSigner(String[] args) {
		this.args = args;
	}

	public DbSigner() {}

	/* the runNotes method, which is the main loop of the Addin */
	@Override
	public void runNotes () {
		// Set the Java thread name to the class name (default would be "Thread-n")
		this.setName(JADDIN_NAME);

		// Create the status line showed in 'Show Task' console command
		this.dominoTaskID = createAddinStatusLine(this.JADDIN_NAME);

		try {
			m_session = NotesFactory.createSession();

			showInfo();

			listen();
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void listen() {
		StringBuffer qBuffer = new StringBuffer(1024);

		try {
			mq = new MessageQueue();
			int messageQueueState = mq.create(qName, 0, 0);	// use like MQCreate in API
			if (messageQueueState == MessageQueue.ERR_DUPLICATE_MQ) {
				logMessage(this.JADDIN_NAME + " task is already running");
				return;
			}

			if (messageQueueState != MessageQueue.NOERROR) {
				logMessage("Unable to create the Domino message queue");
				return;
			}

			if (mq.open(qName, 0) != MessageQueue.NOERROR) {
				logMessage("Unable to open Domino message queue");
				return;
			}

			setAddinState("Idle");
			while (this.addInRunning() && (messageQueueState != MessageQueue.ERR_MQ_QUITTING)) {
				/* gives control to other task in non preemptive os*/
				OSPreemptOccasionally();

				// check for command from console
				messageQueueState = mq.get(qBuffer, MQ_MAX_MSGSIZE, MessageQueue.MQ_WAIT_FOR_MSG, 1000);
				if (messageQueueState == MessageQueue.ERR_MQ_QUITTING) {
					return;
				}

				// check messages for Genesis
				String cmd = qBuffer.toString().trim();
				if (!cmd.isEmpty()) {
					resolveMessageQueueState(cmd);
				};
			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}

	private void resolveMessageQueueState(String cmd) {
		if ("-h".equals(cmd) || "help".equals(cmd)) {
			showHelp();
		}
		else if ("info".equals(cmd)) {
			showInfo();
		}
		else if (cmd.length()>0) {
			sign(cmd);
		}
		else {
			logMessage("Command is not recognized (use -h or help to get details)");
		}
	}

	private void showInfo() {
		logMessage("version      " + this.JADDIN_VERSION);
		logMessage("date         " + this.JADDIN_DATE);
		logMessage("parameters   " + Arrays.toString(this.args));
	}

	private void showHelp() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		logMessage("*** Usage ***");
		AddInLogMessageText("load runjava DbSigner");
		AddInLogMessageText("tell DbSigner <command>");
		AddInLogMessageText("   quit             Unload DbSigner");
		AddInLogMessageText("   help             Show help information (or -h)");
		AddInLogMessageText("   info             Show version and more of DbSigner");
		AddInLogMessageText("   <filepath>       Sign all design elements in filepath");
		AddInLogMessageText("Copyright (C) Prominic.NET, Inc. 2021" + (year > 2021 ? " - " + Integer.toString(year) : ""));
		AddInLogMessageText("See https://prominic.net for more details.");
	}

	private void sign(String filePath) {
		try {
			Database database = m_session.getDatabase(null, filePath);
			if (database == null || !database.isOpen()) {
				logMessage("database not found: " + filePath);
				return;
			}
			logMessage(database.getTitle().concat(" - initialized"));

			NoteCollection nc = database.createNoteCollection(false);
			nc.selectAllDesignElements(true);
			nc.buildCollection();

			logMessage(database.getTitle().concat(" - design elements to sign: " + String.valueOf(nc.getCount())));

			String noteid = nc.getFirstNoteID();
			while (noteid.length() > 0) {
				Document doc = database.getDocumentByID(noteid);

				doc.sign();
				doc.save();
				doc.recycle();

				noteid = nc.getNextNoteID(noteid);
			}

			logMessage(database.getTitle().concat(" - has been signed (").concat(String.valueOf(nc.getCount())) + " design elements)");
			database.recycle();
		} catch (NotesException e) {
			logMessage("sign command failed: " + e.getMessage());
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
