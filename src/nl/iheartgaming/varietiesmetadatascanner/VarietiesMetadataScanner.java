package nl.iheartgaming.varietiesmetadatascanner;

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.filechooser.FileNameExtensionFilter;
import org.apache.commons.text.WordUtils;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;
import org.jaudiotagger.tag.TagException;

public class VarietiesMetadataScanner implements ActionListener {
	// Create JFrame.
	private static JFrame frame = new JFrame("varieti.es Metadata Scanner");

	// Create ArrayLists for later use.
	private static ArrayList<String> processedAlbumTitles = new ArrayList<String>();
	private static ArrayList<String> processedAlbumYears = new ArrayList<String>();
	private static ArrayList<String> output = new ArrayList<String>();

	public VarietiesMetadataScanner() {
	}

	private JPanel createGUI() {
		// Create JPanel.
		JPanel panel = new JPanel();
		panel.setLayout(null);

		// Create title text.
		JLabel title = new JLabel("varieti.es Metadata Scanner", 0);
		title.setLocation(0, 5);
		title.setSize(390, 30);
		title.setFont(new Font(title.getName(), Font.PLAIN, 24));
		panel.add(title);

		// Instantiate buttons; set button size; set button location; add action
		// listeners; and add to GUI JPanel.
		JButton open = new JButton("Browse for Directory or Directories");
		open.addActionListener(this);
		open.setLocation(5, 40);
		open.setSize(360, 30);
		panel.add(open);

		// Return GUI to main method.
		return panel;
	}

	public void actionPerformed(ActionEvent e) {
		// Instantiate JFileChooser.
		JFileChooser chooseDir = new JFileChooser();

		// Allow JFileChooser to select multiple directories.
		chooseDir.setMultiSelectionEnabled(true);
		chooseDir.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

		chooseDir.showOpenDialog(null);

		// Instantiate file array.
		File[] file = chooseDir.getSelectedFiles();

		// Call searchFolders.
		searchFolders(file);

		// Once scanning is done, clean up the output ArrayList.
		cleanOutput();

		// Save the output .vms file.
		try {
			saveOutput();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
	}

	private void searchFolders(File[] f) {
		for (File fileToAnalyze : f) {
			if (fileToAnalyze.isDirectory()) {
				searchFolders(fileToAnalyze.listFiles());
			}
			if ((fileToAnalyze.getName().endsWith(".aac")) || (fileToAnalyze.getName().endsWith(".aiff"))
					|| (fileToAnalyze.getName().endsWith(".flac")) || (fileToAnalyze.getName().endsWith(".m4a"))
					|| (fileToAnalyze.getName().endsWith(".mp3")) || (fileToAnalyze.getName().endsWith(".oga"))
					|| (fileToAnalyze.getName().endsWith(".ogg")) || (fileToAnalyze.getName().endsWith(".wav"))
					|| (fileToAnalyze.getName().endsWith(".wave")) || (fileToAnalyze.getName().endsWith(".wma"))) {
				try {
					analyzeTags(fileToAnalyze);
				} catch (CannotReadException | InvalidAudioFrameException | IOException | ReadOnlyFileException
						| TagException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	private void analyzeTags(File f)
			throws CannotReadException, IOException, TagException, ReadOnlyFileException, InvalidAudioFrameException {
		// Commented-out variables are not currently needed but could be implemented for
		// later use.
		// String trackNumberTemp = "";
		// int trackNumber = 0;
		String artist = "";
		String year = "";
		String album = "";
		String albumArtist = "";
		String title = "";
		int lengthSeconds = 0;
		String length = "";
		String albumCoverFilename = "";
		AudioFile file = AudioFileIO.read(f);
		Tag tag = file.getTag();

		// Read length in seconds.
		lengthSeconds = file.getAudioHeader().getTrackLength();

		// Track number is not currently needed.
		// // Read track number.
		// trackNumberTemp = tag.getFirst(FieldKey.TRACK);
		// // Condition for extra data: set to track number only.
		// if (trackNumberTemp.indexOf("/") != -1) {
		// trackNumber = Integer.parseInt(trackNumberTemp.substring(0,
		// (trackNumberTemp.indexOf("/"))));
		// }
		// // Set to raw track number.
		// else {
		// trackNumber = Integer.parseInt(trackNumberTemp);
		// }

		// Read year.
		year = tag.getFirst(FieldKey.YEAR);
		// Condition for extra data: set to year only.
		if (year.length() > 4) {
			year = year.substring(0, 4);
		}

		// Set length.
		int secondsTemp = 0;
		String secondsTempString = "";
		// Change 0 and 9 seconds to 00 and 09 if necessary.
		secondsTemp = lengthSeconds % 60;
		if (secondsTemp < 10) {
			secondsTempString = "0" + secondsTemp;
		} else {
			secondsTempString = "" + secondsTemp;
		}
		// Concatenate minutes and seconds into length string.
		length = lengthSeconds / 60 + ":" + secondsTempString;

		// Read artist.
		artist = tag.getFirst(FieldKey.ARTIST);

		// Read album.
		album = tag.getFirst(FieldKey.ALBUM);
		// Correct album title capitalization if needed.
		album = useCorrectCapitalization(album);

		// Read album artist.
		albumArtist = tag.getFirst(FieldKey.ALBUM_ARTIST);
		// Condition if album artist does not exist: set to artist.
		if (albumArtist.equals("")) {
			albumArtist = artist;
		}

		// Set album cover filename to the lowercase album title with all
		// non-letters or digits removed.
		albumCoverFilename = album.replaceAll("[^\\p{L}\\d]", "").toLowerCase() + ".jpg";

		// Read title.
		title = tag.getFirst(FieldKey.TITLE);
		// Correct title capitalization if needed.
		title = useCorrectCapitalization(title);

		// Check for duplicate album entries by title and year, and return only
		// a track entry if one exists.
		boolean albumMatches = false;
		boolean yearMatches = false;
		int albumMatchIndex = 0;

		for (int i = 0; i < processedAlbumTitles.size(); i++) {
			if (album.equalsIgnoreCase(processedAlbumTitles.get(i))) {
				albumMatches = true;
				albumMatchIndex = i;
			}
		}
		if (year.equalsIgnoreCase(processedAlbumYears.get(albumMatchIndex))) {
			yearMatches = true;
		}

		if (albumMatches && yearMatches) {
			// Add each line to output ArrayList.
			output.add("{");
			output.add("title: \"" + title + "\",");
			output.add("romanization: \"\",");
			output.add("duration: \"" + length + "\"");
			output.add("},");
		} else {
			// Add album information to processed album ArrayLists.
			processedAlbumTitles.add(album);
			processedAlbumYears.add(year);

			// Add each line to output ArrayList.
			output.add("]");
			output.add(")");
			output.add("CreateAlbumWithTracks({");
			output.add("title: \"" + album + "\",");
			output.add("romanization: \"\",");
			output.add("romaji_artist: \"" + albumArtist + "\",");
			output.add("japanese_artist: \"\",");
			output.add("year: \"" + year + "\",");
			output.add("description: \"\",");
			output.add("coverlink: \"" + albumCoverFilename + "\",");
			output.add("flavor: \"\"");
			output.add("},");
			output.add("[{");
			output.add("title: \"" + title + "\",");
			output.add("romanization: \"\",");
			output.add("duration: \"" + length + "\"");
			output.add("},");
		}
	}

	private String useCorrectCapitalization(String s) {
		String toReturn = "";

		s = WordUtils.capitalizeFully(s, new char[] { ' ', '(', ')', '[', ']', '.', '-' });

		String[] words = s.split("\\s");

		for (int i = 0; i < words.length; i++) {
			if ((i != 0) && (i != words.length - 1)
					&& ((words[i].equalsIgnoreCase("a")) || (words[i].equalsIgnoreCase("an"))
							|| (words[i].equalsIgnoreCase("the")) || (words[i].equalsIgnoreCase("and"))
							|| (words[i].equalsIgnoreCase("but")) || (words[i].equalsIgnoreCase("or"))
							|| (words[i].equalsIgnoreCase("nor")) || (words[i].equalsIgnoreCase("as"))
							|| (words[i].equalsIgnoreCase("at")) || (words[i].equalsIgnoreCase("by"))
							|| (words[i].equalsIgnoreCase("for")) || (words[i].equalsIgnoreCase("in"))
							|| (words[i].equalsIgnoreCase("of")) || (words[i].equalsIgnoreCase("on"))
							|| (words[i].equalsIgnoreCase("to")) || (words[i].equalsIgnoreCase("cum"))
							|| (words[i].equalsIgnoreCase("mid")) || (words[i].equalsIgnoreCase("off"))
							|| (words[i].equalsIgnoreCase("per")) || (words[i].equalsIgnoreCase("qua"))
							|| (words[i].equalsIgnoreCase("re")) || (words[i].equalsIgnoreCase("up"))
							|| (words[i].equalsIgnoreCase("via")) || (words[i].equalsIgnoreCase("from")))) {
				words[i] = words[i].toLowerCase();
			}
		}

		for (int i = 0; i < words.length; i++) {
			if (i == words.length - 1) {
				toReturn = toReturn + words[i];
			} else {
				toReturn = toReturn + words[i] + " ";
			}
		}

		return toReturn;
	}

	private void cleanOutput() {
		// Remove the first two lines of extra data.
		output.remove(0);

		// Since the 0 index has been removed, the former index 1 will shift to
		// index 0.
		output.remove(0);

		// Add "])" to the end of the output to create the proper syntax.
		output.add("])");
	}

	private void saveOutput() throws IOException {
		// Instantiate JFileChooser.
		JFileChooser saveVms = new JFileChooser();

		// Set dialog type to save dialog.
		saveVms.setDialogType(1);

		// Show only .vms files.
		saveVms.setFileFilter(new FileNameExtensionFilter("varieti.es Metadata Scanner Output (.vms)", "vms"));

		// Open save dialog.
		int saveReturned = saveVms.showSaveDialog(frame);

		// Write file contents.
		if (saveReturned == JFileChooser.APPROVE_OPTION) {
			String outputFilename = saveVms.getSelectedFile().toString();

			// Add the .vms extension to the output file if it does not exist.
			if (!outputFilename.endsWith(".vms")) {
				outputFilename = outputFilename + ".vms";
			}

			// Iterate through the output ArrayList, and write each line to the
			// selected output file.
			// This construction is needed to encode in UTF-8.
			Writer writer = new OutputStreamWriter(new FileOutputStream(outputFilename), StandardCharsets.UTF_8);
			for (String s : output) {
				writer.write(s + "\n");
			}
			writer.close();
		}

		// Setup all ArrayLists for future processing.
		clearAllArrayLists();
		addInitialArrayListData();
	}

	private void clearAllArrayLists() {
		output.clear();
		processedAlbumTitles.clear();
		processedAlbumYears.clear();
	}

	private static void addInitialArrayListData() {
		// This initial data must be added to each processed album ArrayList for
		// duplicate album checking to work.
		processedAlbumTitles.add("FIRST_INDEX_PROCESSED_ALBUM_TITLE");
		processedAlbumYears.add("FIRST_INDEX_PROCESSED_ALBUM_YEAR");
	}

	public static void main(String[] args) {
		javax.swing.SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				// Add GUI to frame.
				VarietiesMetadataScanner metadataScannerGUI = new VarietiesMetadataScanner();
				frame.setContentPane(metadataScannerGUI.createGUI());

				try {
					javax.swing.UIManager.setLookAndFeel("com.seaglasslookandfeel.SeaGlassLookAndFeel");
				} catch (Exception e) {
					e.printStackTrace();
				}

				// Set JFrame properties.
				frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
				frame.setSize(390, 115);
				frame.setVisible(true);

				addInitialArrayListData();
			}
		});
	}
}