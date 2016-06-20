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

public class map_renderer
{

    private static final int TILEW = 50;

    
    static IFn renderable_layers_of = Clojure.var("merpg.mutable.layers", "renderable-layers-of!"),
	peek_registry = Clojure.var("merpg.mutable.registry", "peek-registry"),
	layer_metadata_of = Clojure.var("merpg.mutable.registry-views", "layer-metadata-of!"),
	get_renderable_layer = Clojure.var("merpg.mutable.layers", "get-renderable-layer!"),
	mapwidth = Clojure.var("merpg.mutable.layers", "mapwidth!"),
	mapheight = Clojure.var("merpg.mutable.layers", "mapheight!");
    static Keyword tileset = Keyword.intern("tileset"),
	rotation_kw = Keyword.intern("rotation"),
	images = Keyword.intern("images"),
	x_kw = Keyword.intern("x"),
	y_kw = Keyword.intern("y"),
	visible = Keyword.intern("visible?"),
	opacity = Keyword.intern("opacity"),
	meta_kw = Keyword.intern("meta");
	

    static int toLong(Object o) {
	if (o == null) {
	    throw new RuntimeException ("O is null, you stupid human");
	}
	else if(o instanceof Long) {
	    return ((Long)o).intValue();
	}
	else if (o instanceof Integer) {
	    return ((Integer)o).intValue();
	}

	throw new RuntimeException(o.toString()+" is "+o.getClass().getName());
    }
    
    public map_renderer( )// clojure.lang.Atom map_atom, Atom tileset_atom, Atom selectedtool_atom)
    { }

    public static BufferedImage render(Keyword map_id, Keyword layer_id)
    {
	System.out.println("layerid is " + layer_id);
	BufferedImage final_surface = null;
	Graphics2D final_g = null;
	try {
	    List<List<Map<Keyword, Object>>> layer = (List<List<Map<Keyword, Object>>>)get_renderable_layer.invoke(map_id, layer_id);
	    if(layer == null) {
		System.out.println("layer " + map_id + ":" + layer_id + " is null");
		return null;
	    }

	    Map<Keyword, Object> meta = (Map<Keyword, Object>)layer.get(0).get(0).get(meta_kw);
	    boolean visible_p = ((Boolean)meta.get(visible)).booleanValue();
	    
	    int W_tiles = toLong(mapwidth.invoke(map_id)),
		H_tiles = toLong(mapheight.invoke(map_id)),
		opacity_l = toLong(meta.get(opacity)),
		W_pixels = W_tiles * 50,
		H_pixels = H_tiles * 50;

	    final_surface = new BufferedImage(W_pixels, H_pixels, BufferedImage.TYPE_INT_ARGB);

	    if(opacity_l <= 0) return final_surface;
	    
	    final_g = final_surface.createGraphics();
	    final_g.setColor(Color.BLACK);
	    final_g.fill(new Rectangle(0, 0, W_pixels, H_pixels));

	    System.out.println("According to mapwidth!, mapw is " + W_tiles+", but according to the data structure, the width is " +layer.size());

	    for(int x = 0; x < layer.size(); x++) {
		for(int y = 0; y < layer.get(x).size(); y++) {
		    try {
			Map<Keyword, Object> tiledata = layer.get(x).get(y);
			Keyword current_tileset = (Keyword)tiledata.get(tileset);
			Map<Keyword, Object> tileset_meta = (Map<Keyword, Object>)peek_registry.invoke(current_tileset);
			List<List<BufferedImage>> tileset_surfaces = (List<List<BufferedImage>>)tileset_meta.get(images);
			System.out.println("---Here's some meta------------");
			for(Keyword k: tiledata.keySet()) {
				System.out.println(k + " = " + tiledata.get(k));
			}
			System.out.println("-------------------------------");
			int tilex = toLong(tiledata.get(x_kw));
			int tiley = toLong(tiledata.get(y_kw));
			int rotation = toLong(tiledata.get(rotation_kw)) * 90;
			BufferedImage tile_surface;

			if(rotation != 0) 
			    tile_surface = Rotate(tileset_surfaces.get(tilex).get(tiley), rotation);
			else
			    tile_surface = tileset_surfaces.get(tilex).get(tiley);
			
			final_g.drawImage(tile_surface, null, x * TILEW, y * TILEW);
		    }
		    catch(IndexOutOfBoundsException ex) {
			System.out.println("Index out of bounds");
			System.out.println("x, y: " + x + ", " + y);
			System.out.println("W_tiles, H_tiles: " + W_tiles + ", "+ H_tiles);
			System.out.println("w: " + layer.size());
			System.out.println("h: " + layer.get(x).size());
		    }
		}
	    }

	    if (opacity_l >= 255)
		return final_surface;
	    else return SetOpacity(final_surface, (double)opacity_l);
	}
	catch(Exception ex) {
	    ex.printStackTrace();
	    return new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
	}
    }

    // an instance of reagi-events
    // public static BufferedImage render(Keyword map_id)
    // {
    // 	System.out.println("Mapid is " +map_id);
    // 	BufferedImage final_surface = null;
    // 	Graphics2D final_g = null;
    // 	try {
    // 	    List<List<List<Map<Keyword, Object>>>> layers = (List<List<List<Map<Keyword, Object>>>>)renderable_layers_of.invoke(map_id);
	    
    // 	    List<Map<Keyword, Object>> layer_metas = (List<Map<Keyword, Object>>)layer_metadata_of.invoke(map_id);
    // 	    int layer_count = layers.size();
    // 	    int W, H;

    // 	    for(int l = 0; l < layer_count; l++) {

    // 		boolean visible_p = ((Boolean)layer_metas.get(l).get(Keyword.intern("visible?"))).booleanValue();
    // 		String layer_name = layer_metas.get(l).get(Keyword.intern("name")).toString();
    // 		System.out.println("Rendering "+layer_name);
    // 		if(!visible_p) continue;
		
    // 		List<List<Map<Keyword, Object>>> layer = layers.get(l);
    // 		// pixels
    // 		int w_tiles = layer.size(),
    // 		    h_tiles = layer.get(0).size(),
    // 		    opacity = ((Long)layer_metas.get(l).get(Keyword.intern("opacity"))).intValue();

    // 		W = w_tiles * TILEW;
    // 		H = h_tiles * TILEW;
		
    // 		BufferedImage layer_surface = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
    // 		final_surface = final_surface == null? new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB): final_surface;
    // 		final_g = final_g == null? final_surface.createGraphics(): final_g;
    // 		Graphics2D g = layer_surface.createGraphics();

    // 		g.setColor(Color.BLACK);
    // 		g.fill(new Rectangle(0, 0, W, H));

    // 		for(int x = 0; x < w_tiles; x++) {
    // 		    for(int y = 0; y < h_tiles; y++) {
    // 			Map<Keyword, Object> tile_data = layer.get(x).get(y);
    // 			Keyword tileset = (Keyword)tile_data.get(Keyword.intern("tileset"));

    // 			Map<Keyword, Object> tileset_meta = (Map<Keyword, Object>)peek_registry.invoke(tileset);
    // 			List<List<BufferedImage>> tileset_surfaces = (List<List<BufferedImage>>)tileset_meta.get(Keyword.intern("images"));
    // 			int tileset_x = ((Long)tile_data.get(Keyword.intern("x"))).intValue(),
    // 			    tileset_y = ((Long)tile_data.get(Keyword.intern("y"))).intValue(),
    // 			    rotation = ((Long)tile_data.get(Keyword.intern("rotation"))).intValue() * 90;
    // 			BufferedImage tile_surface;
			
    // 			if(rotation != 0) 
    // 			    tile_surface = Rotate(tileset_surfaces.get(tileset_x).get(tileset_y), rotation);
    // 			else
    // 			    tile_surface = tileset_surfaces.get(tileset_x).get(tileset_y);
			
    // 			g.drawImage(tile_surface, null, x * TILEW, y * TILEW);
    // 		    }
    // 		}			
    
    // 		if(opacity <= 0 || opacity >= 255)
    // 		    final_g.drawImage(layer_surface, null, 0, 0);
    // 		else final_g.drawImage(SetOpacity(layer_surface, (double)opacity), null, 0, 0);
    // 	    }

    // 	    // TODO implement hit-data rendering when the new tool infrastructure is up and running
	    

    // 	}
    // 	catch(Exception ex) {
    // 	    ex.printStackTrace();
    // 	}	

    // 	return final_surface;
    // }
    
    public static BufferedImage Rotate(BufferedImage img, int degrees)
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

    public static BufferedImage SetOpacity(BufferedImage img, double opacity)
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
