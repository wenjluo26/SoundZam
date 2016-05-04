 
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.imageio.ImageIO;
import javax.swing.JFrame;

public class spectrumgramMonitorM extends JFrame {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private double [][] spectrogramData;
	private boolean keyVisible;
	private ArrayList<ArrayList<Integer>> KEY;
	private String filename;
	private int blockSizeX=2;
	private int blockSizeY=3;
    public spectrumgramMonitorM(double[][]c, ArrayList<ArrayList<Integer>> k,boolean keypoint,int songID) {
        super("spectrumgramMonitorM");
        keyVisible=keypoint;
        KEY=k;
        spectrogramData=c;
        filename="temp/spectrograma_"+Integer.toString(songID)+".jpg";
        setSize(spectrogramData.length*blockSizeX,getheight()*blockSizeY);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
    }
 
    void draw(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        if (spectrogramData!=null){
			int width=spectrogramData.length;
			int height=getheight();
		        int x=0;
				for (int i=0; i<width; i++){
					int freq =1;
					for (int line=1; line<height; line++){
						int value;
						if (!KEY.get(i).isEmpty()&& KEY.get(i).get(freq)==1){
							if(keyVisible)g2d.setColor(new Color(255,0,0));
							else{
								value=255-(int)(spectrogramData[i][freq]*255);
								g2d.setColor(new Color( value/2,255-value,value));
							}
						}
						else{
							value=255-(int)(spectrogramData[i][freq]*255);
							g2d.setColor(new Color( 255/2,255-value,value));
						}
						g2d.fillRect(x*blockSizeX, (height-1-line)*blockSizeY,blockSizeX,blockSizeY);
						if ((Math.log10(line) * Math.log10(line))> 1) freq += (int) (Math.log10(line) * Math.log10(line)*Math.log10(line));
						else freq++;
						if (freq>1024)break;
					}
				x++;
				i+=(spectrogramData.length/2500);
				}
			height=spectrogramData[0].length;
            BufferedImage bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            for (int i=0; i<width; i++){                   
            	for (int j=0; j<height; j++){
            		int value;
            		if (!KEY.get(i).isEmpty() && KEY.get(i).get(j)==1){
            			if(keyVisible){
            				value=0xFF0000; // red
                			bufferedImage.setRGB(i, height-1-j, value);
            			}
            			else{
            				value=255-(int)(spectrogramData[i][j]*255);
                    		bufferedImage.setRGB(i, height-1-j, value<<16|value<<8|value);
            			}
            		}
            		else {
            			value=255-(int)(spectrogramData[i][j]*255);
            			bufferedImage.setRGB(i, height-1-j, value<<16|value<<8|value);
            		}
            	}
            }             
            try {
            	int dotPos = filename.lastIndexOf(".");
            	String extension=filename.substring(dotPos + 1);
            	ImageIO.write(bufferedImage, extension, new File(filename));
            } catch (IOException e) {
                    e.printStackTrace();
            }
        }
        else System.err.println("renderSpectrogramData error: Empty Wave");
    }
    public void paint(Graphics g) {
        super.paint(g);
        draw(g);
    }
    private int getheight(){
    	int count=1;
    	for(int i=1;i<spectrogramData[0].length;i++){
    		if ((Math.log10(i) * Math.log10(i))> 1) count += (int) (Math.log10(i) * Math.log10(i)*Math.log10(i));
    		else count++;
    		if (count>spectrogramData[0].length) return i;
    	}
    	return 0;
    }
    
}