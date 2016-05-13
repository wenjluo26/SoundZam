# SoundZam
Introduction - 
SoundZam is a music identification program written in JAVA. Soundzam uses a microphone to gather a short 25 seconds sample of audio being played. It creates an acoustic fingerprint based on the sample, and compares it against a database for a match. If it finds a match, it sends information such as the artist, song title, and album back to the user. In addition to music identification function, Soundzam is also able to give user a list of the recommendation music based on the identified music.
How does it works - 
Before soundzam is able to identify a music, it needs to build its own music database that contain the information of music name, artist, album, and fingerprints. Fingerprints contains key points which marked the frequencies of “peak intensity” of given moment in time, and it keeps track of the frequency and the amount of time from the beginning of the music. In order to make Soundzam works, Soundzam will gather every music from a user provided music library folder. Than soundzam runs an analysis on every music to create fingerprints and store those fingerprints in to database. Second, user record a sample of a song they hear, which is 25 seconds long. Soundzam analysis the sample and creates fingerprints for it. Than soundzam runs a search for a matching fingerprint in their database. Soundzam compare key points of sample and every song from database. The matching process take a “offset time” into account by Subtract moments in time for more accuracy matching. If key point at 1 second form sample audio is matching with key point at 5 second from “song 1” from data base with offset time (5 second -1 second =4). If they have more matching at same offset time (6 second -2 second =4)  (7 second -3 second =4)  (8 second -4 second =4), we can assume that audio sample and “song 1”  have same melody, which means that the sample audio is more likely to be “song 1”. Matching is compare with hits in time instead of hits in keys. If a match is found, the song info is returned to the user, otherwise an error is returned. 

Project
Microphone/Audio input-
I implemented a microphone in Soundzam with no problem by using “TargetDataLine“ function. But I hit my first challenge when I implemented an audio input in Soundzam. Java doesn’t support MPEG3 format of audio. I got error whhen I tried to read MPEG3 format audio files. However, since MPEG3 is the most used format in music industry, I decided to do a research on google. I eventually found a solution for it. I downloaded Jar files from http://www.jsresources.org/ that allow me to read in the MPEG3 format. Every music and sample that read in by soundzam was saved as a “ByteArrayOutputStream“, which is a very long array of frequencies known as “time domain”. Time domain representation the information of time and frequencies. However, in order to create fingerprints of songs, we need an addition information on the intensity.  In this case spectrogram is what we need. 
Spectrogram/Discrete Fourier Transform
A spectrogram is a visual representation of the spectrum of frequencies in a sound or other signal as they vary with time. Spectrogram is contains three axis: On one axis is time, on another is frequency, and on the 3rd is intensity.  Each point on the graph represents the intensity of a given frequency at a specific point in time. To turn our “time domain” data into “frequency domain” data we need to apply “Discrete Fourier Transform”. However, we lost our time information during the transforming. So I sliced Time domain into many tiny pieces, than I apply “Discrete Fourier Transform” on every piece. Create an array that sort “frequency domain” by time order. The algorithm for Discrete Fourier Transform I used can be found here http://introcs.cs.princeton.edu/java/97data/FFT.java 
Create key points/fingerprints
	Fingerprints is a hash map of key points, song id, and timing. Key points is the loudest frequencies for a given moment in time.  To create key points, I designed a frequencies ranges for each line in the spectrogram. In my project, the ranges are 40-80, 80-120, 120-180, 180-300. (400 Hz-800Hz, 800 Hz-1200 Hz, 1200 Hz-1800 Hz, 1800 Hz-3000 Hz). Key points are marked for each ranges.  I included the fuzz factor in the result reduce the effect of noise environment while recording. The result I got look like it:
	
40  43  104  127  236
40  43  103  172  243
40  44  82  130  237
40  63  92  130  289
40  46  86  123  203
40  45  92  148  269
40  77  91  150  252
40  62  106  144  239
Etc…

And the spectrogram with highlighted key points looks like this:  

Then I put frequencies into a single “long” that looks like this: 2361271044340. I use this long as the key of the hash map, and save other information (song id, time) as the value part of hash map. After having all the data we need. We can finally come to the last part and the most important part of project- fingerprints matching!

Fingerprint Matching/recommendation playlist
	During the matching process, I created a hash map that use song ID as its key, offset time and counts of repeating match offset as its value. If a certain song has the most hits count in offset time with sample.  Soundzam returns the infomation of song to the user. Soundzam will also acquire more information about the song from Last.fm such as information about artist, album, and tag of song. User has the option to get the recommendation playlist based on the tag of song 


Conclusions - 
During this project, I have accomplished on creating a fully working music identification program with an addition function that suggest user a playlist based on the tag of a song. I have learned how to implement microphone, spectrum analyzer, spectrogram monitor with java.  I also became more skilled on working with audio file, working with third party API, working with GUI, and writing java in more efficient way. 


References -
Creating Shazam in Java | royvanrijn http://www.royvanrijn.com/blog/2010/06/creating-shazam-in-java/ 
How Shazam Works | Free Won't https://laplacian.wordpress.com/2009/01/10/how-shazam-works/ 
FFT.java http://introcs.cs.princeton.edu/java/97data/FFT.java 
Java Sound Resources http://www.jsresources.org/ 
lastfm-java - Last.fm API bindings for Java - Google Project Hosting https://code.google.com/p/lastfm-java/ 
mpatric/mp3agic · GitHub https://github.com/mpatric/mp3agic 
