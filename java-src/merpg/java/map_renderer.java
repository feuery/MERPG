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
	layer_metadata_of = Clojure.var("merpg.mutable.registry-views", "layer-metadata-of!");
    
    public map_renderer( )// clojure.lang.Atom map_atom, Atom tileset_atom, Atom selectedtool_atom)
    { }

    // an instance of reagi-events
    public static BufferedImage render(Keyword map_id)
    {
	BufferedImage final_surface = null;
	Graphics2D final_g = null;
	try {
	    List<List<List<Map<Keyword, Object>>>> layers = (List<List<List<Map<Keyword, Object>>>>)renderable_layers_of.invoke(map_id);
	    List<Map<Keyword, Object>> layer_metas = (List<Map<Keyword, Object>>)layer_metadata_of.invoke(map_id);
	    int layer_count = layers.size();
	    int W, H;

	    for(int l = 0; l < layer_count; l++) {
		List<List<Map<Keyword, Object>>> layer = layers.get(l);
		// pixels
		int w_tiles = layer.size(),
		    h_tiles = layer.get(0).size(),
		    opacity = ((Long)layer_metas.get(l).get(Keyword.intern("opacity"))).intValue();

		W = w_tiles * TILEW;
		H = h_tiles * TILEW;
		
		BufferedImage layer_surface = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
		final_surface = final_surface == null? new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB): final_surface;
		final_g = final_g == null? final_surface.createGraphics(): final_g;
		Graphics2D g = layer_surface.createGraphics();

		g.setColor(Color.BLACK);
		g.fill(new Rectangle(0, 0, W, H));

		for(int x = 0; x < w_tiles; x++) {
		    for(int y = 0; y < h_tiles; y++) {
			Map<Keyword, Object> tile_data = layer.get(x).get(y);
			Keyword tileset = (Keyword)tile_data.get(Keyword.intern("tileset"));

			Map<Keyword, Object> tileset_meta = (Map<Keyword, Object>)peek_registry.invoke(tileset);
			List<List<BufferedImage>> tileset_surfaces = (List<List<BufferedImage>>)tileset_meta.get(Keyword.intern("images"));
			int tileset_x = ((Long)tile_data.get(Keyword.intern("x"))).intValue(),
			    tileset_y = ((Long)tile_data.get(Keyword.intern("y"))).intValue(),
			    rotation = ((Long)tile_data.get(Keyword.intern("rotation"))).intValue() * 90;
			BufferedImage tile_surface;

			if(rotation != 0) 
			    tile_surface = Rotate(tileset_surfaces.get(tileset_x).get(tileset_y), rotation);
			else
			    tile_surface = tileset_surfaces.get(tileset_x).get(tileset_y);
			
			g.drawImage(tile_surface, null, x * TILEW, y * TILEW);
		    }
		}			

		if(opacity <= 0 || opacity >= 255)
		    final_g.drawImage(layer_surface, null, 0, 0);
		else final_g.drawImage(SetOpacity(layer_surface, (double)opacity), null, 0, 0);
	    }

	    // TODO implement hit-data rendering when the new tool infrastructure is up and running
	    

	}
		catch(Exception ex) {
		    ex.printStackTrace();
		}	

	    return final_surface;
    }
    
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
