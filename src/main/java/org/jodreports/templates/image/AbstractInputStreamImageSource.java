package org.jodreports.templates.image;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.imageio.ImageIO;

import org.apache.commons.io.IOUtils;

public abstract class AbstractInputStreamImageSource implements ImageSource {

	protected abstract InputStream getInputStream() throws IOException;

	public void write(OutputStream outputStream) throws IOException {
		InputStream inputStream = null;
		try {
			inputStream = getInputStream();
			IOUtils.copy(inputStream, outputStream);
		} finally {
			inputStream.close();;
		}
	}
	
	public int getWidth() {
		try {
			return ImageIO.read(getInputStream()).getWidth();
		} catch (IOException e) {}
		return 0;
	}

	public int getHeight() {
		try {
			return ImageIO.read(getInputStream()).getHeight();
		} catch (IOException e) {}
		return 0;
	}

}