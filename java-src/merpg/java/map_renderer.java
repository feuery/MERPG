package merpg.java;

import clojure.java.api.Clojure;
import clojure.lang.*;
import java.awt.geom.AffineTransform;
import java.util.*;
import java.awt.image.*;
import java.awt.AlphaComposite;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Color;

import javax.imageio.ImageIO;

// TODO: hit tiles, this-tile-selected - rectangle of fill-tool

public class map_renderer
{
    //We need a static method that kills all the running threads
    public BufferedImage visible_buffer;
    private BufferedImage drawing_buffer;

    private static final int TILEW = 50;
    
    Atom map_atom, tileset_atom, selectedtool_atom;

    int w=0, h=0; //tiles, not pixels
    IFn layer_visible = Clojure.var("merpg.immutable.basic-map-stuff", "layer-visible"),
	meta = Clojure.var("clojure.core", "meta"),
	get_hitdata = Clojure.var("merpg.immutable.map-layer-editing", "get-hitdata");
    
    public map_renderer(clojure.lang.Atom map_atom, Atom tileset_atom, Atom selectedtool_atom)
    {
	// List<List<List<Map>>> is the type of whatever map_atom contains
	List<List<List<Map>>> map = (List<List<List<Map>>>)map_atom.deref();

	//Map<kw, List<List<BufferedImage>> is the type of tileset_atom
	
	w = map.get(0).size();
	h = map.get(0).get(0).size();
	
	visible_buffer = new BufferedImage(w * 50, h * 50, BufferedImage.TYPE_INT_ARGB);
	drawing_buffer = new BufferedImage(w * 50, h * 50, BufferedImage.TYPE_INT_ARGB);
	
	this.map_atom = map_atom;
	this.tileset_atom = tileset_atom;
	this.selectedtool_atom = selectedtool_atom;
	

	System.out.println("I'm ready, size of map is " + w +", "+ h + " and size of the visible_buffer is " + (w * 50) + ", "+ (h*50));
    }

    public void resize_happened ()
    {
	List<List<List<Map>>> map = (List<List<List<Map>>>)map_atom.deref();

	w = map.get(0).size();
	h = map.get(0).get(0).size();
	
	visible_buffer = new BufferedImage(w * 50, h * 50, BufferedImage.TYPE_INT_ARGB);
	drawing_buffer = new BufferedImage(w * 50, h * 50, BufferedImage.TYPE_INT_ARGB);
    }

    private boolean rendering=false;
    
    public BufferedImage render()
    {
	try {
	    System.out.println("Rendering");
	    rendering = true;

	    List<List<List<Map<Keyword, Object>>>> map = (List<List<List<Map<Keyword, Object>>>>)map_atom.deref();
	    Map<Keyword, List<List<BufferedImage>>> tileset_collection = (Map<Keyword, List<List<BufferedImage>>>)tileset_atom.deref();
			
	    Graphics2D map_g = drawing_buffer.createGraphics();				

	    for(int layer = 0; layer < map.size(); layer++) {

		if(layer_visible.invoke(map.get(layer)).equals(false)) continue;
		Map<Keyword, Object> layerMeta = (Map<Keyword, Object>)meta.invoke(map.get(layer));				    
		long opacity = (long)layerMeta.get(Keyword.intern("opacity"));
				    
		for(int x = 0; x < map.get(layer).size(); x++) {
		    for(int y = 0; y < map.get(layer).get(x).size(); y++) {
			// :tileset is a keyword, rest are integers
			Keyword x_kw = Keyword.intern( "x"),
			    y_kw = Keyword.intern( "y");

			if (x_kw == null) {
			    // System.out.println(x_kw+" is null");
			    return visible_buffer;
			}

			if (y_kw == null) {
			    // System.out.println(y_kw+" is null");
			    return visible_buffer;
			}
			
			Map<Keyword, Object> tile = map.get(layer).get(x).get(y);

			if (tile == null) {
			    // System.out.println("tile is null");
			}

			// System.out.println("Tile is "+tile+"");
			// System.out.println("x_kw is "+x_kw);
			// System.out.println("tile.get(x_kw) is " + tile.get(x_kw));
		        
			int tile_x = ((Long)tile.get(x_kw)).intValue(),
			    tile_y = ((Long)tile.get(y_kw)).intValue(),
			    rotation_degrees = ((Long)tile.get(Keyword.intern( "rotation"))).intValue() * 90;
			Keyword tileset_index = (Keyword)tile.get(Keyword.intern( "tileset"));

			List<List<BufferedImage>> tileset = tileset_collection.get(tileset_index);
			BufferedImage imgtile = tileset.get(tile_x).get(tile_y);

			if(rotation_degrees != 0) {
			    imgtile = Rotate(imgtile, rotation_degrees);
			}

			if(opacity > 0) {
			    if(opacity < 255) {
				map_g.drawImage(SetOpacity(imgtile, (double)opacity), x * 50, y * 50, null);
			    }
			    else map_g.drawImage(imgtile, x * 50, y * 50, null);
			}
			    
		    }
		}
	    }

	    Keyword selected_tool = (Keyword)selectedtool_atom.deref();

	    if(selected_tool.equals(Keyword.intern("hit-tool"))) {
		Object MAP = map_atom.deref();
		for(int x = 0; x < w; x++)
		    for(int y = 0; y < h; y++) {
			
			if(get_hitdata.invoke(MAP, x, y).equals(true)) {
			    map_g.setColor(new Color(255, 0, 0, 112));
			}
			else map_g.setColor(new Color(0, 255, 0, 112));

			map_g.fill(new Rectangle(x * TILEW, y * TILEW,
						 TILEW, TILEW));
		    }
	    }

	    System.out.println("Rendered. Swapping buffers");
				
	    BufferedImage old_visible_buffer = visible_buffer;
	    visible_buffer = drawing_buffer;
	    drawing_buffer = old_visible_buffer;

	}
	catch(Exception ex) {
	    ex.printStackTrace();
	}
	finally {
	    rendering = false;
	}

	return visible_buffer;
    }

    /*
      (defn rotate [img degrees]
  (if (instance? java.awt.Image img)
    (let [W (img-width img)
          H (img-height img)
          toret (image (img-width img)
                       (img-height img))
          rad-rot (Math/toRadians (double degrees))
          tx (doto (AffineTransform.)
               (.rotate rad-rot (double (/ W 2)) (double (/ H 2))))
          op (AffineTransformOp. tx AffineTransformOp/TYPE_BILINEAR)]
      (.filter op img toret)
      (draw-to-surface toret
                       (with-handle
                         (.translate handle 0 0))))
    (println "Img's class is " (class img) " at merpg.2D.core/rotate")))
     */

    public BufferedImage Rotate(BufferedImage img, int degrees)
    {
	double w = img.getWidth(), h = img.getHeight();
	BufferedImage new_img = new BufferedImage((int)w, (int)h, BufferedImage.TYPE_INT_ARGB);
	Graphics2D g = new_img.createGraphics();

	double rad_rot = Math.toRadians((double)degrees);
	AffineTransform tx = new AffineTransform();
	tx.rotate(rad_rot, w / 2, h / 2);

	AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
	op.filter(img, new_img);
	g.translate(0, 0);

	return new_img;	
    }

    public BufferedImage SetOpacity(BufferedImage img, double opacity)
    {
	if(opacity > 255 || opacity < 0) throw new RuntimeException("Opacity should be in-between 0 <= opacity <= 255");
	BufferedImage new_img = new BufferedImage(img.getWidth(), img.getHeight(), BufferedImage.TYPE_INT_ARGB);
	double new_opacity = opacity / 255.0;
	Graphics2D g = new_img.createGraphics();

	try {
	    g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, (float)new_opacity));
	    g.drawImage(img, 0, 0, null);

	    return new_img;
	}
	catch(Exception ex) {
	    System.out.println("New opacity: "+opacity);
	    throw new RuntimeException(ex);
	}
    }

}
