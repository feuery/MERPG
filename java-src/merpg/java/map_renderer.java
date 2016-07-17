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

    
    static IFn peek_registry = Clojure.var("merpg.mutable.registry", "peek-registry"),
	layer_metadata_of = Clojure.var("merpg.mutable.registry-views", "layer-metadata-of!"),
	get_renderable_layer = Clojure.var("merpg.mutable.layers", "get-renderable-layer!"),
	mapwidth = Clojure.var("merpg.mutable.layers", "mapwidth!"),
	mapheight = Clojure.var("merpg.mutable.layers", "mapheight!"),
	can_render = Clojure.var("merpg.mutable.registry", "is-render-allowed?");
    
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
    
    public map_renderer( )
    { }

    public static BufferedImage render(Keyword map_id, Keyword layer_id)
    {
	Boolean can_render_bool = (Boolean)can_render.invoke();

	if(!can_render_bool) return new BufferedImage(50, 50, BufferedImage.TYPE_INT_ARGB);
	
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

	    for(int x = 0; x < layer.size(); x++) {
		for(int y = 0; y < layer.get(x).size(); y++) {
		    try {
			Map<Keyword, Object> tiledata = layer.get(x).get(y);
			Keyword current_tileset = (Keyword)tiledata.get(tileset);
			Map<Keyword, Object> tileset_meta = (Map<Keyword, Object>)peek_registry.invoke(current_tileset);
			List<List<BufferedImage>> tileset_surfaces = (List<List<BufferedImage>>)tileset_meta.get(images);
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

//     public static BufferedImage rotate(BufferedImage image, double angle) {
	
//     int w = image.getWidth(), h = image.getHeight();
//     int neww = (int)Math.floor(w*cos+h*sin), newh = (int)Math.floor(h*cos+w*sin);
//     GraphicsConfiguration gc = getDefaultConfiguration();
//     BufferedImage result = gc.createCompatibleImage(neww, newh, Transparency.TRANSLUCENT);
//     Graphics2D g = result.createGraphics();
//     g.translate((neww-w)/2, (newh-h)/2);
//     g.rotate(rad_rot, w/2, h/2);
//     g.drawRenderedImage(image, null);
//     g.dispose();
//     return result;
// }
    
    public static BufferedImage Rotate(BufferedImage img, int degrees)
    {
	double rad_rot = Math.toRadians((double)degrees);
	
	double sin = Math.abs(Math.sin(rad_rot)), cos = Math.abs(Math.cos(rad_rot));
	double w = img.getWidth(), h = img.getHeight();
	int neww = (int)Math.floor(w*cos+h*sin), newh = (int)Math.floor(h*cos+w*sin);
	
	// System.out.println("W and H: " + w + ", "+ h +"  |  newW and newH: " + neww + ", " + newh);
	
	BufferedImage new_img = new BufferedImage(neww, newh, BufferedImage.TYPE_INT_ARGB);
	Graphics2D g = new_img.createGraphics();

        g.translate(((int)(neww-w))/2, ((int)(newh-h)/2));
        g.rotate(rad_rot, (int)w/2, (int)h/2);
        // g.translate(-(((int)w) / 2), -(((int)h) / 2));
        // g2.drawImage(image, 0, 0, null);
	g.drawRenderedImage(img, null);

        g.dispose();


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
