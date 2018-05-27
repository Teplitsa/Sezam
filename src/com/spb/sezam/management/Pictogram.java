package com.spb.sezam.management;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.graphics.drawable.BitmapDrawable;

public class Pictogram  implements Comparable<Pictogram>{
	
	/** Full path of the file/folder (inside assets)*/
	private String path;
	
	private String fileName = null;
	
	private float number = 0;
	
	private BitmapDrawable icon;
	
	public Pictogram(String path){
		this.path = path;
		Pattern afterLastSlash = Pattern.compile("[^\\" + File.separator + "]([^\\" +  File.separator + "]*)$");
		Matcher m = afterLastSlash.matcher(path);
	    if(m.find()){
	    	fileName = m.group(0);
	    }
	    if(fileName != null){
	    	Pattern beforeDotAndLetter = Pattern.compile("^.*?(?=.[a-zA-Z])");
	    	m = beforeDotAndLetter.matcher(fileName);
	    	if(m.find()){
	    		number = Float.parseFloat(m.group(0));
	    	}
	    	//otherwise number=0
	    }
	}
	

	public ElementType getType() {
		return ElementType.FILE;
	}

	/** Returns full path of the file/folder (inside assets)
	 */
	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}
	
	/**
	 *  Returns only file/folder name (without path information) 
	 */
	public String getFileName(){
		return fileName;
	}
	
	/**
	 *  Name as will be seen in the app(without '.jpg', path and other information) 
	 */
	public String getName(){
		if(fileName==null){
			return null;
		}
		String[] texts = fileName.split("\\.");
		int size = texts.length;
		if (ElementType.FILE == getType()) {
			// assume size > 1
			return texts[size - 2];
		} else {
			return texts[size - 1];
		}
		
		//Armen's Varyant
		/*Pattern regex = Pattern.compile(".*\\.(.*?)\\..*?$");
		Matcher regexMatcher = regex.matcher("asd.jpg");
		return regexMatcher.group(0);*/
	}
	
	public String getPathWithAssests(){
		return "assets://" + PictogramManager.BASE_FOLDER +
				 File.separator + path;
	}
	

	/**
	 * @return Number before file's name.
	 * If there is no number, then returns 0.
	 */
	public float getNumber() {
		return number;
	}


	/*public BitmapDrawable getIcon() {
		return icon;
	}

	public void setIcon(BitmapDrawable icon) {
		this.icon = icon;
	}*/

	@Override
	public boolean equals(Object obj) {
		if (!(obj instanceof Pictogram))
			return false;
		if (obj == this)
			return true;
		//let throw null pointer to check
		return path.equals(((Pictogram)obj).path);
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		return result;
	}

	@Override
	public String toString() {
		return "Pictogram [path=" + path + "]";
	}

	@Override
	public int compareTo(Pictogram pictogram) {
		return  Float.valueOf(this.number).compareTo(pictogram.getNumber());
	}
	
}
