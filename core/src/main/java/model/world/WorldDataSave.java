package model.world;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.jme3.math.ColorRGBA;
import com.jme3.scene.Geometry;
import com.jme3.scene.Node;
import com.jme3.scene.shape.Line;
import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityData;
import com.simsilica.es.EntityId;

import app.AppFacade;
import model.ES.component.Naming;
import model.ES.component.hierarchy.Parenting;
import model.ES.component.motion.PlanarStance;
import model.ES.component.motion.physic.EdgedCollisionShape;
import model.ES.component.motion.physic.Physic;
import model.ES.serial.EntityInstance;
import model.world.terrain.heightmap.HeightMapExplorer;
import model.world.terrain.heightmap.HeightMapNode;
import model.world.terrain.heightmap.Parcel;
import model.world.terrain.heightmap.Parcelling;
import util.LogUtil;
import util.geometry.geom2d.Point2D;
import util.geometry.geom2d.Segment2D;
import util.geometry.geom3d.Point3D;
import util.geometry.geom3d.Segment3D;
import util.geometry.geom3d.Triangle3D;
import util.math.Fraction;
import view.drawingProcessors.TerrainDrawer;
import view.material.MaterialManager;
import view.math.TranslateUtil;

public class WorldDataSave {
	private final EntityData ed;
	private final EntityId worldEntity;
	public EntityId getWorldEntity() {
		return worldEntity;
	}

	private final Node worldNode;

	private List<Region> drawnRegions = new ArrayList<Region>();
	private Map<Region, TerrainDrawer> drawers = new HashMap<>();
	private List<TerrainDrawer> drawersToAttach = new ArrayList<TerrainDrawer>();
	private List<TerrainDrawer> drawersToDetach = new ArrayList<TerrainDrawer>();

	private Region lastRegion;
	public Region getLastRegion() {
		return lastRegion;
	}

	private RegionLoader regionLoader = new RegionLoader();
	
	public WorldDataSave(EntityData ed) {
		this.ed = ed;
		worldNode = new Node("World");
		AppFacade.getMainSceneNode().attachChild(worldNode);
		worldEntity = ed.createEntity();
		ed.setComponent(worldEntity, new Naming("World"));
	}
	
	public void setCoord(Point2D coord){
		Region actualRegion = regionLoader.getRegion(coord);
		
		if(actualRegion != lastRegion){
			// We pass from a region to another
			lastRegion = actualRegion;
			new Thread(() -> loadAndDrawRegionsAround(new Point2D(coord))).start();
			//loadAndDrawRegionsAround(coord);
		}
	}
	
	private void loadAndDrawRegionsAround(Point2D coord){
		List<Region> toDraw = get9RegionsAround(coord);
		synchronized (drawnRegions) {
			for(Region r : toDraw){
				// create an entity for this region
				if(r.getEntityId() == null){
					EntityId eid = ed.createEntity();
					ed.setComponent(eid, new Naming("Region "+r.getId()));
					ed.setComponent(eid, new Parenting(worldEntity));
					r.setEntityId(eid);
				}
				
				if(!drawnRegions.contains(r)){
					drawnRegions.add(r);
					drawRegion(r);
				}
			}
			
			for(Region r : drawnRegions)
				if(!toDraw.contains(r) && !r.isModified())
					undrawRegion(r);
			drawnRegions = toDraw;
		}
	}
	
	public void attachDrawers(){
		synchronized (drawersToAttach) {
			for(TerrainDrawer d : drawersToAttach){
				worldNode.attachChild(d.mainNode);
			}
			drawersToAttach.clear();
		}
		synchronized (drawersToDetach) {
			for(TerrainDrawer d : drawersToDetach){
				worldNode.detachChild(d.mainNode);
			}
			drawersToDetach.clear();
		}
	}

	public void drawRegion(Region region){
		if(!drawers.containsKey(region))
			drawers.put(region, new TerrainDrawer(region.getTerrain(), region.getCoord()));
		RegionArtisan.drawRegion(ed, region);
		drawers.get(region).render();
		synchronized (drawersToAttach) {
			drawersToAttach.add(drawers.get(region));
		}
	}
	
	private void undrawRegion(Region region){
		RegionArtisan.undrawRegion(ed, region);
		synchronized (drawersToDetach) {
			drawersToDetach.add(drawers.get(region));
		}
	}
	
	public TerrainDrawer getTerrainDrawer(Region region){
		return drawers.get(region);
	}
	
	private List<Region> get9RegionsAround(Point2D coord){
		List<Region> res = new ArrayList<Region>();
		int r = Region.RESOLUTION;
		for(int x = -r; x <= r; x += r)
			for(int y = -r; y <= r; y += r)
				res.add(regionLoader.getRegion(coord.getAddition(x, y)));
		return res;
	}
	
	public List<Region> getRegions(Point2D coord){
		coord = new Point2D((int)Math.floor(coord.x), (int)Math.floor(coord.y));
		List<Region> res = new ArrayList<>();
		res.add(regionLoader.getRegion(coord));
		if(coord.x % Region.RESOLUTION == 0)
			res.add(regionLoader.getRegion(coord.getAddition(-1, 0)));
		if(coord.y % Region.RESOLUTION == 0)
			res.add(regionLoader.getRegion(coord.getAddition(0, -1)));
		if(coord.x % Region.RESOLUTION == 0 && coord.y % Region.RESOLUTION == 0)
			res.add(regionLoader.getRegion(coord.getAddition(-1, -1)));
		for(Region r : res)
			if(!drawnRegions.contains(r)){
				drawnRegions.add(r);
				drawRegion(r);
			}
		return res; 
	}
	
	/**
	 * find the height at given coordinate, plus the heights in neighboring regions if coordinate
	 * is above west or south border.
	 * @param coord
	 * @return
	 */
	public List<HeightMapNode> getHeights(Point2D coord){
		List<HeightMapNode> res = new ArrayList<>();
		for(Region r : getRegions(coord))
			res.add(r.getTerrain().getHeightMap().get(coord));
		return res;
	}

	public List<HeightMapNode> get4HeightsAround(Point2D coord){
		List<HeightMapNode> res = new ArrayList<>();
		res.addAll(getHeights(coord.getAddition(1, 0)));
		res.addAll(getHeights(coord.getAddition(-1, 0)));
		res.addAll(getHeights(coord.getAddition(0, 1)));
		res.addAll(getHeights(coord.getAddition(0, -1)));
		return res;
	}
	
	public void addEntityInstance(EntityInstance ei){
		for(EntityComponent comp : ei.getComps())
			if(comp instanceof PlanarStance){
				Point2D coord = ((PlanarStance)comp).getCoord();
				getRegions(coord).get(0).getEntities().add(ei);
			}
		ei.instanciate(ed, worldEntity);
	}
	
	public void saveDrawnRegions(){
		for(Region r : drawnRegions)
			RegionLoader.saveRegion(r);
	}
}