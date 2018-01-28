package main;

import javax.swing.SwingUtilities;

import connection.server.Server;
import gui.server.MainFrame;

public class RunServer {
	
	public static void main(String[] args) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				new MainFrame();
			}
		});
		
		Server server = new Server();
		server.start();
	}
	
}
