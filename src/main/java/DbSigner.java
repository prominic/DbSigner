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
		return "0.4.6";
	}
	
	@Override
	protected String getJavaAddinDate() {
		return "2022-03-09 16:05";
	}

	protected boolean resolveMessageQueueState(String cmd) {
		boolean flag = super.resolveMessageQueueState(cmd);
		if (flag) return true;

		sign(cmd);
		return true;
	}

	protected void showHelp() {
		int year = Calendar.getInstance().get(Calendar.YEAR);
		logMessage("*** Usage ***");
		AddInLogMessageText("load runjava DbSigner");
		AddInLogMessageText("tell DbSigner <command>");
		AddInLogMessageText("   quit             Unload addin");
		AddInLogMessageText("   help             Show help information (or -h)");
		AddInLogMessageText("   info             Show version and more");
		AddInLogMessageText("   reload           Restart the addin");
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
