import lotus.domino.Database;
import lotus.domino.Document;
import lotus.domino.NoteCollection;
import lotus.domino.NotesException;

import net.prominic.gja_v20220405.JavaServerAddinGenesis;

public class DbSigner extends JavaServerAddinGenesis {
	@Override
	protected String getJavaAddinVersion() {
		return "0.4.11";
	}
	
	@Override
	protected String getJavaAddinDate() {
		return "2022-04-05 18:05";
	}

	protected boolean resolveMessageQueueState(String cmd) {
		boolean flag = super.resolveMessageQueueState(cmd);
		if (flag) return true;

		sign(cmd);
		return true;
	}

	@Override
	protected void showHelpExt() {
		AddInLogMessageText("   <filepath>       Sign all design elements in filepath");
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
