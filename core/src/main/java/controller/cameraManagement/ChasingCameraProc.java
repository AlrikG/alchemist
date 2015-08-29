package controller.cameraManagement;

import util.geometry.geom2d.Point2D;

import com.simsilica.es.Entity;

import model.ES.component.camera.ChasingCamera;
import model.ES.component.motion.PlanarMotionCapacity;
import model.ES.component.motion.PlanarStance;
import controller.CameraManager;
import controller.entityAppState.Processor;

public class ChasingCameraProc extends Processor {
	
	private final CameraManager camManager;

	public ChasingCameraProc(CameraManager camManager) {
		this.camManager = camManager;
	}
	
	@Override
	protected void registerSets() {
		register(PlanarStance.class, ChasingCamera.class);
	}
	
	@Override
	protected void onEntityAdded(Entity e, float elapsedTime){
		manage(e, elapsedTime);
	}

	@Override
	protected void onEntityUpdated(Entity e, float elapsedTime){
		manage(e, elapsedTime);
	}
	
	private void manage(Entity e, float elapsedTime){
		PlanarStance stance = e.get(PlanarStance.class);
		PlanarMotionCapacity capacity = e.get(PlanarMotionCapacity.class);
		ChasingCamera cam = e.get(ChasingCamera.class);
		PlanarStance targetStance = entityData.getComponent(cam.getEntityToChase(), PlanarStance.class);
		
		Point2D toTarget = targetStance.getCoord().getSubtraction(stance.getCoord());
		
		double minBrakingDistance = (cam.getSpeed()*cam.getSpeed())/(cam.getDeceleration()*2);

		double newSpeed;
		if(minBrakingDistance >= toTarget.getLength())
			// deceleration
			newSpeed = Math.max(0, cam.getSpeed()-cam.getDeceleration()*elapsedTime);
		else
			// acceleration
			newSpeed = Math.min(cam.getMaxSpeed(), cam.getSpeed()+cam.getAcceleration()*elapsedTime);
			
		toTarget = toTarget.getScaled(newSpeed*elapsedTime);
		
		Point2D newCoord = stance.getCoord().getAddition(toTarget);
		
		
		setComp(e, new ChasingCamera(cam.getEntityToChase(), cam.getMaxSpeed(), newSpeed, cam.getAcceleration(), cam.getDeceleration()));
		setComp(e, new PlanarStance(newCoord, stance.getOrientation(), stance.getElevation(), stance.getUpVector()));
		
		camManager.setLocation(newCoord.get3D(stance.getElevation()));
		camManager.lookAt(targetStance.getCoord().get3D(targetStance.getElevation()));
		
		StringBuilder sb = new StringBuilder(this.getClass().getSimpleName() + System.lineSeparator());
		sb.append("    camera pos : "+ newCoord.get3D(stance.getElevation()) + System.lineSeparator());
		sb.append("    speed : " + newSpeed + System.lineSeparator());
		sb.append("    accelerate : " + (minBrakingDistance >= toTarget.getLength()) + System.lineSeparator());
		app.getDebugger().add(sb.toString());

		
	}

}