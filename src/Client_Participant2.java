
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
import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.Observable;
import java.util.Observer;
import java.util.Date;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

public class Client_Participant2 {
	private static JTextArea textArea;
	private static JTextField inputTextField;
	private static JButton sendButton;
	private static JButton preCommitButton;
	private static JButton abortButton;
	private static JButton ackReceivedButton;
	public static ArrayList<String> votes = new ArrayList<String>();
	public static String reply = null;
	public static FileReader in = null;
	public static FileOutputStream out = null;
	public static String content = null;
	public static String httpResponse = null;
	public static Thread abort = null;
	//http response formation
	
	
	
	
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

			outputStream.write(("Participant_2\n").getBytes());
			outputStream.flush();

			
			// Create thread to receive input
			Thread receivingThread = new Thread() {
				@Override
				public void run() {
					try {
						BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
						String line;
						while ((line = reader.readLine()) != null)
							notifyObservers(line);
					} catch (IOException ex) {
						notifyObservers(ex);
					}
				}
			};
			receivingThread.start();
			Date today = new Date();
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

			try {
				in = new FileReader(new File("Participant_2.txt"));
			} catch (FileNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			BufferedReader br = new BufferedReader(in);
			String line;
			try {
				if ((line = br.readLine()) != null) {
					textArea.append("File Content:" + line + "\n");
					while ((line = br.readLine()) != null) {
						textArea.append("File Content: " + line + "\n"); // Store the list in an array
					}
				} else {
					textArea.append("No Contents in file" + "\n");
				}

			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}

			Box box = Box.createHorizontalBox();
			add(box, BorderLayout.SOUTH);
			// inputTextField = new JTextField();
			// sendButton = new JButton("Send");
			preCommitButton = new JButton("preCommit");
			abortButton = new JButton("Abort");
			ackReceivedButton = new JButton("Acknowledge");

			// box.add(inputTextField);
			// box.add(sendButton);

			box.add(preCommitButton);
			box.add(abortButton);
			box.add(ackReceivedButton);

			ActionListener preCommitListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abortButton.setEnabled(false);
					preCommitButton.setEnabled(false);

					abort = new Thread() {
						@Override
						public void run() {
							phasePreCommit("VOTE_PRECOMMIT");
						}
					};
					abort.start();
				}
			};

			ActionListener abortListener = new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					abortButton.setEnabled(false);
					preCommitButton.setEnabled(false);
					abort = new Thread() {
						@Override
						public void run() {
							phasePreCommit("VOTE_ABORT");
						}
					};
					abort.start();
				}
			};

			ActionListener ackListener = new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					// TODO Auto-generated method stub
					Thread acknowledge = new Thread() {
						public void run() {
							phaseCommit("ACKNOWLEDGEMENT_RECEIVED");
						}
					};
					acknowledge.start();

				}
			};

			// inputTextField.addActionListener(sendListener);
			// sendButton.addActionListener(sendListener);
			preCommitButton.addActionListener(preCommitListener);
			abortButton.addActionListener(abortListener);
			ackReceivedButton.addActionListener(ackListener);

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

					if (finalArg.toString().contains("VOTE_REQUEST: PRECOMMIT")) {
						textArea.append("START_3PC\nState: INIT");
						textArea.append("\n");
						textArea.append(finalArg.toString());
						textArea.append("\n");
					} else if (finalArg.toString().contains("GLOBAL_PRECOMMIT")
							|| finalArg.toString().contains("GLOBAL_ABORT")
							|| finalArg.toString().contains("GLOBAL_COMMIT")) {
						reply = finalArg.toString();
						textArea.append(finalArg.toString());
						textArea.append("\n");
					} else if (finalArg.toString().contains("Store String -")) {
						String[] words = finalArg.toString().split("-", 2);
						content = words[1];
						System.out.println(content.trim());
						textArea.append(finalArg.toString());
						textArea.append("\n");
					} else {
						textArea.append(finalArg.toString());
						textArea.append("\n");
					}
				}
			});
		}

		/*
		 * Initiate prepare commit phase
		 */
		@SuppressWarnings("deprecation")
		public void phasePreCommit(String action) {

			if (action.contains("PRECOMMIT")) { // Send what action the user wishes to perform
				chatAccess.send("@Coordinator " + action);
				textArea.append("GLOBAL_PRECOMMIT\nState: PRECOMMIT\n");
				textArea.append("\n");
				textArea.append(
						"Votes acknowledged by the coordinator. \nReady to commit once acknowledgement received message appears");
				textArea.append("\nPress acknowledge button");
			} else if (action.contains("ABORT")) {
				chatAccess.send("@Coordinator " + action);
				textArea.append("VOTE_ABORT\nState: ABORT");
				textArea.append("\n");
			}
				

			long endTimeMillis = System.currentTimeMillis() + 20000; // Wait for Coordinator Reply
			while (true) {
				if ((System.currentTimeMillis() - endTimeMillis) > 0) {
					
					break;
				}
			}

			System.out.println("Reply:" + reply);
			if (reply != null) { // Perform write to file or don't depending on the coordinator decision
				if (reply.contains("GLOBAL_PRECOMMIT")) {
					textArea.append("GLOBAL_PRECOMMIT\nState: PRECOMMIT\n");
					textArea.append("\n");

					abortButton.setEnabled(false);
					preCommitButton.setEnabled(false);
					ackReceivedButton.setEnabled(true);
//					reply = null;
				}  else {
					textArea.append("GLOBAL_ABORT\nState: ABORT");
					textArea.append("\n");
					chatAccess.send("@Coordinator Abort Complete");
					reply = null;
				}
			}

			abortButton.setEnabled(true);
			preCommitButton.setEnabled(true);
			ackReceivedButton.setEnabled(true);
			

		}


		public void phaseCommit(String action) {
			chatAccess.send("@Coordinator " +action);
			textArea.append("Acknowledgement Sent");
			long endTimeMillis = System.currentTimeMillis() + 20000; // Wait for Coordinator Reply
			while(true) {
				System.out.println(reply);
				//if the coordinator times out we are making ssure that the string gets committed according to 3 phase protocol
				if((System.currentTimeMillis() - endTimeMillis) > 0) {
					textArea.append("\n Failure of response from coordinator. Global Commit implemented acc to 3 phase protocol\n");
					textArea.append("GLOBAL_COMMIT\nState: COMMIT\n");
					textArea.append("\n");

					abortButton.setEnabled(true);
					preCommitButton.setEnabled(true);
					ackReceivedButton.setEnabled(true);

					// for final global commit action
					try {
						FileWriter writer = new FileWriter(new File("Participant_2.txt"), true);
						writer.append(content.trim());
						writer.append("\n");
						writer.flush();
						writer.close();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
		
					chatAccess.send("@Coordinator Commit Complete");
					break;
				}
				if(reply.contains("GLOBAL_COMMIT")){
				textArea.append("GLOBAL_COMMIT\nState: COMMIT\n");
				textArea.append("\n");

				

				// for final global commit action
				try {
					FileWriter writer = new FileWriter(new File("Participant_2.txt"), true);
					writer.append(content.trim());
					writer.append("\n");
					writer.flush();
					writer.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				reply = null;
				chatAccess.send("@Coordinator Commit Complete");
				
				break;
				}

			}
			
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
			access.InitSocket("localhost", 80);
		} catch (IOException ex) {
			System.out.println("Cannot connect to Localhost : 80 ");
			ex.printStackTrace();
			System.exit(0);
		}
	}

}
