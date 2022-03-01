import java.util.Arrays;
import java.util.Calendar;

import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NoteCollection;
import lotus.domino.NotesException;

public class DbSigner extends JavaServerAddinGenesis {
	@Override
	protected String getJavaAddinName() {
		return "DbSigner";
	}
	
	@Override
	protected String getJavaAddinVersion() {
		return "0.3.0 (base class)";
	}
	
	@Override
	protected String getJavaAddinDate() {
		return "2022-03-01 14:30";
	}

	@Override
	protected String getCmdFileName() {
		return "dbsigner.txt";
	}

	protected void resolveMessageQueueState(String cmd) {
		if ("-h".equals(cmd) || "help".equals(cmd)) {
			showHelp();
		}
		else if ("info".equals(cmd)) {
			showInfo();
		}
		else if ("quit".equals(cmd)) {
			quit();
		}
		else if (cmd.length()>0) {
			sign(cmd);
		}
		else {
			logMessage("Command is not recognized (use -h or help to get details)");
		}
	}

	protected void showInfo() {
		logMessage("version      " + this.getJavaAddinName());
		logMessage("date         " + this.getJavaAddinDate());
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
			
			nc.recycle();
			database.recycle();
		} catch (NotesException e) {
			logMessage("sign command failed: " + e.getMessage());
		}
	}

}
