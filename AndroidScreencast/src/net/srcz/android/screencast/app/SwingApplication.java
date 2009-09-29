package net.srcz.android.screencast.app;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import net.srcz.android.screencast.ui.JDialogError;

public class SwingApplication extends Application {

	JDialogError jd = null;

	public SwingApplication() {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch(Exception ex) {
			throw new RuntimeException(ex);
		}
	}
	
	@Override
	protected void close() {
		super.close();
	}

	@Override
	protected void handleException(Thread thread, Throwable ex) {
		try {
			ex.printStackTrace(System.err);
			if(jd != null && jd.isVisible())
				return;
			jd = new JDialogError(ex);
			SwingUtilities.invokeLater(new Runnable() {
				
				public void run() {
					jd.setVisible(true);
					
				}
			});
		} catch(Exception ex2) {
			// ignored
		}
	}
	
	

}
