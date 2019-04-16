package org.heigit.bigspatialdata.eventfinder;

public class EditCountEnum {
	private int GEOM;
	private int TAG;
	
	EditCountEnum(int geom, int tag) {
		this.GEOM = geom;
		this.TAG = tag;
	}
	
	public int get_GEOM() {
		return this.GEOM;
	}
	
	public int get_TAG() {
		return this.TAG;
	}
	
	public void set_GEOM(int GEOM) {
		this.GEOM = GEOM;
	}
	
	public void set_TAG(int TAG) {
		this.TAG = TAG;
	}
}
