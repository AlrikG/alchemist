package model.ECS;

import java.util.HashMap;
import java.util.Map;

import com.simsilica.es.EntityComponent;
import com.simsilica.es.EntityId;
import com.simsilica.es.base.DefaultEntityData;

import javafx.application.Platform;
import model.ES.component.Naming;
import model.ES.component.Parenting;
import presenter.common.EntityNode;

/***
 * A Sepacialized EntityData that maintain a tree of nodes representing the hierarchy of the entities and their components
 * 
 * Used to observe all data changes
 * @author benoit
 *
 */
public class TraversableEntityData extends DefaultEntityData{
	private final EntityNode rootEntityNode;
	private final Map<EntityId, EntityNode> entityNodes = new HashMap<>();
	
	

	public TraversableEntityData() {
		rootEntityNode = new EntityNode(null, "root");
	}
	
	@Override
	public void setComponent(EntityId entityId, EntityComponent component) {
		EntityComponent lastComp = getComponent(entityId, component.getClass());
		super.setComponent(entityId, component);
		handleComponentChange(entityId, component.getClass(), lastComp, component);
	}
	
	@Override
	public boolean removeComponent(EntityId entityId, Class type) {
		EntityComponent lastComp = getComponent(entityId, type);
		handleComponentChange(entityId, type, lastComp, null);
		return super.removeComponent(entityId, type);
	}

	@Override
	public EntityId createEntity() {
		EntityId res = super.createEntity();
		Platform.runLater(() -> {
			EntityNode ep = new EntityNode(res, "Just created. Should not be seen.");
			rootEntityNode.childrenListProperty().add(ep);
			entityNodes.put(ep.getEntityId(), ep);
		});
		return res;
	}
	
	@Override
	public void removeEntity(EntityId eid) {
		super.removeEntity(eid);
		Platform.runLater(() -> {
			Parenting parenting = getComponent(eid, Parenting.class);
			removeNodeFromParent(getNode(eid), parenting);
			entityNodes.remove(eid);
		});
	}
	
	public EntityNode getRootNode() {
		return rootEntityNode;
	}
	
	public EntityNode getNode(EntityId eid){
		return entityNodes.get(eid);
	}
	
	private void removeNodeFromParent(EntityNode ep, Parenting parenting){
		if(parenting != null){
			EntityNode parentPresenter = getNode(parenting.getParent());
			if(parentPresenter != null)
				parentPresenter.childrenListProperty().remove(ep);
		} else
			rootEntityNode.childrenListProperty().remove(ep);
	}
	
	private void handleComponentChange(EntityId eid, Class<? extends EntityComponent> compClass, EntityComponent lastComp, EntityComponent newComp){
		Platform.runLater(() -> {
			if(!entityNodes.containsKey(eid)){
				EntityNode ep = new EntityNode(eid, "Just created. Should not be seen.");
				rootEntityNode.childrenListProperty().add(ep);
				entityNodes.put(ep.getEntityId(), ep);
			}
		});

		Platform.runLater(() -> {
				if(compClass == Parenting.class){
					removeNodeFromParent(getNode(eid), (Parenting)lastComp);
					if(newComp != null){
						// The entity has a new parent. We register the entity in the new parent's presenter's children list
						EntityNode newParent = entityNodes.get(((Parenting)newComp).getParent());
						newParent.childrenListProperty().add(getNode(eid));
					}
				} else if(compClass == Naming.class){
					if(newComp != null)
						getNode(eid).nameProperty().setValue(((Naming)newComp).getName());
				}
				
				EntityNode ep = getNode(eid);
				if(ep != null){
					// we set the component instead of remove&add to get the correct event for listeners
					if(lastComp != null && newComp == null){
						// component is removed
						ep.componentListProperty().remove(lastComp);
					} else if(lastComp != null && newComp != null){
						// component is replaced
						int index = ep.componentListProperty().indexOf(lastComp);
						ep.componentListProperty().set(index, newComp);
					} else if(lastComp == null && newComp != null){
						// component is added
						ep.componentListProperty().add(newComp);
					}
				}
		});
	}
	
	public void setState(Map<EntityId, Map<Class<? extends EntityComponent>, EntityComponent>> entities){
		// we remove all entities that we can find
		// this trick seems ugly...
		long l = createEntity().getId();
		for(long i = 0; i <= l; i++)
			removeEntity(new EntityId(i));
		
		// then we set all components that have been stored by the observer
		for(EntityId eid : entities.keySet()){
			Map<Class<? extends EntityComponent>, EntityComponent> components = entities.get(eid);
			for(EntityComponent comp : components.values())
				setComponent(eid, comp);
		}
		// will it work with a new instance of entity data, where the entity Ids havn't already been created??
		// it remains to be tested
	}
}
