

import java.awt.Button;
import java.awt.FlowLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.text.DefaultCaret;

import org.tritonus.sampled.convert.PCM2PCMConversionProvider;

import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;

import de.umass.lastfm.Playlist;
import de.umass.lastfm.Track;

public class SOUNDZAM extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private boolean running = false;
	private ArrayList<ArrayList<String>> songList;
	private Map<Long, List<DataPoint>> hashMap;
	private Map<Integer, Map<Integer, Integer>> matchMap; // Map<SongId, Map<Offset,Count>>
	private ArrayList<Integer> matchingSongList=new ArrayList<Integer>();
	private JTextField fileTextField = null;
	private long nrSong = 1;
	private Track track;
	private String tag="";
	private JTextArea taskOutput= new JTextArea(16, 25);
	private static final int FUZ_FACTOR = 2;
	double topMagnitude[][];
	double highestFrequency[][];
	long keyPoints[][];
	public final int UPPER_LIMIT = 300;
	public final int LOWER_LIMIT = 40;
	public final int[] RANGE = new int[] { 40, 80, 120, 180, UPPER_LIMIT + 1 };
	public static void main(String[] args) {	
		SOUNDZAM audioWindow = new SOUNDZAM("CSE 364 SOUNDZAM");
		audioWindow.createWindow();
	}
	SOUNDZAM(String windowName) {
		super(windowName);
	}	
	public void createWindow() {		
		this.songList=new ArrayList<ArrayList<String>>();
		this.hashMap = new HashMap<Long, List<DataPoint>>();
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Button buttonLoader = new Button("Load Music Library");
		Button buttonBrowser = new Button("Browse Music Folder");
		Button buttonMatch = new Button("Match");
		Button buttonRecord = new Button("Record Audio");
		Button buttonDeserialize = new Button("Deserialize");
		Button buttonSongList = new Button("Get Song List");
		Button buttonRecommendations = new Button("Get Song Recommendations");
		fileTextField = new JTextField(25);
	    taskOutput.setMargin(new Insets(5,5,5,5));
	    taskOutput.setEditable(false);
	    DefaultCaret caret = (DefaultCaret)taskOutput.getCaret();
	    caret.setUpdatePolicy(DefaultCaret.ALWAYS_UPDATE);
	    fileTextField.setText("");
		buttonLoader.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(fileTextField.getText()=="")taskOutput.append("Please seleate a folder!\n");
				else harvest(new File(fileTextField.getText()),nrSong);
			}
		});
		buttonSongList.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(songList.size()<1)taskOutput.append("no song!\n");
				else for(int i=0;i<songList.size();i++)taskOutput.append(songList.get(i).get(0)+" by "+songList.get(i).get(2)+"\n");
			}
		});		
		buttonBrowser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				JFileChooser chooser = new JFileChooser();
				chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				int returnVal = chooser.showOpenDialog(SOUNDZAM.this);
			    if(returnVal == JFileChooser.APPROVE_OPTION) {
			    	fileTextField.setText(chooser.getSelectedFile().getAbsolutePath());
			    }
			}
		});
		buttonRecord.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hashMap.isEmpty())taskOutput.append("hashMap is empty!\n");
				else{
					taskOutput.append("start!\n");
					System.out.println("start");
					try {
						try {
							audioRecorder(nrSong);
							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
							}
					} catch (LineUnavailableException ex) {
						ex.printStackTrace();
					}
				}
			}
		});
		buttonDeserialize.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				DeserializeHashMap();
			}
		});		
		buttonMatch.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (hashMap.isEmpty())taskOutput.append("hashMap is empty!\n");
				else if(matchMap==null)taskOutput.append("matchMap is empty!\n");
				else{
					songMatching();
					SerializeHashMap();
					matchingSongList.clear();
				}
			}
		});
		buttonRecommendations.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if(tag=="")taskOutput.append("'tag=null' \n");
				else{
					Playlist pl=Playlist.fetchTagPlaylist(tag,"961efa5313c1ad6a357bf504f76cb38c");
					if(pl.getTracks().isEmpty())taskOutput.append("Recommendation Playlist is not found!\n");
					else{
						List<Track> lt=pl.getTracks();
						for(int i=0;i<lt.size();i++){
							System.out.println(lt.get(i).getName());
						taskOutput.append("Recommendation #"+Integer.toString(i+1)+": \n");
						taskOutput.append("\tSong: "+lt.get(i).getName()+"\n");
						taskOutput.append("\tAlbum: "+lt.get(i).getAlbum()+"\n");
						taskOutput.append("\tArtist: "+lt.get(i).getArtist()+"\n\n");
						}
						taskOutput.append("Playlist is based on tag: "+tag+"\n");
						lt.clear();
					}
				}
			}
		});		
		this.add(fileTextField);
		this.add(buttonLoader);
		this.add(buttonBrowser);
		this.add(new JScrollPane(taskOutput));
		this.add(buttonMatch);
		this.add(buttonRecord);
		this.add(buttonDeserialize);
		this.add(buttonSongList);
		this.add(buttonRecommendations);
		this.setLayout(new FlowLayout());
		this.setSize(320,480);
		this.setVisible(true);		
	}
	public void harvest(File mp3Directory,long nrSong) {
		  String[] itemsInDirectory = mp3Directory.list();
		  for(String itemInDirectory:itemsInDirectory) {	
			  if(itemInDirectory.endsWith(".mp3")||itemInDirectory.endsWith(".wav")) {
				  System.out.println(nrSong+": "+itemInDirectory);
				  taskOutput.append(nrSong+": "+itemInDirectory+"/n");
				  songList.add(new ArrayList<String>());
				  File musicFile = new File(mp3Directory, itemInDirectory);
				  try {
					  int i=songList.size()-1;
					  Mp3File song=new Mp3File(musicFile.getAbsolutePath());
					  songList.get(i).add(song.getId3v2Tag().getTitle());
					  songList.get(i).add(song.getId3v2Tag().getAlbum());
					  songList.get(i).add(song.getId3v2Tag().getArtist());
				} catch (UnsupportedTagException e1) {
					e1.printStackTrace();
				} catch (InvalidDataException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				}
				  try {
					songRecorder(nrSong,musicFile);
				} catch (LineUnavailableException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch (UnsupportedAudioFileException e) {
					e.printStackTrace();
				}	
				  nrSong++;
				  } 
			  else if(new File(mp3Directory, itemInDirectory).isDirectory()){
				  harvest(new File(mp3Directory, itemInDirectory),nrSong);
			  }
		  }
	  }
	public void songMatching() {
		int bestCount = 0;
		int bestSong = -1;
		for (int id = 0; id < matchingSongList.size(); id++) {
			System.out.println("For song id: " + matchingSongList.get(id));
			Map<Integer, Integer> tmpMap = matchMap.get(matchingSongList.get(id));
			int bestCountForSong = 0;
			for (Map.Entry<Integer, Integer> entry : tmpMap.entrySet()) {
				if (entry.getValue() > bestCountForSong)bestCountForSong = entry.getValue();
				System.out.println("Time offset = " + entry.getKey()+ ", Count = " + entry.getValue());
			}
			if (bestCountForSong > bestCount) {
				bestCount = bestCountForSong;
				bestSong = matchingSongList.get(id)-1;
			}
		}
		if(bestSong<0||bestCount<2){
			taskOutput.append("Best song is not found \n");
			tag="";
		}else{
			track=Track.getInfo(songList.get(bestSong).get(2), songList.get(bestSong).get(0), "961efa5313c1ad6a357bf504f76cb38c");
			if(track.getTags().isEmpty())tag=songList.get(bestSong).get(2);
			else for (String elem : track.getTags()){
				tag=elem;
				break;
			}
			taskOutput.append("Best song is: \n");
			taskOutput.append("\t"+songList.get(bestSong).get(0)+"\n");
			taskOutput.append("from the album: \n");
			taskOutput.append("\t"+songList.get(bestSong).get(1)+"\n");
			taskOutput.append("by artist: \n");
			taskOutput.append("\t"+songList.get(bestSong).get(2)+"\n\n");
			System.out.println("counts: "+bestCount);
			System.out.println("Best song is '" + songList.get(bestSong).get(0)+"' from the album '"+songList.get(bestSong).get(1)+"' by '"+songList.get(bestSong).get(2)+"'");
	
		}
		matchingSongList.clear();
	}
	AudioFormat getFormat() {
		float sampleRate = 44100;
		int sampleSizeInBits = 8;
		int channels = 1; // mono
		boolean signed = true;
		boolean bigEndian = true;
		return new AudioFormat(sampleRate, sampleSizeInBits, channels, signed,
				bigEndian);
	}
	public void songRecorder(long songId,File file)throws LineUnavailableException, IOException,UnsupportedAudioFileException {		
		PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider();
		AudioInputStream in = AudioSystem.getAudioInputStream(file);		
		AudioFormat baseFormat = in.getFormat();
		System.out.println(baseFormat.toString());
		taskOutput.append(baseFormat.toString()+"\n");
		AudioFormat decodedFormat = new AudioFormat(
			AudioFormat.Encoding.PCM_SIGNED,
			baseFormat.getSampleRate(), 16, baseFormat.getChannels(),
			baseFormat.getChannels() * 2, baseFormat.getSampleRate(),
			false);
		AudioInputStream din = AudioSystem.getAudioInputStream(decodedFormat, in);
		if (!conversionProvider.isConversionSupported(getFormat(),decodedFormat))System.out.println("Conversion is not supported");
		System.out.println(decodedFormat.toString());
		AudioInputStream outDin = conversionProvider.getAudioInputStream(getFormat(), din);
		AudioFormat formatTmp = decodedFormat;
		DataLine.Info info = new DataLine.Info(TargetDataLine.class,formatTmp);
		TargetDataLine lineTmp = (TargetDataLine) AudioSystem.getLine(info);
		final TargetDataLine line = lineTmp;
		final AudioInputStream outDinSound = outDin;
		final long id = songId;
		Thread listeningThread = new Thread(new Runnable() {
			public void run() {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				running = true;
				int n = 0;
				byte[] buffer = new byte[(int) 1024];
				try {
					while (running) {
						n++;
						if (n > 1000)break;
						int count = 0;
						count = outDinSound.read(buffer, 0, 1024);
						if (count > 0)out.write(buffer, 0, count);
					}
					determineKeyPoints(makeSpectrum(out), id, false);
					out.close();
					line.close();
				} catch (IOException e) {
					System.err.println("I/O problems: " + e);
					System.exit(-1);
				}
			}
		});
		listeningThread.start();
	}
	public void audioRecorder(long songId)throws LineUnavailableException, IOException,UnsupportedAudioFileException {
		new PCM2PCMConversionProvider();
		AudioFormat formatTmp = getFormat();
		DataLine.Info info = new DataLine.Info(TargetDataLine.class,formatTmp);
		TargetDataLine lineTmp = (TargetDataLine) AudioSystem.getLine(info);
		final AudioFormat format = formatTmp;
		final TargetDataLine line = lineTmp;
		try {
			line.open(format);
			line.start();
		} catch (LineUnavailableException e) {
			e.printStackTrace();
		}
		final long id = songId;		
		Thread listeningThread = new Thread(new Runnable() {
			public void run() {
				ByteArrayOutputStream out = new ByteArrayOutputStream();
				running = true;
				int n = 0;
				byte[] buffer = new byte[(int) 1024];
				Complex[][] results;
				try {
					while (running) {
						n++;
						if(n%40==0)taskOutput.append(Integer.toString(n/40)+" sec\n");
						if (n > 1000){  //25 seconds
							taskOutput.append("finished\n");
							System.out.println("finished");
							break;
						}
						int count = 0;
						count = line.read(buffer, 0, 1024);						
						if (count > 0)out.write(buffer, 0, count);
					}
					try {
						results=makeSpectrum(out);
						determineKeyPoints(results, id, true);
					} catch (Exception e) {
						System.err.println("Error: " + e.getMessage());
					}
					out.close();
					line.close();
				} catch (IOException e) {
					System.err.println("I/O problems: " + e);
					System.exit(-1);
				}				 
			}
		});
		listeningThread.start();	
		}
	public Complex[][] makeSpectrum(ByteArrayOutputStream out) {
		byte audio[] = out.toByteArray();
		final int totalSize = audio.length;
		int amountPossible = totalSize / 4096;
		Complex[][] results = new Complex[amountPossible][];
		for (int times = 0; times < amountPossible; times++) {
			Complex[] complex = new Complex[4096];
			for (int i = 0; i < 4096; i++)complex[i] = new Complex(audio[(times * 4096) + i], 0);
			results[times] = fft(complex);
		}
		return results;
	}
	private void SerializeHashMap (){
		  try
	      {
			  	 OutputStream fos =new FileOutputStream("serialized/MusicHashmap.ser");
	             ObjectOutputStream oos = new ObjectOutputStream(fos);
	             oos.writeObject(hashMap);
	             oos.close();
	             fos.close();             
	             OutputStream fosL =new FileOutputStream("serialized/songlist.ser");
	             ObjectOutputStream oosL = new ObjectOutputStream(fosL);
	             oosL.writeObject(songList);
	             oosL.close();
	             fosL.close();
	      }catch(IOException ioe)
	       {
	    	  ioe.printStackTrace();
	       }
	  }
	  @SuppressWarnings("unchecked")
	private void DeserializeHashMap(){
		  try
	      { 
			  FileInputStream fis = new FileInputStream("serialized/MusicHashmap.ser");
			  ObjectInputStream ois = new ObjectInputStream(fis);
		      hashMap = (Map<Long, List<DataPoint>>) ois.readObject();
	          ois.close();
	          fis.close();
	          FileInputStream fisL = new FileInputStream("serialized/songlist.ser");
			  ObjectInputStream oisL = new ObjectInputStream(fisL);
			  songList = (ArrayList<ArrayList<String>>) oisL.readObject();
	          oisL.close();
	          fisL.close();
	          }
		  catch(FileNotFoundException e)
	      {
			 System.out.println("'MusicHashmap.ser' & 'songlist.ser' not found");
			 taskOutput.append("'MusicHashmap.ser' & 'songlist.ser' not found\n");
			 taskOutput.append("'Please browse a music folder to read in!\n");
	      }catch(IOException ioe)
	      {
	    	  System.out.println("ERROR");
	    	  ioe.printStackTrace();
	         return;
	      }catch(ClassNotFoundException c)
	      {
	         System.out.println("Class not found");
	         return;
	      }
		  if(songList.size()>0){
		  taskOutput.append("Deserialized 'MusicHashmap.ser'\n");
		  taskOutput.append("Deserialized 'songlist.ser'\n");
		  taskOutput.append("Deserialized total "+songList.size()+" songs\n");
	      System.out.println("Deserialized 'MusicHashmap.ser'");
	      System.out.println("Deserialized 'songlist.ser'");
		  }
	    }
		
	public void determineKeyPoints(Complex[][] results, long songId, boolean isMatching) {
			this.matchMap = new HashMap<Integer, Map<Integer, Integer>>();
			topMagnitude = new double[results.length][5];
			for (int i = 0; i < results.length; i++)for (int j = 0; j < 5; j++)topMagnitude[i][j] = 0;
			highestFrequency = new double[results.length][UPPER_LIMIT];
			for (int i = 0; i < results.length; i++)for (int j = 0; j < UPPER_LIMIT; j++)highestFrequency[i][j] = 0;
			keyPoints = new long[results.length][5];
			for (int i = 0; i < results.length; i++)for (int j = 0; j < 5; j++)keyPoints[i][j] = 0;
			for (int t = 0; t < results.length; t++) {
				for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
					double mag = Math.log(results[t][freq].abs() + 1);
					int index = getIndex(freq);
					if (mag > topMagnitude[t][index]) {
						topMagnitude[t][index] = mag;
						highestFrequency[t][freq] = 1;
						keyPoints[t][index] = freq;
					}
				}
				long h = hash(keyPoints[t][0], keyPoints[t][1], keyPoints[t][2],keyPoints[t][3]);
				if (isMatching) {
					List<DataPoint> listPoints;
					if ((listPoints = hashMap.get(h)) != null) {
						for (DataPoint dP : listPoints) {
							int offset = Math.abs(dP.getTime() - t);
							Map<Integer, Integer> tmpMap = null;
							if ((tmpMap = this.matchMap.get(dP.getSongId())) == null) {
								tmpMap = new HashMap<Integer, Integer>();
								tmpMap.put(offset, 1);
								matchMap.put(dP.getSongId(), tmpMap);
								matchingSongList.add(dP.getSongId());
							} else {
								Integer count = tmpMap.get(offset);
								if (count == null)tmpMap.put(offset, new Integer(1));
								else tmpMap.put(offset, new Integer(count + 1));
							}
						}
					}
				} else {
					List<DataPoint> listPoints = null;
					if ((listPoints = hashMap.get(h)) == null) {
						listPoints = new ArrayList<DataPoint>();
						DataPoint point = new DataPoint((int) songId, t);
						listPoints.add(point);
						hashMap.put(h, listPoints);
					} else {
						DataPoint point = new DataPoint((int) songId, t);
						listPoints.add(point);
					}
				}
			}			
		}
			public int getIndex(int freq) {
				int i = 0;
				while (RANGE[i] < freq)
					i++;
				return i;
			}
		private long hash(long p1, long p2, long p3, long p4) {
			return (p4 - (p4 % FUZ_FACTOR)) * 100000000 + (p3 - (p3 % FUZ_FACTOR))* 100000 + (p2 - (p2 % FUZ_FACTOR)) * 100+ (p1 - (p1 % FUZ_FACTOR));
		}
		private static Complex[] fft(Complex[] x) {
			int N = x.length;
			if (N == 1)return new Complex[] { x[0] };
			if (N % 2 != 0)throw new RuntimeException("N is not a power of 2");
			Complex[] even = new Complex[N / 2];
			for (int k = 0; k < N / 2; k++)even[k] = x[2 * k];
			Complex[] q = fft(even);
			Complex[] odd = even;
			for (int k = 0; k < N / 2; k++)odd[k] = x[2 * k + 1];
			Complex[] r = fft(odd);
			Complex[] y = new Complex[N];
			for (int k = 0; k < N / 2; k++) {
				double kth = -2 * k * Math.PI / N;
				Complex wk = new Complex(Math.cos(kth), Math.sin(kth));
				y[k] = q[k].plus(wk.times(r[k]));
				y[k + N / 2] = q[k].minus(wk.times(r[k]));
			}
			return y;
		}
		public class DataPoint implements Serializable {
			
			/**
			 * 
			 */
			private static final long serialVersionUID = 11L;
			private int time;
			private int songId;

			public DataPoint(int songId, int time) {
				this.songId = songId;
				this.time = time;
			}
			public int getTime() {
				return time;
			}

			public int getSongId() {
		return songId;
			}
		}
		private static  class Complex {
			private final double re; // the real part
			private final double im; // the imaginary part
			public Complex(double real, double imag) {
				re = real;
				im = imag;
			}
			// return a string representation of the invoking Complex object
			public String toString() {
				if (im == 0)
					return re + "";
				if (re == 0)
					return im + "i";
				if (im < 0)
					return re + " - " + (-im) + "i";
				return re + " + " + im + "i";
			}

			// return abs/modulus/magnitude and angle/phase/argument
			public double abs() {
				return Math.hypot(re, im);
			} // Math.sqrt(re*re + im*im)

			@SuppressWarnings("unused")
			public double phase() {
				return Math.atan2(im, re);
			} // between -pi and pi

			// return a new Complex object whose value is (this + b)
			public Complex plus(Complex b) {
				Complex a = this; // invoking object
				double real = a.re + b.re;
				double imag = a.im + b.im;
				return new Complex(real, imag);
			}

			// return a new Complex object whose value is (this - b)
			public Complex minus(Complex b) {
				Complex a = this;
				double real = a.re - b.re;
				double imag = a.im - b.im;
				return new Complex(real, imag);
			}
			// return a new Complex object whose value is (this * b)
			public Complex times(Complex b) {
				Complex a = this;
				double real = a.re * b.re - a.im * b.im;
				double imag = a.re * b.im + a.im * b.re;
				return new Complex(real, imag);
			}
			// scalar multiplication
			// return a new object whose value is (this * alpha)
			@SuppressWarnings("unused")
			public Complex times(double alpha) {
				return new Complex(alpha * re, alpha * im);
			}
			// return a new Complex object whose value is the conjugate of this
			@SuppressWarnings("unused")
			public Complex conjugate() {
				return new Complex(re, -im);
			}
			// return a new Complex object whose value is the reciprocal of this
			public Complex reciprocal() {
				double scale = re * re + im * im;
				return new Complex(re / scale, -im / scale);
			}
			// return the real or imaginary part
			@SuppressWarnings("unused")
			public double re() {
				return re;
			}
			@SuppressWarnings("unused")
			public double im() {
				return im;
			}
			// return a / b
			public Complex divides(Complex b) {
				Complex a = this;
				return a.times(b.reciprocal());
			}
			// return a new Complex object whose value is the complex exponential of
			// this
			@SuppressWarnings("unused")
			public Complex exp() {
				return new Complex(Math.exp(re) * Math.cos(im), Math.exp(re)
						* Math.sin(im));
			}

			// return a new Complex object whose value is the complex sine of this
			public Complex sin() {
				return new Complex(Math.sin(re) * Math.cosh(im), Math.cos(re)
						* Math.sinh(im));
			}
			// return a new Complex object whose value is the complex cosine of this
			public Complex cos() {
				return new Complex(Math.cos(re) * Math.cosh(im), -Math.sin(re)
						* Math.sinh(im));
			}
			// return a new Complex object whose value is the complex tangent of this
			@SuppressWarnings("unused")
			public Complex tan() {
				return sin().divides(cos());
			}
			// a static version of plus
			@SuppressWarnings("unused")
			public static Complex plus(Complex a, Complex b) {
				double real = a.re + b.re;
				double imag = a.im + b.im;
				Complex sum = new Complex(real, imag);
				return sum;
			}

		}
}
