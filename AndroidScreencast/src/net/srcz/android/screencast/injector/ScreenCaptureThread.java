package net.srcz.android.screencast.injector;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import javax.swing.SwingUtilities;

import net.srcz.android.screencast.recording.QuickTimeOutputStream;
import net.srcz.android.screencast.ui.JPanelScreen;

import com.android.ddmlib.Device;
import com.android.ddmlib.RawImage;

public class ScreenCaptureThread extends Thread {

	private BufferedImage image;
	private Dimension size;
	private Device device;
	private JPanelScreen jp;
	private QuickTimeOutputStream qos = null;
	private boolean landscape = false;

	public ScreenCaptureThread(Device device, JPanelScreen jp) {
		this.device = device;
		this.jp = jp;
		image = null;
		size = new Dimension();
	}

	public Dimension getPreferredSize() {
		return size;
	}

	public void run() {
		do {
			try {
				fetchImage();
			} catch (java.nio.channels.ClosedByInterruptException ciex) {
				break;
			} catch (IOException e) {
				System.err.println((new StringBuilder()).append(
						"Exception fetching image: ").append(e.toString())
						.toString());
			}

		} while (true);
	}

	public void startRecording(File f) {
		try {
			qos = new QuickTimeOutputStream(f,
					QuickTimeOutputStream.VideoFormat.JPG);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		qos.setVideoCompressionQuality(1f);
		qos.setTimeScale(30); // 30 fps
	}

	public void stopRecording() {
		try {
			qos.close();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		qos = null;
	}

	int nb = -1;
	int rwidth = -1;
	int rheight = -1;
	private void fetchImage() throws IOException {
		if (device == null) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				return;
			}
			return;
		}
		byte[] buff = new byte[5000];
		
		if(nb == -1) {
			RawImage rawImage = null;
			synchronized (device) {
				rawImage = device.getScreenshot();
			}
			if(rawImage != null) {
				nb = rawImage.data.length;
				rwidth = rawImage.width;
				rheight = rawImage.height;
				System.out.println("length = "+nb);
			}
		}
		if(nb != -1) {
			if(Injector.s != null) {
				final InputStream is = Injector.s.getInputStream();
				while(true) {
					int nbRead = is.read(buff,0,nb);
					display(buff,rwidth,rheight);
				}
			}
		}
		//if (rawImage != null && rawImage.bpp == 16) {
			
			//display(rawImage.data, rawImage.width, rawImage.height);
		//} else {
			//System.err.println("Received invalid image");
		//}
		
	}

	/*
	 * public void convert() { PaletteData palette = new PaletteData(0xf800,
	 * 0x07e0, 0x001f); ImageData imageData = new ImageData(rawImage.width,
	 * rawImage.height,rawImage.bpp, palette, 1, rawImage.data); return new
	 * Image(getParent().getDisplay(), imageData);
	 * 
	 * byte buffer[] = rawImage.data; int index = 0; for (int y = 0; y <
	 * rawImage.height; y++) { for (int x = 0; x < rawImage.width; x++) { int
	 * value = buffer[index++] & 0xff; value |= buffer[index++] << 8 & 0xff00;
	 * int r = (value >> 11 & 0x1f) << 3; int g = (value >> 5 & 0x3f) << 2; int
	 * b = (value & 0x1f) << 3; value = 0xFF << 24 | r << 16 | g << 8 | b;
	 * //scanline[x] = 0xff000000 | r << 16 | g << 8 | b; image.setRGB(x, y,
	 * value); } } }
	 */

	public void toogleOrientation() {
		landscape = !landscape;
	}
	
	public void display(byte[] buffer, int width, int height) {
		int width2 = landscape ? height : width;
		int height2 = landscape ? width : height;
		if (image == null) {
			image = new BufferedImage(width2, height2,
					BufferedImage.TYPE_INT_RGB);
			size.setSize(image.getWidth(), image.getHeight());
		} else {
			if (image.getHeight() != height2 || image.getWidth() != width2) {
				image = new BufferedImage(width2, height2,
						BufferedImage.TYPE_INT_RGB);
				size.setSize(image.getWidth(), image.getHeight());
			}
		}
		int index = 0;
		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {
				int value = buffer[index++] & 0xff;
				value |= buffer[index++] << 8 & 0xff00;
				int r = (value >> 11 & 0x1f) << 3;
				int g = (value >> 5 & 0x3f) << 2;
				int b = (value & 0x1f) << 3;

				value = 0xFF << 24 | r << 16 | g << 8 | b;
				if (landscape)
					image.setRGB(y, width - x - 1, value);
				else
					image.setRGB(x, y, value);
			}
		}
		try {
			if (qos != null)
				qos.writeFrame(image, 10);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		SwingUtilities.invokeLater(new Runnable() {

			public void run() {
				jp.handleNewImage(size, image, landscape);
			}
		});
	}

}
