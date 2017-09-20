import java.awt.*;
import java.awt.event.*;
import java.util.*;
import javax.sound.midi.*;
import javax.swing.*;
import java.io.*;

public class BeatBox implements Serializable {
	JPanel mainPanel;
	ArrayList<JCheckBox> checkboxList;
	Sequencer sequencer;
	Sequence sequence;
	JFrame mainFrame;
	Track track;
	String[] instrumentsNames = {"Bass Drum","Closed Hi-Hat","Open Hi-Hat","Acoustic Snare","Crash Cymbal","Hand Clap","High Tom","Hi Bongo","Maracas",
							"Whistle","Low Conga","Cowbell","Vibraslap","Low-Mid Tom","High Agogo","Open Hi Conga"};
	int[] instruments = {35,42,46,38,49,39,50,60,70,72,64,56,58,47,67,63};
	public static void main(String args[]) {
		new BeatBox().buildGUI();
	}
	public void buildGUI() {
		mainFrame = new JFrame("Cyber Beat Box");
		mainFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel background = new JPanel(new BorderLayout());
		background.setBorder(BorderFactory.createEmptyBorder(10,10,10,10));
		
		checkboxList = new ArrayList<>();
		Box buttonBox = new Box(BoxLayout.Y_AXIS);
		
		JButton start = new JButton("Start");
		start.addActionListener(new StartListener());
		buttonBox.add(start);
		
		JButton stop = new JButton("Stop");
		stop.addActionListener(new StopListener());
		buttonBox.add(stop);
		
		JButton upTempo = new JButton("Tempo Up");
		upTempo.addActionListener(new UpTempoListener());
		buttonBox.add(upTempo);
		
		JButton downTempo = new JButton("Tempo Down");
		downTempo.addActionListener(new DownTempoListener());
		buttonBox.add(downTempo);
		
		JButton serializeIt = new JButton("Serialize it");
		serializeIt.addActionListener(new SaveListener());
		buttonBox.add(serializeIt);
		
		JButton restore = new JButton("Restore");
		restore.addActionListener(new RestoreListener());
		buttonBox.add(restore);
		
		Box nameBox = new Box(BoxLayout.Y_AXIS);
		for(int i=0;i<16;i++) {
			nameBox.add(new Label(instrumentsNames[i]));
		}
		
		background.add(BorderLayout.EAST,buttonBox);
		background.add(BorderLayout.WEST,nameBox);
		
		mainFrame.getContentPane().add(background);
		
		GridLayout grid = new GridLayout(16,16);
		grid.setVgap(1);
		grid.setHgap(2);
		mainPanel = new JPanel(grid);
		background.add(BorderLayout.CENTER,mainPanel);
		
		for(int i=0;i<256;i++) {
			JCheckBox c = new JCheckBox();
			c.setSelected(false);
			checkboxList.add(c);
			mainPanel.add(c);
		}
		
		setUpMidi();
		
		mainFrame.setBounds(50,50,300,300);
		mainFrame.pack();
		mainFrame.setVisible(true);
	}
	
	public void setUpMidi() {
		try {
			sequencer = MidiSystem.getSequencer();
			sequencer.open();
			sequence = new Sequence(Sequence.PPQ,4);
			track = sequence.createTrack();
			sequencer.setTempoInBPM(120);
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
	}
	public void buildTrackAndStart() {
		int trackList[] = null;
		
		sequence.deleteTrack(track);
		track = sequence.createTrack();
		
		for(int i=0;i<16;i++) {
			trackList = new int[16];
			int key = instruments[i];
			
			for(int j=0;j<16;j++) {
				JCheckBox jc = (JCheckBox)checkboxList.get(j+(16*i));
				if(jc.isSelected()) {
					trackList[j] = key;
				}
				else {
					trackList[j] = 0;
				}
			}
			
			makeTracks(trackList);
			track.add(makeEvent(176,1,127,0,16));
		}
		
		track.add(makeEvent(192,9,1,0,15));
		
		try {
			sequencer.setSequence(sequence);
			sequencer.setLoopCount(sequencer.LOOP_CONTINUOUSLY);
			sequencer.start();
			sequencer.setTempoInBPM(120);
		}
		catch (Exception exc) {
			exc.printStackTrace();
		}
	}
	
	public class StartListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			buildTrackAndStart();
		}
	}
	
	public class StopListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			sequencer.stop();
		}
	}
	
	public class UpTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*1.03));
		}
	}
	
	public class DownTempoListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			float tempoFactor = sequencer.getTempoFactor();
			sequencer.setTempoFactor((float)(tempoFactor*0.97));
		}
	}
	
	public void makeTracks(int[] list) {
		for(int i=0;i<16;i++) {
			int key = list[i];
			if(key!=0) {
				track.add(makeEvent(144,9,key,100,i));
				track.add(makeEvent(128,9,key,100,i+1));
			}
		}
	}
	
	public MidiEvent makeEvent(int comd,int chan,int one,int two,int tick) {
		MidiEvent event = null;
		try {
			ShortMessage sa = new ShortMessage();
			sa.setMessage(comd,chan,one,two);
			event = new MidiEvent(sa,tick);
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
		return event;
	}
	
	public class SaveListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fileSave = new JFileChooser();
			fileSave.showSaveDialog(mainFrame);
			saveFile(fileSave.getSelectedFile());
		}
	}
	
	public class RestoreListener implements ActionListener {
		public void actionPerformed(ActionEvent ae) {
			JFileChooser fileOpen = new JFileChooser();
			fileOpen.showOpenDialog(mainFrame);
			loadFile(fileOpen.getSelectedFile());
		}
	}
	
	public void saveFile(File file) {
		boolean checkboxState[] = new boolean[256];
			
		for(int i=0;i<256;i++) {
			JCheckBox check = (JCheckBox) checkboxList.get(i);
			if(check.isSelected())
				checkboxState[i]=true;
			}
			
		try {
			FileOutputStream fileSer = new FileOutputStream(file);
			ObjectOutputStream os = new ObjectOutputStream(fileSer);
			os.writeObject(checkboxState);
		}
		catch(Exception exc) {
			exc.printStackTrace();
		}
	}
	
	public void loadFile(File file) {
		boolean checkboxState[] = null;
			try {
				FileInputStream fileDeser = new FileInputStream(new File("checkbox.ser"));
				ObjectInputStream is = new ObjectInputStream(fileDeser);
				checkboxState = (boolean[])is.readObject();
			}
			catch(Exception exc) {
				exc.printStackTrace();
			}
			for(int i=0;i<256;i++) {
				JCheckBox check = (JCheckBox)checkboxList.get(i);
				if(checkboxState[i]) 
					check.setSelected(true);
				else
					check.setSelected(false);
			}
			
			sequencer.stop();
			buildTrackAndStart();
	}
}