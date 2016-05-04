	
	import java.awt.image.BufferedImage;
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
	

	import javax.imageio.ImageIO;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.TargetDataLine;
import javax.sound.sampled.UnsupportedAudioFileException;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.tritonus.sampled.convert.PCM2PCMConversionProvider;
	

	import com.mpatric.mp3agic.InvalidDataException;
import com.mpatric.mp3agic.Mp3File;
import com.mpatric.mp3agic.UnsupportedTagException;
	

	import de.umass.lastfm.Playlist;
import de.umass.lastfm.Track;
	

	import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
	
	
	public class soundzam_2 {
	
		protected Shell shell;
		
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
	public final int UPPER_LIMIT = 300; //high freq limit
	public final int LOWER_LIMIT = 40;	//low freq limit
	public final int[] RANGE = new int[] { 40, 80, 120, 180, UPPER_LIMIT + 1 };
	private Text text;
	/**
	 * Launch the application.
	 * @param args
	 */
	public static void main(String[] args) {
		try {
			soundzam_2 window = new soundzam_2();
			window.open();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Open the window.
	 */
	public void open() {
		Display display = Display.getDefault();
		this.songList=new ArrayList<ArrayList<String>>();
		this.hashMap = new HashMap<Long, List<DataPoint>>();
		createContents();
		shell.open();
		shell.layout();
		while (!shell.isDisposed()) {
			if (!display.readAndDispatch()) {
				display.sleep();
			}
		}
	}
	
	/**
	 * Create contents of the window.
	 */
	protected void createContents() {
		shell = new Shell();
		shell.setSize(674, 450);
		shell.setText("SWT Application");
		FormLayout layout = new FormLayout();
		layout.marginWidth = 5;
		layout.marginHeight = 5;
	    shell.setLayout(layout);
	    
	    text = new Text(shell, SWT.BORDER);
	    
	    Button loadButton = new Button(shell, SWT.NONE);
	    loadButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		if(text.getText()=="")System.out.println("Please seleate a folder!\n");
				else musicHarvest(new File(text.getText()),nrSong);
	    	}
	    });
	    
	    
	    Button browseButton = new Button(shell, SWT.NONE);
	    browseButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		DirectoryDialog dialog = new  DirectoryDialog(shell, SWT.OPEN);
	    		dialog.setFilterPath("C:\\Users\\Wen-Jye\\Documents\\eclipse workspace\\CSE_364_FINAL_PROJECT_SOUNDZAM\\musicIndex"); 
			    String result = dialog.open();
			    if(result != null) {
			    	text.setText(result);
			    }
	    	}
	    });
	    
	    
	    Label image = new Label(shell , SWT.BORDER);
	    FormData fd_image = new FormData();
	    fd_image.bottom = new FormAttachment(0, 300);
	    fd_image.left = new FormAttachment(text, 0, SWT.LEFT);
	    fd_image.right = new FormAttachment(browseButton, 0, SWT.RIGHT);
	    fd_image.top = new FormAttachment(text, 34);
	    image.setLayoutData(fd_image);
	    
	    Button matchButton = new Button(shell, SWT.NONE);
	    matchButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		if (hashMap.isEmpty())System.out.println("hashMap is empty!\n");
				else if(matchMap==null)System.out.println("matchMap is empty!\n");
				else{
					audioSampleMatching();
					SerializeHashMap();
					matchingSongList.clear();
				}
	    	}
	    });
	    
	    
	    Button recordButton = new Button(shell, SWT.NONE);
	    recordButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		//if (hashMap.isEmpty())System.out.println("hashMap is empty!\n");
				//else{
					System.out.println("start!\n");
					try {
						try {
							sampleRecorder(nrSong);
							} catch (IOException e1) {
								e1.printStackTrace();
							} catch (UnsupportedAudioFileException e1) {
						e1.printStackTrace();
							}
					} catch (LineUnavailableException ex) {
						ex.printStackTrace();
					}
				//}
	    	}
	    });
	    
	    
	    Button deserializeButton = new Button(shell, SWT.NONE);
	    deserializeButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		DeserializeHashMap();
	    	}
	    });
	    
	    
	    Button songListButton = new Button(shell, SWT.NONE);
	    songListButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		if(songList.size()<1)System.out.println("no song!\n");
				else for(int i=0;i<songList.size();i++)System.out.println(songList.get(i).get(0)+" by "+songList.get(i).get(2)+"\n");
	    	}
	    });
	    
	    
	    Button songRecommButton = new Button(shell, SWT.NONE);
	    songRecommButton.addSelectionListener(new SelectionAdapter() {
	    	@Override
	    	public void widgetSelected(SelectionEvent e) {
	    		if(tag=="")System.out.println("'tag=null' \n");
				else{
					Playlist pl=Playlist.fetchTagPlaylist(tag,"961efa5313c1ad6a357bf504f76cb38c");
					if(pl.getTracks().isEmpty())System.out.println("Recommendation Playlist is not found!\n");
					else{
						List<Track> lt=pl.getTracks();
						for(int i=0;i<lt.size();i++){
							System.out.println(lt.get(i).getName());
						System.out.println("Recommendation #"+Integer.toString(i+1)+": \n");
						System.out.println("\tSong: "+lt.get(i).getName()+"\n");
						System.out.println("\tAlbum: "+lt.get(i).getAlbum()+"\n");
						System.out.println("\tArtist: "+lt.get(i).getArtist()+"\n\n");
						}
						System.out.println("Playlist is based on tag: "+tag+"\n");
						lt.clear();
					}
				}
	    	}
	    });
	    FormData fd_text = new FormData();
	    fd_text.top = new FormAttachment(0, 15);
	    fd_text.left = new FormAttachment(0, 15);
	    fd_text.right = new FormAttachment(0, 435);
	    text.setLayoutData(fd_text);
	    FormData fd_loadButton = new FormData();
	    fd_loadButton.left = new FormAttachment(text, 24);
	    fd_loadButton.top = new FormAttachment(text, -2, SWT.TOP);
	    loadButton.setLayoutData(fd_loadButton);
	    loadButton.setText("Load");
	    FormData fd_browseButton = new FormData();
	    fd_browseButton.left = new FormAttachment(100, -97);
	    fd_browseButton.right = new FormAttachment(100, -22);
	    fd_browseButton.top = new FormAttachment(text, -2, SWT.TOP);
	    browseButton.setLayoutData(fd_browseButton);
	    browseButton.setText("Browse");
	    FormData fd_matchButton = new FormData();
	    fd_matchButton.top = new FormAttachment(image, 25);
	    fd_matchButton.left = new FormAttachment(0, 25);
	    matchButton.setLayoutData(fd_matchButton);
	    matchButton.setText("Match");
	    FormData fd_recordButton = new FormData();
	    fd_recordButton.top = new FormAttachment(matchButton, 0, SWT.TOP);
	    fd_recordButton.left = new FormAttachment(matchButton, 138);
	    recordButton.setLayoutData(fd_recordButton);
	    recordButton.setText("Record");
	    FormData fd_deserializeButton = new FormData();
	    fd_deserializeButton.bottom = new FormAttachment(matchButton, 0, SWT.BOTTOM);
	    fd_deserializeButton.left = new FormAttachment(loadButton, 0, SWT.LEFT);
	    deserializeButton.setLayoutData(fd_deserializeButton);
	    deserializeButton.setText("Deserialize");
	    FormData fd_songListButton = new FormData();
	    fd_songListButton.bottom = new FormAttachment(100, -10);
	    fd_songListButton.left = new FormAttachment(0, 60);
	    songListButton.setLayoutData(fd_songListButton);
	    songListButton.setText("Get Song List");
	    FormData fd_songRecommButton = new FormData();
	    fd_songRecommButton.top = new FormAttachment(songListButton, 0, SWT.TOP);
	    fd_songRecommButton.right = new FormAttachment(100, -199);
	    songRecommButton.setLayoutData(fd_songRecommButton);
	    songRecommButton.setText("Song Recommendation");
	
	    
	    
	}
	public void musicHarvest(File mp3Directory,long nrSong) {
		  String[] itemsInDirectory = mp3Directory.list();
		  for(String itemInDirectory:itemsInDirectory) {	
			  if(itemInDirectory.endsWith(".mp3")||itemInDirectory.endsWith(".wav")) {
				  int musicLength=0;
				  System.out.println(nrSong+": "+itemInDirectory);
				  System.out.println(nrSong+": "+itemInDirectory+"/n");
				  songList.add(new ArrayList<String>());
				  File musicFile = new File(mp3Directory, itemInDirectory);
				  try {
					  int i=songList.size()-1;
					  Mp3File song=new Mp3File(musicFile.getAbsolutePath());
					  //song.set
					  songList.get(i).add(song.getId3v2Tag().getTitle());
					  songList.get(i).add(song.getId3v2Tag().getAlbum());
					  songList.get(i).add(song.getId3v2Tag().getArtist());
					  musicLength = (int)song.getLengthInSeconds();
					System.out.println("length: "+musicLength);
				} catch (UnsupportedTagException e1) {
					e1.printStackTrace();
				} catch (InvalidDataException e1) {
					e1.printStackTrace();
				} catch (IOException e1) {
					e1.printStackTrace();
				} catch (NullPointerException e1) {
					e1.printStackTrace();
				}
				  try {
					musicRecorder(nrSong,musicFile, musicLength);
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
				  musicHarvest(new File(mp3Directory, itemInDirectory),nrSong);
			  }
		  }
	  }
	public void audioSampleMatching() {
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
			System.out.println("Best song is not found \n");
			tag="";
		}else{
			track=Track.getInfo(songList.get(bestSong).get(2), songList.get(bestSong).get(0), "961efa5313c1ad6a357bf504f76cb38c");
			if(track.getTags().isEmpty())tag=songList.get(bestSong).get(2);
			else for (String elem : track.getTags()){
				tag=elem;
				break;
			}
			System.out.println("Best song is: \n");
			System.out.println("\t"+songList.get(bestSong).get(0)+"\n");
			System.out.println("from the album: \n");
			System.out.println("\t"+songList.get(bestSong).get(1)+"\n");
			System.out.println("by artist: \n");
			System.out.println("\t"+songList.get(bestSong).get(2)+"\n\n");
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
	public void musicRecorder(long songId,File file, int time)throws LineUnavailableException, IOException,UnsupportedAudioFileException {		
		final int musicLength= time;
		PCM2PCMConversionProvider conversionProvider = new PCM2PCMConversionProvider();
		AudioInputStream in = AudioSystem.getAudioInputStream(file);		
		AudioFormat baseFormat = in.getFormat();
		System.out.println(baseFormat.toString());
		System.out.println(baseFormat.toString()+"\n");
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
				ByteArrayOutputStream rawAudio = new ByteArrayOutputStream();
				running = true;
				int n = 0;
				byte[] buffer = new byte[(int) 1024];
				try {
					while (running) {
						n++;
						//if (n > 1000)break;
						if (n > musicLength*40)break;
						int count = 0;
						count = outDinSound.read(buffer, 0, 1024);
						if (count > 0)rawAudio.write(buffer, 0, count);
					}
					determineKeyPoints(spectrumGenerator(rawAudio, false, id), id, false);
					rawAudio.close();
					line.close();
				} catch (IOException e) {
					System.err.println("I/O problems: " + e);
					System.exit(-1);
				}
			}
		});
		listeningThread.start();
	}
	public void sampleRecorder(long songId)throws LineUnavailableException, IOException,UnsupportedAudioFileException {
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
				ByteArrayOutputStream rawSample = new ByteArrayOutputStream();
				running = true;
				int n = 0;
				byte[] buffer = new byte[(int) 1024];
				Complex[][] spectrumSample;
				try {
					while (running) {
						n++;
						if(n%40==0)System.out.println(Integer.toString(n/40)+" sec\n");
						//if (n > 1000){  //25 seconds
						if (n > 400){  //10 seconds
							System.out.println("finished\n");
							break;
						}
						int count = 0;
						count = line.read(buffer, 0, 1024);						
						if (count > 0)rawSample.write(buffer, 0, count);
					}
					try {
						spectrumSample=spectrumGenerator(rawSample, true, id);
						determineKeyPoints(spectrumSample, id, true);
					} catch (Exception e) {
						System.err.println("Error: " + e.getMessage());
					}
					rawSample.close();
					line.close();
				} catch (IOException e) {
					System.err.println("I/O problems: " + e);
					System.exit(-1);
				}				 
			}
		});
		listeningThread.start();	
		}
	public Complex[][] spectrumGenerator(ByteArrayOutputStream rawSample, boolean record, long songID) {
		byte audio[] = rawSample.toByteArray();
		final int sampleSize = audio.length;
		int frames = sampleSize / 4096;
		Complex[][] spectrumSample = new Complex[frames][];
		for (int timeFrame = 0; timeFrame < frames; timeFrame++) {
			Complex[] complex = new Complex[4096];
			for (int i = 0; i < 4096; i++)complex[i] = new Complex(audio[(timeFrame * 4096) + i], 0);
			spectrumSample[timeFrame] = fft(complex);
		}
		
		if (!record){
			final ArrayList<ArrayList<Integer>> KEY; 
			final double[][] spectrogram=new double[frames][spectrumSample[0].length/2];
			double[][] mag=new double[frames][spectrumSample[0].length/2];
			System.out.println("["+frames+"] ["+spectrumSample[0].length);
			for (int i=0; i<frames; i++){
				for (int j = LOWER_LIMIT; j < UPPER_LIMIT - 1; j++) {
				//for (int j=0; j<spectrumSample[0].length/2; j++){
					mag[i][j]=((Math.log(spectrumSample[i][j].abs()+1))+(Math.log(spectrumSample[i][spectrumSample[0].length-1-j].abs()+1)))/2;
					spectrogram[i][j]=(Math.log10(mag[i][j]));
					//System.out.println("["+i+"] ["+j+"]: "+spectrogram[i][j]);
				}
			}
			
			KEY = new ArrayList<ArrayList<Integer>>();
		      for(int i = 0; i < spectrumSample.length; i++) {
		    	  double [] topMagnitude =new double [RANGE.length];
		          double [] highestFrequency =new double [RANGE.length];
		    	  KEY.add(new ArrayList<Integer>());
		    	  System.out.println("test1_"+i);
		    	  for (int X = 0; X < mag[i].length; X++)KEY.get(i).add(0);
		    	  for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT-1; freq++) {  
		    		  int index = getIndex(freq);    		  
		    		  if (mag [i][freq]> topMagnitude[index]) {
		    			  topMagnitude[index]= mag [i][freq];    			  
		    			  highestFrequency[index] = freq;
		    			  }
		    		  }
		    	  System.out.println("test2_"+i);
		    	  for (int points = 0; points < RANGE.length; points++){
		    		  KEY.get(i).set((int)highestFrequency[points], 1);    		  
		    	  }
		      }
		      int height=spectrogram[0].length/2;
		      int width=spectrogram.length;
		      //String filename="temp/spectrograma.jpg";
		      String filename="temp/spectrograma_"+Integer.toString((int)songID)+".jpg";
		      System.out.print("test");
		      BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
	            for (int i=0; i<width; i++){                   
	            	for (int j = LOWER_LIMIT; j < UPPER_LIMIT - 1; j++){
	            	//for (int j=0; j<height; j++){
	            		int value;
	            		if (!KEY.get(i).isEmpty() && KEY.get(i).get(j)==1){
	            			if(true){
	            				value=0xFF0000; // red
	                			bufferedImage.setRGB(i, height-1-j-LOWER_LIMIT, value);
	            			}
	            			else{
	            				value=255-(int)(spectrogram[i][j]*255);
	                    		bufferedImage.setRGB(i, height-1-j-LOWER_LIMIT, value<<16|value<<8|value);
	            			}
	            		}
	            		else {
	            			value=255-(int)(spectrogram[i][j]*255);
	            			bufferedImage.setRGB(i, height-1-j-LOWER_LIMIT, value<<16|value<<8|value);
	            		}
	            	}
	            }        
	            System.out.print("test_1");
				try {
	                int dotPos = filename.lastIndexOf(".");
	                String extension=filename.substring(dotPos + 1);
	                ImageIO.write(bufferedImage, extension, new File(filename));
				} catch (IOException e) {
					e.printStackTrace();
				}
				
		}
		
		
		return spectrumSample;
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
			 System.out.println("'MusicHashmap.ser' & 'songlist.ser' not found\n");
			 System.out.println("'Please browse a music folder to read in!\n");
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
		  System.out.println("Deserialized 'MusicHashmap.ser'\n");
		  System.out.println("Deserialized 'songlist.ser'\n");
		  System.out.println("Deserialized total "+songList.size()+" songs\n");
	      System.out.println("Deserialized 'MusicHashmap.ser'");
	      System.out.println("Deserialized 'songlist.ser'");
		  }
	    }
	public void determineKeyPoints(Complex[][] spectrumSample, long songId, boolean isMatching) {
		int sampleSize = spectrumSample.length;											//get freq size
		this.matchMap = new HashMap<Integer, Map<Integer, Integer>>();					//create hash ID
		topMagnitude = new double[sampleSize][5];										//create magnitude list
		for (int i = 0; i < sampleSize; i++){
			for (int j = 0; j < 5; j++)topMagnitude[i][j] = 0; 							//initial magnitude list
		}
		highestFrequency = new double[sampleSize][UPPER_LIMIT];							//create high-freq list
		for (int i = 0; i < sampleSize; i++){
			for (int j = 0; j < UPPER_LIMIT; j++)highestFrequency[i][j] = 0;			//initial high-freq list
		}
		keyPoints = new long[sampleSize][5];											//create keyPoints list
		for (int i = 0; i < sampleSize; i++){
			for (int j = 0; j < 5; j++)keyPoints[i][j] = 0;								//initial keyPoints list
		}
		for (int t = 0; t < sampleSize; t++) {
			for (int freq = LOWER_LIMIT; freq < UPPER_LIMIT - 1; freq++) {
				double mag = Math.log(spectrumSample[t][freq].abs() + 1);
				if (!isMatching) {														//change music from stereo to mono
					mag = (Math.log(spectrumSample[t][freq].abs() + 1)+Math.log(spectrumSample[t][(spectrumSample[0].length)-1-freq].abs() + 1))/2;
				}
				else mag = Math.log(spectrumSample[t][freq].abs() + 1);
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
