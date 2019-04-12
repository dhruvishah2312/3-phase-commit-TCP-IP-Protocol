/*
Name: Dhruvi Shah
Login ID: dds0403
MavID: 1001550403

Reference code: https://github.com/aboullaite/Multi-Client-Server-chat-application/
 */
/*
 * References: 
 *  1. https://www.tutorialspoint.com/java/java_filewriter_class.htm
 *  2. https://www.mkyong.com/java/how-to-read-file-in-java-fileinputstream/
 *  3. https://github.com/aboullaite/Multi-Client-Server-chat-application/
 *  
 */

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;

import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;


public class Coordinator {

	private static JTextArea textArea;
	private static JTextField inputTextField;
	private static JButton sendButton;
	private static JButton preCommitButton;
	private static JButton commitButton;

	public static ArrayList<String> votes = new ArrayList<String>();
	public static int clientsConnected = 0;
	public static String abort = null; 


	/** Chat client access */
	static class ChatAccess extends Observable {
		private Socket socket;
		private OutputStream outputStream;

		@Override
		public void notifyObservers(Object arg) {
			super.setChanged();
			super.notifyObservers(arg);
		}

		/** Create socket, and receiving thread */
		public void InitSocket(String server, int port) throws IOException {
			socket = new Socket(server, port);
			outputStream = socket.getOutputStream();

			outputStream.write(("Coordinator\n").getBytes());
			outputStream.flush();
			// no auto-flushing
			/*PrintWriter pw = new PrintWriter(outputStream, false);
            // native line endings are uncertain so add them manually
            pw.print("GET index HTTP/1.0\r\n");
            pw.print("Accept: text/plain, text/html, text/*\r\n");
            pw.print("\r\n");
            pw.flush();*/

			Thread receivingThread = new Thread() {
				@Override
				public void run() {
					while(true) {
						try {

							BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
							String line;
							while ((line = reader.readLine()) != null)
								notifyObservers(line);
						} catch (IOException ex) {
							notifyObservers(ex);
						}
					}
				}
			};
			receivingThread.start();
		}

		private static final String CRLF = "\r\n"; // newline

		/** Send a line of text */
		public void send(String text) {
			try {
				outputStream.write((text + CRLF).getBytes());
				outputStream.flush();
			} catch (IOException ex) {
				notifyObservers(ex);
			}
		}


		/** Close the socket */
		public void close() {
			try {
				socket.close();
			} catch (IOException ex) {
				notifyObservers(ex);
			}
		}
	}

	/** Chat client UI */
	static class ChatFrame extends JFrame implements Observer {

		private static ChatAccess chatAccess;

		public ChatFrame(ChatAccess chatAccess) {
			this.chatAccess = chatAccess;
			chatAccess.addObserver(this);
			buildGUI();
		}

		/** Builds the user interface */
		private void buildGUI() {
			textArea = new JTextArea(20, 50);
			textArea.setEditable(false);
			textArea.setLineWrap(true);
			add(new JScrollPane(textArea), BorderLayout.CENTER);

			Box box = Box.createHorizontalBox();
			add(box, BorderLayout.SOUTH);
			inputTextField = new JTextField();
			sendButton = new JButton("Send");
			preCommitButton = new JButton("PRECOMMIT");
			commitButton = new JButton("COMMIT");

			box.add(inputTextField);
			box.add(sendButton);
			box.add(preCommitButton);
			box.add(commitButton);


			// Action for the inputTextField and the goButton
			ActionListener sendListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = inputTextField.getText();
					if (str != null && str.trim().length() > 0)
						str = "Store String - " + str;
					chatAccess.send(str);
					inputTextField.selectAll();
					inputTextField.requestFocus();
					inputTextField.setText("");
				}
			};

			ActionListener preCommitListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					preCommitButton.setEnabled(false);
					textArea.append("START_3PC\nState: INIT");
					chatAccess.send("VOTE_REQUEST: PRECOMMIT");

					Thread preCommit = new Thread() {
						@Override
						public void run() {
							prePhaseCommit();
						}
					};
					preCommit.start();
					//chatAccess.send("VOTE_REQUEST\nState: WAIT");

					//phaseCommit();
				}
			};
			
			ActionListener commitListener  = new ActionListener() {
				
				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					preCommitButton.setEnabled(false);
					sendButton.setEnabled(false);
					commitButton.setEnabled(false);
					
					Thread commit = new Thread() {
						
						@Override
						public void run() {
							phaseCommit();
						}
					};
					commit.start();
					
					
				}
			};

			inputTextField.addActionListener(sendListener);
			sendButton.addActionListener(sendListener);
			preCommitButton.addActionListener(preCommitListener);
			commitButton.addActionListener(commitListener);

			this.addWindowListener(new WindowAdapter() {
				@Override
				public void windowClosing(WindowEvent e) {
					chatAccess.close();
				}
			});
		}

		/** Updates the UI depending on the Object argument */
		public void update(Observable o, Object arg) {
			final Object finalArg = arg;
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					if(finalArg.toString().contains("ABORT")) {
						
						abort = finalArg.toString();
					} else if(finalArg.toString().contains("VOTE_PRECOMMIT")){
						votes.add(finalArg.toString());
						System.out.println("On vote recieve size:" + votes.size());
						textArea.append(finalArg.toString());
						textArea.append("\n");
					} else if(finalArg.toString().matches("\\d")) {
						clientsConnected = clientsConnected + Integer.parseInt(finalArg.toString());
						System.out.println("Clients connected: "+ clientsConnected);
					} else {
						textArea.append(finalArg.toString());
						textArea.append("\n");
					}

				}
			});
		}

		/*
		 * Initiate pre Phase Commit
		 */
		public void prePhaseCommit() {

			boolean check = true;


			System.out.println("In phase pre_commit");
			long endTimeMillis = System.currentTimeMillis() + 20000;	//Waits until all the participants have voted else timeouts after 20 seconds
			while(true) {
				if((System.currentTimeMillis()-endTimeMillis) > 0) {
					check = false;
					textArea.append("Timeout");
					textArea.append("\n");
					System.out.println("In phase commit - Time Out Condition");
					chatAccess.send("GLOBAL_ABORT");
					initialiseVariables();
					break;
				}
				
				if(abort!=null && abort.contains("ABORT")) {
					textArea.append("GLOBAL_ABORT\nState: ABORT");
					textArea.append("\n");
					check = false;
					chatAccess.send("GLOBAL_ABORT");
					initialiseVariables();
					break;
				}

				System.out.println("In phase commit - Vote" + votes.size());

				if(votes.size() == clientsConnected) {
					System.out.println("In phase commit - Vote" + votes.size());
					break;
				}
			}
			System.out.println("In phase commit after wait");

			if(votes.size() < clientsConnected) {	//Check if all the clients have voted
				textArea.append("GLOBAL_ABORT\nState: ABORT");
				textArea.append("\n");
				chatAccess.send("GLOBAL_ABORT");
				check = false;
				System.out.println("In incomplete vote condition");
				initialiseVariables();
			} 

			if(votes.size() == clientsConnected) {

				if(votes.contains("ABORT")) {	//Check if a client votes abort
					textArea.append("GLOBAL_ABORT\nState: ABORT");
					textArea.append("\n");
					chatAccess.send("GLOBAL_ABORT");
					check = false;
					System.out.println("In abort vote condition");
					initialiseVariables();
				}
			}
			if(check) {	//Send Global precommit message
				textArea.append("GLOBAL_PRECOMMIT\nState: PRECOMMIT ");
				textArea.append("\n");
				chatAccess.send("GLOBAL_PRECOMMIT");
				System.out.println("In all precommit vote condition");
				initialiseVariables();
			}

			preCommitButton.setEnabled(true);
			

		}
		
		public void phaseCommit() {
			
			textArea.append("GLOBAL_COMMIT \nState: COMMIT ");
			textArea.append("\n");
			chatAccess.send("GLOBAL_COMMIT");
			System.out.println("In all acknowledge received condition");
			initialiseVariables();
		}
		
		public void initialiseVariables() {
			votes.clear();
			sendButton.setEnabled(true);
			commitButton.setEnabled(true);
			preCommitButton.setEnabled(true);
			
		}
		
	}


	public static void main(String[] args) {
		
		ChatAccess access = new ChatAccess();

		JFrame frame = new ChatFrame(access);
		frame.setTitle("MyChatApp - connected to localhost : 80");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.pack();
		frame.setLocationRelativeTo(null);
		frame.setResizable(false);
		frame.setVisible(true);

		try {
			access.InitSocket("localhost",80);
		} catch (IOException ex) {
			System.out.println("Cannot connect to Localhost : 80 ");
			ex.printStackTrace();
			System.exit(0);
		}
	}

}
